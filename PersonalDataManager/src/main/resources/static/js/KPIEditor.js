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
                console.log(authenticated);
                if (authenticated) {
                    KPIDataTabler.renderTable();
                    console.log("AUTHENTICATED");
                } else {
                    $("#loginForm").show();
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
                    KPIDataTabler.renderTable();
                };
            } else {
                alert("Insert Password");
            }
        } else {
            alert("Insert Username");
        }



    }
}