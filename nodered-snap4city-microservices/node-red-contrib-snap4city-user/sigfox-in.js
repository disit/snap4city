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
    var bodyParser = require("body-parser");
    var cookieParser = require("cookie-parser");
    var getBody = require('raw-body');
    var cors = require('cors');
    var jsonParser = bodyParser.json();
    var urlencParser = bodyParser.urlencoded({
        extended: true
    });
    var onHeaders = require('on-headers');
    var typer = require('media-typer');
    var isUtf8 = require('is-utf8');

    function eventLog(inPayload, outPayload, config, _agent, _motivation, _ipext, _modcom) {
        var os = require('os');
        var ifaces = os.networkInterfaces();
        var uri = "http://192.168.1.43/RsyslogAPI/rsyslog.php";

        var pidlocal = RED.settings.APPID;
        var iplocal = null;
        Object.keys(ifaces).forEach(function (ifname) {
            ifaces[ifname].forEach(function (iface) {
                if ('IPv4' !== iface.family || iface.internal !== false) {
                    // skip over internal (i.e. 127.0.0.1) and non-ipv4 addresses
                    return;
                }
                iplocal = iface.address;
            });
        });
        iplocal = iplocal + ":" + RED.settings.uiPort;
        var timestamp = new Date().getTime();
        var modcom = _modcom;
        var ipext = _ipext;
        var payloadsize = JSON.stringify(outPayload).length / 1000;
        var agent = _agent;
        var motivation = _motivation;
        var lang = (inPayload.lang ? inPayload.lang : config.lang);
        var lat = (inPayload.lat ? inPayload.lat : config.lat);
        var lon = (inPayload.lon ? inPayload.lon : config.lon);
        var serviceuri = (inPayload.serviceuri ? inPayload.serviceuri : config.serviceuri);
        var message = (inPayload.message ? inPayload.message : config.message);
        var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
        var xmlHttp = new XMLHttpRequest();
        console.log(encodeURI(uri + "?p=log" + "&pid=" + pidlocal + "&tmstmp=" + timestamp + "&modCom=" + modcom + "&IP_local=" + iplocal + "&IP_ext=" + ipext +
            "&payloadSize=" + payloadsize + "&agent=" + agent + "&motivation=" + motivation + "&lang=" + lang + "&lat=" + (typeof lat != "undefined" ? lat : 0.0) + "&lon=" + (typeof lon != "undefined" ? lon : 0.0) + "&serviceUri=" + serviceuri + "&message=" + message));
        xmlHttp.open("GET", encodeURI(uri + "?p=log" + "&pid=" + pidlocal + "&tmstmp=" + timestamp + "&modCom=" + modcom + "&IP_local=" + iplocal + "&IP_ext=" + ipext +
            "&payloadSize=" + payloadsize + "&agent=" + agent + "&motivation=" + motivation + "&lang=" + lang + "&lat=" + (typeof lat != "undefined" ? lat : 0.0) + "&lon=" + (typeof lon != "undefined" ? lon : 0.0) + "&serviceUri=" + serviceuri + "&message=" + message), true); // false for synchronous request
        xmlHttp.send(null);
    }

    function rawBodyParser(req, res, next) {
        if (req._body) {
            return next();
        }
        req.body = "";
        req._body = true;

        var isText = true;
        var checkUTF = false;

        if (req.headers['content-type']) {
            var parsedType = typer.parse(req.headers['content-type'])
            if (parsedType.type === "text") {
                isText = true;
            } else if (parsedType.subtype === "xml" || parsedType.suffix === "xml") {
                isText = true;
            } else if (parsedType.type !== "application") {
                isText = false;
            } else if (parsedType.subtype !== "octet-stream") {
                checkUTF = true;
            }
        }

        getBody(req, {
            length: req.headers['content-length'],
            encoding: isText ? "utf8" : null
        }, function (err, buf) {
            if (err) {
                return next(err);
            }
            if (!isText && checkUTF && isUtf8(buf)) {
                buf = buf.toString()
            }

            req.body = buf;
            next();
        });
    }

    var corsSetup = false;

    function createResponseWrapper(node, res) {
        var wrapper = {
            _res: res
        };
        var toWrap = [
            "append",
            "attachment",
            "cookie",
            "clearCookie",
            "download",
            "end",
            "format",
            "get",
            "json",
            "jsonp",
            "links",
            "location",
            "redirect",
            "render",
            "send",
            "sendfile",
            "sendFile",
            "sendStatus",
            "set",
            "status",
            "type",
            "vary"
        ];
        toWrap.forEach(function (f) {
            wrapper[f] = function () {
                node.warn("sigfox in: Deprecated call to msg.res." + f);
                var result = res[f].apply(res, arguments);
                if (result === res) {
                    return wrapper;
                } else {
                    return result;
                }
            }
        });
        return wrapper;
    }

    var corsHandler = function (req, res, next) {
        next();
    }

    if (RED.settings.httpNodeCors) {
        corsHandler = cors(RED.settings.httpNodeCors);
        RED.httpNode.options("*", corsHandler);
    }

    function sigfoxIn(config) {
        RED.nodes.createNode(this, config);
        if (RED.settings.httpNodeRoot !== false) {

            if (!config.device) {
                this.warn("sigfox in: Missing device type ID");
                return;
            }
            this.url = "/sigfox/callback/" + config.device;
            this.method = "post";

            var node = this;

            this.errorHandler = function (err, req, res, next) {
                node.warn("sigfox in: " + err);
                res.sendStatus(500);
            };

            this.callback = function (req, res) {
                var msgid = RED.util.generateId();
                res._msgid = msgid;
                eventLog({}, req.body, config, "Node-Red", "SigFox", "/sigfox/callback/" + config.device, "RX");
                res.sendStatus(200);
                node.send({
                    _msgid: msgid,
                    req: req,
                    res: createResponseWrapper(node, res),
                    payload: req.body
                });
            };

            var httpMiddleware = function (req, res, next) {
                next();
            }

            if (RED.settings.httpNodeMiddleware) {
                if (typeof RED.settings.httpNodeMiddleware === "function") {
                    httpMiddleware = RED.settings.httpNodeMiddleware;
                }
            }

            var metricsHandler = function (req, res, next) {
                next();
            }

            RED.httpNode.post(this.url, cookieParser(), httpMiddleware, corsHandler, metricsHandler, jsonParser, urlencParser, rawBodyParser, this.callback, this.errorHandler);

            this.on("close", function () {
                var node = this;
                RED.httpNode._router.stack.forEach(function (route, i, routes) {
                    if (route.route && route.route.path === node.url && route.route.methods[node.method]) {
                        routes.splice(i, 1);
                    }
                });
            });
        } else {
            this.warn("sigfox in: Cannot create sigfox-in node when httpNodeRoot set to false");
        }
    }
    RED.nodes.registerType("sigfox in", sigfoxIn);
}