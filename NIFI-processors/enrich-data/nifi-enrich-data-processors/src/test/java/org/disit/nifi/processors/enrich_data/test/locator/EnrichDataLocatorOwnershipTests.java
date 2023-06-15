/** 
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

package org.disit.nifi.processors.enrich_data.test.locator;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.locators.iotdirectory.IOTDirectoryLocatorControllerService;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipControllerService;
import org.disit.nifi.processors.enrich_data.test.EnrichDataTestBase;
import org.disit.nifi.processors.enrich_data.test.TestUtils;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import groovy.mock.interceptor.MockFor;

/**
 * Unit tests for the EnrichData processor using:
 * 	Servicemap 		<- OAUTH
 *  Ownership
 */
public class EnrichDataLocatorOwnershipTests extends EnrichDataTestBase{
	
	// Ownership
	protected static final String mockOwnershipEndpoint = "/ownership";
	protected static final String ownershipUrl = "http://localhost:" + mockServicemapPort + mockOwnershipEndpoint;
	
	protected OwnershipControllerService ownershipService;
	protected static JsonResourceMockHandler ownership;
	
	// IoTDirectory
	protected static final String iotDirectoryEndpoint = "/iotdirectory";
	protected static final String iotDirectoryUrl = "http://localhost:" +
//	protected static final String iotDirectoryUrl = "http://wronghost:" +
									   mockServicemapPort +
									   iotDirectoryEndpoint;
	
	protected static String subIdAttrName = "subscriptionId";
	protected static String subIdReqName = "sub_ID";
	protected static String serviceUriPrefixRespPath = "content/serviceUriPrefix";
	protected static String organizationRespPath = "content/organization";
	protected static String cbNameRespPath = "content/name";
	protected static String iotDirectoryAdditionalQueryString = "action=get_specific_context_broker";
	
	// IOTDirectory server mock
	// Add the mapping sub_id -> file_path here
	protected static JsonResourceMockHandler iotDirectory;
	protected IOTDirectoryLocatorControllerService iotDirectoryService;
	
	// Setup Ownership CS
	protected void setupOwnershipControllerService() throws InitializationException {
		ownershipService = new OwnershipControllerService();
		String csName = "OwnershipControllerService";
		testRunner.addControllerService( csName , ownershipService );
		testRunner.setProperty( ownershipService , OwnershipControllerService.OWNERSHIP_API_URL , ownershipUrl );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.ELEMENT_ID_NAME , "elementId" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.ELEMENT_ID_PREFIX , "organization:broker:" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.OWNERSHIP_FIELDS , "username" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.FIELDS_MAPPING , "{\"username\":\"owner\"}" );
    	testRunner.setProperty( ownershipService , "elementType" , "IOTID" );
    	testRunner.assertValid( ownershipService );
    	testRunner.enableControllerService( ownershipService );
    	testRunner.setProperty( EnrichData.OWNERSHIP_CLIENT_SERVICE , csName );
	}
	
	// IOTDirectoryLocator CS	
	protected void setupIOTDirectoryControllerService() throws InitializationException{
		String csName = "IOTDirectoryLocatorControllerService";
		iotDirectoryService = new IOTDirectoryLocatorControllerService();
		testRunner.addControllerService( csName , iotDirectoryService );
//			testRunner.setProperty( iotDirectoryService , 
//				IOTDirectoryLocatorControllerService.IOTDIRECTORY_URL , iotDirectoryUrl );
		testRunner.setProperty( iotDirectoryService , 
				IOTDirectoryLocatorControllerService.IOTDIRECTORY_URL , iotDirectoryUrl );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.SUBSCRIPTION_ID_ATTRIBUTE_NAME , subIdAttrName );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.SUBSCRIPTION_ID_REQUEST_NAME , subIdReqName );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.SERVICE_URI_PREFIX_RESPONSE_PATH , serviceUriPrefixRespPath );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.ORGANIZATION_RESPONSE_PATH , organizationRespPath );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.CB_NAME_RESPONSE_PATH , cbNameRespPath );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.ADDITIONAL_QUERY_STRING , iotDirectoryAdditionalQueryString );
		
		testRunner.setProperty( iotDirectoryService ,
				IOTDirectoryLocatorControllerService.MAX_CACHE_SIZE , "50" );
		testRunner.setProperty( iotDirectoryService , 
				IOTDirectoryLocatorControllerService.EXPIRE_CACHE_ENTRIES_TIME , "1 sec" );
		
		testRunner.assertValid( iotDirectoryService );
		testRunner.enableControllerService( iotDirectoryService );
		testRunner.setProperty( EnrichData.ENRICHMENT_RESOURCE_LOCATOR_SERVICE , csName );
	}
	
	protected static void setupOwnershipMock() {
		ownership = new JsonResourceMockHandler( "elementId" , "Ownership Mock Handler" );
		srv.addHandler( ownership , mockOwnershipEndpoint );
	}
	
	protected static void setupIOTDirectoryMock() throws IOException {
		iotDirectory = new JsonResourceMockHandler( subIdReqName , "IOTDirectory Mock Handler" );
		srv.addHandler( iotDirectory , iotDirectoryEndpoint );
	}
	
	protected static String addOwnershipResource( String deviceIdName , String idPrefix , String inputFilePath , String resourceFilePath ) throws IOException {
		JsonObject inputObj = TestUtils.mockJsonObjFromFile( Paths.get( inputFilePath ) );
		String deviceId = inputObj.get( deviceIdName ).getAsString();
		
		StringBuilder ownershipIdentifier = new StringBuilder( idPrefix ).append( deviceId );
		ownership.addJsonResourceFromFile( ownershipIdentifier.toString() , resourceFilePath );
		return deviceId;
	}
	
	protected static String addIOTDirectoryResource( String subId , String resourceFilePath , String serviceUriPrefixPath ) throws IOException {
		JsonObject resourceObj = TestUtils.mockJsonObjFromFile( Paths.get( resourceFilePath ) );
		
		List<String> path = Arrays.asList( serviceUriPrefixPath.split("/") );
		JsonElement cur = resourceObj;
		for( String p : path ) {
			cur = cur.getAsJsonObject().get( p );
		}
		String resourceUriPrefix = cur.getAsString();
		
		iotDirectory.addJsonResource( subId , resourceObj );
		return resourceUriPrefix;
	}
	
	@BeforeClass
	public static void startServices() throws Exception {
		setupJettyLogging();
//		if( srv.isRunning() )
//			srv.stop();
		setupServicemapMock( false );
		setupOwnershipMock();
		setupIOTDirectoryMock();
		srv.start();
	}
	
	@Override
	public void init() throws Exception {
		super.init();
		setupOwnershipControllerService();
		setupIOTDirectoryControllerService();
		validateProcessorProperties();
	}
	
	@Test
	public void testLocatorOwnership() throws IOException {
		System.out.println( "######## " + testName() + " ########" );
		addOwnershipResource( "id" , "Organization:orionBroker-NAME:", 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_ownership_response/ownership_mock.resp" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		addIOTDirectoryResource( 
			"subid_1" , 
			"src/test/resources/mock_iotdirectory_response/subid_1.json" ,
			serviceUriPrefixRespPath 
		);
		configureFFAttributes( ImmutableMap.of( "subscriptionId" , "subid_1" ) );
		
//		MockFlowFile inputFF = testRunner.enqueue( Paths.get( "src/test/resources/mock_in_ff/testOutputs.ff" ) );
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testOutputs.ff" );
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
		
		JsonObject expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/ownership/testOutputs_ownershipJsonObject.ref" , 
			inputFF ).getAsJsonObject();
		
		MockFlowFile outFF = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP ).get(0);
//		TestUtils.prettyOutFF(outFF);
		System.out.println( TestUtils.prettyOutFF( outFF ) );
		JsonElement content = JsonParser.parseString( new String( outFF.toByteArray() ) );
		assertEquals( true , content.isJsonObject() );
		assertEquals( true , expectedResult.equals( content.getAsJsonObject() ) );
	}
	
	public void testFailure() throws IOException {
		System.out.println( "######## " + testName() + " ########" );
		addOwnershipResource( "id" , "Organization:orionBroker-NAME:", 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_ownership_response/ownership_mock.resp" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		addIOTDirectoryResource( 
			"subid_1" , 
			"src/test/resources/mock_iotdirectory_response/subid_1.json" ,
			serviceUriPrefixRespPath 
		);
		configureFFAttributes( ImmutableMap.of( "subscriptionId" , "subid_1" ) );
	}
	
}
