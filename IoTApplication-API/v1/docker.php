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

function new_nodered($db,$uid) {
  if($r=mysqli_query($db, "SELECT COUNT(*) as napps FROM application WHERE uid='$uid' AND (status='RUNNING' OR status='STOPPED')") or die(mysqli_error($db))) {
    if($o=mysqli_fetch_object($r)) {
      if($o->napps >= 10) {
        echo "{error:\"cannot create you reached maximum number\"}";
        return;
      }
    }
  }
  
  mysqli_query($db, "INSERT INTO application(uid,status) VALUES($uid,'PRE-CREATE')");
  $aid=mysqli_insert_id($db);
  $name = "nr$aid";

  //find host
  $host="192.168.0.86";
  //find free port
  $port=10000;
  if($r=mysqli_query($db, "SELECT MAX(ext_port) as maxport FROM application WHERE host='$host' AND status='RUNNING' OR status='STOPPED'") or die(mysqli_error($db))) {
    if($o=mysqli_fetch_object($r)) {
      if($o->maxport>0)
        $port=$o->maxport+1;
    }
  }
  
  $id=new_docker_container($host,"nodered/node-red-docker:latest",1880,$port,$name);
  if($id) {
    $result=http_post("http://$host:3000/v1.30/containers/$id/start", "", "application/json");
    if($result["httpcode"]==204) {
      mysqli_query($db, "UPDATE application SET name='$name',ext_port='$port',container_id='$id',host='$host' WHERE id=$aid") or die(mysqli_error($db));
      $url = "http://www.snap4city.org/nodered/$name";
      mysqli_query($db, "UPDATE application SET status='RUNNING',status_description='' WHERE id=$aid") or die(mysqli_error($db));
      echo "{\"id\":\"$aid\",\"url\":\"$url\"}";
    } else {
      $msg = $result["result"]["message"];
      $result = http_delete("http://$host:3000/v1.30/containers/$id");
      if($result["httpcode"]!=204) {
        $msg .= " - failed container delete ".$result["result"]["message"];
      }
      mysqli_query($db, "UPDATE application SET status='ERROR',status_description='cannot start container $msg' WHERE id=$aid") or die(mysqli_error($db));
      echo "{error:\"cannot start container $msg\"}";
    }
  } else {
    mysqli_query($db, "UPDATE application SET status='ERROR',status_description='cannot create container' WHERE id=$aid") or die(mysqli_error($db));
    echo "{error:\"cannot create container\"}";
  }
}

function stop_nodered($db,$uid,$aid) {
  if($r=mysqli_query($db, "SELECT container_id,host FROM application WHERE id='$aid' AND uid='$uid' AND status='RUNNING'") or die(mysqli_error($db))) {
    if($o=mysqli_fetch_object($r)) {
      $host = $o->host;
      $id= $o->container_id;
      $result = http_post("http://$host:3000/v1.30/containers/$id/stop","","");
      if($result["httpcode"]==204) {
        mysqli_query($db, "UPDATE application SET status='STOPPED',status_description='' WHERE id=$aid") or die(mysqli_error($db));
        echo "{}";
      } else {
        $msg = $result["result"]["message"];
        echo "{error:\"cannot stop container $msg\"}";
      }
    } else {
        echo "{error:\"cannot find running container $aid for user $uid\"}";      
    }
  }
}

function start_nodered($db,$uid,$aid) {
  if($r=mysqli_query($db, "SELECT container_id,host FROM application WHERE id='$aid' AND uid='$uid' AND status='STOPPED'") or die(mysqli_error($db))) {
    if($o=mysqli_fetch_object($r)) {
      $host = $o->host;
      $id= $o->container_id;
      $result = http_post("http://$host:3000/v1.30/containers/$id/start","","");
      if($result["httpcode"]==204) {
        mysqli_query($db, "UPDATE application SET status='RUNNING',status_description='' WHERE id=$aid") or die(mysqli_error($db));
        echo "{}";
      } else {
        $msg = $result["result"]["message"];
        echo "{error:\"cannot stop container $msg\"}";
      }
    } else {
        echo "{error:\"cannot find stopped container $aid for user $uid\"}";      
    }
  }  
}

function rm_nodered($db,$uid,$aid) {
  if($r=mysqli_query($db, "SELECT container_id,host FROM application WHERE id='$aid' AND uid='$uid'") or die(mysqli_error($db))) {
    if($o=mysqli_fetch_object($r)) {
      $host = $o->host;
      $id= $o->container_id;
      $result = http_delete("http://$host:3000/v1.30/containers/$id?force=true");
      if($result["httpcode"]==204) {
        mysqli_query($db, "UPDATE application SET status='REMOVED',status_description='removed container $id' WHERE id=$aid") or die(mysqli_error($db));
        echo "{}";
      } else {
        $msg = $result["result"]["message"];
        echo "{error:\"cannot remove container $msg\"}";
      }
    } else {
        echo "{error:\"cannot find container $aid for user $uid\"}";      
    }
  }  
}

function new_docker_container($host,$image, $port, $ext_port, $name) {
  $json = '{
    "AttachStdin": false,
    "AttachStdout": true,
    "AttachStderr": true,
    "Tty": false,
    "OpenStdin": false,
    "StdinOnce": false,
    "Image": "'.$image.'",
    "ExposedPorts": {
      "'.$port.'/tcp": {}
    },
    "HostConfig": {
      "Binds": [
        "/root/datadirs/'.$name.':/data"
      ],
      "PortBindings": {
        "'.$port.'/tcp": [
          {
            "HostPort": "'.$ext_port.'"
          }
        ]
      },
      "RestartPolicy": {
        "Name": "always"
      }
    }
  }';
  $result = http_post("http://$host:3000/v1.30/containers/create?name=$name",$json, "application/json");
  if($result["httpcode"]==201)
    return $result["result"]["Id"];
  else {
    var_dump("result");
    return "";
  }
}
