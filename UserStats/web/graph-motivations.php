<?php
header('Access-Control-Allow-Origin: *');
// motivation for each user in the last x days
$motivation = file_get_contents("http://localhost/iot/json-general.php?db=iot&table=data&field=" . $_REQUEST["field"] . "&username=all&days=" . $_REQUEST["days"]);
// all motivations for a user in the last x days
$motivations = file_get_contents("http://localhost/iot/json-general.php?db=iot&table=data&field=all&days=" . $_REQUEST["days"]);
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
            // http://jsfiddle.net/6zYmJ/80/
            var defaultTitle = '<?php echo $_REQUEST["field"]; ?>';
            var drilldownTitle = '<?php
$name = explode("_", $_REQUEST["field"]);
echo "motivations (" . $name[count($name) - 1] . ")";
?>';
            // motivation facets
            //$.getJSON('json.php?json=1', function (data) {
            $(function () {
                var chart = new Highcharts.chart('motivations', {
                    chart: {
                        type: 'column',
                        renderTo: 'container',
                        events: {
                            drilldown: function (e) {
                                chart.setTitle({text: drilldownTitle + ' - ' + e.point.name});
                            },
                            drillup: function (e) {
                                chart.setTitle({text: defaultTitle});
                            }
                        }
                    },
                    title: {
                        text: defaultTitle
                    },
                    subtitle: {
                        text: 'Click the columns to see user\'s motivations in the last <?php echo $_REQUEST["days"]; ?> days'
                    },
                    xAxis: {
                        type: 'category'
                    },
                    yAxis: {
                        title: {
                            text: 'kB'
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
                                format: '{point.y:.1f}'
                            }
                        }
                    },

                    tooltip: {
                        headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
                        pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.2f}</b><br/>'
                    },

                    "series": [
                        {
                            "name": "<?php echo $_REQUEST["field"]; ?>",
                            "colorByPoint": true,
                            "data": <?php echo $motivation; ?>
                        }
                    ],
                    "drilldown": {
                        "series": <?php echo $motivations; ?>
                    }
                });
            });
        </script>
        <div id="motivations" style="height: 400px"></div>
    </body>
</html>