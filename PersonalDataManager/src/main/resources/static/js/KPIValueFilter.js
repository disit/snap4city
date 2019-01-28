var KPIValueFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: function (_currentSearchKey) {
        KPIValueFilter.currentSearchKey = _currentSearchKey;
        KPIValuePager.currentPage = 0;
    }
}