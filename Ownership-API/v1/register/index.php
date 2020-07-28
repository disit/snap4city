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

$postdata = file_get_contents("php://input");
if($postdata==NULL) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "no data via POST";
  ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>'NO POST']);
  exit;  
}
$o = json_decode($postdata);
if($o==NULL) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "invalid JSON format";
  ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>'invalid JSON format']);
  exit;  
}

if(!is_object($o)) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "no JSON object";
  ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>'no JSON object']);
  exit;    
}

if(isset($o->elementDetails)) {
  $elementDetails = $o->elementDetails;
  $o->elementDetails=json_encode($o->elementDetails);
}

//check id manadtory attrs are present
$mandatoryAttrs = array('elementId','elementType','elementName');
foreach($mandatoryAttrs as $a) {
  if(!isset($o->$a)) {
    header("HTTP/1.1 400 BAD REQUEST");
    echo '{"error":"missing '.$a.' attribute"}';
    ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>'missing '.$a.' attribute']);
    exit;  
  }
}

//check if it is of a valid type
$validTypes = array('AppID','IOTID','ServiceURI','ServiceGraphID','DashboardID','DAAppID','BrokerID','ModelID','PortiaID','HeatmapID','DeviceGroupID','SynopticTmplID','SynopticID');
if(!in_array($o->elementType,$validTypes)) {
    header("HTTP/1.1 400 BAD REQUEST");
    echo '{"error":"invalid elementType '.$o->elementType.'"}';
    ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>"invalid elementType '".$o->elementType."'"]);
    exit;    
}
$optionalAttrs = array('elementUrl','elementDetails');

$db=mysqli_connect($db_host,$db_user,$db_pwd,$database) or die("DB connection error ");

//check if it is already present
if($uinfo->mainRole=='RootAdmin')
  $userFilter = "1 ";
else
  $userFilter = "username='".mysqli_escape_string($db, $uinfo->username)."' ";

$q = "SELECT username FROM ownership WHERE ".$userFilter.
        " AND elementId='".mysqli_escape_string($db, $o->elementId).
        "' AND elementType='".mysqli_escape_string($db, $o->elementType)."' AND deleted IS NULL";

$r = mysqli_query($db, $q) or die(mysqli_error($db));
$idxAttrs = array(array('publickey','publickeySHA1'));
if($r && $c=mysqli_fetch_array($r)) {
  //update
  $curUsername = $c[0];
  //check username is valid
  if(isset($o->username) && ($urole = get_user_role($o->username))=='') { 
    header("HTTP/1.1 400 BAD REQUEST");
    echo '{"error":"invalid username '.$o->username.' attribute"}';
    ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>'invalid username '.$o->username.' attribute']);
    exit;
  }
  
  //check if changing ownership exceeds the limit for the new user
  if(isset($o->username) && $curUsername!=$o->username) {
    $org = get_user_organization($o->username);
    list($limit,$qry) = get_limit_user($db, $org, $o->username, $urole, $o->elementType);

    $q = "SELECT count(*) as count FROM ownership WHERE username='".mysqli_escape_string($db, $o->username)."'".
        " AND elementType='".mysqli_escape_string($db, $o->elementType)."' AND deleted is NULL";
    $r = mysqli_query($db, $q);
    if($r && ($c=mysqli_fetch_array($r)) && $c[0]>=$limit) {
      header("HTTP/1.1 400 BAD REQUEST");
      echo '{"error":"reached limit of '.$limit.'","limit":'.$limit.',"current":'.$c[0].'}';
      ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>$o->username."@$org $urole: ".$o->elementType." reached limit ".$c[0]."/$limit"]);
      exit;          
    }
  }
  //check if it is registering an edge iotapp
  if(isset($_REQUEST['wstunnel']) && $_REQUEST['wstunnel']=='true' && 
          $o->elementType=='AppID' && isset($elementDetails->edgegateway_type)) {       
      $wstunnel_result = 'wstun: ' . wsTunnelRegisterClient($o);
  }  
  //start update data
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
    ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>" UPDATE ownnership - query error ".  mysqli_error($db)]);
    exit;
  }
  
  //if changing ownership change also the delegations to the new user
  if(isset($o->username) && $curUsername!=$o->username) {
    $update = "UPDATE delegation SET username_delegator='".mysqli_escape_string($db, $o->username)."'"
            . " WHERE element_id='".mysqli_escape_string($db, $o->elementId)."'"
            . " AND element_type='".mysqli_escape_string($db, $o->elementType)."'"
            . " AND delete_time IS NULL";
    if(!mysqli_query($db,$update)) {
      ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>" UPDATE delegation - query error ".  mysqli_error($db)]);
      exit;
    }    
  }
} else { //insert data
  $org = get_user_organization($uinfo->username);
  list($limit,$qry) = get_limit_user($db, $org, $uinfo->username, $uinfo->mainRole, $o->elementType);

  $q = "SELECT count(*) as count FROM ownership WHERE username='".mysqli_escape_string($db, $uinfo->username)."'".
      " AND elementType='".mysqli_escape_string($db, $o->elementType)."' AND deleted is NULL";
  $r = mysqli_query($db, $q);
  if($r && ($c=mysqli_fetch_array($r)) && $c[0]<$limit) {
    $o->username=$uinfo->username;

    //check if it is registering an edge iotapp
    if(isset($_REQUEST['wstunnel']) && $_REQUEST['wstunnel']=='true' && 
            $o->elementType=='AppID' && isset($elementDetails->edgegateway_type)) {       
      $wstunnel_result = 'wstun: ' . wsTunnelRegisterClient($o);
    }

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
      ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>"INSERT ownership - query error ".  mysqli_error($db)]);
      exit;
    }
    $id=mysqli_insert_id($db);
    $o->id=$id;
  } else {
    header("HTTP/1.1 400 BAD REQUEST");
    echo '{"error":"reached limit of '.$limit.'","limit":'.$limit.',"current":'.$c[0].'}';
    ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>" for $org:".$o->elementType." reached limit ".$c[0]."/$limit"]);
    exit;          
  }  
}

if(isset($o->elementDetails)) {
  $o->elementDetails = json_decode($o->elementDetails);
}
echo json_encode($o);

ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'type'=>$o->elementType,'id'=>$o->elementId,'result'=>'SUCCESS '.$wstunnel_result]);
