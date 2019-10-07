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

        _response["sort" + _response.sort[0].property + _response.sort[0].direction] = true;
        _response.kpiId = KPIMetadataTabler.currentKpiId;
        _response.currentKPIData = KPIDataTabler.getCurrentKPIData(KPIMetadataTabler.currentKpiId);

        _response.enableEdit = KPIDataTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpimetadatatable", "templates/kpimetadata/kpimetadata.mst.html");

        $('table').DataTable({
            "searching": false,
            "paging": false,
            "ordering": false,
            "info": false,
            responsive: true
        });

        $('table').css("width", "");

        $('#inputFilterKPIMetadata').val(KPIMetadataFilter.currentSearchKey);
        $('#selectSizeKPIMetadata').val(KPIMetadataPager.currentSize);

        if (KPIEditor.withParameters) {
            $("#backButtonToMyKPIDataList").hide();
        }
    },

    editKPIMetadataModal: function (_kpiId, _id) {
        if (_id != null && _id != "") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createGetKPIMetadataByIdQuery(KPIEditor.keycloak.token, _kpiId, _id);
                APIClient.executeGetQuery(query, KPIMetadataTabler.successEditKPIMetadataModal, KPIMetadataTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createGetKPIMetadataByIdQuery(Authentication.refreshTokenGetAccessToken(), _kpiId, _id);
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

        console.log(kpiMetadata);
        if (typeof kpiMetadata.id != "undefined") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createPatchKPIMetadataQuery(KPIEditor.keycloak.token, kpiMetadata.kpiId, kpiMetadata.id);
                APIClient.executePatchQuery(query, kpiMetadata, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createPatchKPIMetadataQuery(Authentication.refreshTokenGetAccessToken(), kpiMetadata.kpiId, kpiMetadata.id);
                APIClient.executePatchQuery(query, kpiMetadata, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
            });
        } else {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createPostKPIMetadataQuery(KPIEditor.keycloak.token, kpiMetadata.kpiId);
                APIClient.executePostQuery(query, kpiMetadata, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createPostKPIMetadataQuery(Authentication.refreshTokenGetAccessToken(), kpiMetadata.kpiId);
                APIClient.executePostQuery(query, kpiMetadata, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
            });
        }
    },

    deleteKPIMetadata(_kpiId, _id) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createDeleteKPIMetadataQuery(KPIEditor.keycloak.token, _kpiId, _id);
            APIClient.executeDeleteQuery(query, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createDeleteKPIMetadataQuery(Authentication.refreshTokenGetAccessToken(), _kpiId, _id);
            APIClient.executeDeleteQuery(query, KPIMetadataTabler.successSaveKPIMetadata, KPIMetadataTabler.errorQuery);
        });
    },

    successSaveKPIMetadata: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIMetadataTabler.renderTable(_response.kpiId);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null) {
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    },


}