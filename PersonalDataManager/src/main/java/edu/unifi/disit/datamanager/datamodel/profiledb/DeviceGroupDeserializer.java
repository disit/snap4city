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

public class DeviceGroupDeserializer extends StdDeserializer<DeviceGroup> {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger();

	public DeviceGroupDeserializer() {
		this(null);
	}

	public DeviceGroupDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public DeviceGroup deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		JsonNode jnode = jp.getCodec().readTree(jp);
		DeviceGroup kpidata = new DeviceGroup();

		if (jnode.get("id") != null) {
			kpidata.setId(jnode.get("id").asLong());
		}		
		if (jnode.get("description") != null) {
			kpidata.setDescription(jnode.get("description").asText());
		}		
		if (jnode.get("highLevelType") != null) {
			kpidata.setHighLevelType(jnode.get("highLevelType").asText());
		}		
		if (jnode.get("name") != null) {
			kpidata.setName(jnode.get("name").asText());
		}
		if (jnode.get("insertTime") != null) {
			Date date = new Date();
			if (jnode.get("insertTime").asLong() != 0) {
				date.setTime(jnode.get("insertTime").asLong());
				kpidata.setInsertTime(date);
			} else {
				try {
					kpidata.setInsertTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("insertTime").asText()));
				} catch (ParseException e) {
					try {
						kpidata.setInsertTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("insertTime").asText()));
					} catch (ParseException d) {
						logger.error("Parsing error {}", d);
					}
				}
			}
		}	
                
                if (jnode.get("updateTime") != null) {
			Date date = new Date();
			if (jnode.get("updateTime").asLong() != 0) {
				date.setTime(jnode.get("updateTime").asLong());
				kpidata.setUpdateTime(date);
			} else {
				try {
					kpidata.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("updateTime").asText()));
				} catch (ParseException e) {
					try {
						kpidata.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("updateTime").asText()));
					} catch (ParseException d) {
						logger.error("Parsing error {}", d);
					}
				}
			}
		}	
                
                if (jnode.get("deleteTime") != null) {
			Date date = new Date();
			if (jnode.get("deleteTime").asLong() != 0) {
				date.setTime(jnode.get("deleteTime").asLong());
				kpidata.setDeleteTime(date);
			} else {
				try {
					kpidata.setDeleteTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jnode.get("deleteTime").asText()));
				} catch (ParseException e) {
					try {
						kpidata.setDeleteTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jnode.get("deleteTime").asText()));
					} catch (ParseException d) {
						logger.error("Parsing error {}", d);
					}
				}
			}
		}
                
		if (jnode.get("organizations") != null) {
			kpidata.setOrganizations(jnode.get("organizations").asText());
		}
		if (jnode.get("ownership") != null) {
			kpidata.setOwnership(jnode.get("ownership").asText());
		}		
		if (jnode.get("username") != null) {
			kpidata.setUsername(jnode.get("username").asText());
		}
		

		return kpidata;
	}
}