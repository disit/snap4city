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


function new_nodered($db,$uname, $name) {
  if($r=mysqli_query($db, "SELECT COUNT(*) as napps FROM application WHERE uname='$uname' AND (status='CREATED')") or die(mysqli_error($db))) {
    if($o=mysqli_fetch_object($r)) {
      if($o->napps >= 10) {
        echo "{error:\"cannot create you reached maximum number\"}";
        return;
      }
    }
  }
  
  mysqli_query($db, "INSERT INTO application(uname,status) VALUES('$uname','PRE-CREATE')");
  $aid=mysqli_insert_id($db);
  $id = "nr$aid";

  $result=new_kubernates_nodered_container("192.168.1.30","disitlab/tst-dkr:003",$id);
  if($result) {
    $ext_port=$result["service"]["spec"]["ports"][0]["nodePort"];

    $proxy='        location  /nodered/'.$id.' {
                proxy_set_header Host $http_host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;
                proxy_http_version 1.1;
                proxy_set_header Upgrade $http_upgrade;
                proxy_set_header Connection "upgrade";
                proxy_pass "http://192.168.1.35:'.$ext_port.'";
        }';
    $f=fopen("/mnt/data/proxy/nr/$id.conf","w");
    if($f) {
      fwrite($f,$proxy);
      fclose($f);
    }
    //TBD force reload nginx
    
    mysqli_query($db, "UPDATE application SET name='nodered-$id',ext_port=$ext_port,status='CREATED',status_description='' WHERE id=$aid") or die(mysqli_error($db));
    $url = "http://iot-app.snap4city.org/nodered/$id";
    return array("id"=>$aid,"url"=>$url,'name'=>$name);
  } else {
    mysqli_query($db, "UPDATE application SET status='ERROR',status_description='cannot create container' WHERE id=$aid") or die(mysqli_error($db));
    return array("error"=>"cannot create container");
  }  
}

function new_plumber($db,$uid) {
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
  $name = "plumber$aid";

  $result=  new_marathon_plumber_container("192.168.1.187",null,$name);
  if($result) {
    $did = "";
    if(is_string($result)) {
      mysqli_query($db, "UPDATE application SET status='DEPLOYING', name='$name',deployment_id='$result' WHERE id=$aid") or die(mysqli_error($db));
    }
    else if(count($result["tasks"])==0){
      $did = $result["deployments"][0]["id"];
      mysqli_query($db, "UPDATE application SET status='DEPLOYING', name='$name',deployment_id='$did' WHERE id=$aid") or die(mysqli_error($db));
    } else {
      var_dump($result);
      $id = $result["tasks"][0]["id"];
      mysqli_query($db, "UPDATE application SET status='RUNNING', name='$name',container_id='$id' WHERE id=$aid") or die(mysqli_error($db));
    }
    $url = "http://www.snap4city.org/plumber/$name";
    echo "{\"id\":\"$aid\",\"url\":\"$url\"}";
  } else {
    mysqli_query($db, "UPDATE application SET status='ERROR',status_description='cannot create container' WHERE id=$aid") or die(mysqli_error($db));
    echo "{error:\"cannot create container\"}";
  }  
}

function stop_app($db,$uid,$aid) {
  if($r=mysqli_query($db, "SELECT container_id,host FROM application WHERE id='$aid' AND uid='$uid' AND status='RUNNING'") or die(mysqli_error($db))) {
    if($o=mysqli_fetch_object($r)) {
    } else {
        echo "{error:\"cannot find running container $aid for user $uid\"}";      
    }
  }
}

function start_app($db,$uid,$aid) {
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

function restart_app($db,$uname,$aid) {
  if($r=mysqli_query($db, "SELECT name FROM application WHERE id='$aid' AND uname='$uname' AND status='CREATED'") or die(mysqli_error($db))) {
    if($o=mysqli_fetch_object($r)) {
      $name= $o->name;
      //retrive POD
      $result = http_get("http://192.168.1.30:8080/api/v1/namespaces/default/pods?labelSelector=app%3D$name");
      if($result["httpcode"]==200) {
        if(count($result["result"]["items"])==1) {
          $pod_name = $result["result"]["items"][0]["metadata"]["name"];

          //delete POD
          $result = http_delete("http://192.168.1.30:8080/api/v1/namespaces/default/pods/$pod_name");
          if($result["httpcode"]==200) {
            mysqli_query($db, "UPDATE application SET status='CREATED',status_description='restarted' WHERE id=$aid") or die(mysqli_error($db));
            echo "{}";
          } else {
            $msg = $result["result"]["message"];
            echo "{error:\"cannot restart app: $msg\"}";
          }
        } else {
          echo "{error:\"restart in progress\"}";                
        }
      } else {
        echo "{error:\"failed pod retrieval for app $name\"}";              
      }
    } else {
        echo "{error:\"cannot find app $aid for user $uname\"}";      
    }
  }  
}

function rm_app($db,$uname,$aid) {
  if($r=mysqli_query($db, "SELECT name FROM application WHERE id='$aid' AND uname='$uname'") or die(mysqli_error($db))) {
    if($o=mysqli_fetch_object($r)) {
      $name= $o->name;
      $result = http_delete("http://192.168.1.30:8080/apis/apps/v1beta1/namespaces/default/deployments/$name");
      if($result["httpcode"]==200) {
        mysqli_query($db, "UPDATE application SET status='REMOVED',status_description='removed app' WHERE id=$aid") or die(mysqli_error($db));
        echo "{}";
      } else {
        $msg = $result["result"]['message'];
        echo "{error:\"cannot remove app $msg\"}";
      }
    } else {
        echo "{error:\"cannot find app $aid for user $uname\"}";      
    }
  }  
}

function status_app($db,$uname,$aid) {
  if($r=mysqli_query($db, "SELECT name FROM application WHERE id='$aid' AND uname='$uname'") or die(mysqli_error($db))) {    
    if($o=mysqli_fetch_object($r)) {
      $result = http_get("http://192.168.1.30:8080/apis/apps/v1beta1/namespaces/default/deployments/".$o->name);
      if($result["httpcode"]==200) {
        echo json_encode($result["result"]["status"]);
      }
      else
        echo "{error:\"failed access to status for app $o->name\"}";
    } else {
        echo "{error:\"cannot find app $aid for user $uname\"}";
    }
  }  
}

function new_kubernates_nodered_container($host,$image, $id) {
  $name = "nodered-$id";
  $json = '{
	"apiVersion": "apps/v1beta1",
	"kind": "Deployment",
	"metadata": {
		"name": "'.$name.'"
	},
	"spec": {
		"replicas": 1,
		"template": {
			"metadata": {
				"labels": {
					"app": "'.$name.'"
				}
			},
			"spec": {
				"containers": [{
					"name": "'.$name.'",
					"image": "'.$image.'",
          "command": ["/bin/sh"],
          "args":  ["-c", "npm start -- --userDir /data"],
            "ports": [
              {
                "containerPort": 1880,
                "protocol": "TCP"
              }
            ],
					"volumeMounts": [{
						"mountPath": "/data",
						"name": "volume"
					}],
          "livenessProbe": {
            "httpGet": {
              "path": "/nodered/'.$id.'/ui",
              "port": 1880
            },
            "initialDelaySeconds": 60,
            "periodSeconds": 20
          }
				}],
				"volumes": [{
					"name": "volume",
					"hostPath": {
						"path": "/mnt/data/nr-data/'.$id.'",
						"type": "DirectoryOrCreate"
					}
				}],
				"portMappings": [{
					"port": 1880,
					"targetPort": 1880,
					"protocol": "TCP"
				}]
			}
		}
	}
  }';
  $out = array();
  $ret = null;
  exec("/home/ubuntu/add-nodered.sh $id",$out,$ret);
  
  $result = http_post("http://$host:8080/apis/apps/v1beta1/namespaces/default/deployments",$json, "application/json");
  if($result["httpcode"]==201) {
    //create service
    $service_json='{
      "kind": "Service",
      "apiVersion": "v1",
      "metadata": {
        "name": "'.$name.'",
        "namespace": "default",
        "labels": {"name": "'.$name.'-service"}
      },
      "spec": {
        "type": "NodePort",
        "ports": [{"port": 1880}],
        "selector": {"app": "'.$name.'"}
      }
    }';
    $s_result = http_post("http://$host:8080/api/v1/namespaces/default/services",$service_json,"application/json");
    if($s_result["httpcode"]==201) {
      return array("deploy"=>$result["result"],"service"=>$s_result["result"]);
    }
    else {
      echo $service_json;
      var_dump($s_result);
      return "";
    }
  }
  else {
    echo $json;
    var_dump($result);
    return "";
  }
  return "";
}

function new_marathon_plumber_container($host,$image, $id) {
  $json = '{
    "cmd": "Rscript /root/Snap4City/Snap4CityStatistics/RunRestApi.R",
    "id": "'. $id .'",
    "labels": {
      "HAPROXY_GROUP":"external",
      "HAPROXY_0_VHOST":"nr.snap4city.org",
      "HAPROXY_0_PATH":"/plumber/'. $id .'/",
      "HAPROXY_0_HTTP_BACKEND_PROXYPASS_PATH":"/plumber/'. $id .'/"
    },
    "container": {
        "type": "DOCKER", 
        "docker": {
            "network": "BRIDGE", 
            "image": "disit/plumber:version7",
            "portMappings": [
              {
                "containerPort": 8080,
                "hostPort": 0,
                "protocol": "tcp",
                "name": "http"
              }]
        },
        "volumes": [
        {
            "containerPath": "/root",
            "hostPath": "/mnt/R",
            "mode": "RW"
        }
    ]        
    },
    "cpus": 0.095, 
    "portDefinitions": [{"name": null, "protocol": "tcp", "port": 0, "labels": null}],
    "instances": 1, 
    "env": {}, 
    "mem": 140, 
    "disk": 128, 
    "healthChecks": [{
        "maxConsecutiveFailures": 0, 
        "protocol": "HTTP", 
        "portIndex": 0, 
        "gracePeriodSeconds": 240, 
        "path": "/sum?a=1&b=1", 
        "timeoutSeconds": 10, 
        "intervalSeconds": 15}], 
    "appId": "'. $id .'"
}';
  $out = array();
  $ret = null;
  //exec("/home/ubuntu/add-nodered.sh $id",$out,$ret);
  $result = http_post("http://$host:8080/v2/apps",$json, "application/json");
  if($result["httpcode"]==201)
    return $result["result"];
  else if($result["httpcode"]==200)
    return $result["result"]["deploymentId"];
  else {
    var_dump($result);
    return "";
  }
  return "";
}
