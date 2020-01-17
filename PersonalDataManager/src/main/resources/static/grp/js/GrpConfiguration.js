var GrpConfiguration = {
    checkHostname: function () {      
        console.log("Trying to connect to :");
    console.log(location.protocol + "//" + location.hostname + (location.port != "" ? ":" + location.port : "") + location.pathname  + "/../../api/configuration/v1/?sourceRequest=" + GrpQueryManager.sourceRequest);
        $.ajax({
            method: "GET",
            url: encodeURI(location.protocol + "//" + location.hostname + (location.port != "" ? ":" + location.port : "") + location.pathname  + "/../../api/configuration/v1/?sourceRequest=" +GrpQueryManager.sourceRequest),
            async: false,
            success: function (_response) {
                APIClient.url = location.protocol + "//" + location.hostname + (location.port != "" ? ":" + location.port : "") + location.pathname  + "/../../api/v1/";
                Authentication.url = _response["Authentication.url"];
                Authentication.clientId = _response["grp.Authentication.clientId"];
            },
            error: function (_error) {
                console.log("Configuration Not Found");
            }
        });
    }
}