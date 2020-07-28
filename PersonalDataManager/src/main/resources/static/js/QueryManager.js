var QueryManager = {

    sourceRequest: "kpieditor",

    createGetKPIDataTableQuery: function (privacy, pageNumber, pageSize, sortDirection, sortBy, searchKey) {
    	if (privacy != ""){
    		return "kpidata/" + privacy + "/?sourceRequest=" + QueryManager.sourceRequest  + (pageNumber || pageNumber == 0 ? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    	} else {
    		return "kpidata/?sourceRequest=" + QueryManager.sourceRequest  + (pageNumber || pageNumber == 0 ? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    	}
    },

    createGetKPIDataByIdQuery: function (id) {
        return "kpidata/" + id + "?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createPostKPIDataQuery: function () {
        return "kpidata/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createPatchKPIDataQuery: function (id) {
        return "kpidata/"+ id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createDeleteKPIDataQuery: function (id) {
        return "kpidata/"+ id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createGetKPIValueTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey) {
        return "kpidata/" + kpiId + "/values/?sourceRequest=" + QueryManager.sourceRequest  + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIValueByIdQuery: function (kpiId, id) {
        return "kpidata/" + kpiId + "/values/" + id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createPostKPIValueQuery: function (kpiId) {
        return "kpidata/" + kpiId + "/values/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createPatchKPIValueQuery: function (kpiId, id) {
        return "kpidata/" + kpiId + "/values/" + id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createDeleteKPIValueQuery: function (kpiId, id) {
        return "kpidata/" + kpiId + "/values/" + id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createGetKPIMetadataTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey) {
        return "kpidata/" + kpiId + "/metadata/?sourceRequest=" + QueryManager.sourceRequest  + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIMetadataByIdQuery: function (kpiId, id) {
        return "kpidata/" + kpiId + "/metadata/" + id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createPostKPIMetadataQuery: function (kpiId) {
        return "kpidata/" + kpiId + "/metadata/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createPatchKPIMetadataQuery: function (kpiId, id) {
        return "kpidata/" + kpiId + "/metadata/" + id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createDeleteKPIMetadataQuery: function (kpiId, id) {
        return "kpidata/" + kpiId + "/metadata/" + id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createGetKPIDelegationTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey) {
        return "kpidata/" + kpiId + "/delegations/?sourceRequest=" + QueryManager.sourceRequest  + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },

    createGetKPIDelegationByIdQuery: function (kpiId, id) {
        return "kpidata/" + kpiId + "/delegations/" + id + "?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createPostKPIDelegationQuery: function (kpiId) {
        return "kpidata/" + kpiId + "/delegations/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createPatchKPIDelegationQuery: function (kpiId, id) {
        return "kpidata/" + kpiId + "/delegations/" + id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createDeleteKPIDelegationQuery: function (kpiId, id) {
        return "kpidata/" + kpiId + "/delegations/" + id + "/?sourceRequest=" + QueryManager.sourceRequest ;
    },

    createAddMyPOIQuery: function(highLevelType, lang, searchKey){
        return "poidata?sourceRequest=" + QueryManager.sourceRequest + (highLevelType ? "&highLevelType=" + highLevelType : "") ;
    },

    createGetMyPOIQuery: function(idPOI, fromDate, toDate){
        return "poidata/"+ idPOI + "/?sourceRequest=" + QueryManager.sourceRequest  + (fromDate ? "&from=" + fromDate : "") + (toDate ? "&to=" + toDate : "");
    },

    createGetUsernameOrganizationQuery: function(){
        return "username/organization/?sourceRequest=" + QueryManager.sourceRequest ;
    }

}