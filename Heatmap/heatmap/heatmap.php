<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
ini_set('memory_limit', '256M');
include_once 'settings.php';
$data = array();
if (isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"]) && isset($_REQUEST["limit"])) {
    $mapData = array();
    $metadata = array();
    $metadata_tmp = array();
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT a.id, a.map_name, a.metric_name, a.latitude, a.longitude, a.value, a.sum, " .
             "a.num, a.average, a.clustered, a.`date`, b.days, b.description " .
             "FROM heatmap.`data` a LEFT JOIN heatmap.metadata b ON a.map_name = b.map_name AND " .
             "a.metric_name = b.metric_name AND a.`date` = b.`date` AND a.clustered = b.clustered WHERE a.map_name = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "' ORDER BY a.date DESC";
    $result = mysqli_query($connection, $query);
    $date = NULL;
    // count the number of dates returned
    $count = 1;
    while ($row = mysqli_fetch_assoc($result)) {
        if($date != NULL && $date != $row["date"]) {
         $count += 1;
         if($count > intval($_REQUEST["limit"])) {
          break;
         }
         $metadata = array("clustered" => intval($row["clustered"]), "date" => $row["date"], "days" => intval($row["days"]), "description" => $row["description"], "mapName" => $row["map_name"], "metricName" => $row["metric_name"]);
         $data[] = array("data" => $mapData, "metadata" => $metadata_tmp);
         $metadata_tmp = $metadata;
         $mapData = array();
         $metadata = array();
        }
        $metadata = array("clustered" => intval($row["clustered"]), "date" => $row["date"], "days" => intval($row["days"]), "description" => $row["description"], "mapName" => $row["map_name"], "metricName" => $row["metric_name"]);
        $metadata_tmp = $metadata;
        $mapData[] = array("latitude" => doubleval($row["latitude"]), "longitude" => doubleval($row["longitude"]), "value" => doubleval($row["value"]));
        //$metadata = array("clustered" => intval($row["clustered"]), "date" => $row["date"], "days" => intval($row["days"]), "description" => $row["description"], "mapName" => $row["map_name"], "metricName" => $row["metric_name"]);
        $date = $row["date"];
    }
    $data[] = array("data" => $mapData, "metadata" => $metadata_tmp);
    mysqli_close($connection);
} else if (isset($_REQUEST["days"]) && isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT latitude, longitude, value FROM heatmap.data WHERE date >= NOW() - INTERVAL '" . mysqli_real_escape_string($connection, $_REQUEST["days"]) . "' DAY AND map_name = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "'" .
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
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT latitude, longitude, value FROM heatmap.data WHERE date(date) ='" . mysqli_real_escape_string($connection, $_REQUEST["date"]) . "' AND map_name = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "'" .
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
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT latitude, longitude, value FROM heatmap.data WHERE date = (SELECT MAX(date) WHERE map_name = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "') AND map_name = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "'" .
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
} else if (isset($_REQUEST["limit"]) && isset($_REQUEST["sort"]) && isset($_REQUEST["dataset"])) {
    $sort = $_REQUEST["sort"] == "ASC" ? "ASC" : "DESC";
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT date FROM heatmap.metadata WHERE map_name = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "' ORDER BY date " . (strtolower($_REQUEST["sort"]) == "desc" ? "desc" : "asc") . " LIMIT " . (is_numeric($_REQUEST["limit"]) ? $_REQUEST["limit"] : "10");
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = $row["date"];
    }
    mysqli_close($connection);
}
echo json_encode($data, JSON_PRETTY_PRINT);
?>
