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
import org.disit.TrafficFlowManager.persistence.JSONReconstructionPersistence;
import org.disit.TrafficFlowManager.persistence.JSONStaticGraphPersistence;
import org.disit.TrafficFlowManager.persistence.ReconstructionPersistenceInterface;
import org.apache.commons.io.FileUtils;

import org.disit.TrafficFlowManager.persistence.OpenSearchReconstructionPersistence;
import org.json.JSONObject;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.json.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

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

            // Handle payload based on type
            JsonValue body = getJSONBody(req);
            switch (PayloadType.valueOf(type)) {
                case staticGraph:
                    handleStaticGraph(body);
                    resp.getWriter().print(buildResponse(true, "", ""));
                    break;
                case reconstruction:
                    String uploadedLayerName = handleReconstruction(body);
                    JSONObject jbody = new JSONObject(body.toString());
                    if (conf.getProperty("saveToES", "onrequest").equals("always") || jbody.has("saveToES")) {
                        int[] metadata = OpenSearchReconstructionPersistence.sendToEs(jbody, "reconstructed");
                        resp.getWriter().print(buildResponse(true, "layerName", uploadedLayerName, metadata));
                    } else {
                        System.out.println("SKIP save on elastic search");
                        resp.getWriter().print(buildResponse(true, "layerName", uploadedLayerName));
                    }
                    break;
                case TTT:
                    // do somethig

                    System.out.println("IN TTT CASE!!!");

                    JSONObject TTT = new JSONObject(body.toString());
                    System.out.println(TTT.toString(2));
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
        builder.add("Details",
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