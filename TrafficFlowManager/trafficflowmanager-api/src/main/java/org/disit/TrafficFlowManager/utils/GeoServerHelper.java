/* TrafficFlowManager.
   Copyright (C) 2023 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.disit.TrafficFlowManager.utils;

public class GeoServerHelper {

    private final String user;
    private final String pass;
    private final String endpoint;
    private final String workspace;

    public GeoServerHelper(String user, String pass, String endpoint, String workspace) {
        this.user = user;
        this.pass = pass;
        this.endpoint = endpoint;
        this.workspace = workspace;
    }

    /* Uploads shp to the data store, creating it if necessary
       Doc: https://docs.geoserver.org/latest/en/api/#/latest/en/api/1.0.0/datastores.yaml

       This is the equivalent of:

        curl -v -u user:pass -XPUT -H "Content-type: application/zip"
        --data-binary @zipFile.zip
        http://localhost:8080/geoserver/rest/workspaces/workspace/datastores/datastore/file.shp

       Expected HTTP Response: 201 Created
    */
    public void publishShp(String datastore, String zipFile) throws Exception {
        Logger.log("[GeoServerHelper] Publishing shapefile to datastore " + datastore + "...");
        String datastoreEndpoint = endpoint + "/workspaces/" + workspace + "/datastores/" + datastore;
        String urlString = datastoreEndpoint + "/file.shp";
        Integer response = HTTPHelper.uploadFile(urlString, "application/zip", user, pass, zipFile);
        if (response != 201) {
            throw new Exception("Failed to publish shapefile. Response code: " + response.toString());
        }
    }

    /* Set the specified style to the specified layer
       Doc: https://docs.geoserver.org/latest/en/api/#/latest/en/api/1.0.0/layers.yaml
       This is the equivalent of:

        curl -v -u user:pass -XPUT -H "Content-type: text/xml"
        -d "<layer><defaultStyle><name>style_name</name></defaultStyle></layer>"
        http://localhost:8080/geoserver/rest/layers/workspace:layer

       Expected HTTP Response: 200 OK
    */
    public void setLayerStyle(String layerName, String styleName) throws Exception {
        Logger.log("[GeoServerHelper] Applying style " + styleName + " to layer " + layerName + "...");
        String urlString = endpoint + "/layers/" + workspace + ":" + layerName;
        String xmlData = "<layer><defaultStyle><name>" + styleName + "</name></defaultStyle></layer>";
        Integer response = HTTPHelper.uploadData(urlString, "text/xml", user, pass, xmlData);
        if (response != 200) {
            throw new Exception("Failed to set layer style. Response code: " + response.toString());
        }
    }
}