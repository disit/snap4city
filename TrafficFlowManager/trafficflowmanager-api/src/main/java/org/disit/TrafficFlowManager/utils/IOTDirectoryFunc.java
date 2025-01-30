package org.disit.TrafficFlowManager.utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

//import org.disit.TrafficFlowManager.utils.ConfigProperties;
import org.json.JSONObject;

public class IOTDirectoryFunc {

    /* DEPRECATED ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // function to get attributes of a device. If device exist it return a JSON listing the device attribute with status=ok, 
    // otherwise a JSON with status=ko and a msg=Unrecognized device is returned 
    public static JSONObject getDeviceAttribute(String base_url, String device_name, String broker, String access_token) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String responseBody = "";
        int statusCode = -1;
        try {
            // The URL of the server endpoint
            String url = base_url + "iot-directory/api/device.php";
            HttpPost httpPost = new HttpPost(url);

            // Create form parameters
            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("action", "get_device_attributes"));
            formParams.add(new BasicNameValuePair("nodered", "accesstoken"));
            formParams.add(new BasicNameValuePair("nodered", "accesstoken"));
            formParams.add(new BasicNameValuePair("id", device_name));
            formParams.add(new BasicNameValuePair("contextbroker", broker));

            // Set the entity with form data
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded"); // "application/json" or "multipart/form-data"
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Authorization", "Bearer "+ access_token);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            
            try {
                statusCode = response.getStatusLine().getStatusCode();
                //System.out.println("Response Code: " + statusCode);
                responseBody = EntityUtils.toString(response.getEntity());
                //System.out.println("Response Body: " + responseBody);                
            } finally {
                response.close();
            }           
        } catch (Exception e) {
            //e.printStackTrace();
            Logger.log("[TFM] Error in IOTDirectoryFunc.getDeviceAttribute: " + e);
            throw new Exception("[TFM] Error in IOTDirectoryFunc.getDeviceAttribute: " + e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                Logger.log("[TFM] Error in IOTDirectoryFunc.getDeviceAttribute: " + e);
                throw new Exception("[TFM] Error in IOTDirectoryFunc.getDeviceAttribute: " + e);
            }
        }
        JSONObject response = new JSONObject();
        response.put("statusCode", statusCode);
        response.put("responseBody", responseBody);
        return response;
    }*/

    public static JSONObject getBigDataFromDB(String SURI, String access_token, String dateObserved, String dashURL) throws Exception {        
        String responseBody = "";
        int statusCode = -1;
        CloseableHttpClient httpClient = HttpClients.createDefault();  

        // reformat dateObserved to be compatible with DB
        // System.out.println("Before: " + dateObserved);
        // String [] doParts = dateObserved.split("T");
        // String date = doParts[0];
        // doParts =  doParts[1].split("\\.");
        // String hour = doParts[0];
        // dateObserved = date + "%20" + hour;
        String[] dtParts = dateObserved.split(" ");
        dateObserved = dtParts[0] + "%20" + dtParts[1];

        // System.out.println("After: " + dateObserved);

        try {
            String url = dashURL + "/processloader/api/bigdatafordevice/getOneSpecific.php?suri=" + SURI + "&accessToken=" + access_token + "&dateObserved=" + dateObserved;
            Logger.log("[TFM][getBigDataFromDB] URL: "+ url);

            HttpGet httpget = new HttpGet(url);
            //httpget.setHeader("Accept", "application/json");
            httpget.setHeader("Authorization", "Bearer "+ access_token);
            CloseableHttpResponse response = httpClient.execute(httpget);            
            try {
                statusCode = response.getStatusLine().getStatusCode();
                responseBody = EntityUtils.toString(response.getEntity());              
            } catch (Exception e) {
                Logger.log("[TFM][getBigDataFromDB] Response error");
            }finally {
                response.close();
            }           
        } catch (Exception e) {
            //e.printStackTrace();
            Logger.log("[TFM][getBigDataFromDB] Error in IOTDirectoryFunc.getBigDataFromDB: " + e);
            throw new Exception("[TFM][getBigDataFromDB] Error in IOTDirectoryFunc.getBigDataFromDB: " + e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                //e.printStackTrace();
                Logger.log("[TFM][getBigDataFromDB] Error in IOTDirectoryFunc.getBigDataFromDB: " + e);
                throw new Exception("[TFM][getBigDataFromDB] Error in IOTDirectoryFunc.getBigDataFromDB: " + e);
            }
        }
        JSONObject response = new JSONObject();
        response.put("statusCode", statusCode);
        response.put("responseBody", responseBody);
        return response;
    }

    public static String buildSURI(String device_name, String broker, String organization, String baseSURI) throws Exception{
        String SURI = "";
        try{
            SURI = baseSURI + broker + "/" + organization + "/" + device_name;
        } catch (Exception e) {
            Logger.log("[TFM] Error in IOTDirectoryFunc.buildSURI: " + e);
            throw new Exception("[TFM] Error in IOTDirectoryFunc.buildSURI: " + e);
        }
        return SURI;
    }

    public static JSONObject getDeviceFromSURI(String SURI, String access_token, String dashURL) throws Exception {        
        String responseBody = "";
        int statusCode = -1;
        CloseableHttpClient httpClient = HttpClients.createDefault();  
        try {
            String url = dashURL + "/superservicemap/api/v1/?serviceUri=" + SURI;
            HttpGet httpget = new HttpGet(url);
            httpget.setHeader("Accept", "application/json");
            httpget.setHeader("Authorization", "Bearer "+ access_token);
            CloseableHttpResponse response = httpClient.execute(httpget);            
            try {
                statusCode = response.getStatusLine().getStatusCode();
                if(statusCode != 200){
                    responseBody = "KO";
                }else{
                    responseBody = EntityUtils.toString(response.getEntity());   
                }             
            } finally {
                response.close();
            }           
        } catch (Exception e) {
            //e.printStackTrace();
            Logger.log("[TFM] Error in IOTDirectoryFunc.getDeviceFromSURI: " + e);
            throw new Exception("[TFM] Error in IOTDirectoryFunc.getDeviceFromSURI: " + e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                //e.printStackTrace();
                Logger.log("[TFM] Error in IOTDirectoryFunc.getDeviceFromSURI: " + e);
                throw new Exception("[TFM] Error in IOTDirectoryFunc.getDeviceFromSURI: " + e);
            }
        }
        JSONObject response = new JSONObject();
        response.put("statusCode", statusCode);
        response.put("responseBody", responseBody);
        return response;
    }

    public static JSONObject createTrafficFlowDevice(String device_name, String broker, String access_token, String device_lat, String device_lon, String device_wkt, String dashURL)throws Exception {
        String device_model = "trafficFlowModel";
        JSONObject response = null;
        try{
            JSONObject res = getDeviceModel(access_token, device_model, dashURL);
            JSONObject jbody = new JSONObject(res.getString("responseBody"));
            // System.out.println(jbody.toString(2));

            String device_type = jbody.getJSONObject("content").getString("devicetype");
            String device_kind = jbody.getJSONObject("content").getString("kind");;
            String device_producer = jbody.getJSONObject("content").getString("producer");;
            String device_subnature = jbody.getJSONObject("content").getString("subnature");;
            String device_attr_json = jbody.getJSONObject("content").getString("attributes");;
            String device_hlt = jbody.getJSONObject("content").getString("hlt");;
            response = createNewDevice(device_name, broker, access_token, device_type, device_kind, device_model, device_producer, device_lat, 
                                       device_lon, device_subnature, device_hlt, device_wkt, device_attr_json, dashURL);
    
        } catch (Exception e) {
            //e.printStackTrace();
            Logger.log("[TFM] Error in IOTDirectoryFunc.createTrafficFlowDevice: " + e);
            throw new Exception("[TFM] Error in IOTDirectoryFunc.createTrafficFlowDevice: " + e);
        }
        return response;
    }

    public static JSONObject getDeviceModel(String access_token,String device_model, String dashURL) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String responseBody = "";
        int statusCode = -1;
        try {
            String url = dashURL + "/iot-directory/api/model.php";
            HttpPost httpPost = new HttpPost(url);

            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("action", "get_model"));
            formParams.add(new BasicNameValuePair("nodered", "accesstoken"));            
            formParams.add(new BasicNameValuePair("token", access_token));
            formParams.add(new BasicNameValuePair("name", device_model));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams);
            httpPost.setEntity(entity);

            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded"); 
            httpPost.setHeader("Accept", "*/*");
            httpPost.setHeader("Authorization", "Bearer "+ access_token);

            // Execute the request
            CloseableHttpResponse response = httpClient.execute(httpPost);            
            try {
                statusCode = response.getStatusLine().getStatusCode();
                //System.out.println("Response Code: " + statusCode);
                responseBody = EntityUtils.toString(response.getEntity());
                //System.out.println("Response Body: " + responseBody);                
            } finally {
                response.close();
            }           
        } catch (Exception e) {
            //e.printStackTrace();
            Logger.log("[TFM] Error in IOTDirectoryFunc.getDeviceModel: " + e);
            throw new Exception("[TFM] Error in IOTDirectoryFunc.getDeviceModel: " + e);            
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                //e.printStackTrace();
                Logger.log("[TFM] Error in IOTDirectoryFunc.getDeviceModel: " + e);
                throw new Exception("[TFM] Error in IOTDirectoryFunc.getDeviceModel: " + e);  
            }
        }
        JSONObject response = new JSONObject();
        response.put("statusCode", statusCode);
        response.put("responseBody", responseBody);
        return response;    
    }

    public static JSONObject createNewDevice(
            String device_name, 
            String broker, 
            String access_token,
            String device_type,
            String device_kind,
            String device_model,
            String device_producer,
            String device_lat,
            String device_lon,
            String device_subnature,
            String device_hlt,
            String device_wkt,
            String device_attr_json,
            String dashURL
    ) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String responseBody = "";
        int statusCode = -1;
        try {
            String url = dashURL + "/iot-directory/api/device.php";
            HttpPost httpPost = new HttpPost(url);

            // Create form parameters
            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("action", "insert"));
            formParams.add(new BasicNameValuePair("nodered", "accesstoken"));            
            formParams.add(new BasicNameValuePair("contextbroker", broker));
            formParams.add(new BasicNameValuePair("id", device_name));
            formParams.add(new BasicNameValuePair("type", device_type));
            formParams.add(new BasicNameValuePair("kind", device_kind));
            formParams.add(new BasicNameValuePair("format", "json"));
            formParams.add(new BasicNameValuePair("model", device_model));
            formParams.add(new BasicNameValuePair("producer", device_producer));
            formParams.add(new BasicNameValuePair("latitude", device_lat));
            formParams.add(new BasicNameValuePair("longitude", device_lon));
            formParams.add(new BasicNameValuePair("visibility", "private"));
            formParams.add(new BasicNameValuePair("frequency", "600"));
            formParams.add(new BasicNameValuePair("token", access_token));
            formParams.add(new BasicNameValuePair("k1", "4f7ccbf8-1c3f-4580-a9c1-a7f7969f890f"));
            formParams.add(new BasicNameValuePair("k2", "44dad5c4-407f-4c9d-b852-bee2d4fe8a13"));
            formParams.add(new BasicNameValuePair("subnature", device_subnature));
            formParams.add(new BasicNameValuePair("hlt", device_hlt));
            formParams.add(new BasicNameValuePair("wktGeometry", device_wkt));
            formParams.add(new BasicNameValuePair("attributes", device_attr_json));
            formParams.add(new BasicNameValuePair("static_attributes", "[]"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded"); 
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Authorization", "Bearer "+ access_token);
            CloseableHttpResponse response = httpClient.execute(httpPost);            
            try {
                statusCode = response.getStatusLine().getStatusCode();
                //System.out.println("Response Code: " + statusCode);
                responseBody = EntityUtils.toString(response.getEntity());
                //System.out.println("Response Body: " + responseBody);

                JSONObject rb = new JSONObject(responseBody);
                String operationStatus = rb.getString("status");
                if(!operationStatus.equals("ok")){
                    Logger.log("[TFM][createNewDevice] Device not created correctly");
                    throw new Exception("[TFM][createNewDevice] Device not created correctly");
                }
            } finally {
                response.close();
            }        
        } catch (Exception e) {
            //e.printStackTrace();
            Logger.log("[TFM][createNewDevice] Error in IOTDirectoryFunc.createNewDevice: " + e);
            throw new Exception("[TFM][createNewDevice] Error in IOTDirectoryFunc.createNewDevice: " + e);  
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                //e.printStackTrace();
                Logger.log("[TFM][createNewDevice] Error in IOTDirectoryFunc.createNewDevice: " + e);
                throw new Exception("[TFM][createNewDevice] Error in IOTDirectoryFunc.createNewDevice: " + e); 
            }
        }
        JSONObject response = new JSONObject();
        response.put("statusCode", statusCode);
        response.put("responseBody", responseBody);
        return response;    
    }

    public static JSONObject setDeviceValues(String device_name, String device_type, String data, String broker, String access_token, String dashURL) throws Exception {        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String responseBody = "";
        int statusCode = -1;
        try {
            //String url = dashURL + "orion-filter/iotobsf/v2/entities/" + device_name + "/attrs?elementid=" + device_name + "&type=userdata";
            String orionfilterstring = "";
            if(dashURL.contains("www.snap4city.org")){
                orionfilterstring = "/orionfilter/";
            } else { // on microX
                orionfilterstring = "/orion-filter/";
            }
            String url = dashURL + orionfilterstring + broker + "/v2/entities/" + device_name + "/attrs?elementid=" + device_name + "&type=" + device_type;
            Logger.log("[TFM][setDeviceValues] URL: " +url);
            HttpPatch httpPatch = new HttpPatch(url);
            StringEntity entity = new StringEntity(data);
            httpPatch.setEntity(entity);            
            httpPatch.setHeader("Content-Type", "application/json"); 
            httpPatch.setHeader("Accept", "application/json");
            httpPatch.setHeader("Authorization", "Bearer "+ access_token);
            CloseableHttpResponse response = httpClient.execute(httpPatch);
            try {
                statusCode = response.getStatusLine().getStatusCode();
                //System.out.println("Response Code: " + statusCode);
                responseBody = EntityUtils.toString(response.getEntity());
                //System.out.println("Response Body: " + responseBody);
            } catch (IllegalArgumentException ee){
                Logger.log("[TFM][setDeviceValues] Empty response body, but it is OK ("+ ee.toString() + ")");  
            } finally {
                response.close();
            }
        } catch (Exception e) {
            //e.printStackTrace();
            Logger.log("[TFM][setDeviceValues] Error in IOTDirectoryFunc.setDeviceValues: " + e);
            throw new Exception("[TFM][setDeviceValues] Error in IOTDirectoryFunc.setDeviceValues: " + e); 
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                //e.printStackTrace();
                Logger.log("[TFM][setDeviceValues] Error in IOTDirectoryFunc.setDeviceValues: " + e);
                throw new Exception("[TFM][setDeviceValues] Error in IOTDirectoryFunc.setDeviceValues: " + e); 
            }
        }
        if(!responseBody.equals("")){
            Logger.log("[TFM][setDeviceValues] Error in IOTDirectoryFunc.setDeviceValues: " + responseBody);
            throw new Exception("[TFM][setDeviceValues] Error in IOTDirectoryFunc.setDeviceValues: " + responseBody); 
        }

        JSONObject response = new JSONObject();
        response.put("statusCode", statusCode);
        response.put("responseBody", responseBody);
        return response;
    }
}
