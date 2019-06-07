var EngagerTabler = {

    currentEngagementPage: null, 
    engagementEnabledJSON: null ,

	checkEngagementEnabled: function () {
		EngagerEditor.keycloak.updateToken(30).success(function () {
			console.log(EngagerEditor.keycloak);
            var query = QueryManager.createGetEngagementEnabledQuery(EngagerEditor.keycloak);
            APIClient.executeGetQueryMPD(query, EngagerTabler.renderTable, EngagerTabler.errorQuery);
        }).error(function () {
            var query = QueryManager.createGetEngagementEnabledQuery(Authentication.refreshTokenGetAccessToken());
            APIClient.executeGetQueryMPD(query, EngagerTabler.renderTable, EngagerTabler.errorQuery);
        });
	},
	
    renderTable: function (_response) {
    	//enable default scenario with no parameters passed (like for prev, next buttons)
    	if ((_response===undefined)&&(EngagerTabler.engagementEnabledJSON!==null))
    		_response=new Array(EngagerTabler.engagementEnabledJSON);
    	
    	//save for later edit
    	if ((_response!== undefined)&&(_response[0]!==undefined))
    		EngagerTabler.engagementEnabledJSON=_response[0];	
    	
    	//check if we need to enable engagement or not 
    	if ((_response!== undefined)&&(_response[0]!==undefined)&&(_response[0].variableValue==='true'))	{	
    		EngagerEditor.keycloak.updateToken(30).success(function () {
    				var query = QueryManager.createGetEngagementTableQuery(EngagerPager.currentPage, EngagerPager.currentSize, EngagerSorter.currentSortDirection, EngagerSorter.currentSortBy, EngagerFilter.currentSearchKey, EngagerEditor.keycloak.token);
    				APIClient.executeGetQuery(query, EngagerTabler.engagementEnabledResponse, EngagerTabler.errorQuery);
    		});
    	}
    	else
    		EngagerTabler.engagementDisabledResponse();
    },

    engagementEnabledResponse: function (_response) {
     	
    	$("#engagementdisabled").remove();
    	
        EngagerTabler.currentEngagementPage = _response;
        if ($("#engagementtable").length == 0) {
            $("#indexPage").
            append("<div id=\"engagementtable\" style=\"margin: 0px 20px\"></div>")
        }
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
            if (_response.content[i].message.length>30)
            	_response.content[i].messageshort=_response.content[i].message.substring(0,30)+"[...]";
            else
            	_response.content[i].messageshort=_response.content[i].message;
        }

        ViewManager.renderTable({
            "response": _response
        }, "#engagementtable", "templates/engager/engagement.mst.html");

        $('table').DataTable({
            "searching": false,
            "paging": false,
            "ordering": false,
            "info": false,
            responsive: true
        });

        $('table').css("width", "");

        $('#inputFilterEngager').val(EngagerFilter.currentSearchKey);
        $('#selectSizeEngager').val(EngagerPager.currentSize);
    },

    engagementDisabledResponse: function () {
    	
    	$("#engagementtable").remove();
    	
    	if ($("#engagementdisabled").length == 0) {
            $("#indexPage").
            append("<div id=\"engagementdisabled\" style=\"margin: 0px 20px\">prot</div>")
        }
    	
    	ViewManager.render("#engagementdisabled", "templates/engager/engagementDisabled.mst.html");
    },
    
    errorQuery: function (_error) {
        console.log(_error);
        if (_error.responseText != null) {
            alert(_error.responseText);
        }
        $('#genericModal').modal('hide');
    },

    editEngagerModal: function (_type) {
    	
    	//can be 
    	//1-disable
    	//2-enable
    	//3-delete all
    	
    	switch (_type){
    	case 'deleteall':
    		ViewManager.render("#genericModal", "templates/engager/deleteall.mst.html");
            $('#genericModal').modal('show');
            break;
    	case 'disable':
    		ViewManager.render("#genericModal", "templates/engager/disable.mst.html");
        	$('#genericModal').modal('show');
    		break;
    	case 'enable':
    		if (EngagerTabler.engagementEnabledJSON == undefined) {
    			//create a new one TODO
    			EngagerTabler.engagementEnabledJSON= {
    					dataTime: new Date().getTime(),
    					motivation: "ASSISTANCE_ENABLED",
    					variableUnit: "boolean"
    			};
    		}

    		EngagerTabler.changeEngagementEnabled('true');
    		
    	}
    },  

    deleteEngagementModal: function (_id, _title, _ruleName) {
        ViewManager.renderTable({
            "engagement": {
                "id": _id,
                "title": _title,
                "ruleName": _ruleName
            }
        }, "#genericModal", "templates/engager/deleteengagement.mst.html");
        $('#genericModal').modal('show');
    },

    deleteEngagement(_id) {
    	EngagerEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createDeleteEngagementQuery(EngagerEditor.keycloak.token, _id);
            APIClient.executeDeleteQuery(query, EngagerTabler.successSaveEngager, EngagerTabler.errorQuery);
        });
    },
    
    deleteAllEngagement() {
    	EngagerEditor.keycloak.updateToken(30).success(function () {
            var query = QueryManager.createDeleteAllEngagementQuery(EngagerEditor.keycloak.token);
            APIClient.executeDeleteQuery(query, EngagerTabler.successSaveEngager, EngagerTabler.errorQuery);
        });
    },
    
    disableEngagement() {
    	EngagerTabler.deleteAllEngagement();
    	EngagerTabler.changeEngagementEnabled('false');
    },

    changeEngagementEnabled(_value){
    	EngagerTabler.engagementEnabledJSON.variableValue=_value;
    	
		EngagerEditor.keycloak.updateToken(30).success(function () {
			var query = QueryManager.createPostEngagementEnabledQuery(EngagerEditor.keycloak);
            APIClient.executePostQueryMPD(query, EngagerTabler.engagementEnabledJSON, EngagerTabler.renderTable, EngagerTabler.errorQuery);
        });
    },
    
    
    successSaveEngager: function (_response) {
        console.log(_response);
        $('#genericModal').modal('hide');
        EngagerTabler.renderTable();
    },
}