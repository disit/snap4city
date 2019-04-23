<?php
header('Access-Control-Allow-Origin: *');

session_start();

// check the permission
if (!isset($_SESSION["role"])) {
    header("location: ssoLogin.php");
}

// RootAdmin has the right to make this query by any username
$username = ($_SESSION["role"] == "RootAdmin" && isset($_REQUEST["username"])) ? $_REQUEST["username"] : $_SESSION["username"];
$role = ($_SESSION["role"] == "RootAdmin" && isset($_REQUEST["role"])) ? $_REQUEST["role"] : $_SESSION["role"];
$level = ($_SESSION["role"] == "RootAdmin" && isset($_REQUEST["level"])) ? $_REQUEST["level"] : $_SESSION["level"];

$db_storage_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_db_storage_tx&username=" . $username);
$db_storage_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_db_storage_rx&username=" . $username);

$filesystem_storage_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_filesystem_storage_tx&username=" . $username);
$filesystem_storage_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_filesystem_storage_rx&username=" . $username);

$db_request_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_db_request_tx&username=" . $username);
$db_request_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_db_request_rx&username=" . $username);

$ascapi_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_ascapi_tx&username=" . $username);
$ascapi_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_ascapi_rx&username=" . $username);

$disces_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_disces_tx&username=" . $username);
$disces_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_disces_rx&username=" . $username);

$dashboard_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_dashboard_tx&username=" . $username);
$dashboard_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_dashboard_rx&username=" . $username);

$datagate_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_datagate_tx&username=" . $username);
$datagate_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_datagate_rx&username=" . $username);

$external_service_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_external_service_tx&username=" . $username);
$external_service_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_external_service_rx&username=" . $username);

$iot_service_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_iot_service_tx&username=" . $username);
$iot_service_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_iot_service_rx&username=" . $username);

$mapping_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_mapping_tx&username=" . $username);
$mapping_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_mapping_rx&username=" . $username);

$microserviceusercreated_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_microserviceusercreated_tx&username=" . $username);
$microserviceusercreated_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_microserviceusercreated_rx&username=" . $username);

$mydata_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_mydata_tx&username=" . $username);
$mydata_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_mydata_rx&username=" . $username);

$notificator_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_notificator_tx&username=" . $username);
$notificator_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_notificator_rx&username=" . $username);

$rstatistics_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_rstatistics_tx&username=" . $username);
$rstatistics_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_rstatistics_rx&username=" . $username);

$sigfox_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_sigfox_tx&username=" . $username);
$sigfox_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_sigfox_rx&username=" . $username);

$undefined_tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_undefined_tx&username=" . $username);
$undefined_rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_undefined_rx&username=" . $username);

$tx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_tx&username=" . $username);
$rx = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=iot_rx&username=" . $username);

$devices_public = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=devices_public&username=" . $username);
$devices_private = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=devices_private&username=" . $username);
$dashboards_public = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=dashboards_public&username=" . $username);
$dashboards_private = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=dashboards_private&username=" . $username);
$dashboards_accesses = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=dashboards_accesses&username=" . $username);
$dashboards_minutes = file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&field=dashboards_minutes&username=" . $username);

// average of bytes (tx, rx) until last day
$iot_tx_rx_average = json_decode(file_get_contents("http://localhost/userstats/json-user.php?db=iot&table=data&time=all&username=" . $username));
?>
<html>
    <head>
        <script src="https://code.jquery.com/jquery-3.1.1.min.js"></script>
        <script src="https://code.highcharts.com/stock/highstock.js"></script>
        <script src="https://code.highcharts.com/stock/modules/exporting.js"></script>
        <script src="https://code.highcharts.com/stock/modules/export-data.js"></script>
        <script src="https://code.highcharts.com/modules/drilldown.js"></script>
        <style>
            div#graphs {
                border: 1px solid;
                padding: 10px;
                box-shadow: 5px 5px;
                margin-top: 30px;
                margin-bottom: 30px;
            }
            div.graph {
                margin-bottom: 10px;
                display: inline;
                width: 50%;
                float: left;
                /*width: 900px;*/
            }
        </style>
    </head>
    <body>
        <script>
            Highcharts.setOptions({
                global: {
                    useUTC: false
                }
            });

            // Node-RED tx/rx
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('iottxrx', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT tx/rx (kB)',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    plotOptions: {
                        column: {
                            stacking: 'normal',
                            dataLabels: {
                                enabled: false,
                                color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
                                style: {
                                    textShadow: '0 0 3px black'
                                }
                            },
                            pointWidth: 20,
                            pointPadding: 0.5, // Defaults to 0.1
                            groupPadding: 0.5 // Defaults to 0.2
                        },
                    },
                    yAxis: {
                        minorTickInterval: 0.1
                    },
                    series: [{
                            type: 'column',
                            name: 'tx',
                            data: <?php echo str_replace("\"", "", $tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'rx',
                            data: <?php echo str_replace("\"", "", $rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // Node-RED tx motivations
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('iottxmotivations', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT motivations tx (kB)',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    plotOptions: {
                        column: {
                            stacking: 'normal',
                            dataLabels: {
                                enabled: false,
                                color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
                                style: {
                                    textShadow: '0 0 3px black'
                                }
                            },
                            pointWidth: 20,
                            pointPadding: 0.5, // Defaults to 0.1
                            groupPadding: 0.5 // Defaults to 0.2
                        },
                    },
                    yAxis: {
                        //type: 'logarithmic',
                        minorTickInterval: 0.1
                    },
                    series: [{
                            type: 'column',
                            name: 'db_storage_tx',
                            data: <?php echo str_replace("\"", "", $db_storage_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'filesystem_storage_tx',
                            data: <?php echo str_replace("\"", "", $filesystem_storage_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'db_request_tx',
                            data: <?php echo str_replace("\"", "", $db_request_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'ascapi_tx',
                            data: <?php echo str_replace("\"", "", $ascapi_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'disces_tx',
                            data: <?php echo str_replace("\"", "", $disces_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'dashboard_tx',
                            data: <?php echo str_replace("\"", "", $dashboard_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'datagate_tx',
                            data: <?php echo str_replace("\"", "", $datagate_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'external_service_tx',
                            data: <?php echo str_replace("\"", "", $external_service_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'iot_service_tx',
                            data: <?php echo str_replace("\"", "", $iot_service_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'mapping_tx',
                            data: <?php echo str_replace("\"", "", $mapping_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'microserviceusercreated_tx',
                            data: <?php echo str_replace("\"", "", $microserviceusercreated_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'mydata_tx',
                            data: <?php echo str_replace("\"", "", $mydata_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'notificator_tx',
                            data: <?php echo str_replace("\"", "", $notificator_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'rstatistics_tx',
                            data: <?php echo str_replace("\"", "", $rstatistics_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'sigfox_tx',
                            data: <?php echo str_replace("\"", "", $sigfox_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'undefined_tx',
                            data: <?php echo str_replace("\"", "", $undefined_tx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // Node-RED rx motivations
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('iotrxmotivations', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT motivations rx (kB)',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    plotOptions: {
                        column: {
                            stacking: 'normal',
                            dataLabels: {
                                enabled: false,
                                color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
                                style: {
                                    textShadow: '0 0 3px black'
                                }
                            },
                            pointWidth: 20,
                            pointPadding: 0.5, // Defaults to 0.1
                            groupPadding: 0.5 // Defaults to 0.2
                        },
                    },
                    yAxis: {
                        //type: 'logarithmic',
                        minorTickInterval: 0.1
                    },
                    series: [{
                            type: 'column',
                            name: 'db_storage_rx',
                            data: <?php echo str_replace("\"", "", $db_storage_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'filesystem_storage_rx',
                            data: <?php echo str_replace("\"", "", $filesystem_storage_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'db_request_rx',
                            data: <?php echo str_replace("\"", "", $db_request_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'ascapi_rx',
                            data: <?php echo str_replace("\"", "", $ascapi_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'disces_rx',
                            data: <?php echo str_replace("\"", "", $disces_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'dashboard_rx',
                            data: <?php echo str_replace("\"", "", $dashboard_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'datagate_rx',
                            data: <?php echo str_replace("\"", "", $datagate_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'external_service_rx',
                            data: <?php echo str_replace("\"", "", $external_service_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'iot_service_rx',
                            data: <?php echo str_replace("\"", "", $iot_service_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'mapping_rx',
                            data: <?php echo str_replace("\"", "", $mapping_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'microserviceusercreated_rx',
                            data: <?php echo str_replace("\"", "", $microserviceusercreated_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'mydata_rx',
                            data: <?php echo str_replace("\"", "", $mydata_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'notificator_rx',
                            data: <?php echo str_replace("\"", "", $notificator_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'rstatistics_rx',
                            data: <?php echo str_replace("\"", "", $rstatistics_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'sigfox_rx',
                            data: <?php echo str_replace("\"", "", $sigfox_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'undefined_rx',
                            data: <?php echo str_replace("\"", "", $undefined_rx); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // iot apps
            $.getJSON('json-user.php?db=iot&table=data&field=iot_apps&username=<?php echo $_REQUEST["username"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('iotapps', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT Apps (#)',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    series: [{
                            type: 'column',
                            name: 'IoT Apps (#)',
                            data: data,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // Devices public/private
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('devices', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Devices (#)',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    plotOptions: {
                        column: {
                            stacking: 'normal',
                            dataLabels: {
                                enabled: false,
                                color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
                                style: {
                                    textShadow: '0 0 3px black'
                                }
                            },
                            pointWidth: 20,
                            pointPadding: 0.5, // Defaults to 0.1
                            groupPadding: 0.5 // Defaults to 0.2
                        },
                        //showCheckbox: true
                    },
                    yAxis: {
                        minorTickInterval: 0.1
                    },
                    series: [{
                            type: 'column',
                            name: 'public',
                            data: <?php echo str_replace("\"", "", $devices_public); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'private',
                            data: <?php echo str_replace("\"", "", $devices_private); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // Dashboards public/private
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('dashboards', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Dashboards (#)',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    plotOptions: {
                        column: {
                            stacking: 'normal',
                            dataLabels: {
                                enabled: false,
                                color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
                                style: {
                                    textShadow: '0 0 3px black'
                                }
                            },
                            pointWidth: 20,
                            pointPadding: 0.5, // Defaults to 0.1
                            groupPadding: 0.5 // Defaults to 0.2
                        },
                    },
                    yAxis: {
                        minorTickInterval: 0.1
                    },
                    series: [{
                            type: 'column',
                            name: 'public',
                            data: <?php echo str_replace("\"", "", $dashboards_public); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }, {
                            type: 'column',
                            name: 'private',
                            data: <?php echo str_replace("\"", "", $dashboards_private); ?>,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // Dashboards accesses (#)
            $.getJSON('json-user.php?db=iot&table=data&field=dashboards_accesses&username=<?php echo $_REQUEST["username"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('dashboardsaccesses', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Dashboards accesses (#)',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    series: [{
                            type: 'column',
                            name: 'Dashboards accesses (#)',
                            data: data,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // Dashboards minutes
            $.getJSON('json-user.php?db=iot&table=data&field=dashboards_minutes&username=<?php echo $_REQUEST["username"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('dashboardsminutes', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Dashboards minutes',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    series: [{
                            type: 'column',
                            name: 'Dashboards minutes',
                            data: data,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // motivation facets
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                Highcharts.chart('container', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    title: {
                        text: 'Browser market shares. January, 2018',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    subtitle: {
                        text: 'Click the columns to user\'s detail'
                    },
                    xAxis: {
                        type: 'category'
                    },
                    yAxis: {
                        title: {
                            text: 'Total percent market share'
                        }

                    },
                    legend: {
                        enabled: false
                    },
                    plotOptions: {
                        series: {
                            borderWidth: 0,
                            dataLabels: {
                                enabled: true,
                                format: '{point.y:.1f}%'
                            }
                        }
                    },
                    tooltip: {
                        headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
                        pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.2f}%</b> of total<br/>'
                    },
                    "series": [
                        {
                            "name": "Browsers",
                            "colorByPoint": true,
                            "data": [
                                {
                                    "name": "Chrome",
                                    "y": 62.74,
                                    "drilldown": "Chrome"
                                },
                                {
                                    "name": "Firefox",
                                    "y": 10.57,
                                    "drilldown": "Firefox"
                                },
                                {
                                    "name": "Internet Explorer",
                                    "y": 7.23,
                                    "drilldown": "Internet Explorer"
                                },
                                {
                                    "name": "Safari",
                                    "y": 5.58,
                                    "drilldown": "Safari"
                                },
                                {
                                    "name": "Edge",
                                    "y": 4.02,
                                    "drilldown": "Edge"
                                },
                                {
                                    "name": "Opera",
                                    "y": 1.92,
                                    "drilldown": "Opera"
                                },
                                {
                                    "name": "Other",
                                    "y": 7.62,
                                    "drilldown": null
                                }
                            ]
                        }
                    ]
                });
            });

            // IoT messages tx
            $.getJSON('json-user.php?db=iot&table=data&field=iot_tx&username=<?php echo $_REQUEST["username"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('iotmessagestx', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT messages tx (kB) - average: <?php echo $iot_tx_rx_average[0][0]; ?>',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    series: [{
                            type: 'column',
                            name: 'IoT messages (kB)',
                            data: data,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // IoT messages rx
            $.getJSON('json-user.php?db=iot&table=data&field=iot_rx&username=<?php echo $_REQUEST["username"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('iotmessagesrx', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT messages rx (kB) - average: <?php echo $iot_tx_rx_average[0][1]; ?>',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    series: [{
                            type: 'column',
                            name: 'IoT messages (kB)',
                            data: data,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // IoT reads
            $.getJSON('json-user.php?db=iot&table=data&field=iot_reads&username=<?php echo $_REQUEST["username"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('iotreads', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT reads',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    series: [{
                            type: 'column',
                            name: 'IoT reads (#)',
                            data: data,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // IoT writes
            $.getJSON('json-user.php?db=iot&table=data&field=iot_writes&username=<?php echo $_REQUEST["username"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('iotwrites', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT writes (#)',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    series: [{
                            type: 'column',
                            name: 'IoT writes (#)',
                            data: data,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });

            // ETL writes
            $.getJSON('json-user.php?db=iot&table=data&field=etl_writes&username=<?php echo $_REQUEST["username"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('etlwrites', {
                    chart: {
                        alignTicks: false,
                        borderColor: '#C8C8C8',
                        borderWidth: 2,
                        shadow: true
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'ETL writes',
                        style: {
                            color: '#000000',
                            fontWeight: 'bold',
                            fontSize: '18px'
                        }
                    },
                    series: [{
                            type: 'column',
                            name: 'ETL writes (#)',
                            data: data,
                            dataGrouping: {
                                units: [[
                                        'day', // unit name
                                        [1] // allowed multiples
                                    ], [
                                        'month',
                                        [1, 2, 3, 4, 5, 6]
                                    ]]
                            }
                        }],
                    legend: {
                        enabled: true
                    }
                });
            });
        </script>
        <!--<div id="graphs">-->
        <div><b>Username:</b> <?php echo $username; ?>&nbsp;<b>Role:</b> <?php echo $role; ?>&nbsp;<b>Level:</b> <?php echo $level; ?></div>
        <div id="iottxrx" class="graph" style="height: 400px"></div>
        <div id="iottxmotivations" class="graph" style="height: 400px"></div>
        <div id="iotrxmotivations" class="graph" style="height: 400px"></div>
        <div id="iotapps" class="graph" style="height: 400px"></div>
        <div id="iotmessagestx" class="graph" style="height: 400px"></div>
        <div id="iotmessagesrx" class="graph" style="height: 400px"></div>
        <div id="iotreads" class="graph" style="height: 400px"></div>
        <div id="iotwrites" class="graph" style="height: 400px"></div>
        <!--</div>-->
        <!--<div id="graphs">-->
        <div id="devices" class="graph" style="height: 400px"></div>
        <div id="dashboards" class="graph" style="height: 400px"></div>
        <div id="dashboardsaccesses" class="graph" style="height: 400px"></div>
        <div id="dashboardsminutes" class="graph" style="height: 400px"></div>
        <!--</div>-->
        <!--<div id="graphs">-->
        <div id="etlwrites" class="graph" style="height: 400px"></div>
        <!--</div>-->
    </body>
</html>
