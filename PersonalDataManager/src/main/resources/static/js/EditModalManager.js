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
        if (_error != null) {
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

    createNatureSelection: function () {
        $.ajax({
            url: "json/categories.json",
            async: false,
            cache: false,
            dataType: "json",
            success: function (_data) {
                var natureArray = [];
                for (var i = 0; i < _data.length; i++) {
                    natureArray.push({
                        "key": _data[i].key,
                        "value": _data[i].title
                    });
                }
                ViewManager.render({
                    "arrayToSelection": natureArray
                }, "#selectNatureKPIDataEdit", "templates/arrayToSelection.mst.html");
                $("#selectNatureKPIDataEdit").on('change', function () {
                    EditModalManager.createSubNatureSelection($("#selectNatureKPIDataEdit").val());
                });
            }
        });
    },

    createSubNatureSelection: function (_nature) {
        $.ajax({
            url: "json/categories.json",
            async: false,
            cache: false,
            dataType: "json",
            success: function (_data) {
                var subNatureArray = [];
                for (var i = 0; i < _data.length; i++) {
                    if (_data[i].key == _nature) {
                        for (var j = 0; j < _data[i].children.length; j++) {
                            subNatureArray.push({
                                "key": _data[i].children[j].key,
                                "value": _data[i].children[j].title
                            });
                        }
                    }
                }
                ViewManager.render({
                    "arrayToSelection": subNatureArray
                }, "#selectSubNatureKPIDataEdit", "templates/arrayToSelection.mst.html");
            }
        });
    }

}