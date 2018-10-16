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
$aid=0;
$name='';
$type='';

if(isset($_REQUEST['op']) && is_string($_REQUEST['op']))
  $op = $_REQUEST['op'];
if(isset($_REQUEST['name']) && is_string($_REQUEST['name']))
  $name = $_REQUEST['name'];
if(isset($_REQUEST['id']) && is_string($_REQUEST['id']))
  $aid = $_REQUEST['id'];

switch ($op) {
  case 'new_nodered':
    if(isset($_REQUEST['type']) && ($_REQUEST['type']=='basic' || $_REQUEST['type']=='advanced'))
      $type = $_REQUEST['type'];
    if(strlen($uname)>0 && strlen($name)>0 && $type) {
      if($type=='basic')
        $image = $nodered_basic_img;
      else
        $image = $nodered_adv_img;
      $aid="nr".random_str($app_id_length);
      $app = array("id"=>$aid,"url"=>"https://iot-app.snap4city.org/nodered/$aid",'name'=>$name, 'type'=>$type, 'image'=>$image);
      $r=register_app($app);
      if($r['httpcode']!=200) {
          $app=array("error"=>$r['result']);
      } else {
        $r=new_nodered($db, $aid, $uname, $name, $image);
        if(isset($r['error'])) {
          $app = $r;
        }
      }
      echo json_encode($app);
    } else {
      echo "{error:\"invalid user name '$uname' or app name '$name' or type '$type'\"}";
    }      
    break;
  case 'new_plumber':
    if(strlen($uname)>0 && strlen($name)>0) {
      $app=new_plumber($db, $uname, $name, $plumber_img);
      if(!isset($app['error'])) {
        $app['type'] = 'plumber';
        $app['image'] = $plumber_img;
        $r=register_app($app);
        if($r['httpcode']!=200) {
          $app=array("error"=>$r['result']);
        }
      }
      echo json_encode($app);
    } else {
      echo "{error:\"invalid user name '$uname' or app name '$name'\"}";
    }      
    break;
    if($uid>0) {
      $id=new_plumber($db,$uid);
    } else {
      echo "{error:\"invalid uid $uid\"}";
    }      
    break;
  case 'stop_app':
    if(strlen($uname)>0 && strlen($aid)>0) {
      stop_app($db,$uname,$aid);
    } else {
      echo "{error:\"invalid uname or id\"}";
    }
    break;
  case 'start_app':
    if(strlen($uname)>0 && strlen($aid)>0) {
      start_app($db,$uname,$aid);
    } else {
      echo "{error:\"invalid uname or id\"}";
    }
    break;
  case 'restart_app':
    if(strlen($uname)>0 && strlen($aid)>0) {
      restart_app($db,$uname,$aid);
    } else {
      echo "{error:\"invalid uname or id\"}";
    }
    break;
  case 'rm_app':
    if(strlen($uname)>0 && strlen($aid)>0) {
      $r=delete_app($aid);
      if($r['httpcode']==200) {
        remove_from_disces_em($aid);
        rm_app($db,$uname,$aid);
      } else {
        echo '{"error":"'.json_encode($r['result']).'}';
      }
    } else {
      echo "{error:\"invalid uname or id\"}";
    }
    break;
  case 'status':
    if(strlen($uname)>0 && strlen($aid)>0) {
      status_app($db,$uname,$aid);
    } else {
      echo "{error:\"invalid uname or id\"}";
    }
    break;
  default:
    echo "{error: \"operation '$op' is not valid\"}";
}

function register_app($app) {
  include "../config.php";
  
  $element=array();
  $element['elementName']=$app['name'];
  $element['elementType']='AppID';
  $element['elementUrl']=$app['url'];
  $element['elementId']=$app['id'];
  $element['elementDetails']=array('type'=>$app['type'],'image'=>$app['image']);
  if(isset($_REQUEST['accessToken'])) {
    $result = http_post($ownership_api_url."/v1/register/?accessToken=".$_REQUEST['accessToken'], json_encode($element), "application/json");
    //var_dump($result);
  } else if(isset($_REQUEST['username'])){
    $result = http_post($ownership_api_url."/v1/register/?username=".$_REQUEST['username'], json_encode($element), "application/json");
    //var_dump($result);
  }
  return $result;
}

function delete_app($appid) {
  include "../config.php";

  if(isset($_REQUEST['accessToken'])) {
    $result = @http_get($ownership_api_url."/v1/delete/?type=AppID&elementId=$appid&accessToken=".$_REQUEST['accessToken']);
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