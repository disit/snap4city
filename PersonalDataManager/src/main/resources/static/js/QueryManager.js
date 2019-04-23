var QueryManager = {

    sourceRequest: "kpieditor",

    createGetKPIDataTableQuery: function (privacy, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "kpidata/" + privacy + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken + (pageNumber || pageNumber == 0 ? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIDataByIdQuery: function (id, accessToken) {
        return "kpidata/" + id + "?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPostKPIDataQuery: function (accessToken) {
        return "kpidata/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPatchKPIDataQuery: function (accessToken, id) {
        return "kpidata/"+ id + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createDeleteKPIDataQuery: function (accessToken, id) {
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
    },

    createAddMyPOIQuery: function(accessToken, highLevelType, lang, searchKey){
        return "poidata?sourceRequest=" + QueryManager.sourceRequest + (highLevelType ? "&highLevelType=" + highLevelType : "") + "&accessToken=" + accessToken;
    },

    createGetMyPOIQuery: function(idPOI, accessToken, fromDate, toDate){
        return "poidata/"+ idPOI + "/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken + (fromDate ? "&from=" + fromDate : "") + (toDate ? "&to=" + toDate : "");
    },

    createGetUsernameOrganizationQuery: function(accessToken){
        return "username/organization/?sourceRequest=" + QueryManager.sourceRequest + "&accessToken=" + accessToken;
    }
}