<?php

include("connection.php");

if (isset($_POST["id"])) {
    $output = array();
    $query = "SELECT * FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " WHERE id = '" . mysqli_real_escape_string($connection, $_POST["id"]) . "'";
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_array($result)) {
        /* $output["name"] = $row["name"];
          $output["execution_time"] = $row["execution_time"];
          $output["deadline"] = $row["deadline"];
          $output["cpu"] = $row["cpu"];
          $output["ram"] = $row["ram"];
          $output["disk"] = $row["disk"];
          $output["type"] = $row["type"]; */

        $fields = json_decode(urldecode($_REQUEST["fields"]));
        //error_log(json_last_error());
        file_put_contents("prova.txt", urldecode($_REQUEST["fields"]));
        foreach ($fields as $field) {
            $output[$field] = $row[$field];
        }
    }
    echo json_encode($output);
}
$connection->close();
?>
