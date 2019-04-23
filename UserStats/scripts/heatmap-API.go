// https://gobyexample.com/command-line-flags <- utilizzato
// https://github.com/jessevdk/go-flags <- alternativa
// https://github.com/ant0ine/go-json-rest

/*
Author: Daniele Cenni, daniele.cenni@unifi.it
Heatmap API
example usage heatmap-API --address 0.0.0.0 --port 8080
curl -i http://127.0.0.1:[port]/data/[mapName]/[YYYY-mm-dd]
*/

package main

import (
	"database/sql"
	"flag"
	"fmt"
	"github.com/ant0ine/go-json-rest/rest"
	_ "github.com/go-sql-driver/mysql"
	"github.com/jinzhu/gorm"
	"github.com/jonas-p/go-shp"
	"github.com/paulmach/go.geojson"
	"log"
	"math"
	"net"
	"net/http"
	"os"
	"strconv"
	"time"
)

// @title Heatmap API
// @version 1.0
// @description The Heatmap Server API
// @termsOfService http://swagger.io/terms/

// @contact.name DISIT Lab
// @contact.url http://www.disit.dinfo.unifi.it
// @contact.email info@disit.org

// @license.name GNU Affero General Public License
// @license.url http://www.gnu.org/licenses

// @host localhost:8000
// @BasePath /

// @securityDefinitions.basic BasicAuth

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
	fmt.Println("Heatmap API for Snap4City v" + AppVersion + " - Author: Daniele Cenni, daniele.cenni@unifi.it")
	fmt.Println("DISIT Lab http://www.disit.dinfo.unifi.it - University of Florence ©2018")
	fmt.Println("Error: " + error)
	fmt.Println("Use --help for usage")
	os.Exit(1)
}

// data MySQL table
type Data struct {
	Id        int64     `json:"id"`
	MapName   string    `json:"mapName"`
	Latitude  float64   `json:"latitude"`
	Longitude float64   `json:"longitude"`
	Value     float64   `json:"value"`
	Sum       float64   `json:"sum"`
	Num       float64   `json:"num"`
	Average   float64   `json:"average"`
	Clustered int       `json:"clustered"`
	Date      time.Time `json:"date"`
}

// metadata MySQL table
type Metadata struct {
	Id          int64     `json:"id"`
	MapName     string    `json:"mapName"`
	Clustered   int       `json:"clustered"`
	Description string    `json:"description"`
	Days        int       `json:"days"` // how many days in the past are included with the date (default 0)
	Date        time.Time `json:"date"`
}

// general data structure
type GeneralData struct {
	Id          int64     `json:"id"`
	MapName     string    `json:"mapName"`
	Latitude    float64   `json:"latitude"`
	Longitude   float64   `json:"longitude"`
	Value       float64   `json:"value"`
	Sum         float64   `json:"sum"`
	Num         float64   `json:"num"`
	Average     float64   `json:"average"`
	Clustered   int       `json:"clustered"`
	Description string    `json:"description"`
	Days        int       `json:"days"` // how many days in the past are included with the date (default 0)
	Date        time.Time `json:"date"`
}

type Impl struct {
	DB *gorm.DB
}

func (i *Impl) InitDB() {
	var err error
	i.DB, err = gorm.Open("mysql", "user:passw@tcp(localhost:3306)/heatmap?charset=utf8&parseTime=True")
	if err != nil {
		log.Fatalf("Got error when connect database, the error is '%v'", err)
	}
	i.DB.LogMode(true)
}

// init data and metadata tables
func (i *Impl) InitSchema() {
	i.DB.AutoMigrate(&Data{})
	i.DB.AutoMigrate(&Metadata{})
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
	rows, _ := i.DB.Table("data").Raw("SELECT DISTINCT latitude, longitude FROM heatmap.`data` WHERE map_name = '" + mapName + "' AND date = '" + date + "'").Rows()
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

// GetData godoc
// @Summary Get map data
// @Description Get map data for a particulare date
// @Tags example
// @Accept json
// @Produce json
// @Param val1 query int true "used for calc"
// @Param val2 query int true "used for calc"
// @Success 200 {integer} integer "answer"
// @Failure 400 {string} string "ok"
// @Failure 404 {string} string "ok"
// @Failure 500 {string} string "ok"
// @Router /data/{mapName}/{date}/{clustered} [get]
func (i *Impl) GetData(w rest.ResponseWriter, r *rest.Request) {
	mapName := r.PathParam("mapName")
	date := r.PathParam("date") // + "T00:00:00Z"
	clustered := r.PathParam("clustered")
	//rows, _ := i.DB.Table("data").Raw("SELECT latitude, longitude, IFNULL(value, 0.0) AS value, `sum`, num, average FROM heatmap.`data` WHERE map_name = '" + mapName + "' AND date = '" + date + "' AND clustered = '"+ clustered + "'").Rows()
	// manage if the field "value" is null in the query (IFNULL), otherwise GORM will not get the following fields
	// get data
	data := []map[string]interface{}{}
	// if data to be retrieved is not clustered, then include date for each record
	if clustered == "0" {
		rows1, _ := i.DB.Table("data").Select("latitude, longitude, IFNULL(value, 0.0) AS value, date").Where("map_name = ? AND date(date) = ? AND clustered = ?", mapName, date, clustered).Rows()
		defer rows1.Close()
		for rows1.Next() {
			var latitude, longitude, value float64
			var d string
			rows1.Scan(&latitude, &longitude, &value, &d)
			data = append(data, map[string]interface{}{"latitude": latitude, "longitude": longitude, "value": value, "date": d})
		}
	} else if clustered == "1" { // if data to be retrieved is clustered, then don't include date for each record
		rows1, _ := i.DB.Table("data").Select("latitude, longitude, IFNULL(`sum`, 0.0) AS `sum`, IFNULL(num, 0.0) AS num, IFNULL(average, 0.0) AS average").Where("map_name = ? AND date(date) = ? AND clustered = ?", mapName, date, clustered).Rows()
		defer rows1.Close()
		for rows1.Next() {
			var latitude, longitude, sum, num, average float64
			rows1.Scan(&latitude, &longitude, &sum, &num, &average)
			data = append(data, map[string]interface{}{"latitude": latitude, "longitude": longitude, "sum": sum, "num": num, "average": average})
		}
	}

	// get map's description and days
	metadata := map[string]interface{}{}
	rows2, _ := i.DB.Table("metadata").Select("description, days").Where("map_name = ? AND date(date) = ? AND clustered = ?", mapName, date, clustered).Rows()
	defer rows2.Close()
	for rows2.Next() {
		var description, days string
		rows2.Scan(&description, &days)
		metadata = map[string]interface{}{"mapName": mapName, "date": date, "clustered": clustered, "description": description, "days": days}
	}

	result := map[string]interface{}{"metadata": metadata, "data": data}

	w.WriteJson(result)
}

func (i *Impl) GetList(w rest.ResponseWriter, r *rest.Request) {
	// get data
	data := []map[string]interface{}{}
	// if data to be retrieved is not clustered, then include date for each record

	rows, _ := i.DB.Table("metadata").Select("DISTINCT map_name, clustered, description, days, DATE_FORMAT(date, '%Y-%m-%d') AS date").Rows()
	defer rows.Close()
	for rows.Next() {
		var clustered, days int64
		var map_name, description, date string
		rows.Scan(&map_name, &clustered, &description, &days, &date)
		data = append(data, map[string]interface{}{"map_name": map_name, "clustered": clustered, "description": description, "days": days, "date": date})
	}

	w.WriteJson(data)
}

// https://github.com/jonas-p/go-shp
func createShapeFile(data []map[string]interface{}, metadata map[string]interface{}) string {
	// points to write
	points := []shp.Point{
		shp.Point{10.0, 10.0},
		shp.Point{10.0, 15.0},
		shp.Point{15.0, 15.0},
		shp.Point{15.0, 10.0},
	}

	for _, point := range data {

	}

	// fields to write
	fields := []shp.Field{
		// String attribute field with length 25
		shp.StringField("NAME", 25),
	}

	// create and open a shapefile for writing points
	shape, err := shp.Create("points.shp", shp.POINT)
	if err != nil {
		log.Fatal(err)
	}
	defer shape.Close()

	// setup fields for attributes
	shape.SetFields(fields)

	// write points and attributes
	for n, point := range points {
		shape.Write(&point)

		// write attribute for object n for field 0 (NAME)
		shape.WriteAttribute(n, 0, "Point "+strconv.Itoa(n+1))
	}
}

// https://www.socketloop.com/tutorials/golang-how-to-stream-file-to-client-browser-or-write-to-http-responsewriter
// https://play.golang.org/p/v9IAu2Xu3_
func (i *Impl) DownloadShapeFile(w rest.ResponseWriter, r *rest.Request) {
	mapName := r.PathParam("mapName")
	date := r.PathParam("date") // + "T00:00:00Z"
	clustered := r.PathParam("clustered")
	//rows, _ := i.DB.Table("data").Raw("SELECT latitude, longitude, IFNULL(value, 0.0) AS value, `sum`, num, average FROM heatmap.`data` WHERE map_name = '" + mapName + "' AND date = '" + date + "' AND clustered = '"+ clustered + "'").Rows()
	// manage if the field "value" is null in the query (IFNULL), otherwise GORM will not get the following fields
	// get data
	data := []map[string]interface{}{}
	// if data to be retrieved is not clustered, then include date for each record
	if clustered == "0" {
		rows1, _ := i.DB.Table("data").Select("latitude, longitude, IFNULL(value, 0.0) AS value, date").Where("map_name = ? AND date(date) = ? AND clustered = ?", mapName, date, clustered).Rows()
		defer rows1.Close()
		for rows1.Next() {
			var latitude, longitude, value float64
			var d string
			rows1.Scan(&latitude, &longitude, &value, &d)
			data = append(data, map[string]interface{}{"latitude": latitude, "longitude": longitude, "value": value, "date": d})
		}
	} else if clustered == "1" { // if data to be retrieved is clustered, then don't include date for each record
		rows1, _ := i.DB.Table("data").Select("latitude, longitude, IFNULL(`sum`, 0.0) AS `sum`, IFNULL(num, 0.0) AS num, IFNULL(average, 0.0) AS average").Where("map_name = ? AND date(date) = ? AND clustered = ?", mapName, date, clustered).Rows()
		defer rows1.Close()
		for rows1.Next() {
			var latitude, longitude, sum, num, average float64
			rows1.Scan(&latitude, &longitude, &sum, &num, &average)
			data = append(data, map[string]interface{}{"latitude": latitude, "longitude": longitude, "sum": sum, "num": num, "average": average})
		}
	}

	// get map's description and days
	metadata := map[string]interface{}{}
	rows2, _ := i.DB.Table("metadata").Select("description, days").Where("map_name = ? AND date(date) = ? AND clustered = ?", mapName, date, clustered).Rows()
	defer rows2.Close()
	for rows2.Next() {
		var description, days string
		rows2.Scan(&description, &days)
		metadata = map[string]interface{}{"mapName": mapName, "date": date, "clustered": clustered, "description": description, "days": days}
	}

	createShapeFile(data, metatada)
}

func (i *Impl) PostData(w rest.ResponseWriter, r *rest.Request) {
	// general data
	generalData := GeneralData{}
	if err := r.DecodeJsonPayload(&generalData); err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	// data
	data := Data{
		MapName:   generalData.MapName,
		Latitude:  generalData.Latitude,
		Longitude: generalData.Longitude,
		Value:     generalData.Value,
		Sum:       generalData.Sum,
		Num:       generalData.Num,
		Average:   generalData.Average,
		Clustered: generalData.Clustered,
		Date:      generalData.Date,
	}
	// metadata
	mapName := generalData.MapName
	clustered := generalData.Clustered
	description := generalData.Description
	days := generalData.Days
	date := generalData.Date

	// insert data
	if err := i.DB.Table("data").Save(&data).Error; err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	// insert metadata (using standard MySQL, not GORM)
	db, _ := sql.Open("mysql", "user:passw@tcp(localhost:3306)/heatmap")
	defer db.Close()
	stmt, _ := db.Prepare("INSERT IGNORE INTO metadata (map_name, clustered, description, days, date) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE description = ?, days = ?")
	stmt.Exec(mapName, clustered, description, days, date, description, days)

	w.WriteJson(&data)
}

func (i *Impl) PostDataArray(w rest.ResponseWriter, r *rest.Request) {
	// general data
	generalDataArray := []GeneralData{}
	if err := r.DecodeJsonPayload(&generalDataArray); err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	// setup MySQL connection for inserting metadata
	db, _ := sql.Open("mysql", "user:passw@tcp(localhost:3306)/heatmap")
	defer db.Close()

	for _, generalData := range generalDataArray {
		// data
		data := Data{
			MapName:   generalData.MapName,
			Latitude:  generalData.Latitude,
			Longitude: generalData.Longitude,
			Value:     generalData.Value,
			Sum:       generalData.Sum,
			Num:       generalData.Num,
			Average:   generalData.Average,
			Clustered: generalData.Clustered,
			Date:      generalData.Date,
		}
		// metadata
		mapName := generalData.MapName
		clustered := generalData.Clustered
		description := generalData.Description
		days := generalData.Days
		date := generalData.Date

		// insert data
		if err := i.DB.Table("data").Save(&data).Error; err != nil {
			rest.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		// insert metadata (using standard MySQL, not GORM)
		stmt, _ := db.Prepare("INSERT IGNORE INTO metadata (map_name, clustered, description, days, date) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE description = ?, days = ?")
		stmt.Exec(mapName, clustered, description, days, date, description, days)
	}
	//w.WriteJson(&data)
}

func (i *Impl) PostUpdateData(w rest.ResponseWriter, r *rest.Request) {
	generalData := GeneralData{}
	if err := r.DecodeJsonPayload(&generalData); err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	// data
	data := Data{
		MapName:   generalData.MapName,
		Latitude:  generalData.Latitude,
		Longitude: generalData.Longitude,
		Value:     generalData.Value,
		Sum:       generalData.Sum,
		Num:       generalData.Num,
		Average:   generalData.Average,
		Clustered: generalData.Clustered,
		Date:      generalData.Date,
	}
	// metadata
	mapName := generalData.MapName
	latitude := generalData.Latitude
	longitude := generalData.Longitude
	clustered := generalData.Clustered
	description := generalData.Description
	days := generalData.Days
	date := generalData.Date

	// update data
	if err := i.DB.Table("data").Where("date = ? AND map_name = ? AND latitude = ? AND longitude = ?", date, mapName, latitude, longitude).Update(&data).Error; err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// update metadata (using standard MySQL, not GORM)
	db, _ := sql.Open("mysql", "user:passw@tcp(localhost:3306)/heatmap")
	defer db.Close()
	stmt, _ := db.Prepare("UPDATE metadata SET description = ?, days = ? WHERE map_name = ? AND clustered = ? AND date = ?")
	stmt.Exec(description, days, mapName, clustered, date)

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
	// json object to delete
	data := Data{}
	// json object deleted
	d := Data{}
	if err := r.DecodeJsonPayload(&data); err != nil {
		rest.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	//if err := i.DB.Table("data").Where("date = ? AND map_name = ? AND latitude = ? AND longitude = ?", date, map_name, latitude, longitude).Delete(&data).Error; err != nil {
	if err := i.DB.Table("data").Find(&d, &data).Delete(&data).Error; err != nil {
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
		fmt.Println("Heatmap API for Snap4City v" + AppVersion + " - Author: Daniele Cenni (daniele.cenni@unifi.it)")
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

	// CORS
	/*api.Use(&rest.CorsMiddleware{
		RejectNonCorsRequests: false,
		OriginValidator: func(origin string, request *rest.Request) bool {
			return origin == "http://my.other.host"
		},
		AllowedMethods: []string{"GET", "POST", "PUT", "DELETE"},
		AllowedHeaders: []string{
			"Accept", "Content-Type", "X-Custom-Header", "Origin"},
		AccessControlAllowCredentials: true,
		AccessControlMaxAge:           3600,
	})*/

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
		rest.Get("/list", i.GetList),
		rest.Post("/insert", i.PostData),
		rest.Post("/insertArray", i.PostDataArray),
		rest.Post("/update", i.PostUpdateData),
		rest.Get("/data/:mapName/:date/:clustered", i.GetData),
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
