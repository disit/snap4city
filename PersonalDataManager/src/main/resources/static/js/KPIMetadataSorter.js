var KPIMetadataSorter = {
    currentSortDirection: "asc",
    currentSortBy: "id",

    setSortOnField: function (field) {
        if (KPIMetadataSorter.currentSortBy == field) {
            if (KPIMetadataSorter.currentSortDirection == "asc") {
                KPIMetadataSorter.currentSortDirection = "desc";
            } else if (KPIMetadataSorter.currentSortDirection == "desc") {
                KPIMetadataSorter.currentSortDirection = "asc";
            }
        } else {
            KPIMetadataSorter.currentSortBy = field;
            KPIMetadataSorter.currentSortDirection = "asc";
        }
        KPIMetadataPager.setCurrentPage(0);
    }
}