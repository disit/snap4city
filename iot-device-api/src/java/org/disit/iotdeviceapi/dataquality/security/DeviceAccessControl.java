/* IOTDEVICEAPI
   Copyright (C) 2017 DISIT Lab http://www.disit.org - University of Florence

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
package org.disit.iotdeviceapi.dataquality.security;

import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.disit.iotdeviceapi.dataquality.Validator;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.logging.XLogger;
import org.disit.iotdeviceapi.utils.Const;
import org.w3c.dom.Element;
import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.util.Base64;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class DeviceAccessControl extends Validator {

    public DeviceAccessControl(String id, Element config, XLogger xlogger) {
        super(id, config, xlogger);
    }

    @Override
    public Data clean(Data currentData, HashMap<String, Data> builtData) {
        try {
            if(currentData.getValue() == null) return currentData;
            String deviceUri = new String();
            if(currentData.getValue() != null && currentData.getValue().length > 0) deviceUri = currentData.getValue()[0].toString();
            String id = new String();
            if(!deviceUri.isEmpty()) id = deviceUri.split("/")[7]+":"+deviceUri.split("/")[6]+":"+deviceUri.split("/")[8];
            Element endpoint = (Element)config.getElementsByTagName(DeviceAccessControlConst.CFG_EL_ENDPOINT).item(0);
            String endpointUrl = new String();
            if(endpoint.hasAttribute(DeviceAccessControlConst.CFG_AT_REF)) {
                String endpointName = endpoint.getAttribute(DeviceAccessControlConst.CFG_AT_REF);
                Data endpointData = builtData.get(endpointName);
                if(endpointData.getValue() != null && endpointData.getValue().length > 0) endpointUrl = endpointData.getValue()[0].toString();
            }
            else {
                endpointUrl = endpoint.getTextContent();
            }
            Element accessTokenElmt = (Element)config.getElementsByTagName(DeviceAccessControlConst.CFG_EL_AUTHORIZATION).item(0);
            String accessToken = new String();
            if(accessTokenElmt.hasAttribute(DeviceAccessControlConst.CFG_AT_REF)) {
                String accessTokenName = accessTokenElmt.getAttribute(DeviceAccessControlConst.CFG_AT_REF);
                Data accessTokenData = builtData.get(accessTokenName);
                if(accessTokenData.getValue() != null && accessTokenData.getValue().length > 0) accessToken = accessTokenData.getValue()[0].toString().split(" ")[1];
            }
            else {
                accessToken = accessTokenElmt.getTextContent().split(" ")[1];
            }
            String requestUrl = MessageFormat.format(DeviceAccessControlConst.CFG_REQ_URL, new Object[]{endpointUrl, id, accessToken});
            URL url = new URL(requestUrl);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            if(http.getResponseCode() == 200) {
                InputStream in = http.getInputStream();
                String encoding = http.getContentEncoding();
                encoding = encoding == null ? "UTF-8" : encoding;
                String body = IOUtils.toString(in, encoding);
                if(!"[\n]\n".equals(body)) return currentData;
                else {
                    String[] tokenParts = accessToken.split( "\\." );
                    String payload = new String( Base64.getDecoder().decode( tokenParts[1] ) );
                    JSONObject payloadObj = ((JSONObject)JSONValue.parse(payload));
                    String username = "";
                    if (payloadObj.get("username") != null)
                        username = payloadObj.get("username").toString();
                    else {
                        username = payloadObj.get("preferred_username").toString();
                    }
                    // Check delegations

                    endpoint = (Element)config.getElementsByTagName(DeviceAccessControlConst.DELEGATION_EL_ENDPOINT).item(0);
                    endpointUrl = endpoint.getTextContent();
                    requestUrl = MessageFormat.format(DeviceAccessControlConst.DELEGATION_REQ_URL, new Object[]{endpointUrl, username, accessToken});
                    url = new URL(requestUrl);
                    http = (HttpURLConnection)url.openConnection();
                    if(http.getResponseCode() == 200) {
                        in = http.getInputStream();
                        encoding = http.getContentEncoding();
                        encoding = encoding == null ? "UTF-8" : encoding;
                        body = IOUtils.toString(in, encoding);
                        if(!"[\n]\n".equals(body)) {
                            Object parsed = JSONValue.parse(body);
                            for(int i = 0; i < ((JSONArray)parsed).size(); i++) {
                                JSONObject delegation = (JSONObject)((JSONArray) parsed).get(i);
                                if (delegation.get("elementId").equals(id) && delegation.get("kind").equals("MODIFY")) {
                                    return currentData;
                                }
                            }
                        }
                        throw new IotDeviceApiException("No permissions available");
                    } else throw new IotDeviceApiException("Error retrieving delegation infos");
                }
            }
            else throw new IotDeviceApiException("Error retrieving ownership infos");
        }
        catch(Exception e) {
            xlogger.log(DeviceAccessControl.class.getName(), Level.SEVERE, "Security check failed", e);
            setStatus(Const.ERROR, e.getMessage());
            return currentData;
        }
             
    }    
}