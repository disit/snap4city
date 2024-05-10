var GrpDelegationFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: async function (_currentSearchKey) {
        GrpDelegationFilter.currentSearchKey = _currentSearchKey;
        GrpDelegationPager.currentPage = 0;
    }
}