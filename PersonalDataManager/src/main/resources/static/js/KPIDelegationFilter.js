var KPIDelegationFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: function (_currentSearchKey) {
        KPIDelegationFilter.currentSearchKey = _currentSearchKey;
        KPIDelegationPager.currentPage = 0;
    }
}