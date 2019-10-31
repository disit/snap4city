<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
$username = "admin";
$password = "geoserver";
$layer = $_REQUEST["layer"];
$context = stream_context_create(array (
    'http' => array (
        'header' => 'Authorization: Basic ' . base64_encode("$username:$password")
    )
));
$json = file_get_contents("http://localhost:8080/geoserver/rest/workspaces/Snap4City/coveragestores/" . $layer . "/coverages/" . $layer . ".json", false, $context);
$json = json_decode($json, true);
echo json_encode($json["coverage"]["latLonBoundingBox"]);
?>
