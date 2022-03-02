/* Snap4BIM.
   Copyright (C) 2022 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */

import { superserviceMapUrl } from "./config.js";

$(document).ready(
    function timeTrend(
        firstLoad,
        metricNameFromDriver,
        widgetTitleFromDriver,
        widgetHeaderColorFromDriver,
        widgetHeaderFontColorFromDriver,
        fromGisExternalContent,
        fromGisExternalContentServiceUri,
        fromGisExternalContentField,
        fromGisExternalContentRange,
        fromGisMarker,
        fromGisMapRef,
        fromGisFakeId,
        fromTrackerFlag,
        fromTrackerDay,
        fromTrackerParams,
        futureLastDate) {

        //RANGE TEMPORALI GESTIBILI DAL WIDGET: 4/HOUR, 12/HOUR, 1/DAY, 7/DAY, 30/DAY, 365/DAY (IL DRAW CANCELLA DA SOLO IL LOADING)
        var widgetName = "timeTrend";
        var hostFile = "hostFileNotKnown";
        var wsRetryActive, wsRetryTime = null;
        var thresholdObject, chartColor, chartRef, styleParameters, metricType, pattern, totValues,
            shownValues, showTitle, showHeader, hasTimer, timeRange, globalDiagramRange, myKPITimeRange,
            threshold, thresholdEval, delta, deltaPerc, originalMetricType, widgetContentColor,
            widgetHeaderColor, widgetHeaderFontColor, fontSize, fontColor, timeToReload, dataLabelsFontSize,
            dataLabelsFontColor, chartLabelsFontSize, chartLabelsFontColor, widgetParameters,
            sizeRowsWidget, desc, plotLinesArray, sm_based, rowParameters, sm_field, value, day, dayParts,
            timeParts, date, maxValue, minValue, nInterval, alarmSet, plotLineObj, metricName,
            widgetTitle, countdownRef, widgetOriginalBorderColor, serviceMapTimeRange, unitsWidget,
            webSocket, openWs, manageIncomingWsMsg, openWsConn, wsClosed, gridLineColor, chartAxesColor, infoJson = null;
        var elToEmpty = $("#" + widgetName + "_chartContainer");
        var seriesData = [];
        var valuesData = [];
        var embedWidget = true; // true or false
        var embedWidgetPolicy = '$_REQUEST[\'embedWidgetPolicy\']';
        var headerHeight = 25;
        var needWebSocket = false;
        //    var loggedRole = "<?php echo $_SESSION['loggedRole'] ?>";
        var loggedOrg = '<%= Session["loggedOrganization"] %>';
        var orgKbUrl = '<%= Session["orgKbUrl"] %>';
        var orgCentreGpsCoords = '<%= Session["orgGpsCentreLatLng"] %>';
        var now = new Date();
        var nowUTC = now.toUTCString();
        var isoDate = new Date(nowUTC).toISOString();
        var isoDateTrimmed = now.getFullYear() + "-" + (101 + now.getMonth() + "").slice(-2) + "-" + (100 + now.getDate() + "").slice(-2) + "T" + (100 + now.getHours() + "").slice(-2) + ":" + (100 + now.getMinutes() + "").slice(-2);
        var myKPIFromTimeRange = "";
        var refreshToken = '<%= Session["refreshToken"] %>';
        var accessToken = '<%= Session["accessToken"] %>';
        var dayTracker = fromTrackerDay;
        var flagTracker = fromTrackerFlag;
        var upperTimeLimitISOTrimmed = null;
        //    this["timeNavCount_"+widgetName] = 0;
        var timeNavCount = 0;
        var fromGisExternalContentRangePrevious = null;
        var fromGisExternalContentServiceUriPrevious = null;
        var fromGisExternalContentFieldPrevious = null;
        var dataFut = null;
        var upLimit = null;
        var currentWidth = null;
        var udmFromUserOptions = null;
        var udm = null;
        var titleUdm = null;
        var viewUdm, xOffsetUdm = null;
        var expandedTimeRangeFlag = false;
        var currentTimeRange = null;
        var lastDateinDataArray = null;

        console.log("Entrato in widgetTimeTrend --> " + widgetName);

        $(document).off('changeMetricFromButton_' + widgetName);
        $(document).on('changeMetricFromButton_' + widgetName,
            function (event) {
                if ((event.targetWidget === widgetName) && (event.newMetricName !== "noMetricChange")) {
                    clearInterval(countdownRef);
                    $("#" + widgetName + "_content").hide();
                    timeTrend(true, event.newMetricName, event.newTargetTitle, event.newHeaderAndBorderColor, event.newHeaderFontColor, false, null, null, null, null, null, null);
                }
            });

        $(document).off('mouseOverTimeTrendFromExternalContentGis_' + widgetName);
        $(document).on('mouseOverTimeTrendFromExternalContentGis_' + widgetName,
            function (event) {
                widgetOriginalBorderColor = $("#" + widgetName).css("border-color");
                $("#" + widgetName + "_titleDiv").html(event.widgetTitle);
                $("#" + widgetName).css("border-color", event.color1);
                $("#" + widgetName + "_header").css("background", event.color1);
                $("#" + widgetName + "_header").css("background", "-webkit-linear-gradient(left, " + event.color1 + ", " + event.color2 + ")");
                $("#" + widgetName + "_header").css("background", "-o-linear-gradient(left, " + event.color1 + ", " + event.color2 + ")");
                $("#" + widgetName + "_header").css("background", "-moz-linear-gradient(left, " + event.color1 + ", " + event.color2 + ")");
                $("#" + widgetName + "_header").css("background", "linear-gradient(to left, " + event.color1 + ", " + event.color2 + ")");
                $("#" + widgetName + "_header").css("color", "black");
            });

        $(document).off('mouseOutTimeTrendFromExternalContentGis_' + widgetName);
        $(document).on('mouseOutTimeTrendFromExternalContentGis_' + widgetName,
            function (event) {
                $("#" + widgetName + "_titleDiv").html(widgetTitle);
                $("#" + widgetName).css("border-color", widgetOriginalBorderColor);
                $("#" + widgetName + "_header").css("background", widgetHeaderColor);
                $("#" + widgetName + "_header").css("color", widgetHeaderFontColor);
            });

        $(document).off('showTimeTrendFromExternalContentGis_' + widgetName);
        $(document).on('showTimeTrendFromExternalContentGis_' + widgetName,
            function (event) {
                if (event.targetWidget === widgetName) {
                    clearInterval(countdownRef);
                    $("#" + widgetName + "_content").hide();
                    timeTrend(true, metricName, event.widgetTitle, event.color1, "black", true, event.serviceUri, event.field, event.range, event.marker, event.mapRef, event.fakeId, false, null, null, event.futureLastDate);
                }
            });

        $(document).off('restoreOriginalTimeTrendFromExternalContentGis_' + widgetName);
        $(document).on('restoreOriginalTimeTrendFromExternalContentGis_' + widgetName,
            function (event) {
                if (event.targetWidget === widgetName) {
                    clearInterval(countdownRef);
                    $("" + widgetName + "_content").hide();
                    timeTrend(true, metricName, "<?= sanitizeTitle($_REQUEST['title_w']) ?>", "<?= escapeForJS($_REQUEST['frame_color_w']) ?>", "<?= $_REQUEST['headerFontColor'] ?>", false, null, null, null, null, null, null, false, null);
                }
            });

        $(document).off('mouseOverTimeTrendFromTracker_' + widgetName);
        $(document).on('mouseOverTimeTrendFromTracker_' + widgetName,
            function (event) {
                widgetOriginalBorderColor = $("#" + widgetName).css("border-color");
                $("#" + widgetName + "_titleDiv").html(event.widgetTitle);
                $("#" + widgetName).css("border-color", event.color1);
                $("" + widgetName + "_header").css("background", event.color1);
                $("" + widgetName + "_header").css("background", "-webkit-linear-gradient(left, " + event.color1 + ", " + event.color2 + ")");
                $("" + widgetName + "_header").css("background", "-o-linear-gradient(left, " + event.color1 + ", " + event.color2 + ")");
                $("" + widgetName + "_header").css("background", "-moz-linear-gradient(left, " + event.color1 + ", " + event.color2 + ")");
                $("" + widgetName + "_header").css("background", "linear-gradient(to left, " + event.color1 + ", " + event.color2 + ")");
                $("" + widgetName + "_header").css("color", "black");
            });

        $(document).off('mouseOutTimeTrendFromTracker_' + widgetName);
        $(document).on('mouseOutTimeTrendFromTracker_' + widgetName,
            function (event) {
                $("" + widgetName + "_titleDiv").html(widgetTitle);
                $("#" + widgetName).css("border-color", widgetOriginalBorderColor);
                $("" + widgetName + "_header").css("background", widgetHeaderColor);
                $("" + widgetName + "_header").css("color", widgetHeaderFontColor);
            });

        $(document).off('showTimeTrendFromTracker_' + widgetName);
        $(document).on('showTimeTrendFromTracker_' + widgetName,
            function (event) {
                if (event.targetWidget === widgetName) {
                    clearInterval(countdownRef);
                    $("" + widgetName + "_content").hide();
                    timeTrend(true, metricName, event.widgetTitle, event.color1, "black", false, event.serviceUri, event.field, event.range, event.marker, event.mapRef, event.fakeId, true, event.day, event.rowParams);
                }
            });

        // display widget content
        function showWidgetContent(widgetName) {
            $("#" + widgetName + "_loading").css("display", "none");
            $("#" + widgetName + "_content").css("display", "block");
        }

        $(document).off('resizeHighchart_' + widgetName);
        $(document).on('resizeHighchart_' + widgetName,
            function (event) {
                showHeader = event.showHeader;
                $(widgetName + '_chartContainer').highcharts().reflow();
            });

        //Serve per il sorting dei dati ce verrano passati a Highcharts
        function compareSeriesData(a, b) {
            var x = a[0];
            var y = b[0];

            if (x < y) {
                return -1
            } else {
                if (x > y) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        $("#" + widgetName + "_content").hover(
            function () {
                $.ajax({
                    url: "../controllers/getWidgetParams.php",
                    type: "GET",
                    data: {
                        widgetName: "<?= $_REQUEST['name_w'] ?>"
                    },
                    async: true,
                    dataType: 'json',
                    success: function (widgetData) {
                        var widgetNameD = widgetData.params.name_w;
                        var showTitleD = widgetData.params.showTitle;
                        var widgetContentColorD = widgetData.params.color_w;
                        var fontSizeD = widgetData.params.fontSize;
                        var fontColorD = widgetData.params.fontColor;
                        var timeToReloadD = widgetData.params.frequency_w;
                        var hasTimerD = widgetData.params.hasTimer;
                        var chartColorD = widgetData.params.chartColor;
                        var dataLabelsFontSizeD = widgetData.params.dataLabelsFontSize;
                        var dataLabelsFontColorD = widgetData.params.dataLabelsFontColor;
                        var chartLabelsFontSizeD = widgetData.params.chartLabelsFontSize;
                        var chartLabelsFontColorD = widgetData.params.chartLabelsFontColor;
                        var appIdD = widgetData.params.appId;
                        var flowIdD = widgetData.params.flowId;
                        var nrMetricTypeD = widgetData.params.nrMetricType;
                        var webLinkD = widgetData.params.link_w;

                        if (location.href.includes("index.php") && webLinkD != "" && webLinkD != "none" && webLinkD != null) {
                            $("#" + widgetName).css("cursor", "pointer");
                        }
                    },
                    error: function () {
                        // non vengono gestiti gli errori
                    }
                }) // close ajax call
            }) // close .hover func

        $("#" + widgetName + "_content").off("click").click(
            function () {
                $.ajax({
                    url: "../controllers/getWidgetParams.php",
                    type: "GET",
                    data: {
                        widgetName: "<?= $_REQUEST['name_w'] ?>"
                    },
                    async: true,
                    dataType: 'json',
                    success: function (widgetData) {
                        showTitle = widgetData.params.showTitle;
                        widgetContentColor = widgetData.params.color_w;
                        fontSize = widgetData.params.fontSize;
                        fontColor = widgetData.params.fontColor;
                        timeToReload = widgetData.params.frequency_w;
                        hasTimer = widgetData.params.hasTimer;
                        chartColor = widgetData.params.chartColor;
                        dataLabelsFontSize = widgetData.params.dataLabelsFontSize;
                        dataLabelsFontColor = widgetData.params.dataLabelsFontColor;
                        chartLabelsFontSize = widgetData.params.chartLabelsFontSize;
                        chartLabelsFontColor = widgetData.params.chartLabelsFontColor;
                        appId = widgetData.params.appId;
                        flowId = widgetData.params.flowId;
                        nrMetricType = widgetData.params.nrMetricType;
                        var styleParametersString = widgetData.params.styleParameters;
                        styleParameters = jQuery.parseJSON(styleParametersString);
                        webLink = widgetData.params.link_w;

                        if (location.href.includes("index.php") && webLink != "" && webLink != "none") {

                            if (styleParameters != null) {
                                if (styleParameters['openNewTab'] === "yes") {
                                    var newTab = window.open(webLink);
                                    if (newTab) {
                                        newTab.focus();
                                    } else {
                                        alert('Please allow popups for this website');
                                    }
                                } else {
                                    window.location.href = webLink;
                                }
                            } else {
                                var newTab = window.open(webLink);
                                if (newTab) {
                                    newTab.focus();
                                } else {
                                    alert('Please allow popups for this website');
                                }
                            }
                        }
                    },
                    error: function () {
                        console.log("Error in opening web link.");
                    }
                });
            });

        // not used
        function isOkComputationDate(date, timeRange, lastDateInDataArray) {

            var returnFlag = false;
            var date1 = moment(date);
            var hoursToSubtract = null;
            switch (timeRange) {
                case "4 Ore":
                    hoursToSubtract = 4;
                    break;

                case "12 Ore":
                    hoursToSubtract = 12;
                    break;

                case "Giornaliera":
                    hoursToSubtract = 24;
                    break;

                case "Settimanale":
                    hoursToSubtract = 24 * 7;
                    break;

                case "Mensile":
                    hoursToSubtract = 24 * 30;
                    break;

                case "Semestrale":
                    hoursToSubtract = 24 * 180;
                    break;

                case "Annuale":
                    hoursToSubtract = 24 * 365;
                    break;
            }

            var date2 = moment(lastDateInDataArray);
            var refDate = date2.subtract(hoursToSubtract, 'hours');
            //    var diff = date2.diff(date1);
            if (date1.isAfter(refDate)) {
                returnFlag = true;
            } else {
                returnFlag = false;
            }
            return returnFlag;
        }

        function updateTimeRange(newTimeRange) {

            $.ajax({
                url: "../controllers/updateWidget.php",
                data:
                {
                    action: "updateTimeRange",
                    widgetName: "<?= $_REQUEST['name_w'] ?>",
                    //    newTimeRange: $('#<?= $_REQUEST['name_w'] ?>_header').attr('data-newTimeRange')
                    newTimeRange: newTimeRange
                },
                type: "POST",
                async: true,
                dataType: 'json',
                success: function (data) {
                    if (data.detail === 'Ok') {
                        //    button.parents('div.container-fluid').find('div.contextMenuMsgRow div.col-xs-12').html('Saved&nbsp;<i class="fa fa-thumbs-up" style="font-size:14px"></i>');
                        $('#<?= $_REQUEST[\'name_w\'] ?>_header').attr('data-currentTimeRange', newTimeRange);

                    }
                },
                error: function (errorData) {
                    console.log("Error in Updating time range.")
                }
            })
        }


        function expandTimeRange(localTimeRange, timeNavCount, udmFromUserOptions) {

            //   if (timeRange == '')
            expandedTimeRangeFlag = true;
            //   currentTimeRange = timeRange;
            switch (localTimeRange) {

                case "4 Ore":
                    timeRange = "12 Ore";
                    populateWidget("12 Ore", null, null, timeNavCount, udmFromUserOptions);
                    //   $("#<?= $_REQUEST['name_w'] ?>_timeRangeSlider").slider('setValue', 2);
                    //   timeRangeSlider.slider('setValue', 2);
                    //   timeRangeSlider[0].setAttribute('data-value', 2);
                    //   $("#<?= $_REQUEST['name_w'] ?>_timeRangeSlider").trigger("change");
                    break;

                case "12 Ore":
                    timeRange = "Giornaliera";
                    //   $("#<?= $_REQUEST['name_w'] ?>_timeRangeSlider").slider('setValue', 3);
                    //   timeRangeSlider.slider('setValue', 3);
                    //   timeRangeSlider[0].setAttribute('data-value', 2);
                    populateWidget("Giornaliera", null, null, timeNavCount, udmFromUserOptions);
                    break;

                case "Giornaliera":
                    timeRange = "Settimanale";
                    //    $("#<?= $_REQUEST['name_w'] ?>_timeRangeSlider").slider('setValue', 4);
                    //  timeRangeSlider.slider('setValue', 4);
                    populateWidget("Settimanale", null, null, timeNavCount, udmFromUserOptions);
                    break;

                case "Settimanale":
                    timeRange = "Mensile";
                    //    $("#<?= $_REQUEST['name_w'] ?>_timeRangeSlider").slider('setValue', 5);
                    //  timeRangeSlider.slider('setValue', 5);
                    //   $("#<?= $_REQUEST['name_w'] ?>_timeRangeSlider").trigger("change");
                    populateWidget("Mensile", null, null, timeNavCount, udmFromUserOptions);
                    break;

                case "Mensile":
                    timeRange = "Semestrale";
                    //    $("#<?= $_REQUEST['name_w'] ?>_timeRangeSlider").slider('setValue', 6);
                    //   timeRangeSlider.slider('setValue', 6);
                    populateWidget("Semestrale", null, null, timeNavCount, udmFromUserOptions);
                    break;

                case "Semestrale":
                    timeRange = "Annuale";
                    //    $("#<?= $_REQUEST['name_w'] ?>_timeRangeSlider").slider('setValue', 7);
                    //   timeRangeSlider.slider('setValue', 7);
                    populateWidget("Annuale", null, null, timeNavCount, udmFromUserOptions);
                    break;

                case "Annuale":
                    showWidgetContent(widgetName);
                    $("#" + widgetName + "_chartContainer").hide();
                    $('#' + widgetName + '_noDataAlertText').text("No Data Available or Last Data Older Than 1 Year.");
                    $('#' + widgetName + '_noDataAlertText').css("font-size", "14px");
                    $('#' + widgetName + '_noDataAlert').show();
                    console.log("Dati non presenti su Service Map o ultimi dati più vecchi di 1 anno.");
                    break;
            }
            updateTimeRange(timeRange);
        }


        function drawDiagram(metricData, timeRange, seriesName, fromSelector, timeZone, udm) {
            if (metricData.data.length > 0) {
                desc = metricData.data[0].commit.author.descrip;
                metricType = '<?= escapeForJS($_REQUEST[\'id_metric\']) ?>';
                seriesData = [];
                valuesData = [];
                if (metricData.data[0]) {
                    lastDateinDataArray = metricData.data[0].commit.author.computationDate;
                }
                for (var i = 0; i < metricData.data.length; i++) {
                    day = metricData.data[i].commit.author.computationDate;

                    //  if (expandedTimeRangeFlag && (isOkComputationDate(day, timeRange, lastDateinDataArray))) {

                    if ((metricData.data[i].commit.author.value !== null) && (metricData.data[i].commit.author.value !== "")) {
                        /*    var e = 1;
                            while (Math.round(metricData.data[i].commit.author.value * e) / e !== metricData.data[i].commit.author.value) e *= 10;
                            var precision = Math.log(e) / Math.LN10;    */
                        value = parseFloat(parseFloat(metricData.data[i].commit.author.value).toFixed(2));
                        flagNumeric = true;
                    } else if ((metricData.data[i].commit.author.value_perc1 !== null) && (metricData.data[i].commit.author.value_perc1 !== "")) {
                        if (value >= 100) {
                            value = parseFloat(parseFloat(metricData.data[i].commit.author.value_perc1).toFixed(0));
                        } else {
                            value = parseFloat(parseFloat(metricData.data[i].commit.author.value_perc1).toFixed(1));
                        }
                        flagNumeric = true;
                    }
                    dayParts = day.substring(0, day.indexOf(' ')).split('-');
                    if (fromSelector) {
                        timeParts = day.substr(day.indexOf(' ') + 1, 5).split(':');

                        if ((timeRange === '1/DAY') || (timeRange.includes("HOUR"))) {
                            unitsWidget = [['millisecond',
                                [1, 2, 5, 10, 20, 25, 50, 100, 200, 500]
                            ], [
                                'second',
                                [1, 2, 5, 10, 15, 30]
                            ], [
                                'minute',
                                [1, 2, 5, 10, 15, 30]
                            ], [
                                'hour',
                                [1, 2, 3, 4, 6, 8, 12]
                            ], [
                                'day',
                                [1]
                            ], [
                                'week',
                                [1]
                            ], [
                                'month',
                                [1]
                                //[1, 3, 4, 6, 8, 10, 12]
                            ], [
                                'year',
                                null
                            ]];
                            date = Date.UTC(dayParts[0], dayParts[1] - 1, dayParts[2], timeParts[0], timeParts[1]);
                        } else {
                            unitsWidget = [['millisecond',
                                [1]
                            ], [
                                'second',
                                [1, 30]
                            ], [
                                'minute',
                                [1, 30]
                            ], [
                                'hour',
                                [1, 6]
                            ], [
                                'day',
                                [1]
                            ], [
                                'week',
                                [1]
                            ], [
                                'month',
                                [1]
                            ], [
                                'year',
                                [1]
                            ]];
                            date = Date.UTC(dayParts[0], dayParts[1] - 1, dayParts[2], timeParts[0]);
                        }
                        timeParts = day.substr(day.indexOf(' ') + 1, 5).split(':');
                        date = Date.UTC(dayParts[0], dayParts[1] - 1, dayParts[2], timeParts[0], timeParts[1]);
                    } else {
                        unitsWidget = [['millisecond',
                            [1, 2, 5, 10, 20, 25, 50, 100, 200, 500]
                        ], [
                            'second',
                            [1, 2, 5, 10, 15, 30]
                        ], [
                            'minute',
                            [1, 2, 5, 10, 15, 30]
                        ], [
                            'hour',
                            [1, 2, 3, 4, 6, 8, 12]
                        ], [
                            'day',
                            [1]
                        ], [
                            'week',
                            [1]
                        ], [
                            'month',
                            [1]
                            //[1, 3, 4, 6, 8, 10, 12]
                        ], [
                            'year',
                            null
                        ]];
                        if ((timeRange === '1/DAY') || (timeRange.includes("HOUR"))) {
                            timeParts = day.substr(day.indexOf(' ') + 1, 5).split(':');
                            date = Date.UTC(dayParts[0], dayParts[1] - 1, dayParts[2], timeParts[0], timeParts[1]);
                        } else {
                            date = Date.UTC(dayParts[0], dayParts[1] - 1, dayParts[2]);
                        }
                    }
                    //   if (!Number.isNaN(date) && !Number.isNaN(value)) {
                    seriesData.push([date, value]);
                    valuesData.push(value);
                    //   }
                    //   }
                }
                seriesData.sort(compareSeriesData);
                maxValue = Math.max.apply(Math, valuesData);
                minValue = Math.min.apply(Math, valuesData);
                nInterval = parseFloat((Math.abs(maxValue - minValue) / 4).toFixed(2));
                if (flagNumeric && (thresholdObject !== null)) {
                    plotLinesArray = [];
                    var op, op1, op2 = null;

                    for (var i in thresholdObject) {
                        //Semiretta sinistra
                        if ((thresholdObject[i].op === "less") || (thresholdObject[i].op === "lessEqual")) {
                            if (thresholdObject[i].op === "less") {
                                op = "<";
                            }
                            else {
                                op = "<=";
                            }
                            plotLineObj = {
                                color: thresholdObject[i].color,
                                dashStyle: 'shortdash',
                                value: parseFloat(thresholdObject[i].thr1),
                                width: 1,
                                zIndex: 5,
                                label: {
                                    text: thresholdObject[i].desc + " " + op + " " + thresholdObject[i].thr1,
                                    y: 12
                                }
                            };
                            plotLinesArray.push(plotLineObj);
                        } else {
                            //Semiretta destra
                            if ((thresholdObject[i].op === "greater") || (thresholdObject[i].op === "greaterEqual")) {
                                if (thresholdObject[i].op === "greater") {
                                    op = ">";
                                }
                                else {
                                    op = ">=";
                                }
                                //Semiretta destra
                                plotLineObj = {
                                    color: thresholdObject[i].color,
                                    dashStyle: 'shortdash',
                                    value: parseFloat(thresholdObject[i].thr1),
                                    width: 1,
                                    zIndex: 5,
                                    label: {
                                        text: thresholdObject[i].desc + " " + op + " " + thresholdObject[i].thr1
                                    }
                                };
                                plotLinesArray.push(plotLineObj);
                            } else {
                                //Valore uguale a
                                if (thresholdObject[i].op === "equal") {
                                    op = "=";
                                    plotLineObj = {
                                        color: thresholdObject[i].color,
                                        dashStyle: 'shortdash',
                                        value: parseFloat(thresholdObject[i].thr1),
                                        width: 1,
                                        zIndex: 5,
                                        label: {
                                            text: thresholdObject[i].desc + " " + op + " " + thresholdObject[i].thr1
                                        }
                                    };
                                    plotLinesArray.push(plotLineObj);
                                } else {
                                    //Valore diverso da
                                    if (thresholdObject[i].op === "notEqual") {
                                        op = "!=";
                                        plotLineObj = {
                                            color: thresholdObject[i].color,
                                            dashStyle: 'shortdash',
                                            value: parseFloat(thresholdObject[i].thr1),
                                            width: 1,
                                            zIndex: 5,
                                            label: {
                                                text: thresholdObject[i].desc + " " + op + " " + thresholdObject[i].thr1
                                            }
                                        };
                                        plotLinesArray.push(plotLineObj);
                                    } else {
                                        //Intervallo bi-limitato
                                        switch (thresholdObject[i].op) {
                                            case "intervalOpen":
                                                op1 = ">";
                                                op2 = "<";
                                                break;

                                            case "intervalClosed":
                                                op1 = ">=";
                                                op2 = "<=";
                                                break;

                                            case "intervalLeftOpen":
                                                op1 = ">";
                                                op2 = "<=";
                                                break;

                                            case "intervalRightOpen":
                                                op1 = ">=";
                                                op2 = "<";
                                                break;
                                        }
                                        plotLineObj = {
                                            color: thresholdObject[i].color,
                                            dashStyle: 'shortdash',
                                            value: parseFloat(thresholdObject[i].thr1),
                                            width: 1,
                                            zIndex: 5,
                                            label: {
                                                text: thresholdObject[i].desc + " " + op1 + " " + thresholdObject[i].thr1
                                            }
                                        };
                                        plotLinesArray.push(plotLineObj);
                                        plotLineObj = {
                                            color: thresholdObject[i].color,
                                            dashStyle: 'shortdash',
                                            value: parseFloat(thresholdObject[i].thr2),
                                            width: 1,
                                            zIndex: 5,
                                            label: {
                                                text: thresholdObject[i].desc + " " + op2 + " " + thresholdObject[i].thr2,
                                                y: 12
                                            }
                                        };
                                        plotLinesArray.push(plotLineObj);
                                    }
                                }
                            }
                        }
                    }
                }

                if (firstLoad !== false) {
                    showWidgetContent(widgetName);
                    $('#' + widgetName + '_noDataAlert').hide();
                    $("#" + widgetName + "_chartContainer").show();
                }
                else {
                    elToEmpty.empty();
                    $('#' + widgetName + '_noDataAlert').hide();
                    $("#" + widgetName + "_chartContainer").show();
                }
                //    if (udm != null && viewUdm != null && viewUdm != "no" && seriesData.length > 0) {
                if (udm != null && udm != "null" && viewUdm != "no" && seriesData.length > 0) {
                    if (xOffsetUdm != null) {
                        titleUdm = JSON.parse('{ "text": "' + udm + '", "align": "high", "offset": 0, "rotation": 0, "x": ' + xOffsetUdm + ' }');
                    } else {
                        titleUdm = JSON.parse('{ "text": "' + udm + '", "align": "high", "offset": 0, "rotation": 0, "x": 25 }');
                    }
                } else {
                    titleUdm = '';
                }
                if (metricType === "isAlive") {
                    //Calcolo del vettore delle zones
                    var myZonesArray = [];
                    var newZoneItem = null;
                    var areaColor = null;
                    for (var i = 1; i < seriesData.length; i++) {
                        switch (seriesData[i - 1][1]) {
                            case 2:
                                areaColor = '#ff0000';
                                break;

                            case 4:
                                areaColor = '#f96f06';
                                break;

                            case 6:
                                areaColor = '#ffcc00';
                                break;

                            case 8:
                                areaColor = '#00cc00';
                                break;

                        }
                        if (i < seriesData.length - 1) {
                            newZoneItem = {
                                value: seriesData[i][0],
                                color: areaColor
                            };
                        }
                        else {
                            newZoneItem = {
                                color: areaColor
                            };
                        }
                        myZonesArray.push(newZoneItem);
                    }
                    //Disegno del diagramma
                    chartRef = Highcharts.chart(widgetName + '_chartContainer', {
                        credits: {
                            enabled: false
                        },
                        chart: {
                            backgroundColor: 'transparent',
                            type: 'areaspline',
                            events: {
                                load: function () {
                                    $("#" + wigetName + "_chartColorMenuItem").trigger('chartCreated');
                                    $("#" + wigetName + "_chartPlaneColorMenuItem").trigger('chartCreated');
                                    $("#" + wigetName + "_chartLabelsColorMenuItem").trigger('chartCreated');
                                    $("#" + wigetName + "_chartAxesColorMenuItem").trigger('chartCreated');
                                }
                            }
                        },
                        exporting: {
                            enabled: false
                        },
                        title: {
                            text: ''
                        },
                        xAxis: {
                            type: 'datetime',
                            units: unitsWidget,
                            /*    title:
                                    {
                                        enabled: true,
                                        text: "Time - zone: " + timeZone,
                                        style: {
                                            fontFamily: 'Montserrat',
                                            color: chartLabelsFontColor,
                                            fontSize: fontSize + "px"
                                        }
                                    },  */
                            labels: {
                                enabled: true,
                                useHTML: true,
                                style: {
                                    fontFamily: 'Montserrat',
                                    color: chartLabelsFontColor,
                                    fontSize: fontSize + "px",
                                    /*"text-shadow": "1px 1px 1px rgba(0,0,0,0.12)",
                                    "textOutline": "1px 1px contrast"*/
                                }
                            }
                        },
                        yAxis: {
                            title: titleUdm,
                            min: minValue,
                            max: 8,
                            //    tickInterval: nInterval,
                            plotLines: plotLinesArray,
                            gridLineColor: gridLineColor,
                            lineWidth: 1,
                            labels: {
                                enabled: true,
                                style: {
                                    fontFamily: 'Montserrat',
                                    color: chartLabelsFontColor,
                                    fontSize: fontSize + "px",
                                    /*"text-shadow": "1px 1px 1px rgba(0,0,0,0.12)",
                                    "textOutline": "1px 1px contrast"*/
                                },
                                formatter: function () {
                                    switch (this.value) {
                                        case 2:
                                            return "Time out";
                                            break;
                                        case 4:
                                            return "Error";
                                            break;
                                        case 6:
                                            return "Token not found";
                                            break;
                                        case 8:
                                            return "Ok";
                                            break;
                                        default:
                                            return null;
                                            break;
                                    }
                                    return this.value;
                                }
                            }
                        },
                        tooltip: {
                            xDateFormat: '%A, %e %b %Y, %H:%M',
                            valueSuffix: ''
                        },
                        series: [{
                            showInLegend: false,
                            name: seriesName,
                            data: seriesData,
                            step: 'left',
                            zoneAxis: 'x',
                            zones: myZonesArray,
                            color: chartColor,
                            fillColor: {
                                linearGradient: {
                                    x1: 0,
                                    y1: 0,
                                    x2: 0,
                                    y2: 1
                                },
                                stops: [
                                    [0, Highcharts.Color(chartColor).setOpacity(0.75).get('rgba')],
                                    [1, Highcharts.Color(chartColor).setOpacity(0.25).get('rgba')]
                                ]
                            }
                        }]
                    })
                }
                else {
                    //Disegno del diagramma
                    //    $('#<?= str_replace('.', '_', str_replace('-', '_', $_REQUEST['name_w'])) ?>_chartContainer').highcharts({
                    chartRef = Highcharts.chart(widgetName + '_chartContainer', {
                        credits: {
                            enabled: false
                        },
                        chart: {
                            backgroundColor: 'transparent',
                            type: 'areaspline',
                            events: {
                                load: function () {
                                    $("#" + widgetName + "_chartColorMenuItem").trigger('chartCreated');
                                    $("#" + widgetName + "_chartPlaneColorMenuItem").trigger('chartCreated');
                                    $("#" + widgetName + "_chartLabelsColorMenuItem").trigger('chartCreated');
                                    $("#" + widgetName + "_chartAxesColorMenuItem").trigger('chartCreated');
                                }
                            }
                        },
                        plotOptions: {
                            spline: {

                            }
                        },
                        exporting: {
                            enabled: false
                        },
                        title: {
                            text: ''
                        },
                        xAxis: {
                            type: 'datetime',
                            units: unitsWidget,
                            className: 'timeTrendXAxis',
                            lineColor: chartAxesColor,
                            /*   title:
                                   {
                                       enabled: true,
                                       text: "Time - zone: " + timeZone,
                                       style: {
                                           fontFamily: 'Montserrat',
                                           color: chartLabelsFontColor,
                                           fontSize: fontSize + "px"
                                       }
                                   },  */
                            labels: {
                                enabled: true,
                                style: {
                                    fontFamily: 'Montserrat',
                                    color: chartLabelsFontColor,
                                    fontSize: fontSize + "px",
                                    /*"text-shadow": "1px 1px 1px rgba(0,0,0,0.12)",
                                    "textOutline": "1px 1px contrast"*/
                                }
                            }
                        },
                        yAxis: {
                            title: titleUdm,
                            min: minValue,
                            max: maxValue,
                            //    tickInterval: nInterval,
                            plotLines: plotLinesArray,
                            lineColor: chartAxesColor,
                            lineWidth: 1,
                            className: 'timeTrendYAxis',
                            gridLineColor: gridLineColor,
                            labels: {
                                enabled: true,
                                style: {
                                    fontFamily: 'Montserrat',
                                    color: chartLabelsFontColor,
                                    fontSize: fontSize + "px",
                                    /*"text-shadow": "1px 1px 1px rgba(0,0,0,0.12)",
                                    "textOutline": "1px 1px contrast"*/
                                }
                            }
                        },
                        tooltip:
                        {
                            xDateFormat: '%A, %e %b %Y, %H:%M',
                            valueSuffix: ''
                        },
                        series: [{
                            showInLegend: false,
                            name: seriesName,
                            data: seriesData,
                            color: chartColor,
                            fillColor: {
                                linearGradient: {
                                    x1: 0,
                                    y1: 0,
                                    x2: 0,
                                    y2: 1
                                },
                                stops: [
                                    [0, Highcharts.Color(chartColor).setOpacity(0.75).get('rgba')],
                                    [1, Highcharts.Color(chartColor).setOpacity(0.25).get('rgba')]
                                ]
                            }
                        }]
                    })
                }
            }
            else {
                showWidgetContent(widgetName);
                $("#" + widgetName + "_chartContainer").hide();
                $('#' + widgetName + '_noDataAlert').show();
            }
            /*   if ($("#" + widgetName + "_content").css("display") == "none") {
                   $("#" + widgetName + "_content").css("display", "block");
               }*/
            showWidgetContent(widgetName);

        }


        /**
         * @param originalData {object} Object returned from service uri
         * @param field Type of data to be loaded ex. concentration/temperature...
         * @param udmFromUserOptions Metric unit used for this type of data ex. cars/km
         */
        function convertDataFromSmToDm(originalData, field, udmFromUserOptions) {
            var singleOriginalData, singleData, convertedDate = null;
            var convertedData = {
                data: []
            };
            var originalDataWithNoTime = 0;
            var originalDataNotNumeric = 0;
            if (originalData.hasOwnProperty("realtime")) {
                if (originalData.realtime.hasOwnProperty("results")) {
                    if (originalData.realtime.results.hasOwnProperty("bindings")) {
                        if (originalData.realtime.results.bindings.length > 0) {
                            let propertyJson = "";
                            if (originalData.hasOwnProperty("BusStop")) {
                                propertyJson = originalData.BusStop;
                            }
                            else {
                                if (originalData.hasOwnProperty("Sensor")) {
                                    propertyJson = originalData.Sensor;
                                }
                                else {
                                    if (originalData.hasOwnProperty("Service")) {
                                        propertyJson = originalData.Service;
                                    }
                                    else {
                                        propertyJson = originalData.Services;
                                    }
                                }
                            }
                            if (udmFromUserOptions != null) {
                                udm = udmFromUserOptions;
                            } else {
                                if (propertyJson.features[0].properties.realtimeAttributes[field] != null) {
                                    if (propertyJson.features[0].properties.realtimeAttributes[field].value_unit != null) {
                                        udm = propertyJson.features[0].properties.realtimeAttributes[field].value_unit;
                                    }
                                }
                            }
                            for (var i = 0; i < originalData.realtime.results.bindings.length; i++) {
                                singleData = {
                                    commit: {
                                        author: {
                                            IdMetric_data: null, //Si può lasciare null, non viene usato dal widget
                                            computationDate: null,
                                            value_perc1: null, //Non lo useremo mai
                                            value: null,
                                            descrip: null, //Mettici il nome della metrica splittato
                                            threshold: null, //Si può lasciare null, non viene usato dal widget
                                            thresholdEval: null //Si può lasciare null, non viene usato dal widget
                                        },
                                        range_dates: 0//Si può lasciare null, non viene usato dal widget
                                    }
                                };
                                singleOriginalData = originalData.realtime.results.bindings[i];
                                if (singleOriginalData.hasOwnProperty("updating")) {
                                    convertedDate = singleOriginalData.updating.value;
                                }
                                else {
                                    if (singleOriginalData.hasOwnProperty("measuredTime")) {
                                        convertedDate = singleOriginalData.measuredTime.value;
                                    }
                                    else {
                                        if (singleOriginalData.hasOwnProperty("instantTime")) {
                                            convertedDate = singleOriginalData.instantTime.value;
                                        }
                                        else {
                                            originalDataWithNoTime++;
                                            continue;
                                        }
                                    }
                                }
                                // TIME-ZONE CONVERSION
                                var localTimeZone = moment.tz.guess();
                                var momentDateTime = moment(convertedDate);
                                var localDateTime = momentDateTime.tz(localTimeZone).format();
                                localDateTime = localDateTime.replace("T", " ");
                                var plusIndexLocal = localDateTime.indexOf("+");
                                localDateTime = localDateTime.substr(0, plusIndexLocal);
                                convertedDate = convertedDate.replace("T", " ");
                                var plusIndex = convertedDate.indexOf("+");
                                convertedDate = convertedDate.substr(0, plusIndex);
                                if (localDateTime == "") {
                                    singleData.commit.author.computationDate = convertedDate;
                                } else {
                                    singleData.commit.author.computationDate = localDateTime;
                                }
                                if (singleOriginalData[field] !== undefined) {
                                    if (!isNaN(parseFloat(singleOriginalData[field].value))) {
                                        singleData.commit.author.value = parseFloat(singleOriginalData[field].value);
                                    } else {
                                        originalDataNotNumeric++;
                                        continue;
                                    }
                                } else {
                                    originalDataNotNumeric++;
                                    continue;
                                }
                                convertedData.data.push(singleData);
                            }
                            return convertedData;
                        }
                        else {
                            return false;
                        }
                    }
                    else {
                        return false;
                    }
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }

        function convertDataFromPersonalDataToDm(originalData) {
            var singleOriginalData, singleData, convertedDate = null;
            var convertedData = {
                data: []
            };
            for (var i = 0; i < originalData.length; i++) {
                singleData = {
                    commit: {
                        author: {
                            IdMetric_data: null, //Si può lasciare null, non viene usato dal widget
                            computationDate: null,
                            value_perc1: null, //Non lo useremo mai
                            value: null,
                            descrip: null, //Mettici il nome della metrica splittato
                            threshold: null, //Si può lasciare null, non viene usato dal widget
                            thresholdEval: null //Si può lasciare null, non viene usato dal widget
                        },
                        range_dates: 0//Si può lasciare null, non viene usato dal widget
                    }
                };
                singleOriginalData = originalData[i];
                convertedDate = new Date(singleOriginalData.dataTime); //2001-11-23 03:08:46
                convertedDate = convertedDate.getFullYear() + "-" + parseInt(convertedDate.getMonth() + 1) + "-" + convertedDate.getDate() + " " + convertedDate.getHours() + ":" + convertedDate.getMinutes() + ":" + convertedDate.getSeconds();
                singleData.commit.author.computationDate = convertedDate;
                if (!isNaN(parseFloat(singleOriginalData.variableValue))) {
                    singleData.commit.author.value = parseFloat(singleOriginalData.variableValue);
                }
                else {
                    singleData.commit.author.value = singleOriginalData.variableValue;
                }
                convertedData.data.push(singleData);
            }
            return convertedData;
        }


        function convertDataFromTimeNavToDm(originalData, field, udmFromUserOptions) {
            var singleOriginalData, singleData, convertedDate, futureDate = null;
            var convertedData = {
                data: []
            };
            var originalDataWithNoTime = 0;
            var originalDataNotNumeric = 0;
            if (originalData.hasOwnProperty("realtime")) {
                if (originalData.realtime.hasOwnProperty("results")) {
                    if (originalData.realtime.results.hasOwnProperty("bindings")) {
                        if (originalData.realtime.results.bindings.length > 0) {
                            let propertyJson = "";
                            if (originalData.hasOwnProperty("BusStop")) {
                                propertyJson = originalData.BusStop;
                            } else {
                                if (originalData.hasOwnProperty("Sensor")) {
                                    propertyJson = originalData.Sensor;
                                } else {
                                    if (originalData.hasOwnProperty("Service")) {
                                        propertyJson = originalData.Service;
                                    } else {
                                        propertyJson = originalData.Services;
                                    }
                                }
                            }

                            if (udmFromUserOptions != null) {
                                udm = udmFromUserOptions;
                            } else {
                                if (propertyJson.features[0].properties.realtimeAttributes[field] != null) {
                                    if (propertyJson.features[0].properties.realtimeAttributes[field].value_unit != null) {
                                        udm = propertyJson.features[0].properties.realtimeAttributes[field].value_unit;
                                    }
                                }
                            }
                            for (var i = 0; i < originalData.realtime.results.bindings.length; i++) {
                                singleData = {
                                    commit: {
                                        author: {
                                            IdMetric_data: null, //Si può lasciare null, non viene usato dal widget
                                            computationDate: null,
                                            futureDate: null,
                                            value_perc1: null, //Non lo useremo mai
                                            value: null,
                                            descrip: null, //Mettici il nome della metrica splittato
                                            threshold: null, //Si può lasciare null, non viene usato dal widget
                                            thresholdEval: null //Si può lasciare null, non viene usato dal widget
                                        },
                                        range_dates: 0//Si può lasciare null, non viene usato dal widget
                                    }
                                };
                                singleOriginalData = originalData.realtime.results.bindings[i];
                                if (singleOriginalData.hasOwnProperty("updating")) {
                                    convertedDate = singleOriginalData.updating.value;
                                } else {
                                    if (singleOriginalData.hasOwnProperty("measuredTime")) {
                                        convertedDate = singleOriginalData.measuredTime.value;
                                    } else {
                                        if (singleOriginalData.hasOwnProperty("instantTime")) {
                                            convertedDate = singleOriginalData.instantTime.value;
                                        } else {
                                            originalDataWithNoTime++;
                                            continue;
                                        }
                                    }
                                }
                                // TIME-ZONE CONVERSION
                                var localTimeZone = moment.tz.guess();
                                var momentDateTime = moment(convertedDate);
                                var localDateTime = momentDateTime.tz(localTimeZone).format();
                                localDateTime = localDateTime.replace("T", " ");
                                var plusIndexLocal = localDateTime.indexOf("+");
                                localDateTime = localDateTime.substr(0, plusIndexLocal);
                                convertedDate = convertedDate.replace("T", " ");
                                var plusIndex = convertedDate.indexOf("+");
                                convertedDate = convertedDate.substr(0, plusIndex);
                                if (singleOriginalData[field].hasOwnProperty("valueDate")) {
                                    futureDate = singleOriginalData[field].valueDate.replace("T", " ");
                                    var plusIndexFuture = futureDate.indexOf("+");
                                    futureDate = futureDate.substr(0, plusIndexFuture);
                                    var momentDateTimeFuture = moment(futureDate);
                                    var localDateTimeFuture = momentDateTimeFuture.tz(localTimeZone).format();
                                    localDateTimeFuture = localDateTimeFuture.replace("T", " ");
                                    var plusIndexLocalFuture = localDateTimeFuture.indexOf("+");
                                    localDateTimeFuture = localDateTimeFuture.substr(0, plusIndexLocalFuture);
                                }
                                if (localDateTime == "") {
                                    singleData.commit.author.computationDate = convertedDate;
                                    singleData.commit.author.futureDate = futureDate;
                                } else {
                                    singleData.commit.author.computationDate = localDateTime;
                                    singleData.commit.author.futureDate = localDateTimeFuture;
                                }
                                if (singleOriginalData[field] !== undefined) {
                                    if (!isNaN(parseFloat(singleOriginalData[field].value))) {
                                        singleData.commit.author.value = parseFloat(singleOriginalData[field].value);
                                    } else {
                                        originalDataNotNumeric++;
                                        continue;
                                    }
                                } else {
                                    originalDataNotNumeric++;
                                    continue;
                                }
                                convertedData.data.push(singleData);
                            }
                            if (convertedData.data.length > 0) {
                                return convertedData;
                            } else {
                                convertedData.data.push(singleData)
                                return convertedData;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        // not used function
        function zeroSpanDateAndHour(number) { }

        function resizeWidget() {
            setWidgetLayout(hostFile, widgetName, widgetContentColor, widgetHeaderColor, widgetHeaderFontColor, showHeader, headerHeight, hasTimer);
            var bodyHeight = parseInt($("#" + widgetName + "_div").prop("offsetHeight") - widgetHeaderHeight);
            $("#" + widgetName + "_loading").css("height", bodyHeight + "px");
            $("#" + widgetName + "_content").css("height", bodyHeight + "px");
            if (infoJson != "fromTracker" || fromGisExternalContent === true) {
                var titleDiv = $('#' + widgetName + '_titleDiv');
                //    $('#'+ widgetName +'_infoButtonDiv').css("width", "3.5%");
                //    $('#'+ widgetName +'_countdownContainerDiv').css("width", "3%");
                $('#' + widgetName + '_prevButton').css("color", widgetHeaderFontColor);
                $('#' + widgetName + '_nextButton').css("color", widgetHeaderFontColor);
                titleDiv.css("width", "70%");

                if ($('#' + widgetName + '_header').width() < 400) {
                    titleDiv.css("width", "65%");
                    $("#" + widgetName + "_timeControlsContainer").css("width", "19%");
                    $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                } else if ($('#' + widgetName + '_header').width() < 480) {
                    titleDiv.css("width", "74%");
                    $("#" + widgetName + "_timeControlsContainer").css("width", "14%");
                    $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                } else if ($('#' + widgetName + '_header').width() < 560) {
                    titleDiv.css("width", "75%");
                    $("#" + widgetName + "_timeControlsContainer").css("width", "15%");
                    $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                } else if ($('#' + widgetName + '_header').width() < 700) {
                    titleDiv.css("width", "80%");
                    $("#" + widgetName + "_timeControlsContainer").css("width", "11%");
                    $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                } else if ($('#' + widgetName + '_header').width() < 900) {
                    titleDiv.css("width", "84%");
                    $("#" + widgetName + "_timeControlsContainer").css("width", "9%");
                    $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                } else if ($('#' + widgetName + '_header').width() < 1000) {
                    titleDiv.css("width", "85%");
                    $("#" + widgetName + "_timeControlsContainer").css("width", "8%");
                    $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                } else if ($('#' + widgetName + '_header').width() < 1050) {
                    titleDiv.css("width", "85%");
                    $("#" + widgetName + "_timeControlsContainer").css("width", "7%");
                    $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                } else {
                    titleDiv.css("width", "87%");
                    $("#" + widgetName + "_timeControlsContainer").css("width", "7%");
                    $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                }
            }
        }

        function getUpperTimeLimit(hours) {
            let now = new Date();
            let timeZoneOffsetHours = now.getTimezoneOffset() / 60;
            let upperTimeLimit = now.setHours(now.getHours() - hours - timeZoneOffsetHours);
            let upperTimeLimitUTC = new Date(upperTimeLimit).toUTCString();
            let upperTimeLimitISO = new Date(upperTimeLimitUTC).toISOString();
            let upperTimeLimitISOTrim = upperTimeLimitISO.substring(0, isoDate.length - 5);
            return upperTimeLimitISOTrim;
            //    myKPITimeRange = "&from=" + myKPIFromTimeRangeISOTrimmed + "&to=" + isoDateTrimmed;
        }

        function convertFromMomentToTime(momentDate) {
            var momentDateTime = momentDate.format();
            //  momentDateTime = momentDateTime.replace("T", " ");
            var plusIndexLocal = momentDateTime.indexOf("+");
            momentDateTime = momentDateTime.substr(0, plusIndexLocal);
            var convertedDateTime = momentDateTime;
            return convertedDateTime;
        }

        function populateWidget(localTimeRange, kpiTracker, timeNavDirection, timeCount, dateInFuture, udmFromUserOptions) {
            if (fromGisExternalContent) {
                // Reset Time Navigation
                if (fromGisExternalContentRangePrevious !== fromGisExternalContentRange || fromGisExternalContentFieldPrevious != fromGisExternalContentField || fromGisExternalContentServiceUriPrevious != fromGisExternalContentServiceUri) {
                    timeNavCount = 0;
                    timeCount = 0;
                    fromGisExternalContentRangePrevious = fromGisExternalContentRange;
                    fromGisExternalContentFieldPrevious = fromGisExternalContentField;
                    fromGisExternalContentServiceUriPrevious = fromGisExternalContentServiceUri;
                    dataFut = null;
                    upLimit = null;
                }

                $('#' + widgetName + '_infoButtonDiv a.info_source').hide();
                $('#' + widgetName + '_infoButtonDiv i.gisDriverPin').show();
                $('#' + widgetName + '_infoButtonDiv i.gisDriverPin').off('click');
                $('#' + widgetName + '_infoButtonDiv i.gisDriverPin').click(function () {
                    if ($(this).attr('data-onMap') === 'false') {
                        if (fromGisMapRef.hasLayer(fromGisMarker)) {
                            fromGisMarker.fire('click');
                        }
                        else {
                            fromGisMapRef.addLayer(fromGisMarker);
                            fromGisMarker.fire('click');
                        }
                        $(this).attr('data-onMap', 'true');
                        $(this).html('near_me');
                        $(this).css('color', 'white');
                        $(this).css('text-shadow', '2px 2px 4px black');
                    }
                    else {
                        fromGisMapRef.removeLayer(fromGisMarker);
                        $(this).attr('data-onMap', 'false');
                        $(this).html('navigation');
                        $(this).css('color', '#337ab7');
                        $(this).css('text-shadow', 'none');
                    }
                });

                switch (fromGisExternalContentRange) {
                    case "4/HOUR":
                        serviceMapTimeRange = "fromTime=4-hour";
                        //    var deltaT = 4 + parseInt(timeCount) * 4;
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-hour";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(4 * timeCount);
                        break;
                    case "1/DAY":
                        serviceMapTimeRange = "fromTime=1-day";
                        //    var deltaT = 1 + parseInt(timeCount);
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-day";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(24 * timeCount);
                        break;
                    case "7/DAY":
                        serviceMapTimeRange = "fromTime=7-day";
                        //    var deltaT = 7 + parseInt(timeCount) * 7;
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-day";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(7 * 24 * timeCount);
                        break;
                    case "30/DAY":
                        serviceMapTimeRange = "fromTime=30-day";
                        //    var deltaT = 30 + parseInt(timeCount) * 30;
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-day";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(30 * 24 * timeCount);
                        break;

                    case "180/DAY":
                        serviceMapTimeRange = "fromTime=180-day";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(180 * 24 * timeCount);

                        break;

                    case "365/DAY":
                        serviceMapTimeRange = "fromTime=365-day";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(365 * 24 * timeCount);

                        break;
                    default:
                        serviceMapTimeRange = "fromTime=1-day";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(24 * timeCount);
                        break;
                }

                $.ajax({
                    // returns an obj that contains all the related data to that selected uri = "http://www.disit.org/km4city/resource/METRO28"
                    // example url= "https://www.snap4city.org/superservicemap/api/v1/?serviceUri=http://www.disit.org/km4city/resource/METRO28&fromTime=7-day&toTime=2021-04-29T12:49:34&valueName=avgTime"
                    url: superserviceMapUrl + encodeServiceUri(fromGisExternalContentServiceUri) + "&" + serviceMapTimeRange + "&toTime=" + upperTimeLimitISOTrimmed + "&valueName=" + fromGisExternalContentField,
                    type: "GET",
                    data: {},
                    async: true,
                    dataType: 'json',
                    success: function (originalData) {
                        var convertedData = convertDataFromSmToDm(originalData, fromGisExternalContentField, udm);
                        if (convertedData) {
                            if (convertedData.data.length > 0) {
                                var localTimeZone = moment.tz.guess();
                                var momentDateTime = moment();
                                var localDateTime = momentDateTime.tz(localTimeZone).format();
                                localDateTime = localDateTime.replace("T", " ");
                                var plusIndexLocal = localDateTime.indexOf("+");
                                localDateTime = localDateTime.substr(0, plusIndexLocal);
                                var localTimeZoneString = "";
                                if (localDateTime == "") {
                                    localTimeZoneString = "(not recognized) --> Europe/Rome"
                                } else {
                                    localTimeZoneString = localTimeZone;
                                }

                                drawDiagram(convertedData, fromGisExternalContentRange, fromGisExternalContentField, true, localTimeZoneString, udm);

                                upLimit = convertedData.data[0].commit.author.computationDate;
                                if (timeNavCount < 0) {
                                    if (moment(upLimit).isBefore(moment(dateInFuture))) {

                                    } else {
                                        $("#" + widgetName + "_nextButton").hide();
                                    }
                                }
                            }
                            else {
                                showWidgetContent(widgetName);
                                $("#" + widgetName + "_chartContainer").hide();
                                $('#' + widgetName + '_noDataAlertText').text("No Data Available in the Selected Time-Range.");
                                $('#' + widgetName + '_noDataAlert').show();
                                console.log("Dati non disponibili da Service Map");
                            }
                        }
                        else {
                            showWidgetContent(widgetName);
                            $("#" + widgetName + "_chartContainer").hide();
                            $('#' + widgetName + '_noDataAlertText').text("No Data Available in the Selected Time-Range.");
                            $('#' + widgetName + '_noDataAlert').show();
                            console.log("Dati non disponibili da Service Map");
                        }
                    },
                    error: function (data) {
                        showWidgetContent(widgetName);
                        $("#" + widgetName + "_chartContainer").hide();
                        $('#' + widgetname + '_noDataAlertText').text("API Error in Data Retrieval.");
                        $('#' + widgetname + '_noDataAlert').show();
                        console.log("Errore in scaricamento dati da Service Map");
                        console.log(JSON.stringify(data));
                    }
                })
            } else {
                $('#' + widgetname + '_infoButtonDiv i.gisDriverPin').hide();
                $('#' + widgetname + '_infoButtonDiv a.info_source').show();
                switch (localTimeRange) {

                    case "4 Ore":
                        serviceMapTimeRange = "fromTime=4-hour";
                        //    var deltaT = 4 + parseInt(timeCount) * 4;
                        //    serviceMapTimeRange = "fromTime=" + deltaT.toString() + "-day";
                        globalDiagramRange = "4/HOUR";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(4 * timeCount);
                        if (flagTracker === true) {
                            myKPITimeRange = "&from=" + dayTracker + "T00:00:00&to=" + dayTracker + "T23:59:59";
                        } else {
                            var now = new Date();
                            myKPIFromTimeRange = now.setHours(now.getHours() - 4);
                            var myKPIFromTimeRangeUTC = new Date(myKPIFromTimeRange).toUTCString();
                            var myKPIFromTimeRangeISO = new Date(myKPIFromTimeRangeUTC).toISOString();
                            var myKPIFromTimeRangeISOTrimmed = myKPIFromTimeRangeISO.substring(0, isoDate.length - 8);
                            //   myKPITimeRange = "&from=" + myKPIFromTimeRangeISOTrimmed + "&to=" + isoDateTrimmed;
                            var myKPIFromTimeRangeNew = moment(upperTimeLimitISOTrimmed).subtract(4, 'hours');
                            var myKPIFromTimeRangeNewTrimmed = convertFromMomentToTime(myKPIFromTimeRangeNew);
                            myKPITimeRange = "&from=" + myKPIFromTimeRangeNewTrimmed + "&to=" + upperTimeLimitISOTrimmed;
                        }
                        break;

                    case "12 Ore":
                        serviceMapTimeRange = "fromTime=12-hour";
                        //    var deltaT = 12 + parseInt(timeCount) * 12;
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-hour";
                        globalDiagramRange = "12/HOUR";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(12 * timeCount);
                        if (flagTracker === true) {
                            myKPITimeRange = "&from=" + dayTracker + "T00:00:00&to=" + dayTracker + "T23:59:59";
                        } else {
                            var now = new Date();
                            myKPIFromTimeRange = now.setHours(now.getHours() - 12);
                            var myKPIFromTimeRangeUTC = new Date(myKPIFromTimeRange).toUTCString();
                            var myKPIFromTimeRangeISO = new Date(myKPIFromTimeRangeUTC).toISOString();
                            var myKPIFromTimeRangeISOTrimmed = myKPIFromTimeRangeISO.substring(0, isoDate.length - 8);
                            //   myKPITimeRange = "&from=" + myKPIFromTimeRangeISOTrimmed + "&to=" + isoDateTrimmed;
                            var myKPIFromTimeRangeNew = moment(upperTimeLimitISOTrimmed).subtract(12, 'hours');
                            var myKPIFromTimeRangeNewTrimmed = convertFromMomentToTime(myKPIFromTimeRangeNew);
                            myKPITimeRange = "&from=" + myKPIFromTimeRangeNewTrimmed + "&to=" + upperTimeLimitISOTrimmed;
                        }
                        break;

                    case "Giornaliera":
                        serviceMapTimeRange = "fromTime=1-day";
                        //    var deltaT = 1 + parseInt(timeCount);
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-day";
                        globalDiagramRange = "1/DAY";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(24 * timeCount);
                        if (flagTracker === true) {
                            myKPITimeRange = "&from=" + dayTracker + "T00:00:00&to=" + dayTracker + "T23:59:59";
                        } else {
                            var now = new Date();
                            myKPIFromTimeRange = now.setHours(now.getHours() - 24);
                            var myKPIFromTimeRangeUTC = new Date(myKPIFromTimeRange).toUTCString();
                            var myKPIFromTimeRangeISO = new Date(myKPIFromTimeRangeUTC).toISOString();
                            var myKPIFromTimeRangeISOTrimmed = myKPIFromTimeRangeISO.substring(0, isoDate.length - 8);
                            //    myKPITimeRange = "&from=" + myKPIFromTimeRangeISOTrimmed + "&to=" + isoDateTrimmed;
                            var myKPIFromTimeRangeNew = moment(upperTimeLimitISOTrimmed).subtract(24, 'hours');
                            var myKPIFromTimeRangeNewTrimmed = convertFromMomentToTime(myKPIFromTimeRangeNew);
                            myKPITimeRange = "&from=" + myKPIFromTimeRangeNewTrimmed + "&to=" + upperTimeLimitISOTrimmed;
                        }
                        break;

                    case "Settimanale":
                        serviceMapTimeRange = "fromTime=7-day";
                        //    var deltaT = 7 + parseInt(timeCount) * 7;
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-day";
                        globalDiagramRange = "7/DAY";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(7 * 24 * timeCount);
                        if (flagTracker === true) {
                            myKPITimeRange = "&from=" + dayTracker + "T00:00:00&to=" + dayTracker + "T23:59:59";
                        } else {
                            var now = new Date();
                            myKPIFromTimeRange = now.setHours(now.getHours() - 168);
                            var myKPIFromTimeRangeUTC = new Date(myKPIFromTimeRange).toUTCString();
                            var myKPIFromTimeRangeISO = new Date(myKPIFromTimeRangeUTC).toISOString();
                            var myKPIFromTimeRangeISOTrimmed = myKPIFromTimeRangeISO.substring(0, isoDate.length - 8);
                            //    myKPITimeRange = "&from=" + myKPIFromTimeRangeISOTrimmed + "&to=" + isoDateTrimmed;
                            var myKPIFromTimeRangeNew = moment(upperTimeLimitISOTrimmed).subtract(7, 'days');
                            var myKPIFromTimeRangeNewTrimmed = convertFromMomentToTime(myKPIFromTimeRangeNew);
                            myKPITimeRange = "&from=" + myKPIFromTimeRangeNewTrimmed + "&to=" + upperTimeLimitISOTrimmed;
                        }
                        break;

                    case "Mensile":
                        serviceMapTimeRange = "fromTime=30-day";
                        //    var deltaT = 30 + parseInt(timeCount) * 30;
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-day";
                        globalDiagramRange = "30/DAY";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(30 * 24 * timeCount);
                        if (flagTracker === true) {
                            myKPITimeRange = "&from=" + dayTracker + "T00:00:00&to=" + dayTracker + "T23:59:59";
                        } else {
                            var now = new Date();
                            myKPIFromTimeRange = now.setHours(now.getHours() - 720);
                            var myKPIFromTimeRangeUTC = new Date(myKPIFromTimeRange).toUTCString();
                            var myKPIFromTimeRangeISO = new Date(myKPIFromTimeRangeUTC).toISOString();
                            var myKPIFromTimeRangeISOTrimmed = myKPIFromTimeRangeISO.substring(0, isoDate.length - 8);
                            //   myKPITimeRange = "&from=" + myKPIFromTimeRangeISOTrimmed + "&to=" + isoDateTrimmed;
                            var myKPIFromTimeRangeNew = moment(upperTimeLimitISOTrimmed).subtract(1, 'month');
                            var myKPIFromTimeRangeNewTrimmed = convertFromMomentToTime(myKPIFromTimeRangeNew);
                            myKPITimeRange = "&from=" + myKPIFromTimeRangeNewTrimmed + "&to=" + upperTimeLimitISOTrimmed;
                        }
                        break;

                    case "Semestrale":
                        serviceMapTimeRange = "fromTime=180-day";
                        //    var deltaT = 365 + parseInt(timeCount) * 365;
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-day";
                        globalDiagramRange = "180/DAY";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(180 * 24 * timeCount);
                        if (flagTracker === true) {
                            myKPITimeRange = "&from=" + dayTracker + "T00:00:00&to=" + dayTracker + "T23:59:59";
                        } else {
                            var now = new Date();
                            myKPIFromTimeRange = now.setHours(now.getHours() - 4320);
                            var myKPIFromTimeRangeUTC = new Date(myKPIFromTimeRange).toUTCString();
                            var myKPIFromTimeRangeISO = new Date(myKPIFromTimeRangeUTC).toISOString();
                            var myKPIFromTimeRangeISOTrimmed = myKPIFromTimeRangeISO.substring(0, isoDate.length - 8);
                            //    myKPITimeRange = "&from=" + myKPIFromTimeRangeISOTrimmed + "&to=" + isoDateTrimmed;
                            var myKPIFromTimeRangeNew = moment(upperTimeLimitISOTrimmed).subtract(6, 'month');
                            var myKPIFromTimeRangeNewTrimmed = convertFromMomentToTime(myKPIFromTimeRangeNew);
                            myKPITimeRange = "&from=" + myKPIFromTimeRangeNewTrimmed + "&to=" + upperTimeLimitISOTrimmed;
                        }
                        break;

                    case "Annuale":
                        serviceMapTimeRange = "fromTime=365-day";
                        //    var deltaT = 365 + parseInt(timeCount) * 365;
                        //    serviceMapTimeRange = "fromTime=" + deltaT + "-day";
                        globalDiagramRange = "365/DAY";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(365 * 24 * timeCount);
                        if (flagTracker === true) {
                            myKPITimeRange = "&from=" + dayTracker + "T00:00:00&to=" + dayTracker + "T23:59:59";
                        } else {
                            var now = new Date();
                            myKPIFromTimeRange = now.setHours(now.getHours() - 8760);
                            var myKPIFromTimeRangeUTC = new Date(myKPIFromTimeRange).toUTCString();
                            var myKPIFromTimeRangeISO = new Date(myKPIFromTimeRangeUTC).toISOString();
                            var myKPIFromTimeRangeISOTrimmed = myKPIFromTimeRangeISO.substring(0, isoDate.length - 8);
                            //    myKPITimeRange = "&from=" + myKPIFromTimeRangeISOTrimmed + "&to=" + isoDateTrimmed;
                            var myKPIFromTimeRangeNew = moment(upperTimeLimitISOTrimmed).subtract(1, 'year');
                            var myKPIFromTimeRangeNewTrimmed = convertFromMomentToTime(myKPIFromTimeRangeNew);
                            myKPITimeRange = "&from=" + myKPIFromTimeRangeNewTrimmed + "&to=" + upperTimeLimitISOTrimmed;
                        }
                        break;

                    default:
                        serviceMapTimeRange = "fromTime=1-day";
                        globalDiagramRange = "1/DAY";
                        upperTimeLimitISOTrimmed = getUpperTimeLimit(24 * timeCount);
                        if (flagTracker === true) {
                            myKPITimeRange = "&from=" + dayTracker + "T00:00:00&to=" + dayTracker + "T23:59:59";
                        } else {
                            var now = new Date();
                            myKPIFromTimeRange = now.setHours(now.getHours() - 24);
                            var myKPIFromTimeRangeUTC = new Date(myKPIFromTimeRange).toUTCString();
                            var myKPIFromTimeRangeISO = new Date(myKPIFromTimeRangeUTC).toISOString();
                            var myKPIFromTimeRangeISOTrimmed = myKPIFromTimeRangeISO.substring(0, isoDate.length - 8);
                            //    myKPITimeRange = "&from=" + myKPIFromTimeRangeISOTrimmed + "&to=" + isoDateTrimmed;
                            var myKPIFromTimeRangeNew = moment(upperTimeLimitISOTrimmed).subtract(24, 'hours');
                            var myKPIFromTimeRangeNewTrimmed = convertFromMomentToTime(myKPIFromTimeRangeNew);
                            myKPITimeRange = "&from=" + myKPIFromTimeRangeNewTrimmed + "&to=" + upperTimeLimitISOTrimmed;
                        }
                        break;
                }

                switch (sm_based) {
                    case 'yes':
                        $.ajax({
                            // this url returns an obj that contains all the dataseries of a certain service uri = "http://www.disit.org/km4city/resource/METRO28"
                            // ex. url = "https://www.snap4city.org/superservicemap/api/v1/?serviceUri=http://www.disit.org/km4city/resource/METRO28&fromTime=7-day&toTime=2021-04-29T12:49:34&valueName=avgTime"
                            // url: rowParameters + "&" + serviceMapTimeRange + "&valueName=" + sm_field,

                            url: superserviceMapUrl + encodeServiceUri(rowParameters) + "&" + serviceMapTimeRange + "&toTime=" + upperTimeLimitISOTrimmed + "&valueName=" + sm_field,
                            type: "GET",
                            data: {},
                            async: true,
                            dataType: 'json',
                            success: function (originalData) {
                                var convertedData = convertDataFromSmToDm(originalData, sm_field, udm);
                                if (convertedData) {
                                    if (convertedData.data.length > 0) {
                                        var localTimeZone = moment.tz.guess();
                                        var momentDateTime = moment();
                                        var localDateTime = momentDateTime.tz(localTimeZone).format();
                                        localDateTime = localDateTime.replace("T", " ");
                                        var plusIndexLocal = localDateTime.indexOf("+");
                                        localDateTime = localDateTime.substr(0, plusIndexLocal);
                                        var localTimeZoneString = "";
                                        if (localDateTime == "") {
                                            localTimeZoneString = "(not recognized) --> Europe/Rome"
                                        } else {
                                            localTimeZoneString = localTimeZone;
                                        }

                                        drawDiagram(convertedData, globalDiagramRange, sm_field, true, localTimeZoneString, udm);

                                        upLimit = convertedData.data[0].commit.author.computationDate;
                                        if (timeNavCount < 0) {
                                            if (moment(upLimit).isBefore(moment(dateInFuture))) {

                                            } else {
                                                $("#" + widgetName + "_nextButton").hide();
                                            }
                                        }
                                    }
                                    else {
                                        expandTimeRange(timeRange, timeNavCount, udmFromUserOptions);
                                        /*   showWidgetContent(widgetName);
                                           $("#"+ widgetName +"_chartContainer").hide();
                                           $('#'+ widgetName +'_noDataAlert').show();
                                           console.log("Dati non disponibili da Service Map");*/
                                    }
                                }
                                else {
                                    expandTimeRange(timeRange, timeNavCount, udmFromUserOptions);
                                    /*  showWidgetContent(widgetName);
                                      $("#"+ widgetName +"_chartContainer").hide();
                                      $('#'+ widgetName +'_noDataAlert').show();
                                      console.log("Dati non disponibili da Service Map");*/
                                }
                            },
                            error: function (data) {
                                showWidgetContent(widgetName);
                                $("#" + widgetName + "_chartContainer").hide();
                                $('#' + widgetName + '_noDataAlertText').text("API Error in Data Retrieval.");
                                $('#' + widgetName + '_noDataAlert').show();
                                console.log("Errore in scaricamento dati da Service Map");
                                console.log(JSON.stringify(data));
                            }
                        });
                        break;

                    case 'no':
                        $.ajax({
                            url: "../widgets/getDataMetricsForTimeTrend.php",
                            data: {
                                "IdMisura": ['<?= escapeForJS($_REQUEST[\'id_metric\']) ?>'],
                                "time": globalDiagramRange,
                                "compare": 0,
                                "lowerDateTime": myKPIFromTimeRangeNewTrimmed.replace("T", " "),
                                "upperDateTime": upperTimeLimitISOTrimmed.replace("T", " ")
                            },
                            type: "GET",
                            async: true,
                            dataType: 'json',
                            success: function (metricData) {
                                if (metricData.metricType === 'personal') {
                                    needWebSocket = true;
                                }
                                var localTimeZone = moment.tz.guess();
                                var momentDateTime = moment();
                                var localDateTime = momentDateTime.tz(localTimeZone).format();
                                localDateTime = localDateTime.replace("T", " ");
                                var plusIndexLocal = localDateTime.indexOf("+");
                                localDateTime = localDateTime.substr(0, plusIndexLocal);
                                var localTimeZoneString = "";
                                if (localDateTime == "") {
                                    localTimeZoneString = "(not recognized) --> Europe/Rome"
                                } else {
                                    localTimeZoneString = localTimeZone;
                                }

                                drawDiagram(metricData, globalDiagramRange, '<?= escapeForJS($_REQUEST[\'id_metric\']) ?>', false, localTimeZoneString, udm);

                                if (needWebSocket) {
                                    openWs();
                                }
                            },
                            error: function (errorData) {
                                expandTimeRange(timeRange, timeNavCount, udmFromUserOptions);
                                /*   showWidgetContent(widgetName);
                                   $("#"+ widgetName +"_chartContainer").hide();
                                   $('#'+ widgetName +'_noDataAlert').show();
                                   console.log("Errore in chiamata di getDataMetricsForTimeTrend.php.");
                                   console.log(JSON.stringify(errorData));*/
                            }
                        });
                        break;

                    case 'myPersonalData':
                        $.ajax({
                            //   url: "../controllers/myPersonalDataProxy.php?variableName=" + sm_field + "&last=0&" + serviceMapTimeRange,
                            url: "../controllers/myPersonalDataProxy.php?variableName=" + sm_field + "&" + serviceMapTimeRange,
                            type: "GET",
                            data: {},
                            async: true,
                            dataType: 'json',
                            success: function (data) {
                                var convertedData = convertDataFromPersonalDataToDm(data);
                                if (convertedData) {
                                    if (convertedData.data.length > 0) {
                                        var localTimeZone = moment.tz.guess();
                                        var momentDateTime = moment();
                                        var localDateTime = momentDateTime.tz(localTimeZone).format();
                                        localDateTime = localDateTime.replace("T", " ");
                                        var plusIndexLocal = localDateTime.indexOf("+");
                                        localDateTime = localDateTime.substr(0, plusIndexLocal);
                                        var localTimeZoneString = "";
                                        if (localDateTime == "") {
                                            localTimeZoneString = "(not recognized) --> Europe/Rome"
                                        } else {
                                            localTimeZoneString = localTimeZone;
                                        }

                                        drawDiagram(convertedData, globalDiagramRange, sm_field, true, localTimeZoneString, udm);
                                    }
                                    else {
                                        expandTimeRange(timeRange, timeNavCount, udmFromUserOptions);
                                        /*  showWidgetContent(widgetName);
                                          $("#"+ widgetName +"_chartContainer").hide();
                                          $('#'+ widgetName +'_noDataAlert').show();
                                          console.log("Dati non disponibili da Service Map");*/
                                    }
                                }
                                else {
                                    expandTimeRange(timeRange, timeNavCount, udmFromUserOptions);
                                    /*    showWidgetContent(widgetName);
                                        $("#"+ widgetName +"_chartContainer").hide();
                                        $('#'+ widgetName +'_noDataAlert').show();
                                        console.log("Dati non disponibili da Service Map");*/
                                }
                            },
                            error: function (errorData) {
                                metricData = null;
                                console.log("Error in data retrieval");
                                console.log(JSON.stringify(errorData));
                                if (firstLoad !== false) {
                                    $("#" + widgetName + "_chartContainer").hide();
                                    $("#" + widgetName + "_loading").hide();
                                    $('#' + widgetName + '_noDataAlertText').text("API Error in Data Retrieval.");
                                    $('#' + widgetName + '_noDataAlert').show();
                                }
                            }
                        })
                        break;
                    case 'myData':
                    case 'myKPI':
                        //   console.log("KPI Api Call.");
                        if (fromTrackerParams != null && fromTrackerParams != undefined) {
                            rowParameters = fromTrackerParams;
                        } else {
                            if (rowParameters.includes("datamanager/api/v1/poidata/")) {
                                rowParameters = rowParameters.split("datamanager/api/v1/poidata/")[1];
                            }
                        }

                        $.ajax({
                            url: "../controllers/myKpiProxy.php",
                            type: "GET",
                            data: {
                                myKpiId: rowParameters,
                                timeRange: myKPITimeRange,
                                action: "getValueUnitForTrend"
                            },
                            async: true,
                            dataType: 'json',
                            success: function (data) {
                                var stopFlag = 1;
                                var convertedData = convertDataFromMyKpiToDm(data);
                                if (convertedData) {
                                    if (convertedData.data.length > 1 || convertedData.data[0].commit.author.value != null) {
                                        if (udmFromUserOptions != null) {
                                            udm = udmFromUserOptions;
                                        } else {
                                            if (data[0].variableUnit != null) {
                                                udm = data[0].variableUnit;
                                            } else if (data[0].valueUnit != null) {
                                                udm = data[0].valueUnit;
                                            }
                                        }
                                        var localTimeZone = moment.tz.guess();
                                        var momentDateTime = moment();
                                        var localDateTime = momentDateTime.tz(localTimeZone).format();
                                        localDateTime = localDateTime.replace("T", " ");
                                        var plusIndexLocal = localDateTime.indexOf("+");
                                        localDateTime = localDateTime.substr(0, plusIndexLocal);
                                        var localTimeZoneString = "";
                                        if (localDateTime == "") {
                                            localTimeZoneString = "(not recognized) --> Europe/Rome"
                                        } else {
                                            localTimeZoneString = localTimeZone;
                                        }

                                        drawDiagram(convertedData, globalDiagramRange, sm_field, true, localTimeZoneString, udm);
                                    }
                                    //else if (convertedData.data[0].commit.author.value == null)
                                    else {
                                        expandTimeRange(timeRange, timeNavCount, udmFromUserOptions);
                                        /*    showWidgetContent(widgetName);
                                            $("#"+ widgetName +"_chartContainer").hide();
                                            $('#'+ widgetName +'_noDataAlert').show();
                                            console.log("Dati MyKPI non presenti");*/
                                    }
                                }
                                else {
                                    expandTimeRange(timeRange, timeNavCount, udmFromUserOptions);
                                    /*    showWidgetContent(widgetName);
                                        $("#"+ widgetName +"_chartContainer").hide();
                                        $('#'+ widgetName +'_noDataAlert').show();
                                        console.log("Dati MyKPI non presenti");*/
                                }
                            },
                            error: function (data) {
                                showWidgetContent(widgetName);
                                $("#" + widgetName + "_chartContainer").hide();
                                $('#' + widgetName + '_noDataAlertText').text("API Error in Data Retrieval.");
                                $('#' + widgetName + '_noDataAlert').show();
                                console.log("Errore!");
                                console.log(widgetName + "_titleDiv").html(widgetTitle);
                            }
                        });

                        //Fine definizioni di funzione
                        $('#' + widgetName + '_timeTrendPrevBtn').off("click").click(
                            function () {
                                //  alert("PREV Clicked!");
                                if (timeNavCount == 0) {
                                    //   if (widgetData.params.sm_based == "yes" || fromGisExternalContent === true) {
                                    let urlKBToBeCalled = "";
                                    let field = "";
                                    let dashboardOrgKbUrl = "<?= $superServiceMapUrlPrefix ?>api/v1/";
                                    if (fromGisExternalContent) {
                                        // urlKBToBeCalled = dashboardOrgKbUrl + "?serviceUri=" + encodeURI(fromGisExternalContentServiceUri) + "&valueName=" + fromGisExternalContentField;
                                        urlKBToBeCalled = dashboardOrgKbUrl + "?serviceUri=" + encodeServiceUri(fromGisExternalContentServiceUri);
                                        field = fromGisExternalContentField;

                                    } else {
                                        //  urlKBToBeCalled = rowParameters + "&" + "&valueName=" + sm_field;
                                        urlKBToBeCalled = encodeServiceUri(rowParameters);
                                        field = sm_field;
                                    }

                                    if (rowParameters != null) {
                                        if (rowParameters.includes("https:")) {
                                            $.ajax({
                                                url: "<?=$superServiceMapProxy?>" + urlKBToBeCalled,
                                                type: "GET",
                                                data: {},
                                                async: true,
                                                dataType: 'json',
                                                success: function (originalData) {
                                                    var stopFlag = 1;
                                                    var convertedData = convertDataFromTimeNavToDm(originalData, field, udm);
                                                    if (convertedData) {
                                                        if (convertedData.data.length > 0) {
                                                            var localTimeZone = moment.tz.guess();
                                                            var momentDateTime = moment();
                                                            var localDateTime = momentDateTime.tz(localTimeZone).format();
                                                            localDateTime = localDateTime.replace("T", " ");
                                                            var plusIndexLocal = localDateTime.indexOf("+");
                                                            localDateTime = localDateTime.substr(0, plusIndexLocal);
                                                            var localTimeZoneString = "";
                                                            if (localDateTime == "") {
                                                                localTimeZoneString = "(not recognized) --> Europe/Rome"
                                                            } else {
                                                                localTimeZoneString = localTimeZone;
                                                            }
                                                            if (convertedData.data[0].commit.author.futureDate != null && convertedData.data[0].commit.author.futureDate != undefined) {
                                                                dataFut = (convertedData.data[0].commit.author.futureDate);
                                                                if (moment(dataFut).isAfter(momentDateTime)) {
                                                                    $("#" + widgetName + "_nextButton").show();
                                                                } else {
                                                                    $("#" + widgetName + "_nextButton").show();
                                                                }
                                                            } else {
                                                                $("#" + widgetName + "_nextButton").show();
                                                            }
                                                        } else {
                                                            showWidgetContent(widgetName);
                                                            $("#" + widgetName + "_chartContainer").hide();
                                                            $('#' + widgetName + '_noDataAlert').show();
                                                            console.log("Dati non disponibili da Service Map");
                                                        }
                                                    } else {
                                                        showWidgetContent(widgetName);
                                                        $("#" + widgetName + "_chartContainer").hide();
                                                        $('#<?= str_repl' + widgetName + '').show();
                                                        console.log("Dati non disponibili da Service Map");
                                                    }
                                                },
                                                error: function (data) {
                                                    //  showWidgetContent(widgetName);
                                                    //  $("#"+ widgetName +"_chartContainer").hide();
                                                    //  $('#'+ widgetName +'_noDataAlert').show();
                                                    console.log("Errore in chiamata prima API");
                                                    console.log(JSON.stringify(data));
                                                }
                                            })
                                        } else {
                                            $("#" + widgetName + "_nextButton").show();
                                        }
                                    } else {
                                        $("#" + widgetName + "_nextButton").show();
                                    }
                                } else if (timeNavCount < 0 && $("#" + widgetName + "_nextButton").is(":hidden")) {
                                    $("#" + widgetName + "_nextButton").show();
                                }
                                timeNavCount++;
                                setupLoadingPanel(widgetName, widgetContentColor, true);
                                //    populateWidget(timeRange, null, "minus", timeNavCount, null, udmFromUserOptions);
                                /*    if (expandedTimeRangeFlag) {
                                        populateWidget(currentTimeRange, null, "minus", timeNavCount, null, udm);
                                    } else {*/
                                populateWidget(timeRange, null, "minus", timeNavCount, null, udm);
                                //  }
                            });

                        $("#" + widgetName + "_timeTrendNextBtn").off("click").click(function () {
                            //   alert("NEXT Clicked!");
                            timeNavCount--;
                            if (timeNavCount == 0) {
                                //    if (widgetData.params.sm_based == "yes" || fromGisExternalContent === true) {
                                let urlKBToBeCalled = "";
                                let field = "";
                                let dashboardOrgKbUrl = "<?= $superServiceMapUrlPrefix ?>api/v1/";
                                if (fromGisExternalContent) {
                                    // urlKBToBeCalled = dashboardOrgKbUrl + "?serviceUri=" + encodeURI(fromGisExternalContentServiceUri) + "&valueName=" + fromGisExternalContentField;
                                    urlKBToBeCalled = dashboardOrgKbUrl + "?serviceUri=" + encodeServiceUri(fromGisExternalContentServiceUri);
                                    field = fromGisExternalContentField;

                                } else {
                                    //  urlKBToBeCalled = rowParameters + "&" + "&valueName=" + sm_field;
                                    urlKBToBeCalled = encodeServiceUri(rowParameters);
                                    field = sm_field;
                                }
                                if (rowParameters != null) {
                                    if (rowParameters.includes("https:")) {
                                        $.ajax({
                                            url: "<?=$superServiceMapProxy?>" + urlKBToBeCalled,
                                            type: "GET",
                                            data: {},
                                            async: true,
                                            dataType: 'json',
                                            success: function (originalData) {
                                                var stopFlag = 1;
                                                var convertedData = convertDataFromTimeNavToDm(originalData, field, udm);
                                                if (convertedData) {
                                                    if (convertedData.data.length > 0) {
                                                        var localTimeZone = moment.tz.guess();
                                                        var momentDateTime = moment();
                                                        var localDateTime = momentDateTime.tz(localTimeZone).format();
                                                        localDateTime = localDateTime.replace("T", " ");
                                                        var plusIndexLocal = localDateTime.indexOf("+");
                                                        localDateTime = localDateTime.substr(0, plusIndexLocal);
                                                        var localTimeZoneString = "";
                                                        if (localDateTime == "") {
                                                            localTimeZoneString = "(not recognized) --> Europe/Rome"
                                                        } else {
                                                            localTimeZoneString = localTimeZone;
                                                        }
                                                        if (convertedData.data[0].commit.author.futureDate != null && convertedData.data[0].commit.author.futureDate != undefined) {
                                                            dataFut = (convertedData.data[0].commit.author.futureDate);
                                                            if (moment(dataFut).isAfter(momentDateTime)) {
                                                                $("#" + widgetName + "_nextButton").show();
                                                            } else {
                                                                $("#" + widgetName + "_nextButton").hide();
                                                            }
                                                        } else {
                                                            $("#" + widgetName + "_nextButton").hide();
                                                        }
                                                    } else {
                                                        showWidgetContent(widgetName);
                                                        $("#" + widgetName + "_chartContainer").hide();
                                                        ///$('#<?= str_repl' + widgetName ).show();
                                                        console.log("Dati non disponibili da Service Map");
                                                    }
                                                } else {
                                                    showWidgetContent(widgetName);
                                                    $("#" + widgetName + "_chartContainer").hide();
                                                    ///$('#<?= str_repl' + widgetName + '').show();
                                                    console.log("Dati non disponibili da Service Map");
                                                }
                                            },
                                            error: function (data) {
                                                //  showWidgetContent(widgetName);
                                                //  $("#"+ widgetName +"_chartContainer").hide();
                                                //  $('#'+ widgetName +'_noDataAlert').show();
                                                console.log("Errore in chiamata prima API");
                                                console.log(JSON.stringify(data));
                                            }
                                        })
                                    } else {
                                        $("#" + widgetName + "_nextButton").hide();
                                    }
                                } else {
                                    $("#" + widgetName + "_nextButton").hide();
                                }
                            }
                            setupLoadingPanel(widgetName, widgetContentColor, true);
                            //    populateWidget(timeRange, null, "plus", timeNavCount, dataFut, udmFromUserOptions);
                            /*    if (expandedTimeRangeFlag) {
                                    populateWidget(currentTimeRange, null, "plus", timeNavCount, dataFut, udm)
                                } else {*/
                            populateWidget(timeRange, null, "plus", timeNavCount, dataFut, udm);
                            //   }

                        });

                        $.ajax({
                            url: "../controllers/getWidgetParams.php",
                            type: "GET",
                            data: {
                                widgetName: widgetName
                            },
                            async: true,
                            dataType: 'json',
                            success: function (widgetData) {
                                // Hide Next Button at first instantiation
                                if (timeNavCount == 0) {
                                    if (widgetData.params.sm_based == "yes" || fromGisExternalContent === true) {
                                        let urlKBToBeCalled = "";
                                        let field = "";
                                        let dashboardOrgKbUrl = "<?= $superServiceMapUrlPrefix ?>api/v1/";
                                        //let dashboardOrgKbUrl = "https://servicemap.disit.org/WebAppGrafo/api/v1/";
                                        if (fromGisExternalContent) {
                                            // urlKBToBeCalled = dashboardOrgKbUrl + "?serviceUri=" + encodeURI(fromGisExternalContentServiceUri) + "&valueName=" + fromGisExternalContentField;
                                            urlKBToBeCalled = "<?=$superServiceMapProxy?>" + dashboardOrgKbUrl + "?serviceUri=" + encodeServiceUri(fromGisExternalContentServiceUri);
                                            field = fromGisExternalContentField;
                                        } else {
                                            //  urlKBToBeCalled = rowParameters + "&" + "&valueName=" + sm_field;
                                            urlKBToBeCalled = "<?=$superServiceMapProxy?>" + encodeServiceUri(widgetData.params.rowParameters);
                                            field = widgetData.params.sm_field;
                                        }
                                        $.ajax({
                                            url: urlKBToBeCalled,
                                            type: "GET",
                                            data: {},
                                            async: true,
                                            dataType: 'json',
                                            success: function (originalData) {
                                                var stopFlag = 1;
                                                var convertedData = convertDataFromTimeNavToDm(originalData, field, udmFromUserOptions);
                                                if (convertedData) {
                                                    if (convertedData.data.length > 0) {
                                                        var localTimeZone = moment.tz.guess();
                                                        var momentDateTime = moment();
                                                        var localDateTime = momentDateTime.tz(localTimeZone).format();
                                                        localDateTime = localDateTime.replace("T", " ");
                                                        var plusIndexLocal = localDateTime.indexOf("+");
                                                        localDateTime = localDateTime.substr(0, plusIndexLocal);
                                                        var localTimeZoneString = "";
                                                        if (localDateTime == "") {
                                                            localTimeZoneString = "(not recognized) --> Europe/Rome"
                                                        } else {
                                                            localTimeZoneString = localTimeZone;
                                                        }
                                                        if (convertedData.data[0].commit.author.futureDate != null && convertedData.data[0].commit.author.futureDate != undefined) {
                                                            dataFut = (convertedData.data[0].commit.author.futureDate);
                                                            if (moment(dataFut).isAfter(momentDateTime)) {
                                                                $("#" + widgetName + "_nextButton").show();
                                                            } else {
                                                                $("#" + widgetName + "_nextButton").hide();
                                                            }
                                                        } else {
                                                            $("#" + widgetName + "_nextButton").hide();
                                                        }
                                                    } else {
                                                        showWidgetContent(widgetName);
                                                        $("#" + widgetName + "_chartContainer").hide();
                                                        $('#' + widgetName + '_noDataAlert').show();
                                                        console.log("Dati non disponibili da Service Map");
                                                    }
                                                } else {
                                                    showWidgetContent(widgetName);
                                                    $("#" + widgetName + "_chartContainer").hide();
                                                    $('#' + widgetName + '_noDataAlert').show();
                                                    console.log("Dati non disponibili da Service Map");
                                                }
                                            },
                                            error: function (data) {
                                                //  showWidgetContent(widgetName);
                                                //  $("#"+ widgetName +"_chartContainer").hide();
                                                //  $('#'+ widgetName +'_noDataAlert').show();
                                                console.log("Errore in chiamata prima API");
                                                console.log(JSON.stringify(data));
                                            }
                                        })
                                    } else {
                                        $("#" + widgetName + "_nextButton").hide();
                                    }
                                }
                                showTitle = widgetData.params.showTitle;
                                widgetContentColor = widgetData.params.color_w;
                                fontSize = widgetData.params.fontSize;
                                timeToReload = widgetData.params.frequency_w;
                                hasTimer = widgetData.params.hasTimer;
                                widgetTitle = widgetData.params.title_w;
                                widgetHeaderColor = widgetData.params.frame_color_w;
                                widgetHeaderFontColor = widgetData.params.headerFontColor;
                                chartColor = widgetData.params.chartColor;
                                dataLabelsFontSize = widgetData.params.dataLabelsFontSize;
                                dataLabelsFontColor = widgetData.params.dataLabelsFontColor;
                                chartLabelsFontSize = widgetData.params.chartLabelsFontSize;
                                chartLabelsFontColor = widgetData.params.chartLabelsFontColor;
                                infoJson = widgetData.params.infoJson;
                                sm_based = widgetData.params.sm_based;
                                udmFromUserOptions = widgetData.params.udm;
                                if (udmFromUserOptions != null) {
                                    var udmFromUserOptions = udmFromUserOptions.replace(/<script[^>]*>([\S\s]*?)<\/script>/gmi, '');
                                    udmFromUserOptions = udmFromUserOptions.replace(/<\/?\w(?:[^"'>]|"[^"]*"|'[^']*')*>/gmi, '');
                                    udmFromUserOptions = udmFromUserOptions.replace(/&deg;/g, "Â°");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&num;/g, "#");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&dollar;/g, "$");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&percnt;/g, "%");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&pound;/g, "Â£");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&lt;/g, "<");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&gt;/g, ">");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&agrave;/g, "Ã ");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&egrave;/g, "Ã¨");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&eacute;/g, "Ã©");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&igrave;/g, "Ã¬");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&ograve;/g, "Ã²");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&ugrave;/g, "Ã¹");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&micro;/g, "Âµ");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&sol;/g, "/");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&bsol;/g, "\\");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&lpar;/g, "(");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&rpar;/g, ")");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&lsqb;/g, "[");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&rsqb;/g, "]");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&lcub;/g, "{");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&rcub;/g, "}");
                                    udmFromUserOptions = udmFromUserOptions.replace(/&Hat;/g, "^");
                                }
                                var styleParametersString = widgetData.params.styleParameters;
                                styleParameters = jQuery.parseJSON(styleParametersString);
                                if (styleParameters != null) {
                                    if (styleParameters.viewUdm != null) {
                                        viewUdm = styleParameters.viewUdm;
                                    }
                                    if (styleParameters.xOffsetUdm != null) {
                                        xOffsetUdm = styleParameters.xOffsetUdm;
                                    }
                                }
                                //    if ((sm_based === "myKPI" || sm_based === "no") && fromGisExternalContent != true) {
                                //    if ((sm_based === "no" || infoJson === "fromTracker") && fromGisExternalContent != true) {
                                if (infoJson === "fromTracker" && fromGisExternalContent != true) {
                                    $("#" + widgetName + "_timeControlsContainer").hide();
                                    $("#" + widgetName + "_titleDiv").css("width", "95%");
                                } else {
                                    $("#" + widgetName + "_timeControlsContainer").show();
                                    $("#" + widgetName + "_titleDiv").css("width", "95%");
                                }
                                rowParameters = widgetData.params.rowParameters;
                                sm_field = widgetData.params.sm_field;
                                gridLineColor = widgetData.params.chartPlaneColor;
                                chartAxesColor = widgetData.params.chartAxesColor;
                                if (((embedWidget === true) && (embedWidgetPolicy === 'auto')) || ((embedWidget === true) && (embedWidgetPolicy === 'manual') && (showTitle === "no")) || ((embedWidget === false) && (showTitle === "no"))) {
                                    showHeader = false;
                                }
                                else {
                                    showHeader = true;
                                }
                                if ((metricNameFromDriver === "undefined") || (metricNameFromDriver === undefined) || (metricNameFromDriver === "null") || (metricNameFromDriver === null)) {
                                    metricName = "<?= escapeForJS($_REQUEST['id_metric']) ?>";
                                    widgetTitle = widgetData.params.title_w;
                                    widgetHeaderColor = widgetData.params.frame_color_w;
                                    widgetHeaderFontColor = widgetData.params.headerFontColor;
                                    timeRange = widgetData.params.temporal_range_w;
                                }
                                else {
                                    metricName = metricNameFromDriver;
                                    widgetTitleFromDriver.replace(/_/g, " ");
                                    widgetTitleFromDriver.replace(/\'/g, "&apos;");
                                    widgetTitle = widgetTitleFromDriver;
                                    $("#" + widgetName).css("border-color", widgetHeaderColorFromDriver);
                                    widgetHeaderColor = widgetHeaderColorFromDriver;
                                    widgetHeaderFontColor = widgetHeaderFontColorFromDriver;
                                    //   $("#" + widgetName + "_titleDiv").html(widgetTitle + " On Day: " + dayTracker);
                                }
                                setWidgetLayout(hostFile, widgetName, widgetContentColor, widgetHeaderColor, widgetHeaderFontColor, showHeader, headerHeight, hasTimer);
                                $('#' + widgetName + '_div').parents('li.gs_w').off('resizeWidgets');
                                $('#' + widgetName + '_div').parents('li.gs_w').on('resizeWidgets', resizeWidget);
                                if (firstLoad === false) {
                                    showWidgetContent(widgetName);
                                }
                                else {
                                    setupLoadingPanel(widgetName, widgetContentColor, firstLoad);
                                }
                                //Nuova versione
                                if (('<?= sanitizeJsonRelaxed2(\'styleParameters\') ?>' !== "") && ('<?= sanitizeJsonRelaxed2(\'styleParameters\') ?>' !== "null")) {
                                    styleParameters = JSON.parse('<?= sanitizeJsonRelaxed2(\'styleParameters\') ?>');
                                }
                                if ('<?= sanitizeJsonRelaxed2(\'parameters\') ?>'.length > 0) {
                                    widgetParameters = JSON.parse('<?= sanitizeJsonRelaxed2(\'parameters\') ?>');
                                }
                                if (widgetParameters !== null && widgetParameters !== undefined) {
                                    if (widgetParameters.hasOwnProperty("thresholdObject")) {
                                        thresholdObject = widgetParameters.thresholdObject;
                                    }
                                }
                                sizeRowsWidget = parseInt('<?= escapeForJS($_REQUEST[\'size_rows\']) ?>');
                                if (timeRange == null || timeRange == undefined) {
                                    timeRange = widgetData.params.temporal_range_w;
                                }
                                currentTimeRange = timeRange;
                                populateWidget(timeRange, null, null, timeNavCount, null, udmFromUserOptions);
                                // Modify width to show newly implemented PREV and NEXT buttons
                                //    if ((sm_based != "myKPI" && sm_based != "no") || fromGisExternalContent === true) {
                                //    if ((sm_based != "no" &&  infoJson != "fromTracker") || fromGisExternalContent === true) {
                                if (infoJson != "fromTracker" || fromGisExternalContent === true) {
                                    var titleDiv = $('#' + widgetName + '_titleDiv');
                                    //    $('#'+ widgetName +'_infoButtonDiv').css("width", "3.5%");
                                    //    $('#'+ widgetName +'_countdownContainerDiv').css("width", "3%");
                                    $('#' + widgetName + '_prevButton').css("color", widgetHeaderFontColor);
                                    $('#' + widgetName + '_nextButton').css("color", widgetHeaderFontColor);
                                    titleDiv.css("width", "70%");

                                    if ($('#' + widgetName + '_header').width() < 400) {
                                        titleDiv.css("width", "65%");
                                        $("#" + widgetName + "_timeControlsContainer").css("width", "19%");
                                        $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                                    } else if ($('#' + widgetName + '_header').width() < 480) {
                                        titleDiv.css("width", "74%");
                                        $("#" + widgetName + "_timeControlsContainer").css("width", "14%");
                                        $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                                    } else if ($('#' + widgetName + '_header').width() < 560) {
                                        titleDiv.css("width", "75%");
                                        $("#" + widgetName + "_timeControlsContainer").css("width", "15%");
                                        $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                                    } else if ($('#' + widgetName + '_header').width() < 700) {
                                        titleDiv.css("width", "80%");
                                        $("#" + widgetName + "_timeControlsContainer").css("width", "11%");
                                        $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                                    } else if ($('#' + widgetName + '_header').width() < 900) {
                                        titleDiv.css("width", "84%");
                                        $("#" + widgetName + "_timeControlsContainer").css("width", "9%");
                                        $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                                    } else if ($('#' + widgetName + '_header').width() < 1000) {
                                        titleDiv.css("width", "85%");
                                        $("#" + widgetName + "_timeControlsContainer").css("width", "8%");
                                        $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                                    } else if ($('#' + widgetName + '_header').width() < 1050) {
                                        titleDiv.css("width", "85%");
                                        $("#" + widgetName + "_timeControlsContainer").css("width", "7%");
                                        $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                                    } else {
                                        titleDiv.css("width", "87%");
                                        $("#" + widgetName + "_timeControlsContainer").css("width", "7%");
                                        $('#' + widgetName + '_prevButton').css("padding-right", "0px");
                                    }
                                }


                                //Web socket
                                //openWs = function(e)
                                //{
                                //try
                                //{
                                //<?php
                                //$genFileContent = parse_ini_file("../conf/environment.ini");
                                //$wsServerContent = parse_ini_file("../conf/webSocketServer.ini");
                                //$wsServerAddress = $wsServerContent["wsServerAddressWidgets"][$genFileContent['environment']['value']];
                                //$wsServerPort = $wsServerContent["wsServerPort"][$genFileContent['environment']['value']];
                                //$wsPath = $wsServerContent["wsServerPath"][$genFileContent['environment']['value']];
                                //$wsProtocol = $wsServerContent["wsServerProtocol"][$genFileContent['environment']['value']];
                                //$wsRetryActive = $wsServerContent["wsServerRetryActive"][$genFileContent['environment']['value']];
                                //$wsRetryTime = $wsServerContent["wsServerRetryTime"][$genFileContent['environment']['value']];
                                //echo 'wsRetryActive = "' . $wsRetryActive . '";';
                                //echo 'wsRetryTime = ' . $wsRetryTime . ';';
                                //echo 'webSocket = new WebSocket("' . $wsProtocol . '://' . $wsServerAddress . ':' . $wsServerPort . '/' . $wsPath . '");';
                                //?>
                                //webSocket.addEventListener('open', openWsConn);
                                //webSocket.addEventListener('close', wsClosed);
                                //setTimeout(function(){
                                //    webSocket.removeEventListener('close', wsClosed);
                                //    webSocket.removeEventListener('open', openWsConn);
                                //    webSocket.removeEventListener('message', manageIncomingWsMsg);
                                //    webSocket.close();
                                //    webSocket = null;
                                //}, (timeToReload - 2)*1000);
                                //}
                                //catch(e)
                                //{
                                //console.log("Widget " + widgetTitle + " could not connect to WebSocket");
                                //    wsClosed();
                                //}
                                //};

                                manageIncomingWsMsg = function (msg) {
                                    var msgObj = JSON.parse(msg.data);

                                    switch (msgObj.msgType) {
                                        case "newNRMetricData":
                                            if (encodeURIComponent(msgObj.metricName) === encodeURIComponent(metricName)) {
                                                /*   webSocket.close();
                                                   clearInterval(countdownRef);
                                                   <?= str_replace('.', '_', str_replace('-', '_', $_REQUEST['name_w'])) ?>(firstLoad, metricNameFromDriver, widgetTitleFromDriver, widgetHeaderColorFromDriver, widgetHeaderFontColorFromDriver, fromGisExternalContent, fromGisExternalContentServiceUri, fromGisExternalContentField, fromGisExternalContentRange, fromGisMarker, fromGisMapRef, fromGisFakeId);*/
                                                var newValue = msgObj.newValue;

                                                // TIME-ZONE CONVERSION and ADJUSTMENT TO ADD DIRECTLY INTO HIGHCHART SERIES (DEFAULT-UTC HIGHCHARTS MODE)
                                                var localTimeZone = moment.tz.guess();
                                                var momentDateTime = moment(Date.now());
                                                var offset = momentDateTime.utcOffset();
                                                var localDteTimeAdj = momentDateTime.tz(localTimeZone).valueOf() + 60000 * offset;

                                                chartRef.series[0].addPoint([localDteTimeAdj, newValue], true);

                                            }
                                            break;

                                        default:
                                            break;
                                    }
                                };

                                openWsConn = function (e) {
                                    var wsRegistration = {
                                        msgType: "ClientWidgetRegistration",
                                        userType: "widgetInstance",
                                        metricName: encodeURIComponent(metricName),
                                        widgetUniqueName: "<?= $_REQUEST['name_w'] ?>"
                                    };
                                    webSocket.send(JSON.stringify(wsRegistration));

                                    setTimeout(function () {
                                        webSocket.removeEventListener('close', wsClosed);
                                        webSocket.close();
                                    }, (timeToReload - 2) * 1000);

                                    webSocket.addEventListener('message', manageIncomingWsMsg);
                                };

                                wsClosed = function (e) {
                                    webSocket.removeEventListener('close', wsClosed);
                                    webSocket.removeEventListener('open', openWsConn);
                                    webSocket.removeEventListener('message', manageIncomingWsMsg);
                                    webSocket = null;
                                    if (wsRetryActive === 'yes') {
                                        setTimeout(openWs, parseInt(wsRetryTime * 1000));
                                    }
                                };

                                //Per ora non usata
                                //wsError = function(e) {};

                                $("#" + widgetName + "").on('customResizeEvent', function (event) {
                                    resizeWidget();
                                    $('#' + widgetName + '_chartContainer').highcharts().reflow();
                                });

                                $("#" + widgetName + "").off('updateFrequency');
                                $("#" + widgetName + "").on('updateFrequency', function (event) {
                                    clearInterval(countdownRef);
                                    timeToReload = event.newTimeToReload;
                                    countdownRef = startCountdown(widgetName, timeToReload, widgetName, metricNameFromDriver, widgetTitleFromDriver, widgetHeaderColorFromDriver, widgetHeaderFontColorFromDriver, fromGisExternalContent, fromGisExternalContentServiceUri, fromGisExternalContentField, fromGisExternalContentRange, fromGisMarker, fromGisMapRef, fromGisFakeId);
                                });

                                $("#<?= $_REQUEST['name_w'] ?>").off('changeTimeRangeEvent');
                                $("#<?= $_REQUEST['name_w'] ?>").on('changeTimeRangeEvent', function (event) {
                                    //    currentWidth = $('#'+ widgetName +'_chartContainer').height();
                                    //   $('#'+ widgetName +'_content').hide();
                                    $('#' + widgetName + '_loading').show();
                                    //    $('#'+ widgetName +'_chartContainer').css('height', currentWidth);
                                    timeRange = event.newTimeRange;
                                    populateWidget(event.newTimeRange, null, null, 0, null, udmFromUserOptions);
                                });

                                countdownRef = startCountdown(widgetName, timeToReload, widgetName, metricNameFromDriver, widgetTitleFromDriver, widgetHeaderColorFromDriver, widgetHeaderFontColorFromDriver, fromGisExternalContent, fromGisExternalContentServiceUri, fromGisExternalContentField, fromGisExternalContentRange, fromGisMarker, fromGisMapRef, fromGisFakeId);
                            },
                            error: function (errorData) {

                            }
                        });

                });//Fine document ready





