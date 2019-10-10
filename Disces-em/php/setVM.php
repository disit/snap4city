<?php
header('Access-Control-Allow-Origin: *');
include_once 'settings.php';
global $config;

$conn = new mysqli($config["mysql_host"], $config["mysql_username"], $config["mysql_password"], $config["mysql_schema"]);
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
$stmt = $conn->prepare("UPDATE quartz.vms_dict SET offline = ? WHERE vm_id = ?");
$stmt->bind_param("ss", $offline, $vm_id);
$vm_id = $_REQUEST["vm_id"];
$offline = $_REQUEST["offline"];
$stmt->execute();
echo "done updating " . $stmt->affected_rows . " rows";
$stmt->close();
$conn->close();
?>
