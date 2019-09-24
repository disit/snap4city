<?php
// set unlimited memory usage for server
ini_set('memory_limit', '-1');

//set headers to NOT cache a page
header("Cache-Control: no-cache, must-revalidate"); //HTTP 1.1
header("Pragma: no-cache"); //HTTP 1.0
header("Expires: Sat, 26 Jul 1997 05:00:00 GMT"); // Date in the past

session_start();
$_REQUEST["profile"] = "S4C" . $_REQUEST["org"] . "TrackerLocation";

?>
<html>
    <head>
        <!-- refresh page after 3 hours -->
        <meta http-equiv="refresh" content="10800" >
        <title><?php
if (isset($_REQUEST["title"])) {
	filter_var($_REQUEST["title"], FILTER_SANITIZE_STRING);
} else {
	echo "Heatmap";
}
?>
        </title>
        <link rel="stylesheet" type="text/css" href="css/reset.css" />
        <link rel="stylesheet" type="text/css" href="css/style.css" />
        <link rel="stylesheet" type="text/css" href="css/typography1.css" />
        <link rel="stylesheet" type="text/css" href = "css/jquery-ui.css"/>
        <script type="text/javascript" src="javascript/jquery-2.1.0.min.js"></script>
        <script type="text/javascript" src="javascript/jquery-ui.min.js"></script>
        <script type="text/javascript" src="javascript/jquery.redirect.js"></script>

        <!-- map headers -->
        <!--<link rel="stylesheet" href="http://cdn.leafletjs.com/leaflet-0.7.5/leaflet.css" />-->
        <!--<link rel="stylesheet" href="javascript/leaflet.css" />-->
        <!--<link rel="stylesheet" href="css/leaflet.css" />-->
        <!--<script src="http://cdn.leafletjs.com/leaflet-0.7.5/leaflet.js"></script>-->
        <!--<script src="javascript/leaflet.js"></script>-->
        <!--<script type="text/javascript" src="javascript/maps/leaflet.js"></script>-->
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.3.1/leaflet.css" />
        <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.3.1/leaflet.js"></script>

        <!--leaflet label plugin includes https://github.com/Leaflet/Leaflet.label-->
        <script src = "javascript/maps/leaflet-label-plugin/Label.js" ></script>
        <script src="javascript/solr.js" type="text/javascript"></script>
        <script src="javascript/maps/leaflet-label-plugin/BaseMarkerMethods.js"></script>
        <script src="javascript/maps/leaflet-label-plugin/Marker.Label.js"></script>
        <script src="javascript/maps/leaflet-label-plugin/CircleMarker.Label.js"></script>
        <script src="javascript/maps/leaflet-label-plugin/Path.Label.js"></script>
        <script src="javascript/maps/leaflet-label-plugin/Map.Label.js"></script>
        <script src="javascript/maps/leaflet-label-plugin/FeatureGroup.Label.js"></script>
        <link rel="stylesheet" href="javascript/maps/leaflet-label-plugin/leaflet.label.css" />
        <!-- jquery scroll to plugin includes http://demos.flesler.com/jquery/scrollTo/ -->
        <script type="text/javascript" src="javascript/maps/jquery.scrollTo-2.1.2/jquery.scrollTo.min.js"></script>
                <!-- <style>
                    .map {
                        position: absolute;
                        width: 100%;
                        height: 100%;
                    }
                </style> -->
        <!-- heat map and leaflet plugin http://www.patrick-wied.at/static/heatmapjs/example-heatmap-leaflet.html -->
        <script src = "javascript/maps/heatmap.js" ></script>
        <script src = "javascript/maps/leaflet-heatmap.js" ></script>

        <!-- marker cluster plugin https://github.com/Leaflet/Leaflet.markercluster-->
        <!--<link rel="stylesheet" href="javascript/maps/markercluster/MarkerCluster.css" />
        <link rel="stylesheet" href="javascript/maps/markercluster/MarkerCluster.Default.css" />
        <script src="javascript/maps/markercluster/leaflet.markercluster-src.js"></script>-->
        <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/leaflet.markercluster-src.js" integrity="sha256-UxMFwvJ2+HaHDi1Ik5WYCuUcv1yS+hS5QYitB0ev0JQ=" crossorigin="anonymous"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/leaflet.markercluster.js" integrity="sha256-WL6HHfYfbFEkZOFdsJQeY7lJG/E5airjvqbznghUzRw=" crossorigin="anonymous"></script>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/MarkerCluster.Default.css" integrity="sha256-LWhzWaQGZRsWFrrJxg+6Zn8TT84k0/trtiHBc6qcGpY=" crossorigin="anonymous" />
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/MarkerCluster.Default.css" integrity="sha256-LWhzWaQGZRsWFrrJxg+6Zn8TT84k0/trtiHBc6qcGpY=" crossorigin="anonymous" />

        <?php
if ((isset($_REQUEST["showFrame"]) && $_REQUEST['showFrame'] == 'false') || $_SESSION['showFrame'] == 'false') {
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
if (isset($_REQUEST["showFrame"]) && $_REQUEST['showFrame'] != 'false') {
	include_once "header.php"; //include header
}
include_once "functions.php"; //function getBannedUsers
?>
        <div id='container1'> <!-- div container -->
            <?php
include_once "settings.php";
include_once "Polyline.php";

// get the heatmap data with default values
$hour = "";
$clusterSize = "552";
function getHeatMapData($org, $hour, $clusterSize) {
	global $config;
	$nodes = array();
	$json = json_decode(file_get_contents("./flows/" . $org . "/nodes_" . ($hour != "" ? $hour . "_" : "") . $clusterSize . ".geojson"), true);
	foreach ($json["features"] as $feature) {
		$nodes[$feature["id"]] = array($feature["properties"]["LAT"], $feature["properties"]["LON"]);
	}
	// Open the file for reading
	$i = 0;
        $csv = array();
	if (($h = fopen("./flows/" . $_REQUEST["org"] . "/links_" . ($hour != "" ? $hour . "_" : "") . $clusterSize . ".csv", "r")) !== FALSE) {
		// Convert each line into the local $data variable
		while (($data = fgetcsv($h, 1000, ",")) !== FALSE) {
			// Read the data from a single line
			if ($i != 0) {
				$csv[] = $data;
			}
			$i++;
		}

		// Close the file
		fclose($h);
	}
	$i = 0;
	$heatmapdata = "";
	foreach ($csv as $c) {
		$heatmapdata .= ($i != 0 ? ", " : "") . " {\"lat\": " . $nodes[$c[0]][0] . ", \"lng\":" . $nodes[$c[0]][1] . ", \"count\": " . $c[2] . "}";
		$i++;
	}
	return "[" . $heatmapdata . "]";
}

// get the heatmap data
$heatMapData = getHeatMapData($_REQUEST["org"], $hour, $clusterSize);
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
                    maxZoom: 22,
                });
                //heatmap flows data
                var flowsData = {
                    max: 8,
                    data: <?php echo $heatMapData; ?>
                };
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
                //setup flows heatmap
                // get map's center from organization
        var lat_lng = [43.76990127563477, 11.25531959533691];
        $.ajax({
         url: 'https://main.snap4city.org/api/organizations.php?org=<?php echo $_REQUEST["org"] == "Florence" ? "DISIT" : $_REQUEST["org"]; ?>',
         dataType: 'json',
         async: false,
         //data: myData,
         success: function(data) {
          lat_lng = data[0].gpsCentreLatLng.trim().split(",");
         }, 
         error: function(data) {
         }
        });
                var heatmapLayer = new HeatmapOverlay(cfg);
                var map = new L.Map('map', {
                    center: new L.LatLng(lat_lng[0], lat_lng[1]),
                    zoom: 14,
                    layers: [baseLayer, heatmapLayer]
                });
                heatmapLayer.setData(flowsData);

                // set cluster trajectories to be set by the function getTrajectory (generated by the Java application TrajectoriesClustering) in clusteredTrajectories_[profile].js
                var clusterTrajectories = "";
                map.on('click', function () {
                    map.removeLayer(clusterTrajectories);
                });
                // view clustered trajectory
<?php
if (isset($_REQUEST["cluster"])) {
	$data = file_get_contents("http://localhost/screcommender/recommender/getCluster.php?profile=" . $_REQUEST["profile"] . "&cluster=" . $_REQUEST["cluster"]);
	echo "var clusterTrajectory = " . $data . ";";
	echo "map.addLayer(clusterTrajectory);";
	echo "map.fitBounds(clusterTrajectory.getBounds());";
}
?>
</script>
<!--include clustered trajectories-->
            <script type="text/javascript" src="javascript/clusteredTrajectories_<?php echo isset($_REQUEST["profile"]) ? $_REQUEST["profile"] : "";
?>.js"></script>
<script>
map.on("zoomend", function () {
        zoomLevel = map.getZoom();
        // set zoom level in legend
        $("#zoom").text(zoomLevel);console.log(zoomLevel);
        redraw();
        toggleClusteredTrajectories($("#checkBoxClusteredTrajectories").is(":checked"));
        });

                function redraw() {
                 var hour = $("input:radio[name=hour]:checked").val();
                 clusterSize = getClusterSize();
                 map.eachLayer(function (layer) {
                  if (layer != baseLayer) {
                   map.removeLayer(layer);
                  }
                 });
                 $.ajax({
         url: './getHeatMapData.php?org=<?php echo $_REQUEST["org"]; ?>&hour='+hour+'&clusterSize='+clusterSize,
         dataType: 'json',
         async: false,
         success: function(d) {
         flowsData = {
                    max: 8,
                    data: d
                };
         heatmapLayer.setData(flowsData);
         heatmapLayer.addTo(map);
         //console.log(d);
         }
        });
                }

/*function getClusterSize() {
                 zoomLevel = map.getZoom();
                 switch (zoomLevel) {
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
                return 1104;
        }
        }*/

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
                return 276;
        }
        }
                // add a legend, http://leafletjs.com/examples/choropleth.html
                var legend = L.control({position: 'topright'});
                function setColor(color, value, decimals) {
                    cfg["gradient"][value] = color;
                    //document.getElementById("range" + color).innerHTML = value;
                    $("#range" + color).text(parseFloat(value).toFixed(parseInt(decimals)));
                    $("#slider" + color).attr("value", parseFloat(value).toFixed(parseInt(decimals)));
                    map.removeLayer(heatmapLayer);
                    heatmapLayer = new HeatmapOverlay(cfg);
                    heatmapLayer.setData(flowsData);
                    map.addLayer(heatmapLayer);
                }
                function setOption(option, value, decimals) {
                    cfg[option] = value;
                    //set values for sliders, not for checkboxes
                    if (decimals) {
                        $("#range" + option).text(parseFloat(value).toFixed(parseInt(decimals)));
                        $("#slider" + option).attr("value", parseFloat(value).toFixed(parseInt(decimals)));
                    }
                    heatmapLayer.configure(cfg);
                }
                function toggleFlowsHeatmap(toggle) {
                    if (toggle) {
                        map.addLayer(heatmapLayer);
                    } else {
                        map.removeLayer(heatmapLayer);
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
                function filterClusteredTrajectories(option, limit) {
                    $("#range" + option).text(parseInt(limit));
                    $("#slider" + option).attr("value", parseInt(limit));
                    $("#checkBoxClusteredTrajectories").prop("checked", true);
                    map.removeLayer(clusteredTrajectories);
                    var tmp = [];
                    for (i = 0; i < polyline_list.length; i++) {
                        var layers = polyline_list[i].getLayers();
                        var size = parseInt(layers[0].options.className);
                        if (size >= limit) {
                            tmp.push(polyline_list[i]);
                        }
                    }
                    clusteredTrajectories = L.layerGroup(tmp);
                    // update the number of filtered clustered trajectories in the legend
                    $("#numClusteredTrajectories").text(clusteredTrajectories.getLayers().length);
                    map.addLayer(clusteredTrajectories);
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
                    div.innerHTML += '<div class="text">' + '<?php echo ucfirst(isset($_REQUEST["profile"]) ? $_REQUEST["profile"] : "Global"); ?>' + '</div>';
                    // radius
                    div.innerHTML +=
                            '<br>Radius: &nbsp;&nbsp;&nbsp;&nbsp;' +
                            '<a id="unselect" style="cursor:pointer" onclick="downSlider(\'radius\',0.00001,6,0);">&#10094;</a>&nbsp;&nbsp;&nbsp;' +
                            '<input id="sliderradius" type="range" min="0" max="0.0010" value="0.0008" step="0.00001" onchange="setOption(\'radius\',this.value,6);">' +
                            '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a id="unselect" style="cursor:pointer" onclick="upSlider(\'radius\',0.00001,6,0.0010);">&#10095;</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
                            '<span id="rangeradius">0.0008</span>';
                    // max opacity
                    div.innerHTML +=
                            '<br>Max Opacity: &nbsp;&nbsp;&nbsp;&nbsp;' +
                            '<a id="unselect" style="cursor:pointer" onclick="downSlider(\'maxOpacity\',0.1,2,0);">&#10094;</a>&nbsp;&nbsp;&nbsp;' +
                            '<input id="slidermaxOpacity" type="range" min="0" max="1" value="0.8" step="0.01" onchange="setOption(\'maxOpacity\',this.value,2);">' +
                            '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a id="unselect" style="cursor:pointer" onclick="upSlider(\'maxOpacity\',0.01,2,0.8);">&#10095;</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
                            '<span id="rangemaxOpacity">0.80</span>';
                    // scale radius
                    div.innerHTML +=
                            '<br>Scale Radius: ' +
                            '<input id="checkBoxscaleRadius" type="checkbox" name="scaleRadius" value="true" checked onclick="setOption(\'scaleRadius\',this.checked);">';
                    // use local extrema
                    div.innerHTML +=
                            '<br>Use Local Extrema: ' +
                            '<input id="checkBoxuseLocalExtrema" type="checkbox" name="useLocalExtrema" value="false" onclick="setOption(\'useLocalExtrema\',this.checked);">';
                    // view flows heatmap
                    div.innerHTML +=
                            //'<i style="background: ' + color_urls[i] + '"></i> ' +
                            '<br>Flows Heatmap: ' +
                            '<input id="checkBoxFlowsHeatmap" type="checkbox" name="FlowsHeatmap" value="false" checked onclick="toggleFlowsHeatmap(this.checked);">';
                    // select heatmap hour
                    div.innerHTML +=
                    '<div class="title">Hour</div>' +
                    '<input type="radio" name="hour" value="00" onclick="redraw()">00&nbsp;' +
                    '<input type="radio" name="hour" value="01" onclick="redraw()">01&nbsp;' +
                    '<input type="radio" name="hour" value="02" onclick="redraw()">02&nbsp;' +
                    '<input type="radio" name="hour" value="03" onclick="redraw()">03&nbsp;' +
                    '<input type="radio" name="hour" value="04" onclick="redraw()">04&nbsp;' +
                    '<input type="radio" name="hour" value="05" onclick="redraw()">05&nbsp;<br>' +
                    '<input type="radio" name="hour" value="06" onclick="redraw()">06&nbsp;' +
                    '<input type="radio" name="hour" value="07" onclick="redraw()">07&nbsp;' +
                    '<input type="radio" name="hour" value="08" onclick="redraw()">08&nbsp;' +
                    '<input type="radio" name="hour" value="09" onclick="redraw()">09&nbsp;' +
                    '<input type="radio" name="hour" value="10" onclick="redraw()">10&nbsp;' +
                    '<input type="radio" name="hour" value="11" onclick="redraw()">11&nbsp;<br>' +
                    '<input type="radio" name="hour" value="12" onclick="redraw()">12&nbsp;' +
                    '<input type="radio" name="hour" value="13" onclick="redraw()">13&nbsp;' +
                    '<input type="radio" name="hour" value="14" onclick="redraw()">14&nbsp;' +
                    '<input type="radio" name="hour" value="15" onclick="redraw()">15&nbsp;' +
                    '<input type="radio" name="hour" value="16" onclick="redraw()">16&nbsp;' +
                    '<input type="radio" name="hour" value="17" onclick="redraw()">17&nbsp;<br>' +
                    '<input type="radio" name="hour" value="18" onclick="redraw()">18&nbsp;' +
                    '<input type="radio" name="hour" value="19" onclick="redraw()">19&nbsp;' +
                    '<input type="radio" name="hour" value="20" onclick="redraw()">20&nbsp;' +
                    '<input type="radio" name="hour" value="21" onclick="redraw()">21&nbsp;' +
                    '<input type="radio" name="hour" value="22" onclick="redraw()">22&nbsp;' +
                    '<input type="radio" name="hour" value="23" onclick="redraw()">23&nbsp;' +
                    '<input type="radio" name="hour" value="" checked onclick="redraw()">All' +
                    '</div>';
                    // view clustered trajectories
                    div.innerHTML +=
                            //'<i style="background: ' + color_urls[i] + '"></i> ' +
                            '<br>Clustered trajectories (<span id="numClusteredTrajectories">' + numClusteredTrajectories + '</span>): ' +
                            '<input id="checkBoxClusteredTrajectories" type="checkbox" name="clusteredTrajectories" value="true" checked onclick="toggleClusteredTrajectories(this.checked);">' +
                            '<br>(' + String.fromCharCode(0x03B5) + ': ' + eps + ', minLns: ' + minLns + ')' +
                            '<br> ' +
                            '<a id="unselect" style="cursor:pointer" onclick="downSlider(\'clusteredTrajectories\',1,1,0);">&#10094;</a>&nbsp;&nbsp;&nbsp;' +
                            '<input id="sliderclusteredTrajectories" type="range" min="0" max="300" value="2" step="1" style="width: 250px;" onchange="filterClusteredTrajectories(\'clusteredTrajectories\',this.value);">' +
                            '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a id="unselect" style="cursor:pointer" onclick="upSlider(\'clusteredTrajectories\',1,1,300);">&#10095;</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
                            '<span id="rangeclusteredTrajectories">2</span>';
                    // disable interaction of this div with map
                    L.DomEvent.disableClickPropagation(div);
                    /*if (!L.Browser.touch) {
                        L.DomEvent.on(div, 'mousewheel', L.DomEvent.stopPropagation);
                    } else {
                        L.DomEvent.on(div, 'click', L.DomEvent.stopPropagation);
                    }*/
                    return div;
                };
                // add legend to map
                legend.addTo(map);
                //map.fitBounds(coordinatesLine.getBounds());
                //map.setView([57.505, -0.01], 13);
                toggleClusteredTrajectories($("#checkBoxClusteredTrajectories").is(":checked"));
            </script>
        </div> <!-- div container -->
    </body>
</html>
