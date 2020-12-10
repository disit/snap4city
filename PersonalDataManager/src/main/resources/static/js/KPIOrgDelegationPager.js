var KPIOrgDelegationPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
    	KPIOrgDelegationPager.currentPage = _currentPage;
    },

    setCurrentSize: function (_currentSize) {
    	KPIOrgDelegationPager.currentSize = _currentSize;
    	KPIOrgDelegationPager.currentPage = 0;
    }


}