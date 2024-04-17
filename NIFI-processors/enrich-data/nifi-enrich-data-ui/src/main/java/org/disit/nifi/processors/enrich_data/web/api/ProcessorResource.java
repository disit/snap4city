/*
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
package org.disit.nifi.processors.enrich_data.web.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.web.ComponentDescriptor;
import org.apache.nifi.web.ComponentDetails;
import org.apache.nifi.web.NiFiWebConfigurationContext;
import org.apache.nifi.web.NiFiWebConfigurationRequestContext;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.EnrichDataProperties;
import org.disit.nifi.processors.enrich_data.web.api.tester.EnrichDataWebTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.test.custom_ui.test_processor.TestProcessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * EnrichData web service resource.
 * 
 * @author panconi.christian@gmail.com
 */
@Path( "/enrich-data" )
public class ProcessorResource extends AbstractStandardResource {
	
	private static final Logger logger = LoggerFactory.getLogger( ProcessorResource.class );

	/**
	 * Exposes an endpoint to run a test using the specified tests configurations.
	 * 
	 * @param processorId 
	 * @param groupId
	 * @param body
	 * @return
	 */
	@POST
	@Produces({MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("/test")
	public Response remoteTest( @QueryParam("processorId") final String processorId , @QueryParam("groupId") final String groupId , String body ) {		
		JsonParser parser = new JsonParser();
		JsonElement bodyEl;
		try {
			bodyEl = parser.parse( body );
		}catch( JsonParseException ex ) {
			final Response.ResponseBuilder response = ProcessorWebUtils.applyCacheControl( 
				Response.status(400 , "{\"error\":\"The POST request body cannot be parsed as valid JSON.\"}" ) 
			);
			return response.build();
		}
		
		if( bodyEl.isJsonObject() ) {
			
			// Test processor properties
			JsonObject testConfigs = bodyEl.getAsJsonObject();
			if( !testConfigs.has( "properties") || !testConfigs.get("properties").isJsonObject() ) {
				final Response.ResponseBuilder response = ProcessorWebUtils.applyCacheControl( 
					Response.status(400 , "{\"error\":\"The 'properties' field in the POST request body is missing or not of type 'object'.\"}" ) 
				);
				return response.build();
			}
			
			if( !testConfigs.has("inputFlowFile") || !testConfigs.get("inputFlowFile").isJsonObject() ) {
				final Response.ResponseBuilder response = ProcessorWebUtils.applyCacheControl( 
						Response.status(400 , "{\"error\":\"The 'inputFlowFile' field in the POST request body is missing or not of type 'object'.\"}" ) 
				);
				return response.build();
			}
			
			// Instance the web tester
			try {
				
				Map<String,String> registryVariables = new HashMap<>();
				if( testConfigs.has("variableRegistry") && testConfigs.get("variableRegistry").isJsonObject() ) {
					JsonObject variableRegistryObj = testConfigs.get("variableRegistry")
																.getAsJsonObject();
					variableRegistryObj.keySet().forEach( (String name) -> {
						registryVariables.put( name , variableRegistryObj.get(name).getAsString() );
					});
				}
				
				EnrichDataWebTester webTester = new EnrichDataWebTester(
					processorId , groupId , 
					testConfigs.get("properties").getAsJsonObject() ,
					registryVariables
				);
				
				// Collect ff attributes
				Map<String , String> ffAttributes = testConfigs
					.get( "inputFlowFile" ).getAsJsonObject()
				    .get("attributes").getAsJsonObject()
				    .entrySet().stream()
				    .collect(
				    	Collectors.toMap( 
				    		Map.Entry<String,JsonElement>::getKey ,
				           (Map.Entry<String , JsonElement> entry) -> { return entry.getValue().getAsString(); } 
				        )
				    );
				
				// Collect ff content
				String ffContent = testConfigs.get("inputFlowFile").getAsJsonObject()
								   			  .get("content").getAsString();

				//Run test
				JsonObject result = webTester.runTest( ffContent , ffAttributes );
				
				//Build response
				final Response.ResponseBuilder response = ProcessorWebUtils.applyCacheControl( Response.ok( result.toString() ) );
				return response.build();
			}catch( AssertionError ae ) {
				logger.error( ExceptionUtils.getStackTrace( ( ae ) ) );
				// AssertionError during test configuration / run
				final Response.ResponseBuilder response = ProcessorWebUtils.applyCacheControl( 
					Response.status(400 ,  "{\"error\":\"" + ae.getMessage() + "\"}" ) 
				);
				return response.build();
			}
		}else {
			// POST body is not a JSON object.
			final Response.ResponseBuilder response = ProcessorWebUtils.applyCacheControl( 
				Response.status(400 , "{\"error\":\"The POST request body is not a JSON object.\"}" ) 
			);
			return response.build();
		}
	}
	
	/**
	 * Exposes an endpoint to set processor properties.
	 * 
	 * @param processorId 
	 * @param revisionId 
	 * @param clientId cliend id.
	 * @param isDisconnectionAcknowledged 
	 * @param properties
	 * @return
	 */
	@PUT
	@Produces({MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("/properties")
	public Response setProperties( @QueryParam("processorId") final String processorId , 
								   @QueryParam("revisionId") final Long revisionId , 
								   @QueryParam("clientId") final String clientId , 
								   @QueryParam("disconnectedNodeAcknowledged") final Boolean isDisconnectionAcknowledged ,
								   Map<String , String> properties ) {
		
		final NiFiWebConfigurationContext nifiWebContext = getWebConfigurationContext();
		final NiFiWebConfigurationRequestContext nifiRequestContext = ProcessorWebUtils.getRequestContext(processorId, revisionId , clientId, isDisconnectionAcknowledged, request);
		final ComponentDetails componentDetails = nifiWebContext.updateComponent( nifiRequestContext , null , properties );
		final Response.ResponseBuilder response = ProcessorWebUtils.applyCacheControl( Response.ok(componentDetails) );
		return response.build();
	}
	
	/**
	 * Exposes an endpoint to retrieve processor details.
	 * 
	 * @param processorId
	 * @return
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Path("/details")
	public Response getDetails(@QueryParam("processorId") final String processorId ) {
		final NiFiWebConfigurationContext nifiWebContext = getWebConfigurationContext();
		final ProcessorComponentDetails componentDetails = detailsCorrection( 
			ProcessorWebUtils.getComponentDetails(nifiWebContext, processorId, request) ,
			EnrichData.getStaticDescriptors()
		);
		final Response.ResponseBuilder response = ProcessorWebUtils.applyCacheControl( Response.ok( componentDetails ) );
		return response.build();
	}
	
	/**
	 * Merge the ComponentDetails with informations from the
	 * given List of ProperyDescriptors.
	 * 
	 * @param details
	 * @param propertyDescriptors
	 * @return
	 */
	private ProcessorComponentDetails detailsCorrection( ComponentDetails details , List<PropertyDescriptor> propertyDescriptors ) {
		Map<String , PropertyDescriptor> staticDescriptors = new HashMap<>();
		propertyDescriptors.stream().forEach( (PropertyDescriptor p) -> {
			staticDescriptors.put( p.getName() , p );
		});
		
		Map<String , PropertyComponentDescriptor> descriptors = new HashMap<>();
		details.getDescriptors().forEach( (String name , ComponentDescriptor cd) -> {
			if( staticDescriptors.keySet().contains( name ) ) { // static property
				descriptors.put( 
					name , 
					PropertyComponentDescriptor.fromPropertyDescriptor( 
						staticDescriptors.get( name ) 
					) 
				);
			} else {
				descriptors.put( 
					name , 
					new PropertyComponentDescriptor.Builder()
						.name( cd.getName() )
						.description( cd.getDescription() != null && !cd.getDescription().isEmpty() ? 
										cd.getDescription() : EnrichDataProperties.DYNAMIC_PROPERTIES_DESCRIPTION )
						.displayName( cd.getDisplayName() )
						.isSensitive( false )
						.isDynamic( true )
						.isRequired( false )
						.isControllerService( false )
						.build()
				);
			}
		});
		
		return new ProcessorComponentDetails.Builder()
				.id( details.getId() )
				.name( details.getName() )
				.type( details.getType() )
				.annotationData( details.getAnnotationData() )
				.properties( details.getProperties() )
				.state( details.getState() )
				.validateErrors( details.getValidationErrors() )
				.supportsDynamicProperties( true )
				.dynamicPropertiesDescription( EnrichDataProperties.DYNAMIC_PROPERTIES_DESCRIPTION )
				.descriptors( descriptors )
				.build();
	}

}
