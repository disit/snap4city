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
package org.disit.iotdeviceapi.builders.template;

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
public class TemplateBuilder extends Builder {

    public TemplateBuilder(Element cfg, HashMap<String, Repos> datasources, HashMap<String, Class<?>> datatypes, HashMap<String, Provider> providers, HashMap<String, Data> data, XLogger xlogger) {
        super(cfg, datasources, datatypes, providers, data, xlogger);
    }

    @Override
    public Data build() {
        
        try {
            
            // Retrieving template...

            Object[] templates;
            if(!((Element)getCfg().getElementsByTagName(TemplateBuilderConst.CFG_EL_TEMPLATE).item(0)).hasAttribute(TemplateBuilderConst.CFG_AT_TEMPLATE_REF)) {
                templates = new Object[]{getCfg().getElementsByTagName(TemplateBuilderConst.CFG_EL_TEMPLATE).item(0).getTextContent()};
            }
            else
            {
                String ref = ((Element)getCfg().getElementsByTagName(TemplateBuilderConst.CFG_EL_TEMPLATE).item(0)).getAttribute(TemplateBuilderConst.CFG_AT_TEMPLATE_REF);
                templates = getData().get(ref).getValue();
            }

            // Retrieving parameters...

            NodeList paramNodes = getCfg().getElementsByTagName(TemplateBuilderConst.CFG_EL_TEMPLATE_PARAM);
            ArrayList<Object[]> params = new ArrayList<>(paramNodes.getLength());
            for(int i = 0; i < paramNodes.getLength(); i++) {
                int index = Integer.parseInt(((Element)paramNodes.item(i)).getAttribute(TemplateBuilderConst.CFG_AT_TEMPLATE_PARAM_INDEX));
                String value = ((Element)paramNodes.item(i)).getAttribute(TemplateBuilderConst.CFG_AT_TEMPLATE_PARAM_VALUE);
                params.add(index, getData().get(value).getValue());
            }

            // Building...

            int c = 1;
            for(Object[] param: params) {
                if(param == null) c = 0;
                else c = c*param.length;
            }
            ArrayList<Object[]> paramsCartesian = new ArrayList<>(c);
            for(int i = 0; i < c; i++) {
                paramsCartesian.add(new Object[paramNodes.getLength()]);
            }
            for(int col = 0; col < params.size(); col++) {
                for(int row = 0; row < c; row++) {
                    int keepFor = c/(params.get(col).length);
                    int index = new Double(Math.floor(row/keepFor)).intValue();
                    paramsCartesian.get(row)[col] = params.get(col)[index];
                }
            }

            ArrayList<Object> output = null;
            for(Object[] paramsCartesianRow:paramsCartesian) {
                for(Object template: templates) {
                    Object formatted = MessageFormat.format(template.toString(), paramsCartesianRow);
                    if(output == null) output = new ArrayList<>();
                    output.add(formatted);
                }
            }

            String dataId = getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID);
            String type = getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE);
            if(output == null) return new Data(dataId, type, null);
            else  {
                ArrayList<DataType> tOutput = new ArrayList<>();
                for(Object oneOutput: output) {
                    Class<?> typeClass = getDatatypes().get(type);
                    Constructor<?> typeConstructor = typeClass.getConstructor();
                    DataType typeInst = (DataType)typeConstructor.newInstance();
                    typeInst = typeInst.fromString(oneOutput.toString());
                    if(typeInst == null) {
                        throw new IotDeviceApiException(MessageFormat.format("Unable to produce \"{0}\" from \"{1}\".", new Object[]{type, oneOutput.toString()}));
                    }
                    tOutput.add(typeInst);
                }
                return new Data(dataId, type, tOutput.toArray());
            } 
        
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(TemplateBuilder.class.getName(), Level.SEVERE, "TemplateBuilder error", e);
            return new Data(getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID), getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE), null);
        }
        
    }
    
}