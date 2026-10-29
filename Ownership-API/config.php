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

// db access

$db_host = "localhost";
$database = "profiledb";
$db_user = "user";
$db_pwd = "password";

// SSO configuration (needed only for debugging)
$sso_base_url = 'http://localhost:8088';
$sso_client_id = 'php-ownership-api';
$sso_client_secret = '...secret...';
$sso_login_redirect = 'http://localhost/ownership-api/login/';

// SSO mandatory configuration
$sso_userinfo_endpoint = 'https://localhost:8088/auth/realms/master/protocol/openid-connect/userinfo';

$log_path = '/tmp';

// can be used to bypass authentication, use ONLY if needed
$trustedIpAddrs = array();

//keycloak admin user used for user search

$keycloack_base_url = 'http://localhost:8088/auth';
$keycloack_admin = 'admin';
$keycloack_pwd = 'password';

//ldap access, used for organization and role search

$ldapServer = 'localhost';
$ldapPort = '389';
$ldapBaseDN = 'dc=ldap,dc=organization,dc=org';
$ldapAdminDN = 'cn=admin,dc=ldap,dc=organization,dc=org';
$ldapAdminPwd = 'password';

//set the keys with the same values used in dashboard-builder
$encryptionMethod = "AES-256-CBC";
$encryptionInitKey = "...";
$encryptionIvKey = "...";

$datamanager_api_url = 'http://localhost/mypersonaldata/api';

// uncomment if using wstunnel server for edge iotapps
#$wstunnel_db_host = 'localhost';
#$wstunnel_database = 'wstunnel';
#$wstunnel_db_user = 'user';
#$wstunnel_db_passw = 'password';

#$iot_app_edge_url = 'http://localhost/edge/';
