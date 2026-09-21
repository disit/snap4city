<?php
header('Access-Control-Allow-Origin: *');
$data = array();
if (isset($_REQUEST["days"]) && isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $clustered = isset($_REQUEST["clustered"]) ? intval($_REQUEST["clustered"]) : 0;
    $connection = mysqli_connect("192.168.0.59", "root", "ubuntu", "heatmap");
    $days = intval($_REQUEST["days"]);
    $query = "SELECT latitude, longitude, value FROM heatmap.data WHERE date >= NOW() - INTERVAL ? DAY AND map_name = ?" .
            " AND latitude >= ? AND latitude <= ? AND longitude >= ? AND longitude <= ? AND clustered = ?";
    $stmt = mysqli_prepare($connection, $query);
    mysqli_stmt_bind_param($stmt, "isddddi", $days, $_REQUEST["dataset"], $_REQUEST["latitude_min"], $_REQUEST["latitude_max"], $_REQUEST["longitude_min"], $_REQUEST["longitude_max"], $clustered);
    mysqli_stmt_execute($stmt);
    $result = mysqli_stmt_get_result($stmt);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => floatval($row["value"]));
    }
    mysqli_close($connection);
} else if (isset($_REQUEST["date"]) && isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $clustered = isset($_REQUEST["clustered"]) ? intval($_REQUEST["clustered"]) : 0;
    $connection = mysqli_connect("192.168.0.59", "root", "ubuntu", "heatmap");
    $query = "SELECT latitude, longitude, value FROM heatmap.data WHERE date(date) = ? AND map_name = ?" .
            " AND latitude >= ? AND latitude <= ? AND longitude >= ? AND longitude <= ? AND clustered = ?";
    $stmt = mysqli_prepare($connection, $query);
    mysqli_stmt_bind_param($stmt, "ssddddi", $_REQUEST["date"], $_REQUEST["dataset"], $_REQUEST["latitude_min"], $_REQUEST["latitude_max"], $_REQUEST["longitude_min"], $_REQUEST["longitude_max"], $clustered);
    mysqli_stmt_execute($stmt);
    $result = mysqli_stmt_get_result($stmt);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => intval($row["value"]));
    }
    mysqli_close($connection);
} else if (isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $clustered = isset($_REQUEST["clustered"]) ? intval($_REQUEST["clustered"]) : 0;
    $connection = mysqli_connect("192.168.0.59", "root", "ubuntu", "heatmap");
    $query = "SELECT latitude, longitude, value FROM heatmap.data WHERE date = (SELECT MAX(date) WHERE map_name = ?) AND map_name = ?" .
            " AND latitude >= ? AND latitude <= ? AND longitude >= ? AND longitude <= ? AND clustered = ?";
    $stmt = mysqli_prepare($connection, $query);
    mysqli_stmt_bind_param($stmt, "ssddddi", $_REQUEST["dataset"], $_REQUEST["dataset"], $_REQUEST["latitude_min"], $_REQUEST["latitude_max"], $_REQUEST["longitude_min"], $_REQUEST["longitude_max"], $clustered);
    mysqli_stmt_execute($stmt);
    $result = mysqli_stmt_get_result($stmt);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => intval($row["value"]));
    }
    mysqli_close($connection);
}
echo json_encode($data);
?>
