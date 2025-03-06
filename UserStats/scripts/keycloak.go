'''
Snap4city -- Keycloak logger --
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
	"encoding/base64"
	"encoding/json"
	"flag"
	_ "github.com/go-sql-driver/mysql"
	"github.com/tidwall/gjson"
	"gopkg.in/resty.v1"
	"os"
	"strconv"
	"time"
)

// get the value of the JSON for a key
func val_j(key, json_str string) string {
	return gjson.Get(json_str, key).String()
}

// check if a key exists in a JSON
func key_j(key, json_str string) bool {
	return gjson.Get(json_str, key).Exists()
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

// Function to build the HttpBasicAuth Base64 String
func getBasicAuthForClient(clientId string, clientSecret string) string {
	var httpBasicAuth string
	if len(clientId) > 0 && len(clientSecret) > 0 {
		httpBasicAuth = base64.URLEncoding.EncodeToString([]byte(clientId + ":" + clientSecret))
	}

	return "Basic " + httpBasicAuth
}

// get KeyCloak's token
func getKeyCloakToken(conf map[string]string) string {
	resp, err := resty.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetHeader("Authorization", getBasicAuthForClient("admin-cli", "r7jKG5Pn")).
		SetFormData(map[string]string{
			"grant_type": conf["KeyCloakApiGrantType"],
			"username":   conf["KeyCloakApiUsername"],
			"password":   conf["KeyCloakApiPassword"],
		}).Post(conf["KeyCloakApiUrl"] + "/auth/realms/master/protocol/openid-connect/token")
	if err != nil {
		return ""
	}

	// get JSON result
	var result map[string]interface{}
	if err := json.Unmarshal(resp.Body(), &result); err != nil {
		return ""
	}

	return result["access_token"].(string)
}

// get users' logins
func getKeyCloakUsersLogins(token, dateFrom, dateTo string, offset int64, conf map[string]string) []map[string]interface{} {
	resp, _ := resty.R().
		SetHeader("Content-Type", "application/json").
		SetHeader("Authorization", "Bearer "+token).
		Get(conf["KeyCloakApiUrl"] + "/auth/admin/realms/master/events?type=LOGIN&dateFrom=" + dateFrom + "&dateTo=" + dateTo + "&first=" + strconv.FormatInt(offset, 10) + "&max=100")

	var result []map[string]interface{}

	json.Unmarshal(resp.Body(), &result)

	return result
}

// get users' logins map
func getKeyCloakUsersLoginsMap(dateFrom, dateTo string, conf map[string]string) map[string]map[string]interface{} {
	m := make(map[string]map[string]interface{})
	var offset int64 = 0

	for {
		token := getKeyCloakToken(conf)
		logins := getKeyCloakUsersLogins(token, dateFrom, dateTo, offset, conf)
		if len(logins) == 0 {
			break
		}
		for _, login := range logins {
			userId := login["userId"]
			details := login["details"].(map[string]interface{})
			username := details["username"].(string)
			tm := time.Unix(int64(login["time"].(float64)/1000), 0)
			date := tm.Format("2006-01-02")
			var numLogins int64
			if m[username]["numLogins"] == nil {
				numLogins = 1
			} else {
				numLogins = m[username]["numLogins"].(int64) + 1
			}
			m[username] = map[string]interface{}{"username": username, "userId": userId, "numLogins": numLogins, "date": date}
		}
		offset += 1000
	}
	return m
}

// insert KeyCloak user's # logins into MySQL
func insertKeyCloakUserLogins(username, userId, date string, numLogins int64, conf map[string]string) {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	stmt, err := db.Prepare("INSERT IGNORE INTO keycloak_logins (username, userId, num_login, date) " +
		"VALUES (?, ?, ?, ?) " +
		"ON DUPLICATE KEY UPDATE num_login = ?")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
	// don't consider 'rootfilter', 'rootmetric' and 'idpadmin' users
	if username != "rootfilter" &&
		username != "rootmetric" &&
		username != "idpadmin" {

		_, err = stmt.Exec(username,
			userId,
			numLogins,
			date,
			numLogins)
	}
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// insert KeyCloak users's # logins into MySQL
func insertKeyCloakUsersLogins(startDate time.Time, endDate time.Time, usersLogins map[string]map[string]interface{}, conf map[string]string) {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// for every day in the date range insert events into MySQL
	for rd := rangeDateRecentToOld(endDate, startDate); ; {
		dateFrom := rd()
		if dateFrom.IsZero() {
			break
		}
		// dateTo is dateFrom + 1 day
		dateTo := dateFrom.AddDate(0, 0, 1)
		//dateTo = dateTo.strftime("%Y-%m-%d")
		//dateFrom = dateFrom.strftime("%Y-%m-%d")
		// get users' logins map
		usersLogins := getKeyCloakUsersLoginsMap(dateFrom.Format("2006-01-02"), dateTo.Format("2006-01-02"), conf)
		// for each user
		for username, loginData := range usersLogins {
			insertKeyCloakUserLogins(username, loginData["userId"].(string), loginData["date"].(string), loginData["numLogins"].(int64), conf)
		}
	}
}

func main() {
	// Settings map
	conf := map[string]string{}
	// Default settings
	// MySQL Dashboard
	conf["MySQL_hostname"] = "localhost"
	conf["MySQL_username"] = "user"
	conf["MySQL_password"] = "password"
	conf["MySQL_port"] = "3306"
	conf["MySQL_database"] = "iot"
	conf["KeyCloakApiUrl"] = "http://localhost:8088"
	conf["KeyCloakApiUsername"] = "idpadmin"
	conf["KeyCloakApiPassword"] = "password"
	conf["KeyCloakApiGrantType"] = "password"

	// Custom settings
	// get conf flag command line parameter
	c := flag.String("conf", "", "Configuration file path (JSON)")
	// parse flag
	//flag.Parse()
	// don't use lowercase letter in struct members' initial letter, otherwise it does not work
	// https://stackoverflow.com/questions/24837432/golang-capitals-in-struct-fields
	type Configuration struct {
		MySQLHostname        string
		MySQLUsername        string
		MySQLPassword        string
		MySQLPort            string
		MySQLDatabase        string
		KeyCloakApiUrl       string
		KeyCloakApiUsername  string
		KeyCloakApiPassword  string
		KeyCloakApiGrantType string
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
			conf["KeyCloakApiUrl"] = configuration.KeyCloakApiUrl
			conf["KeyCloakApiUsername"] = configuration.KeyCloakApiUsername
			conf["KeyCloakApiPassword"] = configuration.KeyCloakApiPassword
			conf["KeyCloakApiGrantType"] = configuration.KeyCloakApiGrantType
		}
	}

	// parse flag
	flag.Parse()

	// now
	now := time.Now()
	// dateFrom is now - 2 day
	dateFrom := now.AddDate(0, 0, -2)
	// dateTo is now -1 1 day
	dateTo := now.AddDate(0, 0, -1)
	usersLogins := getKeyCloakUsersLoginsMap(dateTo.Format("2006-01-02"), dateFrom.Format("2006-01-02"), conf)
	insertKeyCloakUsersLogins(dateFrom, dateTo, usersLogins, conf)
}
