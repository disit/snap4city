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
package edu.unifi.disit.datamanager.datamodel.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class KPIMetadataDTODeserializer extends StdDeserializer<KPIMetadataDTO> {

	private static final long serialVersionUID = 1L;

	public KPIMetadataDTODeserializer() {
		this(null);
	}

	public KPIMetadataDTODeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public KPIMetadataDTO deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		JsonNode jnode = jp.getCodec().readTree(jp);
		KPIMetadataDTO kpimetadataDTO = new KPIMetadataDTO();

		
		if(jnode.get("id") != null) {
			kpimetadataDTO.setId(jnode.get("id").asLong());
		}
		if(jnode.get("kpiId") != null) {
			kpimetadataDTO.setKpiId(jnode.get("kpiId").asLong());
		}
		if (jnode.get("key") != null) {
			kpimetadataDTO.setKey(jnode.get("key").asText());
		}
		if (jnode.get("value") != null) {
			kpimetadataDTO.setValue(jnode.get("value").asText());
		}

		return kpimetadataDTO;
	}
}