var EngagerSorter = {
    currentSortDirection: "desc",
    currentSortBy: "id",

    setSortOnField: function (field) {
        if (EngagerSorter.currentSortBy == field) {
            if (EngagerSorter.currentSortDirection == "asc") {
            	EngagerSorter.currentSortDirection = "desc";
            } else if (EngagerSorter.currentSortDirection == "desc") {
            	EngagerSorter.currentSortDirection = "asc";
            }
        } else {
        	EngagerSorter.currentSortBy = field;
        	EngagerSorter.currentSortDirection = "asc";
        }
        EngagerPager.setCurrentPage(0);
    }
}