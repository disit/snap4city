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
import java.util.Date;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class POIDataDeserializer extends StdDeserializer<POIData> {

	private static final long serialVersionUID = 1L;

	public POIDataDeserializer() {
		this(null);
	}

	public POIDataDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public POIData deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		JsonNode jnode = jp.getCodec().readTree(jp);
		POIData poidata = new POIData();
		
		if(jnode.get("id") != null) {
			poidata.getKpidata().setId(jnode.get("id").asLong());
		}
		if (jnode.get("appId") != null) {
			poidata.getKpidata().setAppId(jnode.get("appId").asText());
		}
		if (jnode.get("appName") != null) {
			poidata.getKpidata().setAppName(jnode.get("appName").asText());
		}
		if (jnode.get("dbValuesLink") != null) {
			poidata.getKpidata().setDbValuesLink(jnode.get("dbValuesLink").asText());
		}
		if (jnode.get("dbValuesType") != null) {
			poidata.getKpidata().setDbValuesType(jnode.get("dbValuesType").asText());
		}
		if (jnode.get("description") != null) {
			poidata.getKpidata().setDescription(jnode.get("description").asText());
		}
		if (jnode.get("getInstances") != null) {
			poidata.getKpidata().setGetInstances(jnode.get("getInstances").asText());
		}
		if (jnode.get("healthiness") != null) {
			poidata.getKpidata().setHealthiness(jnode.get("healthiness").asText());
		}
		if (jnode.get("highLevelType") != null) {
			poidata.getKpidata().setHighLevelType(jnode.get("highLevelType").asText());
		}
		if (jnode.get("info") != null) {
			poidata.getKpidata().setInfo(jnode.get("info").asText());
		}
		if (jnode.get("instanceUri") != null) {
			poidata.getKpidata().setInstanceUri(jnode.get("instanceUri").asText());
		}
		if (jnode.get("kbBased") != null) {
			poidata.getKpidata().setKbBased(jnode.get("kbBased").asText());
		}
		if (jnode.get("valueName") != null) {
			poidata.getKpidata().setValueName(jnode.get("valueName").asText());
		}
		if (jnode.get("lastDate") != null) {
			Date date = new Date();
			date.setTime(jnode.get("lastDate").asLong());
			poidata.getKpidata().setLastDate(date);
		}
		if (jnode.get("lastValue") != null) {
			poidata.getKpidata().setLastValue(jnode.get("lastValue").asText());
		}
		if (jnode.get("latitude") != null) {
			poidata.getKpidata().setLatitude(jnode.get("latitude").asText());
		}
		if (jnode.get("longitude") != null) {
			poidata.getKpidata().setLongitude(jnode.get("longitude").asText());
		}
		if (jnode.get("valueType") != null) {
			poidata.getKpidata().setValueType(jnode.get("valueType").asText());
		}
		if (jnode.get("metric") != null) {
			poidata.getKpidata().setMetric(jnode.get("metric").asText());
		}
		if (jnode.get("microAppExtServIcon") != null) {
			poidata.getKpidata().setMicroAppExtServIcon(jnode.get("microAppExtServIcon").asText());
		}
		if (jnode.get("nature") != null) {
			poidata.getKpidata().setNature(jnode.get("nature").asText());
		}
		if (jnode.get("organizations") != null) {
			poidata.getKpidata().setOrganizations(jnode.get("organizations").asText());
		}
		if (jnode.get("ownership") != null) {
			poidata.getKpidata().setOwnership(jnode.get("ownership").asText());
		}
		if (jnode.get("parameters") != null) {
			poidata.getKpidata().setParameters(jnode.get("parameters").asText());
		}
		if (jnode.get("savedDirect") != null) {
			poidata.getKpidata().setSavedDirect(jnode.get("savedDirect").asText());
		}
		if (jnode.get("smBased") != null) {
			poidata.getKpidata().setSmBased(jnode.get("smBased").asText());
		}
		if (jnode.get("subNature") != null) {
			poidata.getKpidata().setSubNature(jnode.get("subNature").asText());
		}
		if (jnode.get("dataType") != null) {
			poidata.getKpidata().setDataType(jnode.get("dataType").asText());
		}
		if (jnode.get("username") != null) {
			poidata.getKpidata().setUsername(jnode.get("username").asText());
		}
		if (jnode.get("widgets") != null) {
			poidata.getKpidata().setWidgets(jnode.get("widgets").asText());
		}
		if (jnode.get("metadata") != null) {
			Iterator<JsonNode> iterator = jnode.get("metadata").elements();
			while (iterator.hasNext()) {
				JsonNode singleMetadata = iterator.next();
				poidata.getListKPIMetadata().add(new KPIMetadata(singleMetadata.get("key").asText(), singleMetadata.get("value").asText(), null, null));
			}
		}
		

		return poidata;
	}
}