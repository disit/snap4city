var KPIValueTabler = {

    currentKpiId: null,
    currentKpiDataType: null,




    renderTable: function (kpiId, dataType) {
        if (KPIValueTabler.currentKpiId != kpiId) {
            KPIValuePager.currentPage = 0;
        }
        KPIValueTabler.currentKpiId = kpiId;
        KPIValueTabler.currentKpiDataType = dataType;
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIValueTableQuery(KPIValueTabler.currentKpiId, KPIValuePager.currentPage, KPIValuePager.currentSize, KPIValueSorter.currentSortDirection, KPIValueSorter.currentSortBy, KPIValueFilter.currentSearchKey);
            APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIValueTabler.successQuery, KPIValueTabler.errorQuery);
        }).error(function (_error) {
            console.log("updateToken error: " + _error);
            /* var query = QueryManager.createGetKPIValueTableQuery(KPIValueTabler.currentKpiId, KPIValuePager.currentPage, KPIValuePager.currentSize, KPIValueSorter.currentSortDirection, KPIValueSorter.currentSortBy, KPIValueFilter.currentSearchKey);
            APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIValueTabler.successQuery, KPIValueTabler.errorQuery);*/
        });
    },

    successQuery: function (_response) {
        if ($("#kpivaluetable").length == 0) {
            $("#indexPage").
            append("<div id=\"kpivaluetable\" style=\"margin: 0px 20px\"></div>")
        }
        $("#kpidatatable").remove();
        _response.showingFrom = _response.size * _response.number + 1;
        _response.showingTo = (_response.size * _response.number + _response.size > _response.totalElements ? _response.totalElements : _response.size * _response.number + _response.size);
        _response.labelNumber = _response.number + 1;
        _response.previousNumber = "-";
        _response.twoPreviousNumber = "-";
        _response.nextNumber = "-";
        _response.twoNextNumber = "-";
        if (_response.labelNumber > 1) {
            _response.previousNumber = _response.labelNumber - 1;
            if (_response.labelNumber > 2) {
                _response.twoPreviousNumber = _response.labelNumber - 2;
            }
        }
        if (_response.labelNumber < _response.totalPages) {
            _response.nextNumber = _response.labelNumber + 1;
            if (_response.labelNumber < (_response.totalPages - 1)) {
                _response.twoNextNumber = _response.labelNumber + 2;
            }
        }

        if (_response.previousNumber == "-") {
            _response.disablePreviousNumber = true;
        }
        if (_response.twoPreviousNumber == "-") {
            _response.disableTwoPreviousNumber = true;
        }
        if (_response.nextNumber == "-") {
            _response.disableNextNumber = true;
        }
        if (_response.twoNextNumber == "-") {
            _response.disableTwoNextNumber = true;
        }

        _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
        _response.dataTime = Utility.timestampToFormatDate(_response.dataTime);

        _response["sort" + _response.sort[0].property + _response.sort[0].direction] = true;

        _response.kpiId = KPIValueTabler.currentKpiId;
        _response.kpiDataType = KPIValueTabler.currentKpiDataType;
        _response.currentKPIData = KPIDataTabler.getCurrentKPIData(KPIValueTabler.currentKpiId);

        _response.timestampToDate = MustacheFunctions.timestampToDate;

        _response.enableEdit = KPIDataTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpivaluetable", "templates/kpivalue/kpivalue.mst.html");

        $('table').DataTable({
            "searching": false,
            "paging": false,
            "ordering": false,
            "info": false,
            responsive: true
        });

        $('table').css("width", "");

        $('#inputFilterKPIValue').val(KPIValueFilter.currentSearchKey);
        $('#selectSizeKPIValue').val(KPIValuePager.currentSize);

        if (KPIEditor.withParameters) {
            $("#backButtonToMyKPIDataList").hide();
        }
    },

    editKPIValueModal: function (_kpiId, _id) {
        if (_id != null && _id != "") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createGetKPIValueByIdQuery(_kpiId, _id);
                APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIValueTabler.successEditKPIValueModal, KPIValueTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var query = QueryManager.createGetKPIValueByIdQuery(_kpiId, _id);
                    APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIValueTabler.successEditKPIValueModal, KPIValueTabler.errorQuery);*/
            });
        } else {
            KPIValueTabler.successEditKPIValueModal(null);
        }
    },

    successEditKPIValueModal: function (_response) {
        console.log(_response);
        var isEdit = false;
        if (_response != null && _response != "") {
            _response.dataTime = Utility.timestampToFormatDate(_response.dataTime);
            _response.elapseTime = Utility.timestampToFormatDate(_response.elapseTime);
            _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
            _response.deleteTime = Utility.timestampToFormatDate(_response.deleteTime);
            isEdit = true;
        } else {
            _response = {};
        }

        if (KPIValueTabler.currentKpiDataType == "integer") {
            _response.validDataType = "number";
            _response.validStep = 1;
        } else if (KPIValueTabler.currentKpiDataType == "float") {
            _response.validDataType = "number";
            _response.validStep = 0.01;
        } else if (KPIValueTabler.currentKpiDataType == "status") {
            _response.validDataType = "text";
        }
        ViewManager.render({
            "kpivalue": _response,
            "isEdit": isEdit
        }, "#genericModal", "templates/kpivalue/editkpivalue.mst.html");
        $('#genericModal').modal('show');
        EditModalManager.currentLatitude = _response.latitude;
        EditModalManager.currentLongitude = _response.longitude;
        EditModalManager.checkOrganizationAndCreateMap("KPIValueEdit");
        //CHECK FIREFOX FOR DATETIME-LOCAL
        $("#timezonedesignator").hide();
        if (typeof InstallTrigger !== 'undefined') {
            $("#timezonedesignator").show();
        }
    },

    deleteKPIValueModal: function (_id, _kpiId, _value, _dataTime, _insertTime, _latitude, _longitude) {
        ViewManager.render({
            "kpivalue": {
                "id": _id,
                "kpiId": _kpiId,
                "dataTime": _dataTime,
                "value": _value,
                "latitude": _latitude,
                "longitude": _longitude
            }
        }, "#genericModal", "templates/kpivalue/deletekpivalue.mst.html");
        $('#genericModal').modal('show');
    },

    clearAllValuesModal: function () {
        ViewManager.render({
            "kpiId": KPIValueTabler.currentKpiId
        }, "#genericModal", "templates/kpivalue/clearallvalues.mst.html");
        $('#genericModal').modal('show');
    },

    clearAllValues: function (_kpiId) {
        if (_kpiId != null && _kpiId != "") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createClearAllKPIValueQuery(_kpiId);
                APIClient.executeDeleteQuery(query, KPIEditor.keycloak.token, KPIValueTabler.successClearAllValues, KPIValueTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var query = QueryManager.createClearAllKPIValueQuery(_kpiId);
                    APIClient.executeDeleteQuery(query, Authentication.refreshTokenGetAccessToken(), KPIValueTabler.successClearAllValues, KPIValueTabler.errorQuery);*/
            });
        } else {
            KPIValueTabler.successEditKPIValueModal(null);
        }
    },

    saveKPIValue: function () {

        let kpiValue = {
            "value": $("#inputValueKPIValueEdit").val(),
            "latitude": $("#inputLatitudeKPIValueEdit").val(),
            "longitude": $("#inputLongitudeKPIValueEdit").val(),
            "dataTime": new Date($("#inputDataTimeKPIValueEdit").val()).getTime()
        }

        if ($("#inputIdKPIValueEdit").val() != "") {
            kpiValue.id = $("#inputIdKPIValueEdit").val();
        }

        if ($("#inputKpiIdKPIValueEdit").val() != "") {
            kpiValue.kpiId = $("#inputKpiIdKPIValueEdit").val();
        } else {
            kpiValue.kpiId = KPIValueTabler.currentKpiId;
        }

        console.log(kpiValue);
        if (typeof kpiValue.id != "undefined") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                delete kpiValue.dataTime;
                var query = QueryManager.createPatchKPIValueQuery(kpiValue.kpiId, kpiValue.id);
                APIClient.executePatchQuery(query, kpiValue, KPIEditor.keycloak.token, KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var query = QueryManager.createPatchKPIValueQuery(kpiValue.kpiId, kpiValue.id);
                    APIClient.executePatchQuery(query, kpiValue, Authentication.refreshTokenGetAccessToken(), KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);*/
            });
        } else {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createPostKPIValueQuery(kpiValue.kpiId);
                APIClient.executePostQuery(query, kpiValue, KPIEditor.keycloak.token, KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var query = QueryManager.createPostKPIValueQuery(kpiValue.kpiId);
                    APIClient.executePostQuery(query, kpiValue, Authentication.refreshTokenGetAccessToken(), KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);*/
            });
        }
    },

    successClearAllValues: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIValueTabler.renderTable(KPIValueTabler.currentKpiId);
    },

    deleteKPIValue(_kpiId, _id) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createDeleteKPIValueQuery(_kpiId, _id);
            APIClient.executeDeleteQuery(query, KPIEditor.keycloak.token, KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);
        }).error(function (_error) {
            console.log("updateToken error: " + _error);
            /* var query = QueryManager.createDeleteKPIValueQuery(_kpiId, _id);
            APIClient.executeDeleteQuery(query, Authentication.refreshTokenGetAccessToken(), KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);*/
        });
    },

    successSaveKPIValue: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIValueTabler.renderTable(_response.kpiId);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null) {
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    },


}