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
package org.disit.iotdeviceapi.dataquality;

import java.util.HashMap;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.logging.XLogger;
import org.w3c.dom.Element;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.text.MessageFormat;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public abstract class Validator {
    
    public Element config; 
    public String id;
    public XLogger xlogger;
    public int status;
    public String statusMsg = "";
    
    public Validator(String id, Element config, XLogger xlogger) {
        this.config = config;
        this.id = id;
        this.xlogger = xlogger;
    }
    
    public abstract Data clean(Data currentData, HashMap<String, Data> builtData);

    public Element getConfig() {
        return config;
    }

    public void setConfig(Element config) {
        this.config = config;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public XLogger getXlogger() {
        return xlogger;
    }

    public void setXlogger(XLogger xlogger) {
        this.xlogger = xlogger;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status, String msg) {
        this.status = status;
        this.statusMsg = msg;
    }
    
    public String toString() { 
        return statusMsg;
        /*
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(this.getConfig());
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(source, result);
            String strObject = result.getWriter().toString();
            
            return MessageFormat.format("The test that failed is the following:\n\n{0}",new Object[]{strObject});
        }
        catch(Exception e) {
            return "Unable to produce a string representation for the configuration of the validator.";
        }*/
    } 
    
}
