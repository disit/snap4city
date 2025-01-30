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

package org.disit.TrafficFlowManager;

import org.disit.TrafficFlowManager.utils.GeoServerHelper;
import org.disit.TrafficFlowManager.utils.FileZipper;
import org.disit.TrafficFlowManager.utils.ConfigProperties;
import org.disit.TrafficFlowManager.utils.Logger;
import org.disit.TrafficFlowManager.utils.SHPExtractor;
import org.disit.TrafficFlowManager.utils.CSVExtractor;
import org.disit.TrafficFlowManager.utils.IOTDirectoryFunc;
import org.disit.TrafficFlowManager.persistence.JSONReconstructionPersistence;
import org.disit.TrafficFlowManager.persistence.JSONStaticGraphPersistence;
import org.disit.TrafficFlowManager.persistence.ReconstructionPersistenceInterface;
import org.apache.commons.io.FileUtils;
// import org.apache.http.client.methods.CloseableHttpResponse;
// import org.apache.http.client.methods.HttpPost;
// import org.apache.http.entity.StringEntity;
// import org.apache.http.impl.client.CloseableHttpClient;
// import org.apache.http.impl.client.HttpClients;
// import org.apache.http.message.BasicNameValuePair;
// import org.apache.http.util.EntityUtils;
import org.disit.TrafficFlowManager.persistence.OpenSearchReconstructionPersistence;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.json.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

//import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.Instant;

@WebServlet(name = "UploadLayerServlet", value = "/api/upload")
public class UploadLayerServlet extends HttpServlet {

    /**
     * Enum representing all possible payload types that can be handled
     */
    enum PayloadType {
        staticGraph,
        reconstruction,
        TTT
    }

    /**
     * Handle GET request
     * 
     * @param req  request object
     * @param resp response object
     * @throws IOException if an error occurs in response getWriter()
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().write("please do a POST request instead");
        resp.getWriter().close();
    }

    /**
     * Handle POST request
     * 
     * @param req  request object
     * @param resp response object
     * @throws IOException if an error occurs in response getWriter()
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String sessionId = req.getSession().getId(); // GET SESSION ID
        System.out.println(">>>>> Session ID = " + sessionId + " <<<<<<<");

        // Set content type and encoding
        resp.setContentType("application/json");
        resp.setCharacterEncoding("utf-8");

        try {

            // Get payload type parameter
            String type = req.getParameter("type");
            if (type == null) {
                throw new Exception("Please specify a payload type!");
            }
            Properties conf = ConfigProperties.getProperties();
            String legacyTFRString = conf.getProperty("legacyScenarios");

            // Handle payload based on type
            JsonValue body = getJSONBody(req);
            
            switch (PayloadType.valueOf(type)) {
                case staticGraph:
                    //TODO check if legacy or if there is a valid token?
                    handleStaticGraph(body);
                    resp.getWriter().print(buildResponse(true, "", ""));
                    break;
                case reconstruction:

                    JSONObject jbody = new JSONObject(body.toString());
                    String fluxName = jbody.getJSONObject("metadata").getString("fluxName");

                    // Imposta come default la timeZone dell'Italia
                    String dateObserved = jbody.getJSONObject("metadata").getString("dateTime");                    
                    String dateTimeWithTimeZone = (String) ConfigProperties.getProperties().getOrDefault("dateTimeWithTimeZone", "no");
                    if (dateTimeWithTimeZone.equals("yes") && !dateObserved.contains("+") && !dateObserved.contains("Z")) {

                        TimeZone timeZone = TimeZone.getTimeZone("Europe/Rome");

                        String[] d = dateObserved.split("-");
                        String year = d[0];
                        String month = d[1];
                        String day = d[2].split("T")[0];

                        Calendar calendar = Calendar.getInstance(timeZone);
                        calendar.set(Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day), 0, 0, 0);
                        // -1 al mese perchè gennaio corrisponde allo 0
                        calendar.setTimeZone(timeZone);

                        if (timeZone.inDaylightTime(calendar.getTime())) {
                            dateObserved = dateObserved + "+02:00";
                        } else {
                            dateObserved = dateObserved + "+01:00";
                        }
                    }
                    jbody.getJSONObject("metadata").put("dateTime", dateObserved);
                    
                    String[] legacyTFR = legacyTFRString.split("#");
                    boolean isLegacyTFR = false;
                    for (String tfr : legacyTFR) {
                        if (fluxName.equals(tfr)) {
                            isLegacyTFR = true;
                            Logger.log("[TFM] LEGACY Reconstruction identified - " + fluxName);
                            break;
                        }
                    }

                    String uploadedLayerName = "";
                    

                    if (isLegacyTFR) {   
                        // Remove token if present
                        if(jbody.getJSONObject("metadata").has("access_token")){
                            jbody.getJSONObject("metadata").remove("access_token");
                        }

                        // Convert the updated JSONObject (variable "jbody") to a JsonValue (variable "body")
                        String tmp_jsonString = jbody.toString();
                        JsonReader tmp_jsonReader = Json.createReader(new StringReader(tmp_jsonString));
                        body = tmp_jsonReader.readValue();
                        tmp_jsonReader.close();

                        uploadedLayerName = handleReconstruction(body); // for old e new version                     
                        if (conf.getProperty("saveToES", "onrequest").equals("always") || jbody.has("saveToES")) {
                            int[] metadata = OpenSearchReconstructionPersistence.sendToEs(jbody, "reconstructed");
                            resp.getWriter().print(buildResponse(true, "layerName", uploadedLayerName, metadata));
                        } else {
                            Logger.log("[TFM] SKIP save on open Search");
                            resp.getWriter().print(buildResponse(true, "layerName", uploadedLayerName));
                        }
                    } else {
                        // SCENARIO TFR INGESTION   

                        // Get road graph associated to the scenario device from DB 
                        String scenarioDT = "";
                        try{
                            scenarioDT = jbody.getJSONObject("metadata").getString("scenarioDT");
                        }catch(Exception e){
                            Logger.log("[TFM] Exception: " + e);
                            Logger.log("[TFM] fluxName: " + fluxName);
                        }
                        String scenarioSURI = jbody.getJSONObject("metadata").getString("scenarioSURI");
                        String access_token = jbody.getJSONObject("metadata").getString("access_token");     
                        String dashURL = jbody.getJSONObject("metadata").getString("base_url");                    
                        
                        JSONObject res = IOTDirectoryFunc.getBigDataFromDB(scenarioSURI, access_token, scenarioDT, dashURL);
                        JSONArray resArray = new JSONArray(res.getString("responseBody"));
                        JSONObject dataDB = new JSONObject(resArray.getJSONObject(0).getString("data"));
                        JSONObject grandidatiDB = dataDB.getJSONObject("grandidati");
                        JSONArray roadGraph = new JSONArray(grandidatiDB.getJSONArray("roadGraph"));
                        
                        // Create/Update device using IoTDirectory APIs
                        uploadedLayerName = handleReconstructionFromScenario(body, roadGraph);

                        // Remove token if present
                        if(jbody.getJSONObject("metadata").has("access_token")){
                            jbody.getJSONObject("metadata").remove("access_token");
                        }                    
                        // Convert the updated JSONObject (variable "jbody") to a JsonValue (variable "body")
                        String tmp_jsonString = jbody.toString();
                        JsonReader tmp_jsonReader = Json.createReader(new StringReader(tmp_jsonString));
                        body = tmp_jsonReader.readValue();
                        tmp_jsonReader.close();
                        
                        // if not authorized (wrong or missing token in the request), the code should never reach this point
                        uploadedLayerName = handleReconstruction(body); // for old e new version

                        // Send data to OpenSearch 
                        int[] metadata = OpenSearchReconstructionPersistence.sendToEs(jbody, roadGraph, "reconstructed");
                        
                        Logger.log("[TFM] ALL DONE! - " + uploadedLayerName + " - " + java.util.Arrays.toString(metadata));
                        
                        resp.getWriter().print(buildResponse(true, "layerName", uploadedLayerName, metadata));
                    }
                    break;
                case TTT:
                    // do somethig

                    Logger.log("[TFM] IN TTT CASE! Under development");

                    // JSONObject TTT = new JSONObject(body.toString());
                    // System.out.println(TTT.toString(2));
                    // OpenSearchReconstructionPersistence.ingestTTT(TTT, "TTT");
                    break;
            }
            resp.getWriter().close();

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(buildResponse(false, "error", "Unknown payload type!"));
            resp.getWriter().close();
            e.printStackTrace();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(buildResponse(false, "error", e.getMessage()));
            resp.getWriter().close();
            e.printStackTrace();
        }
    }

    private String buildResponse(Boolean success, String name, String value) {
        JsonObjectBuilder builder = Json.createObjectBuilder().add("success", success);
        if (!name.isEmpty() && !value.isEmpty()) {
            builder.add(name, value);
        }
        return builder.build().toString();
    }

    // overloaded method to print metadata from Elastic Search insert
    private String buildResponse(Boolean success, String name, String value, int[] metadata) {
        JsonObjectBuilder builder = Json.createObjectBuilder().add("success", success);
        if (!name.isEmpty() && !value.isEmpty()) {
            builder.add(name, value);
        }
        builder.add("details",
                "sent: " + String.valueOf(metadata[0]) + ", failed: " + String.valueOf(metadata[2]));

        return builder.build().toString();
    }

    private void handleStaticGraph(JsonValue json) throws IOException {

        Logger.log("[Servlet] Handling static graph upload");

        // Read JSON
        JsonObject object = json.asJsonObject();
        String staticGraphName = object.getJsonObject("nameGraphID").getString("staticGraphName");

        // Save static graph to disk
        Logger.log("[Servlet] Saving static graph " + staticGraphName);
        new JSONStaticGraphPersistence().saveStaticGraph(staticGraphName, json);
    }

    private String handleReconstruction(JsonValue json) throws Exception {

        Logger.log("[Servlet] Handling reconstruction upload");

        // Extract metadata & reconstruction data from JSON
        JsonObject object = json.asJsonObject();
        JsonObject reconstructionData = object.getJsonObject("reconstructionData");
        JsonObject metadata = object.getJsonObject("metadata");

        // Get associated static graph array data
        String associatedStaticGraphName = metadata.getString("staticGraphName");
        JsonValue staticGraph = new JSONStaticGraphPersistence().getStaticGraph(associatedStaticGraphName);
        JsonArray staticDataGraph = staticGraph.asJsonObject().getJsonArray("dataGraph");

        // Generate a unique layer name
        String locality = metadata.getString("locality");
        String scenarioID = metadata.getString("scenarioID");
        String dateTime = metadata.getString("dateTime");
        if (dateTime.contains("+")) {
            dateTime = dateTime.split("\\+")[0];
        }
        dateTime = dateTime.replace(':', '-'); // colon causes problems when uploading to GeoServer

        String layerName = locality + "_" + scenarioID + "_" + dateTime;

        // Convert to SHP and upload to GeoServer
        convertToShapefileAndUpload(layerName, staticDataGraph, reconstructionData);

        // Add entry to db and save as zipped json file to reconstructions folder
        ReconstructionPersistenceInterface db = new JSONReconstructionPersistence();
        db.addEntry(metadata, layerName);
        db.saveReconstructionAsZippedJson(json, layerName);

        // Done! Return final layer name
        return layerName;
    }

    private String handleReconstructionFromScenario(JsonValue json, JSONArray roadGraph) throws Exception {
        Logger.log("[Servlet] Handling reconstruction from scenario upload");
        JSONObject jbody = new JSONObject(json.toString());
        String layerName = "";
        try {
            // [1] Get infos from JD20, output of TFR ///////////////
            String broker = jbody.getJSONObject("metadata").getString("broker");
            String access_token = jbody.getJSONObject("metadata").getString("access_token");
            String dashURL = jbody.getJSONObject("metadata").getString("base_url");
            String baseSURI = jbody.getJSONObject("metadata").getString("baseSURI");
            String organization = jbody.getJSONObject("metadata").getString("organization");

            String dateTime = jbody.getJSONObject("metadata").getString("dateTime");
            // LocalDateTime dateTime_ = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            // String dateObserved = dateTime_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));

            String dateObserved;  
            if(dateTime.substring(dateTime.length()-1).equals("Z")){ // dateTime is in UTC, ensure that milliseconds are set
                Instant instant = Instant.parse(dateTime);
                dateObserved = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC).format(instant);
            } else {
                String tmp = dateTime.split("T")[1];
                if(tmp.contains("+") || tmp.contains("-")) { // dateTime is in a local time
                    OffsetDateTime dateTimeWithOffset = OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    OffsetDateTime dateTimeInUTC = dateTimeWithOffset.withOffsetSameInstant(ZoneOffset.UTC);
                    dateObserved = dateTimeInUTC.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
                } else { // dateTime is in an unknown timezone
                    LocalDateTime localDateTime = LocalDateTime.parse(dateTime);
                    ZoneId localZoneId = ZoneId.systemDefault();
                    ZonedDateTime zonedDateTime = localDateTime.atZone(localZoneId);
                    ZonedDateTime utcZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
                    Instant instant = utcZonedDateTime.toInstant();
                    dateObserved = instant.toString();
                }
            }

            String scenarioName = jbody.getJSONObject("metadata").getString("scenarioID");
            String scenarioSURI = jbody.getJSONObject("metadata").getString("scenarioSURI");
            String scenarioDT = jbody.getJSONObject("metadata").getString("scenarioDT");
            String durationString = jbody.getJSONObject("metadata").getString("duration"); // has the format XXmin
                                                                                           // ALWAYS
            String[] parts = durationString.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            int duration = -1;
            for (String part : parts) {
                if (Character.isDigit(part.charAt(0))) { // part is a number
                    duration = Integer.parseInt(part);
                }
            }
            String metricName = jbody.getJSONObject("metadata").getString("metricName");
            // NOTE: In OpenSearch densities are converted from "vehicle per 20m" to "vehicle per 1km"
            String unitOfMeasure = "vehicle per 1km"; //jbody.getJSONObject("metadata").getString("unitOfMeasure");
            String locality = jbody.getJSONObject("metadata").getString("locality");

            layerName = locality + "_" + scenarioName + "_" + dateTime;

            JSONObject res = null;

            // [2] Define TF Device name according to the schema: <scenarioName>_TFR_<name_num> ///////////////
            int name_num = 1;
            String device_name = scenarioName + "_TFR_" + Integer.toString(name_num);

            // [3] Check if device exist and if its scenarioDT correspond to the timestamp recieved from the TFR. ///////////////
            // If not, name_num is incremented. 
            // The loop continues until a correct device is found or the device_name do not exist 
            int instances = 1;
            boolean goon = true;
            boolean createDevice = false;
            while (goon) {
                String SURI = IOTDirectoryFunc.buildSURI(device_name, broker, organization, baseSURI);
                Logger.log("[TFM] Searching for device with SURI: " + SURI);

                res = IOTDirectoryFunc.getDeviceFromSURI(SURI, access_token, dashURL);
                // System.out.println(res.toString(2));

                int statusCode = res.getInt("statusCode");
                if (statusCode != 400) { // a device with device_name has been found
                    JSONObject tmp = new JSONObject(res.getString("responseBody"));
                    String sdt = "";
                    int instNum = -1;
                    try { // get device values scenarioDT and instances
                        sdt = tmp.getJSONObject("realtime").getJSONObject("results").getJSONArray("bindings")
                                .getJSONObject(0).getJSONObject("scenarioDT").getString("value");
                        instNum = tmp.getJSONObject("realtime").getJSONObject("results").getJSONArray("bindings")
                                .getJSONObject(0).getJSONObject("instances").getInt("value");
                    } catch (Exception e) {
                        // if enter here the device has no data... it is strange.
                        // however the code goes on searching for and possibly creating a new
                        // device
                        Logger.log("[TFM] Error in reading TFR device data. Device " + device_name
                                + " seems empty. This is strange...");
                        name_num = name_num + 1;
                        device_name = scenarioName + "_TFR_" + Integer.toString(name_num);
                    }
                    if (sdt.equals(scenarioDT)) {
                        // device exist and the scenarioDT is OK, exit
                        goon = false;
                        createDevice = false;
                        instances = instNum + 1;
                        Logger.log("[TFM] Device found! Increment instances and send data");
                    } else {
                        // device exist but its scenarioDT is not OK, then increment name_num and try
                        // again
                        name_num = name_num + 1;
                        device_name = scenarioName + "_TFR_" + Integer.toString(name_num);
                        Logger.log("[TFM] Device DT wrong! Test new device name: " + device_name);
                    }
                } else {
                    // the device do not exist, exit loop and create a new one using the last
                    // device_name
                    goon = false;
                    createDevice = true;
                    Logger.log("[TFM] Device NOT found! Continue and create a new one with name: " + device_name);
                }
            }

            // [4] Create a new device if required ///////////////
            if (createDevice) {

                // build WKT of the TFR as a bounding box including all the road elements
                float min_lat = Float.POSITIVE_INFINITY;
                float min_lon = Float.POSITIVE_INFINITY;
                float max_lat = Float.NEGATIVE_INFINITY;
                float max_lon = Float.NEGATIVE_INFINITY;
                for (int i = 0; i < roadGraph.length(); i++) {
                    JSONObject element = roadGraph.getJSONObject(i);
                    float element_nALat = element.getFloat("nALat");
                    if (element_nALat <= min_lat) {
                        min_lat = element_nALat;
                    }
                    if (element_nALat >= max_lat) {
                        max_lat = element_nALat;
                    }
                    float element_nALon = element.getFloat("nALong");
                    if (element_nALon <= min_lon) {
                        min_lon = element_nALon;
                    }
                    if (element_nALon >= max_lon) {
                        max_lon = element_nALon;
                    }
                    float element_nBLat = element.getFloat("nBLat");
                    if (element_nBLat <= min_lat) {
                        min_lat = element_nBLat;
                    }
                    if (element_nBLat >= max_lat) {
                        max_lat = element_nBLat;
                    }
                    float element_nBLon = element.getFloat("nBLong");
                    if (element_nBLon <= min_lon) {
                        min_lon = element_nBLon;
                    }
                    if (element_nBLon >= max_lon) {
                        max_lon = element_nBLon;
                    }
                }
                // System.out.println("minLatLon = (" + min_lat + ", " + min_lon + ") --- maxLatLon = (" + max_lat + ", " + max_lon + ")");
                String device_wkt = "POLYGON((" + min_lon + " " + min_lat + ", " +
                        max_lon + " " + min_lat + ", " +
                        max_lon + " " + max_lat + ", " +
                        min_lon + " " + max_lat + ", " +
                        min_lon + " " + min_lat + "))";
                // System.out.println(device_wkt);

                // get lat/lon of WKT centroid
                String latitude =  Float.toString(((max_lat - min_lat) / 2) + min_lat);
                String longitude = Float.toString(((max_lon - min_lon) / 2) + min_lon);

                // create the device
                res = IOTDirectoryFunc.createTrafficFlowDevice(device_name, broker, access_token, latitude, longitude,
                        device_wkt, dashURL);
                // System.out.println(res.toString(2));
            }

            // [5] Send data to the device ///////////////

            // Build the data JSON. Values are defined according to the trafficFlowModel
            JSONObject data = new JSONObject();
            JSONObject dateObservedOBJ = new JSONObject();
            dateObservedOBJ.put("type", "string");
            dateObservedOBJ.put("value", dateObserved);
            data.put("dateObserved", dateObservedOBJ);
            JSONObject scenarioNameOBJ = new JSONObject();
            scenarioNameOBJ.put("type", "string");
            scenarioNameOBJ.put("value", scenarioName);
            data.put("scenarioName", scenarioNameOBJ);
            JSONObject scenarioSURIOBJ = new JSONObject();
            scenarioSURIOBJ.put("type", "string");
            scenarioSURIOBJ.put("value", scenarioSURI);
            data.put("scenarioSURI", scenarioSURIOBJ);
            JSONObject scenarioDTOBJ = new JSONObject();
            scenarioDTOBJ.put("type", "string");
            scenarioDTOBJ.put("value", scenarioDT);
            data.put("scenarioDT", scenarioDTOBJ);
            JSONObject organizationOBJ = new JSONObject();
            organizationOBJ.put("type", "string");
            organizationOBJ.put("value", organization);
            data.put("organization", organizationOBJ);
            JSONObject instancesOBJ = new JSONObject();
            instancesOBJ.put("type", "integer");
            instancesOBJ.put("value", instances);
            data.put("instances", instancesOBJ);
            JSONObject durationOBJ = new JSONObject();
            durationOBJ.put("type", "integer");
            durationOBJ.put("value", duration);
            data.put("duration", durationOBJ);
            JSONObject metricNameOBJ = new JSONObject();
            metricNameOBJ.put("type", "string");
            metricNameOBJ.put("value", metricName);
            data.put("metricName", metricNameOBJ);
            JSONObject unitOfMeasureOBJ = new JSONObject();
            unitOfMeasureOBJ.put("type", "string");
            unitOfMeasureOBJ.put("value", unitOfMeasure);
            data.put("unitOfMeasure", unitOfMeasureOBJ);
            JSONObject localityOBJ = new JSONObject();
            localityOBJ.put("type", "string");
            localityOBJ.put("value", locality);
            data.put("locality", localityOBJ);
            // System.out.println(data.toString(2));

            String device_type = "Traffic_flow";
            res = IOTDirectoryFunc.setDeviceValues(device_name, device_type, data.toString(), broker, access_token, dashURL);
            // System.out.println(res.toString(2));
        
        } catch (Exception e) {
            Logger.log("[Servlet] Error in handleReconstructionFromScenario: " + e);
            throw new Exception("[Servlet] Error in handleReconstructionFromScenario: " + e);
        }
        
        return layerName;
    }

    private void convertToShapefileAndUpload(String layerName, JsonArray staticGraph, JsonObject reconstructionData)
            throws Exception {

        Logger.log("[Servlet] Starting conversion to Shapefile...");

        // Setup tmp folders and filenames
        String tmpLayersFolder = ConfigProperties.getProperties().getProperty("tmpLayersFolder");
        String layerFolder = tmpLayersFolder + "/" + layerName;
        new File(tmpLayersFolder).mkdir();
        new File(layerFolder).mkdir();

        String outputShp = layerFolder + "/" + layerName + ".shp";
        String outputCsv = layerFolder + "/" + layerName + ".csv";
        String outputZip = layerFolder + "/" + layerName + ".zip";
        String outputDbf = layerFolder + "/" + layerName + ".dbf";
        String outputFix = layerFolder + "/" + layerName + ".fix";
        String outputPrj = layerFolder + "/" + layerName + ".prj";
        String outputShx = layerFolder + "/" + layerName + ".shx";

        // Merge static graph with reconstruction data by producing a single CSV file
        CSVExtractor.extract(staticGraph, reconstructionData, outputCsv);

        // Convert the CSV file into a Shapefile using GeoTools
        SHPExtractor.extract(outputCsv, outputShp);

        // Prepare for upload: zip {.dbf, .fix, .prj, .shp, .shx} files
        List<String> filesToZip = Arrays.asList(outputDbf, outputFix, outputPrj, outputShx, outputShp);
        FileZipper.zipFiles(filesToZip, outputZip);

        // Publish Shapefile as .zip
        // NOTE: specified workspace MUST exist already on the server!!!
        Properties properties = ConfigProperties.getProperties();
        String endpoint = properties.getProperty("geoServerUrl");
        String user = properties.getProperty("geoServerUser");
        String pass = properties.getProperty("geoServerPass");
        String workspace = properties.getProperty("geoServerWorkspace");
        String datastore = "ds_" + layerName; // datastore must be unique
        GeoServerHelper geoServerHelper = new GeoServerHelper(user, pass, endpoint, workspace);
        geoServerHelper.publishShp(datastore, outputZip);

        // Apply correct layer style
        // NOTE: specified style MUST exist already on the server!!!
        String styleName = properties.getProperty("geoServerStyleName");
        geoServerHelper.setLayerStyle(layerName, styleName);

        // Cleanup tmp layer folder
        FileUtils.deleteDirectory(new File(layerFolder));

        Logger.log("[Servlet] Done conversion and layer upload!");
    }

    /**
     * Parse and return the body of the specified HTTP request as a JSON object
     * 
     * @param request the HttpServletRequest object
     * @return the JSON value
     * @throws IOException exception if body is not in JSON format
     */
    public static JsonValue getJSONBody(HttpServletRequest request) throws IOException {

        String body;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        JsonValue result;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(request.getInputStream()));
                char[] charBuffer = new char[128];
                int bytesRead;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
            body = stringBuilder.toString();
            result = Json.createReader(new StringReader(body)).readValue();
        } catch (Exception e) {
            throw new IOException("Failed to parse body as JSON! Please check your input");
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return result;
    }
}