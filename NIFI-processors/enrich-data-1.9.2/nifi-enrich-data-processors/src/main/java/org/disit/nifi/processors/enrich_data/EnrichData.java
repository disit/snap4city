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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.naming.ConfigurationException;

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
import org.apache.nifi.processor.util.StandardValidators;
import org.disit.nifi.processors.enrich_data.enricher.Enricher;
import org.disit.nifi.processors.enrich_data.enricher.OnePhaseEnricher;
import org.disit.nifi.processors.enrich_data.enricher.TwoPhaseEnricher;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.enrichment_source.ServicemapSource;
import org.disit.nifi.processors.enrich_data.logging.LoggingUtils;
import org.disit.nifi.processors.enrich_data.output_producer.ElasticsearchBulkIndexingOutputProducer;
import org.disit.nifi.processors.enrich_data.output_producer.JsonOutputProducer;
import org.disit.nifi.processors.enrich_data.output_producer.OutputProducer;
import org.disit.nifi.processors.enrich_data.output_producer.SplitObjectOutputProducer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Tags({"snap4city" , "servicemap" , "enrichment","enrich"})
@CapabilityDescription("This processor enirches data incoming from a broker subscription with informations retrieved from Servicemap." )
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({ 
					@WritesAttribute(attribute="requestUrl", description="The URL used by the processor for the request to Servicemap.") ,
					@WritesAttribute(attribute="deviceId", description="The parsed deviceId.") , 
					@WritesAttribute(attribute="timestampSource", description="Track from where the timestamp is picked from.")
				  })

public class EnrichData extends AbstractProcessor {
	
	// Constants
	public static final String OUTPUT_FF_CONTENT_FORMAT_VALUES[] = { 
			"JSON" , 
			"Elasticsearch bulk indexing compliant" , 
			"Split Json Object"
	};
	
	public static final String ENRICHMENT_BEHAVIOR_VALUES[] = {
		"Remove not matched" , 
		"Keep not matched"
	}; 
	
	public static final String ENRICHMENT_LAT_LON_FORMAT_VALUES[] = {
		"[lat , lon]" ,
		"[lon , lat]"
	};

	// This set is needed to distinguish between user-defined properties and 
	// the properties specified by the descriptors.
	private static final Set<String> staticProperties = new HashSet<>( Arrays.asList(
			"ENRICHMENT_SOURCE_CLIENT_SERVICE" ,
		  	"DEVICE_ID_NAME" , 
		  	"DEVICE_ID_NAME_MAPPING" ,
		  	"DEVICE_ID_VALUE_PREFIX_SUBST" ,
		  	"ENRICHMENT_RESPONSE_BASE_PATH" ,
		  	"ENRICHMENT_LAT_LON_PATH" ,
		  	"ENRICHMENT_LAT_LON_FORMAT" ,
		  	"ENRICHMENT_BEHAVIOR" , 
		  	"TIMESTAMP_FIELD_NAME" ,
		  	"TIMESTAMP_FROM_CONTENT_PROPERTY_NAME" ,
		  	"TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE" ,
		  	"URI_PREFIX_FROM_ATTR_NAME" ,
		  	"VALUE_FIELD_NAME" ,  
		  	"SRC_PROPERTY" , 
		  	"KIND_PROPERTY", 
		  	"PURGE_FIELDS" ,
		  	"OUTPUT_FF_CONTENT_FORMAT" , 
		  	"ES_INDEX" , 
		  	"ES_TYPE" , 
		  	"NODE_CONFIG_FILE_PATH" , 
		  	"ATTEMPT_STRING_VALUES_PARSING" ) 
	);
	
	// File configs
	private static final List<String> allowedFileConfigs = Arrays.asList( "timestampFromContent.useFallback" );
	private final int FILE_CONFIGS_USE_FALLBACK = 0;
	
	// Property Descriptors
	public static final PropertyDescriptor ENRICHMENT_SOURCE_CLIENT_SERVICE = new PropertyDescriptor
			.Builder().name( "ENRICHMENT_SOURCE_CLIENT_SERVICE" )
			.displayName( "Enrichment Source Client Service" )
			.identifiesControllerService( EnrichmentSourceClientService.class )
			.description( "The client service which identifies the enrichment source to fetch enrichment data from." )
			.required( true )
			.addValidator( EnrichmentSourceServiceValidators.STANDARD_ENRICHMENT_SOURCE_VALIDATOR )
			.build();
			
    
    public static final PropertyDescriptor DEVICE_ID_NAME = new PropertyDescriptor
            .Builder().name( "DEVICE_ID_NAME" )
            .displayName( "Device Id Name" )
            .description( "The name of the JSON field containing the device id. The value of such field will be concatenated to the 'Service URI Prefix' to obtain the full Service URI to pass to Servicemap." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    public static final PropertyDescriptor DEVICE_ID_NAME_MAPPING = new PropertyDescriptor
            .Builder().name( "DEVICE_ID_NAME_MAPPING" )
            .displayName( "Device Id New Name" )
            .description( "If this property is not empty, the device id parsed from the field specified by 'Device Id Name' is embedded in every enriched object with this new name." )
            .required(false)
            .defaultValue("")
            .addValidator(Validator.VALID)
            .build();  
    
    public static final PropertyDescriptor DEVICE_ID_VALUE_PREFIX_SUBST = new PropertyDescriptor
            .Builder().name( "DEVICE_ID_VALUE_PREFIX_SUBST" )
            .displayName( "Device Id value prefix substitution" )
            .description( "This property allows to substitute a prefix in the id value with another specified by the user. The value of this property must contains a JsonObject with the desired mappings. (Ex: {\"TA120-\":\"TA120_\"}" )
            .required(false)
            .defaultValue("")
            .addValidator(Validator.VALID)
            .build();
    
    public static final PropertyDescriptor TIMESTAMP_FIELD_NAME = new PropertyDescriptor
            .Builder().name( "TIMESTAMP_FIELD_NAME" )
            .displayName( "Timestamp field name" )
            .description( "The name of the JsonObject field containing the timestamp. This field will be added to every produced JSON object." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    public static final PropertyDescriptor TIMESTAMP_FROM_CONTENT_MAPPING = new PropertyDescriptor
            .Builder().name( "TIMESTAMP_FROM_CONTENT_MAPPING" )
            .displayName( "Timestamp from content mapping" )
            .description( "This property allows to pick the timestamp from the enriched object. To accomplish this, the value of this property must be a JsonObject describing a field (with name and value) to be contained in the object to pick timestamp from. If multiple fields are specified in the mapping, only the first will be taken into account. If the enriched object contains multiple objects matching the mapping field, the first object from which the timestamp can be correctly parsed is used. If there's no object matching the mapping the value from the field specified as 'Timestamp field name' is used. (Ex: { \"value_type\":\"observation_time\" } )." )
            .required(false)
            .defaultValue("")
            .addValidator(Validator.VALID)
            .build();
    
    public static final PropertyDescriptor TIMESTAMP_FROM_CONTENT_PROPERTY_NAME = new PropertyDescriptor
            .Builder().name( "TIMESTAMP_FROM_CONTENT_PROPERTY_NAME" )
            .displayName( "Timestamp from content property" )
            .description( "The name of the property to check to determine if a member can be a candidate to pick the timestamp from. (Es: 'value_type')." )
            .required(false)
            .defaultValue("")
            .addValidator(Validator.VALID)
            .build();
    
    public static final PropertyDescriptor TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE = new PropertyDescriptor
            .Builder().name( "TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE" )
            .displayName( "Timestamp from content property value" )
            .description( "The value that the field specified by 'Timestamp from content property' needs to have to be a candidate to pick the timestamp from." )
            .required(false)
            .defaultValue("")
            .addValidator(Validator.VALID)
            .build();
    
    public static final PropertyDescriptor URI_PREFIX_FROM_ATTR_NAME = new PropertyDescriptor
            .Builder().name( "URI_PREFIX_FROM_ATTR_NAME" )
            .displayName( "Service uri prefix from attribute name" )
            .description( "If this property is set the processor first looks for an attribute with the specified name in the input flow file, to parse the service uri prefix from. Otherwise it uses the configured 'Service Uri Prefix' in the processor's configurations." )
            .required(false)
            .defaultValue("")
            .addValidator(Validator.VALID)
            .build();
    
    public static final PropertyDescriptor VALUE_FIELD_NAME = new PropertyDescriptor
    		.Builder().name( "VALUE_FIELD_NAME" )
            .displayName( "Value field name" )
            .description( "The name of the field containing the value (in every JSON object of the input flow file content). This in needed to correctly produce the output value member. The processor will determine if the content of such field is a number, a string, or a JsonObject. Based on the value type the JSON object in the output flow file will have the value attribute with the same name if it was a number, with the '_str' suffix if it was a string and with '_obj' suffix if it was a JSON object." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    public static final PropertyDescriptor ENRICHMENT_RESPONSE_BASE_PATH = new PropertyDescriptor
            .Builder().name( "ENRICHMENT_RESPONSE_BASE_PATH" )
            .displayName( "Enrichment Response Base Path" )
            .description( "The base path in the JsonObject response from which to extract the enrichment data for every object in the flow file content. If the path contains fields containing a JsonArray in the response, the enrichment data are picked from the first element of the array. (Example path: Service/features/properties/realtimeAttributes)" )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    public static final PropertyDescriptor ENRICHMENT_LAT_LON_PATH = new PropertyDescriptor
            .Builder().name( "ENRICHMENT_LAT_LON_PATH" )
            .displayName( "Enrichment latlon Path" )
            .description( "The path in the enrichment object to get latitude and longitude from. This path must point to a 2-elements array in the enrichment response object containing the latitude and the longitude (Es. 'Service/features/geometry/coordinates'). The latitude and longitude in the array will be concatenated (with a ',' comma) an put in a field named 'latlon' in every enriched object." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    public static final PropertyDescriptor ENRICHMENT_LAT_LON_FORMAT = new PropertyDescriptor
    		.Builder().name( "ENRICHMENT_LAT_LON_FORMAT" )
    		.displayName( "Enrichment lat lon format" )
    		.description( "Specifies the ordering of latitude and longitude in the coordinates array retrieved from Servicemap." )
    		.required( true )
    		.defaultValue( ENRICHMENT_LAT_LON_FORMAT_VALUES[0] )
    		.allowableValues( ENRICHMENT_LAT_LON_FORMAT_VALUES )
    		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    		.build();
    
    public static final PropertyDescriptor ENRICHMENT_BEHAVIOR = new PropertyDescriptor
    		.Builder().name( "ENRICHMENT_BEHAVIOR" )
    		.displayName( "Enrichment behavior" )
    		.description( "Specifies the behavior of the enricher: if this property is set to 'Remove not matched' the objects which are not in the servicemap response are removed from the final object, if it's set to 'Keep not matched' the objects which are not in the servicemap response are not enriched, but kept as they are in the final object." )
    		.required( true )
    		.defaultValue( ENRICHMENT_BEHAVIOR_VALUES[0] )
    		.allowableValues( ENRICHMENT_BEHAVIOR_VALUES )
    		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    		.build();
    
    public static final PropertyDescriptor SRC_PROPERTY = new PropertyDescriptor
            .Builder().name( "SRC_PROPERTY" )
            .displayName( "src" )
            .description( "If the value of this property is not empty, an attribute 'src' is added to every produced JsonObject (containing the specified value)." )
            .required(false)
            .defaultValue( "" )
            .addValidator( Validator.VALID )
            .build();
    
    public static final PropertyDescriptor KIND_PROPERTY = new PropertyDescriptor
            .Builder().name( "KIND_PROPERTY" )
            .displayName( "kind" )
            .description( "If the value of this property is not empty, an attribute 'kind' is added to every produced JsonObject (containing the specified value)." )
            .required(false)
            .defaultValue( "" )
            .addValidator( Validator.VALID )
            .build();
    
    public static final PropertyDescriptor PURGE_FIELDS = new PropertyDescriptor
            .Builder().name( "PURGE_FIELDS" )
            .displayName( "Purge fields list" )
            .description( "This property allows to specify a list of attributes to be removed from the enriched objects. This property must contains a COMMA SEPARATED list of attribute names. Note: the remove phase is done before the enrichment phase." )
            .required(false)
            .defaultValue( "" )
            .addValidator( Validator.VALID )
            .build();
    
    public static final PropertyDescriptor ATTEMPT_STRING_VALUES_PARSING = new PropertyDescriptor
            .Builder().name( "ATTEMPT_STRING_VALUES_PARSING" )
            .displayName( "Attempt string values parsing" )
            .description( "Specify if the processor should attempt to parse string values into number formats." )
            .required( false )
            .allowableValues( "Yes" , "No" )
            .defaultValue( "No" )
            .addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
            .build();
    
    public static final PropertyDescriptor OUTPUT_FF_CONTENT_FORMAT = new PropertyDescriptor
    		.Builder().name( "OUTPUT_FF_CONTENT_FORMAT" )
    		.displayName( "Output flow file content format" )
    		.description( "Specify the format of the final flow file content. The 'JSON' option produces a JSON object.\n The 'Elasticsearch bulk indexing compliant' option produces an output flowfile containing the string ready to be posted to the '_bulk' API of Elasticsearch, if this option is used, an index and a type must be supplied throught 'ES Index' and 'ES Type' options." )
    		.required( true )
    		.allowableValues( OUTPUT_FF_CONTENT_FORMAT_VALUES )
    		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    		.build();
    
    public static final PropertyDescriptor ES_INDEX = new PropertyDescriptor
            .Builder().name( "ES_INDEX" )
            .displayName( "ES Index" )
            .description( "Sepcify the ES index name to index data into. This option is used only if 'Output flow file content format' is set to 'Elasticsearch bulk indexing compliant'" )
            .required(false)
            .defaultValue( "" )
            .addValidator( Validator.VALID )
            .build();
    
    public static final PropertyDescriptor ES_TYPE = new PropertyDescriptor
            .Builder().name( "ES_TYPE" )
            .displayName( "ES Type" )
            .description( "Sepcify the ES type name to index data. This option is used only if 'Output flow file content format' is set to 'Elasticsearch bulk indexing compliant'" )
            .required(false)
            .defaultValue( "" )
            .addValidator( Validator.VALID )
            .build();
    
    public static final PropertyDescriptor NODE_CONFIG_FILE_PATH = new PropertyDescriptor
    		.Builder().name( "NODE_CONFIG_FILE_PATH" )
    		.displayName( "Node config file path" )
    		.description( "A file used to specify additional configurations for the processor. In case of clustered NiFi the processor configurations are the same on all the nodes, this file allows to specify some configurations specific for each node (and thus must be configured on all the cluster nodes). This is a REQUIRED property, even if empty THIS FILE MUST EXISTS." )
    		.required( true )
    		.addValidator( StandardValidators.FILE_EXISTS_VALIDATOR )
    		.build();

    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name("SUCCESS_RELATIONSHIP")
            .description("The correctly enriched flow files will be routed to this relationship." )
            .build();
    
    public static final Relationship FAILURE_RELATIONSHIP = new Relationship.Builder()
            .name("FAILURE_RELATIONSHIP")
            .description("Flow files which cannot be correctly enriched will be routed to this relationship." )
            .build();

    
    // PropertyDescriptors and Relationships sets
    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    // Servicemap configuration properties
    private String serviceUriPrefixAttrName;
    
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
    private List<String> enrichmentLatLonPath;
    private boolean enrichmentLatitudeFirst;
    private String srcPropertyValue;
    private String kindPropertyValue;
    private List<String> fieldsToPurge;
    private boolean attemptNumericStringParsing;
    
    // Enrichment join type
    private boolean leftJoin;
    
    // Enricher
    private Enricher enricher;
    
    // Output properties
    private String esIndex;
    private String esType;
    
    // Output producer
    private OutputProducer outProducer;
    
    // Mappings for additional fields and prefix substitutions
    private Map< String , List<String> > additionalFieldPaths;
    private Map< String , String > deviceIdValuePrefixSubst;
    
    // Controller services and enrichment client
    private EnrichmentSourceClientService enrichmentSourceClientService;
    private EnrichmentSourceClient enrichmentSourceClient;
    private String defaultServiceUriPrefix = null;
    
    // Logger
    private ComponentLog logger;
    
    //GSON parser
    private JsonParser parser;
    
    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add( ENRICHMENT_SOURCE_CLIENT_SERVICE );
        descriptors.add( DEVICE_ID_NAME );
        descriptors.add( DEVICE_ID_NAME_MAPPING );
        descriptors.add( DEVICE_ID_VALUE_PREFIX_SUBST );
        descriptors.add( TIMESTAMP_FIELD_NAME );
        descriptors.add( TIMESTAMP_FROM_CONTENT_PROPERTY_NAME );
        descriptors.add( TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE );
        descriptors.add( URI_PREFIX_FROM_ATTR_NAME );
        descriptors.add( VALUE_FIELD_NAME );
        descriptors.add( ENRICHMENT_RESPONSE_BASE_PATH );
        descriptors.add( ENRICHMENT_LAT_LON_PATH );
        descriptors.add( ENRICHMENT_LAT_LON_FORMAT );
        descriptors.add( ENRICHMENT_BEHAVIOR );
        descriptors.add( SRC_PROPERTY );
        descriptors.add( KIND_PROPERTY );
        descriptors.add( PURGE_FIELDS );
        descriptors.add( OUTPUT_FF_CONTENT_FORMAT );
        descriptors.add( ES_INDEX );
        descriptors.add( ES_TYPE );
        descriptors.add( NODE_CONFIG_FILE_PATH );
        descriptors.add( ATTEMPT_STRING_VALUES_PARSING );
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS_RELATIONSHIP);
        relationships.add(FAILURE_RELATIONSHIP);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }
    
    /**
     * This method provides the dynamic attributes descriptor.
     */
    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName ) {
    	
    	return new PropertyDescriptor.Builder().name( propertyDescriptorName )
    							     .addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    							     .description( "A dynamic property attribute specifies a field of the enrichment response object to be added to every enriched object.\nThe field will be named as the property name and the value is picked according to the property value which must be a valid path in the enrichment response object.\n" +
    							    		 	   "The field pointed by the specified path must contains a primitive type." + 
    							    		 	   "Example: 'Service/properties/organization'" )
    							     .dynamic( true )
    							     .required( false )
    							     .build();
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) throws ConfigurationException{
    	// Set up processor
    	this.logger = getLogger();
    	
    	// JSON parser
    	this.parser = new JsonParser();
    	
    	// Controller service
    	//
    	// Do NOT cache client here, otherwise if we edit controller settings configs
    	// the modifications are not reflected to the client instance already owned by the processor
    	
//    	if( this.enrichmentSourceClientService == null ) {
		this.enrichmentSourceClientService = context.getProperty( ENRICHMENT_SOURCE_CLIENT_SERVICE )
													.asControllerService( EnrichmentSourceClientService.class );
//    	}
    	
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
    	
    	// Enrichment configs
    	this.deviceIdName = context.getProperty( DEVICE_ID_NAME ).getValue();
    	this.deviceIdNameMapping = context.getProperty( DEVICE_ID_NAME_MAPPING ).getValue();
    	
    	this.deviceIdValuePrefixSubst = new HashMap<>();
    	if( !context.getProperty( DEVICE_ID_VALUE_PREFIX_SUBST ).getValue().isEmpty() ) {
    		JsonElement mappingRootEl = this.parser.parse( context.getProperty( DEVICE_ID_VALUE_PREFIX_SUBST ).getValue() );
    		if( mappingRootEl.isJsonObject() ) {
    			mappingRootEl.getAsJsonObject().entrySet().stream()
    						 .forEach( (Map.Entry<String , JsonElement> subst) -> {
    							 if( subst.getValue().isJsonPrimitive() )
    								 this.deviceIdValuePrefixSubst.put( subst.getKey() , subst.getValue().getAsString() );
    						 });
    		}
    	}
    		
    	this.valueFieldName = context.getProperty( VALUE_FIELD_NAME ).getValue();
    	this.enrichmentResponseBasePath = Arrays.asList( context.getProperty( ENRICHMENT_RESPONSE_BASE_PATH ).getValue().split( "/" ) )
    		  									.stream().map( ( String pathEl ) -> { return pathEl.trim(); } )
    		  									.collect( Collectors.toList() );
    	
    	this.enrichmentLatLonPath = Arrays.asList( context.getProperty( ENRICHMENT_LAT_LON_PATH ).getValue().split( "/" ) )
    									  .stream().map( (String pathEl ) -> { return pathEl.trim(); } )
    									  .collect( Collectors.toList() );
    	
    	// True if [lat , lon] false if [lon , lat]
    	this.enrichmentLatitudeFirst = context.getProperty( ENRICHMENT_LAT_LON_FORMAT ).getValue().equals( ENRICHMENT_LAT_LON_FORMAT_VALUES[0] );
    	
    	this.srcPropertyValue = context.getProperty( SRC_PROPERTY ).getValue();
    	this.kindPropertyValue = context.getProperty( KIND_PROPERTY ).getValue();
    	
    	this.fieldsToPurge = Arrays.asList( context.getProperty( PURGE_FIELDS ).toString().split( "," ) )
    							   .stream().map( (String fieldName) -> { return fieldName.trim(); } )
    							   			.collect( Collectors.toList() );

    	// Enrichment type
    	if( context.getProperty( ENRICHMENT_BEHAVIOR ).getValue().equals( ENRICHMENT_BEHAVIOR_VALUES[0]) ) { // Join
    		this.leftJoin = false;
    	}
    	
    	if( context.getProperty( ENRICHMENT_BEHAVIOR ).getValue().equals( ENRICHMENT_BEHAVIOR_VALUES[1]) ) { // Left Join
    		this.leftJoin = true;
    	}
    	
    	// Timestamp configs
    	this.timestampFieldName = context.getProperty( TIMESTAMP_FIELD_NAME ).getValue();
    	this.timestampFromContentPropertyName = context.getProperty( TIMESTAMP_FROM_CONTENT_PROPERTY_NAME ).getValue();
    	this.timestampFromContentPropertyValue = context.getProperty( TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE ).getValue();
    	
    	if( !this.timestampFromContentPropertyName.isEmpty() && !this.timestampFromContentPropertyValue.isEmpty() )
    		this.timestampFromContent = true;
    	else
    		this.timestampFromContent = false;
    	
    	// Service URI from content
    	this.serviceUriPrefixAttrName = context.getProperty( URI_PREFIX_FROM_ATTR_NAME ).getValue();

    	// Output configs
    	//
    	// Json output format
    	if( context.getProperty( OUTPUT_FF_CONTENT_FORMAT ).getValue().equals( OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ) ) {
    		this.outProducer = new JsonOutputProducer();
    	}
    	
    	// Elasticsearch bulk compliant output format
    	if( context.getProperty( OUTPUT_FF_CONTENT_FORMAT ).getValue().equals( OUTPUT_FF_CONTENT_FORMAT_VALUES[1] ) ) {
    		
    		// Check if 'ES Index' property is set
    		if( context.getProperty( ES_INDEX ).getValue().isEmpty() ) {
    			throw new ConfigurationException( "The output ff content format is set to 'Elasticsearch bulk indexing compliant' but 'ES Index' is not set." );
    		}
    		
    		// Check if 'ES Type' property is set
    		if( context.getProperty( ES_TYPE).getValue().isEmpty() ) {
    			throw new ConfigurationException( "The output ff content format is set to 'Elasticsearch bulk indexing compliant' but 'ES Type' is not set." );
    		}
    		
    		this.esIndex = context.getProperty( ES_INDEX ).getValue();
    		this.esType = context.getProperty( ES_TYPE ).getValue();
    		
    		this.outProducer = new ElasticsearchBulkIndexingOutputProducer( this.esIndex , this.esType );
    	}
    
    	// Split Json Object
    	if( context.getProperty( OUTPUT_FF_CONTENT_FORMAT ).getValue().equals( OUTPUT_FF_CONTENT_FORMAT_VALUES[2] ) ) {
    		this.outProducer = new SplitObjectOutputProducer();
    	}
    	
    	//User defined properties
    	this.additionalFieldPaths = new ConcurrentHashMap<>();
    	context.getAllProperties().forEach( (String k, String v) -> {
    		System.out.println( k + " " + v );
    		if( !staticProperties.contains( k ) ) {
    			this.additionalFieldPaths.put( k , 
    										   Arrays.asList( v.split("/") ).stream()
    										   		 .map( s -> { return s.trim(); } )
    										   		 .collect( Collectors.toList() ) 
    										 );
    		}
    	});
    	
    	this.attemptNumericStringParsing = context.getProperty( ATTEMPT_STRING_VALUES_PARSING ).getValue()
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
    		this.enricher.putStaticProperty( "src" , this.srcPropertyValue );
    	
    	if( !this.kindPropertyValue.isEmpty() )
    		this.enricher.putStaticProperty( "kind" , this.kindPropertyValue );
    	
    	this.enricher.setLeftJoin( this.leftJoin );
    }
    
    /**
     * Loads the configurations from the node configuration file.
     * 
     * @param context the processor context.
     * @throws ConfigurationException in case of IOException during file operations.
     */
    private void loadNodeFileConfigs( final ProcessContext context ) throws ConfigurationException{
    	
    	Properties props = new Properties();
    	String filePath = context.getProperty( NODE_CONFIG_FILE_PATH ).getValue();
    	
    	try( FileInputStream in = new FileInputStream( filePath ) ){
    		props.load( in );
    	} catch ( IOException ex ) {
    		throw new ConfigurationException( String.format( "IOException while parsing file '%s': %s" , filePath , ex.getMessage() ) );
    	}
    	
    	allowedFileConfigs.stream().forEach( (String conf) -> { 
    		String val = props.getProperty( conf );
    		if( val != null ) {
    			setConfigFromFile( conf , val );
    		}
    	});
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
    	if( confName.equals( allowedFileConfigs.get( FILE_CONFIGS_USE_FALLBACK ) ) ) {
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
        String uuid = flowFile.getAttribute( "uuid" );
        
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
			LoggingUtils.produceErrorObj( reason , rootEl )
						.withProperty( "ff-uuid" , uuid )
						.logAsError( logger );
			flowFile = session.putAttribute( flowFile , "failure" , reason );
			session.transfer( flowFile , FAILURE_RELATIONSHIP );
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
			flowFile = session.putAttribute( flowFile , "failure" , reason );
			session.transfer( flowFile , FAILURE_RELATIONSHIP );
			return;
    	}
    	
    	// Device id
		String deviceId = getSubstitutedPrefixValue( rootObject.get( this.deviceIdName ).getAsString() );
		
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
		
		// Service uri prefix from attributes handling
		String uriPrefix = null;
		if( !this.serviceUriPrefixAttrName.isEmpty() ) {
			// If the input ff does not contain such attribute the getAttribute() returns null
			uriPrefix = flowFile.getAttribute( this.serviceUriPrefixAttrName );
//			uriPrefix = ( flowFile.getAttribute( this.serviceUriPrefixAttrName ) != null ? 
//					      	flowFile.getAttribute( this.serviceUriPrefixAttrName ) : 
//						  this.serviceURIPrefix );
			
			if( !uriPrefix.endsWith( "/" ) ) {
				uriPrefix = uriPrefix.concat( "/" );
			}
		} 
//		else {
//			uriPrefix = this.serviceURIPrefix; 
//		}
    		
		JsonElement responseRootEl;
		try {
			if( uriPrefix != null )
				responseRootEl = enrichmentSourceClient.getEnrichmentData( deviceId , uriPrefix );
			else
				responseRootEl = enrichmentSourceClient.getEnrichmentData( deviceId );
		} catch (EnrichmentSourceException e) {
			String reason = "EnrichmentSource exception while retrieving enrichment data.";
			LoggingUtils.produceErrorObj( reason , rootEl )
						.withExceptionInfo( e )
						.withProperty( "ff-uuid" , uuid )
						.logAsError( logger );
			
			flowFile = session.putAttribute( flowFile , "failure" , e.getMessage() );
			flowFile = session.putAttribute( flowFile , "deviceId" , deviceId );
			session.transfer( flowFile , FAILURE_RELATIONSHIP );
			return;
		}
		
		// Enrichment source response processing
		//
//		JsonElement responseRootEl;
		String latlonStr;
		JsonObject enrichmentObj;
		try {
//			responseRootEl = parser.parse( responseBody );		
			latlonStr = getLatLonString( this.enrichmentLatLonPath , responseRootEl );
			enrichmentObj = getEnrichmentObject( enrichmentResponseBasePath , responseRootEl );
		} catch( ProcessException ex ) {
			String reason = "Exception while processing the response from the enrichment source (Servicemap).";
			LoggingUtils.produceErrorObj( reason , rootEl )
						.withExceptionInfo( ex )
						.withProperty( "ff-uuid" , uuid )
						.withProperty( "GET_ResponseBody", responseRootEl.toString() )
						.logAsError( logger );
			
			flowFile = session.putAttribute( flowFile , "failure" , reason );
			flowFile = session.putAttribute( flowFile , "deviceId" , deviceId );
			session.transfer( flowFile , FAILURE_RELATIONSHIP );
			return;
		}
					
		// Enrichment
		//
		try {
			// Use configured enricher implementation
			Map<String, String> additionalProperties = new TreeMap<>();

			StringBuilder serviceUriPropertyValue = new StringBuilder("");
			if( uriPrefix != null )
				serviceUriPropertyValue.append( uriPrefix );
			else if( this.defaultServiceUriPrefix != null )
				serviceUriPropertyValue.append( this.defaultServiceUriPrefix );
			serviceUriPropertyValue.append( deviceId );
				
			
			additionalProperties.put( "serviceUri" , serviceUriPropertyValue.toString() );
			additionalProperties.put( "uuid" , uuid );
			additionalProperties.put( "latlon" , latlonStr );
			
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
			
			List<FlowFile> outList = this.outProducer.produceOutput( rootObject , flowFile , session );
			
			//this.outProducer.produceOutput( rootObject , flowFile , session )
			outList.stream().forEach( (FlowFile ff) -> {
					
					// Additional ff attributes 
//					ff = session.putAttribute( ff , "requestUrl" , requestUrl );
					ff = session.putAttribute( ff , "deviceId" , deviceId );
					ff = session.putAttribute( ff , "timestampSource" , this.enricher.getLastTimestampSource() );
					
					ff = session.putAllAttributes( ff , additionalFieldsErrors );
					
					//Transfer to success
					session.transfer( ff , SUCCESS_RELATIONSHIP );
				});
			
		}catch( ProcessException ex ) { // Exceptions during enrichment object retriveal from service response body			
			String reason = "Exception while enriching data.";
			LoggingUtils.produceErrorObj( reason , flowFileContent )
					    .withExceptionInfo( ex )
					    .withProperty( "ff-uuid" , uuid )
					    .logAsError( logger );
			
			flowFile = session.putAttribute( flowFile , "failure" , ex.getMessage() );
			flowFile = session.putAttribute( flowFile , "deviceId" , deviceId );
			session.transfer( flowFile , FAILURE_RELATIONSHIP );
		}
    }
    
    /**
     * Get the enrichment object from the enrichment service response according to the 
     * base path.
     * 
     * @param basePath the path from which to pick the enrichment object from
     * @param responseRootEl response root element
     * @return the enrichment object as a JsonObject
     * @throws ProcessException if the path is invalid for the passed response element.
     */
    
    private JsonObject getEnrichmentObject( List<String> basePath , JsonElement responseRootEl ) throws ProcessException{
    	JsonElement el = responseRootEl;
    	for( String pathEl : basePath ) {
    		while( el.isJsonArray() ) {
				el = el.getAsJsonArray();
				if( ((JsonArray)el).size() > 0 ) {
					el = ((JsonArray)el).get( 0 );
				}else {
					throw new ProcessException( String.format( "Cannot obtain enrichment object. The '%s' field in the enrichment response contains an empty array." , pathEl ) );
				}
			}
    		
    		if( el.isJsonObject() ) {
    			JsonObject elObj = el.getAsJsonObject();
    			
    			if( elObj.size() > 0 ) {
	    			if( elObj.has( pathEl ) ) {
	    				el = elObj.get( pathEl );
	    			}else {
	    				throw new ProcessException( String.format( "Cannot obtain enrichment object. The '%s' field in the enrichment service response does not exists. ", 
								   							        pathEl ) );
	    			}
    			}else {
    				throw new ProcessException( String.format( "Cannot obtain enrichment object. The '%s' field in the enrichment service response is empty. ", 
							   								   pathEl ) );
    			}
    		} else {
    			throw new ProcessException( String.format( "The %s field in the enrichment service response is not a JsonObject." , pathEl ) );
    		}
    	}
    	
    	if( el.isJsonObject() ) {
    		
    		return el.getAsJsonObject();
    		
    	}else {
    		throw new ProcessException( "The last path field in the enrichment service response is not a JsonObject." );
    	}
    }
    
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
    	
    	for( String pathEl : latlonPath ) {	
			while( el.isJsonArray() ) {
				el = el.getAsJsonArray();
				if( ((JsonArray)el).size() > 0 ) {
					el = ((JsonArray) el).get( 0 );
				}else {
					throw new ProcessException( String.format( "Cannot obtain [lat,lon]. The '%s' field in the enrichment response contains an empty array." , pathEl ) );
				}
			}
			
			if( el.isJsonObject() ) {
				JsonObject elObj = el.getAsJsonObject();
				
				if( elObj.size() > 0 ) {
					if( elObj.has( pathEl ) ) {
						el = elObj.get( pathEl );
					}else {
						throw new ProcessException( String.format( "Cannot obtain [lat,lon]. The '%s' field (from the latlon path) in the enrichment service response does not exists. ", 
																   pathEl ) );
					}
				} else {
					throw new ProcessException( String.format( "Cannot obtain [lat,lon]. The '%s' field in the enrichment response object is empty." , 
															   pathEl ) );
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
				throw new ProcessException( String.format( "The latlonPath last field '%s' points to an array, but it's not a 2-elements array." , 
														   latlonPath.get( latlonPath.size() - 1 ) ) );
			}
			
		} else {
			throw new ProcessException( String.format( "The latlonPath last field '%s' does not point to a JsonArray in the enrichment response." , 
													   latlonPath.get( latlonPath.size() - 1 ) ) );
		}
    	
    	return latlonStr; //If there are some errors an exception is thrown
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
}
