'''
Snap4city -- Costmodel general --
   Copyright (C) 2020 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
'''
package main

import (
	"database/sql"
	"encoding/json"
	"flag"
	_ "github.com/go-sql-driver/mysql"
	"os"
	"time"
)

// get the total number of users
func getTotalUsers(conf map[string]string) int64 {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/profiledb")

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
func getTotalUsersWithMetrics(day string, conf map[string]string) int64 {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

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
func getTotalDashboardsWithMetrics(day string, conf map[string]string) int64 {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

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
func getTotalIoTAppsWithMetrics(day string, conf map[string]string) int64 {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

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
func getTotalIoTTxWithMetrics(day string, conf map[string]string) float64 {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

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
func getTotalIoTRxWithMetrics(day string, conf map[string]string) float64 {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

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
func getTotalDashboardsAccessesWithMetrics(day string, conf map[string]string) int64 {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

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
func getTotalDashboardsMinutesWithMetrics(day string, conf map[string]string) float64 {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

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
func insertMetrics(day string, conf map[string]string) {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// calculate metrics
	totalUsers := getTotalUsers(conf)
	totalUsersWithMetrics := getTotalUsersWithMetrics(day, conf)
	totalDashboardsWithMetrics := getTotalDashboardsWithMetrics(day, conf)
	totalIoTAppsWithMetrics := getTotalIoTAppsWithMetrics(day, conf)
	totalIoTTxWithMetrics := getTotalIoTTxWithMetrics(day, conf)
	totalIoTRxWithMetrics := getTotalIoTRxWithMetrics(day, conf)
	totalDashboardsAccessesWithMetrics := getTotalDashboardsAccessesWithMetrics(day, conf)
	totalDashboardsMinutesWithMetrics := getTotalDashboardsMinutesWithMetrics(day, conf)

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

	// get s flag command line parameter, default ""
	s := flag.String("startDate", "", "Start date from which perform indexing (excluded), YYYY-mm-dd")

	// get s flag command line parameter, default ""
	e := flag.String("endDate", "", "End date from which perform indexing (included), YYYY-mm-dd")

	// Settings map
	conf := map[string]string{}
	// Default settings
	// MySQL Dashboard
	conf["MySQL_hostname"] = "localhost"
	conf["MySQL_username"] = "user"
	conf["MySQL_password"] = "password"
	conf["MySQL_port"] = "3306"
	conf["MySQL_database"] = "iot"

	// Custom settings
	// get conf flag command line parameter
	c := flag.String("conf", "", "Configuration file path (JSON)")
	// parse flag
	//flag.Parse()
	// don't use lowercase letter in struct members' initial letter, otherwise it does not work
	// https://stackoverflow.com/questions/24837432/golang-capitals-in-struct-fields
	type Configuration struct {
		MySQLHostname string
		MySQLUsername string
		MySQLPassword string
		MySQLPort     string
		MySQLDatabase string
	}
	// if a configuration file (JSON) is specified as a command line parameter (-conf), then attempt to read it
	if *c != "" {
		configuration := Configuration{}
		file, err := os.Open(*c)
		defer file.Close()
		decoder := json.NewDecoder(file)
		err = decoder.Decode(&configuration)
		// if configuration file reading is ok, update the settings map
		if err == nil {
			// MySQL
			conf["MySQL_hostname"] = configuration.MySQLHostname
			conf["MySQL_username"] = configuration.MySQLUsername
			conf["MySQL_password"] = configuration.MySQLPassword
			conf["MySQL_port"] = configuration.MySQLPort
			conf["MySQL_database"] = configuration.MySQLDatabase
		}
	}

	// parse flag
	flag.Parse()

	// startDate is now - n day
	startDate := now.AddDate(0, 0, -*n)

	// endDate is now -1 day
	endDate := now.AddDate(0, 0, -1)

	// if startDate parameter is not empty, use it as the start date
	if *s != "" {
		layout := "2006-01-02"
		sDate, err := time.Parse(layout, *s)
		if err == nil {
			startDate = sDate
		}
	}

	// if endDate parameter is not empty, use it as the end date
	if *e != "" {
		layout := "2006-01-02"
		eDate, err := time.Parse(layout, *e)
		if err == nil {
			endDate = eDate
		}
	}

	// for every day in the date range insert data into MySQL
	for rd := rangeDateRecentToOld(endDate, startDate); ; {
		single_date := rd()
		if single_date.IsZero() {
			break
		}
		day := single_date.Format("2006-01-02")
		insertMetrics(day, conf)
	}
}
