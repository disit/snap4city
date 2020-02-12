var GrpElementTabler = {

    currentGrpId: null,
    forceReadonly: false,

    deleteElementModal: function (_id, _grpId, _username, _elementId, _elementType, _elementName, _insertTime ) {
        ViewManager.render({
            "groupelement": {
                "no": _id,
                "username": _username,
                "grpId": _grpId,                
                "elementId": _elementId,
                "elementType": _elementType,
                "elementName": _elementName,
                "insertTime": Utility.timestampToFormatDate(_insertTime)
            }
        }, "#genericModal", "templates/grpdata/elems/del.mst.html");
        $('#genericModal').modal('show');
    },
    
    deleteElement(_grpId, _id, successCallBack, errorCallBack) {
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createDeleteElmtFromGroupQuery(GrpEditor.keycloak.token, _grpId, _id);
            if (successCallBack != null && errorCallBack != null) {
                APIClient.executeDeleteQuery(query, successCallBack, errorCallBack);
            } else {
                APIClient.executeDeleteQuery(query, GrpElementTabler.successAddElmtToGrp, GrpElementTabler.errorQuery);
            }
        }).error(function () {
            var query = GrpQueryManager.createDeleteElmtFromGroupQuery(Authentication.refreshTokenGetAccessToken(), _id);
            if (successCallBack != null && errorCallBack != null) {
                APIClient.executeDeleteQuery(query, successCallBack, errorCallBack);
            } else {
                APIClient.executeDeleteQuery(query, GrpElementTabler.successAddElmtToGrp, GrpElementTabler.errorQuery);
            }
        });
    },
    
    submitAddElmts: function() {
        var elmtToAdd = [];
        var d = new Date();
        $(".newelchkbox:checked").not(":disabled").each(function(){
            elmtToAdd.push({
                deviceGroupId: GrpElementTabler.currentGrpId,
                elementId: $(this).data("elementid")?$(this).data("elementid"):$(this).data("surrogatekey"),
                elementType: $(this).data("elementtype"),
                insertTime: d.getTime()                 
            });
        });
        console.log(elmtToAdd);
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createPostAddElmtToGrp(GrpEditor.keycloak.token, GrpElementTabler.currentGrpId);
            APIClient.executePostQuery(query, elmtToAdd, GrpElementTabler.successAddElmtToGrp, GrpElementTabler.errorQuery);
        }).error(function () {
            var query = GrpQueryManager.createPostAddElmtToGrp(Authentication.refreshTokenGetAccessToken(), GrpElementTabler.currentGrpId);
            APIClient.executePostQuery(query, elmtToAdd, GrpElementTabler.successAddElmtToGrp, GrpElementTabler.errorQuery);
        });
    },
    
    successAddElmtToGrp: function() {
        $('#genericModal').modal('hide');
        GrpElementTabler.renderTable(GrpElementTabler.currentGrpId);        
    },
    
    searchInNewElmts: function() {
        $(".newel").each(
            function() {
                if($(this).text().toLowerCase().indexOf($("#searchNewElmtTxtbox").val().toLowerCase()) > -1 ) {
                    $(this).show();
                } else { 
                    $(this).hide();
                }
            }
        ); 
    },
    selAll: function() {
        $(".newel").each(
            function() {
                if($(this).text().indexOf($("#searchNewElmtTxtbox").val()) > -1 ) {
                    $(this).find("input").prop("checked",$("#selvisible").prop("checked"));
                } 
            }
        ); 
        GrpElementTabler.updAddNewElmtBtnLbl();
    },
    updAddNewElmtBtnLbl: function() {
      if($(".newelchkbox:checked").not(":disabled").length > 0) {
          $(".addNewElmtBtn").text("Add ("+$(".newelchkbox:checked").not(":disabled").length+")");
          $(".addNewElmtBtn").prop("disabled",false);
          $(".addNewElmtBtn").css("cursor","pointer");
      }
      else {
          $(".addNewElmtBtn").prop("disabled",true);
          $(".addNewElmtBtn").text("Add");
          $(".addNewElmtBtn").css("cursor","not-allowed");
      }
    },
    addNewElmtToGrp: function(elmtType) {
        ViewManager.render({}, "#genericModal", "templates/grpdata/elems/wait.mst.html"); $('#genericModal').modal('show');
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createGetAvailItemsQuery(GrpEditor.keycloak.token,GrpElementTabler.currentGrpId,elmtType);
            APIClient.executeGetQuery(query, GrpElementTabler.getAvailItemsSuccess, function(){ console.log("createGetAvailItemsQuery error"); }); 
        }).error(function () {
            var query = GrpQueryManager.createGetAvailItemsQuery(Authentication.refreshTokenGetAccessToken(),GrpElementTabler.currentGrpId,elmtType);
            APIClient.executeGetQuery(query, GrpElementTabler.getAvailItemsSuccess, function(){ console.log("createGetAvailItemsQuery error"); }); 
        });
    },
    getAvailItemsSuccess: function(_response) {
        setTimeout(function(){            
            $('#genericModal').modal('hide');
            console.log(_response);
            var elementType = _response[0].elementType;
            if(elementType == null) elementType = "MyKPI";            
            ViewManager.render({
                "elementType": elementType,
                "isHeatmap": (elementType == "HeatmapID"),
                "isNotHeatmap": (elementType != "HeatmapID"),
                "availItems": _response
            }, "#genericModal", "templates/grpdata/elems/add.mst.html");
            $('#genericModal').modal('show');
            GrpElementTabler.updAddNewElmtBtnLbl();
            GrpEditor.keycloak.updateToken(30).success(function () {
                var query = GrpQueryManager.createGetGrpElemsTableQuery(GrpElementTabler.currentGrpId, -1, false, false, false, false, GrpEditor.keycloak.token);
                APIClient.executeGetQuery(query, GrpElementTabler.preselectMembers, function(){ console.log("createGetGrpElemsTableQuery error"); });     
            }).error(function () {
                var query = GrpQueryManager.createGetGrpElemsTableQuery(GrpElementTabler.currentGrpId, -1, false, false, false, false, Authentication.refreshTokenGetAccessToken());
                APIClient.executeGetQuery(query, GrpElementTabler.preselectMembers, function(){ console.log("createGetGrpElemsTableQuery error"); });     
            });
        },1000);
    },
    preselectMembers: function(_response) {
        console.log("selecting and disabling the following:");
        _response.forEach(function(element){
            console.log(element);
            console.log("#add"+$.escapeSelector((element.username && element.elementType != "Sensor"?element.username:"")+(element.elementType?element.elementType:"")+(element.elementType == "MyKPI" ? element.elementName : element.elementId)));
            console.log("occurrences: "+$("#add"+$.escapeSelector((element.username?element.username:"")+(element.elementType?element.elementType:"")+(element.elementType == "MyKPI" ? element.elementName : element.elementId))).length);
            $("#add"+$.escapeSelector((element.username && element.elementType != "Sensor"?element.username:"")+(element.elementType?element.elementType:"")+(element.elementType == "MyKPI" ? element.elementName : element.elementId))).prop('checked',true);
            $("#add"+$.escapeSelector((element.username && element.elementType != "Sensor"?element.username:"")+(element.elementType?element.elementType:"")+(element.elementType == "MyKPI" ? element.elementName : element.elementId))).prop('disabled',true);
        });
    },
    renderTable: function (grpId, forceReadonly = false) {        
        GrpElementTabler.currentGrpId = grpId;
        GrpElementTabler.forceReadonly = forceReadonly;
        console.log("forceReadonly: "+GrpElementTabler.forceReadonly);
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createGetGrpElemsTableQuery(GrpElementTabler.currentGrpId, GrpElementPager.currentPage, GrpElementPager.currentSize, GrpElementSorter.currentSortDirection, GrpElementSorter.currentSortBy, GrpElementFilter.currentSearchKey, GrpEditor.keycloak.token);
            APIClient.executeGetQuery(query, GrpElementTabler.successQuery, GrpElementTabler.errorQuery);
        }).error(function () {
            var query = GrpQueryManager.createGetGrpElemsTableQuery(GrpElementTabler.currentGrpId, GrpElementPager.currentPage, GrpElementPager.currentSize, GrpElementSorter.currentSortDirection, GrpElementSorter.currentSortBy, GrpElementFilter.currentSearchKey, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, GrpElementTabler.successQuery, GrpElementTabler.errorQuery);
        });
    },

    successQuery: function (_response) {
        console.log("forceReadonly: "+GrpElementTabler.forceReadonly);
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

        _response.grpId = GrpElementTabler.currentGrpId;
        _response.currentGrpData = DeviceGrpTabler.getCurrentKPIData(GrpElementTabler.currentGrpId);
        if(_response.currentGrpData == null) {
            var query = GrpQueryManager.createGetDeviceGrpByIdQuery(_response.grpId, GrpEditor.keycloak.token);
            APIClient.executeGetQuery(query, function(_iresponse){ _response.currentGrpData = _iresponse; }, function(){} );
        }

        _response.timestampToDate = MustacheFunctions.timestampToDate;

        _response.enableEdit = DeviceGrpTabler.enableEdit;
        if(GrpElementTabler.forceReadonly) _response.enableEdit = false;
        
        for (var i = 0; i < _response.content.length; i++) {
            _response.content[i].escapedElementName = _response.content[i].elementName.replace(/'/g, "\\'");
        }

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpidelegationtable", "templates/grpdata/elems/elems.mst.html");

        $('table').DataTable({
            "searching": false,
            "paging": false,
            "ordering": false,
            "info": false,
            responsive: true
        });

        $('table').css("width", "");

        $('#inputFilterKPIDelegation').val(GrpElementFilter.currentSearchKey);
        $('#selectSizeKPIDelegation').val(GrpElementPager.currentSize);
       
        var query = GrpQueryManager.createGetNewGrpElmtBtnsQuery(GrpEditor.keycloak.token,GrpElementTabler.currentGrpId);
        APIClient.executeGetQuery(query, GrpElementTabler.newGrpElmtBtnsSuccess, function(){ console.log("GetNewGrpElmtBtnsQuery error"); });
        var sensorsQuery = GrpQueryManager.createGetSensorsAPIQuery(GrpEditor.keycloak.token);
        APIClient.executeGetQuery(sensorsQuery, GrpElementTabler.addSensorBtn, function(){ console.log("createGetSensorsAPIQuery error"); });
        
        $('#simplyclose').click();
        
    },
    
    addSensorBtn: function(_response) {
        if(_response["payload"] !== undefined && _response["payload"].length > 0) {
            $("#addnewelement").append($('<button class="btn btn-warning" style="color: white;padding: 0.5rem; margin-bottom:0.5rem; margin-left:0.5rem; margin-right:0.5rem;" type="button" onclick="GrpElementTabler.addNewSensorToGrp();">Add Sensor</button>"'));            
        }
    },
    
    newGrpElmtBtnsSuccess: function(_response) {
        if(_response) {
            _response.forEach(function(elementType) {
                $("#addnewelement").append($('<button class="btn btn-warning" style="color: white;padding: 0.5rem; margin-bottom:0.5rem; margin-left:0.5rem; margin-right:0.5rem;" type="button" onclick="GrpElementTabler.addNewElmtToGrp(\''+elementType+'\');">Add '+elementType+'</button>"'));            
            });
        }
        else {
            $("#addnewelement").append($('<div class="alert alert-light" style="padding: 0.375rem 0.75rem; position:relative; top: -0.3rem; font-weight:bold;">NOTHING CAN BE ADDED</div>'));
        }        
        console.log("available element types");
        console.log(_response);        
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
        $('#genericModal').modal('hide');
        GrpDelegationTabler.renderTable(_response.elementId, _response.elementType);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null) {
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    },
    
    addNewSensorToGrp: function(pagesize=10, pagenum=1, search="") {
        ViewManager.render({}, "#genericModal", "templates/grpdata/elems/wait.mst.html"); $('#genericModal').modal('show');
        GrpEditor.keycloak.updateToken(30).success(function () {
            var query = GrpQueryManager.createGetSensorsAPIQuery(GrpEditor.keycloak.token,pagesize,pagenum,search);
            APIClient.executeGetQuery(query, GrpElementTabler.getAvailSensorsSuccess, function(){ console.log("createGetAvailItemsQuery error"); }); 
        }).error(function () {
            var query = GrpQueryManager.createGetSensorsAPIQuery(Authentication.refreshTokenGetAccessToken(),pagesize,pagenum,search);
            APIClient.executeGetQuery(query, GrpElementTabler.getAvailSensorsSuccess, function(){ console.log("createGetAvailItemsQuery error"); }); 
        });
    },
    
    getAvailSensorsSuccess: function(_response) {
        setTimeout(function(){            
            $('#genericModal').modal('hide');      
            for(var i = 0; i < _response["payload"].length; i++) {
                _response["payload"][i]["deviceType"] = _response["payload"][i]["deviceType"]
                        .split("_").join(" ")
                        .replace("IoTSensor","IoT Sensor")
                        .replace("SensorSite","Sensor Site");
                _response["payload"][i]["deviceName"] = _response["payload"][i]["deviceName"]
                        .split("_").join(" ");
            }
            ViewManager.render({
                "availItems": _response["payload"],
                "pageNum": _response["heading"]["pageNum"],
                "pageSize": _response["heading"]["pageSize"],
                "prevDisabled": (_response["heading"]["pageNum"] == 1 ? "disabled" : ""),
                "prevPointer": (_response["heading"]["pageNum"] == 1 ? "" : "cursor:pointer;"),
                "prev": _response["heading"]["pageNum"]-1,
                "nextDisabled": (_response["heading"]["pageSize"] != _response["payload"].length ? "disabled" : ""),
                "nextPointer": (_response["heading"]["pageSize"] != _response["payload"].length ? "" : "cursor:pointer;"),
                "next": _response["heading"]["pageNum"]+1,                
            }, "#genericModal", "templates/grpdata/elems/sensors/add.mst.html");
            $('#genericModal').modal('show');
            GrpElementTabler.updAddNewElmtBtnLbl();
            GrpEditor.keycloak.updateToken(30).success(function () {
                var query = GrpQueryManager.createGetGrpElemsTableQuery(GrpElementTabler.currentGrpId, -1, false, false, false, false, GrpEditor.keycloak.token);
                APIClient.executeGetQuery(query, GrpElementTabler.preselectMembers, function(){ console.log("createGetGrpElemsTableQuery error"); });     
            }).error(function () {
                var query = GrpQueryManager.createGetGrpElemsTableQuery(GrpElementTabler.currentGrpId, -1, false, false, false, false, Authentication.refreshTokenGetAccessToken());
                APIClient.executeGetQuery(query, GrpElementTabler.preselectMembers, function(){ console.log("createGetGrpElemsTableQuery error"); });     
            });
        },1000);
    }

}