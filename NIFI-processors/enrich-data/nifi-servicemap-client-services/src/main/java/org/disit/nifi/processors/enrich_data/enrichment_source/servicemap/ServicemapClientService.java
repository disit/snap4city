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

package org.disit.nifi.processors.enrich_data.enrichment_source.servicemap;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.processor.util.StandardValidators;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceUpdaterService;

/**
 * ServicemapClientService interface.
 * 
 * This interface defines the property descriptors common
 * to all the ServicemapClientService implementations 
 */
public interface ServicemapClientService 
       extends EnrichmentSourceClientService, EnrichmentSourceUpdaterService{
 
	public static final PropertyDescriptor SERVICEMAP_URL = new PropertyDescriptor
            .Builder().name( "SERVICEMAP_URL" )
            .displayName("ServiceMap URL")
            .description( "The URL of the Servicemap instance to connect to." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
	
	public static final PropertyDescriptor SERVICE_URI_PREFIX = new PropertyDescriptor
            .Builder().name( "SERVICE_URI_PREFIX" )
            .displayName("Service Uri Prefix")
            .description("The service uri prefix. The value of this property will be concatenated to the device id, and used as argument for the 'serviceUri' paramenter for the request to servicemap." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
	
	public static final PropertyDescriptor ADDITIONAL_QUERY_STRING = new PropertyDescriptor
            .Builder().name( "ADDITIONAL_QUERY_STRING" )
            .displayName("Additional query string")
            .description( "The query string for the servicemap request. This allows to specify additional parameters to add to the query string along with the 'serviceUri' (which is automatically inserted).\nExample: realtime=false." )
            .required(false)
            .defaultValue( "" )
            .addValidator(Validator.VALID)
            .build();
	
	/**
	 * The interface methods are inherited from the EnrichmentSourceClientService interface.
	 */
	
}
