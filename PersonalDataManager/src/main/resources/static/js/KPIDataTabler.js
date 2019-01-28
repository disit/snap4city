var KPIDataTabler = {

    currentKPIDataPage: null,
    privacy: "",
    enableEdit: true,

    renderTable: function () {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDataTableQuery(KPIDataTabler.privacy, KPIDataPager.currentPage, KPIDataPager.currentSize, KPIDataSorter.currentSortDirection, KPIDataSorter.currentSortBy, KPIDataFilter.currentSearchKey, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, KPIDataTabler.successQuery, KPIDataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIDataTableQuery(KPIDataTabler.privacy, KPIDataPager.currentPage, KPIDataPager.currentSize, KPIDataSorter.currentSortDirection, KPIDataSorter.currentSortBy, KPIDataFilter.currentSearchKey, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, KPIDataTabler.successQuery, KPIDataTabler.errorQuery);
        });
    },

    successQuery: function (_response) {
        KPIDataTabler.currentKPIDataPage = _response;
        if ($("#kpidatatable").length == 0) {
            $("#indexPage").
            append("<div id=\"kpidatatable\" style=\"margin: 0px 20px\"></div>")
        }
        $("#loginForm").remove();
        $("#kpivaluetable").remove();
        $("#kpimetadatatable").remove();
        $("#kpidelegationtable").remove();
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

        _response.timestampToDate = MustacheFunctions.timestampToDate;

        for (var i = 0; i < _response.content.length; i++) {
            _response.content[i].isPublic = (_response.content[i].ownership == "public");
        }

        _response.enableEdit = KPIDataTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpidatatable", "templates/kpidata/kpidata.mst.html");

        $('table').DataTable({
            "searching": false,
            "paging": false,
            "ordering": false,
            "info": false,
            responsive: true
        });

        $('table').css("width", "");

        $("input[name=inlineRadioOptions][value='" + KPIDataTabler.privacy + "']").prop("checked", true);
        $('#inputFilterKPIData').val(KPIDataFilter.currentSearchKey);
        $('#selectSizeKPIData').val(KPIDataPager.currentSize);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null) {
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    },

    showKPIDataModal: function (_id) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDataByIdQuery(_id, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, KPIDataTabler.successShowKPIDataModal, KPIDataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIDataByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, KPIDataTabler.successShowKPIDataModal, KPIDataTabler.errorQuery);
        });
    },

    successShowKPIDataModal: function (_response) {
        console.log(_response);
        _response.lastDate = Utility.timestampToFormatDate(_response.lastDate);
        _response.lastCheck = Utility.timestampToFormatDate(_response.lastCheck);
        _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
        ViewManager.render({
            "kpidata": _response
        }, "#genericModal", "templates/kpidata/showkpidata.mst.html");
        $('#genericModal').modal('show');
    },

    editKPIDataModal: function (_id,_highLevelType) {
        KPIDataTabler.currentHighLevelType = _highLevelType;
        if (_id != null && _id != "") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createGetKPIDataByIdQuery(_id, KPIEditor.keycloak.token);
                APIClient.executeGetQuery(query, KPIDataTabler.successEditKPIDataModal, KPIDataTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createGetKPIDataByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
                APIClient.executeGetQuery(query, KPIDataTabler.successEditKPIDataModal, KPIDataTabler.errorQuery);
            });
        } else {
            KPIDataTabler.successEditKPIDataModal(null);
        }
    },

    successEditKPIDataModal: function (_response) {
        console.log(_response);
        if (_response != null && _response != "") {
            if (_response.dataType != null) {
                _response[_response.dataType + "Selected"] = true;
            }
            if (_response.ownership != null) {
                _response[_response.ownership + "Selected"] = true;
            }
            _response.lastDate = Utility.timestampToFormatDate(_response.lastDate);
            _response.lastCheck = Utility.timestampToFormatDate(_response.lastCheck);
        } else {
            _response = {};
            _response.highLevelType = KPIDataTabler.currentHighLevelType;
        }
        ViewManager.render({
            "kpidata": _response
        }, "#genericModal", "templates/kpidata/edit" +_response.highLevelType.toLowerCase() +".mst.html");
        $('#genericModal').modal('show');
        
        EditModalManager.createMap();
        EditModalManager.createNatureSelection();
        $("#selectNatureKPIDataEdit").val(_response.nature).trigger('change');
        $("#selectSubNatureKPIDataEdit").val(_response.subNature);
    },

    deleteKPIDataModal: function (_id, _highLevelType, _valueName) {
        ViewManager.render({
            "kpidata": {
                "id": _id,
                "highLevelType": _highLevelType,
                "valueName": _valueName
            }
        }, "#genericModal", "templates/kpidata/deletekpidata.mst.html");
        $('#genericModal').modal('show');
    },

    saveKPIData: function (kpiDataToSave) {
        if (kpiDataToSave == null) {
            kpiData = {
                "highLevelType": $("#inputHighLevelTypeKPIDataEdit").val(),
                "nature": $("#selectNatureKPIDataEdit").val(),
                "subNature": $("#selectSubNatureKPIDataEdit").val(),
                "valueName": $("#inputValueNameKPIDataEdit").val(),
                "valueType": $("#inputValueTypeKPIDataEdit").val(),
                "dataType": $("#selectDataTypeKPIDataEdit").val(),
                "lastValue": $("#inputLastValueKPIDataEdit").val(),
                "metric": $("#inputMetricKPIDataEdit").val(),
                "description": $("#inputDescriptionKPIDataEdit").val(),
                "info": $("#inputInfoKPIDataEdit").val(),
                "latitude": $("#inputLatitudeKPIDataEdit").val(),
                "longitude": $("#inputLongitudeKPIDataEdit").val(),
                "insertTime": Utility.timestampToFormatDate(new Date().getTime()) + ":00"
            }

            if ($("#inputIdKPIDataEdit").val() != "") {
                kpiData.id = $("#inputIdKPIDataEdit").val();
            }
            if ($("#inputInstanceUriKPIDataEdit").val() != "") {
                kpiData.instanceUri = $("#inputInstanceUriKPIDataEdit").val();
            }
            if ($("#inputGetInstancesKPIDataEdit").val() != "") {
                kpiData.getInstances = $("#inputGetInstancesKPIDataEdit").val();
            }
            if ($("#inputSavedDirectKPIDataEdit").val() != "") {
                kpiData.savedDirect = $("#inputSavedDirectKPIDataEdit").val();
            }
            if ($("#inputKBBasedKPIDataEdit").val() != "") {
                kpiData.kbBased = $("#inputKBBasedKPIDataEdit").val();
            }
            if ($("#inputSMBasedKPIDataEdit").val() != "") {
                kpiData.smBased = $("#inputSMBasedKPIDataEdit").val();
            }
            if ($("#inputAppIdKPIDataEdit").val() != "") {
                kpiData.appId = $("#inputAppIdKPIDataEdit").val();
            }
            if ($("#inputAppNameKPIDataEdit").val() != "") {
                kpiData.appName = $("#inputAppNameKPIDataEdit").val();
            }
            if ($("#inputWidgetsKPIDataEdit").val() != "") {
                kpiData.widgets = $("#inputWidgetsKPIDataEdit").val();
            }
            if ($("#inputParametersKPIDataEdit").val() != "") {
                kpiData.parameters = $("#inputParametersKPIDataEdit").val();
            }
            if ($("#inputMicroAppExtServIconKPIDataEdit").val() != "") {
                kpiData.microAppExtServIcon = $("#inputMicroAppExtServIconKPIDataEdit").val();
            }
            if ($("#inputDBValuesLinkKPIDataEdit").val() != "") {
                kpiData.dbValuesLink = $("#inputDBValuesLinkKPIDataEdit").val();
            }
            if ($("#inputDBValuesTypeKPIDataEdit").val() != "") {
                kpiData.dbValuesType = $("#inputDBValuesTypeKPIDataEdit").val();
            }
            if ($("#inputUsernameKPIDataEdit").val() != "") {
                kpiData.username = $("#inputUsernameKPIDataEdit").val();
            }
            if ($("#inputOwnershipKPIDataEdit").val() != "") {
                kpiData.ownership = $("#inputOwnershipKPIDataEdit").val();
            } else {
                kpiData.ownership = "private";
            }
            if ($("#inputHealthinessKPIDataEdit").val() != "") {
                kpiData.healthiness = $("#inputHealthinessKPIDataEdit").val();
            } else {
                kpiData.healthiness = false;
            }
            if ($("#inputLastDateKPIDataEdit").val() != "") {
                kpiData.lastDate = $("#inputLastDateKPIDataEdit").val() + ":00";
            }
            if ($("#inputLastCheckKPIDataEdit").val() != "") {
                kpiData.lastCheck = $("#inputLastCheckKPIDataEdit").val() + ":00";
            }
        } else {
            kpiData = kpiDataToSave;
        }

        console.log(kpiData);
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createSaveKPIDataQuery(KPIEditor.keycloak.token);
            APIClient.executePostQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createSaveKPIDataQuery(Authentication.refreshTokenGetAccessToken());
            APIClient.executePostQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
        });
    },

    deleteKPIData(_id) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDataByIdQuery(_id, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, function (_response) {
                _response.deleteTime = Utility.timestampToFormatDate(new Date().getTime()) + ":00";
                if (_response.lastDate != null) {
                    _response.lastDate = Utility.timestampToFormatDate(_response.lastDate) + ":00";
                }
                if (_response.lastCheck != null) {
                    _response.lastCheck = Utility.timestampToFormatDate(_response.lastCheck) + ":00";
                }
                if (_response.insertTime != null) {
                    _response.insertTime = Utility.timestampToFormatDate(_response.insertTime) + ":00";
                }
                var saveQuery = QueryManager.createSaveKPIDataQuery(KPIEditor.keycloak.token);
                APIClient.executePostQuery(saveQuery, _response, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);

            }, KPIDataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIDataByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, function (_response) {
                _response.deleteTime = Utility.timestampToFormatDate(new Date().getTime()) + ":00";
                if (_response.lastDate != null) {
                    _response.lastDate = Utility.timestampToFormatDate(_response.lastDate) + ":00";
                }
                if (_response.lastCheck != null) {
                    _response.lastCheck = Utility.timestampToFormatDate(_response.lastCheck) + ":00";
                }
                if (_response.insertTime != null) {
                    _response.insertTime = Utility.timestampToFormatDate(_response.insertTime) + ":00";
                }

                var saveQuery = QueryManager.createSaveKPIDataQuery(Authentication.refreshTokenGetAccessToken());
                APIClient.executePostQuery(saveQuery, _response, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
            }, KPIDataTabler.errorQuery);
        });
    },

    successSaveKPIData: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIDataTabler.renderTable();
    },

    getCurrentKPIData: function (_kpiId) {
        for (var i = 0; i < KPIDataTabler.currentKPIDataPage.content.length; i++) {
            if (KPIDataTabler.currentKPIDataPage.content[i].id == _kpiId) {
                return KPIDataTabler.currentKPIDataPage.content[i];
            }
        }
        return null;
    },

    setPrivacy: function () {
        KPIDataTabler.privacy = $("input[name=inlineRadioOptions]:checked").val();
        if (KPIDataTabler.privacy != "") {
            KPIDataTabler.enableEdit = false;
        } else {
            KPIDataTabler.enableEdit = true;
        }
        KPIDataPager.currentPage = 0;
        KPIDataTabler.renderTable();
    },

    makePublic: function (_kpiId) {
        KPIDataTabler.currentMakePublicKPIId = _kpiId;
        KPIDelegationTabler.createAnonymousDelegation(KPIDataTabler.currentMakePublicKPIId, KPIDataTabler.successCreatePublicDelegation, KPIDataTabler.errorQuery);
    },

    successCreatePublicDelegation: function () {
        var kpiData = KPIDataTabler.getCurrentKPIData(KPIDataTabler.currentMakePublicKPIId);
        kpiData.ownership = "public";
        if (kpiData.lastDate != null) {
            kpiData.lastDate = Utility.timestampToFormatDate(kpiData.lastDate) + ":00";
        }
        if (kpiData.lastCheck != null) {
            kpiData.lastCheck = Utility.timestampToFormatDate(kpiData.lastCheck) + ":00";
        }
        if (kpiData.insertTime != null) {
            kpiData.insertTime = Utility.timestampToFormatDate(kpiData.insertTime) + ":00";
        }
        KPIDataTabler.saveKPIData(kpiData);
    },

    makePrivate: function (_kpiId) {
        KPIDataTabler.currentMakePublicKPIId = _kpiId;
        KPIDelegationTabler.getDelegations(_kpiId, KPIDataTabler.successGetDelegationsForMakePrivate, KPIDataTabler.errorQuery);
    },

    successGetDelegationsForMakePrivate: function (_response) {
        var currentDelegation = null
        for (var i = 0; i < _response.length; i++) {
            if (_response[i].usernameDelegated == "ANONYMOUS") {
                currentDelegation = _response[i];
            }
        }
        if (currentDelegation != null) {
            KPIDelegationTabler.deleteKPIDelegation(currentDelegation.id, KPIDataTabler.successDeletePublicDelegation, KPIDataTabler.errorQuery);
        }
    },

    successDeletePublicDelegation: function () {
        var kpiData = KPIDataTabler.getCurrentKPIData(KPIDataTabler.currentMakePublicKPIId);
        kpiData.ownership = "private";
        if (kpiData.lastDate != null) {
            kpiData.lastDate = Utility.timestampToFormatDate(kpiData.lastDate) + ":00";
        }
        if (kpiData.lastCheck != null) {
            kpiData.lastCheck = Utility.timestampToFormatDate(kpiData.lastCheck) + ":00";
        }
        if (kpiData.insertTime != null) {
            kpiData.insertTime = Utility.timestampToFormatDate(kpiData.insertTime) + ":00";
        }
        KPIDataTabler.saveKPIData(kpiData);
    },


}