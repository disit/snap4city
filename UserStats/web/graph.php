<?php
/*
Snap4city -- graph.php --
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
header('Access-Control-Allow-Origin: *');
$nodered_db_storage_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_db_storage_tx");
$nodered_db_storage_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_db_storage_rx");

$nodered_filesystem_storage_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_filesystem_storage_tx");
$nodered_filesystem_storage_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_filesystem_storage_rx");

$nodered_db_request_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_db_request_tx");
$nodered_db_request_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_db_request_rx");

$nodered_ascapi_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_ascapi_tx");
$nodered_ascapi_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_ascapi_rx");

$nodered_disces_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_disces_tx");
$nodered_disces_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_disces_rx");

$nodered_dashboard_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_dashboard_tx");
$nodered_dashboard_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_dashboard_rx");

$nodered_datagate_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_datagate_tx");
$nodered_datagate_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_datagate_rx");

$nodered_external_service_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_external_service_tx");
$nodered_external_service_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_external_service_rx");

$nodered_iot_service_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_iot_service_tx");
$nodered_iot_service_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_iot_service_rx");

$nodered_mapping_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_mapping_tx");
$nodered_mapping_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_mapping_rx");

$nodered_microserviceusercreated_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_microserviceusercreated_tx");
$nodered_microserviceusercreated_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_microserviceusercreated_rx");

$nodered_mydata_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_mydata_tx");
$nodered_mydata_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_mydata_rx");

$nodered_notificator_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_notificator_tx");
$nodered_notificator_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_notificator_rx");

$nodered_rstatistics_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_rstatistics_tx");
$nodered_rstatistics_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_rstatistics_rx");

$nodered_sigfox_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_sigfox_tx");
$nodered_sigfox_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_sigfox_rx");

$nodered_undefined_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_undefined_tx");
$nodered_undefined_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_undefined_rx");

$nodered_tx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_tx");
$nodered_rx = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=nodered_rx");

$devices_public = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=devices_public");
$devices_private = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=devices_private");
$dashboards_public = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=dashboards_public");
$dashboards_private = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&username=" . $_REQUEST["username"] . "&field=dashboards_private");
?>
<html>
    <head>
        <script src="https://code.jquery.com/jquery-3.1.1.min.js"></script>
        <script src="https://code.highcharts.com/stock/highstock.js"></script>
        <script src="https://code.highcharts.com/stock/modules/exporting.js"></script>
        <script src="https://code.highcharts.com/stock/modules/export-data.js"></script>
    </head>
    <body>
        <script>
            Highcharts.setOptions({
                global: {
                    useUTC: false
                }
            });

            // Node-RED tx/rx
            $.getJSON('json.php?json=1', function (data) {

                // create the chart
                Highcharts.stockChart('noderedtxrx', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Node-RED tx/rx (kB)'
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
                            data: <?php echo str_replace("\"", "", $nodered_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_rx); ?>,
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
            $.getJSON('json.php?json=1', function (data) {

                // create the chart
                Highcharts.stockChart('noderedtxmotivations', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Node-RED motivations tx (kB)'
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
                            data: <?php echo str_replace("\"", "", $nodered_db_storage_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_filesystem_storage_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_db_request_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_ascapi_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_disces_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_dashboard_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_datagate_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_external_service_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_iot_service_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_mapping_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_microserviceusercreated_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_mydata_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_notificator_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_rstatistics_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_sigfox_tx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_undefined_tx); ?>,
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
            $.getJSON('json.php?json=1', function (data) {

                // create the chart
                Highcharts.stockChart('noderedrxmotivations', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Node-RED motivations rx (kB)'
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
                            data: <?php echo str_replace("\"", "", $nodered_db_storage_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_filesystem_storage_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_db_request_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_ascapi_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_disces_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_dashboard_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_datagate_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_external_service_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_iot_service_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_mapping_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_microserviceusercreated_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_mydata_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_notificator_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_rstatistics_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_sigfox_rx); ?>,
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
                            data: <?php echo str_replace("\"", "", $nodered_undefined_rx); ?>,
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
            $.getJSON('json.php?db=<?php echo $_REQUEST["db"]; ?>&table=<?php echo $_REQUEST["table"]; ?>&field=iot_apps&username=<?php echo $_REQUEST["username"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('iotapps', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT Apps (#)'
                    },
                    series: [{
                            type: 'column',
                            name: 'IoT Apps',
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
            $.getJSON('json.php?json=1', function (data) {

                // create the chart
                Highcharts.stockChart('devices', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Devices'
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
            $.getJSON('json.php?json=1', function (data) {

                // create the chart
                Highcharts.stockChart('dashboards', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Dashboards - <?php echo $_REQUEST["username"]; ?>'
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
        </script>
        <div id="noderedtxrx" style="height: 400px"></div>
        <div id="noderedtxmotivations" style="height: 400px"></div>
        <div id="noderedrxmotivations" style="height: 400px"></div>
        <div id="iotapps" style="height: 400px"></div>
        <div id="devices" style="height: 400px"></div>
        <div id="dashboards" style="height: 400px"></div>
    </body>
</html>
