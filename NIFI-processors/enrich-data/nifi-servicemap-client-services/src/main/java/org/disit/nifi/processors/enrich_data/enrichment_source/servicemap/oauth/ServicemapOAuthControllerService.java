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

package org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.oauth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.reporting.InitializationException;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceUpdater;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapControllerService;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProvider;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderService;

public class ServicemapOAuthControllerService extends ServicemapControllerService{
	
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
			
	
	public static final PropertyDescriptor OAUTH_TOKEN_PROVIDER_SERVICE = new PropertyDescriptor
			.Builder().name( "OAUTH_TOKEN_PROVIDER_SERVICE" )
			.displayName( "OAuth token provider service" )
			.identifiesControllerService( OAuthTokenProviderService.class )
			.description( "The client service which identifies the OAuth access token provider." )
			.required( true )
			.addValidator( OAUTH_TOKEN_PROVIDER_SERVICE_VALIDATOR )
			.build();
	
	
	private static final List<PropertyDescriptor> descriptors;
	
	private OAuthTokenProviderService tokenProviderService;
	
	static {
		final List<PropertyDescriptor> descs = new ArrayList<>();
		descs.add( OAUTH_TOKEN_PROVIDER_SERVICE );
		descs.addAll( ServicemapControllerService.descriptors );
		descriptors  = Collections.unmodifiableList( descs );
	}
	
	@Override
	protected List<PropertyDescriptor> getSupportedPropertyDescriptors(){
		return descriptors;
	}
	
	@Override
	@OnEnabled
	public void onEnable( final ConfigurationContext context ) throws InitializationException {
		super.onEnable( context );
		
		this.tokenProviderService = context.getProperty( OAUTH_TOKEN_PROVIDER_SERVICE )
										   .asControllerService( OAuthTokenProviderService.class );
	}
	
	@Override
	public EnrichmentSourceClient getClient( ProcessContext context ) throws InstantiationException{
		OAuthTokenProvider tokenProvider = tokenProviderService.getTokenProvider();
		ServicemapHttpOAuthClient servicemapOAuthClient = new ServicemapHttpOAuthClient( 
				tokenProvider, 
				servicemapConfigs, 
				context
		);
		
		return servicemapOAuthClient;
	}
	
	public EnrichmentSourceUpdater getUpdaterClient( String endpoint , ProcessContext context ) throws InstantiationException {
		OAuthTokenProvider tokenProvider = tokenProviderService.getTokenProvider();
		
		ServicemapHttpOAuthUpdater servicemapOAuthUpdater = new ServicemapHttpOAuthUpdater( 
			tokenProvider, 
			endpoint, 
			servicemapConfigs, 
			context 
		);
		
		return servicemapOAuthUpdater;
	}

}
