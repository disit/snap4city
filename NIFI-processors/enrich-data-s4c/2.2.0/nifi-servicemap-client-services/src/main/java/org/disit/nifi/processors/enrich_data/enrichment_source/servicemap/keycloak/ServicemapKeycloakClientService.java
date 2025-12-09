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

package org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.keycloak;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.util.StandardValidators;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapClientService;

public interface ServicemapKeycloakClientService extends ServicemapClientService{

	public static final PropertyDescriptor KEYCLOAK_URL = new PropertyDescriptor
			.Builder().name( "KEYCLOAK_URL" )
			.displayName( "Keycloak Url" )
			.description( "The Keycloak base url." )
			.required( true )
			.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
			.build();
	
	public static final PropertyDescriptor CLIENT_ID = new PropertyDescriptor
            .Builder().name( "CLIENT_ID" )
            .displayName("Client ID")
            .description( "The client/application ID to use for the authentication service." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
	
	public static final PropertyDescriptor CLIENT_SECRET = new PropertyDescriptor
            .Builder().name( "CLIENT_SECRET" )
            .displayName("Client secret")
            .description( "The client/application secret to use for the authentication service." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
	
	public static final PropertyDescriptor REALM = new PropertyDescriptor
            .Builder().name( "REALM" )
            .displayName("Realm")
            .description( "The Keycloak realm to use." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
	
	public static final PropertyDescriptor USERNAME = new PropertyDescriptor
            .Builder().name( "USERNAME" )
            .displayName("Username")
            .description( "The username to use for the OAuth flow." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
	
	public static final PropertyDescriptor PASSWORD = new PropertyDescriptor
            .Builder().name( "PASSWORD" )
            .displayName("Password")
            .description( "The password to use for the OAuth flow." )
            .required(true)
            .sensitive(true) //Set sensitive to hide the password in the UI
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
	
}
