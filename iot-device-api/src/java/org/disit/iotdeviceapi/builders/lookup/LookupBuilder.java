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
package org.disit.iotdeviceapi.builders.lookup;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import org.disit.iotdeviceapi.builders.Builder;
import org.disit.iotdeviceapi.parser.ParserConst;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.datatypes.DataType;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.utils.Const;
import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.disit.iotdeviceapi.logging.XLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class LookupBuilder extends Builder {

    public LookupBuilder(Element cfg, HashMap<String, Repos> datasources, HashMap<String, Class<?>> datatypes, HashMap<String, Provider> providers, HashMap<String, Data> data, XLogger xlogger) {
        super(cfg, datasources, datatypes, providers, data, xlogger);
    }

    @Override
    public Data build() {
        
        try {
            
            Object[] value = null;
            
            NodeList lookups = getCfg().getElementsByTagName(LookupBuilderConst.CFG_EL_LOOKUP);
            ArrayList<Element> sortedLookups = new ArrayList<>(lookups.getLength());
            for(int i = 0; i < lookups.getLength(); i++) {
                int priority;
                Element priorityElement = (Element)((Element)lookups.item(i)).getElementsByTagName(LookupBuilderConst.CFG_EL_LOOKUP_PRIORITY).item(0);
                if(!priorityElement.hasAttribute(LookupBuilderConst.CFG_AT_LOOKUP_PRIORITY_REF)) {
                    priority = Integer.parseInt(priorityElement.getTextContent());
                }
                else {
                    String priorityRef = priorityElement.getAttribute(LookupBuilderConst.CFG_AT_LOOKUP_PRIORITY_REF);
                    priority = Integer.parseInt(getData().get(priorityRef).getValue()[0].toString());
                }
                sortedLookups.add(-1+priority, (Element)lookups.item(i));
            }
            
            for(Element lookup:sortedLookups) {

                Object[] query;
                Element queryElement = (Element)lookup.getElementsByTagName(LookupBuilderConst.CFG_EL_LOOKUP_QUERY).item(0);
                
                if(!queryElement.hasAttribute(LookupBuilderConst.CFG_AT_LOOKUP_QUERY_REF)) {
                    query = new Object[]{queryElement.getTextContent()};
                }
                else {
                    String queryVar = queryElement.getAttribute(LookupBuilderConst.CFG_AT_LOOKUP_QUERY_REF);
                    query = getData().get(queryVar).getValue();
                }

                Object[] providerIDs;
                Element providerElement = (Element)lookup.getElementsByTagName(LookupBuilderConst.CFG_EL_LOOKUP_PROVIDER).item(0);
                if(!providerElement.hasAttribute(LookupBuilderConst.CFG_AT_LOOKUP_PROVIDER_REF)) {
                    providerIDs = new Object[]{providerElement.getTextContent()};
                }
                else {
                    String providerRef = providerElement.getAttribute(LookupBuilderConst.CFG_AT_LOOKUP_PROVIDER_REF);
                    providerIDs = getData().get(providerRef).getValue();
                }
                
                for(Object providerID: providerIDs ) {
                    Provider provider = getProviders().get(providerID.toString());
                    value = provider.get(query);
                    if(Const.ERROR == provider.getStatus()) {
                        throw new Exception("Data collection from provider failed.");
                    }
                    if(value != null) break;
                }

                if(value != null) break;
                
            }

            String dataId = getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID);
            String type = getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE);
            ArrayList<DataType> tValue = new ArrayList<>();
            if(value != null) {
                for(Object oneValue: value) {
                    Class<?> typeClass = getDatatypes().get(type);
                    Constructor<?> typeConstructor = typeClass.getConstructor();
                    DataType typeInst = (DataType)typeConstructor.newInstance();
                    typeInst = typeInst.fromString(oneValue.toString());
                    if(typeInst == null) {
                        throw new IotDeviceApiException(MessageFormat.format("Unable to produce \"{0}\" from \"{1}\".", new Object[]{type, oneValue.toString()}));
                    }
                    tValue.add(typeInst);
                }
            }
            
            if(tValue.isEmpty()) return new Data(dataId, type, null);
            return new Data(dataId, type,tValue.toArray());
            
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(LookupBuilder.class.getName(), Level.SEVERE, "LookupBuilder error", e);
            return new Data(getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID), getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE),null);
        }
        
    }

}
