var EditModalManager = {

    createMap: function () {

        map = L.map('mapContainer').setView(EditModalManager.currentCoordinates, 9);
        L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);

        var mapLayers = {};

        drawnItems = new L.FeatureGroup();
        map.addLayer(drawnItems);

        var editControl = new L.Control.Draw({
            draw: false,
            edit: {
                featureGroup: drawnItems
            }
        });
        map.addControl(editControl);

        drawControl = new L.Control.Draw({
            draw: {
                position: 'topleft',
                polyline: false,
                marker: {
                    icon: new L.DivIcon({
                        iconSize: new L.Point(8, 8),
                        className: 'leaflet-div-icon leaflet-editing-icon test'
                    })
                },
                circlemarker: false,
                circle: false,
                polygon: false,
                rectangle: false
            }
        });
        map.addControl(drawControl);

        L.control.layers(mapLayers, {
            'drawlayer': drawnItems
        }, {
            collapsed: true
        }).addTo(map);

        map.on(L.Draw.Event.CREATED, function (e) {
            var fence = e.layer;
            if (drawnItems.hasLayer(fence) == false) {
                drawnItems.addLayer(fence);
            }

            drawControl.remove();

            marker = {};

            drawnItems.eachLayer(function (layer) {
                marker = layer.toGeoJSON();
            });

            $("#inputLatitude" + EditModalManager.editType).val(marker.geometry.coordinates[1]);
            $("#inputLongitude" + EditModalManager.editType).val(marker.geometry.coordinates[0]);
        });

        map.on('draw:edited', function (e) {
            var fences = e.layers;
            fences.eachLayer(function (fence) {
                fence.shape = "geofence";
                if (drawnItems.hasLayer(fence) == false) {
                    drawnItems.addLayer(fence);
                }
            });

            marker = {};

            drawnItems.eachLayer(function (layer) {
                marker = layer.toGeoJSON();
            });

            $("#inputLatitude" + EditModalManager.editType).val(marker.geometry.coordinates[1]);
            $("#inputLongitude" + EditModalManager.editType).val(marker.geometry.coordinates[0]);
        });

        map.on('draw:deleted', function (e) {
            drawControl.addTo(map);
            $("#inputLatitude" + EditModalManager.editType).val("");
            $("#inputLongitude" + EditModalManager.editType).val("");
        });

        if (EditModalManager.currentLatitude != null && EditModalManager.currentLatitude != "" && EditModalManager.currentLongitude != null && EditModalManager.currentLongitude != "") {
            L.marker([
                EditModalManager.currentLatitude,
                EditModalManager.currentLongitude
            ]).addTo(drawnItems);

            drawControl.remove();

            map.setView([EditModalManager.currentLatitude,EditModalManager.currentLongitude], 11);

            EditModalManager.currentLatitude = null;
            EditModalManager.currentLongitude = null;
        }

        map.invalidateSize(true);
    },


    checkOrganizationAndCreateMap: function (editType) {
        EditModalManager.editType = editType;
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetUsernameOrganizationQuery();
            APIClient.executeGetQuery(query, KPIEditor.keycloak.token, EditModalManager.successCheckOrganization, EditModalManager.errorCheckOrganization);
        }).error(function () {
            var query = QueryManager.createGetUsernameOrganizationQuery();
            APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), EditModalManager.successCheckOrganization, EditModalManager.errorCheckOrganization);

        });
    },

    successCheckOrganization: function (_data) {
        if (_data != null) {
            var organization = _data.substring(_data.indexOf("=") + 1, _data.indexOf(","));
            if (organization.toLowerCase() == "helsinki") {
                EditModalManager.currentCoordinates = [60.169286, 24.939103];
            } else if (organization.toLowerCase() == "antwerp") {
                EditModalManager.currentCoordinates = [51.216784, 4.405688];
            } else {
                EditModalManager.currentCoordinates = [43.78, 11.23];
            }
        } else {
            EditModalManager.currentCoordinates = [43.78, 11.23];
        }

        EditModalManager.createMap();
    },

    errorCheckOrganization: function (_error) {
        console.log(_error);
        if (_error != null && typeof _error == "string") {
            var organization = _error.substring(_error.indexOf("=") + 1, _error.indexOf(","));
            if (organization.toLowerCase() == "helsinki") {
                EditModalManager.currentCoordinates = [60.169286, 24.939103];
            } else if (organization.toLowerCase() == "antwerp") {
                EditModalManager.currentCoordinates = [51.216784, 4.405688];
            } else {
                EditModalManager.currentCoordinates = [43.78, 11.23];
            }
        } else {
            EditModalManager.currentCoordinates = [43.78, 11.23];
        }

        EditModalManager.createMap();
    },

    createNatureSelection: function (_savedNature, _savedSubNature) {
        EditModalManager.savedNature = _savedNature;
        EditModalManager.savedSubNature = _savedSubNature;
        $.ajax({
            url: EditModalManager.dictionaryUrl + "?type=nature",
            cache: false,
            dataType: "json",
            success: function (_data) {
                var natureArray = [];
                for (var i = 0; i < _data.content.length; i++) {
                    natureArray.push({
                        "key": _data.content[i].value,
                        "id": _data.content[i].id,
                        "value": _data.content[i].label
                    });
                }
                ViewManager.render({
                    "arrayToSelection": natureArray
                }, "#selectNatureKPIDataEdit", "templates/arrayToSelection.mst.html");
                $("#selectNatureKPIDataEdit").on('change', function () {
                    $("#selectSubNatureKPIDataEdit").html("");
                    EditModalManager.createSubNatureSelection($("#selectNatureKPIDataEdit").find(':selected').attr('data-id'));
                });
                $("#selectNatureKPIDataEdit").val(EditModalManager.savedNature).trigger('change');
                EditModalManager.savedNature = "";
            }
        });
    },

    createSubNatureSelection: function (_nature) {
        $.ajax({
            url: EditModalManager.dictionaryUrl + "?type=subnature&parent=" + _nature,
            cache: false,
            dataType: "json",
            success: function (_data) {
                var subNatureArray = [];
                for (var i = 0; i < _data.content.length; i++) {
                    subNatureArray.push({
                        "key": _data.content[i].value,
                        "id": _data.content[i].id,
                        "value": _data.content[i].label
                    });
                }
                ViewManager.render({
                    "arrayToSelection": subNatureArray
                }, "#selectSubNatureKPIDataEdit", "templates/arrayToSelection.mst.html");
                
                if (EditModalManager.savedSubNature != ""){
                    $("#selectSubNatureKPIDataEdit").val(EditModalManager.savedSubNature);
                    EditModalManager.savedSubNature = "";
                }
            }
        });
    },

    createValueTypeSelection: function (_savedValueType, _savedValueUnit) {
        EditModalManager.savedValueType = _savedValueType;
        EditModalManager.savedValueUnit = _savedValueUnit;
        $.ajax({
            url: EditModalManager.dictionaryUrl + "?type=valuetype",
            cache: false,
            dataType: "json",
            success: function (_data) {
                var valueTypeArray = [];
                for (var i = 0; i < _data.content.length; i++) {
                    valueTypeArray.push({
                        "key": _data.content[i].value,
                        "id": _data.content[i].id,
                        "value": _data.content[i].label
                    });
                }
                ViewManager.render({
                    "arrayToSelection": valueTypeArray
                }, "#selectValueTypeKPIDataEdit", "templates/arrayToSelection.mst.html");
                $("#selectValueTypeKPIDataEdit").on('change', function () {
                    $("#selectValueUnitKPIDataEdit").html("");
                    EditModalManager.createValueUnitSelection($("#selectValueTypeKPIDataEdit").find(':selected').attr('data-id'));
                });
                $("#selectValueTypeKPIDataEdit").val(EditModalManager.savedValueType).trigger('change');
                EditModalManager.savedValueType = "";
            }
        });
    },

    createValueUnitSelection: function (_valuetype) {
        $.ajax({
            url: EditModalManager.dictionaryUrl + "?type=valueunit&parent=" + _valuetype,
            cache: false,
            dataType: "json",
            success: function (_data) {
                var valueUnitArray = [];
                for (var i = 0; i < _data.content.length; i++) {
                    valueUnitArray.push({
                        "key": _data.content[i].value,
                        "id": _data.content[i].id,
                        "value": _data.content[i].label
                    });
                }
                ViewManager.render({
                    "arrayToSelection": valueUnitArray
                }, "#selectValueUnitKPIDataEdit", "templates/arrayToSelection.mst.html");
                if (EditModalManager.savedValueUnit != ""){
                    $("#selectValueUnitKPIDataEdit").val(EditModalManager.savedValueUnit);
                    EditModalManager.savedValueUnit = "";
                }
            }
        });
    },
    
    createOrganizationListSelection: function (_organization) {
    	EditModalManager.savedOrganization = _organization;
        var organizationArray = [];
        for (var i = 0; i < EditModalManager.organizationList.length; i++) {
            organizationArray.push({
                "key": EditModalManager.organizationList[i],
                "id": EditModalManager.organizationList[i],
                "value": EditModalManager.organizationList[i]
            });
        }
        ViewManager.render({
            "arrayToSelection": organizationArray
        }, "#selectOrganizationKPIDataEdit", "templates/arrayToSelection.mst.html");
        if (EditModalManager.savedOrganization != ""){
            $("#selectOrganizationKPIDataEdit").val(EditModalManager.savedOrganization);
            EditModalManager.savedOrganization = "";
        }
    }

}