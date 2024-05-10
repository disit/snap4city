var APIClient = {
    suffix: "",

    executeGetQuery: async function (query, successCallback, errorCallback) {
        $.ajax({
            method: "GET",
            url: encodeURI(APIClient.url + APIClient.suffix + query),
            async: true,
            success: function (_response) {
                if (successCallback != null) {
                    successCallback(_response);
                }
            },
            error: function (_error) {
                if (errorCallback != null) {
                    errorCallback(_error);
                }
            }
        });
    },
    
    executeAsyncGetQuery: async function (query, successCallback, errorCallback) {        
        $.ajax({
            method: "GET",
            url: encodeURI(APIClient.url + APIClient.suffix + query),
            async: true,
            success: function (_response) {
                if (successCallback != null) {
                    successCallback(_response);
                }
            },
            error: function (_error) {
                if (errorCallback != null) {
                    errorCallback(_error);
                }
            }
        });
    },

    executePostQuery: async function (_query, _jsonData, successCallback, errorCallback) {
        $.ajax({
            method: "POST",
            url: encodeURI(APIClient.url + _query),
            async: true,
            dataType: "json",
            contentType: "application/json",
            data: JSON.stringify(_jsonData),
            success: function (_response) {
                if (successCallback != null) {
                    successCallback(_response);
                }
            },
            error: function (_error) {
                if (errorCallback != null) {
                    errorCallback(_error);
                }
            }
        });
    },

    executePutQuery: async function (_query, _jsonData, successCallback, errorCallback) {
        $.ajax({
            method: "PUT",
            url: encodeURI(APIClient.url + _query),
            async: true,
            dataType: "json",
            contentType: "application/json",
            data: JSON.stringify(_jsonData),
            success: function (_response) {
                if (successCallback != null) {
                    successCallback(_response);
                }
            },
            error: function (_error) {
                if (errorCallback != null) {
                    errorCallback(_error);
                }
            }
        });
    },

    executePatchQuery: async function (_query, _jsonData, successCallback, errorCallback) {
        $.ajax({
            method: "PATCH",
            url: encodeURI(APIClient.url + _query),
            async: true,
            dataType: "json",
            contentType: "application/json",
            data: JSON.stringify(_jsonData),
            success: function (_response) {
                if (successCallback != null) {
                    successCallback(_response);
                }
            },
            error: function (_error) {
                if (errorCallback != null) {
                    errorCallback(_error);
                }
            }
        });
    },

    executeDeleteQuery: async function (query, successCallback, errorCallback) {
        $.ajax({
            method: "DELETE",
            url: encodeURI(APIClient.url + query),
            async: true,
            success: function (_response) {
                if (successCallback != null) {
                    successCallback(_response);
                }
            },
            error: function (_error) {
                if (errorCallback != null) {
                    errorCallback(_error);
                }
            }
        });
    }
}