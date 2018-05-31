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
    "use strict";
    var path = require("path");
    var express = require("express");
    var sockjs = require('sockjs');
    var fs = require('fs');
    var socket;

    var ShowMicroWebApp = function (n) {
        RED.nodes.createNode(this, n);
        if (!socket) {
            var fullPath = path.posix.join(RED.settings.httpNodeRoot, 'web', 'lib', 'sockjs.min.js');
            socket = sockjs.createServer({
                sockjs_url: fullPath,
                log: function () {},
                transports: "xhr-polling"
            });
            socket.installHandlers(RED.server, {
                prefix: path.posix.join(RED.settings.httpNodeRoot, '/s4cmicro/socket')
            });
        }
        var node = this;
        var clients = {};
        RED.httpNode.use("/s4cmicro", express.static(__dirname + '/web'));

        var callback = function (client) {
            clients[client.id] = client;
            node.status({
                fill: "green",
                shape: "dot",
                text: "connected " + Object.keys(clients).length
            });
            client.on('data', function (message) {
                client.write(message);
            });
            client.on('close', function () {
                delete clients[client.id];
                node.status({
                    fill: "green",
                    shape: "ring",
                    text: "connected " + Object.keys(clients).length
                });
            });
        }
        node.on('input', function (msg) {
            for (var c in clients) {
                if (clients.hasOwnProperty(c)) {
                    var date = new Date().getTime();
                    var filename = __dirname + "/web/json/" + date + ".json";
                    fs.writeFile(filename, JSON.stringify(msg.payload), function (err) {
                        if (err) {
                            return console.log(err);
                        }
                        console.log("The file was saved!");
                    });
                    msg.payload.src = RED.settings.httpNodeRoot + "s4cmicro/json/" + date + ".json";
                    clients[c].write(JSON.stringify(msg.payload));
                }
            }
        });
        node.on("close", function () {
            for (var c in clients) {
                if (clients.hasOwnProperty(c)) {
                    clients[c].end();
                }
            }
            node.status({});
        });
        socket.on('connection', callback);
    }
    RED.nodes.registerType("show-micro-web-app", ShowMicroWebApp);

}