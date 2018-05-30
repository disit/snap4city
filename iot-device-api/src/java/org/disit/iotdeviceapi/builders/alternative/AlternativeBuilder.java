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
package org.disit.iotdeviceapi.builders.alternative;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import org.disit.iotdeviceapi.builders.Builder;
import org.disit.iotdeviceapi.builders.constb.ConstBuilder;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.datatypes.DataType;
import org.disit.iotdeviceapi.logging.XLogger;
import org.disit.iotdeviceapi.parser.ParserConst;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.utils.Const;
import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class AlternativeBuilder extends Builder {
    
    public AlternativeBuilder(Element cfg, HashMap<String, Repos> datasources, HashMap<String, Class<?>> datatypes, HashMap<String, Provider> providers, HashMap<String, Data> data, XLogger xlogger) {
        super(cfg, datasources, datatypes, providers, data, xlogger);
    }

    @Override
    public Data build() {
        try {
            
            NodeList alternatives = getCfg().getElementsByTagName(AlternativeBuilderConst.CFG_EL_ALTERNATIVE);
            Object[] values = new Object[]{};
            for(int i = 0; i < alternatives.getLength(); i++) {
                Element alternative = (Element)alternatives.item(i);
                if(alternative.hasAttribute(AlternativeBuilderConst.CFG_AT_ALTERNATIVE_REF)) {
                    String ref = alternative.getAttribute(AlternativeBuilderConst.CFG_AT_ALTERNATIVE_REF);
                    Data altData = getData().get(ref);
                    Object[] altDataValues = altData.getValue();
                    if(values.length == 0 && altDataValues != null && altDataValues.length > 0) {
                        values = altDataValues;
                    }
                }
                else {
                    String altStr = alternative.getTextContent();
                    if(values.length == 0 && !altStr.isEmpty()) {
                        values = new Object[]{altStr};
                    }
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
                if(typeInst == null) {
                    throw new IotDeviceApiException(MessageFormat.format("Unable to produce \"{0}\" from \"{1}\".", new Object[]{type, oneValue.toString()}));
                }
                tValues.add(typeInst);
            }

            if(!tValues.isEmpty()) return new Data(dataId, type, tValues.toArray());
            else return new Data(dataId, type, null);
            
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(ConstBuilder.class.getName(), Level.SEVERE, "ConstBuilder error", e);
            return new Data(getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID), getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE), null); 
        }
    }
    
    
    
}
