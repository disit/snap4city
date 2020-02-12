var GrpEditor = {

    initialize: function () {

        GrpEditor.keycloak = Keycloak({
            "realm": "master",
            "url": Authentication.url,
            "clientId": Authentication.clientId
        });

        GrpEditor.keycloak.init({
            onLoad: 'check-sso'
        }).success(
            function (authenticated) {
                console.log(authenticated);
                if (authenticated) {
                    console.log("AUTHENTICATED");
                    console.log("Render Table");
                    DeviceGrpTabler.renderTable();
                } else {
                    GrpEditor.keycloak.login();
                }
            }).error(function () {

        });
    },

    checkLogin: function () {
        if ($("#inputUsername").val() != "") {
            Authentication.username = $("#inputUsername").val();
            if ($("#inputPassword").val() != "") {
                Authentication.password = $("#inputPassword").val();
                if (Authentication.refreshTokenGetAccessToken() != "") {
                    DeviceGrpTabler.renderTable();
                };
            } else {
                alert("Insert Password");
            }
        } else {
            alert("Insert Username");
        }
    },

    checkParameters: function () {
        var id = null;
        var op = null;
        var decodedParameters = window.location.search;
        while (decodedParameters.indexOf("%") != -1) {
            decodedParameters = decodeURIComponent(decodedParameters);
        };
        GrpEditor.parametersArray = decodedParameters.substring(1).split("&");
        if (GrpEditor.parametersArray.length != 0 && GrpEditor.parametersArray[0] != "") {
            GrpEditor.withParameters = true;
            for (var i = 0; i < GrpEditor.parametersArray.length; i++) {
                if (GrpEditor.parametersArray[i].indexOf("id") != -1) {
                    id = GrpEditor.parametersArray[i].substring(GrpEditor.parametersArray[i].indexOf("=") + 1).split(";");
                }
                if (GrpEditor.parametersArray[i].indexOf("op") != -1) {
                    op = GrpEditor.parametersArray[i].substring(GrpEditor.parametersArray[i].indexOf("=") + 1).split(";");
                }
            }
            if(id != null) {
                if (op == null) {
                    DeviceGrpTabler.showGrpDataModal(id);
                } else if (op == "content") {
                    GrpElementTabler.renderTable(id);
                } else if (op == "visib") {
                    GrpDelegationTabler.renderTable(id);                    
                }                
            }
        }
        else {
            GrpEditor.withParameters = false;
        }
    }
}