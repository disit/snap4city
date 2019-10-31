<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
include_once 'settings.php';

require_once __DIR__ . '/Navigator/lib/Treffynnon/Navigator.php';
use Treffynnon\Navigator as N;
use Treffynnon\Navigator\Distance as D;
N::autoloader();

include 'AnthonyMartin/GeoLocation/GeoLocation.php';
use AnthonyMartin\GeoLocation\GeoLocation as GeoLocation;

/**
 * Calculates the great-circle distance between two points, with
 * the Haversine formula.
 * @param float $latitudeFrom Latitude of start point in [deg decimal]
 * @param float $longitudeFrom Longitude of start point in [deg decimal]
 * @param float $latitudeTo Latitude of target point in [deg decimal]
 * @param float $longitudeTo Longitude of target point in [deg decimal]
 * @param float $earthRadius Mean earth radius in [m]
 * @return float Distance between points in [m] (same as earthRadius)
 */
function haversineGreatCircleDistance(
  $latitudeFrom, $longitudeFrom, $latitudeTo, $longitudeTo, $earthRadius = 6371000) {
  // convert from degrees to radians
  $latFrom = deg2rad($latitudeFrom);
  $lonFrom = deg2rad($longitudeFrom);
  $latTo = deg2rad($latitudeTo);
  $lonTo = deg2rad($longitudeTo);

  $latDelta = $latTo - $latFrom;
  $lonDelta = $lonTo - $lonFrom;

  $angle = 2 * asin(sqrt(pow(sin($latDelta / 2), 2) +
    cos($latFrom) * cos($latTo) * pow(sin($lonDelta / 2), 2)));
  return $angle * $earthRadius;
}

// get geographical distance with Vincenty formula (m)
function vincentyDistance($lat_from, $lon_from, $lat_to, $lon_to) {
 $coord1 = new N\LatLong(
    new N\Coordinate($lat_from),
    new N\Coordinate($lon_from)
);
$coord2 = new N\LatLong(
    new N\Coordinate($lat_to),
    new N\Coordinate($lon_to)
);
$Distance = new N\Distance($coord1, $coord2);
$distance = $Distance->get();
return $distance;
}

$data = array();

function getInterpolatedValue($latitude, $longitude, $threshold) {
    global $settings;
    $mapName = "";
    $metricName = "";
    $date = "";
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $smoothing = isset($_REQUEST["smoothing"]) ? $_REQUEST["smoothing"] : 0;
    $power = isset($_REQUEST["power"]) ? $_REQUEST["power"] : 2;
    $distance = False;
    $numerator = 0;
    $denominator = 0;
    // calculate bounding box
    // https://github.com/anthonymartin/GeoLocation.php
    $edison = GeoLocation::fromDegrees($_REQUEST["latitude"], $_REQUEST["longitude"]);
    $coordinates = $edison->boundingCoordinates($threshold/1000, 'kilometers');
    $min_lat = $coordinates[0]->getLatitudeInDegrees();
    $min_lon = $coordinates[0]->getLongitudeInDegrees();
    $max_lat = $coordinates[1]->getLatitudeInDegrees();
    $max_lon = $coordinates[1]->getLongitudeInDegrees();
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT map_name, metric_name, latitude, longitude, value, clustered, `date` " .
             "FROM heatmap.`data` WHERE map_name = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "' AND `date` = '" . mysqli_real_escape_string($connection, $_REQUEST["date"]) . "' AND latitude >= '" . mysqli_real_escape_string($connection, $min_lat) . "' " .
             " AND latitude <= '" . mysqli_real_escape_string($connection, $max_lat) . "' AND longitude >= '" . mysqli_real_escape_string($connection, $min_lon) . "' AND longitude <= '" . mysqli_real_escape_string($connection, $max_lon) . "' AND clustered = " . $clustered;
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $mapName = $row["map_name"];
        $metricName = $row["metric_name"];
        $lat = $row["latitude"];
        $lon = $row["longitude"];
        $v = $row["value"];
        $date = $row["date"];
        $dist = vincentyDistance($_REQUEST["latitude"], $_REQUEST["longitude"], $lat, $lon);
        /*if($dist > $threshold) {
         continue;
        }*/
        if($dist < 0.0000000001) {
            $value = $v;
            $distance = True;
            break;
        }
        $numerator = $numerator + ($v / pow($dist, $power));
        $denominator = $denominator + (1 / pow($dist, $power));
    }
    if(!$distance) {
    $value = 0;
    if($denominator > 0) {
        $value = $numerator / $denominator;
    } else {
        $value = NULL;
    }
}
    mysqli_close($connection);
    return array($value, $metricName);
}

function is_date_old($date, $format = 'Y-m-d H:i:s') {
    $d = DateTime::createFromFormat($format, $date);
    return $d && $d->format($format) == $date;
}

function is_date($date) {
    $a = date_parse($date);
    return $a["error_count"] == 0 && $a["warning_count"] == 0 ? 1 : 0;
}

// calculate interpolation using IDW (Inverse Distance Weighting)
if (is_numeric($_REQUEST["latitude"]) && is_numeric($_REQUEST["longitude"]) && ctype_alnum(str_replace("_", "", $_REQUEST["dataset"])) &&
    is_date($_REQUEST["date"]) && isset($_REQUEST["method"]) && $_REQUEST["method"] == "idw") {
    global $settings;
    $mapName = "";
    $metricName = "";
    $date = "";
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $smoothing = isset($_REQUEST["smoothing"]) ? $_REQUEST["smoothing"] : 0;
    $power = isset($_REQUEST["power"]) ? $_REQUEST["power"] : 2;
    $distance = False;
    $numerator = 0;
    $denominator = 0;
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    $query = "SELECT map_name, metric_name, latitude, longitude, value, clustered, `date` " .
             "FROM heatmap.`data` WHERE map_name = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "' AND `date` = '" . mysqli_real_escape_string($connection, $_REQUEST["date"]) . "' AND clustered = '" . mysqli_real_escape_string($connection, $clustered) ."'";
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $mapName = $row["map_name"];
        $metricName = $row["metric_name"];
        $lat = $row["latitude"];
        $lon = $row["longitude"];
        $v = $row["value"];
        $date = $row["date"];
        //$dist = sqrt(($REQUEST["latitude"]-lat)*($REQUEST["latitude"]-lat) + ($REQUEST["longitude"]-lon)*($REQUEST["longitude"]-lon) + $smoothing*$smoothing);
        //$dist = haversineGreatCircleDistance($_REQUEST["latitude"], $_REQUEST["longitude"], $lat, $lon, 6371000);
        $dist = vincentyDistance($_REQUEST["latitude"], $_REQUEST["longitude"], $lat, $lon);
        if($dist < 0.0000000001) {
            $value = $v;
            $distance = True;
            break;
        }
        $numerator = $numerator + ($v / pow($dist, $power));
        $denominator = $denominator + (1 / pow($dist, $power));
    }
    if(!$distance) {
    $value = 0;
    if($denominator > 0) {
        $value = $numerator / $denominator;
    } else {
        $value = -9999;
    }
}
    $data = array("mapName" => $mapName, "metricName" => $metricName, "date" => $date, "value" => $value);
    mysqli_close($connection);
}
else if (is_numeric($_REQUEST["latitude"]) && is_numeric($_REQUEST["longitude"]) && ctype_alnum(str_replace("_", "", $_REQUEST["dataset"])) && is_date($_REQUEST["date"])) {
    global $settings;
    $value = "";
    $threshold = 2;
    for($i = 1; $i <= 14; $i++) {
     $value_metricName = getInterpolatedValue($_REQUEST["latitude"], $_REQUEST["longitude"], $threshold);
     if($value_metricName[0] != NULL) {
      $value = $value_metricName[0];
      break;
     }
     $threshold *= 2;
    }
    $data = array("mapName" => $_REQUEST["dataset"], "metricName" => $value_metricName[1], "date" => $_REQUEST["date"], "value" => $value);
}

else if (isset($_REQUEST["lat_lon"]) && ctype_alnum(str_replace("_", "", $_REQUEST["dataset"])) && is_date($_REQUEST["date"])) {
    global $settings;
    $mapName = "";
    $metricName = "";
    $date = "";
    $lat_lon_array = json_decode($_REQUEST["lat_lon"]);
    $clustered = isset($_REQUEST["clustered"]) ? $_REQUEST["clustered"] : 0;
    $smoothing = isset($_REQUEST["smoothing"]) ? $_REQUEST["smoothing"] : 0;
    $power = isset($_REQUEST["power"]) ? $_REQUEST["power"] : 2;
    $distance = False;
    $numerator = 0;
    $denominator = 0;
    $connection = mysqli_connect($settings["db_host"], $settings["db_username"], $settings["db_password"], $settings["db_schema"]);
    foreach ($lat_lon_array as $lat_lon) {
     $query = "SELECT map_name, metric_name, latitude, longitude, value, clustered, `date` " .
              "FROM heatmap.`data` WHERE map_name = '" . mysqli_real_escape_string($connection, $_REQUEST["dataset"]) . "' AND `date` = '" . mysqli_real_escape_string($connection, $_REQUEST["date"]) . "' AND clustered = '" . mysqli_real_escape_string($connection, $clustered) . "'";
     $result = mysqli_query($connection, $query);
     while ($row = mysqli_fetch_assoc($result)) {
         $mapName = $row["map_name"];
         $metricName = $row["metric_name"];
         $lat = $row["latitude"];
         $lon = $row["longitude"];
         $v = $row["value"];
         $date = $row["date"];
         //$dist = sqrt(($REQUEST["latitude"]-lat)*($REQUEST["latitude"]-lat) + ($REQUEST["longitude"]-lon)*($REQUEST["longitude"]-lon) + $smoothing*$smoothing);
         //$dist = haversineGreatCircleDistance($_REQUEST["latitude"], $_REQUEST["longitude"], $lat, $lon, 6371000);
         $dist = vincentyDistance($lat_lon[0], $lat_lon[1], $lat, $lon);
         if($dist < 0.0000000001) {
             $value = $v;
             $distance = True;
             break;
         }
         $numerator = $numerator + ($v / pow($dist, $power));
         $denominator = $denominator + (1 / pow($dist, $power));
     }
     if(!$distance) {
      $value = 0;
     if($denominator > 0) {
      $value = $numerator / $denominator;
     } else {
        $value = -9999;
     }
}
    $data[] = array("latitude" => $lat_lon[0], "longitude" => $lat_lon[1], "value" => $value);
  }
  mysqli_close($connection);
}

echo json_encode($data, JSON_PRETTY_PRINT);
?>
