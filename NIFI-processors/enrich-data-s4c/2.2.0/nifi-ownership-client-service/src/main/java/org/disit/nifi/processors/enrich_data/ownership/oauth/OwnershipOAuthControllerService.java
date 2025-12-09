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

package org.disit.nifi.processors.enrich_data.ownership.oauth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProvider;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderService;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipControllerService;

public class OwnershipOAuthControllerService extends OwnershipControllerService{
	
	/**
	 * OAuth token provider service validator
	 */
	public static final Validator OAUTH_TOKEN_PROVIDER_SERVICE_VALIDATOR = new Validator() {

		@Override
		public ValidationResult validate(String subject, String input, ValidationContext context) {
			ValidationResult.Builder builder = new ValidationResult.Builder();
			
			try {
				context.getProperty( OAUTH_TOKEN_PROVIDER_SERVICE )
					   .asControllerService( OAuthTokenProviderService.class );
				builder.subject( subject ).explanation( "Valid OAuthTokenProviderService." )
					   .valid( true );
			} catch( IllegalArgumentException e ) {
				builder.subject( subject ).explanation( "Invalid OAuthTokenProviderService." )
					   .valid( false );
			}
			
			return builder.build();
		}
		
	};
	
	public static final String TOKEN_MODE_VALUES[] = {
		"Token as 'accessToken' query string parameter" ,
		"Token as 'Authorization' header"
	};
 	
	public static final Map<String , Integer> tokenModeMapping = new HashMap<>();
	
	public static final PropertyDescriptor OAUTH_TOKEN_PROVIDER_SERVICE = new PropertyDescriptor
			.Builder().name( "OAUTH_TOKEN_PROVIDER_SERVICE" )
			.displayName( "OAuth token provider service" )
			.identifiesControllerService( OAuthTokenProviderService.class )
			.description( "The client service which identifies the OAuth access token provider." )
			.required( true )
			.addValidator( OAUTH_TOKEN_PROVIDER_SERVICE_VALIDATOR )
			.build();
	
	public static final PropertyDescriptor TOKEN_MODE = new PropertyDescriptor
			.Builder().name( "TOKEN_MODE" )
			.displayName( "Token mode" )
			.description( "Specify how the client will pass the token when making requests." )
			.required( true )
			.defaultValue( TOKEN_MODE_VALUES[0] )
			.allowableValues( TOKEN_MODE_VALUES )
			.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
			.build();
	
	private static final List<PropertyDescriptor> descriptors;
	
	static {
		final List<PropertyDescriptor> descs = new ArrayList<>();
		descs.addAll( OwnershipControllerService.descriptors );
		descs.add( OAUTH_TOKEN_PROVIDER_SERVICE );
		descs.add( TOKEN_MODE );
		descriptors = Collections.unmodifiableList( descs );
		
		staticProperties.add( OAUTH_TOKEN_PROVIDER_SERVICE.getName() );
		staticProperties.add( TOKEN_MODE.getName() );
		
		tokenModeMapping.put( TOKEN_MODE_VALUES[0] , OwnershipHttpOAuthClient.TOKEN_AS_QUERY_STRING_PARAMETER );
		tokenModeMapping.put( TOKEN_MODE_VALUES[1] , OwnershipHttpOAuthClient.TOKEN_AS_HEADER );
	}
	
	private OAuthTokenProviderService tokenProviderService;
	private int tokenMode;
	
	@Override
	protected List<PropertyDescriptor> getSupportedPropertyDescriptors(){
		return descriptors;
	}
	
	@Override
	@OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException {
		super.onEnabled( context );
		
		this.tokenProviderService = context.getProperty( OAUTH_TOKEN_PROVIDER_SERVICE )
										   .asControllerService( OAuthTokenProviderService.class );
		this.tokenMode = tokenModeMapping.get( context.getProperty( TOKEN_MODE ).getValue() );
	}

	@Override
    public EnrichmentSourceClient getClient( ProcessContext context ) throws InstantiationException {
		OAuthTokenProvider tokenProvider = tokenProviderService.getTokenProvider();
		
		OwnershipHttpOAuthClient ownershipKeycloakClient = new OwnershipHttpOAuthClient( 
				tokenProvider , 
				clientConfig , 
				context , 
				tokenMode 
		);
		
		return ownershipKeycloakClient;
	}
}
