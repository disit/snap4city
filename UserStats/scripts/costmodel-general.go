package main

import (
	"database/sql"
	"flag"
	_ "github.com/go-sql-driver/mysql"
	"time"
)

// get the total number of users
func getTotalUsers() int64 {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/profiledb")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT count(DISTINCT username) AS num FROM profiledb.ownership")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	var num int64 = 0

	for results.Next() {
		// for each row, scan the result into our tag composite object
		err = results.Scan(&num)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
	}
	return num
}

// get the total number of users having metrics for a day
func getTotalUsersWithMetrics(day string) int64 {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT COUNT(DISTINCT username) FROM iot.`data` WHERE date = \"" + day + "\"")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	var num int64

	for results.Next() {
		// for each row, scan the result into our tag composite object
		err = results.Scan(&num)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
	}
	return num
}

// get the total number of users' dashboards for a day
func getTotalDashboardsWithMetrics(day string) int64 {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT COALESCE(SUM(dashboards_public),0)+COALESCE(SUM(dashboards_private),0) FROM iot.`data` WHERE date = \"" + day + "\"")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	var num int64

	for results.Next() {
		// for each row, scan the result into our tag composite object
		err = results.Scan(&num)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
	}
	return num
}

// get the total number of IoT apps for a day
func getTotalIoTAppsWithMetrics(day string) int64 {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT COALESCE(SUM(iot_apps),0) FROM iot.`data` WHERE date = \"" + day + "\"")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	var num int64

	for results.Next() {
		// for each row, scan the result into our tag composite object
		err = results.Scan(&num)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
	}
	return num
}

// get the total number of IoT transmitted kB for a day
func getTotalIoTTxWithMetrics(day string) float64 {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT COALESCE(SUM(iot_tx),0) FROM iot.`data` WHERE date = \"" + day + "\"")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	var num float64

	for results.Next() {
		// for each row, scan the result into our tag composite object
		err = results.Scan(&num)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
	}
	return num
}

// get the total number of IoT received kB for a day
func getTotalIoTRxWithMetrics(day string) float64 {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT COALESCE(SUM(iot_rx),0) FROM iot.`data` WHERE date = \"" + day + "\"")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	var num float64

	for results.Next() {
		// for each row, scan the result into our tag composite object
		err = results.Scan(&num)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
	}
	return num
}

// get the total number of dashboards' accesses for a day
func getTotalDashboardsAccessesWithMetrics(day string) int64 {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT COALESCE(SUM(dashboards_accesses),0) FROM iot.`data` WHERE date = \"" + day + "\"")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	var num int64

	for results.Next() {
		// for each row, scan the result into our tag composite object
		err = results.Scan(&num)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
	}
	return num
}

// get the total number of dashboards' minutes for a day
func getTotalDashboardsMinutesWithMetrics(day string) float64 {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT COALESCE(SUM(dashboards_minutes),0) FROM iot.`data` WHERE date = \"" + day + "\"")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	var num float64

	for results.Next() {
		// for each row, scan the result into our tag composite object
		err = results.Scan(&num)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
	}
	return num
}

// insert general metrics for a day
func insertMetrics(day string) {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// calculate metrics
	totalUsers := getTotalUsers()
	totalUsersWithMetrics := getTotalUsersWithMetrics(day)
	totalDashboardsWithMetrics := getTotalDashboardsWithMetrics(day)
	totalIoTAppsWithMetrics := getTotalIoTAppsWithMetrics(day)
	totalIoTTxWithMetrics := getTotalIoTTxWithMetrics(day)
	totalIoTRxWithMetrics := getTotalIoTRxWithMetrics(day)
	totalDashboardsAccessesWithMetrics := getTotalDashboardsAccessesWithMetrics(day)
	totalDashboardsMinutesWithMetrics := getTotalDashboardsMinutesWithMetrics(day)

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO data_general (totalUsers, totalUsersWithMetrics, totalDashboardsWithMetrics, " +
			"totalIoTAppsWithMetrics, totalIoTTxWithMetrics, totalIoTRxWithMetrics, " +
			"totalDashboardsAccessesWithMetrics, totalDashboardsMinutesWithMetrics, date)" +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
			"ON DUPLICATE KEY UPDATE totalUsers = ?, totalUsersWithMetrics = ?, " +
			"totalDashboardsWithMetrics = ?, totalIoTAppsWithMetrics = ?, " +
			"totalIoTTxWithMetrics = ?, totalIoTRxWithMetrics = ?, " +
			"totalDashboardsAccessesWithMetrics = ?, totalDashboardsMinutesWithMetrics = ?")
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	_, err = stmt.Exec(totalUsers,
		totalUsersWithMetrics,
		totalDashboardsWithMetrics,
		totalIoTAppsWithMetrics,
		totalIoTTxWithMetrics,
		totalIoTRxWithMetrics,
		totalDashboardsAccessesWithMetrics,
		totalDashboardsMinutesWithMetrics,
		day,
		totalUsers,
		totalUsersWithMetrics,
		totalDashboardsWithMetrics,
		totalIoTAppsWithMetrics,
		totalIoTTxWithMetrics,
		totalIoTRxWithMetrics,
		totalDashboardsAccessesWithMetrics,
		totalDashboardsMinutesWithMetrics,
	)
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// rangeDate returns a date range function over start date to end date inclusive.
// After the end of the range, the range function returns a zero date,
// date.IsZero() is true.
// this function ranges from the most recent date to the less (with the less excluded)
func rangeDateRecentToOld(start, end time.Time) func() time.Time {
	y, m, d := start.Date()
	start = time.Date(y, m, d, 0, 0, 0, 0, time.UTC)
	y, m, d = end.Date()
	end = time.Date(y, m, d, 0, 0, 0, 0, time.UTC)

	return func() time.Time {
		// uncomment the following if you want to range from the most to
		// the less recent date, including the less
		//if start.Before(end) {
		if start.Equal(end) {
			return time.Time{}
		}
		date := start
		start = start.AddDate(0, 0, -1)
		return date
	}
}

// rangeDate returns a date range function over start date to end date inclusive.
// After the end of the range, the range function returns a zero date,
// date.IsZero() is true.
// this function ranges from the less recent date to the most (with the most excluded)
func rangeDateOldToRecent(start, end time.Time) func() time.Time {
	y, m, d := start.Date()
	start = time.Date(y, m, d, 0, 0, 0, 0, time.UTC)
	y, m, d = end.Date()
	end = time.Date(y, m, d, 0, 0, 0, 0, time.UTC)

	return func() time.Time {
		// uncomment the following if you want to range from the less to
		// the most recent date, including the most
		//if start.After(end) {
		if start.Equal(end) {
			return time.Time{}
		}
		date := start
		start = start.AddDate(0, 0, 1)
		return date
	}
}

func main() {
	// now
	now := time.Now()

	// get n flag command line parameter, default 2
	n := flag.Int("days", 2, "Number of days in the past to start indexing from")

	// parse flag
	flag.Parse()

	// startDate is now - n day
	startDate := now.AddDate(0, 0, -*n)
	// endDate is now -1 day
	endDate := now.AddDate(0, 0, -1)

	// for every day in the date range insert data into MySQL
	for rd := rangeDateRecentToOld(endDate, startDate); ; {
		single_date := rd()
		if single_date.IsZero() {
			break
		}
		day := single_date.Format("2006-01-02")
		insertMetrics(day)
	}
}
