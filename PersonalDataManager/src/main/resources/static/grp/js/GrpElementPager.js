var GrpElementPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
        GrpElementPager.currentPage = _currentPage;
    },

    setCurrentSize: function (_currentSize) {
        GrpElementPager.currentSize = _currentSize;
        GrpElementPager.currentPage = 0;
    }


}