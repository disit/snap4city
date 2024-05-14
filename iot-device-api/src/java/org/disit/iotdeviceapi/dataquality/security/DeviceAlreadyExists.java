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

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class DeviceAlreadyExists extends Validator {

    public DeviceAlreadyExists(String id, Element config, XLogger xlogger) {
        super(id, config, xlogger);
    }

    @Override
    public Data clean(Data currentData, HashMap<String, Data> builtData) {        
        try {
            if(builtData.get(DeviceAlreadyExistsConst.CFG_DK_RECEIVED_SERVICE_URI).getValue() != null) {
                return currentData;
            }
            String deviceUri = new String();
            if(currentData.getValue() != null && currentData.getValue().length > 0) deviceUri = currentData.getValue()[0].toString();            
            Element endpoint = (Element)config.getElementsByTagName(DeviceAlreadyExistsConst.CFG_EL_ENDPOINT).item(0);
            String endpointUrl = new String();
            if(endpoint.hasAttribute(DeviceAlreadyExistsConst.CFG_AT_REF)) {
                String endpointName = endpoint.getAttribute(DeviceAlreadyExistsConst.CFG_AT_REF);
                Data endpointData = builtData.get(endpointName);
                if(endpointData.getValue() != null && endpointData.getValue().length > 0) endpointUrl = endpointData.getValue()[0].toString();
            }
            else {
                endpointUrl = endpoint.getTextContent();
            }
            Element accessTokenElmt = (Element)config.getElementsByTagName(DeviceAlreadyExistsConst.CFG_EL_AUTHORIZATION).item(0);
            String accessToken = new String();
            if(accessTokenElmt.hasAttribute(DeviceAlreadyExistsConst.CFG_AT_REF)) {
                String accessTokenName = accessTokenElmt.getAttribute(DeviceAlreadyExistsConst.CFG_AT_REF);
                Data accessTokenData = builtData.get(accessTokenName);
                if(accessTokenData.getValue() != null && accessTokenData.getValue().length > 0) accessToken = accessTokenData.getValue()[0].toString().split(" ")[1];
            }
            else {
                accessToken = accessTokenElmt.getTextContent().split(" ")[1];
            }
            String requestUrl = MessageFormat.format(DeviceAlreadyExistsConst.CFG_REQ_URL, new Object[]{endpointUrl, deviceUri, accessToken});
            URL url = new URL(requestUrl);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            if(http.getResponseCode() == 400) {
                return currentData;
            }
            else {       
                throw new IotDeviceApiException("Device already exists");
            }   
        }
        catch(Exception e) {
            xlogger.log(DeviceAlreadyExists.class.getName(), Level.SEVERE, "Device already exists", e);
            setStatus(Const.ERROR, e.getMessage());
            return currentData;
        }
             
    }    
}