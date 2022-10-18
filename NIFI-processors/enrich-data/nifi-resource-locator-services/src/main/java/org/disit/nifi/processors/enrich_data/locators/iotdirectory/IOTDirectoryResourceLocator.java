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
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.http.HttpBaseClient;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocator;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocatorException;
import org.disit.nifi.processors.enrich_data.locators.ResourceLocations;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class IOTDirectoryResourceLocator extends HttpBaseClient implements EnrichmentResourceLocator {

	protected ComponentLog logger;
	protected IOTDirectoryResourceLocatorConfig config;
	protected ResponseHandler<String> responseHandler;
	
	protected String staticRequestUrlPart;
	
	protected LoadingCache<String, ResourceLocations> locationsCache;
	
	protected IOTDirectoryResourceLocator( IOTDirectoryResourceLocatorConfig config , ProcessContext context ) {
		super(context);
		this.config = config;
		this.responseHandler = new BasicResponseHandler();
		
		this.staticRequestUrlPart = buildStaticRequestUrlPart();
		
		
		CacheBuilder<Object,Object> cacheBuilder = CacheBuilder.newBuilder()
				.concurrencyLevel( context.getMaxConcurrentTasks() )
				.maximumSize( config.getMaxCacheSize() );
		if( config.cacheExpires() ) {
			cacheBuilder.expireAfterWrite( config.expireCacheEntriesAfterMillis , TimeUnit.MILLISECONDS );
		}
		
//		cacheBuilder.recordStats();
		this.locationsCache = cacheBuilder
			.build(
				new CacheLoader<String , ResourceLocations>(){
					@Override
					public ResourceLocations load(String subscriptionId) throws EnrichmentResourceLocatorException{
						return fetchResourceLocations( subscriptionId );
					}
				}
			);
	}
	
	@Override
	public void setLogger( ComponentLog logger ) {
		this.logger = logger;
	}
	
	@Override 
	public ResourceLocations getResourceLocations( FlowFile ff ) throws EnrichmentResourceLocatorException {
		String subId = subIdFromFlowFileAttributes(ff);
		return getResourceLocations( subId );
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
	public ResourceLocations getResourceLocations( String resourceReference ) throws EnrichmentResourceLocatorException{
		try {
			ResourceLocations location = locationsCache.get( resourceReference );
			return location;
		} catch( ExecutionException ex ) {
			throw (EnrichmentResourceLocatorException) ex.getCause();
		}
	}
	
	public ResourceLocations fetchResourceLocations( String subscriptionId ) throws EnrichmentResourceLocatorException {
		String requestUrl = buildRequestUrl( subscriptionId );
		HttpGet get = new HttpGet( requestUrl );
		HttpResponse iotDirectoryResponse;
		try {
			iotDirectoryResponse = executeRequest( get );
		}catch( EnrichmentSourceException ex ) {
			throw new EnrichmentResourceLocatorException( "Exception while fetching the resource location.", ex );
		}
		return processResponseBody( iotDirectoryResponse );
	}
	
	protected ResourceLocations processResponseBody( HttpResponse response ) throws EnrichmentResourceLocatorException {
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
		
		if( responseBody == null ) {
			throw new EnrichmentResourceLocatorException( "Null body in the response from the IOTDirectory service." );
		}
		
		JsonElement iotDirectoryResponse;
		try {
			iotDirectoryResponse = JsonParser.parseString( responseBody );
		}catch( JsonParseException ex ) {
			EnrichmentResourceLocatorException newEx = new EnrichmentResourceLocatorException( 
				String.format( "%s while parsing the IOTDirectory service response body." , 
				ex.getClass().getName() ) , 
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
		
		// service uri prefix
		ResourceLocations locations = new ResourceLocations();
		try {
			String serviceUriPrefix = getResponseObjectElement( 
				iotDirectoryResponse.getAsJsonObject() , 
				config.getServiceUriPrefixResponsePath() );
			locations.putLocation( 
				ResourceLocations.Service.SERVICEMAP , 
				serviceUriPrefix
			);
		} catch( EnrichmentResourceLocatorException ex ) {
			StringBuilder msg = new StringBuilder( "Cannot retrieve the service uri prefix from the IOTDirectory response.\n" )
				.append( ex.toString() );
			logger.warn( msg.toString() );
		}
		
		// ownership prefix
		if( config.getOrganizationResponsePath() != null && 
			config.getCBNameResponsePath() != null ) {
			
			try {
				String organization = getResponseObjectElement(
					iotDirectoryResponse.getAsJsonObject() ,
					config.getOrganizationResponsePath()
				);
				String contextBrokerName = getResponseObjectElement(
					iotDirectoryResponse.getAsJsonObject() ,
					config.getCBNameResponsePath()
				);
				StringBuilder ownershipPrefix = new StringBuilder( organization )
					.append( ":" ).append( contextBrokerName ).append( ":" );
				locations.putLocation( 
					ResourceLocations.Service.OWNERSHIP , 
					ownershipPrefix.toString()
				);
			}catch( EnrichmentResourceLocatorException ex ) {
				StringBuilder msg = new StringBuilder( "Cannot retrieve the ownership prefix form the IOTDirectory service response.\n" )
					.append( ex.getMessage() );
				logger.warn( msg.toString() );
			}
		}
		return locations;		
	}
	
	private String getResponseObjectElement( JsonObject responseObj , List<String> responsePath ) throws EnrichmentResourceLocatorException{
		StringBuilder exploredPath = new StringBuilder("");
		
		JsonElement curEl = responseObj;
		String targetElement = null;
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
				targetElement = targetEl.getAsString();
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
		return targetElement;
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
	
	@Override
	public String buildRequestUrl( String resourceReference ) {
		return new StringBuilder( this.staticRequestUrlPart )
				.append( "&" )
				.append( config.getSubscriptionIdRequestParamName() )
				.append( "=" )
				.append( resourceReference )
				.toString();
	}
	
	@Override
	public String buildRequestUrl( FlowFile ff ) throws EnrichmentResourceLocatorException {
		String subId = subIdFromFlowFileAttributes( ff );
		return buildRequestUrl( subId );
	}

}
