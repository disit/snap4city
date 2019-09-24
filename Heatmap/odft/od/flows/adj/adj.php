<?php
$parameters = array();
$parameters_s = "";
if(isset($_REQUEST["org"])) {
$parameters[] = "org=" . $_REQUEST["org"];
}
if (isset($_REQUEST["hour"])) {
    $parameters[] = "hour=" . $_REQUEST["hour"];
}
if (isset($_REQUEST["radius"])) {
    $parameters[] = "radius=" . $_REQUEST["radius"];
}
if (isset($_REQUEST["cluster"])) {
    $parameters[] = "cluster=" . $_REQUEST["cluster"];
}
for ($i = 0; $i < count($parameters); $i++) {
    if ($i == 0) {
        $parameters_s.="?" . $parameters[$i];
    } else {
        $parameters_s.="&" . $parameters[$i];
    }
}

function getFilename() {
    $geojson = "";
    if (isset($_REQUEST["hour"]))
        $geojson .= "_" . $_REQUEST["hour"];
    if (isset($_REQUEST["cluster"]))
        $geojson .= "_" . $_REQUEST["cluster"];
    return $geojson;
}

/* $clusters = array();
  $geojson = file_get_contents("../permutations/nodes" . getFilename() . ".geojson");
  $geojson = json_decode($geojson, true);
  foreach ($geojson["features"] as $k => $v) {
  $clusters[$v["id"]]["lat"] = doubleval($v["properties"]["LAT"]);
  $clusters[$v["id"]]["lon"] = doubleval($v["properties"]["LON"]);
  } */
?>
<!DOCTYPE html>
<html class="ocks-org do-not-copy">
    <header>
        <meta charset="utf-8">
        <title><?php
            if (isset($_REQUEST["title"])) {
                echo $_REQUEST["title"];
            } else {
                echo "Recommender";
            }
            ?>
        </title>
        <link rel="stylesheet" type="text/css" href="../../css/adj.css" />
        <!-- use leaflet 0.7.5 css with leaflet 1.0 js to make work the leaflet.curve.js plugin and the legend -->
        <!--<link rel="stylesheet" href="http://cdn.leafletjs.com/leaflet-0.7.5/leaflet.css" />-->
        <!--<link rel="stylesheet" type="text/css" href="../../css/leaflet.css" />-->
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.3.1/leaflet.css" />
        <style>

            @import url(../../css/style1.css?aea6f0a);

            .background {
                fill: #eee;
            }

            line {
                stroke: #fff;
            }

            .row text.active {
                fill: darkred;
                font-weight: bold;
                font-size: 11px;
            }

            .column text.active {
                fill: darkblue;
                font-weight: bold;
                font-size: 11px;
            }

            .row text {
                font-size: 11px;
            }

            .column text {
                font-size: 11px;
            }

        </style>
        <!--<script src="http://d3js.org/d3.v2.min.js?2.8.1"></script>-->
        <script type="text/javascript" src="../../javascript/d3.v2.min.js"></script>
        <script type="text/javascript" src="../../javascript/jquery-2.1.0.min.js"></script>
        <script src="../../javascript/jquery.csv.js"></script>
        <!--<script src="../../javascript/maps/leaflet-1.0.js"></script>-->
        <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.3.1/leaflet.js"></script>
        <!-- leaflet curve plugin https://github.com/elfalem/Leaflet.curve -->
        <script src="../../javascript/maps/leaflet.curve.js"></script>

    </header>
    <body>
        <?php include_once "header.php"; //include header?>
        <div class="container"> <!-- container -->
            <div class="rows"> <!-- container -->
                <aside style="margin-top:80px;">
                    <p>Order: <select id="order">
                            <option value="name">by Name</option>
                            <option value="count">by Frequency</option>
                            <option value="group">by Cluster</option>
                        </select>

                </aside>

                <script>
                    // load geoJSON
                    var geoJSON = [];
                    var csv = [];
                    var links = [];
                    var clusterSize = <?php echo $_REQUEST["cluster"]; ?>;
                    $.getJSON("<?php echo "../".$_REQUEST["org"]."/nodes" . getFilename() . ".geojson"; ?>", function (data) {
                    }).done(function (data) {
                        for (var i = 0; i < data["features"].length; i++) {
                            if (!geoJSON[data["features"][i]["id"]]) {
                                geoJSON[data["features"][i]["id"]] = {};
                            }
                            geoJSON[data["features"][i]["id"]]["lat"] = parseFloat(data["features"][i]["properties"]["LAT"]);
                            geoJSON[data["features"][i]["id"]]["lon"] = parseFloat(data["features"][i]["properties"]["LON"]);
                        }
                        getMatrix();
                    });

                    // load links
                    /*function getCSV(csv) {
                     $.ajax({
                     url: ">",
                     success: function (csvd) {
                     csv = $.csv.toArrays(csvd);
                     },
                     dataType: "text",
                     complete: function () {
                     // skip header (i = 0)
                     for (var i = 1; i < csv.length; i++) {
                     if (!links[csv[i][0]]) {
                     links[csv[i][0]] = {};
                     }
                     links[csv[i][0]][csv[i][1]] = parseInt(csv[i][0]);
                     }
                     getMatrix();
                     }
                     });
                     }*/

                    // get cluster square coordinates (decimal latitude and longitude) 
                    // ([lat_center, lon_center], [lat_top_right, lon_top_right], [lat_top_left, lon_top_left], [lat_bottom_left, lon_bottom_left], [lat_bottom_right, lon_bottom_right] from coordinates
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

                    // get bezier curve joining two coordinates (lat, lon)
                    function getBezier(lat1, lon1, lat2, lon2, steps, angle, weight) {
                        var bezierPath = getBezierPath(lat1, lon1, lat2, lon2, steps, angle);
                        var color = "darkblue";//flow_direction == "outflow" ? "darkblue" : "darkred";
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

                    // build a bezier path with steps and angle from decimal coordinates (lat1, lon1, lat2, lon2), javascript version of https://gist.github.com/Reflejo/f5addfa6408d521a971f
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

                    // javascript version of https://gist.github.com/Reflejo/f5addfa6408d521a971f
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

                    // load OD matrix
                    function getMatrix() {
                        var margin = {top: 80, right: 0, bottom: 10, left: 80},
                        width = 1024,
                                height = 1024;

                        var x = d3.scale.ordinal().rangeBands([0, width]),
                                //z = d3.scale.linear().domain([0, 100]).clamp(true),
                                c = d3.scale.category10().domain(d3.range(10));

                        var svg = d3.select("#od").append("svg")
                                .attr("width", width + margin.left + margin.right)
                                .attr("height", height + margin.top + margin.bottom)
                                .style("margin-left", -margin.left + "px")
                                .append("g")
                                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

                        d3.json("./adj_matrix.php<?php echo $parameters_s; ?>", function (miserables) {
                            var matrix = [],
                                    nodes = miserables.nodes,
                                    n = nodes.length;
                            // Compute index per node.
                            nodes.forEach(function (node, i) {
                                node.index = i;
                                node.count = 0;
                                matrix[i] = d3.range(n).map(function (j) {
                                    return {x: j, y: i, z: 0};
                                });
                            });
                            // Convert links to matrix; count character occurrences.
                            miserables.links.forEach(function (link) {
                                matrix[link.source][link.target].z = link.value;
                                //matrix[link.target][link.source].z += link.value;
                                //matrix[link.source][link.source].z += link.value;
                                //matrix[link.target][link.target].z += link.value;
                                nodes[link.source].count += link.value;
                                nodes[link.target].count += link.value;
                            });
                            // Precompute the orders.
                            var orders = {
                                name: d3.range(n).sort(function (a, b) {
                                    //return d3.ascending(nodes[a].name, nodes[b].name);
                                    var lat_lon = geoJSON[nodes[a].name];
                                    var name_a = "[" + (lat_lon["lat"]).toFixed(2) + ", " + (lat_lon["lon"]).toFixed(2) + "]";
                                    lat_lon = geoJSON[nodes[b].name];
                                    var name_b = (lat_lon["lat"]).toFixed(2) + " " + (lat_lon["lon"]).toFixed(2);
                                    return d3.ascending(name_a, name_b);
                                }),
                                count: d3.range(n).sort(function (a, b) {
                                    return nodes[b].count - nodes[a].count;
                                }),
                                group: d3.range(n).sort(function (a, b) {
                                    return nodes[b].group - nodes[a].group;
                                })
                            };
                            // The default sort order.
                            x.domain(orders.name);

                            z = d3.scale.linear().domain([0, d3.max(matrix, function (a) {
                                    return d3.max(a, function (n) {
                                        return n.z
                                    });
                                })]);//.clamp(true);

                            svg.append("rect")
                                    .attr("class", "background")
                                    .attr("width", width)
                                    .attr("height", height);
                            var row = svg.selectAll(".row")
                                    .data(matrix)
                                    .enter().append("g")
                                    .attr("class", "row")
                                    .attr("transform", function (d, i) {
                                        return "translate(0," + x(i) + ")";
                                    })
                                    .each(row);
                            row.append("line")
                                    .attr("x2", width);
                            row.append("text")
                                    .attr("x", -6)
                                    .attr("y", x.rangeBand() / 2)
                                    .attr("dy", ".32em")
                                    .attr("text-anchor", "end")
                                    .text(function (d, i) {
                                        //return nodes[i].name;
                                        var lat_lon = geoJSON[nodes[i].name];
                                        return "[" + (lat_lon["lat"]).toFixed(2) + ", " + (lat_lon["lon"]).toFixed(2) + "]";
                                    });
                            var column = svg.selectAll(".column")
                                    .data(matrix)
                                    .enter().append("g")
                                    .attr("class", "column")
                                    .attr("transform", function (d, i) {
                                        return "translate(" + x(i) + ")rotate(-90)";
                                    });
                            column.append("line")
                                    .attr("x1", -width);
                            column.append("text")
                                    .attr("x", 6)
                                    .attr("y", x.rangeBand() / 2)
                                    .attr("dy", ".32em")
                                    .attr("text-anchor", "start")
                                    .text(function (d, i) {
                                        //return nodes[i].name;
                                        var lat_lon = geoJSON[nodes[i].name];
                                        return "[" + (lat_lon["lat"]).toFixed(2) + ", " + (lat_lon["lon"]).toFixed(2) + "]";
                                    });
                            function row(row) {
                                var cell = d3.select(this).selectAll(".cell")
                                        .data(row.filter(function (d) {
                                            return d.z;
                                        }))
                                        .enter().append("rect")
                                        .attr("class", "cell")
                                        .attr("x", function (d) {
                                            return x(d.x);
                                        })
                                        .attr("width", x.rangeBand())
                                        .attr("height", x.rangeBand())
                                        .style("fill-opacity", function (d) {
                                            return z(d.z);
                                        })
                                        .style("fill", function (d) {
                                            //return nodes[d.x].group == nodes[d.y].group ? c(nodes[d.x].group) : null;
                                            return null;
                                        })
                                        .on("mouseover", mouseover)
                                        .on("mouseout", mouseout)
                                        .on("click", click);
                            }

                            function mouseover(p) {
                                d3.selectAll(".row text").classed("active", function (d, i) {
                                    return i == p.y;
                                });
                                d3.selectAll(".column text").classed("active", function (d, i) {
                                    return i == p.x;
                                });
                            }

                            function mouseout() {
                                d3.selectAll("text").classed("active", false);
                            }

                            function click(p) {
                                // set span with flow value
                                $("#flow").html("<div class='outflow'><b>Target:</b> [" + geoJSON[nodes[p.x].name]["lat"] + ", " + geoJSON[nodes[p.x].name]["lon"] + "]</div><br><div class='inflow'><b>Source:</b> [" + geoJSON[nodes[p.y].name]["lat"] + ", " + geoJSON[nodes[p.y].name]["lon"] + "]</div><br><b>Flow:</b> " + p.z);

                                // remove the old layer
                                // remove every layer
                                map.eachLayer(function (layer) {
                                    if (layer != baseLayer) {
                                        map.removeLayer(layer);
                                    }
                                });

                                var target = [parseFloat(geoJSON[nodes[p.x].name]["lat"]), parseFloat(geoJSON[nodes[p.x].name]["lon"])];
                                var source = [parseFloat(geoJSON[nodes[p.y].name]["lat"]), parseFloat(geoJSON[nodes[p.y].name]["lon"])];

                                var gridLayer = L.layerGroup([]);
                                // get cluster square for target (darkblue)
                                var cluster_square_target = getClusterSquare(target[0], target[1], clusterSize);
                                // add cluster square to layer
                                gridLayer.addLayer(L.rectangle([[cluster_square_target[3], cluster_square_target[5]], [cluster_square_target[2], cluster_square_target[4]]], {color: 'darkblue', fill: false, weight: 3}));
                                // get cluster square for source (darkred)
                                var cluster_square_source = getClusterSquare(source[0], source[1], clusterSize);
                                // add cluster square to layer
                                gridLayer.addLayer(L.rectangle([[cluster_square_source[3], cluster_square_source[5]], [cluster_square_source[2], cluster_square_source[4]]], {color: 'darkred', fill: false, weight: 3}));

                                // add clusters to map
                                map.addLayer(gridLayer);

                                // get bezier connecting the clusters
                                var bezier = getBezier(source[0], source[1], target[0], target[1], 10, 120, 3/*Math.max(1, Math.min(Math.log(flow) / Math.log(1.8), 5))*/);

                                // add bezier to map
                                map.addLayer(bezier);

                                // set map view
                                map.fitBounds([
                                    [
                                        Math.min(cluster_square_target[3], cluster_square_source[3]),
                                        Math.min(cluster_square_target[5], cluster_square_source[5])
                                    ],
                                    [
                                        Math.max(cluster_square_source[2], cluster_square_target[2]),
                                        Math.max(cluster_square_source[4], cluster_square_target[4])
                                    ]
                                ], {padding: [30, 30]});
                            }

                            d3.select("#order").on("change", function () {
                                clearTimeout(timeout);
                                order(this.value);
                            });
                            function order(value) {
                                x.domain(orders[value]);
                                var t = svg.transition().duration(2500);
                                t.selectAll(".row")
                                        .delay(function (d, i) {
                                            return x(i) * 4;
                                        })
                                        .attr("transform", function (d, i) {
                                            return "translate(0," + x(i) + ")";
                                        })
                                        .selectAll(".cell")
                                        .delay(function (d) {
                                            return x(d.x) * 4;
                                        })
                                        .attr("x", function (d) {
                                            return x(d.x);
                                        });
                                t.selectAll(".column")
                                        .delay(function (d, i) {
                                            return x(i) * 4;
                                        })
                                        .attr("transform", function (d, i) {
                                            return "translate(" + x(i) + ")rotate(-90)";
                                        });
                            }

                            var timeout = setTimeout(function () {
                                order("group");
                                d3.select("#order").property("selectedIndex", 2).node().focus();
                            }, 0);
                        });
                    }
                </script>
                <div id="od" class="cell">
                    <div class="row">
                        <b><?php echo ucfirst($_REQUEST["profile"]) . " (" . (isset($_REQUEST["hour"]) ? "time: " . $_REQUEST["hour"] . ":00:00 - " . $_REQUEST["hour"] . ":59:59," : "") . " " . "cluster: " . $_REQUEST["cluster"] . (isset($_REQUEST["radius"]) ? ", radius: " . $_REQUEST["radius"] . " km" : "") . ")"; ?> - OD Matrix</b>
                        <hr/>
                    </div>
                </div>
                <div class="cell">
                    <span id="flow"><div class='outflow'><b>Target:</b></div><br><div class='inflow'><b>Source:</b></div><br><b>Flow:</b></span>
                    <div id="map" class="cell" style="height: 1040px; width: 700px;"></div>
                </div>
                <script>
                    //setup map
                    var mbAttr = 'Map data &copy; <a class="leafletAttribution" href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
                            '<a class="leafletAttribution" href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
                            'Imagery © <a class="leafletAttribution" href="http://mapbox.com">Mapbox</a>';
                    // for satellite map use mapbox.streets-satellite in the url
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
                </script>
            </div> <!-- rows -->
        </div> <!-- container -->
    </body>
</html>
