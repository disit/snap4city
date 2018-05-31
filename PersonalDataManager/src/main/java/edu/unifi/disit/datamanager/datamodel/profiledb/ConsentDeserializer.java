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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ConsentDeserializer extends StdDeserializer<Consent> {

	private static final long serialVersionUID = 1L;

	public ConsentDeserializer() {
		this(null);
	}

	public ConsentDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public Consent deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = jp.getCodec().readTree(jp);

		Long idValue = null;
		String uidValue = null;
		String appIdValue = null;
		Boolean valueValue = null;

		if (node.get("id") != null)
			idValue = node.get("id").asLong();

		if (node.get("uid") != null)
			uidValue = node.get("uid").asText();

		if (node.get("APPID") != null)
			appIdValue = node.get("APPID").asText();

		if (node.get("value") != null)
			valueValue = node.get("value").asBoolean();

		return new Consent(idValue, uidValue, appIdValue, valueValue);
	}
}