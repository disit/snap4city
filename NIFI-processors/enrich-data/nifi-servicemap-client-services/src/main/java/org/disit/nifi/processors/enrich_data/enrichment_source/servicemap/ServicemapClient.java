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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.ServicemapSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * A client to retrieve enrichment object from Servicemap.
 *
 */
public class ServicemapClient implements EnrichmentSourceClient, ServicemapSource{

	// HttpClient 
	PoolingHttpClientConnectionManager connectionManager;
	protected CloseableHttpClient httpClient;
	protected ResponseHandler<String> responseHandler;
	
	// Servicemap URL and default Service URI prefix
	protected String servicemapUrl;
	protected String defaultUriPrefix;
	protected String additionalQueryString;
	
	/**
	 * Constructor with explicit connection pool settings. 
	 *  
	 * @param defaultMaxPerRoute sets the default max per route poolable connections.
	 * @param maxTotal sets the max total poolable connections. 
	 */
	public ServicemapClient( String servicemapUrl , String defaultUriPrefix , int defaultMaxPerRoute , int maxTotal ) {
		connectionManager = new PoolingHttpClientConnectionManager();
		
		connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		connectionManager.setMaxTotal(maxTotal);
		
		httpClient = HttpClients.custom()
						        .setConnectionManager( connectionManager )
						        .build();
		responseHandler = new BasicResponseHandler();
		
		this.servicemapUrl = servicemapUrl;
		this.defaultUriPrefix = defaultUriPrefix;
		this.additionalQueryString = "";
	}
	
	public ServicemapClient( ServicemapConfigs sc , int maxPerRoute , int maxTotal ) {
		this( sc.servicemapUrl , sc.defaultUriPrefix , maxPerRoute , maxTotal );
	}
	
	/**
	 * No arguments constructor with defaults.
	 */
	public ServicemapClient( String servicemapUrl , String defaultUriPrefix ) {
		this( servicemapUrl , defaultUriPrefix , 
			  4 , 4 ); // Defaults: 
					   // 	defaultMaxPerRoute: 4
					   // 	maxTotal: 4
	}
	
	/**
	 * Uses the context argument to set the connection pool.
	 * 
	 * @param context the ProcessContext to use.
	 */
	public ServicemapClient( String servicemapUrl , String defaultUriPrefix , ProcessContext context ) {
		/**
		 * Sets the default max per route and mat total connections 
		 * equal to the processor concurrency.
		 * This client uses only one route/host. 
		 */
		this( servicemapUrl , defaultUriPrefix , 
			  context.getMaxConcurrentTasks() , // default max per route 
			  context.getMaxConcurrentTasks() ); // max total
	}
	
	public ServicemapClient( ServicemapConfigs sc , ProcessContext context ) {
		this( sc.servicemapUrl , sc.defaultUriPrefix , context );
	}
	
	@Override
	public String getServicemapUrl() {
		return servicemapUrl;
	}
	
	@Override
	public String getDefaultUriPrefix() {
		return defaultUriPrefix;
	}
	
	@Override
	public String getAdditionalQueryString() {
		return additionalQueryString;
	}
	
	/**
	 * Set the additional query string for the servicemap requests.
	 * 
	 * @param additionalQueryString
	 */
	public void setAdditionalQueryString( String additionalQueryString ) {
		this.additionalQueryString = additionalQueryString;
	}
	
	@Override
	public JsonElement getEnrichmentData(String deviceId) throws EnrichmentSourceException {
		String requestUrl;
		try {
			requestUrl = buildRequestUrl( deviceId );
		} catch (UnsupportedEncodingException e) {
			throw new EnrichmentSourceException( "Exception while encoding the query string." , e );
		}
		
		return fetchEnrichmentData( requestUrl );
	}

	@Override
	public JsonElement getEnrichmentData(String uriPrefix, String deviceId) throws EnrichmentSourceException{		
		String requestUrl;
		try {
			requestUrl = buildRequestUrl( uriPrefix , deviceId );
		} catch (UnsupportedEncodingException e) {
			throw new EnrichmentSourceException( "Exception while encoding the query string." , e );
		}
		
		return fetchEnrichmentData( requestUrl );
	}
	
	protected JsonElement fetchEnrichmentData( String requestUrl ) throws EnrichmentSourceException{
		String responseBody;
		try {
			HttpResponse response = httpClient.execute( new HttpGet( requestUrl ) );
			responseBody = responseHandler.handleResponse( response );
		} catch (ClientProtocolException e) {
			throw new EnrichmentSourceException( "ClientProtocolException while retrieving enrichment data from Servicemap." , e );
		} catch (IOException e) {
			throw new EnrichmentSourceException( "IOException while retrieving enrichment data from Servicemap." , e );
		}
		
		return JsonParser.parseString( responseBody );
	}
	
	// moved to HttpBaseClient class
	@Override
	public void close() throws EnrichmentSourceException {
		try {
			this.httpClient.close();
			this.connectionManager.close();
		} catch (IOException e) {
			throw new EnrichmentSourceException( String.format( "Exception while closing ServicemapClient: %s" , e.getMessage() ) );
		}
	}
	
	/**
	 * Build the request url from an uri prefix and device id.
	 */
	public String buildRequestUrl( String uriPrefix , String deviceId ) throws UnsupportedEncodingException {
		StringBuilder reqUrlBuilder = new StringBuilder( this.servicemapUrl );
		reqUrlBuilder.append( "?serviceUri=" )
					 .append( URLEncoder.encode( uriPrefix + deviceId , StandardCharsets.UTF_8.name() ) )
					 .append( this.additionalQueryString );

		return reqUrlBuilder.toString();
	}
	
	/**
	 * Build the request url from a device id using the default uri prefix.
	 */
	public String buildRequestUrl( String deviceId ) throws UnsupportedEncodingException{
		return buildRequestUrl( this.defaultUriPrefix , deviceId );
	}

}
