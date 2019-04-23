var MustacheFunctions = {
    
    timestampToDate: function () {
        return function (text, render) {
            var value = render(text);
            return Utility.timestampToFormatDate(value);
        }
    }
}