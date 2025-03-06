<?php
/*
Snap4city -- getUserData.php --
   Copyright (C) 2020 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
include("connection.php");
$data = array();
$resources = array();
session_start();

// check the permission
if (!isset($_SESSION["role"])) {
    header("location: ssoLogin.php");
}

function logtext($text) {
 $fp = fopen("log.txt", "at");
 fwrite($fp, $text . "\n");
 fclose($fp);
}

// RootAdmin has the right to make this query by any username
$username = ($_SESSION["role"] == "RootAdmin" && isset($_REQUEST["username"])) ? $_REQUEST["username"] : $_SESSION["username"];
$role = "";
$level = "";

$query = "SELECT * FROM iot.roles_levels WHERE username = '" . mysqli_real_escape_string($connection, $username) . "'";

$result = mysqli_query($connection, $query);
while ($row = mysqli_fetch_assoc($result)) {
       $role = $row["role"];
       $level = $row["level"];
}

if (isset($username) && isset($_REQUEST["date"])) {
    $year_month = explode("-", $_REQUEST["date"]);
    // if date is yyyy-mm-dd
    if (count($year_month) == 3) {
        $query = "SELECT * FROM iot.data a LEFT JOIN iot.roles_levels b ON a.username = b.username WHERE a.username = '" . mysqli_real_escape_string($connection, $username) . "' AND a.date = '" . mysqli_real_escape_string($connection, $_REQUEST["date"]) . "'";
    }
    // if date is yyyy-mm
    else if (count($year_month) == 2) {
        //$query = "SELECT * FROM iot.data a LEFT JOIN iot.roles_levels b ON a.username = b.username WHERE a.username = '" . $username . "' AND YEAR(a.date) = '" . $year_month[0] . "' AND MONTH(a.date) = '" . $year_month[1] . "'";
        $query = "SELECT a.username, role, level, SUM(iot_db_storage_tx) AS iot_db_storage_tx, SUM(iot_db_storage_rx) AS iot_db_storage_rx, 
                  SUM(iot_filesystem_storage_tx) AS iot_filesystem_storage_tx, SUM(iot_filesystem_storage_rx) AS iot_filesystem_storage_rx,
                  SUM(iot_db_request_tx) AS iot_db_request_tx, SUM(iot_db_request_rx) AS iot_db_request_rx,
                  SUM(iot_ascapi_tx) AS iot_ascapi_tx, SUM(iot_ascapi_rx) AS iot_ascapi_rx,
                  SUM(iot_disces_tx) AS iot_disces_tx, SUM(iot_disces_rx) AS iot_disces_rx,
                  SUM(iot_dashboard_tx) AS iot_dashboard_tx, SUM(iot_dashboard_rx) AS iot_dashboard_rx,
                  SUM(iot_datagate_tx) AS iot_datagate_tx, SUM(iot_datagate_rx) AS iot_datagate_rx,
                  SUM(iot_external_service_tx) AS iot_external_service_tx, SUM(iot_external_service_rx) AS iot_external_service_rx,
                  SUM(iot_iot_service_tx) AS iot_iot_service_tx, SUM(iot_iot_service_rx) AS iot_iot_service_rx,
                  SUM(iot_mapping_tx) AS iot_mapping_tx, SUM(iot_mapping_rx) AS iot_mapping_rx,
                  SUM(iot_microserviceusercreated_tx) AS iot_microserviceusercreated_tx, SUM(iot_microserviceusercreated_rx) AS iot_microserviceusercreated_rx,
                  SUM(iot_mydata_tx) AS iot_mydata_tx, SUM(iot_mydata_rx) AS iot_mydata_rx,
                  SUM(iot_notificator_tx) AS iot_notificator_tx, SUM(iot_notificator_rx) AS iot_notificator_rx,
                  SUM(iot_rstatistics_tx) AS iot_rstatistics_tx, SUM(iot_rstatistics_rx) AS iot_rstatistics_rx,
                  SUM(iot_sigfox_tx) AS iot_sigfox_tx, SUM(iot_sigfox_rx) AS iot_sigfox_rx,
                  SUM(iot_undefined_tx) AS iot_undefined_tx, SUM(iot_undefined_rx) AS iot_undefined_rx,
                  SUM(iot_tx) AS iot_tx, SUM(iot_rx) AS iot_rx,
                  ROUND(AVG(iot_apps)) AS iot_apps, ROUND(AVG(devices_public)) AS devices_public,
                  ROUND(AVG(devices_private)) AS devices_private, ROUND(AVG(dashboards_public)) AS dashboards_public,
                  ROUND(AVG(dashboards_private)) AS dashboards_private,
                  SUM(dashboards_accesses) AS dashboards_accesses, SUM(dashboards_minutes) AS dashboards_minutes,
                  SUM(iot_reads) AS iot_reads, SUM(iot_writes) AS iot_writes,
                  SUM(etl_writes) AS etl_writes,
                  a.date
                  FROM iot.data a LEFT JOIN iot.roles_levels b ON a.username = b.username WHERE a.username = '" . mysqli_real_escape_string($connection, $username) . "' AND YEAR(a.date) = '" . mysqli_real_escape_string($connection, $year_month[0]) . "' AND MONTH(a.date) = '" . mysqli_real_escape_string($connection, $year_month[1]) . "'";
    }
    // if date is not set
    else if (count($year_month) == 1) {
        if ($_REQUEST["date"] == "Last 7 days") {
            $days = 7;
        } else if ($_REQUEST["date"] == "Last 30 days") {
            $days = 30;
        }
        $query = "SELECT a.username, role, level, SUM(iot_db_storage_tx) AS iot_db_storage_tx, SUM(iot_db_storage_rx) AS iot_db_storage_rx,
                  SUM(iot_filesystem_storage_tx) AS iot_filesystem_storage_tx, SUM(iot_filesystem_storage_rx) AS iot_filesystem_storage_rx,
                  SUM(iot_db_request_tx) AS iot_db_request_tx, SUM(iot_db_request_rx) AS iot_db_request_rx,
                  SUM(iot_ascapi_tx) AS iot_ascapi_tx, SUM(iot_ascapi_rx) AS iot_ascapi_rx,
                  SUM(iot_disces_tx) AS iot_disces_tx, SUM(iot_disces_rx) AS iot_disces_rx,
                  SUM(iot_dashboard_tx) AS iot_dashboard_tx, SUM(iot_dashboard_rx) AS iot_dashboard_rx,
                  SUM(iot_datagate_tx) AS iot_datagate_tx, SUM(iot_datagate_rx) AS iot_datagate_rx,
                  SUM(iot_external_service_tx) AS iot_external_service_tx, SUM(iot_external_service_rx) AS iot_external_service_rx,
                  SUM(iot_iot_service_tx) AS iot_iot_service_tx, SUM(iot_iot_service_rx) AS iot_iot_service_rx,
                  SUM(iot_mapping_tx) AS iot_mapping_tx, SUM(iot_mapping_rx) AS iot_mapping_rx,
                  SUM(iot_microserviceusercreated_tx) AS iot_microserviceusercreated_tx, SUM(iot_microserviceusercreated_rx) AS iot_microserviceusercreated_rx,
                  SUM(iot_mydata_tx) AS iot_mydata_tx, SUM(iot_mydata_rx) AS iot_mydata_rx,
                  SUM(iot_notificator_tx) AS iot_notificator_tx, SUM(iot_notificator_rx) AS iot_notificator_rx,
                  SUM(iot_rstatistics_tx) AS iot_rstatistics_tx, SUM(iot_rstatistics_rx) AS iot_rstatistics_rx,
                  SUM(iot_sigfox_tx) AS iot_sigfox_tx, SUM(iot_sigfox_rx) AS iot_sigfox_rx,
                  SUM(iot_undefined_tx) AS iot_undefined_tx, SUM(iot_undefined_rx) AS iot_undefined_rx,
                  SUM(iot_tx) AS iot_tx, SUM(iot_rx) AS iot_rx,
                  ROUND(AVG(iot_apps)) AS iot_apps, ROUND(AVG(devices_public)) AS devices_public,
                  ROUND(AVG(devices_private)) AS devices_private, ROUND(AVG(dashboards_public)) AS dashboards_public,
                  ROUND(AVG(dashboards_private)) AS dashboards_private,
                  SUM(dashboards_accesses) AS dashboards_accesses, SUM(dashboards_minutes) AS dashboards_minutes,
                  SUM(iot_reads) AS iot_reads, SUM(iot_writes) AS iot_writes,
                  SUM(etl_writes) AS etl_writes,
                  a.date
                  FROM iot.data a LEFT JOIN iot.roles_levels b ON a.username = b.username WHERE a.username = '" . mysqli_real_escape_string($connection, $username) . "' AND a.date > NOW() - INTERVAL " . mysqli_real_escape_string($connection, $days) . " DAY";
    }
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $data = $row;
    }
    $connection->close();

    $connection = mysqli_connect("localhost", "user", "passw", "processloader_db");
    // if date is yyyy-mm-dd
    if (count($year_month) == 3) {
        $query = "SELECT file_type, COUNT(*) AS num FROM processloader_db.uploaded_files WHERE username = '" . mysqli_real_escape_string($connection, $username) . "' AND creation_date <= '" . mysqli_real_escape_string($connection, $_REQUEST["date"]) . " 23:59:59' GROUP BY file_type";
    }
    // if date is yyyy-mm
    else if (count($year_month) == 2) {
        $query = "SELECT file_type, COUNT(*) AS num FROM processloader_db.uploaded_files WHERE username = '" . mysqli_real_escape_string($connection, $username) . "' AND YEAR(creation_date) = '" . mysqli_real_escape_string($connection, $year_month[0]) . "' AND MONTH(creation_date) = '" . mysqli_real_escape_string($connection, $year_month[1]) . "' GROUP BY file_type";
    }
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $resources[$row["file_type"]] = $row["num"];
    }
    $connection->close();

    if (count($resources) > 0) {
        $data["resources"] = $resources;
    }
}

if($data["username"] == null) {
 $data["username"] = $username;
 $data["role"] = $role;
 $data["level"] = $level;
 echo json_encode($data);
} else {
 echo json_encode($data);
 }
?>
