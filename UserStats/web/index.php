<?php
//ini_set('session.save_path', '/mnt/nfs');
//ini_set('session.gc_probability', 1);
//header("Cache-Control: no cache");
//session_cache_limiter("private_no_expire");

session_start();

// check the permission
if (!isset($_SESSION["role"])) {
    header("location: ssoLogin.php");
}

/* if (session_status() == PHP_SESSION_NONE) {
  session_set_cookie_params(0, '/', '.disit.org');
  session_start();
  } */
?>
<html xmlns="http://www.w3.org/1999/xhtml"> 
    <head> 
        <title>IOT Metrics</title> 
        <link rel="stylesheet" type="text/css" href="css/typography.css" />
        <link rel="stylesheet" type="text/css" href="css/index.css" />
    </head>
    <body>
        <div class="main">
            <div class="menu">
                <?php
                /* include_once "header.php";
                  include_once "ldap.php";

                  if (isset($_REQUEST["username"])) {
                  $_SESSION["username"] = $_REQUEST["username"];
                  }
                  if (isset($_REQUEST["password"])) {
                  $_SESSION["password"] = $_REQUEST["password"];
                  }

                  $admin = password_verify($_SESSION["username"], '$2y$10$pC..3ZSmPvQjv0byw9nwfu7FcjmLmvLhueFAVWB40tFDo9lka.CGC') &&
                  password_verify($_SESSION["password"], '$2y$10$Z5G04N3Y0tljsVDaJyNL/.neoVkvgfemW3IyJavzNKJTPrQqevBLy');

                  if ($admin) {
                  $_SESSION["role"] = "AreaManager";
                  }
                  // if default credentials are not correct, then try to authenticate with LDAP
                  if (!$admin) {
                  $areaManager = check_auth_ldap($_REQUEST["username"], $_REQUEST["password"], "WiFi", "AreaManager");
                  if ($areaManager) {
                  $_SESSION["role"] = "AreaManager";
                  }
                  }

                  // logout
                  if (isset($_REQUEST["logout"])) {
                  unset($_SESSION["username"]);
                  unset($_SESSION["password"]);
                  unset($_SESSION["role"]);
                  $admin = false;
                  $areaManager = false;
                  } */

                echo "<ul class=\"rig columns-4\">";

                if ($_SESSION["role"] == "RootAdmin") {
                    echo "<li>
                <a href=\"list.php?db=iot&table=data&days=1&title=" . urlencode("User Metrics (last day)&nbsp;") . "\">
                    <h3><img src=\"images/iot.png\" /><br>User Metrics<br>(last day)</h3></a>
            </li>";
                    echo "<li>
                <a href=\"list.php?db=iot&table=data&days=7&title=" . urlencode("User Metrics (last 7 days)&nbsp;") . "\">
                    <h3><img src=\"images/iot.png\" /><br>User Metrics<br>(last 7 days)</h3></a>
            </li>";
                    echo "<li>
                <a href=\"list.php?db=iot&table=data&days=30&title=" . urlencode("User Metrics (last 30 days)&nbsp;") . "\">
                    <h3><img src=\"images/iot.png\" /><br>User Metrics<br>(last 30 days)</h3></a>
            </li>";
                   echo "<li>
                <a href=\"list.php?db=iot&table=data&days=90&title=" . urlencode("User Metrics (last 90 days)&nbsp;") . "\">
                    <h3><img src=\"images/iot.png\" /><br>User Metrics<br>(last 90 days)</h3></a>
            </li>";
                    echo "<li>
                <a href=\"graph-general.php?db=iot&table=nodered&field=tx&days=30&title=" . urlencode("IoT Graphs &nbsp;") . "\">
                    <h3><img src=\"images/iot.png\" /><br>User Graphs</h3></a>
            </li>";
                    echo "<li>
                <a href=\"graph-etl.php?db=iot&table=etl&days=30&title=" . urlencode("ETL Graphs &nbsp;") . "\">
                    <h3><img src=\"images/pentaho.png\" /><br>ETL Graphs</h3></a>
            </li>";
                    echo "<li>
                <a href=\"graph-r.php?db=iot&table=r&field=num&days=30&title=" . urlencode("R Graphs &nbsp;") . "\">
                    <h3><img src=\"images/R.png\" /><br>R Graphs</h3></a>
            </li>";
                }
                echo "<li>
                <a href=\"user.php?title=" . urlencode("Daily User Stats &nbsp;") . "&time=day\">
                    <h3><img src=\"images/iot.png\" /><br>Daily User Stats</h3></a>
            </li>";
                echo "<li>
                <a href=\"user.php?title=" . urlencode("Monthly User Stats &nbsp;") . "&time=month\">
                    <h3><img src=\"images/iot.png\" /><br>Monthly User Stats</h3></a>
            </li>";
                echo "<li>
                <a href=\"list.php?db=iot&table=links&days=1&title=" . urlencode("Help Links&nbsp;") . "\">
                    <h3><img src=\"images/iot.png\" /><br>Help Links</h3></a>
            </li>";
                echo "<li>
                <a href=\"list.php?db=iot&table=rules&days=1&title=" . urlencode("Rules&nbsp;") . "\">
                    <h3><img src=\"images/iot.png\" /><br>Rules</h3></a>
            </li>";
                ?>
                </ul>
            </div>
            <?php
            /* if (!$admin && !$areaManager) {
              echo "<div class=\"login\"><form action=\"index.php\" method=\"post\" target=\"_self\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" autocomplete=\"off\" novalidate>";
              echo "Username: <input type=\"text\" name=\"username\" value=\"\">";
              echo "&nbsp;Password: <input type=\"password\" name=\"password\" value=\"\">";
              echo "&nbsp;<input type=\"submit\" value=\"Submit\">";
              echo "</form></div>";
              } else {
              echo "<div class=\"login\"><form action=\"index.php\" method=\"post\" target=\"_self\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" autocomplete=\"off\" novalidate>";
              echo "&nbsp;<input type=\"submit\" value=\"Logout\">";
              echo "</form></div>";
              } */
            ?>
        </div>
    </body>
</html>
