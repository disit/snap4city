<?php
/*
Snap4city -- fetch.php --
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
header('Access-Control-Allow-Origin: *');
require 'sso/autoload.php';

use Jumbojett\OpenIDConnectClient;

session_start();

// check the permission
if (!isset($_SESSION["role"])) {
    header("location: ssoLogin.php");
}

include("connection.php");

$query = '';
$data = array();
$records_per_page = 10;
$start_from = 0;
$current_page_number = 0;

if (isset($_POST["rowCount"])) {
    $records_per_page = $_POST["rowCount"];
} else {
    $records_per_page = 10;
}

if (isset($_POST["current"])) {
    $current_page_number = $_POST["current"];
} else {
    $current_page_number = 1;
}

$start_from = ($current_page_number - 1) * $records_per_page;

if($_SESSION["role"] != "RootAdmin") {
 $output = array(
    'current' => intval($_POST["current"]),
    'rowCount' => 10,
    'total' => intval(0),
    'rows' => null
);
 $connection->close();
 echo json_encode($output);
 exit();
}

// SELECT @i:=@i+1 AS id, username, IF(SUM(iot_apps_tx) IS NOT NULL, CONVERT(SUM(iot_apps_tx), CHAR), '') AS iot_apps_tx, IF(SUM(iot_apps_rx) IS NOT NULL, CONVERT(SUM(iot_apps_rx), CHAR), '') AS iot_apps_rx, IF(SUM(iot_apps) IS NOT NULL, CONVERT(SUM(iot_apps), CHAR), '') AS iot_apps, IF(SUM(devices_public) IS NOT NULL, CONVERT(SUM(devices_public), CHAR), '') AS devices_public, IF(SUM(devices_private) IS NOT NULL, CONVERT(SUM(devices_private), CHAR), '') AS devices_private, IF(SUM(dashboards_public) IS NOT NULL, CONVERT(SUM(dashboards_public), CHAR), '') AS dashboards_public, IF(SUM(dashboards_private) IS NOT NULL, CONVERT(SUM(dashboards_private), CHAR), '') AS dashboards_private, '' AS date
// FROM iot.data, (SELECT @i:=0) AS foo WHERE date >= CURDATE() - INTERVAL 1 DAY GROUP BY username

if ($_REQUEST["days"] == 1) {
    $query .= "SELECT a.id, a.username, b.role, b.level, " .
            "IF(iot_db_storage_tx IS NOT NULL, ROUND(iot_db_storage_tx, 2), 0) AS iot_db_storage_tx, " .
            "IF(iot_db_storage_rx IS NOT NULL, ROUND(iot_db_storage_rx, 2), 0) AS iot_db_storage_rx, " .
            "IF(iot_filesystem_storage_tx IS NOT NULL, ROUND(iot_filesystem_storage_tx, 2), 0) AS iot_filesystem_storage_tx, " .
            "IF(iot_filesystem_storage_rx IS NOT NULL, ROUND(iot_filesystem_storage_rx, 2), 0) AS iot_filesystem_storage_rx, " .
            "IF(iot_db_request_tx IS NOT NULL, ROUND(iot_db_request_tx, 2), 0) AS iot_db_request_tx, " .
            "IF(iot_db_request_rx IS NOT NULL, ROUND(iot_db_request_rx, 2), 0) AS iot_db_request_rx, " .
            "IF(iot_ascapi_tx IS NOT NULL, ROUND(iot_ascapi_tx, 2), 0) AS iot_ascapi_tx, " .
            "IF(iot_ascapi_rx IS NOT NULL, ROUND(iot_ascapi_rx, 2), 0) AS iot_ascapi_rx, " .
            "IF(iot_disces_tx IS NOT NULL, ROUND(iot_disces_tx, 2), 0) AS iot_disces_tx, " .
            "IF(iot_disces_rx IS NOT NULL, ROUND(iot_disces_rx, 2), 0) AS iot_disces_rx, " .
            "IF(iot_dashboard_tx IS NOT NULL, ROUND(iot_dashboard_tx, 2), 0) AS iot_dashboard_tx, " .
            "IF(iot_dashboard_rx IS NOT NULL, ROUND(iot_dashboard_rx, 2), 0) AS iot_dashboard_rx, " .
            "IF(iot_datagate_tx IS NOT NULL, ROUND(iot_datagate_tx, 2), 0) AS iot_datagate_tx, " .
            "IF(iot_datagate_rx IS NOT NULL, ROUND(iot_datagate_rx, 2), 0) AS iot_datagate_rx, " .
            "IF(iot_external_service_tx IS NOT NULL, ROUND(iot_external_service_tx, 2), 0) AS iot_external_service_tx, " .
            "IF(iot_external_service_rx IS NOT NULL, ROUND(iot_external_service_rx, 2), 0) AS iot_external_service_rx, " .
            "IF(iot_iot_service_tx IS NOT NULL, ROUND(iot_iot_service_tx, 2), 0) AS iot_iot_service_tx, " .
            "IF(iot_iot_service_rx IS NOT NULL, ROUND(iot_iot_service_rx, 2), 0) AS iot_iot_service_rx, " .
            "IF(iot_mapping_tx IS NOT NULL, ROUND(iot_mapping_tx, 2), 0) AS iot_mapping_tx, " .
            "IF(iot_mapping_rx IS NOT NULL, ROUND(iot_mapping_rx, 2), 0) AS iot_mapping_rx, " .
            "IF(iot_microserviceusercreated_tx IS NOT NULL, ROUND(iot_microserviceusercreated_tx, 2), 0) AS iot_microserviceusercreated_tx, " .
            "IF(iot_microserviceusercreated_rx IS NOT NULL, ROUND(iot_microserviceusercreated_rx, 2), 0) AS iot_microserviceusercreated_rx, " .
            "IF(iot_mydata_tx IS NOT NULL, ROUND(iot_mydata_tx, 2), 0) AS iot_mydata_tx, " .
            "IF(iot_mydata_rx IS NOT NULL, ROUND(iot_mydata_rx, 2), 0) AS iot_mydata_rx, " .
            "IF(iot_notificator_tx IS NOT NULL, ROUND(iot_notificator_tx, 2), 0) AS iot_notificator_tx, " .
            "IF(iot_notificator_rx IS NOT NULL, ROUND(iot_notificator_rx, 2), 0) AS iot_notificator_rx, " .
            "IF(iot_rstatistics_tx IS NOT NULL, ROUND(iot_rstatistics_tx, 2), 0) AS iot_rstatistics_tx, " .
            "IF(iot_rstatistics_rx IS NOT NULL, ROUND(iot_rstatistics_rx, 2), 0) AS iot_rstatistics_rx, " .
            "IF(iot_sigfox_tx IS NOT NULL, ROUND(iot_sigfox_tx, 2), 0) AS iot_sigfox_tx, " .
            "IF(iot_sigfox_rx IS NOT NULL, ROUND(iot_sigfox_rx, 2), 0) AS iot_sigfox_rx, " .
            "IF(iot_undefined_tx IS NOT NULL, ROUND(iot_undefined_tx, 2), 0) AS iot_undefined_tx, " .
            "IF(iot_undefined_rx IS NOT NULL, ROUND(iot_undefined_rx, 2), 0) AS iot_undefined_rx, " .
            "IF(iot_tx IS NOT NULL, ROUND(iot_tx, 2), 0) AS iot_tx, " .
            "IF(iot_rx IS NOT NULL, ROUND(iot_rx, 2), 0) AS iot_rx, " .
            "IF(iot_apps IS NOT NULL, iot_apps, 0) AS iot_apps, " .
            "IF(iot_reads IS NOT NULL, iot_reads, 0) AS iot_reads, " .
            "IF(iot_writes IS NOT NULL, iot_writes, 0) AS iot_writes, " .
            "IF(etl_writes IS NOT NULL, etl_writes, 0) AS etl_writes, " .
            "IF(devices_public IS NOT NULL, devices_public, 0) AS devices_public, " .
            "IF(devices_private IS NOT NULL, devices_private, 0) AS devices_private, " .
            "IF(dashboards_public IS NOT NULL, dashboards_public, 0) AS dashboards_public, " .
            "IF(dashboards_private IS NOT NULL, dashboards_private, 0) AS dashboards_private, " .
            "IF(dashboards_accesses IS NOT NULL, dashboards_accesses, 0) AS dashboards_accesses, " .
            "IF(dashboards_minutes IS NOT NULL, dashboards_minutes, 0) AS dashboards_minutes, " .
            "a.date " .
            "FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " a " .
            "LEFT JOIN " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . ".roles_levels b ON a.username = b.username " .
            "WHERE a.date >= CURDATE() - INTERVAL " . mysqli_real_escape_string($connection, $_REQUEST["days"]) . " DAY";
} else if (($_REQUEST["days"] == 7 || $_REQUEST["days"] == 30 || $_REQUEST["days"] == 90) && $_REQUEST["db"] == 'iot' && $_REQUEST["table"] == 'data') {
    /*
     * $query .= "SELECT @i:=@i+1 AS id, username, " .
      "IF(SUM(iot_db_storage_tx) IS NOT NULL, ROUND(SUM(iot_db_storage_tx), 2), 0) AS iot_db_storage_tx, " .
      "IF(SUM(iot_db_storage_rx) IS NOT NULL, ROUND(SUM(iot_db_storage_rx), 2), 0) AS iot_db_storage_rx, " .
      "IF(SUM(iot_filesystem_storage_tx) IS NOT NULL, ROUND(SUM(iot_filesystem_storage_tx), 2), 0) AS iot_filesystem_storage_tx, " .
      "IF(SUM(iot_filesystem_storage_rx) IS NOT NULL, ROUND(SUM(iot_filesystem_storage_rx), 2), 0) AS iot_filesystem_storage_rx, " .
      "IF(SUM(iot_db_request_tx) IS NOT NULL, ROUND(SUM(iot_db_request_tx), 2), 0) AS iot_db_request_tx, " .
      "IF(SUM(iot_db_request_rx) IS NOT NULL, ROUND(SUM(iot_db_request_rx), 2), 0) AS iot_db_request_rx, " .
      "IF(SUM(iot_ascapi_tx) IS NOT NULL, ROUND(SUM(iot_ascapi_tx), 2), 0) AS iot_ascapi_tx, " .
      "IF(SUM(iot_ascapi_rx) IS NOT NULL, ROUND(SUM(iot_ascapi_rx), 2), 0) AS iot_ascapi_rx, " .
      "IF(SUM(iot_disces_tx) IS NOT NULL, ROUND(SUM(iot_disces_tx), 2), 0) AS iot_disces_tx, " .
      "IF(SUM(iot_disces_rx) IS NOT NULL, ROUND(SUM(iot_disces_rx), 2), 0) AS iot_disces_rx, " .
      "IF(SUM(iot_dashboard_tx) IS NOT NULL, ROUND(SUM(iot_dashboard_tx), 2), 0) AS iot_dashboard_tx, " .
      "IF(SUM(iot_dashboard_rx) IS NOT NULL, ROUND(SUM(iot_dashboard_rx), 2), 0) AS iot_dashboard_rx, " .
      "IF(SUM(iot_datagate_tx) IS NOT NULL, ROUND(SUM(iot_datagate_tx), 2), 0) AS iot_datagate_tx, " .
      "IF(SUM(iot_datagate_rx) IS NOT NULL, ROUND(SUM(iot_datagate_rx), 2), 0) AS iot_datagate_rx, " .
      "IF(SUM(iot_external_service_tx) IS NOT NULL, ROUND(SUM(iot_external_service_tx), 2), 0) AS iot_external_service_tx, " .
      "IF(SUM(iot_external_service_rx) IS NOT NULL, ROUND(SUM(iot_external_service_rx), 2), 0) AS iot_external_service_rx, " .
      "IF(SUM(iot_iot_service_tx) IS NOT NULL, ROUND(SUM(iot_iot_service_tx), 2), 0) AS iot_iot_service_tx, " .
      "IF(SUM(iot_iot_service_rx) IS NOT NULL, ROUND(SUM(iot_iot_service_rx), 2), 0) AS iot_iot_service_rx, " .
      "IF(SUM(iot_mapping_tx) IS NOT NULL, ROUND(SUM(iot_mapping_tx), 2), 0) AS iot_mapping_tx, " .
      "IF(SUM(iot_mapping_rx) IS NOT NULL, ROUND(SUM(iot_mapping_rx), 2), 0) AS iot_mapping_rx, " .
      "IF(SUM(iot_microserviceusercreated_tx) IS NOT NULL, ROUND(SUM(iot_microserviceusercreated_tx), 2), 0) AS iot_microserviceusercreated_tx, " .
      "IF(SUM(iot_microserviceusercreated_rx) IS NOT NULL, ROUND(SUM(iot_microserviceusercreated_rx), 2), 0) AS iot_microserviceusercreated_rx, " .
      "IF(SUM(iot_mydata_tx) IS NOT NULL, ROUND(SUM(iot_mydata_tx), 2), 0) AS iot_mydata_tx, " .
      "IF(SUM(iot_mydata_rx) IS NOT NULL, ROUND(SUM(iot_mydata_rx), 2), 0) AS iot_mydata_rx, " .
      "IF(SUM(iot_notificator_tx) IS NOT NULL, ROUND(SUM(iot_notificator_tx), 2), 0) AS iot_notificator_tx, " .
      "IF(SUM(iot_notificator_rx) IS NOT NULL, ROUND(SUM(iot_notificator_rx), 2), 0) AS iot_notificator_rx, " .
      "IF(SUM(iot_rstatistics_tx) IS NOT NULL, ROUND(SUM(iot_rstatistics_tx), 2), 0) AS iot_rstatistics_tx, " .
      "IF(SUM(iot_rstatistics_rx) IS NOT NULL, ROUND(SUM(iot_rstatistics_rx), 2), 0) AS iot_rstatistics_rx, " .
      "IF(SUM(iot_sigfox_tx) IS NOT NULL, ROUND(SUM(iot_sigfox_tx), 2), 0) AS iot_sigfox_tx, " .
      "IF(SUM(iot_sigfox_rx) IS NOT NULL, ROUND(SUM(iot_sigfox_rx), 2), 0) AS iot_sigfox_rx, " .
      "IF(SUM(iot_undefined_tx) IS NOT NULL, ROUND(SUM(iot_undefined_tx), 2), 0) AS iot_undefined_tx, " .
      "IF(SUM(iot_undefined_rx) IS NOT NULL, ROUND(SUM(iot_undefined_rx), 2), 0) AS iot_undefined_rx, " .
      "IF(SUM(iot_tx) IS NOT NULL, ROUND(SUM(iot_tx), 2), 0) AS iot_tx, " .
      "IF(SUM(iot_rx) IS NOT NULL, ROUND(SUM(iot_rx), 2), 0) AS iot_rx, " .
      "IF(SUM(iot_apps) IS NOT NULL, SUM(iot_apps), 0) AS iot_apps, " .
      "IF(SUM(iot_reads) IS NOT NULL, SUM(iot_reads), 0) AS iot_reads, " .
      "IF(SUM(iot_writes) IS NOT NULL, SUM(iot_writes), 0) AS iot_writes, " .
      "IF(SUM(etl_writes) IS NOT NULL, SUM(etl_writes), 0) AS etl_writes, " .
      "IF(AVG(devices_public) IS NOT NULL, SUM(devices_public), 0) AS devices_public, IF(devices_private IS NOT NULL, SUM(devices_private), 0) AS devices_private, IF(dashboards_public IS NOT NULL, SUM(dashboards_public), 0) AS dashboards_public, " .
      "IF(AVG(dashboards_private) IS NOT NULL, SUM(dashboards_private), 0) AS dashboards_private, '' AS date " .
      "FROM " . $_REQUEST["db"] . "." . $_REQUEST["table"] . ", (SELECT @i:=0) AS f WHERE date >= CURDATE() - INTERVAL " . $_REQUEST["days"] . " DAY";
     */
    $query .= "SELECT @i:=@i+1 AS id, a.username, b.role, b.level, " .
            "IF(SUM(iot_db_storage_tx) IS NOT NULL, ROUND(SUM(iot_db_storage_tx), 2), 0) AS iot_db_storage_tx, " .
            "IF(SUM(iot_db_storage_rx) IS NOT NULL, ROUND(SUM(iot_db_storage_rx), 2), 0) AS iot_db_storage_rx, " .
            "IF(SUM(iot_filesystem_storage_tx) IS NOT NULL, ROUND(SUM(iot_filesystem_storage_tx), 2), 0) AS iot_filesystem_storage_tx, " .
            "IF(SUM(iot_filesystem_storage_rx) IS NOT NULL, ROUND(SUM(iot_filesystem_storage_rx), 2), 0) AS iot_filesystem_storage_rx, " .
            "IF(SUM(iot_db_request_tx) IS NOT NULL, ROUND(SUM(iot_db_request_tx), 2), 0) AS iot_db_request_tx, " .
            "IF(SUM(iot_db_request_rx) IS NOT NULL, ROUND(SUM(iot_db_request_rx), 2), 0) AS iot_db_request_rx, " .
            "IF(SUM(iot_ascapi_tx) IS NOT NULL, ROUND(SUM(iot_ascapi_tx), 2), 0) AS iot_ascapi_tx, " .
            "IF(SUM(iot_ascapi_rx) IS NOT NULL, ROUND(SUM(iot_ascapi_rx), 2), 0) AS iot_ascapi_rx, " .
            "IF(SUM(iot_disces_tx) IS NOT NULL, ROUND(SUM(iot_disces_tx), 2), 0) AS iot_disces_tx, " .
            "IF(SUM(iot_disces_rx) IS NOT NULL, ROUND(SUM(iot_disces_rx), 2), 0) AS iot_disces_rx, " .
            "IF(SUM(iot_dashboard_tx) IS NOT NULL, ROUND(SUM(iot_dashboard_tx), 2), 0) AS iot_dashboard_tx, " .
            "IF(SUM(iot_dashboard_rx) IS NOT NULL, ROUND(SUM(iot_dashboard_rx), 2), 0) AS iot_dashboard_rx, " .
            "IF(SUM(iot_datagate_tx) IS NOT NULL, ROUND(SUM(iot_datagate_tx), 2), 0) AS iot_datagate_tx, " .
            "IF(SUM(iot_datagate_rx) IS NOT NULL, ROUND(SUM(iot_datagate_rx), 2), 0) AS iot_datagate_rx, " .
            "IF(SUM(iot_external_service_tx) IS NOT NULL, ROUND(SUM(iot_external_service_tx), 2), 0) AS iot_external_service_tx, " .
            "IF(SUM(iot_external_service_rx) IS NOT NULL, ROUND(SUM(iot_external_service_rx), 2), 0) AS iot_external_service_rx, " .
            "IF(SUM(iot_iot_service_tx) IS NOT NULL, ROUND(SUM(iot_iot_service_tx), 2), 0) AS iot_iot_service_tx, " .
            "IF(SUM(iot_iot_service_rx) IS NOT NULL, ROUND(SUM(iot_iot_service_rx), 2), 0) AS iot_iot_service_rx, " .
            "IF(SUM(iot_mapping_tx) IS NOT NULL, ROUND(SUM(iot_mapping_tx), 2), 0) AS iot_mapping_tx, " .
            "IF(SUM(iot_mapping_rx) IS NOT NULL, ROUND(SUM(iot_mapping_rx), 2), 0) AS iot_mapping_rx, " .
            "IF(SUM(iot_microserviceusercreated_tx) IS NOT NULL, ROUND(SUM(iot_microserviceusercreated_tx), 2), 0) AS iot_microserviceusercreated_tx, " .
            "IF(SUM(iot_microserviceusercreated_rx) IS NOT NULL, ROUND(SUM(iot_microserviceusercreated_rx), 2), 0) AS iot_microserviceusercreated_rx, " .
            "IF(SUM(iot_mydata_tx) IS NOT NULL, ROUND(SUM(iot_mydata_tx), 2), 0) AS iot_mydata_tx, " .
            "IF(SUM(iot_mydata_rx) IS NOT NULL, ROUND(SUM(iot_mydata_rx), 2), 0) AS iot_mydata_rx, " .
            "IF(SUM(iot_notificator_tx) IS NOT NULL, ROUND(SUM(iot_notificator_tx), 2), 0) AS iot_notificator_tx, " .
            "IF(SUM(iot_notificator_rx) IS NOT NULL, ROUND(SUM(iot_notificator_rx), 2), 0) AS iot_notificator_rx, " .
            "IF(SUM(iot_rstatistics_tx) IS NOT NULL, ROUND(SUM(iot_rstatistics_tx), 2), 0) AS iot_rstatistics_tx, " .
            "IF(SUM(iot_rstatistics_rx) IS NOT NULL, ROUND(SUM(iot_rstatistics_rx), 2), 0) AS iot_rstatistics_rx, " .
            "IF(SUM(iot_sigfox_tx) IS NOT NULL, ROUND(SUM(iot_sigfox_tx), 2), 0) AS iot_sigfox_tx, " .
            "IF(SUM(iot_sigfox_rx) IS NOT NULL, ROUND(SUM(iot_sigfox_rx), 2), 0) AS iot_sigfox_rx, " .
            "IF(SUM(iot_undefined_tx) IS NOT NULL, ROUND(SUM(iot_undefined_tx), 2), 0) AS iot_undefined_tx, " .
            "IF(SUM(iot_undefined_rx) IS NOT NULL, ROUND(SUM(iot_undefined_rx), 2), 0) AS iot_undefined_rx, " .
            "IF(SUM(iot_tx) IS NOT NULL, ROUND(SUM(iot_tx), 2), 0) AS iot_tx, " .
            "IF(SUM(iot_rx) IS NOT NULL, ROUND(SUM(iot_rx), 2), 0) AS iot_rx, " .
            "IF(SUM(iot_apps) IS NOT NULL, SUM(iot_apps), 0) AS iot_apps, " .
            "IF(SUM(iot_reads) IS NOT NULL, SUM(iot_reads), 0) AS iot_reads, " .
            "IF(SUM(iot_writes) IS NOT NULL, SUM(iot_writes), 0) AS iot_writes, " .
            "IF(SUM(etl_writes) IS NOT NULL, SUM(etl_writes), 0) AS etl_writes, " .
            "IF(AVG(devices_public) IS NOT NULL, SUM(devices_public), 0) AS devices_public, " .
            "IF(AVG(devices_private) IS NOT NULL, SUM(devices_private), 0) AS devices_private, " .
            "IF(AVG(dashboards_public) IS NOT NULL, SUM(dashboards_public), 0) AS dashboards_public, " .
            "IF(AVG(dashboards_private) IS NOT NULL, SUM(dashboards_private), 0) AS dashboards_private, " .
            "IF(SUM(dashboards_accesses) IS NOT NULL, SUM(dashboards_accesses), 0) AS dashboards_accesses, " .
            "IF(SUM(dashboards_minutes) IS NOT NULL, SUM(dashboards_minutes), 0) AS dashboards_minutes, " .
            "'' AS date " .
            "FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " a " .
            "LEFT JOIN " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . ".roles_levels b ON a.username = b.username, " .
            "(SELECT @i:=0) AS f WHERE a.date >= CURDATE() - INTERVAL " . mysqli_real_escape_string($connection, $_REQUEST["days"]) . " DAY";
}

if ($_REQUEST["table"] == "links" || $_REQUEST["table"] == "rules") {
    $query = "SELECT * FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]);
}

if (!empty($_REQUEST["searchPhrase"])) {
    $fields = json_decode(urldecode($_REQUEST["fields"]));
    for ($i = 0; $i < count($fields); $i++) {
        if ($i == 0) {
            $query .= "AND (" . $fields[$i] . " LIKE '%" . mysqli_real_escape_string($connection, $_REQUEST["searchPhrase"]) . "%'";
        } else {
            $query .= "OR " . $fields[$i] . " LIKE '%" . mysqli_real_escape_string($connection, $_REQUEST["searchPhrase"]) . "%'";
        }
    }
    $query .= ')';
}

if (($_REQUEST["days"] == 7 || $_REQUEST["days"] == 30 || $_REQUEST["days"] == 90) && $_REQUEST["db"] == 'iot' && $_REQUEST["table"] == 'data') {
    $query .= ' GROUP BY username';
}

// calculate total rows
$result = mysqli_query($connection, $query);
$total_records = mysqli_num_rows($result);

$order_by = "";

if (isset($_POST["sort"]) && is_array($_POST["sort"])) {
    foreach ($_POST["sort"] as $key => $value) {
        $order_by .= mysqli_real_escape_string($connection, $key) . " " . mysqli_real_escape_string($connection, $value) . ", ";
    }
} else {
    $query .= " ORDER BY id DESC ";
}

if ($order_by != "") {
    $query .= " ORDER BY " . substr($order_by, 0, -2);
}

if ($records_per_page != -1) {
    $query .= " LIMIT " . mysqli_real_escape_string($connection, $start_from) . ", " . mysqli_real_escape_string($connection, $records_per_page);
}

$result = mysqli_query($connection, $query);

while ($row = mysqli_fetch_assoc($result)) {
    $data[] = $row;
}

$output = array(
    'current' => intval($_POST["current"]),
    'rowCount' => 10,
    'total' => intval($total_records),
    'rows' => $data
);
//$fp = fopen("/var/www/html/userstats/log.txt", "at");
//fwrite($fp, $query . "\n");
//fclose($fp);
$connection->close();
echo json_encode($output);
?>
