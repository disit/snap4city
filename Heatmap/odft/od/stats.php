<?php
/* Personal Recommender Web Interface
  Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. */
?>
<?php
session_start();
// check the permission
if (!isset($_SESSION["role"])) {
    header("location: ../ssoLogin.php");
}
?>
<html xmlns="http://www.w3.org/1999/xhtml"> 
    <head> 
        <title><?php
            if (isset($_REQUEST["title"])) {
                echo $_REQUEST["title"];
            } else {
                echo "Recommender";
            }
            ?>
        </title>
        <link rel="stylesheet" type="text/css" href="css/typography.css" />
        <link rel="stylesheet" type="text/css" href="../index.css" />
        <?php
        if ((isset($_REQUEST["showFrame"]) && $_REQUEST['showFrame'] == 'false') || $_SESSION['showFrame'] == 'false') {
            $_SESSION['showFrame'] = 'false';
            echo "<style>
            body { zoom: 0.8; }
            th, td, caption {
            width: 100px !important;
            max-height: 100px !important;
            }
            #resultsTable { padding-top: 0px !important; }
        </style>";
        }
        ?>
    </head>
    <body>
        <?php
        //include_once "login.php";

        function mdate($format, $microtime = null) {
            $microtime = explode(' ', ($microtime ? $microtime : microtime()));
            if (count($microtime) != 2) {
                return false;
            }
            $microtime[0] = $microtime[0] * 1000000;
            $format = str_replace('u', $microtime[0], $format);
            return date($format, $microtime[1]);
        }

        //convert stdClass Objects to multidimensional array
        function objectToArray($d) {
            if (is_object($d)) {
                // Gets the properties of the given object
                // with get_object_vars function
                $d = get_object_vars($d);
            }

            if (is_array($d)) {
                /*
                 * Return array converted to object
                 * Using __FUNCTION__ (Magic constant)
                 * for recursive call
                 */
                return array_map(__FUNCTION__, $d);
            } else {
                // Return array
                return $d;
            }
        }

        function bytesToSize($bytes, $precision = 2) {
            $kilobyte = 1024;
            $megabyte = $kilobyte * 1024;
            $gigabyte = $megabyte * 1024;
            $terabyte = $gigabyte * 1024;

            if (($bytes >= 0) && ($bytes < $kilobyte)) {
                return $bytes . ' B';
            } elseif (($bytes >= $kilobyte) && ($bytes < $megabyte)) {
                return min_precision($bytes / $kilobyte, $precision) . ' KB';
            } elseif (($bytes >= $megabyte) && ($bytes < $gigabyte)) {
                return min_precision($bytes / $megabyte, $precision) . ' MB';
            } elseif (($bytes >= $gigabyte) && ($bytes < $terabyte)) {
                return min_precision($bytes / $gigabyte, $precision) . ' GB';
            } elseif ($bytes >= $terabyte) {
                return min_precision($bytes / $terabyte, $precision) . ' TB';
            } else {
                return $bytes . ' B';
            }
        }

        // use this instead of php round function, find the minimum precision of a number (e.g. precision 2, 1 -> 1.00, 0.1 -> 0.10, 0.001 -> 0.001)
        function min_precision($val, $min = 2, $max = 4) {
            $result = round($val, $min);
            if ($result == 0 && $min < $max) {
                return min_precision($val, ++$min, $max);
            } else {
                return $result;
            }
        }

        // calculate the number of recommendations in the last n days
        function recommendations($link, $days) {
            $num;
            $sql = "SELECT SUM(nrecommendations_total) AS num FROM recommender.recommendations_log a LEFT JOIN recommender.users b ON a.user = b.user WHERE b.label IS NULL AND timestamp > DATE(NOW() - INTERVAL " . $days . " DAY)";
            $result = mysqli_query($link, $sql) or die(mysqli_error());
            while ($row = mysqli_fetch_assoc($result)) {
                $num = $row['num'];
            }
            return $num;
        }

        // calculate the number of active users in the last n days
        function active_users($link, $days) {
            $num;
            if ($days == 0) {
                $sql = "SELECT COUNT(DISTINCT(a.user)) AS num FROM recommender.recommendations_log a LEFT JOIN recommender.users b ON a.user = b.user WHERE b.label IS NULL";
            } else {
                $sql = "SELECT COUNT(DISTINCT(a.user)) AS num FROM recommender.recommendations_log a LEFT JOIN recommender.users b ON a.user = b.user WHERE b.label IS NULL AND timestamp > DATE(NOW() - INTERVAL " . $days . " DAY)";
            }
            $result = mysqli_query($link, $sql) or die(mysqli_error());
            while ($row = mysqli_fetch_assoc($result)) {
                $num = $row['num'];
            }
            return $num;
        }

        // calculate the number of views after recommendations in the last n days
        function recommendations_after_views($link, $days) {
            $num = 0;
            $sql = "SELECT COUNT(*) AS num FROM recommender.recommendations_stats a LEFT JOIN recommender.users b ON a.user = b.user WHERE b.label IS NULL AND viewedAt > DATE(NOW() - INTERVAL " . $days . " DAY)";
            $result = mysqli_query($link, $sql) or die(mysqli_error());
            while ($row = mysqli_fetch_assoc($result)) {
                $num = $row['num'];
            }
            return $num;
        }

        if ((!isset($_REQUEST["showFrame"]) || $_REQUEST['showFrame'] != 'false') && $_SESSION['showFrame'] != 'false') {
            include_once "header.php"; //include header
        }
        //include_once "settings.php";

        $milliseconds = round(microtime(true) * 1000);

        //CONNECT
        $link = mysqli_connect($config['host'], $config['user'], $config['pass'], $config['database']);

        /* check connection */
        if (mysqli_connect_errno()) {
            printf("Connection failed: %s\n", mysqli_connect_error());
            exit();
        }

        $nrecommendations_1_day = recommendations($link, 1); // the number of recommendations in the last day
        $nrecommendations_7_days = recommendations($link, 7); // the number of recommendations in the last 7 days
        $nrecommendations_per_hour = round($nrecommendations_1_day / 24, 2); // the number of recommendations per hour in the last day
        $active_users_1_day = active_users($link, 1); // the number of active users in the last day
        $active_users_7_days = active_users($link, 7); // the number of active users in the last 7 days
        $active_users_1_month = active_users($link, 30); // the number of active users in the last month
        $users_total = active_users($link, 0); // the total number of users
        $recommendations_per_user_1_day = round($nrecommendations_1_day / $active_users_1_day, 2); // the number of recommendations per user in the last day
        $recommendations_per_user_7_days = round($nrecommendations_7_days / $active_users_7_days, 2); // the number of recommendations per user in the last 7 days
        $recommendations_after_views_1_day = recommendations_after_views($link, 1); // the number of recommendations and views after them in the last day
        $recommendations_after_views_7_days = recommendations_after_views($link, 7); // the number of recommendations and views after them in the last 7 days
        // close MySQL connection
        mysqli_close($link);

        echo "<div class='clusterContainer'/>";
        echo "<div class='clusterTotal' id='gradient'>";

        echo "<table>";

        echo "<thead>";
        echo "<tr>";
        echo "<th>Recs (24 h)</th><th>Recs (7 days)</th><th>Recs/h (24 h)</th><th>Active Users (24 h)</th><th>Active Users (7 days)</th><th>Active Users (30 days)</th><th>Active Users (Total)</th><th>Recs/User (24 h)</th><th>Recs/User (7 days)</th>";
        echo "</tr>";
        echo "</thead>";

        echo "<tbody>";
        echo "<tr>";
        echo "<td>" . ($nrecommendations_1_day > 0 ? $nrecommendations_1_day : "-") . "</td>";
        echo "<td>" . ($nrecommendations_7_days > 0 ? $nrecommendations_7_days : "-") . "</td>";
        echo "<td>" . ($nrecommendations_per_hour > 0 ? $nrecommendations_per_hour : "-") . "</td>";
        echo "<td>" . ($active_users_1_day > 0 ? $active_users_1_day : "-") . "</td>";
        echo "<td>" . ($active_users_7_days > 0 ? $active_users_7_days : "-") . "</td>";
        echo "<td>" . ($active_users_1_month > 0 ? $active_users_1_month : "-") . "</td>";
        echo "<td>" . ($users_total > 0 ? $users_total : "-") . "</td>";
        echo "<td>" . ($recommendations_per_user_1_day > 0 ? $recommendations_per_user_1_day : "-") . "</td>";
        echo "<td>" . ($recommendations_per_user_7_days > 0 ? $recommendations_per_user_7_days : "-") . "</td>";
        //echo "<td>" . ($nrecommendations_1_day > 0 ? 100 * $recommendations_after_views_1_day / $nrecommendations_1_day . "&#37;" : "-") . "</td>";
        //echo "<td>" . ($nrecommendations_7_days > 0 ? 100 * $recommendations_after_views_7_days / $nrecommendations_7_days . "&#37;" : "-") . "</td>";
        echo "</tr>";
        echo "</tbody>";

        echo "<thead>";
        echo "<tr>";
        echo "<th>(Views after Recs)/Recs (24 h)</th><th>(Views after Recs)/Recs (7 days)</th>";
        echo "</tr>";
        echo "</thead>";

        echo "<tbody>";
        echo "<tr>";
        echo "<td>" . $recommendations_after_views_1_day . "/" . $nrecommendations_1_day . " (" . round(100 * $recommendations_after_views_1_day / $nrecommendations_1_day, 2) . "%)</td>";
        echo "<td>" . $recommendations_after_views_7_days . "/" . $nrecommendations_7_days . " (" . round(100 * $recommendations_after_views_7_days / $nrecommendations_7_days, 2) . "%)</td>";
        echo "</tr>";
        echo "</tbody>";

        echo "</table>";
        echo "</div>";

        echo "</div>";

        echo '<div class="clusterTime">Last updated on: ' . mdate('D d-m-Y H:i:s.u') . ' generated in ' . (round(microtime(true) * 1000) - $milliseconds) . ' ms (refresh time ' . $config['refreshTime'] . ' ms)';
        echo '&emsp;<a style="font-size:10px" class="pointer" title="View Cluster Status" href="cluster.php">Static</a>';
        echo '&emsp;<a style="font-size:10px" class="pointer" title="View Cluster Status" href="reload-cluster.php">Push</a>';
        echo '</div>';

        /* if (isset($_SESSION["role"]) && ((isset($_REQUEST["showFrame"]) && $_REQUEST['showFrame'] != 'false') || (isset($_SESSION["showFrame"]) && $_SESSION["showFrame"] != 'false'))) {
          echo "<div class=\"logout\"><form action=\"index.php\" method=\"post\" target=\"_self\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" autocomplete=\"off\" novalidate>";
          echo "&nbsp;<input type=\"submit\" value=\"Logout\">";
          echo "</form></div>";
          } */
        ?>
    </body>
</html>
