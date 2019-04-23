<?php

if ($_REQUEST["json"]) {
    $dummy[] = array(1, 1);
    echo json_encode($dummy);
    exit();
}
include("connection.php");

$query = "SELECT UNIX_TIMESTAMP(date) * 1000 AS timestamp, IF(SUM(" . $_REQUEST["field"] . ") IS NOT NULL, SUM(" . $_REQUEST["field"] . "), 0) AS " . $_REQUEST["field"] . " FROM " . $_REQUEST["db"] . "." . $_REQUEST["table"] . " WHERE username = '" . $_REQUEST["username"] . "' GROUP BY date ORDER BY date"; // . " WHERE date >= CURDATE() - INTERVAL " . $_REQUEST["days"] . " DAY ";
$result = mysqli_query($connection, $query);

while ($row = mysqli_fetch_assoc($result)) {
    if ($row[$_REQUEST["field"]] != null && $row[$_REQUEST["field"]] > 0) {
        $data[] = array(intval($row["timestamp"]), doubleval($row[$_REQUEST["field"]]));
    }
}

mysqli_close($connection);

echo json_encode($data);
?>