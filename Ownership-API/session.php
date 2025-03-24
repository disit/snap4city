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

require '../../vendor/autoload.php';
require_once '../../config.php';
require_once 'common.php';

use Jumbojett\OpenIDConnectClient;

$ipAddress = @get_client_ip_server();
if(isset($_REQUEST['username']) && in_array($ipAddress, $trustedIpAddrs)) {
  $uinfo=new stdClass();
  $uinfo->username=$_REQUEST['username'];
  $uinfo->mainRole='';
} else {
  $oidc = new OpenIDConnectClient();
  $oidc->providerConfigParam(array('userinfo_endpoint'=>$sso_userinfo_endpoint));

  if(isset($_SESSION['accessToken'])) {
    $accessToken=$_SESSION['accessToken'];
  } else if(isset($_REQUEST['accessToken'])) {
    $accessToken=$_REQUEST['accessToken'];
  } else {
    $headers = array_change_key_case(getallheaders(), CASE_LOWER);
    if (isset($headers['authorization']) && strlen($headers['authorization'])>8 && 
            substr( strtolower($headers['authorization']), 0, 7 ) === "bearer ") {
        $accessToken=substr($headers['authorization'],7);
    } else {
        header("HTTP/1.1 401 Unauthorized");
        echo "No token provided or invalid authorization header";
        ownership_access_log(['op'=>$OPERATION,'result'=>'NO_TOKEN']);
        exit;
    }
  }
  $oidc->setAccessToken($accessToken);
  $payload=$oidc->getAccessTokenPayload();
  //var_dump($payload);
  $uinfo = $oidc->requestUserInfo();
  if(isset($uinfo->error)) {
    header("HTTP/1.1 401 Unauthorized");
    echo json_encode($uinfo);
    ownership_access_log(['op'=>$OPERATION,'result'=>'UNAUTHORIZED']);
    $f=fopen($log_path."/ownership-error.log","a");
    fwrite($f,date('c')." $OPERATION USERINFO ERROR: ".json_encode($uinfo)." tkn:".$accessToken."\n");
    exit;  
  }

  if(!isset($uinfo->username) && isset($uinfo->preferred_username))
    $uinfo->username = $uinfo->preferred_username;
  
  if(!isset($uinfo->username)) {
    header("HTTP/1.1 400 BAD REQUEST");
    echo "No username found ".json_encode($uinfo);
    $f=fopen($log_path."/ownership-error.log","a");
    fwrite($f,date('c')." $OPERATION USERINFO ERROR: no username found ".json_encode($uinfo)." accessToken: ".$accessToken."\n");
    ownership_access_log(['op'=>$OPERATION,'result'=>'NO_USERNAME','id'=>$_REQUEST['elementId']]);
    exit;  
  }
  
  $ROLES = array('RootAdmin','ToolAdmin','AreaManager','Manager','Public');
  $uinfo->mainRole = 'none';

  if(isset($uinfo->roles)) {
    foreach($ROLES as $r) {
      if(in_array($r, $uinfo->roles)) {
        $uinfo->mainRole = $r;
        break;
      }
    }
  }
}

function get_client_ip_server() {
    $ipaddress = '';
    if ($_SERVER['HTTP_CLIENT_IP'])
        $ipaddress = $_SERVER['HTTP_CLIENT_IP'];
    else if($_SERVER['HTTP_X_FORWARDED_FOR'])
        $ipaddress = $_SERVER['HTTP_X_FORWARDED_FOR'];
    else if($_SERVER['HTTP_X_FORWARDED'])
        $ipaddress = $_SERVER['HTTP_X_FORWARDED'];
    else if($_SERVER['HTTP_FORWARDED_FOR'])
        $ipaddress = $_SERVER['HTTP_FORWARDED_FOR'];
    else if($_SERVER['HTTP_FORWARDED'])
        $ipaddress = $_SERVER['HTTP_FORWARDED'];
    else if($_SERVER['REMOTE_ADDR'])
        $ipaddress = $_SERVER['REMOTE_ADDR'];
    else
        $ipaddress = 'UNKNOWN';
 
    return $ipaddress;
}
