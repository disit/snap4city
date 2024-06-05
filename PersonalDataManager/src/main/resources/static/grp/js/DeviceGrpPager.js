var DeviceGrpPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
        //
        //var load = $('#loading_changing_page').html();
        console.log('changing current page');
        $('#modal_loading_page').modal('show');
        //
        DeviceGrpPager.currentPage = _currentPage;
        
    },

    setCurrentSize: function (_currentSize) {
        DeviceGrpPager.currentSize = _currentSize;
        DeviceGrpPager.currentPage = 0;
    }


}