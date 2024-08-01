var GrpEditor = {
    initialize: async function () {
        GrpEditor.keycloak = new Keycloak({
            "realm": "master",
            "url": Authentication.url,
            "clientId": Authentication.clientId
        });

        try {
            await GrpEditor.keycloak.init({
                onLoad: 'check-sso',
                checkLoginIframe: (location.protocol === 'https')
            });
            const authenticated = GrpEditor.keycloak.authenticated;
            console.log(authenticated);
            if (authenticated) {
                console.log("AUTHENTICATED");
                console.log("Render Table");
                DeviceGrpTabler.renderTable();
            } else {
                GrpEditor.keycloak.login();
            }
        } catch (error) {
            console.error(error);
        }
    },

    checkLogin: function () {
        if ($("#inputUsername").val() != "") {
            Authentication.username = $("#inputUsername").val();
            if ($("#inputPassword").val() != "") {
                Authentication.password = $("#inputPassword").val();
                if (Authentication.refreshTokenGetAccessToken() != "") {
                    DeviceGrpTabler.renderTable();
                }
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
        }
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
            if (id != null) {
                if (op == null) {
                    DeviceGrpTabler.showGrpDataModal(id);
                } else if (op == "content") {
                    GrpElementTabler.renderTable(id);
                } else if (op == "visib") {
                    GrpDelegationTabler.renderTable(id);
                }
            }
        } else {
            GrpEditor.withParameters = false;
        }
    }
};
