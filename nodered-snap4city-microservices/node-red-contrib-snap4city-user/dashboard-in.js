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
    /*var WebSocket = require('ws');
        var path = require("path");
        var fs = require('fs');
        var configJson = null;
        var os = require('os');
        var http = require('http');
        var path = require("path");*/
    var request = require('request');

    var bodyParser = require('body-parser');
    var express = require('express');
    var app = express();

    function DashboardInNode(config) {
        RED.nodes.createNode(this, config);
        var node = this;
        //Meccanismo di passaggio dei valori tra il menu di add/edit node e il codice del nodo
        node.name = config.name;
        node.username = config.username;
        node.flowName = config.flowName;
        node.selectedDashboard = config.selectedDashboard;
        node.dashboardTitle = config.dashboardTitle;
        node.valueType = config.valueType;
        node.startValue = null;
        node.minValue = null;
        node.maxValue = null;
        node.offValue = null;
        node.onValue = null;
        node.domain = config.domain;
        node.httpServer = null;

        var payload = null;

        this.getNow = function () {
            var now = new Date();
            return now.getDate() + "/" + (now.getMonth() + 1) + "/" + now.getFullYear() + " " + now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds();
        };

        app.use(bodyParser.json());

        switch (node.domain) {
            case "contRange":
                this.minValue = config.minValue;
                this.maxValue = config.maxValue;
                this.offValue = null;
                this.onValue = null;
                node.startValue = config.minValue;
                break;

            case "onOff":
            case "impulse":
                this.minValue = null;
                this.maxValue = null;
                this.offValue = config.offValue;
                this.onValue = config.onValue;
                node.startValue = config.offValue;
                break;

            default:
                this.minValue = null;
                this.maxValue = null;
                this.offValue = null;
                this.onValue = null;
                node.startValue = 0;
                break;
        }

        this.httpServerPostCallback = function (req, res) {
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

            node.send(msg);

            console.log(node.getNow() + " - New HTTP request received from dashboard in node " + node.name + ": " + msg);

            res.set("Access-Control-Allow-Origin", "*");
            res.set('Content-Type', 'application/json');
            var responseBody = JSON.stringify({
                result: 'Ok'
            });
            res.send(responseBody);
        };

        this.httpServerPostErrorHandler = function (e) {
            console.log(node.getNow() + " - Error managing HTTP request for dashboard in node " + node.name + ": " + e);
        };

        if (RED.settings.hasOwnProperty('httpRoot')) {
            if (RED.settings.httpRoot !== '/') {
                var httpRoot = RED.settings.httpRoot;

            } else {
                var httpRoot = null;
            }
        }

        console.log(node.getNow() + " - httpRoot parameter for dashboard in node " + node.name + ": " + RED.settings.httpRoot);

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
                dashboardTitle: node.dashboardTitle,
                refreshToken: retrieveRefreshToken()
            }
        };

        request({
                url: RED.settings.dashboardManagerBaseUrl + "/api/nodeRedEmittersApi.php",
                method: "POST",
                json: false,
                body: JSON.stringify(payload)
            },
            function (error, response, body) {
                console.log(node.getNow() + " - Dashboard in node " + node.name + " sent ADD_EMITTER request to Dashboard manager - Response: " + response + " - Error: " + error);
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
                    console.log(node.getNow() + " - Dashboard in node " + node.name + " sent DEL_EMITTER request to Dashboard manager - Response: " + response + " - Error: " + error);
                });
        };

        //Lasciarlo, altrimenti va in timeout!!! https://nodered.org/docs/creating-nodes/node-js#closing-the-node
        this.nodeClosingDone = function () {
            console.log(node.getNow() + " - Dashboard in node " + node.name + " has been closed");
        };

        this.on('close', function (removed, nodeClosingDone) {
            if (removed) {
                // Cancellazione nodo
                console.log(node.getNow() + " - Dashboard in node " + node.name + " is being removed from flow");
                node.delEmitter();
            } else {
                // Riavvio nodo
                console.log(node.getNow() + " - Dashboard in node " + node.name + " is being rebooted");
            }
            nodeClosingDone();

        });
    }

    function retrieveRefreshToken() {
        var fs = require('fs');
        var refreshToken = fs.readFileSync('refresh_token', 'utf-8');
        var url = "https://www.snap4city.org/auth/realms/master/protocol/openid-connect/token/";
        var params = "client_id=nodered&client_secret=943106ae-c62c-4961-85a2-849f6955d404&grant_type=refresh_token&scope=openid profile&refresh_token=" + refreshToken;
        var response = "";
        var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
        var xmlHttp = new XMLHttpRequest();
        console.log(encodeURI(url));
        xmlHttp.open("POST", encodeURI(url), false);
        xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        xmlHttp.send(params);
        if (xmlHttp.responseText != "") {
            try {
                response = JSON.parse(xmlHttp.responseText);
            } catch (e) {}
        }
        if (response != "") {
            fs.writeFileSync('refresh_token', response.refresh_token);
            return response.refresh_token;
        }
        return response;
    }
    RED.nodes.registerType("dashboard in", DashboardInNode);
};