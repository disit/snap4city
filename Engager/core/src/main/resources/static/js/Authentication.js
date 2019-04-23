var Authentication = {

    expiresTimestamp: null,
    refreshToken: null,
    username: null,
    password: null,

    refreshTokenGetAccessToken: function () {
        if (Authentication.refreshToken == null || Authentication.expiresTimestamp == null || new Date().getTime() > Authentication.expiresTimestamp) {
            params = "client_id=" + Authentication.clientId + "&client_secret=" + Authentication.clientSecret + "&grant_type=password&username=" + Authentication.username + "&password=" + Authentication.password;
        } else {
            params = "client_id=" + Authentication.clientId + "&client_secret=" + Authentication.clientSecret + "&grant_type=refresh_token&scope=openid profile&refresh_token=" + Authentication.refreshToken;
        }
        var result = "";
        $.ajax({
            method: "POST",
            url: encodeURI(Authentication.url + "/realms/master/protocol/openid-connect/token/"),
            data: params,
            async: false,
            success: function (_response) {
                response = _response;
                if (response != "") {
                    if (response.refresh_token != null) {
                        Authentication.refreshToken = response.refresh_token;
                    }
                    if (response.refresh_expires_in != null) {
                        Authentication.expiresTimestamp = (new Date().getTime()) + response.refresh_expires_in * 1000;
                    }
                    if (response.access_token != null) {
                        result = response.access_token;
                    }
                }

            },
            error: function (_error) {
                console.log(_error);
                result = _error;
            }
        });
        return result;
    }

}