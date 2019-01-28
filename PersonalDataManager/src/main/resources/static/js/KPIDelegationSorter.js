var KPIDelegationSorter = {
    currentSortDirection: "asc",
    currentSortBy: "id",

    setSortOnField: function (field) {
        if (KPIDelegationSorter.currentSortBy == field) {
            if (KPIDelegationSorter.currentSortDirection == "asc") {
                KPIDelegationSorter.currentSortDirection = "desc";
            } else if (KPIDelegationSorter.currentSortDirection == "desc") {
                KPIDelegationSorter.currentSortDirection = "asc";
            }
        } else {
            KPIDelegationSorter.currentSortBy = field;
            KPIDelegationSorter.currentSortDirection = "asc";
        }
        KPIDelegationPager.setCurrentPage(0);
    }
}