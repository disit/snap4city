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

package org.disit.nifi.processors.enrich_data.enrichment_source;

import java.io.UnsupportedEncodingException;

import com.google.gson.JsonElement;

/**
 * Interface for the Enrichment Source Client.
 * A class implementing this interface is responsible to 
 * retrieve enrichment objects from a remote source 
 * and supply them to the caller as JsonObject instances.
 */
public interface EnrichmentSourceClient {

	public JsonElement getEnrichmentData( String arg ) throws EnrichmentSourceException;
	
	public JsonElement getEnrichmentData( String arg1 , String arg2 ) throws EnrichmentSourceException;
	
	public void close() throws EnrichmentSourceException;
	
	public String buildRequestUrl( String arg1 , String arg2 ) throws UnsupportedEncodingException;
	
	public String buildRequestUrl( String arg ) throws UnsupportedEncodingException;
}
