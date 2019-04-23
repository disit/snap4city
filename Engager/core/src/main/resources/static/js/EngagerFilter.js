var EngagerFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: function (_currentSearchKey) {
        EngagerFilter.currentSearchKey = _currentSearchKey;
        EngagerPager.currentPage = 0;
    }
}