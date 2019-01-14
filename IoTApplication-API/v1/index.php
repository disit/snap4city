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

$db=mysqli_connect($db_host,$db_user,$db_pwd,$database) or die("DB connection error ");

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
  case 'new_nodered':
    if(isset($_REQUEST['type']) && ($_REQUEST['type']=='basic' || $_REQUEST['type']=='advanced'))
      $type = $_REQUEST['type'];
    if(strlen($uname)>0 && strlen($name)>0 && $type) {
      if($type=='basic')
        $image = $nodered_basic_img;
      else
        $image = $nodered_adv_img;
      $aid = $nrapp_id_prefix . random_str($app_id_length);
      $app = array("id"=>$aid,"url"=>"https://iot-app.snap4city.org/nodered/$aid",'name'=>$name, 'type'=>$type, 'image'=>$image);
      $r=register_app($app);
      if($r['httpcode']!=200) {
          header("HTTP/1.1 400 BAD REQUEST");
          $app=array("error"=>$r['result']);
      } else {
        $r=new_nodered($db, $aid, $uname, $name, $image);
        if(isset($r['error'])) {
          $app = $r;
        }
      }
      echo json_encode($app);
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{error:\"invalid user name '$uname' or app name '$name' or type '$type'\"}";
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
            $app = array("id"=>$aid,"url"=>"https://iot-app.snap4city.org/plumber/$aid", 'name'=>$name, 'type'=>'plumber', 'image'=>$image, 'health'=>$health, 'iotappid' => $iotappid);
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
            echo "{error:\"missing R_file\"}";      
          }
        } else {
            header("HTTP/1.1 400 BAD REQUEST");
            echo "{error:\"use POST method\"}";                
        }
      }
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{error:\"invalid user name '$uname' or app name '$name' or missing id\"}";
    }      
    break;
  case 'restart_app':
    if(strlen($uname)>0 && strlen($aid)>0) {
      $r=get_app($aid);
      if($r['httpcode']==200 && count($r['result'])>0) {
        restart_app($db,$uname,$aid);
      }
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{error:\"invalid uname or id\"}";
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
          rm_app($db,$uname,$aid);
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
      echo "{error:\"invalid uname or id\"}";
    }
    break;
  case 'status':
    if(strlen($uname)>0 && strlen($aid)>0) {
      status_app($db,$uname,$aid);
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo "{error:\"invalid uname or id\"}";
    }
    break;
  default:
    header("HTTP/1.1 400 BAD REQUEST");
    echo "{error: \"operation '$op' is not valid\"}";
}

function register_app($app) {
  include "../config.php";
  
  $element=array();
  $element['elementName'] = $app['name'];
  $element['elementType'] = $app['type']=='plumber' ? 'DAAppID' : 'AppID';
  $element['elementUrl'] = $app['url'];
  $element['elementId'] = $app['id'];
  $element['elementDetails']=array('type'=>$app['type'],'image'=>$app['image']);
  if(isset($app['health']))
    $element['elementDetails']['health'] = $app['health'];
  if(isset($app['iotappid']) || @$app['iotappid']===NULL) {
    $element['elementDetails']['iotappids'] = $app['iotappid']===NULL ? array() : array($app['iotappid']);
  }
  
  if(isset($_REQUEST['accessToken'])) {
    $result = http_post($ownership_api_url."/v1/register/?accessToken=".$_REQUEST['accessToken'], json_encode($element), "application/json");
    //var_dump($result);
  } else if(isset($_REQUEST['username'])){
    $result = http_post($ownership_api_url."/v1/register/?username=".$_REQUEST['username'], json_encode($element), "application/json");
    //var_dump($result);
  }
  return $result;
}

function get_app($appid) {
  include "../config.php";

  if(isset($_REQUEST['accessToken'])) {
    $result = @http_get($ownership_api_url."/v1/list/?type=AppID;DAAppID&elementId=$appid&accessToken=".$_REQUEST['accessToken']);
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
  $db=mysqli_connect($disces_em,$disces_em_user,$disces_em_pwd,$disces_em_database) or die("DB connection error ");
  $q = "INSERT INTO marathon_apps(app,json) VALUES ('".$id."','".mysqli_escape_string($db, $json)."')";
  mysqli_query($db, $q) or die("error disces-em ".  mysqli_error($db));
}

function remove_from_disces_em($id) {
  include '../config.php';
  
  if(substr($id,0,1)!='/')
    $id = '/'.$id;
  $db=mysqli_connect($disces_em,$disces_em_user,$disces_em_pwd,$disces_em_database) or die("DB connection error ");
  $q = "DELETE FROM marathon_apps WHERE app='".$id."'";
  mysqli_query($db, $q) or die("error disces-em ".  mysqli_error($db));
}