var KPIOrgDelegationSorter = {
    currentSortDirection: "desc",
    currentSortBy: "id",

    setSortOnField: function (field) {
        if (KPIOrgDelegationSorter.currentSortBy == field) {
            if (KPIOrgDelegationSorter.currentSortDirection == "asc") {
            	KPIOrgDelegationSorter.currentSortDirection = "desc";
            } else if (KPIOrgDelegationSorter.currentSortDirection == "desc") {
            	KPIOrgDelegationSorter.currentSortDirection = "asc";
            }
        } else {
        	KPIOrgDelegationSorter.currentSortBy = field;
        	KPIOrgDelegationSorter.currentSortDirection = "asc";
        }
        KPIOrgDelegationPager.setCurrentPage(0);
    }
}