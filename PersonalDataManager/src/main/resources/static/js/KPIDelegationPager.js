var KPIDelegationPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
        KPIDelegationPager.currentPage = _currentPage;
    },

    setCurrentSize: function (_currentSize) {
        KPIDelegationPager.currentSize = _currentSize;
        KPIDelegationPager.currentPage = 0;
    }


}