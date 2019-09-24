<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
include_once 'settings.php';

$data = array();
if (isset($_REQUEST["metricName"]) && isset($_REQUEST["value"])) {
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT `min`, `max`, rgb FROM heatmap.colors WHERE metric_name = '" . mysqli_real_escape_string($connection, $_REQUEST["metricName"]) . "' ORDER BY `order`";
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
     $min = $row["min"];
     $max = $row["max"];
     $rgb = json_decode($row["rgb"]);
     if ($min == "") {
	if ($_REQUEST["value"] < $max) {
	 $data=$rgb;
         break;
	}
      } else if ($max == "") {
	if ($_REQUEST["value"] >= $min) {
         $data=$rgb;
         break;
	}
     } else {
	if ($_REQUEST["value"] >= $min && $_REQUEST["value"] < $max) {
         $data=$rgb;
         break;
	}
      }
    }
    mysqli_close($connection);
}
echo json_encode($data/*, JSON_PRETTY_PRINT*/);
?>
