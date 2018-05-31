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
	var os = require('os');
	var ifaces = os.networkInterfaces();
	var msgs = [{}, {}];

	function EventLog(config) {
		RED.nodes.createNode(this, config);
		var node = this;
		node.on('input', function (msg) {
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
			var modcom = config.modcom;

			var ipext = (msg.payload.ipext ? msg.payload.ipext : config.ipext);
			var payloadsize = JSON.stringify(msg.payload).length / 1000;
			var agent = "Node-Red";
			var motivation = config.motivation;
			var lang = (msg.payload.lang ? msg.payload.lang : config.lang);
			var lat = (msg.payload.lat ? msg.payload.lat : config.lat);
			var lon = (msg.payload.lon ? msg.payload.lon : config.lon);
			var serviceuri = (msg.payload.serviceuri ? msg.payload.serviceuri : config.serviceuri);
			var message = (msg.payload.message ? msg.payload.message : config.message);
			var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
			var xmlHttp = new XMLHttpRequest();
			xmlHttp.open("GET", encodeURI(uri + "?p=log" + "&pid=" + pidlocal + "&tmstmp=" + timestamp + "&modCom=" + modcom + "&IP_local=" + iplocal + "&IP_ext=" + ipext +
				"&payloadSize=" + payloadsize + "&agent=" + agent + "&motivation=" + motivation + "&lang=" + lang + "&lat=" + (typeof lat != "undefined" ? lat : 0.0) + "&lon=" + (typeof lon != "undefined" ? lon : 0.0) + "&serviceUri=" + serviceuri + "&message=" + message), false); // false for synchronous request
			xmlHttp.send(null);
			msgs[1].payload = xmlHttp.responseText;
			msgs[0].payload = msg.payload;
			node.send(msgs);
		});
	}
	RED.nodes.registerType("event-log", EventLog);
}