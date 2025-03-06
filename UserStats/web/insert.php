<?php

/*
Snap4city -- insert.php --
   Copyright (C) 2020 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/include("connection.php");

if (!isset($_REQUEST["db"]) || $_REQUEST["db"] != "iot" || !isset($_REQUEST["table"]) || ($_REQUEST["table"] != "data" && $_REQUEST["table"] != "links" && $_REQUEST["table"] != "rules")) {
    echo 'Error';
    exit();
}

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
        $query = "INSERT INTO " . mysqli_real_escape_string($connection, $_REQUEST["dbName"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["tableName"]) . " (" . $f . ") VALUES (" . $v . ")";
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
        $query = "UPDATE " . $_REQUEST["dbName"] . "." . mysqli_real_escape_string($connection, $_REQUEST["tableName"]) . " SET " . $v . "  WHERE id = '" . mysqli_real_escape_string($connection, $_REQUEST["id"]) . "'";
        //file_put_contents("prova.txt", $query);
        if (mysqli_query($connection, $query)) {
            echo 'Row Updated';
        }
    }
}
$connection->close();
?>
