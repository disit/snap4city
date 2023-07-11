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
    function CSVNode(n) {
        RED.nodes.createNode(this,n);
        this.template = (n.temp || "").split(",");
        this.sep = (n.sep || ',').replace("\\t","\t").replace("\\n","\n").replace("\\r","\r");
        this.quo = '"';
        this.ret = (n.ret || "\n").replace("\\n","\n").replace("\\r","\r");
        this.winflag = (this.ret === "\r\n");
        this.lineend = "\n";
        this.multi = n.multi || "one";
        this.hdrin = n.hdrin || false;
        this.hdrout = n.hdrout || "none";
        this.goodtmpl = true;
        this.skip = parseInt(n.skip || 0);
        this.store = [];
        this.parsestrings = n.strings;
        this.include_empty_strings = n.include_empty_strings || false;
        this.include_null_values = n.include_null_values || false;
        if (this.parsestrings === undefined) { this.parsestrings = true; }
        if (this.hdrout === false) { this.hdrout = "none"; }
        if (this.hdrout === true) { this.hdrout = "all"; }
        var tmpwarn = true;
        var node = this;

        // pass in an array of column names to be trimed, de-quoted and retrimed
        var clean = function(col) {
            for (var t = 0; t < col.length; t++) {
                col[t] = col[t].trim(); // remove leading and trailing whitespace
                if (col[t].charAt(0) === '"' && col[t].charAt(col[t].length -1) === '"') {
                    // remove leading and trailing quotes (if they exist) - and remove whitepace again.
                    col[t] = col[t].substr(1,col[t].length -2).trim();
                }
            }
            if ((col.length === 1) && (col[0] === "")) { node.goodtmpl = false; }
            else { node.goodtmpl = true; }
            return col;
        }
        node.template = clean(node.template);
        node.hdrSent = false;

        this.on("input", function(msg) {
            if (msg.hasOwnProperty("reset")) {
                node.hdrSent = false;
            }
            if (msg.hasOwnProperty("payload")) {
                if (typeof msg.payload == "object") { // convert object to CSV string
                    try {
                        var ou = "";
                        if (node.hdrout !== "none" && node.hdrSent === false) {
                            if ((node.template.length === 1) && (node.template[0] === '') && (msg.hasOwnProperty("columns"))) {
                                node.template = clean((msg.columns || "").split(","));
                            }
                            ou += node.template.join(node.sep) + node.ret;
                            if (node.hdrout === "once") { node.hdrSent = true; }
                        }
                        if (!Array.isArray(msg.payload)) { msg.payload = [ msg.payload ]; }
                        for (var s = 0; s < msg.payload.length; s++) {
                            if ((Array.isArray(msg.payload[s])) || (typeof msg.payload[s] !== "object")) {
                                if (typeof msg.payload[s] !== "object") { msg.payload = [ msg.payload ]; }
                                for (var t = 0; t < msg.payload[s].length; t++) {
                                    if (!msg.payload[s][t] && (msg.payload[s][t] !== 0)) { msg.payload[s][t] = ""; }
                                    if (msg.payload[s][t].toString().indexOf(node.quo) !== -1) { // add double quotes if any quotes
                                        msg.payload[s][t] = msg.payload[s][t].toString().replace(/"/g, '""');
                                        msg.payload[s][t] = node.quo + msg.payload[s][t].toString() + node.quo;
                                    }
                                    else if (msg.payload[s][t].toString().indexOf(node.sep) !== -1) { // add quotes if any "commas"
                                        msg.payload[s][t] = node.quo + msg.payload[s][t].toString() + node.quo;
                                    }
                                }
                                ou += msg.payload[s].join(node.sep) + node.ret;
                            }
                            else {
                                if ((node.template.length === 1) && (node.template[0] === '') && (msg.hasOwnProperty("columns"))) {
                                    node.template = clean((msg.columns || "").split(","));
                                }
                                if ((node.template.length === 1) && (node.template[0] === '')) {
                                    /* istanbul ignore else */
                                    if (tmpwarn === true) { // just warn about missing template once
                                        node.warn(RED._("csv.errors.obj_csv"));
                                        tmpwarn = false;
                                    }
                                    for (var p in msg.payload[0]) {
                                        /* istanbul ignore else */
                                        if (msg.payload[0].hasOwnProperty(p)) {
                                            /* istanbul ignore else */
                                            if (typeof msg.payload[0][p] !== "object") {
                                                var q = "" + msg.payload[0][p];
                                                if (q.indexOf(node.quo) !== -1) { // add double quotes if any quotes
                                                    q = q.replace(/"/g, '""');
                                                    ou += node.quo + q + node.quo + node.sep;
                                                }
                                                else if (q.indexOf(node.sep) !== -1) { // add quotes if any "commas"
                                                    ou += node.quo + q + node.quo + node.sep;
                                                }
                                                else { ou += q + node.sep; } // otherwise just add
                                            }
                                        }
                                    }
                                    ou = ou.slice(0,-1) + node.ret;
                                }
                                else {
                                    for (var t=0; t < node.template.length; t++) {
                                        if (node.template[t] === '') {
                                            ou += node.sep;
                                        }
                                        else {
                                            var p = RED.util.ensureString(RED.util.getMessageProperty(msg,"payload["+s+"]['"+node.template[t]+"']"));
                                            /* istanbul ignore else */
                                            if (p === "undefined") { p = ""; }
                                            if (p.indexOf(node.quo) !== -1) { // add double quotes if any quotes
                                                p = p.replace(/"/g, '""');
                                                ou += node.quo + p + node.quo + node.sep;
                                            }
                                            else if (p.indexOf(node.sep) !== -1) { // add quotes if any "commas"
                                                ou += node.quo + p + node.quo + node.sep;
                                            }
                                            else { ou += p + node.sep; } // otherwise just add
                                        }
                                    }
                                    ou = ou.slice(0,-1) + node.ret; // remove final "comma" and add "newline"
                                }
                            }
                        }
                        msg.payload = ou;
                        msg.columns = node.template.join(',');
                        if (msg.payload !== '') { node.send(msg); }
                    }
                    catch(e) { node.error(e,msg); }
                }
                else if (typeof msg.payload == "string") { // convert CSV string to object
                    try {
                        var f = true; // flag to indicate if inside or outside a pair of quotes true = outside.
                        var j = 0; // pointer into array of template items
                        var k = [""]; // array of data for each of the template items
                        var o = {}; // output object to build up
                        var a = []; // output array is needed for multiline option
                        var first = true; // is this the first line
                        var line = msg.payload;
                        var linecount = 0;
                        var tmp = "";
                        var reg = /^[-]?(?!E)(?!0\d)\d*\.?\d*(E-?\+?)?\d+$/i;
                        if (msg.hasOwnProperty("parts")) {
                            linecount = msg.parts.index;
                            if (msg.parts.index > node.skip) { first = false; }
                        }

                        // For now we are just going to assume that any \r or \n means an end of line...
                        //   got to be a weird csv that has singleton \r \n in it for another reason...

                        // Now process the whole file/line
                        for (var i = 0; i < line.length; i++) {
                            if (first && (linecount < node.skip)) {
                                if (line[i] === "\n") { linecount += 1; }
                                continue;
                            }
                            if ((node.hdrin === true) && first) { // if the template is in the first line
                                if ((line[i] === "\n")||(line[i] === "\r")||(line.length - i === 1)) { // look for first line break
                                    if (line.length - i === 1) { tmp += line[i]; }
                                    node.template = clean(tmp.split(node.sep));
                                    first = false;
                                }
                                else { tmp += line[i]; }
                            }
                            else {
                                if (line[i] === node.quo) { // if it's a quote toggle inside or outside
                                    f = !f;
                                    if (line[i-1] === node.quo) {
                                        if (f === false) { k[j] += '\"'; }
                                    } // if it's a quotequote then it's actually a quote
                                    //if ((line[i-1] !== node.sep) && (line[i+1] !== node.sep)) { k[j] += line[i]; }
                                }
                                else if ((line[i] === node.sep) && f) { // if it is the end of the line then finish
                                    if (!node.goodtmpl) { node.template[j] = "col"+(j+1); }
                                    if ( node.template[j] && (node.template[j] !== "") ) {
                                        // if no value between separators ('1,,"3"...') or if the line beings with separator (',1,"2"...') treat value as null
                                        if (line[i-1] === node.sep || line[i-1].includes('\n','\r')) k[j] = null;
                                        if ( (k[j] !== null && node.parsestrings === true) && reg.test(k[j]) ) { k[j] = parseFloat(k[j]); }
                                        if (node.include_null_values && k[j] === null) o[node.template[j]] = k[j];
                                        if (node.include_empty_strings && k[j] === "") o[node.template[j]] = k[j];
                                        if (k[j] !== null && k[j] !== "") o[node.template[j]] = k[j];
                                    }
                                    j += 1;
                                    // if separator is last char in processing string line (without end of line), add null value at the end - example: '1,2,3\n3,"3",'
                                    k[j] = line.length - 1 === i ? null : "";
                                }
                                else if (((line[i] === "\n") || (line[i] === "\r")) && f) { // handle multiple lines
                                    //console.log(j,k,o,k[j]);
                                    if (!node.goodtmpl) { node.template[j] = "col"+(j+1); }
                                    if ( node.template[j] && (node.template[j] !== "") ) {
                                        // if separator before end of line, set null value ie. '1,2,"3"\n1,2,\n1,2,3'
                                        if (line[i-1] === node.sep) k[j] = null;
                                        if ( (k[j] !== null && node.parsestrings === true) && reg.test(k[j]) ) { k[j] = parseFloat(k[j]); }
                                        else { if (k[j] !== null) k[j].replace(/\r$/,''); }
                                        if (node.include_null_values && k[j] === null) o[node.template[j]] = k[j];
                                        if (node.include_empty_strings && k[j] === "") o[node.template[j]] = k[j];
                                        if (k[j] !== null && k[j] !== "") o[node.template[j]] = k[j];
                                    }
                                    if (JSON.stringify(o) !== "{}") { // don't send empty objects
                                        a.push(o); // add to the array
                                    }
                                    j = 0;
                                    k = [""];
                                    o = {};
                                    f = true; // reset in/out flag ready for next line.
                                }
                                else { // just add to the part of the message
                                    k[j] += line[i];
                                }
                            }
                        }
                        // Finished so finalize and send anything left
                        if (f === false) { node.warn(RED._("csv.errors.bad_csv")); }
                        if (!node.goodtmpl) { node.template[j] = "col"+(j+1); }
                        
                        if ( node.template[j] && (node.template[j] !== "") ) {
                            if ( (k[j] !== null && node.parsestrings === true) && reg.test(k[j]) ) { k[j] = parseFloat(k[j]); }
                            else { if (k[j] !== null) k[j].replace(/\r$/,''); }
                            if (node.include_null_values && k[j] === null) o[node.template[j]] = k[j];
                            if (node.include_empty_strings && k[j] === "") o[node.template[j]] = k[j];
                            if (k[j] !== null && k[j] !== "") o[node.template[j]] = k[j];
                        }
                        if (JSON.stringify(o) !== "{}") { // don't send empty objects
                            a.push(o); // add to the array
                        }
                        var has_parts = msg.hasOwnProperty("parts");

                        if (node.multi !== "one") {
                            msg.payload = a;
                            if (has_parts) {
                                if (JSON.stringify(o) !== "{}") {
                                    node.store.push(o);
                                }
                                if (msg.parts.index + 1 === msg.parts.count) {
                                    msg.payload = node.store;
                                    msg.columns = node.template.filter(val => val).join(',');
                                    delete msg.parts;
                                    node.send(msg);
                                    node.store = [];
                                }
                            }
                            else {
                                msg.columns = node.template.filter(val => val).join(',');
                                node.send(msg); // finally send the array
                            }
                        }
                        else {
                            var len = a.length;
                            for (var i = 0; i < len; i++) {
                                var newMessage = RED.util.cloneMessage(msg);
                                newMessage.columns = node.template.filter(val => val).join(',');
                                newMessage.payload = a[i];
                                if (!has_parts) {
                                    newMessage.parts = {
                                        id: msg._msgid,
                                        index: i,
                                        count: len
                                    };
                                }
                                else {
                                    newMessage.parts.index -= node.skip;
                                    newMessage.parts.count -= node.skip;
                                    if (node.hdrin) { // if we removed the header line then shift the counts by 1
                                        newMessage.parts.index -= 1;
                                        newMessage.parts.count -= 1;
                                    }
                                }
                                node.send(newMessage);
                            }
                        }
                        node.linecount = 0;
                    }
                    catch(e) { node.error(e,msg); }
                }
                else { node.warn(RED._("csv.errors.csv_js")); }
            }
            else {
                if (!msg.hasOwnProperty("reset")) {
                    node.send(msg); // If no payload and not reset - just pass it on.
                }
            }
        });
    }
    RED.nodes.registerType("csv",CSVNode);
}
