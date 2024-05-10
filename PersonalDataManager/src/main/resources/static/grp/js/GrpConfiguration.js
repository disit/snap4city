var GrpConfiguration = {
    checkHostname: async function () {      
        console.log("Trying to connect to :");
        console.log(location.protocol + "//" + location.hostname + (location.port != "" ? ":" + location.port : "") + location.pathname  + "/../../api/configuration/v1/?sourceRequest=" + GrpQueryManager.sourceRequest);
        
        try {
            const response = await $.ajax({
                method: "GET",
                url: encodeURI(location.protocol + "//" + location.hostname + (location.port != "" ? ":" + location.port : "") + location.pathname  + "/../../api/configuration/v1/?sourceRequest=" + GrpQueryManager.sourceRequest),
            });
            
            // Codice da eseguire in caso di successo
            APIClient.url = location.protocol + "//" + location.hostname + (location.port != "" ? ":" + location.port : "") + location.pathname  + "/../../api/v1/";
            Authentication.url = response["Authentication.url"];
            Authentication.clientId = response["grp.Authentication.clientId"];
        } catch (error) {
            // Codice da eseguire in caso di errore
            console.log("Configuration Not Found");
        }
    }
};
