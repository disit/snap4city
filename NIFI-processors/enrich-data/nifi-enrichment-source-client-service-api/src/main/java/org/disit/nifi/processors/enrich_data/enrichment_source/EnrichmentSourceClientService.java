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

import org.apache.nifi.controller.ControllerService;
import org.apache.nifi.processor.ProcessContext;

/**
 * Interface for a generic EnrichmentSourceClientService.
 * A class implementing this interface is responsible to provide an
 * EnrichmentSourceClient implementation.
 */
public interface EnrichmentSourceClientService extends ControllerService{

	public EnrichmentSourceClient getClient( ProcessContext contex ) throws InstantiationException;
	
}
