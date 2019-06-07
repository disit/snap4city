var Survey1Tabler = {

		renderTable: function (organization, questionNumber) {
			
			Survey1Editor.keycloak.updateToken(30).success(function () {
	            var query = QueryManager.createSurvey1ResponseTableQuery(questionNumber, organization, Survey1Editor.keycloak);
	            APIClient.executeGetQuery(query, Survey1Tabler.successQuery, Survey1Tabler.errorQuery);
	        }).error(function () {
	            var query = QueryManager.createSurvey1ResponseTableQuery(questionNumber, organization, Authentication.refreshTokenGetAccessToken());
	            APIClient.executeGetQuery(query, Survey1Tabler.successQuery, Survey1Tabler.errorQuery);
	        });
			
	    },
	    
	   successQuery: function (_response) {
    	
	        if ($("#survey1responsetable").length == 0) {
	            $("#indexPage").
	            append("<div id=\"survey1responsetable\" style=\"margin: 0px 10px\"></div>")
	        }      

	        ViewManager.renderTable({
	            "response": _response
	        }, "#survey1responsetable", "../templates/survey1response/survey1response.mst.html");

	        $('table').css("width", "");
	    }, 

	    errorQuery: function (_error) {
	        if (_error.responseText != null) {
	            alert(_error.responseText);
	        }
	        $('#genericModal').modal('hide');
	    },
}