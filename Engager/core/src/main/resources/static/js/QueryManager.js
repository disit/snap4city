var QueryManager = {

    sourceRequest: "engagereditor",

    createGetEngagementTableQuery: function (pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "engagements?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken + (pageNumber || pageNumber == 0 ? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },
    
    createDeleteEngagementQuery: function (accessToken, id) {
        return "engagements/"+ id + "?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    
    createGetEngagementEnabledQuery: function(jwt){
    	return "username/"+jwt.tokenParsed.preferred_username+"/data?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + jwt.token+"&motivation=ASSISTANCE_ENABLED";
    },
    
    createPostEngagementEnabledQuery: function(jwt){
    	return "username/"+jwt.tokenParsed.preferred_username+"/data?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + jwt.token;
    },
    
    createDeleteAllEngagementQuery: function (accessToken) {
        return "engagements?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
        
    createSurvey1ResponseTableQuery:function(questionNumber, organization, accessToken){
    	return "survey1?question="+questionNumber+"&sourceRequest=" + QueryManager.sourceRequest +"&organization="+organization + "&accessToken=" + accessToken.token;
    }
    
    /*
    
    
    
    
    
    createGetKPIDataByIdQuery: function (id, accessToken) {
        return "kpidata/" + id + "?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPostKPIDataQuery: function (accessToken) {
        return "kpidata/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPatchKPIDataQuery: function (accessToken, id) {
        return "kpidata/"+ id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },



    createGetKPIValueTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "kpidata/" + kpiId + "/values/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIValueByIdQuery: function (accessToken, kpiId, id) {
        return "kpidata/" + kpiId + "/values/" + id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPostKPIValueQuery: function (accessToken, kpiId) {
        return "kpidata/" + kpiId + "/values/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPatchKPIValueQuery: function (accessToken, kpiId, id) {
        return "kpidata/" + kpiId + "/values/" + id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createDeleteKPIValueQuery: function (accessToken, kpiId, id) {
        return "kpidata/" + kpiId + "/values/" + id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createGetKPIMetadataTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "kpidata/" + kpiId + "/metadata/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIMetadataByIdQuery: function (accessToken, kpiId, id) {
        return "kpidata/" + kpiId + "/metadata/" + id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPostKPIMetadataQuery: function (accessToken, kpiId) {
        return "kpidata/" + kpiId + "/metadata/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPatchKPIMetadataQuery: function (accessToken, kpiId, id) {
        return "kpidata/" + kpiId + "/metadata/" + id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createDeleteKPIMetadataQuery: function (accessToken, kpiId, id) {
        return "kpidata/" + kpiId + "/metadata/" + id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createGetKPIDelegationTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "kpidata/" + kpiId + "/delegations/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIDelegationByIdQuery: function (accessToken, kpiId, id) {
        return "kpidata/" + kpiId + "/delegations/" + id + "?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPostKPIDelegationQuery: function (accessToken, kpiId) {
        return "kpidata/" + kpiId + "/delegations/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPatchKPIDelegationQuery: function (accessToken, kpiId, id) {
        return "kpidata/" + kpiId + "/delegations/" + id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createDeleteKPIDelegationQuery: function (accessToken, kpiId, id) {
        return "kpidata/" + kpiId + "/delegations/" + id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },*/
}