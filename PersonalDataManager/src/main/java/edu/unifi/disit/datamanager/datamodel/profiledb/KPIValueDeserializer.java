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
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class KPIValueDeserializer extends StdDeserializer<KPIValue> {

	private static final long serialVersionUID = 1L;

	public KPIValueDeserializer() {
		this(null);
	}

	public KPIValueDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public KPIValue deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode jnode = jp.getCodec().readTree(jp);
		KPIValue kpivalues = new KPIValue();

		
		if(jnode.get("id") != null) {
			kpivalues.setId(jnode.get("id").asLong());
		}
		if(jnode.get("kpiId") != null) {
			kpivalues.setKpiId(jnode.get("kpiId").asLong());
		}
		if (jnode.get("dataTime") != null) {
			try {
				kpivalues.setDataTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("dataTime").asText()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (jnode.get("elapseTime") != null) {
			try {
				kpivalues.setElapseTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("elapseTime").asText()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (jnode.get("deleteTime") != null) {
			try {
				kpivalues.setDeleteTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("deleteTime").asText()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (jnode.get("insertTime") != null) {
			try {
				kpivalues.setInsertTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("insertTime").asText()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (jnode.get("value") != null) {
			kpivalues.setValue(jnode.get("value").asText());
		}

		return kpivalues;
	}
}