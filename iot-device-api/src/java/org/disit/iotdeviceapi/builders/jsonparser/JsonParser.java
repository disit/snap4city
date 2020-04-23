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
package org.disit.iotdeviceapi.builders.jsonparser;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import org.disit.iotdeviceapi.builders.Builder;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.datatypes.DataType;
import org.disit.iotdeviceapi.logging.XLogger;
import org.disit.iotdeviceapi.parser.ParserConst;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.utils.Const;
// import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Element;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class JsonParser extends Builder {
    
    public JsonParser(Element cfg, HashMap<String, Repos> datasources, HashMap<String, Class<?>> datatypes, HashMap<String, Provider> providers, HashMap<String, Data> data, XLogger xlogger) {
        super(cfg, datasources, datatypes, providers, data, xlogger);
    }

    @Override
    public Data build() {
        try {
            
            ArrayList<Object> values = new ArrayList<>();
            
            Object[] defaultValues = null;
            
            if(1 == getCfg().getElementsByTagName(JsonParserConst.CFG_EL_DEFAULT).getLength()) {
                Element defaultElement = (Element)getCfg().getElementsByTagName(JsonParserConst.CFG_EL_DEFAULT).item(0);
                if(defaultElement.hasAttribute(JsonParserConst.CFG_AT_DEFAULT_REF)) {
                    Data defaultData = getData().get(defaultElement.getAttribute(JsonParserConst.CFG_AT_DEFAULT_REF));
                    defaultValues = defaultData.getValue();
                }
                else {
                    defaultValues = new String[]{defaultElement.getTextContent()};
                }
            }
            
            ArrayList<Object> srcTxts = new ArrayList<>();
            Element src = (Element)getCfg().getElementsByTagName(JsonParserConst.CFG_EL_SRC).item(0);
            if(src.hasAttribute(JsonParserConst.CFG_AT_SRC_REF)) {
                Data srcData = getData().get(src.getAttribute(JsonParserConst.CFG_AT_SRC_REF));
                if(srcData.getValue() != null) srcTxts.addAll(Arrays.asList(srcData.getValue()));
            }
            else {
                srcTxts.add(src.getTextContent());
            }
            
            ArrayList<Object> queries = new ArrayList<>();
            Element qry = (Element)getCfg().getElementsByTagName(JsonParserConst.CFG_EL_QUERY).item(0);
            if(qry.hasAttribute(JsonParserConst.CFG_AT_QUERY_REF)) {
                Data qryData = getData().get(qry.getAttribute(JsonParserConst.CFG_AT_QUERY_REF));
                queries.addAll(Arrays.asList(qryData.getValue()));
            }
            else {
                queries.add(qry.getTextContent());
            }
            
            for(Object txt: srcTxts) {

                ArrayList<JSONObject> newJsonObjects = new ArrayList<>();

                for(Object query:queries) {

                    String[] querySegments = null;
                    try {
                        querySegments = ((String)query).split("\\.");
                    }
                    catch(Exception e) {
                        querySegments = new String[1];
                        querySegments[0] = query.toString();
                    }
                    ArrayList<JSONObject> jsonObjects = new ArrayList<>();
                    Object parsed = JSONValue.parse(txt.toString());
                    if(parsed instanceof JSONObject) {
                        jsonObjects.add((JSONObject)parsed);
                    }
                    else if (parsed instanceof JSONArray) {
                        for(int i = 0; i < ((JSONArray)parsed).size(); i++) {
                            JSONObject obj = (JSONObject)((JSONArray) parsed).get(i);
                            jsonObjects.add(obj);
                        }
                    }
                    for (String querySegment : querySegments) {
                        newJsonObjects = new ArrayList<>();
                        for(JSONObject jsonObject: jsonObjects) {
                            Object obj = jsonObject.get(querySegment);
                            if(obj instanceof JSONObject) {
                                newJsonObjects.add((JSONObject)obj);
                            }
                            else if(obj instanceof JSONArray) {
                                for(int i = 0; i < ((JSONArray)obj).size(); i++) {
                                    if(((JSONArray) obj).get(i) instanceof JSONObject) {
                                        newJsonObjects.add(((JSONObject)((JSONArray)obj).get(i)));  
                                    }
                                    else {
                                        values.add(((JSONArray) obj).toString());
                                    }
                                }
                            }
                            else if(obj != null && (!(obj.toString().isEmpty()))) {
                                values.add(obj.toString());
                            }
                            else if(defaultValues != null) {
                                if(defaultValues.length == 1) {
                                    if(!defaultValues[0].toString().isEmpty()) {
                                        values.add(defaultValues[0].toString());
                                    }
                                }
                                else if(defaultValues.length > values.size()){
                                    values.add(defaultValues[values.size()].toString());                                
                                }                                
                            }
                            else { 
                                if("clean".equals(qry.getAttribute(JsonParserConst.CFG_AT_QUERY_ONMISSING))) {
                                    if(qry.hasAttribute(JsonParserConst.CFG_AT_QUERY_REF)) {
                                        Object[] ref = getData().get(qry.getAttribute(JsonParserConst.CFG_AT_QUERY_REF)).getValue();
                                        Vector<Object> newRef = new Vector<>();
                                        for(Object o: ref) {
                                            if(!o.equals(query)) newRef.add(o);
                                        }
                                        getData().get(qry.getAttribute(JsonParserConst.CFG_AT_QUERY_REF)).setValue(newRef.toArray());
                                    }
                                }
                            }
                        }
                        jsonObjects = newJsonObjects;
                    }

                }

                if(!newJsonObjects.isEmpty()) {
                    values.add(newJsonObjects.toString());
                }
                
            }
            
            String dataId = getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID);
            String type = getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE);
            ArrayList<DataType> tValues = new ArrayList<>();
            for(Object oneValue: values) {
                Class<?> typeClass = getDatatypes().get(type);
                Constructor<?> typeConstructor = typeClass.getConstructor();
                DataType typeInst = (DataType)typeConstructor.newInstance();
                typeInst = typeInst.fromString(oneValue.toString());
                // if(typeInst == null) {
                //     throw new IotDeviceApiException(MessageFormat.format("Unable to produce \"{0}\" from \"{1}\".", new Object[]{type, oneValue.toString()}));
                // }
                // tValues.add(typeInst);
                if(typeInst != null) {
                    tValues.add(typeInst);
                }
                else {
                    getXlogger().log(JsonParser.class.getName(), Level.WARNING, MessageFormat.format("Unable to produce \"{0}\" from \"{1}\".", new Object[]{type, oneValue.toString()}), oneValue);
                }
            }

            if(!values.isEmpty()) return new Data(dataId, type, tValues.toArray());
            else return new Data(dataId, type, null);
            
            
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(JsonParser.class.getName(), Level.SEVERE, "JsonParser error", e);
            return new Data(getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID), getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE), null); 
        }
    }
    
}
