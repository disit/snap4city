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
var httpclient = require('httpclient');
var assert = require('assert');

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
  "use strict";
    var RED2 = require.main.require('node-red');
	  
	  
   function deviceregistration(config) {

            RED.nodes.createNode(this,config);
            var node = this;

          /*  node.devicename = config.devicename;
            node.devicetype = config.devicetype,
                node.devicekind = config.devicekind,
                node.contextbroker = config.contextbroker,
                node.deviceprotocol = config.deviceprotocol,
                node.deviceformat = config.deviceformat,
                node.devicecreateddate = config.devicecreateddate,
                node.devicemac = config.devicemac,
                node.devicemodel = config.devicemodel,
                node.deviceproducer = config.deviceproducer,
                node.devicelatitude = config.devicelatitude,
                node.devicelongitude = config.devicelongitude,
                node.name = config.name;
                node.attrs = config.attrs;*/

            node.on("input",function(msg)  {jsdom
                    node.send(msg);
                });
    }
  
    
	

  RED.nodes.registerType("device-registration-user",deviceregistration);

  /* RED.httpAdmin.post("/ckants_create/:id", RED.auth.needsPermission("ckants.query"), function(req,res) {
    var node = RED.nodes.getNode(req.params.id);
    if (node != null) {
      try {
          node.receive();
          res.sendStatus(200);
      } catch(err) {
          res.sendStatus(500);
          node.error(RED._("ckants.failed",{error:err.toString()}));
      }
    } else {
        res.sendStatus(404);
    }
  }); */

  RED.httpAdmin.get('/s4c/js/*', function(req, res) {
    var options = {
        root: __dirname + '/lib/js/',
        dotfiles: 'deny'
    };
  
    res.sendFile(req.params[0], options);
  });
  
  RED.httpAdmin.get('/s4c/css/*', function(req, res) {
    var options = {
        root: __dirname + '/lib/css/',
        dotfiles: 'deny'
    };
  
    res.sendFile(req.params[0], options);
  });
    
    

    RED.httpAdmin.get("/deviceregistration/:id", function(req,res) {
        res.json({"accessToken": retrieveAccessToken()});
        //res.json({"refreshToken": retrieveAccessToken()});
    });
    
    function retrieveAccessToken(){
        console.log("here"); 
        var fs = require('fs');
        var refreshToken = fs.readFileSync('refresh_token', 'utf-8');
        console.log("refresh_token is");
        console.log(refreshToken);
        var url = "https://www.snap4city.org/auth/realms/master/protocol/openid-connect/token/";
        var params= "client_id=nodered&client_secret=943106ae-c62c-4961-85a2-849f6955d404&grant_type=refresh_token&scope=openid profile&refresh_token=" + refreshToken;
        var response = "";
        var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
        var xmlHttp = new XMLHttpRequest();
        console.log(encodeURI(url));
        xmlHttp.open("POST", encodeURI(url), false);
        xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        xmlHttp.send(params);
        if (xmlHttp.responseText != "") {
            try {
                console.log("xmlHttp.responseText");
                console.log(xmlHttp.responseText);
                response = JSON.parse(xmlHttp.responseText);
            } catch (e) {
            }
        }
        if(response != ""){
            fs.writeFileSync('refresh_token', response.refresh_token);
            console.log("response.refresh_token is:" );
            console.log(response.refresh_token );
            console.log("response.access_token is:" );
            console.log(response.access_token );
            return response.access_token;
            //return response.refresh_token;
        }
        console.log("response is:"+ response );
        return response;
    } 
}

