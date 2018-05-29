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

include("../../session.php");
include("../../config.php");

//echo 'register '.$uinfo->username;
$postdata = file_get_contents("php://input");
if($postdata==NULL) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "no data via POST";
  exit;  
}
$o = json_decode($postdata);
if($o==NULL) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "invalid JSON data";
  exit;  
}

if(!is_object($o)) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "no JSON object";
  exit;    
}

if(isset($o->elementDetails)) {
  $o->elementDetails=json_encode($o->elementDetails);
}
$mandatoryAttrs = array('elementId','elementType','elementName');
foreach($mandatoryAttrs as $a) {
  if(!isset($o->$a)) {
    header("HTTP/1.1 400 BAD REQUEST");
    echo '{"error":"missing '.$a.' attribute"}';
    exit;  
  }
}
$validTypes = array('AppID',"IOTID","ServiceURI","ServiceGraphID","DashboardID");
if(!in_array($o->elementType,$validTypes)) {
    header("HTTP/1.1 400 BAD REQUEST");
    echo '{"error":"invalid elementType '.$o->elementType.'"}';
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
        "' AND elementType='".mysqli_escape_string($db, $o->elementType)."'";

$r = mysqli_query($db, $q) or die(mysqli_error($db));
if($r && $c=mysqli_fetch_array($r)) {
  if($c[0]>0) {
    //update
    $attrs = array('username','elementName','elementUrl','elementDetails');
    $sets = array();
    foreach($attrs as $a) {
      if(isset($o->$a))
        $sets[] = "$a='".mysqli_escape_string($db, $o->$a)."'";
    }

    $update = "UPDATE ownership SET ".join(",",$sets)." WHERE ".$userFilter.
        " AND elementId='".mysqli_escape_string($db, $o->elementId).
        "' AND elementType='".mysqli_escape_string($db, $o->elementType)."'";
    mysqli_query($db,$update) or die("query error ".  mysqli_error($db));

  } else {
    $o->username=$uinfo->username;
    //insert
    $attrs =  array_merge(array('username'), $mandatoryAttrs, $optionalAttrs);
    $values = array();
    foreach($attrs as $a) {
      if(!isset($o->$a))
        $values[] = "NULL";
     else
        $values[] = "'".mysqli_escape_string($db, $o->$a)."'";
    }

    $insert = "INSERT INTO ownership(".join(",", $attrs).") VALUES (".join(",",$values).")";

    $r=mysqli_query($db,$insert) or die("query error ".  mysqli_error($db));
    $id=mysqli_insert_id($db);
    $o->id=$id;
  }
}
$o->elementDetails = json_decode($o->elementDetails);
echo json_encode($o);
