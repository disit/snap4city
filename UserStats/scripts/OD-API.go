'''
Snap4city -- OD API --
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
	"flag"
	"fmt"
	"github.com/ant0ine/go-json-rest/rest"
	_ "github.com/go-sql-driver/mysql"
	"github.com/jinzhu/gorm"
	"github.com/paulmach/go.geojson"
	"log"
	"math"
	"net"
	"net/http"
	"os"
	"strconv"
	"time"
)

const AppVersion = "1.0.0"

// check if a string is a number
func is_number(s string) bool {
	if _, err := strconv.ParseFloat(s, 64); err == nil {
		return true
	} else {
		return false
	}
}

func printError(error string) {
	fmt.Println("OD matrix API for Snap4City v" + AppVersion + " - Author: Daniele Cenni, daniele.cenni@unifi.it")
	fmt.Println("DISIT Lab http://www.disit.dinfo.unifi.it - University of Florence ©2018")
	fmt.Println("Error: " + error)
	fmt.Println("Use --help for usage")
	os.Exit(1)
}

type Data struct {
	Id            int64     `json:"id"`
	MapName       string    `json:"mapName"`
	LatitudeFrom  float64   `json:"latitudeFrom"`
	LongitudeFrom float64   `json:"longitudeFrom"`
	LatitudeTo    float64   `json:"latitudeTo"`
	LongitudeTo   float64   `json:"longitudeTo"`
	Num           int64     `json:"num"`
	Date          time.Time `json:"date"`
}

type Impl struct {
	DB *gorm.DB
}

func (i *Impl) InitDB() {
	var err error
	i.DB, err = gorm.Open("mysql", "user:passw@tcp(localhost:3306)/od?charset=utf8&parseTime=True")
	if err != nil {
		log.Fatalf("Got error when connect database, the error is '%v'", err)
	}
	i.DB.LogMode(true)
}

func (i *Impl) InitSchema() {
	i.DB.AutoMigrate(&Data{})
}

func (i *Impl) GetAllData(w rest.ResponseWriter, r *rest.Request) {
	data := []Data{}
	i.DB.Table("data").Find(&data)
	w.WriteJson(&data)
}

func (i *Impl) GetGeoJsonData(w rest.ResponseWriter, r *rest.Request) {
	mapName := r.PathParam("mapName")
	date := r.PathParam("date") + "T00:00:00Z"

	// set a GeoJSON feature collection
	fc := geojson.NewFeatureCollection()

	// perform the select
	rows, _ := i.DB.Table("data").Raw("SELECT DISTINCT * FROM (SELECT DISTINCT latitude_from AS latitude, longitude_from AS longitude FROM od.`data` WHERE map_name = '" + mapName + "' AND date = '" + date + "' UNION SELECT DISTINCT latitude_to AS latitude, longitude_to AS longitude FROM od.`data` WHERE map_name = '" + mapName + "' AND date = '" + date + "') AS a").Rows()
	defer rows.Close()

	// scan the results
	for rows.Next() {
		var latitude, longitude float64
		rows.Scan(&latitude, &longitude)

		// build the point GeoJSON feature
		feature := geojson.NewPointFeature([]float64{longitude, latitude})
		feature.ID = getPointId(latitude, longitude)
		feature.Properties["LAT"] = latitude
		feature.Properties["LON"] = longitude

		// add the feature to the GeoJSON feature collection
		fc.AddFeature(feature)
	}

	// return the GeoJSON
	w.WriteJson(fc)
}

func (i *Impl) GetFlowsData(w rest.ResponseWriter, r *rest.Request) {
	mapName := r.PathParam("mapName")
	date := r.PathParam("date") + "T00:00:00Z"
	//rows, _ := i.DB.Table("data").Raw("SELECT SUM(num) AS total, latitude_from, longitude_from, latitude_to, longitude_to FROM od.`data` WHERE map_name = '" + mapName + "' AND date = '" + date + "' GROUP BY latitude_from, longitude_from, latitude_to, longitude_to").Rows()
	rows, _ := i.DB.Table("data").Select("SUM(num) AS total, latitude_from, longitude_from, latitude_to, longitude_to").Where("map_name = ? AND date = ?", mapName, date).Group("latitude_from, longitude_from, latitude_to, longitude_to").Rows()
	defer rows.Close()
	result := []string{}
	for rows.Next() {
		var latitude_from, longitude_from, latitude_to, longitude_to float64
		var total string
		rows.Scan(&total, &latitude_from, &longitude_from, &latitude_to, &longitude_to)
		result = append(result, getPointId(latitude_to, longitude_to)+","+getPointId(latitude_from, longitude_from)+","+total)
	}
	w.WriteJson(result)
}

func (i *Impl) PostData(w rest.ResponseWriter, r *rest.Request) {
	data := Data{}
	if err := r.DecodeJsonPayload(&data); err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	if err := i.DB.Table("data").Save(&data).Error; err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteJson(&data)
}

func (i *Impl) PostUpdateData(w rest.ResponseWriter, r *rest.Request) {
	data := Data{}
	if err := r.DecodeJsonPayload(&data); err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	date := data.Date
	map_name := data.MapName
	latitude_from := data.LatitudeFrom
	longitude_from := data.LongitudeFrom
	latitude_to := data.LatitudeTo
	longitude_to := data.LongitudeTo
	if err := i.DB.Table("data").Where("date = ? AND map_name = ? AND latitude_from = ? AND longitude_from = ?  AND latitude_to = ? AND longitude_to = ?", date, map_name, latitude_from, longitude_from, latitude_to, longitude_to).Update(&data).Error; err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteJson(&data)
}

func (i *Impl) PutData(w rest.ResponseWriter, r *rest.Request) {
	id := r.PathParam("id")
	data := Data{}
	if i.DB.Table("data").First(&data, id).Error != nil {
		rest.NotFound(w, r)
		return
	}

	updated := Data{}
	if err := r.DecodeJsonPayload(&updated); err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	data.Date = updated.Date

	if err := i.DB.Table("data").Save(&data).Error; err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteJson(&data)
}

/*func (i *Impl) DeleteData(w rest.ResponseWriter, r *rest.Request) {
	id := r.PathParam("id")
	data := Data{}
	if i.DB.Table("data").First(&data, id).Error != nil {
		rest.NotFound(w, r)
		return
	}
	if err := i.DB.Table("data").Delete(&data).Error; err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)
}*/

func (i *Impl) DeleteData(w rest.ResponseWriter, r *rest.Request) {
	data := Data{}
	if err := r.DecodeJsonPayload(&data); err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	/*date := data.Date
	map_name := data.MapName
	latitude_from := data.LatitudeFrom
	longitude_from := data.LongitudeFrom
	latitude_to := data.LatitudeTo
	longitude_to := data.LongitudeTo*/
	//if err := i.DB.Table("data").Where("date = ? AND map_name = ? AND latitude_from = ? AND longitude_from = ?  AND latitude_to = ? AND longitude_to = ?", date, map_name, latitude_from, longitude_from, latitude_to, longitude_to).Delete(&data).Error; err != nil {
	if err := i.DB.Table("data").Find(&data).Delete(&data).Error; err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteJson(&data)
}

// get geoJSON of a point (latitude, longitude)
func getGeoJSON(id string, latitude, longitude float64) string {
	fc := geojson.NewFeatureCollection()
	feature := geojson.NewPointFeature([]float64{longitude, latitude})
	feature.ID = id
	feature.Properties["LAT"] = latitude
	feature.Properties["LON"] = longitude
	fc.AddFeature(feature)
	rawJSON, _ := fc.MarshalJSON()
	return string(rawJSON)
}

// get the id of coordinates
func getPointId(latitude, longitude float64) string {
	// get latitude as hex
	lat_hex := fmt.Sprintf("%x", math.Float64bits(latitude))
	// get longitude as hex
	lon_hex := fmt.Sprintf("%x", math.Float64bits(longitude))

	return lat_hex + "#" + lon_hex
}

func main() {
	// setup the command line flags
	address := flag.String("address", "127.0.0.1", "binding address")
	port := flag.String("port", "8080", "listening port")
	username := flag.String("username", "", "username for Basic Authentication")
	password := flag.String("password", "", "password for Basic Authentication")
	version := flag.Bool("version", false, "prints current version")

	// override standard usage message
	f := flag.Usage
	flag.Usage = func() {
		fmt.Println("OD matrix API for Snap4City v" + AppVersion + " - Author: Daniele Cenni (daniele.cenni@unifi.it)")
		fmt.Println("DISIT Lab http://www.disit.dinfo.unifi.it - University of Florence ©2018")
		fmt.Println("")
		f()
	}

	flag.Parse()

	// if the user specifies localhost for address, then use 127.0.0.1
	if *address == "localhost" {
		*address = "127.0.0.1"
	}
	// check for validity of IP address
	if net.ParseIP(*address) == nil {
		printError("you must specify a valid IP address")
	}
	// check for validity of TCP port
	if is_number(*port) {
		i, err := strconv.ParseInt(*port, 10, 64)
		if err == nil && i < 0 || i > 65535 {
			printError("you must specify a valid TCP port [0-65535]")
		}
	}

	// print app version
	if *version {
		fmt.Println(AppVersion)
		os.Exit(0)
	}

	/*if flag.NArg() == 0 {
		fmt.Println("OD matrix API for Snap4City - Author: Daniele Cenni (daniele.cenni@unifi.it)")
		fmt.Println("DISIT Lab - University of Florence ©2018")
		os.Exit(1)
	}

	// get port from command line
	// args with program
	//args := os.Args
	// args without program
	args := os.Args[1:]
	address := ""
	if len(args) == 0 || !is_number(args[0]) {
		fmt.Println("OD matrix API for Snap4City - Author: Daniele Cenni (daniele.cenni@unifi.it)")
		fmt.Println("DISIT Lab - University of Florence ©2018")
		fmt.Println("Error: you must specify a listening port")
		os.Exit(1)
	}
	// if the bind address is specified set it
	if len(args) == 2 {
		address = args[1]
	}
	port := args[0]*/

	i := Impl{}
	i.InitDB()
	i.InitSchema()

	api := rest.NewApi()
	api.Use(rest.DefaultDevStack...)

	// if username and password are defined then use Basic Authentication
	if *username != "" && *password != "" {
		api.Use(&rest.AuthBasicMiddleware{
			Realm: "DISIT",
			Authenticator: func(usr string, pwd string) bool {
				if usr == *username && pwd == *password {
					return true
				}
				return false
			},
		})
	}

	router, err := rest.MakeRouter(
		rest.Get("/data", i.GetAllData),
		rest.Post("/insert", i.PostData),
		rest.Post("/update", i.PostUpdateData),
		rest.Get("/data/geoJSON/:mapName/:date", i.GetGeoJsonData),
		rest.Get("/data/flows/:mapName/:date", i.GetFlowsData),
		rest.Delete("/delete", i.DeleteData),
		//rest.Put("/data/:id", i.PutData),
	)
	if err != nil {
		log.Fatal(err)
	}
	api.SetApp(router)
	fmt.Print("Starting listening on " + *address + ":" + *port + "\n")
	log.Fatal(http.ListenAndServe(*address+":"+*port, api.MakeHandler()))
}
