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

var path = require("path");
var fs = require("fs");
var clone = require("clone");
var util = require("util");

var registry = require("@node-red/registry");

var credentials = require("./credentials");
var flows = require("../flows");
var flowUtil = require("../flows/util")
var context = require("./context");
var Node = require("./Node");
var log;

const events = require("@node-red/util").events;

var settings;

/**
 * Registers a node constructor
 * @param nodeSet - the nodeSet providing the node (module/set)
 * @param type - the string type name
 * @param constructor - the constructor function for this node type
 * @param opts - optional additional options for the node
 */
function registerType(nodeSet,type,constructor,opts) {
    if (typeof type !== "string") {
        // This is someone calling the api directly, rather than via the
        // RED object provided to a node. Log a warning
        log.warn("["+nodeSet+"] Deprecated call to RED.runtime.nodes.registerType - node-set name must be provided as first argument");
        opts = constructor;
        constructor = type;
        type = nodeSet;
        nodeSet = "";
    }
    if (opts) {
        if (opts.credentials) {
            credentials.register(type,opts.credentials);
        }
        if (opts.settings) {
            try {
                settings.registerNodeSettings(type,opts.settings);
            } catch(err) {
                log.warn("["+type+"] "+err.message);
            }
        }
    }
    if(!(constructor.prototype instanceof Node)) {
        if(Object.getPrototypeOf(constructor.prototype) === Object.prototype) {
            util.inherits(constructor,Node);
        } else {
            var proto = constructor.prototype;
            while(Object.getPrototypeOf(proto) !== Object.prototype) {
                proto = Object.getPrototypeOf(proto);
            }
            //TODO: This is a partial implementation of util.inherits >= node v5.0.0
            //      which should be changed when support for node < v5.0.0 is dropped
            //      see: https://github.com/nodejs/node/pull/3455
            proto.constructor.super_ = Node;
            if(Object.setPrototypeOf) {
                Object.setPrototypeOf(proto, Node.prototype);
            } else {
                // hack for node v0.10
                proto.__proto__ = Node.prototype;
            }
        }
    }
    registry.registerType(nodeSet,type,constructor,opts);
}

/**
 * Called from a Node's constructor function, invokes the super-class
 * constructor and attaches any credentials to the node.
 * @param node the node object being created
 * @param def the instance definition for the node
 */
function createNode(node,def) {
    Node.call(node,def);
    var id = node.id;
    if (def._alias) {
        id = def._alias;
    }
    var creds = credentials.get(id);
    if (creds) {
        creds = clone(creds);
        //console.log("Attaching credentials to ",node.id);
        // allow $(foo) syntax to substitute env variables for credentials also...
        for (var p in creds) {
            if (creds.hasOwnProperty(p)) {
                flowUtil.mapEnvVarProperties(creds,p,node._flow,node);
            }
        }
        node.credentials = creds;
    } else if (credentials.getDefinition(node.type)) {
        node.credentials = {};
    }
}

function registerSubflow(nodeSet, subflow) {
    // TODO: extract credentials definition from subflow properties
    var registeredType = registry.registerSubflow(nodeSet,subflow);

    if (subflow.env) {
        var creds = {};
        var hasCreds = false;
        subflow.env.forEach(e => {
            if (e.type === "cred") {
                creds[e.name] = {type: "password"};
                hasCreds = true;
            }
        })
        if (hasCreds) {
            credentials.register(registeredType.type,creds);
        }
    }
}

function init(runtime) {
    settings = runtime.settings;
    log = runtime.log;
    credentials.init(runtime);
    flows.init(runtime);
    registry.init(runtime);
    context.init(runtime.settings);
}

function disableNode(id) {
    flows.checkTypeInUse(id);
    return registry.disableNode(id).then(function(info) {
        reportNodeStateChange(info,false);
        return info;
    });
}

function enableNode(id) {
    return registry.enableNode(id).then(function(info) {
        reportNodeStateChange(info,true);
        return info;
    });
}

function reportNodeStateChange(info,enabled) {
    if (info.enabled === enabled && !info.err) {
        events.emit("runtime-event",{id:"node/"+(enabled?"enabled":"disabled"),retain:false,payload:info});
        log.info(" "+log._("api.nodes."+(enabled?"enabled":"disabled")));
        for (var i=0;i<info.types.length;i++) {
            log.info(" - "+info.types[i]);
        }
    } else if (enabled && info.err) {
    log.warn(log._("api.nodes.error-enable"));
        log.warn(" - "+info.name+" : "+info.err);
    }
}

function installModule(module,version,url) {
    return registry.installModule(module,version,url).then(function(info) {
        if (info.pending_version) {
            events.emit("runtime-event",{id:"node/upgraded",retain:false,payload:{module:info.name,version:info.pending_version}});
        } else {
            events.emit("runtime-event",{id:"node/added",retain:false,payload:info.nodes});
        }
        return info;
    });
}

//S4C Nodes Start
function installMicroservice(url) {
    return registry.installMicroservice(url).then(function(info) {
        events.emit("runtime-event",{id:"node/added",retain:false,payload:info.nodes});
        return info;
    });
}
//S4C Nodes End

function uninstallModule(module) {
    var info = registry.getModuleInfo(module);
    if (!info || !info.user) {
        throw new Error(log._("nodes.index.unrecognised-module", {module:module}));
    } else {
        var nodeTypesToCheck = info.nodes.map(n => `${module}/${n.name}`);
        for (var i=0;i<nodeTypesToCheck.length;i++) {
            flows.checkTypeInUse(nodeTypesToCheck[i]);
        }
        return registry.uninstallModule(module).then(function(list) {
            events.emit("runtime-event",{id:"node/removed",retain:false,payload:list});
            return list;
        });
    }
}

module.exports = {
    // Lifecycle
    init: init,
    load: registry.load,

    // Node registry
    createNode: createNode,
    getNode: flows.get,
    eachNode: flows.eachNode,
    getContext: context.get,

    clearContext: context.clear,

    installerEnabled: registry.installerEnabled,
    installModule: installModule,
    uninstallModule: uninstallModule,

	//S4C Nodes Start
    installMicroservice: installMicroservice,
    //S4C Nodes End

    enableNode: enableNode,
    disableNode: disableNode,

    // Node type registry
    registerType: registerType,
    registerSubflow: registerSubflow,
    getType: registry.get,

    getNodeInfo: registry.getNodeInfo,
    getNodeList: registry.getNodeList,

    getModuleInfo: registry.getModuleInfo,

    getNodeConfigs: registry.getNodeConfigs,
    getNodeConfig: registry.getNodeConfig,
    getNodeIconPath: registry.getNodeIconPath,
    getNodeIcons: registry.getNodeIcons,
    getNodeExampleFlows: registry.getNodeExampleFlows,
    getNodeExampleFlowPath: registry.getNodeExampleFlowPath,
    getModuleResource: registry.getModuleResource,

    clearRegistry: registry.clear,
    cleanModuleList: registry.cleanModuleList,

    // Flow handling
    loadFlows:  flows.load,
    startFlows: flows.startFlows,
    stopFlows:  flows.stopFlows,
    setFlows:   flows.setFlows,
    getFlows:   flows.getFlows,

    addFlow:     flows.addFlow,
    getFlow:     flows.getFlow,
    updateFlow:  flows.updateFlow,
    removeFlow:  flows.removeFlow,
    // disableFlow: flows.disableFlow,
    // enableFlow:  flows.enableFlow,

    // Credentials
    addCredentials: credentials.add,
    getCredentials: credentials.get,
    deleteCredentials: credentials.delete,
    getCredentialDefinition: credentials.getDefinition,
    setCredentialSecret: credentials.setKey,
    clearCredentials: credentials.clear,
    exportCredentials: credentials.export,
    getCredentialKeyType: credentials.getKeyType,

    // Contexts
    loadContextsPlugin: context.load,
    closeContextsPlugin: context.close,
    listContextStores: context.listStores,
};
