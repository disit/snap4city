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
		return (NiFiWebConfigurationContext) servletContext.getAttribute( "nifi-web-configuration-context" );
	}
	
}

