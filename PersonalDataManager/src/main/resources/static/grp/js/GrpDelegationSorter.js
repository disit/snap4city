var GrpDelegationSorter = {
    currentSortDirection: "desc",
    currentSortBy: "id",

    setSortOnField: function (field) {
        if (GrpDelegationSorter.currentSortBy == field) {
            if (GrpDelegationSorter.currentSortDirection == "asc") {
                GrpDelegationSorter.currentSortDirection = "desc";
            } else if (GrpDelegationSorter.currentSortDirection == "desc") {
                GrpDelegationSorter.currentSortDirection = "asc";
            }
        } else {
            GrpDelegationSorter.currentSortBy = field;
            GrpDelegationSorter.currentSortDirection = "asc";
        }
        GrpDelegationPager.setCurrentPage(0);
    }
}