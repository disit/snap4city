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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.disit.nifi.processors.enrich_data.enricher.converter.DeviceStateConverter;

public class EnrichDataConstants {
	
	public static final String OWNERSHIP_BEHAVIOR_FAILURE_ON_ERR = "Route to failure on ownership error.";
	public static final String OWNERSHIP_BEHAVIOR_DEFAULTS_ON_ERR = "Use defaults on ownership error.";
	public static final String OWNERSHIP_BEHAVIOR_DEFAULTS_IF_NO_CONTROLLER = "Use defaults if no controller service configured.";
	
	public static final String OWNERSHIP_BEHAVIOR_VALUES[] = {
		OWNERSHIP_BEHAVIOR_FAILURE_ON_ERR ,
		OWNERSHIP_BEHAVIOR_DEFAULTS_ON_ERR ,
		OWNERSHIP_BEHAVIOR_DEFAULTS_IF_NO_CONTROLLER
	};
	
//	public static final String OWNERSHIP_BEHAVIOR_VALUES[] = {
//		"Route to failure on ownership error." , 
//		"Use defaults on ownership error." ,
//		"Use defaults if no controller service configured."
//	};
	
	public static final String OUTPUT_FF_CONTENT_FORMAT_JSON = "JSON";
	public static final String OUTPUT_FF_CONTENT_FORMAT_ES_BULK = "Elasticsearch bulk indexing compliant";
	public static final String OUTPUT_FF_CONTENT_FORMAT_SPLIT_JSON = "Split Json Object";
	
	public static final String OUTPUT_FF_CONTENT_FORMAT_VALUES[] = { 
			OUTPUT_FF_CONTENT_FORMAT_JSON , 
			OUTPUT_FF_CONTENT_FORMAT_ES_BULK , 
			OUTPUT_FF_CONTENT_FORMAT_SPLIT_JSON
	};
	
//	public static final String OUTPUT_FF_CONTENT_FORMAT_VALUES[] = { 
//			"JSON" , 
//			"Elasticsearch bulk indexing compliant" , 
//			"Split Json Object"
//	};
	
	public static final String ENRICHMENT_BEHAVIOR_REMOVE_NOT_MATCHED = "Remove not matched";
	public static final String ENRICHMENT_BEHAVIOR_KEEP_NOT_MATCHED = "Keep not matched";
	public static final String ENRICHMENT_BEHAVIOR_VALUES[] = {
		ENRICHMENT_BEHAVIOR_REMOVE_NOT_MATCHED ,
		ENRICHMENT_BEHAVIOR_KEEP_NOT_MATCHED
	};
	
//	public static final String ENRICHMENT_BEHAVIOR_VALUES[] = {
//		"Remove not matched" , 
//		"Keep not matched"
//	};
	
	public static final String ENRICHMENT_LAT_LON_FORMAT_LAT_FIRST = "[lat , lon]";
	public static final String ENRICHMENT_LAT_LON_FORMAT_LON_FIRST = "[lon , lat]";
	
	public static final String ENRICHMENT_LAT_LON_FORMAT_VALUES[] = {
		ENRICHMENT_LAT_LON_FORMAT_LAT_FIRST ,
		ENRICHMENT_LAT_LON_FORMAT_LON_FIRST
	};
	
//	public static final String ENRICHMENT_LAT_LON_FORMAT_VALUES[] = {
//		"[lat , lon]" ,
//		"[lon , lat]"
//	};
	
	public static final String LATLON_PRIORITY_ENRICHMENT_SOURCE_FIRST = "Enrichment source response property first."; 
	public static final String LATLON_PRIORITY_FF_CONTENT_FIRST = "Flow file content object property first.";
	
	public static final String LATLON_PRIORITY_VALUES[] = {
		LATLON_PRIORITY_ENRICHMENT_SOURCE_FIRST,
		LATLON_PRIORITY_FF_CONTENT_FIRST
	};
	
//	public static final String LATLON_PRIORITY_VALUES[] = {
//		"Enrichment source response property first." ,
//		"Flow file content object property first."
//	};
	
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

	public static final Set<String> DEVICE_STATE_OUTPUT_FORMAT_VALUES = Collections.unmodifiableSet(
		new HashSet<>( Arrays.asList( 
			DeviceStateConverter.OutputMode.MINIMAL.toString() ,
			DeviceStateConverter.OutputMode.FULL.toString()
		) )
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

	// Attributes/properties names constants
	public static final String SERVICE_URI_OUTPUT_NAME = "serviceUri";
	public static final String COORDS_OUTPUT_NAME = "latlon";
	public static final String SRC_PROPERTY_OUTPUT_NAME = "src";
	public static final String KIND_PROPERTY_OUTPUT_NAME = "kind";
	public static final String DEVICE_ID_ATTRIBUTE_NAME = "deviceId";
	public static final String VALUE_NAME_ATTRIBUTE_NAME = "value_name";
	public static final String UUID_ATTRIBUTE_NAME = "uuid";
	public static final String MIME_TYPE_ATTRIBUTE_NAME = "mime.type";
	public static final String ENTRY_DATE_ATTRIBUTE_NAME = "entry_date";
	
	public static final String FAILURE_ATTRIBUTE_NAME = "failure";
	public static final String FAILURE_CAUSE_ATTRIBUTE_NAME = "failure.cause";

	// File configs
	static final List<String> fileConfigs = Arrays.asList( 
		"timestampFromContent.useFallback" 
	);
	static final int FILE_CONFIGS_USE_FALLBACK = 0;
	
	private EnrichDataConstants() { }
	
}
