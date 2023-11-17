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

if(!isset($_REQUEST['type']) || strlen(trim($_REQUEST['type']))==0) {
  $_REQUEST['type'] = 'DashboardID;AppID;IOTID;DAAppID';
}

$types = explode(";", $_REQUEST['type']);

$db=mysqli_connect($db_host,$db_user,$db_pwd,$database) or die("DB connection error ");

if(isset($_REQUEST['username']) && $uinfo->mainRole=='RootAdmin') {
  $username = $_REQUEST['username'];
  $role = get_user_role($username);
  if(!$role) {
    header("HTTP/1.1 400 BAD REQUEST");
    echo "invalid username no role found for $username";
    ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'forUser'=>$username, 'result'=>'NO_ROLE']);
    exit;  
  }
} else {
  $username = $uinfo->username;
  $role = $uinfo->mainRole;
}

$org = get_user_organization($username);

$o = array();
foreach($types as $elementType) {
  list($limit,$qry) = get_limit_user($db, $org, $username, $role, $elementType);
    
  $q = "SELECT count(*) as count FROM ownership WHERE username='".mysqli_escape_string($db, $username)."'".
          " AND elementType='".mysqli_escape_string($db, $elementType)."' AND deleted is NULL";
  $r = mysqli_query($db, $q);
  if($r && ($c=mysqli_fetch_array($r))) {
    $o[] = array('elementType'=>$elementType, 'limit'=>$limit, 'current'=>$c[0]);
  }
}
echo json_encode(array('username'=>$username, 'role'=>$role, 'organization'=>$org, 'limits'=>$o));

ownership_access_log(['op'=>$OPERATION,'user'=>$uinfo->username,'forUser'=>$username,'role'=>$role,'organization'=>$org,'type'=>$elementType,'result'=>'SUCCESS']);
