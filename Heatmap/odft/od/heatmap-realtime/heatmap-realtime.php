<?php
session_start();

// check the permission
if (!isset($_SESSION["role"])) {
    header("location: ../../ssoLogin.php");
    exit();
}
?>
<?php
// set unlimited memory usage for server
ini_set('memory_limit', '-1');

//include_once "../login.php";
// heatmap of users per profile
//http://www.patrick-wied.at/static/heatmapjs/
//http://jsonviewer.stack.hu/
// calculate distance in km between coordinates in decimal degrees (latitude, longitude)
function distFrom($lat1, $lng1, $lat2, $lng2) {
    if (($lat2 == 0 && $lng2 == 0) || ($lat1 == $lat2 && $lng1 == $lng2))
        return 0;
    $earthRadius = 6371000; // meters
    $dLat = deg2rad($lat2 - $lat1);
    $dLng = deg2rad($lng2 - $lng1);
    $a = sin($dLat / 2) * sin($dLat / 2) + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * sin($dLng / 2) * sin($dLng / 2);
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
    $dist = $earthRadius * $c;

    return $dist / 1000;
}

// calculate distance in km between coordinates in decimal degrees (latitude, longitude)
// http://www.geodatasource.com/developers/php
function distance($lat1, $lon1, $lat2, $lon2, $unit = "K") {
    if (($lat2 == 0 && $lon2 == 0) || ($lat1 == $lat2 && $lon1 == $lon2))
        return 0;
    $theta = $lon1 - $lon2;
    $dist = sin(deg2rad($lat1)) * sin(deg2rad($lat2)) + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * cos(deg2rad($theta));
    $dist = acos($dist);
    $dist = rad2deg($dist);
    $miles = $dist * 60 * 1.1515;
    $unit = strtoupper($unit);

    if ($unit == "K") {
        return ($miles * 1.609344);
    } else if ($unit == "N") {
        return ($miles * 0.8684);
    } else {
        return $miles;
    }
}

// get the inferred route distance from coordinates
function getRouteDistance($coordinates) {
    global $config;
    $osrm = "";
    foreach ($coordinates as $value) {
        $osrm .= "&loc=" . $value[0] . "," . $value[1];
    }
    $osrm = substr($osrm, 1);
    $json = json_decode(file_get_contents($config["osrm_server_url"] . "/viaroute?" . $osrm));
    $json = objectToArray($json);
    if (!isset($json["route_summary"]["total_distance"])) {
        return null;
    }
    $distance = $json["route_summary"]["total_distance"];
    if ($distance > 1000) {
        return ($distance / 1000) . " km";
    } else {
        return $distance . " m";
    }
}

// get the reverse geocoding (street name, civic number, city, nation from coordinates using Nominatim http://wiki.openstreetmap.org/wiki/Nominatim)
function getLocationInfo($latitude, $longitude) {
    global $config;
    $nominatim = "";
    if (!isset($config["nominatim_server_url"])) {
        return $nominatim;
    }
    $json = json_decode(file_get_contents($config["nominatim_server_url"] . "/reverse.php?format=json&lat=" . $latitude . "&lon=" . $longitude . "&zoom=18&addressdetails=1"));
    $json = objectToArray($json);
    if (isset($json["address"])) {
        if (isset($json["address"]["road"])) {
            $road = $json["address"]["road"];
        } else if (isset($json["address"]["footway"])) {
            $road = $json["address"]["footway"];
        } else if (isset($json["address"]["pedestrian"])) {
            $road = $json["address"]["pedestrian"];
        } else if (isset($json["address"]["suburb"])) {
            $road = $json["address"]["suburb"];
        } else {
            $road = "";
        }
        $house_number = $road != "" && isset($json["address"]["house_number"]) ? ", " . $json["address"]["house_number"] : "";
        $postcode = isset($json["address"]["postcode"]) ? "<br>" . $json["address"]["postcode"] : "";
        $city = isset($json["address"]["city"]) ? " " . $json["address"]["city"] : (isset($json["address"]["town"]) ? " " . $json["address"]["town"] : "");
        $country = isset($json["address"]["country"]) ? " (" . $json["address"]["country"] . ")" : "";
        $nominatim = "<br><br>" . $road . $house_number . $postcode . $city . $country . "<br><br>";
    } else {
        $nominatim = "<br><br><br><br><br>";
    }
    return $nominatim;
}
?>
<html>
    <head>
        <title><?php
if (isset($_REQUEST["title"])) {
    echo $_REQUEST["title"];
} else {
    echo "Recommender";
}
?>
        </title>
        <link rel="stylesheet" type="text/css" href="../css/reset.css" />
        <link rel="stylesheet" type="text/css" href="../css/style.css" />
        <link rel="stylesheet" type="text/css" href="../css/typography1.css" />
        <link rel="stylesheet" type="text/css" href = "../css/jquery-ui.css"/>
        <link rel="stylesheet" type="text/css" href="../css/jquery-ui-timepicker-addon.css" />
        <link href="../javascript/toast/toastr.css" rel="stylesheet"/>
        <script type="text/javascript" src="../javascript/jquery-2.1.0.min.js"></script>
        <script type="text/javascript" src="../javascript/jquery-ui.min.js"></script>
        <script type="text/javascript" src="../javascript/jquery.redirect.js"></script>
        <script type="text/javascript" src="../javascript/jquery-ui-timepicker-addon.js"></script>
        <script type="text/javascript">
            //date picker
            /*$(document).ready(function () {
             $("#datepicker").each(function () {
             $(this).datetimepicker({
             timeFormat: "HH:mm:ss",
             dateFormat: "yy/mm/dd",
             autoclose: true
             });
             });
             });*/
            $(function () {
                $("#datepicker").each(function () {
                    $(this).datetimepicker({
                        beforeShow: function (input, inst)
                        {
                            inst.dpDiv.css({marginTop: 30 + 'px', marginLeft: -330 + 'px'});
                        },
                        timeFormat: "HH:mm:ss",
                        dateFormat: "yy/mm/dd",
                        autoclose: true
                    });
                });
            });</script>
        <script src="../javascript/toast/toastr.min.js" type = "text/javascript"></script>

        <!-- map headers -->
        <!--<link rel="stylesheet" href="http://cdn.leafletjs.com/leaflet-0.7.5/leaflet.css" />-->
        <link rel="stylesheet" href="../css/leaflet.css" />
        <!--<script src="http://cdn.leafletjs.com/leaflet-0.7.5/leaflet.js"></script>-->
        <script type="text/javascript" src="../javascript/maps/leaflet.js"></script>

        <!--leaflet label plugin includes https://github.com/Leaflet/Leaflet.label-->
        <script src = "../javascript/maps/leaflet-label-plugin/Label.js" ></script>
        <script src="../javascript/solr.js" type="text/javascript"></script>
        <script src="../javascript/maps/leaflet-label-plugin/BaseMarkerMethods.js"></script>
        <script src="../javascript/maps/leaflet-label-plugin/Marker.Label.js"></script>
        <script src="../javascript/maps/leaflet-label-plugin/CircleMarker.Label.js"></script>
        <script src="../javascript/maps/leaflet-label-plugin/Path.Label.js"></script>
        <script src="../javascript/maps/leaflet-label-plugin/Map.Label.js"></script>
        <script src="../javascript/maps/leaflet-label-plugin/FeatureGroup.Label.js"></script>
        <link rel="stylesheet" href="../javascript/maps/leaflet-label-plugin/leaflet.label.css" />
        <!-- jquery scroll to plugin includes http://demos.flesler.com/jquery/scrollTo/ -->
        <script type="text/javascript" src="../javascript/maps/jquery.scrollTo-2.1.2/jquery.scrollTo.min.js"></script>
                <!-- <style>
                    .map {
                        position: absolute;
                        width: 100%;
                        height: 100%;
                    }
                </style> -->
        <!-- heat map and leaflet plugin http://www.patrick-wied.at/static/heatmapjs/example-heatmap-leaflet.html -->
        <script src = "../javascript/maps/heatmap.js" ></script>
        <script src = "../javascript/maps/leaflet-heatmap.js" ></script>

        <!-- marker cluster plugin https://github.com/Leaflet/Leaflet.markercluster-->
        <link rel="stylesheet" href="../javascript/maps/markercluster/MarkerCluster.css" />
        <link rel="stylesheet" href="../javascript/maps/markercluster/MarkerCluster.Default.css" />
        <script src="../javascript/maps/markercluster/leaflet.markercluster-src.js"></script>

        <!-- polyline decorator https://github.com/bbecquet/Leaflet.PolylineDecorator-->
        <script src="../javascript/maps/leaflet_polylineDecorator/leaflet.polylineDecorator.js"></script>
        <?php
        if ((isset($_REQUEST["showFrame"]) && $_REQUEST['showFrame'] == 'false') || $_SESSION['showFrame'] == 'false') {
            $_SESSION['showFrame'] = 'false';
            echo "<style>
            body { zoom: 0.8; }
            th, td, caption {
            width: 100px !important;
            max-height: 100px !important;
            }
            #resultsTable { padding-top: 0px !important; }
            #container1 { top: 0px !important; }
        </style>";
        }
        ?>
    </head>
    <body>
        <?php
        if ((!isset($_REQUEST["showFrame"]) || $_REQUEST['showFrame'] != 'false') && $_SESSION['showFrame'] != 'false') {
            include_once "../header.php"; //include header
        }
        ?>
        <div id='container1'> <!-- div container -->
            <?php
            //include_once "../settings.php";
            include_once "../Polyline.php";
            include_once "../SolrPhpClient/Apache/Solr/Service.php";

            // print the shortest route path connecting the user's locations
            function printShortestRoute($coordinates) {
                global $config;
                // encoded polyline as returned by OSRM
                //https://github.com/Project-OSRM/osrm-backend
                //http://localhost:5000/viaroute?loc=43.7727,11.2532&loc=43.71328,11.22361
                //$encoded = objectToArray(json_decode(file_get_contents($config['osrm_server_url'] . "/viaroute?" . substr($lat_lng, 1))));

                $lat_lng = array();
                foreach ($coordinates as $coordinate) {
                    $lat_lng[] = $coordinate[0] . "," . $coordinate[1];
                }
                // workaround to post an array of key => value with the same key (loc)
                $vars = array('loc' => $lat_lng);
                $query = http_build_query($vars, null, '&');
                $data = preg_replace('/%5B(?:[0-9]|[1-9][0-9]+)%5D=/', '=', $query); //loc=x&loc=y&loc=z...
                $options = array(
                    'http' => array(
                        'header' => "Content-type: application/x-www-form-urlencoded\r\n",
                        'method' => 'POST',
                        'content' => $data,
                    )
                );
                $context = stream_context_create($options);
                $result = file_get_contents($config['osrm_server_url'] . "/viaroute", false, $context);
                $json = objectToArray(json_decode($result));
                $points = Polyline::Decode($json["route_geometry"]);
                // list of tuples
                $points = Polyline::Pair($points);
                $javascript = "";
                foreach ($points as $point) {
                    // coordinates of geometry as returned by the OSRM server must be scaled by 10
                    $javascript = $javascript . ",L.latLng(" . ($point[0] / 10) . "," . ($point[1] / 10) . ")\n";
                }
                return substr($javascript, 1);
            }

            // get path coordinates (mode = gps or manual)
            function getCoordinates() {
                global $config;

                //CONNECT
                $link = mysqli_connect($config['wifi_host'], $config['wifi_user'], $config['wifi_pass'], $config['wifi_database']);

                /* check connection */
                if (mysqli_connect_errno()) {
                    printf("Connection failed: %s\n", mysqli_connect_error());
                    exit();
                }
                // GET DATA
                $coordinates = array();
                $sql = "SELECT latitude, longitude, address FROM wifi.aps_new";
                // if id is set, then search only the coordinates for this AP in the table wifi.all_aps
                if (isset($_REQUEST["id"])) {
                    $sql = "SELECT latitude, longitude FROM wifi.aps WHERE id = " . $_REQUEST["id"];
                    $result = mysqli_query($link, $sql) or die(mysqli_error());
                    while ($row = mysqli_fetch_assoc($result)) {
                        $latitude = $row["latitude"];
                        $longitude = $row["longitude"];
                    }
                    $sql = "SELECT latitude, longitude, address,
                             ( 6371000 * acos( cos( radians(" . $latitude . ") ) * cos( radians(latitude) ) 
                             * cos( radians(longitude) - radians(" . $longitude . ")) + sin(radians(" . $latitude . ")) 
                             * sin( radians(latitude)))) AS distance 
                             FROM wifi.all_aps HAVING distance <= 100 ORDER BY distance ASC LIMIT 1";
                }
                $result = mysqli_query($link, $sql) or die(mysqli_error());
                while ($row = mysqli_fetch_assoc($result)) {
                    if (!in_array($row["latitude"] . "|" . $row["longitude"] . "|" . $row["address"], $coordinates)) {
                        $coordinates[] = $row["latitude"] . "|" . $row["longitude"] . "|" . $row["address"];
                    }
                }
                //close connection
                mysqli_close($link);
                return $coordinates;
            }

            function printCoordinates($coordinates) {
                $javascript = "";
                foreach ($coordinates as $coordinate) {
                    $lat_lng_mode_timestamp = split("\|", $coordinate);
                    $latitude = $lat_lng_mode_timestamp[0];
                    $longitude = $lat_lng_mode_timestamp[1];
                    $javascript .= ",L.latLng(" . $latitude . "," . $longitude . ")\n";
                }
                return substr($javascript, 1);
            }

            // print markers (gps or manual)
            function printMarkers($coordinates, $mac_aps) {
                $markers = "var markers = [];\n";
                $markers .= "//Extend the Default marker class
                    var ManualMarkerIcon = L.Icon.Default.extend({
                    options: {
            	    iconUrl: 'images/location_red.png' 
                    }
                    });
                    var manualMarkerIcon = new ManualMarkerIcon();";
                $markers .= "var markersClusterGroup = L.markerClusterGroup();\n";
                $markersNotCluster = "";
                $i = 0;
                foreach ($coordinates as $coordinate) {
                    $lat_lng_mode_address = split("\|", $coordinate);
                    $latitude = $lat_lng_mode_address[0];
                    $longitude = $lat_lng_mode_address[1];
                    $address = $lat_lng_mode_address[2];
                    if (isset($mac_aps[$latitude . " " . $longitude])) {
                        if (count($mac_aps[$latitude . " " . $longitude]) > 1) {
                            foreach ($mac_aps[$latitude . " " . $longitude] as $mac_ap) {
                                $markers .= "markers[" . $i . "] = L.marker([" . $latitude . "," . $longitude . "])" .
                                        ".bindPopup('Lat: " . $latitude . "<br>Lon: " . $longitude . "<br>" . addslashes($address) . "<br>Mac: " . $mac_ap . "<br>N: <span id=\"a" . str_replace(".", "", $latitude) . str_replace(".", "", $longitude) . "\"></span>')" .
                                        ".on('click', " .
                                        "function(event){" .
                                        "solr(" . $latitude . "," . $longitude . ", '" . $mac_ap . "');" .
                                        "});\n";
                                $markers .= "markersClusterGroup.addLayer(markers[" . $i . "]);\n";
                                $i++;
                            }
                        } else {
                            $markers .= "markers[" . $i . "] = L.marker([" . $latitude . "," . $longitude . "])" .
                                    ".bindPopup('Lat: " . $latitude . "<br>Lon: " . $longitude . "<br>" . addslashes($address) . "<br>Mac: " . $mac_aps[$latitude . " " . $longitude][0] . "<br>N: <span id=\"a" . str_replace(".", "", $latitude) . str_replace(".", "", $longitude) . "\"></span>')" .
                                    ".on('click', " .
                                    "function(event){" .
                                    "solr(" . $latitude . "," . $longitude . ", '" . $mac_aps[$latitude . " " . $longitude][0] . "');" .
                                    "});\n";
                            $markersNotCluster .= ",markers[" . $i . "]";
                            $i++;
                        }
                    } else {
                        $markers .= "markers[" . $i . "] = L.marker([" . $latitude . "," . $longitude . "], {icon: manualMarkerIcon})" .
                                ".bindPopup('Lat: " . $latitude . "<br>Lon: " . $longitude . "<br>" . addslashes($address) . "')" .
                                ".on('click', " .
                                "function(event){" .
                                "solr(" . $latitude . "," . $longitude . ");" .
                                "});\n";
                        $i++;
                    }
                }
                return $markers . "var markersLayerGroup = L.layerGroup([markersClusterGroup" . $markersNotCluster . "]);";
            }

            // get the heatmap data, https://wiki.apache.org/solr/SpatialSearch
            function getHeatMapData() {
                global $config;
                $solr = new Apache_Solr_Service($config["solr_host"], $config["solr_port"], $config["solr_collection"]);
                $heatmapdata = "";
                // limit result to 100 m radius
                //$params = array('sfield' => 'latitude_longitude', 'pt' => $latitude . ',' . $longitude, 'sort' => 'geodist() asc', 'd' => '0.1', 'fq' => '{!geofilt}');
                $results = $solr->search("network_name:FirenzeWiFi AND accuracy:*", 0, 1000000, null);
                $i = 0;
                foreach ($results->response->docs as $doc) {
                    foreach ($doc as $field => $value) {
                        if ($field == "latitude") {
                            $latitude = $value;
                        } else if ($field == "longitude") {
                            $longitude = $value;
                        } else if ($field == "rssi") {
                            $rssi = $value;
                        } else if ($field == "distance_by_rssi") {
                            $distance_by_rssi = $value;
                        } else if ($field == "MAC_address") {
                            $mac_address = $value;
                        }
                    }
                    $heatmapdata .= ($i != 0 ? ", " : "") . " {
                    \"lat\": " . $latitude . ", \"lng\":" . $longitude . ", \"rssi\": " . abs($rssi) . "}";
                    $i++;
                }
                return "[" . $heatmapdata . "]";
            }

            // get APs with a mac
            function getMacAPs() {
                global $config;

                //CONNECT
                $link = mysqli_connect($config['wifi_host'], $config['wifi_user'], $config['wifi_pass'], $config['wifi_database']);

                /* check connection */
                if (mysqli_connect_errno()) {
                    printf("Connection failed: %s\n", mysqli_connect_error());
                    exit();
                }
                // get the nearest AP from wifi.all_aps (RDF) to that in wifi.aps (Comune di Firenze)
                $coordinates = array();
                $ap = isset($_REQUEST["id"]) ? " WHERE id = " . $_REQUEST["id"] : "";
                $sql1 = "SELECT mac_radio, latitude, longitude FROM wifi.aps_new" . $ap;
                $result = mysqli_query($link, $sql1) or die(mysqli_error());
                while ($row1 = mysqli_fetch_assoc($result)) {
                    $coordinates[$row1["latitude"] . " " . $row1["longitude"]][] = $row1["mac_radio"];
                }
                //close connection
                mysqli_close($link);
                return $coordinates;
            }

            // get APs without a mac
            function getNoMacAPs($mac_aps) {
                global $config;

                //CONNECT
                $link = mysqli_connect($config['wifi_host'], $config['wifi_user'], $config['wifi_pass'], $config['wifi_database']);

                /* check connection */
                if (mysqli_connect_errno()) {
                    printf("Connection failed: %s\n", mysqli_connect_error());
                    exit();
                }
                // GET DATA
                $coordinates = array();
                $sql = "SELECT latitude, longitude FROM wifi.all_aps";
                $result = mysqli_query($link, $sql) or die(mysqli_error());
                while ($row = mysqli_fetch_assoc($result)) {
                    if (!in_array($row["latitude"] . " " . $row["latitude"], $mac_aps)) {
                        $coordinates[] = $row1["latitude"] . " " . $row1["latitude"];
                    }
                }
                //close connection
                mysqli_close($link);
                return $coordinates;
            }

            // get aps with a mac
            $mac_aps = getMacAPs();
            $coordinates = getCoordinates();
            if (count($coordinates) == 0) {
                echo "No results found.";
                exit();
            }
            ?>
            <!-- display map javascript -->
            <div id="map" class="map"></div>
            <script type="text/javascript">
            var mbAttr = 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
                    '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
                    'Imagery © <a href="http://mapbox.com">Mapbox</a>';
            // for satellite map use mapbox.streets-satellite in the url
            var baseLayer = L.tileLayer('https://api.mapbox.com/v4/mapbox.streets/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoicGJlbGxpbmkiLCJhIjoiNTQxZDNmNDY0NGZjYTk3YjlkNTAzNWQwNzc0NzQwYTcifQ.CNfaDbrJLPq14I30N1EqHg', {
                attribution: mbAttr,
                maxZoom: 25,
            });
            //heatmap configuration
            var cfg = {
                // radius should be small ONLY if scaleRadius is true (or small radius is intended)
                // if scaleRadius is false it will be the constant radius used in pixels
                "radius": 0.0008,
                "maxOpacity": .8,
                // scales the radius based on map zoom
                "scaleRadius": true,
                // if set to false the heatmap uses the global maximum for colorization
                // if activated: uses the data maximum within the current map boundaries 
                //   (there will always be a red spot with useLocalExtremas true)
                "useLocalExtrema": false,
                // which field name in your data represents the latitude - default "lat"
                latField: 'lat',
                // which field name in your data represents the longitude - default "lng"
                lngField: 'lng',
                // which field name in your data represents the data value - default "value"
                valueField: 'count',
                gradient: {
                    // enter n keys between 0 and 1 here
                    // for gradient color customization
                    '.0': 'blue',
                    '.1': 'cyan',
                    '.2': 'green',
                    '.3': 'yellowgreen',
                    '.4': 'yellow',
                    '.5': 'gold',
                    '.6': 'orange',
                    '.7': 'darkorange',
                    '.8': 'tomato',
                    '.9': 'orangered',
                    '1.0': 'red'
                }
            };
            //setup position heatmap
            var heatmapLayer = new HeatmapOverlay(cfg);
            var map = new L.Map('map', {
                center: new L.LatLng(43.76990127563477, 11.25531959533691),
                zoom: 14,
                layers: [baseLayer, heatmapLayer]
            });

            // profiles
            var profiles = ["Global", "All", "Citizen", "Commuter", "Student", "Tourist", "Disabled", "Operator", "Unknown"];

            // time ranges (minutes)
            var time_ranges = ["5", "10", "15", "30", "60", "180", "360", "540", "720", "1440", "4320", "10080", "44640"];

            // reply speeds (seconds)
            var reply_speeds = ["0", "10", "30", "60", "300", "900", "1800", "3600", "7200", "10800"];

            // people location markers
            var markers = L.markerClusterGroup();

            // people location-clustered markers
            var peopleClusteredMarkers = L.markerClusterGroup();

            // people movements layer
            var movementsLayer = L.layerGroup();

            // people movements markers
            var movementsMarkers = L.markerClusterGroup();

            // polyline decorator pattern for people movements markers
            var polyline_decorator_pattern = {
                patterns: [
                    {offset: 100, endOffset: 100, repeat: 250, symbol: L.Symbol.arrowHead({pixelSize: 20, pathOptions: {fillOpacity: 0.75, color: '#f0000'}})}
                ]
            };

            // icons https://thenounproject.com/
            // marker icon blue default (provider = gps)
            // marker icon red (provider = network)
            var MarkerIconRed = L.Icon.Default.extend({
                options: {
                    iconUrl: '../images/location_red.png'
                }
            }
            );
            var markerIconRed = new MarkerIconRed();
            // marker icon black (provider = fused)
            var MarkerIconBlack = L.Icon.Default.extend({
                options: {
                    iconUrl: '../images/location_black.png'
                }
            });
            var markerIconBlack = new MarkerIconBlack();

            // replace all function
            String.prototype.replaceAll = function (str1, str2, ignore) {
                return this.replace(new RegExp(str1.replace(/([\/\,\!\\\^\$\{\}\[\]\(\)\.\*\+\?\|\<\>\-\&])/g, "\\$&"), (ignore ? "gi" : "g")), (typeof (str2) == "string") ? str2.replace(/\$/g, "$$$$") : str2);
            }

            function refreshHeatmap() {
                // get time range
                var time_range;
                for (var j = 0; j < time_ranges.length; j++) {
                    if ($('#timeRange' + time_ranges[j]).is(":checked")) {
                        time_range = time_ranges[j];
                        break;
                    }
                }
                //get "To Date" if it is checked
                var to = "";
                if ($('#checkBoxToDate').is(":checked") && $('#datepicker').val() != "") {
                    to = $('#datepicker').val();
                    to = to.replaceAll("/", "-");
                }
                // set distinct people to the checkbox value
                var distinct = $('#checkBoxDistinctPeople').is(":checked");
                for (var i = 0; i < profiles.length; i++) {
                    if ($('#checkBoxHeatmap' + profiles[i]).is(":checked")) {
                        $.ajax({url: 'refresh-heatmap-realtime.php', dataType: 'json', data: {profile: profiles[i], minutes: time_range, distinct: distinct, to: to}, success: function (data) {
                                var testDataPeople = {
                                    max: 1,
                                    data: data[1]
                                };
                                // set the number of people in legend
                                $('#people' + profiles[i]).text(JSON.stringify(data[0]));
                                // refresh people location markers javascript
                                eval(data[2]);
                                // refresh people location-clustered markers javascript
                                eval(data[3]);
                                // refresh people movements markers javascript
                                eval(data[4]);
                                // set the number of people for each profile in legend
                                if (profiles[i] == "Global") {
                                    for (var k = 1; k < profiles.length; k++) {
                                        $('#people' + profiles[k]).text(JSON.stringify(data[5][profiles[k]]));
                                    }
                                    $('#peopleUnknown').text(JSON.stringify(data[5]["Unknown"]));
                                }
                                heatmapLayer.setData(testDataPeople);
                                // if the show/hide heatmap checkbox is checked, then add the heatmap layer to the map
                                if ($('#checkBoxShowHideHeatmap').is(":checked")) {
                                    map.removeLayer(heatmapLayer);
                                    map.addLayer(heatmapLayer);
                                }
                            }, complete: function (data) {
                                // show a notification
                                toastr["success"]("Heatmap reloaded", "", {"timeOut": "1000", "iconClass": "customer-info"});

                                // update "To Date" field (previous time + elapsed time), if checked, not empty, and not static
                                var tmp = new Date().getTime() / 1000;
                                if ($('#checkBoxToDate').is(":checked") && $('#datepicker').val() != "") {
                                    var end = $('#datepicker').val();
                                    var start = getDateOffset(end, -parseInt(time_range) * 60 * 1000);
                                    // if static checkbox is not checked, then update "To Date" with elapsed time
                                    if (!$('#checkBoxStaticToDate').is(":checked")) {
                                        if ($('#replySpeed0').is(":checked")) {
                                            updateDate(tmp - currentTime);
                                        } else if ($('#replySpeed10').is(":checked")) {
                                            updateDate(10);
                                        } else if ($('#replySpeed30').is(":checked")) {
                                            updateDate(30);
                                        } else if ($('#replySpeed60').is(":checked")) {
                                            updateDate(60);
                                        } else if ($('#replySpeed300').is(":checked")) {
                                            updateDate(300);
                                        } else if ($('#replySpeed900').is(":checked")) {
                                            updateDate(900);
                                        } else if ($('#replySpeed1800').is(":checked")) {
                                            updateDate(1800);
                                        } else if ($('#replySpeed3600').is(":checked")) {
                                            updateDate(3600);
                                        } else if ($('#replySpeed7200').is(":checked")) {
                                            updateDate(7200);
                                        } else if ($('#replySpeed10800').is(":checked")) {
                                            updateDate(10800);
                                        }
                                    }
                                    // update legend title with date range
                                    $('#legendTitle').text(start.replaceAll("/", "-") + " - " + end.replaceAll("/", "-"));
                                    currentTime = tmp;
                                } else {
                                    var end = new Date();
                                    end = getDateString(end);
                                    var start = getDateOffset(end, -parseInt(time_range) * 60 * 1000);
                                    // update legend title with date range
                                    $('#legendTitle').text(start.replaceAll("/", "-") + " - " + end.replaceAll("/", "-"));
                                }

                                // reload the heatmap after timeout
                                setTimeout(refreshHeatmap, <?php
            global $config;
            echo $config["timeout"];
            ?>);
                            }
                        });
                        break;
                    }
                }
            }

            // set cluster trajectories to be set by the function getTrajectory (generated by the Java application TrajectoriesClustering) in clusteredTrajectories_[profile].js
            var clusterTrajectories = "";
            map.on('click', function () {
                map.removeLayer(clusterTrajectories);
            });
            var coordinateList = [<?php /* echo $coordinates_javascript; */ ?>];
            //var routeList = [<?php /* echo $route_javascript; */ ?>];

            // line of user's locations
            var coordinatesLine = new L.Polyline(coordinateList, {
                color: 'blue',
                weight: 8,
                opacity: 0.5,
                smoothFactor: 1
            });
            // line of shortest route path between user's locations
            /*var routeLine = new L.Polyline(routeList, {
             color: 'red',
             weight: 8,
             opacity: 0.5,
             smoothFactor: 1
             });*/
            pMarkers = [];
            function solr(latitude, longitude, mac) {
                $.getJSON('solr.php', {latitude: latitude, longitude: longitude, mac: mac}, function (data) {
                    // remove previous markers
                    for (var i = 0; i < pMarkers.length; i++) {
                        map.removeLayer(pMarkers[i]);
                    }
                    pMarkers = [];
                    // data will hold the php array as a javascript object
                    $.each(data, function (key, val) {
                        var lat = data[key].latitude;
                        var lng = data[key].longitude;
                        var distance_by_rssi = data[key].distance_by_rssi;
                        var rssi = data[key].rssi;
                        var distance = data[key].distance;
                        var MAC_address = data[key].MAC_address;
                        var accuracy = typeof (data[key].accuracy) != "undefined" ? "<br>Accuracy: " + data[key].accuracy + " m" : "";
                        var pMarker;
                        if (rssi <= -90) {
                            pMarker = L.marker([lat, lng], {icon: whiteIcon}).bindPopup("RSSI: " + rssi + " dB<br>Distance: " + distance + " m<br>Distance by RSSI: " + distance_by_rssi + "  m<br>MAC address: " + MAC_address + accuracy);
                        } else if (rssi > -90 && rssi <= -80) {
                            pMarker = L.marker([lat, lng], {icon: brownIcon}).bindPopup("RSSI: " + rssi + " dB<br>Distance: " + distance + " m<br>Distance by RSSI: " + distance_by_rssi + "  m<br>MAC address: " + MAC_address + accuracy);
                        } else if (rssi > -80 && rssi <= -70) {
                            pMarker = L.marker([lat, lng], {icon: yellowIcon}).bindPopup("RSSI: " + rssi + " dB<br>Distance: " + distance + " m<br>Distance by RSSI: " + distance_by_rssi + "  m<br>MAC address: " + MAC_address + accuracy);
                        } else if (rssi > -70 && rssi <= -60) {
                            pMarker = L.marker([lat, lng], {icon: orangeIcon}).bindPopup("RSSI: " + rssi + " dB<br>Distance: " + distance + " m<br>Distance by RSSI: " + distance_by_rssi + "  m<br>MAC address: " + MAC_address + accuracy);
                        } else if (rssi > -60) {
                            pMarker = L.marker([lat, lng], {icon: redIcon}).bindPopup("RSSI: " + rssi + " dB<br>Distance: " + distance + " m<br>Distance by RSSI: " + distance_by_rssi + "  m<br>MAC address: " + MAC_address + accuracy);
                        }
                        map.addLayer(pMarker);
                        pMarkers.push(pMarker);
                    });
                    // replace span in popup with number of markers
                    $("#a" + (latitude + "").replace(".", "") + (longitude + "").replace(".", "")).html(pMarkers.length);
                });
            }

            // add a legend, http://leafletjs.com/examples/choropleth.html
            var legend = L.control({position: 'topright'});
            /*legend.onAdd = function (map) {
             var div = L.DomUtil.create('div', 'info legend');
             categories = ['< -89 dB', '-89/-80 dB', '-79/-70 dB', '-69/-60 dB', '> -60 dB'];
             color_urls = ['url(images/white_icon.png)', 'url(images/brown_icon.png)', 'url(images/yellow_icon.png)', 'url(images/orange_icon.png)', 'url(images/red_icon.png)'];
             for (var i = 0; i < categories.length; i++) {
             div.innerHTML +=
             '<i style="background: ' + color_urls[i] + '"></i> ' +
             (categories[i] ? categories[i] + '<br>' : '+');
             }
             return div;
             };*/
            function setColor(color, value, decimals) {
                cfg["gradient"][value] = color;
                //document.getElementById("range" + color).innerHTML = value;
                $("#range" + color).text(parseFloat(value).toFixed(parseInt(decimals)));
                $("#slider" + color).attr("value", parseFloat(value).toFixed(parseInt(decimals)));
                map.removeLayer(heatmapLayer);
                heatmapLayer = new HeatmapOverlay(cfg);
                heatmapLayer.setData(testData);
                map.addLayer(heatmapLayer);
                /*map.removeLayer(heatmapLayerWiFi);
                 heatmapLayerWiFi = new HeatmapOverlay(cfg);
                 heatmapLayerWiFi.setData(testDataWiFi);
                 map.addLayer(heatmapLayerWiFi);*/
            }
            function setOption(option, value, decimals) {
                cfg[option] = value;
                //document.getElementById("range" + color).innerHTML = value;
                //set values for sliders, not for checkboxes
                if (decimals) {
                    $("#range" + option).text(parseFloat(value).toFixed(parseInt(decimals)));
                    $("#slider" + option).attr("value", parseFloat(value).toFixed(parseInt(decimals)));
                }
                /*map.removeLayer(heatmapLayer);
                 heatmapLayer = new HeatmapOverlay(cfg);
                 heatmapLayer.setData(testData);
                 map.addLayer(heatmapLayer);*/
                heatmapLayer.configure(cfg); // metodo aggiunto in leaflet-heatmap.js per aggiornare la heatmap con la nuova configurazione
                //heatmapLayerWiFi.configure(cfg);
            }
            function toggleClusteredMarkers(toggle) {
                if (toggle) {
                    map.addLayer(markers);
                } else {
                    map.removeLayer(markers);
                }
            }
            function togglePeopleClusteredMarkers(toggle) {
                if (toggle) {
                    map.addLayer(peopleClusteredMarkers);
                } else {
                    map.removeLayer(peopleClusteredMarkers);
                }
            }
            function toggleMovementsClusteredMarkers(toggle) {
                if (toggle) {
                    map.addLayer(movementsLayer);
                } else {
                    map.removeLayer(movementsLayer);
                }
            }
            function toggleClusteredTrajectories(toggle) {
                if (toggle) {
                    map.addLayer(clusteredTrajectories);
                } else {
                    map.removeLayer(clusteredTrajectories);
                }
            }
            function toggleTrajectories(toggle) {
                if (toggle) {
                    map.addLayer(trajectories);
                } else {
                    map.removeLayer(trajectories);
                }
            }
            function toggleTrajectoriesDecorator(toggle) {
                if (toggle) {
                    map.addLayer(trajectories_decorator);
                } else {
                    map.removeLayer(trajectories_decorator);
                }
            }
            function toggleAPsMarkers(toggle) {
                if (toggle) {
                    map.addLayer(markersLayerGroup);
                } else {
                    map.removeLayer(markersLayerGroup);
                }
            }
            // show/hide an individual heatmap
            function toggleHeatmap(toggle, profile) {
                if (toggle) {
                    for (var i = 0; i < profiles.length; i++) {
                        if (profiles[i] != profile)
                            $('#checkBoxHeatmap' + profiles[i]).prop('checked', false);
                    }
                    //map.addLayer(heatmapLayer);
                }
                // prevent unchecking the checkbox        
                else {
                    $('#checkBoxHeatmap' + profile).prop('checked', true);
                }
            }
            // show/hide the checked heatmap, without unchecking any heatmap checkbox
            function showHideHeatmap(toggle) {
                if (toggle) {
                    map.addLayer(heatmapLayer);
                } else {
                    map.removeLayer(heatmapLayer);
                }
            }
            function toggleWiFiHeatmap(toggle) {
                if (toggle) {
                    map.addLayer(heatmapLayerWiFi);
                } else {
                    map.removeLayer(heatmapLayerWiFi);
                }
            }
            function toggleTimeRange(toggle, time) {
                if (toggle) {
                    for (var i = 0; i < time_ranges.length; i++) {
                        if (time_ranges[i] != time)
                            $('#timeRange' + time_ranges[i]).prop('checked', false);
                    }
                }
                // prevent unchecking the checkbox        
                else {
                    $('#timeRange' + time).prop('checked', true);
                }
            }
            function toggleReplySpeed(toggle, time) {
                if (toggle) {
                    for (var i = 0; i < reply_speeds.length; i++) {
                        if (reply_speeds[i] != time)
                            $('#replySpeed' + reply_speeds[i]).prop('checked', false);
                    }
                }
                // prevent unchecking the checkbox        
                else {
                    $('#replySpeed' + time).prop('checked', true);
                }
            }
            function upSlider(color, step, decimals, max) {
                var value = $("#slider" + color).attr("value");
                //setColor(color, value, 0.01);           
                if (parseFloat(parseFloat(value) + parseFloat(step)) <= max) {
                    $("#range" + color).text(parseFloat(parseFloat(value) + parseFloat(step)).toFixed(parseInt(decimals)));
                    //$("#slider" + color).attr("value", parseFloat(parseFloat(value) + parseFloat(0.01)).toFixed(2));
                    document.getElementById("slider" + color).value = parseFloat(parseFloat(value) + parseFloat(step)).toFixed(parseInt(decimals));
                    $("#slider" + color).trigger('change');
                }
            }
            function downSlider(color, step, decimals, min) {
                var value = $("#slider" + color).attr("value");
                //setColor(color, value, parseFloat(-0.01));
                if (parseFloat(parseFloat(value) - parseFloat(step)) >= min) {
                    $("#range" + color).text(parseFloat(parseFloat(value) - parseFloat(step)).toFixed(parseInt(decimals)));
                    //$("#slider" + color).attr("value", parseFloat(parseFloat(value) + parseFloat(0.01)).toFixed(2));
                    document.getElementById("slider" + color).value = parseFloat(parseFloat(value) - parseFloat(step)).toFixed(parseInt(decimals));
                    $("#slider" + color).trigger('change');
                }
            }
            function updateDate(offset) {
                var date = $('#datepicker').datepicker('getDate');
                var newDate = new Date();
                newDate.setTime(date.getTime() + offset * 1000);
                $('#datepicker').datepicker('setDate', newDate);
            }
            function getDateOffset(dateString, offset) {
                var date = new Date(dateString.replaceAll("-", "/"));
                var newDate = new Date();
                newDate.setTime(date.getTime() + offset);
                return getDateString(newDate);
            }
            function getDateString(date) {
                var dateStr = padStr(date.getFullYear()) + "-" +
                        padStr(1 + date.getMonth()) + "-" +
                        padStr(date.getDate()) + " " +
                        padStr(date.getHours()) + ":" +
                        padStr(date.getMinutes()) + ":" +
                        padStr(date.getSeconds());
                return dateStr;
            }

            function padStr(i) {
                return (i < 10) ? "0" + i : "" + i;
            }
            // number of people (set by refreshHeatmap)
            var people;
            legend.onAdd = function (map) {
                var div = L.DomUtil.create('div', 'info legend');
                categories = ['blue', 'cyan', 'green', 'yellowgreen', 'yellow', 'gold', 'orange', 'darkorange', 'tomato', 'orangered', 'red'];
                var colors = new Array();
                colors['blue'] = '#0000FF';
                colors['cyan'] = '#00FFFF';
                colors['green'] = '#008000';
                colors['yellowgreen'] = '#9ACD32';
                colors['yellow'] = '#FFFF00';
                colors['gold'] = '#FFD700';
                colors['orange'] = '#FFA500';
                colors['darkorange'] = '#FF8C00';
                colors['orangered'] = '#FF4500';
                colors['tomato'] = '#FF6347';
                colors['red'] = '#FF0000';
                var colors_value = new Array();
                colors_value['blue'] = <?php echo $config["legend_color_blue"]; ?>;
                colors_value['cyan'] = <?php echo $config["legend_color_cyan"]; ?>;
                colors_value['green'] = <?php echo $config["legend_color_green"]; ?>;
                colors_value['yellowgreen'] = <?php echo $config["legend_color_yellowgreen"]; ?>;
                colors_value['yellow'] = <?php echo $config["legend_color_yellow"]; ?>;
                colors_value['gold'] = <?php echo $config["legend_color_gold"]; ?>;
                colors_value['orange'] = <?php echo $config["legend_color_orange"]; ?>;
                colors_value['darkorange'] = <?php echo $config["legend_color_darkorange"]; ?>;
                colors_value['tomato'] = <?php echo $config["legend_color_tomato"]; ?>;
                colors_value['orangered'] = <?php echo $config["legend_color_orangered"]; ?>;
                colors_value['red'] = <?php echo $config["legend_color_red"]; ?>;
                //color_urls = ['url(images/white_icon.png)', 'url(images/brown_icon.png)', 'url(images/yellow_icon.png)', 'url(images/orange_icon.png)', 'url(images/red_icon.png)', 'url(images/white_icon.png)', 'url(images/brown_icon.png)', 'url(images/yellow_icon.png)', 'url(images/orange_icon.png)', 'url(images/red_icon.png)'];
                div.innerHTML += 'Time Range: <span id="legendTitle"></span>';
                /*for (var i = 0; i < categories.length; i++) {
                 div.innerHTML +=
                 '<div class="input-color"><div class="color-box" style="background-color: ' + colors[categories[i]] + ';"></div></div>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
                 '<a id="unselect" style="cursor:pointer" onclick="downSlider(\'' + categories[i] + '\',0.01,2,0);">&#10094;</a>&nbsp;&nbsp;&nbsp;' +
                 '<input id="slider' + categories[i] + '" name="sl' + categories[i] + '" type="range" min="0" max="1" value="' + colors_value[categories[i]] + '" step="0.01" style="width: 190px;" onchange="setColor(\'' + categories[i] + '\',this.value,2);"/>' +
                 '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a id="unselect" style="cursor:pointer" onclick="upSlider(\'' + categories[i] + '\',0.01,2,1);">&#10095;</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
                 '<span id="range' + categories[i] + '">' + colors_value[categories[i]] + '</span>';
                 }*/
                // radius
                div.innerHTML +=
                        '<br>Radius: &nbsp;&nbsp;&nbsp;&nbsp;' +
                        '<a id="unselect" style="cursor:pointer" onclick="downSlider(\'radius\',0.00001,6,0);">&#10094;</a>&nbsp;&nbsp;&nbsp;' +
                        '<input id="sliderradius" type="range" min="0" max="0.0010" value="0.0008" step="0.00001" onchange="setOption(\'radius\',this.value,6);"/>' +
                        '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a id="unselect" style="cursor:pointer" onclick="upSlider(\'radius\',0.00001,6,0.0010);">&#10095;</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
                        '<span id="rangeradius">0.0008</span>';
                // max opacity                   
                div.innerHTML +=
                        '<br>Max Opacity: &nbsp;&nbsp;&nbsp;&nbsp;' +
                        '<a id="unselect" style="cursor:pointer" onclick="downSlider(\'maxOpacity\',0.1,2,0);">&#10094;</a>&nbsp;&nbsp;&nbsp;' +
                        '<input id="slidermaxOpacity" type="range" min="0" max="1" value="0.8" step="0.01" onchange="setOption(\'maxOpacity\',this.value,2);"/>' +
                        '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a id="unselect" style="cursor:pointer" onclick="upSlider(\'maxOpacity\',0.01,2,0.8);">&#10095;</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
                        '<span id="rangemaxOpacity">0.80</span>';
                // heatmaps
                div.innerHTML += '<br><br>Heatmaps ';
                // heatmaps show/hide
                div.innerHTML += '<input id="checkBoxShowHideHeatmap" type="checkbox" name="checkBoxShowHideHeatmap" value="false" checked onclick="showHideHeatmap(this.checked);"/>';
                // view heatmap (Global)
                div.innerHTML +=
                        '<br>Global (<span id="peopleGlobal"></span>) ' +
                        '<input id="checkBoxHeatmapGlobal" type="checkbox" name="Heatmap" value="false" checked onclick="toggleHeatmap(this.checked,\'Global\');"/>';
                // view heatmap (All)
                div.innerHTML +=
                        ' All (<span id="peopleAll"></span>) ' +
                        '<input id="checkBoxHeatmapAll" type="checkbox" name="Heatmap" value="false" onclick="toggleHeatmap(this.checked,\'All\');"/>';
                // view heatmap (Citizen)
                div.innerHTML +=
                        'Citizen (<span id="peopleCitizen"></span>) ' +
                        '<input id="checkBoxHeatmapCitizen" type="checkbox" name="Heatmap" value="false" onclick="toggleHeatmap(this.checked,\'Citizen\');"/>';
                // view heatmap (Commuter)
                div.innerHTML +=
                        '<br>Commuter (<span id="peopleCommuter"></span>) ' +
                        '<input id="checkBoxHeatmapCommuter" type="checkbox" name="Heatmap" value="false" onclick="toggleHeatmap(this.checked,\'Commuter\');"/>';
                // view heatmap (Student)
                div.innerHTML +=
                        ' Student (<span id="peopleStudent"></span>) ' +
                        '<input id="checkBoxHeatmapStudent" type="checkbox" name="Heatmap" value="false" onclick="toggleHeatmap(this.checked,\'Student\');"/>';
                // view heatmap (Tourist)
                div.innerHTML +=
                        ' Tourist (<span id="peopleTourist"></span>) ' +
                        '<input id="checkBoxHeatmapTourist" type="checkbox" name="Heatmap" value="false" onclick="toggleHeatmap(this.checked,\'Tourist\');"/>';
                // view heatmap (Disabled)
                div.innerHTML +=
                        '<br>Disabled (<span id="peopleDisabled"></span>) ' +
                        '<input id="checkBoxHeatmapDisabled" type="checkbox" name="Heatmap" value="false" onclick="toggleHeatmap(this.checked,\'Disabled\');"/>';
                // view heatmap (Operator)
                div.innerHTML +=
                        ' Operator (<span id="peopleOperator"></span>) ' +
                        '<input id="checkBoxHeatmapOperator" type="checkbox" name="Heatmap" value="false" onclick="toggleHeatmap(this.checked,\'Operator\');"/>';
                // view heatmap (Unknown)
                div.innerHTML +=
                        ' Unknown (<span id="peopleUnknown"></span>) ' +
                        '<input id="checkBoxHeatmapUnknown" type="checkbox" name="Heatmap" value="false" onclick="toggleHeatmap(this.checked,\'Unknown\');"/>';
                // time range
                div.innerHTML +=
                        '<br><br>Time Range: ' +
                        '<br>5 min ' +
                        '<input id="timeRange5" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'5\');"/>' +
                        ' 10 min ' +
                        '<input id="timeRange10" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'10\');"/>' +
                        ' 15 min ' +
                        '<input id="timeRange15" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'15\');"/>' +
                        ' 30 min ' +
                        '<input id="timeRange30" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'30\');"/>' +
                        ' 60 min ' +
                        '<input id="timeRange60" type="checkbox" name="timeRange" value="false" checked onclick="toggleTimeRange(this.checked,\'60\');"/>' +
                        '<br>3 hours ' +
                        '<input id="timeRange180" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'180\');"/>' +
                        ' 6 hours ' +
                        '<input id="timeRange360" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'360\');"/>' +
                        ' 9 hours ' +
                        '<input id="timeRange540" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'540\');"/>' +
                        ' 12 hours ' +
                        '<input id="timeRange720" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'720\');"/>' +
                        '<br>1 day ' +
                        '<input id="timeRange1440" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'1440\');"/>' +
                        ' 3 days ' +
                        '<input id="timeRange4320" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'4320\');"/>' +
                        ' 1 week ' +
                        '<input id="timeRange10080" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'10080\');"/>' +
                        ' 1 month ' +
                        '<input id="timeRange44640" type="checkbox" name="timeRange" value="false" onclick="toggleTimeRange(this.checked,\'44640\');"/>';
                // view distinct people
                div.innerHTML +=
                        '<br><br>Distinct people ' +
                        '<input id="checkBoxDistinctPeople" type="checkbox" name="DistinctPeople" value="false"/>';
                // use local extrema
                div.innerHTML +=
                        ' Local Extrema ' +
                        '<input id="checkBoxuseLocalExtrema" type="checkbox" name="useLocalExtrema" value="false" onclick="setOption(\'useLocalExtrema\',this.checked);"/>';
                // scale radius
                //div.innerHTML +=
                //' Scale Radius ' +
                //'<input id="checkBoxscaleRadius" type="checkbox" name="scaleRadius" value="true" checked onclick="setOption(\'scaleRadius\',this.checked);"/>';
                // view clustered markers
                // view movements clustered markers
                div.innerHTML +=
                        ' Movements ' +
                        '<input id="checkBoxMovementsClusteredMarkers" type="checkbox" name="movementsClusteredMarkers" value="false" onclick="toggleMovementsClusteredMarkers(this.checked);"/>';
                div.innerHTML +=
                        '<br>Clustered markers ' +
                        '<input id="checkBoxclusteredMarkers" type="checkbox" name="clusteredMarkers" value="false" onclick="toggleClusteredMarkers(this.checked);"/>';
                // view people clustered markers
                div.innerHTML +=
                        ' Location clustered markers ' +
                        '<input id="checkBoxPeopleClusteredMarkers" type="checkbox" name="peopleClusteredMarkers" value="false" onclick="togglePeopleClusteredMarkers(this.checked);"/>';
                // date picker
                div.innerHTML += '<br><br>To Date ' +
                        '<input id="checkBoxToDate" type="checkbox" name="toDate" value="false"/>';
                div.innerHTML +=
                        ' ' +
                        '<input type="text" name="startAt" id="datepicker">';
                // static date
                div.innerHTML += '&nbsp;&nbsp;&nbsp;static ' +
                        '<input id="checkBoxStaticToDate" type="checkbox" name="staticToDate" value="false"/>';
                // reply speed
                div.innerHTML += '<br><br>Reply Speed:<br>' +
                        'Realtime <input id="replySpeed0" type="checkbox" name="replySpeed0" checked value="false" onclick="toggleReplySpeed(this.checked,\'0\');"/>' +
                        '10 s <input id="replySpeed10" type="checkbox" name="replySpeed10" value="false" onclick="toggleReplySpeed(this.checked,\'10\');"/>' +
                        '30 s <input id="replySpeed30" type="checkbox" name="replySpeed30" value="false" onclick="toggleReplySpeed(this.checked,\'30\');"/>' +
                        '1 min <input id="replySpeed60" type="checkbox" name="replySpeed60" value="false" onclick="toggleReplySpeed(this.checked,\'60\');"/>' +
                        '5 min <input id="replySpeed300" type="checkbox" name="replySpeed300" value="false" onclick="toggleReplySpeed(this.checked,\'300\');"/>' +
                        '<br>15 min <input id="replySpeed900" type="checkbox" name="replySpeed900" value="false" onclick="toggleReplySpeed(this.checked,\'900\');"/>' +
                        '30 min <input id="replySpeed1800" type="checkbox" name="replySpeed1800" value="false" onclick="toggleReplySpeed(this.checked,\'1800\');"/>' +
                        '1 h <input id="replySpeed3600" type="checkbox" name="replySpeed3600" value="false" onclick="toggleReplySpeed(this.checked,\'3600\');"/>' +
                        '2 h <input id="replySpeed7200" type="checkbox" name="replySpeed7200" value="false" onclick="toggleReplySpeed(this.checked,\'7200\');"/>' +
                        '3 h <input id="replySpeed10800" type="checkbox" name="replySpeed10800" value="false" onclick="toggleReplySpeed(this.checked,\'10800\');"/>';
                // disable interaction of this div with map
                if (!L.Browser.touch) {
                    L.DomEvent.disableClickPropagation(div);
                    L.DomEvent.on(div, 'mousewheel', L.DomEvent.stopPropagation);
                } else {
                    L.DomEvent.on(div, 'click', L.DomEvent.stopPropagation);
                }
                return div;
            };
            // add legend to map
            legend.addTo(map);
            //map.fitBounds(coordinatesLine.getBounds());
            //map.setView([57.505, -0.01], 13);
            // refresh heat map 
            //setInterval(refreshHeatmap, 3000);
            // setInterval synchronous https://gist.github.com/adjohnson916/4385908
            /*var setIntervalSynchronous = function (func, delay) {
             var intervalFunction, timeoutId, clear;
             // Call to clear the interval.
             clear = function () {
             clearTimeout(timeoutId);
             };
             intervalFunction = function () {
             func();
             timeoutId = setTimeout(intervalFunction, delay);
             }
             // Delay start.
             timeoutId = setTimeout(intervalFunction, delay);
             // You should capture the returned function for clearing.
             return clear;
             };
             setIntervalSynchronous(refreshHeatmap, 10000);*/
            // get current time
            var currentTime = new Date().getTime() / 1000;
            refreshHeatmap();
            </script>
        </div> <!-- div container -->
    </body>
</html>
