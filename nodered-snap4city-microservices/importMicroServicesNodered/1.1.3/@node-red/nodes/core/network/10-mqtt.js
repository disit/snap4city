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
    var util = require("util");
    var isUtf8 = require('is-utf8');
    var HttpsProxyAgent = require('https-proxy-agent');
    var url = require('url');

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

    function MQTTBrokerNode(n) {
        RED.nodes.createNode(this,n);

        // Configuration options passed by Node Red
        this.broker = n.broker;
        this.port = n.port;
        this.clientid = n.clientid;
        this.usetls = n.usetls;
        this.usews = n.usews;
        this.verifyservercert = n.verifyservercert;
        this.compatmode = n.compatmode;
        this.keepalive = n.keepalive;
        this.cleansession = n.cleansession;

        // Config node state
        this.brokerurl = "";
        this.connected = false;
        this.connecting = false;
        this.closing = false;
        this.options = {};
        this.queue = [];
        this.subscriptions = {};

        if (n.birthTopic) {
            this.birthMessage = {
                topic: n.birthTopic,
                payload: n.birthPayload || "",
                qos: Number(n.birthQos||0),
                retain: n.birthRetain=="true"|| n.birthRetain===true
            };
        }

        if (n.closeTopic) {
            this.closeMessage = {
                topic: n.closeTopic,
                payload: n.closePayload || "",
                qos: Number(n.closeQos||0),
                retain: n.closeRetain=="true"|| n.closeRetain===true
            };
        }

        if (this.credentials) {
            this.username = this.credentials.user;
            this.password = this.credentials.password;
        }

        // If the config node is missing certain options (it was probably deployed prior to an update to the node code),
        // select/generate sensible options for the new fields
        if (typeof this.usetls === 'undefined') {
            this.usetls = false;
        }
        if (typeof this.usews === 'undefined') {
            this.usews = false;
        }
        if (typeof this.compatmode === 'undefined') {
            this.compatmode = false;
        }
        if (typeof this.verifyservercert === 'undefined') {
            this.verifyservercert = false;
        }
        if (typeof this.keepalive === 'undefined') {
            this.keepalive = 60;
        } else if (typeof this.keepalive === 'string') {
            this.keepalive = Number(this.keepalive);
        }
        if (typeof this.cleansession === 'undefined') {
            this.cleansession = true;
        }

        var prox, noprox;
        if (process.env.http_proxy) { prox = process.env.http_proxy; }
        if (process.env.HTTP_PROXY) { prox = process.env.HTTP_PROXY; }
        if (process.env.no_proxy) { noprox = process.env.no_proxy.split(","); }
        if (process.env.NO_PROXY) { noprox = process.env.NO_PROXY.split(","); }


        // Create the URL to pass in to the MQTT.js library
        if (this.brokerurl === "") {
            // if the broker may be ws:// or wss:// or even tcp://
            if (this.broker.indexOf("://") > -1) {
                this.brokerurl = this.broker;
                // Only for ws or wss, check if proxy env var for additional configuration
                if (this.brokerurl.indexOf("wss://") > -1 || this.brokerurl.indexOf("ws://") > -1 ) {
                    // check if proxy is set in env
                    var noproxy;
                    if (noprox) {
                        for (var i = 0; i < noprox.length; i += 1) {
                            if (this.brokerurl.indexOf(noprox[i].trim()) !== -1) { noproxy=true; }
                        }
                    }
                    if (prox && !noproxy) {
                        var parsedUrl = url.parse(this.brokerurl);
                        var proxyOpts = url.parse(prox);
                        // true for wss
                        proxyOpts.secureEndpoint = parsedUrl.protocol ? parsedUrl.protocol === 'wss:' : true;
                        // Set Agent for wsOption in MQTT
                        var agent = new HttpsProxyAgent(proxyOpts);
                        this.options.wsOptions = {
                            agent: agent
                        }
                    }
                }
            } else {
                // construct the std mqtt:// url
                if (this.usetls) {
                    this.brokerurl="mqtts://";
                } else {
                    this.brokerurl="mqtt://";
                }
                if (this.broker !== "") {
                    //Check for an IPv6 address
                    if (/(?:^|(?<=\s))(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))(?=\s|$)/.test(this.broker)) {
                        this.brokerurl = this.brokerurl+"["+this.broker+"]:";
                    } else {
                        this.brokerurl = this.brokerurl+this.broker+":";
                    }
                    // port now defaults to 1883 if unset.
                    if (!this.port){
                        this.brokerurl = this.brokerurl+"1883";
                    } else {
                        this.brokerurl = this.brokerurl+this.port;
                    }
                } else {
                    this.brokerurl = this.brokerurl+"localhost:1883";
                }
            }
        }

        if (!this.cleansession && !this.clientid) {
            this.cleansession = true;
            this.warn(RED._("mqtt.errors.nonclean-missingclientid"));
        }

        // Build options for passing to the MQTT.js API
        this.options.clientId = this.clientid || 'mqtt_' + (1+Math.random()*4294967295).toString(16);
        this.options.username = this.username;
        this.options.password = this.password;
        this.options.keepalive = this.keepalive;
        this.options.clean = this.cleansession;
        this.options.reconnectPeriod = RED.settings.mqttReconnectTime||5000;
        if (this.compatmode == "true" || this.compatmode === true) {
            this.options.protocolId = 'MQIsdp';
            this.options.protocolVersion = 3;
        }
        if (this.usetls && n.tls) {
            var tlsNode = RED.nodes.getNode(n.tls);
            if (tlsNode) {
                tlsNode.addTLSOptions(this.options);
            }
        }
        // console.log(this.brokerurl,this.options);

        // If there's no rejectUnauthorized already, then this could be an
        // old config where this option was provided on the broker node and
        // not the tls node
        if (typeof this.options.rejectUnauthorized === 'undefined') {
            this.options.rejectUnauthorized = (this.verifyservercert == "true" || this.verifyservercert === true);
        }

        if (n.willTopic) {
            this.options.will = {
                topic: n.willTopic,
                payload: n.willPayload || "",
                qos: Number(n.willQos||0),
                retain: n.willRetain=="true"|| n.willRetain===true
            };
        }

        // Define functions called by MQTT in and out nodes
        var node = this;
        this.users = {};

        this.register = function(mqttNode) {
            node.users[mqttNode.id] = mqttNode;
            if (Object.keys(node.users).length === 1) {
                node.connect();
            }
        };

        this.deregister = function(mqttNode,done) {
            delete node.users[mqttNode.id];
            if (node.closing) {
                return done();
            }
            if (Object.keys(node.users).length === 0) {
                if (node.client && node.client.connected) {
                    return node.client.end(done);
                } else {
                    node.client.end();
                    return done();
                }
            }
            done();
        };

        this.connect = function () {
            if (!node.connected && !node.connecting) {
                node.connecting = true;
                try {
                    node.client = mqtt.connect(node.brokerurl ,node.options);
                    node.client.setMaxListeners(0);
                    // Register successful connect or reconnect handler
                    node.client.on('connect', function () {
                        node.connecting = false;
                        node.connected = true;
                        node.log(RED._("mqtt.state.connected",{broker:(node.clientid?node.clientid+"@":"")+node.brokerurl}));
                        for (var id in node.users) {
                            if (node.users.hasOwnProperty(id)) {
                                node.users[id].status({fill:"green",shape:"dot",text:"node-red:common.status.connected"});
                            }
                        }
                        // Remove any existing listeners before resubscribing to avoid duplicates in the event of a re-connection
                        node.client.removeAllListeners('message');

                        // Re-subscribe to stored topics
                        for (var s in node.subscriptions) {
                            if (node.subscriptions.hasOwnProperty(s)) {
                                var topic = s;
                                var qos = 0;
                                for (var r in node.subscriptions[s]) {
                                    if (node.subscriptions[s].hasOwnProperty(r)) {
                                        qos = Math.max(qos,node.subscriptions[s][r].qos);
                                        node.client.on('message',node.subscriptions[s][r].handler);
                                    }
                                }
                                var options = {qos: qos};
                                node.client.subscribe(topic, options);
                            }
                        }

                        // Send any birth message
                        if (node.birthMessage) {
                            node.publish(node.birthMessage);
                        }
                    });
                    node.client.on("reconnect", function() {
                        for (var id in node.users) {
                            if (node.users.hasOwnProperty(id)) {
                                node.users[id].status({fill:"yellow",shape:"ring",text:"node-red:common.status.connecting"});
                            }
                        }
                    })
                    // Register disconnect handlers
                    node.client.on('close', function () {
                        if (node.connected) {
                            node.connected = false;
                            node.log(RED._("mqtt.state.disconnected",{broker:(node.clientid?node.clientid+"@":"")+node.brokerurl}));
                            for (var id in node.users) {
                                if (node.users.hasOwnProperty(id)) {
                                    node.users[id].status({fill:"red",shape:"ring",text:"node-red:common.status.disconnected"});
                                }
                            }
                        } else if (node.connecting) {
                            node.log(RED._("mqtt.state.connect-failed",{broker:(node.clientid?node.clientid+"@":"")+node.brokerurl}));
                        }
                    });

                    // Register connect error handler
                    // The client's own reconnect logic will take care of errors
                    node.client.on('error', function (error) {});
                }catch(err) {
                    console.log(err);
                }
            }
        };

        this.subscribe = function (topic,qos,callback,ref) {
            ref = ref||0;
            node.subscriptions[topic] = node.subscriptions[topic]||{};
            var sub = {
                topic:topic,
                qos:qos,
                handler:function(mtopic,mpayload, mpacket) {
                    if (matchTopic(topic,mtopic)) {
                        callback(mtopic,mpayload, mpacket);
                    }
                },
                ref: ref
            };
            node.subscriptions[topic][ref] = sub;
            if (node.connected) {
                node.client.on('message',sub.handler);
                var options = {};
                options.qos = qos;
                node.client.subscribe(topic, options);
            }
        };

        this.unsubscribe = function (topic, ref, removed) {
            ref = ref||0;
            var sub = node.subscriptions[topic];
            if (sub) {
                if (sub[ref]) {
                    node.client.removeListener('message',sub[ref].handler);
                    delete sub[ref];
                }
                if (removed) {
                    if (Object.keys(sub).length === 0) {
                        delete node.subscriptions[topic];
                        if (node.connected) {
                            node.client.unsubscribe(topic);
                        }
                    }
                }
            }
        };

        this.publish = function (msg,done) {
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
                node.client.publish(msg.topic, msg.payload, options, function(err) {
                    done && done();
                    return
                });
            }
        };

        this.on('close', function(done) {
            this.closing = true;
            if (this.connected) {
                // Send close message
                if (node.closeMessage) {
                    node.publish(node.closeMessage);
                }
                this.client.once('close', function() {
                    done();
                });
                this.client.end();
            } else if (this.connecting || node.client.reconnecting) {
                node.client.end();
                done();
            } else {
                done();
            }
        });

    }

    RED.nodes.registerType("mqtt-broker",MQTTBrokerNode,{
        credentials: {
            user: {type:"text"},
            password: {type: "password"}
        }
    });

    function MQTTInNode(n) {
        RED.nodes.createNode(this,n);
        this.topic = n.topic;
        this.qos = parseInt(n.qos);
        if (isNaN(this.qos) || this.qos < 0 || this.qos > 2) {
            this.qos = 2;
        }
        this.broker = n.broker;
        this.brokerConn = RED.nodes.getNode(this.broker);
        if (!/^(#$|(\+|[^+#]*)(\/(\+|[^+#]*))*(\/(\+|#|[^+#]*))?$)/.test(this.topic)) {
            return this.warn(RED._("mqtt.errors.invalid-topic"));
        }
        this.datatype = n.datatype || "utf8";
        var node = this;
        if (this.brokerConn) {
            this.status({fill:"red",shape:"ring",text:"node-red:common.status.disconnected"});
            if (this.topic) {
                node.brokerConn.register(this);
                this.brokerConn.subscribe(this.topic,this.qos,function(topic,payload,packet) {
                    if (node.datatype === "buffer") {
                        // payload = payload;
                    } else if (node.datatype === "base64") {
                        payload = payload.toString('base64');
                    } else if (node.datatype === "utf8") {
                        payload = payload.toString('utf8');
                    } else if (node.datatype === "json") {
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
                    if ((node.brokerConn.broker === "localhost")||(node.brokerConn.broker === "127.0.0.1")) {
                        msg._topic = topic;
                    }
                    node.send(msg);
                }, this.id);
                if (this.brokerConn.connected) {
                    node.status({fill:"green",shape:"dot",text:"node-red:common.status.connected"});
                }
            }
            else {
                this.error(RED._("mqtt.errors.not-defined"));
            }
            this.on('close', function(removed, done) {
                if (node.brokerConn) {
                    node.brokerConn.unsubscribe(node.topic,node.id, removed);
                    node.brokerConn.deregister(node,done);
                }
            });
        } else {
            this.error(RED._("mqtt.errors.missing-config"));
        }
    }
    RED.nodes.registerType("mqtt in",MQTTInNode);

    function MQTTOutNode(n) {
        RED.nodes.createNode(this,n);
        this.topic = n.topic;
        this.qos = n.qos || null;
        this.retain = n.retain;
        this.broker = n.broker;
        this.brokerConn = RED.nodes.getNode(this.broker);
        var node = this;
        var chk = /[\+#]/;

        if (this.brokerConn) {
            this.status({fill:"red",shape:"ring",text:"node-red:common.status.disconnected"});
            this.on("input",function(msg,send,done) {
                if (msg.qos) {
                    msg.qos = parseInt(msg.qos);
                    if ((msg.qos !== 0) && (msg.qos !== 1) && (msg.qos !== 2)) {
                        msg.qos = null;
                    }
                }
                msg.qos = Number(node.qos || msg.qos || 0);
                msg.retain = node.retain || msg.retain || false;
                msg.retain = ((msg.retain === true) || (msg.retain === "true")) || false;
                if (node.topic) {
                    msg.topic = node.topic;
                }
                if ( msg.hasOwnProperty("payload")) {
                    if (msg.hasOwnProperty("topic") && (typeof msg.topic === "string") && (msg.topic !== "")) { // topic must exist
                        if (chk.test(msg.topic)) { node.warn(RED._("mqtt.errors.invalid-topic")); }
                        this.brokerConn.publish(msg, done);  // send the message
                    } else {
                        node.warn(RED._("mqtt.errors.invalid-topic"));
                        done();
                    }
                } else {
                    done();
                }
            });
            if (this.brokerConn.connected) {
                node.status({fill:"green",shape:"dot",text:"node-red:common.status.connected"});
            }
            node.brokerConn.register(node);
            this.on('close', function(done) {
                node.brokerConn.deregister(node,done);
            });
        } else {
            this.error(RED._("mqtt.errors.missing-config"));
        }
    }
    RED.nodes.registerType("mqtt out",MQTTOutNode);
};
