/* NODE-RED-CONTRIB-SNAP4CITY-USER
   Copyright (C) 2018 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */
module.exports = function (RED) {
    var request = require('request');

    var bodyParser = require('body-parser');
    var express = require('express');
    var app = express();

    function ImpulseButton(config) {
        RED.nodes.createNode(this, config);
        var node = this;
        //Meccanismo di passaggio dei valori tra il menu di add/edit node e il codice del nodo
        node.name = "NR_" + node.id.replace(".", "_");
        node.widgetTitle = config.name,
            node.username = config.username;
        node.flowName = config.flowName;
        node.selectedDashboard = config.selectedDashboard;
        node.dashboardTitle = config.dashboardTitle;
        node.valueType = config.valueType;
        node.startValue = config.offValue;
        node.minValue = null;
        node.maxValue = null;
        node.offValue = config.offValue;
        node.onValue = config.onValue;
        node.domain = "impulse";
        node.httpServer = null;

        var payload = null;

        node.getNow = function () {
            var now = new Date();
            return now.getDate() + "/" + (now.getMonth() + 1) + "/" + now.getFullYear() + " " + now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds();
        };

        app.use(bodyParser.json());

        node.httpServerPostCallback = function (req, res) {
            var msg, temp = null;
            const body = req.body.Body;

            if (config.valueType === 'Float') {
                temp = parseFloat(req.body.newValue);
                msg = {
                    payload: temp
                };
            } else {
                if (config.valueType === 'Integer') {
                    temp = parseInt(req.body.newValue);
                    msg = {
                        payload: temp
                    };
                } else {
                    msg = {
                        payload: req.body.newValue
                    };
                }
            }

            console.log(node.getNow() + " - New HTTP request received from impulse-button node " + node.name + ": " + msg);

            res.sendStatus(200);

            if (msg.payload == node.onValue) {
                node.send([msg, null]);
            } else {
                node.send([null, msg]);
            }

        };

        node.httpServerPostErrorHandler = function (e) {
            console.log(node.getNow() + " - Error managing HTTP request for impulse-button node " + node.name + ": " + e);
        };

        if (RED.settings.hasOwnProperty('httpRoot')) {
            if (RED.settings.httpRoot !== '/') {
                var httpRoot = RED.settings.httpRoot;

            } else {
                var httpRoot = null;
            }
        }

        console.log(node.getNow() + " - httpRoot parameter for impulse-button node " + node.name + ": " + RED.settings.httpRoot);

        var payload = {
            message: {
                msgType: "AddEmitter",
                name: node.name,
                valueType: node.valueType,
                user: node.username,
                startValue: node.startValue,
                domainType: node.domain,
                offValue: node.offValue,
                onValue: node.onValue,
                minValue: node.minValue,
                maxValue: node.maxValue,
                endPointPort: RED.settings.externalPort,
                endPointHost: RED.settings.dashInNodeBaseUrl,
                httpRoot: httpRoot,
                appId: RED.settings.APPID,
                flowId: node.z,
                flowName: node.flowName,
                nodeId: node.id,
                widgetType: "widgetImpulseButton",
                widgetTitle: node.widgetTitle,
                dashboardTitle: node.dashboardTitle
            }
        };

        request({
                url: RED.settings.dashboardManagerBaseUrl + "/api/nodeRedEmittersApi.php",
                method: "POST",
                json: false,
                body: JSON.stringify(payload)
            },
            function (error, response, body) {
                if (error === null) {
                    console.log(node.getNow() + " - impulse-button node " + node.name + " sent ADD_EMITTER request to Dashboard manager - Response: " + JSON.stringify(response));
                } else {
                    console.log(node.getNow() + " - impulse-button node " + node.name + " sent ADD_EMITTER request to Dashboard manager BUT GOT ERROR - Response: " + JSON.stringify(response) + " - Error: " + JSON.stringify(error));
                }
            }
        );

        RED.httpNode.post("/" + node.name, /*next,next,next,jsonParser,urlencParser,rawBodyParser,*/ node.httpServerPostCallback, node.httpServerPostErrorHandler);

        node.delEmitter = function () {
            var payload = {
                message: {
                    msgType: "DelEmitter",
                    name: node.name,
                    user: node.username
                }
            };

            request({
                    url: RED.settings.dashboardManagerBaseUrl + "/api/nodeRedEmittersApi.php",
                    method: "POST",
                    json: false,
                    body: JSON.stringify(payload)
                },
                function (error, response, body) {
                    console.log(node.getNow() + " - impulse-button node " + node.name + " sent DEL_EMITTER request to Dashboard manager - Response: " + response + " - Error: " + error);
                });
        };

        //Lasciarlo, altrimenti va in timeout!!! https://nodered.org/docs/creating-nodes/node-js#closing-the-node
        node.nodeClosingDone = function () {
            console.log(node.getNow() + " - impulse-button node " + node.name + " has been closed");
        };

        node.on('close', function (removed, nodeClosingDone) {
            if (removed) {
                // Cancellazione nodo
                console.log(node.getNow() + " - impulse-button node " + node.name + " is being removed from flow");
                node.delEmitter();
            } else {
                // Riavvio nodo
                console.log(node.getNow() + " - impulse-button node " + node.name + " is being rebooted");
            }
            nodeClosingDone();

        });
    }
    RED.nodes.registerType("impulse-button", ImpulseButton);
};