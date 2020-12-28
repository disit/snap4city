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

package org.disit.nifi.processors.enrich_data;

import static org.junit.Assert.assertEquals;

import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipClientConfig;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipHttpClient;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class OwnershipClientTests {

	SimpleProtectedAPIServer srv;
	JsonResourceMockHandler handler;
	
	int ownershipMockPort = 8090;
	String ownershipMockEndpoint = "/ownership";
	String ownershipApiUrl = "http://localhost:" + ownershipMockPort + ownershipMockEndpoint;
	
	@Before
	public void init() throws Exception{
		System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
		
		srv = new SimpleProtectedAPIServer( ownershipMockPort );
		handler = new JsonResourceMockHandler( "elementId" );
		srv.addHandler( handler , ownershipMockEndpoint );
		srv.start();
		
		JsonObject resource1 = new JsonObject();
		resource1.addProperty( "username" , "user1" );
		JsonArray arrayProp = new JsonArray();
		arrayProp.add( "element1" );
		arrayProp.add( "element2" );
		arrayProp.add( "element3" );
		resource1.add( "arrayProp" , arrayProp );
		
		handler.addJsonResource( "resource1" , resource1 );
//		srv.addHandler( handler , "ownership" );
	}
	
//	@Test
	public void testOwnershipClient() throws EnrichmentSourceException , Exception{
		OwnershipClientConfig config;
		
		String resourceIdentifier = "resource1";
		
		config = new OwnershipClientConfig( ownershipApiUrl , "elementId" );
		config.addOwnershipField( "username" );
		config.addOwnershipField( "arrayProp" );
		
		OwnershipHttpClient client = new OwnershipHttpClient( config , 1 , 1 );
		JsonElement response = client.getEnrichmentData( resourceIdentifier );
		
		assertEquals( handler.getResource( resourceIdentifier ) , response );
	}
	
	@After
	public void tearDown() throws Exception {
		srv.close();
	}

}
