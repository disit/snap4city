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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class KPIDataSerializer extends StdSerializer<KPIData> {

	private static final long serialVersionUID = 1L;

	public KPIDataSerializer() {
		this(null);
	}

	public KPIDataSerializer(Class<KPIData> t) {
		super(t);
	}

	@Override
	public void serialize(KPIData kpiData, JsonGenerator jgen, SerializerProvider provider) throws IOException {

		jgen.writeStartObject();

		if (kpiData.getId() != null) {
			jgen.writeNumberField("id", kpiData.getId());
		}
		if (kpiData.getAppId() != null) {
			jgen.writeStringField("appId", kpiData.getAppId());
		}
		if (kpiData.getAppName() != null) {
			jgen.writeStringField("appName", kpiData.getAppName());
		}
		if (kpiData.getDbValuesLink() != null) {
			jgen.writeStringField("dbValuesLink", kpiData.getDbValuesLink());
		}
		if (kpiData.getDbValuesType() != null) {
			jgen.writeStringField("dbValuesType", kpiData.getDbValuesType());
		}
		if (kpiData.getDescription() != null) {
			jgen.writeStringField("description", kpiData.getDescription());
		}
		if (kpiData.getDeleteTime() != null) {
			jgen.writeNumberField("deleteTime", kpiData.getDeleteTime().getTime());
		}
		if (kpiData.getGetInstances() != null) {
			jgen.writeStringField("getInstances", kpiData.getGetInstances());
		}
		if (kpiData.getHealthiness() != null) {
			jgen.writeStringField("healthiness", kpiData.getHealthiness());
		}
		if (kpiData.getHighLevelType() != null) {
			jgen.writeStringField("highLevelType", kpiData.getHighLevelType());
		}
		if (kpiData.getInfo() != null) {
			jgen.writeStringField("info", kpiData.getInfo());
		}
		if (kpiData.getInsertTime() != null) {
			jgen.writeNumberField("insertTime", kpiData.getInsertTime().getTime());
		}
		if (kpiData.getInstanceUri() != null) {
			jgen.writeStringField("instanceUri", kpiData.getInstanceUri());
		}
		if (kpiData.getKbBased() != null) {
			jgen.writeStringField("kbBased", kpiData.getKbBased());
		}
		if (kpiData.getValueName() != null) {
			jgen.writeStringField("valueName", kpiData.getValueName());
		}
		if (kpiData.getLastCheck() != null) {
			jgen.writeNumberField("lastCheck", kpiData.getLastCheck().getTime());
		}
		if (kpiData.getLastDate() != null) {
			jgen.writeNumberField("lastDate", kpiData.getLastDate().getTime());
		}
		if (kpiData.getLastValue() != null) {
			jgen.writeStringField("lastValue", kpiData.getLastValue());
		}
		if (kpiData.getLatitude() != null) {
			jgen.writeStringField("latitude", kpiData.getLatitude());
		}
		if (kpiData.getLongitude() != null) {
			jgen.writeStringField("longitude", kpiData.getLongitude());
		}
		if (kpiData.getLastLatitude() != null) {
			jgen.writeStringField("lastLatitude", kpiData.getLastLatitude());
		}
		if (kpiData.getLastLongitude() != null) {
			jgen.writeStringField("lastLongitude", kpiData.getLastLongitude());
		}
		if (kpiData.getValueType() != null) {
			jgen.writeStringField("valueType", kpiData.getValueType());
		}
		if (kpiData.getMetric() != null) {
			jgen.writeStringField("metric", kpiData.getMetric());
		}
		if (kpiData.getMicroAppExtServIcon() != null) {
			jgen.writeStringField("microAppExtServIcon", kpiData.getMicroAppExtServIcon());
		}
		if (kpiData.getNature() != null) {
			jgen.writeStringField("nature", kpiData.getNature());
		}
		if (kpiData.getOrganizations() != null) {
			jgen.writeStringField("organizations", kpiData.getOrganizations());
		}
		if (kpiData.getOwnership() != null) {
			jgen.writeStringField("ownership", kpiData.getOwnership());
		}
		if (kpiData.getParameters() != null) {
			jgen.writeStringField("parameters", kpiData.getParameters());
		}
		if (kpiData.getSavedDirect() != null) {
			jgen.writeStringField("savedDirect", kpiData.getSavedDirect());
		}
		if (kpiData.getSmBased() != null) {
			jgen.writeStringField("smBased", kpiData.getSmBased());
		}
		if (kpiData.getSubNature() != null) {
			jgen.writeStringField("subNature", kpiData.getSubNature());
		}
		if (kpiData.getDataType() != null) {
			jgen.writeStringField("dataType", kpiData.getDataType());
		}
		if (kpiData.getUsername() != null) {
			jgen.writeStringField("username", kpiData.getUsername());
		}

		if (kpiData.getWidgets() != null) {
			jgen.writeStringField("widgets", kpiData.getWidgets());
		}
		//
		// if (value.getUidName() != null) // transient
		// jgen.writeStringField("uidName", value.getUidName());

		jgen.writeEndObject();
	}
}