<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
include_once 'settings.php';

$conn = new mysqli($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
$maps = array();
$stmt = $conn->prepare("SELECT DISTINCT(map_name) FROM maps_completed WHERE indexed = 1");
$stmt->execute();
$result = $stmt->get_result();
while ($myrow = $result->fetch_assoc()) {
 $maps[] = $myrow['map_name'];
}
$stmt->close();
$conn->close();
echo json_encode($maps, true);
?>
