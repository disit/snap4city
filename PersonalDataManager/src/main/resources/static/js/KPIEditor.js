var KPIEditor = {

    initialize: function () {

        KPIEditor.keycloak = Keycloak({
            "realm": "master",
            "url": Authentication.url,
            "clientId": Authentication.clientId
        });

        KPIEditor.keycloak.init({
            onLoad: 'check-sso'
        }).success(
            function (authenticated) {
                //console.log(authenticated);
                if (authenticated) {
                    console.log("AUTHENTICATED");
                    console.log("Render Table");
                    KPIDataTabler.renderTable();
                } else {
                    KPIEditor.keycloak.login();
                }
            }).error(function () {

        });
    },

    checkLogin:  function () {
        if ($("#inputUsername").val() != "") {
            Authentication.username = $("#inputUsername").val();
            if ($("#inputPassword").val() != "") {
                Authentication.password = $("#inputPassword").val();
                if (Authentication.refreshTokenGetAccessToken() != "") {
                    KPIDataTabler.renderTable();
                };
            } else {
                alert("Insert Password");
            }
        } else {
            alert("Insert Username");
        }
    },

    isRoot:  function(){
        return KPIEditor.keycloak.tokenParsed.roles.includes("RootAdmin");
    },

    checkParameters:  function () {
        var kpiId = "";
        var operation = "";
        var dataType = "";
        var highLevelType = "";


        var decodedParameters = window.location.search;
        while (decodedParameters.indexOf("%") != -1) {
            decodedParameters = decodeURIComponent(decodedParameters);
        };
        KPIEditor.parametersArray = decodedParameters.substring(1).split("&");

        if (KPIEditor.parametersArray.length != 0 && KPIEditor.parametersArray[0] != "") {

            KPIEditor.withParameters = true;

            for (var i = 0; i < KPIEditor.parametersArray.length; i++) {
                if (KPIEditor.parametersArray[i].indexOf("kpiId") != -1) {
                    kpiId = KPIEditor.parametersArray[i].substring(KPIEditor.parametersArray[i].indexOf("=") + 1).split(";");
                }
                if (KPIEditor.parametersArray[i].indexOf("operation") != -1) {
                    operation = KPIEditor.parametersArray[i].substring(KPIEditor.parametersArray[i].indexOf("=") + 1).split(";");
                }
                if (KPIEditor.parametersArray[i].indexOf("dataType") != -1) {
                    dataType = KPIEditor.parametersArray[i].substring(KPIEditor.parametersArray[i].indexOf("=") + 1).split(";");
                }
                if (KPIEditor.parametersArray[i].indexOf("highLevelType") != -1) {
                    highLevelType = KPIEditor.parametersArray[i].substring(KPIEditor.parametersArray[i].indexOf("=") + 1).split(";");
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