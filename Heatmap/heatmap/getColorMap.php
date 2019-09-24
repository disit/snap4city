<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
include_once 'settings.php';
$data = array();
if (isset($_REQUEST["metricName"])) {
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT `min`, `max`, rgb FROM heatmap.colors WHERE metric_name = '" . mysqli_real_escape_string($connection, $_REQUEST["metricName"]) . "' ORDER BY `order`";
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
     $data[] = $row;
    }
    mysqli_close($connection);
}
echo json_encode($data/*, JSON_PRETTY_PRINT*/);
?>
