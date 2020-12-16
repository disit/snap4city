"use strict";
/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


var http = require('http');
var fs = require('fs');
var ChildProcess = require('child_process');
var mysql = require('mysql');

let rawcfgdata = fs.readFileSync('config.json');
let config = JSON.parse(rawcfgdata);
console.log(new Date()+" START with config\n"+JSON.stringify(config, null, 2));

var nginxProxyTimeout = config.nginxProxyTimeout || 1800;
var marathon_url = config.marathonUrl || "http://localhost:8080";

var db = mysql.createConnection({
  host: config.dbHost || "localhost",
  user: config.dbUser || "user",
  password: config.dbPassw || "password",
  database: config.dbSchema || "marathonproxy"
});

db.connect(function(err) {
  if (err) throw err;
  console.log("Connected!");
  saveEvent("","START");
});

var tasks = {};
var EventSource = require('eventsource');
var es = new EventSource(marathon_url+'/v2/events');
var lastSave = null;
var delayedSave = null;

es.addEventListener('status_update_event', function (e) {
  var data = JSON.parse(e.data);
  //console.log(data);
  if(data.taskStatus=="TASK_RUNNING") {
    var path = "nodered"+data.appId;
    if(data.appId.startsWith("/nrr-test-"))
      path = "nodered/nrt"+Number(data.appId.substr(10));
    var path2 = path;
    if(data.appId.startsWith("/pl")) {
      path = "plumber"+data.appId;
      path2 = "";
    } else if(data.appId.startsWith("/pt")) {
      path = "portia"+data.appId;
      path2 = path;
    } else if(appId.startsWith("/py")) {
      path = "python"+appId;
      path2 = "";
    }

    tasks[data.appId] = { "host": data.host, "port": data.ports[0], "alive":false, "taskId": data.taskId, "path": path, "path2": path2};
    console.log(new Date()+" "+data.appId+" RUNNING ON "+data.host+":"+data.ports[0]);
    saveConf(false);
    saveEvent(data.appId,data.taskStatus,data.host,data.ports[0]);
  } else if(data.taskStatus=="TASK_KILLED") {
    if(tasks.hasOwnProperty(data.appId) && data.taskId==tasks[data.appId].taskId) {
      console.log(new Date()+" "+data.appId+" KILLED");
      delete tasks[data.appId];
      saveConf(false);
      saveEvent(data.appId,data.taskStatus);
    }
  } else {
    console.log(new Date()+" "+data.appId+" "+data.taskStatus);
    saveEvent(data.appId,data.taskStatus);
  }
});

es.addEventListener('health_status_changed_event', function (e) {
  var data = JSON.parse(e.data);
  console.log(new Date()+" "+data.appId+" ALIVE: "+data.alive);
  if(tasks.hasOwnProperty(data.appId) && data.instanceId==tasks[data.appId].taskId) {
    tasks[data.appId].alive = data.alive;
  }
  saveEvent(data.appId,"ALIVE:"+data.alive);
});

updateConf();

function updateConf() {
  http.get(marathon_url+'/v2/tasks', function(resp) {
    var data = '';

    // A chunk of data has been recieved.
    resp.on('data', function(chunk) {
      data += chunk;
    });

    // The whole response has been received. Print out the result.
    resp.on('end', function() {
      var tsks = JSON.parse(data).tasks;
      tsks.forEach(function(t) {
        //console.log(t);
        var appId = t.appId;
        var alive = false;
        if(t.healthCheckResults[0])
          alive=t.healthCheckResults[0].alive;
        var host = t.host;
        //var ipAddr = t.ipAddresses[0];
        var port = t.ports[0];
        var taskId = t.id;
        var path = "nodered"+appId;
        var path2 = path;
        if(appId.startsWith("/pl")) {
          path = "plumber"+appId;
          path2 = "";
        }
        if(appId.startsWith("/py")) {
          path = "python"+appId;
          path2 = "";
        }
        if(appId.startsWith("/pt")) {
          path = "portia"+appId;
          path2 = path;
        }
        if(appId.startsWith("/nrr-test-")) {
          path = "nodered/nrt"+Number(appId.substr(10));
          path2 = path;
        }
        tasks[appId] = {"host": host, "port": port, "alive":alive, "taskId": taskId, "path": path, "path2": path2};
      });
      //console.log(tasks);
      console.log(new Date()+" UPDATE CONF "+tsks.length);
      saveConf(true);
      //setTimeout(updateConf,60000);
    });
  }).on("error", function(err) {
    console.log("Error: " + err.message);
  });
}

function saveConf(force) {
  if(delayedSave!=null)
    clearTimeout(delayedSave);
  delayedSave=null;
  if(!force && lastSave!=null && (new Date()-lastSave)<2000) {
    console.log(new Date()+" SKIP SAVE");
    delayedSave=setTimeout(function() {
      console.log(new Date()+" DELAYED SAVE");
      saveConf(true);
    },5000);
    return;
  }
  var conf = '#updated '+new Date()+'\n\n';
  var count = 0;

  for(var t in tasks) {
    var appId=t;
    var alive = tasks[t].alive;
    var host = tasks[t].host;
    var port = tasks[t].port;
    var path = tasks[t].path;
    var path2 = tasks[t].path2;
    if(path2!="")
      path2 += "/";

    conf += "location  /"+path+"/ {\n"+
        "  proxy_set_header Host $http_host;\n"+
        "  proxy_set_header X-Real-IP $remote_addr;\n"+
        "  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"+
        "  proxy_set_header X-Forwarded-Proto $scheme;\n"+
        "  proxy_http_version 1.1;\n"+
        "  proxy_set_header Upgrade $http_upgrade;\n"+
        "  proxy_set_header Connection \"upgrade\";\n"+
        "  proxy_connect_timeout "+nginxProxyTimeout+";\n"+
        "  proxy_send_timeout "+nginxProxyTimeout+";\n"+
        "  proxy_read_timeout "+nginxProxyTimeout+";\n"+
        "  send_timeout "+nginxProxyTimeout+";\n"+
        "  proxy_pass \"http://"+host+":"+port+"/"+path2+"\";\n"+
        "}\n\n";
    count++;
  }
  lastSave = new Date();
  if(process.argv.length>2) {
    fs.writeFile(process.argv[2], conf, function(err) {
      if(err) {
        return console.log(err);
      }
      console.log(process.argv[2]+" SAVED "+count);
      if(process.argv.length>3) {
        ChildProcess.exec(process.argv[3], function(err, stdout, stderr) {
          if (err) {
            // node couldn't execute the command
            console.log(err);
            return;
          }
          console.log(process.argv[3]+" : "+stdout+" "+stderr);
        });          
      }
    }); 
  } else {
    console.log(conf);    
  }
}
/*
function saveEvent(appId, eventType, host, port) {
db.connect(function(err) {
  if (err) throw err;
  console.log("Connected!");
  var sql = "INSERT INTO marathonevent (appId,eventType,host,port) VALUES ('"+appId+"', '"+eventType+"','"+host+"','"+port+"')";
  con.query(sql, function (err, result) {
    if (err) throw err;
    console.log("event logged");
  });
});
}


function saveEvent(appId, eventType, host, port) {
  if(!host)
    host="NULL"
  else
    host="'"+host+"'"
  if(!port)
    port="NULL"
  else
    port="'"+port+"'"
  db.connect(function(err) {
    if (err) throw err;
    console.log("Connected!");
    var sql = "INSERT INTO marathonevent (appId,eventType,host,port) VALUES ('"+appId+"', '"+eventType+"',"+host+","+port+")";
    db.query(sql, function (err, result) {
      if (err) throw err;
      console.log("1 record inserted");
    });
  });
}
*/

function saveEvent(appId, eventType, host, port) {
  if(!host)
    host="NULL"
  else
    host="'"+host+"'"
  if(!port)
    port="NULL"
  else
    port="'"+port+"'"
  var sql = "INSERT INTO marathonevent (appId,eventType,host,port) VALUES ('"+appId+"', '"+eventType+"',"+host+","+port+")";
  db.query(sql, function (err, result) {
    if (err) throw err;
    console.log("event logged");
  });
}

