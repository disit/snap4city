/**
 * Copyright 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

/* NODE-RED-CONTRIB-SNAP4CITY-USER    Copyright (C) 2018 DISIT Lab http://www.disit.org - University of Florence     This program is free software: you can redistribute it and/or modify    it under the terms of the GNU Affero General Public License as    published by the Free Software Foundation, either version 3 of the    License, or (at your option) any later version.     This program is distributed in the hope that it will be useful,    but WITHOUT ANY WARRANTY; without even the implied warranty of    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the    GNU Affero General Public License for more details.     You should have received a copy of the GNU Affero General Public License    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
module.exports = function (RED) {
	"use strict";

	var bodyParser = require("body-parser");
	var getBody = require('raw-body');
	var jsonParser = bodyParser.json();
	var urlencParser = bodyParser.urlencoded({
		extended: true
	});
	var typer = require('media-typer');
	var isUtf8 = require('is-utf8');

	var http = require("follow-redirects").http;
	var https = require("follow-redirects").https;
	var urllib = require("url");
	var util = require('util');

	var testSubscriptionIDs = {}; //PB fix
	var subscriptionIDs = {}; //PB fix

	var when = require('when');
	var token = "";
	var INCLUDEATTR = false;

	var LIMIT = 30;
	var TIMEOUT = 30000; //30 seconds?		
	//var snap_key1= "";
	//var snap_key2= "";
	var elementID = "";


	function OrionService(n) {
		RED.nodes.createNode(this, n);
		var serviceNode = this;

		this.url = n.url;
		this.port = n.port;
		var orionUrl = getOrionUrl(n);
		var credentials = this.credentials;
		//snap_key1= credentials.user;
		//snap_key2=credentials.password;


		credentials.user = "";
		credentials.password = "";


		if (/\/$/.test(this.url)) {
			this.url = this.url.substring(this.url.length - 1);
		}

		var err = "";
		if (((!!credentials.password) != (!!credentials.user))) {
			err = "Missing orion credentials";
		}

		if (!this.port) {
			err = "Missing port";
		}

		if (err) {
			throw err;
		}


		this.init = function (node, n) {
			node.status({
				fill: "blue",
				shape: "dot",
				text: "Initializing"
			});

			return when.promise(function (resolve, reject) {
				// get token from context broker
				getToken(node, serviceNode.url, credentials).then(function () {
					/*console.log("validating broker connectivity");
					validateOrionConnectivity(node, orionUrl).then(function(){
						console.log("cleaning up");
						cleanupTestEnv(node, orionUrl).then(function(){
							console.log("initialization finished");
							resolve();
						});
					});*/
					resolve();
				});
			});
		};

		this.queryContext = function (node, n, payload) {
			node.status({
				fill: "blue",
				shape: "dot",
				text: "Querying data"
			});

			return when.promise(function (resolve, reject) {


				// for (var i=0; i<payload.length; i++){
				//		payload[i];
				//		console.log("elementID: " + payload[i]);
				//	}

				elementID = payload.entities[0].id;
				//snap_key1 = payload.entities[0].user;
				//snap_key2 = payload.entities[0].passuser;
				//console.log("elementID: " + elementID);	
				payload = JSON.stringify(payload);
				console.log("Payload: " + payload);
				var url = orionUrl + "/v1/queryContext?limit=" + n.limit + "&elementid=" + elementID + "&k1=" + n.userk1 + "&k2=" + n.passk2 + "&details=on";
				console.log("ORIONURL" + url);
				var opts = urllib.parse(url);
				opts.method = "POST";
				opts.headers = {};
				opts.headers['content-type'] = "application/json";
				opts.headers["Accept"] = "application/json";

				if (token) {
					opts.headers["X-Auth-Token"] = token;
				}

				opts.headers['content-length'] = payload.length;
				var msg = {};

				/*		var data_json = JSON.parse(payload);  //parse the JSON
				
			    data_json["snap_key1"]= snap_key1;
			    data_json["snap_key2"] = snap_key2;
				payload = JSON.stringify(data_json);
		

   			console.log("payload: " + payload);*/
				// http request with query parameters



				var req = ((/^https/.test(url)) ? https : http).request(opts, function (res) {
					(node.ret === "bin") ? res.setEncoding('binary'): res.setEncoding('utf8');
					msg.statusCode = res.statusCode;
					msg.payload = "";
					res.on('data', function (chunk) {
						msg.payload += chunk;
					});
					res.on('end', function () {

						if (res.statusCode === 200) {
							node.status({});
							resolve(msg);
						} else {
							reject(msg);
							node.status({
								fill: "red",
								shape: "ring",
								text: res.statusCode
							});
						}
					});
				});
				req.on('error', function (err) {
					reject(err);
				});

				req.write(payload);
				req.end();
			});
		};

		this.createContext = function (node, n, payload) {
			node.status({
				fill: "blue",
				shape: "dot",
				text: "sending request"
			});
			console.log("----------createContext payload: " + JSON.stringify(payload));
			elementID = payload.contextElements[0].id;
			return when.promise(function (resolve, reject) {
				request(node, "POST", payload, orionUrl + "/v1/updateContext?elementid=" + elementID + "&k1=" + n.userk1 + "&k2=" + n.passk2).then(function (result) {
					console.log("----------updateContext RESULT: " + JSON.stringify(result));
					node.status({});
					resolve(result);
				});
			});
		};

		this.subscribe = function (node, n, payload) {
			console.log("subscribe in with: " + orionUrl);
			console.log("with node id: " + node.id);

			var reference = payload.reference;
			node.status({
				fill: "blue",
				shape: "dot",
				text: "Subscribing"
			});
			elementID = payload.entities[0].id;
			var url = orionUrl + "/v1/subscribeContext?elementid=" + elementID + "&k1=" + n.userk1 + "&k2=" + n.passk2;
			console.log("ORIONURL" + url);
			payload = JSON.stringify(payload);

			var opts = urllib.parse(url);
			opts.method = "POST";
			opts.headers = {};
			opts.headers['content-type'] = "application/json";
			opts.headers["Accept"] = "application/json";
			opts.headers["X-Auth-Token"] = token;
			opts.headers['content-length'] = payload.length;

			var msg = {};

			// subscibing to context broker
			var req = ((/^https/.test(url)) ? https : http).request(opts, function (res) {
				(node.ret === "bin") ? res.setEncoding('binary'): res.setEncoding('utf8');
				msg.statusCode = res.statusCode;
				msg.headers = res.headers;
				msg.payload = "";
				res.on('data', function (chunk) {
					msg.payload += chunk;
				});
				res.on('end', function () {
					console.log("STOP " + msg.payload);
					if (JSON.parse(msg.payload).subscribeResponse != null) {
						var subscriptionID = JSON.parse(msg.payload).subscribeResponse.subscriptionId;

						var nodeID = (node.id + "").replace('.', '');
						console.log("---- " + nodeID + " pre subID:" + subscriptionIDs[nodeID]);
						subscriptionIDs[nodeID] = subscriptionID;

						if (subscriptionID) {
							var data = {
								"_id": node.id,
								"subscriptionId": subscriptionID,
								"brokerUrl": orionUrl
							}
						}
					}
				});
			});
			req.on('error', function (err) {
				msg.payload = err.toString() + " : " + url;
				msg.statusCode = err.code;
				node.send(msg);
				node.status({
					fill: "red",
					shape: "ring",
					text: err.code
				});
			});

			var errorHandler = function (err, req, res, next) {
				res.sendStatus(500);
			};

			var nodeID = (node.id + "").replace('.', '');
			listenOnUrl(nodeID, function (req, res) {
				console.log("----------------------");
				console.log("+++++++++++++++++++Received response with SUBID: " + req.body.subscriptionId);

				if (req.body.subscriptionId != subscriptionIDs[nodeID]) {
					console.log("Recognized invalid subscription in response body: " + req.body.subscriptionId);
					unsubscribeFromOrion(node, req.body.subscriptionId, orionUrl).then(function (res) {
						console.log("Unsubscribed invalid subscription: " + req.body.subscriptionId);
					});
				} else {
					var payload = formatOutput(node, n, req.body);
					console.log("formatted payload: " + payload);
					console.log("node id: " + node.id);
					node.send({
						payload: payload,
						statusCode: 200
					});
				}
			});

			node.status({
				fill: "blue",
				shape: "dot",
				text: "listening on " + reference
			});

			if (payload) {
				req.write(payload);
			}

			req.end();
		};
	}

	RED.nodes.registerType("orion-service", OrionService, {
		credentials: {
			user: {
				type: "text"
			},
			password: {
				type: "password"
			}
		}
	});

	function unsubscribeFromOrion(node, subscriptionId, url) {
		node.log("in unsubscribeFromOrion with: " + JSON.stringify(subscriptionId));
		url = url + "/v1/unsubscribeContext";

		// url must start http:// or https:// so assume http:// if not set
		if (!((url.indexOf("http://") === 0) || (url.indexOf("https://") === 0))) {
			url = "http://" + url;
		}

		var payload = JSON.stringify({
			"subscriptionId": subscriptionId
		});
		var opts = urllib.parse(url);
		opts.method = "POST";
		opts.headers = {
			'content-type': "application/json",
			"Accept": "application/json",
			'content-length': payload.length
		};

		if (token) {
			opts.headers["X-Auth-Token"] = token;
		}

		return when.promise(
			function (resolve, reject) {
				var req = ((/^https/.test(url)) ? https : http).request(opts, function (res) {
					(node.ret === "bin") ? res.setEncoding('binary'): res.setEncoding('utf8');
					var payload = "";
					res.on('data', function (chunk) {
						payload += chunk;
					});
					res.on('end', function () {
						node.status({});
						console.log("Unsubscribed: " + subscriptionId);
						resolve({});
					});
				});

				req.on('error', function (err) {
					node.status({
						fill: "red",
						shape: "ring",
						text: err.code
					});
					reject(err.toString());
				});

				req.write(payload);
				req.end();
			}
		);
	}

	// retrieve token from context broker
	function getToken(node, orionUrl, credentials) {
		var tokenUrl = orionUrl + "/token";
		if (tokenUrl.indexOf("http://") >= 0) {
			tokenUrl = "https://" + tokenUrl.substring(7);
		} else if (orionUrl.indexOf("https://") < 0) {
			tokenUrl = "https://" + tokenUrl;
		}

		var opts = urllib.parse(tokenUrl);

		opts.method = "POST";
		opts.headers = {};
		opts.headers['content-type'] = "application/json";
		opts.headers["Accept"] = "application/json";
		var payload = {
			"username": credentials.user,
			"password": credentials.password
		};

		payload = JSON.stringify(payload);

		opts.headers['content-length'] = payload.length;
		token = "";

		return when.promise(function (resolve, reject) {
			if (!credentials.user) {
				resolve();
			} else {
				console.log("--requesting token using payload: " + payload);
				var req = (https).request(opts, function (res) {
					(node.ret === "bin") ? res.setEncoding('binary'): res.setEncoding('utf8');

					res.on('data', function (chunk) {
						token += chunk;
					});

					res.on('end', function () {
						console.log("--resolved token: " + token);
						resolve(token);
					});
				});

				req.on('error', function (err) {
					reject(err);
					node.status({
						fill: "red",
						shape: "ring",
						text: err.code
					});
					node.send({
						payload: err.toString() + " : " + tokenUrl,
						statusCode: err.code
					});
				});

				req.write(payload);
				req.end();
			}
		});
	}

	function formatOutput(node, n, msg) {

		console.log("MSG: " + JSON.stringify(msg));
		var contextResponses = msg.contextResponses;
		var payload = [];

		contextResponses.forEach(function (entry) {
			var contextElement = entry.contextElement;
			delete contextElement.isPattern;
			if (!n.includeattr) {
				// removing attribute metadata
				node.log("cleaning contextElement.attributes: " + JSON.stringify(contextElement.attributes));
				contextElement.attributes.forEach(function (entry) {
					node.log("deleting: " + JSON.stringify(entry.metadatas));
					delete entry.metadatas;
				});
			}

			payload.push(contextElement);
		});

		return JSON.stringify(payload);
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

	function validateInput(node, n) {
		var err = null;

		n.port = n.port * 1;

		if (!n.enid || !n.entype) {
			err = "Missing subscription parameters";
		}

		if (err) {
			throw err;
		}

		//		n.attributes = n.attributes || '.*';
		n.attributes = n.attributes || [];
		n.condvals = n.condvalues || '.*';

	}

	function getSubscribePayload(node, n) {
		// prepare payload for context subscription
		// contains node uid and url besides data supplied in node fields

		var nodeID = node.id + "";
		nodeID = nodeID.replace('.', '');

		return when.promise(
			function (resolve, reject) {
				getMyUri(n).then(function (myUri) {
					resolve({
						"entities": [{
							"type": n.entype,
							"isPattern": n.ispattern,
							"id": n.enid
						}],
						"attributes": n.attributes,
						"reference": "http://" + myUri + "/" + nodeID,
						"duration": n.duration,
						"notifyConditions": [{
							"type": "ONCHANGE",
							"condValues": n.condvals
						}],
						"throttling": n.throttle
					});
				})
			}
		);
	}

	function getOrionUrl(n) {
		var orionUrl = n.url;

		if (!/^((http|https):\/\/)/.test(orionUrl)) {
			orionUrl = "http://" + orionUrl + ":" + n.port;
		}

		return orionUrl;
	}

	function getCreateTestElementPayload(node) {
		var nodeID = node.id + "";
		nodeID = nodeID.replace('.', '');

		var myval = Math.floor((Math.random() * 100) + 1);

		var payload = {
			"contextElements": [{
				"type": "Test",
				"isPattern": "false",
				"id": nodeID,
				"attributes": [{
					"name": "test",
					"type": "float",
					"value": myval
				}]
			}],
			"updateAction": "APPEND"
		};

		return payload;
	}

	function getDeleteElementPayload(node) {
		var nodeID = node.id + "";
		nodeID = nodeID.replace('.', '');

		var myval = Math.floor((Math.random() * 100) + 1);

		var payload = {
			"contextElements": [{
				"type": "Test",
				"isPattern": "false",
				"id": nodeID
			}],
			"updateAction": "DELETE"
		};

		return payload;
	}

	function request(node, method, payload, url) {
		console.log("URL:  " + url);
		var opts = urllib.parse(url);

		opts.method = method; //"POST";
		opts.headers = {};
		opts.headers['Content-Type'] = "application/json";
		opts.headers["Accept"] = "application/json";
		payload = JSON.stringify(payload);

		opts.headers['content-length'] = payload.length;
		if (token) {
			opts.headers["X-Auth-Token"] = token;
		}

		return when.promise(function (resolve, reject) {
			var req = ((/^https/.test(url)) ? https : http).request(opts, function (res) {
				(node.ret === "bin") ? res.setEncoding('binary'): res.setEncoding('utf8');

				var result = "";
				res.on('data', function (chunk) {
					result += chunk;
				});

				res.on('end', function () {
					resolve(JSON.parse(result));
				});
			});

			req.on('error', function (err) {
				reject(err);
				node.status({
					fill: "red",
					shape: "ring",
					text: err.code
				});
				node.send({
					payload: err.toString() + " : " + url,
					statusCode: err.code
				});
			});

			req.write(payload);
			req.end();
		});
	}

	function validateOrionConnectivity(node, orionUrl) {
		var createElementPayload = getCreateTestElementPayload(node);

		if (node.type == "fiware orion in") {
			console.log("doing two way broker connectivity validation");
			return validateOrionConnectivityTwoWays(node, orionUrl, createElementPayload);
		} else {
			console.log("doing one way broker connectivity validation");
			return validateOrionConnectivityOneWay(node, orionUrl, createElementPayload);
		}
	}

	// To validate two ways connectivity following flow implemented	
	// init: 
	// 1. (v) create context element of type Test with id: node uid
	// 2. temporary subscribe to changes to that element

	// action:
	// 3. change test element
	// 4. validate update message received

	// #cleanup
	// 5. delete element
	// 6. delete subscription
	function validateOrionConnectivityTwoWays(node, orionUrl, createElementPayload) {
		var nodeID = node.id + "";
		nodeID = nodeID.replace('.', '');

		return when.promise(function (resolve, reject) {
			listenOnUrl(nodeID, function (req, res) {
				var result = req.body;
				var testSubscriptionID = testSubscriptionIDs[nodeID]; //PB fix

				/*PB commentato forza la validazione
				if(result.subscriptionId != testSubscriptionID){
                	console.log(nodeID+" result.subscriptionId: " + result.subscriptionId + "!=" + testSubscriptionID);
                	node.error("Communication with context broker failed: received wrong subId");
					unsubscribeFromOrion(node, result.subscriptionId, orionUrl).then(function(res){
						console.log("Unsubscribed invalid subscription: " + result.subscriptionId + " res: " + JSON.stringify(res));
					});
					unsubscribeFromOrion(node, testSubscriptionID, orionUrl).then(function(res){
						console.log("Unsubscribed test subscription: " + testSubscriptionID + " res: " + JSON.stringify(res));
					});
					
                	reject("Communication with context broker failed");
                }else{*/
				console.log(nodeID + " result.subscriptionId: " + result.subscriptionId + "==" + testSubscriptionID);
				resolve();
				//}
			});

			request(node, "POST", createElementPayload, orionUrl + "/v1/updateContext").then(function (result) {

				getSubscribeTestPayload(node).then(function (subscribePayload) {
					request(node, "POST", subscribePayload, orionUrl + "/v1/subscribeContext").then(function (subscription) {

						try {
							var testSubscriptionID = subscription.subscribeResponse.subscriptionId
							console.log(node.id + " " + nodeID + " pre test:" + testSubscriptionIDs[nodeID]);
							testSubscriptionIDs[nodeID] = testSubscriptionID;

							if (testSubscriptionID) {
								var data = {
									"_id": "test_" + node.id,
									"subscriptionId": testSubscriptionID,
									"brokerUrl": orionUrl
								}
							}

						} catch (e) {
							node.error(RED._("httpin.errors.json-error"));
							reject(e);
						}
					});
				});
			});
		});
	}

	function validateOrionConnectivityOneWay(node, orionUrl, createElementPayload) {
		console.log("in validateOrionConnectivityOneWay with createElementPayload: " + JSON.stringify(createElementPayload));

		return when.promise(function (resolve, reject) {
			request(node, "POST", createElementPayload, orionUrl + "/v1/updateContext").then(function (result) {
				rcValidate(200, result, reject);
				resolve();
			});
		});
	}

	function rcValidate(expected, result, reject) {
		var rc = result.contextResponses[0].statusCode.code;
		if (rc != expected) {
			reject("Return Code: " + rc + " is not " + expected)
		}
	}

	function cleanupTestEnv(node, orionUrl) {
		console.log("deleting listener path");
		return when.promise(function (resolve, reject) {

			//delete test context entity, common for both nodes 
			request(node, "POST", getDeleteElementPayload(node), orionUrl + "/v1/updateContext").then(function (res) {});

			if (node.type != "fiware orion in") {
				resolve();
			} else {
				var nodeID = node.id + "";
				nodeID = nodeID.replace('.', '');
				RED.httpNode._router.stack.forEach(function (route, i, routes) {
					if (route.route && route.route.path) {
						routes.splice(i, 1);
					}
				});


				// unsubscribe exisiting subscription from context broker
				unsubscribeFromOrion(node, testSubscriptionIDs[nodeID], orionUrl).then(function (res) {
					resolve({});
				});
			}
		});
	}

	function OrionSubscribe(n) {
		RED.nodes.createNode(this, n);
		this.service = n.service;
		this.brokerConn = RED.nodes.getNode(this.service);
		this.noderedhost = n.noderedhost;

		var node = this;

		this.on("close", function () {
			var node = this;

			var nodeID = node.id + "";
			nodeID = nodeID.replace('.', '');

			RED.httpNode._router.stack.forEach(function (route, i, routes) {
				if (route.route && route.route.path == "/" + nodeID) {
					routes.splice(i, 1);
				}
			});
		});

		// validate mandatory fields
		validateInput(this, n);

		node.brokerConn.init(node, n).then(function () {
			getSubscribePayload(node, n).then(function (payload) {
				node.brokerConn.subscribe(node, n, payload);
			});
		});
	}

	//Register OrionSubscribe node
	RED.nodes.registerType("fiware orion in", OrionSubscribe, {
		credentials: {
			user: {
				type: "text"
			},
			password: {
				type: "password"
			}
		}
	});

	function listenOnUrl(url, callback) {
		var errorHandler = function (err, req, res, next) {
			res.sendStatus(500);
		};

		var next = function (req, res, next) {
			next();
		}

		// will listen on 'localhost/url' for notifications from context broker and call callback function
		RED.httpNode.post("/" + url, next, next, next, jsonParser, urlencParser, rawBodyParser, callback, errorHandler);
	}

	function getSubscribeTestPayload(node, n) {
		// prepare payload for context subscription
		// contains node uid and url besides data supplied in node fields

		var nodeID = node.id + "";
		nodeID = nodeID.replace('.', '');

		return when.promise(
			function (resolve, reject) {
				getMyUri(node).then(function (myUri) {
					console.log("myUri: " + myUri);
					resolve({
						"entities": [{
							"type": "Test",
							"isPattern": "false",
							"id": nodeID
						}],
						"attributes": "test",
						"reference": "http://" + myUri + "/" + nodeID,
						"duration": "PT30S",
						"notifyConditions": [{
							"type": "ONCHANGE",
							"condValues": ["test"]
						}],
						"throttling": "PT5S"
					});
				});
			});
	}

	function getMyUri(node) {
		return when.promise(
			function (resolve, reject) {

				// first try to get user specified uri, TODO: many input validations...
				var myUri = node.noderedhost;
				if (myUri) {
					resolve(myUri + RED.settings.httpRoot /*+ ":" + RED.settings.uiPort*/ ); //PB removed port
				} else {
					myUri = RED.settings.externalHost; //PB fix added
					if (myUri)
						resolve(myUri + RED.settings.httpRoot /*+ ":" + RED.settings.uiPort*/ ); //PB remove port
					else {
						// attempt to get from bluemix
						try {
							var app = JSON.parse(process.env.VCAP_APPLICATION);
							myUri = app['application_uris'][0];
						} catch (e) {
							console.log("Probably not running in bluemix...");
						}
					}
				}

				if (myUri) {
					resolve(myUri);
				} else {
					var net = require('net');
					var client = net.connect({
							port: 80,
							host: "google.com"
						},
						function () {
							if (!client.localAddress) {
								reject("Failed to get local address");
							} else {
								resolve(client.localAddress + ":" + RED.settings.uiPort);
							}
						}
					);
				}
			}
		);
	}

	//////Orion-request node constructor
	function Orion(n) {
		RED.nodes.createNode(this, n);

		this.on("input", function (msg) {
			this.service = n.service;
			this.brokerConn = RED.nodes.getNode(this.service);
			var node = this;

			// process input from UI and input pipe
			processInput(this, n, msg);

			// create json payload for context request
			var payload = {
				"entities": [{
					"type": n.entype,
					"isPattern": true,
					"id": n.enid
				}],
				"attributes": n.attributes
			};

			if (n.rtype && n.rvalue) {
				payload.restriction = {
					"scopes": [{
						"type": n.rtype,
						"value": n.rvalue
					}]
				};
			}

			try {
				node.brokerConn.init(node, n).then(function () {
					node.brokerConn.queryContext(node, n, payload).then(
						function (msg) {
							msg = formatOutput(node, n, JSON.parse(msg.payload));

							/*var cred_json = {
					        	   "key1": snap_key1,
					        	   "key2": snap_key2
					           	};
								
								var topic =  JSON.stringify(cred_json);
								console.log("TOPIC" + topic);*/

							node.send({
								payload: msg,
								statusCode: 200
							});
						},
						function (reason) {
							node.error("failed to query, reason: " + reason.payload);
						}
					);
				});
			} catch (err) {
				node.error(err, msg);
				node.send({
					payload: err.toString(),
					statusCode: err.code
				});
			}
		});
	}

	// register node
	RED.nodes.registerType("fiware orion", Orion, {
		credentials: {
			user: {
				type: "text"
			},
			password: {
				type: "password"
			},
			token: {
				type: "text"
			}
		}
	});

	function processInput(node, n, msg) {
		n.url = n.url || msg.url;
		n.port = n.port || msg.port;
		n.enid = n.enid || msg.enid || ".*";
		n.entype = n.entype || msg.entype;
		n.limit = n.limit || msg.limit || LIMIT;
		n.userk1 = n.userk1 || msg.userk1;
		n.passk2 = n.passk2 || msg.passk2;
		n.attributes = n.attributes || msg.attributes;
		n.ispattern = n.ispattern || msg.ispattern || false;
		n.includeattr = n.includeattr || msg.includeattr;

		n.rtype = n.rtype || msg.rtype;
		n.rvalue = n.rvalue || msg.rvalue;

		if (n.rtype && !n.rvalue) {
			n.rvalue = "entity::type";
		}

		//	n.attributes = n.attributes || '.*';
		n.attributes = n.attributes || [];
		if (n.attributes.constructor !== Array) {
			n.attributes = (n.attributes || "").split(",");
			for (var i = 0; i < n.attributes.length; i++) {
				n.attributes[i] = n.attributes[i].trim();
			}
		}
	}

	// register node
	RED.nodes.registerType("orion-test", OrionTest);

	//Orion-test node constructor
	function OrionTest(n) {
		RED.nodes.createNode(this, n);

		this.on("input", function (msg) {
			this.service = n.service;
			this.brokerConn = RED.nodes.getNode(this.service);
			var node = this;

			// create json payload for context update
			var payload = generateCreateElementPayload(node, n, msg);

			try {
				node.brokerConn.init(node, n).then(function () {
					node.brokerConn.createContext(node, n, payload).then(
						function (msg) {},
						function (reason) {
							node.error("failed to create, reason: " + reason.payload);
						}
					);
				});
			} catch (err) {
				node.error(err, msg);
				node.send({
					payload: err.toString(),
					statusCode: err.code
				});
			}
		});
	}

	function generateCreateElementPayload(node, n, msg) {
		console.log("msg: " + JSON.stringify(msg));

		var attributes = [];
		if (n.attrkey && n.attrvalue) {
			var name = n.attrkey.trim();
			var value = n.attrvalue.trim();
			attributes.push({
				name,
				value
			});
		} else {
			attributes = msg.attributes;
		}

		if (!attributes) {
			throw "Missing 'attributes' property";
		}

		console.log("attributes2: " + attributes);
		var payload = {
			"contextElements": [{
				"type": n.entype,
				"isPattern": "false",
				"id": n.enid,
				"attributes": attributes
			}],
			"updateAction": "APPEND"
		};

		return payload;
	}
}