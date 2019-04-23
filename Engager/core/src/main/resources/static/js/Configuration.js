var Configuration = {
    checkHostname: function () {
        if (location.hostname == "www.snap4city.org") {
            APIClient.url = "https://www.snap4city.org/engager/api/v1/";
            Authentication.url = "https://www.snap4city.org/auth/";
            Authentication.clientId = "js-engager-client";
            APIClient.urlMPD="https://www.snap4city.org/mypersonaldata/api/v1/";            
        } else {
            if (location.hostname == "localhost") {
                APIClient.url = "http://localhost:8082/engager-core/api/v1/";
            } else {
                APIClient.url = "http://192.168.0.47:8081/test/engager-core/api/v1/";
            }
            Authentication.url = "https://www.disit.org/auth";
            Authentication.clientId = "js-engager-client-test";
            APIClient.urlMPD="http://192.168.0.47:8081/test/datamanager/api/v1/";
        }
        console.log(APIClient.url);
        console.log(Authentication);
    }
}