var QueryManager = {

    createGetKPIDataTableQuery: function (privacy, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "kpidata/" + privacy + "/?sourceRequest=kpieditor&accessToken=" + accessToken + (pageNumber || pageNumber == 0 ? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIDataByIdQuery: function (id, accessToken) {
        return "kpidata/" + id + "?sourceRequest=kpieditor&accessToken=" + accessToken;
    },

    createSaveKPIDataQuery: function (accessToken) {
        return "kpidata/save?sourceRequest=kpieditor&accessToken=" + accessToken;
    },

    createGetKPIValueTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "kpidata/" + kpiId + "/values?sourceRequest=kpieditor&accessToken=" + accessToken + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIValueByIdQuery: function (id, accessToken) {
        return "kpivalue/" + id + "?sourceRequest=kpieditor&accessToken=" + accessToken;
    },

    createSaveKPIValueQuery: function (accessToken) {
        return "kpivalue/save?sourceRequest=kpieditor&accessToken=" + accessToken;
    },

    createGetKPIMetadataTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "kpidata/" + kpiId + "/metadata?sourceRequest=kpieditor&accessToken=" + accessToken + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIMetadataByIdQuery: function (id, accessToken) {
        return "kpimetadata/" + id + "?sourceRequest=kpieditor&accessToken=" + accessToken;
    },

    createSaveKPIMetadataQuery: function (accessToken) {
        return "kpimetadata/save?sourceRequest=kpieditor&accessToken=" + accessToken;
    },

    createGetKPIDelegationTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "kpidata/" + kpiId + "/delegation?sourceRequest=kpieditor&accessToken=" + accessToken + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIDelegationByIdQuery: function (id, accessToken) {
        return "kpidelegation/" + id + "?sourceRequest=kpieditor&accessToken=" + accessToken;
    },

    createSaveKPIDelegationQuery: function (accessToken) {
        return "kpidelegation/save?sourceRequest=kpieditor&accessToken=" + accessToken;
    }
}