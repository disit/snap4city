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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.disit.iotdeviceapi.dataquality.Validator;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.logging.XLogger;
import org.disit.iotdeviceapi.utils.Const;
import org.w3c.dom.Element;
import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class ValidateToken extends Validator {

    public ValidateToken(String id, Element config, XLogger xlogger) {
        super(id, config, xlogger);
    }

    @Override
    public Data clean(Data currentData, HashMap<String, Data> builtData) {        
        try {
            Element endpoint = (Element)config.getElementsByTagName(ValidateTokenConst.CFG_EL_ENDPOINT).item(0);
            String endpointUrl = new String();
            if(endpoint.hasAttribute(ValidateTokenConst.CFG_AT_REF)) {
                String endpointName = endpoint.getAttribute(ValidateTokenConst.CFG_AT_REF);
                Data endpointData = builtData.get(endpointName);
                if(endpointData.getValue() != null && endpointData.getValue().length > 0) endpointUrl = endpointData.getValue()[0].toString();
            }
            else {
                endpointUrl = endpoint.getTextContent();
            }
            Element accessTokenElmt = (Element)config.getElementsByTagName(ValidateTokenConst.CFG_EL_AUTHORIZATION).item(0);
            String authorization = new String();
            if(accessTokenElmt.hasAttribute(ValidateTokenConst.CFG_AT_REF)) {
                String accessTokenName = accessTokenElmt.getAttribute(ValidateTokenConst.CFG_AT_REF);
                Data accessTokenData = builtData.get(accessTokenName);
                if(accessTokenData.getValue() != null && accessTokenData.getValue().length > 0) authorization = accessTokenData.getValue()[0].toString();
            }
            else {
                authorization = accessTokenElmt.getTextContent();
            }
            URL url = new URL(endpointUrl);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestProperty("Authorization",authorization);
            if(http.getResponseCode() == 200) {
                if(config.getElementsByTagName(ValidateTokenConst.CFG_EL_ROLES).getLength() > 0) {
                    Element levelsElmt = (Element)config.getElementsByTagName(ValidateTokenConst.CFG_EL_ROLES).item(0);
                    String[] levels = null;
                    if(levelsElmt.hasAttribute(ValidateTokenConst.CFG_AT_REF)) {
                        String levelsName = levelsElmt.getAttribute(ValidateTokenConst.CFG_AT_REF);
                        Data levelsData = builtData.get(levelsName);
                        if(levelsData.getValue() != null && levelsData.getValue().length > 0) levels = levelsData.getValue()[0].toString().split(",");
                    }
                    else {
                        levels = levelsElmt.getTextContent().split(",");
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(http.getInputStream()));
                    String r = ""; String l; while ((l = br.readLine()) != null) { r = r.concat(l);}                    
                    JSONObject userInfo = (JSONObject)JSONValue.parse(r);
                    Set<String> userRoles = new HashSet<>(Arrays.asList((String[])((JSONArray)userInfo.get("roles")).toArray(new String[0])));                
                    Set<String> requiredRoles = new HashSet<>(Arrays.asList(levels));
                    userRoles.retainAll(requiredRoles);
                    if(!userRoles.isEmpty()) return currentData;
                    else throw new IotDeviceApiException("Invalid user role");
                }
                else {              
                    return currentData;
                }
            }
            else {       
                throw new IotDeviceApiException("Invalid access token");
            }   
        }
        catch(Exception e) {
            xlogger.log(ValidateToken.class.getName(), Level.SEVERE, "Invalid access token", e);
            setStatus(Const.ERROR);
            return currentData;
        }
             
    }    
}