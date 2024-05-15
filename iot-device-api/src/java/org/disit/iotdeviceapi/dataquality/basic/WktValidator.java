/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.disit.iotdeviceapi.dataquality.basic;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import java.util.ArrayList;
import java.util.HashMap;
import org.disit.iotdeviceapi.dataquality.Validator;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.logging.XLogger;
import org.disit.iotdeviceapi.utils.Const;
import org.w3c.dom.Element;

/**
 *
 * @author pierf
 */
public class WktValidator extends Validator {

    public WktValidator(String id, Element config, XLogger xlogger) {
        super(id, config, xlogger);
    }

    @Override
    public Data clean(Data currentData, HashMap<String, Data> builtData) {
        Object[] value = currentData.getValue();
        if(value != null && value.length>=1) {
            String wkt = value[0].toString();
            System.out.println("VALIDATE WKT: "+wkt);
            try {
                WKTReader reader = new WKTReader();

                Geometry geometry = reader.read(wkt);
                System.out.println("WKT type: " + geometry.getGeometryType());
                if(!wkt.equals(wkt.toUpperCase())) {
                    System.err.println("WKT is not uppercase");
                    setStatus(Const.ERROR, "WKT is not uppercase");            
                }
            } catch (ParseException e) {
                System.err.println("WKT is not valid: " + e.getMessage());
                setStatus(Const.ERROR, "WKT is not valid: " + e.getMessage());
            }
        }
        return currentData;
    }
    
}
