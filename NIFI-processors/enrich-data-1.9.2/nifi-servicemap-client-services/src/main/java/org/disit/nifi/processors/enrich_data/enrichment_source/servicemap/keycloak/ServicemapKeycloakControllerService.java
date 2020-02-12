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

package org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.keycloak;

import java.util.ArrayList;
import java.util.List;

import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapConfigs;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapControllerService;

public class ServicemapKeycloakControllerService extends ServicemapControllerService implements ServicemapKeycloakClientService{
	
	private static List<PropertyDescriptor> descs = new ArrayList<>();
	
	static {
		descs.addAll( ServicemapControllerService.descriptors );
		descs.add( KEYCLOAK_URL );
		descs.add( CLIENT_ID );
		descs.add( CLIENT_SECRET );
		descs.add( REALM );
		descs.add( USERNAME );
		descs.add( PASSWORD );
	}
	
	private KeycloakConfigs kc;
	
	@Override
	protected List<PropertyDescriptor> getSupportedPropertyDescriptors(){
		return descs;
	}
	
	@Override
	@OnEnabled
	public void onEnable( final ConfigurationContext context ) {
		super.onEnable( context );
		
		kc = new KeycloakConfigs();
		
		kc.keycloakUrl = context.getProperty( KEYCLOAK_URL ).getValue();
		kc.clientId = context.getProperty( CLIENT_ID ).getValue();
		kc.clientSecret = context.getProperty( CLIENT_SECRET ).getValue();
		kc.username = context.getProperty( USERNAME ).getValue();
		kc.password = context.getProperty( PASSWORD ).getValue();
		kc.realm = context.getProperty( REALM ).getValue();
	}
	
	@Override
	@OnDisabled 
	public void onDisable() { 
		super.onDisable();
	}
	
	@Override
	@OnStopped
	public void onStop() {
		super.onStop();
	}

	@Override
	public EnrichmentSourceClient getClient(ProcessContext context ) throws InstantiationException {
		ServicemapKeycloakClient client;
		
		ServicemapConfigs sc = new ServicemapConfigs( this.servicemapUrl , this.serviceUriPrefix , this.additionalQueryString );
		
		if( context == null ) {
			client = new ServicemapKeycloakClient( sc , kc );
		} else {
			client = new ServicemapKeycloakClient( sc , kc , context );
		}
		
		if( !additionalQueryString.isEmpty() ) {
			client.setAdditionalQueryString( additionalQueryString );
		}
		
		return client;
	}
	
}
