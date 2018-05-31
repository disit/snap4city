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

  var TIMESERIES_API = "/api/3/action/datastore_ts";
  var DATASTORE_API = "/api/3/action/datastore";

  var TIMESERIES_CREATE = TIMESERIES_API + "_create";
  var DATASTORE_CREATE = DATASTORE_API + "_create";

  function validateNode(node) {
    if (!node.packageId) {
      throw "No packageId specified";
    }

    if (!node.fields) {
      throw "No fields specified";
    }

    if (!node.auth) {
      throw "No credentials specified";
    }
  }

  function DatagateCreateNode(n) {
    validateNode(n);

    RED.nodes.createNode(this, n);
    var node = this;

    node.packageId = n.packageId;
    node.timeseries = n.timeseries;
    node.name = n.name;
    node.fields = n.fields;
    node.auth = RED.nodes.getNode(n.auth);

    node.token = node.auth.token;
    node.datagate = node.auth.datagate;

    node.on("input", function (msg) {
      var payload = {
        resource: {
          package_id: node.packageId
        },
        fields: node.fields
      };

      if (node.name) {
        payload.resource.name = node.name;
      }
      if (node.description) {
        payload.resource.description = node.description;
      }
      if (node.timeseries) {
        payload.resource.format = 'timeseries'
      }

      var endpoint = node.datagate + (node.timeseries ? TIMESERIES_CREATE : DATASTORE_CREATE);

      node.log(endpoint);
      node.log(node.token);
      node.log(JSON.stringify(payload));

      node.status({
        fill: "yellow",
        shape: "dot",
        text: "requesting..."
      });
      httpclient.post(endpoint, node.token, payload, function (res) {
        try {
          var res = JSON.parse(res);
          assert(res.success);
          var resourceId = res.result.resource_id;
          node.status({
            fill: "green",
            shape: "dot",
            text: "success"
          });
          node.warn('resource created, id: ' + resourceId);
        } catch (err) {
          node.error(res)
          node.status({
            fill: "red",
            shape: "dot",
            text: "error"
          });
        }
        (function (node) {
          setTimeout(function () {
            node.status({})
          }, 2000)
        })(node);
      });
    });
  }

  RED.nodes.registerType("datagate create", DatagateCreateNode);

  RED.httpAdmin.post("/datagate_create/:id", RED.auth.needsPermission("ckants.query"), function (req, res) {
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