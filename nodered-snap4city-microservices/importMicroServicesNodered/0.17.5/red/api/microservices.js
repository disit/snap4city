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
var locales = require("./locales");
var redNodes;
var log;
var i18n;
var settings;
var events;

module.exports = {
    init: function (runtime) {
        redNodes = runtime.nodes;
        log = runtime.log;
        i18n = runtime.i18n;
        settings = runtime.settings;
        events = runtime.events;
    },

    post: function (req, res) {
        if (!settings.available()) {
            log.audit({
                event: "microservices.install",
                error: "settings_unavailable"
            }, req);
            res.status(400).json({
                error: "settings_unavailable",
                message: "Settings unavailable"
            });
            return;
        }
        var node = req.body;
        var promise;
        if (node.url) {
            promise = redNodes.installMicroservice(node.url);
        } else {
            log.audit({
                event: "microservices.install",
                microservice: node.url,
                error: "invalid_request"
            }, req);
            res.status(400).json({
                error: "invalid_request",
                message: "Invalid request"
            });
            return;
        }
        promise.then(function (info) {
            events.emit("runtime-event", {
                id: "node/added",
                retain: false,
                payload: info.nodes
            });
            if (node.url) {
                log.audit({
                    event: "nodes.install",
                    microservice: node.url
                }, req);
                res.json(info);
            }
        }).otherwise(function (err) {
            if (err.code === 404) {
                log.audit({
                    event: "microservices.install",
                    microservice: node.url,
                    error: "not_found"
                }, req);
                res.status(404).end();
            } else if (err.code) {
                log.audit({
                    event: "microservices.install",
                    microservice: node.url,
                    error: err.code
                }, req);
                res.status(400).json({
                    error: err.code,
                    message: err.message
                });
            } else {
                log.audit({
                    event: "microservices.install",
                    microservice: node.url,
                    error: err.code || "unexpected_error",
                    message: err.toString()
                }, req);
                res.status(400).json({
                    error: err.code || "unexpected_error",
                    message: err.toString()
                });
            }
        });
    }

};