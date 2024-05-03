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

import { ProcessorConfigApp } from "./modules/processor-config-app.js";

// *************************
// Main
// *************************

var urlParams = new URLSearchParams( document.location.search );
var params = {};
for ( var key of urlParams.keys() ){
	params[key] = urlParams.get(key);
}


var processorId = params['id'];
var revision = params['revision'];
var clientId = params['clientId'];
var editable = params['editable'];
var disconnectedNodeAcknowledged = params['disconnectedNodeAcknowledged'];

var app = new ProcessorConfigApp({
	processorId: processorId,
	revision: revision,
	clientId: clientId,
	editable: editable,
	disconnectedNodeAcknowledged: disconnectedNodeAcknowledged
});
app.configureGroup( "Enrichment sources" ,
	[
		"ENRICHMENT_SOURCE_CLIENT_SERVICE" ,
		"ENRICHMENT_RESPONSE_BASE_PATH" ,
		"ENRICHMENT_BEHAVIOR" ,
		"ENRICHMENT_RESOURCE_LOCATOR_SERVICE" ,
		"SERVICE_URI_PREFIX_FROM_ATTRIBUTE",
		"DEVICE_ID_FROM_ATTRIBUTE" ,
		"OWNERSHIP_CLIENT_SERVICE" ,
		"OWNERSHIP_BEHAVIOR" ,
		"DEFAULT_OWNERSHIP_PROPERTIES",
		"EXTRACT_ENRICHMENT_ATTRIBUTES"
	]
)
.configureJsonProperty( "DEFAULT_OWNERSHIP_PROPERTIES" )
.configureGroup( "Fields" ,
	[
		"DEVICE_ID_NAME" ,
		"DEVICE_ID_NAME_MAPPING" ,
		"DEVICE_ID_VALUE_PREFIX_SUBST" ,
		"VALUE_FIELD_NAME" ,
//		"URI_PREFIX_FROM_ATTR_NAME" ,
		"PURGE_FIELDS" ,
		"ATTEMPT_STRING_VALUES_PARSING"
	]
)
.configureJsonProperty( "DEVICE_ID_VALUE_PREFIX_SUBST" )
.configureGroup( "Timestamp" ,
	[
		"TIMESTAMP_FIELD_NAME" ,
		"TIMESTAMP_FROM_CONTENT_PROPERTY_NAME" ,
		"TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE" ,
		"TIMESTAMP_THRESHOLD"
	]
)
.configureGroup( "Coordinates" ,
	[
		"LATLON_PRIORITY" ,
		"ENRICHMENT_LAT_LON_PATH" ,
		"ENRICHMENT_LAT_LON_FORMAT" ,
		"INNER_LAT_LON_CONFIG"
	]
)
.configureJsonProperty( "INNER_LAT_LON_CONFIG" )
.configureGroup( "Output" ,
	[
		"OUTPUT_FF_CONTENT_FORMAT" ,
		"HASHED_ID_FIELDS" ,
		"ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG" ,
		"ES_INDEX" ,
		"ES_TYPE" ,
		"DEVICE_STATE_OUTPUT_FORMAT" ,
		"DEVICE_STATE_UPDATE_FREQUENCY_FIELD" ,
		"DEVICE_STATE_DROP_UPDATE_THRESHOLD" ,
		"DEVICE_STATE_METRICS_ARRAYS"
	]
)
.configureJsonProperty( "ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG" )
.configureGroup( "Other" ,
	[
		"NODE_CONFIG_FILE_PATH" ,
		"SRC_PROPERTY" ,
		"KIND_PROPERTY"
	]
)
.init();
