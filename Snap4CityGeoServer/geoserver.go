/*
Snap4city -- GeoServer API --
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
*/

package main

import (
	"archive/zip"
	"bytes"
	"database/sql"
	"encoding/binary"
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"github.com/StefanSchroeder/Golang-Ellipsoid/ellipsoid"
	_ "github.com/go-sql-driver/mysql"
	"github.com/lukeroth/gdal"
	"github.com/tidwall/gjson"
	"bufio"
	"github.com/fhs/go-netcdf/netcdf"
	"github.com/im7mortal/UTM"
	"io"
	"io/ioutil"
	"log"
	"math"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"time"
	"unicode"
)

// get RGB color from name NOT USED
func getRGB(colorName string) (int, int, int) {
	if colorName == "blue" {
		return 0, 0, 255
	} else if colorName == "cyan" {
		return 0, 153, 255
	} else if colorName == "green" {
		return 0, 153, 0
	} else if colorName == "darkgreen" {
		return 0, 95, 0
	} else if colorName == "yellowgreen" {
		return 0, 255, 0
	} else if colorName == "yellow" {
		return 255, 255, 0
	} else if colorName == "gold" {
		return 255, 187, 0
	} else if colorName == "orange" {
		return 255, 102, 0
	} else if colorName == "red" {
		return 255, 0, 0
	} else if colorName == "darkred" {
		return 153, 0, 0
	} else if colorName == "maroon" {
		return 84, 0, 0
	} else {
		return -1, -1, -1
	}
}

// get color NOT USED
func getColorOld(conf map[string]string, metricName string, value float64) (int, int, int) {
	if metricName == "noiseLAeq" || metricName == "noiseLA" ||
		metricName == "noiseLAmax" || metricName == "LAeq" {
		if value < 44.3 {
			return getRGB("blue")
		} else if value >= 44.3 && value <= 48.8 {
			return getRGB("cyan")
		} else if value > 48.8 && value <= 53.3 {
			return getRGB("green")
		} else if value > 53.3 && value <= 57.7 {
			return getRGB("yellowgreen")
		} else if value > 57.7 && value <= 62.1 {
			return getRGB("yellow")
		} else if value > 62.1 && value <= 66.6 {
			return getRGB("gold")
		} else if value > 66.6 && value <= 71 {
			return getRGB("orange")
		} else if value > 71 && value <= 75.5 {
			return getRGB("red")
		} else if value > 75.5 && value <= 79.9 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "airTemperature" || metricName == "minTemperature" ||
		metricName == "maxTemperature" || metricName == "minGroundTemperature" {
		if value < -20 {
			return getRGB("blue")
		} else if value >= -20 && value <= 0 {
			return getRGB("cyan")
		} else if value > 0 && value <= 9 {
			return getRGB("green")
		} else if value > 9 && value <= 15 {
			return getRGB("yellowgreen")
		} else if value > 15 && value <= 18 {
			return getRGB("yellow")
		} else if value > 18 && value <= 21 {
			return getRGB("gold")
		} else if value > 21 && value <= 25 {
			return getRGB("orange")
		} else if value > 25 && value <= 30 {
			return getRGB("red")
		} else if value > 30 && value <= 34 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "airHumidity" {
		if value < 40 {
			return getRGB("blue")
		} else if value >= 40 && value <= 45.5 {
			return getRGB("cyan")
		} else if value > 45.5 && value <= 51.1 {
			return getRGB("green")
		} else if value > 51.1 && value <= 56.7 {
			return getRGB("yellowgreen")
		} else if value > 56.7 && value <= 62.2 {
			return getRGB("yellow")
		} else if value > 62.2 && value <= 67.8 {
			return getRGB("gold")
		} else if value > 67.8 && value <= 73.3 {
			return getRGB("orange")
		} else if value > 73.3 && value <= 78.9 {
			return getRGB("red")
		} else if value > 78.9 && value <= 84.4 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "windSpeed" {
		if value <= 3.9 {
			return getRGB("blue")
		} else if value > 3.9 && value <= 7.9 {
			return getRGB("cyan")
		} else if value > 7.9 && value <= 11.9 {
			return getRGB("green")
		} else if value > 11.9 && value <= 15.9 {
			return getRGB("yellowgreen")
		} else if value > 15.9 && value <= 19.9 {
			return getRGB("yellow")
		} else if value > 19.9 && value <= 23.9 {
			return getRGB("gold")
		} else if value > 23.9 && value <= 27.9 {
			return getRGB("orange")
		} else if value > 27.9 && value <= 31.9 {
			return getRGB("red")
		} else if value > 31.9 && value <= 35.9 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "windGust" {
		if value <= 3.32 {
			return getRGB("blue")
		} else if value > 3.32 && value <= 6.66 {
			return getRGB("cyan")
		} else if value > 6.66 && value <= 9.99 {
			return getRGB("green")
		} else if value > 9.99 && value <= 13.32 {
			return getRGB("yellowgreen")
		} else if value > 13.32 && value <= 16.66 {
			return getRGB("yellow")
		} else if value > 16.66 && value <= 19.99 {
			return getRGB("gold")
		} else if value > 19.99 && value <= 23.32 {
			return getRGB("orange")
		} else if value > 23.32 && value <= 26.66 {
			return getRGB("red")
		} else if value > 26.66 && value <= 30 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "dewPoint" {
		if value < -10 {
			return getRGB("blue")
		} else if value >= -10 && value <= -7.99 {
			return getRGB("cyan")
		} else if value > -7.99 && value <= -5.99 {
			return getRGB("green")
		} else if value > -5.99 && value <= -3.99 {
			return getRGB("yellowgreen")
		} else if value > -3.99 && value <= -1.99 {
			return getRGB("yellow")
		} else if value > -1.99 && value <= -0.01 {
			return getRGB("gold")
		} else if value > -0.01 && value <= 1.99 {
			return getRGB("orange")
		} else if value > 1.99 && value <= 3.99 {
			return getRGB("red")
		} else if value > 3.99 && value <= 5.99 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "airQualityAQI" || metricName == "airqualityAQI" {
		if int(value) == 0 {
			return getRGB("blue")
		} else if int(value) == 1 {
			return getRGB("cyan")
		} else if int(value) == 2 {
			return getRGB("green")
		} else if int(value) == 3 {
			return getRGB("yellowgreen")
		} else if int(value) == 4 {
			return getRGB("yellow")
		} else if int(value) == 5 {
			return getRGB("gold")
		} else if int(value) == 6 {
			return getRGB("orange")
		} else if int(value) == 7 {
			return getRGB("red")
		} else if int(value) == 8 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "airQualityPM10" || metricName == "PM10" {
		if value <= 10 {
			return getRGB("blue")
		} else if value > 10 && value <= 20 {
			return getRGB("cyan")
		} else if value > 20 && value <= 30 {
			return getRGB("green")
		} else if value > 30 && value <= 40 {
			return getRGB("yellowgreen")
		} else if value > 40 && value <= 50 {
			return getRGB("yellow")
		} else if value > 50 && value <= 60 {
			return getRGB("gold")
		} else if value > 60 && value <= 70 {
			return getRGB("orange")
		} else if value > 70 && value <= 80 {
			return getRGB("red")
		} else if value > 80 && value <= 90 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "airQualityNO2" || metricName == "NO2" {
		if value <= 20 {
			return getRGB("blue")
		} else if value > 20 && value <= 50 {
			return getRGB("cyan")
		} else if value > 50 && value <= 70 {
			return getRGB("green")
		} else if value > 70 && value <= 120 {
			return getRGB("yellowgreen")
		} else if value > 120 && value <= 150 {
			return getRGB("yellow")
		} else if value > 150 && value <= 180 {
			return getRGB("gold")
		} else if value > 180 && value <= 200 {
			return getRGB("orange")
		} else if value > 200 && value <= 250 {
			return getRGB("red")
		} else if value > 250 && value <= 300 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "airQualityPM2_5" || metricName == "PM2_5" {
		if value <= 5 {
			return getRGB("blue")
		} else if value > 5 && value <= 10 {
			return getRGB("cyan")
		} else if value > 10 && value <= 15 {
			return getRGB("green")
		} else if value > 15 && value <= 25 {
			return getRGB("yellowgreen")
		} else if value > 25 && value <= 35 {
			return getRGB("yellow")
		} else if value > 35 && value <= 40 {
			return getRGB("gold")
		} else if value > 40 && value <= 50 {
			return getRGB("orange")
		} else if value > 50 && value <= 60 {
			return getRGB("red")
		} else if value > 60 && value <= 70 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "safetyOnBikeDensity" || metricName == "bikeSafety" {
		if int(value) <= -7 {
			return getRGB("red")
		} else if int(value) >= -6 && int(value) <= -4 {
			return getRGB("orange")
		} else if int(value) >= -3 && int(value) <= -1 {
			return getRGB("gold")
		} else if int(value) == 0 {
			return getRGB("yellow")
		} else if int(value) > 1 && int(value) <= 3 {
			return getRGB("yellowgreen")
		} else if int(value) > 4 && int(value) <= 6 {
			return getRGB("green")
		} else {
			return getRGB("darkgreen")
		}
	} else if metricName == "accidentDensity" {
		if value == 1 {
			return getRGB("yellowgreen")
		} else if int(value) >= 2 && int(value) <= 3 {
			return getRGB("yellow")
		} else if int(value) >= 4 && int(value) <= 5 {
			return getRGB("gold")
		} else if int(value) >= 6 && int(value) <= 7 {
			return getRGB("orange")
		} else if int(value) >= 8 && int(value) <= 9 {
			return getRGB("red")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "airQualityNOx" {
		if value <= 10 {
			return getRGB("blue")
		} else if value > 10 && value <= 25 {
			return getRGB("cyan")
		} else if value > 25 && value <= 35 {
			return getRGB("green")
		} else if value > 35 && value <= 60 {
			return getRGB("yellowgreen")
		} else if value > 60 && value <= 75 {
			return getRGB("yellow")
		} else if value > 75 && value <= 90 {
			return getRGB("gold")
		} else if value > 90 && value <= 105 {
			return getRGB("orange")
		} else if value > 105 && value <= 125 {
			return getRGB("red")
		} else if value > 125 && value <= 150 {
			return getRGB("darkred")
		} else {
			return getRGB("maroon")
		}
	} else if metricName == "CAQI" {
		if value <= 25 {
			return getRGB("yellowgreen")
		} else if value > 25 && value <= 50 {
			return getRGB("yellow")
		} else if value > 50 && value <= 75 {
			return getRGB("gold")
		} else if value > 75 && value <= 100 {
			return getRGB("orange")
		} else {
			return getRGB("darkred")
		}
	} else if metricName == "EAQI" {
		if value == 1 {
			return getRGB("green")
		} else if value == 2 {
			return getRGB("yellowgreen")
		} else if value == 3 {
			return getRGB("yellow")
		} else if value == 4 {
			return getRGB("orange")
		} else {
			return getRGB("darkred")
		}
	} else if metricName == "CO" {
		if value < 1.9 {
			return getRGB("green")
		} else if value >= 1.9 && value <= 3.9 {
			return getRGB("yellowgreen")
		} else if value > 3.9 && value <= 5.9 {
			return getRGB("yellow")
		} else if value > 5.9 && value <= 7.9 {
			return getRGB("gold")
		} else if value > 7.9 && value <= 10 {
			return getRGB("orange")
		} else {
			return getRGB("red")
		}
	} else if metricName == "Benzene" {
		if value < 0.9 {
			return getRGB("green")
		} else if value >= 0.9 && value <= 1.9 {
			return getRGB("yellowgreen")
		} else if value > 1.9 && value <= 2.9 {
			return getRGB("yellow")
		} else if value > 2.9 && value <= 3.9 {
			return getRGB("gold")
		} else if value > 3.9 && value <= 5 {
			return getRGB("orange")
		} else {
			return getRGB("red")
		}
	} else {
		log.Println("Color table not found for metric: " + metricName)
		return -1, -1, -1
	}
}

// get color
func getColor(colorMap map[string]map[int]map[string]interface{}, metricName string, value float64) (int, int, int) {
	var max, min float64
	var rgb []int
	for order := 1; order <= len(colorMap[metricName]); order++ {
		if _, ok := colorMap[metricName][order]; ok {
			max_bool := false
			min_bool := false
			if val, ok := colorMap[metricName][order]["max"]; ok {
				max_bool = true
				max = val.(float64)
			}
			if val, ok := colorMap[metricName][order]["min"]; ok {
				min_bool = true
				min = val.(float64)
			}
			rgb = colorMap[metricName][order]["rgb"].([]int)
			if !min_bool && max_bool {
				if value < max {
					return rgb[0], rgb[1], rgb[2]
				}
			} else if !max_bool && min_bool {
				if value >= min {
					return rgb[0], rgb[1], rgb[2]
				}
			} else {
				if value >= min && value < max {
					return rgb[0], rgb[1], rgb[2]
				}
			}
		}
	}
	return -1, -1, -1
}

// get colors' maps from MySQL
func getColorsMaps(conf map[string]string) map[string]map[int]map[string]interface{} {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	// if there is an error opening the connection, handle it
	if err != nil {
		log.Panic(err.Error())
	}

	// defer the close till after the main function has finished
	defer db.Close()

	// get map's colors
	results, err := db.Query("SELECT metric_name, `min`, `max`, `order`, rgb FROM heatmap.colors")
	if err != nil {
		log.Panic(err.Error())
	}
	var metricName, min_b, max_b, order_b, rgb_b []byte
	result := map[string]map[int]map[string]interface{}{}
	for results.Next() {
		// for each row, scan the result into variables
		err = results.Scan(&metricName, &min_b, &max_b, &order_b, &rgb_b)
		min, err_min := strconv.ParseFloat(string(min_b), 64)
		max, err_max := strconv.ParseFloat(string(max_b), 64)
		order, err_order := strconv.Atoi(string(order_b))
		var rgb []int
		rgb_err := json.Unmarshal(rgb_b, &rgb)
		if err_order == nil {
			if _, ok := result[string(metricName)]; !ok {
				result[string(metricName)] = map[int]map[string]interface{}{}
			}
			if _, ok := result[string(metricName)][order]; !ok {
				result[string(metricName)][order] = map[string]interface{}{}
			}
			if err_min == nil {
				result[string(metricName)][order]["min"] = min
			}
			if err_max == nil {
				result[string(metricName)][order]["max"] = max
			}
			if rgb_err == nil {
				result[string(metricName)][order]["rgb"] = rgb
			}
		}
	}
	return result
}

// get destination point given distance (m) and bearing (clockwise from north) from start point
func getDestinationPoint(latitude, longitude, distance, bearing float64) (float64, float64) {
	var radius float64 = 6371000

	delta := distance / radius
	theta := bearing * math.Pi / 180

	fi1 := latitude * math.Pi / 180
	lambda1 := longitude * math.Pi / 180

	sinfi1 := math.Sin(fi1)
	cosfi1 := math.Cos(fi1)
	sindelta := math.Sin(delta)
	cosdelta := math.Cos(delta)
	sintheta := math.Sin(theta)
	costheta := math.Cos(theta)

	sinfi2 := sinfi1*cosdelta + cosfi1*sindelta*costheta
	fi2 := math.Asin(sinfi2)
	y := sintheta * sindelta * cosfi1
	x := cosdelta - sinfi1*sinfi2
	lambda2 := lambda1 + math.Atan2(y, x)

	return fi2 * 180 / math.Pi, lambda2 * 180 / math.Pi
}

// get bounding coordinates (decimal latitude and longitude)
func getBoundingCoordinates(lat_center, lon_center, bboxLengthX, bboxLengthY float64, deltaLatLon bool) (float64, float64, float64, float64) {
	if deltaLatLon {
		return lat_center - bboxLengthX/2, lat_center + bboxLengthX/2, lon_center - bboxLengthY/2, lon_center + bboxLengthY/2
	} else {
		geo1 := ellipsoid.Init("WGS84", ellipsoid.Degrees, ellipsoid.Meter, ellipsoid.LongitudeIsSymmetric, ellipsoid.BearingIsSymmetric)
		minLat, _ := geo1.At(lat_center, lon_center, bboxLengthY/2, 180)
		maxLat, _ := geo1.At(lat_center, lon_center, bboxLengthY/2, 0)
		_, minLon := geo1.At(lat_center, lon_center, bboxLengthX/2, -90)
		_, maxLon := geo1.At(lat_center, lon_center, bboxLengthX/2, 90)

		return minLat, maxLat, minLon, maxLon
	}
}

// save a GeoTIFF to disk
func saveGeoTIFF(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, mapName, metricName string, latitude, longitude, value float64, date string, bboxLengthX, bboxLengthY float64, deltaLatLon bool) bool {
        defer func() {
          if r := recover(); r != nil {
             log.Println("saveGeoTIFF Recovered", r)
          }
        }()
	dateString := strings.Replace(date, ":", "", -1)
	dateString = strings.Replace(dateString, "-", "", -1)
	dateString = strings.Replace(dateString, " ", "T", -1) + "Z"

	latitude_s := fmt.Sprintf("%v", latitude)
	longitude_s := fmt.Sprintf("%v", longitude)
	fileName := filePath + "/" + strings.Replace(latitude_s, ".", "-", -1) + "_" + strings.Replace(longitude_s, ".", "-", -1) + "_" + dateString + ".tiff"

	nx := 1
	ny := 1

	r, g, b := getColor(colorMap, metricName, value)

	if r == -1 {
		log.Println("error getting color for metric", metricName)
		return false
	}

	var minLat, maxLat, minLon, maxLon float64

	minLat, maxLat, minLon, maxLon = getBoundingCoordinates(latitude, longitude, bboxLengthX, bboxLengthY, deltaLatLon)

	r_pixels := make([]uint8, ny*nx)
	g_pixels := make([]uint8, ny*nx)
	b_pixels := make([]uint8, ny*nx)
	alpha := make([]uint8, ny*nx)
	for index, _ := range alpha {
		alpha[index] = 255
	}

	for x := 0; x < nx; x++ {
		for y := 0; y < ny; y++ {
			loc := x + y*nx

			r_pixels[loc] = uint8(r)
			g_pixels[loc] = uint8(g)
			b_pixels[loc] = uint8(b)
		}
	}

	xmin, ymin, xmax, ymax := minLon, minLat, maxLon, maxLat
	xres := (xmax - xmin) / float64(nx)
	yres := (ymax - ymin) / float64(ny)
	geotransform := [6]float64{xmin - xres/2, xres, 0, ymax + yres/2, 0, -yres}

	driver, err := gdal.GetDriverByName("GTiff")
	if err != nil {
		log.Println(err)
		return false
	}
	dst_ds := driver.Create(fileName, ny, nx, 4, gdal.Byte, nil)
	defer dst_ds.Close()

	spatialRef := gdal.CreateSpatialReference("") 
	spatialRef.FromEPSG(4326)                     
	srString, err := spatialRef.ToWKT()
	dst_ds.SetProjection(srString)       
	dst_ds.SetGeoTransform(geotransform) 

	r_band := dst_ds.RasterBand(1)
	r_band.IO(gdal.Write, 0, 0, nx, ny, r_pixels, nx, ny, 0, 0)
	g_band := dst_ds.RasterBand(2)
	g_band.IO(gdal.Write, 0, 0, nx, ny, g_pixels, nx, ny, 0, 0)
	b_band := dst_ds.RasterBand(3)
	b_band.IO(gdal.Write, 0, 0, nx, ny, b_pixels, nx, ny, 0, 0)
	alpha_band := dst_ds.RasterBand(4)
	alpha_band.IO(gdal.Write, 0, 0, nx, ny, alpha, nx, ny, 0, 0) 

	return true
}

// save a GeoTIFF to disk from a whole dataset, reading data from MySQL
func saveGeoTIFFDataset(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, mapName, metricName, date string) bool {
        defer func() {
          if r := recover(); r != nil {
             log.Println("saveGeoTIFFDataset Recovered", r)
          }
        }()
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		os.Mkdir(filePath, 0700)
	}

	dateString := strings.Replace(date, ":", "", -1)
	dateString = strings.Replace(dateString, "-", "", -1)
	dateString = strings.Replace(dateString, " ", "T", -1) + "Z"

	fileName := filePath + "/" + dateString + ".tiff"

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Println(err)
		return false
	}

	defer db.Close()

	results, err := db.Query("SELECT x_length, y_length, projection FROM heatmap.metadata WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}
	var xLength, yLength int
	var projection int
	for results.Next() {
		err = results.Scan(&xLength, &yLength, &projection)
		if err != nil {
			log.Println(err)
			return false
		}
	}

	results, err = db.Query("SELECT (max(latitude)-min(latitude))/" + fmt.Sprintf("%v", yLength) + " AS ny, (max(longitude)-min(longitude))/" + fmt.Sprintf("%v", xLength) + " AS nx, max(latitude) AS ymax, min(latitude) AS ymin, max(longitude) AS xmax, min(longitude) AS xmin FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}
	var nx, ny, xmax, xmin, ymax, ymin int
	for results.Next() {
		err = results.Scan(&ny, &nx, &ymax, &ymin, &xmax, &xmin)
		if err != nil {
			log.Println(err)
			return false
		}
	}

	nx += 1
	ny += 1

	r_pixels := make([]uint8, ny*nx)
	g_pixels := make([]uint8, ny*nx)
	b_pixels := make([]uint8, ny*nx)
	alpha := make([]uint8, ny*nx)

	results, err = db.Query("SELECT latitude AS y, longitude AS x, value FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}
	var x, y int
	var value float64
	for results.Next() {
		err = results.Scan(&y, &x, &value)
		if err != nil {
			log.Println(err)
			return false
		}
		r, g, b := getColor(colorMap, metricName, value)
		if r == -1 {
			log.Println("error getting color for metric", metricName)
			return false
		}
		loc := (x-xmin)/xLength + ((ymax-y)/yLength)*nx
		r_pixels[loc] = uint8(r)
		g_pixels[loc] = uint8(g)
		b_pixels[loc] = uint8(b)
		alpha[loc] = 255
	}

	xres := float64((xmax - xmin + xLength)) / float64(nx)
	yres := float64((ymax - ymin + yLength)) / float64(ny)
	geotransform := [6]float64{float64(xmin) - xres/2, xres, 0, float64(ymax) + yres/2, 0, -yres}

	driver, err := gdal.GetDriverByName("GTiff")
	if err != nil {
		log.Println(err)
		return false
	}
	dst_ds := driver.Create(filePath+"/tmp.tiff", nx, ny, 4, gdal.Byte, nil)

	spatialRef := gdal.CreateSpatialReference("") 
	spatialRef.FromEPSG(projection)               
	srString, err := spatialRef.ToWKT()
	dst_ds.SetProjection(srString)       
	dst_ds.SetGeoTransform(geotransform) 

	r_band := dst_ds.RasterBand(1)
	r_band.IO(gdal.Write, 0, 0, nx, ny, r_pixels, nx, ny, 0, 0)
	g_band := dst_ds.RasterBand(2)
	g_band.IO(gdal.Write, 0, 0, nx, ny, g_pixels, nx, ny, 0, 0)
	b_band := dst_ds.RasterBand(3)
	b_band.IO(gdal.Write, 0, 0, nx, ny, b_pixels, nx, ny, 0, 0)
	alpha_band := dst_ds.RasterBand(4)
	alpha_band.IO(gdal.Write, 0, 0, nx, ny, alpha, nx, ny, 0, 0) 
	dst_ds.Close()

	cmd := conf["gdalwarp_path"] + " " + filePath + "/tmp.tiff" + " " + fileName + " -t_srs \"+proj=longlat +datum=WGS84 +ellps=WGS84\""
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "rm " + filePath + "/tmp.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}
	return true
}

// save a GeoTIFF to disk from a whole dataset, reading data from a binary file (GRAL)
func saveGeoTIFFDatasetFile(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, mapName, metricName, date string) bool {
        defer func() {
          if r := recover(); r != nil {
             log.Println("saveGeoTIFFDatasetFile Recovered", r)
          }
        }()
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		os.Mkdir(filePath, 0700)
	}

	dateString := strings.Replace(date, ":", "", -1)
	dateString = strings.Replace(dateString, "-", "", -1)
	dateString = strings.Replace(dateString, " ", "T", -1) + "Z"

	fileName := filePath + "/" + dateString + ".tiff"

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Println(err)
		return false
	}

	defer db.Close()

	results, err := db.Query("SELECT x_length, y_length, projection, insertOnDB FROM heatmap.metadata WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}
	var xLength, yLength int32
	var projection, insertOnDB int
	for results.Next() {
		err = results.Scan(&xLength, &yLength, &projection, &insertOnDB)
		if err != nil {
			log.Println(err)
			return false
		}
	}

	data_stmt, err := db.Prepare("INSERT IGNORE INTO heatmap.data " +
		"(map_name, metric_name, latitude, longitude, value, date) " +
		"VALUES (?,?,?,?,?,?)")
	if err != nil {
		log.Panic(err.Error())
	}

	var ymax int32 = -1 
	var ymin int32 = -1 
	var xmax int32 = -1 
	var xmin int32 = -1 
	file, err := os.Open(conf["gral_data"] + "/" + mapName + dateString)
	if err != nil {
		log.Println(err)
		return false
	}
	m := payload{}
	fileinfo, err := file.Stat()
	if err != nil {
		log.Println(err)
		return false
	}
	filesize := fileinfo.Size()
	readNextBytes(file, 4)
	var c int64
	for c = 0; c < (filesize-4)/12; c++ {
		data := readNextBytes(file, 12)
		buffer := bytes.NewBuffer(data)
		err = binary.Read(buffer, binary.LittleEndian, &m)
		if err != nil {
			log.Println(err)
			return false
		}
		if m.UTMN > ymax || ymax == -1 {
			ymax = m.UTMN
		}
		if m.UTMN < ymin || ymin == -1 {
			ymin = m.UTMN
		}
		if m.UTME > xmax || xmax == -1 {
			xmax = m.UTME
		}
		if m.UTME < xmin || xmin == -1 {
			xmin = m.UTME
		}
	}
	file.Close()

	ny := int((ymax-ymin)/yLength) + 1
	nx := int((xmax-xmin)/xLength) + 1

	r_pixels := make([]uint8, ny*nx)
	g_pixels := make([]uint8, ny*nx)
	b_pixels := make([]uint8, ny*nx)
	alpha := make([]uint8, ny*nx)

	file, err = os.Open(conf["gral_data"] + "/" + mapName + dateString)
	defer file.Close()
	if err != nil {
		log.Println(err)
		return false
	}
	fileinfo, err = file.Stat()
	if err != nil {
		log.Println(err)
		return false
	}
	filesize = fileinfo.Size()
	readNextBytes(file, 4)
	for c = 0; c < (filesize-4)/12; c++ {
		data := readNextBytes(file, 12)
		buffer := bytes.NewBuffer(data)
		err = binary.Read(buffer, binary.LittleEndian, &m)
		if err != nil {
			log.Fatal("binary.Read failed", err)
		}
		r, g, b := getColor(colorMap, metricName, float64(m.Value))
		if r == -1 {
			log.Println("error getting color for metric", metricName)
			return false
		}
		loc := (m.UTME-xmin)/xLength + ((ymax-m.UTMN)/yLength)*int32(nx)
		r_pixels[loc] = uint8(r)
		g_pixels[loc] = uint8(g)
		b_pixels[loc] = uint8(b)
		alpha[loc] = 255

		if insertOnDB == 1 || strings.Contains(mapName, "GRALheatmapHelsinki") {
			latitude, longitude, _ := UTM.ToLatLon(float64(m.UTME), float64(m.UTMN), getUTMZone(projection), "", true)
			if latitude >= 60.15500512818767 && latitude <= 60.16173190973275 && longitude >= 24.911051048489185 && longitude <= 24.92336775228557 {
				_, err = data_stmt.Exec(mapName, metricName, float64(m.UTME), float64(m.UTMN), m.Value, date)
				if err != nil {
					log.Println(err)
					return false
				}
			}
		}
	}

	xres := float64((xmax-xmin)+xLength) / float64(nx)
	yres := float64((ymax-ymin)+yLength) / float64(ny)
	geotransform := [6]float64{float64(xmin) - xres/2, xres, 0, float64(ymax) + yres/2, 0, -yres}

	driver, err := gdal.GetDriverByName("GTiff")
	if err != nil {
		log.Println(err)
		return false
	}
	dst_ds := driver.Create(filePath+"/tmp.tiff", nx, ny, 4, gdal.Byte, nil)

	spatialRef := gdal.CreateSpatialReference("") 
	spatialRef.FromEPSG(projection)               
	srString, err := spatialRef.ToWKT()
	dst_ds.SetProjection(srString)       
	dst_ds.SetGeoTransform(geotransform) 

	r_band := dst_ds.RasterBand(1)
	r_band.IO(gdal.Write, 0, 0, nx, ny, r_pixels, nx, ny, 0, 0)
	g_band := dst_ds.RasterBand(2)
	g_band.IO(gdal.Write, 0, 0, nx, ny, g_pixels, nx, ny, 0, 0)
	b_band := dst_ds.RasterBand(3)
	b_band.IO(gdal.Write, 0, 0, nx, ny, b_pixels, nx, ny, 0, 0)
	alpha_band := dst_ds.RasterBand(4)
	alpha_band.IO(gdal.Write, 0, 0, nx, ny, alpha, nx, ny, 0, 0) // set alpha channel as opaque (nodata = transparent)
	dst_ds.Close()

	cmd := conf["gdalwarp_path"] + " " + filePath + "/tmp.tiff" + " " + filePath + "/uncompressed.tiff -t_srs \"+proj=longlat +datum=WGS84 +ellps=WGS84\""
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "rm " + filePath + "/tmp.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = conf["gdal_translate_path"] + " -co compress=deflate -co zlevel=9 -co tiled=yes -co NUM_THREADS=ALL_CPUS --config GDAL_CACHEMAX 512 -of GTiff " + filePath + "/uncompressed.tiff" + " " + fileName
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "rm " + filePath + "/uncompressed.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "tar cJvf " + conf["gral_data"] + "/" + mapName + dateString + ".tar.xz" + " " + conf["gral_data"] + "/" + mapName + dateString
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	} else {
		cmd = "rm " + conf["gral_data"] + "/" + mapName + dateString
		_, err := exec.Command("sh", "-c", cmd).Output()
		if err != nil {
			log.Println(err)
			return false
		}
	}
	return true
}

// save a GeoTIFF to disk from a whole dataset, reading data from a text file (csv)
func saveGeoTIFFDatasetWGS84TextFile(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, mapName, metricName, job_token, date string) bool {
        defer func() {
          if r := recover(); r != nil {
             log.Println("saveGeoTIFFDatasetWGS84TextFile Recovered", r)
          }
        }()
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		os.Mkdir(filePath, 0700)
	}

	dateString := strings.Replace(date, ":", "", -1)
	dateString = strings.Replace(dateString, "-", "", -1)
	dateString = strings.Replace(dateString, " ", "T", -1) + "Z"

	fileName := filePath + "/" + dateString + ".tiff"

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Println(err)
		return false
	}

	defer db.Close()

	results, err := db.Query("SELECT x_length, y_length, projection FROM heatmap.metadata WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}
	var xLength, yLength int32
	var projection int
	for results.Next() {
		err = results.Scan(&xLength, &yLength, &projection)
		if err != nil {
			log.Println(err)
			return false
		}
	}

	if projection == 0 {
		projection = 3857
	}

	file, err := os.Open(conf["copernicus_data"] + "/" + job_token + "/" + mapName + dateString + ".csv")
	if err != nil {
		log.Println(err)
		return false
	}

	r := csv.NewReader(file)

	i := 0

	var ymax int32 = -1 
	var ymin int32 = -1 
	var xmax int32 = -1 
	var xmin int32 = -1 

	for {
		record, err := r.Read()
		if err == io.EOF {
			break
		}
		if err != nil {
			log.Fatal(err)
		}
		if i > 0 {
			UTM_E, err_UTME := strconv.ParseInt(record[0], 10, 32)
			UTM_N, err_UTMN := strconv.ParseInt(record[1], 10, 32)
			if err_UTME == nil && err_UTMN == nil {
				UTME := int32(UTM_E)
				UTMN := int32(UTM_N)
				if UTMN > ymax || ymax == -1 {
					ymax = UTMN
				}
				if UTMN < ymin || ymin == -1 {
					ymin = UTMN
				}
				if UTME > xmax || xmax == -1 {
					xmax = UTME
				}
				if UTME < xmin || xmin == -1 {
					xmin = UTME
				}
			}
		}
		i += 1
	}

	ny := int((ymax-ymin)/yLength) + 1
	nx := int((xmax-xmin)/xLength) + 1

	r_pixels := make([]uint8, ny*nx)
	g_pixels := make([]uint8, ny*nx)
	b_pixels := make([]uint8, ny*nx)
	alpha := make([]uint8, ny*nx)

	file.Seek(0, io.SeekStart)

	i = 0

	for {
		record, err := r.Read()
		if err == io.EOF {
			break
		}
		if err != nil {
			log.Fatal(err)
		}
		if i > 0 {
			UTM_E, _ := strconv.ParseInt(record[0], 10, 32)
			UTM_N, _ := strconv.ParseInt(record[1], 10, 32)
			UTME := int32(UTM_E)
			UTMN := int32(UTM_N)
			value, _ := strconv.ParseFloat(record[2], 64)
			r, g, b := getColor(colorMap, metricName, value)
			if r == -1 {
				log.Println("the color is missing for this metricName", metricName, " value ", value)
				return false
			}
			loc := (UTME-xmin)/xLength + ((ymax-UTMN)/yLength)*int32(nx)
			r_pixels[loc] = uint8(r)
			g_pixels[loc] = uint8(g)
			b_pixels[loc] = uint8(b)
			alpha[loc] = 255
		}
		i += 1
	}

	geotransform := [6]float64{float64(xmin), float64(xLength), 0, float64(ymax), 0, float64(-yLength)}

	driver, err := gdal.GetDriverByName("GTiff")
	if err != nil {
		log.Println(err)
		return false
	}
	dst_ds := driver.Create(filePath+"/tmp.tiff", nx, ny, 4, gdal.Byte, nil)

	spatialRef := gdal.CreateSpatialReference("") 
	spatialRef.FromEPSG(projection)               
	srString, err := spatialRef.ToWKT()
	dst_ds.SetProjection(srString)       
	dst_ds.SetGeoTransform(geotransform) 

	r_band := dst_ds.RasterBand(1)
	r_band.IO(gdal.Write, 0, 0, nx, ny, r_pixels, nx, ny, 0, 0)
	g_band := dst_ds.RasterBand(2)
	g_band.IO(gdal.Write, 0, 0, nx, ny, g_pixels, nx, ny, 0, 0)
	b_band := dst_ds.RasterBand(3)
	b_band.IO(gdal.Write, 0, 0, nx, ny, b_pixels, nx, ny, 0, 0)
	alpha_band := dst_ds.RasterBand(4)
	alpha_band.IO(gdal.Write, 0, 0, nx, ny, alpha, nx, ny, 0, 0) 
	dst_ds.Close()

	cmd := conf["gdalwarp_path"] + " " + filePath + "/tmp.tiff" + " " + filePath + "/uncompressed.tiff -t_srs \"+proj=longlat +datum=WGS84 +ellps=WGS84\""
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "rm " + filePath + "/tmp.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = conf["gdal_translate_path"] + " -co compress=deflate -co zlevel=9 -co tiled=yes -co NUM_THREADS=ALL_CPUS --config GDAL_CACHEMAX 512 -of GTiff " + filePath + "/uncompressed.tiff" + " " + fileName
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "rm " + filePath + "/uncompressed.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	return true
}

// save a GeoTIFF to disk from a whole dataset, reading data from MySQL
func saveGeoTIFFDatasetMySQL(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, mapName, metricName, date string) bool {
        defer func() {
          if r := recover(); r != nil {
             log.Println("saveGeoTIFFDatasetMySQL Recovered", r)
          }
        }()
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		os.Mkdir(filePath, 0700)
	}

	dateString := strings.Replace(date, ":", "", -1)
	dateString = strings.Replace(dateString, "-", "", -1)
	dateString = strings.Replace(dateString, " ", "T", -1) + "Z"

	fileName := filePath + "/" + dateString + ".tiff"

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Println(err)
		return false
	}

	defer db.Close()

	results, err := db.Query("SELECT FLOOR(x_length), FLOOR(y_length), projection FROM heatmap.metadata WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}
	var xLength, yLength int32
	var projection int
	for results.Next() {
		err = results.Scan(&xLength, &yLength, &projection)
		if err != nil {
			log.Println(err)
			return false
		}
	}

	results, err = db.Query("SELECT FLOOR(MAX(latitude)) AS xmax, FLOOR(MIN(latitude)) AS xmin, FLOOR(MAX(longitude)) AS ymax, FLOOR(MIN(longitude)) AS ymin FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}
	var ymax int32 
	var ymin int32 
	var xmax int32 
	var xmin int32 
	for results.Next() {
		err = results.Scan(&xmax, &xmin, &ymax, &ymin)
		if err != nil {
			log.Println(err)
			return false
		}
	}

	ny := int((ymax-ymin)/yLength) + 1
	nx := int((xmax-xmin)/xLength) + 1

	r_pixels := make([]uint8, ny*nx)
	g_pixels := make([]uint8, ny*nx)
	b_pixels := make([]uint8, ny*nx)
	alpha := make([]uint8, ny*nx)

	results, err = db.Query("SELECT FLOOR(latitude) AS UTME, FLOOR(longitude) AS UTMN, value FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}
	var UTME, UTMN int32
	var Value float32
	for results.Next() {
		err = results.Scan(&UTME, &UTMN, &Value)
		if err != nil {
			log.Println(err)
			return false
		}
		r, g, b := getColor(colorMap, metricName, float64(Value))
		if r == -1 {
			log.Println("error getting color for metric", metricName, "value", Value)
			return false
		}
		loc := (UTME-xmin)/xLength + ((ymax-UTMN)/yLength)*int32(nx)
		r_pixels[loc] = uint8(r)
		g_pixels[loc] = uint8(g)
		b_pixels[loc] = uint8(b)
		alpha[loc] = 255
	}

	xres := float64((xmax-xmin)+xLength) / float64(nx)
	yres := float64((ymax-ymin)+yLength) / float64(ny)
	geotransform := [6]float64{float64(xmin) - xres/2, xres, 0, float64(ymax) + yres/2, 0, -yres}

	driver, err := gdal.GetDriverByName("GTiff")
	if err != nil {
		log.Println(err)
		return false
	}
	dst_ds := driver.Create(filePath+"/tmp.tiff", nx, ny, 4, gdal.Byte, nil)

	spatialRef := gdal.CreateSpatialReference("") 
	spatialRef.FromEPSG(projection)               
	srString, err := spatialRef.ToWKT()
	dst_ds.SetProjection(srString)       
	dst_ds.SetGeoTransform(geotransform) 

	r_band := dst_ds.RasterBand(1)
	r_band.IO(gdal.Write, 0, 0, nx, ny, r_pixels, nx, ny, 0, 0)
	g_band := dst_ds.RasterBand(2)
	g_band.IO(gdal.Write, 0, 0, nx, ny, g_pixels, nx, ny, 0, 0)
	b_band := dst_ds.RasterBand(3)
	b_band.IO(gdal.Write, 0, 0, nx, ny, b_pixels, nx, ny, 0, 0)
	alpha_band := dst_ds.RasterBand(4)
	alpha_band.IO(gdal.Write, 0, 0, nx, ny, alpha, nx, ny, 0, 0) 
	dst_ds.Close()
	log.Println("saved "+filePath+"/tmp.tiff")

	cmd := conf["gdalwarp_path"] + " " + filePath + "/tmp.tiff" + " " + filePath + "/uncompressed.tiff -t_srs \"+proj=longlat +datum=WGS84 +ellps=WGS84\""
	log.Println("exec "+cmd)
	var out []byte
	out, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		log.Println(cmd)
		return false
	}
        log.Printf("out:\n%s\n",out)
	cmd = "rm " + filePath + "/tmp.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = conf["gdal_translate_path"] + " -co compress=deflate -co zlevel=9 -co tiled=yes -co NUM_THREADS=ALL_CPUS --config GDAL_CACHEMAX 512 -of GTiff " + filePath + "/uncompressed.tiff" + " " + fileName
	log.Println("exec "+cmd)
	out, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}
        log.Printf("out:\n%s\n",out)

	cmd = "rm " + filePath + "/uncompressed.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	return true
}

// save a GeoTIFF to disk from a whole dataset, reading data from a binary file (GRAL)
func saveGeoTIFFDatasetTextFile(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, mapName, metricName, date string) bool {
        defer func() {
          if r := recover(); r != nil {
             log.Println("saveGeoTIFFDatasetTextFile Recovered", r)
          }
        }()
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		os.Mkdir(filePath, 0700)
	}

	dateString := strings.Replace(date, ":", "", -1)
	dateString = strings.Replace(dateString, "-", "", -1)
	dateString = strings.Replace(dateString, " ", "T", -1) + "Z"

	fileName := filePath + "/" + dateString + ".tiff"

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Println(err)
		return false
	}

	defer db.Close()

	results, err := db.Query("SELECT x_length, y_length, projection, insertOnDB FROM heatmap.metadata WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}
	var xLength, yLength int32
	var projection, insertOnDB int
	for results.Next() {
		err = results.Scan(&xLength, &yLength, &projection, &insertOnDB)
		if err != nil {
			log.Println(err)
			return false
		}
	}

	data_stmt, err := db.Prepare("INSERT IGNORE INTO heatmap.data " +
		"(map_name, metric_name, latitude, longitude, value, date) " +
		"VALUES (?,?,?,?,?,?)")
	if err != nil {
		log.Panic(err.Error())
	}

	var ymax int32 = -1 
	var ymin int32 = -1 
	var xmax int32 = -1 
	var xmin int32 = -1 
	file, err := os.Open(conf["gral_data"] + "/" + mapName + dateString)
	if err != nil {
		log.Println(err)
		return false
	}
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		text := scanner.Text()
		UTME_UTMN_Value := strings.Split(text, " ")
		UTME, err := strconv.Atoi(UTME_UTMN_Value[0])
		UTMN, err := strconv.Atoi(UTME_UTMN_Value[1])
		if err != nil {
			log.Println(err)
			return false
		}
		if int32(UTMN) > ymax || ymax == -1 {
			ymax = int32(UTMN)
		}
		if int32(UTMN) < ymin || ymin == -1 {
			ymin = int32(UTMN)
		}
		if int32(UTME) > xmax || xmax == -1 {
			xmax = int32(UTME)
		}
		if int32(UTME) < xmin || xmin == -1 {
			xmin = int32(UTME)
		}
	}
	file.Close()

	ny := int((ymax-ymin)/yLength) + 1
	nx := int((xmax-xmin)/xLength) + 1

	r_pixels := make([]uint8, ny*nx)
	g_pixels := make([]uint8, ny*nx)
	b_pixels := make([]uint8, ny*nx)
	alpha := make([]uint8, ny*nx)

	file, err = os.Open(conf["gral_data"] + "/" + mapName + dateString)
	defer file.Close()
	if err != nil {
		log.Println(err)
		return false
	}
	scanner = bufio.NewScanner(file)
	for scanner.Scan() {
		text := scanner.Text()
		UTME_UTMN_Value := strings.Split(text, " ")
		UTME, err := strconv.Atoi(UTME_UTMN_Value[0])
		UTMN, err := strconv.Atoi(UTME_UTMN_Value[1])
		Value, err := strconv.ParseFloat(UTME_UTMN_Value[2], 64)
		if err != nil {
			log.Println(err)
			return false
		}
		r, g, b := getColor(colorMap, metricName, Value)
		if r == -1 {
			log.Println("error getting color for metric", metricName)
			return false
		}
		loc := (int32(UTME)-xmin)/xLength + ((ymax-int32(UTMN))/yLength)*int32(nx)
		r_pixels[loc] = uint8(r)
		g_pixels[loc] = uint8(g)
		b_pixels[loc] = uint8(b)
		alpha[loc] = 255

		if insertOnDB == 1 || strings.Contains(mapName, "GRALheatmapHelsinki") {
			latitude, longitude, _ := UTM.ToLatLon(float64(UTME), float64(UTMN), getUTMZone(projection), "", true)
			if latitude >= 60.15500512818767 && latitude <= 60.16173190973275 && longitude >= 24.911051048489185 && longitude <= 24.92336775228557 {
				_, err = data_stmt.Exec(mapName, metricName, latitude, longitude, Value, date)
				if err != nil {
					log.Println(err)
					return false
				}
			}
		}
	}

	xres := float64((xmax-xmin)+xLength) / float64(nx)
	yres := float64((ymax-ymin)+yLength) / float64(ny)
	geotransform := [6]float64{float64(xmin) - xres/2, xres, 0, float64(ymax) + yres/2, 0, -yres}

	driver, err := gdal.GetDriverByName("GTiff")
	if err != nil {
		log.Println(err)
		return false
	}
	dst_ds := driver.Create(filePath+"/tmp.tiff", nx, ny, 4, gdal.Byte, nil)

	spatialRef := gdal.CreateSpatialReference("") 
	spatialRef.FromEPSG(projection)               
	srString, err := spatialRef.ToWKT()
	dst_ds.SetProjection(srString)       
	dst_ds.SetGeoTransform(geotransform) 

	r_band := dst_ds.RasterBand(1)
	r_band.IO(gdal.Write, 0, 0, nx, ny, r_pixels, nx, ny, 0, 0)
	g_band := dst_ds.RasterBand(2)
	g_band.IO(gdal.Write, 0, 0, nx, ny, g_pixels, nx, ny, 0, 0)
	b_band := dst_ds.RasterBand(3)
	b_band.IO(gdal.Write, 0, 0, nx, ny, b_pixels, nx, ny, 0, 0)
	alpha_band := dst_ds.RasterBand(4)
	alpha_band.IO(gdal.Write, 0, 0, nx, ny, alpha, nx, ny, 0, 0) 
	dst_ds.Close()

	cmd := conf["gdalwarp_path"] + " " + filePath + "/tmp.tiff" + " " + filePath + "/uncompressed.tiff -t_srs \"+proj=longlat +datum=WGS84 +ellps=WGS84\""
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "rm " + filePath + "/tmp.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = conf["gdal_translate_path"] + " -co compress=deflate -co zlevel=9 -co tiled=yes -co NUM_THREADS=ALL_CPUS --config GDAL_CACHEMAX 512 -of GTiff " + filePath + "/uncompressed.tiff" + " " + fileName
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "rm " + filePath + "/uncompressed.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "tar cJvf " + conf["gral_data"] + "/" + mapName + dateString + ".tar.xz" + " " + conf["gral_data"] + "/" + mapName + dateString
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	} else {
		cmd = "rm " + conf["gral_data"] + "/" + mapName + dateString
		_, err := exec.Command("sh", "-c", cmd).Output()
		if err != nil {
			log.Println(err)
			return false
		}
	}
	return true
}

// save a map's GeoTIFFs to disk (calculate the latitude and longitude deltas to determine the cluster size's length)
func saveGeoTIFFsDeltaLatLonOld(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, mapName, metricName string, date string) bool {
	var latitude float64 = -100  
	var longitude float64 = -200 
	save := false

	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		os.Mkdir(filePath, 0700)
	}

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Panic(err.Error())
	}

	defer db.Close()

	results, err := db.Query("SELECT latitude FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "' ORDER BY latitude")
	if err != nil {
		log.Panic(err.Error())
	}

	var lat float64

	var lat_delta_min float64 = -1

	for results.Next() {
		err = results.Scan(&lat)
		if err != nil {
			log.Panic(err.Error())
		}
		if latitude != -100 && latitude != lat {
			lat_delta := math.Abs(latitude - lat)
			if lat_delta_min == -1 || lat_delta < lat_delta_min {
				lat_delta_min = lat_delta
			}
		}
		latitude = lat
	}

	results, err = db.Query("SELECT longitude FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "' ORDER BY longitude")
	if err != nil {
		log.Panic(err.Error())
	}

	var lon float64

	var lon_delta_min float64 = -1

	for results.Next() {
		err = results.Scan(&lon)
		if err != nil {
			log.Panic(err.Error())
		}
		if longitude != -200 && longitude != lon {
			lon_delta := math.Abs(longitude - lon)
			if lon_delta_min == -1 || lon_delta < lon_delta_min {
				lon_delta_min = lon_delta
			}
		}
		longitude = lon
	}

	results, err = db.Query("SELECT latitude, longitude, value FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "'")
	if err != nil {
		log.Panic(err.Error())
	}

	var value float64

	for results.Next() {
		err = results.Scan(&lat, &lon, &value)
		if err != nil {
			log.Panic(err.Error())
		}
		latitude = lat
		longitude = lon
		save = saveGeoTIFF(conf, colorMap, filePath, mapName, metricName, latitude, longitude, value, date, lat_delta_min, lon_delta_min, true)
	}
	return save
}

// save a map's GeoTIFFs to disk (calculate the latitude and longitude deltas to determine the cluster size's length)
func saveGeoTIFFsDeltaLatLon(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, mapName, metricName string, date string) bool {
	var latitude float64 = -100  
	var longitude float64 = -200 

	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		os.Mkdir(filePath, 0700)
	}

	dateString := strings.Replace(date, ":", "", -1)
	dateString = strings.Replace(dateString, "-", "", -1)
	dateString = strings.Replace(dateString, " ", "T", -1) + "Z"

	fileName := filePath + "/" + dateString + ".tiff"

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Println(err)
		return false
	}

	defer db.Close()

	results, err := db.Query("SELECT max(latitude), min(latitude), max(longitude), min(longitude) FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "' ORDER BY latitude")
	if err != nil {
		log.Println(err)
		return false
	}

	var maxLat, minLat, maxLon, minLon float64

	for results.Next() {
		err = results.Scan(&maxLat, &minLat, &maxLon, &minLon)
	}
	xmin, ymin, xmax, ymax := minLon, minLat, maxLon, maxLat

	results, err = db.Query("SELECT projection FROM heatmap.metadata WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}

	var projection int

	for results.Next() {
		err = results.Scan(&projection)
	}
	if projection == 0 {
		projection = 4326
	}

	results, err = db.Query("SELECT DISTINCT(latitude) FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "' ORDER BY latitude")
	if err != nil {
		log.Println(err)
		return false
	}

	var lat float64

	var lat_delta_min float64 = -1

	for results.Next() {
		err = results.Scan(&lat)
		if err != nil {
			log.Println(err)
			return false
		}
		if latitude != -100 && latitude != lat {
			lat_delta := math.Abs(latitude - lat)
			if lat_delta_min == -1 || lat_delta < lat_delta_min {
				lat_delta_min = lat_delta
			}
		}
		latitude = lat
	}

	results, err = db.Query("SELECT DISTINCT(longitude) FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "' ORDER BY longitude")
	if err != nil {
		log.Println(err)
		return false
	}

	var lon float64

	var lon_delta_min float64 = -1

	for results.Next() {
		err = results.Scan(&lon)
		if err != nil {
			log.Println(err)
			return false
		}
		if longitude != -200 && longitude != lon {
			lon_delta := math.Abs(longitude - lon)
			if lon_delta_min == -1 || lon_delta < lon_delta_min {
				lon_delta_min = lon_delta
			}
		}
		longitude = lon
	}

	if strings.Contains(mapName, "Enfuser") {
		lat_delta_min *= 1.05
		lon_delta_min *= 1.05
	}


	ny := int((maxLat-minLat)/lat_delta_min) + 1
	nx := int((maxLon-minLon)/lon_delta_min) + 1

	r_pixels := make([]uint8, ny*nx)
	g_pixels := make([]uint8, ny*nx)
	b_pixels := make([]uint8, ny*nx)
	alpha := make([]uint8, ny*nx)

	results, err = db.Query("SELECT latitude, longitude, value FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}

	var value float64

	for results.Next() {
		err = results.Scan(&lat, &lon, &value)
		if err != nil {
			log.Println(err)
			return false
		}
		r, g, b := getColor(colorMap, metricName, value)
		if r == -1 {
			log.Println("the color is missing for this metricName", metricName)
			return false
		}
		loc := int32((lon-minLon)/lon_delta_min) + int32((maxLat-lat)/lat_delta_min)*int32(nx)
		r_pixels[loc] = uint8(r)
		g_pixels[loc] = uint8(g)
		b_pixels[loc] = uint8(b)
		alpha[loc] = 255
	}

	xres := (xmax - xmin + lon_delta_min) / float64(nx)
	yres := (ymax - ymin + lat_delta_min) / float64(ny)
	geotransform := [6]float64{float64(xmin) - xres/2, xres, 0, float64(ymax) + yres/2, 0, -yres}

	driver, err := gdal.GetDriverByName("GTiff")
	if err != nil {
		log.Println(err)
		return false
	}
	dst_ds := driver.Create(filePath+"/uncompressed.tiff", nx, ny, 4, gdal.Byte, nil)

	spatialRef := gdal.CreateSpatialReference("") 
	spatialRef.FromEPSG(projection)               
	srString, err := spatialRef.ToWKT()
	dst_ds.SetProjection(srString)       
	dst_ds.SetGeoTransform(geotransform) 

	r_band := dst_ds.RasterBand(1)
	r_band.IO(gdal.Write, 0, 0, nx, ny, r_pixels, nx, ny, 0, 0)
	g_band := dst_ds.RasterBand(2)
	g_band.IO(gdal.Write, 0, 0, nx, ny, g_pixels, nx, ny, 0, 0)
	b_band := dst_ds.RasterBand(3)
	b_band.IO(gdal.Write, 0, 0, nx, ny, b_pixels, nx, ny, 0, 0)
	alpha_band := dst_ds.RasterBand(4)
	alpha_band.IO(gdal.Write, 0, 0, nx, ny, alpha, nx, ny, 0, 0) // set alpha channel as opaque (nodata = transparent)
	dst_ds.Close()

	cmd := conf["gdal_translate_path"] + " -co compress=deflate -co zlevel=9 -co tiled=yes -co NUM_THREADS=ALL_CPUS --config GDAL_CACHEMAX 512 -of GTiff " + filePath + "/uncompressed.tiff" + " " + fileName
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println("error compressing GeoTIFF")
		return false
	}

	cmd = "rm " + filePath + "/uncompressed.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println("error removing uncompressed GeoTIFF")
		return false
	}

	return true
}

// get the UTM zone from the EPSG code
func getUTMZone(EPSG int) int {
	if EPSG == 32632 {
		return 32
	} else if EPSG == 32635 {
		return 35
	}
	return 32
}

// save a map's GeoTIFFs to disk (calculate the geographic distance between points to determine the cluster size's length)
func saveGeoTIFFs(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, mapName, metricName, date string, xLength, yLength float64) bool {
	var save bool

	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		os.Mkdir(filePath, 0700)
	}

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Println(err)
		return false
	}

	defer db.Close()

	var latitude float64
	var longitude float64
	var value float64

	results, err := db.Query("SELECT latitude, longitude, value FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}

	for results.Next() {
		err = results.Scan(&latitude, &longitude, &value)
		if err != nil {
			log.Println(err)
			return false
		}
		save = saveGeoTIFF(conf, colorMap, filePath, mapName, metricName, latitude, longitude, value, date, xLength, yLength, false)
	}
	return save
}

func saveGeoTIFFsNetCDFFile(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, filePath, NetCDFFile, mapName, metricName, date string) bool {
	var latitude float32 = -100  
	var longitude float32 = -200 

	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		os.Mkdir(filePath, 0700)
	}

	dateString := strings.Replace(date, ":", "", -1)
	dateString = strings.Replace(dateString, "-", "", -1)
	dateString = strings.Replace(dateString, " ", "T", -1) + "Z"

	fileName := filePath + "/" + dateString + ".tiff"

	ds, err := netcdf.OpenFile(NetCDFFile, netcdf.NOWRITE)
	if err != nil {
		log.Println(err)
		return false
	}
	defer ds.Close()
	var indexName string
	nVars, _ := ds.NVars()
	for i := 0; i < nVars; i++ {
		varn := ds.VarN(i)
		varName, _ := varn.Name()
		if varName != "crs" && varName != "time" && varName != "lat" && varName != "lon" {
			indexName = varName
		}
	}
	lat_v, err := ds.Var("lat")
	if err != nil {
		log.Println(err)
		return false
	}
	lon_v, err := ds.Var("lon")
	if err != nil {
		log.Println(err)
		return false
	}
	index_v, err := ds.Var(indexName)
	if err != nil {
		log.Println(err)
		return false
	}
	lat, err := netcdf.GetFloat32s(lat_v)
	if err != nil {
		log.Println(err)
		return false
	}
	lon, err := netcdf.GetFloat32s(lon_v)
	if err != nil {
		log.Println(err)
		return false
	}
	index, err := netcdf.GetFloat32s(index_v)
	if err != nil {
		log.Println(err)
		return false
	}
	index_dims, err := index_v.LenDims()
	if err != nil {
		log.Println(err)
		return false
	}

	var ymax float32 = -1 
	var ymin float32 = -1 
	var xmax float32 = -1 
	var xmin float32 = -1 
	for lat_index := 0; lat_index < int(index_dims[1]); lat_index++ {
		for lon_index := 0; lon_index < int(index_dims[2]); lon_index++ {
			if lat[lat_index] > ymax || ymax == -1 {
				ymax = lat[lat_index]
			}
			if lat[lat_index] < ymin || ymin == -1 {
				ymin = lat[lat_index]
			}
			if lon[lon_index] > xmax || xmax == -1 {
				xmax = lon[lon_index]
			}
			if lon[lon_index] < xmin || xmin == -1 {
				xmin = lon[lon_index]
			}
		}
	}

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Println(err)
		return false
	}

	defer db.Close()

	results, err := db.Query("SELECT projection FROM heatmap.metadata WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND clustered = " + conf["clustered"] + " AND date = '" + date + "'")
	if err != nil {
		log.Println(err)
		return false
	}

	var projection int

	for results.Next() {
		err = results.Scan(&projection)
	}
	if projection == 0 {
		projection = 4326
	}

	lat_sorted := make([]float32, len(lat))
	lon_sorted := make([]float32, len(lon))
	copy(lat_sorted, lat)
	copy(lon_sorted, lon)

	var yLength float32 = -1
	for lat_index := 0; lat_index < int(index_dims[1]); lat_index++ {
		for lon_index := 0; lon_index < int(index_dims[2]); lon_index++ {
			if latitude != -100 && latitude != lat_sorted[lat_index] {
				lat_delta := float32(math.Abs(float64(latitude) - float64(lat_sorted[lat_index])))
				if yLength == -1 || lat_delta < yLength {
					yLength = lat_delta
				}
			}
			latitude = lat_sorted[lat_index]
		}
	}

	var xLength float32 = -1
	for lat_index := 0; lat_index < int(index_dims[1]); lat_index++ {
		for lon_index := 0; lon_index < int(index_dims[2]); lon_index++ {
			if longitude != -200 && longitude != lon_sorted[lon_index] {
				lon_delta := float32(math.Abs(float64(longitude) - float64(lon_sorted[lon_index])))
				if xLength == -1 || lon_delta < xLength {
					xLength = lon_delta
				}
			}
			longitude = lon_sorted[lon_index]
		}
	}

	ny := int((ymax-ymin)/yLength) + 1
	nx := int((xmax-xmin)/xLength) + 1

	r_pixels := make([]uint8, ny*nx)
	g_pixels := make([]uint8, ny*nx)
	b_pixels := make([]uint8, ny*nx)
	alpha := make([]uint8, ny*nx)

	for time_index := 1; time_index < 2; time_index++ {
		for lat_index := 0; lat_index < int(index_dims[1]); lat_index++ {
			for lon_index := 0; lon_index < int(index_dims[2]); lon_index++ {
				r, g, b := getColor(colorMap, metricName, float64(index[time_index*lat_index*lon_index]))
				if r == -1 {
					log.Println("error getting color for metric", metricName)
					return false
				}
				loc := int32((lon[lon_index]-xmin)/xLength) + int32((ymax-lat[lat_index])/yLength)*int32(nx)
				r_pixels[loc] = uint8(r)
				g_pixels[loc] = uint8(g)
				b_pixels[loc] = uint8(b)
				alpha[loc] = 255
			}
		}
	}

	xres := float64(xmax-xmin+xLength) / float64(nx)
	yres := float64(ymax-ymin+yLength) / float64(ny)
	geotransform := [6]float64{float64(xmin) - xres/2, xres, 0, float64(ymax) + yres/2, 0, -yres}

	driver, err := gdal.GetDriverByName("GTiff")
	if err != nil {
		log.Println(err)
		return false
	}
	dst_ds := driver.Create(filePath+"/tmp.tiff", nx, ny, 4, gdal.Byte, nil)

	spatialRef := gdal.CreateSpatialReference("") 
	spatialRef.FromEPSG(projection)               
	srString, err := spatialRef.ToWKT()
	dst_ds.SetProjection(srString) 
	dst_ds.SetGeoTransform(geotransform) 

	r_band := dst_ds.RasterBand(1)
	r_band.IO(gdal.Write, 0, 0, nx, ny, r_pixels, nx, ny, 0, 0)
	g_band := dst_ds.RasterBand(2)
	g_band.IO(gdal.Write, 0, 0, nx, ny, g_pixels, nx, ny, 0, 0)
	b_band := dst_ds.RasterBand(3)
	b_band.IO(gdal.Write, 0, 0, nx, ny, b_pixels, nx, ny, 0, 0)
	alpha_band := dst_ds.RasterBand(4)
	alpha_band.IO(gdal.Write, 0, 0, nx, ny, alpha, nx, ny, 0, 0) 
	dst_ds.Close()

	cmd := conf["gdalwarp_path"] + " " + filePath + "/tmp.tiff" + " " + filePath + "/uncompressed.tiff -t_srs \"+proj=longlat +datum=WGS84 +ellps=WGS84\""
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "rm " + filePath + "/tmp.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = conf["gdal_translate_path"] + " -co compress=deflate -co zlevel=9 -co tiled=yes -co NUM_THREADS=ALL_CPUS --config GDAL_CACHEMAX 512 -of GTiff " + filePath + "/uncompressed.tiff" + " " + fileName
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}

	cmd = "rm " + filePath + "/uncompressed.tiff"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Println(err)
		return false
	}
	return true
}

func floatInSlice(a float64, list []float64) bool {
	for _, b := range list {
		if b == a {
			return true
		}
	}
	return false
}

// index this map into GeoServer
func indexMap(conf map[string]string, colorMap map[string]map[int]map[string]interface{}, archive, mapName, metricName string, filePath, date, geoTIFF string, xLength, yLength float64, projection, file int, binary int, job_token, fileType string) bool {
	var save bool
	if projection != 4326 && projection != 0 && file == 1 && binary == 1 && job_token == "" { 
		log.Println("saveGeoTIFFDatasetFile")
		save = saveGeoTIFFDatasetFile(conf, colorMap, filePath, mapName, metricName, date)
	} else if projection != 4326 && projection != 0 && file == 1 && binary == 0 && job_token == "" { 
		log.Println("saveGeoTIFFDatasetTextFile")
		save = saveGeoTIFFDatasetTextFile(conf, colorMap, filePath, mapName, metricName, date)
	} else if projection != 4326 && file == 1 && binary == 0 && job_token != "" { 
		log.Println("saveGeoTIFFDatasetWGS84TextFile")
		save = saveGeoTIFFDatasetWGS84TextFile(conf, colorMap, filePath, mapName, metricName, job_token, date)
	} else if projection != 4326 && projection != 0 && file == 0 && binary == 0 { 
		log.Println("saveGeoTIFFDatasetMySQL")
		save = saveGeoTIFFDatasetMySQL(conf, colorMap, filePath, mapName, metricName, date)
	} else if projection != 4326 && projection != 0 { 
		log.Println("saveGeoTIFFDataset")
		save = saveGeoTIFFDataset(conf, colorMap, filePath, mapName, metricName, date)
	} else if xLength == 0 && yLength == 0 { 
		log.Println("saveGeoTIFFsDeltaLatLon")
		save = saveGeoTIFFsDeltaLatLon(conf, colorMap, filePath, mapName, metricName, date)
	} else { 
		log.Println("saveGeoTIFFs")
		save = saveGeoTIFFs(conf, colorMap, filePath, mapName, metricName, date, xLength, yLength)
	}

	if !save {
		log.Println("error indexing map " + mapName +" "+metricName+" "+date+" into GeoServer")
		return false
	}

	if _, err := os.Stat(filePath + "/merged"); os.IsNotExist(err) {
		os.Mkdir(filePath+"/merged", 0700)
	}

	files_to_mosaic, err := FilterDirsGlob(filePath, "/*.tiff")
	if err != nil {
		log.Fatal(err)
	}

	if len(files_to_mosaic) > 1 {
		f, err := os.OpenFile(filePath+"/input.txt", os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0600)
		if err != nil {
			log.Panic(err)
		}
		defer f.Close()
		for _, file_to_mosaic := range files_to_mosaic {
			if _, err = f.WriteString(file_to_mosaic + "\n"); err != nil {
				log.Panic(err)
			}
		}

		cmd := conf["gdalbuildvrt_path"] + " -input_file_list " + filePath + "/input.txt " + filePath + "/" + geoTIFF + ".vrt"
		_, err = exec.Command("sh", "-c", cmd).Output()
		if err != nil {
			log.Fatal(err)
		}

		cmd = conf["gdal_translate_path"] + " -co compress=deflate -co zlevel=9 -co tiled=yes -co NUM_THREADS=ALL_CPUS --config GDAL_CACHEMAX 512 -of GTiff " + filePath + "/" + geoTIFF + ".vrt" + " " + filePath + "/merged/" + geoTIFF
		_, err = exec.Command("sh", "-c", cmd).Output()
		if err != nil {
			log.Fatal(err)
		}
	} else { 
		CopyFile(files_to_mosaic[0], filePath+"/merged/"+geoTIFF)
	}

	layer := conf["GeoServer_workspace"] + ":" + mapName
	isLayer := checkLayer(conf, layer)

	compressFiles(conf, mapName, isLayer, filePath, archive)

	if conf["update_index_map"]=="true" {
		if !sendGeoTIFF(conf, mapName, isLayer, filePath, archive) {
			return false
		}
	}

	os.RemoveAll(filePath)

	return true
}

// index maps into GeoServer
func indexMaps(conf map[string]string) {
	filePath := conf["data_folder"]
	//log.Print("Connecting to heatmap db");
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Panic(err.Error())
	}

	defer db.Close()

	colorMap := getColorsMaps(conf)
	//log.Println(colorMap)

	results, err := db.Query(`SELECT a.map_name, a.metric_name, b.x_length, b.y_length, 
		b.projection, b.file, b.binary, b.fileType, a.date, c.token 
		FROM heatmap.maps_completed a 
		LEFT JOIN heatmap.metadata b ON a.map_name = b.map_name AND a.date = b.date
		LEFT JOIN heatmap.completed_jobs d ON a.date = d.from_date
		LEFT JOIN heatmap.jobs c ON a.map_name = c.map_name AND c.token = d.token 
		WHERE a.completed = '1' AND (a.indexed = '0' OR a.indexed = '-1') 
		AND a.attempts <= ` + conf["Map_Indexing_Attempts"] +
		` ORDER BY a.id DESC, a.indexed DESC, a.attempts ASC`)
	if err != nil {
		log.Panic(err.Error())
	}

	var mapName string
	var metricName string
	var xLength float64
	var yLength float64
	var date string
	var geoTIFFArchive string
	var geoTIFF string
	var projection int
	var file int
	var token []byte 
	var binary int
	var fileType []byte
	for results.Next() {
		err = results.Scan(&mapName, &metricName, &xLength, &yLength, &projection,
			&file, &binary, &fileType, &date, &token)
		if err != nil {
			continue
		} else {
			dateString := strings.Replace(date, ":", "", -1)
			dateString = strings.Replace(dateString, "-", "", -1)
			dateString = strings.Replace(dateString, " ", "T", -1) + "Z"
			geoTIFF = dateString + ".tiff"
			geoTIFFArchive = mapName + "-" + dateString + ".zip"

			log.Print("Indexing map: " + mapName + " metric: " + metricName + " date: " + date)

			setIndexedMap(conf, mapName, metricName, date, "2")
			tmpPath := fmt.Sprintf("%d", time.Now().UnixNano())

			job_token := ""
			if token != nil {
				job_token = string(token)
			}

			i := indexMap(conf, colorMap, geoTIFFArchive, mapName, metricName, filePath+"/"+mapName+"_"+tmpPath, date, geoTIFF, xLength, yLength, projection, file, binary, job_token, string(fileType))
			if conf["update_index_map"]=="true" {
				if i == true {
					setIndexedMap(conf, mapName, metricName, date, "1")
				} else { 
					setIndexedMap(conf, mapName, metricName, date, "-1")
				}
			}
		}
	}
}

// set a map as indexed into MySQL
func setIndexedMap(conf map[string]string, mapName, metricName, date, indexed string) {

	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["Mysql_database"])

	if err != nil {
		log.Panic(err.Error())
	}

	defer db.Close()

	stmt, err := db.Prepare(
		"UPDATE heatmap.maps_completed SET indexed = ?, attempts = attempts + 1 WHERE map_name = ? AND metric_name = ? AND date = ?",
	)
	if err != nil {
		log.Panic(err.Error())
	}
	_, err = stmt.Exec(indexed, mapName, metricName, date)
	if err != nil {
		log.Panic(err.Error())
	}
}

// get the map's number of points NOT USED
func getMapPointsNumber(conf map[string]string, mapName, metricName, date string) int {
	db, err := sql.Open("mysql", conf["MySQL_username"]+":"+conf["MySQL_password"]+"@tcp("+conf["MySQL_hostname"]+":"+conf["MySQL_port"]+")/"+conf["MySQL_database"])

	if err != nil {
		log.Panic(err.Error())
	}

	defer db.Close()

	results, err := db.Query("SELECT COUNT(*) AS num FROM heatmap.data WHERE map_name = '" + mapName + "' AND metric_name = '" + metricName + "' AND date = '" + date + "'")

	if err != nil {
		return 0
	}

	var num int

	for results.Next() {
		err = results.Scan(&num)
		if err != nil {
			log.Println(err)
			return 0
		}
	}

	return num
}

// read GRAL binary file
func readGRALFile(fileName string) {
	file, err := os.Open(fileName)
	defer file.Close()
	if err != nil {
		log.Fatal(err)
	}

	m := payload{}
	fileinfo, err := file.Stat()
	if err != nil {
		log.Println(err)
		return
	}
	filesize := fileinfo.Size()
	readNextBytes(file, 4)
	var i int64
	for i = 4; i < filesize; i++ {
		data := readNextBytes(file, 12)
		buffer := bytes.NewBuffer(data)
		err = binary.Read(buffer, binary.LittleEndian, &m)
		if err != nil {
			log.Fatal("binary.Read failed", err)
		}
	}
}

func readNextBytes(file *os.File, number int) []byte {
	bytes := make([]byte, number)
	_, err := file.Read(bytes)
	if err != nil {
		log.Fatal(err)
	}
	return bytes
}

type payload struct {
	UTME  int32 
	UTMN  int32 
	Value float32
}

// find the minimum difference >0 between any two elements in an array
func findMinDiff(arr []int32) int32 {
	var diff int32 = -1

	for i := 0; i < len(arr)-1; i++ {
		for j := i + 1; j < len(arr); j++ {
			d := int32(math.Abs(float64(arr[i]) - float64(arr[j])))
			if d != 0 && (diff == -1 || d < diff) {
				diff = d
			}
		}
	}
	return diff
}

// find the minimum difference >0 between any two elements in an array
func findMinUTMDistance(arr1, arr2 []int32) int32 {
	var diff float64 = -1

	for i := 0; i < len(arr1)-1; i++ {
		for j := i + 1; j < len(arr1); j++ {
			d1 := math.Pow(float64(arr1[i])-float64(arr1[j]), 2)
			d2 := math.Pow(float64(arr2[i])-float64(arr2[j]), 2)
			d := math.Sqrt(d1 - d2)
			if d != 0 && (diff == -1 || d < diff) {
				diff = d
			}
		}
	}
	return int32(math.Round(diff))
}

// split a string array into chunks limited by lim
func split(buf []string, lim int) [][]string {
	var chunk []string
	chunks := make([][]string, 0, len(buf)/lim+1)
	for len(buf) >= lim {
		chunk, buf = buf[:lim], buf[lim:]
		chunks = append(chunks, chunk)
	}
	if len(buf) > 0 {
		chunks = append(chunks, buf[:len(buf)])
	}
	return chunks
}

// write a file
func writeFile(filePath, filename, text string) {
	t := []byte(text)
	err := ioutil.WriteFile(filePath+"/"+filename, t, 0700)
	if err != nil {
		log.Panic(err.Error())
	}
}

// get all file paths
func get_all_file_paths(directory string) []string {
	files, err := ioutil.ReadDir(directory)
	f := []string{}
	if err != nil {
		log.Fatal(err)
	}
	for _, file := range files {
		f = append(f, directory+"/"+file.Name())
	}
	return f
}

func recursiveZip(pathToZip, destinationPath string) error {
	destinationFile, err := os.Create(destinationPath)
	if err != nil {
		return err
	}
	myZip := zip.NewWriter(destinationFile)
	err = filepath.Walk(pathToZip, func(filePath string, info os.FileInfo, err error) error {
		if info.IsDir() {
			return nil
		}
		if err != nil {
			return err
		}
		relPath := strings.TrimPrefix(filePath, filepath.Dir(pathToZip))
		zipFile, err := myZip.Create(relPath)
		if err != nil {
			return err
		}
		fsFile, err := os.Open(filePath)
		if err != nil {
			return err
		}
		_, err = io.Copy(zipFile, fsFile)
		if err != nil {
			return err
		}
		return nil
	})
	if err != nil {
		return err
	}
	err = myZip.Close()
	if err != nil {
		return err
	}
	return nil
}

// compress files with zip
func compressFiles(conf map[string]string, mapName string, isLayer bool, filePath, zipFile string) {
	if !isLayer {
		writeFile(filePath+"/merged", "datastore.properties",
			"SPI=org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory\n"+
				"jndiReferenceName=jdbc/postgres"+"\n"+
				"Loose\\ bbox=true\n"+
				"preparedStatements=false")

		writeFile(filePath+"/merged", "indexer.properties",
			"TimeAttribute=ingestion\n"+
				"Schema=*the_geom:Polygon,location:String,ingestion:java.util.Date\n"+
				"PropertyCollectors=TimestampFileNameExtractorSPI[timeregex](ingestion)")

		writeFile(filePath+"/merged", "timeregex.properties", "regex=[0-9]{8}T[0-9]{6}Z")
	}
	recursiveZip(filePath+"/merged/", filePath+"/"+zipFile)
}

// send GeoTIFF to GeoServer
func sendGeoTIFF(conf map[string]string, layer string, isLayer bool, filePath, filename string) bool {
	if !isLayer {
		f, err := os.Open(filePath + "/" + filename)
		if err != nil {
			log.Println(err)
			return false
		}
		defer f.Close()
		req, err := http.NewRequest("PUT", conf["GeoServer_url"]+"/workspaces/"+conf["GeoServer_workspace"]+"/coveragestores/"+layer+"/file.imagemosaic?recalculate=nativebbox,latlonbbox", f)
		if err != nil {
			log.Println(err)
			return false
		}
		req.SetBasicAuth(conf["GeoServer_username"], conf["GeoServer_password"])
		req.Header.Set("Content-Type", "application/zip")

		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			log.Println(err)
			return false
		}
		defer resp.Body.Close()
		body, err := ioutil.ReadAll(resp.Body)
		log.Println(string(body))
	} else { 
		f, err := os.Open(filePath + "/" + filename)
		if err != nil {
			log.Println(err)
			return false
		}
		defer f.Close()
		req, err := http.NewRequest("POST", conf["GeoServer_url"]+"/workspaces/"+conf["GeoServer_workspace"]+"/coveragestores/"+layer+"/file.imagemosaic?recalculate=nativebbox,latlonbbox", f)
		if err != nil {
			log.Println(err)
			return false
		}
		req.SetBasicAuth(conf["GeoServer_username"], conf["GeoServer_password"])
		req.Header.Set("Content-Type", "application/zip")

		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			log.Println(err)
			return false
		}
		defer resp.Body.Close()
		body, err := ioutil.ReadAll(resp.Body)
		log.Println(string(body))
	}

	if !isLayer {
		body := strings.NewReader(`<coverage><enabled>true</enabled><metadata><entry key="time"><dimensionInfo><enabled>true</enabled><presentation>LIST</presentation><units>ISO8601</units><defaultValue/></dimensionInfo></entry></metadata></coverage>`)
		req, err := http.NewRequest("PUT", conf["GeoServer_url"]+"/workspaces/"+conf["GeoServer_workspace"]+"/coveragestores/"+layer+"/coverages/"+layer, body)
		if err != nil {
			log.Println(err)
			return false
		}
		req.SetBasicAuth(conf["GeoServer_username"], conf["GeoServer_password"])
		req.Header.Set("Content-Type", "application/xml; charset=UTF-8")

		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			log.Println(err)
			return false
		}
		defer resp.Body.Close()
		b, err := ioutil.ReadAll(resp.Body)
		log.Println(string(b))
	}
	return true
}

// check if this layer name is already present in GeoServer
func checkLayer(conf map[string]string, layer string) bool {
	isLayer := false
	req, err := http.NewRequest("GET", conf["GeoServer_url"]+"/layers.json", nil)
	if err != nil {
		log.Println(err)
		return false
	}
	req.SetBasicAuth(conf["GeoServer_username"], conf["GeoServer_password"])
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		log.Println(err)
		return false
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	result := gjson.Get(string(body), "layers")
	result.ForEach(func(key, value gjson.Result) bool {
		value.ForEach(func(key, value gjson.Result) bool {
			layerName := gjson.Get(value.String(), "name").String()
			if layerName == layer {
				isLayer = true
			}
			if isLayer {
				return false 
			} else {
				return true 
			}
		})
		if isLayer {
			return false 
		} else {
			return true 
		}
	})
	return isLayer
}

// CopyFile copies a file from src to dst
func CopyFile(src, dst string) (err error) {
	sfi, err := os.Stat(src)
	if err != nil {
		return
	}
	if !sfi.Mode().IsRegular() {
		return fmt.Errorf("CopyFile: non-regular source file %s (%q)", sfi.Name(), sfi.Mode().String())
	}
	dfi, err := os.Stat(dst)
	if err != nil {
		if !os.IsNotExist(err) {
			return
		}
	} else {
		if !(dfi.Mode().IsRegular()) {
			return fmt.Errorf("CopyFile: non-regular destination file %s (%q)", dfi.Name(), dfi.Mode().String())
		}
		if os.SameFile(sfi, dfi) {
			return
		}
	}
	if err = os.Link(src, dst); err == nil {
		return
	}
	err = copyFileContents(src, dst)
	return
}

// copyFileContents copies the contents of the file named src to the file named
// by dst
func copyFileContents(src, dst string) (err error) {
	in, err := os.Open(src)
	if err != nil {
		return
	}
	defer in.Close()
	out, err := os.Create(dst)
	if err != nil {
		return
	}
	defer func() {
		cerr := out.Close()
		if err == nil {
			err = cerr
		}
	}()
	if _, err = io.Copy(out, in); err != nil {
		return
	}
	err = out.Sync()
	return
}

func FilterDirs(dir, suffix string) ([]string, error) {
	files, err := ioutil.ReadDir(dir)
	if err != nil {
		return nil, err
	}
	res := []string{}
	for _, f := range files {
		if !f.IsDir() && strings.HasSuffix(f.Name(), suffix) {
			res = append(res, filepath.Join(dir, f.Name()))
		}
	}
	return res, nil
}

func FilterDirsGlob(dir, suffix string) ([]string, error) {
	return filepath.Glob(filepath.Join(dir, suffix))
}

func appendToTextFile(filename, text string) {
	f, err := os.OpenFile(filename, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0600)
	if err != nil {
		log.Panic(err)
	}

	defer f.Close()

	if _, err = f.WriteString(text); err != nil {
		log.Panic(err)
	}
}

// make first character of a string upper case
func UcFirst(str string) string {
	for i, v := range str {
		return string(unicode.ToUpper(v)) + str[i+1:]
	}
	return ""
}

// make first character of a string lower case
func LcFirst(str string) string {
	for i, v := range str {
		return string(unicode.ToLower(v)) + str[i+1:]
	}
	return ""
}

func main() {
	conf := map[string]string{}
	conf["GeoServer_username"] = "admin"
	conf["GeoServer_password"] = "password"
	conf["GeoServer_url"] = "http://localhost:8080/geoserver/rest"
	conf["GeoServer_workspace"] = "Snap4City"
	conf["Map_Indexing_Attempts"] = "5"
	conf["PostgreSQL_hostname"] = "localhost"
	conf["PostgreSQL_port"] = "5432"
	conf["PostgreSQL_database"] = "gisdata"
	conf["PostgreSQL_schema"] = "public"
	conf["PostgreSQL_username"] = "snap4city"
	conf["PostgreSQL_password"] = "password"
	conf["MySQL_hostname"] = "localhost"
	conf["MySQL_username"] = "user"
	conf["MySQL_password"] = "password"
	conf["MySQL_port"] = "3306"
	conf["MySQL_database"] = "heatmap"
	conf["Python_path"] = "/usr/bin/python3"
	conf["Python_gdal_merge_path"] = "/usr/bin/gdal_merge.py"
	conf["gdalbuildvrt_path"] = "/usr/bin/gdalbuildvrt"
	conf["gdal_translate_path"] = "/usr/bin/gdal_translate"
	conf["gdalwarp_path"] = "/usr/bin/gdalwarp"
	conf["clustered"] = "0"
	conf["gral_data"] = "/GRAL"
	conf["copernicus_data"] = "/copernicus"
	conf["update_index_map"] = "true"
	conf["sleep_time"] = "30"
	filePath, err := os.Getwd()
	if err == nil {
		conf["data_folder"] = filePath + "/data"
	}

	c := flag.String("conf", "", "Configuration file path (JSON)")
	flag.Parse()
	type Configuration struct {
		GeoServerUsername   string
		GeoServerPassword   string
		GeoServerUrl        string
		GeoServerWorkspace  string
		MapIndexingAttempts string
		PostgreSQLHostname  string
		PostgreSQLPort      string
		PostgreSQLDatabase  string
		PostgreSQLSchema    string
		PostgreSQLUsername  string
		PostgreSQLPassword  string
		MySQLHostname       string
		MySQLUsername       string
		MySQLPassword       string
		MySQLPort           string
		MySQLDatabase       string
		PythonPath          string
		PythonGdalMergePath string
		GdalBuildVrtPath    string
		GdalTranslatePath   string
		GdalWarpPath        string
		Clustered           string
		GRALData            string
		CopernicusData      string
		DataFolder          string
		UpdateIndexMap	    string
		SleepTime	    string
	}
	if *c != "" {
		configuration := Configuration{}
		file, err := os.Open(*c)
		defer file.Close()
		decoder := json.NewDecoder(file)
		err = decoder.Decode(&configuration)
		if err == nil {
			conf["GeoServer_username"] = configuration.GeoServerUsername
			conf["GeoServer_password"] = configuration.GeoServerPassword
			conf["GeoServer_url"] = configuration.GeoServerUrl
			conf["GeoServer_workspace"] = configuration.GeoServerWorkspace
			conf["Map_Indexing_Attempts"] = configuration.MapIndexingAttempts
			conf["PostgreSQL_hostname"] = configuration.PostgreSQLHostname
			conf["PostgreSQL_port"] = configuration.PostgreSQLPort
			conf["PostgreSQL_database"] = configuration.PostgreSQLDatabase
			conf["PostgreSQL_schema"] = configuration.PostgreSQLSchema
			conf["PostgreSQL_username"] = configuration.PostgreSQLUsername
			conf["PostgreSQL_password"] = configuration.PostgreSQLPassword
			conf["MySQL_hostname"] = configuration.MySQLHostname
			conf["MySQL_username"] = configuration.MySQLUsername
			conf["MySQL_password"] = configuration.MySQLPassword
			conf["MySQL_port"] = configuration.MySQLPort
			conf["MySQL_database"] = configuration.MySQLDatabase
			conf["Python_path"] = configuration.PythonPath
			conf["Python_gdal_merge_path"] = configuration.PythonGdalMergePath
			conf["gdalbuildvrt_path"] = configuration.GdalBuildVrtPath
			conf["gdal_translate_path"] = configuration.GdalTranslatePath
			conf["gdalwarp_path"] = configuration.GdalWarpPath
			conf["clustered"] = configuration.Clustered
			conf["gral_data"] = configuration.GRALData
			conf["copernicus_data"] = configuration.CopernicusData
			conf["data_folder"] = configuration.DataFolder
			conf["update_index_map"] = configuration.UpdateIndexMap
			conf["sleep_time"] = configuration.SleepTime
		} else {
			log.Fatal(err)
		}
	}

	for k, _ := range conf {
		env, exists := os.LookupEnv(k)
		if exists {
			conf[k] = env
		}
	}
	log.Printf("Configuration %v",conf)

	if _, err := os.Stat(conf["data_folder"]); os.IsNotExist(err) {
		os.Mkdir(conf["data_folder"], 0755)
	}
	cmd := "rm -rf " + conf["data_folder"] + "/*"
	_, err = exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		log.Fatal(err)
	}

	if err == nil {
		var s int32
		fmt.Sscan(conf["sleep_time"],&s)
		for {
			indexMaps(conf)
			//fmt.Printf("Sleep %ds...\n",s);
			time.Sleep(time.Duration(s) * time.Second)
		}
	}
}
