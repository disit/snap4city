<?php

// set unlimited memory usage for server
ini_set('memory_limit', '-1');

// generate the heatmap of users positions for not banned users in [minLat, maxLat] [minLon, maxLon] for any provider (i.e., gps, network, fused)
include_once "../settings.php";

function getHeatmapDistinct() {
    global $config;
    global $time;
    //CONNECT
    $link = mysqli_connect($config['sensors_host'], $config['sensors_user'], $config['sensors_pass'], $config['sensors_database']);

    /* check connection */
    if (mysqli_connect_errno()) {
        printf("Connection failed: %s\n", mysqli_connect_error());
        exit();
    }
    // GET DATA
    $data = array();
    $distinct_users = array();
    if ($_REQUEST["to"] == "") {
        $date = isset($_REQUEST["minutes"]) ? " AND date > '" . $time . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE" : " AND date > '" . $time . "'";
    } else {
        $date = isset($_REQUEST["minutes"]) ? " AND date >= '" . $_REQUEST["to"] . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE AND date <= '" . $_REQUEST["to"] . "'" : " AND date <= '" . $_REQUEST["to"] . "'";
    }
    $profile = $_REQUEST["profile"] != "Global" ? ($_REQUEST["profile"] != "Unknown" ? "WHERE profile = \"" . $_REQUEST["profile"] . "\" AND" : "WHERE profile IS NULL AND") : "WHERE";
    $markers = "markers.clearLayers();";
    $sql1 = "SELECT DISTINCT user FROM sensors.user_eval a LEFT JOIN sensors.users b ON a.device_id = b.user "
            . $profile . " user IS NOT NULL AND latitude > " . $config["min_latitude"] . " AND latitude < "
            . $config["max_latitude"] . " AND longitude > " . $config["min_longitude"] . " AND longitude < "
            . $config["max_longitude"] . $date;
    $result1 = mysqli_query($link, $sql1) or die(mysqli_error());
    $num = 0;
    while ($row1 = mysqli_fetch_assoc($result1)) {
        $distinct_users[] = $row1["user"];
        //$device_id = $row1["user"] != "null" ? "= \"" . $row1["user"] . "\"" : "IS NULL";
        $sql2 = "SELECT date, device_id, profile, latitude, longitude, speed, altitude, accuracy, heading, "
                . "provider, lat_pre_scan, long_pre_scan, date_pre_scan, prev_status, curr_status "
                . "FROM sensors.user_eval a LEFT JOIN sensors.users b ON a.device_id = b.user WHERE device_id = '" . $row1["user"] . "'"
                . " AND latitude > " . $config["min_latitude"] . " AND latitude < " . $config["max_latitude"] . " AND longitude > "
                . $config["min_longitude"] . " AND longitude < " . $config["max_longitude"] . $date . " ORDER BY date DESC LIMIT 1";
        $result2 = mysqli_query($link, $sql2) or die(mysqli_error());
        $num += mysqli_num_rows($result2);
        while ($row2 = mysqli_fetch_assoc($result2)) {
            $data[] = array("lat" => $row2["latitude"], "lng" => $row2["longitude"], "count" => 1);
            $marker_icon = $row2["provider"] != 'gps' ? ($row2["provider"] == 'network' ? ",{icon: markerIconRed}" : ",{icon: markerIconBlack}") : "";
            $markers .= "markers.addLayer(L.marker([" . $row2["latitude"] . "," . $row2["longitude"] . "]" . $marker_icon . ").bindPopup('Date: " . $row2["date"] . ""
                    . "<br>#: " . substr($row2["device_id"], -10) . ""
                    . "<br>Lat: " . $row2["latitude"] . ""
                    . "<br>Lon: " . $row2["longitude"] . ""
                    . "<br>Profile: " . $row2["profile"] . ""
                    . "<br>Speed: " . $row2["speed"] . " m/s"
                    . "<br>Altitude: " . $row2["altitude"] . " m"
                    . "<br>Accuracy: " . $row2["accuracy"] . " m"
                    . "<br>Heading: " . $row2["heading"] . ""
                    . "<br>Provider: " . $row2["provider"] . ""
                    . "<br>Lat_pre_scan: " . $row2["lat_pre_scan"] . ""
                    . "<br>Lon_pre_scan: " . $row2["long_pre_scan"] . ""
                    . "<br>Date_pre_scan: " . $row2["date_pre_scan"] . ""
                    . "<br>Prev_status: " . $row2["prev_status"] . ""
                    . "<br>Curr_status: " . $row2["curr_status"] . "<br>'));";
        }
    }
    $num_profiles = array();
    // if profile is Global then calculate the number of distinct users for each profile
    if ($_REQUEST["profile"] == 'Global') {
        $profiles = array("All", "Citizen", "Commuter", "Student", "Tourist", "Disabled", "Operator");
        $sum = 0;
        foreach ($profiles as $prf) {
            $sql3 = "SELECT COUNT(DISTINCT user) AS num FROM sensors.user_eval a LEFT JOIN sensors.users b ON a.device_id = b.user WHERE profile = '" . $prf
                    . "' AND latitude > " . $config["min_latitude"] . " AND latitude < " . $config["max_latitude"] . " AND longitude > "
                    . $config["min_longitude"] . " AND longitude < " . $config["max_longitude"] . $date;
            $result3 = mysqli_query($link, $sql3) or die(mysqli_error());
            while ($row3 = mysqli_fetch_assoc($result3)) {
                $num_profiles[$prf] = intval($row3["num"]);
                $sum += intval($row3["num"]);
            }
        }
        $num_profiles["Unknown"] = $num - $sum;
    }
    // get location clustered markers
    $clustered_markers = getDistinctPeopleClusteredMarkers($link, $distinct_users);
    // get location-clustered movements markers
    $movements_markers = getPeopleMovementsMarkers($link);
    //close connection
    mysqli_close($link);

    //$fp = fopen("/var/www/html/log.txt", "at");
    //fwrite($fp, $sql1 . "\n");
    //fclose($fp);
    if (count($num_profiles) > 0) {
        echo json_encode(array($num, $data, $markers, $clustered_markers, $movements_markers, $num_profiles));
    } else {
        echo json_encode(array($num, $data, $markers, $clustered_markers, $movements_markers));
    }
}

// includes users with uid = NULL for profile "Global"
function getHeatmap() {
    global $config;
    global $time;
    //CONNECT
    $link = mysqli_connect($config['sensors_host'], $config['sensors_user'], $config['sensors_pass'], $config['sensors_database']);

    /* check connection */
    if (mysqli_connect_errno()) {
        printf("Connection failed: %s\n", mysqli_connect_error());
        exit();
    }
    // GET DATA
    $data = array();
    if ($_REQUEST["to"] == "") {
        $date = isset($_REQUEST["minutes"]) ? " AND date > '" . $time . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE" : " AND date > '" . $time . "'";
    } else {
        $date = isset($_REQUEST["minutes"]) ? " AND date >= '" . $_REQUEST["to"] . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE AND date <= '" . $_REQUEST["to"] . "'" : " AND date <= '" . $_REQUEST["to"] . "'";
    }
    $profile = $_REQUEST["profile"] != "Global" ? ($_REQUEST["profile"] != "Unknown" ? "WHERE b.profile = \"" . $_REQUEST["profile"] . "\" AND" : "WHERE b.profile IS NULL AND") : "WHERE";
    $markers = "markers.clearLayers();";
    $sql = "SELECT date, device_id, b.profile, latitude, longitude, speed, altitude, accuracy, heading,"
            . " provider, lat_pre_scan, long_pre_scan, date_pre_scan, prev_status, curr_status "
            . "FROM sensors.user_eval a LEFT JOIN sensors.users b ON a.device_id = b.user " . $profile
            . " latitude > " . $config["min_latitude"] . " AND latitude < " . $config["max_latitude"]
            . " AND longitude > " . $config["min_longitude"] . " AND longitude < " . $config["max_longitude"]
            . $date;
    $result = mysqli_query($link, $sql) or die(mysqli_error());
    $num = mysqli_num_rows($result);
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array("lat" => $row["latitude"], "lng" => $row["longitude"], "count" => 1);
        $marker_icon = $row["provider"] != 'gps' ? ($row["provider"] == 'network' ? ",{icon: markerIconRed}" : ",{icon: markerIconBlack}") : "";
        $markers .= "markers.addLayer(L.marker([" . $row["latitude"] . "," . $row["longitude"] . "]" . $marker_icon . ").bindPopup('Date: " . $row["date"] . ""
                . "<br>#: " . substr($row["device_id"], -10) . ""
                . "<br>Lat: " . $row["latitude"] . ""
                . "<br>Lon: " . $row["longitude"] . ""
                . "<br>Profile: " . $row["profile"] . ""
                . "<br>Speed: " . $row["speed"] . " m/s"
                . "<br>Altitude: " . $row["altitude"] . " m"
                . "<br>Accuracy: " . $row["accuracy"] . " m"
                . "<br>Heading: " . $row["heading"] . ""
                . "<br>Provider: " . $row["provider"] . ""
                . "<br>Lat_pre_scan: " . $row["lat_pre_scan"] . ""
                . "<br>Lon_pre_scan: " . $row["long_pre_scan"] . ""
                . "<br>Date_pre_scan: " . $row["date_pre_scan"] . ""
                . "<br>Prev_status: " . $row["prev_status"] . ""
                . "<br>Curr_status: " . $row["curr_status"] . "<br>'));";
    }
    $num_profiles = array();
    // if profile is Global then calculate the number of users for each profile
    if ($_REQUEST["profile"] == 'Global') {
        $profiles = array("All", "Citizen", "Commuter", "Student", "Tourist", "Disabled", "Operator");
        $sum = 0;
        foreach ($profiles as $prf) {
            $sql3 = "SELECT COUNT(*) AS num FROM sensors.user_eval a LEFT JOIN sensors.users b ON a.device_id = b.user WHERE b.profile = '" . $prf
                    . "' AND latitude > " . $config["min_latitude"] . " AND latitude < " . $config["max_latitude"] . " AND longitude > "
                    . $config["min_longitude"] . " AND longitude < " . $config["max_longitude"] . $date;
            $result3 = mysqli_query($link, $sql3) or die(mysqli_error());
            while ($row3 = mysqli_fetch_assoc($result3)) {
                $num_profiles[$prf] = intval($row3["num"]);
                $sum += intval($row3["num"]);
            }
        }
        $num_profiles["Unknown"] = $num - $sum;
    }
    // get location-clustered markers
    $clustered_markers = getPeopleClusteredMarkers($link);
    // get location-clustered movements markers
    $movements_markers = getPeopleMovementsMarkers($link);
    //close connection
    mysqli_close($link);

    //$fp = fopen("/var/www/html/log.txt", "at");
    //fwrite($fp, $markers . "\n");
    //fclose($fp);
    if (count($num_profiles) > 0) {
        echo json_encode(array($num, $data, $markers, $clustered_markers, $movements_markers, $num_profiles));
    } else {
        echo json_encode(array($num, $data, $markers, $clustered_markers, $movements_markers));
    }
}

//get people location-clustered markers
// includes users with uid = NULL for profile "Global"
function getPeopleClusteredMarkers($link) {
    global $config;
    global $time;
    $markers = "peopleClusteredMarkers.clearLayers();";
    if ($_REQUEST["to"] == "") {
        $date = isset($_REQUEST["minutes"]) ? " AND date > '" . $time . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE" : " AND date > '" . $time . "'";
    } else {
        $date = isset($_REQUEST["minutes"]) ? " AND date >= '" . $_REQUEST["to"] . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE AND date <= '" . $_REQUEST["to"] . "'" : " AND date <= '" . $_REQUEST["to"] . "'";
    }
    $profile = $_REQUEST["profile"] != "Global" ? ($_REQUEST["profile"] != "Unknown" ? "WHERE b.profile = \"" . $_REQUEST["profile"] . "\" AND" : "WHERE b.profile IS NULL AND") : "WHERE";
    $sql = "SELECT (2*atan(exp(cc_y/6371000))-PI()/2)*180/PI() AS latitude, 
        cc_x/6371000*180/PI() AS longitude, b.profile, a.date,
        a.device_id, a.speed, a.altitude, a.accuracy, a.heading,
        a.provider, a.lat_pre_scan, a.long_pre_scan, 
        a.date_pre_scan, a.prev_status, a.curr_status 
        FROM sensors.user_eval a LEFT JOIN sensors.users b ON a.device_id = b.user " .
            $profile . " latitude > " . $config["min_latitude"] .
            " AND latitude < " . $config["max_latitude"] . " AND longitude > " .
            $config["min_longitude"] . " AND longitude < " . $config["max_longitude"] .
            $date . " GROUP BY cc_x, cc_y";
    $result = mysqli_query($link, $sql) or die(mysqli_error());
    while ($row = mysqli_fetch_assoc($result)) {
        $marker_icon = $row["provider"] != 'gps' ? ($row["provider"] == 'network' ? ",{icon: markerIconRed}" : ",{icon: markerIconBlack}") : "";
        $markers .= "peopleClusteredMarkers.addLayer(L.marker([" . $row["latitude"] . "," . $row["longitude"] . "]" . $marker_icon . ").bindPopup('Date: " . $row["date"] . ""
                . "<br>#: " . substr($row["device_id"], -10) . ""
                . "<br>Lat: " . $row["latitude"] . ""
                . "<br>Lon: " . $row["longitude"] . ""
                . "<br>Profile: " . $row["profile"] . ""
                . "<br>Speed: " . $row["speed"] . " m/s"
                . "<br>Altitude: " . $row["altitude"] . " m"
                . "<br>Accuracy: " . $row["accuracy"] . " m"
                . "<br>Heading: " . $row["heading"] . ""
                . "<br>Provider: " . $row["provider"] . ""
                . "<br>Lat_pre_scan: " . $row["lat_pre_scan"] . ""
                . "<br>Lon_pre_scan: " . $row["long_pre_scan"] . ""
                . "<br>Date_pre_scan: " . $row["date_pre_scan"] . ""
                . "<br>Prev_status: " . $row["prev_status"] . ""
                . "<br>Curr_status: " . $row["curr_status"] . "<br>'));";
    }
    return $markers;
}

//get distinct people location-clustered markers
function getDistinctPeopleClusteredMarkers($link, $distinct_users) {
    global $config;
    global $time;
    $markers = "peopleClusteredMarkers.clearLayers();";
    if ($_REQUEST["to"] == "") {
        $date = isset($_REQUEST["minutes"]) ? " AND date > '" . $time . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE" : " AND date > '" . $time . "'";
    } else {
        $date = isset($_REQUEST["minutes"]) ? " AND date >= '" . $_REQUEST["to"] . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE AND date <= '" . $_REQUEST["to"] . "'" : " AND date <= '" . $_REQUEST["to"] . "'";
    }
    foreach ($distinct_users as $user) {
        $sql = "SELECT (2*atan(exp(cc_y/6371000))-PI()/2)*180/PI() AS latitude, 
        cc_x/6371000*180/PI() AS longitude, b.profile, a.date,
        a.device_id, a.speed, a.altitude, a.accuracy, a.heading,
        a.provider, a.lat_pre_scan, a.long_pre_scan, 
        a.date_pre_scan, a.prev_status, a.curr_status 
        FROM sensors.user_eval a LEFT JOIN sensors.users b ON a.device_id = b.user " .
                "WHERE a.device_id = '" . $user . "' AND latitude > " . $config["min_latitude"] .
                " AND latitude < " . $config["max_latitude"] . " AND longitude > " .
                $config["min_longitude"] . " AND longitude < " . $config["max_longitude"] .
                $date . " GROUP BY cc_x, cc_y ORDER BY date DESC LIMIT 1";
        $result = mysqli_query($link, $sql) or die(mysqli_error());
        while ($row = mysqli_fetch_assoc($result)) {
            $marker_icon = $row["provider"] != 'gps' ? ($row["provider"] == 'network' ? ",{icon: markerIconRed}" : ",{icon: markerIconBlack}") : "";
            $markers .= "peopleClusteredMarkers.addLayer(L.marker([" . $row["latitude"] . "," . $row["longitude"] . "]" . $marker_icon . ").bindPopup('Date: " . $row["date"] . ""
                    . "<br>#: " . substr($row["device_id"], -10) . ""
                    . "<br>Lat: " . $row["latitude"] . ""
                    . "<br>Lon: " . $row["longitude"] . ""
                    . "<br>Profile: " . $row["profile"] . ""
                    . "<br>Speed: " . $row["speed"] . " m/s"
                    . "<br>Altitude: " . $row["altitude"] . " m"
                    . "<br>Accuracy: " . $row["accuracy"] . " m"
                    . "<br>Heading: " . $row["heading"] . ""
                    . "<br>Provider: " . $row["provider"] . ""
                    . "<br>Lat_pre_scan: " . $row["lat_pre_scan"] . ""
                    . "<br>Lon_pre_scan: " . $row["long_pre_scan"] . ""
                    . "<br>Date_pre_scan: " . $row["date_pre_scan"] . ""
                    . "<br>Prev_status: " . $row["prev_status"] . ""
                    . "<br>Curr_status: " . $row["curr_status"] . "<br>'));";
        }
    }
    return $markers;
}

// get people movements markers
function getPeopleMovementsMarkers($link) {
    global $config;
    global $time;
    $markers = "movementsMarkers.clearLayers();";
    $markers .= "movementsLayer.clearLayers();";
    $previous = array();
    if ($_REQUEST["to"] == "") {
        $date = isset($_REQUEST["minutes"]) ? " AND date > '" . $time . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE" : " AND date > '" . $time . "'";
    } else {
        $date = isset($_REQUEST["minutes"]) ? " AND date >= '" . $_REQUEST["to"] . "' - INTERVAL " . $_REQUEST["minutes"] . " MINUTE AND date <= '" . $_REQUEST["to"] . "'" : " AND date <= '" . $_REQUEST["to"] . "'";
    }
    $profile = $_REQUEST["profile"] != "Global" ? ($_REQUEST["profile"] != "Unknown" ? "WHERE profile = \"" . $_REQUEST["profile"] . "\" AND" : "WHERE profile IS NULL AND") : "WHERE";
    $sql = "SELECT * FROM sensors.user_eval a LEFT JOIN sensors.users b ON a.device_id = b.user " .
            $profile . " latitude > " . $config["min_latitude"] .
            " AND latitude < " . $config["max_latitude"] . " AND longitude > " .
            $config["min_longitude"] . " AND longitude < " . $config["max_longitude"] .
            " AND device_id IS NOT NULL AND (curr_status_time_new IS NOT NULL OR last_status_row IS NOT NULL)" .
            $date . " ORDER BY device_id, user_eval_id";
    $result = mysqli_query($link, $sql) or die(mysqli_error());
    $i = 0;
    while ($row = mysqli_fetch_assoc($result)) {
        //$profile = isset($_REQUEST["profile"]) ? $_REQUEST["profile"] : "";
        $latitude = $row["latitude"];
        $longitude = $row["longitude"];
        $centroid = false;
        if ($row["curr_status"] == "stay") {
            if (!is_null($row["lat_centroid"]) && !is_null($row["lon_centroid"])) {
                $latitude = $row["lat_centroid"];
                $longitude = $row["lon_centroid"];
                $centroid = true;
            }
        }
        $marker_icon = $row["provider"] != 'gps' ? ($row["provider"] == 'network' ? ",{icon: markerIconRed}" : ",{icon: markerIconBlack}") : "";
        $markers .= "var movementMarker_" . $i . " = L.marker([" . $row["latitude"] . "," . $row["longitude"] . "]" . $marker_icon . ").bindPopup('Date: " . $row["date"] . ""
                . "<br>#: " . substr($row["device_id"], -10) . ""
                . "<br>Lat: " . $latitude . ($centroid ? " (centroid)" : "")
                . "<br>Lon: " . $longitude . ($centroid ? " (centroid)" : "")
                . "<br>Profile: " . $row["profile"] . ""
                . "<br>Speed: " . $row["speed"] . " m/s"
                . "<br>Altitude: " . $row["altitude"] . " m"
                . "<br>Accuracy: " . $row["accuracy"] . " m"
                . "<br>Heading: " . $row["heading"] . ""
                . "<br>Provider: " . $row["provider"] . ""
                . "<br>Lat_pre_scan: " . $row["lat_pre_scan"] . ""
                . "<br>Lon_pre_scan: " . $row["long_pre_scan"] . ""
                . "<br>Date_pre_scan: " . $row["date_pre_scan"] . ""
                . "<br>Prev_status: " . $row["prev_status"] . ""
                . "<br>Curr_status: " . $row["curr_status"] . ""
                . "<br>Status: " . $row["curr_status_new"] . ""
                . "<br>Elapsed time: " . (isset($row["curr_status_time_new"]) ? parseTime(intval($row["curr_status_time_new"])) : "-") . "<br>');";
        $markers .= "movementsMarkers.addLayer(movementMarker_" . $i . ");";
        // if a previous point is present for this user, then add a polyline joining that with this one
        if (isset($previous["user"]) && $previous["user"] == $row["device_id"]) {
            // get distance in km between this and previous coordinate
            $distance = distFrom($previous["latitude"], $previous["longitude"], doubleval($row["latitude"]), doubleval($row["longitude"]));
            // get elapsed time in s between this and previous time
            $time = strtotime($row["date"]) - strtotime($previous["date"]);
            $color = getColor($time, $distance);
            $markers .= "var movementPolyline_" . $i . " = new L.Polyline([new L.LatLng(" . $previous["latitude"] . "," . $previous["longitude"] . "),new L.LatLng(" . $latitude . "," . $longitude . ")], {color: '" . $color . "', weight: 5, opacity: 0.75, smoothFactor: 1}).bindLabel('Time: " . parseTime($time) . "<br>Distance: " . round($distance, 3) . " km');";
            $markers .= "movementsLayer.addLayer(movementPolyline_" . $i . ");";
            $markers .= "movementsLayer.addLayer(L.polylineDecorator(movementPolyline_" . $i . ", polyline_decorator_pattern));";
        }
        $previous["user"] = $row["device_id"];
        $previous["latitude"] = doubleval($row["latitude"]);
        $previous["longitude"] = doubleval($row["longitude"]);
        $previous["date"] = $row["date"];
        $i++;
    }
    $markers .= "movementsLayer.addLayer(movementsMarkers);";
    return $markers;
}

// format time in seconds
function parseTime($seconds) {
    $t = round($seconds);
    return sprintf('%02dh %02dm %02ds', ($t / 3600), ($t / 60 % 60), $t % 60);
}

// get the polyline color from the user status http://www.rapidtables.com/web/color/blue-color.htm
function getColor($time, $distance) {
    if ($time == 0) {
        return "black";
    }
    // time in h
    $time /= 3600;
    // speed in km/h
    $speed = $distance / $time;
    $result = "";
    /* switch ($status) {
      case "stay":
      $result = "lightblue";
      break;
      case "walk":
      $result = "lightsteelblue";
      break;
      case "car-moto-bus":
      $result = "cadetblue";
      break;
      case "bus/car/moto":
      $result = "cadetblue";
      break;
      case "car-moto-train":
      $result = "royalblue";
      break;
      case "car/moto/train":
      $result = "royalblue";
      break;
      case "car-train":
      $result = "blue";
      break;
      case "car/train":
      $result = "blue";
      break;
      case "train":
      $result = "darkblue";
      break;
      // error in the database, correct is airplane
      case "airplain":
      $result = "black";
      break;
      default:
      $result = "blue";
      } */
    // stay
    if ($speed < 1.0) {
        $result = "lightblue";
    }
    // walk
    else if ($speed < 6.50) {
        $result = "lightsteelblue";
    }
    // car-moto-bus
    else if ($speed < 32.5) {
        $result = "cadetblue";
    }
    // car-moto-train
    else if ($speed < 52.80) {
        $result = "royalblue";
    }
    // car-train
    else if ($speed < 125) {
        $result = "blue";
    }
    // train
    else if ($speed < 300) {
        $result = "darkblue";
    }
    // airplane
    else {
        $result = "black";
    }
    return $result;
}

// calculate distance in km between coordinates in decimal degrees (latitude, longitude)
function distFrom($lat1, $lng1, $lat2, $lng2) {
    if (($lat2 == 0 && $lng2 == 0) || ($lat1 == $lat2 && $lng1 == $lng2))
        return 0;
    $earthRadius = 6371000; //meters
    $dLat = deg2rad($lat2 - $lat1);
    $dLng = deg2rad($lng2 - $lng1);
    $a = sin($dLat / 2) * sin($dLat / 2) + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * sin($dLng / 2) * sin($dLng / 2);
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
    $dist = $earthRadius * $c;

    return $dist / 1000;
}

$time = date("Y-m-d H:i:s");
if ($_REQUEST["distinct"] == 'true') {
    getHeatmapDistinct();
} else {
    getHeatmap();
}
?>
