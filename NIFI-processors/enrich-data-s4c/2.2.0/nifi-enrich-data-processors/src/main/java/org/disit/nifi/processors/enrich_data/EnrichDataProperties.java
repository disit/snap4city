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

import java.util.concurrent.TimeUnit;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.processor.util.StandardValidators;
import org.disit.nifi.processors.enrich_data.enricher.converter.DeviceStateConverter;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClientService;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocatorService;

public class EnrichDataProperties {

	// PROPERTY DESCRIPTORS
	
	// Controller services
	public static final PropertyDescriptor ENRICHMENT_SOURCE_CLIENT_SERVICE = new PropertyDescriptor
		.Builder().name( "ENRICHMENT_SOURCE_CLIENT_SERVICE" )
		.displayName( "Enrichment Source Client Service" )
		.identifiesControllerService( EnrichmentSourceClientService.class )
		.description( "The client service which identifies an enrichment source. This source will be used to enrich the incoming flow files content." )
		.required( true )
		.addValidator( EnrichmentSourceServiceValidators.STANDARD_ENRICHMENT_SOURCE_VALIDATOR )
		.build();

	public static final PropertyDescriptor ENRICHMENT_RESOURCE_LOCATOR_SERVICE = new PropertyDescriptor
		.Builder().name( "ENRICHMENT_RESOURCE_LOCATOR_SERVICE" )
		.displayName( "Enrichment Resource Locator Service" )
		.identifiesControllerService( EnrichmentResourceLocatorService.class )
		.description( "The client service which identifies the resource locator service. The locator service will be used to retrieve the serviceUriPrefix in order to make requests to the enrichment source. If the resource locator cannot retrieve the serviceUriPrefix, the requests are made using the prefix configured in the enrichment source service." )
		.required( false )
		.addValidator( Validator.VALID )
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
		.allowableValues( EnrichDataConstants.OWNERSHIP_BEHAVIOR_VALUES )
		.required( true )
		.defaultValue( EnrichDataConstants.OWNERSHIP_BEHAVIOR_VALUES[1] )
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

	public static final PropertyDescriptor TIMESTAMP_THRESHOLD = new PropertyDescriptor
		.Builder().name( "TIMESTAMP_THRESHOLD" )
		.displayName( "Timestamp threshold" )
		.description( "If this property is set, the flow files routed to the 'success' relationship will have an attribute containing: [ffTimestamp-(now-timestampThreshold)] in milliseconds." )
		.required( false )
		.addValidator( EnrichDataValidators.timePeriodValidatorOrNotSet( 1 , TimeUnit.SECONDS , 36500, TimeUnit.DAYS ) )
		.build();

	// Value field (flow file content object)
	public static final PropertyDescriptor VALUE_FIELD_NAME = new PropertyDescriptor
		.Builder().name( "VALUE_FIELD_NAME" )
        .displayName( "Value field name" )
        .description( "The name of the field containing the value (in every JSON object of the input flow file content). This in needed to correctly produce the output value member. The processor will determine if the content of such field is a number, a string, or a JsonObject. Based on the value type the JSON object in the output flow file will have the value attribute with the same name if it was a number, with the '_str' suffix if it was a string and with '_obj' suffix if it was a JSON object." )
        .required(true)
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();
	
	public static final PropertyDescriptor SERVICE_URI_PREFIX_FROM_ATTRIBUTE = new PropertyDescriptor
		.Builder().name( "SERVICE_URI_PREFIX_FROM_ATTRIBUTE" )
		.displayName( "Service Uri prefix from attribute" )
		.description( "Specify an attribute in the input flow file to pick the serviceUri prefix from. The attribute has the precedence over the Resource Locator and the default serviceUri prefix configured in the ErichmentSource." )
		.required( false )
		.defaultValue("")
		.addValidator( Validator.VALID )
		.build();
	
	public static final PropertyDescriptor DEVICE_ID_FROM_ATTRIBUTE = new PropertyDescriptor
		.Builder().name( "DEVICE_ID_FROM_ATTRIBUTE" )
		.displayName( "Device Id from attribute" )
		.description( "Specify an attribute in the input flow file to pick the deviceId from. The attribute has the precedence over the id present in the flow file content (specified with 'Device Id Name')." )
		.required( false )
		.defaultValue("")
		.addValidator( Validator.VALID )
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
			EnrichDataConstants.LATLON_PRIORITY_VALUES[1] , 
			EnrichDataConstants.LATLON_PRIORITY_VALUES[0] ) )
		.required( true )
		.allowableValues( EnrichDataConstants.LATLON_PRIORITY_VALUES )
		.defaultValue( EnrichDataConstants.LATLON_PRIORITY_VALUES[0] )
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
		.defaultValue( EnrichDataConstants.ENRICHMENT_LAT_LON_FORMAT_VALUES[1] )
		.allowableValues( EnrichDataConstants.ENRICHMENT_LAT_LON_FORMAT_VALUES )
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
		.defaultValue( EnrichDataConstants.ENRICHMENT_BEHAVIOR_VALUES[0] )
		.allowableValues( EnrichDataConstants.ENRICHMENT_BEHAVIOR_VALUES )
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
		.allowableValues( EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES )
		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
		.build();
	
	// XXX: Extract Enrichment Attributes property
	public static final PropertyDescriptor EXTRACT_ENRICHMENT_ATTRIBUTES = new PropertyDescriptor
		.Builder().name( "EXTRACT_ENRICHMENT_ATTRIBUTES" )
		.displayName( "Extract Enrichment response attributes" )
//			.description( "A comma-separated list of field paths in the enrichment source response to be added as attributes to the flow files emitted on the 'SUCCESS' and 'devices state' relationships. The paths must be specified using the forward slash (/) syntax. The attribute names will be the paths, where the '/' are replaced with '.', for example, the path 'foo/bar/baz' will generate an attribute with name 'foo.bar.baz'." )
		.description( "A comma-separated list of field paths to be extracted as attributes from the Enrichment Source response. These paths are checked inside every object contained in the 'Enrichment Response Base Path'. This property is considered ONLY IF 'Output flow file content format' is set to 'Split Json Object'.")
		.required( false )
		.defaultValue("")
		.addValidator(Validator.VALID)
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
		.description( "A file used to specify additional configurations for the processor. In case of clustered NiFi the processor configurations are the same on all nodes, this file allows to specify some configurations for the nodes individually." )
//    	.required( true )
		.required( false )
//    	.addValidator( StandardValidators.FILE_EXISTS_VALIDATOR )
		.addValidator( Validator.VALID )
		.defaultValue( "" )
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

	// Device state configs
	// output mode
	public static final PropertyDescriptor DEVICE_STATE_OUPUT_FORMAT = new PropertyDescriptor
		.Builder().name( "DEVICE_STATE_OUTPUT_FORMAT" )
		.displayName( "Device state output format" )
		.description( "This property specifies the output format for the content of the flow files routed to the 'Device state' relationship. MINIMAL will output measures objects with their value field only (specified by 'Value field name' and with the appropriate suffix depending on the detected data type). FULL will preserve all the fields contained in every measure object." )
		.required( false )
		.allowableValues( EnrichDataConstants.DEVICE_STATE_OUTPUT_FORMAT_VALUES )
		.defaultValue( DeviceStateConverter.OutputMode.MINIMAL.toString() )
		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
		.build();

	public static final PropertyDescriptor DEVICE_STATE_UPDATE_FREQUENCY_FIELD = new PropertyDescriptor
		.Builder().name( "DEVICE_STATE_UPDATE_FREQUENCY_FIELD" )
		.displayName( "Update frequency field" )
		.description( "This property specifies the path of an attribute in the enrichment response which contains the time (in seconds) to the next sensor update. This time is added to the parsed timestamp to produce a field, in the device state, prefixed with 'expected_next_' which will contain the expected next update time. " )
		.required( false )
		.defaultValue("")
		.addValidator( Validator.VALID )
		.build();

	public static final PropertyDescriptor DEVICE_STATE_DROP_UPDATE_THRESHOLD = new PropertyDescriptor
		.Builder().name( "DEVICE_STATE_DROP_UPDATE_THRESHOLD" )
		.displayName( "Drop device state update threshold" )
		.description("This property specifies a threshold to drop the device state if the timestamp contained in it is older than the current time minus the quantity specified by this property. Leave this property not set to keep every device state update." )
		.required(false)
//  	.addValidator( StandardValidators.createTimePeriodValidator( 1 , TimeUnit.SECONDS , 3650 , TimeUnit.DAYS ) )
		.addValidator(EnrichDataValidators.timePeriodValidatorOrNotSet(1, TimeUnit.SECONDS, 3650, TimeUnit.DAYS) )
		.build();

	public static final PropertyDescriptor DEVICE_STATE_METRICS_ARRAYS = new PropertyDescriptor
		.Builder().name( "DEVICE_STATE_METRICS_ARRAYS" )
		.displayName( "Device state metrics arrays" )
		.description( "Specifies the measure fields for which metrics arrays must be produced. The metrics arrays will contain all the values for the specified fields, this allows to build metrics. Each array will be named as the corresponding field. The \"value_name\" array is added by default and it will contain all the measures names. This property must be a comma-separated list of measure sub-fields." )
		.required( false )
		.addValidator( Validator.VALID )
		.build();

	// Dynamic Properties
	public static String DYNAMIC_PROPERTIES_DESCRIPTION = 
		"A dynamic property attribute specifies a field of the enrichment response object to be added to every enriched object.\nThe field will be named as the property name and the value is picked according to the property value which must be a valid path in the enrichment response object.\n" +
		"The field pointed by the specified path must contains a primitive type." + 
		"Example: 'Service/properties/organization'";
	

	private EnrichDataProperties() { }
}
