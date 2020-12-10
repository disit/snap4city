var KPIOrgDelegationTabler = {

    currentKpiId: null,
    currentHighLevelType: null,

    renderTable: function (kpiId, highLevelType) {
        if (KPIOrgDelegationTabler.currentKpiId != kpiId) {
            KPIOrgDelegationPager.currentPage = 0;
        }
        KPIOrgDelegationTabler.currentKpiId = kpiId;
        KPIOrgDelegationTabler.currentHighLevelType = highLevelType;
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIOrgDelegationTableQuery(KPIOrgDelegationTabler.currentKpiId, KPIOrgDelegationPager.currentPage, KPIOrgDelegationPager.currentSize, KPIOrgDelegationSorter.currentSortDirection, KPIOrgDelegationSorter.currentSortBy, KPIOrgDelegationFilter.currentSearchKey);
            APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIOrgDelegationTabler.successQuery, KPIOrgDelegationTabler.errorQuery);
        }).error(function (_error) {
            console.log("updateToken error: " + _error);
            /* var query = QueryManager.createGetKPIOrgDelegationTableQuery(KPIOrgDelegationTabler.currentKpiId, KPIOrgDelegationPager.currentPage, KPIOrgDelegationPager.currentSize, KPIOrgDelegationSorter.currentSortDirection, KPIOrgDelegationSorter.currentSortBy, KPIOrgDelegationFilter.currentSearchKey);
            APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIOrgDelegationTabler.successQuery, KPIOrgDelegationTabler.errorQuery);*/
        });
    },

    successQuery: function (_response) {
        if ($("#kpiorgdelegationtable").length == 0) {
            $("#indexPage").
            append("<div id=\"kpiorgdelegationtable\" style=\"margin: 0px 20px\"></div>")
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

        _response.kpiId = KPIOrgDelegationTabler.currentKpiId;
        _response.currentKPIData = KPIDataTabler.getCurrentKPIData(KPIOrgDelegationTabler.currentKpiId);

        _response.timestampToDate = MustacheFunctions.timestampToDate;

        for (var i = 0; i < _response.content.length; i++) {
            _response.content[i].groupnameDelegated = _response.content[i].groupnameDelegated.substring(_response.content[i].groupnameDelegated.indexOf("=") + 1, _response.content[i].groupnameDelegated.indexOf(","))
        }

        _response.enableEdit = KPIDataTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpiorgdelegationtable", "templates/kpiorgdelegation/kpiorgdelegation.mst.html");

        $('table').DataTable({
            "searching": false,
            "paging": false,
            "ordering": false,
            "info": false,
            responsive: true
        });

        $('table').css("width", "");

        $('#inputFilterKPIOrgDelegation').val(KPIOrgDelegationFilter.currentSearchKey);
        $('#selectSizeKPIOrgDelegation').val(KPIOrgDelegationPager.currentSize);

        if (KPIEditor.withParameters) {
            $("#backButtonToMyKPIDataList").hide();
        }
    },

    editKPIOrgDelegationModal: function (_kpiId, _id) {
        if (_id != null && _id != "") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createGetKPIDelegationByIdQuery(_kpiId, _id);
                APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIOrgDelegationTabler.successEditKPIOrgDelegationModal, KPIOrgDelegationTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var query = QueryManager.createGetKPIDelegationByIdQuery(_kpiId, _id);
                    APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIOrgDelegationTabler.successEditKPIOrgDelegationModal, KPIOrgDelegationTabler.errorQuery);*/
            });
        } else {
            KPIOrgDelegationTabler.successEditKPIOrgDelegationModal(null);
        }
    },

    successEditKPIOrgDelegationModal: function (_response) {
        console.log(_response);
        if (_response != null && _response != "") {
            _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
            _response.deleteTime = Utility.timestampToFormatDate(_response.deleteTime);
        } else {
            _response = true;
        }
        console.log(_response);

        if (_response.groupnameDelegated != null) {
            _response[_response.groupnameDelegated.substring(_response.groupnameDelegated.indexOf("=") + 1, _response.groupnameDelegated.indexOf(",")) + "Selected"] = true;
        }

        ViewManager.render({
            "kpidelegation": _response
        }, "#genericModal", "templates/kpiorgdelegation/editkpiorgdelegation.mst.html");
        $('#genericModal').modal('show');
    },

    deleteKPIOrgDelegationModal: function (_id, _kpiId, _groupnameDelegated, _insertTime) {
        ViewManager.render({
            "kpidelegation": {
                "id": _id,
                "kpiId": _kpiId,
                "groupnameDelegated": _groupnameDelegated,
                "insertTime": _insertTime
            }
        }, "#genericModal", "templates/kpiorgdelegation/deletekpiorgdelegation.mst.html");
        $('#genericModal').modal('show');
    },

    saveKPIOrgDelegation: function () {
        let kpiDelegation = {}

        if ($("#selectGroupnameDelegatedKPIOrgDelegationEdit").val() != "") {
            kpiDelegation.groupnameDelegated = "ou=" + $("#selectGroupnameDelegatedKPIOrgDelegationEdit").val() + "," + Utility.ldapBasicDn;
        }

        if ($("#inputElementIdKPIOrgDelegationEdit").val() != "") {
            kpiDelegation.elementId = $("#inputElementIdKPIOrgDelegationEdit").val();
        } else {
            kpiDelegation.elementId = KPIOrgDelegationTabler.currentKpiId;
        }
        if ($("#inputIdKPIOrgDelegationEdit").val() != "") {
            kpiDelegation.id = $("#inputIdKPIOrgDelegationEdit").val();
        }
        if (typeof $("#inputElementTypeKPIOrgDelegationEdit").val() != "undefined" && $("#inputElementTypeKPIOrgDelegationEdit").val() != "") {
            kpiDelegation.elementType = $("#inputElementTypeKPIOrgDelegationEdit").val();
        } else {
            kpiDelegation.elementType = KPIOrgDelegationTabler.currentHighLevelType;
        }

        console.log(kpiDelegation);
        if (typeof kpiDelegation.id != "undefined") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createPatchKPIDelegationQuery(kpiDelegation.elementId, kpiDelegation.id);
                APIClient.executePatchQuery(query, kpiDelegation, KPIEditor.keycloak.token, KPIOrgDelegationTabler.successSaveKPIOrgDelegation, KPIOrgDelegationTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var query = QueryManager.createPatchKPIDelegationQuery(kpiDelegation.elementId, kpiDelegation.id);
                   APIClient.executePatchQuery(query, kpiDelegation, Authentication.refreshTokenGetAccessToken(),  KPIOrgDelegationTabler.successSaveKPIOrgDelegation, KPIOrgDelegationTabler.errorQuery);*/
            });
        } else {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createPostKPIDelegationQuery(kpiDelegation.elementId);
                APIClient.executePostQuery(query, kpiDelegation, KPIEditor.keycloak.token, KPIOrgDelegationTabler.successSaveKPIOrgDelegation, KPIOrgDelegationTabler.errorQuery);
            }).error(function (_error) {
                console.log("updateToken error: " + _error);
                /* var  query = QueryManager.createPostKPIDelegationQuery(kpiDelegation.elementId);
                   APIClient.executePostQuery(query, kpiDelegation, Authentication.refreshTokenGetAccessToken(),  KPIOrgDelegationTabler.successSaveKPIOrgDelegation, KPIOrgDelegationTabler.errorQuery);*/
            });
        }
    },

    getDelegations: function (_kpiId, successCallBack, errorCallBack) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIOrgDelegationTableQuery(_kpiId, null, null, null, null, null);
            APIClient.executeGetQuery(query, KPIEditor.keycloak.token, successCallBack, errorCallBack);
        }).error(function (_error) {
            console.log("updateToken error: " + _error);
            /* var query = QueryManager.createGetKPIOrgDelegationTableQuery(_kpiId, null, null, null, null, null);
            APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), successCallBack, errorCallBack);*/
        });
    },

    deleteKPIOrgDelegation(_kpiId, _id, successCallBack, errorCallBack) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createDeleteKPIDelegationQuery(_kpiId, _id);
            if (successCallBack != null && errorCallBack != null) {
                APIClient.executeDeleteQuery(query, KPIEditor.keycloak.token, successCallBack, errorCallBack);
            } else {
                APIClient.executeDeleteQuery(query, KPIEditor.keycloak.token, KPIOrgDelegationTabler.successSaveKPIOrgDelegation, KPIOrgDelegationTabler.errorQuery);
            }
        }).error(function (_error) {
            console.log("updateToken error: " + _error);
            /* var query = QueryManager.createDeleteKPIDelegationQuery(_kpiId, _id);
            if (successCallBack != null && errorCallBack != null) {
                APIClient.executeDeleteQuery(query, Authentication.refreshTokenGetAccessToken(), successCallBack, errorCallBack);
            } else {
                APIClient.executeDeleteQuery(query, Authentication.refreshTokenGetAccessToken(), KPIOrgDelegationTabler.successSaveKPIOrgDelegation, KPIOrgDelegationTabler.errorQuery);
            }*/
        });
    },

    successSaveKPIOrgDelegation: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIOrgDelegationTabler.renderTable(_response.elementId, _response.elementType);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null) {
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    }

}