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
$OPERATION = 'LIST';

include("../../session.php");
require_once ("../../config.php");

$db=mysqli_connect($db_host,$db_user,$db_pwd,$database) or die("DB connection error ");

$deleteFilter = 'deleted IS NULL';
if(isset($_REQUEST['includeDeleted'])) {
  $deleteFilter = '1';
}

$onlyMine = false;
if (isset($_REQUEST['onlyMine']) && $_REQUEST['onlyMine']=='true')
  $onlyMine=true;

$filter = '';
if(isset($_REQUEST['type'])) {
  $types = explode(";", $_REQUEST['type']);
  $ctypes = array();
  foreach($types as $t)
    $ctypes[] = mysqli_escape_string($db,$t);
  $filter = "AND elementType IN ('".implode("','",$ctypes)."') ";
}

if(isset($_REQUEST['elementId'])) {
  $elIds = explode(";", $_REQUEST['elementId']);
  $ids = array();
  foreach($elIds as $id)
    $ids[] = mysqli_escape_string($db,$id);
  $filter .= "AND elementId IN ('".implode("','",$ids)."') ";
}
if(isset($_REQUEST['pubkeySHA1'])) {
  $pubkeySHA1 = mysqli_escape_string($db, $_REQUEST['pubkeySHA1']);
  $filter .= "AND publickeySHA1 = '".$pubkeySHA1."' ";
}

if ($uinfo->mainRole == 'RootAdmin' && !$onlyMine) {
  $userFilter = "1 ";
} else {
  $userFilter = "username='" . mysqli_escape_string($db, $uinfo->username) . "' ";
}

$limit = '';
if (isset($_REQUEST['limit']) && is_numeric($_REQUEST['limit'])) {
  $limit = ' LIMIT ' . $_REQUEST['limit'];
}
$offset = '';
if (isset($_REQUEST['offset']) && is_numeric($_REQUEST['offset'])) {
  $offset = ' OFFSET ' . $_REQUEST['offset'];
}

if($limit || $offset) {
  $r=mysqli_query($db,"SELECT COUNT(*) FROM ownership WHERE $deleteFilter AND ".$userFilter.$filter) or die("query error: ".  mysqli_error($db));
  $o = mysqli_fetch_array($r);
  $count = $o[0];
}

header("Access-Control-Allow-Origin: *");
header("Content-type: application/json");

$r=mysqli_query($db,"SELECT * FROM ownership WHERE $deleteFilter AND ".$userFilter.$filter.$limit.$offset) or die("query error: ".  mysqli_error($db));
if(isset($count)) {
  echo '{ "count":'.$count.", \"data\":\n";
}
echo "[\n";
$i=0;
while($o=mysqli_fetch_object($r)) {
  if($i!=0)
    echo ',';
  unset($o->id);
  if($o->publickeySHA1==null)
    unset($o->publickeySHA1);
  if(!isset($_REQUEST['includeDeleted'])) {
    unset($o->deleted);
    unset($o->deletedBy);
  }
  if($o->elementDetails!=null) {
    $o->elementDetails=json_decode($o->elementDetails);
  }
  if(isset($_REQUEST['delegations'])) {
    $delegations = getElementDelegations($o->elementId, $o->elementType, $accessToken, isset($_REQUEST['includeWriteOnly']));
    $o->usersDelegations = $delegations['users'];
    $o->orgsDelegations = $delegations['orgs'];
    $o->grpsDelegations = $delegations['grps'];
  }
  if(isset($encryptionMethod)) {
    $o->uid = encryptOSSL($o->username);
  }
  echo json_encode($o)."\n";
  $i++;
}
echo "]\n";

if(isset($count)) {
  echo "}";
}

ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'type'=>isset($_REQUEST['type']) ? $_REQUEST['type'] : 'any','role'=>$uinfo->mainRole,'result'=>'SUCCESS']);
