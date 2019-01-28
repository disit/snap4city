var KPIDataFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: function (_currentSearchKey) {
        KPIDataFilter.currentSearchKey = _currentSearchKey;
        KPIDataPager.currentPage = 0;
    }
}