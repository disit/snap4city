var GrpDelegationTabler = {

    currentKpiId: null,
    currentHighLevelType: null,

    renderTable: function (kpiId, highLevelType) {
        GrpDelegationTabler.currentKpiId = kpiId;
        GrpDelegationTabler.currentHighLevelType = highLevelType;
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createGetGrpDelegationTableQuery(GrpDelegationTabler.currentKpiId, GrpDelegationPager.currentPage, GrpDelegationPager.currentSize, GrpDelegationSorter.currentSortDirection, GrpDelegationSorter.currentSortBy, GrpDelegationFilter.currentSearchKey, GrpEditor.keycloak.token);
            APIClient.executeGetQuery(query, GrpDelegationTabler.successQuery, GrpDelegationTabler.errorQuery);
        }).error(function () {
            var query = GrpQueryManager.createGetGrpDelegationTableQuery(GrpDelegationTabler.currentKpiId, GrpDelegationPager.currentPage, GrpDelegationPager.currentSize, GrpDelegationSorter.currentSortDirection, GrpDelegationSorter.currentSortBy, GrpDelegationFilter.currentSearchKey, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, GrpDelegationTabler.successQuery, GrpDelegationTabler.errorQuery);
        });
    },

    successQuery: function (_response) {
        if ($("#kpidelegationtable").length == 0) {
            $("#indexPage").
            append("<div id=\"kpidelegationtable\" style=\"margin: 0px 20px\"></div>")
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
        _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);

        _response.kpiId = GrpDelegationTabler.currentKpiId;
        _response.currentKPIData = DeviceGrpTabler.getCurrentKPIData(GrpDelegationTabler.currentKpiId);

        _response.timestampToDate = MustacheFunctions.timestampToDate;

        _response.enableEdit = DeviceGrpTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpidelegationtable", "templates/grpdelegation/kpidelegation.mst.html");

        $('table').DataTable({
            "searching": false,
            "paging": false,
            "ordering": false,
            "info": false,
            responsive: true
        });

        $('table').css("width", "");

        $('#inputFilterKPIDelegation').val(GrpDelegationFilter.currentSearchKey);
        $('#selectSizeKPIDelegation').val(GrpDelegationPager.currentSize);

        if (GrpEditor.withParameters) {
            $("#backButtonToMyKPIDataList").hide();
        }
    },

    editKPIDelegationModal: function (_kpiId, _id) {
        if (_id != null && _id != "") {
            GrpEditor.keycloak.updateToken(30).success(function () {
                var query = GrpQueryManager.createGetGrpDelegationByIdQuery(GrpEditor.keycloak.token, _kpiId, _id);
                APIClient.executeGetQuery(query, GrpDelegationTabler.successEditKPIDelegationModal, GrpDelegationTabler.errorQuery);
            }).error(function () {
                var query = GrpQueryManager.createGetGrpDelegationByIdQuery(Authentication.refreshTokenGetAccessToken(), _kpiId, _id);
                APIClient.executeGetQuery(query, GrpDelegationTabler.successEditKPIDelegationModal, GrpDelegationTabler.errorQuery);
            });
        } else {
            GrpDelegationTabler.successEditKPIDelegationModal(null);
        }
    },

    successEditKPIDelegationModal: function (_response) {
        console.log(_response);
        if (_response != null && _response != "") {
            _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
            _response.deleteTime = Utility.timestampToFormatDate(_response.deleteTime);
        } else {
            _response = true;
        }
        console.log(_response);
        ViewManager.render({
            "kpidelegation": _response
        }, "#genericModal", "templates/grpdelegation/editkpidelegation.mst.html");
        $('#genericModal').modal('show');
    },

    deleteKPIDelegationModal: function (_id, _kpiId, _usernameDelegated, _insertTime) {
        ViewManager.render({
            "kpidelegation": {
                "id": _id,
                "kpiId": _kpiId,
                "usernameDelegated": _usernameDelegated,
                "insertTime": Utility.timestampToFormatDate(_insertTime)
            }
        }, "#genericModal", "templates/grpdelegation/deletekpidelegation.mst.html");
        $('#genericModal').modal('show');
    },

    saveKPIDelegation: function () {
        kpiDelegation = {
            "usernameDelegated": $("#inputUsernameDelegatedKPIDelegationEdit").val()
        }

        if ($("#inputElementIdKPIDelegationEdit").val() != "") {
            kpiDelegation.elementId = $("#inputElementIdKPIDelegationEdit").val();
        } else {
            kpiDelegation.elementId = GrpDelegationTabler.currentKpiId;
        }
        if ($("#inputIdKPIDelegationEdit").val() != "") {
            kpiDelegation.id = $("#inputIdKPIDelegationEdit").val();
        }
        if (typeof $("#inputElementTypeKPIDelegationEdit").val() != "undefined" && $("#inputElementTypeKPIDelegationEdit").val() != "") {
            kpiDelegation.elementType = $("#inputElementTypeKPIDelegationEdit").val();
        } else {
            kpiDelegation.elementType = GrpDelegationTabler.currentHighLevelType;
        }

        console.log(kpiDelegation);
        if (typeof kpiDelegation.id != "undefined") {
            GrpEditor.keycloak.updateToken(30).success(function () {
                var query = GrpQueryManager.createPatchGrpDelegationQuery(GrpEditor.keycloak.token, kpiDelegation.elementId, kpiDelegation.id);
                APIClient.executePatchQuery(query, kpiDelegation, GrpDelegationTabler.successSaveKPIDelegation, GrpDelegationTabler.errorQuery);
            }).error(function () {
                var query = GrpQueryManager.createPatchGrpDelegationQuery(Authentication.refreshTokenGetAccessToken(), kpiDelegation.elementId, kpiDelegation.id);
                APIClient.executePatchQuery(query, kpiDelegation, GrpDelegationTabler.successSaveKPIDelegation, GrpDelegationTabler.errorQuery);
            });
        } else {
            GrpEditor.keycloak.updateToken(30).success(function () {
                var query = GrpQueryManager.createPostGrpDelegationQuery(GrpEditor.keycloak.token, kpiDelegation.elementId);
                APIClient.executePostQuery(query, kpiDelegation, GrpDelegationTabler.successSaveKPIDelegation, GrpDelegationTabler.errorQuery);
            }).error(function () {
                var query = GrpQueryManager.createPostGrpDelegationQuery(Authentication.refreshTokenGetAccessToken(), kpiDelegation.elementId);
                APIClient.executePostQuery(query, kpiDelegation, GrpDelegationTabler.successSaveKPIDelegation, GrpDelegationTabler.errorQuery);
            });
        }
    },

    getDelegations: function (_kpiId, successCallBack, errorCallBack) {
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDelegationTableQuery(_kpiId, null, null, null, null, null, GrpEditor.keycloak.token);
            APIClient.executeGetQuery(query, successCallBack, errorCallBack);
        }).error(function () {
            var query = QueryManager.createGetKPIDelegationTableQuery(_kpiId, null, null, null, null, null, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, successCallBack, errorCallBack);
        });
    },

    deleteKPIDelegation(_kpiId, _id, successCallBack, errorCallBack) {
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createDeleteGrpDelegationQuery(GrpEditor.keycloak.token, _kpiId, _id);
            if (successCallBack != null && errorCallBack != null) {
                APIClient.executeDeleteQuery(query, successCallBack, errorCallBack);
            } else {
                APIClient.executeDeleteQuery(query, GrpDelegationTabler.successSaveKPIDelegation, GrpDelegationTabler.errorQuery);
            }
        }).error(function () {
            var query = GrpQueryManager.createDeleteGrpDelegationQuery(Authentication.refreshTokenGetAccessToken(), _kpiId, _id);
            if (successCallBack != null && errorCallBack != null) {
                APIClient.executeDeleteQuery(query, successCallBack, errorCallBack);
            } else {
                APIClient.executeDeleteQuery(query, GrpDelegationTabler.successSaveKPIDelegation, GrpDelegationTabler.errorQuery);
            }
        });
    },

    successSaveKPIDelegation: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        GrpDelegationTabler.renderTable(_response.elementId, _response.elementType);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null) {
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    }

}