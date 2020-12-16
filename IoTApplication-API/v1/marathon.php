<?php
/* Snap4city IOT Application API
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

function new_nodered($aid, $uname, $aname, $image) {
  include '../config.php';
  
  //prepare the /data dir for nodered
  $out = array();
  $ret = null;
  exec($nodered_script.' '.$aid.' '.$uname, $out, $ret);

  $result=new_marathon_nodered_container($marathon_url,$image,$aid,$uname);
  if($result && !isset($result['error'])) {
    $did = "";
    if(is_string($result)) {
      $did = $result;
    }
    else if(count($result["tasks"])==0){
      $did = $result["deployments"][0]["id"];
    } else {
      $id = $result["tasks"][0]["id"];
    }
    /*
    $proxy='        location  /nodered/'.$name.'/ {
                proxy_set_header Host $http_host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;
                proxy_http_version 1.1;
                proxy_set_header Upgrade $http_upgrade;
                proxy_set_header Connection "upgrade";
                proxy_pass "http://iot-app.snap4city.org/nodered/'.$name.'/";
        }';
    $f=fopen("/mnt/data/proxy/nr/$name.conf","w");
    if($f) {
      fwrite($f,$proxy);
      fclose($f);
    }
    */
    
    $url = "$apps_base_url/nodered/$aid";
    return array("id"=>$aid,"url"=>$url,'name'=>$aname);
  } else {
    return array('error'=>"cannot create container ".$result['error']);
  }
}

function new_plumber($aid, $uname, $aname, $image, $r_file, $health) {
  include '../config.php';
  
  //prepare the /data dir for nodered
  $out = array();
  $ret = null;
  exec($plumber_script.' '.$aid.' '.$r_file,$out,$ret);
  
  $result=new_marathon_plumber_container($marathon_url, $image, $aid, $uname, $health);
  if($result && !isset($result['error'])) {
    $did = "";
    if(is_string($result)) {
      $did = $result;
    }
    else if(count($result["tasks"])==0){
      $did = $result["deployments"][0]["id"];
    } else {
      $id = $result["tasks"][0]["id"];
    }
    
    $url = "$apps_base_url/plumber/$aid";
    return array("id"=>$aid,"url"=>$url,'name'=>$aname);
  } else {
    return array('error'=>"cannot create container ".$result['error']);
  }
}

function new_python($aid, $uname, $aname, $image, $py_file, $health) {
  include '../config.php';
  
  //prepare the /data dir for nodered
  $out = array();
  $ret = null;
  exec($python_script.' '.$aid.' '.$py_file,$out,$ret);
  
  $result=new_marathon_python_container($marathon_url, $image, $aid, $uname, $health);
  if($result && !isset($result['error'])) {
    $did = "";
    if(is_string($result)) {
      $did = $result;
    }
    else if(count($result["tasks"])==0){
      $did = $result["deployments"][0]["id"];
    } else {
      $id = $result["tasks"][0]["id"];
    }
    
    $url = "$apps_base_url/python/$aid";
    return array("id"=>$aid,"url"=>$url,'name'=>$aname);
  } else {
    return array('error'=>"cannot create container ".$result['error']);
  }
}

function new_portia($aid, $uname, $aname, $image, $health) {
  include '../config.php';
  
  //prepare the /data dir for portia
  $out = array();
  $ret = null;
  exec($portia_script.' '.$aid, $out, $ret);
  
  $result=new_marathon_portia_container($marathon_url, $image, $aid, $uname, $health);
  if($result && !isset($result['error'])) {
    $did = "";
    if(is_string($result)) {
      $did = $result;
    }
    else if(count($result["tasks"])==0){
      $did = $result["deployments"][0]["id"];
    } else {
      $id = $result["tasks"][0]["id"];
    }
    
    $url = "$apps_base_url/portia/$aid";
    return array("id"=>$aid,"url"=>$url,'name'=>$aname);
  } else {
    return array('error'=>"cannot create container ".$result['error']);
  }
}

function run_portia_crawler($aid, $project, $spider, $postTo,  $image) {
  include '../config.php';
  
  //prepare the /data dir for portia
  //$out = array();
  //$ret = null;
  //exec($portia_script.' '.$aid, $out, $ret);
  
  $result=run_marathon_portia_crawler($marathon_url, $image, $aid, $project, $spider, $postTo);
  if($result && !isset($result['error'])) {
    $did = "";
    if(is_string($result)) {
      $did = $result;
    }
    else if(count($result["tasks"])==0){
      $did = $result["deployments"][0]["id"];
    } else {
      $id = $result["tasks"][0]["id"];
    }
    
    return array("id"=>$aid."-crawler");
  } else {
    return array('error'=>"cannot create container ".$result['error']);
  }
}

function stop_nodered($uid,$aid) {
}

function start_nodered($uid,$aid) {
}

function rm_app($uid,$aid) {
  include '../config.php';
  
  $result = http_delete($marathon_url."/v2/apps/".$aid);
  if($result["httpcode"]==200) {
    echo json_encode($result["result"]);
  }
  else {
    echo "{error:\"failed removal of app $aid\"}";
  }  
}

function restart_app($uid,$aid) {
  include '../config.php';
  $result = http_post($marathon_url."/v2/apps/".$aid."/restart","","application/json");
  if($result["httpcode"]==200) {
    echo json_encode($result["result"]);
  }
  else {
    echo "{error:\"failed restart of app $aid\"}";
  }  
}

function upgrade_app($uid, $aid, $app) {
  include '../config.php';
  //var_dump($app);
  $image = '';
  switch($app['elementDetails']['type']) {
    case 'basic':
      $image = $nodered_basic_img;
      break;
    case 'advanced':
      $image = $nodered_adv_img;
      break;
    case 'plumber':
      $image = $plumber_img;
      break;
    case 'portia':
      $image = $portia_img;
      break;
    default:
      //error
      echo "{error:\"failed upgrade of app $aid invalid app type '".$app['elementDetails']['type']."'\"}";
      return;
  }
  
  if($app['elementDetails']['image'] == $image) {
    echo "{error:\"app $aid already uses image '".$image."'\"}";
    return;
  }
  // update image on ownership
  $app['elementDetails']['image'] = $image;
  
  if(isset($_REQUEST['accessToken'])) {
    $result = http_post($ownership_api_url."/v1/register/?accessToken=".$_REQUEST['accessToken'], json_encode($app), "application/json");
    //var_dump($result);
  } else if(isset($_REQUEST['username'])){
    $result = http_post($ownership_api_url."/v1/register/?username=".$_REQUEST['username'], json_encode($app), "application/json");
    //var_dump($result);
  }
  if($result['httpcode']!=200) {
    echo "{error:\"failed upgrade of app $aid - failed update of ownership\"}";
    return;
  }
  
  // update image on disces_em
  update_image_on_disces_em($aid, $image);
  
  // delete app on marathon only so disces_em will restart it with the new image
  $result = http_delete($marathon_url."/v2/apps/".$aid);
  if($result["httpcode"]==200) {
    echo json_encode($result["result"]);
  }
  else {
    echo "{error:\"failed upgrade of app $aid\"}";
  }  
}

function status_app($uid,$aid) {
  include '../config.php';
  $result = http_get($marathon_url."/v2/apps/".$aid);
  if($result["httpcode"]==200) {
    $r = array();
    $alive = NULL;
    if(count(@$result['result']['app']['healthChecks'])>0)
      $alive=false;
    @$r['healthiness']=$result['result']['app']['tasks'][0]['healthCheckResults'][0]['alive'] ?: $alive;
    if($result['result']['app']['tasksRunning']==0) {
      $r['healthiness']=false;
      $r['lastTaskFailure']=$result['result']['app']['lastTaskFailure'];
    }
    echo json_encode($r);
  }
  else {
    echo "{error:\"failed access to status for app $aid\"}";
  }  
}

function new_marathon_nodered_container($base_url,$image, $id, $uname) {
  include '../config.php';

  $healthChecks = '';
  if(!isset($noderedDisableHealthCheck) || !$noderedDisableHealthCheck) {
    $healthChecks = '    "healthChecks": [{
        "maxConsecutiveFailures": '.$appHealthChecksMaxConsecutiveFailures.', 
        "protocol": "MESOS_HTTP", 
        "portIndex": 0, 
        "gracePeriodSeconds": '.$appHealthChecksGracePeriodSeconds.' , 
        "path": "/nodered/'.$id.'/ui", 
        "timeoutSeconds": '.$appHealthChecksTimeoutSeconds.', 
        "intervalSeconds": '.$appHealthChecksIntervalSeconds.'}],';
  }
  $json = '{
    "id": "'. $id .'",
    "cmd": "npm start -- --userDir /data",
    "container": {
        "type": "DOCKER", 
        "docker": {
            "network": "HOST", 
            "image": "'. $image .'"
        },
        "volumes": [
        {
            "containerPath": "/data",
            "hostPath": "/mnt/data/nr-data/'. $id .'",
            "mode": "RW"
        }
    ]        
    },
    "cpus": '.$nodered_cpu.', 
    "portDefinitions": [{"name": null, "protocol": "tcp", "port": 0, "labels": null}],
    "instances": 1,
    "constraints": [["@hostname", "UNLIKE", "mesos[1-3]t"]],
    "env": {}, 
    "mem": '.$nodered_mem.', 
    "disk": 128, 
'.$healthChecks.' 
    "appId": "'. $id .'"
}';

  $log=fopen($logDir."/marathon.log","a");
  fwrite($log,date('c')." ".$json."\n\n");
  $result = http_post($base_url."/v2/apps",$json, "application/json");
  fwrite($log,date('c')." ".var_export($result,true)); 
  
  if($result["httpcode"]==201) {
    store_on_disces_em($id, $json);
    return $result["result"];
  }
  else if($result["httpcode"]==200) {
    store_on_disces_em($id, $json);
    return $result["result"]["deploymentId"];
  } else {
    return array('error'=>$result["httpcode"]." ".$result["result"]);
  }
  return "";
}

function new_marathon_plumber_container($base_url,$image, $id, $uname, $health) {
  include '../config.php';

  $healthCheck = '';
  if($health!=NULL) {
    $healthCheck ='    "healthChecks": [{
        "maxConsecutiveFailures": '.$appHealthChecksMaxConsecutiveFailures.', 
        "protocol": "MESOS_HTTP", 
        "portIndex": 0, 
        "gracePeriodSeconds": '.$appHealthChecksGracePeriodSeconds.' , 
        "path": "/'.$health.'", 
        "timeoutSeconds": '.$appHealthChecksTimeoutSeconds.', 
        "intervalSeconds": '.$appHealthChecksIntervalSeconds.'}],';
  }
  
  $json = '{
    "id": "'. $id .'",
    "args": ["/data/plumber.R"],
    "container": {
        "type": "DOCKER", 
        "docker": {
            "image": "'. $image .'"
        },
        "volumes": [
          {
            "containerPath": "/data",
            "hostPath": "/mnt/data/plumber/'. $id .'",
            "mode": "RW"
          }],
        "portMappings": [
            {
              "containerPort": 8000,
              "hostPort": 0,
              "labels": {},
              "name": "http",
              "protocol": "tcp",
              "servicePort": 10040
            }
        ]    
    },
    "cpus": '.$plumber_cpu.', 
    "networks": [
      {
      "mode": "container/bridge"
      }
    ],
    "portDefinitions": [],
    "instances": 1,
    "constraints": [["@hostname", "UNLIKE", "mesos[1-3]t"]],
    "env": {}, 
    "mem": '.$plumber_mem.', 
    "disk": 128,
    '.$healthCheck.'
    "appId": "'. $id .'"
}';
  $log=fopen($logDir."/marathon.log","a");
  fwrite($log,date('c')." ".$json."\n\n");
  $result = http_post($base_url."/v2/apps", $json, "application/json");
  fwrite($log,date('c')." ".var_export($result,true)); 
  if($result["httpcode"]==201) {
    store_on_disces_em($id, $json);
    return $result["result"];
  }
  else if($result["httpcode"]==200) {
    store_on_disces_em($id, $json);
    return $result["result"]["deploymentId"];
  } else {
    return array('error'=>$result["httpcode"]." ".var_export($result["result"],true));
  }
  return "";
}

function new_marathon_python_container($base_url,$image, $id, $uname, $health) {
  include '../config.php';

  $healthCheck = '';
  if($health!=NULL) {
    $healthCheck ='    "healthChecks": [{
        "maxConsecutiveFailures": '.$appHealthChecksMaxConsecutiveFailures.', 
        "protocol": "MESOS_HTTP", 
        "portIndex": 0, 
        "gracePeriodSeconds": '.$appHealthChecksGracePeriodSeconds.' , 
        "path": "/'.$health.'", 
        "timeoutSeconds": '.$appHealthChecksTimeoutSeconds.', 
        "intervalSeconds": '.$appHealthChecksIntervalSeconds.'}],';
  }
  
  $json = '{
    "id": "'. $id .'",
    "cmd": "python3 /data/daScript.py",
    "container": {
        "type": "DOCKER", 
        "docker": {
            "image": "'. $image .'"
        },
        "volumes": [
          {
            "containerPath": "/data",
            "hostPath": "/mnt/data/python/'. $id .'",
            "mode": "RW"
          }],
        "portMappings": [
            {
              "containerPort": 8080,
              "hostPort": 0,
              "labels": {},
              "name": "http",
              "protocol": "tcp",
              "servicePort": 10040
            }
        ]    
    },
    "cpus": '.$python_cpu.', 
    "networks": [
      {
      "mode": "container/bridge"
      }
    ],
    "portDefinitions": [],
    "instances": 1,
    "constraints": [["@hostname", "UNLIKE", "mesos[1-3]t"]],
    "env": {}, 
    "mem": '.$python_mem.', 
    "disk": 128,
    '.$healthCheck.'
    "appId": "'. $id .'"
}';
  $log=fopen($logDir."/marathon.log","a");
  fwrite($log,date('c')." ".$json."\n\n");
  $result = http_post($base_url."/v2/apps", $json, "application/json");
  fwrite($log,date('c')." ".var_export($result,true)); 
  if($result["httpcode"]==201) {
    store_on_disces_em($id, $json);
    return $result["result"];
  }
  else if($result["httpcode"]==200) {
    store_on_disces_em($id, $json);
    return $result["result"]["deploymentId"];
  } else {
    return array('error'=>$result["httpcode"]." ".var_export($result["result"],true));
  }
  return "";
}

function new_marathon_portia_container($base_url,$image, $id, $uname, $health) {
  include '../config.php';

  $healthCheck = '';
  if($health!=NULL) {
    $healthCheck ='    "healthChecks": [{
        "maxConsecutiveFailures": '.$appHealthChecksMaxConsecutiveFailures.', 
        "protocol": "MESOS_HTTP", 
        "portIndex": 0, 
        "gracePeriodSeconds": '.$appHealthChecksGracePeriodSeconds.' , 
        "path": "/'.$health.'", 
        "timeoutSeconds": '.$appHealthChecksTimeoutSeconds.', 
        "intervalSeconds": '.$appHealthChecksIntervalSeconds.'}],';
  }
  
  $json = '{
    "id": "'. $id .'",
    "cmd": "/app/entry '. $id .' ' . $uname .'",
    "container": {
        "type": "DOCKER", 
        "docker": {
            "image": "'. $image .'"
        },
        "volumes": [
          {
            "containerPath": "/app/data/projects",
            "hostPath": "/mnt/data/portia/'. $id .'/projects",
            "mode": "RW"
          }],
        "portMappings": [
            {
              "containerPort": 9001,
              "hostPort": 0,
              "labels": {},
              "name": "http",
              "protocol": "tcp",
              "servicePort": 10040
            }
        ]    
    },
    "cpus": '.$portia_cpu.', 
    "networks": [
      {
      "mode": "container/bridge"
      }
    ],
    "portDefinitions": [],
    "instances": 1,
    "constraints": [["@hostname", "UNLIKE", "mesos[1-3]t"]],
    "env": {}, 
    "mem": '.$portia_mem.', 
    "disk": 128,
    '.$healthCheck.'
    "appId": "'. $id .'"
}';
  $log=fopen($logDir."/marathon.log","a");
  fwrite($log,date('c')." ".$json."\n\n");
  $result = http_post($base_url."/v2/apps", $json, "application/json");
  fwrite($log,date('c')." ".var_export($result,true)); 
  if($result["httpcode"]==201) {
    store_on_disces_em($id, $json);
    return $result["result"];
  }
  else if($result["httpcode"]==200) {
    store_on_disces_em($id, $json);
    return $result["result"]["deploymentId"];
  } else {
    return array('error'=>$result["httpcode"]." ".var_export($result["result"],true));
  }
  return "";
}

function run_marathon_portia_crawler($base_url, $image, $portia_id, $project, $spider, $postTo) {
  include '../config.php';
  
  $outFile = $spider .'.json';
  $postResult = '';
  if(isset($postTo) && $postTo) {
    $postResult = ' ; curl -X POST --header \\"Content-Type:application/json\\" -d @/mnt/'.$outFile.' '.$postTo .' >> /mnt/post.log';
  }
  
  $id = $portia_id.'crawler';
  $json = '{
    "id": "'. $id .'",
    "cmd": "rm /mnt/'. $outFile .' ; portiacrawl /app/data/projects/'.$project.' '.$spider.' -o /mnt/'. $outFile. $postResult .' ; curl -X DELETE '.$base_url.'/v2/apps/'.$id.'",
    "container": {
        "type": "DOCKER", 
        "docker": {
            "image": "'. $image .'"
        },
        "volumes": [
          {
            "containerPath": "/app/data/projects",
            "hostPath": "/mnt/data/portia/'. $portia_id .'/projects",
            "mode": "RW"
          },{
            "containerPath": "/mnt",
            "hostPath": "/mnt/data/portia/'. $portia_id .'/outs",
            "mode": "RW"
          }],
        "portMappings": [
            {
              "containerPort": 9001,
              "hostPort": 0,
              "labels": {},
              "name": "http",
              "protocol": "tcp",
              "servicePort": 10040
            }
        ]    
    },
    "cpus": '.$portia_crawler_cpu.', 
    "networks": [
      {
      "mode": "container/bridge"
      }
    ],
    "portDefinitions": [],
    "instances": 1,
    "constraints": [["@hostname", "UNLIKE", "mesos[1-3]t"]],
    "env": {}, 
    "mem": '.$portia_crawler_mem.', 
    "disk": 128,
    "appId": "'. $id .'"
}';
  $log=fopen($logDir."/marathon.log","a");
  fwrite($log,date('c')." ".$json."\n\n");
  $result = http_post($base_url."/v2/apps", $json, "application/json");
  fwrite($log,date('c')." ".var_export($result,true)); 
  if($result["httpcode"]==201) {
    //store_on_disces_em($id, $json);
    return $result["result"];
  }
  else if($result["httpcode"]==200) {
    //store_on_disces_em($id, $json);
    return $result["result"]["deploymentId"];
  } else {
    return array('error'=>$result["httpcode"]." ".var_export($result["result"],true));
  }
  return "";
}

function random_str($length, $keyspace = '0123456789abcdefghijklmnopqrstuvwxyz')
{
    $pieces = [];
    $max = strlen($keyspace) - 1;
    for ($i = 0; $i < $length; ++$i) {
        $pieces []= $keyspace[random_int(0, $max)];
    }
    return implode('', $pieces);
}
