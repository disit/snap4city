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
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class KPIDataDeserializer extends StdDeserializer<KPIData> {

	private static final long serialVersionUID = 1L;

	public KPIDataDeserializer() {
		this(null);
	}

	public KPIDataDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public KPIData deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode jnode = jp.getCodec().readTree(jp);
		KPIData kpidata = new KPIData();

		if (jnode.get("id") != null) {
			kpidata.setId(jnode.get("id").asLong());
		}
		// if (jnode.get("appId") != null) {
		// kpidata.setAppId(jnode.get("appId").asText());
		// }
		// if (jnode.get("appName") != null) {
		// kpidata.setAppName(jnode.get("appName").asText());
		// }
		// if (jnode.get("dbValuesLink") != null) {
		// kpidata.setDbValuesLink(jnode.get("dbValuesLink").asText());
		// }
		// if (jnode.get("dbValuesType") != null) {
		// kpidata.setDbValuesType(jnode.get("dbValuesType").asText());
		// }
		// if (jnode.get("description") != null) {
		// kpidata.setDescription(jnode.get("description").asText());
		// }
		// if (jnode.get("deleteTime") != null) {
		// try {
		// kpidata.setDeleteTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("deleteTime").asText()));
		// } catch (ParseException e) {
		// e.printStackTrace();
		// }
		// }
		// if (jnode.get("getInstances") != null) {
		// kpidata.setGetInstances(jnode.get("getInstances").asText());
		// }
		// if (jnode.get("healthiness") != null) {
		// kpidata.setHealthiness(jnode.get("healthiness").asText());
		// }
		// if (jnode.get("highLevelType") != null) {
		// kpidata.setHighLevelType(jnode.get("highLevelType").asText());
		// }
		// if (jnode.get("info") != null) {
		// kpidata.setInfo(jnode.get("info").asText());
		// }
		if (jnode.get("insertTime") != null) {
			kpidata.setInsertTime(new Date((jnode.get("insertTime").asLong())));
		}
		// if (jnode.get("instanceUri") != null) {
		// kpidata.setInstanceUri(jnode.get("instanceUri").asText());
		// }
		// if (jnode.get("kbBased") != null) {
		// kpidata.setKbBased(jnode.get("kbBased").asText());
		// }
		if (jnode.get("valueName") != null) {
			kpidata.setValueName(jnode.get("valueName").asText());
		}
		// if (jnode.get("lastCheck") != null) {
		// try {
		// kpidata.setLastCheck(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("lastCheck").asText()));
		// } catch (ParseException e) {
		// e.printStackTrace();
		// }
		// }
		if (jnode.get("lastDate") != null) {
			kpidata.setLastDate(new Date(Long.parseLong(jnode.get("lastDate").asText())));
		}
		// if (jnode.get("lastValue") != null) {
		// kpidata.setLastValue(jnode.get("lastValue").asText());
		// }
		if (jnode.get("latitude") != null) {
			kpidata.setLatitude((float) jnode.get("latitude").asDouble());
		}
		if (jnode.get("longitude") != null) {
			kpidata.setLongitude((float) jnode.get("longitude").asDouble());
		}
		// if (jnode.get("valueType") != null) {
		// kpidata.setValueType(jnode.get("valueType").asText());
		// }
		// if (jnode.get("metric") != null) {
		// kpidata.setMetric(jnode.get("metric").asText());
		// }
		// if (jnode.get("microAppExtServIcon") != null) {
		// kpidata.setMicroAppExtServIcon(jnode.get("microAppExtServIcon").asText());
		// }
		// if (jnode.get("nature") != null) {
		// kpidata.setNature(jnode.get("nature").asText());
		// }
		// if (jnode.get("organizations") != null) {
		// kpidata.setOrganizations(jnode.get("organizations").asText());
		// }
		// if (jnode.get("ownership") != null) {
		// kpidata.setOwnership(jnode.get("ownership").asText());
		// }
		// if (jnode.get("parameters") != null) {
		// kpidata.setParameters(jnode.get("parameters").asText());
		// }
		// if (jnode.get("savedDirect") != null) {
		// kpidata.setSavedDirect(jnode.get("savedDirect").asText());
		// }
		// if (jnode.get("smBased") != null) {
		// kpidata.setSmBased(jnode.get("smBased").asText());
		// }
		// if (jnode.get("subNature") != null) {
		// kpidata.setSubNature(jnode.get("subNature").asText());
		// }
		// if (jnode.get("dataType") != null) {
		// kpidata.setDataType(jnode.get("dataType").asText());
		// }
		if (jnode.get("username") != null) {
			kpidata.setUsername(jnode.get("username").asText());
		}
		// if (jnode.get("widgets") != null) {
		// kpidata.setWidgets(jnode.get("widgets").asText());
		// }

		return kpidata;
	}
}