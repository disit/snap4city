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

public class KPIElasticValueDTODeserializer extends StdDeserializer<KPIElasticValueDTO> {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger();

	public KPIElasticValueDTODeserializer() {
		this(null);
	}

	public KPIElasticValueDTODeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public KPIElasticValueDTO deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
		JsonNode jnode = jp.getCodec().readTree(jp);
		KPIElasticValueDTO kpielasticvalueDTO = new KPIElasticValueDTO();

		if (jnode.get("id") != null) {
			kpielasticvalueDTO.setId(jnode.get("id").textValue());
		}
		if (jnode.get("kpiId") != null) {
			kpielasticvalueDTO.setSensorId(jnode.get("kpiId").textValue());
		}
		if (jnode.get("sensorID") != null) {
			kpielasticvalueDTO.setSensorId(jnode.get("sensorID").textValue());
		}
		if (jnode.get("dataTime") != null) {
			Date date = new Date();
			if (jnode.get("dataTime").asLong() != 0) {
				date.setTime(jnode.get("dataTime").asLong());
				kpielasticvalueDTO.setDataTime(date);
				kpielasticvalueDTO.setDateTime(date);
			} else {
				try {
					kpielasticvalueDTO.setDataTime(
							new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("dataTime").asText()));
					kpielasticvalueDTO.setDateTime(
							new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("dataTime").asText()));
				} catch (ParseException e) {
					try {
						kpielasticvalueDTO.setDataTime(
								new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("dataTime").asText()));
						kpielasticvalueDTO.setDateTime(
								new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("dataTime").asText()));
					} catch (ParseException d) {
						logger.error("Parsing error", d);
					}
				}
			}
		}
		if (jnode.get("date_time") != null) {
			Date date = new Date();

			if (jnode.get("date_time").asLong() != 0) {
				date.setTime(jnode.get("date_time").asLong());
				kpielasticvalueDTO.setDataTime(date);
				kpielasticvalueDTO.setDateTime(date);
			} else {
				try {
					kpielasticvalueDTO.setDataTime(
							new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("date_time").asText()));
					kpielasticvalueDTO.setDateTime(
							new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("date_time").asText()));
				} catch (ParseException e) {
					try {
						kpielasticvalueDTO.setDataTime(
								new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("date_time").asText()));
						kpielasticvalueDTO.setDateTime(
								new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("date_time").asText()));
					} catch (ParseException d) {
						logger.error("Parsing error", d);
					}
				}
			}
		}
		if (jnode.get("value") != null) {
			try {
				kpielasticvalueDTO.setValue(Float.valueOf(jnode.get("value").asText()));
			} catch (NumberFormatException e) {
				kpielasticvalueDTO.setValueStr(jnode.get("value").asText());
			}
		}
		if (jnode.get("value_str") != null) {
			kpielasticvalueDTO.setValueStr(jnode.get("value_str").asText());
		}
		if (jnode.get("latitude") != null && !jnode.get("latitude").asText().equals("")
				&& jnode.get("longitude") != null && !jnode.get("longitude").asText().equals("")) {
			kpielasticvalueDTO.setLatitude(jnode.get("latitude").asText());
			kpielasticvalueDTO.setLongitude(jnode.get("longitude").asText());
			kpielasticvalueDTO.setLatlon(jnode.get("latitude").asText() + "," + jnode.get("longitude").asText());
		}
		if (jnode.get("latlon") != null) {
			kpielasticvalueDTO
					.setLatitude(jnode.get("latlon").asText().substring(0, jnode.get("latlon").asText().indexOf(',')));
			kpielasticvalueDTO.setLongitude(
					jnode.get("latlon").asText().substring(jnode.get("latlon").asText().indexOf(',') + 1));
			kpielasticvalueDTO.setLatlon(jnode.get("latlon").asText());
		}
		if (jnode.get("value_type") != null) {
			kpielasticvalueDTO.setValueType(jnode.get("value_type").asText());
		}
		if (jnode.get("serviceUri") != null) {
			kpielasticvalueDTO.setServiceUri(jnode.get("serviceUri").asText());
		}
		if (jnode.get("src") != null) {
			kpielasticvalueDTO.setSrc(jnode.get("src").asText());
		}
		if (jnode.get("kind") != null) {
			kpielasticvalueDTO.setKind(jnode.get("kind").asText());
		}
		if (jnode.get("deviceName") != null) {
			kpielasticvalueDTO.setDeviceName(jnode.get("deviceName").asText());
		}
		if (jnode.get("healthiness_criteria") != null) {
			kpielasticvalueDTO.setHealthinessCriteria(jnode.get("healthiness_criteria").asText());
		}
		if (jnode.get("data_type") != null) {
			kpielasticvalueDTO.setDataType(jnode.get("data_type").asText());
		}
		if (jnode.get("value_refresh_rate") != null) {
			kpielasticvalueDTO.setValueRefreshRate(jnode.get("value_refresh_rate").asText());
		}
		if (jnode.get("value_unit") != null) {
			kpielasticvalueDTO.setValueUnit(jnode.get("value_unit").asText());
		}
		if (jnode.get("value_name") != null) {
			kpielasticvalueDTO.setValueName(jnode.get("value_name").asText());
		}
		if (jnode.get("username") != null) {
			kpielasticvalueDTO.setUsername(jnode.get("username").asText());
		}
		if (jnode.get("subnature") != null) {
			kpielasticvalueDTO.setSubNature(jnode.get("subnature").asText());
		}
		if (jnode.get("nature") != null) {
			kpielasticvalueDTO.setNature(jnode.get("nature").asText());
		}
		if (jnode.get("organization") != null) {
			kpielasticvalueDTO.setOrganization(jnode.get("organization").asText());
		}

		return kpielasticvalueDTO;
	}
}