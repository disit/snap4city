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
