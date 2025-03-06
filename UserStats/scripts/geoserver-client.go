'''
Snap4city -- GeoServer Client --
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
	"fmt"
	"gopkg.in/hishamkaram/geoserver.v1"
	"strconv"
)

// create a workspace
func createWorkspace(gsCatalog *geoserver.GeoServer) {
	created, err := gsCatalog.CreateWorkspace("snap4city")
	if err != nil {
		fmt.Printf("\nError:%s\n", err)
	}
	fmt.Println(strconv.FormatBool(created))
}

func main() {
	gsCatalog := geoserver.GetCatalog("http://192.168.1.210:8080/geoserver", "admin", "geoserver")
	layers, err := gsCatalog.GetLayers("snap4city")
	if err != nil {
		fmt.Printf("\nError:%s\n", err)
	}
	for _, lyr := range layers {
		fmt.Printf("\nName:%s  href:%s\n", lyr.Name, lyr.Href)
	}
}
