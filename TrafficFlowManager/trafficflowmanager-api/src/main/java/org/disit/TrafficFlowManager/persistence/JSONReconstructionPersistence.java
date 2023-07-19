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

package org.disit.TrafficFlowManager.persistence;

import org.disit.TrafficFlowManager.utils.ConfigProperties;
import org.disit.TrafficFlowManager.utils.FileZipper;
import org.disit.TrafficFlowManager.utils.Logger;
import org.apache.commons.io.FileUtils;

import javax.json.*;
import javax.json.stream.JsonCollectors;
import java.io.*;
import java.util.*;

public class JSONReconstructionPersistence implements ReconstructionPersistenceInterface {

    private final String jsonDatabasePath;
    private final String reconstructionsFolder;

    public JSONReconstructionPersistence() throws IOException {
        jsonDatabasePath = ConfigProperties.getProperties().getProperty("db");
        reconstructionsFolder = ConfigProperties.getProperties().getProperty("reconstructionsFolder");

        if (!new File(jsonDatabasePath).exists()) {
            Logger.log("[DB] First write, creating json db...");
            PrintWriter writer = new PrintWriter(jsonDatabasePath, "UTF-8");
            writer.println("[]");
            writer.close();
        }

        new File(reconstructionsFolder).mkdir();
    }

    @Override
    public void addEntry(JsonObject metadata, String layerName) throws IOException {
      Logger.log("[DB] PRE Adding entry for layer " + layerName + "...");
      synchronized(JSONReconstructionPersistence.class) {
        Logger.log("[DB] Adding entry for layer " + layerName + "...");
        try (InputStream inputStream = new FileInputStream(jsonDatabasePath)) {

            // Append layer name to metadata object
            JsonObject newMetadata = appendKeyValueToObject(metadata, "layerName", layerName);

            // Append object and build response array
            JsonReader reader = Json.createReader(inputStream);
            JsonArray mainArray = reader.readArray();
            reader.close();
            JsonArrayBuilder builder = Json.createArrayBuilder();
            JsonArray newArray;
            if (mainArray.contains(newMetadata)) {
                newArray = mainArray;
            } else {
                for (JsonValue existingValue: mainArray)
                    builder.add(existingValue);
                builder.add(newMetadata);
                newArray = builder.build();
            }

            // Write response
            try (FileWriter fileWriter = new FileWriter(jsonDatabasePath)) {
                fileWriter.write(newArray.toString());
                fileWriter.flush();
            }

            Logger.log("[DB] Done!");
        }
      }
    }

    @Override
    public void saveReconstructionAsZippedJson(JsonValue json, String layerName) throws IOException {
        Logger.log("[DB] Saving reconstruction zipped JSON to " + reconstructionsFolder);

        // Write .json
        String filenameJson = reconstructionsFolder + "/" + layerName + ".json";
        try (FileWriter file = new FileWriter(filenameJson)) {
            file.write(json.toString());
            file.flush();
        }

        // Zip .json
        String filenameZip = reconstructionsFolder + "/" + layerName + ".zip";
        FileZipper.zipFiles(Collections.singletonList(filenameJson), filenameZip);

        // Delete .json
        FileUtils.deleteQuietly(new File(filenameJson));

        Logger.log("[DB] Done! Saved to " + filenameZip);
    }

    @Override
    public JsonArray allLayersClustered() throws IOException {
        Logger.log("[DB] PRE Retrieving all layers clustered...");
        synchronized(JSONReconstructionPersistence.class) {
          Logger.log("[DB] Retrieving all layers clustered...");
          try (InputStream inputStream = new FileInputStream(jsonDatabasePath)) {

              JsonArray array = Json.createReader(inputStream).readArray();
              JsonBuilderFactory factory = Json.createBuilderFactory(null);
              JsonArrayBuilder builder = Json.createArrayBuilder();

              // Group by flux name
              Set<String> set = new HashSet<>();
              array.forEach(item -> {
                  String fluxName = item.asJsonObject().getString("fluxName");
                  if (set.add(fluxName)) {
                      long instances = array
                              .stream()
                              .filter(jsonValue -> jsonValue.asJsonObject().getString("fluxName").equals(fluxName))
                              .count();
                      builder.add(factory.createObjectBuilder()
                              .add("fluxName", fluxName)
                              .add("locality", item.asJsonObject().getString("locality"))
                              .add("organization", item.asJsonObject().getString("organization"))
                              .add("scenarioID", item.asJsonObject().getString("scenarioID"))
                              .add("colorMap", item.asJsonObject().getString("colorMap"))
                              .add("instances", instances)
                              .add("locality", item.asJsonObject().getString("locality"))
                              .add("metricName", item.asJsonObject().getString("metricName"))
                              .add("unitOfMeasure", item.asJsonObject().getString("unitOfMeasure"))
                              .add("staticGraphName", item.asJsonObject().getString("staticGraphName"))
                      );
                  }

              });

              return builder.build();
          }
        }
    }

    @Override
    public JsonArray layersForFluxName(String fluxName, int offset, int limit) throws IOException {
        Logger.log("[DB] PRE Retrieving all layers for fluxName " + fluxName + "...");
        synchronized(JSONReconstructionPersistence.class) {        
          Logger.log("[DB] Retrieving all layers for fluxName " + fluxName + "...");
          try (InputStream inputStream = new FileInputStream(jsonDatabasePath)) {
              JsonArray array = Json.createReader(inputStream).readArray();
              return array
                      .stream()
                      .filter(jsonValue -> jsonValue.asJsonObject().getString("fluxName").equals(fluxName))
                      .sorted((l1, l2)->l2.asJsonObject().getString("dateTime").
                              compareTo(l1.asJsonObject().getString("dateTime")))
                      .skip(offset)
                      .limit(limit)
                      .collect(JsonCollectors.toJsonArray());
          }
        }
    }

    @Override
    public void changeColorMapForFluxName(String fluxName, String newColorMap) throws IOException {
        Logger.log("[DB] PRE Changing color map to " + newColorMap + " for fluxName " + fluxName + "...");
        synchronized(JSONReconstructionPersistence.class) {
          Logger.log("[DB] Changing color map to " + newColorMap + " for fluxName " + fluxName + "...");

          try (InputStream inputStream = new FileInputStream(jsonDatabasePath)) {
              JsonArray array = Json.createReader(inputStream).readArray();

              // Substitute value
              JsonArrayBuilder builder = Json.createArrayBuilder();
              for (JsonValue value: array) {
                  if (value.asJsonObject().getString("fluxName").equals(fluxName)) {
                      builder.add(substituteValueToObject(value.asJsonObject(), "colorMap", newColorMap));
                  } else {
                      builder.add(value);
                  }
              }

              // Write changes to file
              try (FileWriter fileWriter = new FileWriter(jsonDatabasePath)) {
                  fileWriter.write(builder.build().toString());
                  fileWriter.flush();
              }
          }
        }
    }

    @Override
    public void deleteFlux(String fluxName) throws IOException {
        Logger.log("[DB] PRE Deleting flux " + fluxName + "...");
        synchronized(JSONReconstructionPersistence.class) {
          Logger.log("[DB] Deleting flux " + fluxName + "...");

          try (InputStream inputStream = new FileInputStream(jsonDatabasePath)) {
              JsonArray array = Json.createReader(inputStream).readArray();

              // Delete value
              JsonArrayBuilder builder = Json.createArrayBuilder();
              for (JsonValue value: array) {
                  if (!value.asJsonObject().getString("fluxName").equals(fluxName)) {
                      builder.add(value);
                  }
              }

              // Write changes to file
              try (FileWriter fileWriter = new FileWriter(jsonDatabasePath)) {
                  fileWriter.write(builder.build().toString());
                  fileWriter.flush();
              }
          }
        }
    }

    @Override
    public void deleteLayer(String layerName) throws IOException {
        Logger.log("[DB] PRE Deleting layer " + layerName + "...");
        synchronized(JSONReconstructionPersistence.class) {
          Logger.log("[DB] Deleting layer " + layerName + "...");

          try (InputStream inputStream = new FileInputStream(jsonDatabasePath)) {
              JsonArray array = Json.createReader(inputStream).readArray();

              // Delete layer
              JsonArrayBuilder builder = Json.createArrayBuilder();
              for (JsonValue value: array) {
                  if (!value.asJsonObject().getString("layerName").equals(layerName)) {
                      builder.add(value);
                  }
              }

              // Write changes to file
              try (FileWriter fileWriter = new FileWriter(jsonDatabasePath)) {
                  fileWriter.write(builder.build().toString());
                  fileWriter.flush();
              }
          }
        }
    }

    private JsonObject appendKeyValueToObject(JsonObject obj, String key, String value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : obj.entrySet())
            builder.add(entry.getKey(), entry.getValue());
        builder.add(key,value);
        return builder.build();
    }

    private JsonObject substituteValueToObject(JsonObject obj, String key, String newValue) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
            if (entry.getKey().equals(key))
                builder.add(key, newValue);
            else
                builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
