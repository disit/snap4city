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
	function sigfoxDeviceFilterNode(config) {
		RED.nodes.createNode(this, config);
		var node = this;
		node.device = config.device;

		node.on('input', function (msg) {
			if (!node.device) {
				node.warn("sigfox device filter: Device not set");
				return;
			}

			var message;
			try {
				if (typeof msg.payload === 'string' || msg.payload instanceof String)
					message = JSON.parse(msg.payload);
				else
					message = msg.payload;
			} catch (err) {
				node.warn("sigfox device filter: Can't parse received message");
				return;
			}
			if (!message instanceof Object || message == null) {
				node.warn("sigfox device filter: Message is not an object");
				return;
			}
			if (!message.hasOwnProperty("device")) {
				node.warn("sigfox device filter: Missing property 'device'");
				return;
			}

			if (String(message.device).toLowerCase() == node.device.toLowerCase()) {
				node.send(msg);
			}
		});
	}
	RED.nodes.registerType("sigfox device filter", sigfoxDeviceFilterNode);
}