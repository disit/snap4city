var Authentication = {
    expiresTimestamp: null,
    refreshToken: null,
    username: null,
    password: null,

    refreshTokenGetAccessToken: function () {
        return new Promise(function(resolve, reject) {
            var params;
            if (Authentication.refreshToken == null || Authentication.expiresTimestamp == null || new Date().getTime() > Authentication.expiresTimestamp) {
                params = "client_id=" + Authentication.clientId + "&client_secret=" + Authentication.clientSecret + "&grant_type=password&username=" + Authentication.username + "&password=" + Authentication.password;
            } else {
                params = "client_id=" + Authentication.clientId + "&client_secret=" + Authentication.clientSecret + "&grant_type=refresh_token&scope=openid profile&refresh_token=" + Authentication.refreshToken;
            }
            
            $.ajax({
                method: "POST",
                url: encodeURI(Authentication.url + "/realms/master/protocol/openid-connect/token/"),
                data: params,
                success: function (response) {
                    if (response != "") {
                        if (response.refresh_token != null) {
                            Authentication.refreshToken = response.refresh_token;
                        }
                        if (response.refresh_expires_in != null) {
                            Authentication.expiresTimestamp = (new Date().getTime()) + response.refresh_expires_in * 1000;
                        }
                        if (response.access_token != null) {
                            resolve(response.access_token);
                        }
                    }
                },
                error: function (error) {
                    console.log(error);
                    reject(error);
                }
            });
        });
    }
};