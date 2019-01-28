var KPIDelegationTabler = {

    currentKpiId: null,
    currentHighLevelType: null,

    renderTable: function (kpiId, highLevelType) {
        KPIDelegationTabler.currentKpiId = kpiId;
        KPIDelegationTabler.currentHighLevelType = highLevelType;
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDelegationTableQuery(KPIDelegationTabler.currentKpiId, KPIDelegationPager.currentPage, KPIDelegationPager.currentSize, KPIDelegationSorter.currentSortDirection, KPIDelegationSorter.currentSortBy, KPIDelegationFilter.currentSearchKey, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, KPIDelegationTabler.successQuery, KPIDelegationTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIDelegationTableQuery(KPIDelegationTabler.currentKpiId, KPIDelegationPager.currentPage, KPIDelegationPager.currentSize, KPIDelegationSorter.currentSortDirection, KPIDelegationSorter.currentSortBy, KPIDelegationFilter.currentSearchKey, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, KPIDelegationTabler.successQuery, KPIDelegationTabler.errorQuery);
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

        _response.kpiId = KPIDelegationTabler.currentKpiId;
        _response.currentKPIData = KPIDataTabler.getCurrentKPIData(KPIDelegationTabler.currentKpiId);

        _response.timestampToDate = MustacheFunctions.timestampToDate;
        
        _response.enableEdit = KPIDataTabler.enableEdit;

        console.log(_response);
        ViewManager.render({
            "response": _response
        }, "#kpidelegationtable", "templates/kpidelegation/kpidelegation.mst.html");
        
        $('table').DataTable({"searching": false,"paging":   false,
        "ordering": false,
        "info":     false, responsive: true});

        $('table').css("width", "");

        $('#inputFilterKPIDelegation').val(KPIDelegationFilter.currentSearchKey);
        $('#selectSizeKPIDelegation').val(KPIDelegationPager.currentSize);
    },

    editKPIDelegationModal: function (_id) {
        if (_id != null && _id != "") {
            KPIEditor.keycloak.updateToken(30).success(function () {
                var query = QueryManager.createGetKPIDelegationByIdQuery(_id, KPIEditor.keycloak.token);
                APIClient.executeGetQuery(query, KPIDelegationTabler.successEditKPIDelegationModal, KPIDelegationTabler.errorQuery);
            }).error(function () {
                var query = QueryManager.createGetKPIDelegationByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
                APIClient.executeGetQuery(query, KPIDelegationTabler.successEditKPIDelegationModal, KPIDelegationTabler.errorQuery);
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

    deleteKPIDelegationModal: function (_id, _usernameDelegated, _insertTime) {
        ViewManager.render({
            "kpidelegation": {
                "id": _id,
                "usernameDelegated": _usernameDelegated,
                "insertTime": _insertTime
            }
        }, "#genericModal", "templates/kpidelegation/deletekpidelegation.mst.html");
        $('#genericModal').modal('show');
    },

    saveKPIDelegation: function () {
        kpiDelegation = {
            "usernameDelegated": $("#inputUsernameDelegatedKPIDelegationEdit").val(),
            "insertTime":new Date().getTime() 
        }

        if ($("#inputElementIdKPIDelegationEdit").val() != "") {
            kpiDelegation.elementId = $("#inputElementIdKPIDelegationEdit").val();
        } else {
            kpiDelegation.elementId = KPIDelegationTabler.currentKpiId;
        }
        if ($("#inputIdKPIDelegationEdit").val() != "") {
            kpiDelegation.id = $("#inputIdKPIDelegationEdit").val();
        }
        if ($("#inputElementTypeKPIDelegationEdit").val() != "") {
            kpiDelegation.elementType = $("#inputElementTypeKPIDelegationEdit").val();
        } else {
            kpiDelegation.elementType = KPIDelegationTabler.currentHighLevelType;
        }
        if ($("#inputUsernameDelegatorKPIDelegationEdit").val() != "") {
            kpiDelegation.usernameDelegator = $("#inputUsernameDelegatorKPIDelegationEdit").val();
        }

        console.log(kpiDelegation);
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createSaveKPIDelegationQuery(KPIEditor.keycloak.token);
            APIClient.executePostQuery(query, kpiDelegation, KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createSaveKPIDelegationQuery(Authentication.refreshTokenGetAccessToken());
            APIClient.executePostQuery(query, kpiDelegation, KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);
        });
    },

    getDelegations: function(_kpiId, successCallBack, errorCallBack){
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDelegationTableQuery(_kpiId, null, null, null, null, null, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, successCallBack, errorCallBack);
        }).error(function () {
            var query = QueryManager.createGetKPIDelegationTableQuery(_kpiId, null, null, null, null, null, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, successCallBack, errorCallBack);
        });
    },

    createAnonymousDelegation: function(_kpiId, successCallBack){
        kpiDelegation = {
            "usernameDelegated": "ANONYMOUS",
            "insertTime":new Date().getTime(),
            "elementType": "MyKPI",
            "elementId": _kpiId
        }
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createSaveKPIDelegationQuery(KPIEditor.keycloak.token);
            APIClient.executePostQuery(query, kpiDelegation, successCallBack);
        }).error(function () {
            var query = QueryManager.createSaveKPIDelegationQuery(Authentication.refreshTokenGetAccessToken());
            APIClient.executePostQuery(query, kpiDelegation, successCallBack);
        });
    },

    deleteKPIDelegation(_id, successCallBack, errorCallBack) {
        KPIEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createGetKPIDelegationByIdQuery(_id, KPIEditor.keycloak.token);
            APIClient.executeGetQuery(query, function (_response) {
                _response.deleteTime = new Date().getTime();
                var saveQuery = QueryManager.createSaveKPIDelegationQuery(KPIEditor.keycloak.token);
                if (successCallBack != null && errorCallBack != null){
                    APIClient.executePostQuery(saveQuery, _response, successCallBack, errorCallBack);
                } else {
                    APIClient.executePostQuery(saveQuery, _response, KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);
                }
            }, KPIDelegationTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetKPIDelegationByIdQuery(_id, Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQuery(query, function (_response) {
                _response.deleteTime = new Date().getTime();
                var saveQuery = QueryManager.createSaveKPIDelegationQuery(Authentication.refreshTokenGetAccessToken());
                if (successCallBack != null && errorCallBack != null){
                    APIClient.executePostQuery(saveQuery, _response, successCallBack, errorCallBack);
                } else {
                    APIClient.executePostQuery(saveQuery, _response, KPIDelegationTabler.successSaveKPIDelegation, KPIDelegationTabler.errorQuery);
                }
            }, KPIDelegationTabler.errorQuery);
        });
    },

    successSaveKPIDelegation: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        KPIDelegationTabler.renderTable(_response.elementId, _response.highLevelType);
    },

    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null){
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    },


}