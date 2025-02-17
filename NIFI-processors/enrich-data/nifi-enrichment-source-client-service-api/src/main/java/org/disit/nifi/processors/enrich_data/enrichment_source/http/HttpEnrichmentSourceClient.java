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

package org.disit.nifi.processors.enrich_data.enrichment_source.http;

import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;

public abstract class HttpEnrichmentSourceClient extends HttpBaseClient implements EnrichmentSourceClient {
	
	protected HttpEnrichmentSourceClient( int maxPerRoute , int maxTotal ) {
		super( maxPerRoute , maxTotal );
	}
	
	protected HttpEnrichmentSourceClient( ProcessContext context ) {
		this( context.getMaxConcurrentTasks() , context.getMaxConcurrentTasks() );
	}

}
