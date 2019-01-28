var KPIMetadataFilter = {
    currentSearchKey: "",

    setCurrentSearchKey: function (_currentSearchKey) {
        KPIMetadataFilter.currentSearchKey = _currentSearchKey;
        KPIMetadataPager.currentPage = 0;
    }
}