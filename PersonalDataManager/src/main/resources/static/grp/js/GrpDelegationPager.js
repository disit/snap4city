var GrpDelegationPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: async function (_currentPage) {
        GrpDelegationPager.currentPage = _currentPage;
    },

    setCurrentSize: async function (_currentSize) {
        GrpDelegationPager.currentSize = _currentSize;
        GrpDelegationPager.currentPage = 0;
    }


}