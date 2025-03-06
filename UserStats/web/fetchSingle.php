<?php
/*
Snap4city -- fetchSingle.php --
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
*/
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
