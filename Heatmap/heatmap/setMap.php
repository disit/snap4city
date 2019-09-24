<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
include_once 'settings.php';

$data = array();
if (isset($_REQUEST["mapName"]) && isset($_REQUEST["metricName"]) &&
        isset($_REQUEST["date"]) && isset($_REQUEST["completed"])) {
    $mapData = array();
    $metadata = array();
    $metadata_tmp = array();
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "INSERT IGNORE INTO heatmap.`maps_completed` (map_name, metric_name, date, completed) VALUES(" .
    "'" . mysqli_real_escape_string($connection, $_REQUEST["mapName"]) . "', '" .
    mysqli_real_escape_string($connection, $_REQUEST["metricName"]) . "', '" .
    mysqli_real_escape_string($connection, $_REQUEST["date"]) . "', '" .
    mysqli_real_escape_string($connection, $_REQUEST["completed"]) . "')" .
    "ON DUPLICATE KEY UPDATE completed = '" . mysqli_real_escape_string($connection, $_REQUEST["completed"]) . "'";
    $result = mysqli_query($connection, $query);
    mysqli_close($connection);
}
?>
