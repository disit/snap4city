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

var express = require("express");

var nodes = require("./nodes");
var flows = require("./flows");
var flow = require("./flow");
var context = require("./context");
var auth = require("../auth");
var info = require("./settings");
var plugins = require("./plugins");
//S4C Nodes Start
var microservices = require("./S4CNodes");
var authentication = require("./S4CAuth");
//S4C Nodes End

var apiUtil = require("../util");

module.exports = {
    init: function(settings,runtimeAPI) {
        flows.init(runtimeAPI);
        flow.init(runtimeAPI);
        nodes.init(runtimeAPI);
        context.init(runtimeAPI);
        info.init(settings,runtimeAPI);
        plugins.init(runtimeAPI);
        //S4C Nodes Start
        microservices.init(runtimeAPI);
        authentication.init(runtimeAPI);
        //S4C Nodes End

        var needsPermission = auth.needsPermission;

        var adminApp = express();

        var defaultServerSettings = {
            "x-powered-by": false
        }
        var serverSettings = Object.assign({},defaultServerSettings,settings.httpServerOptions||{});
        for (var eOption in serverSettings) {
            adminApp.set(eOption, serverSettings[eOption]);
        }


        // Flows
        adminApp.get("/flows",needsPermission("flows.read"),flows.get,apiUtil.errorHandler);
        adminApp.post("/flows",needsPermission("flows.write"),flows.post,apiUtil.errorHandler);

        // Flow
        adminApp.get("/flow/:id",needsPermission("flows.read"),flow.get,apiUtil.errorHandler);
        adminApp.post("/flow",needsPermission("flows.write"),flow.post,apiUtil.errorHandler);
        adminApp.delete("/flow/:id",needsPermission("flows.write"),flow.delete,apiUtil.errorHandler);
        adminApp.put("/flow/:id",needsPermission("flows.write"),flow.put,apiUtil.errorHandler);

        // Nodes
        adminApp.get("/nodes",needsPermission("nodes.read"),nodes.getAll,apiUtil.errorHandler);

        if (!settings.externalModules || !settings.externalModules.palette || settings.externalModules.palette.allowInstall !== false) {
            if (!settings.externalModules || !settings.externalModules.palette || settings.externalModules.palette.allowUpload !== false) {
                const multer  = require('multer');
                const upload = multer({ storage: multer.memoryStorage() });
                adminApp.post("/nodes",needsPermission("nodes.write"),upload.single("tarball"),nodes.post,apiUtil.errorHandler);
            } else {
                adminApp.post("/nodes",needsPermission("nodes.write"),nodes.post,apiUtil.errorHandler);
            }
        }
        adminApp.get(/^\/nodes\/messages/,needsPermission("nodes.read"),nodes.getModuleCatalogs,apiUtil.errorHandler);
        adminApp.get(/^\/nodes\/((@[^\/]+\/)?[^\/]+\/[^\/]+)\/messages/,needsPermission("nodes.read"),nodes.getModuleCatalog,apiUtil.errorHandler);
        adminApp.get(/^\/nodes\/((@[^\/]+\/)?[^\/]+)$/,needsPermission("nodes.read"),nodes.getModule,apiUtil.errorHandler);
        adminApp.put(/^\/nodes\/((@[^\/]+\/)?[^\/]+)$/,needsPermission("nodes.write"),nodes.putModule,apiUtil.errorHandler);
        adminApp.delete(/^\/nodes\/((@[^\/]+\/)?[^\/]+)$/,needsPermission("nodes.write"),nodes.delete,apiUtil.errorHandler);
        adminApp.get(/^\/nodes\/((@[^\/]+\/)?[^\/]+)\/([^\/]+)$/,needsPermission("nodes.read"),nodes.getSet,apiUtil.errorHandler);
        adminApp.put(/^\/nodes\/((@[^\/]+\/)?[^\/]+)\/([^\/]+)$/,needsPermission("nodes.write"),nodes.putSet,apiUtil.errorHandler);

        // Context
        adminApp.get("/context/:scope(global)",needsPermission("context.read"),context.get,apiUtil.errorHandler);
        adminApp.get("/context/:scope(global)/*",needsPermission("context.read"),context.get,apiUtil.errorHandler);
        adminApp.get("/context/:scope(node|flow)/:id",needsPermission("context.read"),context.get,apiUtil.errorHandler);
        adminApp.get("/context/:scope(node|flow)/:id/*",needsPermission("context.read"),context.get,apiUtil.errorHandler);

        //S4C Nodes Start
        //Microservices
        adminApp.post("/microservices", needsPermission("microservices.write"), microservices.post, apiUtil.errorHandler);
        adminApp.get("/authentication/refreshtoken", needsPermission("refreshtoken.read"), authentication.getRefreshToken, apiUtil.errorHandler);
        //S4C Nodes End

        // adminApp.delete("/context/:scope(global)",needsPermission("context.write"),context.delete,apiUtil.errorHandler);
        adminApp.delete("/context/:scope(global)/*",needsPermission("context.write"),context.delete,apiUtil.errorHandler);
        // adminApp.delete("/context/:scope(node|flow)/:id",needsPermission("context.write"),context.delete,apiUtil.errorHandler);
        adminApp.delete("/context/:scope(node|flow)/:id/*",needsPermission("context.write"),context.delete,apiUtil.errorHandler);

        adminApp.get("/settings",needsPermission("settings.read"),info.runtimeSettings,apiUtil.errorHandler);

        // Plugins
        adminApp.get("/plugins", needsPermission("plugins.read"), plugins.getAll, apiUtil.errorHandler);
        adminApp.get("/plugins/messages", needsPermission("plugins.read"), plugins.getCatalogs, apiUtil.errorHandler);

        return adminApp;
    }
}
