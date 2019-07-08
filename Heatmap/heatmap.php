<?php
header('Access-Control-Allow-Origin: *');
$data = array();
if (isset($_REQUEST["days"]) && isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $connection = mysqli_connect("192.168.0.59", "root", "ubuntu", "heatmap");
    $query = "SELECT latitude, longitude, value FROM heatmap.data WHERE date >= NOW() - INTERVAL " . $_REQUEST["days"] . " DAY AND map_name = '" . $_REQUEST["dataset"] . "'" .
            " AND latitude >= " . $_REQUEST["latitude_min"] .
            " AND latitude <= " . $_REQUEST["latitude_max"] .
            " AND longitude >= " . $_REQUEST["longitude_min"] .
            " AND longitude <= " . $_REQUEST["longitude_max"] .
            " AND clustered = " . $clustered;
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => floatval($row["value"]));
    }
    mysqli_close($connection);
} else if (isset($_REQUEST["date"]) && isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $connection = mysqli_connect("192.168.0.59", "root", "ubuntu", "heatmap");
    $query = "SELECT latitude, longitude, value FROM heatmap.data WHERE date(date) ='" . $_REQUEST["date"] . "' AND map_name = '" . $_REQUEST["dataset"] . "'" .
            " AND latitude >= " . $_REQUEST["latitude_min"] .
            " AND latitude <= " . $_REQUEST["latitude_max"] .
            " AND longitude >= " . $_REQUEST["longitude_min"] .
            " AND longitude <= " . $_REQUEST["longitude_max"] .
            " AND clustered = " . $clustered;
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => intval($row["value"]));
    }
    mysqli_close($connection);
} else if (isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $connection = mysqli_connect("192.168.0.59", "root", "ubuntu", "heatmap");
    $query = "SELECT latitude, longitude, value FROM heatmap.data WHERE date = (SELECT MAX(date) WHERE map_name = '" . $_REQUEST["dataset"] . "') AND map_name = '" . $_REQUEST["dataset"] . "'" .
            " AND latitude >= " . $_REQUEST["latitude_min"] .
            " AND latitude <= " . $_REQUEST["latitude_max"] .
            " AND longitude >= " . $_REQUEST["longitude_min"] .
            " AND longitude <= " . $_REQUEST["longitude_max"] .
            " AND clustered = " . $clustered;
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => intval($row["value"]));
    }
    mysqli_close($connection);
}
echo json_encode($data);
?>
