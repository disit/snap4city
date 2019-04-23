<?php
header('Access-Control-Allow-Origin: *');
$json = file_get_contents("http://localhost/iot/json.php?db=iot&table=data&field=" . $_REQUEST["field"]);
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
            // etl tx
            $.getJSON('json-general.php?db=<?php echo $_REQUEST["db"]; ?>&table=<?php echo $_REQUEST["table"]; ?>&field=tx&days=<?php echo $_REQUEST["days"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('etltx', {
                    chart: {
                        alignTicks: false
                    },

                    rangeSelector: {
                        selected: 1
                    },

                    title: {
                        text: 'ETL bytes (tx)'
                    },

                    series: [{
                            type: 'column',
                            name: 'ETL bytes (tx)',
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
                        }]
                });
            });

            // etl rx
            $.getJSON('json-general.php?db=<?php echo $_REQUEST["db"]; ?>&table=<?php echo $_REQUEST["table"]; ?>&field=rx&days=<?php echo $_REQUEST["days"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('etlrx', {
                    chart: {
                        alignTicks: false
                    },

                    rangeSelector: {
                        selected: 1
                    },

                    title: {
                        text: 'ETL bytes (rx)'
                    },

                    series: [{
                            type: 'column',
                            name: 'ETL bytes (rx)',
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
                        }]
                });
            });
            
            // etl num
            $.getJSON('json-general.php?db=<?php echo $_REQUEST["db"]; ?>&table=<?php echo $_REQUEST["table"]; ?>&field=num&days=<?php echo $_REQUEST["days"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('etlnum', {
                    chart: {
                        alignTicks: false
                    },

                    rangeSelector: {
                        selected: 1
                    },

                    title: {
                        text: 'ETL #'
                    },

                    series: [{
                            type: 'column',
                            name: 'ETL #',
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
                        }]
                });
            });
        </script>
        <div id="etltx" style="height: 400px"></div>
        <div id="etlrx" style="height: 400px"></div>
        <div id="etlnum" style="height: 400px"></div>
    </body>
</html>