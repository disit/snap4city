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
import edu.unifi.disit.datamanager.service.ICredentialsService;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;

public class DeviceGroupSerializer extends StdSerializer<DeviceGroup> {

	private static final long serialVersionUID = 1L;
        
        @Autowired
	ICredentialsService credentialService;

	public DeviceGroupSerializer() {
		this(null);
	}

	public DeviceGroupSerializer(Class<DeviceGroup> t) {
		super(t);
	}

	@Override
	public void serialize(DeviceGroup grp, JsonGenerator jgen, SerializerProvider provider) throws IOException {

		jgen.writeStartObject();

		if (grp.getId() != null) {
			jgen.writeNumberField("id", grp.getId());
		}
		if (grp.getDescription() != null) {
			jgen.writeStringField("description", grp.getDescription());
		}
		if (grp.getDeleteTime() != null) {
			jgen.writeNumberField("deleteTime", grp.getDeleteTime().getTime());
		}
                if (grp.getInsertTime() != null) {
			jgen.writeNumberField("insertTime", grp.getInsertTime().getTime());
		}
                if (grp.getUpdateTime() != null) {
			jgen.writeNumberField("updateTime", grp.getUpdateTime().getTime());
		}
		if (grp.getHighLevelType() != null) {
			jgen.writeStringField("highLevelType", grp.getHighLevelType());
		}
		if (grp.getName() != null) {
			jgen.writeStringField("name", grp.getName());
		}
		if (grp.getOrganizations() != null) {
			jgen.writeStringField("organizations", grp.getOrganizations());
		}
		if (grp.getOwnership() != null) {
			jgen.writeStringField("ownership", grp.getOwnership());
		}
		if (grp.getUsername() != null) {
			jgen.writeStringField("username", grp.getUsername());
		}
                if (grp.getSize() > 0) {
                    jgen.writeBooleanField("viewEnabled", true);
                    jgen.writeStringField("size", String.valueOf(grp.getSize())+(grp.getSize() > 1 ? " items" : " item"));
		}
                else {
                    jgen.writeBooleanField("viewEnabled", false);
                    jgen.writeStringField("ifClearDisabled", "style=cursor:not-allowed; disabled");
                    jgen.writeStringField("size", "Empty");                    
                }
                try {
                    if(credentialService.isRoot(Locale.getDefault()) || credentialService.getLoggedUsername(Locale.getDefault()).equals(grp.getUsername())) {
                        jgen.writeBooleanField("enableEdit", true);
                    }
                    else {
                        jgen.writeBooleanField("enableEdit", false);
                    }
                }
                catch(Exception e) {
                    jgen.writeBooleanField("enableEdit", false);
                }

		jgen.writeEndObject();
	}
}