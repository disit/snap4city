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
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class KPIActivityDeserializer extends StdDeserializer<KPIActivity> {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger();
	
	public KPIActivityDeserializer() {
		this(null);
	}

	public KPIActivityDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public KPIActivity deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		JsonNode jnode = jp.getCodec().readTree(jp);
		KPIActivity kpiActivity = new KPIActivity();

		if (jnode.get("id") != null) {
			kpiActivity.setId(jnode.get("id").asLong());
		}
		if (jnode.get("kpiId") != null) {
			kpiActivity.setKpiId(jnode.get("kpiId").asLong());
		}
		if (jnode.get("sourceRequest") != null) {
			kpiActivity.setSourceRequest(jnode.get("sourceRequest").asText());
		}
		if (jnode.get("sourceId") != null) {
			kpiActivity.setSourceId(jnode.get("sourceId").asText());
		}
		if (jnode.get("accessType") != null) {
			kpiActivity.setAccessType(jnode.get("accessType").asText());
		}
		if (jnode.get("domain") != null) {
			kpiActivity.setDomain(jnode.get("domain").asText());
		}

		if (jnode.get("insertTime") != null) {
			Date date = new Date();
			if (jnode.get("insertTime").asLong() != 0) {
				date.setTime(jnode.get("insertTime").asLong());
				kpiActivity.setInsertTime(date);
			} else {
				try {
					kpiActivity.setInsertTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("insertTime").asText()));
				} catch (ParseException e) {
					try {
						kpiActivity.setInsertTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("dataTime").asText()));
					} catch (ParseException d) {
						logger.error("Parsing error", d);
					}
				}
			}
		}

		return kpiActivity;
	}
}