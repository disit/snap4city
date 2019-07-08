<?php
function getHeatmap($org, $hour, $clusterSize) {
	$nodes = array();
        $csv = array();
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
	return "[". $heatmapdata . "]";
}
echo getHeatmap($_REQUEST["org"], $_REQUEST["hour"], $_REQUEST["clusterSize"]);
?>

