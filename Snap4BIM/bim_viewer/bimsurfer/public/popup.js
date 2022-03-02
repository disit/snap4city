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

import { UpdateUriPathVars } from "./update_uri_path_vars.js";

export class PopupBuilder {
    static create(popupId, serviceURI) {
        var mainPopupWindow = document.createElement("div");
        mainPopupWindow.id = popupId;
        mainPopupWindow.className = "popup-window";


        // close button
        var closeBtn = document.createElement("span");
        closeBtn.className = "close";
        closeBtn.innerHTML = "&times;";
        mainPopupWindow.appendChild(closeBtn);

        closeBtn.addEventListener("click", (e) => {
            //document.removeChild(mainPopupWindow);
            mainPopupWindow.style.display = "none";
        });

        var popupDataWindow = document.createElement("div");
        popupDataWindow.className = "popup-content";
        popupDataWindow.innerHTML = "Loading data..."
        mainPopupWindow.appendChild(popupDataWindow);

        fetchPopupContent(serviceURI, "bim_wigdget_popup", popupId, popupDataWindow);

        document.body.appendChild(mainPopupWindow);


        return mainPopupWindow;
    }

}

function fetchPopupContent(serviceUri, widgetName, popupId, popupWindow) {
    var snap4cityUrl = "https://www.snap4city.org/superservicemap/api/v1/?serviceUri=";
    var popupText, realTimeData, measuredTime, rtDataAgeSec, targetWidgets, color1, color2 = null;
    var urlToCall, fake, fakeId = null;

    popupId = popupId.replace(".", "");
    popupId = popupId.replace(".", "");

    urlToCall = snap4cityUrl + serviceUri + "&format=json&fullCount=false";

    fetch(urlToCall,
        {
            method: 'GET',
            mode: 'cors',
            cache: 'no-cache',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json'
            },
            redirect: 'follow',
            referrerPolicy: 'no-referrer'
        })
        .then(res => res.json())
        .then(data => {
            var fatherNode = null;

            if (data.hasOwnProperty("BusStop")) {
                fatherNode = data.BusStop;
            } else {
                if (data.hasOwnProperty("Sensor")) {
                    fatherNode = data.Sensor;
                } else {
                    fatherNode = data.Service;
                }
            }

            var udm = null;

            var serviceProperties = fatherNode.features[0].properties;
            var underscoreIndex = serviceProperties.serviceType.indexOf("_");
            var serviceClass = serviceProperties.serviceType.substr(0, underscoreIndex);
            var serviceSubclass = serviceProperties.serviceType.substr(underscoreIndex);
            serviceSubclass = serviceSubclass.replace(/_/g, " ");

            
            fatherNode.features[0].properties.targetWidgets = "widgetTimeTrend";
            fatherNode.features[0].properties.color1 = "#457B9D";
            fatherNode.features[0].properties.color2 = "#A8DADC";

            targetWidgets = "widgetTimeTrend";
            color1 = "#457B9D";
            color2 = "#A8DADC";

            //Popup nuovo stile uguali a quelli degli eventi ricreativi
            popupText = '<h3 class="recreativeEventMapTitle" style="background: ' + color1 + '; background: -webkit-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: -o-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: -moz-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: linear-gradient(to right, ' + color1 + ', ' + color2 + ');">' + serviceProperties.name + '</h3>';

            if ((serviceProperties.serviceUri !== '') && (serviceProperties.serviceUri !== undefined) && (serviceProperties.serviceUri !== 'undefined') && (serviceProperties.serviceUri !== null) && (serviceProperties.serviceUri !== 'null')) {
                popupText += '<div class="recreativeEventMapSubTitle" style="background: ' + color1 + '; background: -webkit-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: -o-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: -moz-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: linear-gradient(to right, ' + color1 + ', ' + color2 + ');">' + "Value Name: " + serviceProperties.serviceUri.split("/")[serviceProperties.serviceUri.split("/").length - 1] + '</div>';
            }

            popupText += '<div class="recreativeEventMapBtnContainer"><button data-id="' + popupId +
                '" class="recreativeEventMapDetailsBtn recreativeEventMapBtn recreativeEventMapBtnActive" type="button" style="background: ' +
                color1 + '; background: -webkit-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: -o-linear-gradient(right, ' +
                color1 + ', ' + color2 + '); background: -moz-linear-gradient(right, ' + color1 + ', ' + color2 +
                '); background: linear-gradient(to right, ' + color1 + ', ' + color2 + ');">Details</button><button data-id="' + popupId +
                '" class="recreativeEventMapDescriptionBtn recreativeEventMapBtn" type="button" style="background: ' + color1 +
                '; background: -webkit-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: -o-linear-gradient(right, ' +
                color1 + ', ' + color2 + '); background: -moz-linear-gradient(right, ' + color1 + ', ' + color2 +
                '); background: linear-gradient(to right, ' + color1 + ', ' + color2 + ');">Description</button><button data-id="' + popupId +
                '" class="recreativeEventMapContactsBtn recreativeEventMapBtn" type="button" style="background: ' + color1 +
                '; background: -webkit-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: -o-linear-gradient(right, ' +
                color1 + ', ' + color2 + '); background: -moz-linear-gradient(right, ' + color1 + ', ' + color2 +
                '); background: linear-gradient(to right, ' + color1 + ', ' + color2 + ');">RT data</button></div>';

            popupText += '<div class="recreativeEventMapDataContainer recreativeEventMapDetailsContainer">';

            popupText += '<table id="' + popupId + '" class="gisPopupGeneralDataTable">';
            //Intestazione
            popupText += '<thead>';
            popupText += '<th style="background: ' + color2 + '">Description</th>';
            popupText += '<th style="background: ' + color2 + '">Value</th>';
            popupText += '</thead>';

            //Corpo
            popupText += '<tbody>';

            for (var featureKey in serviceProperties) {
                if (serviceProperties.hasOwnProperty(featureKey)) {
                    if (serviceProperties[featureKey] != null && serviceProperties[featureKey] !== '' && serviceProperties[featureKey] !== ' ' && featureKey !== 'targetWidgets' && featureKey !== 'color1' && featureKey !== 'color2' && featureKey !== 'realtimeAttributes') {
                        if (!Array.isArray(serviceProperties[featureKey]) || (Array.isArray(serviceProperties[featureKey] && serviceProperties[featureKey].length > 0))) {
                            popupText += '<tr><td>' + featureKey + '</td><td>' + serviceProperties[featureKey] + '</td></tr>';
                        }
                    }
                }
            }

            popupText += '</tbody>';
            popupText += '</table>';

            if (data.hasOwnProperty('busLines')) {
                if (data.busLines.results.bindings.length > 0) {
                    popupText += '<b>Lines: </b>';
                    for (var i = 0; i < data.busLines.results.bindings.length; i++) {
                        popupText += '<span style="background: ' + color1 + '; background: -webkit-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: -o-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: -moz-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: linear-gradient(to right, ' + color1 + ', ' + color2 + ');">' + data.busLines.results.bindings[i].busLine.value + '</span> ';
                    }
                }
            }

            popupText += '</div>';

            popupText += '<div class="recreativeEventMapDataContainer recreativeEventMapDescContainer">';

            if ((serviceProperties.serviceUri !== '') && (serviceProperties.serviceUri !== undefined) && (serviceProperties.serviceUri !== 'undefined') && (serviceProperties.serviceUri !== null) && (serviceProperties.serviceUri !== 'null')) {
                popupText += "Value Name: " + serviceProperties.serviceUri.split("/")[serviceProperties.serviceUri.split("/").length - 1] + "<br>";
            }

            if ((serviceProperties.serviceType !== '') && (serviceProperties.serviceType !== undefined) && (serviceProperties.serviceType !== 'undefined') && (serviceProperties.serviceType !== null) && (serviceProperties.serviceType !== 'null')) {
                popupText += "Nature: " + serviceProperties.serviceType.split(/_(.+)/)[0] + "<br>";
                popupText += "Subnature: " + serviceProperties.serviceType.split(/_(.+)/)[1] + "<br><br>";
            }

            if (serviceProperties.hasOwnProperty('description')) {
                if ((serviceProperties.description !== '') && (serviceProperties.description !== undefined) && (serviceProperties.description !== 'undefined') && (serviceProperties.description !== null) && (serviceProperties.description !== 'null')) {
                    popupText += serviceProperties.description + "<br>";
                }
                else {
                    popupText += "No description available";
                }
            }
            else {
                popupText += 'No description available';
            }

            popupText += '</div>';
            popupText += '<div class="recreativeEventMapDataContainer recreativeEventMapContactsContainer">';
            var hasRealTime = false;


            // crea la tabella dentro il popup con i relativi dati
            if (data.hasOwnProperty("realtime")) {
                if (!jQuery.isEmptyObject(data.realtime)) {
                    realTimeData = data.realtime;
                    popupText += '<div class="popupLastUpdateContainer centerWithFlex"><b>Last update:&nbsp;</b><span class="popupLastUpdate" data-id="' + popupId + '"></span></div>';

                    if ((serviceClass.includes("Emergency")) && (serviceSubclass.includes("First aid"))) {
                        //Tabella ad hoc per First Aid
                        popupText += '<table id="' + popupId + '" class="psPopupTable">';
                        var series = {
                            "firstAxis": {
                                "desc": "Priority",
                                "labels": [
                                    "Red code",
                                    "Yellow code",
                                    "Green code",
                                    "Blue code",
                                    "White code"
                                ]
                            },
                            "secondAxis": {
                                "desc": "Status",
                                "labels": [],
                                "series": []
                            }
                        };

                        var dataSlot = null;

                        measuredTime = realTimeData.results.bindings[0].measuredTime.value.replace("T", " ").replace("Z", "");

                        for (var i = 0; i < realTimeData.results.bindings.length; i++) {
                            if (realTimeData.results.bindings[i].state.value.indexOf("estinazione") > 0) {
                                series.secondAxis.labels.push("Addressed");
                            }

                            if (realTimeData.results.bindings[i].state.value.indexOf("ttesa") > 0) {
                                series.secondAxis.labels.push("Waiting");
                            }

                            if (realTimeData.results.bindings[i].state.value.indexOf("isita") > 0) {
                                series.secondAxis.labels.push("In visit");
                            }

                            if (realTimeData.results.bindings[i].state.value.indexOf("emporanea") > 0) {
                                series.secondAxis.labels.push("Observation");
                            }

                            if (realTimeData.results.bindings[i].state.value.indexOf("tali") > 0) {
                                series.secondAxis.labels.push("Totals");
                            }

                            dataSlot = [];
                            dataSlot.push(realTimeData.results.bindings[i].redCode.value);
                            dataSlot.push(realTimeData.results.bindings[i].yellowCode.value);
                            dataSlot.push(realTimeData.results.bindings[i].greenCode.value);
                            dataSlot.push(realTimeData.results.bindings[i].blueCode.value);
                            dataSlot.push(realTimeData.results.bindings[i].whiteCode.value);

                            series.secondAxis.series.push(dataSlot);
                        }

                        var colsQt = parseInt(parseInt(series.firstAxis.labels.length) + 1);
                        var rowsQt = parseInt(parseInt(series.secondAxis.labels.length) + 1);

                        for (var i = 0; i < rowsQt; i++) {
                            var newRow = $("<tr></tr>");
                            var z = parseInt(parseInt(i) - 1);

                            if (i === 0) {
                                //Riga di intestazione
                                for (var j = 0; j < colsQt; j++) {
                                    if (j === 0) {
                                        //Cella (0,0)
                                        var newCell = $("<td></td>");

                                        newCell.css("background-color", "transparent");
                                    }
                                    else {
                                        //Celle labels
                                        var k = parseInt(parseInt(j) - 1);
                                        var colLabelBckColor = null;
                                        switch (k) {
                                            case 0:
                                                colLabelBckColor = "#ff0000";
                                                break;

                                            case 1:
                                                colLabelBckColor = "#ffff00";
                                                break;

                                            case 2:
                                                colLabelBckColor = "#66ff33";
                                                break;

                                            case 3:
                                                colLabelBckColor = "#66ccff";
                                                break;

                                            case 4:
                                                colLabelBckColor = "#ffffff";
                                                break;
                                        }

                                        newCell = $("<td><span>" + series.firstAxis.labels[k] + "</span></td>");
                                        newCell.css("font-weight", "bold");
                                        newCell.css("background-color", colLabelBckColor);
                                    }
                                    newRow.append(newCell);
                                }
                            }
                            else {
                                //Righe dati
                                for (var j = 0; j < colsQt; j++) {
                                    k = parseInt(parseInt(j) - 1);
                                    if (j === 0) {
                                        //Cella label
                                        newCell = $("<td>" + series.secondAxis.labels[z] + "</td>");
                                        newCell.css("font-weight", "bold");
                                    }
                                    else {
                                        //Celle dati
                                        newCell = $("<td>" + series.secondAxis.series[z][k] + "</td>");
                                        if (i === (rowsQt - 1)) {
                                            newCell.css('font-weight', 'bold');
                                            switch (j) {
                                                case 1:
                                                    newCell.css('background-color', '#ffb3b3');
                                                    break;

                                                case 2:
                                                    newCell.css('background-color', '#ffff99');
                                                    break;

                                                case 3:
                                                    newCell.css('background-color', '#d9ffcc');
                                                    break;

                                                case 4:
                                                    newCell.css('background-color', '#cceeff');
                                                    break;

                                                case 5:
                                                    newCell.css('background-color', 'white');
                                                    break;
                                            }
                                        }
                                    }
                                    newRow.append(newCell);
                                }
                            }
                            popupText += newRow.prop('outerHTML');
                        }

                        popupText += '</table>';
                    }
                    else {
                        //Tabella nuovo stile
                        popupText += '<table id="' + popupId + '" class="gisPopupTable">';

                        //Intestazione
                        popupText += '<thead>';
                        popupText += '<th style="background: ' + color1 + '; background: -webkit-linear-gradient(right, ' + 
                        color1 + ', ' + color2 + '); background: -o-linear-gradient(right, ' + color1 + ', ' + color2 + 
                        '); background: -moz-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: linear-gradient(to right, ' + 
                        color1 + ', ' + color2 + ');">Description</th>';
                        
                        popupText += '<th style="background: ' + color1 + '; background: -webkit-linear-gradient(right, ' + 
                        color1 + ', ' + color2 + '); background: -o-linear-gradient(right, ' + color1 + ', ' + color2 + 
                        '); background: -moz-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: linear-gradient(to right, ' + 
                        color1 + ', ' + color2 + ');">Value</th>';
                        
                        
                        
                        popupText += '<th colspan="7" style="background: ' + color1 + '; background: -webkit-linear-gradient(right, ' + 
                        color1 + ', ' + color2 + '); background: -o-linear-gradient(right, ' + color1 + ', ' + color2 + 
                        '); background: -moz-linear-gradient(right, ' + color1 + ', ' + color2 + '); background: linear-gradient(to right, ' + 
                        color1 + ', ' + color2 + ');">Buttons</th>';
                        
                        popupText += '</thead>';

                        //Corpo
                        popupText += '<tbody>';
                        var dataDesc, dataVal, dataLastBtn, data4HBtn, dataDayBtn, data7DayBtn, data30DayBtn, data6MonthsBtn, data1YearBtn = null;

                        for (var i = 0; i < realTimeData.head.vars.length; i++) {
                            if (realTimeData.results.bindings[0][realTimeData.head.vars[i]] !== null && realTimeData.results.bindings[0][realTimeData.head.vars[i]] !== undefined) {
                                if ((realTimeData.results.bindings[0][realTimeData.head.vars[i]]) && (realTimeData.results.bindings[0][realTimeData.head.vars[i]].value.trim() !== '') && (realTimeData.head.vars[i] !== null) && (realTimeData.head.vars[i] !== 'undefined')) {
                                    if ((realTimeData.head.vars[i] !== 'updating') && (realTimeData.head.vars[i] !== 'measuredTime') && (realTimeData.head.vars[i] !== 'instantTime')) {
                                        if (!realTimeData.results.bindings[0][realTimeData.head.vars[i]].value.includes('Not Available')) {

                                            dataDesc = realTimeData.head.vars[i];
                                            dataVal = realTimeData.results.bindings[0][realTimeData.head.vars[i]].value;
                                            dataLastBtn = '<td><button data-id="' + popupId +
                                                '" type="button" class="lastValueBtn btn btn-sm" data-fake="' + fake + '" data-fakeid="' + fakeId + '" data-id="' + //latLngId + 
                                                '" data-field="' + realTimeData.head.vars[i] + '" data-serviceUri="' + serviceUri +
                                                '" data-lastDataClicked="false" data-targetWidgets="' + targetWidgets + '" data-lastValue="' +
                                                realTimeData.results.bindings[0][realTimeData.head.vars[i]].value + '" data-color1="' + color1 +
                                                '" data-color2="' + color2 + '">Last<br>value</button></td>';

                                            data4HBtn = '<td><button data-id="' + popupId +
                                                '" type="button" class="timeTrendBtn btn btn-sm" data-fake="' + fake + '" data-fakeid="' + fakeId + '" data-id="' + //latLngId + 
                                                '" data-field="' + realTimeData.head.vars[i] + '" data-serviceUri="' + serviceUri +
                                                '" data-timeTrendClicked="false" data-range-shown="4 Hours" data-range="4/HOUR" data-targetWidgets="' +
                                                targetWidgets + '" data-color1="' + color1 + '" data-color2="' + color2 + '">Last<br>4 hours</button></td>';

                                            dataDayBtn = '<td><button data-id="' + popupId +
                                                '" type="button" class="timeTrendBtn btn btn-sm" data-fake="' + fake + '" data-id="' + fakeId + '" data-field="' +
                                                realTimeData.head.vars[i] + '" data-serviceUri="' + serviceUri +
                                                '" data-timeTrendClicked="false" data-range-shown="Day" data-range="1/DAY" data-targetWidgets="' +
                                                targetWidgets + '" data-color1="' + color1 + '" data-color2="' + color2 + '">Last<br>24 hours</button></td>';

                                            data7DayBtn = '<td><button data-id="' + popupId +
                                                '" type="button" class="timeTrendBtn btn btn-sm" data-fake="' + fake + '" data-id="' + fakeId + '" data-field="' +
                                                realTimeData.head.vars[i] + '" data-serviceUri="' + serviceUri +
                                                '" data-timeTrendClicked="false" data-range-shown="7 days" data-range="7/DAY" data-targetWidgets="' +
                                                targetWidgets + '" data-color1="' + color1 + '" data-color2="' + color2 + '">Last<br>7 days</button></td>';

                                            data30DayBtn = '<td><button data-id="' + popupId +
                                                '" type="button" class="timeTrendBtn btn btn-sm" data-fake="' + fake + '" data-id="' + fakeId + '" data-field="' +
                                                realTimeData.head.vars[i] + '" data-serviceUri="' + serviceUri +
                                                '" data-timeTrendClicked="false" data-range-shown="30 days" data-range="30/DAY" data-targetWidgets="' +
                                                targetWidgets + '" data-color1="' + color1 + '" data-color2="' + color2 + '">Last<br>30 days</button></td>';

                                            data6MonthsBtn = '<td><button data-id="' + popupId + '" type="button" class="timeTrendBtn btn btn-sm" data-fake="' + 
                                                fake + '" data-id="' + fakeId + '" data-field="' + realTimeData.head.vars[i] + '" data-serviceUri="' + 
                                                serviceUri + '" data-timeTrendClicked="false" data-range-shown="6 months" data-range="180/DAY" data-targetWidgets="' + 
                                                targetWidgets + '" data-color1="' + color1 + '" data-color2="' + color2 + '">Last<br>6 months</button></td>';
                                            
                                            data1YearBtn = '<td><button data-id="' + popupId + '" type="button" class="timeTrendBtn btn btn-sm" data-fake="' + 
                                                fake + '" data-id="' + fakeId + '" data-field="' + realTimeData.head.vars[i] + '" data-serviceUri="' + 
                                                serviceUri + '" data-timeTrendClicked="false" data-range-shown="1 year" data-range="365/DAY" data-targetWidgets="' + 
                                                targetWidgets + '" data-color1="' + color1 + '" data-color2="' + color2 + '">Last<br>1 year</button></td>';
                                            
                                            popupText += '<tr><td>' + dataDesc + '</td><td>' + dataVal + '</td>' + dataLastBtn + data4HBtn + dataDayBtn + data7DayBtn + data30DayBtn + data6MonthsBtn + data1YearBtn + '</tr>';
                                        }
                                    } else {
                                        measuredTime = realTimeData.results.bindings[0][realTimeData.head.vars[i]].value.replace("T", " ");
                                        var now = new Date();
                                        var measuredTimeDate = new Date(measuredTime);
                                        rtDataAgeSec = Math.abs(now - measuredTimeDate) / 1000;
                                    }
                                }
                            }
                        }
                        popupText += '</tbody>';
                        popupText += '</table>';
                        //popupText += '<p><b>Keep data on target widget(s) after popup close: </b><input data-id="' + popupId +
                        //    '" type="checkbox" class="gisPopupKeepDataCheck" data-keepData="false"/></p>';
                    }

                    hasRealTime = true;
                }
            }

            popupText += '</div>';

            popupWindow.innerHTML = popupText;

            // carica i scripts necessari per il corretto funzionamento del popup 
            $(document).ready(function () {

                if (hasRealTime) {
                    $('button.recreativeEventMapContactsBtn[data-id="' + popupId + '"]').show();
                    $('button.recreativeEventMapContactsBtn[data-id="' + popupId + '"]').trigger("click");
                    $('span.popupLastUpdate[data-id="' + popupId + '"]').html(measuredTime);
                }
                else {
                    $('button.recreativeEventMapContactsBtn[data-id="' + popupId + '"]').hide();
                }

                $('button.recreativeEventMapDetailsBtn[data-id="' + popupId + '"]').off('click');
                $('button.recreativeEventMapDetailsBtn[data-id="' + popupId + '"]').click(function () {
                    $('div.recreativeEventMapDataContainer').hide();
                    $('div.recreativeEventMapDetailsContainer').show();
                    $('button.recreativeEventMapBtn').removeClass('recreativeEventMapBtnActive');
                    $(this).addClass('recreativeEventMapBtnActive');
                });

                $('button.recreativeEventMapDescriptionBtn[data-id="' + popupId + '"]').off('click');
                $('button.recreativeEventMapDescriptionBtn[data-id="' + popupId + '"]').click(function () {
                    $('div.recreativeEventMapDataContainer').hide();
                    $('div.recreativeEventMapDescContainer').show();
                    $('button.recreativeEventMapBtn').removeClass('recreativeEventMapBtnActive');
                    $(this).addClass('recreativeEventMapBtnActive');
                });

                $('button.recreativeEventMapContactsBtn[data-id="' + popupId + '"]').off('click');
                $('button.recreativeEventMapContactsBtn[data-id="' + popupId + '"]').click(function () {
                    $('div.recreativeEventMapDataContainer').hide();
                    $('div.recreativeEventMapContactsContainer').show();
                    $('button.recreativeEventMapBtn').removeClass('recreativeEventMapBtnActive');
                    $(this).addClass('recreativeEventMapBtnActive');
                });


                if (hasRealTime) {
                    $('button.recreativeEventMapContactsBtn[data-id="' + popupId + '"]').trigger("click");
                }

                $('table.gisPopupTable[id="' + popupId + '"] button.btn-sm').css("background", color2);
                $('table.gisPopupTable[id="' + popupId + '"] button.btn-sm').css("border", "none");
                $('table.gisPopupTable[id="' + popupId + '"] button.btn-sm').css("color", "black");

                $('table.gisPopupTable[id="' + popupId + '"] button.btn-sm').focus(function () {
                    $(this).css("outline", "0");
                });


                // these buttons should display a graph
                $('button.lastValueBtn').off('mouseenter');
                $('button.lastValueBtn').off('mouseleave');
                $('button.lastValueBtn[data-id="' + popupId + '"]').hover(function () {
                    if ($(this).attr("data-lastDataClicked") === "false") {
                        $(this).css("background", color1);
                        $(this).css("background", "-webkit-linear-gradient(left, " + color1 + ", " + color2 + ")");
                        $(this).css("background", "background: -o-linear-gradient(left, " + color1 + ", " + color2 + ")");
                        $(this).css("background", "background: -moz-linear-gradient(left, " + color1 + ", " + color2 + ")");
                        $(this).css("background", "background: linear-gradient(to left, " + color1 + ", " + color2 + ")");
                        $(this).css("font-weight", "bold");
                    }

                    var widgetTargetList = $(this).attr("data-targetWidgets").split(',');
                    var colIndex = $(this).parent().index();
                    //var title = $(this).parents("tbody").find("tr").eq(0).find("th").eq(colIndex).html();
                    var title = $(this).parents("tr").find("td").eq(0).html();

                    for (var i = 0; i < widgetTargetList.length; i++) {
                        $.event.trigger({
                            type: "mouseOverLastDataFromExternalContentGis_" + widgetTargetList[i],
                            eventGenerator: $(this),
                            targetWidget: widgetTargetList[i],
                            value: $(this).attr("data-lastValue"),
                            color1: $(this).attr("data-color1"),
                            color2: $(this).attr("data-color2"),
                            widgetTitle: title
                        });
                    }
                },
                    function () {
                        if ($(this).attr("data-lastDataClicked") === "false") {
                            $(this).css("background", color2);
                            $(this).css("font-weight", "normal");
                        }
                        var widgetTargetList = $(this).attr("data-targetWidgets").split(',');

                        for (var i = 0; i < widgetTargetList.length; i++) {
                            $.event.trigger({
                                type: "mouseOutLastDataFromExternalContentGis_" + widgetTargetList[i],
                                eventGenerator: $(this),
                                targetWidget: widgetTargetList[i],
                                value: $(this).attr("data-lastValue"),
                                color1: $(this).attr("data-color1"),
                                color2: $(this).attr("data-color2")
                            });
                        }
                    });


                //Disabilitiamo i 4Hours se last update più vecchio di 4 ore
                if (rtDataAgeSec > 14400) {
                    $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="4/HOUR"]').attr("data-disabled", "true");
                    //Disabilitiamo i 24Hours se last update più vecchio di 24 ore
                    if (rtDataAgeSec > 86400) {
                        $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="1/DAY"]').attr("data-disabled", "true");
                        //Disabilitiamo i 7 days se last update più vecchio di 7 days
                        if (rtDataAgeSec > 604800) {
                            $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="7/DAY"]').attr("data-disabled", "true");
                            //Disabilitiamo i 30 days se last update più vecchio di 30 days
                            ///if (rtDataAgeSec > 18144000) {
                            if(rtDataAgeSec > 2592000) {
                                $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="30/DAY"]').attr("data-disabled", "true");
                                if(rtDataAgeSec > 15552000) {
                                    $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="180/DAY"]').attr("data-disabled", "true");
                                    //Disabilitiamo i 1 year se last update più vecchio di 365 days
                                    if(rtDataAgeSec > 31536000) {
                                        $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="365/DAY"]').attr("data-disabled", "true");
                                    } else {
                                        $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="365/DAY"]').attr("data-disabled", "false");
                                    }
                                } else {
                                    $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="180/DAY"]').attr("data-disabled", "false");
                                }
                            } else {
                                $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="30/DAY"]').attr("data-disabled", "false");
                            }
                        } else {
                            $('#widget_name_modalLinkOpen button.timeTrendBtn[data-id="' + popupId + '"][data-range="7/DAY"]').attr("data-disabled", "false");
                        }
                    } else {
                        $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="1/DAY"]').attr("data-disabled", "false");
                    }
                } else {
                    $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="4/HOUR"]').attr("data-disabled", "false");
                    $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="1/DAY"]').attr("data-disabled", "false");
                    $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="7/DAY"]').attr("data-disabled", "false");
                    $('button.timeTrendBtn[data-id="' + popupId + '"][data-range="30/DAY"]').attr("data-disabled", "false");
                }

                $('button.timeTrendBtn').off('mouseenter');
                $('button.timeTrendBtn').off('mouseleave');
                $('button.timeTrendBtn[data-id="' + popupId + '"]').hover(function () {
                    if (isNaN(parseFloat($(this).parents('tr').find('td').eq(1).html())) || ($(this).attr("data-disabled") === "true")) {
                        $(this).css("background-color", "#e6e6e6");
                        $(this).off("hover");
                        $(this).off("click");
                    }
                    else {
                        if ($(this).attr("data-timeTrendClicked") === "false") {
                            $(this).css("background", color1);
                            $(this).css("background", "-webkit-linear-gradient(left, " + color1 + ", " + color2 + ")");
                            $(this).css("background", "background: -o-linear-gradient(left, " + color1 + ", " + color2 + ")");
                            $(this).css("background", "background: -moz-linear-gradient(left, " + color1 + ", " + color2 + ")");
                            $(this).css("background", "background: linear-gradient(to left, " + color1 + ", " + color2 + ")");
                            $(this).css("font-weight", "bold");
                        }

                        var widgetTargetList = $(this).attr("data-targetWidgets").split(',');
                       
                        var title = $(this).parents("tr").find("td").eq(0).html() + " - " + $(this).attr("data-range-shown");

                        for (var i = 0; i < widgetTargetList.length; i++) {
                            $.event.trigger({
                                type: "mouseOverTimeTrendFromExternalContentGis_" + widgetTargetList[i],
                                eventGenerator: $(this),
                                targetWidget: widgetTargetList[i],
                                value: $(this).attr("data-lastValue"),
                                color1: $(this).attr("data-color1"),
                                color2: $(this).attr("data-color2"),
                                widgetTitle: title
                            });
                        }
                    }
                },
                    function () {
                        if (isNaN(parseFloat($(this).parents('tr').find('td').eq(1).html())) || ($(this).attr("data-disabled") === "true")) {
                            $(this).css("background-color", "#e6e6e6");
                            $(this).off("hover");
                            $(this).off("click");
                        }
                        else {
                            if ($(this).attr("data-timeTrendClicked") === "false") {
                                $(this).css("background", color2);
                                $(this).css("font-weight", "normal");
                            }

                            var widgetTargetList = $(this).attr("data-targetWidgets").split(',');
                            for (var i = 0; i < widgetTargetList.length; i++) {
                                $.event.trigger({
                                    type: "mouseOutTimeTrendFromExternalContentGis_" + widgetTargetList[i],
                                    eventGenerator: $(this),
                                    targetWidget: widgetTargetList[i],
                                    value: $(this).attr("data-lastValue"),
                                    color1: $(this).attr("data-color1"),
                                    color2: $(this).attr("data-color2")
                                });
                            }
                        }
                    });

                $('button.lastValueBtn[data-id=' + popupId + ']').off('click');
                $('button.lastValueBtn[data-id=' + popupId + ']').click(
                    function (event) {
                        $('button.lastValueBtn').each(function (i) {
                            $(this).css("background", $(this).attr("data-color2"));
                        });
                        $('button.lastValueBtn').css("font-weight", "normal");
                        $(this).css("background", $(this).attr("data-color1"));
                        $(this).css("font-weight", "bold");
                        $('button.lastValueBtn').attr("data-lastDataClicked", "false");
                        $(this).attr("data-lastDataClicked", "true");
                        var widgetTargetList = $(this).attr("data-targetWidgets").split(',');
                        var colIndex = $(this).parent().index();
                        var title = $(this).parents("tr").find("td").eq(0).html();
                        var value = $(this).attr("data-lastValue");
                        var field = $(this).attr("data-field");

                        document.getElementById("data-chart-visualizer").style.display = "block";
                        document.getElementById("timetrendVisibilityBtn").classList.add("active");
                        UpdateUriPathVars.setTimetrendVisibility("on");

                        if (fatherNode.features[0].properties.realtimeAttributes[field] != null) {
                            if (fatherNode.features[0].properties.realtimeAttributes[field].value_unit != null) {
                                udm = fatherNode.features[0].properties.realtimeAttributes[field].value_unit;
                            }
                        }

                        let lastValueData = "";
                        let parsedValue = parseFloat(value).toFixed(2);

                        if (!(udm == null || udm == undefined)) {
                            
                            if (parsedValue.isNaN || parsedValue.isEmptyObject || parsedValue  === "NaN") {
                                lastValueData = value + " " + udm; 
                            } else {
                                lastValueData = parsedValue + " " + udm;
                            }

                            document.getElementById('last-value').innerHTML = lastValueData;
                        } else {
                            if (parsedValue.isNaN || parsedValue.isEmptyObject || parsedValue  === "NaN") {
                                lastValueData = value
                            } else {
                                lastValueData = parsedValue
                            }
                            document.getElementById('last-value').innerHTML = lastValueData;
                        }

                        $('button.timeTrendBtn[data-id="' + popupId + '"]').each(function (i) {
                            if (isNaN(parseFloat($(this).parents('tr').find('td').eq(1).html())) || ($(this).attr("data-disabled") === "true")) {
                                $(this).css("background-color", "#e6e6e6");
                                $(this).off("hover");
                                $(this).off("click");
                            }
                        });

                    });

                $('button.timeTrendBtn').off('click');
                $('button.timeTrendBtn').click(function (event) {
                    document.getElementById("data-chart-visualizer").style.display = "block";
                    document.getElementById("timetrendVisibilityBtn").classList.add("active");
                    UpdateUriPathVars.setTimetrendVisibility("on");

                    // loading icong on time trend widget
                    document.getElementById("time-trend-chart").innerHTML = '<div id="loading"><div class="loader"></div><span>Loading</span></div>';

                    if (isNaN(parseFloat($(this).parents('tr').find('td').eq(1).html())) || ($(this).attr("data-disabled") === "true")) {
                        $(this).css("background-color", "#e6e6e6");
                        $(this).off("hover");
                        $(this).off("click");
                    } else {
                        $('button.timeTrendBtn').css("background", $(this).attr("data-color2"));
                        $('button.timeTrendBtn').css("font-weight", "normal");
                        $(this).css("background", $(this).attr("data-color1"));
                        $(this).css("font-weight", "bold");
                        $('button.timeTrendBtn').attr("data-timeTrendClicked", "false");
                        $(this).attr("data-timeTrendClicked", "true");
                        var widgetTargetList = $(this).attr("data-targetWidgets").split(',');
                        var colIndex = $(this).parent().index();
                        var title = $(this).parents("tr").find("td").eq(0).html() + " - " + $(this).attr("data-range-shown");
                        var lastUpdateTime = $(this).parents('div.recreativeEventMapContactsContainer').find('span.popupLastUpdate').html();


                        var now = new Date();
                        var lastUpdateDate = new Date(lastUpdateTime);
                        var diff = parseFloat(Math.abs(now - lastUpdateDate) / 1000);
                        var range = $(this).attr("data-range");
                        var serviceURI = $(this).attr("data-serviceUri");
                        var field = $(this).attr("data-field");

                        var dataRangeParams = timeRangeConverter(range);

                        if (fatherNode.features[0].properties.realtimeAttributes[field] != null) {
                            if (fatherNode.features[0].properties.realtimeAttributes[field].value_unit != null) {
                                udm = fatherNode.features[0].properties.realtimeAttributes[field].value_unit;
                            }
                        }

                        // url for getting realtime data
                        let url_realt_time_data = snap4cityUrl + serviceURI + dataRangeParams +"&valueName=" + field;

                        fetch(url_realt_time_data)
                            .then(r => r.json())
                            .then(d => {
                                
                                var udmFromUser = null;
                                
                                let dd = convertDataFromSmToDm(d, field, udm, udmFromUser);
                                
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
                                
                                document.getElementById('time-trend-title').innerHTML = "Time Trend Chart: " + title;
                                if (dd) {
                                    drawDiagram(dd, range, field, true, localTimeZoneString, udm);
                                } else {
                                    console.log("No data from this pin");
                                    document.getElementById('time-trend-chart').innerHTML = "<span class='timetrend-error'>No data from this pin!</span>";
                                }
                                
                            });

                        

                        $('button.timeTrendBtn[data-id="' + popupId + '"]').each(function (i) {
                            if (isNaN(parseFloat($(this).parents('tr').find('td').eq(1).html())) || ($(this).attr("data-disabled") === "true")) {
                                $(this).css("background-color", "#e6e6e6");
                                $(this).off("hover");
                                $(this).off("click");
                            }
                        });
                    }
                });

                $('button.timeTrendBtn[data-id="' + popupId + '"]').each(function (i) {
                    if (isNaN(parseFloat($(this).parents('tr').find('td').eq(1).html())) || ($(this).attr("data-disabled") === "true")) {
                        $(this).css("background-color", "#e6e6e6");
                        $(this).off("hover");
                        $(this).off("click");
                    }
                });

                $('div.leaflet-popup').off('click');
            });
        });

    // end of ajax call 

}

// converts data for highcharts
function convertDataFromSmToDm(originalData, field, udm,  udmFromUserOptions) {
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

function convertDataFromTimeNavToDm(originalData, field, udm, udmFromUserOptions) {
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

function getUpperTimeLimit(hours) {        
    let now = new Date();
    var nowUTC = now.toUTCString();
    var isoDate = new Date(nowUTC).toISOString();
    let timeZoneOffsetHours = now.getTimezoneOffset() / 60;
    let upperTimeLimit = now.setHours(now.getHours() - hours - timeZoneOffsetHours);
    let upperTimeLimitUTC = new Date(upperTimeLimit).toUTCString();
    let upperTimeLimitISO = new Date(upperTimeLimitUTC).toISOString();
    let upperTimeLimitISOTrim = upperTimeLimitISO.substring(0, isoDate.length - 5);
    return upperTimeLimitISOTrim;
}

function timeRangeConverter(range) {
    var serviceMapTimeRange, upperTimeLimitISOTrimmed = null;
    var timeCount = 0;

    switch (range) {
        case "4/HOUR":
            serviceMapTimeRange = "fromTime=4-hour";
            upperTimeLimitISOTrimmed = getUpperTimeLimit(4 * timeCount);
            break;

        case "1/DAY":
            serviceMapTimeRange = "fromTime=1-day";
            upperTimeLimitISOTrimmed = getUpperTimeLimit(24 * timeCount);
            break;

        case "7/DAY":
            serviceMapTimeRange = "fromTime=7-day";
            upperTimeLimitISOTrimmed = getUpperTimeLimit(7 * 24 * timeCount);
            break;

        case "30/DAY":
            serviceMapTimeRange = "fromTime=30-day";
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

    return "&" + serviceMapTimeRange + "&toTime=" + upperTimeLimitISOTrimmed;

}

function compareSeriesData(a, b) {
            var x = a[0];
            var y = b[0];
            
            if(x < y) {
                return -1
            } else {
                if(x > y) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

function drawDiagram(metricData, timeRange, seriesName, fromSelector, timeZone, udm) {
    var desc, metricType, seriesData, valuesData, lastDateinDataArray, day, value, flagNumeric, dayParts, timeParts,
    unitsWidget, date, maxValue, minValue, nInterval, xOffsetUdm, plotLinesArray, plotLineObj, titleUdm, thresholdObject, chartRef = null; 

    var firstLoad = true;
    var widgetName = "time-trend-chart";
    var chartColor = "rgb(51, 204, 255)";
    var viewUdm = "yes";

    if (metricData.data.length > 0) {
        desc = metricData.data[0].commit.author.descrip;
        metricType = 'IBIMET_SMART09';
        seriesData = [];
        valuesData = [];
        lastDateinDataArray = null;

        if (metricData.data[0]) {
            lastDateinDataArray = metricData.data[0].commit.author.computationDate;
        }
        for (var i = 0; i < metricData.data.length; i++) {
            day = metricData.data[i].commit.author.computationDate;

            if ((metricData.data[i].commit.author.value !== null) && (metricData.data[i].commit.author.value !== "")) {

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
            //showWidgetContent(widgetName);
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
            chartRef = Highcharts.chart('time-trend-chart', {
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
            chartRef = Highcharts.chart('time-trend-chart', {
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
                    lineColor: "rgba(238, 238, 238, 1)",//chartAxesColor,
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
                            color: "#000000",//chartLabelsFontColor,
                            fontSize: 12 + "px",//fontSize + "px",
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
                    lineColor: "rgba(238, 238, 238, 1)",//chartAxesColor,
                    lineWidth: 1,
                    className: 'timeTrendYAxis',
                    gridLineColor: "rgba(238, 238, 238, 1)",//gridLineColor,
                    labels: {
                        enabled: true,
                        style: {
                            fontFamily: 'Montserrat',
                            color: "#000000",//chartLabelsFontColor,
                            fontSize: 12 + "px",//fontSize + "px",
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
                    color: "rgb(51, 204, 255)",//chartColor,
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
        //showWidgetContent(widgetName);
        $("#" + widgetName + "_chartContainer").hide();
        $('#' + widgetName + '_noDataAlert').show();
    }
    //showWidgetContent(widgetName);
}
