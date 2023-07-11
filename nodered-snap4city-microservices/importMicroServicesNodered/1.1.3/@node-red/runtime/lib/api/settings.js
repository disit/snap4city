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
 * @mixin @node-red/runtime_settings
 */

var util = require("util");
var runtime;

function extend(target, source) {
    var keys = Object.keys(source);
    var i = keys.length;
    while(i--) {
        var value = source[keys[i]]
        var type = typeof value;
        if (type === 'string' || type === 'number' || type === 'boolean' || Array.isArray(value)) {
            target[keys[i]] = value;
        } else if (value === null) {
            if (target.hasOwnProperty(keys[i])) {
                delete target[keys[i]];
            }
        } else {
            // Object
            if (target.hasOwnProperty(keys[i])) {
                target[keys[i]] = extend(target[keys[i]],value);
            } else {
                target[keys[i]] = value;
            }
        }
    }
    return target;
}

function getSSHKeyUsername(userObj) {
    var username = '__default';
    if ( userObj && userObj.username ) {
        username = userObj.username;
    }
    return username;
}
var api = module.exports = {
    init: function(_runtime) {
        runtime = _runtime;
    },
    /**
    * Gets the runtime settings object
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<Object>} - the runtime settings
    * @memberof @node-red/runtime_settings
    */
    getRuntimeSettings: function(opts) {
        return new Promise(function(resolve,reject) {
            try {
                var safeSettings = {
                    httpNodeRoot: runtime.settings.httpNodeRoot||"/",
                    version: runtime.settings.version
                }
                if (opts.user) {
                    safeSettings.user = {}
                    var props = ["anonymous","username","image","permissions"];
                    props.forEach(prop => {
                        if (opts.user.hasOwnProperty(prop)) {
                            safeSettings.user[prop] = opts.user[prop];
                        }
                    })
                }

                if (!runtime.settings.disableEditor) {
                    safeSettings.context = runtime.nodes.listContextStores();

                    if (util.isArray(runtime.settings.paletteCategories)) {
                        safeSettings.paletteCategories = runtime.settings.paletteCategories;
                    }

                    if (runtime.settings.flowFilePretty) {
                        safeSettings.flowFilePretty = runtime.settings.flowFilePretty;
                    }

                    if (!runtime.nodes.paletteEditorEnabled()) {
                        safeSettings.editorTheme = safeSettings.editorTheme || {};
                        safeSettings.editorTheme.palette = safeSettings.editorTheme.palette || {};
                        safeSettings.editorTheme.palette.editable = false;
                    }
                    if (runtime.storage.projects) {
                        var activeProject = runtime.storage.projects.getActiveProject();
                        if (activeProject) {
                            safeSettings.project = activeProject;
                        } else if (runtime.storage.projects.flowFileExists()) {
                            safeSettings.files = {
                                flow: runtime.storage.projects.getFlowFilename(),
                                credentials: runtime.storage.projects.getCredentialsFilename()
                            }
                        }
                        safeSettings.git = {
                            globalUser: runtime.storage.projects.getGlobalGitUser()
                        }
                    }

                    safeSettings.flowEncryptionType = runtime.nodes.getCredentialKeyType();
                    runtime.settings.exportNodeSettings(safeSettings);
                }


                resolve(safeSettings);
            }catch(err) {
                console.log(err);
            }
        });
    },

    /**
    * Gets an individual user's settings object
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<Object>} - the user settings
    * @memberof @node-red/runtime_settings
    */
    getUserSettings: function(opts) {
        var username;
        if (!opts.user || opts.user.anonymous) {
            username = '_';
        } else {
            username = opts.user.username;
        }
        return Promise.resolve(runtime.settings.getUserSettings(username)||{});
    },

    /**
    * Updates an individual user's settings object.
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {Object} opts.settings - the updates to the user settings
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<Object>} - the user settings
    * @memberof @node-red/runtime_settings
    */
    updateUserSettings: function(opts) {
        var username;
        if (!opts.user || opts.user.anonymous) {
            username = '_';
        } else {
            username = opts.user.username;
        }
        return new Promise(function(resolve,reject) {
            var currentSettings = runtime.settings.getUserSettings(username)||{};
            currentSettings = extend(currentSettings, opts.settings);
            try {
                runtime.settings.setUserSettings(username, currentSettings).then(function() {
                    runtime.log.audit({event: "settings.update",username:username}, opts.req);
                    return resolve();
                }).catch(function(err) {
                    runtime.log.audit({event: "settings.update",username:username,error:err.code||"unexpected_error",message:err.toString()}, opts.req);
                    err.status = 400;
                    return reject(err);
                });
            } catch(err) {
                runtime.log.warn(runtime.log._("settings.user-not-available",{message:runtime.log._("settings.not-available")}));
                runtime.log.audit({event: "settings.update",username:username,error:err.code||"unexpected_error",message:err.toString()}, opts.req);
                err.status = 400;
                return reject(err);
            }
        });
    },

    /**
    * Gets a list of a user's ssh keys
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<Object>} - the user's ssh keys
    * @memberof @node-red/runtime_settings
    */
    getUserKeys: function(opts) {
        return new Promise(function(resolve,reject) {
            var username = getSSHKeyUsername(opts.user);
            runtime.storage.projects.ssh.listSSHKeys(username).then(function(list) {
                return resolve(list);
            }).catch(function(err) {
                err.status = 400;
                return reject(err);
            });
        });
    },

    /**
    * Gets a user's ssh public key
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {User} opts.id - the id of the key to return
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<String>} - the user's ssh public key
    * @memberof @node-red/runtime_settings
    */
    getUserKey: function(opts) {
        return new Promise(function(resolve,reject) {
            var username = getSSHKeyUsername(opts.user);
            // console.log('username:', username);
            runtime.storage.projects.ssh.getSSHKey(username, opts.id).then(function(data) {
                if (data) {
                    return resolve(data);
                } else {
                    var err = new Error("Key not found");
                    err.code = "not_found";
                    err.status = 404;
                    return reject(err);
                }
            }).catch(function(err) {
                err.status = 400;
                return reject(err);
            });
        });
    },

    /**
    * Generates a new ssh key pair
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {User} opts.name - the id of the key to return
    * @param {User} opts.password - (optional) the password for the key pair
    * @param {User} opts.comment - (option) a comment to associate with the key pair
    * @param {User} opts.size - (optional) the size of the key. Default: 2048
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise<String>} - the id of the generated key
    * @memberof @node-red/runtime_settings
    */
    generateUserKey: function(opts) {
        return new Promise(function(resolve,reject) {
            var username = getSSHKeyUsername(opts.user);
            runtime.storage.projects.ssh.generateSSHKey(username, opts).then(function(name) {
                return resolve(name);
            }).catch(function(err) {
                err.status = 400;
                return reject(err);
            });
        });
    },

    /**
    * Deletes a user's ssh key pair
    * @param {Object} opts
    * @param {User} opts.user - the user calling the api
    * @param {User} opts.id - the id of the key to delete
    * @param {Object} opts.req - the request to log (optional)
    * @return {Promise} - resolves when deleted
    * @memberof @node-red/runtime_settings
    */
    removeUserKey: function(opts) {
        return new Promise(function(resolve,reject) {
            var username = getSSHKeyUsername(opts.user);
            runtime.storage.projects.ssh.deleteSSHKey(username, opts.id).then(function() {
                return resolve();
            }).catch(function(err) {
                err.status = 400;
                return reject(err);
            });
        });

    }
}
