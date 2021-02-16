/* Data Manager (DM).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
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
package edu.unifi.disit.datamanager.datamodel.profiledb;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class DataSerializer extends StdSerializer<Data> {

	private static final long serialVersionUID = 1L;

	public DataSerializer() {
		this(null);
	}

	public DataSerializer(Class<Data> t) {
		super(t);
	}

	@Override
	public void serialize(Data value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

		jgen.writeStartObject();

		if (value.getId() != null)
			jgen.writeNumberField("id", value.getId());

		if (value.getUsername() != null)
			jgen.writeStringField("username", value.getUsername());
		if (value.getDataTime() != null)
			jgen.writeNumberField("dataTime", value.getDataTime().getTime());
        if (value.getDataTimeEnd() != null)
            jgen.writeNumberField("dataTimeEnd", value.getDataTimeEnd().getTime()); 
		if (value.getAppName() != null)
			jgen.writeStringField("APPName", value.getAppName());
		if (value.getAppId() != null)
			jgen.writeStringField("APPID", value.getAppId());
		if (value.getMotivation() != null)
			jgen.writeStringField("motivation", value.getMotivation());
		if (value.getVariableName() != null)
			jgen.writeStringField("variableName", value.getVariableName());
		if (value.getVariableValue() != null)
			jgen.writeStringField("variableValue", value.getVariableValue());
		if (value.getVariableUnit() != null)
			jgen.writeStringField("variableUnit", value.getVariableUnit());
		//
		// if (value.getUidName() != null) // transient
		// jgen.writeStringField("uidName", value.getUidName());

		jgen.writeEndObject();
	}
}
