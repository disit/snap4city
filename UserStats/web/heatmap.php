<?php

/*
Snap4city -- heatmap.php --
   Copyright (C) 2020 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
header('Access-Control-Allow-Origin: *');
include("connection.php");
$data = array();

if (isset($_REQUEST["days"]) && isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $query = "SELECT latitude, longitude, num FROM iot.heatmap WHERE date >= NOW() - INTERVAL " . mysqli_real_escape_string($connection, $_REQUEST["days"]) . " DAY AND dataset = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "'" .
            " AND latitude >= " . mysqli_real_escape_string($connection, $_REQUEST["latitude_min"]) .
            " AND latitude <= " . mysqli_real_escape_string($connection, $_REQUEST["latitude_max"]) .
            " AND longitude >= " . mysqli_real_escape_string($connection, $_REQUEST["longitude_min"]) .
            " AND longitude <= " . mysqli_real_escape_string($connection, $_REQUEST["longitude_max"]);
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => intval($row["num"]));
    }
} else if (isset($_REQUEST["date"]) && isset($_REQUEST["latitude_min"]) && isset($_REQUEST["latitude_max"]) &&
        isset($_REQUEST["longitude_min"]) && isset($_REQUEST["longitude_max"]) && isset($_REQUEST["dataset"])) {
    $query = "SELECT latitude, longitude, num FROM iot.heatmap WHERE date ='" . mysqli_real_escape_string($connection, $_REQUEST["date"]) . "' AND dataset = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "'" .
            " AND latitude >= " . mysqli_real_escape_string($connection, $_REQUEST["latitude_min"]) .
            " AND latitude <= " . mysqli_real_escape_string($connection, $_REQUEST["latitude_max"]) .
            " AND longitude >= " . mysqli_real_escape_string($connection, $_REQUEST["longitude_min"]) .
            " AND longitude <= " . mysqli_real_escape_string($connection, $_REQUEST["longitude_max"]);
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => doubleval($row["latitude"]), "lng" => doubleval($row["longitude"]), "count" => intval($row["num"]));
    }
}

mysqli_close($connection);

echo json_encode($data);
?>
