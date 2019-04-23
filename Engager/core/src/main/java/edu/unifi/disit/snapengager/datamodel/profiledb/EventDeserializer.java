/* Snap4City Engager (SE)
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
package edu.unifi.disit.snapengager.datamodel.profiledb;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class EventDeserializer extends StdDeserializer<Event> {

	private static final long serialVersionUID = 1L;

	public EventDeserializer() {
		this(null);
	}

	public EventDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public Event deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode feature = jp.getCodec().readTree(jp);
		Event event = new Event();

		JsonNode geometries = feature.path("geometry");
		Iterator<JsonNode> GPS_element = geometries.get("coordinates").elements();
		int gps_index = 0;
		while (GPS_element.hasNext()) {
			JsonNode GPS = GPS_element.next();
			if (gps_index == 0) {
				event.setLongitude(Double.toString(GPS.doubleValue()));
			} else {
				event.setLatitude(Double.toString(GPS.doubleValue()));
			}
			gps_index++;
		}

		JsonNode properties = feature.path("properties");

		event.setName(properties.get("name").textValue());
		event.setServiceUri(properties.get("serviceUri").textValue());
		if (properties.get("categoryIT") != null)
			event.setTypeLabel(properties.get("categoryIT").textValue()); // beware, this is specific for Italian
		event.setServiceType(properties.get("serviceType").textValue());
		event.setPlace(properties.get("place").textValue());
		event.setEndDate(properties.get("endDate").textValue());
		event.setStartDate(properties.get("startDate").textValue());

		if (!properties.get("price").isNull())
			event.setPrice(properties.get("price").floatValue());

		return event;
	}
}