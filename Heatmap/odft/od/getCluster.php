<?php

include_once "settings.php";
global $config;

//CONNECT
$link = mysqli_connect($config['host'], $config['user'], $config['pass'], $config['database']);

/* check connection */
if (mysqli_connect_errno()) {
    printf("Connection failed: %s\n", mysqli_connect_error());
    exit();
}
// GET DATA
$coordinates = array();
if ($_REQUEST["profile"] != "") {
    $sql = "SELECT cluster_size, trajectory FROM recommender.clustered_trajectories WHERE profile = '" . $_REQUEST["profile"] . "' AND cluster_id = " . $_REQUEST["cluster"];
} else {
    $sql = "SELECT cluster_size, trajectory FROM recommender.clustered_trajectories WHERE cluster_id = " . $_REQUEST["cluster"];
}
$result = mysqli_query($link, $sql) or die(mysqli_error());
$size = 0;
while ($row = mysqli_fetch_assoc($result)) {
    $trajectory = split(";", $row["trajectory"]);
    $trajectory_javascript = "";
    $size = $row["cluster_size"];
    foreach ($trajectory as $coordinates) {
        $coordinates_array = split(" ", $coordinates);
        $trajectory_javascript .= "new L.LatLng(" . $coordinates_array[0] . "," . $coordinates_array[1] . "),";
    }
}
$trajectory_javascript = "new L.Polyline([" . substr($trajectory_javascript, 0, strlen($trajectory_javascript) - 1) . "], {color: 'blue', weight: 5, opacity: 0.5, smoothFactor: 1}).on('click', function(){getTrajectory(" . $_REQUEST["cluster"] . ",'" . $_REQUEST["profile"] . "');}).bindLabel('Cluster Id: " . $_REQUEST["cluster"] . "<br># trajectories: " . $size . "');\n";
mysqli_close($link);
echo $trajectory_javascript;
?>