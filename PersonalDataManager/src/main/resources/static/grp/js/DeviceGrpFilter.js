var DeviceGrpFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: function (_currentSearchKey) {
        DeviceGrpFilter.currentSearchKey = _currentSearchKey;
        DeviceGrpPager.currentPage = 0;
    }
}