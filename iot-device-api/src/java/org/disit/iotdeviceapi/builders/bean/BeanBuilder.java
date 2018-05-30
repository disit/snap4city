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
package org.disit.iotdeviceapi.builders.bean;

import java.beans.Statement;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.logging.Level;
import org.disit.iotdeviceapi.builders.Builder;
import org.disit.iotdeviceapi.parser.ParserConst;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.utils.Const;
import org.disit.iotdeviceapi.logging.XLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class BeanBuilder extends Builder {

    public BeanBuilder(Element cfg, HashMap<String, Repos> datasources, HashMap<String, Class<?>> datatypes, HashMap<String, Provider> providers, HashMap<String, Data> data, XLogger xlogger) {
        super(cfg, datasources, datatypes, providers, data, xlogger);
    }

    @Override
    public Data build() {
        
        try {
            String dataId = getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID);
            String datatypeName = getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE);
            Class<?> datatype = getDatatypes().get(datatypeName);
            Constructor<?> constructor = datatype.getConstructor();
            Object obj = constructor.newInstance();
            NodeList members = getCfg().getElementsByTagName(BeanBuilderConst.CFG_EL_MEMBER);
            for(int i = 0; i < members.getLength(); i++) {
                Element member = (Element)members.item(i);
                String memberName = member.getAttribute(BeanBuilderConst.CFG_AT_MEMBER_NAME);
                String memberDataID = member.getAttribute(BeanBuilderConst.CFG_AT_MEMBER_REF);
                Data memberData = getData().get(memberDataID);
                String methodName = "set" + memberName.substring(0, 1).toUpperCase()+memberName.substring(1);
                Statement stmt = new Statement(obj, methodName, new Object[]{memberData});
                stmt.execute();
            }
            return new Data(dataId, datatypeName,new Object[]{obj});
            
        } 
        catch (Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(BeanBuilder.class.getName(), Level.SEVERE, "BeanBuilder error", e);
            return new Data(getCfg().getAttribute(ParserConst.CFG_AT_DATA_ID), getCfg().getAttribute(ParserConst.CFG_AT_DATA_TYPE),null);
        } 

    }

}
