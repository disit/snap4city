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
$OPERATION = 'LIMITS';

include("../../session.php");
require_once("../../config.php");

$log=fopen("/tmp/owner.log","a");
fwrite($log, date('c')." limits\n");
//echo 'register '.$uinfo->username;
if(!isset($_REQUEST['type']) || strlen(trim($_REQUEST['type']))==0) {
  $_REQUEST['type'] = 'DashboardID;AppID;IOTID';
}

$types = explode(";", $_REQUEST['type']);

$db=mysqli_connect($db_host,$db_user,$db_pwd,$database) or die("DB connection error ");

$org = get_organization_user($uinfo->username);

$o = array();
foreach($types as $elementType) {
  list($limit,$qry) = get_limit_user($db, $org, $uinfo->username, $uinfo->mainRole, $elementType);
    
  $q = "SELECT count(*) as count FROM ownership WHERE username='".mysqli_escape_string($db, $uinfo->username)."'".
          " AND elementType='".mysqli_escape_string($db, $elementType)."' AND deleted is NULL";
  $r = mysqli_query($db, $q);
  if($r && ($c=mysqli_fetch_array($r))) {
    $o[] = array('elementType'=>$elementType, 'limit'=>$limit, 'current'=>$c[0]);
  }
}
echo json_encode(array('username'=>$uinfo->username, 'role'=>$uinfo->mainRole, 'organization'=>$org, 'limits'=>$o));

ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'type'=>$elementType]);
