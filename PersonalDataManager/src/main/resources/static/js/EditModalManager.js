var EditModalManager = {

    createMap: function () {
        map = L.map('mapContainer').setView([43.78, 11.23], 9);
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

            $("#inputLatitudeKPIDataEdit").val(marker.geometry.coordinates[1]);
            $("#inputLongitudeKPIDataEdit").val(marker.geometry.coordinates[0]);
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

            $("#inputLatitudeKPIDataEdit").val(marker.geometry.coordinates[1]);
            $("#inputLongitudeKPIDataEdit").val(marker.geometry.coordinates[0]);
        });

        map.on('draw:deleted', function (e) {
            drawControl.addTo(map);
            $("#inputLatitudeKPIDataEdit").val(0);
            $("#inputLongitudeKPIDataEdit").val(0);
        });

        map.invalidateSize(true);
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