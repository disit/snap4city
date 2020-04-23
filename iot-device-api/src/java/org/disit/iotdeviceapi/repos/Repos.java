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
package org.disit.iotdeviceapi.repos;

import java.util.HashMap;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class Repos {
    
    String id;
    HashMap<String, String> parameters; 
    
    public Repos(String id) {
        this.id = id;
        this.parameters = new HashMap<>();
    }
    
    public Repos(String id, HashMap<String,String> parameters) {
        this.id = id;
        this.parameters = parameters;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public void setParameter(String key, String value) {
        this.parameters.put(key, value);
    }
    
    public String getParameter(String key) {
        return this.parameters.get(key);
    }
    
    public HashMap<String, String> getParameters() {
        return parameters;
    }
    
}
