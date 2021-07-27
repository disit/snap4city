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
package org.disit.iotdeviceapi.datatypes;

import java.net.URI;
import java.net.URLEncoder;
import org.disit.iotdeviceapi.utils.Const;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class UrlWrapper extends DataType {
    
      URI value;

    public UrlWrapper() {
        super();
        value = null;
    } 
    
    @Override
    public String toString() {
        try {
            return value.toString();
        }
        catch(Exception e) {
            status = Const.ERROR;
            return null;
        }
    }

    @Override
    public UrlWrapper fromString(String str) {
        try {
            URI uri;
            try {
                String input = str;
                String scheme = input.substring(0,input.indexOf("//"));
                input = input.substring(scheme.length());
                String[] inputArray = input.split("/");
                String[] outputArray = new String[inputArray.length];
                for(int i = 0; i < inputArray.length; i++) {
                    outputArray[i] = URLEncoder.encode(inputArray[i], "UTF-8");
                }
                String output = String.join("/", outputArray);
                uri = new URI(scheme.concat(output.replace("%23","#")));
            }
            catch(Exception ie) {                
                if(str.startsWith("urn:")) {
                    uri = URI.create(str);
                }
                else {
                    throw ie;
                }
            }            
            UrlWrapper myUri = new UrlWrapper();
            myUri.setValue(uri);
            return myUri;
        }
        catch(Exception e) {
            status = Const.ERROR;
            return null;            
        }
    }

    public URI getValue() {
        return value;
    }

    public void setValue(URI value) {
        this.value = value;
    }
    
}
