<?php

include("connection.php");
if (isset($_REQUEST["id"]) && isset($_REQUEST["db"]) && isset($_REQUEST["table"])) {
    $query = "DELETE FROM " . $_REQUEST["db"] . "." . $_REQUEST["table"] . " WHERE id = '" . $_POST["id"] . "'";
    if (mysqli_query($connection, $query)) {
        echo 'Data Deleted';
    }
}
$connection->close();
?>