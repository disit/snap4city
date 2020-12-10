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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class KPIValueDTODeserializer extends StdDeserializer<KPIValueDTO> {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger();
	
	public KPIValueDTODeserializer() {
		this(null);
	}

	public KPIValueDTODeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public KPIValueDTO deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		JsonNode jnode = jp.getCodec().readTree(jp);
		KPIValueDTO kpivalueDTO = new KPIValueDTO();

		if (jnode.get("id") != null) {
			if (jnode.get("id").asLong() != 0) {
				kpivalueDTO.setId(jnode.get("id").asLong());
			}
		}
		if (jnode.get("kpiId") != null) {
			kpivalueDTO.setKpiId(jnode.get("kpiId").asLong());
		}
		if (jnode.get("dataTime") != null) {
			Date date = new Date();
			if (jnode.get("dataTime").asLong() != 0) {
				date.setTime(jnode.get("dataTime").asLong());
				kpivalueDTO.setDataTime(date);
			} else {
				try {
					kpivalueDTO.setDataTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("dataTime").asText()));
				} catch (ParseException e) {
					try {
						kpivalueDTO.setDataTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("dataTime").asText()));
					} catch (ParseException d) {
						logger.warn("Parsing error date {}", jnode.get("dataTime").asText());
					}
				}
			}
		}
		if (jnode.get("insertTime") != null) {
			Date date = new Date();
			if (jnode.get("insertTime").asLong() != 0) {
				date.setTime(jnode.get("insertTime").asLong());
				kpivalueDTO.setInsertTime(date);
			} else {
				try {
					kpivalueDTO.setInsertTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("insertTime").asText()));
				} catch (ParseException e) {
					try {
						kpivalueDTO.setInsertTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("insertTime").asText()));
					} catch (ParseException d) {
						logger.warn("Parsing error date {}", jnode.get("insertTime").asText());
					}
				}
			}
		}
		if (jnode.get("value_str") != null) {
			kpivalueDTO.setValue(jnode.get("value_str").asText());
		}
		if (jnode.get("value") != null) {
			kpivalueDTO.setValue(jnode.get("value").asText());
		}
		if (jnode.get("latitude") != null) {
			kpivalueDTO.setLatitude(jnode.get("latitude").asText());
		}
		if (jnode.get("longitude") != null) {
			kpivalueDTO.setLongitude(jnode.get("longitude").asText());
		}

		return kpivalueDTO;
	}
}