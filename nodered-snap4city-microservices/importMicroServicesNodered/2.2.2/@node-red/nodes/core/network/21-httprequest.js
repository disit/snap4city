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
    const got = require("got");
    const {CookieJar} = require("tough-cookie");
    const { HttpProxyAgent, HttpsProxyAgent } = require('hpagent');
    const FormData = require('form-data');
    const { v4: uuid } = require('uuid');
    const crypto = require('crypto');
    const URL = require("url").URL
    var mustache = require("mustache");
    var querystring = require("querystring");
    var cookie = require("cookie");
    var hashSum = require("hash-sum");


    // Cache a reference to the existing https.request function
    // so we can compare later to see if an old agent-base instance
    // has been required.
    // This is generally okay as the core nodes are required before
    // any contrib nodes. Where it will fail is if the agent-base module
    // is required via the settings file or outside of Node-RED before it
    // is started.
    // If there are other modules that patch the function, they will get undone
    // as well. Not much we can do about that right now. Patching core
    // functions is bad.
    const HTTPS_MODULE = require("https");
    const HTTPS_REQUEST = HTTPS_MODULE.request;

    function checkNodeAgentPatch() {
        if (HTTPS_MODULE.request !== HTTPS_REQUEST && HTTPS_MODULE.request.length === 2) {
            RED.log.warn(`

---------------------------------------------------------------------
Patched https.request function detected. This will break the
HTTP Request node. The original code has now been restored.

This is likely caused by a contrib node including an old version of
the 'agent-base@<5.0.0' module.

You can identify what node is at fault by running:
   npm list agent-base
in your Node-RED user directory (${RED.settings.userDir}).
---------------------------------------------------------------------
`);
            HTTPS_MODULE.request = HTTPS_REQUEST
        }
    }

    function HTTPRequest(n) {
        RED.nodes.createNode(this,n);
        checkNodeAgentPatch();
        var node = this;
        var nodeUrl = n.url;
        var isTemplatedUrl = (nodeUrl||"").indexOf("{{") != -1;
        var nodeMethod = n.method || "GET";
        var paytoqs = false;
        var paytobody = false;
        var redirectList = [];
        var sendErrorsToCatch = n.senderr;

        var nodeHTTPPersistent = n["persist"];
        if (n.tls) {
            var tlsNode = RED.nodes.getNode(n.tls);
        }
        this.ret = n.ret || "txt";
        this.authType = n.authType || "basic";
        if (RED.settings.httpRequestTimeout) { this.reqTimeout = parseInt(RED.settings.httpRequestTimeout) || 120000; }
        else { this.reqTimeout = 120000; }

        if (n.paytoqs === true || n.paytoqs === "query") { paytoqs = true; }
        else if (n.paytoqs === "body") { paytobody = true; }


        var prox, noprox;
        if (process.env.http_proxy) { prox = process.env.http_proxy; }
        if (process.env.HTTP_PROXY) { prox = process.env.HTTP_PROXY; }
        if (process.env.no_proxy) { noprox = process.env.no_proxy.split(","); }
        if (process.env.NO_PROXY) { noprox = process.env.NO_PROXY.split(","); }

        var proxyConfig = null;
        if (n.proxy) {
            proxyConfig = RED.nodes.getNode(n.proxy);
            prox = proxyConfig.url;
            noprox = proxyConfig.noproxy;
        }

        let timingLog = false;
        if (RED.settings.hasOwnProperty("httpRequestTimingLog")) {
            timingLog = RED.settings.httpRequestTimingLog;
        }

        this.on("input",function(msg,nodeSend,nodeDone) {
            checkNodeAgentPatch();
            //reset redirectList on each request
            redirectList = [];
            var preRequestTimestamp = process.hrtime();
            node.status({fill:"blue",shape:"dot",text:"httpin.status.requesting"});
            var url = nodeUrl || msg.url;
            if (msg.url && nodeUrl && (nodeUrl !== msg.url)) {  // revert change below when warning is finally removed
                node.warn(RED._("common.errors.nooverride"));
            }

            if (isTemplatedUrl) {
                url = mustache.render(nodeUrl,msg);
            }
            if (!url) {
                node.error(RED._("httpin.errors.no-url"),msg);
                nodeDone();
                return;
            }


            // url must start http:// or https:// so assume http:// if not set
            if (url.indexOf("://") !== -1 && url.indexOf("http") !== 0) {
                node.warn(RED._("httpin.errors.invalid-transport"));
                node.status({fill:"red",shape:"ring",text:"httpin.errors.invalid-transport"});
                nodeDone();
                return;
            }
            if (!((url.indexOf("http://") === 0) || (url.indexOf("https://") === 0))) {
                if (tlsNode) {
                    url = "https://"+url;
                } else {
                    url = "http://"+url;
                }
            }

            // The Request module used in Node-RED 1.x was tolerant of query strings that
            // were partially encoded. For example - "?a=hello%20there&b=20%"
            // The GOT module doesn't like that.
            // The following is an attempt to normalise the url to ensure it is properly
            // encoded. We cannot just encode it directly as we don't want any valid
            // encoded entity to end up doubly encoded.
            if (url.indexOf("?") > -1) {
                // Only do this if there is a query string to deal with
                const [hostPath, ...queryString] = url.split("?")
                const query = queryString.join("?");
                if (query) {
                    // Look for any instance of % not followed by two hex chars.
                    // Replace any we find with %25.
                    const escapedQueryString = query.replace(/(%.?.?)/g, function(v) {
                        if (/^%[a-f0-9]{2}/i.test(v)) {
                            return v;
                        }
                        return v.replace(/%/,"%25")
                    })
                    url = hostPath+"?"+escapedQueryString;
                }
            }

            var method = nodeMethod.toUpperCase() || "GET";
            if (msg.method && n.method && (n.method !== "use")) {     // warn if override option not set
                node.warn(RED._("common.errors.nooverride"));
            }
            if (msg.method && n.method && (n.method === "use")) {
                method = msg.method.toUpperCase();          // use the msg parameter
            }

            // var isHttps = (/^https/i.test(url));

            var opts = {};
            // set defaultport, else when using HttpsProxyAgent, it's defaultPort of 443 will be used :(.
            // Had to remove this to get http->https redirect to work
            // opts.defaultPort = isHttps?443:80;
            opts.timeout = node.reqTimeout;
            opts.throwHttpErrors = false;
            // TODO: add UI option to auto decompress. Setting to false for 1.x compatibility
            opts.decompress = false;
            opts.method = method;
            opts.headers = {};
            opts.retry = 0;
            opts.responseType = 'buffer';
            opts.maxRedirects = 21;
            opts.cookieJar = new CookieJar();
            opts.ignoreInvalidCookies = true;
            opts.forever = nodeHTTPPersistent;
            if (msg.requestTimeout !== undefined) {
                if (isNaN(msg.requestTimeout)) {
                    node.warn(RED._("httpin.errors.timeout-isnan"));
                } else if (msg.requestTimeout < 1) {
                    node.warn(RED._("httpin.errors.timeout-isnegative"));
                } else {
                    opts.timeout = msg.requestTimeout;
                }
            }
            const originalHeaderMap = {};

            opts.hooks = {
                beforeRequest: [
                    options => {
                        // Whilst HTTP headers are meant to be case-insensitive,
                        // in the real world, there are servers that aren't so compliant.
                        // GOT will lower case all headers given a chance, so we need
                        // to restore the case of any headers the user has set.
                        Object.keys(options.headers).forEach(h => {
                            if (originalHeaderMap[h] && originalHeaderMap[h] !== h) {
                                options.headers[originalHeaderMap[h]] = options.headers[h];
                                delete options.headers[h];
                            }
                        })
                    }
                ],
                beforeRedirect: [
                    (options, response) => {
                        let redirectInfo = {
                            location: response.headers.location
                        }
                        if (response.headers.hasOwnProperty('set-cookie')) {
                            redirectInfo.cookies = extractCookies(response.headers['set-cookie']);
                        }
                        redirectList.push(redirectInfo)
                    }
                ]
            }

            var ctSet = "Content-Type"; // set default camel case
            var clSet = "Content-Length";
            if (msg.headers) {
                if (msg.headers.hasOwnProperty('x-node-red-request-node')) {
                    var headerHash = msg.headers['x-node-red-request-node'];
                    delete msg.headers['x-node-red-request-node'];
                    var hash = hashSum(msg.headers);
                    if (hash === headerHash) {
                        delete msg.headers;
                    }
                }
                if (msg.headers) {
                    for (var v in msg.headers) {
                        if (msg.headers.hasOwnProperty(v)) {
                            var name = v.toLowerCase();
                            if (name !== "content-type" && name !== "content-length") {
                                // only normalise the known headers used later in this
                                // function. Otherwise leave them alone.
                                name = v;
                            }
                            else if (name === 'content-type') { ctSet = v; }
                            else { clSet = v; }
                            opts.headers[name] = msg.headers[v];
                        }
                    }
                }
            }

            if (msg.hasOwnProperty('followRedirects')) {
                opts.followRedirect = !!msg.followRedirects;
            }

            if (opts.headers.hasOwnProperty('cookie')) {
                var cookies = cookie.parse(opts.headers.cookie, {decode:String});
                for (var name in cookies) {
                    opts.cookieJar.setCookieSync(cookie.serialize(name, cookies[name], {encode:String}), url, {ignoreError: true});
                }
                delete opts.headers.cookie;
            }
            if (msg.cookies) {
                for (var name in msg.cookies) {
                    if (msg.cookies.hasOwnProperty(name)) {
                        if (msg.cookies[name] === null || msg.cookies[name].value === null) {
                            // This case clears a cookie for HTTP In/Response nodes.
                            // Ignore for this node.
                        } else if (typeof msg.cookies[name] === 'object') {
                            if(msg.cookies[name].encode === false){
                                // If the encode option is false, the value is not encoded.
                                opts.cookieJar.setCookieSync(cookie.serialize(name, msg.cookies[name].value, {encode: String}), url, {ignoreError: true});
                            } else {
                                // The value is encoded by encodeURIComponent().
                                opts.cookieJar.setCookieSync(cookie.serialize(name, msg.cookies[name].value), url, {ignoreError: true});
                            }
                        } else {
                            opts.cookieJar.setCookieSync(cookie.serialize(name, msg.cookies[name]), url, {ignoreError: true});
                        }
                    }
                }
            }
            var parsedURL = new URL(url)
            this.credentials = this.credentials || {}
            if (parsedURL.username && !this.credentials.user) {
                this.credentials.user = parsedURL.username
            }
            if (parsedURL.password && !this.credentials.password) {
                this.credentials.password = parsedURL.password
            }
            if (Object.keys(this.credentials).length != 0) {
                if (this.authType === "basic") {
                    // Workaround for https://github.com/sindresorhus/got/issues/1169 (fixed in got v12)
                    // var cred = ""
                    if (this.credentials.user || this.credentials.password) {
                        // cred = `${this.credentials.user}:${this.credentials.password}`;
                        if (this.credentials.user === undefined) { this.credentials.user = ""}
                        if (this.credentials.password === undefined) { this.credentials.password = ""}
                        opts.headers.Authorization = "Basic " + Buffer.from(`${this.credentials.user}:${this.credentials.password}`).toString("base64");
                    }
                    // build own basic auth header
                    // opts.headers.Authorization = "Basic " + Buffer.from(cred).toString("base64");
                } else if (this.authType === "digest") {
                    let digestCreds = this.credentials;
                    let sentCreds = false;
                    opts.hooks.afterResponse = [(response, retry) => {
                        if (response.statusCode === 401) {
                            if (sentCreds) {
                                return response
                            }
                            const requestUrl = new URL(response.request.requestUrl);
                            const options = response.request.options;
                            const normalisedHeaders = {};
                            Object.keys(response.headers).forEach(k => {
                                normalisedHeaders[k.toLowerCase()] = response.headers[k]
                            })
                            if (normalisedHeaders['www-authenticate']) {
                                let authHeader = buildDigestHeader(digestCreds.user,digestCreds.password, options.method, requestUrl.pathname, normalisedHeaders['www-authenticate'])
                                options.headers.Authorization = authHeader;
                            }
                            sentCreds = true;
                            return retry(options);
                        }
                        return response
                    }];
                } else if (this.authType === "bearer") {
                    opts.headers.Authorization = `Bearer ${this.credentials.password||""}`
                }
            }
            var payload = null;


            if (method !== 'GET' && method !== 'HEAD' && typeof msg.payload !== "undefined") {
                if (opts.headers['content-type'] == 'multipart/form-data' && typeof msg.payload === "object") {
                    let formData = new FormData();
                    for (var opt in msg.payload) {
                        if (msg.payload.hasOwnProperty(opt)) {
                            var val = msg.payload[opt];
                            if (val !== undefined && val !== null) {
                                if (typeof val === 'string' || Buffer.isBuffer(val)) {
                                    formData.append(opt, val);
                                } else if (typeof val === 'object' && val.hasOwnProperty('value')) {
                                    formData.append(opt,val.value,val.options || {});
                                } else {
                                    formData.append(opt,JSON.stringify(val));
                                }
                            }
                        }
                    }
                    // GOT will only set the content-type header with the correct boundary
                    // if the header isn't set. So we delete it here, for GOT to reset it.
                    delete opts.headers['content-type'];
                    opts.body = formData;
                } else {
                    if (typeof msg.payload === "string" || Buffer.isBuffer(msg.payload)) {
                        payload = msg.payload;
                    } else if (typeof msg.payload == "number") {
                        payload = msg.payload+"";
                    } else {
                        if (opts.headers['content-type'] == 'application/x-www-form-urlencoded') {
                            payload = querystring.stringify(msg.payload);
                        } else {
                            payload = JSON.stringify(msg.payload);
                            if (opts.headers['content-type'] == null) {
                                opts.headers[ctSet] = "application/json";
                            }
                        }
                    }
                    if (opts.headers['content-length'] == null) {
                        if (Buffer.isBuffer(payload)) {
                            opts.headers[clSet] = payload.length;
                        } else {
                            opts.headers[clSet] = Buffer.byteLength(payload);
                        }
                    }
                    opts.body = payload;
                }
            }


            if (method == 'GET' && typeof msg.payload !== "undefined" && paytoqs) {
                if (typeof msg.payload === "object") {
                    try {
                        if (url.indexOf("?") !== -1) {
                            url += (url.endsWith("?")?"":"&") + querystring.stringify(msg.payload);
                        } else {
                            url += "?" + querystring.stringify(msg.payload);
                        }
                    } catch(err) {

                        node.error(RED._("httpin.errors.invalid-payload"),msg);
                        nodeDone();
                        return;
                    }
                } else {

                    node.error(RED._("httpin.errors.invalid-payload"),msg);
                    nodeDone();
                    return;
                }
            } else if ( method == "GET" && typeof msg.payload !== "undefined" && paytobody) {
                opts.allowGetBody = true;
                if (typeof msg.payload === "object") {
                    opts.body = JSON.stringify(msg.payload);
                } else if (typeof msg.payload == "number") {
                    opts.body = msg.payload+"";
                } else if (typeof msg.payload === "string" || Buffer.isBuffer(msg.payload)) {
                    opts.body = msg.payload;
                }
            }

            // revert to user supplied Capitalisation if needed.
            if (opts.headers.hasOwnProperty('content-type') && (ctSet !== 'content-type')) {
                opts.headers[ctSet] = opts.headers['content-type'];
                delete opts.headers['content-type'];
            }
            if (opts.headers.hasOwnProperty('content-length') && (clSet !== 'content-length')) {
                opts.headers[clSet] = opts.headers['content-length'];
                delete opts.headers['content-length'];
            }

            var noproxy;
            if (noprox) {
                for (var i = 0; i < noprox.length; i += 1) {
                    if (url.indexOf(noprox[i]) !== -1) { noproxy=true; }
                }
            }
            if (prox && !noproxy) {
                var match = prox.match(/^(https?:\/\/)?(.+)?:([0-9]+)?/i);
                if (match) {
                    let proxyAgent;
                    let proxyURL = new URL(prox);
                    //set username/password to null to stop empty creds header
                    let proxyOptions = {
                        proxy: {
                            protocol: proxyURL.protocol,
                            hostname: proxyURL.hostname,
                            port: proxyURL.port,
                            username: null,
                            password: null
                        },
                        maxFreeSockets: 256,
                        maxSockets: 256,
                        keepAlive: true
                    }
                    if (proxyConfig && proxyConfig.credentials) {
                        let proxyUsername = proxyConfig.credentials.username || '';
                        let proxyPassword = proxyConfig.credentials.password || '';
                        if (proxyUsername || proxyPassword) {
                            proxyOptions.proxy.username = proxyUsername;
                            proxyOptions.proxy.password = proxyPassword;
                        }
                    } else if (proxyURL.username || proxyURL.password){
                        proxyOptions.proxy.username = proxyURL.username;
                        proxyOptions.proxy.password = proxyURL.password;
                    }
                    //need both incase of http -> https redirect
                    opts.agent = {
                        http: new HttpProxyAgent(proxyOptions),
                        https: new HttpsProxyAgent(proxyOptions)
                    };

                } else {
                    node.warn("Bad proxy url: "+ prox);
                }
            }
            if (tlsNode) {
                opts.https = {};
                tlsNode.addTLSOptions(opts.https);
                if (opts.https.ca) {
                    opts.https.certificateAuthority = opts.https.ca;
                    delete opts.https.ca;
                }
                if (opts.https.cert) {
                    opts.https.certificate = opts.https.cert;
                    delete opts.https.cert;
                }
            } else {
                if (msg.hasOwnProperty('rejectUnauthorized')) {
                    opts.https = { rejectUnauthorized: msg.rejectUnauthorized };
                }
            }

            // Now we have established all of our own headers, take a snapshot
            // of their case so we can restore it prior to the request being sent.
            if (opts.headers) {
                Object.keys(opts.headers).forEach(h => {
                    originalHeaderMap[h.toLowerCase()] = h
                })
            }
            got(url,opts).then(res => {
                msg.statusCode = res.statusCode;
                msg.headers = res.headers;
                msg.responseUrl = res.url;
                msg.payload = res.body;
                msg.redirectList = redirectList;
                msg.retry = 0;

                if (msg.headers.hasOwnProperty('set-cookie')) {
                    msg.responseCookies = extractCookies(msg.headers['set-cookie']);
                }
                msg.headers['x-node-red-request-node'] = hashSum(msg.headers);
                // msg.url = url;   // revert when warning above finally removed
                if (node.metric()) {
                    // Calculate request time
                    var diff = process.hrtime(preRequestTimestamp);
                    var ms = diff[0] * 1e3 + diff[1] * 1e-6;
                    var metricRequestDurationMillis = ms.toFixed(3);
                    node.metric("duration.millis", msg, metricRequestDurationMillis);
                    if (res.client && res.client.bytesRead) {
                        node.metric("size.bytes", msg, res.client.bytesRead);
                    }
                    if (timingLog) {
                        emitTimingMetricLog(res.timings, msg);
                    }
                }

                // Convert the payload to the required return type
                if (node.ret !== "bin") {
                    msg.payload = msg.payload.toString('utf8'); // txt

                    if (node.ret === "obj") {
                        try { msg.payload = JSON.parse(msg.payload); } // obj
                        catch(e) { node.warn(RED._("httpin.errors.json-error")); }
                    }
                }
                node.status({});
                nodeSend(msg);
                nodeDone();
            }).catch(err => {
                // Pre 2.1, any errors would be sent to both Catch node and sent on as normal.
                // This is not ideal but is the legacy behaviour of the node.
                // 2.1 adds the 'senderr' option, if set to true, will *only* send errors
                // to Catch nodes. If false, it still does both behaviours.
                // TODO: 3.0 - make it one or the other.

                if (err.code === 'ETIMEDOUT' || err.code === 'ESOCKETTIMEDOUT') {
                    node.error(RED._("common.notification.errors.no-response"), msg);
                    node.status({fill:"red", shape:"ring", text:"common.notification.errors.no-response"});
                } else {
                    node.error(err,msg);
                    node.status({fill:"red", shape:"ring", text:err.code});
                }
                msg.payload = err.toString() + " : " + url;
                msg.statusCode = err.code || (err.response?err.response.statusCode:undefined);
                if (node.metric() && timingLog) {
                    emitTimingMetricLog(err.timings, msg);
                }
                if (!sendErrorsToCatch) {
                    nodeSend(msg);
                }
                nodeDone();
            });
        });

        this.on("close",function() {
            node.status({});
        });

        function emitTimingMetricLog(timings, msg) {
            const props = [
                "start",
                "socket",
                "lookup",
                "connect",
                "secureConnect",
                "upload",
                "response",
                "end",
                "error",
                "abort"
            ];
            if (timings) {
                props.forEach(p => {
                    if (timings[p]) {
                        node.metric(`timings.${p}`, msg, timings[p]);
                    }
                });
            }
        }

        function extractCookies(setCookie) {
            var cookies = {};
            setCookie.forEach(function(c) {
                var parsedCookie = cookie.parse(c);
                var eq_idx = c.indexOf('=');
                var key = c.substr(0, eq_idx).trim()
                parsedCookie.value = parsedCookie[key];
                delete parsedCookie[key];
                cookies[key] = parsedCookie;
            });
            return cookies;
        }
    }

    RED.nodes.registerType("http request",HTTPRequest,{
        credentials: {
            user: {type:"text"},
            password: {type: "password"}
        }
    });

    const md5 = (value) => { return crypto.createHash('md5').update(value).digest('hex') }

    function ha1Compute(algorithm, user, realm, pass, nonce, cnonce) {
        /**
        * RFC 2617: handle both MD5 and MD5-sess algorithms.
        *
        * If the algorithm directive's value is "MD5" or unspecified, then HA1 is
        *   HA1=MD5(username:realm:password)
        * If the algorithm directive's value is "MD5-sess", then HA1 is
        *   HA1=MD5(MD5(username:realm:password):nonce:cnonce)
        */
        var ha1 = md5(user + ':' + realm + ':' + pass)
        if (algorithm && algorithm.toLowerCase() === 'md5-sess') {
            return md5(ha1 + ':' + nonce + ':' + cnonce)
        } else {
            return ha1
        }
    }


    function buildDigestHeader(user, pass, method, path, authHeader) {
        var challenge = {}
        var re = /([a-z0-9_-]+)=(?:"([^"]+)"|([a-z0-9_-]+))/gi
        for (;;) {
            var match = re.exec(authHeader)
            if (!match) {
                break
            }
            challenge[match[1]] = match[2] || match[3]
        }
        var qop = /(^|,)\s*auth\s*($|,)/.test(challenge.qop) && 'auth'
        var nc = qop && '00000001'
        var cnonce = qop && uuid().replace(/-/g, '')
        var ha1 = ha1Compute(challenge.algorithm, user, challenge.realm, pass, challenge.nonce, cnonce)
        var ha2 = md5(method + ':' + path)
        var digestResponse = qop
        ? md5(ha1 + ':' + challenge.nonce + ':' + nc + ':' + cnonce + ':' + qop + ':' + ha2)
        : md5(ha1 + ':' + challenge.nonce + ':' + ha2)
        var authValues = {
            username: user,
            realm: challenge.realm,
            nonce: challenge.nonce,
            uri: path,
            qop: qop,
            response: digestResponse,
            nc: nc,
            cnonce: cnonce,
            algorithm: challenge.algorithm,
            opaque: challenge.opaque
        }

        authHeader = []
        for (var k in authValues) {
            if (authValues[k]) {
                if (k === 'qop' || k === 'nc' || k === 'algorithm') {
                    authHeader.push(k + '=' + authValues[k])
                } else {
                    authHeader.push(k + '="' + authValues[k] + '"')
                }
            }
        }
        authHeader = 'Digest ' + authHeader.join(', ')
        return authHeader
    }
}
