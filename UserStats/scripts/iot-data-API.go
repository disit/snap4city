'''
Snap4city -- IoT Data API --
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
	"log"
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
	fmt.Println("IoT API for Snap4City v" + AppVersion + " - Author: Daniele Cenni, daniele.cenni@unifi.it")
	fmt.Println("DISIT Lab http://www.disit.dinfo.unifi.it - University of Florence ©2018")
	fmt.Println("Error: " + error)
	fmt.Println("Use --help for usage")
	os.Exit(1)
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
		fmt.Println("IoT API for Snap4City v" + AppVersion + " - Author: Daniele Cenni (daniele.cenni@unifi.it)")
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
		fmt.Println("IoT API for Snap4City - Author: Daniele Cenni (daniele.cenni@unifi.it)")
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
		fmt.Println("IoT API for Snap4City - Author: Daniele Cenni (daniele.cenni@unifi.it)")
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
		//rest.Post("/data", i.PostData),
		rest.Get("/data/:username/:date", i.GetData),
		//rest.Put("/data/:id", i.PutData),
		//rest.Delete("/data/:id", i.DeleteData),
	)
	if err != nil {
		log.Fatal(err)
	}
	api.SetApp(router)
	fmt.Print("Starting listening on " + *address + ":" + *port + "\n")
	log.Fatal(http.ListenAndServe(*address+":"+*port, api.MakeHandler()))
}

type Data struct {
	Id                             int64     `json:"id"`
	Username                       string    `json:"username"`
	Iot_db_storage_tx              float64   `json:"iot_db_storage_tx"`
	Iot_db_storage_rx              float64   `json:"iot_db_storage_rx"`
	Iot_filesystem_storage_tx      float64   `json:"iot_filesystem_storage_tx"`
	Iot_filesystem_storage_rx      float64   `json:"iot_filesystem_storage_rx"`
	Iot_db_request_tx              float64   `json:"iot_db_request_tx"`
	Iot_db_request_rx              float64   `json:"iot_db_request_rx"`
	Iot_ascapi_tx                  float64   `json:"iot_ascapi_tx"`
	Iot_ascapi_rx                  float64   `json:"iot_ascapi_rx"`
	Iot_disces_tx                  float64   `json:"iot_disces_tx"`
	Iot_disces_rx                  float64   `json:"iot_disces_rx"`
	Iot_dashboard_tx               float64   `json:"iot_dashboard_tx"`
	Iot_dashboard_rx               float64   `json:"iot_dashboard_rx"`
	Iot_datagate_tx                float64   `json:"iot_datagate_tx"`
	Iot_datagate_rx                float64   `json:"iot_datagate_rx"`
	Iot_external_service_tx        float64   `json:"iot_external_service_tx"`
	Iot_external_service_rx        float64   `json:"iot_external_service_rx"`
	Iot_iot_service_tx             float64   `json:"iot_iot_service_tx"`
	Iot_iot_service_rx             float64   `json:"iot_iot_service_rx"`
	Iot_mapping_tx                 float64   `json:"iot_mapping_tx"`
	Iot_mapping_rx                 float64   `json:"iot_mapping_rx"`
	Iot_microserviceusercreated_tx float64   `json:"iot_microserviceusercreated_tx"`
	Iot_microserviceusercreated_rx float64   `json:"iot_microserviceusercreated_rx"`
	Iot_mydata_tx                  float64   `json:"iot_mydata_tx"`
	Iot_mydata_rx                  float64   `json:"iot_mydata_rx"`
	Iot_notificator_tx             float64   `json:"iot_notificator_tx"`
	Iot_notificator_rx             float64   `json:"iot_notificator_rx"`
	Iot_rstatistics_tx             float64   `json:"iot_rstatistics_tx"`
	Iot_rstatistics_rx             float64   `json:"iot_rstatistics_rx"`
	Iot_sigfox_tx                  float64   `json:"iot_sigfox_tx"`
	Iot_sigfox_rx                  float64   `json:"iot_sigfox_rx"`
	Iot_undefined_tx               float64   `json:"iot_undefined_tx"`
	Iot_undefined_rx               float64   `json:"iot_undefined_rx"`
	Iot_tx                         float64   `json:"iot_tx"`
	Iot_rx                         float64   `json:"iot_rx"`
	Iot_apps                       int64     `json:"iot_apps"`
	Devices_public                 int64     `json:"devices_public"`
	Devices_private                int64     `json:"devices_private"`
	Dashboards_public              int64     `json:"dashboards_public"`
	Dashboards_private             int64     `json:"dashboards_private"`
	Dashboards_accesses            int64     `json:"dashboards_accesses"`
	Dashboards_minutes             int64     `json:"dashboards_accesses"`
	Iot_reads                      int64     `json:"Iot_reads"`
	Iot_writes                     int64     `json:"iot_writes"`
	Etl_writes                     int64     `json:"etl_writes"`
	Date                           time.Time `json:"date"`
}

type Impl struct {
	DB *gorm.DB
}

func (i *Impl) InitDB() {
	var err error
	i.DB, err = gorm.Open("mysql", "user:passw@tcp(localhost:3306)/iot?charset=utf8&parseTime=True")
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

func (i *Impl) GetData(w rest.ResponseWriter, r *rest.Request) {
	username := r.PathParam("username")
	date := r.PathParam("date") + "T00:00:00Z"
	data := Data{}
	if i.DB.Table("data").Where("username = ? AND date = ?", username, date).First(&data).Error != nil {
		rest.NotFound(w, r)
		return
	}
	w.WriteJson(&data)
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

func (i *Impl) DeleteData(w rest.ResponseWriter, r *rest.Request) {
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
}
