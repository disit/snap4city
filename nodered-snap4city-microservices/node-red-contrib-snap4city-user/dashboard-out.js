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
    var WebSocket = require('ws');

    function DashboardOutNode(config) {
        RED.nodes.createNode(this, config);
        var node = this;
        var wsServer = RED.settings.wsServerUrl;
        node.ws = null;

        node.name = config.name;
        node.username = config.username;
        node.flowName = config.flowName;
        node.selectedDashboard = config.selectedDashboard;
        node.dashboardTitle = config.dashboardTitle;
        node.metricName = config.name;
        node.metricType = config.metricType;
        node.startValue = config.startValue;
        node.metricShortDesc = config.name;
        node.metricFullDesc = config.name;

        node.getNow = function () {
            var now = new Date();
            return now.getDate() + "/" + (now.getMonth() + 1) + "/" + now.getFullYear() + " " + now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds();
        };

        node.openWsConn = function () {
            node.status({
                fill: "green",
                shape: "dot",
                text: "connected to " + wsServer
            });

            //Registrazione della nuova metrica presso il Dashboard Manager
            var newMetric = {
                msgType: "AddEditMetric",
                metricName: encodeURIComponent(node.metricName),
                metricType: node.metricType,
                startValue: node.startValue,
                nodeId: node.id,
                user: node.username,
                metricShortDesc: node.metricShortDesc,
                metricFullDesc: node.metricFullDesc,
                appId: RED.settings.APPID,
                flowId: node.z,
                flowName: node.flowName,
                dashboardTitle: node.dashboardTitle,
                httpRoot: node.httpRoot,
                refreshToken: retrieveRefreshToken()
            };

            node.ws.send(JSON.stringify(newMetric));

        };

        node.on('input', function (msg) {
            console.log(node.getNow() + " - Flow input received for dashboard out node " + node.name + ": " + msg);

            var newMetricData = {
                msgType: "AddMetricData",
                metricName: encodeURIComponent(node.metricName),
                metricType: node.metricType,
                nodeId: node.id,
                appId: RED.settings.APPID,
                flowId: node.z,
                flowName: node.flowName,
                user: node.username,
                newValue: msg.payload
            };
            var timeout = 0;
            if ((new Date().getTime() - node.wsStart) > parseInt(RED.settings.wsReconnectTimeout) * 1000) {
                node.ws.removeListener('error', node.wsError);
                node.ws.removeListener('open', node.openWsConn);
                node.ws.removeListener('message', node.manageIncomingWsMsg);
                node.ws.removeListener('close', node.wsClosed);
                node.ws = null;
                node.ws = new WebSocket(wsServer);
                node.ws.on('error', node.wsError);
                node.ws.on('open', node.openWsConn);
                node.ws.on('message', node.manageIncomingWsMsg);
                node.ws.on('close', node.wsClosed);
                console.log(node.getNow() + " - Dashboard out node " + node.name + " is reconnetting to open WebSocket");
                timeout = 1000;
            }
            node.wsStart = new Date().getTime();

            setTimeout(function () {
                try {
                    node.ws.send(JSON.stringify(newMetricData));
                } catch (e) {
                    console.log(node.getNow() + " - Error sending data to WebSocket for dashboard out node " + node.name + ": " + JSON.stringify(e));
                }
            }, timeout);


        });

        this.manageIncomingWsMsg = function (data) {
            var response = JSON.parse(data);
            switch (response.msgType) {
                case "AddEditMetric":
                    if (response.result === "Ok") {
                        console.log(node.getNow() + " - WebSocket server correctly added/edited metric type for dashboard out node " + node.name + ": " + response.result);
                        node.metricId = response.metricId;
                    } else {
                        //TBD - CASI NEGATIVI DA FARE
                        console.log(node.getNow() + " - WebSocket server could not add/edit metric type for dashboard out node " + node.name + ": " + response.result);
                    }
                    break;

                case "DelMetric":
                    if (response.result === "Ok") {
                        console.log(node.getNow() + " - WebSocket server correctly deleted metric type for dashboard out node " + node.name + ": " + response.result);
                    } else {
                        //TBD - CASI NEGATIVI DA FARE
                        console.log(node.getNow() + " - WebSocket server could not delete metric type for dashboard out node " + node.name + ": " + response.result);
                    }
                    console.log(node.getNow() + " - Closing webSocket server for dashboard out node " + node.name);
                    node.ws.close();
                    break;

                default:
                    break;
            }
        };

        this.delMetric = function () {
            console.log(node.getNow() + " - Deleting metric via webSocket for dashboard out node " + node.name);
            var newMsg = {
                msgType: "DelMetric",
                metricName: encodeURIComponent(node.metricName),
                metricType: node.metricType,
                nodeId: node.id,
                user: node.username,
                appId: RED.settings.APPID,
                flowId: node.z,
                flowName: node.flowName
            };

            try {
                node.ws.send(JSON.stringify(newMsg));
            } catch (e) {
                console.log(node.getNow() + " - Error deleting metric via webSocket for dashboard out node " + node.name + ": " + e);
            }
        };

        this.openWs = function (e) {
            console.log(node.getNow() + " - Dashboard out node " + node.name + " is trying to open WebSocket");
            try {
                node.status({
                    fill: "yellow",
                    shape: "dot",
                    text: "connecting to " + wsServer
                });
                node.ws = new WebSocket(wsServer);
                node.ws.on('error', node.wsError);
                node.ws.on('open', node.openWsConn);
                node.ws.on('message', node.manageIncomingWsMsg);
                node.ws.on('close', node.wsClosed);
                node.wsStart = new Date().getTime();
            } catch (e) {
                console.log(node.getNow() + " - Dashboard out node " + node.name + " could not open WebSocket");
                node.status({
                    fill: "red",
                    shape: "ring",
                    text: "unable to connect to " + wsServer
                });
                node.wsClosed();
            }
        };

        this.wsClosed = function (e) {
            console.log(node.getNow() + " - Dashboard out node " + node.name + " closed WebSocket");
            node.status({
                fill: "red",
                shape: "ring",
                text: "lost connection from " + wsServer
            });

            node.ws.removeListener('error', node.wsError);
            node.ws.removeListener('open', node.openWsConn);
            node.ws.removeListener('message', node.manageIncomingWsMsg);
            node.ws.removeListener('close', node.wsClosed);
            node.ws = null;

            if (RED.settings.wsServerRetryActive === 'yes') {
                console.log(node.getNow() + " - Dashboard out node " + node.name + " will try to reconnect to WebSocket in " + parseInt(RED.settings.wsServerRetryTime) + "s");
                setTimeout(node.openWs, parseInt(RED.settings.wsServerRetryTime) * 1000);
            }
        };

        this.wsError = function (e) {
            console.log(node.getNow() + " - Dashboard out node " + node.name + " got WebSocket error: " + e);
        };

        //Inizio del "main"
        try {
            node.openWs();
        } catch (e) {
            console.log(node.getNow() + " - Dashboard out node " + node.name + " got main exception connecting to WebSocket");
        }

        //Lasciarlo, altrimenti va in timeout!!! https://nodered.org/docs/creating-nodes/node-js#closing-the-node
        this.nodeClosingDone = function () {
            console.log(node.getNow() + " - Dashboard out node " + node.name + " has been closed");
        };

        this.on('close', function (removed, nodeClosingDone) {
            //node.ws.off('message');
            //node.ws.off('close');
            if (removed) {
                // Cancellazione nodo
                console.log(node.getNow() + " - Dashboard out node " + node.name + " is being removed from flow");
                node.delMetric();
            } else {
                // Riavvio nodo
                console.log(node.getNow() + " - Dashboard out node " + node.name + " is being rebooted");
                node.ws.close();
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
    RED.nodes.registerType("dashboard out", DashboardOutNode);
};