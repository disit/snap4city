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
    let reconnectTime = RED.settings.socketReconnectTime || 10000;
    let socketTimeout = RED.settings.socketTimeout || null;
    const msgQueueSize = RED.settings.tcpMsgQueueSize || 1000;
    const Denque = require('denque');
    const net = require('net');
    const tls = require('tls');

    let connectionPool = {};

    function normalizeConnectArgs(listArgs) {
      const args = net._normalizeArgs(listArgs);
      const options = args[0];
      const cb = args[1];

      // If args[0] was options, then normalize dealt with it.
      // If args[0] is port, or args[0], args[1] is host, port, we need to
      // find the options and merge them in, normalize's options has only
      // the host/port/path args that it knows about, not the tls options.
      // This means that options.host overrides a host arg.
      if (listArgs[1] !== null && typeof listArgs[1] === 'object') {
        ObjectAssign(options, listArgs[1]);
      } else if (listArgs[2] !== null && typeof listArgs[2] === 'object') {
        ObjectAssign(options, listArgs[2]);
      }

      return cb ? [options, cb] : [options];
    }

    function getAllowUnauthorized() {
      const allowUnauthorized = process.env.NODE_TLS_REJECT_UNAUTHORIZED === '0';

      if (allowUnauthorized) {
        process.emitWarning(
          'Setting the NODE_TLS_REJECT_UNAUTHORIZED ' +
          'environment variable to \'0\' makes TLS connections ' +
          'and HTTPS requests insecure by disabling ' +
          'certificate verification.');
      }
      return allowUnauthorized;
    }

    /**
     * Enqueue `item` in `queue`
     * @param {Denque} queue - Queue
     * @param {*} item - Item to enqueue
     * @private
     * @returns {Denque} `queue`
     */
    const enqueue = (queue, item) => {
        // drop msgs from front of queue if size is going to be exceeded
        if (queue.length === msgQueueSize) { queue.shift(); }
        queue.push(item);
        return queue;
    };

    /**
     * Shifts item off front of queue
     * @param {Deque} queue - Queue
     * @private
     * @returns {*} Item previously at front of queue
     */
    const dequeue = queue => queue.shift();

    function TcpIn(n) {
        RED.nodes.createNode(this,n);
        this.host = n.host;
        this.port = n.port * 1;
        this.topic = n.topic;
        this.stream = (!n.datamode||n.datamode=='stream'); /* stream,single*/
        this.datatype = n.datatype||'buffer'; /* buffer,utf8,base64 */
        this.newline = (n.newline||"").replace("\\n","\n").replace("\\r","\r").replace("\\t","\t");
        this.base64 = n.base64;
        this.server = (typeof n.server == 'boolean')?n.server:(n.server == "server");
        this.closing = false;
        this.connected = false;
        var node = this;
        var count = 0;
        if (n.tls) { var tlsNode = RED.nodes.getNode(n.tls); }

        if (!node.server) {
            var buffer = null;
            var client;
            var reconnectTimeout;
            var end = false;
            var setupTcpClient = function() {
                node.log(RED._("tcpin.status.connecting",{host:node.host,port:node.port}));
                node.status({fill:"grey",shape:"dot",text:"common.status.connecting"});
                var id = RED.util.generateId();
                var connOpts = {host: node.host};
                if (n.tls) {
                    var connOpts = tlsNode.addTLSOptions({host: node.host});
                    client = tls.connect(node.port, connOpts, function() {
                        buffer = (node.datatype == 'buffer') ? Buffer.alloc(0) : "";
                        node.connected = true;
                        node.log(RED._("status.connected", {host: node.host, port: node.port}));
                        node.status({fill:"green",shape:"dot",text:"common.status.connected",_session:{type:"tcp",id:id}});
                    });
                }
                else {
                    client = net.connect(node.port, node.host, function() {
                        buffer = (node.datatype == 'buffer') ? Buffer.alloc(0) : "";
                        node.connected = true;
                        node.log(RED._("tcpin.status.connected",{host:node.host,port:node.port}));
                        node.status({fill:"green",shape:"dot",text:"common.status.connected",_session:{type:"tcp",id:id}});
                    });
                }
                client.setKeepAlive(true, 120000);
                connectionPool[id] = client;

                client.on('data', function (data) {
                    if (node.datatype != 'buffer') {
                        data = data.toString(node.datatype);
                    }
                    if (node.stream) {
                        var msg;
                        if ((node.datatype) === "utf8" && node.newline !== "") {
                            buffer = buffer+data;
                            var parts = buffer.split(node.newline);
                            for (var i = 0; i<parts.length-1; i+=1) {
                                msg = {topic:node.topic, payload:parts[i] + node.newline.trimEnd()};
                                msg._session = {type:"tcp",id:id};
                                node.send(msg);
                            }
                            buffer = parts[parts.length-1];
                        } else {
                            msg = {topic:node.topic, payload:data};
                            msg._session = {type:"tcp",id:id};
                            node.send(msg);
                        }
                    } else {
                        if ((typeof data) === "string") {
                            buffer = buffer+data;
                        } else {
                            buffer = Buffer.concat([buffer,data],buffer.length+data.length);
                        }
                    }
                });
                client.on('end', function() {
                    if (!node.stream || (node.datatype == "utf8" && node.newline !== "" && buffer.length > 0)) {
                        var msg = {topic:node.topic, payload:buffer};
                        msg._session = {type:"tcp",id:id};
                        if (buffer.length !== 0) {
                            end = true; // only ask for fast re-connect if we actually got something
                            node.send(msg);
                        }
                        buffer = null;
                    }
                });
                client.on('close', function() {
                    delete connectionPool[id];
                    node.connected = false;
                    node.status({fill:"red",shape:"ring",text:"common.status.disconnected",_session:{type:"tcp",id:id}});
                    if (!node.closing) {
                        if (end) { // if we were asked to close then try to reconnect once very quick.
                            end = false;
                            reconnectTimeout = setTimeout(setupTcpClient, 20);
                        }
                        else {
                            node.log(RED._("tcpin.errors.connection-lost",{host:node.host,port:node.port}));
                            reconnectTimeout = setTimeout(setupTcpClient, reconnectTime);
                        }
                    } else {
                        if (node.doneClose) { node.doneClose(); }
                    }
                });
                client.on('error', function(err) {
                    node.log(err);
                });
            }
            setupTcpClient();

            this.on('close', function(done) {
                node.doneClose = done;
                this.closing = true;
                if (client) { client.destroy(); }
                clearTimeout(reconnectTimeout);
                if (!node.connected) { done(); }
            });
        }
        else {
            let srv = net;
            let connOpts;
            if (n.tls) {
                srv = tls;
                connOpts = tlsNode.addTLSOptions({});
            }
            var server = srv.createServer(connOpts, function (socket) {
                socket.setKeepAlive(true,120000);
                if (socketTimeout !== null) { socket.setTimeout(socketTimeout); }
                var id = RED.util.generateId();
                var fromi;
                var fromp;
                connectionPool[id] = socket;
                count++;
                node.status({
                    text:RED._("tcpin.status.connections",{count:count}),
                    event:"connect",
                    ip:socket.remoteAddress,
                    port:socket.remotePort,
                    _session: {type:"tcp",id:id}
                });

                var buffer = (node.datatype == 'buffer') ? Buffer.alloc(0) : "";
                socket.on('data', function (data) {
                    if (node.datatype != 'buffer') {
                        data = data.toString(node.datatype);
                    }
                    if (node.stream) {
                        var msg;
                        if ((typeof data) === "string" && node.newline !== "") {
                            buffer = buffer+data;
                            var parts = buffer.split(node.newline);
                            for (var i = 0; i<parts.length-1; i+=1) {
                                msg = {topic:node.topic, payload:parts[i] + node.newline.trimEnd(), ip:socket.remoteAddress, port:socket.remotePort};
                                msg._session = {type:"tcp",id:id};
                                node.send(msg);
                            }
                            buffer = parts[parts.length-1];
                        } else {
                            msg = {topic:node.topic, payload:data, ip:socket.remoteAddress, port:socket.remotePort};
                            msg._session = {type:"tcp",id:id};
                            node.send(msg);
                        }
                    }
                    else {
                        if ((typeof data) === "string") {
                            buffer = buffer+data;
                        } else {
                            buffer = Buffer.concat([buffer,data],buffer.length+data.length);
                        }
                        fromi = socket.remoteAddress;
                        fromp = socket.remotePort;
                    }
                });
                socket.on('end', function() {
                    if (!node.stream || (node.datatype === "utf8" && node.newline !== "") || (node.datatype === "base64")) {
                        if (buffer.length > 0) {
                            var msg = {topic:node.topic, payload:buffer, ip:fromi, port:fromp};
                            msg._session = {type:"tcp",id:id};
                            node.send(msg);
                        }
                        buffer = null;
                    }
                });
                socket.on('timeout', function() {
                    node.log(RED._("tcpin.errors.timeout",{port:node.port}));
                    socket.end();
                });
                socket.on('close', function() {
                    delete connectionPool[id];
                    count--;
                    node.status({
                        text:RED._("tcpin.status.connections",{count:count}),
                        event:"disconnect",
                        ip:socket.remoteAddress,
                        port:socket.remotePort,
                        _session: {type:"tcp",id:id}

                    });
                });
                socket.on('error',function(err) {
                    node.log(err);
                });
            });

            server.on('error', function(err) {
                if (err) {
                    node.error(RED._("tcpin.errors.cannot-listen",{port:node.port,error:err.toString()}));
                }
            });

            server.listen(node.port, function(err) {
                if (err) {
                    node.error(RED._("tcpin.errors.cannot-listen",{port:node.port,error:err.toString()}));
                } else {
                    node.log(RED._("tcpin.status.listening-port",{port:node.port}));
                    node.on('close', function() {
                        for (var c in connectionPool) {
                            if (connectionPool.hasOwnProperty(c)) {
                                connectionPool[c].end();
                                connectionPool[c].unref();
                            }
                        }
                        node.closing = true;
                        server.close();
                        node.log(RED._("tcpin.status.stopped-listening",{port:node.port}));
                    });
                }
            });
        }
    }
    RED.nodes.registerType("tcp in",TcpIn);


    function TcpOut(n) {
        RED.nodes.createNode(this,n);
        this.host = n.host;
        this.port = n.port * 1;
        this.base64 = n.base64;
        this.doend = n.end || false;
        this.beserver = n.beserver;
        this.name = n.name;
        this.closing = false;
        this.connected = false;
        var node = this;
        if (n.tls) { var tlsNode = RED.nodes.getNode(n.tls); }

        if (!node.beserver || node.beserver == "client") {
            var reconnectTimeout;
            var client = null;
            var end = false;

            var setupTcpClient = function() {
                node.log(RED._("tcpin.status.connecting",{host:node.host,port:node.port}));
                node.status({fill:"grey",shape:"dot",text:"common.status.connecting"});
                if (n.tls) {
                    // connOpts = tlsNode.addTLSOptions(connOpts);
                    // client = tls.connect(connOpts, function() {
                    var connOpts = tlsNode.addTLSOptions({host: node.host});
                    client = tls.connect(node.port, connOpts, function() {
                        // buffer = (node.datatype == 'buffer') ? Buffer.alloc(0) : "";
                        node.connected = true;
                        node.log(RED._("status.connected", {host: node.host, port: node.port}));
                        node.status({fill:"green",shape:"dot",text:"common.status.connected"});
                    });
                }
                else {
                    client = net.connect(node.port, node.host, function() {
                        node.connected = true;
                        node.log(RED._("tcpin.status.connected",{host:node.host,port:node.port}));
                        node.status({fill:"green",shape:"dot",text:"common.status.connected"});
                    });
                }
                client.setKeepAlive(true,120000);
                client.on('error', function (err) {
                    node.log(RED._("tcpin.errors.error",{error:err.toString()}));
                });
                client.on('end', function (err) {
                    node.status({});
                    node.connected = false;
                });
                client.on('close', function() {
                    node.status({fill:"red",shape:"ring",text:"common.status.disconnected"});
                    node.connected = false;
                    client.destroy();
                    if (!node.closing) {
                        if (end) {
                            end = false;
                            reconnectTimeout = setTimeout(setupTcpClient,20);
                        }
                        else {
                            node.log(RED._("tcpin.errors.connection-lost",{host:node.host,port:node.port}));
                            reconnectTimeout = setTimeout(setupTcpClient,reconnectTime);
                        }
                    } else {
                        if (node.doneClose) { node.doneClose(); }
                    }
                });
            }
            setupTcpClient();

            node.on("input", function(msg, nodeSend, nodeDone) {
                if (node.connected && msg.payload != null) {
                    if (Buffer.isBuffer(msg.payload)) {
                        client.write(msg.payload);
                    } else if (typeof msg.payload === "string" && node.base64) {
                        client.write(Buffer.from(msg.payload,'base64'));
                    } else {
                        client.write(Buffer.from(""+msg.payload));
                    }
                    if (node.doend === true) {
                        end = true;
                        if (client) { node.status({}); client.destroy(); }
                    }
                }
                nodeDone();
            });

            node.on("close", function(done) {
                node.doneClose = done;
                this.closing = true;
                if (client) { client.destroy(); }
                clearTimeout(reconnectTimeout);
                if (!node.connected) { done(); }
            });

        }
        else if (node.beserver == "reply") {
            node.on("input",function(msg, nodeSend, nodeDone) {
                if (msg._session && msg._session.type == "tcp") {
                    var client = connectionPool[msg._session.id];
                    if (client) {
                        if (Buffer.isBuffer(msg.payload)) {
                            client.write(msg.payload);
                        } else if (typeof msg.payload === "string" && node.base64) {
                            client.write(Buffer.from(msg.payload,'base64'));
                        } else {
                            client.write(Buffer.from(""+msg.payload));
                        }
                    }
                }
                else {
                    for (var i in connectionPool) {
                        if (Buffer.isBuffer(msg.payload)) {
                            connectionPool[i].write(msg.payload);
                        } else if (typeof msg.payload === "string" && node.base64) {
                            connectionPool[i].write(Buffer.from(msg.payload,'base64'));
                        } else {
                            connectionPool[i].write(Buffer.from(""+msg.payload));
                        }
                    }
                }
                nodeDone();
            });
        }
        else {
            var connectedSockets = [];
            node.status({text:RED._("tcpin.status.connections",{count:0})});
            let srv = net;
            let connOpts;
            if (n.tls) {
                srv = tls;
                connOpts = tlsNode.addTLSOptions({});
            }
            var server = srv.createServer(connOpts, function (socket) {
                socket.setKeepAlive(true,120000);
                if (socketTimeout !== null) { socket.setTimeout(socketTimeout); }
                node.log(RED._("tcpin.status.connection-from",{host:socket.remoteAddress, port:socket.remotePort}));
                socket.on('timeout', function() {
                    node.log(RED._("tcpin.errors.timeout",{port:node.port}));
                    socket.end();
                });
                socket.on('data', function(d) {
                    // console.log("DATA",d)
                });
                socket.on('close',function() {
                    node.log(RED._("tcpin.status.connection-closed",{host:socket.remoteAddress, port:socket.remotePort}));
                    connectedSockets.splice(connectedSockets.indexOf(socket),1);
                    node.status({text:RED._("tcpin.status.connections",{count:connectedSockets.length})});
                });
                socket.on('error',function() {
                    node.log(RED._("tcpin.errors.socket-error",{host:socket.remoteAddress, port:socket.remotePort}));
                    connectedSockets.splice(connectedSockets.indexOf(socket),1);
                    node.status({text:RED._("tcpin.status.connections",{count:connectedSockets.length})});
                });
                connectedSockets.push(socket);
                node.status({text:RED._("tcpin.status.connections",{count:connectedSockets.length})});
            });

            node.on("input", function(msg, nodeSend, nodeDone) {
                if (msg.payload != null) {
                    var buffer;
                    if (Buffer.isBuffer(msg.payload)) {
                        buffer = msg.payload;
                    } else if (typeof msg.payload === "string" && node.base64) {
                        buffer = Buffer.from(msg.payload,'base64');
                    } else {
                        buffer = Buffer.from(""+msg.payload);
                    }
                    for (var i = 0; i < connectedSockets.length; i += 1) {
                        if (node.doend === true) { connectedSockets[i].end(buffer); }
                        else { connectedSockets[i].write(buffer); }
                    }
                }
                nodeDone();
            });

            server.on('error', function(err) {
                if (err) {
                    node.error(RED._("tcpin.errors.cannot-listen",{port:node.port,error:err.toString()}));
                }
            });

            server.listen(node.port, function(err) {
                if (err) {
                    node.error(RED._("tcpin.errors.cannot-listen",{port:node.port,error:err.toString()}));
                } else {
                    node.log(RED._("tcpin.status.listening-port",{port:node.port}));
                    node.on('close', function() {
                        for (var c in connectedSockets) {
                            if (connectedSockets.hasOwnProperty(c)) {
                                connectedSockets[c].end();
                                connectedSockets[c].unref();
                            }
                        }
                        server.close();
                        node.log(RED._("tcpin.status.stopped-listening",{port:node.port}));
                    });
                }
            });
        }
    }
    RED.nodes.registerType("tcp out",TcpOut);


    function TcpGet(n) {
        RED.nodes.createNode(this,n);
        this.server = n.server;
        this.port = Number(n.port);
        this.out = n.out;
        this.ret = n.ret || "buffer";
        this.newline = (n.newline||"").replace("\\n","\n").replace("\\r","\r").replace("\\t","\t");
        this.splitc = n.splitc;
        if (n.tls) {
            var tlsNode = RED.nodes.getNode(n.tls);
        }

        if (this.out === "immed") { this.splitc = -1; this.out = "time"; }
        if (this.out !== "char") { this.splitc = Number(this.splitc); }
        else {
            if (this.splitc[0] == '\\') {
                this.splitc = parseInt(this.splitc.replace("\\n",0x0A).replace("\\r",0x0D).replace("\\t",0x09).replace("\\e",0x1B).replace("\\f",0x0C).replace("\\0",0x00));
            } // jshint ignore:line
            if (typeof this.splitc == "string") {
                if (this.splitc.substr(0,2) == "0x") {
                    this.splitc = parseInt(this.splitc);
                }
                else {
                    this.splitc = this.splitc.charCodeAt(0);
                }
            } // jshint ignore:line
        }

        var node = this;

        var clients = {};

        this.on("input", function(msg, nodeSend, nodeDone) {
            var i = 0;
            if ((!Buffer.isBuffer(msg.payload)) && (typeof msg.payload !== "string")) {
                msg.payload = msg.payload.toString();
            }

            var host = node.server || msg.host;
            var port = node.port || msg.port;

            // Store client information independently
            // the clients object will have:
            // clients[id].client, clients[id].msg, clients[id].timeout
            var connection_id = host + ":" + port;
            if (connection_id !== node.last_id) {
                node.status({});
                node.last_id = connection_id;
            }
            clients[connection_id] = clients[connection_id] || {
                msgQueue: new Denque(),
                connected: false,
                connecting: false
            };
            enqueue(clients[connection_id].msgQueue, {msg:msg, nodeSend:nodeSend, nodeDone:nodeDone});
            clients[connection_id].lastMsg = msg;

            if (!clients[connection_id].connecting && !clients[connection_id].connected) {
                var buf;
                if (this.out == "count") {
                    if (this.splitc === 0) { buf = Buffer.alloc(1); }
                    else { buf = Buffer.alloc(this.splitc); }
                }
                else { buf = Buffer.alloc(65536); } // set it to 64k... hopefully big enough for most TCP packets.... but only hopefully

                var connOpts = {host:host, port:port};
                if (n.tls) {
                    connOpts = tlsNode.addTLSOptions(connOpts);
                    const allowUnauthorized = getAllowUnauthorized();

                    let options = {
                        rejectUnauthorized: !allowUnauthorized,
                        ciphers: tls.DEFAULT_CIPHERS,
                        checkServerIdentity: tls.checkServerIdentity,
                        minDHSize: 1024,
                        ...connOpts
                    };

                    if (!options.keepAlive) { options.singleUse = true; }

                    const context = options.secureContext || tls.createSecureContext(options);

                    clients[connection_id].client = new tls.TLSSocket(options.socket, {
                        allowHalfOpen: options.allowHalfOpen,
                        pipe: !!options.path,
                        secureContext: context,
                        isServer: false,
                        requestCert: false, // true,
                        rejectUnauthorized: false, // options.rejectUnauthorized !== false,
                        session: options.session,
                        ALPNProtocols: options.ALPNProtocols,
                        requestOCSP: options.requestOCSP,
                        enableTrace: options.enableTrace,
                        pskCallback: options.pskCallback,
                        highWaterMark: options.highWaterMark,
                        onread: options.onread,
                        signal: options.signal,
                    });
                }
                else {
                    clients[connection_id].client = net.Socket();
                }
                if (socketTimeout !== null) { clients[connection_id].client.setTimeout(socketTimeout);}

                if (host && port) {
                    clients[connection_id].connecting = true;
                    clients[connection_id].client.connect(connOpts, function() {
                        //node.log(RED._("tcpin.errors.client-connected"));
                        node.status({fill:"green",shape:"dot",text:"common.status.connected"});
                        if (clients[connection_id] && clients[connection_id].client) {
                            clients[connection_id].connected = true;
                            clients[connection_id].connecting = false;
                            let event;
                            while (event = dequeue(clients[connection_id].msgQueue)) {
                                clients[connection_id].client.write(event.msg.payload);
                                event.nodeDone();
                            }
                            if (node.out === "time" && node.splitc < 0) {
                                clients[connection_id].connected = clients[connection_id].connecting = false;
                                clients[connection_id].client.end();
                                delete clients[connection_id];
                                node.status({});
                            }
                        }
                    });
                }
                else {
                    node.warn(RED._("tcpin.errors.no-host"));
                }
                var chunk = "";
                clients[connection_id].client.on('data', function(data) {
                    if (node.out === "sit") { // if we are staying connected just send the buffer
                        if (clients[connection_id]) {
                            const msg = clients[connection_id].lastMsg || {};
                            msg.payload = RED.util.cloneMessage(data);
                            if (node.ret === "string") {
                                try {
                                    if (node.newline && node.newline !== "" ) {
                                        chunk += msg.payload.toString();
                                        let parts = chunk.split(node.newline);
                                        for (var p=0; p<parts.length-1; p+=1) {
                                            let m = RED.util.cloneMessage(msg);
                                            m.payload = parts[p] + node.newline.trimEnd();
                                            nodeSend(m);
                                        }
                                        chunk = parts[parts.length-1];
                                    }
                                    else {
                                        msg.payload = msg.payload.toString();
                                        nodeSend(msg);
                                    }
                                }
                                catch(e) { node.error(RED._("tcpin.errors.bad-string"), msg); }
                            }
                            else { nodeSend(msg); }
                        }
                    }
                    // else if (node.splitc === 0) {
                    //     clients[connection_id].msg.payload = data;
                    //     node.send(clients[connection_id].msg);
                    // }
                    else {
                        for (var j = 0; j < data.length; j++ ) {
                            if (node.out === "time") {
                                if (clients[connection_id]) {
                                    // do the timer thing
                                    if (clients[connection_id].timeout) {
                                        i += 1;
                                        buf[i] = data[j];
                                    }
                                    else {
                                        clients[connection_id].timeout = setTimeout(function () {
                                            if (clients[connection_id]) {
                                                clients[connection_id].timeout = null;
                                                const msg = clients[connection_id].lastMsg || {};
                                                msg.payload = Buffer.alloc(i+1);
                                                buf.copy(msg.payload,0,0,i+1);
                                                if (node.ret === "string") {
                                                    try { msg.payload = msg.payload.toString(); }
                                                    catch(e) { node.error("Failed to create string", msg); }
                                                }
                                                nodeSend(msg);
                                                if (clients[connection_id].client) {
                                                    node.status({});
                                                    clients[connection_id].client.destroy();
                                                    delete clients[connection_id];
                                                }
                                            }
                                        }, node.splitc);
                                        i = 0;
                                        buf[0] = data[j];
                                    }
                                }
                            }
                            // count bytes into a buffer...
                            else if (node.out == "count") {
                                buf[i] = data[j];
                                i += 1;
                                if ( i >= node.splitc) {
                                    if (clients[connection_id]) {
                                        const msg = clients[connection_id].lastMsg || {};
                                        msg.payload = Buffer.alloc(i);
                                        buf.copy(msg.payload,0,0,i);
                                        if (node.ret === "string") {
                                            try { msg.payload = msg.payload.toString(); }
                                            catch(e) { node.error("Failed to create string", msg); }
                                        }
                                        nodeSend(msg);
                                        if (clients[connection_id].client) {
                                            node.status({});
                                            clients[connection_id].client.destroy();
                                            delete clients[connection_id];
                                        }
                                        i = 0;
                                    }
                                }
                            }
                            // look for a char
                            else {
                                buf[i] = data[j];
                                i += 1;
                                if (data[j] == node.splitc) {
                                    if (clients[connection_id]) {
                                        const msg = clients[connection_id].lastMsg || {};
                                        msg.payload = Buffer.alloc(i);
                                        buf.copy(msg.payload,0,0,i);
                                        if (node.ret === "string") {
                                            try { msg.payload = msg.payload.toString(); }
                                            catch(e) { node.error("Failed to create string", msg); }
                                        }
                                        nodeSend(msg);
                                        if (clients[connection_id].client) {
                                            node.status({});
                                            clients[connection_id].client.destroy();
                                            delete clients[connection_id];
                                        }
                                        i = 0;
                                    }
                                }
                            }
                        }
                    }
                });

                clients[connection_id].client.on('end', function() {
                    //console.log("END");
                    node.status({fill:"grey",shape:"ring",text:"common.status.disconnected"});
                    if (clients[connection_id] && clients[connection_id].client) {
                        clients[connection_id].connected = clients[connection_id].connecting = false;
                        clients[connection_id].client = null;
                    }
                });

                clients[connection_id].client.on('close', function() {
                    //console.log("CLOSE");
                    if (clients[connection_id]) {
                        clients[connection_id].connected = clients[connection_id].connecting = false;
                    }

                    var anyConnected = false;

                    for (var client in clients) {
                        if (clients[client].connected) {
                            anyConnected = true;
                            break;
                        }
                    }
                    if (node.doneClose && !anyConnected) {
                        clients = {};
                        node.doneClose();
                    }
                });

                clients[connection_id].client.on('error', function() {
                    //console.log("ERROR");
                    node.status({fill:"red",shape:"ring",text:"common.status.error"});
                    node.error(RED._("tcpin.errors.connect-fail") + " " + connection_id, msg);
                    if (clients[connection_id] && clients[connection_id].client) {
                        clients[connection_id].client.destroy();
                        delete clients[connection_id];
                    }
                });

                clients[connection_id].client.on('timeout',function() {
                    //console.log("TIMEOUT");
                    if (clients[connection_id]) {
                        clients[connection_id].connected = clients[connection_id].connecting = false;
                        node.status({fill:"grey",shape:"dot",text:"tcpin.errors.connect-timeout"});
                        //node.warn(RED._("tcpin.errors.connect-timeout"));
                        if (clients[connection_id].client) {
                            clients[connection_id].connecting = true;

                            var connOpts = {host:host, port:port};
                            if (n.tls) {
                                connOpts = tlsNode.addTLSOptions(connOpts);
                            }

                            clients[connection_id].client.connect(connOpts, function() {
                                clients[connection_id].connected = true;
                                clients[connection_id].connecting = false;
                                node.status({fill:"green",shape:"dot",text:"common.status.connected"});
                            });
                        }
                    }
                });
            }
            else if (!clients[connection_id].connecting && clients[connection_id].connected) {
                if (clients[connection_id] && clients[connection_id].client) {
                    let event = dequeue(clients[connection_id].msgQueue)
                    clients[connection_id].client.write(event.msg.payload);
                    event.nodeDone();
                }
            }
        });

        this.on("close", function(done) {
            node.doneClose = done;
            for (var cl in clients) {
                if (clients[cl].hasOwnProperty("client")) {
                    clients[cl].client.destroy();
                }
            }
            node.status({});

            // this is probably not necessary and may be removed
            var anyConnected = false;
            for (var c in clients) {
                if (clients[c].connected) {
                    anyConnected = true;
                    break;
                }
            }
            if (!anyConnected) { clients = {}; }
            done();
        });

    }
    RED.nodes.registerType("tcp request",TcpGet);
}
