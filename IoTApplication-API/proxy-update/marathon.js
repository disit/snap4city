"use strict";
/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


var http = require('http');
var fs = require('fs');
var ChildProcess = require('child_process');
var tasks = {};
var EventSource = require('eventsource');
var es = new EventSource('http://192.168.1.187:8080/v2/events');
var lastSave = null;
var delayedSave = null;

es.addEventListener('status_update_event', function (e) {
  var data = JSON.parse(e.data);
  //console.log(data);
  if(data.taskStatus=="TASK_RUNNING") {
    var path = "nodered"+data.appId;
    if(data.appId.startsWith("/nrr-test-"))
      path = "nodered/nrt"+Number(data.appId.substr(10));
    tasks[data.appId] = { "host": data.host, "port": data.ports[0], "alive":false, "taskId": data.taskId, "path": path};
    console.log(new Date()+" "+data.appId+" RUNNING");
    saveConf(false);
  } else if(data.taskStatus=="TASK_KILLED") {
    if(tasks.hasOwnProperty(data.appId) && data.taskId==tasks[data.appId].taskId) {
      console.log(new Date()+" "+data.appId+" KILLED");
      delete tasks[data.appId];
      saveConf(false);
    }
  } else {
    console.log(new Date()+" "+data.appId+" "+data.taskStatus);
  }
});
es.addEventListener('health_status_changed_event', function (e) {
  var data = JSON.parse(e.data);
  console.log(new Date()+" "+data.appId+" ALIVE: "+data.alive);
  if(tasks.hasOwnProperty(data.appId) && data.instanceId==tasks[data.appId].taskId) {
    tasks[data.appId].alive = data.alive;
  }
});

updateConf();

function updateConf() {
  http.get('http://192.168.1.187:8080/v2/tasks', function(resp) {
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
        if(appId.startsWith("/nrr-test-")) {
          path = "nodered/nrt"+Number(appId.substr(10));
        }
        tasks[appId] = {"host": host, "port": port, "alive":alive, "taskId": taskId, "path": path};
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
    conf += "location  /"+path+"/ {\n"+
        "  proxy_set_header Host $http_host;\n"+
        "  proxy_set_header X-Real-IP $remote_addr;\n"+
        "  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"+
        "  proxy_set_header X-Forwarded-Proto $scheme;\n"+
        "  proxy_http_version 1.1;\n"+
        "  proxy_set_header Upgrade $http_upgrade;\n"+
        "  proxy_set_header Connection \"upgrade\";\n"+
        "  proxy_pass \"http://"+host+":"+port+"/"+path+"/\";\n"+
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
