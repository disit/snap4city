var GrpDelegationPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
        GrpDelegationPager.currentPage = _currentPage;
    },

    setCurrentSize: function (_currentSize) {
        GrpDelegationPager.currentSize = _currentSize;
        GrpDelegationPager.currentPage = 0;
    }


}