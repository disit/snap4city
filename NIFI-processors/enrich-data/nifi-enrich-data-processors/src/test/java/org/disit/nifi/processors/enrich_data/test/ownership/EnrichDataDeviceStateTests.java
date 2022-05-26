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

package org.disit.nifi.processors.enrich_data.test.ownership;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.nifi.processor.Relationship;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.enricher.converter.DeviceStateConverter;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipControllerService;
import org.disit.nifi.processors.enrich_data.test.EnrichDataTestBase;
import org.disit.nifi.processors.enrich_data.test.TestUtils;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EnrichDataDeviceStateTests extends EnrichDataTestBase{
	
	protected static final String mockOwnershipEndpoint = "/ownership";
	protected static final String ownershipUrl = "http://localhost:" + mockServicemapPort + mockOwnershipEndpoint;
	
	protected OwnershipControllerService ownershipService;
	
	protected static JsonResourceMockHandler ownership;
	
	protected void setupOwnershipControllerService() throws InitializationException {
		ownershipService = new OwnershipControllerService();
		String csName = "OwnershipControllerService";
		testRunner.addControllerService( csName , ownershipService );
		testRunner.setProperty( ownershipService , OwnershipControllerService.OWNERSHIP_API_URL , ownershipUrl );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.ELEMENT_ID_NAME , "elementId" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.ELEMENT_ID_PREFIX , "broker:organization:" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.OWNERSHIP_FIELDS , "username" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.FIELDS_MAPPING , "{\"username\":\"owner\"}" );
    	testRunner.setProperty( ownershipService , "elementType" , "IOTID" );
    	testRunner.assertValid( ownershipService );
    	testRunner.enableControllerService( ownershipService );
    	testRunner.setProperty( EnrichData.OWNERSHIP_CLIENT_SERVICE , csName );
    	testRunner.addConnection( EnrichData.DEVICE_STATE_RELATIONSHIP );
    	System.out.println( "Hash connection: " + 
    		testRunner.getProcessContext().hasConnection( EnrichData.DEVICE_STATE_RELATIONSHIP )
		);
	}
	
	protected static void setupOwnershipMock() {
		ownership = new JsonResourceMockHandler( "elementId" , "Ownership Mock Handler" );
		srv.addHandler( ownership , mockOwnershipEndpoint );
	}
	
	@BeforeClass
	public static void startServices() throws Exception {
		setupJettyLogging();
//		if( srv.isRunning() )
//			srv.stop();
		setupServicemapMock( false );
		setupOwnershipMock();
		srv.start();
	}

	@Override
	public void init() throws Exception {
		super.init(); 
		setupOwnershipControllerService();
	}
	
	protected static String addOwnershipResource( String deviceIdName , String idPrefix , String inputFilePath , String resourceFilePath ) throws IOException {
		JsonObject inputObj = TestUtils.mockJsonObjFromFile( Paths.get( inputFilePath ) , parser );
		String deviceId = inputObj.get( deviceIdName ).getAsString();
		
		StringBuilder ownershipIdentifier = new StringBuilder( idPrefix ).append( deviceId );
		ownership.addJsonResourceFromFile( ownershipIdentifier.toString() , resourceFilePath );
		return deviceId;
	}
	
	@Test
	public void testDeviceStateMinimal() throws IOException {
		System.out.println( "######## " + testName() + " ########" );
		addOwnershipResource( "id" , "broker:organization:", 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_ownership_response/ownership_mock.resp" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		MockFlowFile inputFF = testRunner.enqueue( Paths.get( "src/test/resources/mock_in_ff/testOutputs.ff" ) );
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichData.DEVICE_STATE_RELATIONSHIP , 1 );
		
		JsonObject expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/ownership/testOutputs_ownershipJsonObject.ref" , 
			inputFF , parser ).getAsJsonObject();
		
		JsonObject expectedState = TestUtils.prepareExpectedDeviceState( 
			"src/test/resources/reference_results/ownership/testOutputs_DeviceState_MINIMAL.ff", 
			inputFF , parser ).getAsJsonObject();
		
		JsonElement content = parser.parse( new String( testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP ).get(0).toByteArray() ) );
		assertEquals( true , content.isJsonObject() );
		assertEquals( true , expectedResult.equals( content.getAsJsonObject() ) );
		
		JsonElement deviceStateContent = parser.parse( new String( testRunner.getFlowFilesForRelationship( EnrichData.DEVICE_STATE_RELATIONSHIP ).get(0).toByteArray() ) );
		assertEquals( true , deviceStateContent.isJsonObject() );
		assertEquals( true , expectedState.equals( deviceStateContent.getAsJsonObject() ) );
		System.out.println( deviceStateContent.toString() );
	}
	
	@Test
	public void testDeviceStateFull() throws IOException {
		System.out.println( "######## " + testName() + " ########" );
		testRunner.setProperty( 
			EnrichData.DEVICE_STATE_OUPUT_FORMAT , 
			DeviceStateConverter.OutputMode.FULL.toString() );
		
		addOwnershipResource( "id" , "broker:organization:", 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_ownership_response/ownership_mock.resp" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		MockFlowFile inputFF = testRunner.enqueue( Paths.get( "src/test/resources/mock_in_ff/testOutputs.ff" ) );
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichData.DEVICE_STATE_RELATIONSHIP , 1 );
		
		JsonObject expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/ownership/testOutputs_ownershipJsonObject.ref" , 
			inputFF , parser ).getAsJsonObject();
		
		JsonObject expectedState = TestUtils.prepareExpectedDeviceState( 
			"src/test/resources/reference_results/ownership/testOutputs_DeviceState_FULL.ff", 
			inputFF , parser ).getAsJsonObject();
		
		JsonElement content = parser.parse( new String( testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP ).get(0).toByteArray() ) );
		assertEquals( true , content.isJsonObject() );
		assertEquals( true , expectedResult.equals( content.getAsJsonObject() ) );
		
		JsonElement deviceStateContent = parser.parse( new String( testRunner.getFlowFilesForRelationship( EnrichData.DEVICE_STATE_RELATIONSHIP ).get(0).toByteArray() ) );
		assertEquals( true , deviceStateContent.isJsonObject() );
		assertEquals( true , expectedState.equals( deviceStateContent.getAsJsonObject() ) );
//		System.out.println( deviceStateContent.toString() );
	}


}
