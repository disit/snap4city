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
$plumber_script = "/home/user/add-plumber.sh";
$portia_script = "/home/user/add-portia.sh";
$python_script = "/home/user/add-python.sh";

$app_id_length = 5;

$nrapp_id_prefix = "nr";
$plumber_id_prefix = "pl";
$portia_id_prefix = "pt";
$python_id_prefix = "py";

$marathon_url = "http://localhost:8080";

$sso_userinfo_endpoint = 'http://localhost/auth/realms/master/protocol/openid-connect/userinfo';
$apps_base_url = "http://localhost";

$nodered_cpu = 0.085;
$nodered_mem = 140;
$plumber_cpu = 0.085;
$plumber_mem = 500;
$python_cpu = 0.085;
$python_mem = 500;
$portia_cpu = 0.085;
$portia_mem = 200;
$portia_crawler_cpu = 0.085;
$portia_crawler_mem = 200;

// update images if needed, see https://hub.docker.com/u/disitlab
$nodered_basic_img = 'disitlab/snap4city-nodered-v1.1.3-basic:v12';
$nodered_adv_img = 'disitlab/snap4city-nodered-v1.1.3-adv:v4';
$plumber_img = 'disitlab/snap4city-plumber:v8';
$python_img = 'disitlab/snap4city-da-python3:v1';
$portia_img = 'disitlab/snap4city-portia:v2.1';
$portia_crawler_img = 'disitlab/snap4city-portia:v2.1';

$appHealthChecksMaxConsecutiveFailures = 4;
$appHealthChecksGracePeriodSeconds = 240;
$appHealthChecksTimeoutSeconds = 30;
$appHealthChecksIntervalSeconds = 15;