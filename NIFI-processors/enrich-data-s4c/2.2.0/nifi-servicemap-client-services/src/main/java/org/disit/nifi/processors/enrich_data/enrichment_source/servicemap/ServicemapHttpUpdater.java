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

package org.disit.nifi.processors.enrich_data.enrichment_source.servicemap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicHttpResponse;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceUpdater;
import org.disit.nifi.processors.enrich_data.enrichment_source.http.HttpBaseClient;

import com.google.gson.JsonObject;

public class ServicemapHttpUpdater extends HttpBaseClient implements EnrichmentSourceUpdater{
	
	protected ServicemapConfigs configs;
	
	private String endpoint;
	private String staticUrl;
	private ResponseHandler responseHandler;
	
	private void configureClient(String endpoint , ServicemapConfigs configs) {
		this.configs = configs;
		this.endpoint = endpoint;
		this.responseHandler = new BasicResponseHandler();
		
		if( !endpoint.isEmpty() && this.endpoint.startsWith("/") ) {
				this.endpoint = this.endpoint.substring( 1 , this.endpoint.length() );
		}
		StringBuilder urlBuilder = new StringBuilder( configs.servicemapUrl );
		if( !configs.servicemapUrl.endsWith("/") )
			urlBuilder.append( "/" );
		urlBuilder.append( this.endpoint );
		
		if( !configs.additionalQueryString.isEmpty() ) {
			if( !configs.additionalQueryString.startsWith("?") )
				urlBuilder.append("?");
			urlBuilder.append( configs.additionalQueryString );
		}
		this.staticUrl = urlBuilder.toString();
	}
	
	public ServicemapHttpUpdater(String endpoint , ServicemapConfigs configs, int maxPerRoute , int maxTotal ) {
		super( maxPerRoute , maxTotal );
		configureClient( endpoint , configs );
	}
	
	public ServicemapHttpUpdater(String endpoint , ServicemapConfigs configs , ProcessContext context) {
		super(context);
		configureClient( endpoint , configs );
	}
	
	public ServicemapHttpUpdater(String endpoint, ServicemapConfigs configs ) {
		super( 4 , 4 );
		configureClient(endpoint , configs);
	}
	
	public String buildResourceUri( String deviceId , String uriPrefix ) {
		StringBuilder uriBuilder = new StringBuilder( uriPrefix );
		if( !uriPrefix.endsWith("/") )
			uriBuilder.append( "/" );
		return uriBuilder.append( deviceId ).toString();
	}
	
	public String buildResourceUri( String deviceId ) {
		return buildResourceUri(deviceId , configs.defaultUriPrefix);
	}
	
	public HttpPost buildPostRequest( JsonObject reqBodyObj ) {
		HttpPost post = new HttpPost( this.staticUrl );
		HttpEntity entity = new ByteArrayEntity( 
			reqBodyObj.toString().getBytes(StandardCharsets.UTF_8 ) 
		);
		post.setEntity( entity );
		return post;
	}
	
	protected void handleUpdateResponse( HttpResponse response ) throws EnrichmentSourceException{
		try {
			this.responseHandler.handleResponse( response );
		} catch (ClientProtocolException e) {
			throw new EnrichmentSourceException( "ClientProtocolException while performing the enrichment source update." , e );
		} catch (IOException e) {
			throw new EnrichmentSourceException( "IOException while performing the enrichment source update request." , e );
		}
	}

	@Override
	public void performUpdate( JsonObject reqBodyObj ) throws EnrichmentSourceException {
		HttpPost req = buildPostRequest( reqBodyObj );
		HttpResponse updateResponse = executeRequest( req );
		handleUpdateResponse( updateResponse );
	}

}
