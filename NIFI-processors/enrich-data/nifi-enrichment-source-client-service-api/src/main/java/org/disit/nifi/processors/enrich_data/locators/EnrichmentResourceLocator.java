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


package org.disit.nifi.processors.enrich_data.locators;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;

public interface EnrichmentResourceLocator {

//	public String getResourceLocation( String resourceReference ) throws EnrichmentResourceLocatorException;
//	
//	public String getResourceLocation( FlowFile ff ) throws EnrichmentResourceLocatorException;
	
	public ResourceLocations getResourceLocations( String resourceReference ) throws EnrichmentResourceLocatorException;
	
	public ResourceLocations getResourceLocations( FlowFile ff ) throws EnrichmentResourceLocatorException;
	
	public String buildRequestUrl( String resourceReference );
	
	public String buildRequestUrl( FlowFile ff ) throws EnrichmentResourceLocatorException;
	
	public void setLogger( ComponentLog logger );
}
