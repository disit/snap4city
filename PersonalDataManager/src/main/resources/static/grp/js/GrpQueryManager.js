var GrpQueryManager = {

    sourceRequest: "grpeditor",

    createPostDeviceGrpQuery: function (accessToken) {
        return "devicegroup/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    
    createGetDeviceGrpTableQuery: function (privacy, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        if(privacy != "public") {
            return "devicegroup/" + privacy + "/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken + (pageNumber || pageNumber == 0 ? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
        }
        else {
            return "public/devicegroup/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken + (pageNumber || pageNumber == 0 ? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
        }
    },
    
    createPatchDeviceGrpQuery: function (accessToken, id) {
        return "devicegroup/"+ id + "/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    
    createGetGrpDelegationTableQuery: function (kpiId, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        return "devicegroup/" + kpiId + "/delegations/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },
    
    createGetGrpDelegationByIdQuery: function (accessToken, kpiId, id) {
        return "devicegroup/" + kpiId + "/delegations/" + id + "?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    
    createPostGrpDelegationQuery: function (accessToken, kpiId) {
        return "devicegroup/" + kpiId + "/delegations/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },

    createPatchGrpDelegationQuery: function (accessToken, kpiId, id) {
        return "devicegroup/" + kpiId + "/delegations/" + id + "/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    
    createDeleteGrpDelegationQuery: function (accessToken, kpiId, id) {
        return "devicegroup/" + kpiId + "/delegations/" + id + "/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    
    createGetDeviceGrpByIdQuery: function (id, accessToken) {
        return "devicegroup/" + id + "?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    
    createGetGrpElemsTableQuery: function (grpId, pageNumber, pageSize, sortDirection, sortBy, searchKey, accessToken) {
        console.log(grpId);
        return "devicegroup/" + grpId + "/elements/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken + (pageNumber || pageNumber == 0? "&pageNumber=" + pageNumber : "") + (pageSize ? "&pageSize=" + pageSize : "") + (sortDirection ? "&sortDirection=" + sortDirection : "") + (sortBy ? "&sortBy=" + sortBy : "") + (searchKey ? "&searchKey=" + searchKey : "");
    },
    
    createDeleteDeviceGroupQuery: function (accessToken, id) {
        return "devicegroup/"+ id + "/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    createClearDeviceGroupQuery: function (accessToken, id) {
        return "devicegroup/"+ id + "/elements?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },    
    createGetNewGrpElmtBtnsQuery: function (accessToken,id) {
        return "devicegroup/"+id+"/availElmtTypesToAdd/?sourceRequest=" + GrpQueryManager.sourceRequest+"&accessToken="+accessToken;
    },
    
    createGetAvailItemsQuery: function(accessToken,id,elmtType) {
        return "devicegroup/"+id+"/availElmtToAdd/?sourceRequest=" + GrpQueryManager.sourceRequest+"&accessToken="+accessToken+"&elmtType="+elmtType;
    },
    
    createPostAddElmtToGrp: function (accessToken, id) {
        return "devicegroup/" + id + "/elements/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    
    createDeleteElmtFromGroupQuery: function (accessToken, grpId, id) {
        return "devicegroup/" + grpId + "/elements/" + id + "/?sourceRequest=" + GrpQueryManager.sourceRequest + "&accessToken=" + accessToken;
    },
    
    createGetSensorsAPIQuery: function(accessToken, pageSize = 10, pageNum = 1, search = "") {
        return "sensors/?accessToken="+encodeURIComponent(accessToken)+"&pageSize="+encodeURIComponent(pageSize)+"&pageNum="+encodeURIComponent(pageNum)+"&search="+encodeURIComponent(search);
    }
}