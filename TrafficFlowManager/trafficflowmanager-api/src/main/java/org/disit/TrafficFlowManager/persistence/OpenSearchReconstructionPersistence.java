package org.disit.TrafficFlowManager.persistence;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

//import java.util.TimeZone;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.util.EntityUtils;
//import org.checkerframework.checker.units.qual.K;
import org.disit.TrafficFlowManager.utils.CSVExtractor;
import org.disit.TrafficFlowManager.utils.ConfigProperties;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
//import org.elasticsearch.common.recycler.Recycler.V;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.disit.TrafficFlowManager.utils.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.elasticsearch.client.indices.GetIndexRequest;

import org.disit.TrafficFlowManager.utils.GeoTools;

public class OpenSearchReconstructionPersistence {

    // class to count the number of error occurred in this session
    private static class ErrorManager {
        private int errorCount = 0;
        boolean mustStop = false;
        String errorMessage;

        public synchronized void incrementErrorCount() {
            errorCount++;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void setMustStop() {
            mustStop = true;
        }

        public boolean getMustStop() {
            return mustStop;
        }

        public void writeError(String errorMess) {
            errorMessage = errorMess;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static int[] sendToEs(JSONObject dinamico, String kind) throws Exception {
        int[] metadata = null;
        try{
            metadata = OpenSearchReconstructionPersistence.sendToEs(dinamico, null, kind);
        } catch (Exception e) {
            Logger.log("[TFM] Error in sendToEs: " + e);
            throw new Exception("[TFM] Failed to send data to ES: " + e);
        }
        return metadata;
    }

    public static int[] sendToEs(JSONObject dinamico, JSONArray roadGraph, String kind) throws Exception {
        boolean isNewScenario = true;
        if(roadGraph==null){
            isNewScenario = false;
        }
        try {

            Properties conf = ConfigProperties.getProperties();
            String url = conf.getProperty("opensearchHostname");
            if (url == null) {
                Logger.log("[TFM] opensearchHostname not specified in configuration NOT SENDING to opensearch");
                return null;
            }
            String[] hostnames = url.split(";");
            int port = 9200;
            String admin = conf.getProperty("opensearchUsername", "admin");
            String password = conf.getProperty("opensearchPassw", "password");
            String targetEPGS = conf.getProperty("cartesianEPGS", "EPSG:32632");

            String kbUrl = conf.getProperty("kbUrl", "https://www.disit.org/smosm/sparql?format=json");

            int batchSize = Integer.parseInt(conf.getProperty("opensearchBatchSize", "150"));
            int threadNumber = Integer.parseInt(conf.getProperty("opensearchThreadNumber", "150"));
            int maxErrors = Integer.parseInt(conf.getProperty("opensearchMaxErrors", "200"));
            int threadNumberPostProcess = Integer.parseInt(conf.getProperty("postProcessThreadNumber", "5"));

            String indexName = conf.getProperty("opensearchIndexName", "roadelement4"); 

            LocalDate date = LocalDate.now();
            String failDir = conf.getProperty("failFolderTFM") + "/" + date;

            Path failureFolderPath = Paths.get(failDir);
            Files.createDirectories(failureFolderPath);

            createIndex(indexName, url.split(";")[0], port, admin, password);

            long startTime = -1;
            // Inizializza l'oggetto errorManager condiviso
            ErrorManager errorManager = new ErrorManager();
            JSONArray dataToES = null;

            if(isNewScenario){

                // Retrieve static data and related reconstruction
                String associatedStaticGraphName = dinamico.getJSONObject("metadata").getString("staticGraphName");
                String dateObserved = dinamico.getJSONObject("metadata").getString("dateTime");
                String scenario = dinamico.getJSONObject("metadata").getString("fluxName");

                JsonValue staticGraph = new JSONStaticGraphPersistence().getStaticGraph(associatedStaticGraphName);
                JsonArray staticDataGraph = staticGraph.asJsonObject().getJsonArray("dataGraph");

                String tmp_jsonString = dinamico.toString();
                JsonReader tmp_jsonReader = Json.createReader(new StringReader(tmp_jsonString));
                JsonValue body = tmp_jsonReader.readValue();
                tmp_jsonReader.close();
                JsonObject object = body.asJsonObject();
                JsonObject reconstructionData = object.getJsonObject("reconstructionData");

                // Merge static graph with reconstruction data by producing a single JSONObject file
                JSONObject mergedData = CSVExtractor.extractToJSON(staticDataGraph, reconstructionData); 

                // preprocess the data to define appropriate JSONObjects and convert lat/lon to Cartesian coordinates (approximated)
                JSONArray roadElements = mergedData.getJSONArray("roadElements");
                // JSONArray roadElementsINV = mergedData.getJSONArray("roadElementsINV");
                JSONArray data = mergedData.getJSONArray("data");

                JSONObject roadElementsWithCoord = new JSONObject();
                for (int i = 0; i < roadElements.length(); i++){
                    String thisRE = "http://www.disit.org/km4city/resource/" + roadElements.getString(i); // TODO maybe the prefix should be an external parameter???
                    JSONObject thisREData = GeoTools.findObjectInArray(roadGraph, "segment", thisRE);
                    if(thisREData != null){
                        JSONObject newRE = new JSONObject();
                        newRE.put("nALat", thisREData.getDouble("nALat"));
                        newRE.put("nALong", thisREData.getDouble("nALong"));
                        newRE.put("nBLat", thisREData.getDouble("nBLat"));
                        newRE.put("nBLong", thisREData.getDouble("nBLong"));
                        newRE.put("type", thisREData.getString("type"));
                        newRE.put("lanes", thisREData.getString("lanes"));
                        newRE.put("roadElmSpeedLimit", thisREData.getDouble("roadElmSpeedLimit"));
                        newRE.put("length", thisREData.getDouble("length"));
                        double[] cartCoordA = GeoTools.getCartisianApproximatedCoordinates(thisREData.getDouble("nALat"), thisREData.getDouble("nALong"), targetEPGS);
                        double[] cartCoordB = GeoTools.getCartisianApproximatedCoordinates(thisREData.getDouble("nBLat"), thisREData.getDouble("nBLong"), targetEPGS);
                        newRE.put("xA", cartCoordA[0]);
                        newRE.put("yA", cartCoordA[1]);
                        newRE.put("xB", cartCoordB[0]);
                        newRE.put("yB", cartCoordB[1]);
                        roadElementsWithCoord.put(roadElements.getString(i), newRE);
                    } else {
                        Logger.log("[TFM][sendToEs] Road Element not found in Scenario RoadGraph: " + roadElements.getString(i));
                        throw new Exception("[TFM][sendToEs] Road Element not found in Scenario RoadGraph: " + roadElements.getString(i));
                    }
                }

                // JSONObject roadElementsINVWithCoord = new JSONObject();
                // for (int i = 0; i < roadElementsINV.length(); i++){
                //     String thisRE = "http://www.disit.org/km4city/resource/" + roadElementsINV.getString(i); // TODO maybe the prefix should be an external parameter???
                //     JSONObject thisREData = GeoTools.findObjectInArray(roadGraph, "segment", thisRE);
                //     if(thisREData != null){
                //         JSONObject newRE = new JSONObject();
                //         newRE.put("nALat", thisREData.getDouble("nALat"));
                //         newRE.put("nALong", thisREData.getDouble("nALong"));
                //         newRE.put("nBLat", thisREData.getDouble("nBLat"));
                //         newRE.put("nBLong", thisREData.getDouble("nBLong"));
                //         newRE.put("type", thisREData.getString("type"));
                //         newRE.put("lanes", thisREData.getString("lanes"));
                //         newRE.put("roadElmSpeedLimit", thisREData.getDouble("roadElmSpeedLimit"));
                //         newRE.put("length", thisREData.getDouble("length"));
                //         double[] cartCoordA = GeoTools.getCartisianApproximatedCoordinates(thisREData.getDouble("nALat"), thisREData.getDouble("nALong"));
                //         double[] cartCoordB = GeoTools.getCartisianApproximatedCoordinates(thisREData.getDouble("nBLat"), thisREData.getDouble("nBLong"));
                //         newRE.put("xA", cartCoordA[0]);
                //         newRE.put("yA", cartCoordA[1]);
                //         newRE.put("xB", cartCoordB[0]);
                //         newRE.put("yB", cartCoordB[1]);
                //         roadElementsINVWithCoord.put(roadElementsINV.getString(i), newRE);
                //     } else {
                //         System.err.println("Road Element NOT FOUND in RoadGraph!!! >>> RE: " + roadElementsINV.getString(i));
                //     }
                // }

                JSONObject accSegments = new JSONObject();
                Map<String, Integer> isSegmentAssociated = new LinkedHashMap<>();
                for(int i = 0; i<data.length(); i++){
                    
                    JSONObject thisSegment = data.getJSONObject(i);
                    String thisSegmentID = thisSegment.getString("segmentId");

                    isSegmentAssociated.put(thisSegmentID, 0);

                    String[] tmp = thisSegmentID.split("\\.");
                    String thisAccSegmentID = tmp[0];
                    JSONArray segs = null;
                    if(accSegments.has(thisAccSegmentID)){
                        segs = accSegments.getJSONArray(thisAccSegmentID);
                    } else {
                        segs = new JSONArray();
                    }
                    double startLat = Double.parseDouble(thisSegment.getString("startLat"));
                    double endLat = Double.parseDouble(thisSegment.getString("endLat"));
                    double startLon = Double.parseDouble(thisSegment.getString("startLon"));
                    double endLon = Double.parseDouble(thisSegment.getString("endLon")); 
                    double[] cartCoordA = GeoTools.getCartisianApproximatedCoordinates(startLat, startLon, targetEPGS);
                    double[] cartCoordB = GeoTools.getCartisianApproximatedCoordinates(endLat, endLon, targetEPGS);
                    thisSegment.put("xA", cartCoordA[0]);
                    thisSegment.put("yA", cartCoordA[1]);
                    thisSegment.put("xB", cartCoordB[0]);
                    thisSegment.put("yB", cartCoordB[1]);
                    segs.put(thisSegment);
                    accSegments.put(thisAccSegmentID, segs);
                }

                // PROCESSING DATA //////////////////////////////////////////////////////////////////////////////////////////////////////////

                JSONObject prep_roadElementToOS = new JSONObject();

                // For each segment find the closest road element to which the segment should be assigned     
                // 1. Iterate on all the acc groups of the segments       
                Iterator<String> accKeys = accSegments.keys();
                while (accKeys.hasNext()) {
                    // 2. restrieve the acc group and all the segments in the group
                    String key = accKeys.next();
                    JSONArray segs = accSegments.getJSONArray(key);

                    // 3. get the IDs of the road eleements associated with the acc group
                    String[] reIDs = key.split("--");

                    for (int j = 0; j < segs.length(); j++){
                        // 4. get the j-th segment in the acc group
                        JSONObject thisSeg = segs.getJSONObject(j); 
                        double xAj = thisSeg.getDouble("xA");
                        double xBj = thisSeg.getDouble("xB");
                        double yAj = thisSeg.getDouble("yA");
                        double yBj = thisSeg.getDouble("yB");

                        // Line B defined by its endpoints (longitude, latitude)
                        double[][] jSegLine = {
                            {xAj, yAj},
                            {xBj, yBj}
                        };
                        
                        // 5. iterate on all the road elements associated with the acc group
                        boolean[] intersections = new boolean[reIDs.length];
                        double[] distances = new double[reIDs.length];
                        for (int i = 0; i < reIDs.length; i++){
                            // 6. get the i-th road element
                            String reID = reIDs[i];
                            JSONObject thisRE = null;
                            // if(roadElementsWithCoord.has(reID)){
                                thisRE = roadElementsWithCoord.getJSONObject(reID);
                            // } else if (roadElementsINVWithCoord.has(reID)){
                            //     thisRE = roadElementsINVWithCoord.getJSONObject(reID);
                            // }
                            if(thisRE == null){
                                throw new Exception("Road elememt " + reID + " not found.");
                            }
                            double xAi = thisRE.getDouble("xA");
                            double yAi = thisRE.getDouble("yA");
                            double xBi = thisRE.getDouble("xB");
                            double yBi = thisRE.getDouble("yB");

                            double[][] iRELine = {
                                {xAi, yAi},
                                {xBi, yBi}
                            };
                
                            // 7. check if the perpendicular line from the midpoint of jSegLine intersects iRELine and compute 
                            //    the distance between the endpoints of jSegLine and iRELine
                            intersections[i] = GeoTools.doesPerpendicularIntersect(iRELine, jSegLine);
                            double[] pairDists = new double[4];
                            pairDists[0] = GeoTools.computeDistanceXY(xAi, yAi, xAj, yAj);
                            pairDists[1] = GeoTools.computeDistanceXY(xAi, yAi, xBj, yBj);
                            pairDists[2] = GeoTools.computeDistanceXY(xBi, yBi, xAj, yAj);
                            pairDists[3] = GeoTools.computeDistanceXY(xBi, yBi, xBj, yBj);
                            distances[i] = GeoTools.getMinValue(pairDists);
                        }

                        // 8. check if more than one road element is found intersecting the perpendicular of the segment                    
                        String closestReID = null;
                        ArrayList<Integer> idxs = GeoTools.getIndexesOfTrueValues(intersections);
                        if(idxs.size() == 1){
                            // 8.1 select the unique intersecting road element for the association
                            closestReID = reIDs[idxs.get(0)];    
                        } else if (idxs.size() > 1){
                            // 8.2 if found more than one, associate the road element at minimum distance among the intersecting ones 
                            double mindist = Double.MAX_VALUE;
                            int minidx = -1;
                            for (int i = 0; i < idxs.size(); i++) {
                                int idx = idxs.get(i);
                                if(distances[idx] < mindist){
                                    mindist = distances[idx];
                                    minidx = idx;
                                }
                                if(minidx != -1){
                                    closestReID = reIDs[minidx];
                                }
                            }                        
                        } else {
                            Logger.log("[TFM][sendToEs] No association found for segment " + thisSeg.getString("segmentId"));
                            // System.out.println(Arrays.toString(intersections));
                            // System.out.println(Arrays.toString(distances));
                            // throw new Exception("[TFM][sendToEs] No association found for segment " + thisSeg.getString("segmentId"));
                        }

                        if(closestReID != null){
                            Integer check = isSegmentAssociated.get(thisSeg.getString("segmentId"));
                            if(check==0){
                                isSegmentAssociated.put(thisSeg.getString("segmentId"), 1);
                            }  else if (check == null){
                                Logger.log("[TFM][sendToEs] Segment index mismatch: " + thisSeg.getString("segmentId")); 
                                throw new Exception("[TFM][sendToEs] Segment index mismatch: " + thisSeg.getString("segmentId") );
                            }  else if (check == 1){
                                Logger.log("[TFM][sendToEs] Segment already associated: " + thisSeg.getString("segmentId")); 
                                throw new Exception("[TFM][sendToEs] Segment already associated: " + thisSeg.getString("segmentId") );
                            }

                            JSONObject closestRe = null;
                            if(prep_roadElementToOS.has(closestReID)){
                                closestRe = prep_roadElementToOS.getJSONObject(closestReID);
                                closestRe.getJSONArray("associatedSegments").put(thisSeg);
                            } else {
                                JSONObject thisRE = null;
                                // if(roadElementsWithCoord.has(closestReID)){
                                    thisRE = roadElementsWithCoord.getJSONObject(closestReID);
                                // } else if (roadElementsINVWithCoord.has(closestReID)){
                                //     thisRE = roadElementsINVWithCoord.getJSONObject(closestReID);
                                // }
                                closestRe = new JSONObject();
                                closestRe.put("roadElements", closestReID);
                                closestRe.put("dateObserved", dateObserved);
                                closestRe.put("kind", kind);
                                closestRe.put("scenario", scenario);
                                closestRe.put("lane_numbers", Integer.parseInt(thisRE.getString("lanes")));
                                closestRe.put("dir", 0);
                                closestRe.put("vmax", thisRE.getFloat("roadElmSpeedLimit"));
                                closestRe.put("numVehicle", "");
                                closestRe.put("flow", "");
                                
                                JSONArray associatedSegments = new JSONArray();
                                associatedSegments.put(thisSeg);
                                closestRe.put("associatedSegments", associatedSegments);
                                prep_roadElementToOS.put(closestReID, closestRe);
                            }
                        }                   
                    }                    
                }

                // System.out.println(prep_roadElementToOS.toString(2));
                
                // Check if all the segments have been associated to some road element
                List<String> checkList = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : isSegmentAssociated.entrySet()) {
                    if (entry.getValue().equals(0)) {
                        checkList.add(entry.getKey());
                    }
                }
                if(checkList.size()>0){
                    String unassociatedSegments = "";
                    for(int i =0; i < checkList.size(); i++){
                        unassociatedSegments = unassociatedSegments + " " + checkList.get(i);
                    }
                    Logger.log("[TFM][sendToEs] Some segments have not been associated. List: " + unassociatedSegments); 
                    // throw new Exception("[TFM][sendToEs] Some segments have not been associated. List: " + unassociatedSegments);
                }                

                // Finalize the JSON to send to OpenSearch
                JSONArray roadElementToOS = new JSONArray();
                Iterator<String> keys = prep_roadElementToOS.keys();
                while(keys.hasNext()){
                    String key = keys.next();
                    JSONObject re = prep_roadElementToOS.getJSONObject(key);

                    JSONArray associatedSegments = re.getJSONArray("associatedSegments");
                    if(associatedSegments.length() == 1){
                        String startLat = associatedSegments.getJSONObject(0).getString("startLat");
                        String startLon = associatedSegments.getJSONObject(0).getString("startLon");
                        String endLat = associatedSegments.getJSONObject(0).getString("endLat");
                        String endLon = associatedSegments.getJSONObject(0).getString("endLon");
                        String trafficValue = associatedSegments.getJSONObject(0).getString("trafficValue");
                        String segmentID = associatedSegments.getJSONObject(0).getString("segmentId");

                        JSONObject endLocation = new JSONObject();
                        endLocation.put("lat", endLat);
                        endLocation.put("lon", endLon);
                        JSONObject end = new JSONObject();
                        end.put("location", endLocation);
                        re.put("end", end);

                        JSONObject startLocation = new JSONObject();
                        startLocation.put("lat", startLat);
                        startLocation.put("lon", startLon);
                        JSONObject start = new JSONObject();
                        start.put("location", startLocation);
                        re.put("start", start);

                        JSONObject line = new JSONObject();
                        JSONArray coordinates = new JSONArray();
                        JSONArray sCoord = new JSONArray();
                        sCoord.put(Float.parseFloat(startLon));
                        sCoord.put(Float.parseFloat(startLat));
                        coordinates.put(sCoord);
                        JSONArray eCoord = new JSONArray();
                        eCoord.put(Float.parseFloat(endLon));
                        eCoord.put(Float.parseFloat(endLat));
                        coordinates.put(eCoord);
                        line.put("coordinates", coordinates);
                        line.put("type", "LineString");
                        re.put("line", line);

                        re.put("density", Float.parseFloat(trafficValue) * 1000 / 20); // from #car per 20m to #car per 1km

                        JSONArray segments = new JSONArray();
                        JSONObject segment = new JSONObject();
                        segment.put("segment", segmentID);
                        segment.put("densityCar20m", trafficValue);
                        segment.put("density", Float.parseFloat(trafficValue) * 1000 / 20); // from #car per 20m to #car per 1km
                        segment.put("lineString", "LINESTRING (" + startLon + " " + startLat + ", " + endLon + " " + endLat + ")");
                        segments.put(segment);
                        re.put("segments", segments); 

                    } else if (associatedSegments.length() > 1) {
                        double sumTrafficValue = 0;
                        JSONArray segments = new JSONArray();
                        for (int i = 0; i < associatedSegments.length(); i++){
                            JSONObject segment = new JSONObject();
                            segment.put("segment", associatedSegments.getJSONObject(i).getString("segmentId"));
                            segment.put("densityCar20m", associatedSegments.getJSONObject(i).getString("trafficValue"));
                            segment.put("density", Float.parseFloat(associatedSegments.getJSONObject(i).getString("trafficValue")) * 1000 / 20); // from #car per 20m to #car per 1km
                            segment.put("linestring", "LINESTRING (" + 
                                associatedSegments.getJSONObject(i).getString("startLon") + " " + 
                                associatedSegments.getJSONObject(i).getString("startLat") + ", " + 
                                associatedSegments.getJSONObject(i).getString("endLon") + " " + 
                                associatedSegments.getJSONObject(i).getString("endLat") + ")");
                            segments.put(segment);
                            sumTrafficValue += Double.parseDouble(associatedSegments.getJSONObject(i).getString("trafficValue"));
                        }

                        re.put("segments", segments); 

                        double avgTrafficValue = sumTrafficValue / associatedSegments.length();
                        avgTrafficValue = avgTrafficValue * 1000 / 20; // from #car per 20m to #car per 1km
                        re.put("density", (float) avgTrafficValue);

                        String[] extrema = GeoTools.findCorrectOrder(associatedSegments);

                        JSONObject endLocation = new JSONObject();
                        endLocation.put("lat", extrema[2]);
                        endLocation.put("lon", extrema[3]);
                        JSONObject end = new JSONObject();
                        end.put("location", endLocation);
                        re.put("end", end);

                        JSONObject startLocation = new JSONObject();
                        startLocation.put("lat", extrema[0]);
                        startLocation.put("lon", extrema[1]);
                        JSONObject start = new JSONObject();
                        start.put("location", startLocation);
                        re.put("start", start);

                        JSONObject line = new JSONObject();
                        JSONArray coordinates = new JSONArray();
                        JSONArray sCoord = new JSONArray();
                        sCoord.put(Float.parseFloat(extrema[1]));
                        sCoord.put(Float.parseFloat(extrema[0]));
                        coordinates.put(sCoord);
                        JSONArray eCoord = new JSONArray();
                        eCoord.put(Float.parseFloat(extrema[3]));
                        eCoord.put(Float.parseFloat(extrema[2]));
                        coordinates.put(eCoord);
                        line.put("coordinates", coordinates);
                        line.put("type", "LineString");
                        re.put("line", line);

                    } else if (associatedSegments.length() == 0) {
                        Logger.log("[TFM][sendToEs] A road element lost its associated segment: " + key); 
                        // throw new Exception("[TFM][sendToEs] A road element lost its associated segment: " + key);
                    }

                    re.remove("associatedSegments");
                    roadElementToOS.put(re);

                }

                // System.out.println(roadElementToOS.toString(2));
                if(checkList.size()==0){
                    Logger.log("[TFM][sendToEs] All good. The road element set is ready to be sent to Open Search.");
                }

                dataToES = roadElementToOS;
            } else {
                // split road elements
                Logger.log("[TFM][sendToEs] processing dinamic json");
                JSONArray jd20 = preProcess(dinamico, kind, isNewScenario);
                JSONArray reArray = new JSONArray();
                Logger.log("[TFM][sendToEs] pre-processing JD20");
                reArray = splitRoadElement(jd20);

                // calcolo densità media per roadelement
                Map<String, Double> densityAverageMap = new HashMap<>();
                densityAverageMap = mapDensity(reArray);

                Logger.log("[TFM] building inverted JD20");
                JSONArray invertedArray = new JSONArray();

                // inversione indice
                invertedArray = invertedIndex(reArray, densityAverageMap);            

                Logger.log("[TFM][sendToEs] retrieving road element coordinates in KB");
                // creo client per le query
                CloseableHttpClient httpClient = HttpClients.createDefault();
                // recupero coordinate roadelement in KB
                startTime = System.currentTimeMillis();
                JSONObject coord = getCoord(invertedArray, batchSize, kbUrl, httpClient, errorManager);
                Logger.log("[TFM] time retrieving road element coordinates in KB: " + (System.currentTimeMillis() - startTime) + " ms");
                JSONArray coordArray = coord.getJSONArray("results");
                startTime = System.currentTimeMillis();
                PostProcessRes result = postProcess(invertedArray, coordArray, threadNumberPostProcess);
                Logger.log("[TFM] time post processing: " + (System.currentTimeMillis() - startTime) + " ms");
                invertedArray = result.getPostPorcessData();

                // System.out.println(invertedArray.toString(2));

                dataToES = invertedArray;
            }

            Logger.log("[TFM][sendToEs] indexing documents in elasticsearch");

            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(admin, password));

            RestClientBuilder[] builder = new RestClientBuilder[hostnames.length];
            for (int i = 0; i < builder.length; i++) {
                builder[i] = RestClient.builder(
                    new HttpHost(hostnames[i], port, "https"))
                    .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                        @Override
                        public HttpAsyncClientBuilder customizeHttpClient(
                                HttpAsyncClientBuilder httpClientBuilder) {
                            // Configura l'autenticazione HTTP
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                            return httpClientBuilder;
                        }
                    }
                );
            }

            // invio documenti in elasticsearch

            startTime = System.currentTimeMillis();
            int[] metadata = sendToIndex(threadNumber, dataToES, indexName, hostnames, port, admin, password,
                    maxErrors,
                    errorManager, builder, failDir);
            Logger.log("[TFM][sendToEs] time indexing all documents: " + (System.currentTimeMillis() - startTime)
                    + " ms");

            Logger.log("[TFM][sendToEs] done");
            return metadata;

        } catch (JSONException e) {
            Logger.log("[TFM][sendToEs] Error in processing and sending data to OpenSearch: " + e);
            throw new Exception("[TFM][sendToEs] Error in processing and sending data to OpenSearch: " + e);
        }
    }
    // ############################################# PROCESSING METHODS

    private static JSONArray preProcess(JSONObject dinamic, String kind, boolean isNewScenario) throws Exception {
        /*
         * This function split the TFR output creating a new JSONObject for each segment (of 20m) and reporting in 
         * the roadElements key the list of the road elments to which the segment is related
         */
        try {
            String tmp = "{\"scenario\":\"\",\"dateObserved\":\"\",\"segment\":\"\",\"dir\":0,\"roadElements\":[],\"start\":{\"location\":{\"lon\":\"\",\"lat\":\"\"}},\"end\":{\"location\":{\"lon\":\"\",\"lat\":\"\"}},\"flow\":\"\",\"density\":0,\"numVehicle\":\"\"}";
            JSONObject template = new JSONObject(tmp);
            JSONObject reconstructionData = dinamic.getJSONObject("reconstructionData");
            JSONObject metadata = dinamic.getJSONObject("metadata");
            
            //String dateTimeWithTimeZone = (String) ConfigProperties.getProperties().getOrDefault("dateTimeWithTimeZone", "no");
            if(isNewScenario){
                template.put("scenario", metadata.getString("scenarioID"));
            }else{                
                template.put("scenario", metadata.getString("fluxName")); // MOD del 2024-07-29 for legacy reconstructions
            }

            String dateObserved = metadata.getString("dateTime");

            // // Imposta come default la timeZone dell'Italia
            // if (dateTimeWithTimeZone.equals("yes") && !dateObserved.contains("+") && !dateObserved.contains("Z")) {

            //     TimeZone timeZone = TimeZone.getTimeZone("Europe/Rome");

            //     String[] d = dateObserved.split("-");
            //     String year = d[0];
            //     String month = d[1];
            //     String day = d[2].split("T")[0];

            //     Calendar calendar = Calendar.getInstance(timeZone);
            //     calendar.set(Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day), 0, 0, 0);
            //     // -1 al mese perchè gennaio corrisponde allo 0
            //     calendar.setTimeZone(timeZone);

            //     if (timeZone.inDaylightTime(calendar.getTime())) {
            //         dateObserved = dateObserved + "+02:00";
            //     } else {
            //         dateObserved = dateObserved + "+01:00";
            //     }
            // }

            template.put("dateObserved", dateObserved);
            template.put("kind", kind);
            JSONArray JD20 = new JSONArray();

            for (Object key : reconstructionData.keySet()) { // itero sulle road
                String field = (String) key;
                JSONObject fieldData = (JSONObject) reconstructionData.get(field);
                JSONArray data = (JSONArray) fieldData.get("data");

                for (Object item : data) { // itero sui segment

                    JSONObject itemData = (JSONObject) item;
                    for (Object itemKey : itemData.keySet()) {
                        int dir = 0;

                        JSONObject tmpTemplate = new JSONObject(template.toString()); // deep copy del template
                        String itemName = (String) itemKey;
                        String itemValue = (String) itemData.get(itemName);
                        tmpTemplate.put("segment", itemName);
                        tmpTemplate.put("density", itemValue);
                        if (itemName.contains("INV")) {
                            dir = -1;
                            String[] parts = itemName.split("INV");
                            itemName = parts[0];
                            tmpTemplate.put("dir", dir);
                        }

                        String[] roadElements = itemName.split("--");
                        JSONArray roadElementsArray = new JSONArray();

                        for (int i = 0; i < roadElements.length; i++) {
                            roadElements[i] = roadElements[i] + "_" + dir; // serve per distinguere i roadelemet
                                                                           // percorsi in senso invertito

                            if (i == roadElements.length - 1) {
                                // Dividi l'ultimo road element quando trovi "."
                                String[] lastElementParts = roadElements[i].split("\\.");
                                lastElementParts[0] = lastElementParts[0] + "_" + dir;
                                roadElements[i] = lastElementParts[0];
                            }

                            JSONObject tmpRoadElement = new JSONObject();
                            tmpRoadElement.put("roadElement", roadElements[i]);
                            roadElementsArray.put(tmpRoadElement);
                        }
                        tmpTemplate.put("roadElements", roadElementsArray);
                        JD20.put(tmpTemplate);
                    }
                }

            }

            return JD20;
        } catch (Exception e) {
            Logger.log("[TFM] Error processing dynamic json: " + e);
            throw new Exception("[TFM] Failed to send data to ES: " + e);
        }
    }

    private static JSONArray splitRoadElement(JSONArray jd20) throws Exception {
        JSONArray reArray = new JSONArray();
        try {
            for (int i = 0; i < jd20.length(); i++) {
                JSONObject jsonObject = jd20.getJSONObject(i);
                JSONArray roadElementsArray = jsonObject.getJSONArray("roadElements");

                for (int j = 0; j < roadElementsArray.length(); j++) {
                    JSONObject duplicatedObject = new JSONObject(jsonObject, JSONObject.getNames(jsonObject));
                    JSONArray tmp = new JSONArray();
                    JSONObject roadElementObject = roadElementsArray.getJSONObject(j);
                    String roadElement = roadElementObject.getString("roadElement");
                    tmp.put(roadElement);
                    duplicatedObject.put("roadElements", tmp);
                    reArray.put(duplicatedObject);
                }
            }
            return reArray;
        } catch (Exception e) {
            Logger.log("[TFM] Error splitting the dynamic json");
            throw new Exception("[TFM] Failed to send data to ES: " + e);
        }
    }

    private static Map<String, Double> mapDensity(JSONArray reArray) throws Exception {
        // Map "road element" con "density"
        Map<String, Double> densitySumMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, Double> densityAverageMap = new HashMap<>();

        try {
            for (int i = 0; i < reArray.length(); i++) {
                JSONObject jsonObject = reArray.getJSONObject(i);
                String roadElement = jsonObject.getJSONArray("roadElements").getString(0);
                double density = jsonObject.getDouble("density");

                // Aggiorna la somma delle densità per il road element
                if (densitySumMap.containsKey(roadElement)) {
                    double currentSum = densitySumMap.get(roadElement);
                    densitySumMap.put(roadElement, currentSum + density);
                } else {
                    densitySumMap.put(roadElement, density);
                }

                // Incrementa il contatore per il road element
                if (countMap.containsKey(roadElement)) {
                    int currentCount = countMap.get(roadElement);
                    countMap.put(roadElement, currentCount + 1);
                } else {
                    countMap.put(roadElement, 1);
                }
            }

            // Calcola la media per ciascun road element
            for (String roadElement : densitySumMap.keySet()) {
                double totalDensity = densitySumMap.get(roadElement);
                int count = countMap.get(roadElement);
                double averageDensity = totalDensity / count;
                densityAverageMap.put(roadElement, averageDensity);
            }

        } catch (Exception e) {
            Logger.log("[TFM] Error mapping the density: " + e);
            throw new Exception("[TFM] Failed to send data to ES: " + e);
        }
        ;

        return densityAverageMap;
    }

    private static JSONArray invertedIndex(JSONArray reArray, Map<String, Double> densityAverageMap) throws Exception {
        // Map per tenere traccia dei "road element" visitati
        Set<String> seenRoadElements = new HashSet<>();
        JSONArray invertedArray = new JSONArray();

        JSONArray filteredArray = new JSONArray();

        try {

            // filtro duplicati
            for (int i = 0; i < reArray.length(); i++) {
                JSONObject jsonObject = reArray.getJSONObject(i);
                String roadElement = jsonObject.getJSONArray("roadElements").getString(0);

                // verifica se road element è stato già visto
                if (!seenRoadElements.contains(roadElement)) {
                    seenRoadElements.add(roadElement);
                    filteredArray.put(jsonObject);
                }
            }

            // costruzione indice invertito
            Map<String, JSONArray> invertedMap = new HashMap<>();

            for (int i = 0; i < reArray.length(); i++) {
                JSONObject jsonObject = reArray.getJSONObject(i);
                String roadElement = jsonObject.getJSONArray("roadElements").getString(0);
                String segment = jsonObject.getString("segment");

                if (!invertedMap.containsKey(roadElement)) {
                    invertedMap.put(roadElement, new JSONArray());
                }

                invertedMap.get(roadElement).put(segment);

            }

            for (int i = 0; i < filteredArray.length(); i++) {
                JSONObject jsonObject = filteredArray.getJSONObject(i);

                JSONArray segments = invertedMap.get(jsonObject.getJSONArray("roadElements").getString(0));

                // Crea un nuovo array JSON per gli oggetti "roadElement"
                JSONArray newSegments = new JSONArray();

                // Aggiungi gli oggetti "roadElement" all'array nuovo
                for (int j = 0; j < segments.length(); j++) {
                    String roadElementValue = segments.getString(j);
                    JSONObject roadElementObject = new JSONObject();
                    roadElementObject.put("segment", roadElementValue);
                    roadElementObject.put("densityCar20m", "");
                    roadElementObject.put("density", "");
                    roadElementObject.put("linestring", "");
                    newSegments.put(roadElementObject);
                }

                jsonObject.put("segments", newSegments);
                jsonObject.remove("segment");
                jsonObject.put("density",
                        (densityAverageMap.get(jsonObject.getJSONArray("roadElements").getString(0)) * 1000 / 20));
                String reName = jsonObject.getJSONArray("roadElements").getString(0);
                int index_ = reName.indexOf("_");
                jsonObject.put("roadElements", reName.substring(0, index_));
                invertedArray.put(jsonObject);
            }

        } catch (Exception e) {
            Logger.log("[TFM] Error building the inverted index: " + e);
            throw new Exception("[TFM] Failed to send data to ES: " + e);
        }

        return invertedArray;
    }

    // oggetti risultato del post process
    private static class PostProcessRes {
        private JSONArray data;
        private double minlat;
        private double maxlat;
        private double minlong;
        private double maxlong;

        public PostProcessRes(JSONArray data, double minlat, double maxlat, double minlong, double maxlong) {
            this.data = data;
            this.minlat = minlat;
            this.maxlat = maxlat;
            this.minlong = minlong;
            this.maxlong = maxlong;
        }

        public JSONArray getPostPorcessData() {
            return data;
        }

        public double[] getMinMaxCoordinates() {
            double coord[] = new double[4];
            coord[0] = minlat;
            coord[1] = maxlat;
            coord[2] = minlong;
            coord[3] = maxlong;
            return coord;
        }
    }

    static class PostProcessThread extends Thread {

        private JSONArray partialArray;
        private JSONArray coordArray;
        private List<PostProcessRes> totalRes;
        private PostProcessRes res;

        public PostProcessThread(JSONArray partialArray, JSONArray coordArray, List<PostProcessRes> totalRes) {
            this.partialArray = partialArray;
            this.coordArray = coordArray;
            this.totalRes = totalRes;
        }

        @Override
        public void run() {
            try {

                // MODIFICA MARCO ////////////////////////

                double minLat = Double.POSITIVE_INFINITY;
                double minLong = Double.POSITIVE_INFINITY;
                double maxLat = -1 * Double.POSITIVE_INFINITY;
                double maxLong = -1 * Double.POSITIVE_INFINITY;

                ////////////////////////////////////////////////////////

                for (int i = 0; i < partialArray.length(); i++) {
                    String roadelementInverted = partialArray.getJSONObject(i).getString("roadElements");
                    boolean foundMatch = false;

                    // System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< " + roadelementInverted + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                    // System.out.println(partialArray.getJSONObject(i).toString(2));

                    // Scorrere il coordArray per cercare una corrispondenza
                    int j = 0;

                    while (!foundMatch && j < coordArray.length()) {
                        String roadelementCoord = coordArray.getJSONObject(j).getJSONObject("id").getString("value");

                        if (roadelementInverted.equals(roadelementCoord)) {
                            foundMatch = true;
                        } else {
                            j++; // Incrementa j per passare all'elemento successivo in coordArray
                        }
                    }

                    if (!foundMatch) {
                        // System.out.println("Il road element: " +
                        // invertedArray.getJSONObject(i).getString("roadElements")
                        // + " non ha trovato corrispondenza in KB!!");
                        partialArray.remove(i);
                        i--; // Decrementa l'indice per continuare la verifica con il nuovo elemento in

                        // questa posizione
                    } else {

                        String slong = coordArray.getJSONObject(j).getJSONObject("slong").getString("value");
                        String slat = coordArray.getJSONObject(j).getJSONObject("slat").getString("value");
                        String elong = coordArray.getJSONObject(j).getJSONObject("elong").getString("value");
                        String elat = coordArray.getJSONObject(j).getJSONObject("elat").getString("value");
                        String lane_num = coordArray.getJSONObject(j).getJSONObject("lane_num").getString("value");
                        String vmax = coordArray.getJSONObject(j).getJSONObject("vmax").getString("value");
                        
                        // System.out.println(slong + "|" + slat + "|" + elong + "|" + elat);

                        // MODIFICA MARCO ///////////////////////////////////////
                        double slatDouble = Double.valueOf(slat);
                        double slongDouble = Double.valueOf(slong);
                        double elatDouble = Double.valueOf(elat);
                        double elongDouble = Double.valueOf(elong);

                        if (slatDouble < minLat) {
                            minLat = slatDouble;
                        }
                        if (slatDouble > maxLat) {
                            maxLat = slatDouble;
                        }
                        if (slongDouble < minLong) {
                            minLong = slongDouble;
                        }
                        if (slongDouble > maxLong) {
                            maxLong = slongDouble;
                        }

                        if (elatDouble < minLat) {
                            minLat = elatDouble;
                        }
                        if (elatDouble > maxLat) {
                            maxLat = elatDouble;
                        }
                        if (elongDouble < minLong) {
                            minLong = elongDouble;
                        }
                        if (elongDouble > maxLong) {
                            maxLong = elongDouble;
                        }

                        ////////////////////////////////////////////////////////

                        partialArray.getJSONObject(i).getJSONObject("start").getJSONObject("location").put("lon", slong);

                        partialArray.getJSONObject(i).getJSONObject("start").getJSONObject("location").put("lat", slat);

                        partialArray.getJSONObject(i).getJSONObject("end").getJSONObject("location").put("lon", elong);

                        partialArray.getJSONObject(i).getJSONObject("end").getJSONObject("location").put("lat", elat);

                        String lineString = "{\"type\": \"LineString\",\"coordinates\": [[" + slong + ", " + slat
                                + "], ["
                                + elong + ", " + elat + "]]}";

                        JSONObject line = new JSONObject(lineString);

                        partialArray.getJSONObject(i).put("line", line);

                        partialArray.getJSONObject(i).put("lane_numbers", lane_num);

                        partialArray.getJSONObject(i).put("vmax", vmax);

                        // System.out.println(partialArray.getJSONObject(i).toString(2));

                    }

                }
                res = new PostProcessRes(partialArray, minLat, maxLat, minLong, maxLong);
            } catch (Exception e) {
                Logger.log("[TFM] Error in post processing: " + e);
            }
            synchronized (totalRes) {
                totalRes.add(res);
            }
        }
    }

    private static PostProcessRes postProcess(JSONArray invertedArray, JSONArray coordArray, int threadNumber)
            throws Exception {

        // suddivido invertedArray in parti uguali da dare ai thread
        int arrayLen = invertedArray.length();

        // Calcola la dimensione esatta di ogni parte
        int dimensioneParte = (int) Math.ceil((double) arrayLen / threadNumber);

        // Inizializza un array di JSONArray per contenere le parti
        JSONArray[] partsArray = new JSONArray[threadNumber];


        // Suddividi il JSONArray in n parti
        for (int i = 0; i < threadNumber; i++) {
            int inizio = i * dimensioneParte;
            int fine = Math.min((i + 1) * dimensioneParte, arrayLen);

            // Estrai la parte corrente direttamente nella logica
            JSONArray parteCorrente = new JSONArray();
            for (int j = inizio; j < fine; j++) {
                parteCorrente.put(invertedArray.get(j));
            }

            // Assegna la parte all'array di parti
            partsArray[i] = parteCorrente;
        }

        List<Thread> threads = new ArrayList<>();
        List<PostProcessRes> totalRes = new ArrayList<>();

        for (int i = 0; i < threadNumber; i++) {
            Thread PostprocessThread = new PostProcessThread(partsArray[i], coordArray, totalRes);
            threads.add(PostprocessThread);
            PostprocessThread.start();
        }

        // Attendere il completamento di tutti i thread
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        PostProcessRes res;
        JSONArray tmp = new JSONArray();
        double minLat = Double.POSITIVE_INFINITY;
        double minLong = Double.POSITIVE_INFINITY;
        double maxLat = -1 * Double.POSITIVE_INFINITY;
        double maxLong = -1 * Double.POSITIVE_INFINITY;

        double coord[] = new double[4];

        // unisco i risultati
        for (int j = 0; j < totalRes.size(); j++) {
            // tmp.put(totalRes.get(j).getPostPorcessData());
            JSONArray dataTmp = totalRes.get(j).getPostPorcessData();
            for (int k = 0; k < dataTmp.length(); k++) {
                tmp.put(dataTmp.getJSONObject(k));
            }

            coord = totalRes.get(j).getMinMaxCoordinates();

            if (coord[0] < minLat) {
                minLat = coord[0];
            }

            if (coord[2] < minLong) {
                minLong = coord[2];
            }

            if (coord[1] > maxLat) {
                maxLat = coord[1];
            }

            if (coord[3] > maxLong) {
                maxLong = coord[3];
            }
        }
        res = new PostProcessRes(tmp, minLat, maxLat, minLong, maxLong);

        return res;
    }
    // ############################################# QUERY METHODS

    // thread per la richiesta delle coordinate dei road element
    static class KBThread extends Thread {
        private String query;
        private String sparqlEndpoint;
        private CloseableHttpClient httpClient;
        private JSONArray allResults;
        private ErrorManager errorManager;

        public KBThread(int index, String query, String sparqlEndpoint, JSONArray allResults,
                CloseableHttpClient httpClient, ErrorManager errorManager) {
            this.query = query;
            this.allResults = allResults;
            this.sparqlEndpoint = sparqlEndpoint;
            this.httpClient = httpClient;
            this.errorManager = errorManager;
        }

        @Override
        public void run() {

            String encodedQuery;
            JSONArray resultsArray = null;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
                HttpGet httpGet = new HttpGet(sparqlEndpoint + "&query=" + encodedQuery);
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    if (response.getStatusLine().getStatusCode() != 200) {
                        Logger.log("[TFM] Error KBthread: " + response);
                    }
                    ;
                    String resp = EntityUtils.toString(response.getEntity());
                    JSONObject jsonResp = new JSONObject(resp);
                    resultsArray = jsonResp.getJSONObject("results").getJSONArray("bindings");
                }

            } catch (Exception e) {
                Logger.log("[TFM] Error retrieving road element coordinates in KB: " + e);

                errorManager.setMustStop();
                errorManager.writeError(e.getMessage());
            }
            synchronized (allResults) {
                // System.out.println("thread" + index + " " + resultsArray.get(0));
                for (int i = 0; i < resultsArray.length(); i++) {
                    allResults.put(resultsArray.getJSONObject(i));
                }
            }

        }
    }

    private static JSONObject getCoord(JSONArray invertedArray, int batchSize, String kbUrl,
            CloseableHttpClient httpClient, ErrorManager errorManager) throws Exception {

        JSONObject totalJsonResp = new JSONObject(); // Oggetto JSON che conterrà tutti i risultati

        try {
            List<String> seenRoadElement = new ArrayList<>();

            for (int i = 0; i < invertedArray.length(); i++) {
                JSONObject jsonObject = invertedArray.getJSONObject(i);
                String roadElement = jsonObject.getString("roadElements");

                if (!seenRoadElement.contains(roadElement)) {
                    seenRoadElement.add(roadElement);
                }
            }

            String sparqlEndpoint = kbUrl;
            int totalElements = seenRoadElement.size();
            List<String> queryList = new ArrayList<>();

            JSONArray allResults = new JSONArray(); // Array per accumulare tutti i risultati

            for (int start = 0; start < totalElements; start += batchSize) {
                int end = Math.min(start + batchSize, totalElements);
                StringBuilder filter = new StringBuilder();

                for (int i = start; i < end; i++) {
                    filter.append("?id=\"").append(seenRoadElement.get(i)).append("\"");
                    if (i < end - 1) {
                        filter.append(" || ");
                    }
                }
                
                String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                        "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                        "SELECT   (xsd:string(?id) as ?id) (xsd:string(?slong) as ?slong) (xsd:string(?slat) as ?slat) " +
                        "(xsd:string(?elong) as ?elong) (xsd:string(?elat) as ?elat) " +
                        "(IF(bound(?lane_num),?lane_num,1) as ?lane_num) (IF(bound(?vmax),?vmax,\"50\") as ?vmax)\n"
                        +
                        "WHERE {\n" +
                        "  ?s a km4c:RoadElement.\n" +
                        "  ?s dct:identifier ?id.\n" +
                        "  ?s km4c:startsAtNode ?ns.\n" +
                        "  ?s km4c:endsAtNode ?ne.\n" +
                        "  ?ns geo:long ?slong.\n" +
                        "  ?ns geo:lat ?slat.\n" +
                        "  ?ne geo:long ?elong.\n" +
                        "  ?ne geo:lat ?elat.\n" +
                        "  OPTIONAL{\n" +
                        "    ?s km4c:lanes ?lanes.\n" +
                        "    ?lanes km4c:lanesCount ?numerolanes.\n" +
                        "    ?numerolanes km4c:undesignated ?lane_num.\n" +
                        "  }\n" +
                        "  OPTIONAL{?s km4c:speedLimit ?vmax.}\n" + 
                        "  FILTER (" + filter.toString() + ").\n" +
                        "}";
                queryList.add(queryString);
            }

            List<Thread> threads = new ArrayList<>();
            int index = 0;

            for (String query : queryList) {
                Thread KBThread = new KBThread(index, query, sparqlEndpoint, allResults, httpClient, errorManager);
                index++;
                threads.add(KBThread);
                KBThread.start();
            }

            // Attendere il completamento di tutti i thread
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (errorManager.getMustStop()) {
                throw new Exception("[TFM] Failed to retrive coordinates from KB: " + errorManager.getErrorMessage());
            }

            // Aggiungi l'array di tutti i risultati all'oggetto JSON di risposta
            totalJsonResp.put("results", allResults);

        } catch (Exception e) {
            Logger.log("[TFM] Error in getCoord: " + e);
            throw new Exception("[TFM] Failed to retrive coordinates from KB: " + errorManager.getErrorMessage());
        }
        return totalJsonResp;
    }

    private static int[] sendToIndex(int threadNumber, JSONArray invertedArray, String indexName, String[] url,
            int port,
            String admin, String password, int maxErrors, ErrorManager errorManager, RestClientBuilder[] builder,
            String failDir)
            throws Exception {

        try {
            int length = invertedArray.length();
            int numParts = Math.min(threadNumber, length); // si assicura che il numero di parti non superi la lunghezza
                                                           // dell'array
            int chunkSize = (int) Math.ceil((double) length / numParts); // Calcola la dimensione di ciascun chunk
            JSONArray[] dividedArrays = new JSONArray[numParts];

            for (int i = 0; i < numParts; i++) {
                dividedArrays[i] = new JSONArray(); // Crea un nuovo JSONArray per ogni parte
            }

            for (int i = 0; i < length; i++) {
                int chunkIndex = Math.min(i / chunkSize, numParts - 1); // Calcola l'indice della parte in cui
                                                                        // aggiungere l'elemento
                dividedArrays[chunkIndex].put(invertedArray.get(i));
            }

            List<Thread> threads = new ArrayList<>();
            List<int[]> metadataList = new ArrayList<>();
            int[] tmp = { 0, 0, 0 };
            metadataList.add(tmp);

            for (int i = 0; i < numParts; i++) {
                Thread ESThread = new ESThread(i, indexName, dividedArrays[i], url[i % url.length], maxErrors,
                        errorManager, builder[i % builder.length], metadataList, failDir);
                threads.add(ESThread);
                ESThread.start();
            }

            // Attendere il completamento di tutti i thread
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Logger.log("[TFM] Results,sent documents: " + metadataList.get(0)[0] + ",partial failures: "
                    + metadataList.get(0)[1]
                    + ",total failures: " + metadataList.get(0)[2]);

            if (errorManager.getMustStop()) {
                throw new Exception("[TFM] Failed to send data to ES: " + errorManager.getErrorMessage());
            }

            return metadataList.get(0);

        } catch (Exception e) {
            Logger.log("[TFM] Error sending document to ES: " + e);
            throw new Exception("[TFM] Failed to send data to ES: " + errorManager.getErrorMessage());
        }

    }

    // thread per inserimenti in ES
    static class ESThread extends Thread {
        private List<int[]> metadataList;
        private JSONArray documents;
        private String indexName;
        private String url;
        private int maxErrors;
        private ErrorManager errorManager;
        private RestClientBuilder builder;
        private String failDir;

        public ESThread(int index, String indexName, JSONArray documents, String url, int maxErrors,
                ErrorManager errorManager, RestClientBuilder builder, List<int[]> metadataList, String failDir) {
            this.metadataList = metadataList;
            this.documents = documents;
            this.indexName = indexName;
            this.url = url;
            this.maxErrors = maxErrors;
            this.errorManager = errorManager;
            this.builder = builder;
            this.failDir = failDir;
        }

        @Override
        public void run() {
            sendToIndexRes result = null;
            try {

                result = sendDoc(indexName, documents, url, maxErrors, errorManager,
                        builder, failDir);

                if (!result.getStatus()) {
                    Logger.log("[TFM] Max number of errors exceeded");

                    throw new Exception("[TFM] Max number of errors exceeded");
                }

            } catch (Exception e) {

                errorManager.setMustStop();
                errorManager.writeError(e.getMessage());

            }
            synchronized (metadataList) {
                int[] metadata = metadataList.get(0);
                metadata[0] = metadata[0] + result.getNSent();
                metadata[1] = metadata[1] + result.getPartialFailure();
                metadata[2] = metadata[2] + result.getTotalFailure();
                metadataList.add(0, metadata);
            }
        }
    }

    // oggetti risultato dei sendToIndex
    private static class sendToIndexRes {
        private boolean status; // indica se è stato superato il massimo numero di errori
        private int partialFailure;
        private int totalFailure;
        private int nSent;

        public sendToIndexRes(boolean status, int partialFailure, int totalFailure, int nSent) {
            this.status = status;
            this.partialFailure = partialFailure;
            this.totalFailure = totalFailure;
            this.nSent = nSent;
        }

        public boolean getStatus() {
            return status;
        }

        public int getPartialFailure() {
            return partialFailure;
        }

        public int getTotalFailure() {
            return totalFailure;
        }

        public int getNSent() {
            return nSent;
        }
    }

    private static sendToIndexRes sendDoc(String indexName, JSONArray jsonArray, String url, int maxErrors,
            ErrorManager errorManager, RestClientBuilder builder, String failDir)
            throws Exception {

        RestHighLevelClient client = new RestHighLevelClient(builder);
        int nSent = 0;
        int partialFailure = 0;
        int totalFailure = 0;

        Path failureFolderPath = Paths.get(failDir);

        for (int i = 0; i < jsonArray.length(); i++) {
            String jsonDocument = jsonArray.getJSONObject(i).toString();
            boolean documentIndexed = false;

            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    if (errorManager.getErrorCount() < maxErrors) {
                        IndexRequest request = new IndexRequest(indexName).source(jsonDocument, XContentType.JSON);
                        client.index(request, RequestOptions.DEFAULT);
                        nSent++;
                        documentIndexed = true;
                        break;
                    }
                } catch (Exception e) {
                    partialFailure++;
                    Logger.log(
                            "[TFM] Error indexing document, " + Thread.currentThread().getName() + " in hostname " + url
                                    + " error: " + e);
                    Thread.sleep(500);
                    errorManager.incrementErrorCount();

                    Logger.log("[TFM] Retrying the current document (Attempt " + (attempt + 1) + ")");
                }
            }

            if (!documentIndexed) {
                try {
                    totalFailure++;
                    //Logger.log("[TFM] Error multiple times indexing the document, the document has not been indexed. "
                    //        + Thread.currentThread().getName() + " in hostname " + url);
                    JSONObject failDocument = new JSONObject(jsonDocument);
                    // Salva il documento non indicizzato nel file di fallimenti
                    Path failureFilePath = failureFolderPath.resolve(failDocument.getString("scenario"));

                    // Aggiungi il carattere di nuova linea alla fine di ogni documento
                    String documentWithNewLine = jsonDocument + "\n";

                    Files.write(failureFilePath, documentWithNewLine.getBytes(), StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND);
                } catch (Exception e) {
                    Logger.log("[TFM] Error creating the log file for failed document: " + e);
                }
            }
        }

        client.close();
        Logger.log("[TFM] " + Thread.currentThread().getName() + " sent:" + nSent + " hostname:" + url);

        if (errorManager.getErrorCount() < maxErrors) {
            return new sendToIndexRes(true, partialFailure, totalFailure, nSent);
        } else {
            return new sendToIndexRes(false, partialFailure, totalFailure, nSent);
        }
    }

    // #################################### INDEX CREATION

    // FUNZIONI UTILI PER LA CREAZIONE DI INDICE ELASTICSEARCH

    static private boolean indexChecked = false;
    
    private static void createIndex(String indexName, String url, int port, String admin, String password)
            throws Exception {
        try {
            if(indexChecked)
                return;
            // Specifica le credenziali di accesso
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(admin, password));

            RestClientBuilder builder = RestClient.builder(
                    new HttpHost(url, port, "https"))
                    .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                        @Override
                        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                            // Configura l'autenticazione HTTP
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                            return httpClientBuilder;
                        }
                    });
            Logger.log("[TFM][createIndex] builder is ready");

            RestHighLevelClient client = new RestHighLevelClient(builder);

            Logger.log("[TFM][createIndex] client is ready");
            
            boolean exists = client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
            if(exists) {
                Logger.log("[TFM][createIndex] Index "+indexName+" already exists");
                indexChecked = true;
                client.close();
                return;
            }

            Logger.log("[TFM][createIndex] Index "+indexName+" not found, creating");
            
            Settings settings = Settings.builder()
                    .put("index.number_of_shards", 1)
                    .put("index.number_of_replicas", 1)
                    .build();

            CreateIndexRequest request = new CreateIndexRequest(indexName)
                    .settings(settings)
                    .mapping(indexMapping());

            CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
            Logger.log("[TFM][createIndex] create index response id: " + response.index());
            client.close();

            // System.out.println("creato il response");
            indexChecked = true;

        } catch (Exception e) {
            Logger.log("[TFM][createIndex] Error in index creation: " + e);
            throw new Exception("[TFM][createIndex] Error in index creation: " + e);

        }
    }

    private static XContentBuilder indexMapping() throws IOException {
        XContentBuilder mapping = XContentFactory.jsonBuilder();
        mapping.startObject();
        {
            mapping.startObject("properties");
            {
                mapping.startObject("dateObserved");
                {
                    mapping.field("type", "date");
                    mapping.field("format", "strict_date_optional_time||yyyy-MM-dd'T'HH:mm:ssZ" );
                }
                mapping.endObject();

                mapping.startObject("density");
                {
                    mapping.field("type", "float");
                }
                mapping.endObject();

                mapping.startObject("vmax");
                {
                    mapping.field("type", "float");
                }
                mapping.endObject();

                mapping.startObject("lane_numbers");
                {
                    mapping.field("type", "integer");
                }
                mapping.endObject();

                mapping.startObject("scenario");
                {
                    mapping.field("type", "keyword");
                }
                mapping.endObject();

                mapping.startObject("kind");
                {
                    mapping.field("type", "keyword");
                }
                mapping.endObject();

                mapping.startObject("numVehicle");
                {
                    mapping.field("type", "text");
                }
                mapping.endObject();

                mapping.startObject("roadElements");
                {
                    mapping.field("type", "keyword");
                }
                mapping.endObject();

                mapping.startObject("segments");
                {
                    mapping.field("type", "nested");
                    mapping.startObject("properties");
                    {
                        mapping.startObject("densityCar20m");
                        {
                            mapping.field("type", "text");
                        }
                        mapping.endObject();

                        mapping.startObject("density");
                        {
                            mapping.field("type", "float");
                        }
                        mapping.endObject();

                        mapping.startObject("segment");
                        {
                            mapping.field("type", "text");
                        }
                        mapping.endObject();

                        mapping.startObject("linestring");
                        {
                            mapping.field("type", "geo_shape");
                        }
                        mapping.endObject();
                    }
                    mapping.endObject();
                }
                mapping.endObject();

                // mapping.startObject("segments");
                // {
                //     mapping.startObject("properties");
                //     {
                //         mapping.startObject("segment");
                //         {
                //             mapping.field("type", "keyword");
                //         }
                //         mapping.endObject();
                //     }
                //     mapping.endObject();
                // }
                // mapping.endObject();

                mapping.startObject("start");
                {
                    mapping.startObject("properties");
                    {
                        mapping.startObject("location");
                        {
                            mapping.field("type", "geo_point");
                        }
                        mapping.endObject();
                    }
                    mapping.endObject();
                }
                mapping.endObject();

                mapping.startObject("end");
                {
                    mapping.startObject("properties");
                    {
                        mapping.startObject("location");
                        {
                            mapping.field("type", "geo_point");
                        }
                        mapping.endObject();
                    }
                    mapping.endObject();
                }
                mapping.endObject();

                mapping.startObject("dir");
                {
                    mapping.field("type", "integer");
                }
                mapping.endObject();

                mapping.startObject("flow");
                {
                    mapping.field("type", "text");
                }
                mapping.endObject();

                mapping.startObject("line");
                {
                    mapping.field("type", "geo_shape");
                }
                mapping.endObject();
            }
            mapping.endObject();
        }
        mapping.endObject();

        return mapping;
    }

}