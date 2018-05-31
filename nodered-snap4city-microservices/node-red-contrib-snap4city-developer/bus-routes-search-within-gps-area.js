/* NODE-RED-CONTRIB-SNAP4CITY-DEVELOPER
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

    function BusRoutesSearchWithinGpsArea(config) {
        RED.nodes.createNode(this, config);
        var node = this;
        node.on('input', function (msg) {
            var uri = "http://servicemap.km4city.org/WebAppGrafo/api/v1/tpl/";
            var latitudebottomleft = (msg.payload.latitudebottomleft ? msg.payload.latitudebottomleft : config.latitudebottomleft);
            var longitudebottomleft = (msg.payload.longitudebottomleft ? msg.payload.longitudebottomleft : config.longitudebottomleft);
            var latitudetopright = (msg.payload.latitudetopright ? msg.payload.latitudetopright : config.latitudetopright);
            var longitudetopright = (msg.payload.longitudetopright ? msg.payload.longitudetopright : config.longitudetopright);
            var agency = (msg.payload.agency ? msg.payload.agency : config.agency);
            var maxResults = (msg.payload.maxresults ? msg.payload.maxresults : config.maxresults);
            var geometry = (msg.payload.geometry ? msg.payload.geometry : config.geometry);
            var uid = RED.settings.APPID;
            var inPayload = msg.payload;
            var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
            var xmlHttp = new XMLHttpRequest();
            console.log(encodeURI(uri + "?selection=" + latitudebottomleft + ";" + longitudebottomleft + ";" + latitudetopright + ";" + longitudetopright + "&agency=" + agency + "&maxResults=" + maxResults + "&format=json" + "&geometry=" + geometry + (typeof uid != "undefined" && uid != "" ? "&uid=" + uid : "") + "&appID=iotapp"));
            xmlHttp.open("GET", encodeURI(uri + "?selection=" + latitudebottomleft + ";" + longitudebottomleft + ";" + latitudetopright + ";" + longitudetopright + "&agency=" + agency + "&maxResults=" + maxResults + "&format=json" + "&geometry=" + geometry + (typeof uid != "undefined" && uid != "" ? "&uid=" + uid : "") + "&appID=iotapp"), false); // false for synchronous request
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
            eventLog(inPayload, msg, config, "Node-Red", "ASCAPI", uri, "RX");
            node.send(msg);
        });
    }
    RED.nodes.registerType("bus-routes-search-within-gps-area", BusRoutesSearchWithinGpsArea);
}