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
import java.io.UnsupportedEncodingException;
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
import java.util.stream.Collectors;

import javax.naming.ConfigurationException;
import javax.servlet.http.HttpServletResponse;
import javax.sound.midi.MidiDevice.Info;

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
import org.disit.nifi.processors.enrich_data.json_processing.JsonProcessingUtils;
import org.disit.nifi.processors.enrich_data.logging.LoggingUtils;
import org.disit.nifi.processors.enrich_data.logging.LoggingUtils.LoggableObject;
import org.disit.nifi.processors.enrich_data.output_producer.ElasticsearchBulkIndexingOutputProducer;
import org.disit.nifi.processors.enrich_data.output_producer.JsonOutputProducer;
import org.disit.nifi.processors.enrich_data.output_producer.OutputProducer;
import org.disit.nifi.processors.enrich_data.output_producer.SplitObjectOutputProducer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

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
	
	// Constants
	public static final String OWNERSHIP_BEHAVIOR_VALUES[] = {
		"Route to failure on ownership error." , 
		"Use defaults on ownership error." ,
		"Use defaults if no controller service configured."
	};
	
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
	
	public static final String LATLON_PRIORITY_VALUES[] = {
		"Enrichment source response property first." ,
		"Flow file content object property first."
	};
	
	public static final String INNER_LATLON_GEOJSON = "geoJsonFields";
	public static final String INNER_LATLON_GEOPOINT = "geoPointFields";
	public static final List<String> INNER_LATLON_COMPOUND_FIELDS_CONFIGS = Collections.unmodifiableList(
		Arrays.asList(
			INNER_LATLON_GEOJSON , 
			INNER_LATLON_GEOPOINT
		)
    );
	
	public static final String INNER_LATLON_LATITUDE = "latitudeFields";
	public static final String INNER_LATLON_LONGITUDE = "longitudeFields";
	public static final List<String> INNER_LATLON_SINGLE_FIELDS_CONFIGS = Collections.unmodifiableList(
		Arrays.asList(
			INNER_LATLON_LATITUDE , 
			INNER_LATLON_LONGITUDE
		)
    );
	
	public static final String INNER_LATLON_COMPOUND_FIELD_PATH = "path";
	public static final String INNER_LATLON_COMPOUND_FIELD_FORMAT = "format";
	public static final List<String> INNER_LATLON_COMPOUND_FIELD_PROPERTIES = Collections.unmodifiableList( 
		Arrays.asList( 
			INNER_LATLON_COMPOUND_FIELD_PATH , 
			INNER_LATLON_COMPOUND_FIELD_FORMAT	
		)
	);
	
	
	
	public static final List<Integer> RETRIABLE_STATUS_CODES = Collections.unmodifiableList( 
		Arrays.asList(
			// Server retriable status codes
			HttpServletResponse.SC_INTERNAL_SERVER_ERROR ,
			HttpServletResponse.SC_BAD_GATEWAY ,
			HttpServletResponse.SC_SERVICE_UNAVAILABLE ,
			HttpServletResponse.SC_GATEWAY_TIMEOUT ,
			// Client retriable status codes
			HttpServletResponse.SC_REQUEST_TIMEOUT
		)
	);

	// This set is needed to distinguish between user-defined properties and 
	// the properties specified by the descriptors.
	private static final Set<String> staticProperties = new HashSet<>( Arrays.asList(
			"ENRICHMENT_SOURCE_CLIENT_SERVICE" ,
			"OWNERSHIP_CLIENT_SERVICE" ,
			"OWNERSHIP_BEHAVIOR" ,
	        "DEFAULT_OWNERSHIP_PROPERTIES" ,
	        "ADDITIONAL_DEFAULT_OWNERSHIP_PROPERTIES" ,
		  	"DEVICE_ID_NAME" , 
		  	"DEVICE_ID_NAME_MAPPING" ,
		  	"DEVICE_ID_VALUE_PREFIX_SUBST" ,
		  	"ENRICHMENT_RESPONSE_BASE_PATH" ,
		  	"LATLON_PRIORITY",
		  	"ENRICHMENT_LAT_LON_PATH" ,
		  	"INNER_LAT_LON_CONFIG" ,
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
		  	"HASHED_ID_FIELDS",
		  	"ES_INDEX" , 
		  	"ES_TYPE" , 
		  	"NODE_CONFIG_FILE_PATH" , 
		  	"ATTEMPT_STRING_VALUES_PARSING" ,
		  	"ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG" 
		) 
	);
	
	// File configs
	private static final List<String> allowedFileConfigs = Arrays.asList( 
			"timestampFromContent.useFallback" 
	);
	private final int FILE_CONFIGS_USE_FALLBACK = 0;
	
	// Property Descriptors
	
	// Controller services
	public static final PropertyDescriptor ENRICHMENT_SOURCE_CLIENT_SERVICE = new PropertyDescriptor
			.Builder().name( "ENRICHMENT_SOURCE_CLIENT_SERVICE" )
			.displayName( "Enrichment Source Client Service" )
			.identifiesControllerService( EnrichmentSourceClientService.class )
			.description( "The client service which identifies an enrichment source. This source will be used to enrich the incoming flow files content." )
			.required( true )
			.addValidator( EnrichmentSourceServiceValidators.STANDARD_ENRICHMENT_SOURCE_VALIDATOR )
			.build();
	
	public static final PropertyDescriptor OWNERSHIP_CLIENT_SERVICE = new PropertyDescriptor
			.Builder().name( "OWNERSHIP_CLIENT_SERVICE" )
			.displayName( "Ownership Client Service" )
			.identifiesControllerService( EnrichmentSourceClientService.class )
			.description( "The client service which identifies the ownership source. This source will be used to enrich the incoming flow file content with informations about the ownership of the data." )
			.required( false )
			.addValidator( EnrichmentSourceServiceValidators.STANDARD_ENRICHMENT_SOURCE_VALIDATOR )
			.build();			
			
    // Ownership
	public static final PropertyDescriptor OWNERSHIP_BEHAVIOR = new PropertyDescriptor
			.Builder().name( "OWNERSHIP_BEHAVIOR" )
			.displayName("Ownership Behavior" )
			.description( "Controls the behavior of the ownership enrichment. 'Route to failure on ownership error' will route the flow file to failure in case of error while retrieving the ownership data, 'Use defaults on ownership error' will use the configure default ownership properties instead. By default, if there is no 'Ownership Client Service' configured the ownership enrichment is skipped, by setting this property to 'Use defaults if no controller service configured' the default ownership data is always being added to the output flow files." )
			.allowableValues( OWNERSHIP_BEHAVIOR_VALUES )
			.required( true )
			.defaultValue( OWNERSHIP_BEHAVIOR_VALUES[1] )
			.build();
	
	
	public static final PropertyDescriptor DEFAULT_OWNERSHIP_PROPERTIES = new PropertyDescriptor
            .Builder().name( "DEFAULT_OWNERSHIP_PROPERTIES" )
            .displayName( "Default ownership properties" )
            .description( "A JsonObject containing the default properties to enrich with, if the ownership service cannot retrieve a valid ownership response or if the 'Ownership Behavior' is set to 'Use defaults if no controller service configured'." )
            .required( false )
            .addValidator( EnrichDataValidators.jsonPropertyValidator(true) )
            .build();
	
	
	// Device ID
    public static final PropertyDescriptor DEVICE_ID_NAME = new PropertyDescriptor
            .Builder().name( "DEVICE_ID_NAME" )
            .displayName( "Device Id Name" )
            .description( "The name of the JSON field containing the device id. The value of such field will be concatenated to the 'Service URI Prefix' to obtain the full URI to query the enrichment source." )
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
            .description( "This property allows to substitute a prefix in the id value with another specified by the user. The value of this property must contains a JsonObject with the desired mappings. (Ex: {\"TA120-\":\"TA120_\"})" )
            .required(false)
            .defaultValue("")
            .addValidator(Validator.VALID)
            .build();
    
    // Timestamp
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
    
    // Uri prefix form flow file content object property
    public static final PropertyDescriptor URI_PREFIX_FROM_ATTR_NAME = new PropertyDescriptor
            .Builder().name( "URI_PREFIX_FROM_ATTR_NAME" )
            .displayName( "Service uri prefix from attribute name" )
            .description( "If this property is set the processor first looks for an attribute with the specified name in the input flow file to parse the service uri prefix from. Otherwise it uses the configured 'Enrichment Source Client Service'." )
            .required(false)
            .defaultValue("")
            .addValidator(Validator.VALID)
            .build();
    
    // Value field (flow file content object)
    public static final PropertyDescriptor VALUE_FIELD_NAME = new PropertyDescriptor
    		.Builder().name( "VALUE_FIELD_NAME" )
            .displayName( "Value field name" )
            .description( "The name of the field containing the value (in every JSON object of the input flow file content). This in needed to correctly produce the output value member. The processor will determine if the content of such field is a number, a string, or a JsonObject. Based on the value type the JSON object in the output flow file will have the value attribute with the same name if it was a number, with the '_str' suffix if it was a string and with '_obj' suffix if it was a JSON object." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    // Enrichment response object
    public static final PropertyDescriptor ENRICHMENT_RESPONSE_BASE_PATH = new PropertyDescriptor
            .Builder().name( "ENRICHMENT_RESPONSE_BASE_PATH" )
            .displayName( "Enrichment Response Base Path" )
            .description( "The base path in the JsonObject response from which to extract the enrichment data for every object in the flow file content. If the path contains fields containing a JsonArray in the response, the enrichment data are picked from the first element of the array. (Example path: Service/features/properties/realtimeAttributes)" )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    // Latitude and longitude
    public static final PropertyDescriptor LATLON_PRIORITY = new PropertyDescriptor
    		.Builder().name( "LATLON_PRIORITY" )
    		.displayName( "Coordinates enrichment priority" )
    		.description( String.format( 
    				"Specifies the priority for the coordinates to use. If '%s' is selected the coordinates are picked from a property inside the object contained in the incoming flow files if possible.\n If '%s' is selected instead, the coordinates are picked from the object retrieved from the enrichment source.\nIf the priority cannot be satisfied the processor will fallback on the other option.", 
    				LATLON_PRIORITY_VALUES[0] , LATLON_PRIORITY_VALUES[1] ) )
    		.required( true )
    		.allowableValues( LATLON_PRIORITY_VALUES )
    		.defaultValue( LATLON_PRIORITY_VALUES[0] )
    		.addValidator( StandardValidators.NON_EMPTY_EL_VALIDATOR )
    		.build();
    		
    
    public static final PropertyDescriptor ENRICHMENT_LAT_LON_PATH = new PropertyDescriptor
            .Builder().name( "ENRICHMENT_LAT_LON_PATH" )
            .displayName( "Enrichment Response latlon Path" )
            .description( "The path of the property in the enrichment source response object to get latitude and longitude from. This path must point to a 2-elements array in the enrichment response object containing the latitude and the longitude (Es. 'Service/features/geometry/coordinates'). The latitude and longitude in the array will be concatenated (with a ',' comma) an put in a field named 'latlon' in every enriched object." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    public static final PropertyDescriptor ENRICHMENT_LAT_LON_FORMAT = new PropertyDescriptor
    		.Builder().name( "ENRICHMENT_LAT_LON_FORMAT" )
    		.displayName( "Enrichment latlon format" )
    		.description( "Specifies the ordering of latitude and longitude in the coordinates array retrieved from the enrichment source (Ex. Servicemap)." )
    		.required( true )
    		.defaultValue( ENRICHMENT_LAT_LON_FORMAT_VALUES[0] )
    		.allowableValues( ENRICHMENT_LAT_LON_FORMAT_VALUES )
    		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    		.build();
    
    public static final PropertyDescriptor INNER_LAT_LON_CONFIG = new PropertyDescriptor
    		.Builder().name( "INNER_LAT_LON_CONFIG" )
    		.displayName( "Latlon from flow file content configuration" )
    		.description( "The configuration for the latitude and longitude from the flow file content. This configuration must be a special json object. Such object is deeply described in details in the Usage documentation of this processor." )
    		.required(false)
    		.defaultValue("")
    		.addValidator(EnrichDataValidators.innerLatLonPropertyValidator())
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
    
    // Static properties
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
    
    // Output
    public static final PropertyDescriptor OUTPUT_FF_CONTENT_FORMAT = new PropertyDescriptor
    		.Builder().name( "OUTPUT_FF_CONTENT_FORMAT" )
    		.displayName( "Output flow file content format" )
    		.description( "Specify the format of the final flow file content. The 'JSON' option produces a JSON object.\n The 'Elasticsearch bulk indexing compliant' option produces an output flowfile containing the string ready to be posted to the '_bulk' API of Elasticsearch, if this option is used, an index and a type must be supplied throught 'ES Index' and 'ES Type' options." )
    		.required( true )
    		.allowableValues( OUTPUT_FF_CONTENT_FORMAT_VALUES )
    		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    		.build();
    
    public static final PropertyDescriptor HASHED_ID_FIELDS = new PropertyDescriptor
    		.Builder().name( "HASHED_ID_FIELDS" )
    		.displayName( "Hashed id fields" )
    		.description( "This property has effect only if 'Output flow file content format' is set to 'Split JSON'.\nThis property allows to specify a comma-separated list of fields to be concatenated and hashed to build an identifier for every object produced by the split.\nThe identifier is added to every object produced as '_id'." )
    		.required( false )
    		.defaultValue("")
    		.addValidator( Validator.VALID )
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
    
    // Config file
    public static final PropertyDescriptor NODE_CONFIG_FILE_PATH = new PropertyDescriptor
    		.Builder().name( "NODE_CONFIG_FILE_PATH" )
    		.displayName( "Node config file path" )
    		.description( "A file used to specify additional configurations for the processor. In case of clustered NiFi the processor configurations are the same on all the nodes, this file allows to specify some configurations specific for each node (and thus must be configured on all the cluster nodes). This is a REQUIRED property, even if empty THIS FILE MUST EXISTS." )
    		.required( true )
    		.addValidator( StandardValidators.FILE_EXISTS_VALIDATOR )
    		.build();
    
    // Original flow file augmentation
    public static final PropertyDescriptor ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG = new PropertyDescriptor
    		.Builder().name( "ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG" )
    		.displayName( "Original flow file attributes enrichment" )
    		.description( "This property specifies an enrichment on the flow files routed to the 'original' relationship.\nThe content of such flow files is the same as the incoming ones,\nbut properties retrieved from the enrichment source can be put in these flow files as attributes.\nThe value of this processor's property must specify a valid json object where the property names are the name of the attributes to be inserted in the original flow file, and the values are property paths of the enrichment object whose values will be set as the attribute values.\nFor example the mapping '{\"baz\":\"foo/bar\"}' will add the attribute 'baz' with the value contained in the 'foo/bar' property of the enrichment object.\n Properties which cannot be found inside the enrichment object are skipped." )
    		.required( false )
    		.defaultValue( "" )
    		.addValidator( EnrichDataValidators.jsonPropertyValidator(true) )
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
    
    public static final Relationship ORIGINAL_RELATIONSHIP = new Relationship.Builder()
            .name("original")
            .autoTerminateDefault( true )
            .description("The original incoming flow file is routed to this relationship." )
            .build();

    public static final Relationship RETRY_RELATIONSHIP = new Relationship.Builder()
    		.name( "retry" )
    		.description( "Flow files which cannot be correctly enriched but are considered retriable will be routed to this relationship.\nA flow file is considered retriable if the cause of the failure is an enrichment service unavailability." )
    		.build();
    
    private static List<PropertyDescriptor> propertyDescriptors;
    private static Set<Relationship> processorRelationships;
    static {
    	propertyDescriptors = Collections.unmodifiableList( Arrays.asList( 
			ENRICHMENT_SOURCE_CLIENT_SERVICE , 	    // Enrichment sources
	        OWNERSHIP_BEHAVIOR , 					// Enrichment sources
	        OWNERSHIP_CLIENT_SERVICE , 				// Enrichment sources
	        DEFAULT_OWNERSHIP_PROPERTIES , 			// Enrichment sources
	        DEVICE_ID_NAME ,						// Fields
	        DEVICE_ID_NAME_MAPPING ,				// Fields
	        DEVICE_ID_VALUE_PREFIX_SUBST ,			// Fields
	        TIMESTAMP_FIELD_NAME , 					// Timestamp
	        TIMESTAMP_FROM_CONTENT_PROPERTY_NAME , 	// Timestamp
	        TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE , // Timestamp
	        URI_PREFIX_FROM_ATTR_NAME ,				// Fields
	        VALUE_FIELD_NAME ,						// Fields
	        ENRICHMENT_RESPONSE_BASE_PATH ,			// Enrichment sources
	        LATLON_PRIORITY ,						// Coordinates
	        ENRICHMENT_LAT_LON_PATH ,				// Coordinates
	        ENRICHMENT_LAT_LON_FORMAT ,				// Coordinates
	        INNER_LAT_LON_CONFIG ,					// Coordinates
	        ENRICHMENT_BEHAVIOR ,					// Enrichment sources
	        SRC_PROPERTY ,							// Other
	        KIND_PROPERTY ,							// Other
	        PURGE_FIELDS ,							// Fields
	        OUTPUT_FF_CONTENT_FORMAT ,				// Output
	        HASHED_ID_FIELDS ,						// Output
	        ES_INDEX ,								// Output
	        ES_TYPE ,								// Output
	        NODE_CONFIG_FILE_PATH ,					// Other
	        ATTEMPT_STRING_VALUES_PARSING ,			// Fields
	        ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG 		// Output
    	) );
    	
    	final Set<Relationship> rels = new HashSet<>();
    	rels.add( SUCCESS_RELATIONSHIP );
    	rels.add( RETRY_RELATIONSHIP );
    	rels.add( ORIGINAL_RELATIONSHIP );
    	rels.add( FAILURE_RELATIONSHIP );
    	processorRelationships = Collections.unmodifiableSet( rels );
    }
    
    // Static method to retrieve the static property descriptors
    public static final List<PropertyDescriptor> getStaticDescriptors(){
    	return propertyDescriptors;
    }
    
    
    // PropertyDescriptors and Relationships sets
    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    //Ownership
    private boolean ownershipWithoutControllerService;
    private boolean ownershipRouteToFailureOnError;
    private JsonObject defaultOwnershipProperties;
    
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
    
    private EnrichmentSourceClientService ownershipClientService;
    private EnrichmentSourceClient ownershipClient;
    
    // Logger
    private ComponentLog logger;
    
    //GSON parser
    private JsonParser parser;
    
    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add( ENRICHMENT_SOURCE_CLIENT_SERVICE );
        descriptors.add( OWNERSHIP_BEHAVIOR );
        descriptors.add( OWNERSHIP_CLIENT_SERVICE );
        descriptors.add( DEFAULT_OWNERSHIP_PROPERTIES );
        descriptors.add( DEVICE_ID_NAME );
        descriptors.add( DEVICE_ID_NAME_MAPPING );
        descriptors.add( DEVICE_ID_VALUE_PREFIX_SUBST );
        descriptors.add( TIMESTAMP_FIELD_NAME );
        descriptors.add( TIMESTAMP_FROM_CONTENT_PROPERTY_NAME );
        descriptors.add( TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE );
        descriptors.add( URI_PREFIX_FROM_ATTR_NAME );
        descriptors.add( VALUE_FIELD_NAME );
        descriptors.add( ENRICHMENT_RESPONSE_BASE_PATH );
        descriptors.add( LATLON_PRIORITY );
        descriptors.add( ENRICHMENT_LAT_LON_PATH );
        descriptors.add( ENRICHMENT_LAT_LON_FORMAT );
        descriptors.add( INNER_LAT_LON_CONFIG );
        descriptors.add( ENRICHMENT_BEHAVIOR );
        descriptors.add( SRC_PROPERTY );
        descriptors.add( KIND_PROPERTY );
        descriptors.add( PURGE_FIELDS );
        descriptors.add( OUTPUT_FF_CONTENT_FORMAT );
        descriptors.add( HASHED_ID_FIELDS );
        descriptors.add( ES_INDEX );
        descriptors.add( ES_TYPE );
        descriptors.add( NODE_CONFIG_FILE_PATH );
        descriptors.add( ATTEMPT_STRING_VALUES_PARSING );
        descriptors.add( ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG );
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS_RELATIONSHIP);
        relationships.add(FAILURE_RELATIONSHIP);
        relationships.add(RETRY_RELATIONSHIP);
        relationships.add(ORIGINAL_RELATIONSHIP);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
//        return this.relationships;
        return processorRelationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
//        return descriptors;
        return propertyDescriptors;
    }
    
    
    public static String DYNAMIC_PROPERTIES_DESCRIPTION = 
    		"A dynamic property attribute specifies a field of the enrichment response object to be added to every enriched object.\nThe field will be named as the property name and the value is picked according to the property value which must be a valid path in the enrichment response object.\n" +
	 	    "The field pointed by the specified path must contains a primitive type." + 
	    	"Example: 'Service/properties/organization'";
    
    /**
     * This method provides the dynamic attributes descriptor.
     */
    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName ) {
    	
    	return new PropertyDescriptor.Builder().name( propertyDescriptorName )
    							     .addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    							     .description( DYNAMIC_PROPERTIES_DESCRIPTION )
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
    	
    	// Ownership controller service and client
    	if( context.getProperty( OWNERSHIP_CLIENT_SERVICE ).isSet() ) {
    		this.ownershipClientService = context.getProperty( OWNERSHIP_CLIENT_SERVICE )
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
    	String ownershipBehavior = context.getProperty( OWNERSHIP_BEHAVIOR ).getValue();
    	this.ownershipRouteToFailureOnError = false;
    	this.ownershipWithoutControllerService = false;
    	if( ownershipBehavior.equals( OWNERSHIP_BEHAVIOR_VALUES[0] ) ) {
    		this.ownershipRouteToFailureOnError = true;
    		this.ownershipWithoutControllerService = false;
    	}
    	if( ownershipBehavior.equals( OWNERSHIP_BEHAVIOR_VALUES[2] ) ) {
    		this.ownershipWithoutControllerService = true;
    		this.ownershipRouteToFailureOnError = false;
    	}
    	
    	
    	// Default ownership properties
    	String defaultOwnershpVal = context.getProperty( DEFAULT_OWNERSHIP_PROPERTIES ).getValue();
    	if( defaultOwnershpVal != null && !defaultOwnershpVal.isEmpty() )
    		this.defaultOwnershipProperties = parser.parse( defaultOwnershpVal )
    											    .getAsJsonObject();
    	else this.defaultOwnershipProperties = new JsonObject();
    		
    	
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
    	
    	// Coordinates 
    	this.latlonPriorityInner = context.getProperty( LATLON_PRIORITY ).getValue().equals( LATLON_PRIORITY_VALUES[1] );
    	
    	this.enrichmentLatLonPath = Arrays.asList( context.getProperty( ENRICHMENT_LAT_LON_PATH ).getValue().split( "/" ) )
    									  .stream().map( (String pathEl ) -> { return pathEl.trim(); } )
    									  .collect( Collectors.toList() );
    	
    	
    	String innerLatlonConfigVal = context.getProperty( INNER_LAT_LON_CONFIG ).getValue();
    	JsonObject innerLatlonConfig;
    	if( innerLatlonConfigVal.isEmpty() ) {
    		innerLatlonConfig = new JsonObject();
    	}else {
    		innerLatlonConfig = parser.parse( innerLatlonConfigVal ).getAsJsonObject();
    	}
    	
    	
    	
    	// Check inner priority + inner latlon path
    	if( this.latlonPriorityInner == true && innerLatlonConfig.size() == 0 ) {
    		throw new ConfigurationException( String.format( "If the coordinates priority is set to '%s' the '%s' must be specified, but it is configured as empty." , 
    										  LATLON_PRIORITY_VALUES[1] , INNER_LAT_LON_CONFIG.getDisplayName() ) );
    	}
    	
    	this.geoJsonFields = new ArrayList<>();
    	this.geoPointFields = new ArrayList<>();
    	this.latitudeFields = new ArrayList<>();
    	this.longitudeFields = new ArrayList<>();
    	
    	// geo json
    	if( innerLatlonConfig.has( INNER_LATLON_GEOJSON ) ) {
    		JsonArray geoJsonFieldsConf = innerLatlonConfig.get( INNER_LATLON_GEOJSON )
    						 							   .getAsJsonArray();
    		Iterator<JsonElement> it = geoJsonFieldsConf.iterator();
    		while( it.hasNext() ) {
    			JsonObject gfc = it.next().getAsJsonObject();
    			this.geoJsonFields.add( new CompoundLatlonField( gfc ) );
    		}
    	}
    	
    	// geo point
    	if( innerLatlonConfig.has( INNER_LATLON_GEOPOINT ) ) {
    		JsonArray geoPointFieldsConf = innerLatlonConfig.get( INNER_LATLON_GEOPOINT )
					   .getAsJsonArray();
			Iterator<JsonElement> it = geoPointFieldsConf.iterator();
			while( it.hasNext() ) {
				JsonObject gfc = it.next().getAsJsonObject();
				this.geoPointFields.add( new CompoundLatlonField( gfc ) );
			}
    	}
    	
    	// single fields
    	if( innerLatlonConfig.has( INNER_LATLON_LATITUDE ) && innerLatlonConfig.has( INNER_LATLON_LONGITUDE ) ) {
    		JsonArray latitudes = innerLatlonConfig.get( INNER_LATLON_LATITUDE ).getAsJsonArray();
    		Iterator<JsonElement> it = latitudes.iterator();
    		while( it.hasNext() ) {
    			String latFieldPath = it.next().getAsString();
    			this.latitudeFields.add( JsonProcessingUtils.pathStringToPathList( latFieldPath ) );
    		}
    		
    		JsonArray longitudes = innerLatlonConfig.get( INNER_LATLON_LONGITUDE ).getAsJsonArray();
    		it = longitudes.iterator();
    		while( it.hasNext() ) {
    			String lonFieldPath = it.next().getAsString();
    			this.longitudeFields.add( JsonProcessingUtils.pathStringToPathList( lonFieldPath ) );
    		}
    	}
    	
    	
    	// True if [lat , lon] false if [lon , lat]
    	this.enrichmentLatitudeFirst = context.getProperty( ENRICHMENT_LAT_LON_FORMAT ).getValue().equals( ENRICHMENT_LAT_LON_FORMAT_VALUES[0] );
    	
    	// Static properties
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
    		String hashedIdFieldsString = context.getProperty( HASHED_ID_FIELDS ).getValue().trim();
    		
    		if( !hashedIdFieldsString.isEmpty() ) {
    			List<String> hashedIdFields = Arrays.asList( hashedIdFieldsString.split( "," ) ).stream()
    											    .map( (String field) -> { return field.trim(); } )
    											    .collect( Collectors.toList() ); 
    			this.outProducer = new SplitObjectOutputProducer( hashedIdFields );
    		}else {
    			this.outProducer = new SplitObjectOutputProducer();
    		}
    	}
    	
    	//User defined properties
    	this.additionalFieldPaths = new ConcurrentHashMap<>();
    	context.getAllProperties().forEach( (String k, String v) -> {
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
    	
    	// Original FF augmentation
    	originalFFAttributesAugMapping =  new HashMap<>();
    	String augMappingPropVal = context.getProperty( ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG ).getValue();
    	if( !augMappingPropVal.isEmpty() ) {
    		JsonObject propObj = parser.parse( augMappingPropVal ).getAsJsonObject();
    		for( Map.Entry<String , JsonElement> prop : propObj.entrySet() ) {
    			originalFFAttributesAugMapping.put(
    				prop.getKey() , 
    				JsonProcessingUtils.pathStringToPathList( prop.getValue().getAsString() ) 
    			);
    		}
    	}
    }
    
    /**
     * Loads the configurations from the node configuration file.
     * 
     * TODO: allows for skipping node file configs. Currently it throws an IOException if the file does not exists.
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
        
//        FlowFile originalFF = session.clone( flowFile );
        
        // flow file uuid
        String uuid = flowFile.getAttribute( "uuid" );
        
        // Get flow file content as a byte stream
        final ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();
        session.exportTo( flowFile , contentBytes );
        String flowFileContent = contentBytes.toString();
        
        // Content parsing
//        JsonElement rootEl = parser.parse( flowFileContent );
        JsonElement rootEl;
        try {
        	rootEl = parser.parse( flowFileContent );
        }catch( JsonParseException e ) {
        	String reason = "The flow file content is not parsable as Json.";
        	LoggingUtils.produceErrorObj( reason )
        				.withProperty( "ff-uuid" , uuid )
        				.logAsError( logger );
        	routeToRelationship( flowFile , FAILURE_RELATIONSHIP , session ,
        		new ImmutablePair<String , String>( "failure" , reason ) );
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
//			flowFile = session.putAttribute( flowFile , "failure" , reason );
//			session.transfer( flowFile , FAILURE_RELATIONSHIP );
			routeToRelationship( flowFile , FAILURE_RELATIONSHIP , session, 
				new ImmutablePair<String , String>( "failure" , reason ) );
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
//			flowFile = session.putAttribute( flowFile , "failure" , reason );
//			session.transfer( flowFile , FAILURE_RELATIONSHIP );
			routeToRelationship( flowFile , FAILURE_RELATIONSHIP , session, 
					new ImmutablePair<String,String>( "failure" , reason ) );
			return;
    	}
    	
    	// Device id
		String deviceId = getSubstitutedPrefixValue( rootObject.get( this.deviceIdName ).getAsString() );
		session.putAttribute( flowFile , "deviceId" , deviceId ); // deviceId as flow file attribute
		
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
			
			if( uriPrefix != null ) {
				if( !uriPrefix.endsWith( "/" ) ) {
					uriPrefix = uriPrefix.concat( "/" );
				}
			}
		} 
    		
		// Get enrichment data
		JsonElement responseRootEl;
		try {
			if( uriPrefix != null )
				responseRootEl = enrichmentSourceClient.getEnrichmentData( deviceId , uriPrefix );
			else
				responseRootEl = enrichmentSourceClient.getEnrichmentData( deviceId );
		} catch (EnrichmentSourceException e) {
			// EnrichmentSource ERROR handling
			StringBuilder msg = new StringBuilder( e.getMessage() );
			Relationship destination;
			List<Pair<String,String>> errorAttributes = new ArrayList<>();
			
			// Determine if retriable
			if( e.getCause() != null && shouldRetry( e.getCause() ) )
				destination = RETRY_RELATIONSHIP;
			else
				destination = FAILURE_RELATIONSHIP;
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
				if( uriPrefix != null )
					requestUrl = enrichmentSourceClient.buildRequestUrl( uriPrefix , deviceId );
				else
					requestUrl = enrichmentSourceClient.buildRequestUrl( deviceId );
			} catch( UnsupportedEncodingException ue) { requestUrl = ""; }
			
			// Route to destination
			errorAttributes.add( new ImmutablePair<String, String>("requestUrl" , requestUrl) );
			errorAttributes.add( new ImmutablePair<String, String>("failure" , e.getMessage()) );
			errorAttributes.add( new ImmutablePair<String, String>("failure.cause" , e.getCause().toString()) );
			routeToRelationship( flowFile , destination , session , errorAttributes );
			return;
		}
		
		//Get ownership data
		JsonObject ownershipResponseObj = null;
		if( this.ownershipClient != null ) {
			try {
				JsonElement ownershipResponseRootEl = ownershipClient.getEnrichmentData( deviceId );
				if( !ownershipResponseRootEl.isJsonObject() )
					throw new EnrichmentSourceException( "The ownership response is not a valid json object." );
				
				ownershipResponseObj = ownershipResponseRootEl.getAsJsonObject();
				
				// Add defaults if not in ownership service response
				if( this.defaultOwnershipProperties.entrySet().isEmpty() )
					for(Map.Entry<String, JsonElement> dProp : this.defaultOwnershipProperties.entrySet() ) {
						if( !ownershipResponseObj.has( dProp.getKey() ) )
							ownershipResponseObj.add( dProp.getKey() , dProp.getValue() );
					}
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
					
					routeToRelationship( flowFile , FAILURE_RELATIONSHIP , session ,
						new ImmutablePair<String , String>( "failure" , reason ) ,
						new ImmutablePair<String , String>( "failure.cause" , ex.toString()) );
					return;
				}
				
			}
		}else {
			if( this.ownershipWithoutControllerService )
				ownershipResponseObj = this.defaultOwnershipProperties.deepCopy();
		}
		
		
		// Enrichment source response processing
		//
//		JsonElement responseRootEl;
		String latlonStr;
		JsonObject enrichmentObj;
		try {
//			responseRootEl = parser.parse( responseBody );		
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
//			flowFile = session.putAttribute( flowFile , "failure" , reason );
//			flowFile = session.putAttribute( flowFile , "failure.cause", ex.toString() );
//			flowFile = session.putAttribute( flowFile , "deviceId" , deviceId );
//			session.transfer( flowFile , FAILURE_RELATIONSHIP );
			routeToRelationship( flowFile , FAILURE_RELATIONSHIP , session ,
				new ImmutablePair<String , String>( "failure" , reason ) ,
				new ImmutablePair<String , String>( "failure.cause" , ex.toString()) );
			return;
		}
		
		// Determine cordinates to use 
		Map<String, JsonElement> additionalProperties = new TreeMap<>();
		if( this.latlonPriorityInner ) {
			try {
				String innerLatLonStr = getInnerLatLon( rootEl );
				additionalProperties.put( "latlon" , new JsonPrimitive( innerLatLonStr ) );
			}catch( NoSuchElementException ex ) { // Fallback on the enrichment response coordinates
				additionalProperties.put( "latlon" , new JsonPrimitive( latlonStr ) );
			}
		}else{
			additionalProperties.put( "latlon" , new JsonPrimitive( latlonStr ) );
		}
					
		// Enrichment
		//
		try {
			// Use configured enricher implementation
//			Map<String, JsonElement> additionalProperties = new TreeMap<>();

			StringBuilder serviceUriPropertyValue = new StringBuilder("");
			if( uriPrefix != null )
				serviceUriPropertyValue.append( uriPrefix );
			else if( this.defaultServiceUriPrefix != null )
				serviceUriPropertyValue.append( this.defaultServiceUriPrefix );
			serviceUriPropertyValue.append( deviceId );
				
			
			additionalProperties.put( "serviceUri" , new JsonPrimitive( serviceUriPropertyValue.toString() ) );
			additionalProperties.put( "uuid" , new JsonPrimitive( uuid ) );
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
			
			// clone the input ff before producing the output because the output producer removes
			// the input flow file from the session implicitly
			FlowFile originalFF = session.clone( flowFile );  
			List<FlowFile> outList = this.outProducer.produceOutput( rootObject , flowFile , session );
			
			outList.stream().forEach( (FlowFile ff) -> {
					
				// Additional ff attributes 
//				ff = session.putAttribute( ff , "requestUrl" , requestUrl );
//				ff = session.putAttribute( ff , "deviceId" , deviceId );
//				ff = session.putAttribute( ff , "timestampSource" , this.enricher.getLastTimestampSource() );
				
				ff = session.putAllAttributes( ff , additionalFieldsErrors );
				
				//Route to success
//					session.transfer( ff , SUCCESS_RELATIONSHIP );
				routeToRelationship( ff , SUCCESS_RELATIONSHIP , session , 
					new ImmutablePair<String, String>( "timestampSource" , this.enricher.getLastTimestampSource() ) 
				);
			});
			
			//Original flow file augmentation
			for( Map.Entry<String, List<String>> attr : originalFFAttributesAugMapping.entrySet() ) {
				try {
					String attrValue = JsonProcessingUtils.getElementByPath( responseRootEl , attr.getValue() )
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
			routeToRelationship( originalFF , ORIGINAL_RELATIONSHIP , session );
			
		}catch( ProcessException ex ) { // Exceptions during enrichment object retriveal from service response body			
			String reason = "Exception while enriching data.";
			LoggingUtils.produceErrorObj( reason , flowFileContent )
					    .withExceptionInfo( ex )
					    .withProperty( "ff-uuid" , uuid )
					    .withProperty( "enrichmentObj" , enrichmentObj.toString() )
					    .logAsError( logger );
			
			// Route to failure
//			flowFile = session.putAttribute( flowFile , "failure" , ex.getMessage() );
//			flowFile = session.putAttribute( flowFile , "failure.cause" , ex.toString() );
//			flowFile = session.putAttribute( flowFile , "deviceId" , deviceId );
//			session.transfer( flowFile , FAILURE_RELATIONSHIP );
			routeToRelationship( flowFile , FAILURE_RELATIONSHIP , session , 
				new ImmutablePair<String , String>( "failure" , ex.getMessage()) ,
				new ImmutablePair<String , String>( "failure.cause" , ex.toString()) );
		}
    }
    
    /**
     * Get the enrichment object from the enrichment service response according to the 
     * base path.
     * 
     * @param basePath the path from which to pick the enrichment object
     * @param responseRootEl response root element
     * @return the enrichment object as a JsonObject
     * @throws ProcessException if the path is invalid for the passed response element.
     */
    
    private JsonObject getEnrichmentObject( List<String> basePath , JsonElement responseRootEl ) throws ProcessException{
    	JsonElement el = responseRootEl;
    	StringBuilder exploredPath = new StringBuilder(); // logging purpose
    	for( String pathEl : basePath ) {
    		exploredPath.append("/").append( pathEl );
    		
    		while( el.isJsonArray() ) {
				el = el.getAsJsonArray();
				if( ((JsonArray)el).size() > 0 ) {
					el = ((JsonArray)el).get( 0 );
				}else {
					exploredPath.deleteCharAt(0);
					throw new ProcessException( String.format( "Cannot obtain enrichment object. The '%s' field in the enrichment response contains an empty array." , 
															   exploredPath.toString() ) );
				}
			}
    		
    		if( el.isJsonObject() ) {
    			JsonObject elObj = el.getAsJsonObject();
    			
    			if( elObj.size() > 0 ) {
	    			if( elObj.has( pathEl ) ) {
	    				el = elObj.get( pathEl );
	    			}else {
	    				exploredPath.deleteCharAt(0);
	    				throw new ProcessException( String.format( "Cannot obtain enrichment object. The '%s' field in the enrichment service response does not exists. ", 
								   							        exploredPath.toString() ) );
	    			}
    			} else {
    				exploredPath.deleteCharAt(0);
    				throw new ProcessException( String.format( "Cannot obtain enrichment object. The '%s' field in the enrichment service response is empty. ", 
							   								   exploredPath.toString() ) );
    			}
    		} else {
    			exploredPath.deleteCharAt(0);
    			throw new ProcessException( String.format( "The '%s' field in the enrichment service response is not a JsonObject." , 
    													   exploredPath.toString() ) );
    		}
    	}
    	
    	if( el.isJsonObject() ) {
    		return el.getAsJsonObject();
    	}else {
    		exploredPath.deleteCharAt(0);
    		throw new ProcessException( String.format( "The last path field ('%s') in the enrichment service response is not a JsonObject." , 
    												   exploredPath.toString() ) );
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
    	
    	StringBuilder exploredPath = new StringBuilder(); // logging purpose
    	
    	for( String pathEl : latlonPath ) {	
    		exploredPath.append( "/" ).append( pathEl );
    		
			while( el.isJsonArray() ) {
				el = el.getAsJsonArray();
				if( ((JsonArray)el).size() > 0 ) {
					el = ((JsonArray) el).get( 0 );
				}else {
					exploredPath.deleteCharAt(0);
					throw new ProcessException( String.format( "Cannot obtain [lat,lon]. The '%s' field in the target object contains an empty array." , 
															   exploredPath.toString() ) );
				}
			}
			
			if( el.isJsonObject() ) {
				JsonObject elObj = el.getAsJsonObject();
				
				if( elObj.size() > 0 ) {
					if( elObj.has( pathEl ) ) {
						el = elObj.get( pathEl );
					}else {
						exploredPath.deleteCharAt(0);
						throw new ProcessException( String.format( "Cannot obtain [lat,lon]. The '%s' field (from the latlon path) in the target object does not exists. ", 
																   exploredPath.toString() ) );
					}
				} else {
					exploredPath.deleteCharAt(0);
					throw new ProcessException( String.format( "Cannot obtain [lat,lon]. The '%s' field in the target object is empty." , 
															   exploredPath.toString() ) );
				}
			}
    	}
		
    	exploredPath.deleteCharAt(0);
    	
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
														   exploredPath.toString() ) );
			}
			
		} else {
			throw new ProcessException( String.format( "The latlonPath last field '%s' does not point to a JsonArray in the target object." , 
													   exploredPath.toString() ) );
		}
    	
    	return latlonStr; //If there are some errors an exception is thrown
    }
    
    private String getInnerLatLon( JsonElement rootEl ) throws NoSuchElementException{
    	String latlon;
    	// GeoJson fields
		for( CompoundLatlonField f : this.geoJsonFields ) {
			try {
				JsonElement targetField = JsonProcessingUtils.getElementByPath( rootEl , f.getPath() );
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
				JsonElement targetField = JsonProcessingUtils.getElementByPath( rootEl , f.getPath() );
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
				JsonElement targetField = JsonProcessingUtils.getElementByPath( rootEl , latPath );
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
				JsonElement targetField = JsonProcessingUtils.getElementByPath( rootEl , lonPath );
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
    		if( RETRIABLE_STATUS_CODES.contains( httpEx.getStatusCode() ) ) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    /**
     * Routes flow files to the failure relationship.
     * 
     * @param flowFile the flow file to be routed to the failure relationship.
     * @param session the process session.
     * @param reason the failure reason.
     * @param additionalAttributes additional key-value pairs wich will be added as attributes to the flow file routed to failure.
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
    		this( JsonProcessingUtils.pathStringToPathList(path) , 
    			  format );
    	}
    	
    	public CompoundLatlonField( JsonObject conf ) {
    		this( conf.get(INNER_LATLON_COMPOUND_FIELD_PATH).getAsString()  , 
    			  conf.get(INNER_LATLON_COMPOUND_FIELD_FORMAT).getAsString() );
    	}
    	
    	public List<String> getPath(){
    		return Collections.unmodifiableList( this.path );
    	}
    	
    	public boolean isLatitudeFirst() {
    		return this.latitudeFirst;
    	}
    }
}
