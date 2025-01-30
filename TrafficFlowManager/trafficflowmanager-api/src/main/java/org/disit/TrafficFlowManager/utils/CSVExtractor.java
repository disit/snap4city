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

import javax.json.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class CSVExtractor {

    public static void extract(JsonArray staticGraph, JsonObject reconstructionData, String outputFile) throws FileNotFoundException {

        Logger.log("[CSVExtractor] Extracting CSV...");

        try (PrintWriter writer = new PrintWriter(outputFile)) {

            Logger.log("[CSVExtractor] Writing output to " + outputFile);

            StringBuilder sb = new StringBuilder();
            sb.append("segment_id,road_id,start_lat,start_long,end_lat,end_long,lanes,fipili,traffic_value,traffic_label");
            sb.append('\n');

            for (JsonValue road: staticGraph) {
                JsonObject roadObject = road.asJsonObject();

                String roadId = roadObject.getString("road");
                JsonObject roadDensity = reconstructionData.getJsonObject(roadId).getJsonArray("data").getJsonObject(0);

                for (JsonValue segment: roadObject.getJsonArray("segments")) {
                    JsonObject segmentObj = segment.asJsonObject();

                    String segmentId = segmentObj.getString("id");
                    String startLat = segmentObj.getJsonObject("start").getString("lat");
                    String startLon = segmentObj.getJsonObject("start").getString("long");
                    String endLat = segmentObj.getJsonObject("end").getString("lat");
                    String endLon = segmentObj.getJsonObject("end").getString("long");

                    // Field "lanes" could be either a String or an Integer
                    Integer lanes;
                    if (segmentObj.get("lanes").getValueType() == JsonValue.ValueType.NUMBER)
                        lanes = segmentObj.getInt("lanes");
                    else
                        lanes = Integer.parseInt(segmentObj.getString("lanes"));

                    Integer fipili = segmentObj.getInt("FIPILI", 0);
                    String trafficValue = roadDensity.getString(segmentId);
                    String trafficLabel = extractTrafficLabel(trafficValue, lanes, fipili);

                    sb.append(segmentId).append(",");
                    sb.append(roadId).append(",");
                    sb.append(startLat).append(",");
                    sb.append(startLon).append(",");
                    sb.append(endLat).append(",");
                    sb.append(endLon).append(",");
                    sb.append(lanes).append(",");
                    sb.append(fipili).append(",");
                    sb.append(trafficValue).append(",");
                    sb.append(trafficLabel);
                    sb.append("\n");
                }
            }
            writer.write(sb.toString());
        }
    }

    public static JSONObject extractToJSON(JsonArray staticGraph, JsonObject reconstructionData) throws Exception {

        Logger.log("[CSVExtractor] Extracting merged JSON (static and dinamic)...");

        JSONArray merged = new JSONArray();
        List<String> roadElements = new ArrayList<>();
        List<String> roadElementsINV = new ArrayList<>();

        for (JsonValue road: staticGraph) {
            JsonObject roadObject = road.asJsonObject();

            String roadId = roadObject.getString("road");
            JsonObject roadDensity = reconstructionData.getJsonObject(roadId).getJsonArray("data").getJsonObject(0);

            for (JsonValue segment: roadObject.getJsonArray("segments")) {
                JsonObject segmentObj = segment.asJsonObject();

                String segmentId = segmentObj.getString("id");
                String startLat = segmentObj.getJsonObject("start").getString("lat");
                String startLon = segmentObj.getJsonObject("start").getString("long");
                String endLat = segmentObj.getJsonObject("end").getString("lat");
                String endLon = segmentObj.getJsonObject("end").getString("long");

                boolean isINV = false;
                // e.g. INV: OS00022926823RE/0--OS00022926823RE/1--OS00022926823RE/2--OS00022926823RE/3--OS00022926823RE/4--OS00022926823RE/5INV.10
                // e.g. nonINV: OS00022926823RE/0--OS00022926823RE/1--OS00022926823RE/2--OS00022926823RE/3--OS00022926823RE/4--OS00022926823RE/5.10
                if(segmentId.contains("INV")){
                    isINV = true;
                }

                String[] REs = segmentId.split("--");
                for (int i = 0; i < REs.length; i++) {
                    if (i == REs.length - 1) {
                        if(isINV){
                            // Dividi l'ultimo road element quando trovi "INV"
                            String[] tmp = REs[i].split("INV");
                            REs[i] = tmp[0];
                        } else {
                            // Dividi l'ultimo road element quando trovi "."
                            String[] tmp = REs[i].split("\\.");
                            REs[i] = tmp[0];
                        }                        
                    }
                    if(isINV){
                        roadElementsINV.add(REs[i]);
                    } else {
                        roadElements.add(REs[i]);
                    }
                }

                // Field "lanes" could be either a String or an Integer
                Integer lanes;
                if (segmentObj.get("lanes").getValueType() == JsonValue.ValueType.NUMBER)
                    lanes = segmentObj.getInt("lanes");
                else
                    lanes = Integer.parseInt(segmentObj.getString("lanes"));

                Integer fipili = segmentObj.getInt("FIPILI", 0);
                String trafficValue = roadDensity.getString(segmentId);
                String trafficLabel = extractTrafficLabel(trafficValue, lanes, fipili);

                JSONObject segJSON = new JSONObject();
                segJSON.put("segmentId", segmentId);
                segJSON.put("roadId", roadId);
                segJSON.put("startLat", startLat);
                segJSON.put("startLon", startLon);
                segJSON.put("endLat", endLat);
                segJSON.put("endLon", endLon);
                segJSON.put("lanes", lanes);
                segJSON.put("fipili", fipili);
                segJSON.put("trafficValue", trafficValue);
                segJSON.put("trafficLabel", trafficLabel);
                merged.put(segJSON);
            }
        }

        // Create a HashSet to filter duplicates
        HashSet<String> uniqueRoadElementsINV = new HashSet<>(roadElementsINV);
        HashSet<String> uniqueRoadElements = new HashSet<>(roadElements);

        // Convert the HashSet back to a List if needed
        roadElementsINV = new ArrayList<>(uniqueRoadElementsINV);
        roadElements = new ArrayList<>(uniqueRoadElements);

        JSONObject output = new JSONObject();
        output.put("data", merged);
        output.put("roadElements", new JSONArray(roadElements));
        output.put("roadElementsINV", new JSONArray(roadElementsINV));

        return output;
    }

    // Extract traffic label from traffic value
    private static String extractTrafficLabel(String trafficValue, Integer lanes, Integer fipili) {
        double doubleValue = Double.parseDouble(trafficValue.replace(',', '.'));

        double green = 0.3;
        double yellow = 0.6;
        double orange = 0.9;

        if (lanes == 2) {
            green = 0.6;
            yellow = 1.2;
            orange = 1.8;
        }
        if (fipili == 1) {
            green=0.25;
            yellow=0.5;
            orange=0.75;
        }
        if (lanes == 3) {
            green = 0.9;
            yellow = 1.5;
            orange = 2.0;
        }
        if (lanes == 4) {
            green = 1.2;
            yellow = 1.6;
            orange = 2.0;
        }
        if (lanes == 5) {
            green = 1.6;
            yellow = 2.0;
            orange = 2.4;
        }
        if (lanes == 6) {
            green = 2.0;
            yellow = 2.4;
            orange = 2.8;
        }

        if (doubleValue <= green) {
            return "green";
        } else if (doubleValue <= yellow) {
            return "yellow";
        } else if (doubleValue <= orange) {
            return "orange";
        } else {
            return "red";
        }
    }
}
