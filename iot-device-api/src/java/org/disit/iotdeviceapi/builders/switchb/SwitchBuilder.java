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
package org.disit.iotdeviceapi.builders.switchb;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import org.disit.iotdeviceapi.builders.Builder;
import org.disit.iotdeviceapi.parser.ParserConst;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.datatypes.DataType;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.utils.Const;
// import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.disit.iotdeviceapi.logging.XLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class SwitchBuilder extends Builder {

    public SwitchBuilder(Element cfg, HashMap<String, Repos> datasources, HashMap<String, Class<?>> datatypes, HashMap<String, Provider> providers, HashMap<String, Data> data, XLogger xlogger) {
        super(cfg, datasources, datatypes, providers, data, xlogger);
    }

    @Override
    public Data build() {
        
        try {
            
            Element switchElement = (Element)getCfg().getElementsByTagName(SwitchBuilderConst.CFG_EL_SWITCH).item(0);
            Data ref = getData().get(switchElement.getAttribute(SwitchBuilderConst.CFG_AT_SWITCH_REF));
            ArrayList<Object> output = new ArrayList<>();

            if(ref.getValue() != null) {
                for(Object refValue:ref.getValue()) 
                {
                    NodeList cases = switchElement.getElementsByTagName(SwitchBuilderConst.CFG_EL_SWITCH_CASE);
                    for(int i = 0; i < cases.getLength(); i++) {
                        Element caseElement = (Element)cases.item(i);
                        Element ifElement = (Element)caseElement.getElementsByTagName(SwitchBuilderConst.CFG_EL_IF).item(0);
                        Object[] ifCases;
                        if(!ifElement.hasAttribute(SwitchBuilderConst.CFG_AT_IF_REF)) {
                            ifCases = new Object[]{ifElement.getTextContent()};
                        }
                        else {
                            String ifRef = ifElement.getAttribute(SwitchBuilderConst.CFG_AT_IF_REF);
                            ifCases = getData().get(ifRef).getValue();
                        }
                        for(Object ifCase: ifCases) {
                            if(refValue.toString().matches(ifCase.toString())) {
                                Element thenElement = (Element)caseElement.getElementsByTagName(SwitchBuilderConst.CFG_EL_THEN).item(0);
                                if(!thenElement.hasAttribute(SwitchBuilderConst.CFG_AT_THEN_REF)) {
                                    String thenStr = thenElement.getTextContent();
                                    output.add(thenStr);
                                }
                                else {
                                    String thenRef = thenElement.getAttribute(SwitchBuilderConst.CFG_AT_THEN_REF);
                                    Data caseThenData = getData().get(thenRef);
                                    output.addAll(Arrays.asList(caseThenData.getValue()));
                                }
                                break;
                            }
                        }
                    }
                }
            }

            String type = getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE);
            String dataId = getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID);
            if(output.isEmpty() && ref.getValue() != null) output.addAll(Arrays.asList(ref.getValue()));
            
            ArrayList<DataType> tOutput = new ArrayList<>();
            for(Object oneOutput: output) {
                Class<?> typeClass = getDatatypes().get(type);
                Constructor<?> typeConstructor = typeClass.getConstructor();
                DataType typeInst = (DataType)typeConstructor.newInstance();
                typeInst = typeInst.fromString(oneOutput.toString());
                // if(typeInst == null) {
                //    throw new IotDeviceApiException(MessageFormat.format("Unable to produce \"{0}\" from \"{1}\".", new Object[]{type, oneOutput.toString()}));
                // }
                // tOutput.add(typeInst);
                if(typeInst != null) {
                    tOutput.add(typeInst);
                }
                else {
                    getXlogger().log(SwitchBuilder.class.getName(), Level.WARNING, MessageFormat.format("Unable to produce \"{0}\" from \"{1}\".", new Object[]{type, oneOutput.toString()}), oneOutput);
                }                
            }
            
            return new Data(dataId, type, tOutput.toArray());

        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(SwitchBuilder.class.getName(), Level.SEVERE, "SwitchBuilder error", e);
            return new Data(getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID), getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE), null);
        }
    }

}
