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


require '../vendor/autoload.php';

use Jumbojett\OpenIDConnectClient;

include "../config.php";

$oidc = new OpenIDConnectClient(
    $sso_base_url,
    $sso_client_id,
    $sso_client_secret
);

$oidc->setVerifyHost(false);
$oidc->setVerifyPeer(false);

$oidc->providerConfigParam(array('authorization_endpoint'=>"$sso_base_url/auth/realms/master/protocol/openid-connect/auth"));
$oidc->providerConfigParam(array('token_endpoint'=>"$sso_base_url/auth/realms/master/protocol/openid-connect/token"));
$oidc->providerConfigParam(array('userinfo_endpoint'=>"$sso_base_url/auth/realms/master/protocol/openid-connect/userinfo"));
$oidc->providerConfigParam(array('jwks_uri'=>"$sso_base_url/auth/realms/master/protocol/openid-connect/certs"));
$oidc->providerConfigParam(array('issuer'=>"$sso_base_url/auth/realms/master"));
$oidc->providerConfigParam(array('end_session_endpoint'=>"$sso_base_url/auth/realms/master/protocol/openid-connect/logout"));

if(!isset($_SESSION['login'])) {
  $oidc->addScope(array('openid','username','profile'));
  $oidc->setRedirectURL($sso_login_redirect);
  $oidc->authenticate();
  $name = $oidc->requestUserInfo('username');
  $_SESSION['login']=$name;
  $_SESSION['refreshToken']=$oidc->getRefreshToken();
  $_SESSION['accessToken']=$oidc->getAccessToken();
  echo $_SESSION['login']."<br/>";
  echo $_SESSION['accessToken'];
} else {
  echo $_SESSION['login']."<br/>";
  $tkn=$oidc->refreshToken($_SESSION['refreshToken']);
  echo $tkn->access_token;
  $_SESSION['accessToken']=$tkn->access_token;
  $_SESSION['refreshToken']=$tkn->refresh_token;
}