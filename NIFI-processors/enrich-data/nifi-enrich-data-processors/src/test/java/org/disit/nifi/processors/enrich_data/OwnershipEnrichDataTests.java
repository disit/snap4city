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

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipControllerService;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.disit.nifi.processors.enrich_data.utility_processors.OwnershipEnrichData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OwnershipEnrichDataTests {

	private TestRunner testRunner;
	JsonParser parser;
	
	private OwnershipControllerService ownershipService;
	
	private int mockServerPort = 8090;
	private String mockOwnershipEndpoint = "/ownership";
    private String ownershipUrl = "http://localhost:" + mockServerPort + mockOwnershipEndpoint;
	
    private SimpleProtectedAPIServer srv;
    private JsonResourceMockHandler ownership;
    
    public static final String DEVICE_ID_NAME_VALUE = "deviceName";
    
	@Before
	public void init() throws Exception{
		// Jetty Logging
    	System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
    	
    	parser = new JsonParser();
    	
    	testRunner = TestRunners.newTestRunner( OwnershipEnrichData.class );
    	testRunner.setProperty( OwnershipEnrichData.DEVICE_ID_NAME , "deviceName" );
    	testRunner.setProperty( OwnershipEnrichData.USE_DEFAULTS_ON_OWNERSHIP_SERVICE_ERROR , "true" );
    	testRunner.setProperty( OwnershipEnrichData.DEFAULT_OWNERSHIP_PROPERTIES , "{\"username\":\"123456\"}" );
    	testRunner.setProperty( OwnershipEnrichData.ADDITIONAL_DEFAULT_OWNERSHIP_PROPERTIES , "{\"delegations\":[]}" );
    	
    	setupOwnershipControllerService();
    	
    	srv = new SimpleProtectedAPIServer( mockServerPort );
    	ownership = new JsonResourceMockHandler( "elementId" );
        srv.addHandler( ownership , mockOwnershipEndpoint );
        
        srv.start();
	}
	
	public void setupOwnershipControllerService() throws InitializationException{
		ownershipService = new OwnershipControllerService();
    	testRunner.addControllerService( "OwnershipControllerService" , ownershipService );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.OWNERSHIP_API_URL , this.ownershipUrl );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.ELEMENT_ID_NAME , "elementId" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.ELEMENT_ID_PREFIX , "broker:organization:" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.OWNERSHIP_FIELDS , "uid" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.FIELDS_MAPPING , "{\"uid\":\"username\"}" );
    	testRunner.setProperty( ownershipService , "elementType" , "IOTID" );
    	testRunner.assertValid( ownershipService );
    	testRunner.enableControllerService( ownershipService );
    	testRunner.setProperty( OwnershipEnrichData.OWNERSHIP_CLIENT_SERVICE , "OwnershipControllerService" );
	}
	
//	@Test
	public void testOwnershipEnrichData() throws IOException {
		String mockInFFContent = TestUtils.mockJsonStringFromFile( 
			Paths.get("src/test/resources/mock_in_ff/testOwnershipEnrichData.ff") , 
			parser );
		
		JsonObject inFFObj = parser.parse( mockInFFContent ).getAsJsonObject();
		String deviceId = inFFObj.get( DEVICE_ID_NAME_VALUE ).getAsString();
		
		ownership.addJsonResourceFromFile( 
			"broker:organization:" + deviceId , 
			"src/test/resources/mock_ownership_response/ownership_OwnershipEnrichData.resp"
		);
		
		testRunner.enqueue( mockInFFContent );
		testRunner.run();
		
		testRunner.assertAllFlowFilesTransferred( OwnershipEnrichData.SUCCESS_RELATIONSHIP );
		testRunner.assertTransferCount( OwnershipEnrichData.SUCCESS_RELATIONSHIP.getName() , 1 );
		
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( OwnershipEnrichData.SUCCESS_RELATIONSHIP );
		
		outFFList.stream().forEach(
			(MockFlowFile ff) -> {
				System.out.println( TestUtils.prettyOutFF( ff , parser ) );
			}
		);
		
		JsonObject referenceOut = TestUtils.mockJsonObjFromFile( 
			Paths.get("src/test/resources/reference_results/ownership/testOutputs_OwnershipEnrichData.ref" ) , 
			parser
		);
		
		JsonObject result = parser.parse( new String( outFFList.get(0).toByteArray() ) )
								  .getAsJsonObject();
		
		assertEquals( referenceOut , result );
	}
	
	@Test
	public void testDefaults() throws IOException {
		String mockInFFContent = "{ \"deviceName\":\"error404\"}";
			
		JsonObject inFFObj = parser.parse( mockInFFContent ).getAsJsonObject();
		String deviceId = inFFObj.get( DEVICE_ID_NAME_VALUE ).getAsString();
		
		testRunner.enqueue( mockInFFContent );
		testRunner.run();
		
		testRunner.assertAllFlowFilesTransferred( OwnershipEnrichData.SUCCESS_RELATIONSHIP );
		testRunner.assertTransferCount( OwnershipEnrichData.SUCCESS_RELATIONSHIP.getName() , 1 );
		
		
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( OwnershipEnrichData.SUCCESS_RELATIONSHIP );
		
		outFFList.stream().forEach(
			(MockFlowFile ff) -> {
				System.out.println( TestUtils.prettyOutFF( ff , parser ) );
			}
		);
		
		JsonObject referenceOut = TestUtils.mockJsonObjFromFile( 
			Paths.get("src/test/resources/reference_results/ownership/testOutputs_OwnershipEnrichData_default.ref" ) , 
			parser
		);
		
		JsonObject result = parser.parse( new String( outFFList.get(0).toByteArray() ) )
								  .getAsJsonObject();
		
		assertEquals( referenceOut , result );
	}
	
	@After
	public void tearDown() throws Exception {
		srv.close();
	}

}
