<?php
/*
Snap4city -- delete.php --
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
