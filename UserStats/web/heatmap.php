<?php

// http://192.168.0.10/iot/heatmap.php?days=7&latitude_min=43.0&latitude_max=43.32&longitude_min=10.0&longitude_max=11.32
header('Access-Control-Allow-Origin: *');
include("connection.php");
$data = array();

if (isset($_REQUEST["days"]) && isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $query = "SELECT latitude, longitude, num FROM iot.heatmap WHERE date >= NOW() - INTERVAL " . $_REQUEST["days"] . " DAY AND dataset = '" . $_REQUEST["dataset"] . "'" .
            " AND latitude >= " . $_REQUEST["latitude_min"] .
            " AND latitude <= " . $_REQUEST["latitude_max"] .
            " AND longitude >= " . $_REQUEST["longitude_min"] .
            " AND longitude <= " . $_REQUEST["longitude_max"];
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => intval($row["num"]));
    }
} else if (isset($_REQUEST["date"]) && isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $query = "SELECT latitude, longitude, num FROM iot.heatmap WHERE date ='" . $_REQUEST["date"] . "' AND dataset = '" . $_REQUEST["dataset"] . "'" .
            " AND latitude >= " . $_REQUEST["latitude_min"] .
            " AND latitude <= " . $_REQUEST["latitude_max"] .
            " AND longitude >= " . $_REQUEST["longitude_min"] .
            " AND longitude <= " . $_REQUEST["longitude_max"];
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => intval($row["num"]));
    }
}

mysqli_close($connection);

echo json_encode($data);
?>