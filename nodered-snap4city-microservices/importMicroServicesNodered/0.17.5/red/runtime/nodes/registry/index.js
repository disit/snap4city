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

var when = require("when");
var fs = require("fs");
var path = require("path");

var events = require("../../events");
var registry = require("./registry");
var loader = require("./loader");
var installer = require("./installer");

var settings;

function init(runtime) {
    settings = runtime.settings;
    installer.init(runtime.settings);
    loader.init(runtime);
    registry.init(settings, loader);
}

function load() {
    registry.load();
    return installer.checkPrereq().then(loader.load);
}

function addModule(module) {
    return loader.addModule(module).then(function () {
        return registry.getModuleInfo(module);
    });
}


function addMicroservice(module) {
    return loader.addMicroservice(module).then(function () {
        return registry.getModuleInfo(module);
    });
}

function enableNodeSet(typeOrId) {
    return registry.enableNodeSet(typeOrId).then(function () {
        var nodeSet = registry.getNodeInfo(typeOrId);
        if (!nodeSet.loaded) {
            return loader.loadNodeSet(registry.getFullNodeInfo(typeOrId)).then(function () {
                return registry.getNodeInfo(typeOrId);
            });
        }
        return when.resolve(nodeSet);
    });
}

module.exports = {
    init: init,
    load: load,
    clear: registry.clear,
    registerType: registry.registerNodeConstructor,

    get: registry.getNodeConstructor,
    getNodeInfo: registry.getNodeInfo,
    getNodeList: registry.getNodeList,

    getModuleInfo: registry.getModuleInfo,
    getModuleList: registry.getModuleList,

    getNodeConfigs: registry.getAllNodeConfigs,
    getNodeConfig: registry.getNodeConfig,
    getNodeIconPath: registry.getNodeIconPath,

    enableNode: enableNodeSet,
    disableNode: registry.disableNodeSet,

    addModule: addModule,
    removeModule: registry.removeModule,

    installModule: installer.installModule,
    uninstallModule: installer.uninstallModule,

    installMicroservice: installer.installMicroservice,
    addMicroservice: addMicroservice,

    cleanModuleList: registry.cleanModuleList,

    paletteEditorEnabled: installer.paletteEditorEnabled
};