<?php
/*
Snap4city -- graph-etl.php --
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
