var GrpDelegationFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: function (_currentSearchKey) {
        GrpDelegationFilter.currentSearchKey = _currentSearchKey;
        GrpDelegationPager.currentPage = 0;
    }
}