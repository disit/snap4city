/* Data Manager (DM).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
package edu.unifi.disit.datamanager.datamodel.profiledb;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class DeviceGroupElementSerializer extends StdSerializer<DeviceGroupElement> {

	private static final long serialVersionUID = 1L;

	public DeviceGroupElementSerializer() {
		this(null);
	}

	public DeviceGroupElementSerializer(Class<DeviceGroupElement> t) {
		super(t);
	}

	@Override
	public void serialize(DeviceGroupElement grp, JsonGenerator jgen, SerializerProvider provider) throws IOException {

		jgen.writeStartObject();

		if (grp.getId() != null) {
			jgen.writeNumberField("id", grp.getId());
		}
		if (grp.getDeviceGroupId() != null) {
			jgen.writeNumberField("deviceGroupId", grp.getDeviceGroupId());
		}
		if (grp.getDeleteTime() != null) {
			jgen.writeNumberField("deleteTime", grp.getDeleteTime().getTime());
		}
                if (grp.getInsertTime() != null) {
			jgen.writeNumberField("insertTime", grp.getInsertTime().getTime());
		}
                if (grp.getElementId() != null) {
			jgen.writeStringField("elementId", grp.getElementId());
		}
		if (grp.getElementType() != null) {
			jgen.writeStringField("elementType", grp.getElementType());
		}
                if (grp.getElmtTypeLbl() != null) {
			jgen.writeStringField("elmtTypeLbl", grp.getElmtTypeLbl());
		}
                if (grp.getUsername() != null) {
			jgen.writeStringField("username", grp.getUsername());
		}
                if (grp.getElementName() != null && !grp.getElementName().isEmpty()) {
                    jgen.writeStringField("elementName", grp.getElementName());
		}
                else {
                    jgen.writeStringField("elementName", "-- Not Found --");
                }
                
		jgen.writeEndObject();
	}
}