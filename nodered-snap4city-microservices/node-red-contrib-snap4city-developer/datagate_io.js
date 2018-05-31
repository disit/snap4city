/**
 * Copyright 2014 Sense Tecnic Systems, Inc.
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

var util = require("util");
var httpclient = require('./httpclient');
var assert = require('assert');

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
  "use strict";

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

  var TIMESERIES_API = "/api/3/action/datastore_ts";
  var DATASTORE_API = "/api/3/action/datastore";

  var DATASTORE_UPSERT = DATASTORE_API + "_upsert"
  var DATASTORE_SEARCH = DATASTORE_API + "_search"

  var TIMESERIES_UPSERT = TIMESERIES_API + "_upsert"
  var TIMESERIES_SEARCH = TIMESERIES_API + "_search"

  function validateNode(node) {
    if (!node.resourceId) {
      throw "No resourceId specified";
    }

    if (!node.auth) {
      throw "No credentials specified";
    }
  }

  function DatagateSearchNode(n) {
    validateNode(n);

    RED.nodes.createNode(this, n);
    var node = this;

    node.resourceId = n.resourceId;
    node.timeseries = n.timeseries;
    node.fromtime = n.fromtime;
    node.totime = n.totime;
    node.auth = RED.nodes.getNode(n.auth);


    node.token = node.auth.token;
    node.datagate = node.auth.datagate;

    node.on("input", function (msg) {
      if (!msg) {
        msg = {
          payload: {}
        }
      }

      if (!msg.payload) {
        msg.payload = {}
      }

      var payload = {
        resource_id: node.resourceId,
        limit: 500 //default limit
      };

      if (node.fromtime) {
        payload.fromtime = node.fromtime;
      }
      if (node.totime) {
        payload.totime = node.totime;
      }
      if (node.timezone) {
        payload.timezone = node.timezone;
      }
      // overriding from msg.payload if any
      // this needs to be strict as CKAN won't ignore foreign parameters
      if (msg.payload.q) {
        payload.q = msg.payload.q;
      }
      if (msg.payload.filters) {
        payload.filters = msg.payload.filters;
      }
      if (msg.payload.distinct) {
        payload.distinct = msg.payload.distinct;
      }
      if (msg.payload.plain) {
        payload.plain = msg.payload.plain;
      }
      if (msg.payload.language) {
        payload.language = msg.payload.language;
      }
      if (msg.payload.limit) {
        payload.limit = msg.payload.limit;
      }
      if (msg.payload.offset) {
        payload.offset = msg.payload.offset;
      }
      if (msg.payload.sort) {
        payload.sort = msg.payload.sort;
      }
      if (msg.payload.fields) {
        payload.fields = msg.payload.fields;
      }
      if (msg.payload.fromtime) {
        payload.fromtime = msg.payload.fromtime;
      }
      if (msg.payload.totime) {
        payload.totime = msg.payload.totime;
      }
      if (msg.payload.timezone) {
        payload.timezone = msg.payload.timezone;
      }

      if (!node.timeseries) {
        delete payload.fromtime
        delete payload.totime
      }

      var endpoint = node.datagate + (node.timeseries ? TIMESERIES_SEARCH : DATASTORE_SEARCH);

      node.status({
        fill: "green",
        shape: "dot",
        text: "working..."
      });



      httpclient.post(endpoint, node.token, payload, function (res) {
        try {
          var res = JSON.parse(res);
          assert(res.success);
          eventLog({}, {
            payload: res
          }, {}, "Node-Red", "Datagate", endpoint, "RX");
          node.send({
            payload: res
          });
          node.status({})
        } catch (err) {
          node.status({
            fill: "red",
            shape: "dot",
            text: "error"
          })
          node.error(res)
          setTimeout(function () {
            node.status({})
          }, 2000)
        }

      });
    });
  }

  RED.nodes.registerType("datagate search", DatagateSearchNode);

  function DatagateInsertNode(n) {
    validateNode(n);

    RED.nodes.createNode(this, n);
    var node = this;

    node.resourceId = n.resourceId;
    node.auth = RED.nodes.getNode(n.auth);
    node.timeseries = n.timeseries;


    node.token = node.auth.token;
    node.datagate = node.auth.datagate;

    node.on("input", function (msg) {
      if (!msg || msg.payload == null) {
        node.error('no input');
        return;
      }

      if (!Array.isArray(msg.payload)) {
        // this node receives an array of records, converting msg.payload into an array
        msg.payload = [msg.payload]
      }

      var payload = {
        resource_id: node.resourceId,
        method: "insert",
        records: msg.payload
      };

      var endpoint = node.datagate + (node.timeseries ? TIMESERIES_UPSERT : DATASTORE_UPSERT);

      httpclient.post(endpoint, node.token, payload, function (res) {
        try {
          var res = JSON.parse(res);
          assert(res.success);
          eventLog({}, payload, n, "Node-Red", "Datagate", endpoint, "TX");
          node.status({
            fill: "green",
            shape: "dot",
            text: "success"
          });
        } catch (err) {
          node.error(res)
        }
        setTimeout(function () {
          node.status({})
        }, 2000)
      });
    });
  }
  RED.nodes.registerType("datagate insert", DatagateInsertNode);

  RED.httpAdmin.post("/datagate_search/:id", RED.auth.needsPermission("ckants.search"), function (req, res) {
    var node = RED.nodes.getNode(req.params.id);
    if (node != null) {
      try {
        node.receive();
        res.sendStatus(200);
      } catch (err) {
        res.sendStatus(500);
        node.error(RED._("ckants.failed", {
          error: err.toString()
        }));
      }
    } else {
      res.sendStatus(404);
    }
  });

}