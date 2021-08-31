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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapConfigs;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapHttpClient;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProvider;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderException;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.google.gson.JsonElement;

public class ServicemapHttpOAuthClient extends ServicemapHttpClient {

	private OAuthTokenProvider tokenProvider;
	
	public ServicemapHttpOAuthClient( OAuthTokenProvider tokenProvider , 
									  ServicemapConfigs configs ,
									  ProcessContext context ) {
		super( configs , context );
		
		this.tokenProvider = tokenProvider;
	}
	
	@Override
	public JsonElement getEnrichmentData( String elementIdPrefix , String elementId ) throws EnrichmentSourceException{
		String requestUrl;
		try {
			requestUrl = buildRequestUrl( elementIdPrefix , elementId );
		}catch( UnsupportedEncodingException e ) {
			throw new EnrichmentSourceException( 
				String.format( "%s exception while encoding the query string." , e.getClass().getName() ) ,
				e
			);
		}
		
		return fetchEnrichmentData(requestUrl);
	}
	
	@Override
	public JsonElement getEnrichmentData( String elementId ) throws EnrichmentSourceException {
		return getEnrichmentData( elementId , configs.defaultUriPrefix );
	}
	
	private JsonElement fetchEnrichmentData( String requestUrl ) throws EnrichmentSourceException {
		// Get the access token
		OAuth2AccessToken token;
		try {
			token = tokenProvider.getToken();
		}catch( OAuthTokenProviderException e ) {
			throw new EnrichmentSourceException( 
				String.format( "%s while retrieving access token.", e.getClass().getName() ) , 
				e 
			);
		}
		
		// Perform request
		HttpGet get = new HttpGet( requestUrl );
		get.addHeader( "Authorization" , "Bearer " + token.getAccessToken() );
		HttpResponse servicemapResponse = executeRequest( get );
		
		// Process response body 
		String responseBody;
		ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
		try {
			if( servicemapResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK && 
				servicemapResponse.getEntity() != null ) {
					// Copy the response content for logging if any
					try{
						IOUtils.copy( servicemapResponse.getEntity().getContent() , contentStream );
					}catch( IOException ex ) {
						throw new EnrichmentSourceException(
							String.format( "%s while handling the Servicemap response body." , ex.getClass().getName() ) ,
							ex
						);
					}
			}
			responseBody = responseHandler.handleResponse( servicemapResponse );
		}catch( ClientProtocolException e ) {
			// HTTP protocol error
			EnrichmentSourceException newEx = new EnrichmentSourceException(
				String.format( "%s while handling the Servicemap response body." , e.getClass().getName() ), 
				e
			);
			if( servicemapResponse.getEntity() != null ) {
				String errorResponse = new String( contentStream.toByteArray() ).trim();
				newEx.addInfo( "Servicemap_response" , errorResponse );
			}
			throw newEx;
		}catch( IOException e ) {
			// Problem or aborted connection case
			throw new EnrichmentSourceException( 
				String.format( "%s while handling Servicemap response body.", e.getClass().getName() ) ,
				e
			);
		}
		return this.parser.parse( responseBody );
	}
	
}
