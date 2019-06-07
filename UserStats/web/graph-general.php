<?php
header('Access-Control-Allow-Origin: *');

$db_storage_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_db_storage_tx");
$db_storage_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_db_storage_rx");

$filesystem_storage_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_filesystem_storage_tx");
$filesystem_storage_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_filesystem_storage_rx");

$db_request_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_db_request_tx");
$db_request_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_db_request_rx");

$ascapi_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_ascapi_tx");
$ascapi_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_ascapi_rx");

$disces_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_disces_tx");
$disces_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_disces_rx");

$dashboard_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_dashboard_tx");
$dashboard_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_dashboard_rx");

$datagate_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_datagate_tx");
$datagate_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_datagate_rx");

$external_service_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_external_service_tx");
$external_service_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_external_service_rx");

$iot_service_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_iot_service_tx");
$iot_service_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_iot_service_rx");

$mapping_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_mapping_tx");
$mapping_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_mapping_rx");

$microserviceusercreated_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_microserviceusercreated_tx");
$microserviceusercreated_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_microserviceusercreated_rx");

$mydata_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_mydata_tx");
$mydata_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_mydata_rx");

$notificator_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_notificator_tx");
$notificator_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_notificator_rx");

$rstatistics_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_rstatistics_tx");
$rstatistics_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_rstatistics_rx");

$sigfox_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_sigfox_tx");
$sigfox_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_sigfox_rx");

$undefined_tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_undefined_tx");
$undefined_rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_undefined_rx");

$tx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_tx");
$rx = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=iot_rx");

$devices_public = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=devices_public");
$devices_private = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=devices_private");
$dashboards_public = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=dashboards_public");
$dashboards_private = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=dashboards_private");

$users_iot = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=users&field=nodered");
$users_dashboards = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=users&field=dashboards");
$users_devices = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=users&field=devices");
$users_total = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=users&field=total");

$dashboards_minutes = file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&field=dashboards_minutes");

// average of bytes (tx, rx) for Area Manager users until last day
$iot_tx_rx_areamanger_average = json_decode(file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&role=AreaManager&time=all"));
// average of bytes (tx, rx) for Manager users until last day
$iot_tx_rx_manger_average = json_decode(file_get_contents("http://localhost/userstats/json-general.php?db=iot&table=data&role=Manager&time=all"));
?>
<html>
    <head>
        <script src="https://code.jquery.com/jquery-3.1.1.min.js"></script>
        <script src="https://code.highcharts.com/stock/highstock.js"></script>
        <script src="https://code.highcharts.com/stock/modules/exporting.js"></script>
        <script src="https://code.highcharts.com/stock/modules/export-data.js"></script>
        <script src="https://code.highcharts.com/modules/drilldown.js"></script>
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
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT tx/rx (kB)'
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
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT motivations tx (kB)'
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
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT motivations rx (kB)'
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
            $.getJSON('json-general.php?db=iot&table=nodered&field=num', function (data) {

                // create the chart
                Highcharts.stockChart('iotapps', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT Apps'
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
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Dashboards'
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

            // Dashboards minutes
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('dashboardsMinutes', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Dashboards (minutes)'
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
                            data: <?php echo str_replace("\"", "", $dashboards_minutes); ?>,
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

            // IoT users
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('usersiot', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT users (#)'
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
                            data: <?php echo str_replace("\"", "", $users_iot); ?>,
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

            // Dashboards users
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('usersdashboards', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Dashboards users (#)'
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
                            data: <?php echo str_replace("\"", "", $users_dashboards); ?>,
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

            // Devices users
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('usersdevices', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Devices users (#)'
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
                            data: <?php echo str_replace("\"", "", $users_devices); ?>,
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

            // Total users
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                // create the chart
                Highcharts.stockChart('userstotal', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'Total users (#)'
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
                            data: <?php echo str_replace("\"", "", $users_total); ?>,
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

            // IoT messages (Area Manager)
            $.getJSON('json-general.php?db=iot&table=data&field=iot_tx&role=AreaManager', function (data) {

                // create the chart
                Highcharts.stockChart('iotmessagestxareamanager', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT messages tx (average of Area Manager users) - average: <?php echo $iot_tx_rx_areamanger_average[0][0]; ?>'
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

            // IoT messages (Area Manager)
            $.getJSON('json-general.php?db=iot&table=data&field=iot_rx&role=AreaManager', function (data) {

                // create the chart
                Highcharts.stockChart('iotmessagesrxareamanager', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT messages rx (average of Area Manager users) - average: <?php echo $iot_tx_rx_areamanger_average[0][1]; ?>'
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

            // IoT messages (Manager)
            $.getJSON('json-general.php?db=iot&table=data&field=iot_tx&role=Manager', function (data) {

                // create the chart
                Highcharts.stockChart('iotmessagestxmanager', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT messages tx (average of Manager users) - average: <?php echo $iot_tx_rx_manger_average[0][0]; ?>'
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

            // IoT messages (Manager)
            $.getJSON('json-general.php?db=iot&table=data&field=iot_rx&role=Manager', function (data) {

                // create the chart
                Highcharts.stockChart('iotmessagesrxmanager', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT messages rx (average of Manager users) - average: <?php echo $iot_tx_rx_manger_average[0][1]; ?>'
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

            // IoT reads (Area Manager)
            $.getJSON('json-general.php?db=iot&table=data&role=AreaManager&field=iot_reads', function (data) {

                // create the chart
                Highcharts.stockChart('iotreadsareamanager', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT reads (Area Manager)'
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

            // IoT reads (Manager)
            $.getJSON('json-general.php?db=iot&table=data&role=Manager&field=iot_reads', function (data) {

                // create the chart
                Highcharts.stockChart('iotreadsmanager', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT reads (Manager)'
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

            // IoT writes (Area Manager)
            $.getJSON('json-general.php?db=iot&table=data&role=AreaManager&field=iot_writes', function (data) {

                // create the chart
                Highcharts.stockChart('iotwritesareamanager', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT writes (Area Manager)'
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

            // IoT writes (Manager)
            $.getJSON('json-general.php?db=iot&table=data&role=Manager&field=iot_writes', function (data) {

                // create the chart
                Highcharts.stockChart('iotwritesmanager', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'IoT writes (Manager)'
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
            
            // ETL writes ('disit' user)
            $.getJSON('json-general.php?db=iot&table=data&field=etl_writes', function (data) {

                // create the chart
                Highcharts.stockChart('etlwrites', {
                    chart: {
                        alignTicks: false
                    },
                    rangeSelector: {
                        selected: 1
                    },
                    title: {
                        text: 'ETL writes'
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
        <div id="iottxrx" style="height: 400px"></div>
        <div id="iottxmotivations" style="height: 400px"></div>
        <div id="iotrxmotivations" style="height: 400px"></div>
        <div id="iotapps" style="height: 400px"></div>
        <div id="devices" style="height: 400px"></div>
        <div id="dashboards" style="height: 400px"></div>
        <div id="dashboardsMinutes" style="height: 400px"</div>
        <div id="usersiot" style="height: 400px"></div>
        <div id="usersdashboards" style="height: 400px"></div>
        <div id="usersdevices" style="height: 400px"></div>
        <div id="userstotal" style="height: 400px"></div>
        <div id="iotmessagestxareamanager" style="height: 400px"></div>
        <div id="iotmessagesrxareamanager" style="height: 400px"></div>
        <div id="iotmessagestxmanager" style="height: 400px"></div>
        <div id="iotmessagesrxmanager" style="height: 400px"></div>
        <div id="iotreadsareamanager" style="height: 400px"></div>
        <div id="iotreadsmanager" style="height: 400px"></div>
        <div id="iotwritesareamanager" style="height: 400px"></div>
        <div id="iotwritesmanager" style="height: 400px"></div>
        <div id="etlwrites" style="height: 400px"></div>
    </body>
</html>
