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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapConfigs;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapHttpUpdater;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProvider;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderException;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.google.gson.JsonObject;

public class ServicemapHttpOAuthUpdater extends ServicemapHttpUpdater {

	OAuthTokenProvider tokenProvider;
	
	public ServicemapHttpOAuthUpdater( OAuthTokenProvider tokenProvider , String endpoint , ServicemapConfigs servicemapConfigs  , 
									   ProcessContext context ) {
		super( endpoint , servicemapConfigs , context );
		
		this.tokenProvider = tokenProvider;
	}
	
	@Override
	public void performUpdate( JsonObject reqBodyObj ) throws EnrichmentSourceException {
		OAuth2AccessToken token;
		
		try {
			token = tokenProvider.getToken();
		}catch( OAuthTokenProviderException ex ) {
			throw new EnrichmentSourceException( "OAuthTokenProviderException while retrieving access token." , ex );
		}
		
		HttpPost post = buildPostRequest(reqBodyObj);
		post.addHeader( "Authorization" , "Bearer " + token.getAccessToken() );
		
		HttpResponse updateResponse = executeRequest( post );
		handleUpdateResponse( updateResponse );
	}
	
}
