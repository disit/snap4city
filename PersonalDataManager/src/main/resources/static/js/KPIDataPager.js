var KPIDataPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
        KPIDataPager.currentPage = _currentPage;
    },

    setCurrentSize: function (_currentSize) {
        KPIDataPager.currentSize = _currentSize;
        KPIDataPager.currentPage = 0;
    }


}