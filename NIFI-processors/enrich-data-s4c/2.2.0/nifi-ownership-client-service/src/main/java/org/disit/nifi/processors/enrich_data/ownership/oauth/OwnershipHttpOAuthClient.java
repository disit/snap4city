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

import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProvider;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderException;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipClientConfig;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipHttpClient;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.google.gson.JsonElement;

public class OwnershipHttpOAuthClient extends OwnershipHttpClient{
	
	public static final int TOKEN_AS_HEADER = 0;
	public static final int TOKEN_AS_QUERY_STRING_PARAMETER = 1;
	
	private static final List<Integer> allowedTokenModes = Arrays.asList( TOKEN_AS_HEADER , TOKEN_AS_QUERY_STRING_PARAMETER ); 
	
	private OAuthTokenProvider tokenProvider;
	private int tokenMode;
	
	public OwnershipHttpOAuthClient( OAuthTokenProvider tokenProvider , OwnershipClientConfig config , 
								     ProcessContext context , int tokenMode ) {
		super( config , context );
		
		this.tokenProvider = tokenProvider;
		
		if( allowedTokenModes.contains( tokenMode ) ) {
			this.tokenMode = tokenMode;
		}else {
			this.tokenMode = TOKEN_AS_QUERY_STRING_PARAMETER;
		}
	}
	
	@Override
	public JsonElement getEnrichmentData( String elementId ) throws EnrichmentSourceException{
		String requestUrl = buildRequestUrl( elementId );
		return fetchEnrichmentData( requestUrl );
	}
	
	@Override
	public JsonElement getEnrichmentData( String elementId , String elementIdPrefix ) throws EnrichmentSourceException{
		String requestUrl = buildRequestUrl( elementId , elementIdPrefix );
		return fetchEnrichmentData( requestUrl );
	}
	
	private JsonElement fetchEnrichmentData( String requestUrl ) throws EnrichmentSourceException{
		OAuth2AccessToken token;
		try {
			token = tokenProvider.getToken();
		} catch (OAuthTokenProviderException e) {
			throw new EnrichmentSourceException( "OAuthTokenProviderException while retrieving access token." , e );
		};
		
		if( this.tokenMode == TOKEN_AS_QUERY_STRING_PARAMETER ) {
			requestUrl = new StringBuilder( requestUrl )
								.append( "&accessToken=" )
								.append( token.getAccessToken() )
								.toString();						
		}
		
		HttpGet get = buildGetRequest( requestUrl );
		if( this.tokenMode == TOKEN_AS_HEADER ) {
			get.addHeader( "Authorization" , "Bearer " + token.getAccessToken() );
		}
		
		HttpResponse ownershipResponse = executeRequest( get );
		return processResponseBody( ownershipResponse );
	}

}
