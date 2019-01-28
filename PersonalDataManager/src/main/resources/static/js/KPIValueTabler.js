var KPIValueTabler = {

    currentKpiId: null,
    currentKpiDataType: null,

    renderTable: function (kpiId, dataType) {
        KPIValueTabler.currentKpiId = kpiId;
        KPIValueTabler.currentKpiDataType = dataType;
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIValueTableQuery(KPIValueTabler.currentKpiId, KPIValuePager.currentPage, KPIValuePager.currentSize, KPIValueSorter.currentSortDirection, KPIValueSorter.currentSortBy, KPIValueFilter.currentSearchKey, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, KPIValueTabler.successQuery, KPIValueTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIValueTableQuery(KPIValueTabler.currentKpiId, KPIValuePager.currentPage, KPIValuePager.currentSize, KPIValueSorter.currentSortDirection, KPIValueSorter.currentSortBy, KPIValueFilter.currentSearchKey, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, KPIValueTabler.successQuery, KPIValueTabler.errorQuery);
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

        if(_response.previousNumber == "-"){
            _response.disablePreviousNumber = true;
        }
        if(_response.twoPreviousNumber == "-"){
            _response.disableTwoPreviousNumber = true;
        }
        if(_response.nextNumber == "-"){
            _response.disableNextNumber = true;
        }
        if(_response.twoNextNumber == "-"){
            _response.disableTwoNextNumber = true;
        }

        _response["sort" + _response.sort[0].property + _response.sort[0].direction] = true;

        _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
        _response.dataTime = Utility.timestampToFormatDate(_response.dataTime);
        
        _response.kpiId = KPIValueTabler.currentKpiId;
        _response.kpiDataType = KPIValueTabler.currentKpiDataType;
        _response.currentKPIData = KPIDataTabler.getCurrentKPIData(KPIValueTabler.currentKpiId);

        _response.timestampToDate = MustacheFunctions.timestampToDate;

        _response.enableEdit = KPIDataTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpivaluetable", "templates/kpivalue/kpivalue.mst.html");

        $('table').DataTable({"searching": false,"paging":   false,
        "ordering": false,
        "info":     false, responsive: true});

        $('table').css("width", "");
        
        $('#inputFilterKPIValue').val(KPIValueFilter.currentSearchKey);
        $('#selectSizeKPIValue').val(KPIValuePager.currentSize);
    },

    editKPIValueModal: function (_id) {
        if (_id != null && _id != "") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createGetKPIValueByIdQuery(_id, KPIEditor.keycloak.token);
                APIClient.executeGetQuery(query, KPIValueTabler.successEditKPIValueModal, KPIValueTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createGetKPIValueByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
                APIClient.executeGetQuery(query, KPIValueTabler.successEditKPIValueModal, KPIValueTabler.errorQuery);
            });
        } else {
            KPIValueTabler.successEditKPIValueModal(null);
        }
    },

    successEditKPIValueModal: function (_response) {
        console.log(_response);
        if (_response != null && _response != "") {
            _response.dataTime = Utility.timestampToFormatDate(_response.dataTime);
            _response.elapseTime = Utility.timestampToFormatDate(_response.elapseTime);
            _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
            _response.deleteTime = Utility.timestampToFormatDate(_response.deleteTime);
        } else {
            _response = {};
        }

        if (KPIValueTabler.currentKpiDataType == "integer"){
            _response.validDataType = "number";
            _response.validStep = 1;
        } else if (KPIValueTabler.currentKpiDataType == "float"){
            _response.validDataType = "number";
            _response.validStep = 0.01;
        } else if (KPIValueTabler.currentKpiDataType == "status"){
            _response.validDataType = "text";
        }
        ViewManager.render({
            "kpivalue": _response
        }, "#genericModal", "templates/kpivalue/editkpivalue.mst.html");
        $('#genericModal').modal('show');
    },

    deleteKPIValueModal: function (_id, _kpiId, _value, _dataTime, _insertTime) {
        ViewManager.render({
            "kpivalue": {
                "id": _id,
                "kpiId": _kpiId,
                "dataTime": _dataTime,
                "insertTime": _insertTime,
                "value": _value
            }
        }, "#genericModal", "templates/kpivalue/deletekpivalue.mst.html");
        $('#genericModal').modal('show');
    },

    saveKPIValue: function () {
        kpiValue = {
            "value": $("#inputValueKPIValueEdit").val()
        }

        if ($("#inputKpiIdKPIValueEdit").val() != "") {
            kpiValue.kpiId = $("#inputKpiIdKPIValueEdit").val();
        } else {
            kpiValue.kpiId = KPIValueTabler.currentKpiId;
        }
        if ($("#inputIdKPIValueEdit").val() != "") {
            kpiValue.id = $("#inputIdKPIValueEdit").val();
        }
        if ($("#inputDataTimeKPIValueEdit").val() != "") {
            kpiValue.dataTime = $("#inputDataTimeKPIValueEdit").val() + ":00";
        }
        if ($("#inputElapseTimeKPIValueEdit").val() != "") {
            kpiValue.elapseTime = $("#inputElapseTimeKPIValueEdit").val() + ":00";
        }
        if ($("#inputInsertTimeKPIValueEdit").val() != "") {
            kpiValue.insertTime = $("#inputInsertTimeKPIValueEdit").val() + ":00";
        }

        console.log(kpiValue);
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createSaveKPIValueQuery(KPIEditor.keycloak.token);
            APIClient.executePostQuery(query, kpiValue, KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createSaveKPIValueQuery(Authentication.refreshTokenGetAccessToken());
            APIClient.executePostQuery(query, kpiValue, KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);
        });
    },

    deleteKPIValue(_id) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIValueByIdQuery(_id, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, function (_response) {
                _response.deleteTime = Utility.timestampToFormatDate(new Date().getTime()) + ":00";
                if (_response.dataTime != null) {
                    _response.dataTime = Utility.timestampToFormatDate(_response.dataTime) + ":00";
                }
                if (_response.elapseTime != null) {
                    _response.elapseTime = Utility.timestampToFormatDate(_response.elapseTime) + ":00";
                }
                if (_response.insertTime != null) {
                    _response.insertTime = Utility.timestampToFormatDate(_response.insertTime) + ":00";
                }
                var saveQuery = QueryManager.createSaveKPIValueQuery(KPIEditor.keycloak.token);
                APIClient.executePostQuery(saveQuery, _response, KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);
            }, KPIValueTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIValueByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, function (_response) {
                _response.deleteTime = Utility.timestampToFormatDate(new Date().getTime()) + ":00";
                if (_response.dataTime != null) {
                    _response.dataTime = Utility.timestampToFormatDate(_response.dataTime) + ":00";
                }
                if (_response.elapseTime != null) {
                    _response.elapseTime = Utility.timestampToFormatDate(_response.elapseTime) + ":00";
                }
                if (_response.insertTime != null) {
                    _response.insertTime = Utility.timestampToFormatDate(_response.insertTime) + ":00";
                }
                var saveQuery = QueryManager.createSaveKPIValueQuery(Authentication.refreshTokenGetAccessToken());
                APIClient.executePostQuery(saveQuery, _response, KPIValueTabler.successSaveKPIValue, KPIValueTabler.errorQuery);
            }, KPIValueTabler.errorQuery);
        });
    },

    successSaveKPIValue: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIValueTabler.renderTable(_response.kpiId);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null){
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    },


}