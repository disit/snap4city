<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
include_once 'settings.php';

$data = array();
if (isset($_REQUEST["dataset"])) {
    $mapData = array();
    $metadata = array();
    $metadata_tmp = array();
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT a.map_name, a.metric_name, a.date, a.description, a.clustered, a.days, a.file, a.org FROM heatmap.metadata a LEFT JOIN heatmap.maps_completed b ON a.map_name = b.map_name AND a.metric_name = b.metric_name AND a.date = b.date WHERE a.map_name = '" . $_REQUEST["dataset"] . "' AND (b.indexed = 1 OR b.completed = -1) ORDER BY a.date DESC";
    $result = mysqli_query($connection, $query);
    $date = NULL;
    // count the number of dates returned
    $count = 1;
    while ($row = mysqli_fetch_assoc($result)) {
         $metadata = array("clustered" => intval($row["clustered"]), "date" => $row["date"], "days" => intval($row["days"]), "file" => intval($row["file"]), "description" => $row["description"], "mapName" => $row["map_name"], "metricName" => $row["metric_name"], "org" => $row["org"]);
         $data[] = array("metadata" => $metadata);
    }
    mysqli_close($connection);
} else if (isset($_REQUEST["dataset"]) && isset($_REQUEST["serviceUri"])) {
    $mapData = array();
    $metadata = array();
    $metadata_tmp = array();
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT a.map_name, a.metric_name, a.date, a.description, a.clustered, a.days FROM heatmap.metadata a LEFT JOIN heatmap.maps_completed b ON a.map_name = b.map_name AND a.metric_name = b.metric_name AND a.date = b.date WHERE a.map_name = '" . $_REQUEST["dataset"] . "' AND service_uris LIKE '%" . $_REQUEST["serviceUri"] . "%' AND b.indexed = 1 ORDER BY a.date DESC";
    $result = mysqli_query($connection, $query);
    $date = NULL;
    // count the number of dates returned
    $count = 1;
    while ($row = mysqli_fetch_assoc($result)) {
         $metadata = array("clustered" => intval($row["clustered"]), "date" => $row["date"], "days" => intval($row["days"]), "description" => $row["description"], "mapName" => $row["map_name"], "metricName" => $row["metric_name"]);
         $data[] = array("metadata" => $metadata);
    }
    mysqli_close($connection);
}
echo json_encode($data, JSON_PRETTY_PRINT);
?>
