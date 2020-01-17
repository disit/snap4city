var KPIDataTabler = {

    currentKPIDataPage: null,
    privacy: "",
    enableEdit: true,

    renderTable: function () {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDataTableQuery(KPIDataTabler.privacy, KPIDataPager.currentPage, KPIDataPager.currentSize, KPIDataSorter.currentSortDirection, KPIDataSorter.currentSortBy, KPIDataFilter.currentSearchKey);
            APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIDataTabler.successQuery, KPIDataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIDataTableQuery(KPIDataTabler.privacy, KPIDataPager.currentPage, KPIDataPager.currentSize, KPIDataSorter.currentSortDirection, KPIDataSorter.currentSortBy, KPIDataFilter.currentSearchKey);
            APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIDataTabler.successQuery, KPIDataTabler.errorQuery);
        });
    },

    successQuery: function (_response) {
        KPIDataTabler.currentKPIDataPage = _response;
        KPIEditor.checkParameters();
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
        if (_error.responseText != null && _error.responseText != "") {
            alert(_error.responseText);
        } else {
            alert("There was a problem to retrieve the data. Assure you have the right to view this data. The table of your kpi will be shown.")
        }
        $('#genericModal').modal('hide');
    },

    showKPIDataModal: function (_id) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDataByIdQuery(_id);
            APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIDataTabler.successShowKPIDataModal, KPIDataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIDataByIdQuery(_id);
            APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIDataTabler.successShowKPIDataModal, KPIDataTabler.errorQuery);
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

    editKPIDataModal: function (_id, _highLevelType) {
        KPIDataTabler.currentHighLevelType = _highLevelType;
        if (_id != null && _id != "") {
            if (_highLevelType != "MyPOI") {
                KPIEditor.keycloak.updateToken(30).success(function () {
                    var query = QueryManager.createGetKPIDataByIdQuery(_id);
                    APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIDataTabler.successEditKPIDataModal, KPIDataTabler.errorQuery);
                }).error(function () {
                    var query = QueryManager.createGetKPIDataByIdQuery(_id);
                    APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIDataTabler.successEditKPIDataModal, KPIDataTabler.errorQuery);
                });
            } else {
                KPIEditor.keycloak.updateToken(30).success(function () {
                    var query = QueryManager.createGetMyPOIQuery(_id);
                    APIClient.executeGetQuery(query, KPIEditor.keycloak.token, KPIDataTabler.successEditKPIDataModal, KPIDataTabler.errorQuery);
                }).error(function () {
                    var query = QueryManager.createGetMyPOIQuery(_id);
                    APIClient.executeGetQuery(query, Authentication.refreshTokenGetAccessToken(), KPIDataTabler.successEditKPIDataModal, KPIDataTabler.errorQuery);
                });
            }
        } else if (_highLevelType != "changeownership") {
            KPIDataTabler.successEditKPIDataModal(null);
        }
    },

    successEditKPIDataModal: function (_response) {
        console.log(_response);
        if (_response != null && _response != "") {
            if (_response.properties != null) {
                _response = _response.properties;
                for (var category in _response.kpidata) {
                    _response[category] = _response.kpidata[category];
                }
            }
            /* if (_response.valueUnit != null) {
                _response[_response.valueUnit + "Selected"] = true;
            } */
            if (_response.dataType != null) {
                _response[_response.dataType + "Selected"] = true;
            }
            if (_response.ownership != null) {
                _response[_response.ownership + "Selected"] = true;
            }
            _response.lastDate = Utility.timestampToFormatDate(_response.lastDate);
            _response.lastCheck = Utility.timestampToFormatDate(_response.lastCheck);
        } else if (KPIDataTabler.currentHighLevelType != "changeownership") {
            _response = {};
            _response.highLevelType = KPIDataTabler.currentHighLevelType;
        }
        if (KPIDataTabler.currentHighLevelType != "changeownership" && _response != null && _response != "" && _response.highLevelType != null) {
            ViewManager.render({
                "kpidata": _response
            }, "#genericModal", "templates/kpidata/edit" + _response.highLevelType.toLowerCase() + ".mst.html");
            $('#genericModal').modal('show');
            EditModalManager.currentLatitude = _response.latitude;
            EditModalManager.currentLongitude = _response.longitude;
            EditModalManager.checkOrganizationAndCreateMap("KPIDataEdit");
            EditModalManager.createNatureSelection();
            $("#selectNatureKPIDataEdit").val(_response.nature).trigger('change');
            $("#selectSubNatureKPIDataEdit").val(_response.subNature);
        } else {
            ViewManager.render({
                "kpidata": _response
            }, "#genericModal", "templates/kpidata/editchangeownership.mst.html");
            $('#genericModal').modal('show');
        }
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
                "valueName": $("#inputValueNameKPIDataEdit").val(),
                "valueType": $("#inputValueTypeKPIDataEdit").val(),
                "metric": $("#inputMetricKPIDataEdit").val(),
                "description": $("#inputDescriptionKPIDataEdit").val(),
                "info": $("#inputInfoKPIDataEdit").val(),
                "latitude": $("#inputLatitudeKPIDataEdit").val(),
                "longitude": $("#inputLongitudeKPIDataEdit").val(),
                "metadata": [{
                        "key": "note",
                        "value": $("#inputNoteMyPOIModal").val()
                    },
                    {
                        "key": "address",
                        "value": $("#inputAddressMyPOIModal").val()
                    },
                    {
                        "key": "civic",
                        "value": $("#inputCivicMyPOIModal").val()
                    },
                    {
                        "key": "city",
                        "value": $("#inputCityMyPOIModal").val()
                    },
                    {
                        "key": "province",
                        "value": $("#inputProvinceMyPOIModal").val()
                    },
                    {
                        "key": "phone",
                        "value": $("#inputPhoneMyPOIModal").val()
                    },
                    {
                        "key": "fax",
                        "value": $("#inputFaxMyPOIModal").val()
                    },
                    {
                        "key": "website",
                        "value": $("#inputWebsiteMyPOIModal").val()
                    },
                    {
                        "key": "email",
                        "value": $("#inputEmailMyPOIModal").val()
                    }
                ]
            }

            if ($("#selectNatureKPIDataEdit").val() != "") {
                kpiData.nature = $("#selectNatureKPIDataEdit").val();
            }
            if ($("#selectSubNatureKPIDataEdit").val() != "") {
                kpiData.subNature = $("#selectSubNatureKPIDataEdit").val();
            }
            if ($("#selectValueUnitKPIDataEdit").val() != "") {
                kpiData.valueUnit = $("#selectValueUnitKPIDataEdit").val();
            }
            if ($("#selectDataTypeKPIDataEdit").val() != "") {
                kpiData.dataType = $("#selectDataTypeKPIDataEdit").val();
            }
            if ($("#inputIdKPIDataEdit").val() != "") {
                kpiData.id = $("#inputIdKPIDataEdit").val();
            }
            if ($("#inputUsernameKPIDataEdit").val() != "") {
                kpiData.username = $("#inputUsernameKPIDataEdit").val();
            }
            if (typeof $("#inputLastDateKPIDataEdit").val() != "undefined" && $("#inputLastDateKPIDataEdit").val() != "") {
                kpiData.lastDate = new Date($("#inputLastDateKPIDataEdit").val()).getTime();
            }
            if ($("#inputLastValueKPIDataEdit").val() != "") {
                kpiData.lastValue = $("#inputLastValueKPIDataEdit").val();
            }
        } else {
            kpiData = kpiDataToSave;
        }

        console.log(kpiData);
        if (kpiData.highLevelType != "MyPOI") {
            if (typeof kpiData.id != "undefined") {
                KPIEditor.keycloak.updateToken(30).success(function () {
                    var query = QueryManager.createPatchKPIDataQuery(kpiData.id);
                    APIClient.executePatchQuery(query, kpiData, KPIEditor.keycloak.token,  KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
                }).error(function () {
                    var query = QueryManager.createPatchKPIDataQuery(kpiData.id);
                    APIClient.executePatchQuery(query, kpiData, Authentication.refreshTokenGetAccessToken(),  KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
                });
            } else {
                KPIEditor.keycloak.updateToken(30).success(function () {
                    var query = QueryManager.createPostKPIDataQuery();
                    APIClient.executePostQuery(query, kpiData, KPIEditor.keycloak.token,  KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
                }).error(function () {
                    var query = QueryManager.createPostKPIDataQuery();
                    APIClient.executePostQuery(query, kpiData, Authentication.refreshTokenGetAccessToken(),  KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
                });
            }
        } else {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createAddMyPOIQuery();
                APIClient.executePostQuery(query, kpiData, KPIEditor.keycloak.token,  KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createAddMyPOIQuery();
                APIClient.executePostQuery(query, kpiData, Authentication.refreshTokenGetAccessToken(),  KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
            });
        }
    },

    deleteKPIData(_id) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createDeleteKPIDataQuery(_id);
            APIClient.executeDeleteQuery(query, KPIEditor.keycloak.token, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createDeleteKPIDataQuery(_id);
            APIClient.executeDeleteQuery(query, Authentication.refreshTokenGetAccessToken(), KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
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
        if (KPIDataTabler.privacy == "public") {
            APIClient.suffix = KPIDataTabler.privacy + "/";
            KPIDataTabler.privacy = "";
            setTimeout(function(){$("input[name=inlineRadioOptions][value=public]").attr('checked', 'checked');},10);
        } else {
            APIClient.suffix = "";
        }
        KPIDataPager.currentPage = 0;
        KPIDataTabler.renderTable();
    },

    makePublic: function (_kpiId) {
        kpiData = {
            "id": _kpiId,
            "ownership": "public"
        }
        KPIDataTabler.saveKPIData(kpiData);
    },

    makePrivate: function (_kpiId) {
        kpiData = {
            "id": _kpiId,
            "ownership": "private"
        }
        KPIDataTabler.saveKPIData(kpiData);
    }

}