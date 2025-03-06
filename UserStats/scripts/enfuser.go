'''
Snap4city -- Enfuser --
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
	"fmt"
	"github.com/fhs/go-netcdf/netcdf"
	_ "github.com/go-sql-driver/mysql"
	"gopkg.in/gomail.v2"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

// insert NetCDF map's data and stats into MySQL
func insertNetCDFMapData(conf map[string]interface{}, fileName string) {
	fmt.Println("Inserting " + fileName)
	// Open NetCDF file in read-only mode. The dataset is returned.
	ds, err := netcdf.OpenFile(fileName, netcdf.NOWRITE)
	if err != nil {
		//return err
	}
	defer ds.Close()
	// get index's name
	var indexName string
	nVars, _ := ds.NVars()
	for i := 0; i < nVars; i++ {
		varn := ds.VarN(i)
		varName, _ := varn.Name()
		if varName != "crs" && varName != "time" && varName != "lat" && varName != "lon" {
			indexName = varName
		}
	}
	time_v, err := ds.Var("time")
	if err != nil {
		//return err
	}
	lat_v, err := ds.Var("lat")
	if err != nil {
		//return err
	}
	lon_v, err := ds.Var("lon")
	if err != nil {
		//return err
	}
	index_v, err := ds.Var(indexName)
	if err != nil {
		//return err
	}
	// Read data from variables
	time_d, err := netcdf.GetInt32s(time_v)
	if err != nil {
		//return err
	}
	lat, err := netcdf.GetFloat32s(lat_v)
	if err != nil {
		//return err
	}
	lon, err := netcdf.GetFloat32s(lon_v)
	if err != nil {
		//return err
	}
	index, err := netcdf.GetFloat32s(index_v)
	if err != nil {
		//return err
	}
	// Get the length of the dimensions of the data.
	index_dims, err := index_v.LenDims()
	if err != nil {
		//return err
	}

	// get map's name and metric from filename
	mapName, metricName := getEnfuserMapNameMetric(fileName)
	// get map's description from binary file
	long_name_attr := index_v.Attr("long_name")
	long_name_length, _ := long_name_attr.Len()
	var description_b = make([]byte, long_name_length)
	long_name_attr.ReadBytes(description_b)
	description := string(description_b)
	// set projection
	projection := "4326"
	// set organization
	org := "Helsinki"

	// get start date
	units_attr := time_v.Attr("units")
	units_length, _ := units_attr.Len()
	var units = make([]byte, units_length)
	units_attr.ReadBytes(units)
	date_s_arr := strings.Split(string(units), "hours since ")
	date_s := date_s_arr[1]
	date_s = strings.Replace(date_s, " ", "T", -1) + "Z"
	layout := "2006-01-02T15:04:05Z"
	t, err := time.Parse(layout, date_s)
	if err != nil {
		panic(err.Error())
	}

	db, err := sql.Open("mysql", conf["MySQL_username"].(string)+":"+conf["MySQL_password"].(string)+"@tcp("+conf["MySQL_hostname"].(string)+":"+conf["MySQL_port"].(string)+")/"+conf["MySQL_database"].(string))

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished executing
	defer db.Close()

	// get map's data from NetCDF file and insert into MySQL
	// prepared statement for data table
	data_stmt, err := db.Prepare("INSERT IGNORE INTO heatmap.data " +
		"(map_name, metric_name, latitude, longitude, value, date) " +
		"VALUES (?,?,?,?,?,?)")
	if err != nil {
		panic(err.Error())
	}
	// prepared statement for metadata table
	metadata_stmt, err := db.Prepare("INSERT IGNORE INTO heatmap.metadata " +
		"(map_name, metric_name, description, org, projection, date) " +
		"VALUES (?,?,?,?,?,?)")
	if err != nil {
		panic(err.Error())
	}
	// prepared statement for maps_completed table
	mapsCompleted_stmt, err := db.Prepare("INSERT IGNORE INTO heatmap.maps_completed " +
		"(map_name, metric_name, completed, date) " +
		"VALUES (?,?,?,?)")
	if err != nil {
		panic(err.Error())
	}
	for time_index := 0; time_index < int(index_dims[0]); time_index++ {
		d := t.Add(time.Hour * time.Duration(time_d[time_index]))
		date := d.Format(layout)
		// insert this map's data only if it is not present in the MySQL database
		if !checkEnfuserMap(conf, mapName, metricName, date) {
			for lat_index := 0; lat_index < int(index_dims[1]); lat_index++ {
				for lon_index := 0; lon_index < int(index_dims[2]); lon_index++ {
					// get latitude, longitude, value and treat them as strings to avoid strange float conversions
					latitude_v := lat[lat_index]
					longitude_v := lon[lon_index]
					value_v := index[time_index*int(index_dims[1])*int(index_dims[2])+lat_index*int(index_dims[2])+lon_index]
					latitude, err := strconv.ParseFloat(fmt.Sprintf("%v", latitude_v), 64)
					longitude, err := strconv.ParseFloat(fmt.Sprintf("%v", longitude_v), 64)
					value, err := strconv.ParseFloat(fmt.Sprintf("%v", value_v), 64)
					_, err = data_stmt.Exec(mapName, metricName, latitude, longitude, value, date)
					if err != nil {
						panic(err.Error())
					}
				}
			}
			// insert map's metadata
			_, err = metadata_stmt.Exec(mapName, metricName, description, org, projection, date)
			// insert map's maps_completed
			_, err = mapsCompleted_stmt.Exec(mapName, metricName, "1", date)
		}
	}
	// insert Enfuser map's stats
	insertEnfuserMapStats(conf, mapName, metricName)
}

// get Enfuser map's name and metric
func getEnfuserMapNameMetric(fileName string) (string, string) {
	b := strings.Split(fileName, "enfuser_helsinki_metropolitan_")
	c := strings.Split(b[1], ".")
	d := strings.Split(c[0], "_")
	metricName := d[0]
	// if this metric name is airQualityPM2 fix it to airQualityPM2_5 (due to split by "_")
	if strings.Contains(metricName, "airQualityPM2") {
		metricName = metricName + "_5"
	}
	// return the metric's name concatenated with "EnfuserHelsinki" with the first character upper case
	return "EnfuserHelsinki" + strings.Title(metricName), metricName
}

// check if an Enfuser map is in the MySQL database
func checkEnfuserMap(conf map[string]interface{}, mapName, metricName, date string) bool {
	db, err := sql.Open("mysql", conf["MySQL_username"].(string)+":"+conf["MySQL_password"].(string)+"@tcp("+conf["MySQL_hostname"].(string)+":"+conf["MySQL_port"].(string)+")/"+conf["MySQL_database"].(string))

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished executing
	defer db.Close()

	results, err := db.Query("SELECT COUNT(*) AS n FROM heatmap.metadata WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		panic(err.Error())
	}

	var n int
	for results.Next() {
		// for each row, scan the result into variables
		err = results.Scan(&n)
		if err != nil {
			panic(err.Error())
		}
	}
	if n > 0 {
		return true
	} else {
		return false
	}
}

// delete map's data older than n days ago
func deleteMapData(conf map[string]interface{}, mapName, days string) {
	db, err := sql.Open("mysql", conf["MySQL_username"].(string)+":"+conf["MySQL_password"].(string)+"@tcp("+conf["MySQL_hostname"].(string)+":"+conf["MySQL_port"].(string)+")/"+conf["MySQL_database"].(string))

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished executing
	defer db.Close()

	// check if there are rows to delete
	results, err := db.Query("SELECT COUNT(*) AS n FROM heatmap.`data` WHERE map_name = '" + mapName + "' AND date(`date`) <= DATE(NOW()) - INTERVAL " + days + " DAY")
	if err != nil {
		panic(err.Error())
	}

	var n int
	for results.Next() {
		// for each row, scan the result into variables
		err = results.Scan(&n)
		if err != nil {
			panic(err.Error())
		}
	}
	// if there are rows then delete
	if n > 0 {
		data_stmt, err := db.Prepare("DELETE FROM heatmap.`data` WHERE map_name = ? AND date(`date`) <= DATE(NOW()) - INTERVAL 3 DAY")
		if err != nil {
			//panic(err.Error())
			return
		}
		fmt.Println("Deleting " + mapName)
		_, err = data_stmt.Exec(mapName)
		/*if err != nil {
			panic(err.Error())
		}*/
	}
}

// check if Enfuser maps in the future are present in the MySQL database
func checkEnfuserFutureMaps(conf map[string]interface{}, mapName, metricName string) bool {
	db, err := sql.Open("mysql", conf["MySQL_username"].(string)+":"+conf["MySQL_password"].(string)+"@tcp("+conf["MySQL_hostname"].(string)+":"+conf["MySQL_port"].(string)+")/"+conf["MySQL_database"].(string))

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished executing
	defer db.Close()

	results, err := db.Query("SELECT UNIX_TIMESTAMP(date) - UNIX_TIMESTAMP(NOW()) AS dateDiff FROM heatmap.maps_completed WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' ORDER BY date DESC LIMIT 1")
	if err != nil {
		panic(err.Error())
	}

	var dateDiff int
	for results.Next() {
		// for each row, scan the result into variables
		err = results.Scan(&dateDiff)
		if err != nil {
			panic(err.Error())
		}
	}
	if dateDiff > 0 {
		return true
	} else {
		return false
	}
}

// insert Enfuser map's stats
func insertEnfuserMapStats(conf map[string]interface{}, mapName, metricName string) {
	db, err := sql.Open("mysql", conf["MySQL_username"].(string)+":"+conf["MySQL_password"].(string)+"@tcp("+conf["MySQL_hostname"].(string)+":"+conf["MySQL_port"].(string)+")/"+conf["MySQL_database"].(string))

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close till after the main function has finished executing
	defer db.Close()

	// get map's stats (using standard MySQL, not GORM)
	var minDate, maxDate string
	var num int64
	results, _ := db.Query("SELECT IFNULL(MIN(date), ''), IFNULL(MAX(date), '') FROM heatmap.`metadata` WHERE map_name = '" + mapName + "'")
	for results.Next() {
		results.Scan(&minDate, &maxDate)
	}
	results, _ = db.Query("SELECT COUNT(DISTINCT(date)) FROM heatmap.`metadata` WHERE map_name = '" + mapName + "'")
	for results.Next() {
		results.Scan(&num)
	}

	// update map's stats table (using standard MySQL, not GORM)
	stmt, _ := db.Prepare("INSERT IGNORE INTO stats (map_name, metric_name, num, min_date, max_date) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE num = ?, min_date = ?, max_date = ?")
	stmt.Exec(mapName, metricName, num, minDate, maxDate, num, minDate, maxDate)
}

// send an email
func sendMail(recipients []string, subject, body string) {
	m := gomail.NewMessage()
	m.SetHeaders(map[string][]string{
		"From":    {m.FormatAddress("info@disit.org", "DISIT")},
		"To":      recipients,
		"Subject": {subject},
	})
	m.SetBody("text/html", body)
	d := gomail.NewDialer("musicnetwork.dinfo.unifi.it", 25, "", "")
	// Send the email
	if err := d.DialAndSend(m); err != nil {
		panic(err)
	}
}

func main() {
	// Settings map
	conf := map[string]interface{}{}
	// Default settings
	// MySQL
	conf["MySQL_hostname"] = "localhost"
	conf["MySQL_username"] = "user"
	conf["MySQL_password"] = "password"
	conf["MySQL_port"] = "3306"
	conf["MySQL_database"] = "heatmap"
	// Enfuser folder
	conf["Enfuser_folder"] = "/mnt/enfuser/1Last_file"
	// Mail
	conf["EmailRecipients"] = []string{"me@mail.org"}

	// Custom settings
	// get c flag command line parameter
	c := flag.String("conf", "", "Configuration file path (JSON)")
	// parse flag
	flag.Parse()
	// don't use lowercase letter in struct members' initial letter, otherwise it does not work
	// https://stackoverflow.com/questions/24837432/golang-capitals-in-struct-fields
	type Configuration struct {
		MySQLHostname   string
		MySQLUsername   string
		MySQLPassword   string
		MySQLPort       string
		MySQLDatabase   string
		EnfuserFolder   string
		EmailRecipients []string
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
			conf["Enfuser_folder"] = configuration.EnfuserFolder
			// Mail
			conf["EmailRecipients"] = configuration.EmailRecipients
		}
	}
	// index Enfuser maps
	metrics := []string{"EnfuserAirQualityIndex", "HighDensityPM10", "HighDensityPM25"}
	for _, metricName := range metrics {
		// read Enfuser binary files from folder
		pattern := conf["Enfuser_folder"].(string) + "/*" + metricName + "*.nc"
		files, err := filepath.Glob(pattern)
		if err != nil {
			fmt.Println(err)
		}
		// insert NetCDF map's data and stats into MySQL
		for _, file := range files {
			insertNetCDFMapData(conf, file)
		}
	}
	// delete old Enfuser maps from MySQL (only data table) and check if there are future maps in the database, otherwise send email
	for _, metricName := range metrics {
		deleteMapData(conf, "EnfuserHelsinki"+metricName, "3")
		if !checkEnfuserFutureMaps(conf, "EnfuserHelsinki"+metricName, metricName) {
			sendMail(conf["EmailRecipients"].([]string), "Enfuser alert", "Future maps missing for: EnfuserHelsinki"+metricName)
		}
	}
	// delete old GRAL maps from MySQL (only data table)
	GRAL_maps := []string{"GRALheatmapHelsinki3mPM", "GRALheatmapHelsinki6mPM"}
	for _, GRAL_map := range GRAL_maps {
		deleteMapData(conf, GRAL_map, "3")
	}
}
