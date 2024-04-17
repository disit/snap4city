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

package org.disit.nifi.processors.enrich_data;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.naming.ConfigurationException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import org.disit.nifi.processors.enrich_data.enricher.Enricher;
import org.disit.nifi.processors.enrich_data.enricher.OnePhaseEnricher;
import org.disit.nifi.processors.enrich_data.enricher.TwoPhaseEnricher;
import org.disit.nifi.processors.enrich_data.enricher.converter.DeviceStateConverter;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.ServicemapSource;
import org.disit.nifi.processors.enrich_data.json_processing.JsonProcessing;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocator;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocatorException;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocatorService;
import org.disit.nifi.processors.enrich_data.locators.ResourceLocations;
import org.disit.nifi.processors.enrich_data.logging.LoggingUtils;
import org.disit.nifi.processors.enrich_data.logging.LoggingUtils.LoggableObject;
import org.disit.nifi.processors.enrich_data.output_producer.ElasticsearchBulkIndexingOutputProducer;
import org.disit.nifi.processors.enrich_data.output_producer.JsonOutputProducer;
import org.disit.nifi.processors.enrich_data.output_producer.OutputProducer;
import org.disit.nifi.processors.enrich_data.output_producer.SplitObjectOutputProducer;

import com.google.common.net.MediaType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

@Tags({"snap4city" , "servicemap" , "enrichment","enrich"})
@CapabilityDescription("This processor enirches incoming data from a broker subscription with informations retrieved from Servicemap." )
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({ 
	@WritesAttribute(attribute="requestUrl", description="The URL used by the processor for the request to Servicemap.") ,
	@WritesAttribute(attribute="deviceId", description="The parsed deviceId.") , 
	@WritesAttribute(attribute="timestampSource", description="Track from where the timestamp is picked from.")
})

public class EnrichData extends AbstractProcessor {
	
	private static List<PropertyDescriptor> propertyDescriptors;
    private static Set<Relationship> relationships;
    private static List<String> staticPropertyNames;
    static {
    	propertyDescriptors = Collections.unmodifiableList( Arrays.asList( 
			EnrichDataProperties.ENRICHMENT_SOURCE_CLIENT_SERVICE , 	// Enrichment sources
			EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE , 	// Enrichment sources
	        EnrichDataProperties.OWNERSHIP_BEHAVIOR , 					// Enrichment sources
	        EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE , 			// Enrichment sources
	        EnrichDataProperties.DEFAULT_OWNERSHIP_PROPERTIES , 		// Enrichment sources
	        EnrichDataProperties.DEVICE_ID_NAME ,						// Fields
	        EnrichDataProperties.DEVICE_ID_NAME_MAPPING ,				// Fields
	        EnrichDataProperties.DEVICE_ID_VALUE_PREFIX_SUBST ,			// Fields
	        EnrichDataProperties.TIMESTAMP_FIELD_NAME , 				// Timestamp
	        EnrichDataProperties.TIMESTAMP_FROM_CONTENT_PROPERTY_NAME , // Timestamp
	        EnrichDataProperties.TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE ,// Timestamp
	        EnrichDataProperties.TIMESTAMP_THRESHOLD ,					// Timestamp 
	        EnrichDataProperties.VALUE_FIELD_NAME ,						// Fields
	        EnrichDataProperties.ENRICHMENT_RESPONSE_BASE_PATH ,		// Enrichment sources
	        EnrichDataProperties.LATLON_PRIORITY ,						// Coordinates
	        EnrichDataProperties.ENRICHMENT_LAT_LON_PATH ,				// Coordinates
	        EnrichDataProperties.ENRICHMENT_LAT_LON_FORMAT ,			// Coordinates
	        EnrichDataProperties.INNER_LAT_LON_CONFIG ,					// Coordinates
	        EnrichDataProperties.ENRICHMENT_BEHAVIOR ,					// Enrichment sources
	        EnrichDataProperties.SRC_PROPERTY ,							// Other
	        EnrichDataProperties.KIND_PROPERTY ,						// Other
	        EnrichDataProperties.PURGE_FIELDS ,							// Fields
	        EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT ,				// Output
	        EnrichDataProperties.HASHED_ID_FIELDS ,						// Output
	        EnrichDataProperties.ES_INDEX ,								// Output
	        EnrichDataProperties.ES_TYPE ,								// Output
	        EnrichDataProperties.NODE_CONFIG_FILE_PATH ,				// Other
	        EnrichDataProperties.ATTEMPT_STRING_VALUES_PARSING ,		// Fields
	        EnrichDataProperties.ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG ,	// Output
	        EnrichDataProperties.DEVICE_STATE_OUPUT_FORMAT ,			// Output - Device state
	        EnrichDataProperties.DEVICE_STATE_UPDATE_FREQUENCY_FIELD, 	// Output - Device state
	        EnrichDataProperties.DEVICE_STATE_DROP_UPDATE_THRESHOLD ,	// Output - Device state
	        EnrichDataProperties.DEVICE_STATE_METRICS_ARRAYS            // Output - Device state
    	) );
    	
    	staticPropertyNames = propertyDescriptors.stream()
    			.map( (PropertyDescriptor p) -> { return p.getName(); } )
    			.collect( Collectors.toList() );
    											
    	
    	final Set<Relationship> rels = new HashSet<>();
    	rels.add( EnrichDataRelationships.SUCCESS_RELATIONSHIP );
    	rels.add( EnrichDataRelationships.RETRY_RELATIONSHIP );
    	rels.add( EnrichDataRelationships.ORIGINAL_RELATIONSHIP );
    	rels.add( EnrichDataRelationships.FAILURE_RELATIONSHIP );
    	rels.add( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
    	relationships = Collections.unmodifiableSet( rels );
    }
    
    // Static method to retrieve the static property descriptors
    public static final List<PropertyDescriptor> getStaticDescriptors(){
    	return propertyDescriptors;
    }

    //Ownership
    private boolean ownershipWithoutControllerService;
    private boolean ownershipRouteToFailureOnError;
    private JsonObject defaultOwnershipProperties;
    
    // Servicemap configuration properties
//    private String serviceUriPrefixAttrName;
    
    // Timestamp properties
    private String timestampFromContentPropertyName;
    private String timestampFromContentPropertyValue;
    private boolean timestampFromContent;
    private boolean useTimestampFallback = true;
    
    // Enrichment properties
    private String deviceIdName;
    private String deviceIdNameMapping;
    private String timestampFieldName;
    private String valueFieldName;
    private List<String> enrichmentResponseBasePath;
    
    private boolean latlonPriorityInner;
    private List<String> enrichmentLatLonPath;
    private boolean enrichmentLatitudeFirst;
    
    //Inner latlon
//    private JsonObject innerLatlonConfig;
    private List<CompoundLatlonField> geoJsonFields;
    private List<CompoundLatlonField> geoPointFields;
    private List< List<String> > latitudeFields;
    private List< List<String> > longitudeFields;
    
    
    private String srcPropertyValue;
    private String kindPropertyValue;
    private List<String> fieldsToPurge;
    private boolean attemptNumericStringParsing;
    
    // Enrichment join type
    private boolean leftJoin;
    
    // Enricher
    private Enricher enricher;
    
    // Device state converter
    private DeviceStateConverter deviceStateConverter;
    // Determined by the check on the auto-termination of the 
    // DEVICE_STATE relationship
    private boolean outputDeviceState;
    private long deviceStateDropThreshold = 0;
    
    // Output properties
    private String esIndex;
    private String esType;
    
    // Output producer
    private OutputProducer outProducer;
    
    // Mappings for additional fields and prefix substitutions
    private Map< String , List<String> > additionalFieldPaths;
    private Map< String , String > deviceIdValuePrefixSubst;
    
    // Original flow file augmentation
    private Map< String , List<String>> originalFFAttributesAugMapping;
    
    // Controller services and enrichment client
    private EnrichmentSourceClientService enrichmentSourceClientService;
    private EnrichmentSourceClient enrichmentSourceClient;
    private String defaultServiceUriPrefix = null;
    
    private EnrichmentResourceLocatorService enrichmentResourceLocatorService;
    private EnrichmentResourceLocator enrichmentResourceLocator;
    
    private EnrichmentSourceClientService ownershipClientService;
    private EnrichmentSourceClient ownershipClient;
    
    // Logger
    private ComponentLog logger;
    
    @Override
    protected void init(final ProcessorInitializationContext context) {
    	
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return propertyDescriptors;
    }
    
    
    /**
     * This method provides the dynamic attributes descriptor.
     */
    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName ) {
    	return new PropertyDescriptor.Builder().name( propertyDescriptorName )
    		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    		.description( EnrichDataProperties.DYNAMIC_PROPERTIES_DESCRIPTION )
    		.dynamic( true )
    		.required( false )
    		.build();
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) throws ConfigurationException{    	
    	// Set up processor
    	this.logger = getLogger();
    	
    	// Controller service
    	//
    	// DO NOT CACHE CLIENTS HERE! Otherwise if we edit controller settings configs
    	// the modifications are not reflected to the client instance already owned by the processor
    	
		this.enrichmentSourceClientService = context.getProperty( EnrichDataProperties.ENRICHMENT_SOURCE_CLIENT_SERVICE )
													.asControllerService( EnrichmentSourceClientService.class );
    	
    	// EnrichmentSource client
    	try {
			this.enrichmentSourceClient = this.enrichmentSourceClientService.getClient( context );
		} catch (InstantiationException e) {
			String reason = "Unable to obtain an EnrichmentSourceClient instance from the controller service.";
			LoggingUtils.produceErrorObj( reason )
						.withExceptionInfo( e )
						.logAsError( logger );
			throw new ConfigurationException( reason );
		}

    	// Get the default service uri prefix, we need it to build the serviceUriPrefix
    	// which will be added as a static enrichment property
    	if( this.enrichmentSourceClient instanceof ServicemapSource ) {
    		this.defaultServiceUriPrefix = ( (ServicemapSource) this.enrichmentSourceClient )
    										.getDefaultUriPrefix();
    	}
    	
    	// Enrichment resource locator
    	if( context.getProperty( EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE ).isSet() ) {
    		this.enrichmentResourceLocatorService = context.getProperty( EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE )
    													   .asControllerService( EnrichmentResourceLocatorService.class );
    		
    		
			this.enrichmentResourceLocator = enrichmentResourceLocatorService.getResourceLocator(context);
			this.enrichmentResourceLocator.setLogger( logger );
    	}else {
    		this.enrichmentResourceLocatorService = null;
    		this.enrichmentResourceLocator = null;
    	}
    	
    	// Ownership controller service and client
    	if( context.getProperty( EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE ).isSet() ) {
    		this.ownershipClientService = context.getProperty( EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE )
    											 .asControllerService( EnrichmentSourceClientService.class );
    		
    		try {
				this.ownershipClient = this.ownershipClientService.getClient( context );
			} catch (InstantiationException e) {
				String reason = "Unable to obtain Ownership client instance from the controller service.";
				
				LoggingUtils.produceErrorObj( reason )
							.withExceptionInfo( e )
							.logAsError( logger );
			}
    	}else {
    		this.ownershipClientService = null;
    		this.ownershipClient = null;
    	}
    	
    	// Ownership behavior
    	String ownershipBehavior = context.getProperty( EnrichDataProperties.OWNERSHIP_BEHAVIOR ).getValue();
    	this.ownershipRouteToFailureOnError = false;
    	this.ownershipWithoutControllerService = false;
    	if( ownershipBehavior.equals( EnrichDataConstants.OWNERSHIP_BEHAVIOR_VALUES[0] ) ) {
    		this.ownershipRouteToFailureOnError = true;
    		this.ownershipWithoutControllerService = false;
    	}
    	if( ownershipBehavior.equals( EnrichDataConstants.OWNERSHIP_BEHAVIOR_VALUES[2] ) ) {
    		this.ownershipWithoutControllerService = true;
    		this.ownershipRouteToFailureOnError = false;
    	}
    	
    	
    	// Default ownership properties
    	String defaultOwnershpVal = context.getProperty( EnrichDataProperties.DEFAULT_OWNERSHIP_PROPERTIES ).getValue();
    	if( defaultOwnershpVal != null && !defaultOwnershpVal.isEmpty() )
    		this.defaultOwnershipProperties = JsonParser.parseString( defaultOwnershpVal ).getAsJsonObject();
    	else this.defaultOwnershipProperties = new JsonObject();
    		
    	
    	// Enrichment configs
    	this.deviceIdName = context.getProperty( EnrichDataProperties.DEVICE_ID_NAME ).getValue();
    	this.deviceIdNameMapping = context.getProperty( EnrichDataProperties.DEVICE_ID_NAME_MAPPING ).getValue();
    	
    	this.deviceIdValuePrefixSubst = new HashMap<>();
    	if( !context.getProperty( EnrichDataProperties.DEVICE_ID_VALUE_PREFIX_SUBST ).getValue().isEmpty() ) {
    		JsonElement mappingRootEl = JsonParser.parseString( context.getProperty( EnrichDataProperties.DEVICE_ID_VALUE_PREFIX_SUBST ).getValue() );
    		if( mappingRootEl.isJsonObject() ) {
    			mappingRootEl.getAsJsonObject().entrySet().stream()
    						 .forEach( (Map.Entry<String , JsonElement> subst) -> {
    							 if( subst.getValue().isJsonPrimitive() )
    								 this.deviceIdValuePrefixSubst.put( subst.getKey() , subst.getValue().getAsString() );
    						 });
    		}
    	}
    		
    	this.valueFieldName = context.getProperty( EnrichDataProperties.VALUE_FIELD_NAME ).getValue();
    	this.enrichmentResponseBasePath = Arrays.asList( context.getProperty( EnrichDataProperties.ENRICHMENT_RESPONSE_BASE_PATH ).getValue().split( "/" ) )
    		  									.stream().map( ( String pathEl ) -> { return pathEl.trim(); } )
    		  									.collect( Collectors.toList() );
    	
    	// Coordinates 
    	this.latlonPriorityInner = context.getProperty( EnrichDataProperties.LATLON_PRIORITY ).getValue().equals( EnrichDataConstants.LATLON_PRIORITY_VALUES[1] );
    	
    	this.enrichmentLatLonPath = Arrays.asList( context.getProperty( EnrichDataProperties.ENRICHMENT_LAT_LON_PATH ).getValue().split( "/" ) )
    									  .stream().map( (String pathEl ) -> { return pathEl.trim(); } )
    									  .collect( Collectors.toList() );
    	
    	
    	String innerLatlonConfigVal = context.getProperty( EnrichDataProperties.INNER_LAT_LON_CONFIG ).getValue();
    	JsonObject innerLatlonConfig;
    	if( innerLatlonConfigVal.isEmpty() ) {
    		innerLatlonConfig = new JsonObject();
    	}else {
    		innerLatlonConfig = JsonParser.parseString( innerLatlonConfigVal ).getAsJsonObject();
    	}
    	
    	
    	
    	// Check inner priority + inner latlon path
    	if( this.latlonPriorityInner == true && innerLatlonConfig.size() == 0 ) {
    		throw new ConfigurationException( String.format( "If the coordinates priority is set to '%s' the '%s' must be specified, but it is configured as empty." , 
    										  EnrichDataConstants.LATLON_PRIORITY_VALUES[1] , EnrichDataProperties.INNER_LAT_LON_CONFIG.getDisplayName() ) );
    	}
    	
    	this.geoJsonFields = new ArrayList<>();
    	this.geoPointFields = new ArrayList<>();
    	this.latitudeFields = new ArrayList<>();
    	this.longitudeFields = new ArrayList<>();
    	
    	// geo json
    	if( innerLatlonConfig.has( EnrichDataConstants.INNER_LATLON_GEOJSON ) ) {
    		JsonArray geoJsonFieldsConf = innerLatlonConfig.get( EnrichDataConstants.INNER_LATLON_GEOJSON )
    						 							   .getAsJsonArray();
    		Iterator<JsonElement> it = geoJsonFieldsConf.iterator();
    		while( it.hasNext() ) {
    			JsonObject gfc = it.next().getAsJsonObject();
    			this.geoJsonFields.add( new CompoundLatlonField( gfc ) );
    		}
    	}
    	
    	// geo point
    	if( innerLatlonConfig.has( EnrichDataConstants.INNER_LATLON_GEOPOINT ) ) {
    		JsonArray geoPointFieldsConf = innerLatlonConfig.get( EnrichDataConstants.INNER_LATLON_GEOPOINT )
					   .getAsJsonArray();
			Iterator<JsonElement> it = geoPointFieldsConf.iterator();
			while( it.hasNext() ) {
				JsonObject gfc = it.next().getAsJsonObject();
				this.geoPointFields.add( new CompoundLatlonField( gfc ) );
			}
    	}
    	
    	// single fields
    	if( innerLatlonConfig.has( EnrichDataConstants.INNER_LATLON_LATITUDE ) && innerLatlonConfig.has( EnrichDataConstants.INNER_LATLON_LONGITUDE ) ) {
    		JsonArray latitudes = innerLatlonConfig.get( EnrichDataConstants.INNER_LATLON_LATITUDE ).getAsJsonArray();
    		Iterator<JsonElement> it = latitudes.iterator();
    		while( it.hasNext() ) {
    			String latFieldPath = it.next().getAsString();
    			this.latitudeFields.add( JsonProcessing.pathStringToPathList( latFieldPath ) );
    		}
    		
    		JsonArray longitudes = innerLatlonConfig.get( EnrichDataConstants.INNER_LATLON_LONGITUDE ).getAsJsonArray();
    		it = longitudes.iterator();
    		while( it.hasNext() ) {
    			String lonFieldPath = it.next().getAsString();
    			this.longitudeFields.add( JsonProcessing.pathStringToPathList( lonFieldPath ) );
    		}
    	}
    	
    	
    	// True if [lat , lon] false if [lon , lat]
    	this.enrichmentLatitudeFirst = context.getProperty( EnrichDataProperties.ENRICHMENT_LAT_LON_FORMAT ).getValue().equals( EnrichDataConstants.ENRICHMENT_LAT_LON_FORMAT_VALUES[0] );
    	
    	// Static properties
    	this.srcPropertyValue = context.getProperty( EnrichDataProperties.SRC_PROPERTY ).getValue();
    	this.kindPropertyValue = context.getProperty( EnrichDataProperties.KIND_PROPERTY ).getValue();
    	
    	this.fieldsToPurge = Arrays.asList( context.getProperty( EnrichDataProperties.PURGE_FIELDS ).toString().split( "," ) )
    							   .stream().map( (String fieldName) -> { return fieldName.trim(); } )
    							   			.collect( Collectors.toList() );

    	// Enrichment type
    	if( context.getProperty( EnrichDataProperties.ENRICHMENT_BEHAVIOR ).getValue().equals( EnrichDataConstants.ENRICHMENT_BEHAVIOR_VALUES[0]) ) { // Join
    		this.leftJoin = false;
    	}
    	
    	if( context.getProperty( EnrichDataProperties.ENRICHMENT_BEHAVIOR ).getValue().equals( EnrichDataConstants.ENRICHMENT_BEHAVIOR_VALUES[1]) ) { // Left Join
    		this.leftJoin = true;
    	}
    	
    	// Timestamp configs
    	this.timestampFieldName = context.getProperty( EnrichDataProperties.TIMESTAMP_FIELD_NAME ).getValue();
    	this.timestampFromContentPropertyName = context.getProperty( EnrichDataProperties.TIMESTAMP_FROM_CONTENT_PROPERTY_NAME ).getValue();
    	this.timestampFromContentPropertyValue = context.getProperty( EnrichDataProperties.TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE ).getValue();
    	
    	if( !this.timestampFromContentPropertyName.isEmpty() && !this.timestampFromContentPropertyValue.isEmpty() )
    		this.timestampFromContent = true;
    	else
    		this.timestampFromContent = false;

    	// Output configs
    	//
    	// Json output format
    	if( context.getProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT ).getValue().equals( EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ) ) {
    		JsonOutputProducer jsonProducer = new JsonOutputProducer();
//    		jsonProducer.setTimestampAttribute( timestampFieldName );
    		this.outProducer = jsonProducer;
    	}
    	
    	// Elasticsearch bulk compliant output format
    	if( context.getProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT ).getValue().equals( EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[1] ) ) {
    		
    		// Check if 'ES Index' property is set
    		if( context.getProperty( EnrichDataProperties.ES_INDEX ).getValue().isEmpty() ) {
    			throw new ConfigurationException( "The output ff content format is set to 'Elasticsearch bulk indexing compliant' but 'ES Index' is not set." );
    		}
    		
    		// Check if 'ES Type' property is set
    		if( context.getProperty( EnrichDataProperties.ES_TYPE).getValue().isEmpty() ) {
    			throw new ConfigurationException( "The output ff content format is set to 'Elasticsearch bulk indexing compliant' but 'ES Type' is not set." );
    		}
    		
    		this.esIndex = context.getProperty( EnrichDataProperties.ES_INDEX ).getValue();
    		this.esType = context.getProperty( EnrichDataProperties.ES_TYPE ).getValue();
    		
    		this.outProducer = new ElasticsearchBulkIndexingOutputProducer( this.esIndex , this.esType );
    	}
    
    	// Split Json Object
    	if( context.getProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT ).getValue().equals( EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] ) ) {
    		String hashedIdFieldsString = context.getProperty( EnrichDataProperties.HASHED_ID_FIELDS ).getValue().trim();
    		
    		SplitObjectOutputProducer splitProducer;
    		if( !hashedIdFieldsString.isEmpty() ) {
    			List<String> hashedIdFields = Arrays.asList( hashedIdFieldsString.split( "," ) ).stream()
    											    .map( (String field) -> { return field.trim(); } )
    											    .collect( Collectors.toList() ); 
    			splitProducer = new SplitObjectOutputProducer( hashedIdFields );
    		}else {
    			splitProducer = new SplitObjectOutputProducer();
    		}
//    		splitProducer.setTimestampAttribute( this.timestampFieldName );
    		this.outProducer = splitProducer;
    	}
    	
    	this.outProducer.setTimestampAttribute( this.timestampFieldName );
    	if( context.getProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD ).isSet() ) {
    		long time_threshold = context.getProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD )
						    			 .asTimePeriod( TimeUnit.MILLISECONDS ).longValue();
    		this.outProducer.setTimestampThreshold( time_threshold );
    	}
    	
    	//User defined properties
    	this.additionalFieldPaths = new ConcurrentHashMap<>();
    	context.getAllProperties().forEach( (String k, String v) -> {
//    		if( !EnrichDataProperties.staticProperties.contains( k ) ) {
			if( !staticPropertyNames.contains( k ) ) {
    			this.additionalFieldPaths.put( 
    			   k , 
				   Arrays.asList( v.split("/") ).stream()
				   		 .map( s -> { return s.trim(); } )
				   		 .collect( Collectors.toList() ) 
				 );
    		}
    	});
    	
    	this.attemptNumericStringParsing = context.getProperty( EnrichDataProperties.ATTEMPT_STRING_VALUES_PARSING ).getValue()
    											  .equals( "Yes" );
    	
    	// Load node configs from file
    	loadNodeFileConfigs( context );
    	
    	//Initialize enricher with the determined implementation
    	// If the timestamp must be picked from the content we use a TwoPhaseEnricher
    	if( this.timestampFromContent ) {
	    	this.enricher = new TwoPhaseEnricher( this.valueFieldName , 
												  this.timestampFieldName ,
												  this.deviceIdNameMapping ,  
												  this.additionalFieldPaths , 
												  this.fieldsToPurge ,
												  this.timestampFromContentPropertyName , 
												  this.timestampFromContentPropertyValue , 
												  this.attemptNumericStringParsing );
	    	
	    	// Casts to specialized type to use set methods of the TwoPhaseEnricher
	    	TwoPhaseEnricher tpe = (TwoPhaseEnricher) this.enricher;
	    	tpe.setUseTimestampFallback( this.useTimestampFallback );
	    	
    	} else { //Otherwise use a OnePhaseEnricher
    		this.enricher = new OnePhaseEnricher( this.valueFieldName ,  
												  this.timestampFieldName , 
												  this.deviceIdNameMapping , 
												  this.additionalFieldPaths , 
												  this.fieldsToPurge , 
												  this.attemptNumericStringParsing );
    	}
    	
    	// Common to all enricher types
    	if( !this.srcPropertyValue.isEmpty() )
    		this.enricher.putStaticProperty( EnrichDataConstants.SRC_PROPERTY_OUTPUT_NAME , this.srcPropertyValue );
    	
    	if( !this.kindPropertyValue.isEmpty() )
    		this.enricher.putStaticProperty( EnrichDataConstants.KIND_PROPERTY_OUTPUT_NAME , this.kindPropertyValue );
    	
    	this.enricher.setLeftJoin( this.leftJoin );
    	
    	// DeviceState
    	// determine the device state output mode
    	DeviceStateConverter.OutputMode deviceStateOutputMode = DeviceStateConverter.OutputMode.valueOf( 
    		context.getProperty( EnrichDataProperties.DEVICE_STATE_OUPUT_FORMAT ).getValue() );
    	
    	
    	// output the device state only if the dedicated relationship
    	// is connected to another component, in order to skip the
    	// device state operations when the relation is auto-terminated.
    	if( context.hasConnection( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP ) ) {
    		this.outputDeviceState = true;
    		
    		if( context.getProperty( EnrichDataProperties.DEVICE_STATE_DROP_UPDATE_THRESHOLD ).isSet() )
    			this.deviceStateDropThreshold = context.getProperty(EnrichDataProperties.DEVICE_STATE_DROP_UPDATE_THRESHOLD)
    												   .asTimePeriod(TimeUnit.SECONDS).longValue();
    		
    		String updateFrequencyFieldName = null;
    		List<String> metricsArraysFields = null;
    		if( context.getProperty( EnrichDataProperties.DEVICE_STATE_UPDATE_FREQUENCY_FIELD ).isSet() )
    			updateFrequencyFieldName = context.getProperty( EnrichDataProperties.DEVICE_STATE_UPDATE_FREQUENCY_FIELD ).getValue();
    		if( context.getProperty( EnrichDataProperties.DEVICE_STATE_METRICS_ARRAYS ).isSet() )
    			metricsArraysFields = Arrays.asList( context.getProperty( EnrichDataProperties.DEVICE_STATE_METRICS_ARRAYS ).getValue().split( "," ) )
    				.stream().map( (String t) -> { return t.trim(); } )
    				.collect( Collectors.toList() );
    		
    		if( updateFrequencyFieldName != null && metricsArraysFields != null ) 
    			this.deviceStateConverter = new DeviceStateConverter( enricher , deviceStateOutputMode , logger , 
    																  updateFrequencyFieldName , 
    																  metricsArraysFields );
    		else if ( updateFrequencyFieldName != null )
    			this.deviceStateConverter = new DeviceStateConverter( enricher , deviceStateOutputMode , logger , updateFrequencyFieldName );
    		else if ( metricsArraysFields != null )
    			this.deviceStateConverter = new DeviceStateConverter( enricher , deviceStateOutputMode , logger , metricsArraysFields );
    		else
    			this.deviceStateConverter = new DeviceStateConverter( enricher , deviceStateOutputMode , logger );
    	}else {
    		this.outputDeviceState = false;
    	}
    	
    	// Original FF augmentation
    	originalFFAttributesAugMapping =  new HashMap<>();
    	String augMappingPropVal = context.getProperty( EnrichDataProperties.ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG ).getValue();
    	if( !augMappingPropVal.isEmpty() ) {
//    		JsonObject propObj = parser.parse( augMappingPropVal ).getAsJsonObject();
    		JsonObject propObj = JsonParser.parseString( augMappingPropVal ).getAsJsonObject();
    		for( Map.Entry<String , JsonElement> prop : propObj.entrySet() ) {
    			originalFFAttributesAugMapping.put(
    				prop.getKey() , 
    				JsonProcessing.pathStringToPathList( prop.getValue().getAsString() ) 
    			);
    		}
    	}
    }
    
    /**
     * Loads the configurations from the node configuration file.
     * 
     * @param context the processor context.
     * @throws ConfigurationException in case of IOException during file operations.
     */
    private void loadNodeFileConfigs( final ProcessContext context ) throws ConfigurationException{
    	
    	Properties props = new Properties();
    	String filePath = context.getProperty( EnrichDataProperties.NODE_CONFIG_FILE_PATH ).getValue();
    	
    	if( filePath != null && !filePath.isEmpty() ) {
    		try( FileInputStream in = new FileInputStream( filePath ) ){
        		props.load( in );
        		
        		EnrichDataConstants.fileConfigs.stream().forEach( (String conf) -> { 
            		String val = props.getProperty( conf );
            		if( val != null ) {
            			setConfigFromFile( conf , val );
            		}
            	});
        	} catch ( IOException ex ) {
        		LoggingUtils.produceErrorObj( "The specified 'Node config file path property' does not point to a valid configuration file. Using default configurations." )
        			.withExceptionInfo( ex )
        			.logAsWarning( logger );
        	}
    	}
    }
    
    /**
     * Updates the processor configuration with the specified 
     * setting from the node configuration file. 
     * 
     * @param confName a String containing the name for the configuration to set.
     * @param confValue a String containing the value for the configuration to set. Such string 
     * will be parsed to get the correct data type for the configuration value.
     */
    private void setConfigFromFile( String confName , String confValue ) {
    	if( confName.equals( EnrichDataConstants.fileConfigs.get( EnrichDataConstants.FILE_CONFIGS_USE_FALLBACK ) ) ) {
    		this.useTimestampFallback = Boolean.parseBoolean( confValue );
    	}
    }    
    
    /**
     * ON TRIGGER
     */
    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException{
    	
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }
        
        // flow file uuid
        String uuid = flowFile.getAttribute( EnrichDataConstants.UUID_ATTRIBUTE_NAME );
        
        // Get flow file content as a byte stream
        final ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();
        session.exportTo( flowFile , contentBytes );
        String flowFileContent = contentBytes.toString();
        
        // Content parsing
        JsonElement rootEl;
        try {
        	rootEl = JsonParser.parseString( flowFileContent );
        }catch( JsonParseException e ) {
        	String reason = "The flow file content is not parsable as Json.";
        	LoggingUtils.produceErrorObj( reason )
        				.withProperty( "ff-uuid" , uuid )
        				.logAsError( logger );
        	routeToRelationship( flowFile , EnrichDataRelationships.FAILURE_RELATIONSHIP , session ,
        		new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_ATTRIBUTE_NAME , reason ) );
        	return; 
        }
        
        // Base validation
        //
        if( !rootEl.isJsonObject() ) { // Check if the flow file content is a valid JSON object
        	String reason = String.format( "Flow file content is not a valid JsonObject" );
			LoggingUtils.produceErrorObj( reason , rootEl )
						.withProperty( "ff-uuid" , uuid )
						.logAsError( logger );
			// Route to failure
			routeToRelationship( flowFile , EnrichDataRelationships.FAILURE_RELATIONSHIP , session, 
				new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_ATTRIBUTE_NAME , reason ) );
			return;
        }        
        
        // Root object containing the ff data
    	JsonObject rootObject = rootEl.getAsJsonObject(); 
    	
    	if( !rootObject.has( deviceIdName ) || !rootObject.has( timestampFieldName ) ) {
    		String reason = String.format( "Flow file does not contain one (or both) fields: '%s' to pick the device id from, '%s' to pick the timestamp from , routing to failure." , 
					    				   this.deviceIdName , this.timestampFieldName );
			LoggingUtils.produceErrorObj( reason , rootEl )
						.withProperty( "ff-uuid" , uuid )
						.logAsError( logger );
			// Route to failure
			routeToRelationship( flowFile , EnrichDataRelationships.FAILURE_RELATIONSHIP , session, 
					new ImmutablePair<String,String>( EnrichDataConstants.FAILURE_ATTRIBUTE_NAME , reason ) );
			return;
    	}
    	
    	// Device id
		String deviceId = getSubstitutedPrefixValue( rootObject.get( this.deviceIdName ).getAsString() );
		session.putAttribute( flowFile , EnrichDataConstants.DEVICE_ID_ATTRIBUTE_NAME , deviceId ); // deviceId as flow file attribute
		
		// Timestamp handling
		String timestamp;
		
		// If the timestamp must be picked from the content and this node uses 
		// the fallback set the initial timestamp as the content of the 
		// 'timestampFieldName' field.
		if( this.timestampFromContent ) {
			if( this.useTimestampFallback )
				timestamp = rootObject.get( this.timestampFieldName ).getAsString();
			else
				timestamp = "";
		} else { // Otherwise set it to empty string (will be checked later)
			timestamp = rootObject.get( this.timestampFieldName ).getAsString();
		}
		
		// EnrichmentResourceLocator
		// -----
		// Initialize empty resource locations
		ResourceLocations resourceLocations = new ResourceLocations();
		if( this.enrichmentResourceLocator != null ) {
			try {
				resourceLocations = enrichmentResourceLocator.getResourceLocations( flowFile );
			} catch (EnrichmentResourceLocatorException e) {
				StringBuilder msg = new StringBuilder( e.getMessage() );
				List<Pair<String,String>> errorAttributes = new ArrayList<>();
				LoggableObject errorObj = LoggingUtils.produceErrorObj( msg.toString() , rootEl )
					.withExceptionInfo( e )
					.withProperty( "ff-uuid" , uuid );
				e.getAllAdditionalInfos().entrySet().stream().forEach( (Map.Entry<String , String> info) -> {
					errorObj.withProperty( info.getKey() , info.getValue() );
					errorAttributes.add( new ImmutablePair<String, String>( 
						"failure."+info.getKey() , info.getValue() ) 
					);
				});
				
				// Determine if the failure from the resource locator is retriable
				if( e.getCause() != null && shouldRetry( e.getCause() ) ) {
					String locatorRequestUrl; 
					try {
						locatorRequestUrl = enrichmentResourceLocator.buildRequestUrl( flowFile );
					} catch( EnrichmentResourceLocatorException ex ) { locatorRequestUrl = ""; }
					errorAttributes.add( new ImmutablePair<String , String>( 
						"locatorRequestUrl" , locatorRequestUrl ) );
					errorAttributes.add( new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_ATTRIBUTE_NAME , msg.toString() ) );
					errorAttributes.add( new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_CAUSE_ATTRIBUTE_NAME , e.getCause().toString() ) );
					routeToRelationship( flowFile , EnrichDataRelationships.RETRY_RELATIONSHIP , session, errorAttributes);
					
					msg.append( String.format( " Routing to %s" , EnrichDataRelationships.RETRY_RELATIONSHIP.getName() ) );
					errorObj.setReason( msg.toString() );
					errorObj.logAsError( logger );
					return;
				}
				
				msg.append( " The enrichment data will be retrieved using the service uri prefix configured in the Enrichment Source and the default ownership prefix configured in the Ownership Controller Service." );
				errorObj.setReason( msg.toString() );
				errorObj.logAsError( logger );
			}
		}
		
		// -----
		
		// Get enrichment data
		JsonElement responseRootEl;
		try {
			if( resourceLocations.hasLocationForService( ResourceLocations.Service.SERVICEMAP ) ) {
				responseRootEl = enrichmentSourceClient.getEnrichmentData( 
					resourceLocations.getLocationForService( ResourceLocations.Service.SERVICEMAP ) , 
					deviceId );
			}else {
				responseRootEl = enrichmentSourceClient.getEnrichmentData( deviceId );
			}
		} catch (EnrichmentSourceException e) {
			// EnrichmentSource ERROR handling
			StringBuilder msg = new StringBuilder( e.getMessage() );
			Relationship destination;
			List<Pair<String,String>> errorAttributes = new ArrayList<>();
			
			// Determine if retriable
			if( e.getCause() != null && shouldRetry( e.getCause() ) )
				destination = EnrichDataRelationships.RETRY_RELATIONSHIP;
			else
				destination = EnrichDataRelationships.FAILURE_RELATIONSHIP;
			msg.append( " Routing to " ).append( destination.getName() );
			
			LoggableObject errorObj  = LoggingUtils.produceErrorObj( msg.toString() , rootEl )
				.withExceptionInfo( e )
				.withProperty( "ff-uuid" , uuid );
			e.getInfos().entrySet().stream().forEach( (Map.Entry<String , String> info) -> {
				errorObj.withProperty( info.getKey() , info.getValue() );
				errorAttributes.add( new ImmutablePair<String, String>( 
					"failure."+info.getKey() , info.getValue() ) 
				);
			});
			errorObj.logAsError( logger );
			
			String requestUrl = null;
			try {
				if( resourceLocations.hasLocationForService( ResourceLocations.Service.SERVICEMAP ) )
					requestUrl = enrichmentSourceClient.buildRequestUrl( 
						resourceLocations.getLocationForService( ResourceLocations.Service.SERVICEMAP ) , 
						deviceId );
				else
					requestUrl = enrichmentSourceClient.buildRequestUrl( deviceId );
			} catch( UnsupportedEncodingException ue) { requestUrl = ""; }
			
			// Route to destination
			errorAttributes.add( new ImmutablePair<String, String>("requestUrl" , requestUrl) );
			errorAttributes.add( new ImmutablePair<String, String>( EnrichDataConstants.FAILURE_ATTRIBUTE_NAME , e.getMessage()) );
			errorAttributes.add( new ImmutablePair<String, String>( EnrichDataConstants.FAILURE_CAUSE_ATTRIBUTE_NAME , e.getCause().toString()) );
			routeToRelationship( flowFile , destination , session , errorAttributes );
			return;
		}
		
		//Get ownership data
		JsonObject ownershipResponseObj = null;
		if( this.ownershipClient != null ) {
			try {				
				JsonElement ownershipResponseRootEl; 
				if( resourceLocations.hasLocationForService( ResourceLocations.Service.OWNERSHIP ) ) {
					ownershipResponseRootEl = ownershipClient.getEnrichmentData(
						deviceId ,
						resourceLocations.getLocationForService( ResourceLocations.Service.OWNERSHIP ) );
				}else
					ownershipResponseRootEl = ownershipClient.getEnrichmentData( deviceId );
				
				if( !ownershipResponseRootEl.isJsonObject() )
					throw new EnrichmentSourceException( "The ownership response is not a valid json object." );
				
				ownershipResponseObj = ownershipResponseRootEl.getAsJsonObject();
				// Add defaults if not in ownership service response
//				if( !this.defaultOwnershipProperties.entrySet().isEmpty() )
//					for(Map.Entry<String, JsonElement> dProp : this.defaultOwnershipProperties.entrySet() ) {
//						if( !ownershipResponseObj.has( dProp.getKey() ) )
//							ownershipResponseObj.add( dProp.getKey() , dProp.getValue() );
//					}
			} catch (EnrichmentSourceException ex) {
				if( !this.ownershipRouteToFailureOnError ) {
					 // Default ownership properties
					ownershipResponseObj = this.defaultOwnershipProperties.deepCopy();
				} else {
					String reason = "EnrichmentSourceException while retrieving ownership data.";
					LoggingUtils.produceErrorObj( reason , rootEl )
								.withExceptionInfo( ex )
								.withProperty( "ff-uuid" , uuid )
								.logAsError( logger );
					
					routeToRelationship( flowFile , EnrichDataRelationships.FAILURE_RELATIONSHIP , session ,
						new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_ATTRIBUTE_NAME , reason ) ,
						new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_CAUSE_ATTRIBUTE_NAME , ex.toString()) );
					return;
				}
			}
		}else {
			if( this.ownershipWithoutControllerService )
				ownershipResponseObj = this.defaultOwnershipProperties.deepCopy();
		}
		
		// Enrichment source response processing
		//
		String latlonStr;
		JsonObject enrichmentObj;
		try {
			enrichmentObj = getEnrichmentObject( enrichmentResponseBasePath , responseRootEl );
			latlonStr = getLatLonString( this.enrichmentLatLonPath , responseRootEl ); // Coordinates from the enrichment response object, always retrieved
		} catch( ProcessException ex ) {
			String reason = "Exception while processing the response from the enrichment source (Servicemap).";
			LoggingUtils.produceErrorObj( reason , rootEl )
						.withExceptionInfo( ex )
						.withProperty( "ff-uuid" , uuid )
						.withProperty( "GET_ResponseBody", responseRootEl.toString() )
						.logAsError( logger );
			
			// Route to failure
			routeToRelationship( flowFile , EnrichDataRelationships.FAILURE_RELATIONSHIP , session ,
				new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_ATTRIBUTE_NAME , reason ) ,
				new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_CAUSE_ATTRIBUTE_NAME , ex.toString()) );
			return;
		}
		
		// Determine cordinates to use 
		Map<String, JsonElement> additionalProperties = new TreeMap<>();
		if( this.latlonPriorityInner ) {
			try {
				String innerLatLonStr = getInnerLatLon( rootEl );
				additionalProperties.put( EnrichDataConstants.COORDS_OUTPUT_NAME , new JsonPrimitive( innerLatLonStr ) );
			}catch( NoSuchElementException ex ) { // Fallback on the enrichment response coordinates
				additionalProperties.put( EnrichDataConstants.COORDS_OUTPUT_NAME , new JsonPrimitive( latlonStr ) );
			}
		}else{
			additionalProperties.put( EnrichDataConstants.COORDS_OUTPUT_NAME , new JsonPrimitive( latlonStr ) );
		}
					
		// Enrichment
		// uses the configured enricher implementation
		StringBuilder serviceUri = new StringBuilder("");
		try {
			// SERVICE URI 
			if( resourceLocations.hasLocationForService( ResourceLocations.Service.SERVICEMAP ) )
				serviceUri.append( resourceLocations.getLocationForService( ResourceLocations.Service.SERVICEMAP ) );
			else if( this.defaultServiceUriPrefix != null )
				serviceUri.append( this.defaultServiceUriPrefix );
			if( !serviceUri.toString().endsWith("/") )
				serviceUri.append( "/" );
			serviceUri.append( deviceId );
				
			
			additionalProperties.put( EnrichDataConstants.SERVICE_URI_OUTPUT_NAME , new JsonPrimitive( serviceUri.toString() ) );
			additionalProperties.put( EnrichDataConstants.UUID_ATTRIBUTE_NAME , new JsonPrimitive( uuid ) );
			additionalProperties.put( EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME , new JsonPrimitive( Instant.ofEpochMilli( flowFile.getEntryDate() ).toString() ) );
//			additionalProperties.put( "latlon" , new JsonPrimitive( latlonStr ) );

			// Add ownership properties to the additional (static) properties
			if( ownershipResponseObj != null ) {
				ownershipResponseObj.entrySet().forEach( (Map.Entry<String , JsonElement> ownershipProp) -> {
					additionalProperties.put( ownershipProp.getKey() , ownershipProp.getValue() );
				});
			}
			
			Map<String , String> additionalFieldsErrors = this.enricher.enrich( deviceId , 
								  											    rootObject , 
								  											    enrichmentObj , 
								  											    responseRootEl , 
								  											    timestamp ,
								  											    additionalProperties );
			//----
				
			
			if( rootObject.size() == 0 ) {
				throw new ProcessException( "The resulting JsonObject after the enrichment is empty." );
			}
			
			// Output producer ops 
			
			// clone the input ff before producing the output because the output producer removes
			// the input flow file from the session implicitly
			FlowFile originalFF = session.clone( flowFile );  
			List<FlowFile> outList = this.outProducer.produceOutput( rootObject , flowFile , session );
			
			String determinedTimestamp = null;
			String timeSlack = null;
			if( !outList.isEmpty() ) {
				determinedTimestamp = outList.get(0).getAttribute( timestampFieldName );
				timeSlack = outList.get(0).getAttribute( OutputProducer.TIME_SLACK_ATTRIBUTE_NAME );
			}
			
			outList.stream().forEach( (FlowFile ff) -> {
				// Additional ff attributes 
				ff = session.putAllAttributes( ff , additionalFieldsErrors );
				ff = session.putAttribute( ff , EnrichDataConstants.SERVICE_URI_OUTPUT_NAME , serviceUri.toString() );
//				ff = session.putAttribute( ff , timestampFieldName , rootObject.get(timestampFieldName).toString() );
				
				//Route to success
				routeToRelationship( ff , EnrichDataRelationships.SUCCESS_RELATIONSHIP , session , 
					new ImmutablePair<String, String>( "timestampSource" , this.enricher.getLastTimestampSource() ) 
				);
			});
			
			// Device State
			if( outputDeviceState ) {
				JsonElement deviceState = deviceStateConverter.convert( 
					rootObject , additionalProperties ,
					responseRootEl );
				// NOTE: the check on the timestamp for the device state drop must be done 
				// on the result of the conversion because the enricher may use a different timestamp
				// (if configured to pick the timestamp from a measure)
				if( this.deviceStateDropThreshold == 0 || 
					(this.deviceStateDropThreshold > 0 && 
					ChronoUnit.SECONDS.between( getTimestamp(deviceState.getAsJsonObject()) , 
						OffsetDateTime.now(ZoneOffset.UTC) ) <= this.deviceStateDropThreshold ) ) {
					// Create and route the device state flow file to the dedicated relationship
					FlowFile deviceStateFF = session.clone( originalFF );
					deviceStateFF = session.putAllAttributes( deviceStateFF , additionalFieldsErrors );
					// Write flow file content 
					deviceStateFF = session.write( deviceStateFF , new OutputStreamCallback() {
						@Override
						public void process(OutputStream out) throws IOException {
							out.write( deviceState.toString().getBytes() );
						}
					});				
					deviceStateFF = session.putAttribute( deviceStateFF , EnrichDataConstants.SERVICE_URI_OUTPUT_NAME , serviceUri.toString() );
					deviceStateFF = session.putAttribute( deviceStateFF , 
							EnrichDataConstants.MIME_TYPE_ATTRIBUTE_NAME , MediaType.JSON_UTF_8.toString() );
					if( determinedTimestamp != null )
						deviceStateFF = session.putAttribute( deviceStateFF , timestampFieldName , determinedTimestamp );
					if( timeSlack != null )
						deviceStateFF = session.putAttribute( deviceStateFF , OutputProducer.TIME_SLACK_ATTRIBUTE_NAME , timeSlack );
					routeToRelationship( deviceStateFF , EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP , session , 
						new ImmutablePair<String , String>( "timestampSource" , this.enricher.getLastTimestampSource() ) );
				} // Otherwise the device state is ignored
			}
			
			//Original flow file augmentation
			for( Map.Entry<String, List<String>> attr : originalFFAttributesAugMapping.entrySet() ) {
				try {
					String attrValue = JsonProcessing.getElementByPath( responseRootEl , attr.getValue() )
							  							  .getAsString();
					originalFF = session.putAttribute( originalFF , attr.getKey() , attrValue );
				}catch( NoSuchElementException ex ) {
					String attrPath = attr.getValue().stream()
							              .reduce( "" , (s1 , s2 ) -> { return s1 + "/" + s2; } );
					LoggingUtils.produceErrorObj( String.format( "Cannot perform enrichment on the original flow file. The '%s' property is missing from the enrichment source response. SKIPPING ENRICHMENT FOR THE PROPERTY '%s'." , 
												  attrPath , attr.getKey() ) )
								.withExceptionInfo( ex )
								.withProperty( "ff-uuid" , uuid )
								.withProperty( "EnrichmentSource_response" , responseRootEl.toString() )
								.logAsWarning( logger );
				}catch( ProcessException ex ) {
					ex.printStackTrace();
				}
			}
			originalFF = session.putAttribute( originalFF , EnrichDataConstants.SERVICE_URI_OUTPUT_NAME , serviceUri.toString() );
			originalFF = session.putAttribute( originalFF , EnrichDataConstants.MIME_TYPE_ATTRIBUTE_NAME , MediaType.JSON_UTF_8.toString() );
			if( determinedTimestamp != null )
				originalFF = session.putAttribute( originalFF , timestampFieldName , determinedTimestamp );
			if( timeSlack != null )
				originalFF = session.putAttribute( originalFF , OutputProducer.TIME_SLACK_ATTRIBUTE_NAME , timeSlack );
			routeToRelationship( originalFF , EnrichDataRelationships.ORIGINAL_RELATIONSHIP , session );
			
		}catch( ProcessException ex ) { // Exceptions during enrichment object retriveal from service response body			
			String reason = "Exception while enriching data.";
			LoggingUtils.produceErrorObj( reason , flowFileContent )
					    .withExceptionInfo( ex )
					    .withProperty( "ff-uuid" , uuid )
					    .withProperty( "enrichmentObj" , enrichmentObj.toString() )
					    .logAsError( logger );
			
			// Route to failure
			routeToRelationship( flowFile , EnrichDataRelationships.FAILURE_RELATIONSHIP , session , 
				new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_ATTRIBUTE_NAME , ex.getMessage()) ,
				new ImmutablePair<String , String>( EnrichDataConstants.FAILURE_CAUSE_ATTRIBUTE_NAME , ex.toString()) );
		}
    }
    
    /**
     * Return the timestamp property content from the supplied object.
     * The timestamp property name is the one configured for the processor.
     */
    private OffsetDateTime getTimestamp( JsonObject rootObj ) throws NoSuchElementException{
    	if( rootObj.has(timestampFieldName) ) {
    		JsonElement timestampEl = rootObj.get(timestampFieldName);
    		if( timestampEl.isJsonPrimitive() ) {
    			JsonPrimitive timestampPr = timestampEl.getAsJsonPrimitive();
    			if( timestampPr.isString() ) {
    				return OffsetDateTime.parse(timestampPr.getAsString());
    			}else {
					throw new NoSuchElementException( String.format( "The timestamp field field '%s' inside the rootObject is not a string." ,
	    	    			timestampFieldName ) );
    			}
    		}else {
    			throw new NoSuchElementException( String.format( "The timestamp field field '%s' inside the rootObject is not a JsonPrimitive." ,
    	    			timestampFieldName ) );
    		}
    	}else {
    		throw new NoSuchElementException( String.format( "Cannot find the timestamp field '%s' inside the rootObject." ,
    			timestampFieldName ) );
    	}
    }
    
    /**
     * Get the enrichment object from the enrichment service response according to the 
     * base path.
     * 
     * @param basePath the path from which to pick the enrichment object
     * @param responseRootEl root element of the enrichment service response
     * @return the enrichment object as a JsonObject
     * @throws ProcessException if the path is invalid or if it does not correctly match an object field inside the response
     */
    private JsonObject getEnrichmentObject( List<String> basePath , JsonElement responseRootEl ) throws ProcessException {
    	JsonElement el;
    	try {
    		el = JsonProcessing.getElementByPath(responseRootEl, basePath);
    	}catch( NoSuchElementException ex ) {
    		throw new ProcessException( "Unable to get the enrichment data. " + ex.getMessage() );
    	}    	
    	if( !el.isJsonObject() )
    		throw new ProcessException( String.format( 
    			"Unable to get the enrichment data. The last path field (%s) in the enrichment service response is not a JsonObject." ,
    			basePath.stream().reduce( (String s1, String s2) -> { return s1+"/"+s2; } ).get() 
    		) );
    	
    	return el.getAsJsonObject();
    }
    
    // TODO: REMOVE old method
//    private JsonObject getEnrichmentObject( List<String> basePath , JsonElement responseRootEl ) throws ProcessException{
//    	JsonElement el = responseRootEl;
//    	StringBuilder exploredPath = new StringBuilder(); // logging purpose
//    	for( String pathEl : basePath ) {
//    		exploredPath.append("/").append( pathEl );
//    		
//    		while( el.isJsonArray() ) {
//				el = el.getAsJsonArray();
//				if( ((JsonArray)el).size() > 0 ) {
//					el = ((JsonArray)el).get( 0 );
//				}else {
//					exploredPath.deleteCharAt(0);
//					throw new ProcessException( String.format( "Cannot obtain enrichment object. The '%s' field in the enrichment response contains an empty array." , 
//															   exploredPath.toString() ) );
//				}
//			}
//    		
//    		if( el.isJsonObject() ) {
//    			JsonObject elObj = el.getAsJsonObject();
//    			
//    			if( elObj.size() > 0 ) {
//	    			if( elObj.has( pathEl ) ) {
//	    				el = elObj.get( pathEl );
//	    			}else {
//	    				exploredPath.deleteCharAt(0);
//	    				throw new ProcessException( String.format( "Cannot obtain enrichment object. The '%s' field in the enrichment service response does not exists. ", 
//								   							        exploredPath.toString() ) );
//	    			}
//    			} else {
//    				exploredPath.deleteCharAt(0);
//    				throw new ProcessException( String.format( "Cannot obtain enrichment object. The '%s' field in the enrichment service response is empty. ", 
//							   								   exploredPath.toString() ) );
//    			}
//    		} else {
//    			exploredPath.deleteCharAt(0);
//    			throw new ProcessException( String.format( "The '%s' field in the enrichment service response is not a JsonObject." , 
//    													   exploredPath.toString() ) );
//    		}
//    	}
//    	
//    	if( el.isJsonObject() ) {
//    		return el.getAsJsonObject();
//    	}else {
//    		exploredPath.deleteCharAt(0);
//    		throw new ProcessException( String.format( "The last path field ('%s') in the enrichment service response is not a JsonObject." , 
//    												   exploredPath.toString() ) );
//    	}
//    }
    
    /**
     * Get the latitude and longitude as a string (lat and lon are separated from a comma)
     * 
     * @param latlonPath the path in the enrichment response from which to pick the latitude and the longitude.
     * @param responseRootEl response root element
     * @return a String containing latitude and longitude (concatenated with a comma)
     * @throws ProcessException if the path is invalid for the passed response element.
     */
    private String getLatLonString( List<String> latlonPath , JsonElement responseRootEl ) throws ProcessException {
    	
    	JsonElement el = responseRootEl;
    	String latlonStr = "";
    	
    	List<String> expPath = new ArrayList<>(); // logging
    	
    	
    	for( String pathEl : latlonPath ) {	
    		expPath.add( pathEl );
    		
			while( el.isJsonArray() ) {
				el = el.getAsJsonArray();
				if( ((JsonArray)el).size() > 0 ) {
					el = ((JsonArray) el).get( 0 );
				}else {
					throw new ProcessException( String.format( 
						"Cannot obtain [lat,lon]. The '%s' field in the target object contains an empty array." , 
						JsonProcessing.pathListToPathString(expPath)
					) );
				}
			}
			
			if( el.isJsonObject() ) {
				JsonObject elObj = el.getAsJsonObject();
				
				if( elObj.size() > 0 ) {
					if( elObj.has( pathEl ) ) {
						el = elObj.get( pathEl );
					}else {
						throw new ProcessException( String.format( 
							"Cannot obtain [lat,lon]. The '%s' field (from the latlon path) in the target object does not exists. ", 
							JsonProcessing.pathListToPathString( expPath )
						) );
					}
				} else {
					throw new ProcessException( String.format( 
						"Cannot obtain [lat,lon]. The '%s' field in the target object is empty." , 
						JsonProcessing.pathListToPathString( expPath )
				    ) );
				}
			}
    	}
    	
		if( el.isJsonArray() ) { //Last path element, must be a JsonArray in the enrichment response object
			JsonArray latlonArray = el.getAsJsonArray();
			
			if( latlonArray.size() == 2 ) {
				JsonElement lat;
				JsonElement lon;
				
				if( this.enrichmentLatitudeFirst ) {
					lat = latlonArray.get( 0 );
					lon = latlonArray.get( 1 );
				}else {
					lat = latlonArray.get( 1 );
					lon = latlonArray.get( 0 );
				}
				
				if( lat.isJsonPrimitive() && lon.isJsonPrimitive() ) {
					latlonStr = String.format( "%s,%s" , lat.getAsString() , lon.getAsString() ); 
					// The .getAsString() throws a ClassCastException if the value cannot be 
					// retrieved as a String
				}
				
			} else {
				throw new ProcessException( String.format( 
					"The latlonPath last field '%s' points to an array, but it's not a 2-elements array." , 
					JsonProcessing.pathListToPathString( expPath )
				) );
			}
			
		} else {
			throw new ProcessException( String.format( 
				"The latlonPath last field '%s' does not point to a JsonArray in the target object." , 
				JsonProcessing.pathListToPathString( expPath )
			) );
		}
    	
    	return latlonStr; //If there are some errors an exception is thrown
    }
    
    private String getInnerLatLon( JsonElement rootEl ) throws NoSuchElementException{
    	String latlon;
    	// GeoJson fields
		for( CompoundLatlonField f : this.geoJsonFields ) {
			try {
				JsonElement targetField = JsonProcessing.getElementByPath( rootEl , f.getPath() );
				if( targetField.isJsonArray() ) {
					JsonArray targetArr = targetField.getAsJsonArray();
					if( targetArr.size() == 2 ) {
						
						// Check content
						if( Float.isNaN( Float.parseFloat( targetArr.get(0).getAsString() ) ) )
							throw new NoSuchElementException();
						if( Float.isNaN( Float.parseFloat( targetArr.get(1).getAsString() ) ) )
							throw new NoSuchElementException();
						
						if( f.isLatitudeFirst() )
							latlon = targetArr.get(0).getAsString() + "," + targetArr.get(1).getAsString();
						else
							latlon = targetArr.get(1).getAsString() + "," + targetArr.get(0).getAsString();
						return latlon;
					}
				}
			}
			catch( NoSuchElementException ex ) {}
			catch( NumberFormatException ex ) {}
		}
		
		//GeoPoint fields
		for( CompoundLatlonField f : this.geoPointFields ) {
			try {
				JsonElement targetField = JsonProcessing.getElementByPath( rootEl , f.getPath() );
				if( targetField.isJsonPrimitive() ) {
					String targetVal = targetField.getAsString();
					List<String> targetList = Arrays.asList( targetVal.split(",") ).stream()
													.map( s -> s.trim() )
													.collect( Collectors.toList() );
					
					// Check content
					if( Float.isNaN( Float.parseFloat( targetList.get(0) ) ) )
						throw new NoSuchElementException();
					if( Float.isNaN( Float.parseFloat( targetList.get(1) ) ) )
						throw new NoSuchElementException();
					
					
					if( f.isLatitudeFirst() )
						latlon = targetList.get(0) + "," + targetList.get(1);
					else
						latlon = targetList.get(1) + "," + targetList.get(0);
					return latlon;
				}
			}
			catch( NoSuchElementException ex ) { }
			catch( NumberFormatException ex ) { }
		}
		
		// Distinct fields
		String lat = "";
		String lon = "";
		for( List<String> latPath : this.latitudeFields ) {
			try {
				JsonElement targetField = JsonProcessing.getElementByPath( rootEl , latPath );
				if( targetField.isJsonPrimitive() ) {
					lat = targetField.getAsString();
				}
				// Check content
				if( Float.isNaN( Float.parseFloat( lat ) ) )
					lat = "";				
			} 
			catch( NoSuchElementException ex ) { }
			catch( NumberFormatException ex ) { lat = ""; }
		}
		
		for( List<String> lonPath : this.longitudeFields ) {
			try {
				JsonElement targetField = JsonProcessing.getElementByPath( rootEl , lonPath );
				if( targetField.isJsonPrimitive() ) {
					lon = targetField.getAsString();
				}
				
				// Check content
				if( Float.isNaN( Float.parseFloat(lon) ) )
					lon = "";
			}
			catch( NoSuchElementException ex ) { }
			catch( NumberFormatException ex ) { lon = ""; }
		}
		
		if( !lat.isEmpty() && !lon.isEmpty() ) {
			latlon = lat + "," + lon;
			return latlon;
		}
		
		throw new NoSuchElementException( "Cannot retrieve coordinates from the flow file content" );
    }
    
    /**
     * Returns the input deviceId after performing prefix substitutions.
     * 
     * @param deviceId the original id on which to perform prefix substitution.
     * @return the given id after the prefix substitution (if necessary, the original id otherwise).
     */
    private String getSubstitutedPrefixValue( String deviceId ) {
    	if( !this.deviceIdValuePrefixSubst.isEmpty() ) {
	    	Set<Map.Entry<String, String>> mappingSet = this.deviceIdValuePrefixSubst.entrySet();
	    	for( Map.Entry<String, String> entry : mappingSet ) {
	    		if( deviceId.startsWith( entry.getKey() ) ) {
	    			return deviceId.replaceFirst( entry.getKey() , entry.getValue() );
	    		}
	    	}
    	}
    	
    	return deviceId;
    }
    
    // Implements retry policies
    private boolean shouldRetry( Throwable cause ) {
    	
    	if( cause instanceof HttpHostConnectException )
    		return true;
    	
    	if( cause instanceof HttpResponseException ) {
    		HttpResponseException httpEx = (HttpResponseException) cause;
    		if( EnrichDataConstants.RETRIABLE_STATUS_CODES.contains( httpEx.getStatusCode() ) ) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    /**
     * Routes the flow file to the target relationship adding the additional
     * attributes supplied if any.
     */
    private void routeToRelationship( FlowFile flowFile , Relationship rel , ProcessSession session ,
    							 	  Pair<String , String>... additionalAttributes ) {
    	
		for( Pair<String , String> attr : additionalAttributes ) {
			flowFile = session.putAttribute( flowFile , attr.getKey() , attr.getValue() );
		}
		session.transfer( flowFile , rel );
    }
    
    private void routeToRelationship( FlowFile flowFile , Relationship rel , ProcessSession session ,
    	List<Pair<String , String>> additionalAttributes ) {

		for( Pair<String , String> attr : additionalAttributes ) {
			flowFile = session.putAttribute( flowFile , attr.getKey() , attr.getValue() );
		}
		session.transfer( flowFile , rel );
	}
    
    /**
     * Utility class to manage inner latlon configurations.
     */
    private class CompoundLatlonField {
    	private List<String> path;
    	private boolean latitudeFirst;
    	
    	private void determineFormat( String format ) {
    		List<String> formatList = 
	    		Arrays.asList( format.split(",") ).stream()
	    			  .map( (String s) -> { return s.trim(); } )
	    			  .collect( Collectors.toList() );
    		if( formatList.get(0).equals( "lat" ) ) {
    			this.latitudeFirst = true;
    		}else{
    			this.latitudeFirst = false;
    		}
    	}
    	
    	public CompoundLatlonField( List<String> path , String format ) {
    		this.path = path;
    		determineFormat( format );
    	}
    	
    	public CompoundLatlonField( String path , String format ) {
    		this( JsonProcessing.pathStringToPathList(path) , 
    			  format );
    	}
    	
    	public CompoundLatlonField( JsonObject conf ) {
    		this( conf.get(EnrichDataConstants.INNER_LATLON_COMPOUND_FIELD_PATH).getAsString()  , 
    			  conf.get(EnrichDataConstants.INNER_LATLON_COMPOUND_FIELD_FORMAT).getAsString() );
    	}
    	
    	public List<String> getPath(){
    		return Collections.unmodifiableList( this.path );
    	}
    	
    	public boolean isLatitudeFirst() {
    		return this.latitudeFirst;
    	}
    }
}
