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

package org.disit.nifi.processors.enrich_data.web.api;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.apache.nifi.web.NiFiWebConfigurationContext;

public abstract class AbstractStandardResource {
	
	@Context
	protected ServletContext servletContext;
	
	@Context 
	protected HttpServletRequest request;
	
	protected NiFiWebConfigurationContext getWebConfigurationContext() {
		// Retrieve the Nifi Web configuration context
		return (NiFiWebConfigurationContext) servletContext.getAttribute( "nifi-web-configuration-context" );
	}
	
}

