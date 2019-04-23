var EngagerPager = {
    currentPage: 0,
    currentSize: 10,

    setCurrentPage: function (_currentPage) {
    	EngagerPager.currentPage = _currentPage;
    },

    setCurrentSize: function (_currentSize) {
    	EngagerPager.currentSize = _currentSize;
    	EngagerPager.currentPage = 0;
    }
}