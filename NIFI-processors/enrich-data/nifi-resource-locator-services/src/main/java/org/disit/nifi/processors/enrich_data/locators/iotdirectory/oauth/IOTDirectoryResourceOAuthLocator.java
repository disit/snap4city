/** 
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

package org.disit.nifi.processors.enrich_data.locators.iotdirectory.oauth;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocatorException;
import org.disit.nifi.processors.enrich_data.locators.iotdirectory.IOTDirectoryResourceLocator;
import org.disit.nifi.processors.enrich_data.locators.iotdirectory.IOTDirectoryResourceLocatorConfig;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProvider;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderException;

import com.github.scribejava.core.model.OAuth2AccessToken;

public class IOTDirectoryResourceOAuthLocator extends IOTDirectoryResourceLocator {
	
	private OAuthTokenProvider tokenProvider;

	protected IOTDirectoryResourceOAuthLocator( OAuthTokenProvider tokeProvider , 
												IOTDirectoryResourceLocatorConfig config, 
												ProcessContext context) {
		super(config, context);
		this.tokenProvider = tokeProvider;
	}
	
	@Override
	public String fetchServiceUriPrefix( String subscriptionId ) throws EnrichmentResourceLocatorException {
		OAuth2AccessToken token;
		try {
			token = tokenProvider.getToken();
		} catch ( OAuthTokenProviderException ex ) {
			throw new EnrichmentResourceLocatorException( "OAuthTokenProviderException while retrieving the access token." , ex );
		}
		
		String requestUrl = buildRequestUrl( subscriptionId );
		HttpGet get = new HttpGet( requestUrl );
		get.addHeader( "Authorization" , "Bearer " + token.getAccessToken() );
		
		HttpResponse iotDirectoryResponse;
		try {
			iotDirectoryResponse = executeRequest( get );
		} catch( EnrichmentSourceException ex ) {
			throw new EnrichmentResourceLocatorException( "Exception while retrieving the service uri prefix." , ex );
		}
		
		return processResponseBody( iotDirectoryResponse );
	}

}
