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

/**
 * @mixin @node-red/runtime_nodes
 */

var fs = require("fs");

var runtime;

function putNode(node, enabled) {
    var info;
    var promise;
    if (!node.err && node.enabled === enabled) {
        promise = Promise.resolve(node);
    } else {
        if (enabled) {
            promise = runtime.nodes.enableNode(node.id);
        } else {
            promise = runtime.nodes.disableNode(node.id);
        }
    }
    return promise;
}

var api = module.exports = {
    init: function(_runtime) {
        runtime = _runtime;
    },


    /**
    * Gets the info of an individual node set
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {String} opts.id - the id of the node set to return
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<NodeInfo>} - the node information
    * @memberof @node-red/runtime_nodes
    */
    getNodeInfo: function(opts) {
        return new Promise(function(resolve,reject) {
            var id = opts.id;
            var result = runtime.nodes.getNodeInfo(id);
            if (result) {
                runtime.log.audit({event: "nodes.info.get",id:id}, opts.req);
                delete result.loaded;
                return resolve(result);
            } else {
                runtime.log.audit({event: "nodes.info.get",id:id,error:"not_found"}, opts.req);
                var err = new Error("Node not found");
                err.code = "not_found";
                err.status = 404;
                return reject(err);
            }
        })
    },

    /**
    * Gets the list of node modules installed in the runtime
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<NodeList>} - the list of node modules
    * @memberof @node-red/runtime_nodes
    */
    getNodeList: function(opts) {
        return new Promise(function(resolve,reject) {
            runtime.log.audit({event: "nodes.list.get"}, opts.req);
            return resolve(runtime.nodes.getNodeList());
        })
    },

    /**
    * Gets an individual node's html content
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {String} opts.id - the id of the node set to return
    * @param {String} opts.lang - the locale language to return
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<String>} - the node html content
    * @memberof @node-red/runtime_nodes
    */
    getNodeConfig: function(opts) {
        return new Promise(function(resolve,reject) {
            var id = opts.id;
            var lang = opts.lang;
            var result = runtime.nodes.getNodeConfig(id,lang);
            if (result) {
                runtime.log.audit({event: "nodes.config.get",id:id}, opts.req);
                return resolve(result);
            } else {
                runtime.log.audit({event: "nodes.config.get",id:id,error:"not_found"}, opts.req);
                var err = new Error("Node not found");
                err.code = "not_found";
                err.status = 404;
                return reject(err);
            }
        });
    },
    /**
    * Gets all node html content
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {String} opts.lang - the locale language to return
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<String>} - the node html content
    * @memberof @node-red/runtime_nodes
    */
    getNodeConfigs: function(opts) {
        return new Promise(function(resolve,reject) {
            runtime.log.audit({event: "nodes.configs.get"}, opts.req);
            return resolve(runtime.nodes.getNodeConfigs(opts.lang));
        });
    },

    /**
    * Gets the info of a node module
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {String} opts.module - the id of the module to return
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<ModuleInfo>} - the node module info
    * @memberof @node-red/runtime_nodes
    */
    getModuleInfo: function(opts) {
        return new Promise(function(resolve,reject) {
            var result = runtime.nodes.getModuleInfo(opts.module);
            if (result) {
                runtime.log.audit({event: "nodes.module.get",id:opts.module}, opts.req);
                return resolve(result);
            } else {
                runtime.log.audit({event: "nodes.module.get",id:opts.module,error:"not_found"}, opts.req);
                var err = new Error("Module not found");
                err.code = "not_found";
                err.status = 404;
                return reject(err);
            }
        })
    },

    /**
    * Install a new module into the runtime
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {String} opts.module - the id of the module to install
    * @param {String} opts.version - (optional) the version of the module to install
    * @param {String} opts.url - (optional) url to install
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<ModuleInfo>} - the node module info
    * @memberof @node-red/runtime_nodes
    */
    addModule: function(opts) {
        return new Promise(function(resolve,reject) {
            if (!runtime.settings.available()) {
                runtime.log.audit({event: "nodes.install",error:"settings_unavailable"}, opts.req);
                var err = new Error("Settings unavailable");
                err.code = "settings_unavailable";
                err.status = 400;
                return reject(err);
            }
            if (opts.module) {
                var existingModule = runtime.nodes.getModuleInfo(opts.module);
                if (existingModule) {
                    if (!opts.version || existingModule.version === opts.version) {
                        runtime.log.audit({event: "nodes.install",module:opts.module, version:opts.version, error:"module_already_loaded"}, opts.req);
                        var err = new Error("Module already loaded");
                        err.code = "module_already_loaded";
                        err.status = 400;
                        return reject(err);
                    }
                }
                runtime.nodes.installModule(opts.module,opts.version,opts.url).then(function(info) {
                    runtime.log.audit({event: "nodes.install",module:opts.module,version:opts.version,url:opts.url}, opts.req);
                    return resolve(info);
                }).catch(function(err) {
                    if (err.code === 404) {
                        runtime.log.audit({event: "nodes.install",module:opts.module,version:opts.version,url:opts.url,error:"not_found"}, opts.req);
                        // TODO: code/status
                        err.status = 404;
                    } else if (err.code) {
                        err.status = 400;
                        runtime.log.audit({event: "nodes.install",module:opts.module,version:opts.version,url:opts.url,error:err.code}, opts.req);
                    } else {
                        err.status = 400;
                        runtime.log.audit({event: "nodes.install",module:opts.module,version:opts.version,url:opts.url,error:err.code||"unexpected_error",message:err.toString()}, opts.req);
                    }
                    return reject(err);
                })
            } else {
                runtime.log.audit({event: "nodes.install",module:opts.module,error:"invalid_request"}, opts.req);
                var err = new Error("Invalid request");
                err.code = "invalid_request";
                err.status = 400;
                return reject(err);
            }

        });
    },
    /**
    * Removes a module from the runtime
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {String} opts.module - the id of the module to remove
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise} - resolves when complete
    * @memberof @node-red/runtime_nodes
    */
    removeModule: function(opts) {
        return new Promise(function(resolve,reject) {
            if (!runtime.settings.available()) {
                runtime.log.audit({event: "nodes.install",error:"settings_unavailable"}, opts.req);
                var err = new Error("Settings unavailable");
                err.code = "settings_unavailable";
                err.status = 400;
                return reject(err);
            }
            var module = runtime.nodes.getModuleInfo(opts.module);
            if (!module) {
                runtime.log.audit({event: "nodes.remove",module:opts.module,error:"not_found"}, opts.req);
                var err = new Error("Module not found");
                err.code = "not_found";
                err.status = 404;
                return reject(err);
            }
            try {
                runtime.nodes.uninstallModule(opts.module).then(function() {
                    runtime.log.audit({event: "nodes.remove",module:opts.module}, opts.req);
                    resolve();
                }).catch(function(err) {
                    err.status = 400;
                    runtime.log.audit({event: "nodes.remove",module:opts.module,error:err.code||"unexpected_error",message:err.toString()}, opts.req);
                    return reject(err);
                })
            } catch(error) {
                runtime.log.audit({event: "nodes.remove",module:opts.module,error:error.code||"unexpected_error",message:error.toString()}, opts.req);
                error.status = 400;
                return reject(error);
            }
        });
    },

    /**
    * Enables or disables a module in the runtime
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {String} opts.module - the id of the module to enable or disable
    * @param {String} opts.enabled - whether the module should be enabled or disabled
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<ModuleInfo>} - the module info object
    * @memberof @node-red/runtime_nodes
    */
    setModuleState: function(opts) {
        var mod = opts.module;
        return new Promise(function(resolve,reject) {
            if (!runtime.settings.available()) {
                runtime.log.audit({event: "nodes.module.set",error:"settings_unavailable"}, opts.req);
                var err = new Error("Settings unavailable");
                err.code = "settings_unavailable";
                err.status = 400;
                return reject(err);
            }
            try {
                var module = runtime.nodes.getModuleInfo(mod);
                if (!module) {
                    runtime.log.audit({event: "nodes.module.set",module:mod,error:"not_found"}, opts.req);
                    var err = new Error("Module not found");
                    err.code = "not_found";
                    err.status = 404;
                    return reject(err);
                }

                var nodes = module.nodes;
                var promises = [];
                for (var i = 0; i < nodes.length; ++i) {
                    promises.push(putNode(nodes[i],opts.enabled));
                }
                Promise.all(promises).then(function() {
                    return resolve(runtime.nodes.getModuleInfo(mod));
                }).catch(function(err) {
                    err.status = 400;
                    return reject(err);
                });
            } catch(error) {
                runtime.log.audit({event: "nodes.module.set",module:mod,enabled:opts.enabled,error:error.code||"unexpected_error",message:error.toString()}, opts.req);
                error.status = 400;
                return reject(error);
            }
        });
    },

    /**
    * Enables or disables a n individual node-set in the runtime
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {String} opts.id - the id of the node-set to enable or disable
    * @param {String} opts.enabled - whether the module should be enabled or disabled
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<ModuleInfo>} - the module info object
    * @memberof @node-red/runtime_nodes
    */
    setNodeSetState: function(opts) {
        return new Promise(function(resolve,reject) {
            if (!runtime.settings.available()) {
                runtime.log.audit({event: "nodes.info.set",error:"settings_unavailable"}, opts.req);
                var err = new Error("Settings unavailable");
                err.code = "settings_unavailable";
                err.status = 400;
                return reject(err);
            }

            var id = opts.id;
            var enabled = opts.enabled;
            try {
                var node = runtime.nodes.getNodeInfo(id);
                if (!node) {
                    runtime.log.audit({event: "nodes.info.set",id:id,error:"not_found"}, opts.req);
                    var err = new Error("Node not found");
                    err.code = "not_found";
                    err.status = 404;
                    return reject(err);
                } else {
                    delete node.loaded;
                    putNode(node,enabled).then(function(result) {
                        runtime.log.audit({event: "nodes.info.set",id:id,enabled:enabled}, opts.req);
                        return resolve(result);
                    }).catch(function(err) {
                        runtime.log.audit({event: "nodes.info.set",id:id,enabled:enabled,error:err.code||"unexpected_error",message:err.toString()}, opts.req);
                        err.status = 400;
                        return reject(err);
                    });
                }
            } catch(error) {
                runtime.log.audit({event: "nodes.info.set",id:id,enabled:enabled,error:error.code||"unexpected_error",message:error.toString()}, opts.req);
                error.status = 400;
                return reject(error);
            }
        });
    },

    /**
    * Gets all registered module message catalogs
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {User} opts.lang - the i18n language to return. If not set, uses runtime default (en-US)
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<Object>} - the message catalogs
    * @memberof @node-red/runtime_nodes
    */
    getModuleCatalogs: function(opts) {
        return new Promise(function(resolve,reject) {
            var namespace = opts.module;
            var lang = opts.lang;
            var prevLang = runtime.i18n.i.language;
            // Trigger a load from disk of the language if it is not the default
            runtime.i18n.i.changeLanguage(lang, function(){
                var nodeList = runtime.nodes.getNodeList();
                var result = {};
                nodeList.forEach(function(n) {
                    if (n.module !== "node-red") {
                        result[n.id] = runtime.i18n.i.getResourceBundle(lang, n.id)||{};
                    }
                });
                resolve(result);
            });
            runtime.i18n.i.changeLanguage(prevLang);
        });
    },

    /**
    * Gets a modules message catalog
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {User} opts.module - the module
    * @param {User} opts.lang - the i18n language to return. If not set, uses runtime default (en-US)
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<Object>} - the message catalog
    * @memberof @node-red/runtime_nodes
    */
    getModuleCatalog: function(opts) {
        return new Promise(function(resolve,reject) {
            var namespace = opts.module;
            var lang = opts.lang;
            var prevLang = runtime.i18n.i.language;
            // Trigger a load from disk of the language if it is not the default
            runtime.i18n.i.changeLanguage(lang, function(){
                var catalog = runtime.i18n.i.getResourceBundle(lang, namespace);
                resolve(catalog||{});
            });
            runtime.i18n.i.changeLanguage(prevLang);
        });
    },

    /**
    * Gets the list of all icons available in the modules installed within the runtime
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<IconList>} - the list of all icons
    * @memberof @node-red/runtime_nodes
    */
    getIconList: function(opts) {
        return new Promise(function(resolve,reject) {
            runtime.log.audit({event: "nodes.icons.get"}, opts.req);
            return resolve(runtime.nodes.getNodeIcons());
        });

    },
    /**
    * Gets a node icon
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {String} opts.module - the id of the module requesting the icon
    * @param {String} opts.icon - the name of the icon
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<Buffer>} - the icon file as a Buffer or null if no icon available
    * @memberof @node-red/runtime_nodes
    */
    getIcon: function(opts) {
        return new Promise(function(resolve,reject) {
            var iconPath = runtime.nodes.getNodeIconPath(opts.module,opts.icon);
            if (iconPath) {
                fs.readFile(iconPath,function(err,data) {
                    if (err) {
                        err.status = 400;
                        return reject(err);
                    }
                    return resolve(data)
                });
            } else {
                resolve(null);
            }
        });
    },

    //S4C Nodes Start
    installMicroservice: function(opts) {
        return new Promise(function(resolve,reject) {
            if (!runtime.settings.available()) {
                runtime.log.audit({event: "microservices.install", error: "settings_unavailable"}, opts.req);
                var err = new Error("Settings unavailable");
                err.code = "settings_unavailable";
                err.status = 400;
                return reject(err)
            }
            if (opts.url) {
                runtime.nodes.installMicroservice(opts.url).then(function(info) {
                    runtime.log.audit({event: "microservices.install",microservice:opts.url}, opts.req);
                    return resolve(info);
                }).catch(function(err) {
                    if (err.code === 404) {
                        runtime.log.audit({event: "microservices.install",microservices:opts.url,error:"not_found"}, opts.req);
                        err.status = 404;
                    } else if (err.code) {
                        err.status = 400;
                        runtime.log.audit({event: "microservices.install",microservices:opts.url,error:err.code}, opts.req);
                    } else {
                        err.status = 400;
                        runtime.log.audit({event: "microservices.install",microservices:opts.url,error:err.code||"unexpected_error",message:err.toString()}, opts.req);
                    }
                    return reject(err);
                })
            } else {
                runtime.log.audit({event: "microservices.install",microservice:opts.url,error:"invalid_request"}, opts.req);
                var err = new Error("Invalid request");
                err.code = "invalid_request";
                err.status = 400;
                return reject(err);
            }
        });
    }
    //S4C Nodes End
}
