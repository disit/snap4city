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
package org.disit.iotdeviceapi.builders;

import org.disit.iotdeviceapi.datatypes.Data;
import java.util.HashMap;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.utils.Const;
import org.disit.iotdeviceapi.logging.XLogger;
import org.w3c.dom.Element;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class Builder {
    
    private final Element cfg;
    private final HashMap<String, Repos> datasources;
    private final HashMap<String, Class<?>> datatypes;
    private final HashMap<String, Provider> providers;
    private final HashMap<String,Data> data;
    private int status;
    private final XLogger xlogger;
    
    public Builder(Element cfg, HashMap<String, Repos> datasources, HashMap<String, Class<?>> datatypes, HashMap<String, Provider> providers, HashMap<String, Data> data, XLogger xlogger) {
        this.cfg = cfg;
        this.datasources = datasources;
        this.datatypes = datatypes;
        this.providers = providers;
        this.data = data;
        this.status = Const.OK;
        this.xlogger = xlogger;
    }
    
    public Data build() {
        return null;
    }

    public Element getCfg() {
        return cfg;
    }

    public HashMap<String, Repos> getDatasources() {
        return datasources;
    }

    public HashMap<String, Class<?>> getDatatypes() {
        return datatypes;
    }

    public HashMap<String, Provider> getProviders() {
        return providers;
    }

    public HashMap<String, Data> getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public XLogger getXlogger() {
        return xlogger;
    }
    
}
