<?php

//http://www.webslesson.info/2017/04/jquery-bootgrid-server-side-processing-using-ajax-php.html
include("connection.php");

if (isset($_POST["operation"])) {
    if ($_POST["operation"] == "Add") {
        /* $name = mysqli_real_escape_string($connection, $_POST["name"]);
          $execution_time = mysqli_real_escape_string($connection, $_POST["execution_time"]);
          $deadline = mysqli_real_escape_string($connection, $_POST["deadline"]);
          $cpu = mysqli_real_escape_string($connection, $_POST["cpu"]);
          $ram = mysqli_real_escape_string($connection, $_POST["ram"]);
          $disk = mysqli_real_escape_string($connection, $_POST["disk"]);
          $type = mysqli_real_escape_string($connection, $_POST["type"]); */

        // field names
        $f = "";
        // field values
        $v = "";
        $fields = json_decode(urldecode($_REQUEST["fieldNames"]));
        for ($i = 0; $i < count($fields); $i++) {
            if ($fields[$i] != "id") {
                $f .= "`" . $fields[$i] . "`" . ($i != count($fields) - 1 ? "," : "");
                $v .= "'" . mysqli_real_escape_string($connection, $_REQUEST[$fields[$i]]) . "'" . ($i != count($fields) - 1 ? "," : "");
            }
        }
        $query = "INSERT INTO " . $_REQUEST["dbName"] . "." . $_REQUEST["tableName"] . " (" . $f . ") VALUES (" . $v . ")";
        //file_put_contents("prova.txt", $query);
        if (mysqli_query($connection, $query)) {
            echo 'Row Inserted';
        }
    } else if ($_POST["operation"] == "Edit") {
        /* $name = mysqli_real_escape_string($connection, $_POST["name"]);
          $execution_time = mysqli_real_escape_string($connection, $_POST["execution_time"]);
          $deadline = mysqli_real_escape_string($connection, $_POST["deadline"]);
          $cpu = mysqli_real_escape_string($connection, $_POST["cpu"]);
          $ram = mysqli_real_escape_string($connection, $_POST["ram"]);
          $disk = mysqli_real_escape_string($connection, $_POST["disk"]);
          $type = mysqli_real_escape_string($connection, $_POST["type"]); */
        // field values
        $v = "";
        $fields = json_decode(urldecode($_REQUEST["fieldNames"]));
        for ($i = 0; $i < count($fields); $i++) {
            if ($fields[$i] != "id") {
                $v .= "`" . $fields[$i] . "`='" . mysqli_real_escape_string($connection, $_REQUEST[$fields[$i]]) . "'" . ($i != count($fields) - 1 ? "," : "");
            }
        }
        $query = "UPDATE " . $_REQUEST["dbName"] . "." . $_REQUEST["tableName"] . " SET " . $v . "  WHERE id = '" . $_REQUEST["id"] . "'";
        //file_put_contents("prova.txt", $query);
        if (mysqli_query($connection, $query)) {
            echo 'Row Updated';
        }
    }
}
$connection->close();
?>
