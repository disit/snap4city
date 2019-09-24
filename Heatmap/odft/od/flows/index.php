<?php
//set headers to NOT cache a page
header("Cache-Control: no-cache, must-revalidate"); //HTTP 1.1
header("Pragma: no-cache"); //HTTP 1.0
header("Expires: Sat, 26 Jul 1997 05:00:00 GMT"); // Date in the past

session_start();
// check the permission
if (!isset($_SESSION["role"])) {
	header("location: ../../ssoLogin.php?org=" . $_REQUEST['org']);
}
?>
<html>
    <head>
        <!-- refresh page after 3 hours -->
        <meta http-equiv="refresh" content="10800" >
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <title><?php
if (isset($_REQUEST["title"])) {
	echo $_REQUEST["title"];
} else {
	echo $_REQUEST["org"] . " OD Flows";
}
?>
        </title>
        <!-- use leaflet 0.7.5 css with leaflet 1.0 js to make work the leaflet.curve.js plugin and the legend -->
        <!--<link rel="stylesheet" href="http://cdn.leafletjs.com/leaflet-0.7.5/leaflet.css" />-->
        <!--<link rel="stylesheet" type="text/css" href="../css/leaflet.css" />-->
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.3.1/leaflet.css" />
        <!--<link rel="stylesheet" type="text/css" href = "../css/jquery-ui.css"/>-->
        <link rel="stylesheet" type="text/css" href="../css/typography1.css" />
        <!--<script src="http://d3js.org/d3.v3.min.js"></script>-->
        <script src="../javascript/maps/d3.v3.min.js"></script>
        <script type="text/javascript" src="../javascript/jquery-2.1.0.min.js"></script>
        <!--<script src="http://cdn.leafletjs.com/leaflet/v1.0.0-rc.1/leaflet.js"></script>-->
        <!--<script src="../javascript/maps/leaflet-1.0.js"></script>-->
        <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.3.1/leaflet.js"></script>
        <!-- custom marker with id and icon attributes -->
        <script>
            CustomMarker = L.Marker.extend({
            options: {
            id: '',
                    defaultIcon: '',
                    text: '',
                    color: '',
                    radius: ''
            }
            });</script>
        <!-- leaflet curve plugin https://github.com/elfalem/Leaflet.curve -->
        <script src="../javascript/maps/leaflet.curve.js"></script>
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
        <!-- polyline decorator https://github.com/bbecquet/Leaflet.PolylineDecorator-->
        <!--<script src="../javascript/maps/leaflet_polylineDecorator/leaflet.polylineDecorator.js"></script>-->
        <!-- jquery csv plugin https://github.com/evanplaice/jquery-csv/ -->
        <script src="../javascript/jquery.csv.js"></script>
        <!-- include the grids for various cluster sizes -->
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_276.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_552.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_1104.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_2208.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_4416.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_8832.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_17664.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_35328.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_70656.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_141312.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_282624.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_565248.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_1130496.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_2260992.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_4521984.js"></script>
        <!--<script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_9043968.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_18087936.js"></script>
        <script src="../javascript/<?php echo $_REQUEST["org"]; ?>/grid_36175872.js"></script>-->

        <style>
            .curvesettings {
                position: absolute;
                right: 10px;
                top:6px;
                width:120px;
            }
            .box {
                border: 1px solid #EEE;
                margin: 3px;
                padding: 5px;
                background-color: white;
                font-family: sans-serif;
                font-size: 12px;
            }
            .title {
                font-weight: 600;
            }
        </style>
        <?php
if ($_SERVER["HTTP_REFERER"]) {
//(isset($_REQUEST["showFrame"]) && $_REQUEST['showFrame'] == 'false') || $_SESSION['showFrame'] == 'false') {
	$_SESSION['showFrame'] = 'false';
	echo "<style>
            body { zoom: 1.0; }
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
// https://github.com/elfalem/Leaflet.curve
if ($_SERVER["HTTP_REFERER"] == "") { // || ((!isset($_REQUEST["showFrame"]) || $_REQUEST['showFrame'] != 'false') && $_SESSION['showFrame'] != 'false')) {
	include_once "../header.php"; //include header
}
// get the midpoint of decimal coordinates NOT USED

//include_once "../login.php";

function midpoint($lat1, $lng1, $lat2, $lng2) {
	$lat1 = deg2rad($lat1);
	$lng1 = deg2rad($lng1);
	$lat2 = deg2rad($lat2);
	$lng2 = deg2rad($lng2);

	$dlng = $lng2 - $lng1;
	$Bx = cos($lat2) * cos($dlng);
	$By = cos($lat2) * sin($dlng);
	$lat3 = atan2(sin($lat1) + sin($lat2), sqrt((cos($lat1) + $Bx) * (cos($lat1) + $Bx) + $By * $By));
	$lng3 = $lng1 + atan2($By, (cos($lat1) + $Bx));
	$pi = pi();

	return json_encode(array(($lat3 * 180) / $pi, ($lng3 * 180) / $pi));
}

// build a bezier path with steps and angle from decimal coordinates ($lat1, $lon1, $lat2, $lon2), php version of https://gist.github.com/Reflejo/f5addfa6408d521a971f
function bezierPath($lat1, $lon1, $lat2, $lon2, $steps = 100, $angle = 90.0) {
	$auxiliaryPoint = fetchThirdPointByLocations($lat1, $lon1, $lat2, $lon2, $angle);
	$targetPoints = array();

	for ($i = 0; $i < $steps; $i++) {
		$t = $i / $steps;
		// Start point of the Bezier curve
		$bezier1x = $lon1 + ($auxiliaryPoint[1] - $lon1) * $t;
		$bezier1y = $lat1 + ($auxiliaryPoint[0] - $lat1) * $t;

		// End point of the Bezier curve
		$bezier2x = $auxiliaryPoint[1] + ($lon2 - $auxiliaryPoint[1]) * $t;
		$bezier2y = $auxiliaryPoint[0] + ($lat2 - $auxiliaryPoint[0]) * $t;

		$bezierPoint = array($bezier1y + ($bezier2y - $bezier1y) * $t,
			$bezier1x + ($bezier2x - $bezier1x) * $t);
		$targetPoints[] = $bezierPoint;
	}
	return $targetPoints;
}

// php version of https://gist.github.com/Reflejo/f5addfa6408d521a971f
function fetchThirdPointByLocations($lat1, $lon1, $lat2, $lon2, $angle) {
	$btpAngle = atan2(abs($lat1 - $lat2), abs($lon1 - $lon2)) * 180 / pi();
	$center = array(($lat1 + $lat2) / 2.0, ($lon1 + $lon2) / 2.0);

	$a = ($lat1 - $lat2) * ($lat1 - $lat2);
	$b = ($lon1 - $lon2) * ($lon1 - $lon2);
	$distance = sqrt($a + $b);
	$adis = ($distance / 2.0) / tan($angle / 2.0 * pi() / 180);

	$lng = $adis * cos((90 - $btpAngle) * pi() / 180);
	$lat = $adis * sin((90 - $btpAngle) * pi() / 180);

	return array($center[0] + $lat, $center[1] + $lng);
}

// convert decimal coordinates (latitude, longitude) to Mercator Projection (x, y)
// http://stackoverflow.com/questions/1369512/converting-longitude-latitude-to-x-y-on-a-map-with-calibration-points
// https://it.wikipedia.org/wiki/Proiezione_cilindrica_centrografica_modificata_di_Mercatore
// https://en.wikipedia.org/wiki/Mercator_projection
function coordinatesToMercatorProjection($lat, $lon) {
	$EARTH_RADIUS = 6371000;
	$x = $EARTH_RADIUS * log(tan(pi() / 4 + $lat / 180 * pi() / 2));
	$y = $lon / 180 * pi() * $EARTH_RADIUS;
	return array($x, $y);
}

// convert Mercator Projection (x, y) to decimal coordinates (latitude, longitude)
function mercatorProjectionToCoordinates($x, $y) {
	$EARTH_RADIUS = 6371000;
	$lat = (2 * atan(exp($x / $EARTH_RADIUS)) - pi() / 2) * 180 / pi();
	$lon = $y / $EARTH_RADIUS * 180 / pi();
	return array($lat, $lon);
}

// var map is an instance of a Leaflet map
// this function assumes you have added markers as GeoJSON to the map
// it will return an array of all features currently shown in the
// active bounding region.

//$x_y_1 = coordinatesToMercatorProjection(43.76990127563477, 11.25531959533691);
//$x_y_2 = coordinatesToMercatorProjection(44.76990127563477, 11.28531959533691);
//$bezier = bezierPath($x_y_1[0], $x_y_1[1], $x_y_2[0], $x_y_2[1], 10, 10);
/* $bezier = bezierPath(43.76990127563477, 11.25531959533691, 44.76990127563477, 11.28531959533691, 2, 90);
foreach ($bezier as $v) {
//echo json_encode(mercatorProjectionToCoordinates($v[0], $v[1])) . "<br>";
echo $v[0] . " " . $v[1] . "<br>";
} */
?>
        <!--  -->
        <script>
function getFeaturesInView() {
  var features = [];
  map.eachLayer( function(layer) {
    if(layer instanceof L.Marker) {
      if(map.getBounds().contains(layer.getLatLng())) {
        features.push(layer.feature);
      }
    }
  });
  return features;
}
<?php // remove flows from the map and reset markers icons to default ?>
            $(function () {
            $("button").click(function (event) {
<?php // prevent Clean button to propagate on map ?>
            event.preventDefault();
<?php // remove flows from the map ?>
            removeFlows();
<?php // reset markers icons to default ?>
            resetMarkersIcons();
            });
            });</script>
        <div id='container1'> <!-- div container -->
            <div id="map"></div>
            <form class="curvesettings">
                <!-- show/hide grid -->
                <div class="box legend">
                    <div class="title">Grid</div>
                    <input type="radio" name="showHideGrid" id="showHideGridOn" value="1" onclick="toggle(this.value)">on<br>
                    <input type="radio" name="showHideGrid" id="showHideGridOff" value="0" checked onclick="toggle(this.value)">off<br>
                    <!--<img src="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' version='1.2' width='14' height='14'><rect width='14' height='14' style='fill:none;stroke:green;stroke-width:4px;'/></svg>"/>-->
                    <img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0naHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmcnIHZlcnNpb249JzEuMicgd2lkdGg9JzE0JyBoZWlnaHQ9JzE0Jz48cmVjdCB3aWR0aD0nMTQnIGhlaWdodD0nMTQnIHN0eWxlPSdmaWxsOm5vbmU7c3Ryb2tlOmdyZWVuO3N0cm9rZS13aWR0aDo0cHg7Jy8+PC9zdmc+"/>
                    <span id="cluster"></span> km<sup>2</sup>
                </div>
                <div class="box legend" id="curvesettings">
                    <div class="title">Flows</div>
                    <div class="outflow"><input type="radio" name="flows" value="1" checked onclick="toggleFlows(this.value)">outflows</div>
                    <div class="inflow"><input type="radio" name="flows" value="0" onclick="toggleFlows(this.value)">inflows</div>
                    <button>Clean</button><br>
                </div>
                <div class="box legend" id="hour">
                    <div class="title">Hour</div>
                    <input type="radio" name="hour" value="00" onclick="redraw()">00&nbsp;
                    <input type="radio" name="hour" value="01" onclick="redraw()">01<br>
                    <input type="radio" name="hour" value="02" onclick="redraw()">02&nbsp;
                    <input type="radio" name="hour" value="03" onclick="redraw()">03<br>
                    <input type="radio" name="hour" value="04" onclick="redraw()">04&nbsp;
                    <input type="radio" name="hour" value="05" onclick="redraw()">05<br>
                    <input type="radio" name="hour" value="06" onclick="redraw()">06&nbsp;
                    <input type="radio" name="hour" value="07" onclick="redraw()">07<br>
                    <input type="radio" name="hour" value="08" onclick="redraw()">08&nbsp;
                    <input type="radio" name="hour" value="09" onclick="redraw()">09<br>
                    <input type="radio" name="hour" value="10" onclick="redraw()">10&nbsp;
                    <input type="radio" name="hour" value="11" onclick="redraw()">11<br>
                    <input type="radio" name="hour" value="12" onclick="redraw()">12&nbsp;
                    <input type="radio" name="hour" value="13" onclick="redraw()">13<br>
                    <input type="radio" name="hour" value="14" onclick="redraw()">14&nbsp;
                    <input type="radio" name="hour" value="15" onclick="redraw()">15<br>
                    <input type="radio" name="hour" value="16" onclick="redraw()">16&nbsp;
                    <input type="radio" name="hour" value="17" onclick="redraw()">17<br>
                    <input type="radio" name="hour" value="18" onclick="redraw()">18&nbsp;
                    <input type="radio" name="hour" value="19" onclick="redraw()">19<br>
                    <input type="radio" name="hour" value="20" onclick="redraw()">20&nbsp;
                    <input type="radio" name="hour" value="21" onclick="redraw()">21<br>
                    <input type="radio" name="hour" value="22" onclick="redraw()">22&nbsp;
                    <input type="radio" name="hour" value="23" onclick="redraw()">23<br>
                    <input type="radio" name="hour" value="" checked onclick="redraw()">All
                </div>
                <div class="box legend">
                    <div class="title">OD Matrix</div>
                    <!--<a id="od" href=\"./adj/adj.php?cluster=tourist&hour=10&cluster=1104" style="font-size: 12px;"><img src="data:image/svg+xml,<svg width='22' height='22' xmlns='http://www.w3.org/2000/svg'><defs><pattern id='smallGrid' width='5' height='5' patternUnits='userSpaceOnUse'><path d='M 5 0 L 0 0 0 5' fill='none' stroke='black' stroke-width='1'/></pattern><pattern id='grid' width='22' height='22' patternUnits='userSpaceOnUse'><rect width='21' height='21' fill='url(#smallGrid)'/><path d='M 20 0 L 0 0 0 20' fill='none' stroke='black' stroke-width='1'/></pattern></defs><rect width='22' height='22' fill='url(#grid)' /></svg>"/> OD Matrix</a>-->
                    <a id="od" href=\"./adj/adj.php?org=<?php echo $_REQUEST["org"]; ?>&hour=10&cluster=1104" style="font-size: 12px;" target="_blank"><img src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0nMjInIGhlaWdodD0nMjInIHhtbG5zPSdodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2Zyc+PGRlZnM+PHBhdHRlcm4gaWQ9J3NtYWxsR3JpZCcgd2lkdGg9JzUnIGhlaWdodD0nNScgcGF0dGVyblVuaXRzPSd1c2VyU3BhY2VPblVzZSc+PHBhdGggZD0nTSA1IDAgTCAwIDAgMCA1JyBmaWxsPSdub25lJyBzdHJva2U9J2JsYWNrJyBzdHJva2Utd2lkdGg9JzEnLz48L3BhdHRlcm4+PHBhdHRlcm4gaWQ9J2dyaWQnIHdpZHRoPScyMicgaGVpZ2h0PScyMicgcGF0dGVyblVuaXRzPSd1c2VyU3BhY2VPblVzZSc+PHJlY3Qgd2lkdGg9JzIxJyBoZWlnaHQ9JzIxJyBmaWxsPSd1cmwoI3NtYWxsR3JpZCknLz48cGF0aCBkPSdNIDIwIDAgTCAwIDAgMCAyMCcgZmlsbD0nbm9uZScgc3Ryb2tlPSdibGFjaycgc3Ryb2tlLXdpZHRoPScxJy8+PC9wYXR0ZXJuPjwvZGVmcz48cmVjdCB3aWR0aD0nMjInIGhlaWdodD0nMjInIGZpbGw9J3VybCgjZ3JpZCknIC8+PC9zdmc+"/></a>
                    <br><div class="title">Radius (km)</div>
                    <input type="radio" name="radius" value="200" onclick="toggleRadius()">200
                    <input type="radio" name="radius" value="500" onclick="toggleRadius()">500<br>
                    <input type="radio" name="radius" value="1000" onclick="toggleRadius()">1000
                    <input type="radio" name="radius" value="2000" onclick="toggleRadius()">2000<br>
                    <input type="radio" name="radius" value="0" checked onclick="toggleRadius()">All
                </div>
                <div class="box legend">
                    Zoom: <span id="zoom"></span>
                </div>
        </div>
    </form>
    <script>
<?php //setup map ?>
        var mbAttr = 'Map data &copy; <a class="leafletAttribution" href="https://openstreetmap.org">OpenStreetMap</a> contributors, ' +
                '<a class="leafletAttribution" href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
                'Imagery © <a class="leafletAttribution" href="https://mapbox.com">Mapbox</a>';
<?php // for satellite map use mapbox.streets-satellite in the url ?>
        var baseLayer = L.tileLayer('https://api.mapbox.com/v4/mapbox.streets/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoicGJlbGxpbmkiLCJhIjoiNTQxZDNmNDY0NGZjYTk3YjlkNTAzNWQwNzc0NzQwYTcifQ.CNfaDbrJLPq14I30N1EqHg', {
        attribution: mbAttr,
                maxZoom: 22,
        });
        // get map's center from organization
        var lat_lng = [43.76990127563477, 11.25531959533691];
        $.ajax({
         url: 'https://main.snap4city.org/api/organizations.php?org=<?php echo ($_REQUEST["org"] == "Florence" || $_REQUEST["org"] == "Tuscany" ? "DISIT" : $_REQUEST["org"]); ?>',
         dataType: 'json',
         async: false,
         //data: myData,
         success: function(data) {
          lat_lng = data[0].gpsCentreLatLng.trim().split(",");
         }
        });
        var map = new L.Map('map', {
         center: new L.LatLng(lat_lng[0], lat_lng[1]),
                 zoom: 11,
                 layers: [baseLayer]
         });
<?php // csv ?>
        var csv;
<?php // min/max flows ?>
        var flows_min_max;
<?php // flows csv map (id => (outFlow: x, inFlow: y) or id => (inFlow: x, outFlow: y)) ?>
        var links;
<?php // flows totals for every node ?>
        var flows;
<?php // links layers ?>
        var links_layers = [];
<?php // temp beziers curves layer ?>
        var beziers_layer_tmp;
<?php // temp grid layer ?>
        var grid_layer_tmp;
<?php // cluster size ?>
        var clusterSize = getClusterSize();
<?php // get coordinates map (id => (lat, lon)) ?>
        var coordinates;
<?php // flow direction (outflow, inflow) ?>
        var flow_direction = "outflow";
<?php // get the hour value ?>
        var hour = $("input:radio[name=hour]:checked").val();
<?php // set zoom level in legend ?>
        $("#zoom").text(map.getZoom());
<?php // set OD matrix link in legend ?>

<?php // load csv, draw circles and flows ?>
        $.ajax({
        url: "./<?php echo $_REQUEST["org"]; ?>/links_" + (hour != "" ? hour + "_" : "") + clusterSize + ".csv",
                async: true,
                success: function (csvd) {
                csv = $.csv.toArrays(csvd);
                },
                dataType: "text",
                complete: function () {
<?php // get min/max flows ?>
                flows_min_max = getMinMaxFlows(csv);
<?php // get flows totals for every node ?>
                flows = getFlows(csv);
<?php // get geoJSON, draw circles on map and populate links layers ?>
                draw(clusterSize);
                }
        });
<?php // get bezier curve joining two coordinates (lat, lon) ?>
        function getBezier(lat1, lon1, lat2, lon2, steps, angle, weight) {
        var bezierPath = getBezierPath(lat1, lon1, lat2, lon2, steps, angle);
        var color = flow_direction == "outflow" ? "darkblue" : "darkred";
        return L.curve(['M', [lat1, lon1],
                'Q', [bezierPath[0][0], bezierPath[0][1]],
        [bezierPath[1][0], bezierPath[1][1]],
        [bezierPath[2][0], bezierPath[2][1]],
        [bezierPath[3][0], bezierPath[3][1]],
        [bezierPath[4][0], bezierPath[4][1]],
        [bezierPath[5][0], bezierPath[5][1]],
        [bezierPath[6][0], bezierPath[6][1]],
        [bezierPath[7][0], bezierPath[7][1]],
        [bezierPath[8][0], bezierPath[8][1]],
        [bezierPath[9][0], bezierPath[9][1]],
                'T', [lat2, lon2]], {color: color, weight: weight});
        }

<?php // build a bezier path with steps and angle from decimal coordinates (lat1, lon1, lat2, lon2), javascript version of https://gist.github.com/Reflejo/f5addfa6408d521a971f
?>
        function getBezierPath(lat1, lon1, lat2, lon2, steps, angle) {
        auxiliaryPoint = fetchThirdPointByLocations(lat1, lon1, lat2, lon2, angle);
        targetPoints = [];
        for (i = 0; i < steps; i++) {
        t = i / steps;
        // Start point of the Bezier curve
        bezier1x = lon1 + (auxiliaryPoint[1] - lon1) * t;
        bezier1y = lat1 + (auxiliaryPoint[0] - lat1) * t;
        // End point of the Bezier curve
        bezier2x = auxiliaryPoint[1] + (lon2 - auxiliaryPoint[1]) * t;
        bezier2y = auxiliaryPoint[0] + (lat2 - auxiliaryPoint[0]) * t;
        bezierPoint = [bezier1y + (bezier2y - bezier1y) * t,
                bezier1x + (bezier2x - bezier1x) * t];
        targetPoints.push(bezierPoint);
        }
        return targetPoints;
        }

<?php // javascript version of https://gist.github.com/Reflejo/f5addfa6408d521a971f ?>
        function fetchThirdPointByLocations(lat1, lon1, lat2, lon2, angle) {
        btpAngle = Math.atan2(Math.abs(lat1 - lat2), Math.abs(lon1 - lon2)) * 180 / Math.PI;
        center = [(lat1 + lat2) / 2.0, (lon1 + lon2) / 2.0];
        a = (lat1 - lat2) * (lat1 - lat2);
        b = (lon1 - lon2) * (lon1 - lon2);
        distance = Math.sqrt(a + b);
        adis = (distance / 2.0) / Math.tan(angle / 2.0 * Math.PI / 180);
        lng = adis * Math.cos((90 - btpAngle) * Math.PI / 180);
        lat = adis * Math.sin((90 - btpAngle) * Math.PI / 180);
        return [center[0] + lat, center[1] + lng];
        }

<?php // calculate the midpoint of coordinates ?>
        function midpoint(lat1, lng1, lat2, lng2) {
        lat1 = deg2rad(lat1);
        lng1 = deg2rad(lng1);
        lat2 = deg2rad(lat2);
        lng2 = deg2rad(lng2);
        dlng = lng2 - lng1;
        Bx = Math.cos(lat2) * Math.cos(dlng);
        By = Math.cos(lat2) * Math.sin(dlng);
        lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        lng3 = lng1 + Math.atan2(By, (Math.cos(lat1) + Bx));
        return [(lat3 * 180) / Math.PI, (lng3 * 180) / Math.PI];
        }

<?php // calculate the area of a polygon of decimal coordinates (lat1, lon1, lat2, lon2) in m^2 http://mathforum.org/library/drmath/view/63767.html
?>
        function calculatePolygonArea(lat1, lat2, lon1, lon2) {
        //return 2 * Math.PI * Math.pow(6371000, 2) * Math.abs(Math.sin(deg2rad(lat1)) - Math.sin(deg2rad(lat2))) * Math.abs(lon1 - lon2) / 360;
        return (Math.PI / 180) * Math.pow(6371000, 2) * Math.abs(Math.sin(deg2rad(lat1)) - Math.sin(deg2rad(lat2))) * Math.abs(lon1 - lon2);
        }

<?php // calculate the area of a cluster from its center and cluster size (lat, lon, clusterSize) in m^2
?>
        function calculateClusterArea(lat, lon, clusterSize) {
        cluster = getClusterSquare(lat, lon, clusterSize);
        lat_center = cluster[0];
        lon_center = cluster[1];
        lat_right = cluster[2];
        lat_left = cluster[3];
        lon_top = cluster[4];
        lon_bottom = cluster[5];
        return calculatePolygonArea(lat_left, lat_right, lon_top, lon_bottom);
        }

<?php
// get cluster square coordinates (decimal latitude and longitude)
// ([lat_center, lon_center], [lat_top_right, lon_top_right], [lat_top_left, lon_top_left], [lat_bottom_left, lon_bottom_left], [lat_bottom_right, lon_bottom_right]) from coordinates
?>
        function getClusterSquare(latitude, longitude, clusterSize) {
        lat_cluster = Math.round(6371000 * Math.log(Math.tan(Math.PI / 4 + latitude / 180 * Math.PI / 2)) / clusterSize) * clusterSize;
        lon_cluster = Math.round(longitude / 180 * Math.PI * 6371000 / clusterSize) * clusterSize;
        lat_center = (2 * Math.atan(Math.exp(lat_cluster / 6371000)) - Math.PI / 2) * 180 / Math.PI;
        lon_center = lon_cluster / 6371000 * 180 / Math.PI;
        lat_right = (2 * Math.atan(Math.exp((lat_cluster + clusterSize / 2) / 6371000)) - Math.PI / 2) * 180 / Math.PI;
        lat_left = (2 * Math.atan(Math.exp((lat_cluster - clusterSize / 2) / 6371000)) - Math.PI / 2) * 180 / Math.PI;
        lon_top = (lon_cluster + clusterSize / 2) / 6371000 * 180 / Math.PI;
        lon_bottom = (lon_cluster - clusterSize / 2) / 6371000 * 180 / Math.PI;
        return [lat_center, lon_center, lat_right, lat_left, lon_top, lon_bottom];
        }

<?php // convert degrees to radiants ?>
        function deg2rad(value) {
        return value * Math.PI / 180;
        }

        function onMapClick(e) {
        //console.log(e.latlng);
        }

        //map.on('click', onMapClick);

<?php // if the user zooms, remove every layer, except the map, and redraw clusters ?>
        map.on("zoomend", function () {
        zoomLevel = map.getZoom();
        // set zoom level in legend
        $("#zoom").text(zoomLevel);
        redraw();
        });
<?php // remove every layer, except the map, and redraw clusters ?>
        function redraw() {
<?php // get the hour value ?>
        var hour = $("input:radio[name=hour]:checked").val();
        var grid = false;
<?php // check if the map has the grid ?>
        if (map.hasLayer(grid_layer_tmp)) {
        grid = true;
        }
<?php // remove every layer ?>
        map.eachLayer(function (layer) {
        if (layer != baseLayer) {
        map.removeLayer(layer);
        }
        });
        clusterSize = getClusterSize();
<?php // load csv, draw circles and flows ?>
        $.ajax({
        url: "./<?php echo $_REQUEST["org"]; ?>/links_" + (hour != "" ? hour + "_" : "") + clusterSize + ".csv",
                async: true,
                success: function (csvd) {
                csv = $.csv.toArrays(csvd);
                },
                dataType: "text",
                complete: function () {
<?php // get min/max flows ?>
                flows_min_max = getMinMaxFlows(csv);
<?php // get flows totals for every node ?>
                flows = getFlows(csv);
<?php // get geoJSON, draw circles on map and populate links layers ?>
                draw(clusterSize);
<?php // if the map had the grid, add it to the map ?>
                if (grid) {
                grid_layer_tmp = getGridLayer(clusterSize);
                grid_layer_tmp.addTo(map);
                }
                }
        });
        }

<?php // remove flows from the map ?>
        function removeFlows() {
        if (beziers_layer_tmp) {
        map.removeLayer(beziers_layer_tmp);
        }
        }

<?php // reset markers icons to default ?>
        function resetMarkersIcons() {
        $.each(map._layers, function (ml) {
        if (this.options.id) {
        this.setIcon(this.options.defaultIcon);
        }
        })
        }

<?php // set cluster size in legend ?>
        function setClusterArea() {
<?php // get coordinates of first cluster (not important which to choose) ?>
        var lat_lon;
        for (var key in coordinates) {
        lat_lon = coordinates[key];
        lat_lon = lat_lon.split(" ");
        break;
        }
        var area = calculateClusterArea(lat_lon[0], lat_lon[1], clusterSize);
        //console.log(lat_lon[0] + " " + lat_lon[1] + " " + clusterSize);
        $("#cluster").text(Math.round(area * 100 / 1000000) / 100);
        }

<?php // get geoJSON, draw circles on map and populate links layers ?>
        function draw(clusterSize) {
lon_max = map.getBounds().getEast();
lon_min = map.getBounds().getWest();
lat_max = map.getBounds().getNorth();
lat_min = map.getBounds().getSouth();
<?php // get the hour value ?>
        var hour = $("input:radio[name=hour]:checked").val();
<?php // get the radius value ?>
        var radius = $("input:radio[name=radius]:checked").val();
<?php // set OD matrix link in legend ?>
        var hour_p = hour != "" ? "&hour=" + hour : "";
        var radius_p = radius != 0 ? "&radius=" + radius : "";
        $("#od").attr("href", "./adj/adj.php?org=<?php echo $_REQUEST["org"]; ?>&cluster=" + clusterSize + hour_p + radius_p + "&title=<?php echo urlencode("Origin Destination matrix for People Flows&nbsp;"); ?>");
        $.getJSON("./<?php echo $_REQUEST["org"]; ?>/nodes_" + (hour != "" ? hour + "_" : "") + clusterSize + ".geojson").done(function (data) {
<?php // reset links_layers ?>
        links_layers = [];
<?php // get coordinates map (id => (lat, lon)) ?>
        coordinates = getCoordinatesMap(data);
<?php // get flows csv map (id => (outFlow: x, inFlow: y) or id => (inFlow: x, outFlow: y)) ?>
        links = getLinksMap(csv);
<?php // loop links[target][source] array and populate links layers ?>
<?php // note that in case flow_direction = "inflow", target and source are swapped (target is the source, and source is the target)
?>
        for (var target in links) {
        var lat_lon_trg = coordinates[target];
        lat_lon_trg = lat_lon_trg.split(" ");
        if(parseFloat(lat_lon_trg[0]) >= lat_min && parseFloat(lat_lon_trg[0]) <= lat_max && parseFloat(lat_lon_trg[1]) >= lon_min && parseFloat(lat_lon_trg[1]) <= lon_max) {
        for (var source in links[target]) {
        var flow = links[target][source];
        var lat_lon_src = coordinates[source];
        lat_lon_src = lat_lon_src.split(" ");
        if(parseFloat(lat_lon_src[0]) >= lat_min && parseFloat(lat_lon_src[0]) <= lat_max && parseFloat(lat_lon_src[1]) >= lon_min && parseFloat(lat_lon_src[1]) <= lon_max) {
        // Math.log(flow) / Math.log(n) = log in base n of flow
        var bezier = getBezier(parseFloat(lat_lon_src[0]), parseFloat(lat_lon_src[1]), parseFloat(lat_lon_trg[0]), parseFloat(lat_lon_trg[1]), 10, 120, Math.max(1, Math.min(Math.log(flow) / Math.log(1.8), 5)));
        /*if (!links_layers[target]) {
         var layer = L.layerGroup([]);
         layer.addLayer(bezier);
         links_layers[target] = layer;
         } else {
         var layer = links_layers[target];
         layer.addLayer(bezier);
         links_layers[target] = layer;
         }*/
        if (!links_layers[source]) {
        var layer = L.layerGroup([]);
        layer.addLayer(bezier);
        links_layers[source] = layer;
        } else {
        var layer = links_layers[source];
        layer.addLayer(bezier);
        links_layers[source] = layer;
        }
        }
        }
        }
        }
<?php // draw geoJSON data ?>
        drawGeoJSON(data);
<?php // set cluster area in legend ?>
        setClusterArea();
        });
        }

        function onEachFeature(feature, layer) {
        var popupContent = "<p>" + feature.properties.LAT + " " + feature.properties.LON + "</p>";
        if (feature.properties && feature.properties.popupContent) {
        popupContent += feature.properties.popupContent;
        }
        layer.bindPopup(popupContent);
        }

<?php // draw geoJSON data ?>
        function drawGeoJSON(geoJSON) {
        lon_max = map.getBounds().getEast();
        lon_min = map.getBounds().getWest();
        lat_max = map.getBounds().getNorth();
        lat_min = map.getBounds().getSouth();
        L.geoJson(geoJSON, {
        style: function (feature) {
        return feature.properties && feature.properties.style;
        },
                //onEachFeature: onEachFeature,
                pointToLayer: function (feature, latlng) {
                lat = latlng.lat;
                lng = latlng.lng;
                if(lat >= lat_min && lat <= lat_max && lng >= lon_min && lng <= lon_max) {
                flow = flows[feature["id"]];
                flow = isNaN(flow) ? 0 : flow;
                color = getColor(flow, flows_min_max[0], flows_min_max[1]);
                radius = getRadius(flow, flows_min_max[0], flows_min_max[1]);
<?php // make the svg icon https://groups.google.com/forum/#!topic/leaflet-js/GSisdUm5rEc ?>
                //var xml = '<\?xml version="1.0" encoding="utf-8"\?><!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="100" height="100">';
                //var svg = xml + "<circle cx=\"50\" cy=\"50\" r=\"" + radius + "\" opacity=\"0.7\" style=\"fill:" + color + "\"></circle></svg>";
<?php
// if you don't use base64 (data:image/svg+xml;base64,) but svg (data:image/svg+xml,) you must encodeURIComponent the color to be compatible with Firefox (color contains # that must be encoded)
// but this will not work on Interner Explorer
// viewBox is used to allow svg resizing with CTRL-wheel on Firefox
?>
                var svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" version=\"1.2\"><circle cx=\"50\" cy=\"50\" r=\"" + radius + "\" opacity=\"0.7\" style=\"fill:" + color + "\"></circle></svg>";
                var svgURL = "data:image/svg+xml;base64," + btoa(svg); // btoa = Base64 of svg
                // create icon
                var SVGIcon = L.icon({
                iconUrl: svgURL,
                        iconSize: [100, 100],
                        shadowSize: [12, 10],
                        iconAnchor: [50, 50],
                        popupAnchor: [5, - 5],
                        labelAnchor: [0, 0]
                });
                /*return L.circleMarker(latlng, {
                 radius: radius,
                 fillColor: color,
                 color: "#000",
                 weight: 1,
                 opacity: 1,
                 fillOpacity: 0.8
                 }*/
<?php // marker, on click draws beziers links between this marker and its targets ?>
                return new CustomMarker(latlng, {icon: SVGIcon, defaultIcon: SVGIcon, id: feature["id"], text: flow, color: color, radius: radius}).
                        //bindPopup('Lat: ' + latlng.lat + '<br>Lon: ' + latlng.lng).
                        //bindLabel('Lat: ' + latlng.lat + '<br>Lon: ' + latlng.lng).
                        on('click', function (e) {console.log(e.target.options.id);
<?php // remove flows from the map and reset markers icons to default ?>
                        if (beziers_layer_tmp) {
                        map.removeLayer(beziers_layer_tmp);
                        resetMarkersIcons();
                        }
<?php // set text icon with the flow from this marker ?>
                        setMarkerHighlightedIcon(e.target.options.id, e.target.options.text);
<?php // add the flows layers from this marker on the map ?>
                        if (links_layers[e.target.options.id]) {
                        beziers_layer_tmp = links_layers[e.target.options.id];
                        beziers_layer_tmp.addTo(map);
                        }
                        })/*.
                         on('mouseover', function (e) {
                         if (links_layers[feature["id"]]) {
                         beziers_layer_tmp = links_layers[feature["id"]];
                         beziers_layer_tmp.addTo(map);
                         }
                         }).
                         on('mouseout', function (e) {
                         map.removeLayer(beziers_layer_tmp);
                         })*/
                }
               }
        }).addTo(map);
        }

<?php // set the marker highlighted icon ?>
        function setMarkerHighlightedIcon(id, flow) {
<?php // loop markers ?>
        $.each(map._layers, function (ml) {
<?php // if this marker is the clicked marker (which called this method) ?>
        if (this.options.id == id) {
        var icon = getHighlightedIcon(this, this.options.text, this.options.color, this.options.radius);
        sourceFlow = this.options.text;
        this.setIcon(icon);
        }
<?php // if this marker is a target marker for this marker ?>
        else if (this.options.id && links[this.options.id] && links[this.options.id][id]) {
        var text = links[this.options.id][id] + "/" + (Math.round(parseFloat(links[this.options.id][id]) * 100 / parseFloat(flow))) + "%";
        var icon = getHighlightedIcon(this, text, this.options.color, this.options.radius);
        this.setIcon(icon);
        }
        }
        );
        }

<?php // get the highlighted icon for a marker ?>
        function getHighlightedIcon(marker, flow, color, radius) {
        //var xml = '<\?xml version="1.0" encoding="utf-8"\?><!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="100" height="100">';
        //var text = "<text x=\"54%\" y=\"50%\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"22px\" stroke=\"black\" stroke-width=\"1.4\" fill=\"white\" font-weight=\"bold\">" + flow + "</text>";
        var text;
        try {
<?php // svg of the linked zones ?>
        if ((flow + "").indexOf("/") > - 1) {
        var span = flow.split("/");
        text = "<text x=\"54%\" y=\"50%\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"24px\" stroke=\"black\" stroke-width=\"1.4\" fill=\"white\" font-weight=\"bold\"><tspan x=\"50\">" + span[0] + "</tspan><tspan x=\"60\" dy=\"20\">" + span[1] + "</tspan></text>";
        }
<?php // svg of the clicked zone ?>
        else {
        text = "<text x=\"49%\" y=\"54%\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"22px\" stroke=\"black\" stroke-width=\"1.4\" fill=\"white\" font-weight=\"bold\">" + flow + "</text>";
        }
        } catch (err) {
        //console.log(text);
        }
        //var svgHighlighted = xml + "<g><circle cx=\"50\" cy=\"50\" r=\"" + radius + "\" opacity=\"1.0\" style=\"stroke:" + color + "; fill:" + color + "; fill-opacity: 0.3\"></circle>" + text + "</g></svg>";
        //var svgHighlighted = xml + "<g><circle cx=\"50\" cy=\"50\" r=\"" + radius + "\" opacity=\"1.0\" style=\"stroke:black; stroke-width: 2; fill:" + color + "; fill-opacity: 0.7\"></circle>" + text + "</g></svg>";
<?php
// if you don't use base64 (data:image/svg+xml;base64,) but svg (data:image/svg+xml,) you must encodeURIComponent the color to be compatible with Firefox (color contains # that must be encoded)
// but this will not work on Interner Explorer
// viewBox is used to allow svg resizing with CTRL-wheel on Firefox
?>
        var svgHighlighted = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" version=\"1.2\"><g><circle cx=\"50\" cy=\"50\" r=\"" + radius + "\" opacity=\"1.0\" style=\"stroke:black; stroke-width: 2; fill:" + color + "; fill-opacity: 0.7\"></circle>" + text + "</g></svg>";
        var svgURLHighlighted = "data:image/svg+xml;base64," + btoa(svgHighlighted); // btoa = Base64 of svgHighlighted
<?php // create highlighted icon ?>
        return SVGIconHighlighted = L.icon({
        iconUrl: svgURLHighlighted,
                iconSize: [100, 100],
                shadowSize: [12, 10],
                iconAnchor: [50, 50],
                popupAnchor: [5, - 5],
                labelAnchor: [0, 0]
        });
        }

<?php // toggle the grid ?>
        function toggle(action) {
        if (action == 1) {
        if (grid_layer_tmp) {
        map.removeLayer(grid_layer_tmp);
        }
        var clusterSize = getClusterSize();
        grid_layer_tmp = getGridLayer(clusterSize);
        grid_layer_tmp.addTo(map);
        } else {
        map.removeLayer(grid_layer_tmp);
        }
        }

<?php // toggle the flow (outflow, inflow) ?>
        function toggleFlows(action) {
<?php // remove flows from the map ?>
        //removeFlows();
<?php // reset markers icons to default ?>
        //resetMarkersIcons();
<?php // outflow ?>
        if (action == 1) {
        flow_direction = "outflow";
        } else {
        flow_direction = "inflow";
        }
        redraw();
        }

<?php // set the OD matrix link when the radius is set by the radio button in the OD Matrix legend ?>
        function toggleRadius() {
<?php // get the hour value ?>
        var hour = $("input:radio[name=hour]:checked").val();
<?php // get the radius value ?>
        var radius = $("input:radio[name=radius]:checked").val();
<?php // set OD matrix link in legend ?>
        var hour_p = hour != "" ? "&hour=" + hour : "";
        var radius_p = radius != 0 ? "&radius=" + radius : "";
        $("#od").attr("href", "./adj/adj.php?org=<?php echo $_REQUEST["org"];?>&cluster=" + clusterSize + hour_p + radius_p + "&title=<?php echo urlencode("Origin Destination matrix for People Flows&nbsp;"); ?>");
        }

        function getGridLayer(clusterSize) {
        switch (clusterSize) {
        case 276:
                return gridLayer_276;
        case 552:
                return gridLayer_552;
        case 1104:
                return gridLayer_1104;
        case 2208:
                return gridLayer_2208;
        case 4416:
                return gridLayer_4416;
        case 8832:
                return gridLayer_8832;
        case 17664:
                return gridLayer_17664;
        case 35328:
                return gridLayer_35328;
        case 70656:
                return gridLayer_70656;
        case 141312:
                return gridLayer_141312;
        case 282624:
                return gridLayer_282624;
        case 565248:
                return gridLayer_565248;
        case 1130496:
                return gridLayer_1130496;
        case 2260992:
                return gridLayer_2260992;
        case 4521984:
                return gridLayer_4521984;
        case 9043968:
                return gridLayer_9043968;
        case 18087936:
                return gridLayer_18087936;
        case 36175872:
                return gridLayer_36175872;
        default:
                return gridLayer_1104;
        }
        }

        function getClusterSize() {
        zoomLevel = map.getZoom();
        switch (zoomLevel) {
        case 22:
                return 276;
        case 21:
                return 276;
        case 20:
                return 276;
        case 19:
                return 276;
        case 18:
                return 276;
        case 17:
                return 276;
        case 16:
                return 276;
        case 15:
                return 276;
        case 14:
                return 552;
        case 13:
                return 1104;
        case 12:
                return 2208;
        case 11:
                return 4416;
        case 10:
                return 8832;
        case 9:
                return 17664;
        case 8:
                return 35328;
        case 7:
                return 70656;
        case 6:
                return 141312;
        case 5:
                return 282624;
        case 4:
                return 565248;
        case 3:
                return 1130496;
        case 2:
                return 2260992;
        case 1:
                return 4521984;
        case 0:
                return 4521984;
        default:
                return 256;
        }
        }

<?php // get min/max flows ?>
        function getMinMaxFlows(csv) {
        max = 0;
        min = 9999999999;
<?php // skip header (i = 0) ?>
        for (i = 1; i < csv.length; i++) {
        if (parseInt(csv[i][2]) > max) {
        max = parseInt(csv[i][2]);
        }
        if (parseInt(csv[i][2]) < min) {
        min = parseInt(csv[i][2]);
        }
        }
        return [min, max];
        }

<?php // get color for node ?>
        function getColor(flow, flow_min, flow_max) {
        if (flow == 0) {
        return "yellow";
        }
        var diff = flow - flow_min;
        range = flow_max - flow_min;
        load = diff / range;
        color_range = ["yellow", "#e50000"]; // ["yellow", "red"]
<?php // instantiate color scale function ?>
        var color = d3.scale.linear()
                .domain([0, 1])
                .range(color_range);
        return color(load);
        }

<?php // get radius for node ?>
        function getRadius(flow, flow_min, flow_max) {
        var diff = flow - flow_min;
        var range = flow_max - flow_min;
        var radius = (flow_max - flow_min) * (diff / range) + flow_min;
        radius = isNaN(radius) ? 0 : radius;
        return Math.min(30, Math.log(radius) / Math.log(1.4));
        }

<?php // get flows csv map (id => (outFlow: x, inFlow: y) or id => (inFlow: x, outFlow: y)) ?>
        function getLinksMap(csv) {
        var links = [];
<?php // outflows ?>
        if (flow_direction == "outflow") {
<?php // skip header (i = 0) ?>
        for (i = 1; i < csv.length; i++) {
        if (!links[csv[i][0]]) {
        links[csv[i][0]] = {};
        }
        links[csv[i][0]][csv[i][1]] = parseInt(csv[i][2]);
        }
        }
<?php // inflows ?>
        else {
<?php // skip header (i = 0) ?>
        for (i = 1; i < csv.length; i++) {
        if (!links[csv[i][1]]) {
        links[csv[i][1]] = {};
        }
        links[csv[i][1]][csv[i][0]] = parseInt(csv[i][2]);
        }
        }
        return links;
        }

<?php // get totals flows for every node ?>
        function getFlows(csv) {
        var flows = [];
<?php // outflows ?>
        if (flow_direction == "outflow") {
<?php // skip header (i = 0) ?>
        for (i = 1; i < csv.length; i++) {
        if (!flows[csv[i][1]]) {
        flows[csv[i][1]] = parseInt(csv[i][2])
        } else {
        flows[csv[i][1]] += parseInt(csv[i][2]);
        }
        }
        }
<?php // inflows ?>
        else {
<?php // skip header (i = 0) ?>
        for (i = 1; i < csv.length; i++) {
        if (!flows[csv[i][0]]) {
        flows[csv[i][0]] = parseInt(csv[i][2])
        } else {
        flows[csv[i][0]] += parseInt(csv[i][2]);
        }
        }
        }
        return flows;
        }

<?php // get coordinates map (id => (lat, lon)) ?>
        function getCoordinatesMap(geoJSON) {
        var coordinates = [];
        var features = geoJSON.features;
        for (i = 0; i < features.length; i++) {
        id = features[i].id;
        lat = features[i].properties.LAT;
        lon = features[i].properties.LON;
        coordinates[id] = lat + " " + lon;
        }
        return coordinates;
        }
    </script>
</div> <!-- div container -->
</body>
</html>
