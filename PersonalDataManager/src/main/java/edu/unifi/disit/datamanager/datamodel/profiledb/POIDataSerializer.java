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

public class POIDataSerializer extends StdSerializer<POIData> {

	private static final long serialVersionUID = 1L;

	public POIDataSerializer() {
		this(null);
	}

	public POIDataSerializer(Class<POIData> t) {
		super(t);
	}

	@Override
	public void serialize(POIData poiData, JsonGenerator jgen, SerializerProvider provider) throws IOException {

		if (poiData.getKpidata().getLongitude() != null && poiData.getKpidata().getLongitude() != ""
				&& poiData.getKpidata().getLatitude() != null && poiData.getKpidata().getLatitude() != "") {
			jgen.writeStartObject();

			jgen.writeStringField("type", "Feature");
			jgen.writeObjectFieldStart("geometry");
			jgen.writeStringField("type", "Point");
			jgen.writeArrayFieldStart("coordinates");
			jgen.writeNumber(poiData.getKpidata().getLongitude());
			jgen.writeNumber(poiData.getKpidata().getLatitude());
			jgen.writeEndArray();
			jgen.writeEndObject();
			jgen.writeObjectFieldStart("properties");
			jgen.writeObjectFieldStart("kpidata");

			if (poiData.getKpidata().getId() != null) {
				jgen.writeNumberField("id", poiData.getKpidata().getId());
			}
			if (poiData.getKpidata().getAppId() != null) {
				jgen.writeStringField("appId", poiData.getKpidata().getAppId());
			}
			if (poiData.getKpidata().getAppName() != null) {
				jgen.writeStringField("appName", poiData.getKpidata().getAppName());
			}
			if (poiData.getKpidata().getDbValuesLink() != null) {
				jgen.writeStringField("dbValuesLink", poiData.getKpidata().getDbValuesLink());
			}
			if (poiData.getKpidata().getDbValuesType() != null) {
				jgen.writeStringField("dbValuesType", poiData.getKpidata().getDbValuesType());
			}
			if (poiData.getKpidata().getDescription() != null) {
				jgen.writeStringField("description", poiData.getKpidata().getDescription());
			}
			if (poiData.getKpidata().getDeleteTime() != null) {
				jgen.writeNumberField("deleteTime", poiData.getKpidata().getDeleteTime().getTime());
			}
			if (poiData.getKpidata().getGetInstances() != null) {
				jgen.writeStringField("getInstances", poiData.getKpidata().getGetInstances());
			}
			if (poiData.getKpidata().getHealthiness() != null) {
				jgen.writeStringField("healthiness", poiData.getKpidata().getHealthiness());
			}
			if (poiData.getKpidata().getHighLevelType() != null) {
				jgen.writeStringField("highLevelType", poiData.getKpidata().getHighLevelType());
			}
			if (poiData.getKpidata().getInfo() != null) {
				jgen.writeStringField("info", poiData.getKpidata().getInfo());
			}
			if (poiData.getKpidata().getInsertTime() != null) {
				jgen.writeNumberField("insertTime", poiData.getKpidata().getInsertTime().getTime());
			}
			if (poiData.getKpidata().getInstanceUri() != null) {
				jgen.writeStringField("instanceUri", poiData.getKpidata().getInstanceUri());
			}
			if (poiData.getKpidata().getKbBased() != null) {
				jgen.writeStringField("kbBased", poiData.getKpidata().getKbBased());
			}
			if (poiData.getKpidata().getValueName() != null) {
				jgen.writeStringField("valueName", poiData.getKpidata().getValueName());
			}
			if (poiData.getKpidata().getLastCheck() != null) {
				jgen.writeNumberField("lastCheck", poiData.getKpidata().getLastCheck().getTime());
			}
			if (poiData.getKpidata().getLastDate() != null) {
				jgen.writeNumberField("lastDate", poiData.getKpidata().getLastDate().getTime());
			}
			if (poiData.getKpidata().getLastValue() != null) {
				jgen.writeStringField("lastValue", poiData.getKpidata().getLastValue());
			}
			if (poiData.getKpidata().getValueType() != null) {
				jgen.writeStringField("valueType", poiData.getKpidata().getValueType());
			}
			if (poiData.getKpidata().getMetric() != null) {
				jgen.writeStringField("metric", poiData.getKpidata().getMetric());
			}
			if (poiData.getKpidata().getMicroAppExtServIcon() != null) {
				jgen.writeStringField("microAppExtServIcon", poiData.getKpidata().getMicroAppExtServIcon());
			}
			if (poiData.getKpidata().getNature() != null) {
				jgen.writeStringField("nature", poiData.getKpidata().getNature());
			}
			if (poiData.getKpidata().getOrganizations() != null) {
				jgen.writeStringField("organizations", poiData.getKpidata().getOrganizations());
			}
			if (poiData.getKpidata().getOwnership() != null) {
				jgen.writeStringField("ownership", poiData.getKpidata().getOwnership());
			}
			if (poiData.getKpidata().getParameters() != null) {
				jgen.writeStringField("parameters", poiData.getKpidata().getParameters());
			}
			if (poiData.getKpidata().getSavedDirect() != null) {
				jgen.writeStringField("savedDirect", poiData.getKpidata().getSavedDirect());
			}
			if (poiData.getKpidata().getSmBased() != null) {
				jgen.writeStringField("smBased", poiData.getKpidata().getSmBased());
			}
			if (poiData.getKpidata().getSubNature() != null) {
				jgen.writeStringField("subNature", poiData.getKpidata().getSubNature());
			}
			if (poiData.getKpidata().getDataType() != null) {
				jgen.writeStringField("dataType", poiData.getKpidata().getDataType());
			}
			if (poiData.getKpidata().getUsername() != null) {
				jgen.writeStringField("username", poiData.getKpidata().getUsername());
			}
			if (poiData.getKpidata().getWidgets() != null) {
				jgen.writeStringField("widgets", poiData.getKpidata().getWidgets());
			}
			if (poiData.getKpidata().getLongitude() != null) {
				jgen.writeNumberField("longitude", Double.parseDouble(poiData.getKpidata().getLongitude()));
			}
			if (poiData.getKpidata().getLatitude() != null) {
				jgen.writeNumberField("latitude", Double.parseDouble(poiData.getKpidata().getLatitude()));
			}

			jgen.writeEndObject();

			if (poiData.getKpidata().getSubNature() != null && poiData.getKpidata().getNature() != null) {
				jgen.writeStringField("serviceType",
						poiData.getKpidata().getNature() + "_" + poiData.getKpidata().getSubNature());
			}

			if (poiData.getKpidata().getValueName() != null) {
				jgen.writeStringField("name", poiData.getKpidata().getValueName());
			}
			
			if (poiData.getKpidata().getDescription() != null) {
				jgen.writeStringField("description", poiData.getKpidata().getDescription());
			}

			for (KPIMetadata kpiMetadata : poiData.getListKPIMetadata()) {
				jgen.writeStringField(kpiMetadata.getKey(), kpiMetadata.getValue());
			}
			if (!poiData.getListKPIValue().isEmpty()) {
				jgen.writeArrayFieldStart("values");
				for (KPIValue kpiValue : poiData.getListKPIValue()) {
					jgen.writeStartObject();
					jgen.writeStringField("value", kpiValue.getValue());
					jgen.writeNumberField("dataTime", kpiValue.getDataTime().getTime());
					jgen.writeEndObject();
				}
				jgen.writeEndArray();
			}

			jgen.writeEndObject();// Chiude le proprietà
			jgen.writeEndObject();// Chiude il json

		}
	}
}