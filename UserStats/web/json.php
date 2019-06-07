<?php

if ($_REQUEST["json"]) {
    $dummy[] = array(1, 1);
    echo json_encode($dummy);
    exit();
}
include("connection.php");

$query = "SELECT UNIX_TIMESTAMP(date) * 1000 AS timestamp, IF(SUM(" . mysqli_real_escape_string($connection, $_REQUEST["field"]) . ") IS NOT NULL, SUM(" . mysqli_real_escape_string($connection, $_REQUEST["field"]) . "), 0) AS " . mysqli_real_escape_string($connection, $_REQUEST["field"]) . " FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " WHERE username = '" . mysqli_real_escape_string($connection, $_REQUEST["username"]) . "' GROUP BY date ORDER BY date"; // . " WHERE date >= CURDATE() - INTERVAL " . $_REQUEST["days"] . " DAY ";
$result = mysqli_query($connection, $query);

while ($row = mysqli_fetch_assoc($result)) {
    if ($row[$_REQUEST["field"]] != null && $row[$_REQUEST["field"]] > 0) {
        $data[] = array(intval($row["timestamp"]), doubleval($row[$_REQUEST["field"]]));
    }
}

mysqli_close($connection);

echo json_encode($data);
?>
