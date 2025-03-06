// getNodeREDTraffic, getETLTrafficPerDay and getPayload calls are commented in the code to make it work
// Elastic Search API are not working
package main

import (
	"crypto/tls"
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

func getIndexSize(url string) int64 {
	body := strings.NewReader(`{"query": { "match": { "agent": "Node-Red" } } }`)
	req, err := http.NewRequest("POST", url+"/_search?pretty", body)
	if err != nil {
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	return gjson.Get(string(b), "hits.total").Int()
}

func getDailyUsers(date string, conf map[string]string) int {
	d := strings.Split(date, "T")
	toDate := d[0]
	toDate = toDate + "T23:59:59.999Z"
	resp, err := resty.R().
		Get("http://" + conf["SolrUrl"] + ":8983/solr/syslog/select?facet.field=pid_local&facet=on&indent=on&q=date_time:[%22" +
			date + "%22%20TO%20%22" +
			toDate + "%22]%20AND%20agent:%22Node-Red%22&rows=0&sort=date_time%20desc&start=0&wt=json")
	if err != nil {
		return 0
	}
	return len(gjson.Get(string(resp.Body()[:]), "facet_counts.facet_fields.pid_local").Array())
}

func getNodeREDTraffic(startDate, endDate, com_mode string, conf map[string]string) float64 {
	body := strings.NewReader(`{
  "query":"SELECT SUM(payload) FROM syslog-* WHERE com_mode = '` + com_mode + `' AND date_time >= '` + startDate + `' AND date_time <= '` + endDate + `' LIMIT 1"
}`)

	req, err := http.NewRequest("POST", "https://"+conf["ElasticSearchIP"]+":9200/_opendistro/_sql", body)
	req.SetBasicAuth(conf["ElasticSearchUsername"], conf["ElasticSearchPassword"])
	if err != nil {
	}
	req.Header.Set("Content-Type", "application/json")

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	resp, err := client.Do(req)
	if err != nil {
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "datarows").Value()
	a := n.([]interface{})
	return a[0].([]interface{})[0].(float64)
}

func getNodeREDNumber(startDate, endDate string, conf map[string]string) int64 {
	body := strings.NewReader(`{
  "query":"SELECT pid_local FROM syslog-* WHERE agent = 'Node-Red' AND date_time > '` + startDate + `' AND date_time < '` + endDate + `'"
}`)

	req, err := http.NewRequest("POST", "https://"+conf["ElasticSearchIP"]+":9200/_opendistro/_sql", body)
	req.SetBasicAuth(conf["ElasticSearchUsername"], conf["ElasticSearchPassword"])
	if err != nil {
	}
	req.Header.Set("Content-Type", "application/json")

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	resp, err := client.Do(req)
	if err != nil {
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "datarows").Value()
	a := n.([]interface{})
	result := []string{}
	for _, c := range a {
		if !stringInSlice(c.([]interface{})[0].(string), result) {
			result = append(result, c.([]interface{})[0].(string))
		}
	}

	return (int64)(len(result))
}

func getETLProcessesPerDay(startDate, endDate string, conf map[string]string) int64 {
	body := strings.NewReader(`{
  "query":"SELECT COUNT(*) FROM syslog-* WHERE agent = 'ETL' AND com_mode = 'RX' AND date_time > '` + startDate + `' AND date_time < '` + endDate + `'"
}`)

	req, err := http.NewRequest("POST", "https://"+conf["ElasticSearchIP"]+":9200/_opendistro/_sql", body)
	req.SetBasicAuth(conf["ElasticSearchUsername"], conf["ElasticSearchPassword"])
	if err != nil {
	}
	req.Header.Set("Content-Type", "application/json")

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	resp, err := client.Do(req)
	if err != nil {
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "datarows").Value()
	a := n.([]interface{})
	return (int64)(a[0].([]interface{})[0].(float64))
}

func getETLTrafficPerDay(com_mode, startDate, endDate string, conf map[string]string) float64 {
	body := strings.NewReader(`{
  "query":"SELECT SUM(payload) FROM syslog-* WHERE agent = 'ETL' AND com_mode = '` + com_mode + `' AND date_time > '` + startDate + `' AND date_time < '` + endDate + `'"
}`)

	req, err := http.NewRequest("POST", "https://"+conf["ElasticSearchIP"]+":9200/_opendistro/_sql", body)
	req.SetBasicAuth(conf["ElasticSearchUsername"], conf["ElasticSearchPassword"])
	if err != nil {
	}
	req.Header.Set("Content-Type", "application/json")

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	resp, err := client.Do(req)
	if err != nil {
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "datarows").Value()
	a := n.([]interface{})
	return a[0].([]interface{})[0].(float64)
}

func getRStudioPerDay(startDate, endDate string, conf map[string]string) int64 {
	body := strings.NewReader(`{
  "query":"SELECT COUNT(*) FROM syslog-* WHERE motivation = 'RStatistics' AND date_time > '` + startDate + `' AND date_time < '` + endDate + `'"
}`)

	req, err := http.NewRequest("POST", "https://"+conf["ElasticSearchIP"]+":9200/_opendistro/_sql", body)
	req.SetBasicAuth(conf["ElasticSearchUsername"], conf["ElasticSearchPassword"])
	if err != nil {
	}
	req.Header.Set("Content-Type", "application/json")

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	resp, err := client.Do(req)
	if err != nil {
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "datarows").Value()
	a := n.([]interface{})
	return (int64)(a[0].([]interface{})[0].(float64))
}

func getIoTETLWritesPerDay(iotid_username map[string]string, day string, conf map[string]string) (map[string]float64, map[string]float64) {
	iot_writes := map[string]float64{}
	etl_writes := map[string]float64{}
	serviceUrls_etlwrites := map[string]float64{}

	start := day + "T00:00:00.000Z"

	end := day + "T23:59:59.999Z"

	body := strings.NewReader(`{
  "query":"SELECT COUNT(*) AS NUM, serviceUri.keyword FROM org-* WHERE date_time > '` + start + `' AND date_time < '` + end + `' GROUP BY serviceUri.keyword"
}`)

	req, err := http.NewRequest("POST", "https://"+conf["ElasticSearchIP"]+":9200/_opendistro/_sql", body)
	req.SetBasicAuth(conf["ElasticSearchUsername"], conf["ElasticSearchPassword"])
	if err != nil {
	}
	req.Header.Set("Content-Type", "application/json")

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	resp, err := client.Do(req)
	if err != nil {
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "datarows").Value()
	a := n.([]interface{})
	for _, v := range a {
		c := v.([]interface{})[0]
		s := v.([]interface{})[1]
		serviceUrls_etlwrites[s.(string)] = c.(float64)
	}

	for serviceUrl, _ := range serviceUrls_etlwrites {
		if strings.Contains(serviceUrl, "/resource/iot") {
			if _, ok := iotid_username[serviceUrl]; ok {
				if _, ok := iot_writes[iotid_username[serviceUrl]]; ok {
					iot_writes[iotid_username[serviceUrl]] += serviceUrls_etlwrites[serviceUrl]
				} else {
					iot_writes[iotid_username[serviceUrl]] = serviceUrls_etlwrites[serviceUrl]
				}
			}
		} else {
			if _, ok := etl_writes["disit"]; ok {
				etl_writes["disit"] += serviceUrls_etlwrites[serviceUrl]
			} else {
				etl_writes["disit"] = serviceUrls_etlwrites[serviceUrl]
			}
		}
	}
	return iot_writes, etl_writes
}

func getIoTUsername(conf map[string]string) map[string]string {
	result := map[string]string{}

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/"+conf["MySQL_profiledb_database"])

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	results, err := db.Query("SELECT distinct username, elementurl FROM profiledb.ownership WHERE elementtype = \"IOTID\" AND elementurl != \"\"")
	if err != nil {
		panic(err.Error())
	}

	var username string
	var elementurl string

	for results.Next() {
		err = results.Scan(&username, &elementurl)
		if err != nil {
			panic(err.Error())
		}
		result[elementurl] = username
	}
	return result
}

func timestamp(date string) int64 {
	layout := "2006-01-02 15:04:05"
	t, _ := time.Parse(layout, date)
	return t.Unix()
}

func stringInSlice(a string, list []string) bool {
	for _, b := range list {
		if b == a {
			return true
		}
	}
	return false
}

func val_j(key, json_str string) string {
	return gjson.Get(json_str, key).String()
}

func key_j(key, json_str string) bool {
	return gjson.Get(json_str, key).Exists()
}

func IfThenElse(condition bool, a interface{}, b interface{}) interface{} {
	if condition {
		return a
	}
	return b
}

func getDashboardDailyAccessesAndMinutes(idDash string, date string, conf map[string]string) (string, string) {
	resp, _ := resty.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetFormData(map[string]string{
			"idDash": idDash,
			"date":   date,
		}).Post(conf["DashboardsDailyAccessesApiUrl"])
	return val_j("nAccessesPerDay", string(resp.Body()[:])), val_j("nMinutesPerDay", string(resp.Body()[:]))
}

func getDashboardsDailyAccessesAndMinutes(date string, conf map[string]string) gjson.Result {
	resp, _ := resty.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetFormData(map[string]string{
			"date": date,
		}).Post(conf["DashboardsDailyAccessesApiUrl"])
	return gjson.Parse(string(resp.Body()[:]))
}

func getElementIdUsername(conf map[string]string) map[string]string {
	result := map[string]string{}

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/"+conf["MySQL_profiledb_database"])

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	results, err := db.Query("SELECT distinct elementId, username FROM profiledb.ownership WHERE elementtype = \"DashboardID\"")
	if err != nil {
		panic(err.Error())
	}

	type Tag struct {
		elementId string
		username  string
	}

	for results.Next() {
		var tag Tag
		err = results.Scan(&tag.elementId, &tag.username)
		if err != nil {
			panic(err.Error())
		}
		result[tag.elementId] = tag.username
	}
	return result
}

func getUsersDashboardsAccessesAndMinutes(elementIdUsername map[string]string, dashboardDailyAccessesAndMinutes gjson.Result) map[string]map[string]int64 {
	users := map[string]map[string]int64{}

	for elementId, _ := range elementIdUsername {
		var nAccessesPerDay, nMinutesPerDay int64 = 0, 0
		dashboardDailyAccessesAndMinutes.ForEach(func(key, value gjson.Result) bool {
			j := gjson.GetMany(value.String(), "idDashboard", "nAccessesPerDay", "nMinutesPerDay")
			if j[0].String() == elementId {
				nAccessesPerDay += j[1].Int()
				nMinutesPerDay += j[2].Int()
			}
			return true
		})

		username := elementIdUsername[elementId]

		if _, ok := users[username]; !ok {
			users[username] = make(map[string]int64)
		}
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
	return val_j("access_token", string(resp.Body()[:]))
}

func getOwnershipsJSON(conf map[string]string) gjson.Result {
	resty.SetRedirectPolicy(resty.FlexibleRedirectPolicy(15))
	resp, _ := resty.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetFormData(map[string]string{
			"grant_type": conf["OwnershipApiGrantType"],
			"username":   conf["OwnershipApiUsername"],
			"password":   conf["OwnershipApiPassword"],
		}).Get("http://" + conf["OwnershipApiUrl"] + "/ownership-api/v1/list" + "?accessToken=" + getAccessToken(conf) + "&includeDeleted")
	return gjson.Parse(string(resp.Body()[:]))
}

func getDelegationsJSON(conf map[string]string) gjson.Result {
	resp, _ := resty.R().
		SetQueryParams(map[string]string{"accessToken": getAccessToken(conf),
			"sourceRequest": "metricscollector",
			"deleted":       "true"}).
		Get(conf["DataManagerApiUrl"] + "/api/v1/username/ANONYMOUS/delegated")
	return gjson.Parse(string(resp.Body()[:]))
}

func getOwnerships(ownerships gjson.Result, day string, conf map[string]string) (map[string]string, map[string]int64, map[string]string) {
	appid_username := map[string]string{}

	username_apps := map[string]int64{}

	iotid_username := getIoTUsername(conf)

	start_timestamp := timestamp(day + " 00:00:00")

	ownerships.ForEach(func(key, value gjson.Result) bool {
		val_s := value.String()
		if ok := key_j("elementType", val_s) && key_j("elementId", val_s) && key_j("username", val_s); ok {
			start_timestamp = timestamp(day + " 00:00:00")
			if ok := key_j("created", val_s) && val_j("created", val_s) != "null" && (!key_j("deleted", val_s) || val_j("deleted", val_s) == "" || val_j("deleted", val_s) == "null"); ok {
				creation_date := strings.Split(val_j("created", val_s), " ")
				creation_date_v := timestamp(creation_date[0] + " 00:00:00")
				if start_timestamp >= creation_date_v {
					if val_j("elementType", val_s) == "AppID" {
						appid_username[val_j("elementId", val_s)] = val_j("username", val_s)
						if _, ok := username_apps[strings.ToLower(val_j("username", val_s))]; ok {
							username_apps[strings.ToLower(val_j("username", val_s))] = username_apps[strings.ToLower(val_j("username", val_s))] + 1
						} else {
							username_apps[strings.ToLower(val_j("username", val_s))] = 1
						}
					}
				}
			} else if ok := key_j("created", val_s) && val_j("created", val_s) != "null"; ok {
				creation_date := strings.Split(val_j("created", val_s), " ")
				creation_date_v := timestamp(creation_date[0] + " 00:00:00")
				deletion_date := timestamp(val_j("deleted", val_s))
				if start_timestamp >= creation_date_v && start_timestamp <= deletion_date {
					if val_j("elementType", val_s) == "AppID" {
						appid_username[val_j("elementId", val_s)] = val_j("username", val_s)
						if ok := key_j("username", val_s); ok {
							username_apps[strings.ToLower(val_j("username", val_s))] = username_apps[strings.ToLower(val_j("username", val_s))] + 1
						} else {
							username_apps[strings.ToLower(val_j("username", val_s))] = 1
						}
					}
				}
			}
		}
		return true
	})
	return appid_username, username_apps, iotid_username
}

func getElementsPerUser(elementType string, ownerships gjson.Result, day string) (map[string]int64, map[string][]string) {
	result := map[string]int64{}
	username_ids := map[string][]string{}
	start_timestamp := timestamp(day + " 00:00:00")
	ownerships.ForEach(func(key, value gjson.Result) bool {
		val_s := value.String()
		if ok := key_j("created", val_s) && val_j("created", val_s) != "null"; ok {
			created := strings.Split(val_j("created", val_s), " ")
			created_v := timestamp(created[0] + " 00:00:00")

			if ok := key_j("elementType", val_s) &&
				val_j("elementType", val_s) == elementType &&
				(!key_j("deleted", val_s) ||
					val_j("deleted", val_s) == "") &&
				start_timestamp >= created_v; ok {
				if _, ok := result[strings.ToLower(val_j("username", val_s))]; ok {
					result[strings.ToLower(val_j("username", val_s))] = result[strings.ToLower(val_j("username", val_s))] + 1
				} else {
					result[strings.ToLower(val_j("username", val_s))] = 1
				}
				username_ids[strings.ToLower(val_j("username", val_s))] = append(username_ids[strings.ToLower(val_j("username", val_s))], val_j("elementId", val_s))
			} else if ok := key_j("elementType", val_s) &&
				val_j("elementType", val_s) == elementType &&
				key_j("usernameDelegator", val_s) &&
				val_j("deleted", val_s) != "" &&
				start_timestamp >= created_v &&
				start_timestamp <= timestamp(val_j("deleted", val_s)); ok {
				if _, ok := result[strings.ToLower(val_j("username", val_s))]; ok {
					result[strings.ToLower(val_j("username", val_s))] = result[strings.ToLower(val_j("username", val_s))] + 1
				} else {
					result[strings.ToLower(val_j("username", val_s))] = 1
				}
				username_ids[strings.ToLower(val_j("username", val_s))] = append(username_ids[strings.ToLower(val_j("username", val_s))], val_j("elementId", val_s))
			}
		}
		return true
	})
	return result, username_ids
}

func getPublicElementsPerUser(elementType string, delegations gjson.Result, username_ids map[string][]string, day string) map[string]int64 {
	result := map[string]int64{}
	start_timestamp := timestamp(day+" 00:00:00") * 1000
	var insertTime_v int64
	delegations.ForEach(func(key, value gjson.Result) bool {
		val_s := value.String()
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
		return true
	})
	return result
}

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

func getPayload(pid_local, agent, motivation, com_mode, start, end string, conf map[string]string) float64 {
	body := strings.NewReader(`{
  "query":"SELECT SUM(payload) FROM syslog-* WHERE agent = '` + agent + `' AND com_mode = '` + com_mode + `' AND motivation = '` + motivation + `' AND pid_local = '` + pid_local + `' AND date_time > '` + start + `' AND date_time < '` + end + `'"
}`)
	fmt.Println(`{
  "query":"SELECT SUM(payload) FROM syslog-* WHERE agent = '` + agent + `' AND com_mode = '` + com_mode + `' AND motivation = '` + motivation + `' AND pid_local = '` + pid_local + `' AND date_time > '` + start + `' AND date_time < '` + end + `'"
}`)

	req, err := http.NewRequest("POST", "https://"+conf["ElasticSearchIP"]+":9200/_opendistro/_sql", body)
	req.SetBasicAuth(conf["ElasticSearchUsername"], conf["ElasticSearchPassword"])
	if err != nil {
	}
	req.Header.Set("Content-Type", "application/json")

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	resp, err := client.Do(req)
	if err != nil {
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)
	fmt.Println(string(b))
	n := gjson.Get(string(b), "datarows").Value()
	a := n.([]interface{})
	if len(a) > 0 && len(a[0].([]interface{})) > 0 {
		return a[0].([]interface{})[0].(float64)
	} else {
		return 0
	}
}

func getPidLocalList(agent, start, end string, conf map[string]string) []string {
	body := strings.NewReader(`{
  "query":"SELECT COUNT(*) AS num, pid_local.keyword FROM syslog-* WHERE agent = '` + agent + `' AND com_mode = 'TX' AND date_time > '` + start + `' AND date_time < '` + end + `' GROUP BY pid_local.keyword"
}`)

	req, err := http.NewRequest("POST", "https://"+conf["ElasticSearchIP"]+":9200/_opendistro/_sql", body)
	req.SetBasicAuth(conf["ElasticSearchUsername"], conf["ElasticSearchPassword"])
	if err != nil {
	}
	req.Header.Set("Content-Type", "application/json")

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	resp, err := client.Do(req)
	if err != nil {
	}
	defer resp.Body.Close()

	b, _ := ioutil.ReadAll(resp.Body)

	n := gjson.Get(string(b), "datarows").Value()
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

func getDashboardBrokerWidgetDictionary(conf map[string]string) []int64 {
	result := []int64{}

	db, err := sql.Open("mysql", conf["MySQL_Dashboard_username"]+":"+conf["MySQL_Dashboard_password"]+"@tcp("+conf["MySQL_Dashboard_hostname"]+":"+conf["MySQL_Dashboard_port"]+")/"+conf["MySQL_Dashboard_database"])

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	results, err := db.Query("SELECT id_dashboard FROM Dashboard.Config_widget_dashboard " +
		"WHERE actuatorTarget = 'broker' AND cancelDate IS NULL " +
		"GROUP BY id_dashboard")
	if err != nil {
		panic(err.Error())
	}

	type Tag struct {
		id_dashboard int64
	}

	for results.Next() {
		var tag Tag
		err = results.Scan(&tag.id_dashboard)
		if err != nil {
			panic(err.Error())
		}
		result = append(result, tag.id_dashboard)
	}
	return result
}

func getDashboardNodeREDWidgetDictionary(conf map[string]string) []int64 {
	result := []int64{}

	db, err := sql.Open("mysql", conf["MySQL_Dashboard_username"]+":"+conf["MySQL_Dashboard_password"]+"@tcp("+conf["MySQL_Dashboard_hostname"]+":"+conf["MySQL_Dashboard_port"]+")/"+conf["MySQL_Dashboard_database"])

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

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
		panic(err.Error())
	}

	type Tag struct {
		id_dashboard int64
	}

	for results.Next() {
		var tag Tag
		err = results.Scan(&tag.id_dashboard)
		if err != nil {
			panic(err.Error())
		}
		result = append(result, tag.id_dashboard)
	}
	return result
}

func updateDictionaries(username_motivation_tx map[string]map[string]float64,
	username_motivation_rx map[string]map[string]float64,
	pid_local string,
	appid_username map[string]string,
	motivation string,
	tx float64,
	rx float64) (map[string]map[string]float64, map[string]map[string]float64) {

	_, pid_local_in_appid_username := appid_username[pid_local]

	username_in_username_motivation_tx := false
	username_in_username_motivation_rx := false

	if pid_local_in_appid_username {
		_, username_in_username_motivation_tx = username_motivation_tx[appid_username[pid_local]]
		_, username_in_username_motivation_rx = username_motivation_rx[appid_username[pid_local]]
	}

	motivation_in_username_motivation_tx := false
	motivation_in_username_motivation_rx := false

	if username_in_username_motivation_tx {
		_, motivation_in_username_motivation_tx = username_motivation_tx[appid_username[pid_local]][motivation]
	}
	if username_in_username_motivation_rx {
		_, motivation_in_username_motivation_rx = username_motivation_rx[appid_username[pid_local]][motivation]
	}

	if pid_local_in_appid_username {
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

		if pid_local_in_appid_username && username_in_username_motivation_tx && motivation_in_username_motivation_tx {
			username_motivation_tx[appid_username[pid_local]][motivation] += tx
		}
		if pid_local_in_appid_username && username_in_username_motivation_rx && motivation_in_username_motivation_rx {
			username_motivation_rx[appid_username[pid_local]][motivation] += rx
		}
	}
	return username_motivation_tx, username_motivation_rx
}

func updateETLDictionaries(motivation_tx, motivation_rx map[string]float64, motivation string, tx, rx float64) {
	_, ok := motivation_tx[motivation]
	if !ok {
		motivation_tx[motivation] = 0
	}
	_, ok = motivation_rx[motivation]
	if !ok {
		motivation_rx[motivation] = 0
	}

	motivation_tx[motivation] += tx
	motivation_rx[motivation] += rx
}

func is_number(s string) bool {
	if _, err := strconv.ParseFloat(s, 64); err == nil {
		return true
	} else {
		return false
	}
}

func insertAppData(username string,
	tx map[string]float64,
	rx map[string]float64,
	iot_apps int64,
	iot_reads int64,
	date string,
	conf map[string]string) {

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

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
	if err != nil {
		panic(err.Error())
	}
}

func insertDashboardData(username string, dashboards_public int64, dashboards_private int64,
	nAccessesPerDay int64, nMinutesPerDay int64, iot_writes float64,
	etl_writes float64, date string, conf map[string]string) {

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO data (username, dashboards_public, dashboards_private, dashboards_accesses, dashboards_minutes, iot_writes, etl_writes, date)" +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE dashboards_public = ?, dashboards_private = ?, dashboards_accesses = ?, dashboards_minutes = ?, iot_writes = ?, etl_writes = ?")
	if err != nil {
		panic(err.Error())
	}
	_, err = stmt.Exec(username, dashboards_public, dashboards_private,
		nAccessesPerDay, nMinutesPerDay, iot_writes, etl_writes, date,
		dashboards_public, dashboards_private, nAccessesPerDay,
		nMinutesPerDay, iot_writes, etl_writes)
	if err != nil {
		panic(err.Error())
	}
}

func insertDashboardsData(elementId int64, username string, nAccessesPerDay string,
	nMinutesPerDay string, dashboardBrokerWidgetDictionary []int64,
	dashboardNodeREDWidgetDictionary []int64, date string, conf map[string]string) {
	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	if err != nil {
		panic(err.Error())
	}

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
	if err != nil {
		panic(err.Error())
	}
}

func insertDeviceData(username string, devices_public int64, devices_private int64,
	iot_writes float64, etl_writes float64, date string, conf map[string]string) {

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO data (username, devices_public, devices_private, iot_writes, etl_writes, date)" +
			"VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE devices_public = ?, devices_private = ?, iot_writes = ?, etl_writes = ?")
	_, err = stmt.Exec(username, devices_public, devices_private, iot_writes,
		etl_writes, date, devices_public, devices_private, iot_writes,
		etl_writes)
	if err != nil {
		panic(err.Error())
	}
}

func insertUsersData(users_total int, users_nodered int, users_dashboards int,
	username_devices int, date string, conf map[string]string) {

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO users (nodered, dashboards, devices, total, date)" +
			"VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE nodered = ?, dashboards = ?, devices = ?, total = ?")
	_, err = stmt.Exec(users_nodered, users_dashboards, username_devices, users_total,
		date, users_nodered, users_dashboards, username_devices,
		users_total)
	if err != nil {
		panic(err.Error())
	}
}

func indexETL(day string, conf map[string]string) {
	start := day + "T00:00:00.000Z"

	end := day + "T23:59:59.999Z"

	num_etls := getETLProcessesPerDay(start, end, conf)

	tx := 0.0 //getETLTrafficPerDay("TX", start, end, conf)
	rx := 0.0 //getETLTrafficPerDay("RX", start, end, conf)

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO etl (num, tx, rx, date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE num = ?, tx = ?, rx = ?")

	_, err = stmt.Exec(strconv.FormatInt(num_etls, 10),
		strconv.FormatFloat(tx, 'f', -1, 64), strconv.FormatFloat(rx, 'f', -1, 64),
		start, strconv.FormatInt(num_etls, 10), strconv.FormatFloat(tx, 'f', -1, 64),
		strconv.FormatFloat(rx, 'f', -1, 64))
	if err != nil {
		panic(err.Error())
	}
}

func indexR(day string, conf map[string]string) {
	start := day + "T00:00:00.000Z"

	end := day + "T23:59:59.999Z"

	num_r := getRStudioPerDay(start, end, conf)

	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO r (num, date) VALUES (?, ?) ON DUPLICATE KEY UPDATE num = ?")

	_, err = stmt.Exec(strconv.FormatInt(num_r, 10), start, strconv.FormatInt(num_r, 10))
	if err != nil {
		panic(err.Error())
	}
}

func indexUserData(ownerships gjson.Result,
	delegations gjson.Result,
	elementIdUsername map[string]string,
	iotid_username map[string]string,
	dashboardBrokerWidgetDictionary []int64,
	dashboardNodeREDWidgetDictionary []int64,
	day string,
	conf map[string]string) {
	start := day + "T00:00:00.000Z"

	end := day + "T23:59:59.999Z"

	appid_username, username_apps, iotid_username := getOwnerships(ownerships, day, conf)

	iot_writes := map[string]float64{}
	etl_writes := map[string]float64{}

	dashboards, username_dashboardsids := getElementsPerUser("DashboardID", ownerships, day)

	publicDashboardsPerUser := getPublicElementsPerUser("DashboardID", delegations, username_dashboardsids, day)

	privateDashboardsPerUser := getPrivateElementsPerUser(dashboards, publicDashboardsPerUser)

	devices, username_iotdevicesids := getElementsPerUser("IOTID", ownerships, day)

	publicDevicesPerUser := getPublicElementsPerUser("IOTID", delegations, username_iotdevicesids, day)

	privateDevicesPerUser := getPrivateElementsPerUser(devices, publicDevicesPerUser)

	pid_local_list := getPidLocalList("Node-Red", start, end, conf)

	serviceURIsOwners := getServiceURIOwners(conf)

	iot_reads := getIoTReads(day, serviceURIsOwners, conf)

	dashboardsAccessesAndMinutes := getDashboardsDailyAccessesAndMinutes(day, conf)

	usersDashboardsAccessesAndMinutes := getUsersDashboardsAccessesAndMinutes(elementIdUsername, dashboardsAccessesAndMinutes)

	username_motivation_tx := map[string]map[string]float64{}

	username_motivation_rx := map[string]map[string]float64{}

	motivations := [16]string{
		"DB_Storage", "Dashboard", "IoT_Service", "SigFox", "Notificator",
		"ASCAPI", "FileSystem_Storage", "undefined", "MyData", "Datagate",
		"External_Service", "DB_request", "RStatistics",
		"MicroserviceUserCreated", "Mapping", "DISCES"}

	for i := 0; i < len(pid_local_list); i += 2 {
		pid_local := pid_local_list[i]
		pid_local_list_i, _ := strconv.ParseInt(pid_local_list[i+1], 10, 64)
		if is_number(pid_local_list[i+1]) && pid_local_list_i > 0 && (len(pid_local) == 64 ||
			(strings.LastIndex(pid_local, "nr") != -1 && len(pid_local) == 7)) {
			for _, motivation := range motivations {
				if pid_local != "nrwu2iq" {
					tx := 0.0 //getPayload(pid_local, "Node-Red", motivation, "TX", start, end, conf)
					rx := 0.0 //getPayload(pid_local, "Node-Red", motivation, "RX", start, end, conf)
					username_motivation_tx, username_motivation_rx = updateDictionaries(
						username_motivation_tx, username_motivation_rx, pid_local,
						appid_username, motivation, tx, rx)
				}
			}
		}
	}

	pid_local_list = getPidLocalList("ETL", start, end, conf)

	motivation_tx := map[string]float64{}

	motivation_rx := map[string]float64{}

	for i := 0; i < len(pid_local_list); i += 2 {
		if pid_local_list[i+1] != "" {
			for _, motivation := range motivations {
				tx := 0.0 //getPayload(pid_local, "ETL", motivation, "TX", start, end, conf)
				rx := 0.0 //getPayload(pid_local, "ETL", motivation, "RX", start, end, conf)
				updateETLDictionaries(motivation_tx, motivation_rx, motivation, tx, rx)
			}
		}
	}

	for motivation := range username_motivation_tx["disit"] {
		username_motivation_tx["disit"][motivation] += motivation_tx[motivation]
	}
	for motivation := range username_motivation_rx["disit"] {
		username_motivation_rx["disit"][motivation] += motivation_rx[motivation]
	}

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
		return true
	})

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
	for k, _ := range username_apps {
		users_total[k] = 1
	}

	users_nodered := map[string]int{}
	for k, _ := range username_motivation_tx {
		users_nodered[k] = 1
	}
	for k, _ := range username_motivation_rx {
		users_nodered[k] = 1
	}

	users_dashboards := dashboards
	username_devices := devices

	insertUsersData(len(users_total), len(users_nodered),
		len(users_dashboards), len(username_devices), start, conf)

	for username := range users_total {
		var iot_apps int64 = 0
		iot_apps = username_apps[username]
		reads := iot_reads[username]
		tx := username_motivation_tx[username]
		rx := username_motivation_rx[username]
		if username != "" {
			insertAppData(username, tx, rx, iot_apps, reads, start, conf)
		}
	}

	for username := range dashboards {
		publicDashboards := publicDashboardsPerUser[username]
		privateDashboards := privateDashboardsPerUser[username]
		if username != "" {
			iot_writes_username := iot_writes[username]
			etl_writes_username := etl_writes[username]
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

	for username := range devices {
		publicDevices := publicDevicesPerUser[username]
		privateDevices := privateDevicesPerUser[username]
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

func indexNodeRed(day string, conf map[string]string) {
	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/iot")

	if err != nil {
		panic(err.Error())
	}
	
	defer db.Close()

	start := day + "T00:00:00.000Z"

	end := day + "T23:59:59.999Z"

	tx := 0.0 //getNodeREDTraffic(start, end, "TX", conf)
	rx := 0.0 //getNodeREDTraffic(start, end, "RX", conf)

	num := getNodeREDNumber(start, end, conf)

	stmt, err := db.Prepare(
		"INSERT IGNORE INTO nodered (num, tx, rx, date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE num = ?, tx = ?, rx = ?")

	_, err = stmt.Exec(num, tx,
		rx, start,
		num, tx,
		rx)
	if err != nil {
		panic(err.Error())
	}
}

func getIoTReads(start string, serviceURIsOwners, conf map[string]string) map[string]int64 {
	db, err := sql.Open("mysql", conf["MySQL_ServiceMap_username"]+":"+conf["MySQL_ServiceMap_password"]+"@tcp("+conf["MySQL_ServiceMap_hostname"]+":"+conf["MySQL_ServiceMap_port"]+")/"+conf["MySQL_ServiceMap_database"])

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

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
		err = results.Scan(&num, &serviceURI)
		if err != nil {
			panic(err.Error())
		}
		if serviceURIsOwners[serviceURI] != "" {
			serviceURIs[serviceURIsOwners[serviceURI]] = num
		}
	}
	return serviceURIs
}

func getServiceURIOwners(conf map[string]string) map[string]string {
	db, err := sql.Open("mysql", conf["MySQL_profiledb_username"]+":"+conf["MySQL_profiledb_password"]+"@tcp("+conf["MySQL_profiledb_hostname"]+":"+conf["MySQL_profiledb_port"]+")/"+conf["MySQL_profiledb_database"])

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	serviceURIs := map[string]string{}

	results, err := db.Query("SELECT username, elementurl FROM profiledb.ownership WHERE elementtype = \"IOTID\" " +
		"AND elementurl LIKE \"http://www.disit.org/km4city/resource/iot%\"")
	for results.Next() {
		var username string
		var elementurl string
		err = results.Scan(&username, &elementurl)
		if err != nil {
			panic(err.Error())
		}
		serviceURIs[elementurl] = username
	}
	return serviceURIs
}

func getDashboardsIds(conf map[string]string) []string {

	db, err := sql.Open("mysql", conf["MySQL_Dashboard_username"]+":"+conf["MySQL_Dashboard_password"]+"@tcp("+conf["MySQL_Dashboard_hostname"]+":"+conf["MySQL_Dashboard_port"]+")/"+conf["MySQL_Dashboard_database"])

	if err != nil {
		panic(err.Error())
	}

	defer db.Close()

	ids := []string{}

	results, err := db.Query("SELECT id FROM Dashboard.Config_dashboard WHERE deleted = \"no\"")
	for results.Next() {
		var id string
		err = results.Scan(&id)
		if err != nil {
			panic(err.Error())
		}
		ids = append(ids, id)
	}
	return ids
}

func rangeDateRecentToOld(start, end time.Time) func() time.Time {
	y, m, d := start.Date()
	start = time.Date(y, m, d, 0, 0, 0, 0, time.UTC)
	y, m, d = end.Date()
	end = time.Date(y, m, d, 0, 0, 0, 0, time.UTC)

	return func() time.Time {
		if start.Equal(end) {
			return time.Time{}
		}
		date := start
		start = start.AddDate(0, 0, -1)
		return date
	}
}

func rangeDateOldToRecent(start, end time.Time) func() time.Time {
	y, m, d := start.Date()
	start = time.Date(y, m, d, 0, 0, 0, 0, time.UTC)
	y, m, d = end.Date()
	end = time.Date(y, m, d, 0, 0, 0, 0, time.UTC)

	return func() time.Time {
		if start.Equal(end) {
			return time.Time{}
		}
		date := start
		start = start.AddDate(0, 0, 1)
		return date
	}
}

func main() {
	now := time.Now()

	n := flag.Int("days", 2, "Number of days in the past to start indexing from")

	s := flag.String("startDate", "", "Start date from which perform indexing (excluded), YYYY-mm-dd")

	e := flag.String("endDate", "", "End date from which perform indexing (included), YYYY-mm-dd")

	conf := map[string]string{}
	conf["MySQL_Dashboard_hostname"] = "localhost"
	conf["MySQL_Dashboard_username"] = "user"
	conf["MySQL_Dashboard_password"] = "password"
	conf["MySQL_Dashboard_port"] = "3306"
	conf["MySQL_Dashboard_database"] = "Dashboard"

	conf["MySQL_profiledb_hostname"] = "localhost"
	conf["MySQL_profiledb_username"] = "user"
	conf["MySQL_profiledb_password"] = "password"
	conf["MySQL_profiledb_port"] = "3306"
	conf["MySQL_profiledb_database"] = "profiledb"

	conf["MySQL_ServiceMap_hostname"] = "localhost"
	conf["MySQL_ServiceMap_username"] = "user"
	conf["MySQL_ServiceMap_password"] = "password"
	conf["MySQL_ServiceMap_port"] = "3306"
	conf["MySQL_ServiceMap_database"] = "ServiceMap"

	conf["ElasticSearchIP"] = "elastic"
	conf["ElasticSearchUsername"] = "userstats"
	conf["ElasticSearchPassword"] = "password"

	conf["DataManagerApiUrl"] = "http://localhost:8080/datamanager/"

	conf["DashboardsDailyAccessesApiUrl"] = "http://localhost/dashboardSmartCity/api/dashDailyAccess.php"

	conf["OwnershipApiUrl"] = "localhost"
	conf["OwnershipApiUsername"] = "idpadmin"
	conf["OwnershipApiPassword"] = "password"
	conf["OwnershipApiGrantType"] = "password"

	conf["AccessTokenUrl"] = "http://localhost/auth/realms/master/protocol/openid-connect/token"
	conf["AccessTokenPassword"] = "password"
	conf["AccessTokenUsername"] = "rootmetric"
	conf["AccessTokenClientID"] = "metricscollector"
	conf["AccessTokenGrantType"] = "password"

	conf["SolrUrl"] = "solr"

	c := flag.String("conf", "", "Configuration file path (JSON)")

	flag.Parse()

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

		ElasticSearchIP       string
		ElasticSearchUsername string
		ElasticSearchPassword string

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
	if *c != "" {
		configuration := Configuration{}
		file, err := os.Open(*c)
		defer file.Close()
		decoder := json.NewDecoder(file)
		err = decoder.Decode(&configuration)
		if err == nil {
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
			conf["ElasticSearchUsername"] = configuration.ElasticSearchUsername
			conf["ElasticSearchPassword"] = configuration.ElasticSearchPassword

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

	startDate := now.AddDate(0, 0, -*n)

	endDate := now.AddDate(0, 0, -1)

	if *s != "" {
		layout := "2006-01-02"
		sDate, err := time.Parse(layout, *s)
		if err == nil {
			startDate = sDate
		}
	}

	if *e != "" {
		layout := "2006-01-02"
		eDate, err := time.Parse(layout, *e)
		if err == nil {
			endDate = eDate
		}
	}

	ownerships := getOwnershipsJSON(conf)

	delegations := getDelegationsJSON(conf)

	elementIdUsername := getElementIdUsername(conf)

	dashboardBrokerWidgetDictionary := getDashboardBrokerWidgetDictionary(conf)

	dashboardNodeREDWidgetDictionary := getDashboardNodeREDWidgetDictionary(conf)

	iotid_username := getIoTUsername(conf)

	for rd := rangeDateRecentToOld(endDate, startDate); ; {
		single_date := rd()
		if single_date.IsZero() {
			break
		}
		day := single_date.Format("2006-01-02")
		indexUserData(ownerships, delegations, elementIdUsername, iotid_username,
			dashboardBrokerWidgetDictionary,
			dashboardNodeREDWidgetDictionary, day, conf)
		indexR(day, conf)
		indexETL(day, conf)
		indexNodeRed(day, conf)
	}
}
