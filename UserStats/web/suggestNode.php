<?php
header('Access-Control-Allow-Origin: *');
include("connection.php");
$data = array();
$resources = array();
session_start();

function getValue($n) {
 if($n == null || !is_numeric($n)) {
  return 0;
 } else {
  return floatval($n);
 }
}

function logtext($text) {
 $fp = fopen("log.txt", "at");
 fwrite($fp, $text . "\n");
 fclose($fp);
}

// check the permission
if (!isset($_SESSION["role"])) {
    header("location: ssoLogin.php");
}

// get user data
$user_data = array();

//$query = "SELECT * FROM iot.data where username = '" . $_SESSION["username"] . "' ORDER BY date DESC LIMIT 1";
$query = "SELECT a.*, b.organizations FROM iot.data a LEFT JOIN iot.roles_levels b ON a.username = b.username WHERE a.username = '" . $_SESSION["username"] . "' ORDER BY a.date DESC LIMIT 1";
$result = mysqli_query($connection, $query);
while ($row = mysqli_fetch_assoc($result)) {
       $user_data = $row;
}
// if this is a new user set all fields to 0
if(count($user_data) == 0) {
 $query = "SELECT *, 0 AS dashboards, 0 AS dashboards_public, 0 AS dashboards_private, 0 AS dashboards_accesses, 0 AS dashboards_minutes, 0 AS iot_devices, 0 AS iot_devices_public, 0 AS iot_devices_private, 0 AS iot_tx, 0 AS iot_rx, 0 AS iot_applications FROM iot.roles_levels WHERE username = '" . $_SESSION["username"] . "'";
 $result = mysqli_query($connection, $query);
 while ($row = mysqli_fetch_assoc($result)) {
       $user_data = $row;
 }
}

// get node data
//$query = "SELECT * FROM iot.metrics_levels WHERE role = '" . $_SESSION["role"] . "' OR role = 'any'" ;
$query = "SELECT * FROM iot.metrics_levels ORDER BY `order`" ;
$result = mysqli_query($connection, $query);

$condition = null;
$node = null;
while ($row = mysqli_fetch_assoc($result)) {
        $membership = true;
        $user_data_organizations = json_decode($user_data["organizations"]);
        // lowercase all elements of the array
        $user_data_organizations = array_map('strtolower', $user_data_organizations);
        if(strtolower($row["group_membership"]) != "any" && $row["group_membership"] != null) {
         $organizations = explode(",", $row["group_membership"]);
         foreach($organizations as $organization) {
          if(!in_array(trim(strtolower($organization)), $user_data_organizations)) {
           $membership = false;
           break;
          }
         }
        }
        if(!$membership) {
         continue;
        }

        if($row["dashboards"] != null) {
         $condition = getValue($user_data["dashboards_public"]) + getValue($user_data["dashboards_private"]) < getValue($row["dashboards"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["dashboards_public"] != null) {
         $condition = $condition && getValue($user_data["dashboards_public"]) < getValue($row["dashboards_public"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["dashboards_private"] != null) {
         $condition = $condition && getValue($user_data["dashboards_private"]) < getValue($row["dashboards_private"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["dashboards_accesses"] != null) {
         $condition = $condition && getValue($user_data["dashboards_accesses"]) < getValue($row["dashboards_accesses"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["dashboards_minutes"] != null) {
         $condition = $condition && getValue($user_data["dashboards_minutes"]) < getValue($row["dashboards_minutes"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["iot_devices"] != null) {
         $condition = $condition && getValue($user_data["devices_public"]) + getValue($user_data["devices_private"]) < getValue($row["iot_devices"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["iot_devices_public"] != null) {
         $condition = $condition && getValue($user_data["devices_public"]) < getValue($row["iot_devices_public"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["iot_devices_private"] != null) {
         $condition = $condition && getValue($user_data["devices_private"]) < getValue($row["iot_devices_private"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["iot_tx"] != null) {
         $condition = $condition && getValue($user_data["iot_tx"]) < getValue($row["iot_tx"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["iot_rx"] != null) {
         $condition = $condition && getValue($user_data["iot_rx"]) < getValue($row["iot_rx"]);
         if(!$condition) {
          continue;
         }
        }
        if($row["iot_applications"] != null) {
         $condition = $condition && getValue($user_data["iot_apps"]) < getValue($row["iot_applications"]);
         if(!$condition) {
          continue;
         }
        }
        $node = $row["node"];
        break;
}

$connection->close();

if($condition!= null && $node != null) {
 echo $node;
} else {
 echo "";
}
?>
