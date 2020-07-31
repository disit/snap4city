/* Data Manager (DM).
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
package edu.unifi.disit.datamanager.service;

import java.util.HashMap;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationServiceImpl implements IConfigurationService {

	@Value("${spring.openidconnect.userinfo_endpoint}")
	private String authenticationUrl;
	
	@Value("${config.kpi.dictionary}")
	private String dictionaryUrl;
	
	@Value("${config.kpi.organizationlist}")
	private String organizationList;

	@Value("${config.kpi.authentication.clientid}")
	private String kpiAuthenticationClientId;

	@Value("${config.grp.authentication.clientid}")
	private String grpAuthenticationClientId;

	@Override
	public HashMap<String, String> getConfiguration(String version, Locale lang) {
		HashMap<String, String> config = new HashMap<String, String>();
		if (version.equals("v1")) {
			config.put("Authentication.url", authenticationUrl);
			config.put("Dictionary.url", dictionaryUrl);
			config.put("organization.list", organizationList);
			config.put("kpi.Authentication.clientId", kpiAuthenticationClientId);
			config.put("grp.Authentication.clientId", grpAuthenticationClientId);
		}
		return config;
	}
}