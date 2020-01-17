var DeviceGrpSorter = {
    currentSortDirection: "desc",
    currentSortBy: "id",

    setSortOnField: function (field) {
        if (DeviceGrpSorter.currentSortBy == field) {
            if (DeviceGrpSorter.currentSortDirection == "asc") {
                DeviceGrpSorter.currentSortDirection = "desc";
            } else if (DeviceGrpSorter.currentSortDirection == "desc") {
                DeviceGrpSorter.currentSortDirection = "asc";
            }
        } else {
            DeviceGrpSorter.currentSortBy = field;
            DeviceGrpSorter.currentSortDirection = "asc";
        }
        DeviceGrpPager.setCurrentPage(0);
    }
}