// https://play.golang.org/p/xbVxASrffo
// https://gobyexample.com/string-formatting
// https://golang.org/pkg/net/http/
// https://github.com/tidwall/gjson
// https://mholt.github.io/curl-to-go <- curl to Go
// https://mholt.github.io/json-to-go/ <- JSON to Go
// https://stackoverflow.com/questions/24822826/cant-i-get-rid-of-fmt-prefix-when-calling-println-in-golang
// compile statically with CGO_ENABLED=0 go build -ldflags "-w -s" -a costmodel.go

package main

import (
	"database/sql"
	"encoding/json"
	"flag"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"github.com/tidwall/gjson"
	"gopkg.in/resty.v1"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

// get index's size
func getIndexSize(url string) int64 {
	body := strings.NewReader(`{"query": { "match": { "agent": "Node-Red" } } }`)
	req, err := http.NewRequest("POST", url+"/_search?pretty", body)
	if err != nil {
		// handle err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		// handle err
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	return gjson.Get(string(b), "hits.total").Int()
}

// get the number of users for a day
func getDailyUsers(date string, conf map[string]string) int {
	d := strings.Split(date, "T")
	toDate := d[0]
	toDate = toDate + "T23:59:59.999Z"
	// get url's result
	resp, err := resty.R().
		Get("http://" + conf["SolrUrl"] + ":8983/solr/syslog/select?facet.field=pid_local&facet=on&indent=on&q=date_time:[%22" +
			date + "%22%20TO%20%22" +
			toDate + "%22]%20AND%20agent:%22Node-Red%22&rows=0&sort=date_time%20desc&start=0&wt=json")
	if err != nil {
		return 0
	}
	// get JSON result
	return len(gjson.Get(string(resp.Body()[:]), "facet_counts.facet_fields.pid_local").Array())
}

// get the Node-RED traffic (TX/RX) for a day
func getNodeREDTraffic(startDate, endDate, com_mode string, conf map[string]string) float64 {
	body := strings.NewReader(`{
  "query":"SELECT SUM(payload) FROM syslog3 WHERE com_mode = '` + com_mode + `' AND date_time >= '` + startDate + `' AND date_time <= '` + endDate + `' LIMIT 1"
}`)

	req, err := http.NewRequest("POST", "http://"+conf["ElasticSearchIP"]+":9200/_xpack/sql", body)
	if err != nil {
		// handle err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		// handle err
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "rows").Value()
	a := n.([]interface{})
	return a[0].([]interface{})[0].(float64)
}

// get the number of Node-RED for a day
func getNodeREDNumber(startDate, endDate string, conf map[string]string) int64 {
	body := strings.NewReader(`{
  "query":"SELECT pid_local FROM syslog3 WHERE agent = 'Node-Red' AND date_time > '` + startDate + `' AND date_time < '` + endDate + `'"
}`)

	req, err := http.NewRequest("POST", "http://"+conf["ElasticSearchIP"]+":9200/_xpack/sql", body)
	if err != nil {
		// handle err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		// handle err
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "rows").Value()
	a := n.([]interface{})
	result := []string{}
	for _, c := range a {
		if !stringInSlice(c.([]interface{})[0].(string), result) {
			result = append(result, c.([]interface{})[0].(string))
		}
	}

	return (int64)(len(result))
}

// get the number of ETL processed per day
func getETLProcessesPerDay(startDate, endDate string, conf map[string]string) int64 {
	body := strings.NewReader(`{
  "query":"SELECT COUNT(*) FROM syslog3 WHERE agent = 'ETL' AND com_mode = 'RX' AND date_time > '` + startDate + `' AND date_time < '` + endDate + `'"
}`)

	req, err := http.NewRequest("POST", "http://"+conf["ElasticSearchIP"]+":9200/_xpack/sql", body)
	if err != nil {
		// handle err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		// handle err
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	//n := gjson.Get(string(b), "rows").Int()
	n := gjson.Get(string(b), "rows").Value()
	a := n.([]interface{})
	return (int64)(a[0].([]interface{})[0].(float64))
}

// get ETL traffic (TX/RX) per day
func getETLTrafficPerDay(com_mode, startDate, endDate string, conf map[string]string) float64 {
	body := strings.NewReader(`{
  "query":"SELECT SUM(payload) FROM syslog3 WHERE agent = 'ETL' AND com_mode = '` + com_mode + `' AND date_time > '` + startDate + `' AND date_time < '` + endDate + `'"
}`)

	req, err := http.NewRequest("POST", "http://"+conf["ElasticSearchIP"]+":9200/_xpack/sql", body)
	if err != nil {
		// handle err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		// handle err
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	//n := gjson.Get(string(b), "rows").Int()
	n := gjson.Get(string(b), "rows").Value()
	a := n.([]interface{})
	return a[0].([]interface{})[0].(float64)
}

// get the number of R Studio usage per day
func getRStudioPerDay(startDate, endDate string, conf map[string]string) int64 {
	body := strings.NewReader(`{
  "query":"SELECT COUNT(*) FROM syslog3 WHERE motivation = 'RStatistics' AND date_time > '` + startDate + `' AND date_time < '` + endDate + `'"
}`)

	req, err := http.NewRequest("POST", "http://"+conf["ElasticSearchIP"]+":9200/_xpack/sql", body)
	if err != nil {
		// handle err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		// handle err
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "rows").Value()
	a := n.([]interface{})
	return (int64)(a[0].([]interface{})[0].(float64))
}

// get the IoT/ETL writes dictionary per day (username => writes)
func getIoTETLWritesPerDay(iotid_username map[string]string, day string, conf map[string]string) (map[string]float64, map[string]float64) {
	iot_writes := map[string]float64{}
	etl_writes := map[string]float64{}
	serviceUrls_etlwrites := map[string]float64{}

	// start of the day
	start := day + "T00:00:00.000Z"

	// end of the day
	end := day + "T23:59:59.999Z"

	body := strings.NewReader(`{
  "query":"SELECT COUNT(*) AS NUM, serviceUri FROM sensorinew3 WHERE date_time > '` + start + `' AND date_time < '` + end + `' GROUP BY serviceUri"
}`)

	req, err := http.NewRequest("POST", "http://"+conf["ElasticSearchIP"]+":9200/_xpack/sql", body)
	if err != nil {
		// handle err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		// handle err
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "rows").Value()
	a := n.([]interface{})
	for _, v := range a {
		c := v.([]interface{})[0]
		s := v.([]interface{})[1]
		serviceUrls_etlwrites[s.(string)] = c.(float64)
	}

	// for each serviceUrl in the serviceUrls_etlwrites map
	for serviceUrl, _ := range serviceUrls_etlwrites {
		// if this serviceUrl is an IoT
		//if strings.Contains(serviceUrl, "km4city/resource/iot") {
		if strings.Contains(serviceUrl, "/resource/iot") {
			if _, ok := iotid_username[serviceUrl]; ok {
				if _, ok := iot_writes[iotid_username[serviceUrl]]; ok {
					iot_writes[iotid_username[serviceUrl]] += serviceUrls_etlwrites[serviceUrl]
				} else {
					iot_writes[iotid_username[serviceUrl]] = serviceUrls_etlwrites[serviceUrl]
				}
			}
		} else { // else if this serviceUrl is an ETL (user "disit")
			if _, ok := etl_writes["disit"]; ok {
				etl_writes["disit"] += serviceUrls_etlwrites[serviceUrl]
			} else {
				etl_writes["disit"] = serviceUrls_etlwrites[serviceUrl]
			}
		}
	}
	return iot_writes, etl_writes
}

// get IoT => username dictionary from MySQL
func getIoTUsername(conf map[string]string) map[string]string {
	result := map[string]string{}

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/"+conf["MySQL_profiledb_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT distinct username, elementurl FROM profiledb.ownership WHERE elementtype = \"IOTID\" AND elementurl != \"\"")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	var username string
	var elementurl string

	for results.Next() {
		// for each row, scan the result into our tag composite object
		err = results.Scan(&username, &elementurl)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
		result[elementurl] = username
	}
	return result
}

// get the timestamp from date
func timestamp(date string) int64 {
	layout := "2006-01-02 15:04:05"
	t, _ := time.Parse(layout, date)
	/*if err != nil {
		fmt.Println(err)
	}*/
	return t.Unix()
}

// check if a string is present in a slice
func stringInSlice(a string, list []string) bool {
	for _, b := range list {
		if b == a {
			return true
		}
	}
	return false
}

// get the value of the JSON for a key
func val_j(key, json_str string) string {
	return gjson.Get(json_str, key).String()
}

// check if a key exists in a JSON
func key_j(key, json_str string) bool {
	return gjson.Get(json_str, key).Exists()
}

// IfThenElse evaluates a condition, if true returns the first parameter otherwise the second
func IfThenElse(condition bool, a interface{}, b interface{}) interface{} {
	if condition {
		return a
	}
	return b
}

// get dashboard's daily accesses and minutes (Gianni's API), date = YYYY-mm-dd
func getDashboardDailyAccessesAndMinutes(idDash string, date string, conf map[string]string) (string, string) {
	resp, _ := resty.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetFormData(map[string]string{
			"idDash": idDash,
			"date":   date,
		}).Post(conf["DashboardsDailyAccessesApiUrl"])
	// get JSON result
	return val_j("nAccessesPerDay", string(resp.Body()[:])), val_j("nMinutesPerDay", string(resp.Body()[:]))
}

// get dashboards' accesses and minutes (Gianni's API), date = YYYY-mm-dd
func getDashboardsDailyAccessesAndMinutes(date string, conf map[string]string) gjson.Result {
	resp, _ := resty.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetFormData(map[string]string{
			"date": date,
		}).Post(conf["DashboardsDailyAccessesApiUrl"])
	/*if err != nil {
		return ""
	}*/
	return gjson.Parse(string(resp.Body()[:]))
}

// get elementId => username dictionary from MySQL
func getElementIdUsername(conf map[string]string) map[string]string {
	result := map[string]string{}

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/"+conf["MySQL_profiledb_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT distinct elementId, username FROM profiledb.ownership WHERE elementtype = \"DashboardID\"")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	type Tag struct {
		elementId string
		username  string
	}

	for results.Next() {
		var tag Tag
		// for each row, scan the result into our tag composite object
		err = results.Scan(&tag.elementId, &tag.username)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
		result[tag.elementId] = tag.username
	}
	return result
}

// get users' dashboard accesses and minutes
func getUsersDashboardsAccessesAndMinutes(elementIdUsername map[string]string, dashboardDailyAccessesAndMinutes gjson.Result) map[string]map[string]int64 {
	// user's dashboard data
	users := map[string]map[string]int64{}

	for elementId, _ := range elementIdUsername {
		var nAccessesPerDay, nMinutesPerDay int64 = 0, 0
		// iterate dashboard daily accesses and minutes JSON
		dashboardDailyAccessesAndMinutes.ForEach(func(key, value gjson.Result) bool {
			j := gjson.GetMany(value.String(), "idDashboard", "nAccessesPerDay", "nMinutesPerDay")
			// if idDashboard == elementId
			if j[0].String() == elementId {
				nAccessesPerDay += j[1].Int()
				nMinutesPerDay += j[2].Int()
			}
			return true // keep iterating
		})

		username := elementIdUsername[elementId]

		// if username not in users then initialize map
		if _, ok := users[username]; !ok {
			users[username] = make(map[string]int64)
		}
		// populate user's data
		if _, ok := users[username]["nAccessesPerDay"]; ok {
			users[username]["nAccessesPerDay"] += nAccessesPerDay
		} else {
			users[username]["nAccessesPerDay"] = nAccessesPerDay
		}
		if _, ok := users[username]["nMinutesPerDay"]; ok {
			users[username]["nMinutesPerDay"] += nMinutesPerDay
		} else {
			users[username]["nMinutesPerDay"] = nMinutesPerDay
		}
	}
	return users
}

// get the access token
func getAccessToken(conf map[string]string) string {
	resp, err := resty.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetFormData(map[string]string{
			"grant_type": conf["AccessTokenGrantType"],
			"client_id":  conf["AccessTokenClientID"],
			"username":   conf["AccessTokenUsername"],
			"password":   conf["AccessTokenPassword"],
		}).Post(conf["AccessTokenUrl"])
	if err != nil {
		return ""
	}
	// get JSON result
	return val_j("access_token", string(resp.Body()[:]))
}

// get the ownerships (Piero's API)
func getOwnershipsJSON(conf map[string]string) gjson.Result {
	// Assign Client Redirect Policy. Create one as per you need
	resty.SetRedirectPolicy(resty.FlexibleRedirectPolicy(15))
	resp, _ := resty.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetFormData(map[string]string{
			"grant_type": conf["OwnershipApiGrantType"],
			"username":   conf["OwnershipApiUsername"],
			"password":   conf["OwnershipApiPassword"],
		}).Get("http://" + conf["OwnershipApiUrl"] + "/ownership-api/v1/list" + "?accessToken=" + getAccessToken(conf) + "&includeDeleted")
	/*if err != nil {
		return ""
	}*/
	// get JSON result
	return gjson.Parse(string(resp.Body()[:]))
}

// get delegations (Angelo's API)
func getDelegationsJSON(conf map[string]string) gjson.Result {
	resp, _ := resty.R().
		SetQueryParams(map[string]string{"accessToken": getAccessToken(conf),
			"sourceRequest": "metricscollector",
			"deleted":       "true"}).
		Get(conf["DataManagerApiUrl"] + "/api/v1/username/ANONYMOUS/delegated")
	/*if err != nil {
		return ""
	}*/
	// get JSON result
	return gjson.Parse(string(resp.Body()[:]))
}

// get the apps/dashboards/devices ownerships and number of apps per username
func getOwnerships(ownerships gjson.Result, day string, conf map[string]string) (map[string]string, map[string]int64, map[string]string) {
	// map of appid => owner's username
	appid_username := map[string]string{}

	// map username => number of active apps at this day
	username_apps := map[string]int64{}

	// map iotid => owner's username
	iotid_username := getIoTUsername(conf)

	// calculate this day's timestamp
	start_timestamp := timestamp(day + " 00:00:00")

	// for each ownership
	ownerships.ForEach(func(key, value gjson.Result) bool {
		val_s := value.String()
		if ok := key_j("elementType", val_s) && key_j("elementId", val_s) && key_j("username", val_s); ok {
			start_timestamp = timestamp(day + " 00:00:00")
			// if the app is active at this day and has not been deleted, increment the counter
			if ok := key_j("created", val_s) && val_j("created", val_s) != "null" && (!key_j("deleted", val_s) || val_j("deleted", val_s) == ""); ok {
				// consider the creation date at the beginning of the day (00:00:00), since day is YYYY-mm-dd 00:00:00
				creation_date := strings.Split(val_j("created", val_s), " ")
				creation_date_v := timestamp(creation_date[0] + " 00:00:00")
				if start_timestamp >= creation_date_v {
					// if this is an app
					if val_j("elementType", val_s) == "AppID" {
						// add this user's app id to the dictionary
						appid_username[val_j("elementId", val_s)] = val_j("username", val_s)
						// increment the number of user's apps
						if _, ok := username_apps[val_j("username", val_s)]; ok {
							username_apps[val_j("username", val_s)] = username_apps[val_j("username", val_s)] + 1
						} else {
							username_apps[val_j("username", val_s)] = 1
						}
					}
				}
			} else if ok := key_j("created", val_s) && val_j("created", val_s) != "null"; ok { // if the app is active at this day but has been deleted, increment the counter
				// consider the creation date at the beginning of the day (00:00:00), since day is YYYY-mm-dd 00:00:00
				creation_date := strings.Split(val_j("created", val_s), " ")
				creation_date_v := timestamp(creation_date[0] + " 00:00:00")
				deletion_date := timestamp(val_j("deleted", val_s))
				if start_timestamp >= creation_date_v && start_timestamp <= deletion_date {
					// if this is an app
					if val_j("elementType", val_s) == "AppID" {
						// add this user's app id to the dictionary
						appid_username[val_j("elementId", val_s)] = val_j("username", val_s)
						if ok := key_j("username", val_s); ok {
							username_apps[val_j("username", val_s)] = username_apps[val_j("username", val_s)] + 1
						} else {
							username_apps[val_j("username", val_s)] = 1
						}
					}
				}
			}
		}
		return true // keep iterating ownerships
	})
	return appid_username, username_apps, iotid_username
}

// get the number of active elements per user of this day YYYY-mm-dd
// username => number
// and the dictionary of users and their dashboards ids (elementId)
// username => [ids]
func getElementsPerUser(elementType string, ownerships gjson.Result, day string) (map[string]int64, map[string][]string) {
	result := map[string]int64{}
	// map of usernames and their element ids
	username_ids := map[string][]string{}
	start_timestamp := timestamp(day + " 00:00:00")
	// for each ownership
	ownerships.ForEach(func(key, value gjson.Result) bool {
		val_s := value.String()
		// consider the insert time (dashboard's creation date) at the beginning of the day (00:00:00), since day is YYYY-mm-dd 00:00:00
		if ok := key_j("created", val_s) && val_j("created", val_s) != "null"; ok {
			created := strings.Split(val_j("created", val_s), " ")
			created_v := timestamp(created[0] + " 00:00:00")

			if ok := key_j("elementType", val_s) &&
				val_j("elementType", val_s) == elementType &&
				(!key_j("deleted", val_s) ||
					val_j("deleted", val_s) == "") &&
				start_timestamp >= created_v; ok {
				if _, ok := result[val_j("username", val_s)]; ok {
					result[val_j("username", val_s)] = result[val_j("username", val_s)] + 1
				} else {
					result[val_j("username", val_s)] = 1
				}
				username_ids[val_j("username", val_s)] = append(username_ids[val_j("username", val_s)], val_j("elementId", val_s))
			} else if ok := key_j("elementType", val_s) &&
				val_j("elementType", val_s) == elementType &&
				key_j("usernameDelegator", val_s) &&
				val_j("deleted", val_s) != "" &&
				start_timestamp >= created_v &&
				start_timestamp <= timestamp(val_j("deleted", val_s)); ok {
				if _, ok := result[val_j("username", val_s)]; ok {
					result[val_j("username", val_s)] = result[val_j("username", val_s)] + 1
				} else {
					result[val_j("username", val_s)] = 1
				}
				username_ids[val_j("username", val_s)] = append(username_ids[val_j("username", val_s)], val_j("elementId", val_s))
			}
		}
		return true // keep iterating ownerships
	})
	return result, username_ids
}

// get the number of public elements per user of this day YYYY-mm-dd
// username => number
func getPublicElementsPerUser(elementType string, delegations gjson.Result, username_ids map[string][]string, day string) map[string]int64 {
	result := map[string]int64{}
	start_timestamp := timestamp(day+" 00:00:00") * 1000
	var insertTime_v int64
	// for each delegation
	delegations.ForEach(func(key, value gjson.Result) bool {
		val_s := value.String()
		// consider the insert time (dashboard's creation date) at the beginning of the day (00:00:00), since day is YYYY-mm-dd 00:00:00
		if ok := key_j("insertTime", val_s); ok {
			insertTime, _ := strconv.ParseInt(val_j("insertTime", val_s), 10, 64)
			unixTimeUTC := time.Unix(insertTime/1000, 0)
			year, month, day := unixTimeUTC.Date()
			insertTime_v = timestamp(fmt.Sprintf("%d-%02d-%02d 00:00:00", year, month, day)) * 1000
		}
		_, usernameDelegator_in_username_ids := username_ids[val_j("usernameDelegator", val_s)]
		deleteTime, _ := strconv.ParseInt(val_j("deleteTime", val_s), 10, 64)
		if ok := key_j("usernameDelegator", val_s) &&
			usernameDelegator_in_username_ids &&
			key_j("elementId", val_s) &&
			stringInSlice(val_j("elementId", val_s), username_ids[val_j("usernameDelegator", val_s)]) &&
			key_j("elementType", val_s) &&
			val_j("elementType", val_s) == elementType &&
			key_j("insertTime", val_s) &&
			!key_j("deleteTime", val_s) &&
			start_timestamp >= insertTime_v; ok {
			if ok := usernameDelegator_in_username_ids; ok {
				result[val_j("usernameDelegator", val_s)] += 1
			} else {
				result[val_j("usernameDelegator", val_s)] = 1
			}
		} else if ok := key_j("usernameDelegator", val_s) &&
			usernameDelegator_in_username_ids &&
			key_j("elementId", val_s) &&
			stringInSlice(val_j("elementId", val_s), username_ids[val_j("usernameDelegator", val_s)]) &&
			key_j("elementType", val_s) &&
			val_j("elementType", val_s) == elementType &&
			key_j("insertTime", val_s) &&
			key_j("deleteTime", val_s) &&
			start_timestamp >= insertTime_v &&
			start_timestamp <= deleteTime; ok {
			if ok := result[val_j("usernameDelegator", val_s)] > 0; ok {
				result[val_j("usernameDelegator", val_s)] += 1
			} else {
				result[val_j("usernameDelegator", val_s)] = 1
			}
		}
		return true // keep iterating ownerships
	})
	return result
}

// get the number of private elements per user of this day YYYY-mm-dd
// username => number
func getPrivateElementsPerUser(elements, publicElementsPerUser map[string]int64) map[string]int64 {
	result := map[string]int64{}
	for username, count := range elements {
		if _, ok := publicElementsPerUser[username]; ok {
			result[username] = count - publicElementsPerUser[username]
		} else {
			result[username] = count
		}
	}
	return result
}

// get bytes transmitted/received by agent (ETL, Node-RED) with motivation, start end YYYY-mm-ddT00:00:00Z
func getPayload(pid_local, agent, motivation, com_mode, start, end string, conf map[string]string) float64 {
	body := strings.NewReader(`{
  "query":"SELECT SUM(payload) FROM syslog3 WHERE agent = '` + agent + `' AND com_mode = '` + com_mode + `' AND motivation = '` + motivation + `' AND pid_local = '` + pid_local + `' AND date_time > '` + start + `' AND date_time < '` + end + `'"
}`)

	req, err := http.NewRequest("POST", "http://"+conf["ElasticSearchIP"]+":9200/_xpack/sql", body)
	if err != nil {
		// handle err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		// handle err
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "rows").Value()
	a := n.([]interface{})
	if len(a) > 0 && len(a[0].([]interface{})) > 0 {
		return a[0].([]interface{})[0].(float64)
	} else {
		return 0
	}
}

// get the list of pid_local, start end YYYY-mm-ddT00:00:00Z
func getPidLocalList(agent, start, end string, conf map[string]string) []string {
	body := strings.NewReader(`{
  "query":"SELECT COUNT(*) AS num, pid_local FROM syslog3 WHERE agent = '` + agent + `' AND com_mode = 'TX' AND date_time > '` + start + `' AND date_time < '` + end + `' GROUP BY pid_local"
}`)

	req, err := http.NewRequest("POST", "http://"+conf["ElasticSearchIP"]+":9200/_xpack/sql", body)
	if err != nil {
		// handle err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		// handle err
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "rows").Value()
	a := n.([]interface{})
	pid_local_list := []string{}
	for _, v := range a {
		c := v.([]interface{})[0]
		s := v.([]interface{})[1]
		pid_local_list = append(pid_local_list, s.(string))
		pid_local_list = append(pid_local_list, fmt.Sprintf("%d", (int64)(c.(float64))))
	}
	return pid_local_list
}

// get the list of dashboards with a broker widget
func getDashboardBrokerWidgetDictionary(conf map[string]string) []int64 {
	result := []int64{}

	db, err := sql.Open("mysql", conf["MySQL_Dashboard_username"]+":"+conf["MySQL_Dashboard_password"]+"@tcp("+conf["MySQL_Dashboard_hostname"]+":"+conf["MySQL_Dashboard_port"]+")/"+conf["MySQL_Dashboard_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT id_dashboard FROM Dashboard.Config_widget_dashboard " +
		"WHERE actuatorTarget = 'broker' AND cancelDate IS NULL " +
		"GROUP BY id_dashboard")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	type Tag struct {
		id_dashboard int64
	}

	for results.Next() {
		var tag Tag
		// for each row, scan the result into our tag composite object
		err = results.Scan(&tag.id_dashboard)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
		result = append(result, tag.id_dashboard)
	}
	return result
}

// get the list of dashboards with a Node-RED widget
func getDashboardNodeREDWidgetDictionary(conf map[string]string) []int64 {
	result := []int64{}

	db, err := sql.Open("mysql", conf["MySQL_Dashboard_username"]+":"+conf["MySQL_Dashboard_password"]+"@tcp("+conf["MySQL_Dashboard_hostname"]+":"+conf["MySQL_Dashboard_port"]+")/"+conf["MySQL_Dashboard_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT id_dashboard FROM NodeRedMetrics a JOIN Dashboard.Config_widget_dashboard b " +
		"ON a.name = b.id_metric " +
		"WHERE a.appId IS NOT NULL AND a.appId <> '' " +
		"GROUP BY b.id_dashboard " +
		"UNION DISTINCT " +
		"SELECT id_dashboard FROM NodeRedInputs a JOIN Dashboard.Config_widget_dashboard b " +
		"ON a.name = b.id_metric " +
		"WHERE a.appId IS NOT NULL AND a.appId <> ''" +
		"GROUP BY b.id_dashboard")
	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	type Tag struct {
		id_dashboard int64
	}

	for results.Next() {
		var tag Tag
		// for each row, scan the result into our tag composite object
		err = results.Scan(&tag.id_dashboard)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
		result = append(result, tag.id_dashboard)
	}
	return result
}

// update username => motivation => {tx, rx} dictionaries for agent Node-Red
func updateDictionaries(username_motivation_tx map[string]map[string]float64,
	username_motivation_rx map[string]map[string]float64,
	pid_local string,
	appid_username map[string]string,
	motivation string,
	tx float64,
	rx float64) (map[string]map[string]float64, map[string]map[string]float64) {

	// check if pid_local is in appid_username
	_, pid_local_in_appid_username := appid_username[pid_local]

	username_in_username_motivation_tx := false
	username_in_username_motivation_rx := false

	if pid_local_in_appid_username {
		// check if appid_username[pid_local] is in username_in_username_motivation_tx
		_, username_in_username_motivation_tx = username_motivation_tx[appid_username[pid_local]]
		// check if appid_username[pid_local] is in username_in_username_motivation_rx
		_, username_in_username_motivation_rx = username_motivation_rx[appid_username[pid_local]]
	}

	motivation_in_username_motivation_tx := false
	motivation_in_username_motivation_rx := false

	if username_in_username_motivation_tx {
		// check if motivation is in username_motivation_tx[appid_username[pid_local]]
		_, motivation_in_username_motivation_tx = username_motivation_tx[appid_username[pid_local]][motivation]
	}
	if username_in_username_motivation_rx {
		// check if motivation is in username_motivation_rx[appid_username[pid_local]]
		_, motivation_in_username_motivation_rx = username_motivation_rx[appid_username[pid_local]][motivation]
	}

	if pid_local_in_appid_username {
		// init maps if not set
		if !username_in_username_motivation_tx {
			username_motivation_tx[appid_username[pid_local]] = make(map[string]float64)
		}
		if !motivation_in_username_motivation_tx {
			username_motivation_tx[appid_username[pid_local]][motivation] = 0
		}
		if !username_in_username_motivation_rx {
			username_motivation_rx[appid_username[pid_local]] = make(map[string]float64)
		}
		if !motivation_in_username_motivation_rx {
			username_motivation_rx[appid_username[pid_local]][motivation] = 0
		}

		// increment tx in username_motivation_tx map
		if pid_local_in_appid_username && username_in_username_motivation_tx && motivation_in_username_motivation_tx {
			username_motivation_tx[appid_username[pid_local]][motivation] += tx
		}
		// increment rx in username_motivation_rx map
		if pid_local_in_appid_username && username_in_username_motivation_rx && motivation_in_username_motivation_rx {
			username_motivation_rx[appid_username[pid_local]][motivation] += rx
		}
	}
	return username_motivation_tx, username_motivation_rx
}

// update username => motivation => {tx, rx} dictionaries for agent ETL (user 'disit')
func updateETLDictionaries(motivation_tx, motivation_rx map[string]float64, motivation string, tx, rx float64) {
	// init maps if not set
	_, ok := motivation_tx[motivation]
	if !ok {
		motivation_tx[motivation] = 0
	}
	_, ok = motivation_rx[motivation]
	if !ok {
		motivation_rx[motivation] = 0
	}

	// increment tx in this dictionary
	motivation_tx[motivation] += tx
	// increment rx in this dictionary
	motivation_rx[motivation] += rx
}

// check if a string is a number
func is_number(s string) bool {
	if _, err := strconv.ParseFloat(s, 64); err == nil {
		//fmt.Printf("%q looks like a number.\n", s)
		return true
	} else {
		return false
	}
}

// insert user's app data (Node-RED traffic, # IoT apps, # dashboards, # devices) for a day into MySQL
func insertAppData(username string,
	tx map[string]float64,
	rx map[string]float64,
	iot_apps int64,
	iot_reads int64,
	date string,
	conf map[string]string) {

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// calculate tx/rx sums of all motivations
	tx_sum := 0.0
	rx_sum := 0.0

	for motivation, _ := range tx {
		tx_sum += tx[motivation]
	}
	for motivation, _ := range rx {
		rx_sum += rx[motivation]
	}

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO data (username, iot_db_storage_tx, iot_db_storage_rx, " +
			"iot_filesystem_storage_tx, iot_filesystem_storage_rx, " +
			"iot_db_request_tx, iot_db_request_rx, " +
			"iot_ascapi_tx, iot_ascapi_rx, " +
			"iot_disces_tx, iot_disces_rx, " +
			"iot_dashboard_tx, iot_dashboard_rx, " +
			"iot_datagate_tx, iot_datagate_rx, " +
			"iot_external_service_tx, iot_external_service_rx, " +
			"iot_iot_service_tx, iot_iot_service_rx, " +
			"iot_mapping_tx, iot_mapping_rx, " +
			"iot_microserviceusercreated_tx, iot_microserviceusercreated_rx, " +
			"iot_mydata_tx, iot_mydata_rx, " +
			"iot_notificator_tx, iot_notificator_rx, " +
			"iot_rstatistics_tx, iot_rstatistics_rx, " +
			"iot_sigfox_tx, iot_sigfox_rx, " +
			"iot_undefined_tx, iot_undefined_rx, " +
			"iot_tx, iot_rx, " +
			"iot_apps, iot_reads, date)" +
			"VALUES (?, ?, ?, ?, ?, " +
			"?, ?, ?, ?, " +
			"?, ?, ?, ?, " +
			"?, ?, ?, ?, " +
			"?, ?, ?, ?, " +
			"?, ?, ?, ?, " +
			"?, ?, ?, ?, " +
			"?, ?, ?, ?, " +
			"?, ?, ?, ?, ?) " +
			"ON DUPLICATE KEY UPDATE iot_db_storage_tx = ?, iot_db_storage_rx = ?, " +
			"iot_filesystem_storage_tx = ?, iot_filesystem_storage_rx = ?, " +
			"iot_db_request_tx = ?, iot_db_request_rx = ?, " +
			"iot_ascapi_tx = ?, iot_ascapi_rx = ?, " +
			"iot_disces_tx = ?, iot_disces_rx = ?, " +
			"iot_dashboard_tx = ?, iot_dashboard_rx = ?, " +
			"iot_datagate_tx = ?, iot_datagate_rx = ?, " +
			"iot_external_service_tx = ?, iot_external_service_rx = ?, " +
			"iot_iot_service_tx = ?, iot_iot_service_rx = ?, " +
			"iot_mapping_tx = ?, iot_mapping_rx = ?, " +
			"iot_microserviceusercreated_tx = ?, iot_microserviceusercreated_rx = ?, " +
			"iot_mydata_tx = ?, iot_mydata_rx = ?, " +
			"iot_notificator_tx = ?, iot_notificator_rx = ?, " +
			"iot_rstatistics_tx = ?, iot_rstatistics_rx = ?, " +
			"iot_sigfox_tx = ?, iot_sigfox_rx = ?, " +
			"iot_undefined_tx = ?, iot_undefined_rx = ?, " +
			"iot_tx = ?, iot_rx = ?, " +
			"iot_apps = ?, " +
			"iot_reads = ?")
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	_, err = stmt.Exec(
		username, tx["DB_Storage"],
		rx["DB_Storage"],
		tx["FileSystem_Storage"],
		rx["FileSystem_Storage"],
		tx["DB_request"],
		rx["DB_request"],
		tx["ASCAPI"],
		rx["ASCAPI"],
		tx["DISCES"],
		rx["DISCES"],
		tx["Dashboard"],
		rx["Dashboard"],
		tx["Datagate"],
		rx["Datagate"],
		tx["External_Service"],
		rx["External_Service"],
		tx["IoT_Service"],
		rx["IoT_Service"],
		tx["Mapping"],
		rx["Mapping"],
		tx["MicroserviceUserCreated"],
		rx["MicroserviceUserCreated"],
		tx["MyData"],
		rx["MyData"],
		tx["Notificator"],
		rx["Notificator"],
		tx["RStatistics"],
		rx["RStatistics"],
		tx["SigFox"],
		rx["SigFox"],
		tx["undefined"],
		rx["undefined"],
		tx_sum, rx_sum, iot_apps, iot_reads, date,
		tx["DB_Storage"],
		rx["DB_Storage"],
		tx["FileSystem_Storage"],
		rx["FileSystem_Storage"],
		tx["DB_request"],
		rx["DB_request"],
		tx["ASCAPI"],
		rx["ASCAPI"],
		tx["DISCES"],
		rx["DISCES"],
		tx["Dashboard"],
		rx["Dashboard"],
		tx["Datagate"],
		rx["Datagate"],
		tx["External_Service"],
		rx["External_Service"],
		tx["IoT_Service"],
		rx["IoT_Service"],
		tx["Mapping"],
		rx["Mapping"],
		tx["MicroserviceUserCreated"],
		rx["MicroserviceUserCreated"],
		tx["MyData"],
		rx["MyData"],
		tx["Notificator"],
		rx["Notificator"],
		tx["RStatistics"],
		rx["RStatistics"],
		tx["SigFox"],
		rx["SigFox"],
		tx["undefined"],
		rx["undefined"],
		tx_sum, rx_sum, iot_apps, iot_reads)
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// insert user's dashboard data for a day into MySQL
func insertDashboardData(username string, dashboards_public int64, dashboards_private int64,
	nAccessesPerDay int64, nMinutesPerDay int64, iot_writes float64,
	etl_writes float64, date string, conf map[string]string) {

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO data (username, dashboards_public, dashboards_private, dashboards_accesses, dashboards_minutes, iot_writes, etl_writes, date)" +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE dashboards_public = ?, dashboards_private = ?, dashboards_accesses = ?, dashboards_minutes = ?, iot_writes = ?, etl_writes = ?")
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
	_, err = stmt.Exec(username, dashboards_public, dashboards_private,
		nAccessesPerDay, nMinutesPerDay, iot_writes, etl_writes, date,
		dashboards_public, dashboards_private, nAccessesPerDay,
		nMinutesPerDay, iot_writes, etl_writes)
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// insert dashboards' accesses and minutes into MySQL
func insertDashboardsData(elementId int64, username string, nAccessesPerDay string,
	nMinutesPerDay string, dashboardBrokerWidgetDictionary []int64,
	dashboardNodeREDWidgetDictionary []int64, date string, conf map[string]string) {
	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO dashboards (elementId, username, nAccesses, nMinutes, hasBrokerWidget, hasNodeREDWidget, date)" +
			"VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE nAccesses = ?, nMinutes = ?, hasBrokerWidget = ?, hasNodeREDWidget = ?")

	hasBrokerWidget := "0"
	for _, d := range dashboardBrokerWidgetDictionary {
		if d == elementId {
			hasBrokerWidget = "1"
		}
	}
	hasNodeREDWidget := "0"
	for _, d := range dashboardNodeREDWidgetDictionary {
		if d == elementId {
			hasNodeREDWidget = "1"
		}
	}

	_, err = stmt.Exec(elementId, username, nAccessesPerDay, nMinutesPerDay,
		hasBrokerWidget, hasNodeREDWidget, date, nAccessesPerDay,
		nMinutesPerDay, hasBrokerWidget, hasNodeREDWidget)
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// insert user's device data for a day into MySQL
func insertDeviceData(username string, devices_public int64, devices_private int64,
	iot_writes float64, etl_writes float64, date string, conf map[string]string) {

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO data (username, devices_public, devices_private, iot_writes, etl_writes, date)" +
			"VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE devices_public = ?, devices_private = ?, iot_writes = ?, etl_writes = ?")
	_, err = stmt.Exec(username, devices_public, devices_private, iot_writes,
		etl_writes, date, devices_public, devices_private, iot_writes,
		etl_writes)
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// insert apps data for this user into MySQL
func insertUsersData(users_total int, users_nodered int, users_dashboards int,
	username_devices int, date string, conf map[string]string) {

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO users (nodered, dashboards, devices, total, date)" +
			"VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE nodered = ?, dashboards = ?, devices = ?, total = ?")
	_, err = stmt.Exec(users_nodered, users_dashboards, username_devices, users_total,
		date, users_nodered, users_dashboards, username_devices,
		users_total)
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// get and insert ETL processed per day into MySQL
func indexETL(day string, conf map[string]string) {
	// start of the day
	start := day + "T00:00:00.000Z"

	// end of the day
	end := day + "T23:59:59.999Z"

	// get the number of ETL processes of this day
	num_etls := getETLProcessesPerDay(start, end, conf)

	// get ETL traffic (TX/RX) per day
	tx := getETLTrafficPerDay("TX", start, end, conf)
	rx := getETLTrafficPerDay("RX", start, end, conf)

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO etl (num, tx, rx, date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE num = ?, tx = ?, rx = ?")

	_, err = stmt.Exec(strconv.FormatInt(num_etls, 10),
		strconv.FormatFloat(tx, 'f', -1, 64), strconv.FormatFloat(rx, 'f', -1, 64),
		start, strconv.FormatInt(num_etls, 10), strconv.FormatFloat(tx, 'f', -1, 64),
		strconv.FormatFloat(rx, 'f', -1, 64))
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// get and insert R usages per day into MySQL
func indexR(day string, conf map[string]string) {
	// start of the day
	start := day + "T00:00:00.000Z"

	// end of the day
	end := day + "T23:59:59.999Z"

	// get the number of R usages of this day
	num_r := getRStudioPerDay(start, end, conf)

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO r (num, date) VALUES (?, ?) ON DUPLICATE KEY UPDATE num = ?")

	_, err = stmt.Exec(strconv.FormatInt(num_r, 10), start, strconv.FormatInt(num_r, 10))
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// get and insert user data for a day into MySQL
func indexUserData(ownerships gjson.Result,
	delegations gjson.Result,
	elementIdUsername map[string]string,
	iotid_username map[string]string,
	dashboardBrokerWidgetDictionary []int64,
	dashboardNodeREDWidgetDictionary []int64,
	day string,
	conf map[string]string) {
	// start of the day
	start := day + "T00:00:00.000Z"

	// end of the day
	end := day + "T23:59:59.999Z"

	// get ownerships dictionaries
	appid_username, username_apps, iotid_username := getOwnerships(ownerships, day, conf)

	// get the IoT/ETL writes dictionary per day (username => writes)
	iot_writes, etl_writes := getIoTETLWritesPerDay(iotid_username, day, conf)

	// get the number of active dashboards per user of this day YYYY-mm-dd
	// and the dictionary of users and their dashboards ids (elementId)
	dashboards, username_dashboardsids := getElementsPerUser("DashboardID", ownerships, day)

	// get the number of public active dashboards per user of this day YYYY-mm-dd
	publicDashboardsPerUser := getPublicElementsPerUser("DashboardID", delegations, username_dashboardsids, day)

	// get the number of private active dashboards per user of this day YYYY-mm-dd
	privateDashboardsPerUser := getPrivateElementsPerUser(dashboards, publicDashboardsPerUser)

	// get the number of active iot devices per user of this day YYYY-mm-dd
	// and the dictionary of users and their iot devices ids (elementId)
	devices, username_iotdevicesids := getElementsPerUser("IOTID", ownerships, day)

	// get the number of public active iot devices per user of this day YYYY-mm-dd
	publicDevicesPerUser := getPublicElementsPerUser("IOTID", delegations, username_iotdevicesids, day)

	// get the number of private active iot devices per user of this day YYYY-mm-dd
	privateDevicesPerUser := getPrivateElementsPerUser(devices, publicDevicesPerUser)

	// get the list of pid_local for the agent Node-Red
	pid_local_list := getPidLocalList("Node-Red", start, end, conf)

	// get serviceURI owners' dictionary from MySQL
	serviceURIsOwners := getServiceURIOwners(conf)

	// get IoT reads' dictionary from MySQL (username => count)
	iot_reads := getIoTReads(day, serviceURIsOwners, conf)

	// get dashboards' accesses and minutes
	dashboardsAccessesAndMinutes := getDashboardsDailyAccessesAndMinutes(day, conf)

	// get users' dashboard accesses and minutes for a day
	usersDashboardsAccessesAndMinutes := getUsersDashboardsAccessesAndMinutes(elementIdUsername, dashboardsAccessesAndMinutes)

	// username => motivation => tx map
	username_motivation_tx := map[string]map[string]float64{}

	// username => motivation => rx map
	username_motivation_rx := map[string]map[string]float64{}

	// motivations array
	motivations := [16]string{
		"DB_Storage", "Dashboard", "IoT_Service", "SigFox", "Notificator",
		"ASCAPI", "FileSystem_Storage", "undefined", "MyData", "Datagate",
		"External_Service", "DB_request", "RStatistics",
		"MicroserviceUserCreated", "Mapping", "DISCES"}

	// calculate transmitted/received Node-Red data for this day
	for i := 0; i < len(pid_local_list); i += 2 {
		pid_local := pid_local_list[i]
		// if this pid_local has count > 0
		pid_local_list_i, _ := strconv.ParseInt(pid_local_list[i+1], 10, 64)
		if is_number(pid_local_list[i+1]) && pid_local_list_i > 0 && (len(pid_local) == 64 ||
			(strings.LastIndex(pid_local, "nr") != -1 && len(pid_local) == 7)) {
			// get bytes transmitted/received by Node-RED with motivation
			for _, motivation := range motivations {
				// patch for excluding a bugged pid_local
				if pid_local != "nrwu2iq" {
					tx := getPayload(pid_local, "Node-Red", motivation, "TX", start, end, conf)
					rx := getPayload(pid_local, "Node-Red", motivation, "RX", start, end, conf)
					// update Node-Red dictionaries for this motivation
					username_motivation_tx, username_motivation_rx = updateDictionaries(
						username_motivation_tx, username_motivation_rx, pid_local,
						appid_username, motivation, tx, rx)
				}
			}
		}
	}

	// get the list of pid_local for the agent ETL (the user here is assumed to be always 'disit')
	pid_local_list = getPidLocalList("ETL", start, end, conf)

	// motivation => tx dictionary of user 'disit'
	motivation_tx := map[string]float64{}

	// motivation => rx dictionary of user 'disit'
	motivation_rx := map[string]float64{}

	// calculate transmitted/received ETL data for this day
	for i := 0; i < len(pid_local_list); i += 2 {
		pid_local := pid_local_list[i]
		// if this pid_local has count > 0
		if pid_local_list[i+1] != "" {
			// get bytes transmitted/received by ETL with motivation
			for _, motivation := range motivations {
				tx := getPayload(pid_local, "ETL", motivation, "TX", start, end, conf)
				rx := getPayload(pid_local, "ETL", motivation, "RX", start, end, conf)
				// update ETL dictionaries for this motivation
				updateETLDictionaries(motivation_tx, motivation_rx, motivation, tx, rx)
			}
		}
	}

	// cumulate 'disit' user motivations (ETL related) with Node-RED ones
	for motivation := range username_motivation_tx["disit"] {
		username_motivation_tx["disit"][motivation] += motivation_tx[motivation]
	}
	for motivation := range username_motivation_rx["disit"] {
		username_motivation_rx["disit"][motivation] += motivation_rx[motivation]
	}

	// insert dashboards' data into MySQL
	dashboardsAccessesAndMinutes.ForEach(func(key, value gjson.Result) bool {
		elementId := val_j("idDashboard", value.String())
		username := IfThenElse(elementIdUsername[elementId] != "", elementIdUsername[elementId], nil)
		nAccessesPerDay := val_j("nAccessesPerDay", value.String())
		nMinutesPerDay := val_j("nMinutesPerDay", value.String())
		if username != nil {
			elementId_v, err := strconv.ParseInt(elementId, 10, 64)
			if err == nil {
				insertDashboardsData(
					elementId_v, username.(string), nAccessesPerDay,
					nMinutesPerDay, dashboardBrokerWidgetDictionary,
					dashboardNodeREDWidgetDictionary, start, conf)
			}
		}
		return true // keep iterating
	})

	// get the dictionary of total users by merging dictionaries
	users_total := map[string]int{}
	for k, _ := range username_motivation_rx {
		users_total[k] = 1
	}
	for k, _ := range username_motivation_tx {
		users_total[k] = 1
	}
	for k, _ := range dashboards {
		users_total[k] = 1
	}
	for k, _ := range devices {
		users_total[k] = 1
	}

	// get the dictionary of Node-RED + ETL (username 'disit') users
	users_nodered := map[string]int{}
	for k, _ := range username_motivation_tx {
		users_nodered[k] = 1
	}
	for k, _ := range username_motivation_rx {
		users_nodered[k] = 1
	}

	// get the dictionary of dashboards' users
	users_dashboards := dashboards
	// get the dictionary of devices users
	username_devices := devices

	// index users for this day
	insertUsersData(len(users_total), len(users_nodered),
		len(users_dashboards), len(username_devices), start, conf)

	// index apps (Node-Red) for this day
	for username := range users_total {
		var iot_apps int64 = 0
		// get iot apps of this user
		iot_apps = username_apps[username]
		// get iot reads for this user
		reads := iot_reads[username]
		// get username => motivation => tx dictionary
		tx := username_motivation_tx[username]
		// get username => motivation => rx dictionary
		rx := username_motivation_rx[username]
		// insert apps data for this user into MySQL
		if username != "" {
			insertAppData(username, tx, rx, iot_apps, reads, start, conf)
		}
	}

	// index dashboards for this day
	for username := range dashboards {
		// get public dashboards of this user
		publicDashboards := publicDashboardsPerUser[username]
		// get private dashboards of this user
		privateDashboards := privateDashboardsPerUser[username]
		// insert dashboard data for this user into MySQL
		if username != "" {
			iot_writes_username := iot_writes[username]
			etl_writes_username := etl_writes[username]
			// if this user has no dashboard accesses and minutes data for this day, populate with 0
			if len(usersDashboardsAccessesAndMinutes[username]) == 0 {
				usersDashboardsAccessesAndMinutes[username] = map[string]int64{}
				usersDashboardsAccessesAndMinutes[username]["nAccessesPerDay"] = 0
				usersDashboardsAccessesAndMinutes[username] = map[string]int64{}
				usersDashboardsAccessesAndMinutes[username]["nMinutesPerDay"] = 0
			}
			insertDashboardData(
				username, publicDashboards, privateDashboards,
				usersDashboardsAccessesAndMinutes[username]["nAccessesPerDay"],
				usersDashboardsAccessesAndMinutes[username]["nMinutesPerDay"],
				iot_writes_username,
				etl_writes_username,
				start,
				conf)
		}
	}

	// index devices for this day
	for username := range devices {
		// get public iot devices of this user
		publicDevices := publicDevicesPerUser[username]
		// get private iot devices of this user
		privateDevices := privateDevicesPerUser[username]
		// insert device data for this user into MySQL
		if username != "" {
			iot_writes_username := iot_writes[username]
			etl_writes_username := etl_writes[username]
			insertDeviceData(username, publicDevices,
				privateDevices,
				iot_writes_username,
				etl_writes_username,
				start,
				conf)
		}
	}
}

// get and insert Node-RED data into MySQL
func indexNodeRed(day string, conf map[string]string) {
	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// start of the day
	start := day + "T00:00:00.000Z"

	// end of the day
	end := day + "T23:59:59.999Z"

	// get the Node-RED traffic (TX/RX) for a day
	tx := getNodeREDTraffic(start, end, "TX", conf)
	rx := getNodeREDTraffic(start, end, "RX", conf)

	// get the number of Node-RED for a day
	num := getNodeREDNumber(start, end, conf)

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO nodered (num, tx, rx, date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE num = ?, tx = ?, rx = ?")

	_, err = stmt.Exec(num, tx,
		rx, start,
		num, tx,
		rx)
	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}
}

// get IoT reads' dictionary from MySQL
func getIoTReads(start string, serviceURIsOwners, conf map[string]string) map[string]int64 {
	// setup MySQL connection
	db, err := sql.Open("mysql", conf["MySQL_ServiceMap_username"]+":"+conf["MySQL_ServiceMap_password"]+"@tcp("+conf["MySQL_ServiceMap_hostname"]+":"+conf["MySQL_ServiceMap_port"]+")/"+conf["MySQL_ServiceMap_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// serviceURI access dictionary
	serviceURIs := map[string]int64{}

	results, err := db.Query(
		"SELECT COUNT(*) AS num, serviceuri" +
			" FROM ServiceMap.AccessLog WHERE timestamp >=  '" + start +
			" 00:00:00'" +
			" AND timestamp <= '" + start +
			" 23:59:59' AND serviceuri LIKE \"http://www.disit.org/km4city/resource/iot%\" " +
			"GROUP BY serviceuri ORDER BY num")
	for results.Next() {
		var num int64
		var serviceURI string
		// for each row, scan the result into our tag composite object
		err = results.Scan(&num, &serviceURI)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
		if serviceURIsOwners[serviceURI] != "" {
			serviceURIs[serviceURIsOwners[serviceURI]] = num
		}
	}
	return serviceURIs
}

// get serviceURI owners' dictionary from MySQL
func getServiceURIOwners(conf map[string]string) map[string]string {
	// setup MySQL connection
	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/"+conf["MySQL_profiledb_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// serviceURIs => owner dictionary
	serviceURIs := map[string]string{}

	results, err := db.Query("SELECT username, elementurl FROM profiledb.ownership WHERE elementtype = \"IOTID\" " +
		"AND elementurl LIKE \"http://www.disit.org/km4city/resource/iot%\"")
	for results.Next() {
		var username string
		var elementurl string
		// for each row, scan the result into our tag composite object
		err = results.Scan(&username, &elementurl)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
		serviceURIs[elementurl] = username
	}
	return serviceURIs
}

// get Dashboards' Ids from MySQL
func getDashboardsIds(conf map[string]string) []string {

	// setup MySQL connection
	db, err := sql.Open("mysql", conf["MySQL_Dashboard_username"]+":"+conf["MySQL_Dashboard_password"]+"@tcp("+conf["MySQL_Dashboard_hostname"]+":"+conf["MySQL_Dashboard_port"]+")/"+conf["MySQL_Dashboard_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished
	// executing
	defer db.Close()

	// Dashboards' Ids array
	ids := []string{}

	results, err := db.Query("SELECT id FROM Dashboard.Config_dashboard WHERE deleted = \"no\"")
	for results.Next() {
		var id string
		// for each row, scan the result into our tag composite object
		err = results.Scan(&id)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
		ids = append(ids, id)
	}
	return ids
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
	conf["MySQL_Dashboard_hostname"] = "localhost"
	conf["MySQL_Dashboard_username"] = "user"
	conf["MySQL_Dashboard_password"] = "password"
	conf["MySQL_Dashboard_port"] = "3306"
	conf["MySQL_Dashboard_database"] = "Dashboard"

	// MySQL profiledb
	conf["MySQL_profiledb_hostname"] = "localhost"
	conf["MySQL_profiledb_username"] = "user"
	conf["MySQL_profiledb_password"] = "password"
	conf["MySQL_profiledb_port"] = "3306"
	conf["MySQL_profiledb_database"] = "profiledb"

	// MySQL ServiceMap
	conf["MySQL_ServiceMap_hostname"] = "localhost"
	conf["MySQL_ServiceMap_username"] = "user"
	conf["MySQL_ServiceMap_password"] = "password"
	conf["MySQL_ServiceMap_port"] = "3306"
	conf["MySQL_ServiceMap_database"] = "ServiceMap"

	// ElasticSearch
	conf["ElasticSearchIP"] = "localhost"

	// DataManager API (Angelo)
	conf["DataManagerApiUrl"] = "http://localhost:8080/datamanager/"

	// Daily Accesses API
	conf["DashboardsDailyAccessesApiUrl"] = "http://localhost/api/dashDailyAccess.php"

	// Ownership API (Piero)
	conf["OwnershipApiUrl"] = "localhost"
	conf["OwnershipApiUsername"] = "admin"
	conf["OwnershipApiPassword"] = "password"
	conf["OwnershipApiGrantType"] = "password"

	// Access Token
	conf["AccessTokenUrl"] = "http://localhost/auth/realms/master/protocol/openid-connect/token"
	conf["AccessTokenPassword"] = "password"
	conf["AccessTokenUsername"] = "rootuser"
	conf["AccessTokenClientID"] = "metricscollector"
	conf["AccessTokenGrantType"] = "password"

	// Solr Url (not used)
	conf["SolrUrl"] = "localhost"

	// Custom settings
	// get conf flag command line parameter
	c := flag.String("conf", "", "Configuration file path (JSON)")

	// parse flags
	flag.Parse()

	// don't use lowercase letter in struct members' initial letter, otherwise it does not work
	// https://stackoverflow.com/questions/24837432/golang-capitals-in-struct-fields
	type Configuration struct {
		MySQLDashboardHostname string
		MySQLDashboardUsername string
		MySQLDashboardPassword string
		MySQLDashboardPort     string
		MySQLDashboardDatabase string

		MySQLProfiledbHostname string
		MySQLProfiledbUsername string
		MySQLProfiledbPassword string
		MySQLProfiledbPort     string
		MySQLProfiledbDatabase string

		MySQLServiceMapHostname string
		MySQLServiceMapUsername string
		MySQLServiceMapPassword string
		MySQLServiceMapPort     string
		MySQLServiceMapDatabase string

		ElasticSearchIP string

		DataManagerApiUrl string

		DashboardsDailyAccessesApiUrl string

		OwnershipApiUrl       string
		OwnershipApiUsername  string
		OwnershipApiPassword  string
		OwnershipApiGrantType string

		AccessTokenUrl       string
		AccessTokenUsername  string
		AccessTokenPassword  string
		AccessTokenClientID  string
		AccessTokenGrantType string

		SolrUrl string
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
			conf["MySQL_Dashboard_hostname"] = configuration.MySQLDashboardHostname
			conf["MySQL_Dashboard_username"] = configuration.MySQLDashboardUsername
			conf["MySQL_Dashboard_password"] = configuration.MySQLDashboardPassword
			conf["MySQL_Dashboard_port"] = configuration.MySQLDashboardPort
			conf["MySQL_Dashboard_database"] = configuration.MySQLDashboardDatabase

			conf["MySQL_profiledb_hostname"] = configuration.MySQLProfiledbHostname
			conf["MySQL_profiledb_username"] = configuration.MySQLProfiledbUsername
			conf["MySQL_profiledb_password"] = configuration.MySQLProfiledbPassword
			conf["MySQL_profiledb_port"] = configuration.MySQLProfiledbPort
			conf["MySQL_profiledb_database"] = configuration.MySQLProfiledbDatabase

			conf["MySQL_ServiceMap_hostname"] = configuration.MySQLServiceMapHostname
			conf["MySQL_ServiceMap_username"] = configuration.MySQLServiceMapUsername
			conf["MySQL_ServiceMap_password"] = configuration.MySQLServiceMapPassword
			conf["MySQL_ServiceMap_port"] = configuration.MySQLServiceMapPort
			conf["MySQL_ServiceMap_database"] = configuration.MySQLServiceMapDatabase

			conf["ElasticSearchIP"] = configuration.ElasticSearchIP

			conf["DataManagerApiUrl"] = configuration.DataManagerApiUrl

			conf["DashboardsDailyAccessesApiUrl"] = configuration.DashboardsDailyAccessesApiUrl

			conf["OwnershipApiUrl"] = configuration.OwnershipApiUrl
			conf["OwnershipApiUsername"] = configuration.OwnershipApiUsername
			conf["OwnershipApiPassword"] = configuration.OwnershipApiPassword

			conf["AccessTokenUrl"] = configuration.AccessTokenUrl
			conf["AccessTokenUsername"] = configuration.AccessTokenUsername
			conf["AccessTokenPassword"] = configuration.AccessTokenPassword
			conf["AccessTokenClientID"] = configuration.AccessTokenClientID
			conf["AccessTokenGrantType"] = configuration.AccessTokenGrantType

			conf["SolrUrl"] = configuration.SolrUrl
		}
	}

	// startDate is now - n day
	startDate := now.AddDate(0, 0, -*n)

	// endDate is now - 1 day
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

	// get the ownerships (Piero's API)
	ownerships := getOwnershipsJSON(conf)

	// get the delegations (Angelo's API)
	delegations := getDelegationsJSON(conf)

	// get serviceURI owners' dictionary from MySQL
	//serviceURIsOwners := getServiceURIOwners()

	// get elementId => username dictionary from MySQL
	elementIdUsername := getElementIdUsername(conf)

	// get dashboard => broker dictionary
	// list of dashboards with a broker widget
	dashboardBrokerWidgetDictionary := getDashboardBrokerWidgetDictionary(conf)

	// get dashboard => widget Node-RED dictionary
	// list of dashboards with a Node-RED widget
	dashboardNodeREDWidgetDictionary := getDashboardNodeREDWidgetDictionary(conf)

	// dictionary iotid => owner's username
	iotid_username := getIoTUsername(conf)

	// get Dashboards' Ids from MySQL
	//dashboardsids := getDashboardsIds()

	// for every day in the date range insert data into MySQL
	for rd := rangeDateRecentToOld(endDate, startDate); ; {
		single_date := rd()
		if single_date.IsZero() {
			break
		}
		day := single_date.Format("2006-01-02")
		// index user data
		indexUserData(ownerships, delegations, elementIdUsername, iotid_username,
			dashboardBrokerWidgetDictionary,
			dashboardNodeREDWidgetDictionary, day, conf)
		// index R
		indexR(day, conf)
		// index ETL
		indexETL(day, conf)
		// index Node-Red
		indexNodeRed(day, conf)
	}
}
