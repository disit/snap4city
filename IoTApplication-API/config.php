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

$logDir = '/tmp';
$db_host = "localhost";
$database = "snap4cityapps";
$db_user = "user";
$db_pwd = "password";

$disces_em = "localhost";
$disces_em_database = "quartz";
$disces_em_user = "user";
$disces_em_pwd = "password";

$ownership_api_url = "http://localhost/ownership-api";
$nodered_script = "/home/user/add-nodered.sh";
$plumber_script = "/home/ubuntu/add-plumber.sh";
$app_id_length = 5;

$nrapp_id_prefix = "nr";
$plumber_id_prefix = "pl";
$marathon_url = "http://localhost:8080";

$sso_userinfo_endpoint = 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/userinfo';

$nodered_cpu = 0.085;
$nodered_mem = 140;
$plumber_cpu = 0.085;
$plumber_mem = 500;

$nodered_basic_img = 'snap4city-nodered-basic:v25';
$nodered_adv_img = 'snap4city-nodered-adv:v20';
$plumber_img = 'trestletech/plumber';

$appHealthChecksMaxConsecutiveFailures = 4;
$appHealthChecksGracePeriodSeconds = 240;
$appHealthChecksTimeoutSeconds = 30;
$appHealthChecksIntervalSeconds = 15;