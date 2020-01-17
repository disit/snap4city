var GrpElementSorter = {
    currentSortDirection: "desc",
    currentSortBy: "id",

    setSortOnField: function (field) {
        if (GrpElementSorter.currentSortBy == field) {
            if (GrpElementSorter.currentSortDirection == "asc") {
                GrpElementSorter.currentSortDirection = "desc";
            } else if (GrpElementSorter.currentSortDirection == "desc") {
                GrpElementSorter.currentSortDirection = "asc";
            }
        } else {
            GrpElementSorter.currentSortBy = field;
            GrpElementSorter.currentSortDirection = "asc";
        }
        GrpElementPager.setCurrentPage(0);
    }
}