<?php
/*
Snap4city -- sysgraph.php --
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
include_once "settings.php";
//CONNECT
$link = mysqli_connect($config['host'], $config['user'], $config['pass'], $config['database']);

/* check connection */
if (mysqli_connect_errno()) {
    printf("Connection failed: %s\n", mysqli_connect_error());
    exit();
}

//GET DATA
$ip = (isset($_REQUEST['ip_address'])) ? (" AND IP_ADDRESS = '" . $_REQUEST['ip_address'] . "'") : "";

if (isset($_REQUEST['startAt']) && isset($_REQUEST['endAt'])) {
    if (strtotime($_REQUEST['startAt']) < strtotime($_REQUEST['endAt'])) {
        $sql = "SELECT DATE, IP_ADDRESS, " . mysqli_real_escape_string($link, $_REQUEST['metric_name']) . " FROM quartz.QRTZ_NODES WHERE DATE >= '" . mysqli_real_escape_string($link, $_REQUEST['startAt']) . "' AND DATE <= '" . mysqli_real_escape_string($link, $_REQUEST['endAt']) . "'" . mysqli_real_escape_string($link, $ip) . " ORDER BY DATE ASC";
        if ($_REQUEST['metric_name'] == "JOBS_PER_HOUR") {
            $sql = "SELECT DATE, IP_ADDRESS, JOBS_EXECUTED * 3600/(UNIX_TIMESTAMP(DATE)-UNIX_TIMESTAMP(RUNNING_SINCE)) AS JOBS_PER_HOUR FROM quartz.QRTZ_NODES WHERE DATE >= '" . mysqli_real_escape_string($link, $_REQUEST['startAt']) . "' AND DATE <= '" . mysqli_real_escape_string($link, $_REQUEST['endAt']) . "'" . mysqli_real_escape_string($link, $ip) . " ORDER BY DATE ASC";
        }
    } else {
        echo 'Start Time must be <= End Time';
        $error = "error";
    }
} else {
    $sql = "SELECT DATE, IP_ADDRESS, " . mysqli_real_escape_string($link, $_REQUEST['metric_name']) . " FROM quartz.QRTZ_NODES WHERE DATE >= NOW() - INTERVAL 1 HOUR " . mysqli_real_escape_string($link, $ip) . " ORDER BY DATE ASC";
    if ($_REQUEST['metric_name'] == "JOBS_PER_HOUR") {
        $sql = "SELECT DATE, IP_ADDRESS, JOBS_EXECUTED * 3600/(UNIX_TIMESTAMP(DATE)-UNIX_TIMESTAMP(RUNNING_SINCE)) AS JOBS_PER_HOUR FROM quartz.QRTZ_NODES WHERE DATE >= NOW() - INTERVAL 1 HOUR " . mysqli_real_escape_string($link, $ip) . " ORDER BY DATE ASC";
    }
}

if (!isset($error)) {
    $result = mysqli_query($link, $sql) or die(mysqli_error());
    $data = array();
    $metric_unit_array = array();
    $threshold_array = array();
    while ($row = mysqli_fetch_assoc($result)) {
        foreach ($row as $key => $value) {
            if ($key != 'DATE' && $key != 'IP_ADDRESS') {
                //if ($key == 'FREE_PHYSICAL_MEMORY' || $key == 'TOTAL_DISK_SPACE' || $key == 'COMMITTED_VIRTUAL_MEMORY' || $key == 'TOTAL_PHYSICAL_MEMORY' || $key == 'TOTAL_SWAP_SPACE' || $key = 'UNALLOCATED_DISK_SPACE' || $key == 'USABLE_DISK_SPACE')
                //$value = floatval($value) / (1024 * 1024 * 1024);
                $data[$key][$row['IP_ADDRESS']][] = array($row['DATE'], floatval($value), "");
                //$metric_unit_array[$row][$row['IP_ADDRESS']] = "#";
                //$threshold_array[$row][$row['IP_ADDRESS']] = 0;
            }
        }
    }

    //get the javascript arrays to be used by jqplot
    $i = 1;
    $metric_divs = '';
    foreach ($data as $metric_name => $metric_array) {
        foreach ($metric_array as $ip_address => $values) {
            //$metric_unit = $metric_unit_array[$metric_name][$ip_address];
            //$threshold = $threshold_array[$metric_name][$ip_address];
            $metric_unit = "";
            $threshold = 0;
            $ip_addresses .= "'" . $ip_address . "', ";
            $metric_divs .= "<div id=" . $metric_name . " style='height: 600px; width: 900px; position: relative;' class='jqplot-target'></div>\n";
            $plotData .= json_encode($data[$metric_name][$ip_address]) . ', ';
        }
        $plot .= getPlot($i, $plotData, $ip_addresses, $metric_name, $metric_unit, $threshold) . "\n";
        $i++;
    }
}

//get plots javascript
function getPlot($i, $plotData, $virtual_machine_names, $metric_name, $metric_unit, $threshold) {
    return "var plot" . $i . " = $.jqplot('" . $metric_name . "', [" . $plotData . "], {
                title: '$metric_name',
                legend: {
                    show: true,
                    //labels: [$virtual_machine_names 'Threshold'],
                    labels: [" . substr($virtual_machine_names, 0, -2) . "],
                    renderer: $.jqplot.EnhancedLegendRenderer,
                    location: 'ne' ,
                    placement : 'outside',
                    rendererOptions: {
                        numberRows: 1
                    }
                },
                seriesDefaults: {
                    showMarker: false,
                    pointLabels: {show: true},
                    rendererOption: {smooth: true}
                },
                canvasOverlay: {
                    show: true,
                    objects: [target_temp_dashed_line($threshold)],
                },
                axesDefaults: {
                    labelRenderer: $.jqplot.CanvasAxisLabelRenderer,
                },
                axes: {
                    xaxis: {
                        //label: 'Time',
                        renderer: $.jqplot.DateAxisRenderer,
                        tickOptions: {formatString: '%b %#d %H:%M:%S'},
                        tickRenderer: $.jqplot.CanvasAxisTickRenderer,
                        tickOptions: {
                            formatString: '%b %e %H:%M',
                            angle: -60
                        },
                    },
                    yaxis: {
                        label: '$metric_unit',
                        labelRenderer: $.jqplot.CanvasAxisLabelRenderer,
                        min: 0,
                        tickOptions: {formatString: '%.2f'},
                    },
                },
                highlighter: {
                    show: true,
                    tooltipLocation: 'ne',
                    tooltipAxes: 'xy',
                    useAxesFormatters: true,
                    formatString: '%s, %s',
                    sizeAdjust: 7.5
                },
                cursor: {
                    show: false
                }
            });";
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
?>

<!DOCTYPE html>
<html lang="en">
    <head>
        <!-- imports for datepickers -->
        <script type="text/javascript" src="../sce/javascript/jquery-2.1.0.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jquery-ui.js"></script>
        <script type="text/javascript" src="../sce/javascript/jquery-ui-timepicker-addon.js"></script>
        <link rel="stylesheet" type="text/css" href="../sce/css/jquery-ui.css">
        <link rel="stylesheet" type="text/css" href="../sce/css/jquery-ui-timepicker-addon.css" />

        <script>
            $(function() {
                $("#datepicker1, #datepicker2").each(function() {
                    $(this).datetimepicker({
                        timeFormat: "HH:mm:ss",
                        dateFormat: "yy-mm-dd",
                        autoclose: true
                    });
                });
            });
        </script>

        <!-- imports for jqplot -->
        <script type="text/javascript" src="../sce/javascript/jqplot/jquery.jqplot.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.canvasTextRenderer.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.canvasAxisLabelRenderer.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.canvasAxisTickRenderer.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.dateAxisRenderer.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.categoryAxisRenderer.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.pointLabels.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.canvasOverlay.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.highlighter.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.cursor.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.pointLabels.min.js"></script>
        <script type="text/javascript" src="../sce/javascript/jqplot/plugins/jqplot.dateAxisRenderer.min.js"></script>
        <link rel="stylesheet" type="text/css" href="../sce/javascript/jqplot/jquery.jqplot.min.css">
    </head>

    <body>
        <!-- date pickers form-->
        <form action="" method="POST">
            <p><b title="Set the start time of the graph">Start At: </b><input type="text" name="startAt" value="<?php echo (isset($_REQUEST['startAt']) ? $_REQUEST['startAt'] : date("Y-m-d H:i:s", round(microtime(true)) - 3600)) ?>" id="datepicker1" />
                <b title="Set the end time of the graph">End At: </b><input type="text" name="endAt" value="<?php echo (isset($_REQUEST['endAt']) ? $_REQUEST['endAt'] : date("Y-m-d H:i:s", round(microtime(true)))) ?>" id="datepicker2" />
                <input type="hidden" value="<?php echo $_REQUEST['sla']; ?>" name="sla">
                <input type="hidden" value="<?php echo $_REQUEST['metric_name']; ?>" name="metric_name">
                <?php
                if (isset($_REQUEST['ip_address']) && $_REQUEST['ip_address'] != "")
                    echo '<input type="hidden" value="' . $_REQUEST['ip_address'] . '" name="ip_address">';
                ?>
                <input name=confirm" type="submit" value="Plot" /></p>
        </form>
        <?php
        echo $metric_divs;
        ?>
    </body>

    <script class="code" type="text/javascript">
            var target_temp_dashed_line = function(target_temp) {
                return {dashedHorizontalLine: {
                        name: 'Boiling Pt',
                        y: target_temp,
                        lineWidth: 3,
                        color: '#EF3E42',
                        shadow: false
                    }
                };
            }
            $(document).ready(function() {
        <?php
        if (isset($plot))
            echo $plot;
        ?>
                //theme
                var coolTheme = {
                    legend: {
                        location: 'se',
                    },
                    title: {
                        textColor: '#002225',
                        fontSize: '25',
                    },
                    series: [
                        {color: '#00AAA1', lineWidth: 5, markerOptions: {show: true}},
                        {color: '#79ccc7', lineWidth: 2, linePattern: 'dashed'},
                        {color: '#EF3E42'},
                    ],
                    grid: {
                        backgroundColor: '#E3E7EA',
                        gridLineColor: '#002225'
                    },
                };
                //coolTheme = plot1.themeEngine.newTheme('coolTheme', coolTheme);
                //plot1.activateTheme('coolTheme');
            });
    </script>
</html>
