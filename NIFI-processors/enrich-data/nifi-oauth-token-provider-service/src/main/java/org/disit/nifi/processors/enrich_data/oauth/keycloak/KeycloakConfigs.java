/**
 *  Nifi EnrichData processor
 *  
 *  Copyright (C) 2020 DISIT Lab http://www.disit.org - University of Florence
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package org.disit.nifi.processors.enrich_data.oauth.keycloak;

public class KeycloakConfigs {

	public String keycloakUrl;
	
	public String clientId;
	public String clientSecret;
	public String username;
	public String password;
	public String realm;
	private String defaultScope;
	
	public KeycloakConfigs() {
		this.clientId = null;
		this.clientSecret = null;
		this.username = null;
		this.password = null;
		this.realm = null;
		this.defaultScope = "openid";
	}
	
	public KeycloakConfigs( String keycloakUrl , String cliendId , String clientSecret , 
						    String username , String password , String realm ) {
		this();
		
		this.keycloakUrl = keycloakUrl;
		this.clientId = cliendId;
		this.clientSecret = clientSecret;
		this.username = username;
		this.password = password;
		this.realm = realm;
	}
	
	public String getDefaultScope() {
		return defaultScope;
	}
	
}
