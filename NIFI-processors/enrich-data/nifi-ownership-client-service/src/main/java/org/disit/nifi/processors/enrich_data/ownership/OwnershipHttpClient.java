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

package org.disit.nifi.processors.enrich_data.ownership;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.http.HttpEnrichmentSourceClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OwnershipHttpClient extends HttpEnrichmentSourceClient{
	
	//Http client
	protected ResponseHandler<String> responseHandler;
	
	//Json parser (Gson)
	JsonParser parser;
	
	protected OwnershipClientConfig config;
	
	protected String staticUrlPart;
	
	public OwnershipHttpClient( OwnershipClientConfig config , int maxPerRoute , int maxTotal ) {
		super( maxPerRoute , maxTotal );
		
		responseHandler = new BasicResponseHandler();
		
		this.parser = new JsonParser();
		this.config = config;
		this.staticUrlPart = buildStaticUrlPart();
	}
	
	public OwnershipHttpClient( OwnershipClientConfig config , ProcessContext context ) {
		this( config ,
			  context.getMaxConcurrentTasks() , 
			  context.getMaxConcurrentTasks() );
	}
	
	protected String buildStaticUrlPart() {
		StringBuilder builder = new StringBuilder( config.getOwnershipApiUrl() )
									.append( "?" );
		config.getAdditionalQueryParameters().forEach( (String name, String value) -> {
			builder.append( name ).append( "=" ).append( value )
				   .append("&");
		});
		
		builder.append( config.getElementIdParamName() )
			   .append( "=" );
//			   .append( config.getElementIdPrefix() );
		
		return builder.toString();
	}
	
	public String buildRequestUrl( String elementId ) {
		StringBuilder urlBuilder = new StringBuilder( this.staticUrlPart );
		
		if( !config.getElementIdPrefix().isEmpty() )
			urlBuilder.append( config.getElementIdPrefix() );
		
		return urlBuilder.append( elementId )
						 .toString();
	}
	
	public String buildRequestUrl( String elementIdPrefix , String elementId ) {
		return new StringBuilder( this.staticUrlPart )
					.append( elementIdPrefix )
					.append( elementId )
					.toString();
	}
	
	@Override
	public JsonElement getEnrichmentData( String elementId ) throws EnrichmentSourceException{
		String requestUrl = buildRequestUrl( elementId );
		HttpGet get = buildGetRequest( requestUrl );
		HttpResponse ownershipResponse = executeRequest( get );
//		HttpResponse ownershipResponse = fetchOwnershipData( requestUrl );
		
		return processResponseBody( ownershipResponse );
	}
	
	@Override
	public JsonElement getEnrichmentData( String elementId , String elementIdPrefix ) throws EnrichmentSourceException{
		String requestUrl = buildRequestUrl( elementIdPrefix , elementId );
		HttpGet get = buildGetRequest( requestUrl );
		HttpResponse ownershipResponse = executeRequest( get );
		
		return processResponseBody( ownershipResponse );
	}
	
	protected HttpGet buildGetRequest( String requestUrl ) {
		return new HttpGet( requestUrl );
	}
	
	protected JsonObject processResponseBody( HttpResponse response ) throws EnrichmentSourceException{
		String responseBody;
		
		try {
			responseBody = responseHandler.handleResponse( response );
		} catch (ClientProtocolException e) {
			throw new EnrichmentSourceException( "ClientProtocolException while processing response body." , e );
		} catch (IOException e) {
			throw new EnrichmentSourceException( "IOException while processing response body." , e );
		}
		
		JsonElement ownershipResponse = parser.parse( responseBody );
		
		if( ownershipResponse.isJsonArray() ) {
			if( ownershipResponse.getAsJsonArray().size() == 0 )
				throw new EnrichmentSourceException( "The ownership response is an empty json array: " + ownershipResponse.toString() , 
													 new EnrichmentSourceException( "empty response" ) );
			
			ownershipResponse = ownershipResponse.getAsJsonArray().get(0);
		}
		
		if( !ownershipResponse.isJsonObject() )
			throw new EnrichmentSourceException( "The ownership response is not a valid json object: " + ownershipResponse.toString() , 
												 new EnrichmentSourceException( "invalid response" ) );
		
		JsonObject responseObj = ownershipResponse.getAsJsonObject();
		JsonObject filteredResponseObj = new JsonObject();
		
		if( !config.getOwnershipFields().isEmpty() ) {
			responseObj.keySet().stream().forEach( (String prop) -> {
				if( config.ownershipFields.contains( prop ) ) {
					if( config.fieldsMapping.containsKey( prop ) ) {
						filteredResponseObj.add( config.fieldsMapping.get( prop ) , responseObj.get( prop ) );
					} else {
						filteredResponseObj.add( prop , responseObj.get( prop ) );
					}
				}
			});
		} else { //return the full response object if there's no filtering set
			return responseObj;
		}
		
		return filteredResponseObj;
	}

}
