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
        var kpiId = "";
        var operation = "";
        var dataType = "";
        var highLevelType = "";


        var decodedParameters = window.location.search;
        while (decodedParameters.indexOf("%") != -1) {
            decodedParameters = decodeURIComponent(decodedParameters);
        };
        GrpEditor.parametersArray = decodedParameters.substring(1).split("&");

        if (GrpEditor.parametersArray.length != 0 && GrpEditor.parametersArray[0] != "") {

            GrpEditor.withParameters = true;

            for (var i = 0; i < GrpEditor.parametersArray.length; i++) {
                if (GrpEditor.parametersArray[i].indexOf("kpiId") != -1) {
                    kpiId = GrpEditor.parametersArray[i].substring(GrpEditor.parametersArray[i].indexOf("=") + 1).split(";");
                }
                if (GrpEditor.parametersArray[i].indexOf("operation") != -1) {
                    operation = GrpEditor.parametersArray[i].substring(GrpEditor.parametersArray[i].indexOf("=") + 1).split(";");
                }
                if (GrpEditor.parametersArray[i].indexOf("dataType") != -1) {
                    dataType = GrpEditor.parametersArray[i].substring(GrpEditor.parametersArray[i].indexOf("=") + 1).split(";");
                }
                if (GrpEditor.parametersArray[i].indexOf("highLevelType") != -1) {
                    highLevelType = GrpEditor.parametersArray[i].substring(GrpEditor.parametersArray[i].indexOf("=") + 1).split(";");
                }
            }

            if (kpiId == "") {
                alert("kpiId parameter is null and is mandatory");
            } else if (operation == "") {
                alert("operation parameter is null and is mandatory");
            } else {
                if (operation == "show") {
                    KPIDataTabler.showKPIDataModal(kpiId);
                } else if (operation == "values") {
                    if (dataType == "") {
                        alert("dataType parameter is null and is mandatory for 'values' operation");
                    } else {
                        KPIValueTabler.renderTable(kpiId, dataType);
                    }
                } else if (operation == "metadata") {
                    KPIDelegationTabler.renderTable(kpiId);
                } else if (operation == "delegations") {
                    if (highLevelType == "") {
                        alert("highLevelType parameter is null and is mandatory for 'delegations' operation");
                    } else {
                        KPIDelegationTabler.renderTable(kpiId, highLevelType);
                    }
                }
            }
        }
    }
}