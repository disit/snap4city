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

package org.disit.nifi.processors.enrich_data.locators.iotdirectory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.http.HttpBaseClient;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocator;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocatorException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class IOTDirectoryResourceLocator extends HttpBaseClient implements EnrichmentResourceLocator {

	protected IOTDirectoryResourceLocatorConfig config;
	protected JsonParser parser;
	protected ResponseHandler<String> responseHandler;
	
	protected String staticRequestUrlPart;
	
	protected LoadingCache<String, String> locationsCache;
	
	protected IOTDirectoryResourceLocator( IOTDirectoryResourceLocatorConfig config , ProcessContext context ) {
		super(context);
		this.config = config;
		this.parser = new JsonParser();
		this.responseHandler = new BasicResponseHandler();
		
		this.staticRequestUrlPart = buildStaticRequestUrlPart();
		
		
		CacheBuilder<Object,Object> cacheBuilder = CacheBuilder.newBuilder()
				.concurrencyLevel( context.getMaxConcurrentTasks() )
				.maximumSize( config.getMaxCacheSize() );
		if( config.cacheExpires() ) {
			cacheBuilder.expireAfterWrite( config.expireCacheEntriesAfterMillis , TimeUnit.MILLISECONDS );
		}
		
		cacheBuilder.recordStats();
		this.locationsCache = cacheBuilder
			.build(
				new CacheLoader<String , String>(){				
					@Override
					public String load(String subscriptionId) throws EnrichmentResourceLocatorException{
						return fetchServiceUriPrefix( subscriptionId );
					}
				}
			);
		
	}
	
	@Override 
	public String getResourceLocation( FlowFile ff ) throws EnrichmentResourceLocatorException {
		String subId = subIdFromFlowFileAttributes(ff);
		return getResourceLocation( subId );
	}

	protected String subIdFromFlowFileAttributes( FlowFile ff ) throws EnrichmentResourceLocatorException {
		String subIdAttrName = config.getSubscriptionIdAttributeName();
		String subId = ff.getAttribute( subIdAttrName );
		
		if( subId == null ) {
			throw new EnrichmentResourceLocatorException( String.format( 
				"The '%s' attribute is not contained in the flow file. Unable to determine the subscription id." , 
				subIdAttrName )
			);
		}
		
		return subId;
	}
	
	@Override
	public String getResourceLocation( String resourceReference ) throws EnrichmentResourceLocatorException{
		try {
			String location = locationsCache.get( resourceReference );
			System.out.println( locationsCache.stats() );
			return location;
		} catch( ExecutionException ex ) {
			throw (EnrichmentResourceLocatorException) ex.getCause();
		}
	}
	
	public String fetchServiceUriPrefix( String subscriptionId ) throws EnrichmentResourceLocatorException {
		String requestUrl = buildRequestUrl( subscriptionId );
		HttpGet get = new HttpGet( requestUrl );
		HttpResponse iotDirectoryResponse;
		try {
			iotDirectoryResponse = executeRequest( get );
		} catch( EnrichmentSourceException ex ) {
			throw new EnrichmentResourceLocatorException( "Exception while fetching the service uri prefix.", ex );
		}
		
		return processResponseBody( iotDirectoryResponse );
	}
	
	protected String processResponseBody( HttpResponse response ) throws EnrichmentResourceLocatorException {		
		String responseBody;
		ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
		
		try {
			
			if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
				if( response.getEntity() != null ) {
					try {
						IOUtils.copy( response.getEntity().getContent() , contentStream );
					} catch( IOException e ) {
						throw new EnrichmentResourceLocatorException( 
							String.format( "%s while handling the IOTDirectory service response body." , e.getClass().getName() ) , 
							e
						);
					}
				}
			}
			responseBody = responseHandler.handleResponse( response );
		} catch( ClientProtocolException ex ) {
			// HTTP protocol error
			EnrichmentResourceLocatorException newEx = new EnrichmentResourceLocatorException( 
				String.format( "%s" , ex.getClass().getName() ) , 
				ex 
			);
			
			if( response.getEntity() != null ) {
				String errorResponse = new String( contentStream.toByteArray() ).trim();
				newEx.addAdditionalInfo( "IOTDirectory_response" , errorResponse );
			}
			throw newEx;
		} catch( IOException ex ) {
			// Problem or aborted connection
			throw new EnrichmentResourceLocatorException(
				String.format( "%s while handling the IOTDirectory service response." , ex.getClass().getName() ) ,
				ex 
			);
		}
		
		JsonElement iotDirectoryResponse;
		try {
			iotDirectoryResponse = parser.parse( responseBody );
		}catch( JsonParseException ex ) {
			EnrichmentResourceLocatorException newEx = new EnrichmentResourceLocatorException( 
				String.format( "%s while parsing the IOTDirectory service response body." , ex.getClass().getName() ) , 
				ex 
			);
			newEx.addAdditionalInfo( "IOTDirectory_response" , responseBody );
			throw newEx;
		}
		
		if( iotDirectoryResponse.isJsonArray() ) {
			if( iotDirectoryResponse.getAsJsonArray().size() == 0 ) {
				throw new EnrichmentResourceLocatorException( "The IOTDirectory response is an empty JSON array" );
			}
			iotDirectoryResponse = iotDirectoryResponse.getAsJsonArray().get(0);
		}
		
		if( !iotDirectoryResponse.isJsonObject() ) {
			throw new EnrichmentResourceLocatorException( "The IOTDirectory response is not a JSON object: " + iotDirectoryResponse.toString() );
		}
		
		JsonObject responseObj = iotDirectoryResponse.getAsJsonObject();
		StringBuilder exploredPath = new StringBuilder("");
		List<String> responsePath = config.getServiceUriPrefixResponsePath(); 
				
		JsonElement curEl = responseObj;
		String serviceUriPrefix = null;
		for( int i = 0 ; i < responsePath.size() ; i++ ) {
			String nextPathEl = responsePath.get(i);
			exploredPath.append( nextPathEl );
			if( !curEl.getAsJsonObject().has( nextPathEl ) ) {
				throw new EnrichmentResourceLocatorException( 
					String.format( "The field '%s' is not contained in the IOTDirectory response object: %s" , 
						exploredPath.toString() , responseObj.toString() ) );
			}
			
			if( i == responsePath.size() - 1 ) {				
				JsonElement targetEl = curEl.getAsJsonObject().get( nextPathEl );
				if( !targetEl.isJsonPrimitive() || !targetEl.getAsJsonPrimitive().isString() ) {
					throw new EnrichmentResourceLocatorException( 
						String.format( "The target field '%s' is not a string in the IOTDirectory response object: %s" , 
							exploredPath.toString() , responseObj.toString() ) );
				}
				serviceUriPrefix = targetEl.getAsString();
			} else {
				if( !curEl.getAsJsonObject().get( nextPathEl ).isJsonObject() ){
					throw new EnrichmentResourceLocatorException( 
						String.format( "The intermediate field '%s' is not a JSON object in the IOTDirectory response: %s" , 
							exploredPath.toString() , responseObj.toString() ) );
					
				}
				
				curEl = curEl.getAsJsonObject().get( nextPathEl );
				exploredPath.append( "/" );
			}
		}
		return serviceUriPrefix;
	}
	
	protected String buildStaticRequestUrlPart() {
		StringBuilder urlBuilder = new StringBuilder( config.getIotDirectoryUrl() );
		
		if( !urlBuilder.toString().endsWith("?") && 
			!config.getAdditionalQueryString().startsWith("?") ) {
			
			urlBuilder.append( "?" );
		}
		
		urlBuilder.append( config.getAdditionalQueryString() );
		if( urlBuilder.toString().endsWith( "&" ) ) {
			urlBuilder.deleteCharAt( urlBuilder.length()-1 );
		}
		return urlBuilder.toString();
	}
	
	public String buildRequestUrl( String resourceReference ) {
		return new StringBuilder( this.staticRequestUrlPart )
				.append( "&" )
				.append( config.getSubscriptionIdRequestParamName() )
				.append( "=" )
				.append( resourceReference )
				.toString();
	}
	
	public String buildRequestUrl( FlowFile ff ) throws EnrichmentResourceLocatorException {
		String subId = subIdFromFlowFileAttributes( ff );
		return buildRequestUrl( subId );
	}

}
