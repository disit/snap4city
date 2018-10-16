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
$OPERATION = 'DELETE';

include("../../session.php");
require_once("../../config.php");

$db=mysqli_connect($db_host,$db_user,$db_pwd,$database) or die("DB connection error ");

$filter = '';
if(!isset($_REQUEST['type'])) {
  header("HTTP/1.1 400 BAD REQUEST");  
  exit;
}
$types = explode(";", $_REQUEST['type']);
$ctypes = array();
foreach($types as $t)
  $ctypes[] = mysqli_escape_string($db,$t);
$filter = "AND elementType IN ('".implode("','",$ctypes)."') ";

if(!isset($_REQUEST['elementId'])) {
  header("HTTP/1.1 400 BAD REQUEST");  
  exit;
}
$elIds = explode(";", $_REQUEST['elementId']);
$ids = array();
foreach($elIds as $id)
  $ids[] = mysqli_escape_string($db,$id);
$filter .= "AND elementId IN ('".implode("','",$ids)."') ";
  
if($uinfo->mainRole=='RootAdmin')
  $userFilter = "1 ";
else
  $userFilter = "username='".mysqli_escape_string($db, $uinfo->username)."' ";

$r=mysqli_query($db,"UPDATE ownership SET deleted=CURRENT_TIMESTAMP, deletedBy='".mysqli_escape_string($db, $uinfo->username)."' WHERE ".$userFilter.$filter) or die("query error: ".  mysqli_error($db));
$nDeleted = mysqli_affected_rows($db);
if($nDeleted==0)
  header("HTTP/1.1 400 BAD REQUEST");
echo "{ \"deleted\": ".$nDeleted."}\n";

ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'type'=> $_REQUEST['type'],'id'=> $_REQUEST['elementId']]);
