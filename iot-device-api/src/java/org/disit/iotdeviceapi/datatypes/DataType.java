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
package org.disit.iotdeviceapi.datatypes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import org.disit.iotdeviceapi.utils.Const;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class DataType implements Serializable {
     
    private final static long serialVersionUID = 1;
    int status;
    
    public DataType() {
        status = Const.OK;
    }
    
    public DataType fromString(String str) {
        try {
            byte [] data = Base64.getDecoder().decode( str );
            Object o;
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(  data ) )) {
                o = ois.readObject();
            }
            return (DataType)o;
        }
        catch(Exception e) {
            status = Const.ERROR;
            return null;
        }
    }

    @Override
    public String toString() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream( baos )) {
                oos.writeObject( this );
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray()); 
        }
        catch(Exception e) {
            status = Const.ERROR;
            return null;
        }
    }

    public int getStatus() {
        return status;
    }

}
