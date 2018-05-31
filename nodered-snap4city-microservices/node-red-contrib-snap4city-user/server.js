//server.js
'use strict'
//dipendenze
var express = require('express');
var request = require('request');
var bodyParser = require('body-parser');
var mysql = require('mysql');


//istanze
var app = express();
var router = express.Router();
//porta (default=3001)
var port = process.env.API_PORT || 3001;



//configurazione api
app.use(bodyParser.urlencoded({
  extended: true
}));
app.use(bodyParser.json());
app.use(function (req, res, next) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Allow-Methods', 'GET,HEAD,OPTIONS,POST,PUT,DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers');
  res.setHeader('Cache-Control', 'no-cache');
  next();
});
router.get('/', function (req, res) {
  res.json({
    message: 'API Initialized!'
  });
});
//utilizzo configurazione router
app.use('/api', router);
//starts server
app.listen(port, function () {
  console.log('api running on port ${port}');
});


router.route('/get-config-data')
  .get(function (req, res) {

    console.log("get-config-data");


    /* SAMPLE OUTPUT


            "ARDUINO_ST_4201": {
                "contextBroker" : "orionUNIMI" , 
                "ip": "159.149.129.184",
                "port" : "1026",
                "entity-type" : "Temperature",
                "latitude" : "45.54",
                "longitude" : "91.21",

             },
        
    */

    var mysql = require('mysql');


    /* 
      var connection = mysql.createConnection({
        host     : '159.149.129.184',
        port     : 3306,
        user     : 'root',
        password : '!!orion__',
        database : 'contextbrokers'
      });
*/


    var connection = mysql.createConnection({
      host: 'localhost',
      user: 'root',
      password: 'pass',
      database: 'contextbrokers'
    });


    var configData = {};


    var query = "SELECT sensors.id, sensors.type AS entityType, sensors.kind, sensors.protocol,   sensors.longitude,   sensors.latitude, sensors.contextBroker, contextbroker.name, contextbroker.type, contextbroker.ip, contextbroker.port FROM sensors LEFT JOIN contextbroker ON sensors.contextBroker = contextbroker.name  ORDER BY id;"

    connection.connect();

    connection.query(query, function (err, rows, fields) {

      if (err) throw err;
      // if there is no error, you have the result
      // iterate for all the rows in result
      Object.keys(rows).forEach(function (key) {
        var row = rows[key];
        var entityData = {};
        //console.log(row)




        configData[row.id] = {};

        configData[row.id]["contextBroker"] = row.contextBroker;
        configData[row.id]["ip"] = row.ip;
        configData[row.id]["port"] = row.port;
        configData[row.id]["type"] = row.entityType;
        configData[row.id]["latitude"] = row.latitude;
        configData[row.id]["longitude"] = row.longitude;
        configData[row.id]["kind"] = row.kind;

        configData[row.id]["protocol"] = row.protocol;



      });


      connection.end();

      res.json(configData);


    });

  });