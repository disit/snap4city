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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
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
import org.disit.nifi.processors.enrich_data.EnrichDataConstants;
import org.disit.nifi.processors.enrich_data.EnrichDataValidators;
import org.disit.nifi.processors.enrich_data.EnrichmentSourceServiceValidators;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceUpdater;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceUpdaterService;
import org.disit.nifi.processors.enrich_data.logging.LoggingUtils;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderService;

import com.google.common.net.MediaType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

@Tags({"snap4city" , "servicemap" , "update"})
@CapabilityDescription("This processor performs updates on an enrichment source.")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({ 
					@WritesAttribute(attribute="", description="") ,
				  })

public class UpdateEnrichmentSource extends AbstractProcessor {

	private static final Validator ENRICHMENT_SOURCE_UPDATER_SERVICE_VALIDATOR = new Validator() {
		@Override
		public ValidationResult validate(String subject, String input, ValidationContext context) {
			ValidationResult.Builder builder = new ValidationResult.Builder();
			
			try {
				context.getProperty( ENRICHMENT_SOURCE_UPDATER_SERVICE )
					   .asControllerService( EnrichmentSourceUpdaterService.class );
				builder.subject( subject ).explanation( "Valid EnrichmentSourceUpdaterService." )
					   .valid( true );
			} catch( IllegalArgumentException e ) {
				builder.subject( subject ).explanation( "Invalid EnrichmentSourceUpdaterService." )
					   .valid( false );
			}
			
			return builder.build();
		}
	};
	
	// Controller Services descriptors
	public static final PropertyDescriptor ENRICHMENT_SOURCE_UPDATER_SERVICE = new PropertyDescriptor
			.Builder().name( "ENRICHMENT_SOURCE_UPDATER_SERVICE" )
			.displayName( "Enrichment Source Updater Service" )
			.identifiesControllerService( EnrichmentSourceUpdaterService.class )
			.description( "The client service which identifies the enrichment source to update." )
			.required( true )
			.addValidator( ENRICHMENT_SOURCE_UPDATER_SERVICE_VALIDATOR )
			.build();
		
	
	// Descriptors
	public static final PropertyDescriptor ENDPOINT = new PropertyDescriptor
			.Builder().name( "ENDPOINT" )
			.displayName( "Endpoint" )
			.description( "The endpoint part of the servicemap url to perform the request,\n for example \"move\" combined with a \"http://servicemap.org\" configured in the Enrichment source updater service\n will result in a call to \"http://servicemap.org/move\" " )
			.required(true)
			.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
			.build();
	
	
	public static final PropertyDescriptor DEVICE_ID_NAME = new PropertyDescriptor
            .Builder().name( "DEVICE_ID_NAME" )
            .displayName( "Device Id Name" )
            .description( "The name of the JSON field containing the device id. The value of such field will be concatenated to the 'Service URI Prefix' to obtain the full Service URI to pass to Servicemap." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
	
	public static final PropertyDescriptor CONDITION = new PropertyDescriptor
            .Builder().name( "CONDITION" )
            .displayName( "Condition on content" )
            .description( "This property specifies a condition to be met on the json flow file content to perform the update.\nThis property must be a json object which will be checked against the incoming flow files content. If this object is fully contained in the flow file one the condition is met.\nLeave this property value blank for no condition on the flow file content." )
            .required(false)
            .defaultValue( "" )
            .addValidator( EnrichDataValidators.jsonPropertyValidator(true) )
            .build();
	
	public static final PropertyDescriptor ATTRIBUTES_CONDITION = new PropertyDescriptor
			.Builder().name( "ATTRIBUTES_CONDITION" )
			.displayName( "Condition on attributes" )
			.description( "This property specifies a condition to be met on the json flow file content to perform the update.\nThis property must be a json object which will be checked against the incoming flow files attributes.\nEvery property contained in the object must be contained as flow file attribute in the incoming flow file for the condition to be met.\nNote: because the flow file attribute values are always strings the value of the top-level properties in the condition object are converted to strings, regardless of their effective json type.\nLeave this property blank for no condition on the flow file attributes." )
			.required(false)
			.defaultValue( "" )
			.addValidator( EnrichDataValidators.jsonPropertyValidator(true) )
			.build();
	
	public static final PropertyDescriptor REQ_RESOURCE_URI_NAME = new PropertyDescriptor
			.Builder().name( "REQ_RESOURCE_URI_NAME" )
			.description( "This property set the name of the attribute for the resource uri in the request json body.\nIf this property is left empty the uri is not embedded in the request object." )
			.displayName( "Pass resource uri as" )
			.required( false )
			.addValidator(Validator.VALID)
			.build();
	
	public static final String DEFAULT_TIMESTAMP_FIELD_NAME = "date_time";
	public static final PropertyDescriptor TIMESTAMP_FIELD_NAME = new PropertyDescriptor
			.Builder().name( "TIMESTAMP_FIELD_NAME" )
			.description( "The name of the timestamp attribute inside the input flow file content. Used to augment the content of the flow files emitted on the 'Performed updates' relationship." )
			.displayName( "Timestamp field name" )
			.required( false )
			.defaultValue( DEFAULT_TIMESTAMP_FIELD_NAME )
			.addValidator( Validator.VALID )
			.build();
	
	public static final String DEFAULT_STATIC_AUGMENT_PERFORMED_UPDATES = 
			"{\"value_name\":\"__location\",\"value_type\":\"location\"}";
	public static final PropertyDescriptor STATIC_AUGMENT_PERFORMED_UPDATES = new PropertyDescriptor
			.Builder().name( "STATIC_AUGMENT_PERFORMED_UPDATES" )
			.description( "A static JSON object which is merged with the content of the flow files emitted on the 'Performed updates' realtionships." )
			.displayName( "Augment performed updates" )
			.required( false )
			.defaultValue( DEFAULT_STATIC_AUGMENT_PERFORMED_UPDATES )
			.addValidator( EnrichDataValidators.jsonPropertyValidator(true) )
			.build();
	
	// Relationships 
    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name("Success")
            .description("The correctly enriched flow files will be routed to this relationship." )
            .build();
    
    public static final Relationship FAILURE_RELATIONSHIP = new Relationship.Builder()
            .name("Failure")
            .description("Flow files which cannot be correctly enriched will be routed to this relationship." )
            .build();
    
    public static final Relationship CONDITION_NOT_MET_RELATIONSHIP = new Relationship.Builder()
            .name("Condition Not Met")
            .description("Flow files which cannot be correctly enriched will be routed to this relationship." )
            .build();
    
    public static final Relationship PERFORMED_UPDATES_RELATIONSHIP = new Relationship.Builder()
    		.name("Performed updates")
    		.description( "Json objects containing the performed updates." )
    		.build();
    
    private static final Set<String> staticProperties = new HashSet<>( Arrays.asList(
    		"ENRICHMENT_SOURCE_UPDATER_SERVICE" ,
    		"ENDPOINT" ,
    		"DEVICE_ID_NAME" ,
    		"CONDITION",
    		"ATTRIBUTES_CONDITION" ,
    		"REQ_RESOURCE_URI_NAME" ,
    		"TIMESTAMP_FIELD_NAME",
    		"STATIC_AUGMENT_PERFORMED_UPDATES"
    	) 
    );
	
	// Descriptors and relationships sets
	private List<PropertyDescriptor> descriptors;
	private Set<Relationship> relationships;
	
	private EnrichmentSourceUpdaterService enrichmentSourceUpdaterService;
	private EnrichmentSourceUpdater enrichmentSourceUpdater;
    
    // Logger
    private ComponentLog logger;
    
    // Properties
    private String endpoint;
    private String deviceIdName;
    private boolean useCondition;
    private JsonObject conditionObj;
    private Map<String , String> attributesConditionMap;
    private String reqResourceUriName;
    private String timestampFieldName;
    private JsonObject staticAugPerformedUpdates;
    
    private boolean includeResourceUri;
    private boolean outputPerformedUpdates;
    
    private Map<String , String> nameMapping;
    private Map<String , List<String>> pathExpansions;
    private Map<String , List<String>> arrayUnpackings;
    
    @Override
    protected void init( ProcessorInitializationContext context ) {
    	final List<PropertyDescriptor> descs = new ArrayList<>();
    	descs.add( ENRICHMENT_SOURCE_UPDATER_SERVICE );
    	descs.add( ENDPOINT );
    	descs.add( DEVICE_ID_NAME );
    	descs.add( CONDITION );
    	descs.add( ATTRIBUTES_CONDITION );
    	descs.add( REQ_RESOURCE_URI_NAME );
    	descs.add( TIMESTAMP_FIELD_NAME );
    	descs.add( STATIC_AUGMENT_PERFORMED_UPDATES );
    	this.descriptors = Collections.unmodifiableList( descs );
    	
    	final Set<Relationship> rels = new HashSet<>();
    	rels.add( SUCCESS_RELATIONSHIP );
    	rels.add( FAILURE_RELATIONSHIP );
    	rels.add( CONDITION_NOT_MET_RELATIONSHIP );
    	rels.add( PERFORMED_UPDATES_RELATIONSHIP );
    	this.relationships = Collections.unmodifiableSet( rels );
    }
	
    @Override
    public Set<Relationship> getRelationships(){
    	return this.relationships;
    }
    
    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return this.descriptors;
    }
    
    /**
     * This method provides the dynamic attributes descriptor.
     */
    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName ) {
    	
    	return new PropertyDescriptor.Builder().name( propertyDescriptorName )
    							     .addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    							     .description( "A dynamic property specifies a field in the incoming flow file json content to be passed in the update request body.\nEach property name must be a valid path in the incoming flow file json content. Properties of nested objects can be specified with a forward slash-path syntax (Ex: \"foo/bar/\") .\nThe value of the specified attribute is added to the request json body named as specified by the value of the dynamic property.\nTo unpack arrays into named properties specify an array as property value,\nfor example, to unpack the field 'coordinates:[19.2,16.8]' in two separate attributes for the request body\n use '[latitude, longitude]' as property value." )
    							     .dynamic( true )
    							     .required( false )
    							     .build();
    }
    
    protected Map<String , List<String>> parseDynamicProperties(ProcessContext context){
    	Map<String , List<String>> dynamicProperties = new ConcurrentHashMap<>();
    	
    	context.getAllProperties().forEach( (String k, String v) -> {
    		if( !staticProperties.contains( k ) ) {
    			dynamicProperties.put( 
    				k , 
    				Arrays.asList( v.split("/") ).stream().map( s -> { return s.trim(); } )
				  	      .collect( Collectors.toList() ) 
    			);
    		}
    	});
    	
    	return dynamicProperties;
    }
    
    protected void configureMappings(ProcessContext context) {
    	this.nameMapping = new ConcurrentHashMap<String, String>();
    	this.pathExpansions = new ConcurrentHashMap<String, List<String>>();
    	this.arrayUnpackings = new ConcurrentHashMap<String, List<String>>();
    	
    	context.getAllProperties().forEach( (String k , String v)-> { 
    		if( !staticProperties.contains( k ) ) {
    			this.nameMapping.put( k , v ); 
    			this.pathExpansions.put( k ,
    				Arrays.asList( k.split("/") ).stream().map( s -> { return s.trim(); } )
    					  .collect( Collectors.toList() )
    			);
    			// Check if array unpacking
    			try { 
    				JsonElement jsonV = JsonParser.parseString( v );
    				if( jsonV.isJsonArray() ) {
    					JsonArray array = jsonV.getAsJsonArray();
    					if( array.size() > 0 ) {
    						List<String> unpacking = new ArrayList<>();
    						array.forEach( (JsonElement element) -> {
    							if( element.isJsonPrimitive() ) {
    								unpacking.add( element.getAsString() );
    							}
    						});
    						this.arrayUnpackings.put( k , unpacking );
    					}
    				}
    			} catch( JsonParseException ex ) { } //ignore if not json array
    		}
    	});
    }
    
    @OnScheduled
    public void onScheduled(final ProcessContext context) throws ConfigurationException{
    	
    	this.logger = getLogger();
    	
    	// Enrichment source updater service and updater client
    	this.enrichmentSourceUpdaterService = context.getProperty( ENRICHMENT_SOURCE_UPDATER_SERVICE )
    										 .asControllerService( EnrichmentSourceUpdaterService.class );
    	this.endpoint = context.getProperty( ENDPOINT ).getValue();
    	
    	try {
			this.enrichmentSourceUpdater = this.enrichmentSourceUpdaterService
											   .getUpdaterClient( this.endpoint , context );
		} catch (InstantiationException e) {
			String reason = "Unable to obtain an EnrichmentSourceUpdater instance from the controller service.";
			
			LoggingUtils.produceErrorObj( reason )
						.withExceptionInfo( e )
						.logAsError( logger );
			
			throw new ConfigurationException( reason );
		}
    	
    	this.deviceIdName = context.getProperty( DEVICE_ID_NAME )
    							   .getValue();
    	
    	// Determine condition on ff payload
    	if( !context.getProperty( CONDITION ).getValue().isEmpty() ) {
    		this.conditionObj = JsonParser.parseString( context.getProperty( CONDITION ).getValue() ).getAsJsonObject();
    		this.useCondition = true;
    	}else {
    		this.useCondition = false;
    	}
    	
    	// Determine condition of ff attributes
    	this.attributesConditionMap = new HashMap<>();
    	if( !context.getProperty( ATTRIBUTES_CONDITION ).getValue().isEmpty() ) {
    		JsonObject attributesConditionObj = JsonParser.parseString( context.getProperty( ATTRIBUTES_CONDITION ).getValue() )
					  								  .getAsJsonObject();
    		attributesConditionObj.entrySet().forEach( (Map.Entry<String , JsonElement> condEl) -> {
    			attributesConditionMap.put( condEl.getKey() , condEl.getValue().getAsString() );
    		});
    	}
    	
    	
    	//Resource uri 
    	if( !context.getProperty( REQ_RESOURCE_URI_NAME ).getValue().isEmpty() ) {
    		this.includeResourceUri = true;
    		this.reqResourceUriName = context.getProperty( REQ_RESOURCE_URI_NAME ).getValue();
    	}else {
    		this.includeResourceUri = false;
    	}
    	
    	// Dynamic properties
    	configureMappings( context );
    	
    	// Performed updates
    	// output performed updates only if the dedicated relationship
    	// is connected to another component
    	if( context.hasConnection( PERFORMED_UPDATES_RELATIONSHIP ) ) {
    		this.outputPerformedUpdates = true;
    	}else {
    		this.outputPerformedUpdates = false;
    	}
    	// Performed updates augmentation
    	if( context.getProperty( TIMESTAMP_FIELD_NAME ).isSet() &&
			!context.getProperty( TIMESTAMP_FIELD_NAME ).getValue().isEmpty() ) {
    		this.timestampFieldName = context.getProperty( TIMESTAMP_FIELD_NAME ).getValue();
    	}
    	
    	if( context.getProperty( STATIC_AUGMENT_PERFORMED_UPDATES ).isSet() && 
    		!context.getProperty( STATIC_AUGMENT_PERFORMED_UPDATES ).getValue().isEmpty() ) {
    		try {
    			JsonElement staticAugEl = JsonParser.parseString( context.getProperty( STATIC_AUGMENT_PERFORMED_UPDATES ).getValue() );
    			if( staticAugEl.isJsonObject() )
    				this.staticAugPerformedUpdates = staticAugEl.getAsJsonObject();
    			else {
    				logger.warn( "The '" + STATIC_AUGMENT_PERFORMED_UPDATES.getDisplayName() + "' property content is not a valid JsonObject. Skipping augmentation." );
    				this.staticAugPerformedUpdates = null;
    			}
    		}catch( JsonParseException ex ) { 
    			logger.warn( "The '" + STATIC_AUGMENT_PERFORMED_UPDATES.getDisplayName() + "' property content is not a valid JsonObject. Skipping augmentation." );
    			this.staticAugPerformedUpdates = null;
    		}
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
        JsonElement rootEl;
        try {
        	rootEl = JsonParser.parseString( flowFileContent );
        }catch( JsonParseException ex ) {
        	routeToFailure( session , flowFile , "The flow file content is not valid JSON. JsonParseException: " + ex.getMessage() );
        	return;
        }
        
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
    	
    	// Check condition if any
    	//========================================
    	if( this.useCondition ) {
    		if( !checkContains( conditionObj , rootObject ) ) {
    			routeToConditionNotMet(session, flowFile, "Condition not met on flow file content." );
    			return;
    		}
    	}
    	
    	if( !this.attributesConditionMap.isEmpty() ) {
    		if( !checkAttributesCondition( flowFile ) ) {
    			routeToConditionNotMet(session, flowFile, "Condition not met on flow file attributes." );
    			return;
    		}
    	}
    	
    	String deviceId = rootObject.get( this.deviceIdName ).getAsString();
    	String resourceUri = flowFile.getAttribute( EnrichDataConstants.SERVICE_URI_OUTPUT_NAME );
    	if( resourceUri == null ) { // If the serviceUri is not contained in the FF attributes uses the default one
    		resourceUri = enrichmentSourceUpdater.buildResourceUri( deviceId );
    		flowFile = session.putAttribute( flowFile , EnrichDataConstants.SERVICE_URI_OUTPUT_NAME , resourceUri );
    	}
    	
    	// Build the update Json object
    	//========================================
    	JsonObject reqBodyObj = new JsonObject(); 
    	if( this.includeResourceUri ) { // include resource uri in the request payload
    		reqBodyObj.addProperty( this.reqResourceUriName , resourceUri );
    	}
    	
    	// Include all specified fields
    	try {
	    	pathExpansions.forEach( (String path , List<String> expansionList) -> {
	    		JsonElement value = getElementByPath( rootObject , expansionList );
	    		if( !arrayUnpackings.containsKey( path ) ) {
	    			reqBodyObj.add( this.nameMapping.get( path ) , value );
	    		}else { // Array unpacking field
	    			if( !value.isJsonArray() )
	    				throw new NoSuchElementException(
	    					String.format( "Cannot unpack value contained in '%s':'%s' as array." , 
	    								   path , value.toString() )
	    				);
	    			
	    			JsonArray array = value.getAsJsonArray();
	    			if( array.size() == 0 )
	    				throw new NoSuchElementException(
	    					String.format( "Found empty array while unpacking '%s':'%s'." , 
	    								   path , value.toString() )
	    				);
	    			
	    			List<String> unpacking = arrayUnpackings.get( path );
	    			if( array.size() >= unpacking.size() ) {
	    				
	    				for( int i=0 ; i < unpacking.size() ; i++ ) {
	    					reqBodyObj.add( unpacking.get(i) , array.get(i) );
	    				}
	    				
	    			}else {
	    				throw new NoSuchElementException(
	    					String.format( "Not enough values in array to unpack '%s':'%s'. Configured to be: '%s'." , 
	    								   path , value.toString() , unpacking.toString() )
	    				);
	    			}
	    				
	    		}
	    	});
    	}catch( NoSuchElementException ex ) {
    		routeToFailure(
    			session, flowFile, 
    			"Cannot extract some field from the incoming flow file object.", 
    			rootEl, ex );
    		return;
    	}
    	
    	// Perform update
    	//========================================
    	try {
			enrichmentSourceUpdater.performUpdate( reqBodyObj );
		} catch (EnrichmentSourceException e) {
			routeToFailure( 
				session , flowFile, 
				"Update request failed." , 
				reqBodyObj , e );
			return;
		}
		
    	// Produce performed update flow file
    	if( this.outputPerformedUpdates ) { 
	    	FlowFile updateFlowFile = produceUpdateFlowFile( session , flowFile , rootObject , reqBodyObj , deviceId , resourceUri );
	    	session.transfer( updateFlowFile , PERFORMED_UPDATES_RELATIONSHIP );
    	}
    	// Route to success
		session.putAttribute( flowFile , "Update request body" , reqBodyObj.toString() );
		session.transfer( flowFile , SUCCESS_RELATIONSHIP );
	}
	
	private FlowFile produceUpdateFlowFile( ProcessSession session , FlowFile inFlowFile , 
			JsonObject rootObject , JsonObject reqBodyObj , String deviceId , String resourceUri) {
		FlowFile updateFlowFile = session.clone( inFlowFile );
		
		JsonObject updateValueObj = new JsonObject();
		for( Map.Entry<String , JsonElement> reqEntry : reqBodyObj.entrySet() ) {
			if( reqEntry.getKey() != this.deviceIdName && 
				reqEntry.getKey() != this.reqResourceUriName ) {
				updateValueObj.add( reqEntry.getKey() , reqEntry.getValue() );
			}
		}
		JsonObject updateObject = new JsonObject();
		updateObject.add( "value_obj" , updateValueObj );
		
		// TODO: static names?
		updateObject.addProperty( "sensorID" , deviceId );
		updateObject.addProperty( EnrichDataConstants.SERVICE_URI_OUTPUT_NAME , resourceUri );
		
		// date_Time
		if( this.timestampFieldName != null && inFlowFile.getAttributes().containsKey( this.timestampFieldName ) ) {
			updateObject.addProperty( this.timestampFieldName , inFlowFile.getAttribute( timestampFieldName ) );
		}
		
		// static augmentation (value_name,value_type)
		if( this.staticAugPerformedUpdates != null && this.staticAugPerformedUpdates.size() > 0 ) {
			this.staticAugPerformedUpdates.entrySet()
				.stream().forEach( (Map.Entry<String, JsonElement> entry)->{
					updateObject.add( entry.getKey() , entry.getValue() );
				});
		}
		
		// attributes
		updateFlowFile = session.putAttribute( updateFlowFile , EnrichDataConstants.MIME_TYPE_ATTRIBUTE_NAME , MediaType.JSON_UTF_8.toString() );
		for( Map.Entry<String,JsonElement> entry : this.staticAugPerformedUpdates.entrySet() ) {
			updateFlowFile = session.putAttribute( 
				updateFlowFile , entry.getKey() , entry.getValue().getAsString() );
		}
		
		updateFlowFile = session.write( updateFlowFile , new OutputStreamCallback() {
			@Override
			public void process(OutputStream out) throws IOException {
				out.write( updateObject.toString().getBytes() );
			}
		});
		return updateFlowFile;
	}
	
	private boolean checkContains( JsonObject ref , JsonObject container ) {
		for( String prop : ref.keySet() ) {
			if( !container.has( prop ) ) 
				return false;
			if( ref.get(prop).isJsonPrimitive() || ref.get(prop).isJsonArray() || ref.get(prop).isJsonNull() ) {
				if( !container.get( prop ).equals( ref.get( prop ) ) ) return false; 
			}else if( ref.get(prop).isJsonObject() ){
				if( !container.get(prop).isJsonObject() ) return false;
				return checkContains( ref.get(prop).getAsJsonObject() , 
						               container.get( prop ).getAsJsonObject() );
			}
		}
		return true;
	}
	
	private boolean checkAttributesCondition( FlowFile ff ) {
		for( Map.Entry<String, String> condEl : this.attributesConditionMap.entrySet() ) {
			String attrVal = ff.getAttribute( condEl.getKey() );
			if( attrVal == null || !attrVal.equals( condEl.getValue() ) )
				return false;
		}
		return true;
	}
	
	private JsonElement getElementByPath( JsonObject object , List<String> path ) throws NoSuchElementException{
		JsonObject current = object;
		for( int i = 0 ; i < path.size()-1 ; i++ ) {
			if( !current.has( path.get(i) ) )
				throw new NoSuchElementException( "Intermediate path element '" + path.get(i) + "': no such element." );
			
			if(current.get( path.get(i) ).isJsonObject() ) {
				current = current.get( path.get(i) ).getAsJsonObject();
			}else {
				throw new NoSuchElementException( "Intermediate path element '" + path.get(i) + "': found but not a json object." );
			}
		}
		String lastEl = path.get( path.size()-1 );
		if( current.has( lastEl ) ) {
			return current.get( lastEl );
		}else {
			throw new NoSuchElementException( "'" + lastEl + "': no such element."  );
		}
		
	}
	
	private void routeToConditionNotMet( ProcessSession session , FlowFile flowFile , String reason ) {
		flowFile = session.putAttribute( flowFile , "failure" , reason );
		session.transfer( flowFile , CONDITION_NOT_MET_RELATIONSHIP );
	}
	
	
	private void routeToFailure( ProcessSession session , FlowFile flowFile , String reason ) {
		LoggingUtils.produceErrorObj( reason )
					.withProperty( "ff-uuid", flowFile.getAttribute("uuid") )
					.logAsError( logger );
		flowFile = session.putAttribute( flowFile , "failure" , reason );
		session.transfer( flowFile , FAILURE_RELATIONSHIP );
	}
	
	private void routeToFailure( ProcessSession session , FlowFile flowFile , String reason , JsonElement rootEl ) {
		LoggingUtils.produceErrorObj( reason , rootEl )
					.withProperty( "ff-uuid" , flowFile.getAttribute( "uuid" ) )
					.logAsError( logger );
		flowFile = session.putAttribute( flowFile , "failure" , reason );
		session.transfer( flowFile , FAILURE_RELATIONSHIP );
	}
	
	private void routeToFailure( ProcessSession session , FlowFile flowFile , String reason , JsonElement rootEl , Throwable exception ) {
		LoggingUtils.produceErrorObj( reason , rootEl )
					.withExceptionInfo( exception )
					.withProperty( "ff-uuid" , flowFile.getAttribute( "uuid" ) )
					.logAsError( logger );
		flowFile = session.putAttribute( flowFile , "failure" , reason );
		session.transfer( flowFile , FAILURE_RELATIONSHIP );
	}

}
