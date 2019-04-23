package main

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	_ "github.com/go-sql-driver/mysql"
	"github.com/tidwall/gjson"
	"gopkg.in/resty.v1"
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
func getKeyCloakToken() string {
	resp, err := resty.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetHeader("Authorization", getBasicAuthForClient("admin-cli", "...")).
		SetFormData(map[string]string{
			"grant_type": "password",
			"username":   "user",
			"password":   "...",
		}).Post("http://localhost:8088/auth/realms/master/protocol/openid-connect/token")
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
func getKeyCloakUsersLogins(token, dateFrom, dateTo string, offset int64) []map[string]interface{} {
	resp, _ := resty.R().
		SetHeader("Content-Type", "application/json").
		SetHeader("Authorization", "Bearer "+token).
		Get("http://localhost:8088/auth/admin/realms/master/events?type=LOGIN&dateFrom=" + dateFrom + "&dateTo=" + dateTo + "&first=" + strconv.FormatInt(offset, 10) + "&max=100")

	var result []map[string]interface{}

	json.Unmarshal(resp.Body(), &result)

	return result
}

// get users' logins map
func getKeyCloakUsersLoginsMap(dateFrom, dateTo string) map[string]map[string]interface{} {
	m := make(map[string]map[string]interface{})
	var offset int64 = 0

	for {
		token := getKeyCloakToken()
		logins := getKeyCloakUsersLogins(token, dateFrom, dateTo, offset)
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
func insertKeyCloakUserLogins(username, userId, date string, numLogins int64) {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

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
func insertKeyCloakUsersLogins(startDate time.Time, endDate time.Time, usersLogins map[string]map[string]interface{}) {
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

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
		usersLogins := getKeyCloakUsersLoginsMap(dateFrom.Format("2006-01-02"), dateTo.Format("2006-01-02"))
		// for each user
		for username, loginData := range usersLogins {
			insertKeyCloakUserLogins(username, loginData["userId"].(string), loginData["date"].(string), loginData["numLogins"].(int64))
		}
	}
}

func main() {
	// now
	now := time.Now()
	// dateFrom is now - 2 day
	dateFrom := now.AddDate(0, 0, -2)
	// dateTo is now -1 1 day
	dateTo := now.AddDate(0, 0, -1)
	usersLogins := getKeyCloakUsersLoginsMap(dateTo.Format("2006-01-02"), dateFrom.Format("2006-01-02"))
	insertKeyCloakUsersLogins(dateFrom, dateTo, usersLogins)
}
