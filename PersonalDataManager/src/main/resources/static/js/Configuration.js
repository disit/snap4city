var Configuration = {

    checkHostname: function () {
        if (location.hostname == "www.snap4city.org") {
            APIClient.url = "https://www.snap4city.org/mypersonaldata/api/v1/";
            Authentication.url = "https://www.snap4city.org/auth/";
            Authentication.clientId = "js-kpi-client";
            Authentication.clientSecret = "b6195413-2a9f-4a88-a4d1-4bde372693fd";
        } else {
            if (location.hostname == "localhost") {
                APIClient.url = "http://localhost:8080/datamanager/api/v1/";
            } else {
                APIClient.url = "http://192.168.0.47:8081/test/datamanager/api/v1/";
            }
            Authentication.url = "https://www.disit.org/auth/";
            Authentication.clientId = "js-kpi-client-test";
            Authentication.clientSecret = "1999512a-0145-411a-9498-c6c3ad32b559"
        }
    }
}