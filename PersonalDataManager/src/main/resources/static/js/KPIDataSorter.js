var KPIDataSorter = {
    currentSortDirection: "asc",
    currentSortBy: "id",

    setSortOnField: function (field) {
        if (KPIDataSorter.currentSortBy == field) {
            if (KPIDataSorter.currentSortDirection == "asc") {
                KPIDataSorter.currentSortDirection = "desc";
            } else if (KPIDataSorter.currentSortDirection == "desc") {
                KPIDataSorter.currentSortDirection = "asc";
            }
        } else {
            KPIDataSorter.currentSortBy = field;
            KPIDataSorter.currentSortDirection = "asc";
        }
        KPIDataPager.setCurrentPage(0);
    }
}