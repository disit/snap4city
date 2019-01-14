<?php
/* Snap4city Ownership API
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
$OPERATION = 'REGISTER';

include("../../session.php");
require_once("../../config.php");

$log=fopen("/tmp/owner.log","a");
fwrite($log, date('c')." register\n");
//echo 'register '.$uinfo->username;
$postdata = file_get_contents("php://input");
if($postdata==NULL) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "no data via POST";
  fwrite($log,date('c')." NO POST\n");
  exit;  
}
$o = json_decode($postdata);
if($o==NULL) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "invalid JSON data";
  fwrite($log,date('c')." invalid JSON data\n");
  exit;  
}

if(!is_object($o)) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "no JSON object";
  fwrite($log,date('c')." NO JSON\n");

  exit;    
}

if(isset($o->elementDetails)) {
  $elementDetails = $o->elementDetails;
  $o->elementDetails=json_encode($o->elementDetails);
}
$mandatoryAttrs = array('elementId','elementType','elementName');
foreach($mandatoryAttrs as $a) {
  if(!isset($o->$a)) {
    header("HTTP/1.1 400 BAD REQUEST");
    echo '{"error":"missing '.$a.' attribute"}';
    fwrite($log,date('c')." missing '".$a."' attribute\n");
    exit;  
  }
}
$validTypes = array('AppID',"IOTID","ServiceURI","ServiceGraphID","DashboardID",'DAAppID');
if(!in_array($o->elementType,$validTypes)) {
    header("HTTP/1.1 400 BAD REQUEST");
    echo '{"error":"invalid elementType '.$o->elementType.'"}';
    fwrite($log,date('c')." invalid elementType '".$o->elementType."'");
    exit;    
}
$optionalAttrs = array('elementUrl','elementDetails');

$db=mysqli_connect($db_host,$db_user,$db_pwd,$database) or die("DB connection error ");

if($uinfo->mainRole=='RootAdmin')
  $userFilter = "1 ";
else
  $userFilter = "username='".mysqli_escape_string($db, $uinfo->username)."' ";

$q = "SELECT count(*) as count FROM ownership WHERE ".$userFilter.
        " AND elementId='".mysqli_escape_string($db, $o->elementId).
        "' AND elementType='".mysqli_escape_string($db, $o->elementType)."' AND deleted IS NULL";

$r = mysqli_query($db, $q) or die(mysqli_error($db));
if($r && $c=mysqli_fetch_array($r)) {
  $idxAttrs = array(array('publickey','publickeySHA1'));
  if($c[0]>0) { //update data
    //update
    //check username is valid
    if(isset($o->username) && get_user_role($o->username)=='') { 
      header("HTTP/1.1 400 BAD REQUEST");
      echo '{"error":"invalid username '.$o->username.' attribute"}';
      fwrite($log,date('c')." invalid username '".$o->username."' attribute\n");
      exit;
    }
    $attrs = array('username','elementName','elementUrl','elementDetails');
    $sets = array();
    foreach($attrs as $a) {
      if(isset($o->$a))
        $sets[] = "$a='".mysqli_escape_string($db, $o->$a)."'";
    }
    foreach($idxAttrs as $a) {
      $a1 = $a[0];
      $a2 = $a[1];
      if(isset($o->elementDetails)) {
        if(isset($elementDetails->$a1))
          $sets[] = "$a2=SHA1('".mysqli_escape_string($db, $elementDetails->$a1)."')";    
      }
    }

    $update = "UPDATE ownership SET ".join(",",$sets)." WHERE ".$userFilter.
        " AND elementId='".mysqli_escape_string($db, $o->elementId).
        "' AND elementType='".mysqli_escape_string($db, $o->elementType)."' AND deleted IS NULL";
    if(!mysqli_query($db,$update)) {
      fwrite($log,date('c')." UPDATE ownnership - query error ".  mysqli_error($db));
      exit;
    }
  } else { //insert data
    $org = get_user_organization($uinfo->username);
    list($limit,$qry) = get_limit_user($db, $org, $uinfo->username, $uinfo->mainRole, $o->elementType);
    
    $q = "SELECT count(*) as count FROM ownership WHERE username='".mysqli_escape_string($db, $uinfo->username)."'".
        " AND elementType='".mysqli_escape_string($db, $o->elementType)."' AND deleted is NULL";
    $r = mysqli_query($db, $q);
    if($r && ($c=mysqli_fetch_array($r)) && $c[0]<$limit) {
      $o->username=$uinfo->username;
      //insert
      $attrs =  array_merge(array('username'), $mandatoryAttrs, $optionalAttrs);
      $values = array();
      foreach($attrs as $a) {
        if(isset($o->$a))
          $values[] = "'".mysqli_escape_string($db, $o->$a)."'";
         else
          $values[] = "NULL";
      }
      foreach($idxAttrs as $a) {
        $a1 = $a[0];
        $a2 = $a[1];
        if(isset($o->elementDetails)) {
          if(isset($elementDetails->$a1)) {
            $attrs[] = $a2;
            $values[] = "SHA1('".mysqli_escape_string($db, $elementDetails->$a1)."')";
          }
        }
      }

      $insert = "INSERT INTO ownership(".join(",", $attrs).") VALUES (".join(",",$values).")";

      if(!mysqli_query($db,$insert)) {
        fwrite($log,date('c')." INSERT ownnership - query error ".  mysqli_error($db));
        exit;
      }
      $id=mysqli_insert_id($db);
      $o->id=$id;
    } else {
      header("HTTP/1.1 400 BAD REQUEST");
      echo '{"error":"reached limit of '.$limit.'","limit":'.$limit.',"current":'.$c[0].'}';
      fwrite($log,date('c')." for $org,".$uinfo->username.",".$uinfo->mainRole.":".$o->elementType." reached limit ".$c[0]."/$limit --".mysqli_error($db)."\n");
      exit;          
    }
  }
}
if(isset($o->elementDetails)) {
  $o->elementDetails = json_decode($o->elementDetails);
}
echo json_encode($o);

ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'type'=>$o->elementType,'id'=>$o->elementId]);
