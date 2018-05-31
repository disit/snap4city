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

    function GetMyDelegated(config) {
        RED.nodes.createNode(this, config);
        var node = this;
        node.on('input', function (msg) {
            var uri = "http://192.168.0.10:8080/datamanager/api/v1/apps/" + RED.settings.APPID + "/delegated";
            var uid = RED.settings.APPID;
            var inPayload = msg.payload;
            var accessToken = retrieveAccessToken();
            var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
            var xmlHttp = new XMLHttpRequest();
            if (checkConsent) {
                console.log(encodeURI(uri + "?sourceRequest=iotapp&accessToken=" + accessToken));
                xmlHttp.open("GET", encodeURI(uri + "?sourceRequest=iotapp&accessToken=" + accessToken), false);
                xmlHttp.setRequestHeader("Content-Type", "application/json");
                xmlHttp.send(null);
                if (xmlHttp.responseText != "") {
                    try {
                        msg.payload = JSON.parse(xmlHttp.responseText);
                    } catch (e) {
                        msg.payload = xmlHttp.responseText;
                    }
                } else {
                    msg.payload = JSON.parse("{\"status\": \"There was some problem\"}");
                }
                eventLog(inPayload, msg, config, "Node-Red", "MyData", uri, "RX");
            } else {
                msg.payload = JSON.parse("{'failure': 'You don't have the permission'}");
            }
            node.send(msg);
        });
    }

    function checkConsent() {
        return true;
        var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
        var xmlHttp = new XMLHttpRequest();
        console.log(encodeURI("http://192.168.0.10:8080/datamanager/api/v1/apps/" + RED.settings.APPID + "/consents"));
        xmlHttp.open("GET", encodeURI("http://192.168.0.10:8080/datamanager/api/v1/apps/" + RED.settings.APPID + "/consents"), false);
        xmlHttp.send(null);
        if (xmlHttp.responseText != "") {
            return JSON.parse(xmlHttp.responseText)[0].value;
        }
        return false;
    }

    function retrieveAccessToken() {
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
            return response.access_token;
        }
        return response;
    }
    RED.nodes.registerType("get-my-delegated", GetMyDelegated);
}