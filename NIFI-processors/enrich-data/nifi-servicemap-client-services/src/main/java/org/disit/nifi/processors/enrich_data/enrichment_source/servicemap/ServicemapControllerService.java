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

import java.util.ArrayList;
import java.util.List;

import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.reporting.InitializationException;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceUpdater;

/**
 * Provides a ServicemapClient to the processor.
 */
public class ServicemapControllerService extends AbstractControllerService implements ServicemapClientService {

	protected static List<PropertyDescriptor> descriptors = new ArrayList<>();
	
	static {
		descriptors.add( SERVICEMAP_URL );
		descriptors.add( SERVICE_URI_PREFIX );
		descriptors.add( ADDITIONAL_QUERY_STRING );
	}
	
	protected String servicemapUrl;
	protected String serviceUriPrefix;
	protected String additionalQueryString;
	
	protected ServicemapConfigs servicemapConfigs;
	
	@Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors(){
		return descriptors;
	}
	
	@OnEnabled
	public void onEnable(final ConfigurationContext context) throws InitializationException {
		configBaseProperties( context );
	}
	
	protected void configBaseProperties( ConfigurationContext context ) {
		this.servicemapUrl = context.getProperty( SERVICEMAP_URL ).getValue();
		this.serviceUriPrefix = context.getProperty( SERVICE_URI_PREFIX ).getValue();
		if( !this.serviceUriPrefix.endsWith( "/" ) )
			this.serviceUriPrefix = this.serviceUriPrefix.concat( "/" );
		
		this.additionalQueryString = context.getProperty( ADDITIONAL_QUERY_STRING ).getValue();
		if( !this.additionalQueryString.isEmpty() && !this.additionalQueryString.startsWith("&") )
			this.additionalQueryString = String.format( "&%s" , this.additionalQueryString );
		
		servicemapConfigs = new ServicemapConfigs( 
			this.servicemapUrl ,
			this.serviceUriPrefix , 
			this.additionalQueryString
		);
	}
	
	@OnDisabled
	public void onDisable() { }
	
	@OnStopped
	public void onStop() {
		// TODO needed ?
	}

	/**
	 * Provides a parallel client capable of retrieving data from servicemap throught 
	 * an HTTP GET request.
	 */
	@Override
	public EnrichmentSourceClient getClient( ProcessContext context ) throws InstantiationException {
//		ServicemapClient client;
		EnrichmentSourceClient client;
		if( context == null ) {
//			client = new ServicemapClient( servicemapUrl , serviceUriPrefix );
			client = new ServicemapHttpClient( this.servicemapConfigs );
		}else {
//			client = new ServicemapClient( servicemapUrl , serviceUriPrefix , context );
			client = new ServicemapHttpClient( this.servicemapConfigs , context );
		}
		
//		if( !additionalQueryString.isEmpty() )
//			client.setAdditionalQueryString( additionalQueryString );
		
		return client;
	}
	
	/**
	 * Provides a parallel client capable of retrieving data from servicemap throught 
	 * an HTTP POST request.
	 */
	@Override 
	public EnrichmentSourceUpdater getUpdaterClient( String endpoint , ProcessContext context  ) throws InstantiationException{
		ServicemapHttpUpdater updaterClient;
		if( context == null ) {
			updaterClient = new ServicemapHttpUpdater( endpoint , this.servicemapConfigs );
		}else {
			updaterClient = new ServicemapHttpUpdater( endpoint , this.servicemapConfigs , context );
		}
		
		return updaterClient;
	}
	
}
