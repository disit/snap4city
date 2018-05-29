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


require '../vendor/autoload.php';
use Jumbojett\OpenIDConnectClient;

if(isset($_REQUEST['username'])) {
  $uinfo=new stdClass();
  $uinfo->username=$_REQUEST['username'];
} else {
  $oidc = new OpenIDConnectClient();
  $oidc->providerConfigParam(array('authorization_endpoint'=>'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/auth'));
  $oidc->providerConfigParam(array('token_endpoint'=>'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/token'));
  $oidc->providerConfigParam(array('userinfo_endpoint'=>'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/userinfo'));
  $oidc->providerConfigParam(array('jwks_uri'=>'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/certs'));
  $oidc->providerConfigParam(array('issuer'=>'https://www.snap4city.org/auth/realms/master'));

  if(isset($_SESSION['accessToken'])) {
    $accessToken=$_SESSION['accessToken'];
  } else if(isset($_REQUEST['accessToken'])) {
    $accessToken=$_REQUEST['accessToken'];
  } else {
    header("HTTP/1.1 401 Unauthorized");
    echo '{"error":"No token provided"}';
    exit;
  }
  $oidc->setAccessToken($accessToken);
  $payload=$oidc->getAccessTokenPayload();
  //var_dump($payload);
  $uinfo = $oidc->requestUserInfo();
  if(isset($uinfo->error)) {
    header("HTTP/1.1 401 Unauthorized");
    echo json_encode($uinfo);
    exit;  
  }

  if(!isset($uinfo->username) && isset($uinfo->preferred_username))
    $uinfo->username = $uinfo->preferred_username;
  
  if(!isset($uinfo->username)) {
    header("HTTP/1.1 400 BAD REQUEST");
    echo '{"error":"No username found", "user":'.json_encode($uinfo).'}';
    exit;  
  }
  
  $ROLES = array('ToolAdmin','AreaManager','Manager','Public');
  $uinfo->mainRole = '';
  if(isset($uinfo->roles)) {
    foreach($ROLES as $r) {
      if(in_array($r, $uinfo->roles)) {
        $uinfo->mainRole = $r;
        break;
      }
    }
  }
}
