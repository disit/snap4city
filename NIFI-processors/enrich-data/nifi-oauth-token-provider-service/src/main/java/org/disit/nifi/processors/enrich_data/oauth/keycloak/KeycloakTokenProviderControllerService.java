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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProvider;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderService;

public class KeycloakTokenProviderControllerService extends AbstractControllerService implements OAuthTokenProviderService {

	public static final PropertyDescriptor KEYCLOAK_URL = new PropertyDescriptor
			.Builder().name( "KEYCLOAK_URL" )
			.displayName( "Keycloak Url" )
			.description( "The Keycloak base url." )
			.required( true )
			.expressionLanguageSupported( ExpressionLanguageScope.VARIABLE_REGISTRY )
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

    private static final List<PropertyDescriptor> properties;

    static {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(KEYCLOAK_URL);
        props.add(CLIENT_ID);
        props.add(CLIENT_SECRET);
        props.add(REALM);
        props.add(USERNAME);
        props.add(PASSWORD);
        properties = Collections.unmodifiableList(props);
    }
    
    private String keycloakUrl;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;
    private String realm;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException {
//    	keycloakUrl = context.getProperty( KEYCLOAK_URL ).getValue();
    	keycloakUrl = context.getProperty( KEYCLOAK_URL )
    						 .evaluateAttributeExpressions()
    						 .getValue();
		clientId = context.getProperty( CLIENT_ID ).getValue();
		clientSecret = context.getProperty( CLIENT_SECRET ).getValue();
		username = context.getProperty( USERNAME ).getValue();
		password = context.getProperty( PASSWORD ).getValue();
		realm = context.getProperty( REALM ).getValue();
    }

    @OnDisabled
    public void shutdown() {

    }

    @Override
    public OAuthTokenProvider getTokenProvider() {
    	KeycloakConfigs kc = new KeycloakConfigs( 
    			keycloakUrl , 
    			clientId , 
    			clientSecret , 
    			username , 
    			password , 
    			realm 
    	);
    	KeycloakTokenProvider tokenProvider = new KeycloakTokenProvider( kc );
    	
    	return tokenProvider;
    }

}
