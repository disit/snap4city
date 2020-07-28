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
$OPERATION = 'USERS';

include("../../session.php");
require_once ("../../config.php");
if(!isset($_REQUEST['check'])) {
  header("HTTP/1.1 400 BAD REQUEST");
  echo "missing parameter check with email or username";
  ownership_access_log(['op'=>'USERS','check'=>NULL, 'user'=>$uinfo->username,'role'=>$uinfo->mainRole, 'result'=>"FAILURE"]);
  exit;  
}
$checkUname = $_REQUEST['check'];

$data = "username=$keycloack_admin&password=$keycloack_pwd&grant_type=password&client_id=admin-cli";
$result = @http_post($keycloack_base_url."/realms/master/protocol/openid-connect/token",$data,"application/x-www-form-urlencoded");
if(isset($result['result']['access_token'])) {
  $accessToken = $result['result']['access_token'];
  if($accessToken) {
    $result=@http_get($keycloack_base_url."/admin/realms/master/users/?search=".urlencode($checkUname),$accessToken);
    if(isset($result['result'])) {
      if(count($result['result'])>0) {
        $uname = $result['result'][0]["username"];
        $email = $result['result'][0]["email"];
        if($uname == $checkUname || $email == $checkUname) {
          $rslt="FOUND";
          echo '{ "result": "'.$rslt.'", "username":"'.$uname.'" }';
        } else {
          $rslt="NOTFOUND";
          echo '{ "result": "NOTFOUND" }';
        }
      } else {
        $rslt="NOTFOUND";
        echo '{ "result": "NOTFOUND" }';
      }
    } else {
      $rslt="FAILED_SEARCH";
      echo '{ "result": "FAILURE" }';
    }
  } else {
    $rslt="FAILED_NOTOKEN";
    echo '{ "result": "FAILURE" }';
  }
} else {
  $rslt = "FAILED_LOGIN";
  echo '{ "result": "FAILURE" }';
}

ownership_access_log(['op'=>$OPERATION,'check'=>$checkUname, 'user'=>$uinfo->username,'role'=>$uinfo->mainRole,'result'=>$rslt]);
