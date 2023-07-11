/**
 * Copyright JS Foundation and other contributors, http://js.foundation
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

module.exports = function(RED) {
    "use strict";
    var mqtt = require("mqtt");
    var isUtf8 = require('is-utf8');
    var HttpsProxyAgent = require('https-proxy-agent');
    var url = require('url');

    //#region "Supporting functions"
    function matchTopic(ts,t) {
        if (ts == "#") {
            return true;
        }
        /* The following allows shared subscriptions (as in MQTT v5)
           http://docs.oasis-open.org/mqtt/mqtt/v5.0/cs02/mqtt-v5.0-cs02.html#_Toc514345522

           4.8.2 describes shares like:
           $share/{ShareName}/{filter}
           $share is a literal string that marks the Topic Filter as being a Shared Subscription Topic Filter.
           {ShareName} is a character string that does not include "/", "+" or "#"
           {filter} The remainder of the string has the same syntax and semantics as a Topic Filter in a non-shared subscription. Refer to section 4.7.
        */
        else if(ts.startsWith("$share")){
            ts = ts.replace(/^\$share\/[^#+/]+\/(.*)/g,"$1");
        }
        var re = new RegExp("^"+ts.replace(/([\[\]\?\(\)\\\\$\^\*\.|])/g,"\\$1").replace(/\+/g,"[^/]+").replace(/\/#$/,"(\/.*)?")+"$");
        return re.test(t);
    }

    /**
     * Helper function for setting integer property values in the MQTT V5 properties object
     * @param {object} src Source object containing properties
     * @param {object} dst Destination object to set/add properties
     * @param {string} propName The property name to set in the Destination object
     * @param {integer} [minVal] The minimum value. If the src value is less than minVal, it will NOT be set in the destination
     * @param {integer} [maxVal] The maximum value. If the src value is greater than maxVal, it will NOT be set in the destination
     * @param {integer} [def] An optional default to set in the destination object if prop is NOT present in the soruce object
     */
    function setIntProp(src, dst, propName, minVal, maxVal, def) {
        if (hasProperty(src, propName)) {
            var v = parseInt(src[propName]);
            if(isNaN(v)) return;
            if(minVal != null) {
                if(v < minVal) return;
            }
            if(maxVal != null) {
                if(v > maxVal) return;
            }
            dst[propName] = v;
        } else {
            if(def != undefined) dst[propName] = def;
        }
    }

    /**
     * Test a topic string is valid
     * @param {string} topic
     * @returns `true` if it is a valid topic
     */
    function isValidSubscriptionTopic(topic) {
        return /^(#$|(\+|[^+#]*)(\/(\+|[^+#]*))*(\/(\+|#|[^+#]*))?$)/.test(topic)
    }

    /**
     * Helper function for setting string property values in the MQTT V5 properties object
     * @param {object} src Source object containing properties
     * @param {object} dst Destination object to set/add properties
     * @param {string} propName The property name to set in the Destination object
     * @param {string} [def] An optional default to set in the destination object if prop is NOT present in the soruce object
     */
    function setStrProp(src, dst, propName, def) {
        if (src[propName] && typeof src[propName] == "string") {
            dst[propName] = src[propName];
        } else {
            if(def != undefined) dst[propName] = def;
        }
    }

    /**
     * Helper function for setting boolean property values in the MQTT V5 properties object
     * @param {object} src Source object containing properties
     * @param {object} dst Destination object to set/add properties
     * @param {string} propName The property name to set in the Destination object
     * @param {boolean} [def] An optional default to set in the destination object if prop is NOT present in the soruce object
     */
    function setBoolProp(src, dst, propName, def) {
        if (src[propName] != null) {
            if(src[propName] === "true" || src[propName] === true) {
                dst[propName] = true;
            } else if(src[propName] === "false" || src[propName] === false) {
                dst[propName] = true;
            }
        } else {
            if(def != undefined) dst[propName] = def;
        }
    }

    /**
     * Helper function for copying the MQTT v5 srcUserProperties object (parameter1) to the properties object (parameter2).
     * Any property in srcUserProperties that is NOT a key/string pair will be silently discarded.
     * NOTE: if no sutable properties are present, the userProperties object will NOT be added to the properties object
     * @param {object} srcUserProperties An object with key/value string pairs
     * @param {object} properties A properties object in which userProperties will be copied to
     */
    function setUserProperties(srcUserProperties, properties) {
        if (srcUserProperties && typeof srcUserProperties == "object") {
            let _clone = {};
            let count = 0;
            let keys = Object.keys(srcUserProperties);
            if(!keys || !keys.length) return null;
            keys.forEach(key => {
                let val = srcUserProperties[key];
                if(typeof val == "string") {
                    count++;
                    _clone[key] = val;
                }
            });
            if(count) properties.userProperties = _clone;
        }
    }

    /**
     * Helper function for copying the MQTT v5 buffer type properties
     * NOTE: if src[propName] is not a buffer, dst[propName] will NOT be assigned a value (unless def is set)
     * @param {object} src Source object containing properties
     * @param {object} dst Destination object to set/add properties
     * @param {string} propName The property name to set in the Destination object
     * @param {boolean} [def] An optional default to set in the destination object if prop is NOT present in the Source object
     */
    function setBufferProp(src, dst, propName, def) {
        if(!dst) return;
        if (src && dst) {
            var buf = src[propName];
            if (buf && typeof Buffer.isBuffer(buf)) {
                dst[propName] = Buffer.from(buf);
            }
        } else {
            if(def != undefined) dst[propName] = def;
        }
    }

    /**
     * Helper function for applying changes to an objects properties ONLY when the src object actually has the property.
     * This avoids setting a `dst` property null/undefined when the `src` object doesnt have the named property.
     * @param {object} src Source object containing properties
     * @param {object} dst Destination object to set property
     * @param {string} propName The property name to set in the Destination object
     * @param {boolean} force force the dst property to be updated/created even if src property is empty
     */
    function setIfHasProperty(src, dst, propName, force) {
        if (src && dst && propName) {
            const ok = force || hasProperty(src, propName);
            if (ok) {
                dst[propName] = src[propName];
            }
        }
    }

    /**
     * Helper function to test an object has a property
     * @param {object} obj Object to test
     * @param {string} propName Name of property to find
     * @returns true if object has property `propName`
     */
    function hasProperty(obj, propName) {
        //JavaScript does not protect the property name hasOwnProperty
        //Object.prototype.hasOwnProperty.call is the recommended/safer test
        return Object.prototype.hasOwnProperty.call(obj, propName);
    }

    /**
     * Handle the payload / packet recieved in MQTT In and MQTT Sub nodes
     */
    function subscriptionHandler(node, datatype ,topic, payload, packet) {
        const v5 = node.brokerConn.options && node.brokerConn.options.protocolVersion == 5;

        if (datatype === "buffer") {
            // payload = payload;
        } else if (datatype === "base64") {
            payload = payload.toString('base64');
        } else if (datatype === "utf8") {
            payload = payload.toString('utf8');
        } else if (datatype === "json") {
            if (isUtf8(payload)) {
                payload = payload.toString();
                try { payload = JSON.parse(payload); }
                catch(e) { node.error(RED._("mqtt.errors.invalid-json-parse"),{payload:payload, topic:topic, qos:packet.qos, retain:packet.retain}); return; }
            }
            else { node.error((RED._("mqtt.errors.invalid-json-string")),{payload:payload, topic:topic, qos:packet.qos, retain:packet.retain}); return; }
        } else {
            if (isUtf8(payload)) { payload = payload.toString(); }
        }
        var msg = {topic:topic, payload:payload, qos:packet.qos, retain:packet.retain};
        if(v5 && packet.properties) {
            setStrProp(packet.properties, msg, "responseTopic");
            setBufferProp(packet.properties, msg, "correlationData");
            setStrProp(packet.properties, msg, "contentType");
            setIntProp(packet.properties, msg, "messageExpiryInterval", 0);
            setBoolProp(packet.properties, msg, "payloadFormatIndicator");
            setStrProp(packet.properties, msg, "reasonString");
            setUserProperties(packet.properties.userProperties, msg);
        }
        if ((node.brokerConn.broker === "localhost")||(node.brokerConn.broker === "127.0.0.1")) {
            msg._topic = topic;
        }
        node.send(msg);
    }

    /**
     * Send an mqtt message to broker
     * @param {MQTTOutNode} node the owner node
     * @param {object} msg The msg to prepare for publishing
     * @param {function} done callback when done
     */
    function doPublish(node, msg, done) {
        try {
            done = typeof done == "function" ? done : function noop(){};
            let v5 = node.brokerConn.options && node.brokerConn.options.protocolVersion == 5;
            const bsp = (node.brokerConn && node.brokerConn.serverProperties) || {};

            //Sanitise the `msg` object properties ready for publishing
            if (msg.qos) {
                msg.qos = parseInt(msg.qos);
                if ((msg.qos !== 0) && (msg.qos !== 1) && (msg.qos !== 2)) {
                    msg.qos = null;
                }
            }

            /* If node properties exists, override/set that to property in msg  */
            if (node.topic) { msg.topic = node.topic; }
            msg.qos = Number(node.qos || msg.qos || 0);
            msg.retain = node.retain || msg.retain || false;
            msg.retain = ((msg.retain === true) || (msg.retain === "true")) || false;

            if (v5) {
                if (node.userProperties) {
                    msg.userProperties = node.userProperties;
                }
                if (node.responseTopic) {
                    msg.responseTopic = node.responseTopic;
                }
                if (node.correlationData) {
                    msg.correlationData = node.correlationData;
                }
                if (node.contentType) {
                    msg.contentType = node.contentType;
                }
                if (node.messageExpiryInterval) {
                    msg.messageExpiryInterval = node.messageExpiryInterval;
                }
            }
            if (msg.userProperties && typeof msg.userProperties !== "object") {
                delete msg.userProperties;
            }
            if (hasProperty(msg, "topicAlias") && !isNaN(msg.topicAlias) && (msg.topicAlias === 0 || bsp.topicAliasMaximum === 0 || msg.topicAlias > bsp.topicAliasMaximum)) {
                delete msg.topicAlias;
            }

            if (hasProperty(msg, "payload")) {

                //check & sanitise topic
                let topicOK = hasProperty(msg, "topic") && (typeof msg.topic === "string") && (msg.topic !== "");

                if (!topicOK && v5) {
                    //NOTE: A value of 0 (in server props topicAliasMaximum) indicates that the Server does not accept any Topic Aliases on this connection
                    if (hasProperty(msg, "topicAlias") && !isNaN(msg.topicAlias) && msg.topicAlias >= 0 && bsp.topicAliasMaximum && bsp.topicAliasMaximum >= msg.topicAlias) {
                        topicOK = true;
                        msg.topic = ""; //must be empty string
                    } else if (hasProperty(msg, "responseTopic") && (typeof msg.responseTopic === "string") && (msg.responseTopic !== "")) {
                        //TODO: if topic is empty but responseTopic has a string value, use that instead. Is this desirable?
                        topicOK = true;
                        msg.topic = msg.responseTopic;
                        //TODO: delete msg.responseTopic - to prevent it being resent?
                    }
                }
                topicOK = topicOK && !/[\+#\b\f\n\r\t\v\0]/.test(msg.topic);

                if (topicOK) {
                    node.brokerConn.publish(msg, done); // send the message
                } else {
                    node.warn(RED._("mqtt.errors.invalid-topic"));
                    done();
                }
            } else {
                done();
            }
        } catch (error) {
            done(error);
        }
    }

    function setStatusDisconnected(node, allNodes) {
        if(allNodes) {
            for (var id in node.users) {
                if (hasProperty(node.users, id)) {
                    node.users[id].status({ fill: "red", shape: "ring", text: "node-red:common.status.disconnected" });
                }
            }
        } else {
            node.status({ fill: "red", shape: "ring", text: "node-red:common.status.disconnected" });
        }
    }

    function setStatusConnecting(node, allNodes) {
        if(allNodes) {
            for (var id in node.users) {
                if (hasProperty(node.users, id)) {
                    node.users[id].status({ fill: "yellow", shape: "ring", text: "node-red:common.status.connecting" });
                }
            }
        } else {
            node.status({ fill: "yellow", shape: "ring", text: "node-red:common.status.connecting" });
        }
    }

    function setStatusConnected(node, allNodes) {
        if(allNodes) {
            for (var id in node.users) {
                if (hasProperty(node.users, id)) {
                    node.users[id].status({ fill: "green", shape: "dot", text: "node-red:common.status.connected" });
                }
            }
        } else {
            node.status({ fill: "green", shape: "dot", text: "node-red:common.status.connected" });
        }
    }

    function handleConnectAction(node, msg, done) {
        let actionData = typeof msg.broker === 'object' ? msg.broker : null;
        if (node.brokerConn.canConnect()) {
            // Not currently connected/connecting - trigger the connect
            if (actionData) {
                node.brokerConn.setOptions(actionData);
            }
            node.brokerConn.connect(function () {
                done();
            });
        } else {
            // Already Connected/Connecting
            if (!actionData) {
                // All is good - already connected and no broker override provided
                done()
            } else if (actionData.force) {
                // The force flag tells us to cycle the connection.
                node.brokerConn.disconnect(function() {
                    node.brokerConn.setOptions(actionData);
                    node.brokerConn.connect(function () {
                        done();
                    });
                })
            } else {
                // Without force flag, we will refuse to cycle an active connection
                done(new Error(RED._('mqtt.errors.invalid-action-alreadyconnected')));
            }
        }
    }

    function handleDisconnectAction(node, done) {
        node.brokerConn.disconnect(function () {
            done();
        });
    }

    //#endregion  "Supporting functions"

    //#region  "Broker node"
    function MQTTBrokerNode(n) {
        RED.nodes.createNode(this,n);
        const node = this;
        node.users = {};
        // Config node state
        node.brokerurl = "";
        node.connected = false;
        node.connecting = false;
        node.closing = false;
        node.options = {};
        node.queue = [];
        node.subscriptions = {};
        /** @type {mqtt.MqttClient}*/ this.client;
        node.setOptions = function(opts, init) {
            if(!opts || typeof opts !== "object") {
                return; //nothing to change, simply return
            }
            const originalBrokerURL = node.brokerurl;

            //apply property changes (only if the property exists in the opts object)
            setIfHasProperty(opts, node, "url", init);
            setIfHasProperty(opts, node, "broker", init);
            setIfHasProperty(opts, node, "port", init);
            setIfHasProperty(opts, node, "clientid", init);
            setIfHasProperty(opts, node, "autoConnect", init);
            setIfHasProperty(opts, node, "usetls", init);
            setIfHasProperty(opts, node, "usews", init);
            setIfHasProperty(opts, node, "verifyservercert", init);
            setIfHasProperty(opts, node, "compatmode", init);
            setIfHasProperty(opts, node, "protocolVersion", init);
            setIfHasProperty(opts, node, "keepalive", init);
            setIfHasProperty(opts, node, "cleansession", init);
            setIfHasProperty(opts, node, "sessionExpiry", init);
            setIfHasProperty(opts, node, "topicAliasMaximum", init);
            setIfHasProperty(opts, node, "maximumPacketSize", init);
            setIfHasProperty(opts, node, "receiveMaximum", init);
            setIfHasProperty(opts, node, "userProperties", init);//https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901116
            setIfHasProperty(opts, node, "userPropertiesType", init);

            function createLWT(topic, payload, qos, retain, v5opts, v5SubPropName) {
                let message = undefined;
                if(topic) {
                    message = {
                        topic: topic,
                        payload: payload || "",
                        qos: Number(qos||0),
                        retain: retain=="true"|| retain===true,
                    }
                    if (v5opts) {
                        let v5Properties = message;
                        if(v5SubPropName) {
                            v5Properties = message[v5SubPropName] = {};
                        }
                        //re-align local prop name to mqttjs std
                        if(hasProperty(v5opts, "respTopic")) { v5opts.responseTopic = v5opts.respTopic; }
                        if(hasProperty(v5opts, "correl")) { v5opts.correlationData = v5opts.correl; }
                        if(hasProperty(v5opts, "expiry")) { v5opts.messageExpiryInterval = v5opts.expiry; }
                        if(hasProperty(v5opts, "delay")) { v5opts.willDelayInterval = v5opts.delay; }
                        if(hasProperty(v5opts, "userProps")) { v5opts.userProperties = v5opts.userProps; }
                        //setup v5 properties
                        if(typeof v5opts.userProperties == "string" && /^ *{/.test(v5opts.userProperties)) {
                            try {
                                setUserProperties(JSON.parse(v5opts.userProps), v5Properties);
                            } catch(err) {}
                        } else if (typeof v5opts.userProperties == "object") {
                            setUserProperties(v5opts.userProperties, v5Properties);
                        }
                        setStrProp(v5opts, v5Properties, "contentType");
                        setStrProp(v5opts, v5Properties, "responseTopic");
                        setBufferProp(v5opts, v5Properties, "correlationData");
                        setIntProp(v5opts, v5Properties, "messageExpiryInterval");
                        setIntProp(v5opts, v5Properties, "willDelayInterval");
                    }
                }
                return message;
            }

            if(init) {
                if(hasProperty(opts, "birthTopic")) {
                    node.birthMessage = createLWT(opts.birthTopic, opts.birthPayload, opts.birthQos, opts.birthRetain, opts.birthMsg, "");
                };
                if(hasProperty(opts, "closeTopic")) {
                    node.closeMessage = createLWT(opts.closeTopic, opts.closePayload, opts.closeQos, opts.closeRetain, opts.closeMsg, "");
                };
                if(hasProperty(opts, "willTopic")) {
                    //will v5 properties must be set in the "properties" sub object
                    node.options.will = createLWT(opts.willTopic, opts.willPayload, opts.willQos, opts.willRetain, opts.willMsg, "properies");
                };
            } else {
                //update options
                if(hasProperty(opts, "birth")) {
                    if(typeof opts.birth !== "object") { opts.birth = {}; }
                    node.birthMessage = createLWT(opts.birth.topic, opts.birth.payload, opts.birth.qos, opts.birth.retain, opts.birth.properties, "");
                }
                if(hasProperty(opts, "close")) {
                    if(typeof opts.close !== "object") { opts.close = {}; }
                    node.closeMessage = createLWT(opts.close.topic, opts.close.payload, opts.close.qos, opts.close.retain, opts.close.properties, "");
                }
                if(hasProperty(opts, "will")) {
                    if(typeof opts.will !== "object") { opts.will = {}; }
                    //will v5 properties must be set in the "properties" sub object
                    node.options.will = createLWT(opts.will.topic, opts.will.payload, opts.will.qos, opts.will.retain, opts.will.properties, "properties");
                }
            }

            if (node.credentials) {
                node.username = node.credentials.user;
                node.password = node.credentials.password;
            }
            if(!init & hasProperty(opts, "username")) {
                node.username  = opts.username;
            };
            if(!init & hasProperty(opts, "password")) {
                node.password  = opts.password;
            };

            // If the config node is missing certain options (it was probably deployed prior to an update to the node code),
            // select/generate sensible options for the new fields
            if (typeof node.usetls === 'undefined') {
                node.usetls = false;
            }
            if (typeof node.usews === 'undefined') {
                node.usews = false;
            }
            if (typeof node.verifyservercert === 'undefined') {
                node.verifyservercert = false;
            }
            if (typeof node.keepalive === 'undefined') {
                node.keepalive = 60;
            } else if (typeof node.keepalive === 'string') {
                node.keepalive = Number(node.keepalive);
            }
            if (typeof node.cleansession === 'undefined') {
                node.cleansession = true;
            }

            //use url or build a url from usetls://broker:port
            if (node.url && node.brokerurl !== node.url) {
                node.brokerurl = node.url;
            } else {
                // if the broker is ws:// or wss:// or tcp://
                if (node.broker.indexOf("://") > -1) {
                    node.brokerurl = node.broker;
                    // Only for ws or wss, check if proxy env var for additional configuration
                    if (node.brokerurl.indexOf("wss://") > -1 || node.brokerurl.indexOf("ws://") > -1) {
                        // check if proxy is set in env
                        let prox, noprox;
                        if (process.env.http_proxy) { prox = process.env.http_proxy; }
                        if (process.env.HTTP_PROXY) { prox = process.env.HTTP_PROXY; }
                        if (process.env.no_proxy) { noprox = process.env.no_proxy.split(","); }
                        if (process.env.NO_PROXY) { noprox = process.env.NO_PROXY.split(","); }
                        if (noprox) {
                            for (var i = 0; i < noprox.length; i += 1) {
                                if (node.brokerurl.indexOf(noprox[i].trim()) !== -1) { noproxy = true; }
                            }
                        }
                        if (prox && !noproxy) {
                            var parsedUrl = url.parse(node.brokerurl);
                            var proxyOpts = url.parse(prox);
                            // true for wss
                            proxyOpts.secureEndpoint = parsedUrl.protocol ? parsedUrl.protocol === 'wss:' : true;
                            // Set Agent for wsOption in MQTT
                            var agent = new HttpsProxyAgent(proxyOpts);
                            node.options.wsOptions = {
                                agent: agent
                            };
                        }
                    }
                } else {
                    // construct the std mqtt:// url
                    if (node.usetls) {
                        node.brokerurl = "mqtts://";
                    } else {
                        node.brokerurl = "mqtt://";
                    }
                    if (node.broker !== "") {
                        //Check for an IPv6 address
                        if (/(?:^|(?<=\s))(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))(?=\s|$)/.test(node.broker)) {
                            node.brokerurl = node.brokerurl + "[" + node.broker + "]:";
                        } else {
                            node.brokerurl = node.brokerurl + node.broker + ":";
                        }
                        // port now defaults to 1883 if unset.
                        if (!node.port) {
                            node.brokerurl = node.brokerurl + "1883";
                        } else {
                            node.brokerurl = node.brokerurl + node.port;
                        }
                    } else {
                        node.brokerurl = node.brokerurl + "localhost:1883";
                    }
                }
            }

            // Ensure cleansession set if clientid not supplied
            if (!node.cleansession && !node.clientid) {
                node.cleansession = true;
                node.warn(RED._("mqtt.errors.nonclean-missingclientid"));
            }

            // Build options for passing to the MQTT.js API
            node.options.username = node.username;
            node.options.password = node.password;
            node.options.keepalive = node.keepalive;
            node.options.clean = node.cleansession;
            node.options.clientId = node.clientid || 'nodered_' + RED.util.generateId();
            node.options.reconnectPeriod = RED.settings.mqttReconnectTime||5000;
            delete node.options.protocolId; //V4+ default
            delete node.options.protocolVersion; //V4 default
            delete node.options.properties;//V5 only
            if (node.compatmode == "true" || node.compatmode === true || node.protocolVersion == 3) {
                node.options.protocolId = 'MQIsdp';//V3 compat only
                node.options.protocolVersion = 3;
            } else if ( node.protocolVersion == 5 ) {
                delete node.options.protocolId;
                node.options.protocolVersion = 5;
                node.options.properties = {};
                node.options.properties.requestResponseInformation = true;
                node.options.properties.requestProblemInformation = true;
                if(node.userProperties && /^ *{/.test(node.userProperties)) {
                    try {
                        setUserProperties(JSON.parse(node.userProperties), node.options.properties);
                    } catch(err) {}
                }
                if (node.sessionExpiryInterval && node.sessionExpiryInterval !== "0") {
                    setIntProp(node,node.options.properties,"sessionExpiryInterval");
                }
            }
            if (node.usetls && n.tls) {
                var tlsNode = RED.nodes.getNode(n.tls);
                if (tlsNode) {
                    tlsNode.addTLSOptions(node.options);
                }
            }

            // If there's no rejectUnauthorized already, then this could be an
            // old config where this option was provided on the broker node and
            // not the tls node
            if (typeof node.options.rejectUnauthorized === 'undefined') {
                node.options.rejectUnauthorized = (node.verifyservercert == "true" || node.verifyservercert === true);
            }
        }

        n.autoConnect = n.autoConnect === "false" || n.autoConnect === false ? false : true;
        node.setOptions(n, true);

        // Define functions called by MQTT in and out nodes
        node.register = function(mqttNode) {
            node.users[mqttNode.id] = mqttNode;
            if (Object.keys(node.users).length === 1) {
                if(node.autoConnect) {
                    node.connect();
                }
            }
        };

        node.deregister = function(mqttNode,done) {
            delete node.users[mqttNode.id];
            if (!node.closing && node.connected && Object.keys(node.users).length === 0) {
                node.disconnect();
            }
            done();
        };
        node.canConnect = function() {
            return !node.connected && !node.connecting;
        }
        node.connect = function (callback) {
            if (node.canConnect()) {
                node.closing = false;
                node.connecting = true;
                setStatusConnecting(node, true);
                try {
                    node.serverProperties = {};
                    node.client = mqtt.connect(node.brokerurl, node.options);
                    node.client.setMaxListeners(0);
                    let callbackDone = false; //prevent re-connects causing node.client.on('connect' firing callback multiple times
                    // Register successful connect or reconnect handler
                    node.client.on('connect', function (connack) {
                        node.closing = false;
                        node.connecting = false;
                        node.connected = true;
                        if(!callbackDone && typeof callback == "function") {
                            callback();
                        }
                        callbackDone = true;
                        node.topicAliases = {};
                        node.log(RED._("mqtt.state.connected",{broker:(node.clientid?node.clientid+"@":"")+node.brokerurl}));
                        if(node.options.protocolVersion == 5 && connack && hasProperty(connack, "properties")) {
                            if(typeof connack.properties == "object") {
                                //clean & assign all props sent from server.
                                setIntProp(connack.properties, node.serverProperties, "topicAliasMaximum", 0);
                                setIntProp(connack.properties, node.serverProperties, "receiveMaximum", 0);
                                setIntProp(connack.properties, node.serverProperties, "sessionExpiryInterval", 0, 0xFFFFFFFF);
                                setIntProp(connack.properties, node.serverProperties, "maximumQoS", 0, 2);
                                setBoolProp(connack.properties, node.serverProperties, "retainAvailable",true);
                                setBoolProp(connack.properties, node.serverProperties, "wildcardSubscriptionAvailable", true);
                                setBoolProp(connack.properties, node.serverProperties, "subscriptionIdentifiersAvailable", true);
                                setBoolProp(connack.properties, node.serverProperties, "sharedSubscriptionAvailable");
                                setIntProp(connack.properties, node.serverProperties, "maximumPacketSize", 0);
                                setIntProp(connack.properties, node.serverProperties, "serverKeepAlive");
                                setStrProp(connack.properties, node.serverProperties, "responseInformation");
                                setStrProp(connack.properties, node.serverProperties, "serverReference");
                                setStrProp(connack.properties, node.serverProperties, "assignedClientIdentifier");
                                setStrProp(connack.properties, node.serverProperties, "reasonString");
                                setUserProperties(connack.properties, node.serverProperties);
                            }
                        }
                        setStatusConnected(node, true);
                        // Remove any existing listeners before resubscribing to avoid duplicates in the event of a re-connection
                        node.client.removeAllListeners('message');

                        // Re-subscribe to stored topics
                        for (var s in node.subscriptions) {
                            if (node.subscriptions.hasOwnProperty(s)) {
                                let topic = s;
                                let qos = 0;
                                let _options = {};
                                for (var r in node.subscriptions[s]) {
                                    if (node.subscriptions[s].hasOwnProperty(r)) {
                                        qos = Math.max(qos,node.subscriptions[s][r].qos);
                                        _options = node.subscriptions[s][r].options;
                                        node.client.on('message',node.subscriptions[s][r].handler);
                                    }
                                }
                                _options.qos = _options.qos || qos;
                                node.client.subscribe(topic, _options);
                            }
                        }

                        // Send any birth message
                        if (node.birthMessage) {
                            node.publish(node.birthMessage);
                        }
                    });
                    node.client.on("reconnect", function() {
                        setStatusConnecting(node, true);
                    });
                    //Broker Disconnect - V5 event
                    node.client.on("disconnect", function(packet) {
                        //Emitted after receiving disconnect packet from broker. MQTT 5.0 feature.
                        const rc = (packet && packet.properties && packet.reasonCode) || packet.reasonCode;
                        const rs = packet && packet.properties && packet.properties.reasonString || "";
                        const details = {
                            broker: (node.clientid?node.clientid+"@":"")+node.brokerurl,
                            reasonCode: rc,
                            reasonString: rs
                        }
                        node.connected = false;
                        node.log(RED._("mqtt.state.broker-disconnected", details));
                        setStatusDisconnected(node, true);
                    });
                    // Register disconnect handlers
                    node.client.on('close', function () {
                        if (node.connected) {
                            node.connected = false;
                            node.log(RED._("mqtt.state.disconnected",{broker:(node.clientid?node.clientid+"@":"")+node.brokerurl}));
                            setStatusDisconnected(node, true);
                        } else if (node.connecting) {
                            node.log(RED._("mqtt.state.connect-failed",{broker:(node.clientid?node.clientid+"@":"")+node.brokerurl}));
                        }
                    });

                    // Register connect error handler
                    // The client's own reconnect logic will take care of errors
                    node.client.on('error', function (error) {
                    });
                }catch(err) {
                    console.log(err);
                }
            }
        };
        node.disconnect = function (callback) {
            const _callback = function (resetNodeConnectedState) {
                setStatusDisconnected(node, true);
                if(resetNodeConnectedState) {
                    node.closing = true;
                    node.connecting = false;
                    node.connected = false;
                }
                callback && typeof callback == "function" && callback();
            };

            if(node.closing) {
                return _callback(false);
            }
            var endCallBack = function endCallBack() {
            }
            if(node.connected && node.closeMessage) {
                node.publish(node.closeMessage, function (err) {
                    node.client.end(endCallBack);
                    _callback(true);
                });
            } else if(node.connected) {
                node.client.end(endCallBack);
                _callback(true);
            } else {
                _callback(false);
            }
        }
        node.subscriptionIds = {};
        node.subid = 1;
        node.subscribe = function (topic,options,callback,ref) {
            ref = ref||0;
            var qos;
            if(typeof options == "object") {
                qos = options.qos;
            } else {
                qos = options;
                options = {};
            }
            options.qos = qos;
            if (!node.subscriptionIds[topic]) {
                node.subscriptionIds[topic] = node.subid++;
            }
            options.properties = options.properties || {};
            options.properties.subscriptionIdentifier = node.subscriptionIds[topic];

            node.subscriptions[topic] = node.subscriptions[topic]||{};
            var sub = {
                topic:topic,
                qos:qos,
                options:options,
                handler:function(mtopic,mpayload, mpacket) {
                    if(mpacket.properties && options.properties && mpacket.properties.subscriptionIdentifier && options.properties.subscriptionIdentifier && (mpacket.properties.subscriptionIdentifier !== options.properties.subscriptionIdentifier) ) {
                        //do nothing as subscriptionIdentifier does not match
                    } else if (matchTopic(topic,mtopic)) {
                        callback(mtopic,mpayload, mpacket);
                    }
                },
                ref: ref
            };
            node.subscriptions[topic][ref] = sub;
            if (node.connected) {
                node.client.on('message',sub.handler);
                node.client.subscribe(topic, options);
            }
        };

        node.unsubscribe = function (topic, ref, removed) {
            ref = ref||0;
            var sub = node.subscriptions[topic];
            if (sub) {
                if (sub[ref]) {
                    if(node.client) {
                        node.client.removeListener('message',sub[ref].handler);
                    }
                    delete sub[ref];
                }
                //TODO: Review. The `if(removed)` was commented out to always delete and remove subscriptions.
                // if we dont then property changes dont get applied and old subs still trigger
                //if (removed) {
                    if (Object.keys(sub).length === 0) {
                        delete node.subscriptions[topic];
                        delete node.subscriptionIds[topic];
                        if (node.connected) {
                            node.client.unsubscribe(topic);
                        }
                    }
                //}
            }
        };
        node.topicAliases = {};

        node.publish = function (msg,done) {
            if (node.connected) {
                if (msg.payload === null || msg.payload === undefined) {
                    msg.payload = "";
                } else if (!Buffer.isBuffer(msg.payload)) {
                    if (typeof msg.payload === "object") {
                        msg.payload = JSON.stringify(msg.payload);
                    } else if (typeof msg.payload !== "string") {
                        msg.payload = "" + msg.payload;
                    }
                }
                var options = {
                    qos: msg.qos || 0,
                    retain: msg.retain || false
                };
                //https://github.com/mqttjs/MQTT.js/blob/master/README.md#mqttclientpublishtopic-message-options-callback
                if(node.options.protocolVersion == 5) {
                    options.properties = options.properties || {};
                    setStrProp(msg, options.properties, "responseTopic");
                    setBufferProp(msg, options.properties, "correlationData");
                    setStrProp(msg, options.properties, "contentType");
                    setIntProp(msg, options.properties, "messageExpiryInterval", 0);
                    setUserProperties(msg.userProperties, options.properties);
                    setIntProp(msg, options.properties, "topicAlias", 1, node.serverProperties.topicAliasMaximum || 0);
                    setBoolProp(msg, options.properties, "payloadFormatIndicator");
                    //FUTURE setIntProp(msg, options.properties, "subscriptionIdentifier", 1, 268435455);
                    if (options.properties.topicAlias) {
                        if (!node.topicAliases.hasOwnProperty(options.properties.topicAlias) && msg.topic == "") {
                            done("Invalid topicAlias");
                            return
                        }
                        if (node.topicAliases[options.properties.topicAlias] === msg.topic) {
                            msg.topic = ""
                        } else {
                            node.topicAliases[options.properties.topicAlias] = msg.topic
                        }
                    }
                }

                node.client.publish(msg.topic, msg.payload, options, function(err) {
                    done && done(err);
                    return
                });
            }
        };

        node.on('close', function(done) {
            node.closing = true;
            node.disconnect(done);
        });

    }

    RED.nodes.registerType("mqtt-broker",MQTTBrokerNode,{
        credentials: {
            user: {type:"text"},
            password: {type: "password"}
        }
    });
    //#endregion  "Broker node"

    //#region  "MQTTIn node"
    function MQTTInNode(n) {
        RED.nodes.createNode(this,n);
        /**@type {MQTTInNode}*/const node = this;
        /**@type {string}*/node.broker = n.broker;
        /**@type {MQTTBrokerNode}*/node.brokerConn = RED.nodes.getNode(node.broker);

        node.dynamicSubs = {};
        node.isDynamic = n.hasOwnProperty("inputs") && n.inputs == 1
        node.inputs = n.inputs;
        node.topic = n.topic;
        node.qos = parseInt(n.qos);
        node.subscriptionIdentifier = n.subscriptionIdentifier;//https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901117
        node.nl = n.nl;
        node.rap = n.rap;
        node.rh = n.rh;

        const Actions = {
            CONNECT: 'connect',
            DISCONNECT: 'disconnect',
            SUBSCRIBE: 'subscribe',
            UNSUBSCRIBE: 'unsubscribe',
            GETSUBS: 'getSubscriptions',
        };
        const allowableActions = Object.values(Actions);

        if (isNaN(node.qos) || node.qos < 0 || node.qos > 2) {
            node.qos = 2;
        }
        if (!node.isDynamic && !isValidSubscriptionTopic(node.topic)) {
            return node.warn(RED._("mqtt.errors.invalid-topic"));
        }
        node.datatype = n.datatype || "utf8";
        if (node.brokerConn) {
            const v5 = node.brokerConn.options && node.brokerConn.options.protocolVersion == 5;
            setStatusDisconnected(node);
            if (node.topic || node.isDynamic) {
                node.brokerConn.register(node);
                if (!node.isDynamic) {
                    let options = { qos: node.qos };
                    if(v5) {
                        setIntProp(node, options, "rh", 0, 2, 0);
                        if(node.nl === "true" || node.nl === true) options.nl = true;
                        else if(node.nl === "false" || node.nl === false) options.nl = false;
                        if(node.rap === "true" || node.rap === true) options.rap = true;
                        else if(node.rap === "false" || node.rap === false) options.rap = false;
                    }

                    node.brokerConn.subscribe(node.topic,options,function(topic, payload, packet) {
                        subscriptionHandler(node, node.datatype, topic, payload, packet);
                    },node.id);
                }
                if (node.brokerConn.connected) {
                    node.status({fill:"green",shape:"dot",text:"node-red:common.status.connected"});
                }
            }
            else {
                node.error(RED._("mqtt.errors.not-defined"));
            }
            node.on('input', function (msg, send, done) {
                const v5 = node.brokerConn.options && node.brokerConn.options.protocolVersion == 5;
                const action = msg.action;

                if (!allowableActions.includes(action)) {
                    done(new Error(RED._('mqtt.errors.invalid-action-action')));
                    return;
                }

                if (action === Actions.CONNECT) {
                    handleConnectAction(node, msg, done)
                } else if (action === Actions.DISCONNECT) {
                    handleDisconnectAction(node, done)
                } else if (action === Actions.SUBSCRIBE || action === Actions.UNSUBSCRIBE) {
                    const subscriptions = [];
                    let actionData;
                    //coerce msg.topic into an array of strings or objects (for later iteration)
                    if(action === Actions.UNSUBSCRIBE && msg.topic === true) {
                        actionData = Object.values(node.dynamicSubs);
                    } else if (Array.isArray(msg.topic)) {
                        actionData = msg.topic;
                    } else if (typeof msg.topic == 'string' || typeof msg.topic == 'object') {
                        actionData = [msg.topic];
                    } else {
                        done(new Error(RED._('mqtt.errors.invalid-action-badsubscription')));
                        return;
                    }
                    //ensure each subscription is an object with topic etc
                    for (let index = 0; index < actionData.length; index++) {
                        let subscription = actionData[index];
                        if (typeof subscription === 'string') {
                            subscription = { topic: subscription };
                        }
                        if (!subscription.topic || !isValidSubscriptionTopic(subscription.topic)) {
                            done(new Error(RED._('mqtt.errors.invalid-topic')));
                            return;
                        }
                        subscriptions.push(subscription);
                    }
                    if (action === Actions.UNSUBSCRIBE) {
                        subscriptions.forEach(function (sub) {
                            node.brokerConn.unsubscribe(sub.topic, node.id);
                            delete node.dynamicSubs[sub.topic];
                        })
                        //user can access current subscriptions through the complete node is so desired
                        msg.subscriptions = Object.values(node.dynamicSubs);
                        done();
                    } else if (action === Actions.SUBSCRIBE) {
                        subscriptions.forEach(function (sub) {
                            //always unsubscribe before subscribe to prevent multiple subs to same topic
                            if (node.dynamicSubs[sub.topic]) {
                                node.brokerConn.unsubscribe(sub.topic, node.id);
                                delete node.dynamicSubs[sub.topic];
                            }

                            //prepare options. Default qos 2 & rap flag true (same as 'mqtt in' node ui defaults when adding to editor)
                            let options = {}
                            setIntProp(sub, options, 'qos', 0, 2, 2);//default to qos 2 (same as 'mqtt in' default)
                            sub.qos = options.qos;
                            if (v5) {
                                setIntProp(sub, options, 'rh', 0, 2, 0); //default rh to 0:send retained messages (same as 'mqtt in' default)
                                sub.rh = options.rh;
                                setBoolProp(sub, options, 'rap', true); //default rap to true:Keep retain flag of original publish (same as 'mqtt in' default)
                                sub.rap = options.rap;
                                if (sub.nl === 'true' || sub.nl === true) {
                                    options.nl = true;
                                    sub.nl = true;
                                } else if (sub.nl === 'false' || sub.nl === false) {
                                    options.nl = false;
                                    sub.nl = false;
                                } else {
                                    delete sub.nl
                                }
                            }

                            //subscribe to sub.topic & hook up subscriptionHandler
                            node.brokerConn.subscribe(sub.topic, options, function (topic, payload, packet) {
                                subscriptionHandler(node, sub.datatype || node.datatype, topic, payload, packet);
                            }, node.id);
                            node.dynamicSubs[sub.topic] = sub; //save for later unsubscription & 'list' action
                        })
                        //user can access current subscriptions through the complete node is so desired
                        msg.subscriptions = Object.values(node.dynamicSubs);
                        done();
                    }
                } else if (action === Actions.GETSUBS) {
                    //send list of subscriptions in payload
                    msg.topic = "subscriptions";
                    msg.payload = Object.values(node.dynamicSubs);
                    send(msg);
                    done();
                }
            });

            node.on('close', function(removed, done) {
                if (node.brokerConn) {
                    if(node.isDynamic) {
                        Object.keys(node.dynamicSubs).forEach(function (topic) {
                            node.brokerConn.unsubscribe(topic, node.id, removed);
                        });
                        node.dynamicSubs = {};
                    } else {
                        node.brokerConn.unsubscribe(node.topic,node.id, removed);
                    }
                    node.brokerConn.deregister(node, done);
                } else {
                    done();
                }
            });
        } else {
            node.error(RED._("mqtt.errors.missing-config"));
        }
    }
    RED.nodes.registerType("mqtt in",MQTTInNode);
    //#endregion  "MQTTIn node"

    //#region "MQTTOut node"
    function MQTTOutNode(n) {
        RED.nodes.createNode(this,n);
        const node = this;
        node.topic = n.topic;
        node.qos = n.qos || null;
        node.retain = n.retain;
        node.broker = n.broker;
        node.responseTopic = n.respTopic;//https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901114
        node.correlationData = n.correl;//https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901115
        node.contentType = n.contentType;//https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901118
        node.messageExpiryInterval = n.expiry; //https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901112
        try {
            if (/^ *{/.test(n.userProps)) {
                //setup this.userProperties
                setUserProperties(JSON.parse(n.userProps), node);//https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901116
            }
        } catch(err) {}
        // node.topicAlias = n.topicAlias; //https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901113
        // node.payloadFormatIndicator = n.payloadFormatIndicator; //https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901111
        // node.subscriptionIdentifier = n.subscriptionIdentifier;//https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901117

        /** @type {MQTTBrokerNode}*/node.brokerConn = RED.nodes.getNode(node.broker);

        const Actions = {
            CONNECT: 'connect',
            DISCONNECT: 'disconnect',
        };

        if (node.brokerConn) {
            setStatusDisconnected(node);
            node.on("input",function(msg,send,done) {
                if (msg.action) {
                    if (msg.action === Actions.CONNECT) {
                        handleConnectAction(node, msg, done)
                    } else if (msg.action === Actions.DISCONNECT) {
                        handleDisconnectAction(node, done)
                    } else {
                        done(new Error(RED._('mqtt.errors.invalid-action-action')));
                        return;
                    }
                } else {
                    doPublish(node, msg, done);
                }

            });
            if (node.brokerConn.connected) {
                node.status({fill:"green",shape:"dot",text:"node-red:common.status.connected"});
            }
            node.brokerConn.register(node);
            node.on('close', function(done) {
                if (node.brokerConn) {
                    node.brokerConn.deregister(node,done);
                } else {
                    done();
                }
            });
        } else {
            node.error(RED._("mqtt.errors.missing-config"));
        }
    }
    RED.nodes.registerType("mqtt out",MQTTOutNode);
    //#endregion "MQTTOut node"
};
