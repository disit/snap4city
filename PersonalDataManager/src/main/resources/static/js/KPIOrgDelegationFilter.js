var KPIOrgDelegationFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: function (_currentSearchKey) {
    	KPIOrgDelegationFilter.currentSearchKey = _currentSearchKey;
        KPIOrgDelegationPager.currentPage = 0;
    }
}