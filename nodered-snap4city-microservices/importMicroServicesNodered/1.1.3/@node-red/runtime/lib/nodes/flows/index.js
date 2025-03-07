/**
 * Copyright JS Foundation and other contributors, http://js.foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

var clone = require("clone");
var when = require("when");

var Flow = require('./Flow');

var typeRegistry = require("@node-red/registry");
var deprecated = typeRegistry.deprecated;


var context = require("../context")
var credentials = require("../credentials");

var flowUtil = require("./util");
var log;
var events = require("../../events");
var redUtil = require("@node-red/util").util;

var storage = null;
var settings = null;

var activeConfig = null;
var activeFlowConfig = null;

var activeFlows = {};
var started = false;
var credentialsPendingReset = false;

var activeNodesToFlow = {};

var typeEventRegistered = false;

function init(runtime) {
    if (started) {
        throw new Error("Cannot init without a stop");
    }
    settings = runtime.settings;
    storage = runtime.storage;
    log = runtime.log;
    started = false;
    if (!typeEventRegistered) {
        events.on('type-registered',function(type) {
            if (activeFlowConfig && activeFlowConfig.missingTypes.length > 0) {
                var i = activeFlowConfig.missingTypes.indexOf(type);
                if (i != -1) {
                    log.info(log._("nodes.flows.registered-missing", {type:type}));
                    activeFlowConfig.missingTypes.splice(i,1);
                    if (activeFlowConfig.missingTypes.length === 0 && started) {
                        events.emit("runtime-event",{id:"runtime-state",retain: true});
                        start();
                    }
                }
            }
        });
        typeEventRegistered = true;
    }
    Flow.init(runtime);
    flowUtil.init(runtime);
}

function loadFlows() {
    var config;
    return storage.getFlows().then(function(_config) {
        config = _config;
        log.debug("loaded flow revision: "+config.rev);
        return credentials.load(config.credentials).then(function() {
            events.emit("runtime-event",{id:"runtime-state",retain:true});
            return config;
        });
    }).catch(function(err) {
        if (err.code === "credentials_load_failed" && !storage.projects) {
            // project disabled, credential load failed
            credentialsPendingReset = true;
            log.warn(log._("nodes.flows.error",{message:err.toString()}));
            events.emit("runtime-event",{id:"runtime-state",payload:{type:"warning",error:err.code,text:"notification.warnings.credentials_load_failed_reset"},retain:true});
            return config;
        } else {
            activeConfig = null;
            events.emit("runtime-event",{id:"runtime-state",payload:{type:"warning",error:err.code,project:err.project,text:"notification.warnings."+err.code},retain:true});
            if (err.code === "project_not_found") {
                log.warn(log._("storage.localfilesystem.projects.project-not-found",{project:err.project}));
            } else {
                log.warn(log._("nodes.flows.error",{message:err.toString()}));
            }
            throw err;
        }
    });
}
function load(forceStart) {
    if (forceStart && settings.safeMode) {
        // This is a force reload from the API - disable safeMode
        delete settings.safeMode;
    }
    return setFlows(null,null,"load",false,forceStart);
}

/*
 * _config - new node array configuration
 * _credentials - new credentials configuration (optional)
 * type - full/nodes/flows/load (default full)
 * muteLog - don't emit the standard log messages (used for individual flow api)
 */
function setFlows(_config,_credentials,type,muteLog,forceStart) {
    if (typeof _credentials === "string") {
        type = _credentials;
        _credentials = null;
    }
    type = type||"full";
    if (settings.safeMode) {
        if (type !== "load") {
            // If in safeMode, the flows are stopped. We cannot do a modified nodes/flows
            // type deploy as nothing is running. Can only do a "load" or "full" deploy.
            // The "load" case is already handled in `load()` to distinguish between
            // startup-load and api-request-load.
            type = "full";
            delete settings.safeMode;
        }
    }

    var configSavePromise = null;
    var config = null;
    var diff;
    var newFlowConfig;
    var isLoad = false;
    if (type === "load") {
        isLoad = true;
        configSavePromise = loadFlows().then(function(_config) {
            config = clone(_config.flows);
            newFlowConfig = flowUtil.parseConfig(clone(config));
            type = "full";
            return _config.rev;
        });
    } else {
        // Clone the provided config so it can be manipulated
        config = clone(_config);
        // Parse the configuration
        newFlowConfig = flowUtil.parseConfig(clone(config));
        // Generate a diff to identify what has changed
        diff = flowUtil.diffConfigs(activeFlowConfig,newFlowConfig);

        // Now the flows have been compared, remove any credentials from newFlowConfig
        // so they don't cause false-positive diffs the next time a flow is deployed
        for (var id in newFlowConfig.allNodes) {
            if (newFlowConfig.allNodes.hasOwnProperty(id)) {
                delete newFlowConfig.allNodes[id].credentials;
            }
        }
        var credsDirty;

        if (_credentials) {
            // A full set of credentials have been provided. Use those instead
            configSavePromise = credentials.load(_credentials);
            credsDirty = true;
        } else {
            // Allow the credential store to remove anything no longer needed
            credentials.clean(config);

            // Remember whether credentials need saving or not
            var credsDirty = credentials.dirty();

            configSavePromise = Promise.resolve();
        }

        // Get the latest credentials and ask storage to save them (if needed)
        // as well as the new flow configuration.
        configSavePromise = configSavePromise.then(function() {
            return credentials.export()
        }).then(function(creds) {
            var saveConfig = {
                flows: config,
                credentialsDirty:credsDirty,
                credentials: creds
            }
            return storage.saveFlows(saveConfig);
        });
    }

    return configSavePromise
        .then(function(flowRevision) {
            if (!isLoad) {
                log.debug("saved flow revision: "+flowRevision);
            }
            activeConfig = {
                flows:config,
                rev:flowRevision
            };
            activeFlowConfig = newFlowConfig;
            if (forceStart || started) {
                // Flows are running (or should be)

                // Stop the active flows (according to deploy type and the diff)
                return stop(type,diff,muteLog).then(() => {
                    // Once stopped, allow context to remove anything no longer needed
                    return context.clean(activeFlowConfig)
                }).then(() => {
                    // Start the active flows
                    start(type,diff,muteLog).then(() => {
                        events.emit("runtime-event",{id:"runtime-deploy",payload:{revision:flowRevision},retain: true});
                    });
                    // Return the new revision asynchronously to the actual start
                    return flowRevision;
                }).catch(function(err) { })
            } else {
                events.emit("runtime-event",{id:"runtime-deploy",payload:{revision:flowRevision},retain: true});
            }
        });
}

function getNode(id) {
    var node;
    if (activeNodesToFlow[id] && activeFlows[activeNodesToFlow[id]]) {
        return activeFlows[activeNodesToFlow[id]].getNode(id,true);
    }
    for (var flowId in activeFlows) {
        if (activeFlows.hasOwnProperty(flowId)) {
            node = activeFlows[flowId].getNode(id,true);
            if (node) {
                return node;
            }
        }
    }
    return null;
}

function eachNode(cb) {
    for (var id in activeFlowConfig.allNodes) {
        if (activeFlowConfig.allNodes.hasOwnProperty(id)) {
            cb(activeFlowConfig.allNodes[id]);
        }
    }
}

function getFlows() {
    return activeConfig;
}

function start(type,diff,muteLog) {
    type = type||"full";
    started = true;
    var i;
    // If there are missing types, report them, emit the necessary runtime event and return
    if (activeFlowConfig.missingTypes.length > 0) {
        log.info(log._("nodes.flows.missing-types"));
        var knownUnknowns = 0;
        for (i=0;i<activeFlowConfig.missingTypes.length;i++) {
            var nodeType = activeFlowConfig.missingTypes[i];
            var info = deprecated.get(nodeType);
            if (info) {
                log.info(log._("nodes.flows.missing-type-provided",{type:activeFlowConfig.missingTypes[i],module:info.module}));
                knownUnknowns += 1;
            } else {
                log.info(" - "+activeFlowConfig.missingTypes[i]);
            }
        }
        if (knownUnknowns > 0) {
            log.info(log._("nodes.flows.missing-type-install-1"));
            log.info("  npm install <module name>");
            log.info(log._("nodes.flows.missing-type-install-2"));
            log.info("  "+settings.userDir);
        }
        events.emit("runtime-event",{id:"runtime-state",payload:{error:"missing-types", type:"warning",text:"notification.warnings.missing-types",types:activeFlowConfig.missingTypes},retain:true});
        return Promise.resolve();
    }

    // In safe mode, don't actually start anything, emit the necessary runtime event and return
    if (settings.safeMode) {
        log.info("*****************************************************************")
        log.info(log._("nodes.flows.safe-mode"));
        log.info("*****************************************************************")
        events.emit("runtime-event",{id:"runtime-state",payload:{error:"safe-mode", type:"warning",text:"notification.warnings.safe-mode"},retain:true});
        return Promise.resolve();
    }

    if (!muteLog) {
        if (type !== "full") {
            log.info(log._("nodes.flows.starting-modified-"+type));
        } else {
            log.info(log._("nodes.flows.starting-flows"));
        }
    }

    var id;
    if (type === "full") {
        // A full start means everything should

        // Check the 'global' flow is running
        if (!activeFlows['global']) {
            log.debug("red/nodes/flows.start : starting flow : global");
            activeFlows['global'] = Flow.create(flowAPI,activeFlowConfig);
        }

        // Check each flow in the active configuration
        for (id in activeFlowConfig.flows) {
            if (activeFlowConfig.flows.hasOwnProperty(id)) {
                if (!activeFlowConfig.flows[id].disabled && !activeFlows[id]) {
                    // This flow is not disabled, nor is it currently active, so create it
                    activeFlows[id] = Flow.create(flowAPI,activeFlowConfig,activeFlowConfig.flows[id]);
                    log.debug("red/nodes/flows.start : starting flow : "+id);
                } else {
                    log.debug("red/nodes/flows.start : not starting disabled flow : "+id);
                }
            }
        }
    } else {
        // A modified-type deploy means restarting things that have changed

        // Update the global flow
        activeFlows['global'].update(activeFlowConfig,activeFlowConfig);
        for (id in activeFlowConfig.flows) {
            if (activeFlowConfig.flows.hasOwnProperty(id)) {
                if (!activeFlowConfig.flows[id].disabled) {
                    if (activeFlows[id]) {
                        // This flow exists and is not disabled, so update it
                        activeFlows[id].update(activeFlowConfig,activeFlowConfig.flows[id]);
                    } else {
                        // This flow didn't previously exist, so create it
                        activeFlows[id] = Flow.create(flowAPI,activeFlowConfig,activeFlowConfig.flows[id]);
                        log.debug("red/nodes/flows.start : starting flow : "+id);
                    }
                } else {
                    log.debug("red/nodes/flows.start : not starting disabled flow : "+id);
                }
            }
        }
    }
    // Having created or updated all flows, now start them.
    for (id in activeFlows) {
        if (activeFlows.hasOwnProperty(id)) {
            try {
                activeFlows[id].start(diff);

                // Create a map of node id to flow id and also a subflowInstance lookup map
                var activeNodes = activeFlows[id].getActiveNodes();
                Object.keys(activeNodes).forEach(function(nid) {
                    activeNodesToFlow[nid] = id;
                });
            } catch(err) {
                console.log(err.stack);
            }
        }
    }
    events.emit("nodes-started");

    if (credentialsPendingReset === true) {
        credentialsPendingReset = false;
    } else {
        events.emit("runtime-event",{id:"runtime-state",retain:true});
    }

    if (!muteLog) {
        if (type !== "full") {
            log.info(log._("nodes.flows.started-modified-"+type));
        } else {
            log.info(log._("nodes.flows.started-flows"));
        }
    }
    return Promise.resolve();
}

function stop(type,diff,muteLog) {
    if (!started) {
        return Promise.resolve();
    }
    type = type||"full";
    diff = diff||{
        added:[],
        changed:[],
        removed:[],
        rewired:[],
        linked:[]
    };
    if (!muteLog) {
        if (type !== "full") {
            log.info(log._("nodes.flows.stopping-modified-"+type));
        } else {
            log.info(log._("nodes.flows.stopping-flows"));
        }
    }
    started = false;
    var promises = [];
    var stopList;
    var removedList = diff.removed;
    if (type === 'nodes') {
        stopList = diff.changed.concat(diff.removed);
    } else if (type === 'flows') {
        stopList = diff.changed.concat(diff.removed).concat(diff.linked);
    }

    for (var id in activeFlows) {
        if (activeFlows.hasOwnProperty(id)) {
            var flowStateChanged = diff && (diff.added.indexOf(id) !== -1 || diff.removed.indexOf(id) !== -1);
            log.debug("red/nodes/flows.stop : stopping flow : "+id);
            promises.push(activeFlows[id].stop(flowStateChanged?null:stopList,removedList));
            if (type === "full" || flowStateChanged || diff.removed.indexOf(id)!==-1) {
                delete activeFlows[id];
            }
        }
    }

    return Promise.all(promises).then(function() {
        for (id in activeNodesToFlow) {
            if (activeNodesToFlow.hasOwnProperty(id)) {
                if (!activeFlows[activeNodesToFlow[id]]) {
                    delete activeNodesToFlow[id];
                }
            }
        }
        if (stopList) {
            stopList.forEach(function(id) {
                delete activeNodesToFlow[id];
            });
        }
        if (!muteLog) {
            if (type !== "full") {
                log.info(log._("nodes.flows.stopped-modified-"+type));
            } else {
                log.info(log._("nodes.flows.stopped-flows"));
            }
        }
        events.emit("nodes-stopped");
    });
}


function checkTypeInUse(id) {
    var nodeInfo = typeRegistry.getNodeInfo(id);
    if (!nodeInfo) {
        throw new Error(log._("nodes.index.unrecognised-id", {id:id}));
    } else {
        var inUse = {};
        var config = getFlows();
        config.flows.forEach(function(n) {
            inUse[n.type] = (inUse[n.type]||0)+1;
        });
        var nodesInUse = [];
        nodeInfo.types.forEach(function(t) {
            if (inUse[t]) {
                nodesInUse.push(t);
            }
        });
        if (nodesInUse.length > 0) {
            var msg = nodesInUse.join(", ");
            var err = new Error(log._("nodes.index.type-in-use", {msg:msg}));
            err.code = "type_in_use";
            throw err;
        }
    }
}

function updateMissingTypes() {
    var subflowInstanceRE = /^subflow:(.+)$/;
    activeFlowConfig.missingTypes = [];

    for (var id in activeFlowConfig.allNodes) {
        if (activeFlowConfig.allNodes.hasOwnProperty(id)) {
            var node = activeFlowConfig.allNodes[id];
            if (node.type !== 'tab' && node.type !== 'subflow') {
                var subflowDetails = subflowInstanceRE.exec(node.type);
                if ( (subflowDetails && !activeFlowConfig.subflows[subflowDetails[1]]) || (!subflowDetails && !typeRegistry.get(node.type)) ) {
                    if (activeFlowConfig.missingTypes.indexOf(node.type) === -1) {
                        activeFlowConfig.missingTypes.push(node.type);
                    }
                }
            }
        }
    }
}

function addFlow(flow) {
    var i,node;
    if (!flow.hasOwnProperty('nodes')) {
        throw new Error('missing nodes property');
    }
    flow.id = redUtil.generateId();

    var tabNode = {
        type:'tab',
        label:flow.label,
        id:flow.id
    }
    if (flow.hasOwnProperty('info')) {
        tabNode.info = flow.info;
    }
    if (flow.hasOwnProperty('disabled')) {
        tabNode.disabled = flow.disabled;
    }

    var nodes = [tabNode];

    for (i=0;i<flow.nodes.length;i++) {
        node = flow.nodes[i];
        if (activeFlowConfig.allNodes[node.id]) {
            // TODO nls
            return when.reject(new Error('duplicate id'));
        }
        if (node.type === 'tab' || node.type === 'subflow') {
            return when.reject(new Error('invalid node type: '+node.type));
        }
        node.z = flow.id;
        nodes.push(node);
    }
    if (flow.configs) {
        for (i=0;i<flow.configs.length;i++) {
            node = flow.configs[i];
            if (activeFlowConfig.allNodes[node.id]) {
                // TODO nls
                return when.reject(new Error('duplicate id'));
            }
            if (node.type === 'tab' || node.type === 'subflow') {
                return when.reject(new Error('invalid node type: '+node.type));
            }
            node.z = flow.id;
            nodes.push(node);
        }
    }
    var newConfig = clone(activeConfig.flows);
    newConfig = newConfig.concat(nodes);

    return setFlows(newConfig,null,'flows',true).then(function() {
        log.info(log._("nodes.flows.added-flow",{label:(flow.label?flow.label+" ":"")+"["+flow.id+"]"}));
        return flow.id;
    });
}

function getFlow(id) {
    var flow;
    if (id === 'global') {
        flow = activeFlowConfig;
    } else {
        flow = activeFlowConfig.flows[id];
    }
    if (!flow) {
        return null;
    }
    var result = {
        id: id
    };
    if (flow.label) {
        result.label = flow.label;
    }
    if (flow.hasOwnProperty('disabled')) {
        result.disabled = flow.disabled;
    }
    if (flow.hasOwnProperty('info')) {
        result.info = flow.info;
    }
    if (id !== 'global') {
        result.nodes = [];
    }
    if (flow.nodes) {
        var nodeIds = Object.keys(flow.nodes);
        if (nodeIds.length > 0) {
            result.nodes = nodeIds.map(function(nodeId) {
                var node = clone(flow.nodes[nodeId]);
                if (node.type === 'link out') {
                    delete node.wires;
                }
                return node;
            })
        }
    }
    if (flow.configs) {
        var configIds = Object.keys(flow.configs);
        result.configs = configIds.map(function(configId) {
            return clone(flow.configs[configId]);
        })
        if (result.configs.length === 0) {
            delete result.configs;
        }
    }
    if (flow.subflows) {
        var subflowIds = Object.keys(flow.subflows);
        result.subflows = subflowIds.map(function(subflowId) {
            var subflow = clone(flow.subflows[subflowId]);
            var nodeIds = Object.keys(subflow.nodes);
            subflow.nodes = nodeIds.map(function(id) {
                return subflow.nodes[id];
            });
            if (subflow.configs) {
                var configIds = Object.keys(subflow.configs);
                subflow.configs = configIds.map(function(id) {
                    return subflow.configs[id];
                })
            }
            delete subflow.instances;
            return subflow;
        });
        if (result.subflows.length === 0) {
            delete result.subflows;
        }
    }
    return result;
}

function updateFlow(id,newFlow) {
    var label = id;
    if (id !== 'global') {
        if (!activeFlowConfig.flows[id]) {
            var e = new Error();
            e.code = 404;
            throw e;
        }
        label = activeFlowConfig.flows[id].label;
    }
    var newConfig = clone(activeConfig.flows);
    var nodes;

    if (id === 'global') {
        // Remove all nodes whose z is not a known flow
        // When subflows can be owned by a flow, this logic will have to take
        // that into account
        newConfig = newConfig.filter(function(node) {
            return node.type === 'tab' || (node.hasOwnProperty('z') && activeFlowConfig.flows.hasOwnProperty(node.z));
        })

        // Add in the new config nodes
        nodes = newFlow.configs||[];
        if (newFlow.subflows) {
            // Add in the new subflows
            newFlow.subflows.forEach(function(sf) {
                nodes = nodes.concat(sf.nodes||[]).concat(sf.configs||[]);
                delete sf.nodes;
                delete sf.configs;
                nodes.push(sf);
            });
        }
    } else {
        newConfig = newConfig.filter(function(node) {
            return node.z !== id && node.id !== id;
        });
        var tabNode = {
            type:'tab',
            label:newFlow.label,
            id:id
        }
        if (newFlow.hasOwnProperty('info')) {
            tabNode.info = newFlow.info;
        }
        if (newFlow.hasOwnProperty('disabled')) {
            tabNode.disabled = newFlow.disabled;
        }

        nodes = [tabNode].concat(newFlow.nodes||[]).concat(newFlow.configs||[]);
        nodes.forEach(function(n) {
            n.z = id;
        });
    }

    newConfig = newConfig.concat(nodes);
    return setFlows(newConfig,null,'flows',true).then(function() {
        log.info(log._("nodes.flows.updated-flow",{label:(label?label+" ":"")+"["+id+"]"}));
    })
}

function removeFlow(id) {
    if (id === 'global') {
        // TODO: nls + error code
        throw new Error('not allowed to remove global');
    }
    var flow = activeFlowConfig.flows[id];
    if (!flow) {
        var e = new Error();
        e.code = 404;
        throw e;
    }

    var newConfig = clone(activeConfig.flows);
    newConfig = newConfig.filter(function(node) {
        return node.z !== id && node.id !== id;
    });

    return setFlows(newConfig,null,'flows',true).then(function() {
        log.info(log._("nodes.flows.removed-flow",{label:(flow.label?flow.label+" ":"")+"["+flow.id+"]"}));
    });
}

const flowAPI = {
    getNode: getNode,
    handleError: () => false,
    handleStatus: () => false,
    getSetting: k => flowUtil.getEnvVar(k)
}


module.exports = {
    init: init,

    /**
     * Load the current flow configuration from storage
     * @return a promise for the loading of the config
     */
    load: load,

    get:getNode,
    eachNode: eachNode,

    /**
     * Gets the current flow configuration
     */
    getFlows: getFlows,

    /**
     * Sets the current active config.
     * @param config the configuration to enable
     * @param type the type of deployment to do: full (default), nodes, flows, load
     * @return a promise for the saving/starting of the new flow
     */
    setFlows: setFlows,

    /**
     * Starts the current flow configuration
     */
    startFlows: start,

    /**
     * Stops the current flow configuration
     * @return a promise for the stopping of the flow
     */
    stopFlows: stop,

    get started() { return started },

    // handleError: handleError,
    // handleStatus: handleStatus,

    checkTypeInUse: checkTypeInUse,

    addFlow: addFlow,
    getFlow: getFlow,
    updateFlow: updateFlow,
    removeFlow: removeFlow,
    disableFlow:null,
    enableFlow:null,
    isDeliveryModeAsync: function() {
        // If settings is null, this is likely being run by unit tests
        return !settings || !settings.runtimeSyncDelivery
    }

};
