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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapConfigs;

import com.github.scribejava.apis.KeycloakApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.java8.Base64;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ServicemapKeycloakClient extends ServicemapClient{
	
	// Keycloak configs obj
	private KeycloakConfigs kc;
	
	// OAuth2Service
	private OAuth20Service service;
	
	// Token cache
	final ReadWriteLock rwl = new ReentrantReadWriteLock();
	private TokenCache tokenCache;
	
	public ServicemapKeycloakClient( ServicemapConfigs sc , KeycloakConfigs kc , 
									 ProcessContext context ) {
		super( sc , context );
//		super( sc.servicemapUrl , sc.defaultUriPrefix , context );
		
		//Keycloak configuration object
		this.kc = kc;
		
		// Build the Keycloak/OAuth service
		this.service = new ServiceBuilder( this.kc.clientId )
				.apiSecret( this.kc.clientSecret )
				.defaultScope( this.kc.getDefaultScope() )
				.build( KeycloakApi.instance( this.kc.keycloakUrl , this.kc.realm ) );
		
		this.tokenCache = new TokenCache();
	}
	
	public ServicemapKeycloakClient( ServicemapConfigs sc , KeycloakConfigs kc ) {
		super( sc.servicemapUrl , sc.defaultUriPrefix );
		
		//Keycloak configuration object
		this.kc = kc;
		
		// Build the Keycloak/OAuth service
		this.service = new ServiceBuilder( this.kc.clientId )
				.apiSecret( this.kc.clientSecret )
				.defaultScope( this.kc.getDefaultScope() )
				.build( KeycloakApi.instance( this.kc.keycloakUrl , this.kc.realm ) );
		
		this.tokenCache = new TokenCache();
	}
	
	@Override
	protected JsonElement fetchEnrichmentData( String requestUrl ) throws EnrichmentSourceException {
		
		rwl.readLock().lock(); // acquire read lock
		if( !tokenCache.isValid() ) { // check cache validity
			rwl.readLock().unlock(); // release read lock
			rwl.writeLock().lock(); // acquire write lock
			try {
				if( !tokenCache.isValid() ) { // recheck cache validity
					refreshTokenCache();
				}
				
				rwl.readLock().lock(); // downgrade lock
			} catch (IOException | InterruptedException | ExecutionException e) {
				throw new EnrichmentSourceException( "EnrichmentSourceException while getting/refreshing access token." , e );
			} finally {
				rwl.writeLock().unlock(); // release write lock
			}
		}
		
		OAuth2AccessToken token = tokenCache.getToken();
		
		HttpResponse response;
		try {
			HttpGet get = new HttpGet( requestUrl );
			
//			System.out.println( "RequestUrl: " + requestUrl );
			
			// Sign the get request with the token, Header vs Parameter
			get.addHeader( "Authorization" , "Bearer " + token.getAccessToken() );
			
			response = httpClient.execute( get );
			
		} catch (ClientProtocolException e) {
			throw new EnrichmentSourceException( "ClientProtocolException while retrieving enrichment data from Servicemap." , e );
		} catch (IOException e) {
			throw new EnrichmentSourceException( "IOException while retrieving enrichment data from Servicemap." , e );
		} finally {
			rwl.readLock().unlock(); // Release the read lock as soon as we have obtained the response
		}
		
		String responseBody;
		try {
			responseBody = responseHandler.handleResponse( response );
		} catch (ClientProtocolException e) {
			throw new EnrichmentSourceException( "ClientProtocolException while handling Servicemap response body." , e );
		} catch (IOException e) {
			throw new EnrichmentSourceException( "IOException while handling Servicemap response body." , e ); 
		}
		
		return this.parser.parse( responseBody );
	}
	
	// Methods to cache and manage access tokens
	public void refreshTokenCache() throws IOException, InterruptedException, ExecutionException { 
		if( !tokenCache.isValid() && !tokenCache.isRefreshValid() ) {
			// Get a new token
			OAuth2AccessToken token = service.getAccessTokenPasswordGrant( kc.username , kc.password );
			tokenCache.cacheToken( token );
		} else {
			// Use refresh token
			OAuth2AccessToken freshToken = service.refreshAccessToken( tokenCache.getToken().getRefreshToken() );
			tokenCache.cacheToken( freshToken );
		}
	}
	
	@Override
	public void close() throws EnrichmentSourceException {
		super.close();
		
		// Close keycloak resources
		try {
			service.close();
		} catch (IOException e) {
			throw new EnrichmentSourceException( "Exception while closing ServicemapKeycloakClient." , e );
		}
	}
	
	private class TokenCache{
		
		private long cacheThreshold;
		
		private OAuth2AccessToken token;
		// private JsonObject payloadObj;
		private long exp; //in milliseconds
		
		
		public TokenCache( long cacheThresholdMs ) {
			this.cacheThreshold = cacheThresholdMs;
			
			token = null;
//			payloadObj = null;
			exp = 0;
		}
		
		public TokenCache() {
			this( 1000 );
		}
		
		public JsonObject decodeTokenPayload( String tokenStr ) {
			String[] tokenParts = tokenStr.split( "\\." );
			String payload = new String( Base64.getDecoder().decode( tokenParts[1] ) );
			JsonObject payloadObj = ServicemapKeycloakClient.this.parser
									.parse( payload )
									.getAsJsonObject();
			return payloadObj;
		}
		
		public JsonObject decodeTokenPayload( OAuth2AccessToken token ) {
			return decodeTokenPayload( token.getAccessToken() );
		}
		
		public void cacheToken( OAuth2AccessToken token ) {
			this.token = token;
			
//			String[] tokenParts = token.getAccessToken().split( "\\." );
//			String payload = new String( Base64.getDecoder().decode( tokenParts[1] ) );
//			payloadObj = ServicemapKeycloakClient.this.parser
//							.parse( payload )
//							.getAsJsonObject();
			JsonObject payloadObj = decodeTokenPayload( token );
			exp = payloadObj.get("exp").getAsLong();
		}
		
		public OAuth2AccessToken getToken() {
			return token;
		}
		
		public boolean isEmpty() {
			return token == null;
		}
		
		public boolean isValid() {
			if( isEmpty() )
				return false;
			else
				return ( exp * 1000 - System.currentTimeMillis() ) >= cacheThreshold;
		}
		
		public boolean isRefreshValid() {
			if( isEmpty() )
				return false;
			else
				return this.isValid( token.getRefreshToken() );
		}
		
		public boolean isValid( OAuth2AccessToken token ) {
			return isValid( token.getAccessToken() );
		}
		
		public boolean isValid( String tokenStr ) {
			JsonObject payloadObj = decodeTokenPayload( tokenStr );
			long exp = payloadObj.get("exp").getAsLong();
			return ( exp * 1000 - System.currentTimeMillis() ) >= cacheThreshold;
		}
	}
	
}

