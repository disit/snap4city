var KPIMetadataPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
        KPIMetadataPager.currentPage = _currentPage;
    },

    setCurrentSize: function (_currentSize) {
        KPIMetadataPager.currentSize = _currentSize;
        KPIMetadataPager.currentPage = 0;
    }


}