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

    function FullTextSearchWithinCircle(config) {
        RED.nodes.createNode(this, config);
        var node = this;
        var msgs = [{}, {}];
        node.on('input', function (msg) {
            var uri = "http://servicemap.km4city.org/WebAppGrafo/api/v1/";
            var latitude = (msg.payload.latitude ? msg.payload.latitude : config.latitude);
            var longitude = (msg.payload.longitude ? msg.payload.longitude : config.longitude);
            var search = (msg.payload.search ? msg.payload.search : config.search);
            var maxDists = (msg.payload.maxdists ? msg.payload.maxdists : config.maxdists);
            var maxResults = (msg.payload.maxresults ? msg.payload.maxresults : config.maxresults);
            var language = (msg.payload.lang ? msg.payload.lang : config.lang);
            var uid = RED.settings.APPID;
            var inPayload = msg.payload;
            var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
            var xmlHttp = new XMLHttpRequest();
            console.log(encodeURI(uri + "?search=" + search + "&selection=" + latitude + ";" + longitude + "&maxResults=" + maxResults + "&maxDists=" + maxDists + "&format=json" + "&lang=" + language + "&geometry=true" + (typeof uid != "undefined" && uid != "" ? "&uid=" + uid : "") + "&appID=iotapp"));
            xmlHttp.open("GET", encodeURI(uri + "?search=" + search + "&selection=" + latitude + ";" + longitude + "&maxResults=" + maxResults + "&maxDists=" + maxDists + "&format=json" + "&lang=" + language + "&geometry=true" + (typeof uid != "undefined" && uid != "" ? "&uid=" + uid : "") + "&appID=iotapp"), false); // false for synchronous request
            xmlHttp.send(null);

            if (xmlHttp.responseText != "") {
                var response = JSON.parse(xmlHttp.responseText);
                var serviceUriArray = [];
                for (var i = 0; i < response.features.length; i++) {
                    serviceUriArray.push(response.features[i].properties.serviceUri);
                }
                msgs[0].payload = serviceUriArray;
                msgs[1].payload = response;

            } else {
                msgs[0].payload = JSON.parse("{\"status\": \"error\"}");
                msgs[1].payload = JSON.parse("{\"status\": \"error\"}");
            }
            eventLog(inPayload, msgs, config, "Node-Red", "ASCAPI", uri, "RX");
            node.send(msgs);
        });
    }
    RED.nodes.registerType("full-text-search-within-circle", FullTextSearchWithinCircle);

    RED.httpAdmin.get('/s4c/js/*', function (req, res) {
        var options = {
            root: __dirname + '/lib/js/',
            dotfiles: 'deny'
        };

        res.sendFile(req.params[0], options);
    });

    RED.httpAdmin.get('/s4c/css/*', function (req, res) {
        var options = {
            root: __dirname + '/lib/css/',
            dotfiles: 'deny'
        };

        res.sendFile(req.params[0], options);
    });

    RED.httpAdmin.get('/s4c/json/*', function (req, res) {
        var options = {
            root: __dirname + '/lib/json/',
            dotfiles: 'deny'
        };

        res.sendFile(req.params[0], options);
    });

    RED.httpAdmin.get('/s4c/img/*', function (req, res) {
        var options = {
            root: __dirname + '/lib/img/',
            dotfiles: 'deny'
        };

        res.sendFile(req.params[0], options);
    });
}