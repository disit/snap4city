var KPIValueSorter = {
    currentSortDirection: "desc",
    currentSortBy: "dataTime",

    setSortOnField: async function (field) {
        if (KPIValueSorter.currentSortBy == field) {
            if (KPIValueSorter.currentSortDirection == "asc") {
                KPIValueSorter.currentSortDirection = "desc";
            } else if (KPIValueSorter.currentSortDirection == "desc") {
                KPIValueSorter.currentSortDirection = "asc";
            }
        } else {
            KPIValueSorter.currentSortBy = field;
            KPIValueSorter.currentSortDirection = "asc";
        }
        KPIValuePager.setCurrentPage(0);
    }
}