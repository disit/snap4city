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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.ServicemapSource;
import org.disit.nifi.processors.enrich_data.enrichment_source.http.HttpBaseClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


public class ServicemapHttpClient extends HttpBaseClient 
	   implements EnrichmentSourceClient, ServicemapSource{

	protected ServicemapConfigs configs;
	protected ResponseHandler<String> responseHandler;
	protected JsonParser parser;
	
	private void configureClient( ServicemapConfigs configs ) {
		this.configs = configs;
		this.responseHandler = new BasicResponseHandler();
		this.parser = new JsonParser();
	}
	
	public ServicemapHttpClient( ServicemapConfigs configs ) {
		super( 4 , 4 );
		configureClient(configs);
	}
	
	public ServicemapHttpClient( ServicemapConfigs configs , int maxPerRoute , int maxTotal ) {
		super( maxPerRoute , maxTotal );
		configureClient( configs );
	}
	
	public ServicemapHttpClient( ServicemapConfigs configs , ProcessContext context ) {
		super( context );
		configureClient( configs );
	}

	@Override
	public String getServicemapUrl() {
		return configs.servicemapUrl;
	}
	
	@Override
	public String getDefaultUriPrefix() {
		return configs.defaultUriPrefix;
	}
	
	@Override
	public String getAdditionalQueryString() {
		return configs.additionalQueryString;
	}
	
	// TODO: remove if not needed
	public void setAdditionalQueryString( String additionalQueryString ) {
		this.configs.additionalQueryString = additionalQueryString;
	}
	
	@Override
	public JsonElement getEnrichmentData(String deviceId) throws EnrichmentSourceException {
		String requestUrl;
		try {
			requestUrl = buildRequestUrl( deviceId );
		}catch( UnsupportedEncodingException e ) {
			throw new EnrichmentSourceException( 
				String.format( "%s exception while encoding the query string." , e.getClass().getName() ) ,
				e
			);
		}
		
		return fetchEnrichmentData( requestUrl );
	}

	@Override
	public JsonElement getEnrichmentData(String deviceId, String uriPrefix) throws EnrichmentSourceException {
		String requestUrl;
		try {
			requestUrl = buildRequestUrl( deviceId , uriPrefix );
		}catch( UnsupportedEncodingException e ) {
			throw new EnrichmentSourceException( 
				String.format( "%s exception while encoding the query string." , e.getClass().getName() ) ,
				e
			);
		}
		
		return fetchEnrichmentData( requestUrl );
	}
	
	private JsonElement fetchEnrichmentData( String requestUrl ) throws EnrichmentSourceException{
		String responseBody;
		ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
		
		HttpResponse response = executeRequest( new HttpGet( requestUrl ) );
		
		try {
			if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
				if( response.getEntity() != null ) {
					// Copy the response content stream for logging if any
					try {
						IOUtils.copy( response.getEntity().getContent() , contentStream );
					}catch( IOException e ) {
						throw new EnrichmentSourceException( 
							String.format( "%s while handling the Servicemap response body." , e.getClass().getName() ) , 
							e 
						);
					}
				}
			}
			responseBody = responseHandler.handleResponse( response );
		}catch( ClientProtocolException e ) {
			// HTTP protocol error
			EnrichmentSourceException newEx = new EnrichmentSourceException( 
				String.format( "%s while handling the Servicemap response body." , e.getClass().getName() ) , 
				e 
			);
			if( response.getEntity() != null ) {
				String errorResponse = new String( contentStream.toByteArray() ).trim();
				newEx.addInfo( "Servicemap_response" , errorResponse );
			}
			throw newEx;
		}catch( IOException e ) {
			// Problem or aborted connection case
			throw new EnrichmentSourceException( 
				String.format( "%s while handling the Servicemap response body.", e.getClass().getName() ) ,
				e
			);
		}
		return parser.parse( responseBody );
	}

	@Override
	public String buildRequestUrl(String uriPrefix, String deviceId) throws UnsupportedEncodingException {
		if( !uriPrefix.endsWith("/") )
			uriPrefix = uriPrefix.concat("/");
		
		StringBuilder reqUrlBuilder = new StringBuilder( configs.servicemapUrl )
			.append( "?serviceUri=" )
			.append( URLEncoder.encode( uriPrefix + deviceId , StandardCharsets.UTF_8.name() ) )
			.append( configs.additionalQueryString );
		return reqUrlBuilder.toString();
	}

	@Override
	public String buildRequestUrl(String deviceId) throws UnsupportedEncodingException {
		return buildRequestUrl( configs.defaultUriPrefix , deviceId );
	}

}
