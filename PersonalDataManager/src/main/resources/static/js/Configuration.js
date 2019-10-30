var Configuration = {

    checkHostname: function () {
        if (location.hostname == "www.snap4city.org") {
            APIClient.url = "https://www.snap4city.org/mypersonaldata/api/v1/";
            Authentication.url = "https://www.snap4city.org/auth/";
            Authentication.clientId = "js-kpi-client";
        } else {
            if (location.hostname == "dashboard" && location.port=="8080") {
				//this is the appliance VM
                APIClient.url = "http://"+location.hostname+":8080/datamanager/api/v1/";
                Authentication.url = "http://dashboard:8088/auth/";
                Authentication.clientId = "js-kpi-client";
            } else if (location.hostname == "dashboard") {
				//this is the docker with proxy
                APIClient.url = "http://dashboard/datamanager/api/v1/";
                Authentication.url = "http://dashboard/auth/";
                Authentication.clientId = "js-kpi-client";
            } else if (location.hostname == "personaldata") {
				//this is the docker version without proxy
                APIClient.url = "http://"+location.hostname+":8080/datamanager/api/v1/";
                Authentication.url = "http://keycloak:8088/auth";
                Authentication.clientId = "js-kpi-client";
            } else {
				//this is development and test
                if (location.hostname == "localhost") {
                    APIClient.url = "http://"+location.hostname+":8080/datamanager/api/v1/";
                } else {
                    APIClient.url = "http://192.168.0.47:8081/test/datamanager/api/v1/";
                }
                Authentication.url = "https://www.disit.org/auth/";
                Authentication.clientId = "js-kpi-client-test";
            }
        }
    }
}