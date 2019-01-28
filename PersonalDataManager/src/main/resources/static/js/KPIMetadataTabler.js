var KPIMetadataTabler = {

    currentKpiId: null,

    renderTable: function (kpiId) {
        KPIMetadataTabler.currentKpiId = kpiId;
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIMetadataTableQuery(kpiId, KPIMetadataPager.currentPage, KPIMetadataPager.currentSize, KPIMetadataSorter.currentSortDirection, KPIMetadataSorter.currentSortBy, KPIMetadataFilter.currentSearchKey, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, KPIMetadataTabler.successQuery, KPIMetadataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIMetadataTableQuery(kpiId, KPIMetadataPager.currentPage, KPIMetadataPager.currentSize, KPIMetadataSorter.currentSortDirection, KPIMetadataSorter.currentSortBy, KPIMetadataFilter.currentSearchKey, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, KPIMetadataTabler.successQuery, KPIMetadataTabler.errorQuery);
        });
    },

    successQuery: function (_response) {
        if ($("#kpimetadatatable").length == 0) {
            $("#indexPage").
            append("<div id=\"kpimetadatatable\" style=\"margin: 0px 20px\"></div>")
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
        _response.kpiId = KPIMetadataTabler.currentKpiId;
        _response.currentKPIData = KPIDataTabler.getCurrentKPIData(KPIMetadataTabler.currentKpiId);

        _response.enableEdit = KPIDataTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpimetadatatable", "templates/kpimetadata/kpimetadata.mst.html");

        $('table').DataTable({"searching": false,"paging":   false,
        "ordering": false,
        "info":     false, responsive: true});

        $('table').css("width", "");
        
        $('#inputFilterKPIMetadata').val(KPIMetadataFilter.currentSearchKey);
        $('#selectSizeKPIMetadata').val(KPIMetadataPager.currentSize);
    },

    editKPIMetadataModal: function (_id) {
        if (_id != null && _id != "") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createGetKPIMetadataByIdQuery(_id, KPIEditor.keycloak.token);
                APIClient.executeGetQuery(query, KPIMetadataTabler.successEditKPIMetadataModal, KPIMetadataTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createGetKPIMetadataByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
                APIClient.executeGetQuery(query, KPIMetadataTabler.successEditKPIMetadataModal, KPIMetadataTabler.errorQuery);
            });
        } else {
            KPIMetadataTabler.successEditKPIMetadataModal(null);
        }
    },

    successEditKPIMetadataModal: function (_response) {
        console.log(_response);
        if (_response != null && _response != "") {
            _response.elapseTime = Utility.timestampToFormatDate(_response.elapseTime);
            _response.deleteTime = Utility.timestampToFormatDate(_response.deleteTime);
        } else {
            _response = true;
        }
        ViewManager.render({
            "kpimetadata": _response
        }, "#genericModal", "templates/kpimetadata/editkpimetadata.mst.html");
        $('#genericModal').modal('show');
    },

    deleteKPIMetadataModal: function (_id, _kpiId, _key, _value) {
        ViewManager.render({
            "kpimetadata": {
                "id": _id,
                "kpiId": _kpiId,
                "key": _key,
                "value": _value
            }
        }, "#genericModal", "templates/kpimetadata/deletekpimetadata.mst.html");
        $('#genericModal').modal('show');
    },

    saveKPIMetadata: function () {
        kpiMetadata = {
            "value": $("#inputValueKPIMetadataEdit").val(),
            "key": $("#inputKeyKPIMetadataEdit").val()

        }

        if ($("#inputKpiIdKPIMetadataEdit").val() != "") {
            kpiMetadata.kpiId = $("#inputKpiIdKPIMetadataEdit").val();
        } else {
            kpiMetadata.kpiId = KPIMetadataTabler.currentKpiId;
        }
        if ($("#inputIdKPIMetadataEdit").val() != "") {
            kpiMetadata.id = $("#inputIdKPIMetadataEdit").val();
        }
        if ($("#inputElapseTimeKPIMetadataEdit").val() != "") {
            kpiValue.elapseTime = $("#inputElapseTimeKPIMetadataEdit").val() + ":00";
        }

        console.log(kpiMetadata);
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createSaveKPIMetadataQuery(KPIEditor.keycloak.token);
            APIClient.executePostQuery(query, kpiMetadata, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createSaveKPIMetadataQuery(Authentication.refreshTokenGetAccessToken());
            APIClient.executePostQuery(query, kpiMetadata, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
        });
    },

    deleteKPIMetadata(_id) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIMetadataByIdQuery(_id, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, function (_response) {
                _response.deleteTime = Utility.timestampToFormatDate(new Date().getTime()) + ":00";
                if (_response.elapseTime != null) {
                    _response.elapseTime = Utility.timestampToFormatDate(_response.elapseTime) + ":00";
                }
                var saveQuery = QueryManager.createSaveKPIMetadataQuery(KPIEditor.keycloak.token);
                APIClient.executePostQuery(saveQuery, _response, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
            }, KPIMetadataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIMetadataByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, function (_response) {
                _response.deleteTime = Utility.timestampToFormatDate(new Date().getTime()) + ":00";
                if (_response.elapseTime != null) {
                    _response.elapseTime = Utility.timestampToFormatDate(_response.elapseTime) + ":00";
                }
                var saveQuery = QueryManager.createSaveKPIMetadataQuery(Authentication.refreshTokenGetAccessToken());
                APIClient.executePostQuery(saveQuery, _response, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
            }, KPIMetadataTabler.errorQuery);
        });
    },

    successSaveKPIMetadata: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIMetadataTabler.renderTable(_response.kpiId);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null){
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    },


}