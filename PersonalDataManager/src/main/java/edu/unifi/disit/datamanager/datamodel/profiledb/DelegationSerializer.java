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

public class DelegationSerializer extends StdSerializer<Delegation> {

	private static final long serialVersionUID = 1L;

	public DelegationSerializer() {
		this(null);
	}

	public DelegationSerializer(Class<Delegation> vc) {
		super(vc);
	}

	@Override
	public void serialize(Delegation delegation, JsonGenerator jgen, SerializerProvider provider) throws IOException {

		jgen.writeStartObject();

		if (delegation.getId() != null)
			jgen.writeNumberField("id", delegation.getId());
		if (delegation.getUsernameDelegator() != null)
			jgen.writeStringField("usernameDelegator", delegation.getUsernameDelegator());
		if (delegation.getUsernameDelegated() != null)
			jgen.writeStringField("usernameDelegated", delegation.getUsernameDelegated());
		if (delegation.getVariableName() != null)
			jgen.writeStringField("variableName", delegation.getVariableName());
		if (delegation.getMotivation() != null)
			jgen.writeStringField("motivation", delegation.getMotivation());
		if (delegation.getElementId() != null)
			jgen.writeStringField("elementId", delegation.getElementId());
		if (delegation.getElementType() != null)
			jgen.writeStringField("elementType", delegation.getElementType());
		if (delegation.getInsertTime() != null)
			jgen.writeNumberField("insertTime", delegation.getInsertTime().getTime());
		if (delegation.getDelegationDetails() != null)
			jgen.writeStringField("delegationDetails", delegation.getDelegationDetails());
		if (delegation.getGroupnameDelegated() != null)
			jgen.writeStringField("groupnameDelegated", delegation.getGroupnameDelegated());
		if (delegation.getKind() != null)
			jgen.writeStringField("kind", delegation.getKind());

		jgen.writeEndObject();
	}
}
