var Configuration = {
    checkHostname: function () {
        return new Promise(function(resolve, reject) {
            $.ajax({
                method: "GET",
                url: encodeURI(location.protocol + "//" + location.hostname + (location.port != "" ? ":" + location.port : "") + location.pathname  + "api/configuration/v1/?sourceRequest=" + QueryManager.sourceRequest),
                success: function (_response) {
                    APIClient.url = location.protocol + "//" + location.hostname + (location.port != "" ? ":" + location.port : "") + location.pathname  + "api/v1/";
                    Authentication.url = _response["Authentication.url"];
                    Authentication.clientId = _response["kpi.Authentication.clientId"];
                    EditModalManager.dictionaryUrl = _response["Dictionary.url"];
                    EditModalManager.organizationList = JSON.parse(_response["organization.list"]); 
                    EditModalManager.orgInfoUrl = _response["orgInfo.url"];                  
                    Utility.elasticMasterHost = _response["elasticsearch.hosts"];
                    Utility.kibanaDashboardUrl = _response["kibana.dashboardUrl"];
                    Utility.ldapBasicDn = _response["ldap.basicdn"];
                    resolve(_response); // Risolve la promise con la risposta
                },
                error: function (_error) {
                    console.error("Errore durante il recupero della configurazione:", _error);
                    reject(_error); // Rigetta la promise con l'errore
                }
            });
        });
    }
};
