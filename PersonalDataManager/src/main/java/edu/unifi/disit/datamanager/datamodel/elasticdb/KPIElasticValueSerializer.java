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
package edu.unifi.disit.datamanager.datamodel.elasticdb;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class KPIElasticValueSerializer extends StdSerializer<KPIElasticValue> {

	private static final long serialVersionUID = 1L;

	public KPIElasticValueSerializer() {
		this(null);
	}

	public KPIElasticValueSerializer(Class<KPIElasticValue> t) {
		super(t);
	}

	@Override
	public void serialize(KPIElasticValue kpiElasticValues, JsonGenerator jgen, SerializerProvider provider)
			throws IOException {

		jgen.writeStartObject();

		if (kpiElasticValues.getId() != null) {
			jgen.writeStringField("id", kpiElasticValues.getId());
		}
		if (kpiElasticValues.getValueStr() != null) {
			jgen.writeStringField("value_str", kpiElasticValues.getValueStr());
		}
		if (kpiElasticValues.getValueType() != null) {
			jgen.writeStringField("value_type", kpiElasticValues.getValueType());
		}
		if (kpiElasticValues.getServiceUri() != null) {
			jgen.writeStringField("serviceUri", kpiElasticValues.getServiceUri());
		}
		if (kpiElasticValues.getSrc() != null) {
			jgen.writeStringField("src", kpiElasticValues.getSrc());
		}
		if (kpiElasticValues.getKind() != null) {
			jgen.writeStringField("kind", kpiElasticValues.getKind());
		}
		if (kpiElasticValues.getDeviceName() != null) {
			jgen.writeStringField("deviceName", kpiElasticValues.getDeviceName());
		}
		if (kpiElasticValues.getHealthinessCriteria() != null) {
			jgen.writeStringField("healthiness_criteria", kpiElasticValues.getHealthinessCriteria());
		}
		if (kpiElasticValues.getSensorId() != null) {
			jgen.writeStringField("kpiId", kpiElasticValues.getSensorId());
			jgen.writeStringField("sensorID", kpiElasticValues.getSensorId());
		}
		if (kpiElasticValues.getDateTime() != null) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
			jgen.writeStringField("date_time", dateFormat.format(kpiElasticValues.getDateTime()));
			jgen.writeNumberField("dataTime", kpiElasticValues.getDateTime().getTime());
		}
		if (kpiElasticValues.getLatlon() != null) {
			jgen.writeStringField("latitude",
					kpiElasticValues.getLatlon().substring(0, kpiElasticValues.getLatlon().indexOf(',')));
			jgen.writeStringField("longitude",
					kpiElasticValues.getLatlon().substring(kpiElasticValues.getLatlon().indexOf(',') + 1));
			jgen.writeStringField("latlon", kpiElasticValues.getLatlon());
		}
		if (kpiElasticValues.getDataType() != null) {
			jgen.writeStringField("data_type", kpiElasticValues.getDataType());
		}
		if (kpiElasticValues.getValueRefreshRate() != null) {
			jgen.writeStringField("value_refresh_rate", kpiElasticValues.getValueRefreshRate());
		}
		if (kpiElasticValues.getValueUnit() != null) {
			jgen.writeStringField("value_unit", kpiElasticValues.getValueUnit());
		}
		if (kpiElasticValues.getValue() != null) {
			jgen.writeStringField("value", kpiElasticValues.getValue().toString());
		}
		if (kpiElasticValues.getValueName() != null) {
			jgen.writeStringField("value_name", kpiElasticValues.getValueName());
		}
		if (kpiElasticValues.getUsername() != null) {
			jgen.writeStringField("username", kpiElasticValues.getUsername());
		}
		if (kpiElasticValues.getNature() != null) {
			jgen.writeStringField("nature", kpiElasticValues.getNature());
		}
		if (kpiElasticValues.getSubNature() != null) {
			jgen.writeStringField("subnature", kpiElasticValues.getSubNature());
		}
		if (kpiElasticValues.getOrganization() != null) {
			jgen.writeStringField("organization", kpiElasticValues.getOrganization());
		}
		if (kpiElasticValues.getGroups() != null && !kpiElasticValues.getGroups().isEmpty()) {
			jgen.writeArrayFieldStart("groups");
			for (String dge : kpiElasticValues.getGroups()) {
				jgen.writeString(dge);
			}
			jgen.writeEndArray();
		}

		if (kpiElasticValues.getUserDelegations() != null && !kpiElasticValues.getUserDelegations().isEmpty()) {
			jgen.writeArrayFieldStart("user_delegations");
			for (String delegation : kpiElasticValues.getUserDelegations()) {
				jgen.writeString(delegation);
			}

			jgen.writeEndArray();
		}

		if (kpiElasticValues.getOrganizationDelegations() != null
				&& !kpiElasticValues.getOrganizationDelegations().isEmpty()) {
			jgen.writeArrayFieldStart("organization_delegations");
			for (String delegation : kpiElasticValues.getOrganizationDelegations()) {
				jgen.writeString(delegation);
			}

			jgen.writeEndArray();
		}

		jgen.writeStringField("dbType", "ES");

		jgen.writeEndObject();
	}
}