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
package edu.unifi.disit.snapengager.datamodel;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class PoiDeserializer extends StdDeserializer<Poi> {

	private static final long serialVersionUID = 1L;

	public PoiDeserializer() {
		this(null);
	}

	public PoiDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public Poi deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode feature = jp.getCodec().readTree(jp);
		Poi poi = new Poi();

		JsonNode geometries = feature.path("geometry");
		Iterator<JsonNode> GPS_element = geometries.get("coordinates").elements();
		int gps_index = 0;
		while (GPS_element.hasNext()) {
			JsonNode GPS = GPS_element.next();
			if (gps_index == 0) {
				poi.setLongitude(Double.toString(GPS.doubleValue()));
			} else {
				poi.setLatitude(Double.toString(GPS.doubleValue()));
			}
			gps_index++;
		}

		JsonNode properties = feature.path("properties");

		poi.setName(properties.get("name").textValue());
		poi.setTipo(properties.get("tipo").textValue());
		poi.setTypeLabel(properties.get("typeLabel").textValue());
		poi.setServiceType(properties.get("serviceType").textValue());
		poi.setServiceUri(properties.get("serviceUri").textValue());

		return poi;
	}
}