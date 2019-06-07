<?php

include("connection.php");

if ($_REQUEST["db"] != "iot" || ($_REQUEST["table"] != "data" && $_REQUEST["table"] != "links" && $_REQUEST["table"] != "rules")) {
    echo 'Error';
    exit();
}

if (isset($_REQUEST["id"]) && isset($_REQUEST["db"]) && isset($_REQUEST["table"])) {
 if (!($stmt = $mysqli->prepare("DELETE FROM ?.? WHERE id = ?"))) {
    echo "Prepare failed: (" . $mysqli->errno . ") " . $mysqli->error;
 }
 if (!$stmt->bind_param("sss", $_REQUEST["db"], $_REQUEST["table"], $_REQUEST["id"])) {
    echo "Binding parameters failed: (" . $stmt->errno . ") " . $stmt->error;
 }
 if (!$stmt->execute()) {
    echo "Execute failed: (" . $stmt->errno . ") " . $stmt->error;
 } else {
   echo 'Data Deleted';
 }
}
$connection->close();
?>
