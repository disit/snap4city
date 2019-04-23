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
            // r num
            $.getJSON('json-general.php?db=<?php echo $_REQUEST["db"]; ?>&table=<?php echo $_REQUEST["table"]; ?>&field=num&days=<?php echo $_REQUEST["days"]; ?>', function (data) {

                // create the chart
                Highcharts.stockChart('rnum', {
                    chart: {
                        alignTicks: false
                    },

                    rangeSelector: {
                        selected: 1
                    },

                    title: {
                        text: 'R #'
                    },

                    series: [{
                            type: 'column',
                            name: 'R #',
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
        <div id="rnum" style="height: 400px"></div>
    </body>
</html>