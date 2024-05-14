var DeviceGrpTabler = {

    editDeviceGrpModal: async function (_id, _highLevelType) {
        DeviceGrpTabler.currentHighLevelType = _highLevelType;
        if (_id != null && _id != "") {
            if (_highLevelType != "MyPOI") {
                GrpEditor.keycloak.updateToken(30).success(function () {
                    var query = GrpQueryManager.createGetDeviceGrpByIdQuery(_id, GrpEditor.keycloak.token);
                    APIClient.executeGetQuery(query, DeviceGrpTabler.successEditDeviceGrpModal, DeviceGrpTabler.errorQuery);
                }).error(function () {
                    var query = GrpQueryManager.createGetDeviceGrpByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
                    APIClient.executeGetQuery(query, DeviceGrpTabler.successEditDeviceGrpModal, DeviceGrpTabler.errorQuery);
                });
            } else {
                KPIEditor.keycloak.updateToken(30).success(function () {
                    var query = QueryManager.createGetMyPOIQuery(_id, KPIEditor.keycloak.token);
                    APIClient.executeGetQuery(query, KPIDataTabler.successEditKPIDataModal, KPIDataTabler.errorQuery);
                }).error(function () {
                    var query = QueryManager.createGetMyPOIQuery(_id, Authentication.refreshTokenGetAccessToken());
                    APIClient.executeGetQuery(query, KPIDataTabler.successEditKPIDataModal, KPIDataTabler.errorQuery);
                });
            }
        } else if (_highLevelType != "changeownership") {
            DeviceGrpTabler.successEditDeviceGrpModal(null);
        }
    },
    
    successEditDeviceGrpModal: async function (_response) {
        console.log(_response);
        if (_response != null && _response != "") {
            if (_response.properties != null) {
                _response = _response.properties;
                for (var category in _response.kpidata) {
                    _response[category] = _response.kpidata[category];
                }
            }
            if (_response.dataType != null) {
                _response[_response.dataType + "Selected"] = true;
            }
            if (_response.ownership != null) {
                _response[_response.ownership + "Selected"] = true;
            }
            _response.updateTime = Utility.timestampToFormatDate(_response.updateTime);
        } else if (DeviceGrpTabler.currentHighLevelType != "changeownership") {
            _response = {};
            _response.highLevelType = DeviceGrpTabler.currentHighLevelType;
        }
        if (DeviceGrpTabler.currentHighLevelType != "changeownership") {
            ViewManager.render({
                "grpdata": _response
            }, "#genericModal", "templates/grpdata/edit" + _response.highLevelType.toLowerCase() + ".mst.html");
            $('#genericModal').modal('show');
        } else {
            ViewManager.render({
                "kpidata": _response
            }, "#genericModal", "templates/grpdata/editchangeownership.mst.html");
            $('#genericModal').modal('show');
        }
    },
    
    saveGrpData: async function (grpDataToSave) {
        if (grpDataToSave == null) {
            grpData = {
                "highLevelType": $("#inputHighLevelTypeGrpDataEdit").val(),
                "name": $("#inputNameGrpDataEdit").val(),
                "description": $("#inputDescriptionGrpDataEdit").val()
            }

            /*if ($("#inputDescriptionGrpDataEdit").val() != "") {
                grpData.description = $("#inputDescriptionGrpDataEdit").val();
            }*/
            
            if ($("#inputIdGrpDataEdit").val() != "") {
                grpData.id = $("#inputIdGrpDataEdit").val();
            }
            
            if ($("#inputUsernameGrpDataEdit").val() != "") {
                grpData.username = $("#inputUsernameGrpDataEdit").val();
            }

        } else {
            grpData = grpDataToSave;
        }

        if (grpData.highLevelType != "MyPOI") {
            if (typeof grpData.id != "undefined") {
                GrpEditor.keycloak.updateToken(30).success(function () {
                    var query = GrpQueryManager.createPatchDeviceGrpQuery(GrpEditor.keycloak.token, grpData.id);
                    APIClient.executePatchQuery(query, grpData, DeviceGrpTabler.successSaveGrpData, DeviceGrpTabler.errorQuery);
                }).error(function () {
                    var query = GrpQueryManager.createPatchDeviceGrpQuery(Authentication.refreshTokenGetAccessToken(), grpData.id);
                    APIClient.executePatchQuery(query, grpData, DeviceGrpTabler.successSaveGrpData, DeviceGrpTabler.errorQuery);
                });
            } else {
                GrpEditor.keycloak.updateToken(30).success(function () {
                    var query = GrpQueryManager.createPostDeviceGrpQuery(GrpEditor.keycloak.token);
                    APIClient.executePostQuery(query, grpData, DeviceGrpTabler.successSaveGrpData, DeviceGrpTabler.errorQuery);
                }).error(function () {
                    var query = GrpQueryManager.createPostDeviceGrpQuery(Authentication.refreshTokenGetAccessToken());
                    APIClient.executePostQuery(query, grpData, DeviceGrpTabler.successSaveGrpData, DeviceGrpTabler.errorQuery);
                });
            }
        } else {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createAddMyPOIQuery(KPIEditor.keycloak.token);
                APIClient.executePostQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createAddMyPOIQuery(Authentication.refreshTokenGetAccessToken());
                APIClient.executePostQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
            });
        }
    },
    
    successSaveGrpData: async function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        DeviceGrpTabler.renderTable();
    },
    
    errorQuery: async function (_error) {
        console.log(_error);
        if (_error.responseText != null && _error.responseText != "") {
            alert(_error.responseText);
        } else {
            alert("There was a problem to retrieve the data. Assure you have the right to view this data. The table of your groups will be shown.")
        }
        $('#genericModal').modal('hide');
    },

    renderTable: async function () {
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createGetDeviceGrpTableQuery(DeviceGrpTabler.privacy, DeviceGrpPager.currentPage, DeviceGrpPager.currentSize, DeviceGrpSorter.currentSortDirection, DeviceGrpSorter.currentSortBy, DeviceGrpFilter.currentSearchKey, GrpEditor.keycloak.token);
            APIClient.executeGetQuery(query, DeviceGrpTabler.successQuery, DeviceGrpTabler.errorQuery);
        }).error(function () {
            var query = GrpQueryManager.createGetDeviceGrpTableQuery(DeviceGrpTabler.privacy, DeviceGrpPager.currentPage, DeviceGrpPager.currentSize, DeviceGrpSorter.currentSortDirection, DeviceGrpSorter.currentSortBy, DeviceGrpFilter.currentSearchKey, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, DeviceGrpTabler.successQuery, DeviceGrpTabler.errorQuery);
        });
    },

    successQuery: async function (_response) {
        DeviceGrpTabler.currentDeviceGroupsPage = _response;
        GrpEditor.checkParameters();
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

        var isPublic = true;
        for (var i = 0; i < _response.content.length; i++) {
            _response.content[i].isPublic = (_response.content[i].ownership == "public");
            _response.content[i].escapedName = _response.content[i].name.replace(/'/g, "\\'");
            if(_response.content[i].ownership != "public" || _response.content[i].username != null) isPublic = false;
        }

        _response.enableEdit = DeviceGrpTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpidatatable", "templates/grpdata/devicegrp.mst.html");

        if(_response.enableEdit) {
            $('table').DataTable({
                "searching": false,
                "paging": false,
                "ordering": false,
                "info": false,
                responsive: true
            });
        } 
        else {
            $('table').DataTable({
                "searching": false,
                "paging": false,
                "ordering": false,
                "info": false,
                responsive: true,
                columnDefs: [
                    {
                        "targets": [ 8 ],
                        "visible": false
                    }                    
                ]
            });            
        }
    
        $('table').css("width", "");
        
        $("input[name=inlineRadioOptions][value='" + DeviceGrpTabler.privacy + "']").prop("checked", true);
        if(DeviceGrpTabler.privacy != "organization" && DeviceGrpTabler.privacy != "delegated" && isPublic) {             
            $("input[name=inlineRadioOptions][value='public']").prop("checked", true);            
        }        
        $('#inputFilterKPIData').val(DeviceGrpFilter.currentSearchKey);
        $('#selectSizeKPIData').val(DeviceGrpPager.currentSize);
        APIClient.suffix = "";        
        var query = GrpQueryManager.createGetDeviceGrpTableQuery("", DeviceGrpPager.currentPage, DeviceGrpPager.currentSize, DeviceGrpSorter.currentSortDirection, DeviceGrpSorter.currentSortBy, DeviceGrpFilter.currentSearchKey, GrpEditor.keycloak.token);
        APIClient.executeAsyncGetQuery(query, DeviceGrpTabler.chkIfUserHasGroups, function(){});
        var query2 = GrpQueryManager.createGetDeviceGrpTableQuery("organization", DeviceGrpPager.currentPage, DeviceGrpPager.currentSize, DeviceGrpSorter.currentSortDirection, DeviceGrpSorter.currentSortBy, DeviceGrpFilter.currentSearchKey, GrpEditor.keycloak.token);
        APIClient.executeAsyncGetQuery(query2, DeviceGrpTabler.chkIfUserHasPublInOrgGroups, function(){});
        var query3 = GrpQueryManager.createGetDeviceGrpTableQuery("delegated", DeviceGrpPager.currentPage, DeviceGrpPager.currentSize, DeviceGrpSorter.currentSortDirection, DeviceGrpSorter.currentSortBy, DeviceGrpFilter.currentSearchKey, GrpEditor.keycloak.token);
        APIClient.executeAsyncGetQuery(query3, DeviceGrpTabler.chkIfUserHasDelegatedGroups, function(){});
        var query4 = GrpQueryManager.createGetDeviceGrpTableQuery("public", DeviceGrpPager.currentPage, DeviceGrpPager.currentSize, DeviceGrpSorter.currentSortDirection, DeviceGrpSorter.currentSortBy, DeviceGrpFilter.currentSearchKey, GrpEditor.keycloak.token);
        APIClient.executeAsyncGetQuery(query4, DeviceGrpTabler.chkIfUserHasPublicGroups, function(){}); 
    },
    
    getCurrentKPIData: async function (_kpiId) {
        for (var i = 0; i < DeviceGrpTabler.currentDeviceGroupsPage.content.length; i++) {
            if (DeviceGrpTabler.currentDeviceGroupsPage.content[i].id == _kpiId) {
                return DeviceGrpTabler.currentDeviceGroupsPage.content[i];
            }
        }
        return null;
    },
    
    setPrivacy: async function () {
        DeviceGrpTabler.privacy = $("input[name=inlineRadioOptions]:checked").val();
        if (DeviceGrpTabler.privacy != "") {
            DeviceGrpTabler.enableEdit = false;
        } else {
            DeviceGrpTabler.enableEdit = true;
        }
        if (DeviceGrpTabler.privacy == "public") {
            APIClient.suffix = DeviceGrpTabler.privacy + "/";
            DeviceGrpTabler.privacy = "";          
            setTimeout(function(){$("input[name=inlineRadioOptions][value=public]").attr('checked', 'checked');},10);
        } else {
            APIClient.suffix = "";
        }
        DeviceGrpPager.currentPage = 0;
        DeviceGrpTabler.renderTable();        
    },
    
    chkIfUserHasGroups: async function(_response) {
        if(_response.content.length == 0) {
            $("input[name=inlineRadioOptions][value='']").prop("disabled",true);
            $("input[name=inlineRadioOptions][value='']").siblings().css("color","lightgray");
        }
    },
    
    chkIfUserHasPublInOrgGroups: async function(_response) {
        if(_response.content.length == 0) {
            $("input[name=inlineRadioOptions][value='organization']").prop("disabled",true);
            $("input[name=inlineRadioOptions][value='organization']").siblings().css("color","lightgray");
        }
    },
    
    chkIfUserHasDelegatedGroups: async function(_response) {
        console.log(_response);
        if(_response.content.length == 0) {
            $("input[name=inlineRadioOptions][value='delegated']").prop("disabled",true);
            $("input[name=inlineRadioOptions][value='delegated']").siblings().css("color","lightgray");
        }
    },
    
    chkIfUserHasPublicGroups: async function(_response) {
        if(_response.content.length == 0) {
            $("input[name=inlineRadioOptions][value='public']").prop("disabled",true);
            $("input[name=inlineRadioOptions][value='public']").siblings().css("color","lightgray");
        }
    },
    
    makePublic: async function (_kpiId) {
        //       
        
        //
        ownData = {
            "id": _kpiId,
            "ownership": "public"
        }
        const elem = document.getElementById('modal_loading1');
        if (elem !== null){
        elem.style.display = 'inline';
        DeviceGrpTabler.saveGrpData(ownData).then(() => {  
            console.log('change public');
                setTimeout(() => {
                elem.style.display = 'none';
                }, 2000);
            });
          }else{
            DeviceGrpTabler.saveGrpData(ownData);
        }
        //
    },

    makePrivate: async function (_kpiId) {
        //   
        ownData = {
            "id": _kpiId,
            "ownership": "private"
        }
        
        const elem = document.getElementById('modal_loading1');
        if (elem !== null){
        elem.style.display = 'inline';
        DeviceGrpTabler.saveGrpData(ownData).then(() => { 
                setTimeout(() => {
                    elem.style.display = 'none';
                    }, 2000);
                });
        }else{
            DeviceGrpTabler.saveGrpData(ownData);
        }
            
    },
    
    showGrpDataModal: async function (_id) {
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createGetDeviceGrpByIdQuery(_id, GrpEditor.keycloak.token);
            APIClient.executeGetQuery(query, DeviceGrpTabler.successShowGrpDataModal, DeviceGrpTabler.errorQuery);
        }).error(function () {
            var query = GrpQueryManager.createGetDeviceGrpByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, DeviceGrpTabler.successShowGrpDataModal, DeviceGrpTabler.errorQuery);
        });
    },
    
    successShowGrpDataModal: async function (_response) {
        console.log(_response);
        _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
        _response.updateTime = Utility.timestampToFormatDate(_response.updateTime);
        ViewManager.render({
            "kpidata": _response,
            "isPublic": (_response.ownership == "public")
        }, "#genericModal", "templates/grpdata/showkpidata.mst.html");
        $('#genericModal').modal('show');       
        $("#genericModal").draggable();
    },
    
    showReadonlyGrpDataModal: async function (_id) {
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createGetDeviceGrpByIdQuery(_id, GrpEditor.keycloak.token);
            APIClient.executeGetQuery(query, DeviceGrpTabler.successReadonlyShowGrpDataModal, DeviceGrpTabler.errorQuery);
        }).error(function () {
            var query = GrpQueryManager.createGetDeviceGrpByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, DeviceGrpTabler.successReadonlyShowGrpDataModal, DeviceGrpTabler.errorQuery);
        });
    },
    
    successReadonlyShowGrpDataModal: async function (_response) {
        console.log(_response);
        _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
        _response.updateTime = Utility.timestampToFormatDate(_response.updateTime);
        _response.enableEdit = false;
        ViewManager.render({
            "kpidata": _response,
            "isPublic": (_response.ownership == "public")
        }, "#genericModal", "templates/grpdata/showkpidata.mst.html");
        $('#genericModal').modal('show');
    },
    
    deleteDeviceGroupModal: async function (_id, _highLevelType, _valueName) {
        ViewManager.render({
            "kpidata": {
                "id": _id,
                "highLevelType": _highLevelType,
                "name": _valueName
            }
        }, "#genericModal", "templates/grpdata/deletekpidata.mst.html");
        $('#genericModal').modal('show');
    },
    clearDeviceGrpModal: async function (_id, _highLevelType, _valueName) {
        ViewManager.render({
            "kpidata": {
                "id": _id,
                "highLevelType": _highLevelType,
                "name": _valueName
            }
        }, "#genericModal", "templates/grpdata/clearkpidata.mst.html");
        $('#genericModal').modal('show');
    },
    
    deleteDeviceGroup(_id) {
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createDeleteDeviceGroupQuery(GrpEditor.keycloak.token, _id);
            APIClient.executeDeleteQuery(query, DeviceGrpTabler.successSaveGrpData, DeviceGrpTabler.errorQuery);
        }).error(function () {
            var query = GrpQueryManager.createDeleteDeviceGroupQuery(Authentication.refreshTokenGetAccessToken(), _id);
            APIClient.executeDeleteQuery(query, DeviceGrpTabler.successSaveGrpData, DeviceGrpTabler.errorQuery);
        });
    },
    clearDeviceGroup(_id) {
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createClearDeviceGroupQuery(GrpEditor.keycloak.token, _id);
            APIClient.executeDeleteQuery(query, DeviceGrpTabler.successSaveGrpData, DeviceGrpTabler.errorQuery);
        }).error(function () {
            var query = GrpQueryManager.createClearDeviceGroupQuery(Authentication.refreshTokenGetAccessToken(), _id);
            APIClient.executeDeleteQuery(query, DeviceGrpTabler.successSaveGrpData, DeviceGrpTabler.errorQuery);
        });
    },
    showGrpElemsModal: async function (_id) {
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createGetGrpElemsByIdsQuery(_id, GrpEditor.keycloak.token);
            APIClient.executeGetQuery(query, DeviceGrpTabler.successShowGrpElemsModal, DeviceGrpTabler.errorQuery);
        }).error(function () {
            var query = GrpQueryManager.createGetGrpElemsByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, DeviceGrpTabler.successShowGrpElemsModal, DeviceGrpTabler.errorQuery);
        });
    },
    
    successShowGrpElemsModal: async function (_response) {
        console.log(_response);
        if (_response != null && _response != "") {
            _response.insertTime = Utility.timestampToFormatDate(_response.insertTime);
            _response.deleteTime = Utility.timestampToFormatDate(_response.deleteTime);
        } else {
            _response = true;
        }
        console.log(_response);
        ViewManager.render({
            "devicegrpelems": _response
        }, "#genericModal", "templates/grpdata/elems/showelems.mst.html");
        $('#genericModal').modal('show');
    },
    
    currentDeviceGroupsPage: null,
    privacy: "",
    enableEdit: true,

    saveKPIData: async function (kpiDataToSave) {
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
                    var query = QueryManager.createPatchKPIDataQuery(KPIEditor.keycloak.token, kpiData.id);
                    APIClient.executePatchQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
                }).error(function () {
                    var query = QueryManager.createPatchKPIDataQuery(Authentication.refreshTokenGetAccessToken(), kpiData.id);
                    APIClient.executePatchQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
                });
            } else {
                KPIEditor.keycloak.updateToken(30).success(function () {
                    var query = QueryManager.createPostKPIDataQuery(KPIEditor.keycloak.token);
                    APIClient.executePostQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
                }).error(function () {
                    var query = QueryManager.createPostKPIDataQuery(Authentication.refreshTokenGetAccessToken());
                    APIClient.executePostQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
                });
            }
        } else {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createAddMyPOIQuery(KPIEditor.keycloak.token);
                APIClient.executePostQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createAddMyPOIQuery(Authentication.refreshTokenGetAccessToken());
                APIClient.executePostQuery(query, kpiData, KPIDataTabler.successSaveKPIData, KPIDataTabler.errorQuery);
            });
        }
    },

    successSaveKPIData: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIDataTabler.renderTable();
    }

}