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

public class KPIActivitySerializer extends StdSerializer<KPIActivity> {

	private static final long serialVersionUID = 1L;

	public KPIActivitySerializer() {
		this(null);
	}

	public KPIActivitySerializer(Class<KPIActivity> t) {
		super(t);
	}

	@Override
	public void serialize(KPIActivity kpiActivity, JsonGenerator jgen, SerializerProvider provider) throws IOException {

		jgen.writeStartObject();
		
		if (kpiActivity.getId() != null) {
			jgen.writeNumberField("id", kpiActivity.getId());
		}
		if (kpiActivity.getKpiId() != null) {
			jgen.writeNumberField("kpiId", kpiActivity.getKpiId());
		}
		if (kpiActivity.getSourceRequest() != null) {
			jgen.writeStringField("sourceRequest", kpiActivity.getSourceRequest());
		}
		if (kpiActivity.getSourceId() != null) {
			jgen.writeStringField("sourceId", kpiActivity.getSourceId());
		}
		if (kpiActivity.getAccessType() != null) {
			jgen.writeStringField("value", kpiActivity.getAccessType());
		}
		if (kpiActivity.getDomain() != null) {
			jgen.writeStringField("latitude", kpiActivity.getDomain());
		}
		if (kpiActivity.getInsertTime() != null) {
			jgen.writeNumberField("insertTime", kpiActivity.getInsertTime().getTime());
		}
		if (kpiActivity.getElapseTime() != null) {
			jgen.writeNumberField("elapseTime", kpiActivity.getElapseTime().getTime());
		}
		if (kpiActivity.getDeleteTime() != null) {
			jgen.writeNumberField("deleteTime", kpiActivity.getDeleteTime().getTime());
		}
	
		jgen.writeEndObject();
	}
}