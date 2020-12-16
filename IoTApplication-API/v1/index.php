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

header("Content-type: text/plain");
//phpinfo();
include '../config.php';
include '../http.php';
include 'marathon.php';
//include 'kubernates.php';

//check token
include '../session.php';

$op="";
$uname=$uinfo->username;
$aid='';
$name='';
$type='';

if(isset($_REQUEST['op']) && is_string($_REQUEST['op']))
  $op = $_REQUEST['op'];
if(isset($_REQUEST['name']) && is_string($_REQUEST['name']))
  $name = $_REQUEST['name'];
if(isset($_REQUEST['id']) && is_string($_REQUEST['id']))
  $aid = $_REQUEST['id'];
header("Access-Control-Allow-Origin: *");

switch ($op) {
  case 'list':
    echo json_encode(get_apps("AppID;DAAppID;PortiaID", false));
    break;
  case 'new_nodered':
    if(isset($_REQUEST['type']) && ($_REQUEST['type']=='basic' || $_REQUEST['type']=='advanced'))
      $type = $_REQUEST['type'];
    if(strlen($uname)>0 && strlen($name)>0 && $type) {
      if($type=='basic')
        $image = $nodered_basic_img;
      else
        $image = $nodered_adv_img;
      $aid = $nrapp_id_prefix . random_str($app_id_length);
      $app = array("id"=>$aid,"url"=>"$apps_base_url/nodered/$aid",'name'=>$name, 'type'=>$type, 'image'=>$image);
      $r=register_app($app);
      if($r['httpcode']!=200) {
          header("HTTP/1.1 400 BAD REQUEST");
          $app=array("error"=>$r['result']);
      } else {
        $r=new_nodered($aid, $uname, $name, $image);
        if(isset($r['error'])) {
          $app = $r;
        }
      }
      echo json_encode($app);
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{\"error\":\"invalid user name '$uname' or app name '$name' or type '$type'\"}";
    }      
    break;
  case 'new_plumber':
    if(strlen($uname)>0 && strlen($name)>0 && strlen($aid)>0) {
      //check if application is already present
      $r = get_app($aid);
      if($r['httpcode']==200 && count($r['result'])>0) {
        $a = $r['result'][0];
        $app = array("id"=>$a['elementId'],"url"=>$a['elementUrl'], 'name'=>$a['elementName'], 'type'=>$a['elementDetails']['type'], 'image'=>$a['elementDetails']['image'], 'health'=>@$a['elementDetails']['health'] ?: NULL, 'created'=>$a['created']);
        echo json_encode($app);            
      } else {
        if($_SERVER['REQUEST_METHOD']=='POST') {
          if(isset($_FILES['R_file']) && is_uploaded_file($_FILES['R_file']['tmp_name'])) {
            $r_file = $_FILES['R_file']['tmp_name'];
            $image = $plumber_img;
            //$aid = $plumber_id_prefix . random_str($app_id_length);
            $health = NULL;
            /*if(isset($_REQUEST['health'])) {
              $health = $_REQUEST['health'];
            }*/
            $iotappid = NULL;
            if(isset($_REQUEST['iotappid'])) {
              $iotappid = $_REQUEST['iotappid'];
            }
            $app = array("id"=>$aid,"url"=>"$apps_base_url/plumber/$aid", 'name'=>$name, 'type'=>'plumber', 'image'=>$image, 'health'=>$health, 'iotappid' => $iotappid);
            $r=register_app($app);
            if($r['httpcode']!=200) {
                $app=$r['result'];
                $app['error'] = 'failed register '.$app['error'];
            } else {
              $r=new_plumber($aid, $uname, $name, $image, $r_file, $health);
              if(isset($r['error'])) {
                $app = $r;
              }
            }
            echo json_encode($app);
          } else {
            header("HTTP/1.1 400 BAD REQUEST");
            echo "{\"error\":\"missing R_file\"}";      
          }
        } else {
            header("HTTP/1.1 400 BAD REQUEST");
            echo "{\"error\":\"use POST method\"}";                
        }
      }
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{\"error\":\"invalid user name '$uname' or app name '$name' or missing id\"}";
    }      
    break;
  case 'new_python':
    if(strlen($uname)>0 && strlen($name)>0 && strlen($aid)>0) {
      //check if application is already present
      $r = get_app($aid);
      if($r['httpcode']==200 && count($r['result'])>0) {
        $a = $r['result'][0];
        $app = array("id"=>$a['elementId'],"url"=>$a['elementUrl'], 'name'=>$a['elementName'], 'type'=>$a['elementDetails']['type'], 'image'=>$a['elementDetails']['image'], 'health'=>@$a['elementDetails']['health'] ?: NULL, 'created'=>$a['created']);
        echo json_encode($app);            
      } else {
        if($_SERVER['REQUEST_METHOD']=='POST') {
          if(isset($_FILES['PY_file']) && is_uploaded_file($_FILES['PY_file']['tmp_name'])) {
            $r_file = $_FILES['PY_file']['tmp_name'];
            $image = $python_img;
            //$aid = $plumber_id_prefix . random_str($app_id_length);
            $health = NULL;
            /*if(isset($_REQUEST['health'])) {
              $health = $_REQUEST['health'];
            }*/
            $iotappid = NULL;
            if(isset($_REQUEST['iotappid'])) {
              $iotappid = $_REQUEST['iotappid'];
            }
            $app = array("id"=>$aid,"url"=>"$apps_base_url/python/$aid", 'name'=>$name, 'type'=>'python', 'image'=>$image, 'health'=>$health, 'iotappid' => $iotappid);
            $r=register_app($app);
            if($r['httpcode']!=200) {
                $app=$r['result'];
                $app['error'] = 'failed register '.$app['error'];
            } else {
              $r= new_python($aid, $uname, $name, $image, $r_file, $health);
              if(isset($r['error'])) {
                $app = $r;
              }
            }
            echo json_encode($app);
          } else {
            header("HTTP/1.1 400 BAD REQUEST");
            echo "{\"error\":\"missing PY_file\"}";      
          }
        } else {
            header("HTTP/1.1 400 BAD REQUEST");
            echo "{\"error\":\"use POST method\"}";                
        }
      }
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{\"error\":\"invalid user name '$uname' or app name '$name' or missing id\"}";
    }      
    break;
  case 'new_portia':
    if(strlen($uname)>0 && strlen($name)>0) {
      $aid = $portia_id_prefix . random_str($app_id_length);
      $image = $portia_img;
      $app = array("id"=>$aid,"url"=>"$apps_base_url/portia/$aid",'name'=>$name, 'type'=>'portia', 'image'=>$image);
      $r=register_app($app);
      if($r['httpcode']!=200) {
          header("HTTP/1.1 400 BAD REQUEST");
          $app=array("error"=>$r['result']);
      } else {
        $r=new_portia($aid, $uname, $name, $image);
        if(isset($r['error'])) {
          $app = $r;
        }
      }
      echo json_encode($app);
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{\"error\":\"invalid user name '$uname' or app name '$name' or type '$type'\"}";
    }      
    break;
  case 'run_portia_crawler':
    if(!$aid) {
      $portia_apps = get_apps("PortiaID", "true"); //onlyMine
      if(count($portia_apps)==0) {
        header("HTTP/1.1 400 BAD REQUEST");
        echo "{\"error\":\"no portia app found for $uname \"}";        
        return;
      }
      $aid = $portia_apps[0]['id'];
    }
    if(strlen($uname)>0 && strlen($aid)>0) {
      //check if $aid is a portia instance of the user
      if(isset($_REQUEST['project']) && isset($_REQUEST['spider'])) {
        $project = $_REQUEST['project'];
        $spider = $_REQUEST['spider'];
        $postTo = '';
        if(isset($_REQUEST['postTo']))
          $postTo = $_REQUEST['postTo'];
        $r = run_portia_crawler($aid, $project, $spider, $postTo, $portia_crawler_img);
        echo json_encode($r);
      } else {
        header("HTTP/1.1 400 BAD REQUEST");
        echo "{\"error\":\"missing project and/or crawler parameters\"}";        
      }
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{\"error\":\"invalid user name '$uname' or app id '$aid'\"}";
    }
    break;
  case 'restart_app':
    if(strlen($uname)>0 && strlen($aid)>0) {
      $r=get_app($aid);
      if($r['httpcode']==200 && count($r['result'])>0) {
        restart_app($uname,$aid);
      }
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{\"error\":\"invalid uname or id\"}";
    }
    break;
  case 'upgrade_app':
    if(strlen($uname)>0 && strlen($aid)>0) {
      $r=get_app($aid);
      if($r['httpcode']==200 && count($r['result'])>0) {
        upgrade_app($uname,$aid,$r['result'][0]);
      }
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{\"error\":\"invalid uname or id\"}";
    }
    break;
  case 'rm_app':
    if(strlen($uname)>0 && strlen($aid)>0) {
      $r=get_app($aid);
      if($r['httpcode']==200 && count($r['result'])>0) {
        $type = $r['result'][0]['elementType'];
        $r=delete_app($aid, $type);
        if($r['httpcode']==200) {
          remove_from_disces_em($aid);
          rm_app($uname,$aid);
        } else {
          header("HTTP/1.1 400 BAD REQUEST");
          echo '{"error":"delete app id:'.$aid.' type:'.$type.' result:'.json_encode($r['result']).'}';
        }
      } else {
        header("HTTP/1.1 400 BAD REQUEST");
        echo '{"error":"get app id:'.$aid.' result:'.json_encode($r['result']).'}';
      }
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{\"error\":\"invalid uname or id\"}";
    }
    break;
  case 'status':
    if(strlen($uname)>0 && strlen($aid)>0) {
      status_app($uname,$aid);
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{\"error\":\"invalid uname or id\"}";
    }
    break;
  default:
    header("HTTP/1.1 400 BAD REQUEST");
    echo "{\"error\": \"operation '$op' is not valid\"}";
}

function register_app($app) {
  include "../config.php";
  
  switch($app['type']) {
    case 'plumber':
    case 'python':
      $type = 'DAAppID';
      break;
    case 'portia':
      $type = 'PortiaID';
      break;
    default:
      $type = 'AppID';
      break;
  }
  $element=array();
  $element['elementName'] = $app['name'];
  $element['elementType'] = $type;
  $element['elementUrl'] = $app['url'];
  $element['elementId'] = $app['id'];
  $element['elementDetails']=array('type'=>$app['type'],'image'=>$app['image']);
  if(isset($app['health']))
    $element['elementDetails']['health'] = $app['health'];
  if(isset($app['iotappid']) || @$app['iotappid']===NULL) {
    $element['elementDetails']['iotappids'] = @$app['iotappid']===NULL ? array() : array($app['iotappid']);
  }
  
  if(isset($_REQUEST['accessToken'])) {
    //echo json_encode($element);
    $result = http_post($ownership_api_url."/v1/register/?accessToken=".$_REQUEST['accessToken'], json_encode($element), "application/json");
    //var_dump($result);
  } else if(isset($_REQUEST['username'])){
    $result = http_post($ownership_api_url."/v1/register/?username=".$_REQUEST['username'], json_encode($element), "application/json");
    //var_dump($result);
  }
  return $result;
}

function get_apps($type = null, $onlyMine = null) {
  include "../config.php";

  if(!isset($type)) {
    $type = "AppID;DAAppID;PortiaID";
  }
  if(!isset($onlyMine)) {
    $onlyMine = "false";
  }
  
  $result = array();
  if(isset($_REQUEST['accessToken'])) {
    $r = @http_get($ownership_api_url."/v1/list/?type=$type&onlyMine=$onlyMine&accessToken=".$_REQUEST['accessToken']);
    if($r['httpcode']==200) {
      foreach ($r['result'] as $a) {
        $app = array(
            'id' => $a['elementId'],
            'name' => $a['elementName'],
            'url' => $a['elementUrl'],
            'created' => $a['created'],
            'username' => $a['username']);
        //var_dump($a);
        $app['modified'] = 'unknown';
        if(isset($a['elementDetails']['type'])) {
          $app['type'] = $a['elementDetails']['type'];
          $f = '';
          switch($app['type']) {
            case 'basic':
            case 'advanced':
              $f = '/mnt/data/nr-data/'.$app['id'].'/flows.json';
              break;
            case 'plumber':
              $f = '/mnt/data/plumber/'.$app['id'].'/plumber.R';
              break;
            case 'python':
              $f = '/mnt/data/python/'.$app['id'].'/service.py';
              break;
          }
          if($f) {
            if(file_exists($f)) {
              $app['modified'] = date("Y-m-d H:i:s.", filemtime($f));
            } else {
              $app['modified'] = 'never';
            }
          }
        }
        else if(isset($a['elementDetails']['edgegateway_type'])) {
          $app['type'] = 'edge';
          $app['edgetype'] = $a['elementDetails']['edgegateway_type'];
        } else {
          $app['type'] = 'basic';
        }
        if(isset($a['elementDetails']['image']))
          $app['image'] = $a['elementDetails']['image'];
        if(isset($a['elementDetails']['iotappids']))
          $app['iotapps'] = $a['elementDetails']['iotappids'];
        $result[] = $app;
      }
    }
    //var_dump($result);
  }
  return $result;
}

function get_app($appid) {
  include "../config.php";

  $result = array('httpcode'=>0);
  if(isset($_REQUEST['accessToken'])) {
    $result = @http_get($ownership_api_url."/v1/list/?type=AppID;DAAppID;PortiaID&elementId=$appid&accessToken=".$_REQUEST['accessToken']);
    //var_dump($result);
  }
  return $result;
}

function delete_app($appid, $type) {
  include "../config.php";

  if(isset($_REQUEST['accessToken'])) {
    $result = @http_get($ownership_api_url."/v1/delete/?type=$type&elementId=$appid&accessToken=".$_REQUEST['accessToken']);
    //var_dump($result);
  }
  return $result;
}

function store_on_disces_em($id,$json) {
  include '../config.php';
  
  if(substr($id,0,1)!='/')
    $id = '/'.$id;
  if($disces_em) {
    $db=mysqli_connect($disces_em,$disces_em_user,$disces_em_pwd,$disces_em_database) or die("DB disces connection error ");
    $q = "INSERT INTO marathon_apps(app,json) VALUES ('".mysqli_escape_string($db, $id)."','".mysqli_escape_string($db, $json)."')";
    mysqli_query($db, $q) or die("error disces-em ".  mysqli_error($db));
  }
}

function update_image_on_disces_em($id, $image) {
  include '../config.php';
  
  if(substr($id,0,1)!='/')
    $id = '/'.$id;
  if($disces_em) {
    $db=mysqli_connect($disces_em,$disces_em_user,$disces_em_pwd,$disces_em_database) or die("DB disces connection error ");
    $q = "UPDATE marathon_apps SET json=json_replace(json,'$.container.docker.image','".mysqli_escape_string($db, $image)."') WHERE app='".mysqli_escape_string($db, $id)."'";
    mysqli_query($db, $q) or die("error disces-em ".  mysqli_error($db));
  }
}

function remove_from_disces_em($id) {
  include '../config.php';
  
  if(substr($id,0,1)!='/')
    $id = '/'.$id;
  if($disces_em) {
    $db=mysqli_connect($disces_em,$disces_em_user,$disces_em_pwd,$disces_em_database) or die("DB connection error ");
    $q = "DELETE FROM marathon_apps WHERE app='".mysqli_escape_string($db, $id)."'";
    mysqli_query($db, $q) or die("error disces-em ".  mysqli_error($db));
  }
}