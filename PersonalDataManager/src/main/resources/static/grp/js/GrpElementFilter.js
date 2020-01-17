var GrpElementFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: function (_currentSearchKey) {
        GrpElementFilter.currentSearchKey = _currentSearchKey;
        GrpElementPager.currentPage = 0;
    }
}