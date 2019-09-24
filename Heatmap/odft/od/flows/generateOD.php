<?php

// calculate people flows from MySQL and write json and csv files to be used by flows/index.php
// https://github.com/geodesign/spatialsankey
// https://github.com/ilyabo/jflowmap.js
// http://nssdc.gsfc.nasa.gov/planetary/factsheet/earthfact.html
// Earth Equatorial radius (km) 6378.137
// Earth Polar radius (km) 6356.752
// Earth Volumetric mean radius (km) 6371.008
include_once "../settings.php"; // settings
ini_set('max_execution_time', 9999999); //300 seconds = 5 minutes
ini_set("memory_limit", "-1");

function getHeatmap($org, $hour, $clusterSize) {
	$nodes = array();
	$json = json_decode(file_get_contents("./flows/" . $org . "/nodes_" . $hour . ($hour != "" ? "_" : "") . $clusterSize . ".geojson"), true);
	foreach ($json["features"] as $feature) {
		$nodes[$feature["id"]] = array($feature["properties"]["LAT"], $feature["properties"]["LON"]);
	}
	// Open the file for reading
	$i = 0;
	if (($h = fopen("./flows/" . $org . "/links_" . $hour . ($hour != "" ? "_" : "") . $clusterSize . ".csv", "r")) !== FALSE) {
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
	return $heatmapdata;
}

// get people flows
function getPeopleFlows($clusterSize) {
	global $config;

	//CONNECT
	$link = mysqli_connect($config['host'], $config['user'], $config['pass'], $config['database']);

	/* check connection */
	if (mysqli_connect_errno()) {
		printf("Connection failed: %s\n", mysqli_connect_error());
		exit();
	}

	// cluster suffix
	$cluster_suffix = "_" . $clusterSize;

	$previous = array();
	// max row
	$max_row = 0;
	$links = array();
	$clusters_squares_array = array();
	$paths = array();
	//$users_profiles = getUsersProfiles();
	// load the status from file (@ suppress the warning if the file is not there)
	$status = @file_get_contents("./" . $_REQUEST["org"] . "/status" . $cluster_suffix . ".json");
	if ($status !== false) {
		$max_row_previous_links = json_decode($status, true);
		$max_row = intval($max_row_previous_links[0]);
		$previous = $max_row_previous_links[1];
		$links = $max_row_previous_links[2];
	}

	// sort the results by user desc, timestamp desc
	$sql = "SELECT a.id AS idmeasure, b.username AS user, a.latitude, a.longitude, a.data_time AS date, EXTRACT(hour FROM data_time) AS hour, EXTRACT(day FROM data_time) AS day, EXTRACT(month FROM data_time) AS month, EXTRACT(year FROM data_time) AS year FROM profiledb.kpivalues" . ($_REQUEST["test"] == "true" ? "test" : "") . " a LEFT JOIN profiledb.kpidata" . ($_REQUEST["test"] == "true" ? "test" : "") . " b ON a.kpi_id = b.id WHERE b.value_name = 'S4C" . $_REQUEST["org"] . "TrackerLocation' AND a.id > " . $max_row . " ORDER BY b.username DESC, a.data_time DESC";
	//$sql = "SELECT idmeasure, device_id AS user, latitude, longitude, date, EXTRACT(hour FROM date) AS hour, EXTRACT(day FROM date) AS day, EXTRACT(month FROM date) AS month, EXTRACT(year FROM date) AS year FROM profiledb.sensors WHERE idmeasure > " . $max_row . " ORDER BY device_id DESC, date DESC";
	$result = mysqli_query($link, $sql) or die(mysqli_error());
	while ($row = mysqli_fetch_assoc($result)) {
		$max_row = max($max_row, $row["idmeasure"]);

		$cluster = getClusterSquare($row["latitude"], $row["longitude"], $clusterSize);

		$clusters_squares_array[$cluster[0] . "," . $cluster[1]] = $cluster;

		if (isset($previous["latitude"]) && ($cluster[0] != $previous["latitude"] || $cluster[1] != $previous["longitude"]) && $row["user"] == $previous["user"]) {
			$hour = $row["hour"];
			$day = $row["day"];
			$month = $row["month"];
			$year = $row["year"];
			$previous_hour = $previous["hour"];
			$previous_day = $previous["day"];
			$previous_month = $previous["month"];
			$previous_year = $previous["year"];
			$hour_day_month_year = ($hour == $previous_hour) && ($day == $previous_day) && ($month == $previous_month) && ($year == $previous_year);

			// if it is the same hour, day, month, year than the previous one
			if ($hour_day_month_year) {
				// increment flow for all the combinations of clusters preceding this one for this user in the same day and hour range
				if (isset($previous[$row["user"]]["path"])) {
					foreach ($previous[$row["user"]]["path"] as $previous_path) {
						// if this is not the same cluster
						if ($cluster[0] . "," . $cluster[1] != $previous_path) {
							$key = $cluster[0] . "," . $cluster[1] . "|" . $previous_path;
							$links[$hour . ""][$key] = isset($links[$hour . ""][$key]) ? $links[$hour . ""][$key] + 1 : 1;
						}
					}
				}
			} else {
				if (isset($previous[$row["user"]]) && isset($previous[$row["user"]]["path"])) {
					unset($previous[$row["user"]]["path"]);
				}
			}
		}
		if (isset($previous["user"]) && $row["user"] != $previous["user"]) {
			if (isset($previous[$previous["user"]]) && isset($previous[$previous["user"]]["path"])) {
				unset($previous[$previous["user"]]["path"]);
			}
		}
		$previous["latitude"] = $cluster[0];
		$previous["longitude"] = $cluster[1];
		$previous["hour"] = $row["hour"];
		$previous["day"] = $row["day"];
		$previous["month"] = $row["month"];
		$previous["year"] = $row["year"];
		$previous["user"] = $row["user"];
		$previous[$row["user"]]["path"][] = $cluster[0] . "," . $cluster[1];
	}
	//close connection
	mysqli_close($link);

	foreach ($links as $hour => $value) {
		// sort $links by descending value
		arsort($links[$hour]);

		$clusters_array = array();
		$clusters_squares_array_filtered = array();
		$links_file = "target,source,flow\n";
		$grid = "var gridLayer_" . $hour . $cluster_suffix . " = L.layerGroup([]);\n";

		//$counter = 0;
		foreach ($links[$hour] as $k => $v) {
			// do not include values < 10
			/* if ($links[$profile][$hour][$k] < 10) {
	              break;
*/
			// populate node.geojson file
			$target_source = explode("|", $k);
			$target = explode(",", $target_source[0]);
			$source = explode(",", $target_source[1]);
			$target_geojson = getGeoJSON($target);
			$source_geojson = getGeoJSON($source);
			if (!in_array($target_geojson, $clusters_array)) {
				$clusters_array[] = $target_geojson;
			}
			if (!in_array($source_geojson, $clusters_array)) {
				$clusters_array[] = $source_geojson;
			}
			// populate clusters array filtered
			if (isset($clusters_squares_array[$target[0] . "," . $target[1]])) {
				$clusters_squares_array_filtered[$target[0] . "," . $target[1]] = $clusters_squares_array[$target[0] . "," . $target[1]];
			}
			if (isset($clusters_squares_array[$source[0] . "," . $source[1]])) {
				$clusters_squares_array_filtered[$source[0] . "," . $source[1]] = $clusters_squares_array[$source[0] . "," . $source[1]];
			}
			// populate links.csv file
			$links_file .= sha1($target[0] . $target[1]) . "," . sha1($source[0] . $source[1]) . "," . $links[$hour][$k] . "\n";
		}

		// populate grid javascript
		foreach ($clusters_squares_array_filtered as $v) {
			$lat_center = $v[0];
			$lon_center = $v[1];
			$lat_right = $v[2];
			$lat_left = $v[3];
			$lon_top = $v[4];
			$lon_bottom = $v[5];
			$grid .= "gridLayer_" . $hour . $cluster_suffix . ".addLayer(L.rectangle([[" . $v[2] . "," . $v[4] . "],[" . $v[3] . "," . $v[5] . "]], {color:'green',fill:false,weight:1}));";
		}

		// write node.geojson file
		$geoJSON = ["type" => "FeatureCollection", "features" => $clusters_array];
		file_put_contents("./" . $_REQUEST["org"] . "/nodes_" . sprintf("%02d", $hour) . $cluster_suffix . ".geojson", json_encode($geoJSON/* , JSON_PRETTY_PRINT */));
		// write links.csv file
		file_put_contents("./" . $_REQUEST["org"] . "/links_" . sprintf("%02d", $hour) . $cluster_suffix . ".csv", $links_file);
		// write grid.js file
		file_put_contents("../javascript/" . $_REQUEST["org"] . "/grid_" . sprintf("%02d", $hour) . $cluster_suffix . ".js", $grid);
	}
	// save the status to file
	file_put_contents("./" . $_REQUEST["org"] . "/status" . $cluster_suffix . ".json", json_encode(array($max_row, $previous, $links)));

	// save total people flows
	//getTotalPeopleFlows($clusterSize);
}

// get total people flows
function getTotalPeopleFlows($clusterSize) {
	$hours = ["00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"];
	$id_lat_lon = array();
	$links = array();
	$links_profile = array();
	$links_hour = array();
	$clusters_squares_array = array();
	$cluster_suffix = "_" . $clusterSize;
	foreach ($hours as $hour) {
		if (!file_exists("./" . $_REQUEST["org"] . "/links_" . $hour . $cluster_suffix . ".csv")) {
			continue;
		}
		$csv = array_map('str_getcsv', file("./" . $_REQUEST["org"] . "/links_" . $hour . $cluster_suffix . ".csv"));
		// populate array with id => coordinates
		$geojson = json_decode(file_get_contents("./" . $_REQUEST["org"] . "/nodes_" . $hour . $cluster_suffix . ".geojson"), true);
		foreach ($geojson["features"] as $feature) {
			$id_lat_lon[$feature["id"]] = $feature["properties"]["LAT"] . " " . $feature["properties"]["LON"];
		}
		// skip csv header
		for ($i = 1; $i < count($csv); $i++) {
			$target_lat_lon = explode(" ", $id_lat_lon[$csv[$i][0]]);
			$source_lat_lon = explode(" ", $id_lat_lon[$csv[$i][1]]);

			$cluster = getClusterSquare($target_lat_lon[0], $target_lat_lon[1], $clusterSize);
			$clusters_squares_array[$target_lat_lon[0] . "," . $target_lat_lon[1]] = $cluster;
			$cluster = getClusterSquare($source_lat_lon[0], $source_lat_lon[1], $clusterSize);
			$clusters_squares_array[$source_lat_lon[0] . "," . $source_lat_lon[1]] = $cluster;

			$key = $target_lat_lon[0] . "," . $target_lat_lon[1] . "|" . $source_lat_lon[0] . "," . $source_lat_lon[1];
			$links[$key] = isset($links[$key]) ? $links[$key] + intval($csv[$i][2]) : intval($csv[$i][2]);
			//$links_profile[$profile][$key] = isset($links_profile[$profile][$key]) ? $links_profile[$profile][$key] + intval($csv[$i][2]) : intval($csv[$i][2]);
			$links_hour[$hour][$key] = isset($links_hour[$hour][$key]) ? $links_hour[$hour][$key] + intval($csv[$i][2]) : intval($csv[$i][2]);
		}
	}

	// write total links
	// sort $links by descending value
	arsort($links);

	$clusters_array = array();
	$clusters_squares_array_filtered = array();
	$links_file = "target,source,flow\n";
	$grid = "var gridLayer" . $cluster_suffix . " = L.layerGroup([]);\n";

	//$counter = 0;
	foreach ($links as $k => $v) {
		// do not include values < 10
		/*if ($links[$k] < 10) {
			break;
		}*/
		// populate node.geojson file
		$target_source = explode("|", $k);
		$target = explode(",", $target_source[0]);
		$source = explode(",", $target_source[1]);
		$target_geojson = getGeoJSON($target);
		$source_geojson = getGeoJSON($source);
		if (!in_array($target_geojson, $clusters_array)) {
			$clusters_array[] = $target_geojson;
		}
		if (!in_array($source_geojson, $clusters_array)) {
			$clusters_array[] = $source_geojson;
		}
		// populate clusters array filtered
		if (isset($clusters_squares_array[$target[0] . "," . $target[1]])) {
			$clusters_squares_array_filtered[$target[0] . "," . $target[1]] = $clusters_squares_array[$target[0] . "," . $target[1]];
		}
		if (isset($clusters_squares_array[$source[0] . "," . $source[1]])) {
			$clusters_squares_array_filtered[$source[0] . "," . $source[1]] = $clusters_squares_array[$source[0] . "," . $source[1]];
		}
		// populate links.csv file
		$links_file .= sha1($target[0] . $target[1]) . "," . sha1($source[0] . $source[1]) . "," . $links[$k] . "\n";
	}

	// populate grid javascript
	foreach ($clusters_squares_array_filtered as $v) {
		$lat_center = $v[0];
		$lon_center = $v[1];
		$lat_right = $v[2];
		$lat_left = $v[3];
		$lon_top = $v[4];
		$lon_bottom = $v[5];
		$grid .= "gridLayer" . $cluster_suffix . ".addLayer(L.rectangle([[" . $v[2] . "," . $v[4] . "],[" . $v[3] . "," . $v[5] . "]], {color:'green',fill:false,weight:1}));";
	}

	// write node.geojson file
	$geoJSON = ["type" => "FeatureCollection", "features" => $clusters_array];
	file_put_contents("./" . $_REQUEST["org"] . "/nodes" . $cluster_suffix . ".geojson", json_encode($geoJSON/* , JSON_PRETTY_PRINT */));
	// write links.csv file
	file_put_contents("./" . $_REQUEST["org"] . "/links" . $cluster_suffix . ".csv", $links_file);
	// write grid.js file
	file_put_contents("../javascript/" . $_REQUEST["org"] . "/grid" . $cluster_suffix . ".js", $grid);

	// write profile links
	/*foreach ($profiles as $profile) {
	        if (isset($links_profile[$profile])) {
	            // sort $links by descending value
	            arsort($links_profile[$profile]);

	            $clusters_array = array();
	            $clusters_squares_array_filtered = array();
	            $links_file = "target,source,flow\n";
	            $grid = "var gridLayer_" . $profile . $cluster_suffix . " = L.layerGroup([]);\n";

	            //$counter = 0;
	            foreach ($links_profile[$profile] as $k => $v) {
	                // do not include values < 10
	                if ($links_profile[$profile][$k] < 10) {
	                    break;
	                }
	                // populate node.geojson file
	                $target_source = split("\|", $k);
	                $target = split(",", $target_source[0]);
	                $source = split(",", $target_source[1]);
	                $target_geojson = getGeoJSON($target);
	                $source_geojson = getGeoJSON($source);
	                if (!in_array($target_geojson, $clusters_array)) {
	                    $clusters_array[] = $target_geojson;
	                }
	                if (!in_array($source_geojson, $clusters_array)) {
	                    $clusters_array[] = $source_geojson;
	                }
	                // populate clusters array filtered
	                if (isset($clusters_squares_array[$target[0] . "," . $target[1]])) {
	                    $clusters_squares_array_filtered[$target[0] . "," . $target[1]] = $clusters_squares_array[$target[0] . "," . $target[1]];
	                }
	                if (isset($clusters_squares_array[$source[0] . "," . $source[1]])) {
	                    $clusters_squares_array_filtered[$source[0] . "," . $source[1]] = $clusters_squares_array[$source[0] . "," . $source[1]];
	                }
	                // populate links.csv file
	                $links_file .= sha1($target[0] . $target[1]) . "," . sha1($source[0] . $source[1]) . "," . $links_profile[$profile][$k] . "\n";
	            }

	            // populate grid javascript
	            foreach ($clusters_squares_array_filtered as $v) {
	                $lat_center = $v[0];
	                $lon_center = $v[1];
	                $lat_right = $v[2];
	                $lat_left = $v[3];
	                $lon_top = $v[4];
	                $lon_bottom = $v[5];
	                $grid .= "gridLayer_" . $profile . $cluster_suffix . ".addLayer(L.rectangle([[" . $v[2] . "," . $v[4] . "],[" . $v[3] . "," . $v[5] . "]], {color:'green',fill:false,weight:1}));";
	            }

	            // write node.geojson file
	            $geoJSON = ["type" => "FeatureCollection", "features" => $clusters_array];
	            file_put_contents("./" . $_REQUEST["org"] . "/nodes_" . $profile . $cluster_suffix . ".geojson", json_encode($geoJSON , JSON_PRETTY_PRINT));
	            // write links.csv file
	            file_put_contents("./" . $_REQUEST["org"] . "/links_" . $profile . $cluster_suffix . ".csv", $links_file);
	            // write grid.js file
	            file_put_contents("../javascript/" . $_REQUEST["org"] . "/grid_" . $profile . $cluster_suffix . ".js", $grid);
	        }
*/

	// write hour links
	foreach ($hours as $hour) {
		if (isset($links_hour[$hour])) {
			// sort $links by descending value
			arsort($links_hour[$hour]);

			$clusters_array = array();
			$clusters_squares_array_filtered = array();
			$links_file = "target,source,flow\n";
			$grid = "var gridLayer_" . $hour . $cluster_suffix . " = L.layerGroup([]);\n";

			//$counter = 0;
			foreach ($links_hour[$hour] as $k => $v) {
				// do not include values < 10
				/*if ($links_hour[$hour][$k] < 10) {
					break;
				}*/
				// populate node.geojson file
				$target_source = explode("|", $k);
				$target = explode(",", $target_source[0]);
				$source = explode(",", $target_source[1]);
				$target_geojson = getGeoJSON($target);
				$source_geojson = getGeoJSON($source);
				if (!in_array($target_geojson, $clusters_array)) {
					$clusters_array[] = $target_geojson;
				}
				if (!in_array($source_geojson, $clusters_array)) {
					$clusters_array[] = $source_geojson;
				}
				// populate clusters array filtered
				if (isset($clusters_squares_array[$target[0] . "," . $target[1]])) {
					$clusters_squares_array_filtered[$target[0] . "," . $target[1]] = $clusters_squares_array[$target[0] . "," . $target[1]];
				}
				if (isset($clusters_squares_array[$source[0] . "," . $source[1]])) {
					$clusters_squares_array_filtered[$source[0] . "," . $source[1]] = $clusters_squares_array[$source[0] . "," . $source[1]];
				}
				// populate links.csv file
				$links_file .= sha1($target[0] . $target[1]) . "," . sha1($source[0] . $source[1]) . "," . $links_hour[$hour][$k] . "\n";
			}

			// populate grid javascript
			foreach ($clusters_squares_array_filtered as $v) {
				$lat_center = $v[0];
				$lon_center = $v[1];
				$lat_right = $v[2];
				$lat_left = $v[3];
				$lon_top = $v[4];
				$lon_bottom = $v[5];
				$grid .= "gridLayer_" . $hour . $cluster_suffix . ".addLayer(L.rectangle([[" . $v[2] . "," . $v[4] . "],[" . $v[3] . "," . $v[5] . "]], {color:'green',fill:false,weight:1}));";
			}

			// write node.geojson file
			$geoJSON = ["type" => "FeatureCollection", "features" => $clusters_array];
			file_put_contents("./" . $_REQUEST["org"] . "/nodes_" . $hour . $cluster_suffix . ".geojson", json_encode($geoJSON/* , JSON_PRETTY_PRINT */));
			// write links.csv file
			file_put_contents("./" . $_REQUEST["org"] . "/links_" . $hour . $cluster_suffix . ".csv", $links_file);
			// write grid.js file
			file_put_contents("../javascript/" . $_REQUEST["org"] . "/grid_" . $hour . $cluster_suffix . ".js", $grid);
		}
	}
}

function getMaxRow($type) {
	global $config;
	$max_row = 0;
	$client = new MongoClient($config["mongodb_flows_url"]);
	$collection = $client->data->collection;
	$cursor = $collection->find(["type" => $type], ["row" => -1, " _id" => 0])->sort(["row" => -1])->limit(1);
	foreach ($cursor as $v) {
		$max_row = $v["row"];
	}
	$client->close();
	return $v["row"];
}

// get cluster coordinates (decimal latitude and longitude) from coordinates
function getCluster($latitude, $longitude, $clusterSize) {
	$lat_cluster = round(6371000 * log(tan(pi() / 4 + $latitude / 180 * pi() / 2)) / $clusterSize) * $clusterSize;
	$lon_cluster = round($longitude / 180 * pi() * 6371000 / $clusterSize) * $clusterSize;
	$lat = (2 * atan(exp($lat_cluster / 6371000)) - pi() / 2) * 180 / pi();
	$lon = $lon_cluster / 6371000 * 180 / pi();
	return array($lat, $lon);
}

// get cluster square coordinates (decimal latitude and longitude)
// ([lat_center, lon_center], [lat_top_right, lon_top_right], [lat_top_left, lon_top_left], [lat_bottom_left, lon_bottom_left], [lat_bottom_right, lon_bottom_right] from coordinates
function getClusterSquare($latitude, $longitude, $clusterSize) {
	$lat_cluster = round(6371000 * log(tan(pi() / 4 + $latitude / 180 * pi() / 2)) / $clusterSize) * $clusterSize;
	$lon_cluster = round($longitude / 180 * pi() * 6371000 / $clusterSize) * $clusterSize;

	$lat_center = (2 * atan(exp($lat_cluster / 6371000)) - pi() / 2) * 180 / pi();
	$lon_center = $lon_cluster / 6371000 * 180 / pi();
	$lat_right = (2 * atan(exp(($lat_cluster + $clusterSize / 2) / 6371000)) - pi() / 2) * 180 / pi();
	$lat_left = (2 * atan(exp(($lat_cluster - $clusterSize / 2) / 6371000)) - pi() / 2) * 180 / pi();
	$lon_top = ($lon_cluster + $clusterSize / 2) / 6371000 * 180 / pi();
	$lon_bottom = ($lon_cluster - $clusterSize / 2) / 6371000 * 180 / pi();

	return array($lat_center, $lon_center, $lat_right, $lat_left, $lon_top, $lon_bottom);
}

// get GeoJSON object for coordinates
function getGeoJSON($cluster) {
	return [
		"type" => "Feature",
		"id" => sha1($cluster[0] . $cluster[1]),
		"properties" => ["LAT" => doubleval($cluster[0]), "LON" => doubleVal($cluster[1])],
		"geometry" => [
			"type" => "Point",
			"coordinates" => [doubleval($cluster[1]), doubleval($cluster[0])],
		],
	];
}

// http://php.net/manual/en/function.json-last-error.php
function getJSONError($string) {
	json_decode($string);

	switch (json_last_error()) {
	case JSON_ERROR_NONE:
		echo ' - No errors';
		break;
	case JSON_ERROR_DEPTH:
		echo ' - Maximum stack depth exceeded';
		break;
	case JSON_ERROR_STATE_MISMATCH:
		echo ' - Underflow or the modes mismatch';
		break;
	case JSON_ERROR_CTRL_CHAR:
		echo ' - Unexpected control character found';
		break;
	case JSON_ERROR_SYNTAX:
		echo ' - Syntax error, malformed JSON';
		break;
	case JSON_ERROR_UTF8:
		echo ' - Malformed UTF-8 characters, possibly incorrectly encoded';
		break;
	default:
		echo ' - Unknown error';
		break;
	}
}

// calculate the area of a polygon of decimal coordinates ($lat1, $lon1, $lat2, $lon2) in m^2 http://mathforum.org/library/drmath/view/63767.html
function calculatePolygonArea($lat1, $lat2, $lon1, $lon2) {
	//return 2 * pi() * pow(6371000, 2) * abs(sin(deg2rad($lat1)) - sin(deg2rad($lat2))) * abs($lon1 - $lon2) / 360;
	return (pi() / 180) * pow(6371000, 2) * abs(sin(deg2rad($lat1)) - sin(deg2rad($lat2))) * abs($lon1 - $lon2);
}

// calculate the area of a cluster from its center and cluster size ($lat, $lon, $clusterSize) in m^2
function calculateClusterArea($lat, $lon, $clusterSize) {
	$cluster = getClusterSquare($lat, $lon, $clusterSize);
	$lat_center = $cluster[0];
	$lon_center = $cluster[1];
	$lat_right = $cluster[2];
	$lat_left = $cluster[3];
	$lon_top = $cluster[4];
	$lon_bottom = $cluster[5];
	return calculatePolygonArea($lat_left, $lat_right, $lon_top, $lon_bottom);
}

// calculate the distance in km between coordinates in decimal degrees (latitude, longitude)
function distFrom($lat1, $lng1, $lat2, $lng2) {
	if (($lat2 == 0 && $lng2 == 0) || ($lat1 == $lat2 && $lng1 == $lng2)) {
		return 0;
	}
	$earthRadius = 6371000; // meters
	$dLat = deg2rad($lat2 - $lat1);
	$dLng = deg2rad($lng2 - $lng1);
	$a = sin($dLat / 2) * sin($dLat / 2) + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * sin($dLng / 2) * sin($dLng / 2);
	$c = 2 * atan2(sqrt($a), sqrt(1 - $a));
	$dist = $earthRadius * $c;

	return $dist / 1000;
}

/* arr[]  ---> Input Array
data[] ---> Temporary array to store current combination
start & end ---> Staring and Ending indexes in arr[]
index  ---> Current index in data[]
r ---> Size of a combination to be printed */

function combinationUtil($arr, $data, $start, $end, $index, $r) {
	// Current combination is ready to be printed, print it
	if ($index == $r) {
		for ($j = 0; $j < $r; $j++) {
			echo ($data[$j] . " ");
		}

		if ($j % 2 == 0) {
			echo "<br>";
		}
		return;
	}

	// replace index with all possible elements. The condition
	// "end-i+1 >= r-index" makes sure that including one element
	// at index will make a combination with remaining elements
	// at remaining positions
	for ($i = $start; $i <= $end && $end - i + 1 >= $r - $index; $i++) {
		$data[$index] = $arr[$i];
		combinationUtil($arr, $data, $i + 1, $end, $index + 1, $r);
	}
}

// The main function that prints all combinations of size r
// in arr[] of size n. This function mainly uses combinationUtil()
function printCombination($arr, $n, $r) {
	// A temporary array to store all combination one by one
	$data = array();

	// Print all combination using temprary array 'data[]'
	combinationUtil($arr, $data, 0, $n - 1, 0, $r);
}

// get users profiles map
function getUsersProfiles() {
	global $config;

	//CONNECT
	$link = mysqli_connect($config['host'], $config['user'], $config['pass'], $config['database']);

	/* check connection */
	if (mysqli_connect_errno()) {
		printf("Connection failed: %s\n", mysqli_connect_error());
		exit();
	}
	// GET DATA
	$users_profiles = array();
	$sql = "SELECT user, profile FROM recommender.users";
	$result = mysqli_query($link, $sql) or die(mysqli_error());
	while ($row = mysqli_fetch_assoc($result)) {
		$users_profiles[$row["user"]] = $row["profile"];
	}
	//close connection
	mysqli_close($link);
	return $users_profiles;
}

// generate people flows for various cluster sizes
$n = 13;
$clusterSize = 276; //1104;
for ($i = 0; $i < $n; $i++) {
	getPeopleFlows($clusterSize);
	getTotalPeopleFlows($clusterSize);
	$clusterSize *= 2;
}
?>
