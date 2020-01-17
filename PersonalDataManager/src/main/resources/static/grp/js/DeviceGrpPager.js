var DeviceGrpPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
        DeviceGrpPager.currentPage = _currentPage;
    },

    setCurrentSize: function (_currentSize) {
        DeviceGrpPager.currentSize = _currentSize;
        DeviceGrpPager.currentPage = 0;
    }


}