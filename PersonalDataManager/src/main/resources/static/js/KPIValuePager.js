var KPIValuePager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
        KPIValuePager.currentPage = _currentPage;
    },

    setCurrentSize: function (_currentSize) {
        KPIValuePager.currentSize = _currentSize;
        KPIValuePager.currentPage = 0;
    }


}