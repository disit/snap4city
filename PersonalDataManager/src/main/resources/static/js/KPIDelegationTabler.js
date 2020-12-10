var KPIDelegationTabler = {

    currentKpiId: null,
    currentHighLevelType: null,

    renderTable: function (kpiId, highLevelType) {
        if (KPIDelegationTabler.currentKpiId != kpiId) {
            KPIDelegationPager.currentPage = 0;
        }
        KPIDelegationTabler.currentKpiId = kpiId;
        KPIDelegationTabler.currentHighLevelType = highLevelType;
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDelegationTableQuery(KPIDelegationTabler.currentKpiId, KPIDelegationPager.currentPage, KPIDelegationPager.currentSize, KPIDelegationSorter.currentSortDirection, KPIDelegationSorter.currentSortBy, KPIDelegationFilter.currentSearchKey);
            APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIDelegationTabler.successQuery, KPIDelegationTabler.errorQuery);
        }).error(function (_error) {
            console.log("updateToken error: " + _error);
            /* var query = QueryManager.createGetKPIDelegationTableQuery(KPIDelegationTabler.currentKpiId, KPIDelegationPager.currentPage, KPIDelegationPager.currentSize, KPIDelegationSorter.currentSortDirection, KPIDelegationSorter.currentSortBy, KPIDelegationFilter.currentSearchKey);
            APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIDelegationTabler.successQuery, KPIDelegationTabler.errorQuery);*/
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

        _response.kpiId = KPIDelegationTabler.currentKpiId;
        _response.currentKPIData = KPIDataTabler.getCurrentKPIData(KPIDelegationTabler.currentKpiId);

        _response.timestampToDate = MustacheFunctions.timestampToDate;

        _response.enableEdit = KPIDataTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpidelegationtable", "templates/kpidelegation/kpidelegation.mst.html");

        $('table').DataTable({
            "searching": false,
            "paging": false,
            "ordering": false,
            "info": false,
            responsive: true
        });

        $('table').css("width", "");

        $('#inputFilterKPIDelegation').val(KPIDelegationFilter.currentSearchKey);
        $('#selectSizeKPIDelegation').val(KPIDelegationPager.currentSize);

        if (KPIEditor.withParameters) {
            $("#backButtonToMyKPIDataList").hide();
        }
    },

    editKPIDelegationModal: function (_kpiId, _id) {
        if (_id != null && _id != "") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createGetKPIDelegationByIdQuery(_kpiId, _id);
                APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIDelegationTabler.successEditKPIDelegationModal, KPIDelegationTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var query = QueryManager.createGetKPIDelegationByIdQuery(_kpiId, _id);
                    APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIDelegationTabler.successEditKPIDelegationModal, KPIDelegationTabler.errorQuery);*/
            });
        } else {
            KPIDelegationTabler.successEditKPIDelegationModal(null);
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
        }, "#genericModal", "templates/kpidelegation/editkpidelegation.mst.html");
        $('#genericModal').modal('show');
    },

    deleteKPIDelegationModal: function (_id, _kpiId, _usernameDelegated, _insertTime) {
        ViewManager.render({
            "kpidelegation": {
                "id": _id,
                "kpiId": _kpiId,
                "usernameDelegated": _usernameDelegated,
                "insertTime": _insertTime
            }
        }, "#genericModal", "templates/kpidelegation/deletekpidelegation.mst.html");
        $('#genericModal').modal('show');
    },

    saveKPIDelegation: function () {
        kpiDelegation = {
            "usernameDelegated": $("#inputUsernameDelegatedKPIDelegationEdit").val()
        }

        if ($("#inputElementIdKPIDelegationEdit").val() != "") {
            kpiDelegation.elementId = $("#inputElementIdKPIDelegationEdit").val();
        } else {
            kpiDelegation.elementId = KPIDelegationTabler.currentKpiId;
        }
        if ($("#inputIdKPIDelegationEdit").val() != "") {
            kpiDelegation.id = $("#inputIdKPIDelegationEdit").val();
        }
        if (typeof $("#inputElementTypeKPIDelegationEdit").val() != "undefined" && $("#inputElementTypeKPIDelegationEdit").val() != "") {
            kpiDelegation.elementType = $("#inputElementTypeKPIDelegationEdit").val();
        } else {
            kpiDelegation.elementType = KPIDelegationTabler.currentHighLevelType;
        }

        console.log(kpiDelegation);
        if (typeof kpiDelegation.id != "undefined") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createPatchKPIDelegationQuery(kpiDelegation.elementId, kpiDelegation.id);
                APIClient.executePatchQuery(query, kpiDelegation, KPIEditor.keycloak.token, KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var query = QueryManager.createPatchKPIDelegationQuery(kpiDelegation.elementId, kpiDelegation.id);
                    APIClient.executePatchQuery(query, kpiDelegation, Authentication.refreshTokenGetAccessToken(),  KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);*/
            });
        } else {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createPostKPIDelegationQuery(kpiDelegation.elementId);
                APIClient.executePostQuery(query, kpiDelegation, KPIEditor.keycloak.token, KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var query = QueryManager.createPostKPIDelegationQuery(kpiDelegation.elementId);
                    APIClient.executePostQuery(query, kpiDelegation, Authentication.refreshTokenGetAccessToken(),  KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);*/
            });
        }
    },

    getDelegations: function (_kpiId, successCallBack, errorCallBack) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDelegationTableQuery(_kpiId, null, null, null, null, null);
            APIClient.executeGetQuery(query, KPIEditor.keycloak.token, successCallBack, errorCallBack);
        }).error(function (_error) {
            console.log("updateToken error: " + _error);
            /* var query = QueryManager.createGetKPIDelegationTableQuery(_kpiId, null, null, null, null, null);
            APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), successCallBack, errorCallBack);*/
        });
    },

    deleteKPIDelegation(_kpiId, _id, successCallBack, errorCallBack) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createDeleteKPIDelegationQuery(_kpiId, _id);
            if (successCallBack != null && errorCallBack != null) {
                APIClient.executeDeleteQuery(query, KPIEditor.keycloak.token, successCallBack, errorCallBack);
            } else {
                APIClient.executeDeleteQuery(query, KPIEditor.keycloak.token, KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);
            }
        }).error(function (_error) {
            console.log("updateToken error: " + _error);
            /* var query = QueryManager.createDeleteKPIDelegationQuery(_kpiId, _id);
            if (successCallBack != null && errorCallBack != null) {
                APIClient.executeDeleteQuery(query, Authentication.refreshTokenGetAccessToken(), successCallBack, errorCallBack);
            } else {
                APIClient.executeDeleteQuery(query, Authentication.refreshTokenGetAccessToken(), KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);
            }*/
        });
    },

    successSaveKPIDelegation: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIDelegationTabler.renderTable(_response.elementId, _response.elementType);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null) {
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    }

}