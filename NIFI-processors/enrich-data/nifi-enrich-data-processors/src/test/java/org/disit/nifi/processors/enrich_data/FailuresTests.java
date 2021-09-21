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
import java.time.Instant;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapControllerService;
import org.disit.nifi.processors.enrich_data.test.TestUtils;
import org.disit.nifi.processors.enrich_data.test.api_mock.ServicemapMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// TODO: Refactor

public class FailuresTests {

	private TestRunner testRunner;
    JsonParser jsonParser;
    
    private ServicemapControllerService servicemapService;
    
    private static final String serviceUriPrefix = "http://serviceuriprefix.org";
    private static final String additionalQueryString = "realtime=false";
    
    private int mockServicemapPort = 8090;
    private String mockServicemapEndpoint = "/servicemap";
    private String servicemapUrl = "http://localhost:" + mockServicemapPort + mockServicemapEndpoint;
    
    private SimpleProtectedAPIServer srv;
    private ServicemapMockHandler servicemap;
    
    @Before
    public void init() throws Exception{
    	// Jetty Logging
    	System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
    	
    	testRunner = TestRunners.newTestRunner(EnrichData.class);
        jsonParser = new JsonParser();
        
        
        testRunner.setProperty( EnrichData.DEVICE_ID_NAME , "id" );
        testRunner.setProperty( EnrichData.DEVICE_ID_NAME_MAPPING , "sensorId" );
        // testRunner.setProperty( EnrichData.DEVICE_ID_VALUE_PREFIX_SUBST , "{ \"TA-\" : \"TA__\"}" );
        testRunner.setProperty( EnrichData.TIMESTAMP_FIELD_NAME , "date_time" );
        testRunner.setProperty( EnrichData.TIMESTAMP_FROM_CONTENT_PROPERTY_NAME , "value_type" );
        testRunner.setProperty( EnrichData.TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE , "timestamp" );
        testRunner.setProperty( EnrichData.VALUE_FIELD_NAME , "value" );
        testRunner.setProperty( EnrichData.ENRICHMENT_RESPONSE_BASE_PATH , "Service/features/properties/realtimeAttributes" );
        testRunner.setProperty( EnrichData.ENRICHMENT_LAT_LON_PATH , "Service/features/geometry/coordinates" );
        testRunner.setProperty( EnrichData.ENRICHMENT_LAT_LON_FORMAT , "[lon , lat]" );
        testRunner.setProperty( EnrichData.ENRICHMENT_BEHAVIOR , EnrichData.ENRICHMENT_BEHAVIOR_VALUES[0] );
        testRunner.setProperty( EnrichData.SRC_PROPERTY , "IOT" );
        testRunner.setProperty( EnrichData.KIND_PROPERTY , "sensor" );
        testRunner.setProperty( EnrichData.PURGE_FIELDS , "type,metadata,value_bounds,different_values,attr_type" );
        testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] );
        testRunner.setProperty( EnrichData.HASHED_ID_FIELDS , "serviceUri,value_name,date_time" );
        testRunner.setProperty( EnrichData.NODE_CONFIG_FILE_PATH , "src/test/resources/enrich-data.conf" );
        
        testRunner.setProperty( "deviceName" , "Service/features/properties/name" );
        testRunner.setProperty( "organization" , "Service/features/properties/organization" );
        
        setupServicemapControllerService();
        
        srv = new SimpleProtectedAPIServer( mockServicemapPort );
        servicemap = new ServicemapMockHandler();
        servicemap.setVerbose( true );
        srv.addHandler( servicemap , mockServicemapEndpoint );
        
        srv.start();
    }
    
    /**
     * Set up a ServicemapControllerService
     * @throws InitializationException
     */
    public void setupServicemapControllerService() throws InitializationException{
		servicemapService = new ServicemapControllerService();
		testRunner.addControllerService( "ServicemapControllerService" , servicemapService );
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICEMAP_URL , this.servicemapUrl );
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICE_URI_PREFIX , FailuresTests.serviceUriPrefix );
		testRunner.setProperty( servicemapService , ServicemapClientService.ADDITIONAL_QUERY_STRING , FailuresTests.additionalQueryString );
		testRunner.assertValid( servicemapService );
		testRunner.enableControllerService( servicemapService );
		testRunner.setProperty( EnrichData.ENRICHMENT_SOURCE_CLIENT_SERVICE , "ServicemapControllerService" );
    }
    
	/**
	 * Enrichment service server error test 
	 */
    @Test
	public void test500Error() throws Exception{
		
		String failureDeviceId = "error500test";
		servicemap.addError( 
			FailuresTests.serviceUriPrefix + "/" + failureDeviceId , 
			HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
			jsonParser.parse( "{'error':'Internal Server Error'}" ) 
		);
		
		JsonObject mockInFFContent = new JsonObject();
		mockInFFContent.addProperty( "id" , failureDeviceId );
		mockInFFContent.addProperty( "date_time" , Instant.now().toString() );
		
		String uuid = testRunner.enqueue( mockInFFContent.toString() ).getAttribute( "uuid" );
		
		testRunner.run();
		
		testRunner.assertAllFlowFilesTransferred( EnrichData.RETRY_RELATIONSHIP );
		testRunner.assertTransferCount( EnrichData.RETRY_RELATIONSHIP , 1 );
		testRunner.assertAllFlowFilesContainAttribute( EnrichData.RETRY_RELATIONSHIP , "failure" );
		testRunner.assertAllFlowFilesContainAttribute( EnrichData.RETRY_RELATIONSHIP , "failure.cause" );
		
		System.out.println( "-------------- FAILURE_RELATIONSHIP flow files: --------------\n");
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.FAILURE_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
		});
		
		System.out.println( "\n\n-------------- RETRY_RELATIONSHIP flow files: --------------\n");
		outFFList = testRunner.getFlowFilesForRelationship( EnrichData.RETRY_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
		});
	}
	
	/**
	 * Enrichment source unavailable test (connection error)
	 */
//	@Test
	public void testEnrichmentServiceConnectionError() throws Exception {
		srv.close();
		
		String failureDeviceId = "connectionErrorTest";
		JsonObject mockInFFContent = new JsonObject();
		mockInFFContent.addProperty( "id" , failureDeviceId );
		mockInFFContent.addProperty( "date_time" , Instant.now().toString() );
		
		String uuid = testRunner.enqueue( mockInFFContent.toString() ).getAttribute( "uuid" );
		
		testRunner.run();
		
		testRunner.assertAllFlowFilesTransferred( EnrichData.RETRY_RELATIONSHIP );
		testRunner.assertTransferCount( EnrichData.RETRY_RELATIONSHIP , 1 );
		testRunner.assertAllFlowFilesContainAttribute( EnrichData.RETRY_RELATIONSHIP , "failure" );
		testRunner.assertAllFlowFilesContainAttribute( EnrichData.RETRY_RELATIONSHIP , "failure.cause" );
		
		System.out.println( "-------------- FAILURE_RELATIONSHIP flow files: --------------\n");
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.FAILURE_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
		});
		
		System.out.println( "\n\n-------------- RETRY_RELATIONSHIP flow files: --------------\n");
		outFFList = testRunner.getFlowFilesForRelationship( EnrichData.RETRY_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
		});
	}

	/**
	 * Enrichment resource not found error test.
	 */
	@Test
	public void test404Error() {
		String failureDeviceId = "error404test";
		servicemap.addError( 
			FailuresTests.serviceUriPrefix + "/" + failureDeviceId , 
			HttpServletResponse.SC_NOT_FOUND, 
			jsonParser.parse( "{'error':'Not Found'}" ) 
		);
		
		JsonObject mockInFFContent = new JsonObject();
		mockInFFContent.addProperty( "id" , failureDeviceId );
		mockInFFContent.addProperty( "date_time" , Instant.now().toString() );
		
		String uuid = testRunner.enqueue( mockInFFContent.toString() ).getAttribute( "uuid" );
		
		testRunner.run();
		
		testRunner.assertAllFlowFilesTransferred( EnrichData.FAILURE_RELATIONSHIP );
		testRunner.assertTransferCount( EnrichData.FAILURE_RELATIONSHIP , 1 );
		testRunner.assertAllFlowFilesContainAttribute( EnrichData.FAILURE_RELATIONSHIP , "failure" );
		testRunner.assertAllFlowFilesContainAttribute( EnrichData.FAILURE_RELATIONSHIP , "failure.cause" );
		
		System.out.println( "-------------- FAILURE_RELATIONSHIP flow files: --------------\n");
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.FAILURE_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
		});
		
		System.out.println( "\n\n-------------- RETRY_RELATIONSHIP flow files: --------------\n");
		outFFList = testRunner.getFlowFilesForRelationship( EnrichData.RETRY_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
		});
	}
	
	/**
	 * Response processing error test.
	 */
//	@Test
	public void testEnrichmentResponseProcessingError() {
		String failureDeviceId = "errorResponseProcessingTest";
		servicemap.addError( 
			FailuresTests.serviceUriPrefix + "/" + failureDeviceId , 
			HttpServletResponse.SC_OK, 
			// Service/features/properties/realtimeAttributes
			// Service/features/geometry/coordinates
			jsonParser.parse( "{'Service': { 'features': { 'properties': 1 } } }" ) 
		);
		
		JsonObject mockInFFContent = new JsonObject();
		mockInFFContent.addProperty( "id" , failureDeviceId );
		mockInFFContent.addProperty( "date_time" , Instant.now().toString() );
		JsonObject prop = new JsonObject();
		prop.addProperty( "value" , 12.1 );
		mockInFFContent.add( "measure" , prop );
		
		String uuid = testRunner.enqueue( mockInFFContent.toString() ).getAttribute( "uuid" );
		
		testRunner.run();
		
		testRunner.assertAllFlowFilesTransferred( EnrichData.FAILURE_RELATIONSHIP );
		testRunner.assertTransferCount( EnrichData.FAILURE_RELATIONSHIP , 1 );
		testRunner.assertAllFlowFilesContainAttribute( EnrichData.FAILURE_RELATIONSHIP , "failure" );
		testRunner.assertAllFlowFilesContainAttribute( EnrichData.FAILURE_RELATIONSHIP , "failure.cause" );
		
		System.out.println( "-------------- FAILURE_RELATIONSHIP flow files: --------------\n");
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.FAILURE_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
		});
		
		System.out.println( "\n\n-------------- RETRY_RELATIONSHIP flow files: --------------\n");
		outFFList = testRunner.getFlowFilesForRelationship( EnrichData.RETRY_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
		});
		
	}
	
	@After
    public void tearDown() throws Exception {
    	srv.close();
    }
	
}
