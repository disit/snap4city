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

public class DelegationDeserializer extends StdDeserializer<Delegation> {

	private static final long serialVersionUID = 1L;

	public DelegationDeserializer() {
		this(null);
	}

	public DelegationDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public Delegation deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		JsonNode node = jp.getCodec().readTree(jp);

		Long idValue = null;
		String usernameDelegatorValue = null;
		String usernameDelegatedValue = null;
		String variableNameValue = null;
		String motivationValue = null;
		String elementIdValue = null;
		String elementTypeValue = null;
		Date insertTimeValue = null;
		Date deleteTimeValue = null;
		String delegationDetailsValue = null;
		String groupnameDelegatedValue = null;

		if (node.get("id") != null)
			idValue = node.get("id").asLong();
		if (node.get("usernameDelegator") != null)
			usernameDelegatorValue = node.get("usernameDelegator").asText();
		if (node.get("usernameDelegated") != null)
			usernameDelegatedValue = node.get("usernameDelegated").asText();
		if (node.get("variableName") != null)
			variableNameValue = node.get("variableName").asText();
		if (node.get("motivation") != null)
			motivationValue = node.get("motivation").asText();
		if (node.get("elementId") != null)
			elementIdValue = node.get("elementId").asText();
		if (node.get("elementType") != null)
			elementTypeValue = node.get("elementType").asText();
		if (node.get("insertTime") != null)
			insertTimeValue = new Date(node.get("insertTime").asLong());
		if (node.get("deleteTime") != null)
			deleteTimeValue = new Date(node.get("deleteTime").asLong());
		if (node.get("delegationDetails") != null)
			delegationDetailsValue = node.get("delegationDetails").toString();
		if (node.get("groupnameDelegated") != null)
			groupnameDelegatedValue = node.get("groupnameDelegated").asText();

		return new Delegation(idValue, usernameDelegatorValue, usernameDelegatedValue, variableNameValue, motivationValue, elementIdValue, elementTypeValue, insertTimeValue, deleteTimeValue, delegationDetailsValue,
				groupnameDelegatedValue);

	}
}
