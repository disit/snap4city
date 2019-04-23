var APIClient = {

    executeGetQuery: function (query, successCallback, errorCallback) {
        $.ajax({
            method: "GET",
            url: encodeURI(APIClient.url + query),
            async: false,
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

    executePostQuery: function (_query, _jsonData, successCallback, errorCallback) {
        $.ajax({
            method: "POST",
            url: encodeURI(APIClient.url + _query),
            async: false,
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

    executePutQuery: function (_query, _jsonData, successCallback, errorCallback) {
        $.ajax({
            method: "PUT",
            url: encodeURI(APIClient.url + _query),
            async: false,
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

    executePatchQuery: function (_query, _jsonData, successCallback, errorCallback) {
        $.ajax({
            method: "PATCH",
            url: encodeURI(APIClient.url + _query),
            async: false,
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

    executeDeleteQuery: function (query, successCallback, errorCallback) {
        $.ajax({
            method: "DELETE",
            url: encodeURI(APIClient.url + query),
            async: false,
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
    
    executeGetQueryMPD: function (query, successCallback, errorCallback) {
        $.ajax({
            method: "GET",
            url: encodeURI(APIClient.urlMPD + query),
            async: false,
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
    
    executePostQueryMPD: function (_query, _jsonData, successCallback, errorCallback) {
        $.ajax({
            method: "POST",
            url: encodeURI(APIClient.urlMPD + _query),
            async: false,
            dataType: "json",
            contentType: "application/json",
            data: JSON.stringify(_jsonData),
            success: function (_response) {
                if (successCallback != null) {
                    successCallback(new Array(_response));
                }
            },
            error: function (_error) {
                if (errorCallback != null) {
                    errorCallback(_error);
                }
            }
        });
    },
}