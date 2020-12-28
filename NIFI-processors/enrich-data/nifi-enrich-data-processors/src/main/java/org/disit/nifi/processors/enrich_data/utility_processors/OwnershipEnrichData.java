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

package org.disit.nifi.processors.enrich_data.utility_processors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.ConfigurationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.disit.nifi.processors.enrich_data.EnrichmentSourceServiceValidators;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.logging.LoggingUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

@Tags({"snap4city" , "ownership" , "enrichment","enrich"})
@CapabilityDescription("This processor enriches incoming flow files payload with ownership data." )
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({ 
					@WritesAttribute(attribute="", description="") ,
				  })

public class OwnershipEnrichData extends AbstractProcessor {

	// Descriptors
	public static final PropertyDescriptor OWNERSHIP_CLIENT_SERVICE = new PropertyDescriptor
			.Builder().name( "OWNERSHIP_CLIENT_SERVICE" )
			.displayName( "Ownership Client Service" )
			.identifiesControllerService( EnrichmentSourceClientService.class )
			.description( "The client service to retrieve ownership data." )
			.required( false )
			.addValidator( EnrichmentSourceServiceValidators.STANDARD_ENRICHMENT_SOURCE_VALIDATOR )
			.build();
	
	public static final PropertyDescriptor DEVICE_ID_NAME = new PropertyDescriptor
            .Builder().name( "DEVICE_ID_NAME" )
            .displayName( "Device Id Name" )
            .description( "The name of the JSON field containing the device id. The value of such field will be concatenated to the 'Service URI Prefix' to obtain the full Service URI to pass to Servicemap." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
	
	public static final PropertyDescriptor USE_DEFAULTS_ON_OWNERSHIP_SERVICE_ERROR = new PropertyDescriptor
			.Builder().name( "USE_DEFAULTS_ON_OWNERSHIP_SERVICE_ERROR" )
			.displayName( "Use default properties on ownership service error" )
			.description( "Use the default ownership properties on ownership service error if 'true'.\nRoutes the flow file to failure if 'false'." )
			.required( true )
			.allowableValues( "true" , "false" )
			.defaultValue( "true" )
			.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
			.build();
	
	public static final PropertyDescriptor DEFAULT_OWNERSHIP_PROPERTIES = new PropertyDescriptor
            .Builder().name( "DEFAULT_OWNERSHIP_PROPERTIES" )
            .displayName( "Default ownership properties." )
            .description( "A JsonObject containing the default properties to enrich with, if the ownership service cannot retrieve a valid ownership response." )
            .required( false )
            .addValidator(Validator.VALID)
            .build();
	
	public static final PropertyDescriptor ADDITIONAL_DEFAULT_OWNERSHIP_PROPERTIES = new PropertyDescriptor
            .Builder().name( "ADDITIONAL_DEFAULT_OWNERSHIP_PROPERTIES" )
            .displayName( "Additional default ownership properties." )
            .description( "A JsonObject containing the default properties to enrich with, wich are inserted if not present in the ownership response (or in the default ownership properties)." )
            .required( false )
            .addValidator(Validator.VALID)
            .build();
	
	// Relationships 
    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name("SUCCESS_RELATIONSHIP")
            .description("The correctly enriched flow files will be routed to this relationship." )
            .build();
    
    public static final Relationship FAILURE_RELATIONSHIP = new Relationship.Builder()
            .name("FAILURE_RELATIONSHIP")
            .description("Flow files which cannot be correctly enriched will be routed to this relationship." )
            .build();

    public static final Relationship RETRY_RELATIONSHIP = new Relationship.Builder()
    		.name( "retry" )
    		.description( "Flow files which cannot be correctly enriched but are considered retriable will be routed to this relationship.\nA flow file is considered retriable if the cause of the failure is an enrichment service unavailability." )
    		.build();
	
	// Descriptors and relationships sets
	private List<PropertyDescriptor> descriptors;
	private Set<Relationship> relationships;
	
	private EnrichmentSourceClientService ownershipClientService;
    private EnrichmentSourceClient ownershipClient;
    
    // Logger
    private ComponentLog logger;
    
    // GSON parser
    private JsonParser parser;
    
    // Properties
    private String deviceIdName;
    private boolean useDefaults;
    private JsonObject defaultOwnershipProperties;
    private JsonObject additionalDefaultOwnershipProperties;
    
    @Override
    protected void init( ProcessorInitializationContext context ) {
    	final List<PropertyDescriptor> descs = new ArrayList<>();
    	descs.add( OWNERSHIP_CLIENT_SERVICE );
    	descs.add( DEVICE_ID_NAME );
    	descs.add( USE_DEFAULTS_ON_OWNERSHIP_SERVICE_ERROR );
    	descs.add( DEFAULT_OWNERSHIP_PROPERTIES );
    	descs.add( ADDITIONAL_DEFAULT_OWNERSHIP_PROPERTIES );
    	this.descriptors = Collections.unmodifiableList( descs );
    	
    	final Set<Relationship> rels = new HashSet<>();
    	rels.add( SUCCESS_RELATIONSHIP );
    	rels.add( FAILURE_RELATIONSHIP );
    	rels.add( RETRY_RELATIONSHIP );
    	this.relationships = Collections.unmodifiableSet( rels );
    }
	
    @Override
    public Set<Relationship> getRelationships(){
    	return this.relationships;
    }
    
    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }
    
    @OnScheduled
    public void onScheduled(final ProcessContext context) throws ConfigurationException{
    	
    	this.logger = getLogger();
    	this.parser = new JsonParser();
    	
    	// Ownership controller service and client
    	this.ownershipClientService = context.getProperty( OWNERSHIP_CLIENT_SERVICE )
    										 .asControllerService( EnrichmentSourceClientService.class );
    	
    	try {
			this.ownershipClient = this.ownershipClientService.getClient( context );
		} catch (InstantiationException e) {
			String reason = "Unable to obtain Ownership client instance from the controller service.";
			
			LoggingUtils.produceErrorObj( reason )
						.withExceptionInfo( e )
						.logAsError( logger );
			
			throw new ConfigurationException( reason );
		}
    	
    	this.deviceIdName = context.getProperty( DEVICE_ID_NAME )
    							   .getValue();
    	
    	
    	if( context.getProperty( USE_DEFAULTS_ON_OWNERSHIP_SERVICE_ERROR )
    			   .getValue().equals( "true" ) ) {
    		this.useDefaults = true;
    	}else {
    		this.useDefaults = false;
    	}
    	
		String defaultPropsValue = context.getProperty( DEFAULT_OWNERSHIP_PROPERTIES ) 
	  			  						  .getValue();
		String addDefaultPropValue = context.getProperty( ADDITIONAL_DEFAULT_OWNERSHIP_PROPERTIES ) 
					  					    .getValue();
		
		if( defaultPropsValue == null ||  defaultPropsValue.isEmpty() ) {
			defaultPropsValue = "{}";
		}
		
		if( addDefaultPropValue == null ||  addDefaultPropValue.isEmpty() ) {
			addDefaultPropValue = "{}";
		}
	
		try {
	    	JsonElement defaultProps = this.parser.parse( defaultPropsValue );
	    	if( !defaultProps.isJsonObject() )
	    		throw new ConfigurationException( String.format( "'%s' property value is not a valid JsonObject" , DEFAULT_OWNERSHIP_PROPERTIES.getName() ) );
	    	this.defaultOwnershipProperties = defaultProps.getAsJsonObject();
    	} catch( JsonParseException e ) {
    		throw new ConfigurationException( String.format( "JsonParseException while parsing '%s' property value: %s" , 
    											DEFAULT_OWNERSHIP_PROPERTIES.getName() , e.getMessage() ) );
    	}
    
	    try {
	    	JsonElement addDefaultProps = this.parser.parse( addDefaultPropValue );
	    	if( !addDefaultProps.isJsonObject() )
	    		throw new ConfigurationException( String.format( "'%s' property value is not a valid JsonObject" , ADDITIONAL_DEFAULT_OWNERSHIP_PROPERTIES.getName() ) );
	    	this.additionalDefaultOwnershipProperties = addDefaultProps.getAsJsonObject();
    	} catch( JsonParseException e ) {
    		throw new ConfigurationException( String.format( "JsonParseException while parsing '%s' property value: %s" , 
											ADDITIONAL_DEFAULT_OWNERSHIP_PROPERTIES.getName() , e.getMessage() ) );
    	}
    }
    
	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if( flowFile == null )
			return;
		
		// Get flow file content as a byte stream
        final ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();
        session.exportTo( flowFile , contentBytes );
        String flowFileContent = contentBytes.toString();
		
        // Content parsing
        JsonElement rootEl = parser.parse( flowFileContent );
        
        // Base validation
        //
        if( !rootEl.isJsonObject() ) { // Check if the flow file content is a valid JSON object
        	String reason = String.format( "Flow file content is not a valid JsonObject" );
			routeToFailure(session, flowFile, reason, rootEl);
			return;
        }
        
        // Root object containing the ff data
    	JsonObject rootObject = rootEl.getAsJsonObject();
    	
    	if( !rootObject.has( this.deviceIdName ) ) {
    		String reason = String.format( "The JsonObject in the flow file content doesn't contain the  field '%s'." ,  this.deviceIdName );
    		routeToFailure(session, flowFile, reason, rootEl);
			return;
    	}	
    	
    	String deviceId = rootObject.get( this.deviceIdName )
    								.getAsString();
    	
    	// Get ownership data
    	JsonObject ownershipResponseObject;
		try {
			JsonElement ownershipResponseRootEl = ownershipClient.getEnrichmentData( deviceId );
			ownershipResponseObject = ownershipResponseRootEl.getAsJsonObject();
		} catch (EnrichmentSourceException e) {
			String reason = "EnrichmentSourceException while retrieving ownership data";
			if( !canDefault(e) ) {
				routeToFailure(session, flowFile, reason, rootEl);
				return;
			} else {
				// Default ownership
				reason = String.format( "%s: %s. Using default ownership properties." , reason , e.getCause() );
				LoggingUtils.produceErrorObj( reason , rootEl )
							.logAsWarning( logger );
				ownershipResponseObject = this.defaultOwnershipProperties;
			}
		}
	
		// Enrich content with ownership data
		ownershipResponseObject.entrySet().forEach( 
			(Map.Entry<String , JsonElement> ownershipProp) -> {
				rootObject.add( ownershipProp.getKey() , ownershipProp.getValue() );
		});

		additionalDefaultOwnershipProperties.entrySet().forEach( 
			(Map.Entry<String , JsonElement> addOwnershipProp) -> {
				if( !rootObject.has( addOwnershipProp.getKey() ) ) {
					rootObject.add( addOwnershipProp.getKey() , addOwnershipProp.getValue() );
				}
			}
		);
		
		// Write to flow file and route to success
		session.write( flowFile , new OutputStreamCallback() {
			
			@Override
			public void process(OutputStream out) throws IOException {
				out.write( rootObject.toString().getBytes() );
			}
		});
		
		session.transfer( flowFile , SUCCESS_RELATIONSHIP );
	
	}
	
	private void routeToFailure( ProcessSession session , FlowFile flowFile , String reason , JsonElement rootEl ) {
		LoggingUtils.produceErrorObj( reason , rootEl )
					.withProperty( "ff-uuid" , flowFile.getAttribute( "uuid" ) )
					.logAsError( logger );
		flowFile = session.putAttribute( flowFile , "failure" , reason );
		session.transfer( flowFile , FAILURE_RELATIONSHIP );
	}
	
	private boolean canDefault( Throwable e ) {
		
		if( !this.useDefaults )
			return false;
		
		Throwable cause = e.getCause();
		if( cause instanceof HttpResponseException ) {
			HttpResponseException httpCause = (HttpResponseException) cause;
			int statusCode = httpCause.getStatusCode();
			
			if( statusCode == HttpServletResponse.SC_NOT_FOUND )
				return true;
			
		}
		
		if( e instanceof EnrichmentSourceException ) {
			return true;
		}
		
//		if( cause instanceof HttpHostConnectException )
//    		return false;
		
		return false;
	}

}
