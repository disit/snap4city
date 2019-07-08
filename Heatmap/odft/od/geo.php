<?php

include_once "GeoLocation.php";
$geo = GeoLocation::fromDegrees($_POST["latitude"], $_POST["longitude"]);
$bounding = $geo->boundingCoordinates($_POST["radius"], "km");
echo json_encode(array(array($bounding[0]->getLatitudeInDegrees(), $bounding[0]->getLongitudeInDegrees()),
    array($bounding[1]->getLatitudeInDegrees(), $bounding[1]->getLongitudeInDegrees())));
?>