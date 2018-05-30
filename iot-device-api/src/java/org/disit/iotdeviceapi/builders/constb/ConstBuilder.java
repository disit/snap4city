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
package org.disit.iotdeviceapi.builders.constb;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import org.disit.iotdeviceapi.datatypes.Data;
import java.util.HashMap;
import java.util.logging.Level;
import org.disit.iotdeviceapi.builders.Builder;
import org.disit.iotdeviceapi.parser.ParserConst;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.datatypes.DataType;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.utils.Const;
import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.disit.iotdeviceapi.logging.XLogger;
import org.w3c.dom.Element;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class ConstBuilder extends Builder {

    public ConstBuilder(Element cfg, HashMap<String, Repos> datasources, HashMap<String, Class<?>> datatypes, HashMap<String, Provider> providers, HashMap<String, Data> data, XLogger xlogger) {
        super(cfg, datasources, datatypes, providers, data, xlogger);
    }

    @Override
    public Data build() {
        try {
            String dataId = getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID);
            String type = getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE);
            String value = getCfg().getTextContent();
            Class<?> typeClass = getDatatypes().get(type);
            Constructor<?> typeConstructor = typeClass.getConstructor();
            DataType typeInst = (DataType)typeConstructor.newInstance();
            typeInst = typeInst.fromString(value);
            if(typeInst == null) {
                throw new IotDeviceApiException(MessageFormat.format("Unable to produce \"{0}\" from \"{1}\".", new Object[]{type, value}));
            }
            return new Data(dataId, type, new DataType[]{typeInst}); 
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(ConstBuilder.class.getName(), Level.SEVERE, "ConstBuilder error", e);
            return new Data(getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID), getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE), null); 
        }
    }
 
}
