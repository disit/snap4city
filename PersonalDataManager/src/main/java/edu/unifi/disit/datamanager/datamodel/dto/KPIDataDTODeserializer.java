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

public class KPIDataDTODeserializer extends StdDeserializer<KPIDataDTO> {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger();

	public KPIDataDTODeserializer() {
		this(null);
	}

	public KPIDataDTODeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public KPIDataDTO deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		JsonNode jnode = jp.getCodec().readTree(jp);
		KPIDataDTO kpidataDTO = new KPIDataDTO();

		if (jnode.get("id") != null) {
			kpidataDTO.setId(jnode.get("id").asLong());
		}
		if (jnode.get("appId") != null) {
			kpidataDTO.setAppId(jnode.get("appId").asText());
		}
		if (jnode.get("appName") != null) {
			kpidataDTO.setAppName(jnode.get("appName").asText());
		}
		if (jnode.get("dbValuesLink") != null) {
			kpidataDTO.setDbValuesLink(jnode.get("dbValuesLink").asText());
		}
		if (jnode.get("dbValuesType") != null) {
			kpidataDTO.setDbValuesType(jnode.get("dbValuesType").asText());
		}
		if (jnode.get("description") != null) {
			kpidataDTO.setDescription(jnode.get("description").asText());
		}
		if (jnode.get("getInstances") != null) {
			kpidataDTO.setGetInstances(jnode.get("getInstances").asText());
		}
		if (jnode.get("healthiness") != null) {
			kpidataDTO.setHealthiness(jnode.get("healthiness").asText());
		}
		if (jnode.get("highLevelType") != null) {
			kpidataDTO.setHighLevelType(jnode.get("highLevelType").asText());
		}
		if (jnode.get("info") != null) {
			kpidataDTO.setInfo(jnode.get("info").asText());
		}
		if (jnode.get("instanceUri") != null) {
			kpidataDTO.setInstanceUri(jnode.get("instanceUri").asText());
		}
		if (jnode.get("kbBased") != null) {
			kpidataDTO.setKbBased(jnode.get("kbBased").asText());
		}
		if (jnode.get("valueName") != null) {
			kpidataDTO.setValueName(jnode.get("valueName").asText());
		}
		if (jnode.get("lastDate") != null) {
			Date date = new Date();
			if (jnode.get("lastDate").asLong() != 0) {
				date.setTime(jnode.get("lastDate").asLong());
				kpidataDTO.setLastDate(date);
			} else {
				try {
					kpidataDTO.setLastDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("lastDate").asText()));
				} catch (ParseException e) {
					try {
						kpidataDTO.setLastDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("lastDate").asText()));
					} catch (ParseException d) {
						logger.warn("Parsing error date {}", jnode.get("lastDate").asText());
					}
				}
			}
		}
		if (jnode.get("lastValue") != null) {
			kpidataDTO.setLastValue(jnode.get("lastValue").asText());
		}
		if (jnode.get("latitude") != null) {
			kpidataDTO.setLatitude(jnode.get("latitude").asText());
		}
		if (jnode.get("longitude") != null) {
			kpidataDTO.setLongitude(jnode.get("longitude").asText());
		}
		if (jnode.get("lastLatitude") != null) {
			kpidataDTO.setLastLatitude(jnode.get("lastLatitude").asText());
		}
		if (jnode.get("lastLongitude") != null) {
			kpidataDTO.setLastLongitude(jnode.get("lastLongitude").asText());
		}
		if (jnode.get("valueType") != null) {
			kpidataDTO.setValueType(jnode.get("valueType").asText());
		}
		if (jnode.get("valueUnit") != null) {
			kpidataDTO.setValueUnit(jnode.get("valueUnit").asText());
		}
		if (jnode.get("metric") != null) {
			kpidataDTO.setMetric(jnode.get("metric").asText());
		}
		if (jnode.get("microAppExtServIcon") != null) {
			kpidataDTO.setMicroAppExtServIcon(jnode.get("microAppExtServIcon").asText());
		}
		if (jnode.get("nature") != null) {
			kpidataDTO.setNature(jnode.get("nature").asText());
		}
		if (jnode.get("organizations") != null) {
			kpidataDTO.setOrganizations(jnode.get("organizations").asText());
		}
		if (jnode.get("ownership") != null) {
			kpidataDTO.setOwnership(jnode.get("ownership").asText());
		}
		if (jnode.get("parameters") != null) {
			kpidataDTO.setParameters(jnode.get("parameters").asText());
		}
		if (jnode.get("savedDirect") != null) {
			kpidataDTO.setSavedDirect(jnode.get("savedDirect").asText());
		}
		if (jnode.get("smBased") != null) {
			kpidataDTO.setSmBased(jnode.get("smBased").asText());
		}
		if (jnode.get("subNature") != null) {
			kpidataDTO.setSubNature(jnode.get("subNature").asText());
		}
		if (jnode.get("dataType") != null) {
			kpidataDTO.setDataType(jnode.get("dataType").asText());
		}
		if (jnode.get("username") != null) {
			kpidataDTO.setUsername(jnode.get("username").asText());
		}
		if (jnode.get("widgets") != null) {
			kpidataDTO.setWidgets(jnode.get("widgets").asText());
		}

		return kpidataDTO;
	}
}