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
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class DataDeserializer extends StdDeserializer<Data> {

	private static final long serialVersionUID = 1L;

	public DataDeserializer() {
		this(null);
	}

	public DataDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public Data deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		JsonNode node = jp.getCodec().readTree(jp);

		Long idValue = null;
		String usernameValue = null;
		Date dataTimeValue = null;
		// String appNameValue = null;
		String appIdValue = null;
		String motivationValue = null;
		String variableNameValue = null;
		String variableValueValue = null;
		String variableUnitValue = null;
		// String uidNameValue = null; // transient

		if (node.get("id") != null)
			idValue = node.get("id").asLong();

		if (node.get("username") != null)
			usernameValue = node.get("username").asText();
		if (node.get("dataTime") != null)
			dataTimeValue = new Date(node.get("dataTime").asLong());

		// if (node.get("APPName") != null)
		// uidValue = node.get("APPName").asText();
		if (node.get("APPID") != null)
			appIdValue = node.get("APPID").asText();
		if (node.get("motivation") != null)
			motivationValue = node.get("motivation").asText();
		if (node.get("variableName") != null)
			variableNameValue = node.get("variableName").asText();
		if (node.get("variableValue") != null)
			variableValueValue = node.get("variableValue").asText();
		if (node.get("variableUnit") != null)
			variableUnitValue = node.get("variableUnit").asText();

		// if (node.get("uidName") != null) // transient
		// uidNameValue = node.get("uidName").asText();

		return new Data(idValue, usernameValue, dataTimeValue, null, null, null, null, appIdValue, motivationValue, variableNameValue, variableValueValue, variableUnitValue);
		// d.setUidName(uidNameValue); // transient

	}
}
