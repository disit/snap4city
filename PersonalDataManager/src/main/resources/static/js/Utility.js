var Utility = {

	timestampToFormatDate : function(timestamp) {
		if (timestamp != null && timestamp != "" && typeof timestamp != "undefined" && !isNaN(timestamp) ) {
			let date = new Date(parseInt(timestamp));
			return date.toLocaleString();
			// return date.getFullYear() + "-" + (date.getMonth() > 9 ?
			// (date.getMonth() + 1) : "0" + (date.getMonth() + 1)) + "-" +
			// (date.getDate() > 9 ? date.getDate() : "0" + date.getDate()) +
			// "T" + (date.getHours() > 9 ? date.getHours() : "0" +
			// date.getHours()) + ":" + (date.getMinutes() > 9 ?
			// date.getMinutes() : "0" + date.getMinutes());
		} else if (timestamp != "" && typeof timestamp != "undefined" && !isNaN(Utility.parseISOString(timestamp).getTime())) {
			let date = new Date(parseInt(Utility.parseISOString(timestamp).getTime()));
			return date.toLocaleString();
		} else {
			return "";
		}
	},

	parseISOString : function(s) {
		var b = s.split(/\D+/);
		return new Date(Date.UTC(b[0], --b[1], b[2], b[3], b[4], b[5], b[6]));
	}

}