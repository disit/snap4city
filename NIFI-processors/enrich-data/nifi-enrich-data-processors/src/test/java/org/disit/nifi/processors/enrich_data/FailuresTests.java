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
        
        
        testRunner.setProperty( EnrichDataProperties.DEVICE_ID_NAME , "id" );
        testRunner.setProperty( EnrichDataProperties.DEVICE_ID_NAME_MAPPING , "sensorId" );
        // testRunner.setProperty( EnrichData.DEVICE_ID_VALUE_PREFIX_SUBST , "{ \"TA-\" : \"TA__\"}" );
        testRunner.setProperty( EnrichDataProperties.TIMESTAMP_FIELD_NAME , "date_time" );
        testRunner.setProperty( EnrichDataProperties.TIMESTAMP_FROM_CONTENT_PROPERTY_NAME , "value_type" );
        testRunner.setProperty( EnrichDataProperties.TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE , "timestamp" );
        testRunner.setProperty( EnrichDataProperties.VALUE_FIELD_NAME , "value" );
        testRunner.setProperty( EnrichDataProperties.ENRICHMENT_RESPONSE_BASE_PATH , "Service/features/properties/realtimeAttributes" );
        testRunner.setProperty( EnrichDataProperties.ENRICHMENT_LAT_LON_PATH , "Service/features/geometry/coordinates" );
        testRunner.setProperty( EnrichDataProperties.ENRICHMENT_LAT_LON_FORMAT , "[lon , lat]" );
        testRunner.setProperty( EnrichDataProperties.ENRICHMENT_BEHAVIOR , EnrichDataConstants.ENRICHMENT_BEHAVIOR_VALUES[0] );
        testRunner.setProperty( EnrichDataProperties.SRC_PROPERTY , "IOT" );
        testRunner.setProperty( EnrichDataProperties.KIND_PROPERTY , "sensor" );
        testRunner.setProperty( EnrichDataProperties.PURGE_FIELDS , "type,metadata,value_bounds,different_values,attr_type" );
        testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] );
        testRunner.setProperty( EnrichDataProperties.HASHED_ID_FIELDS , "serviceUri,value_name,date_time" );
        testRunner.setProperty( EnrichDataProperties.NODE_CONFIG_FILE_PATH , "src/test/resources/enrich-data.conf" );
        
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
		testRunner.setProperty( EnrichDataProperties.ENRICHMENT_SOURCE_CLIENT_SERVICE , "ServicemapControllerService" );
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
			JsonParser.parseString( "{'error':'Internal Server Error'}" )
		);
		
		JsonObject mockInFFContent = new JsonObject();
		mockInFFContent.addProperty( "id" , failureDeviceId );
		mockInFFContent.addProperty( "date_time" , Instant.now().toString() );
		
		String uuid = testRunner.enqueue( mockInFFContent.toString() ).getAttribute( "uuid" );
		
		testRunner.run();
		
		testRunner.assertAllFlowFilesTransferred( EnrichDataRelationships.RETRY_RELATIONSHIP );
		testRunner.assertTransferCount( EnrichDataRelationships.RETRY_RELATIONSHIP , 1 );
		testRunner.assertAllFlowFilesContainAttribute( EnrichDataRelationships.RETRY_RELATIONSHIP , "failure" );
		testRunner.assertAllFlowFilesContainAttribute( EnrichDataRelationships.RETRY_RELATIONSHIP , "failure.cause" );
		
		System.out.println( "-------------- FAILURE_RELATIONSHIP flow files: --------------\n");
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.FAILURE_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		});
		
		System.out.println( "\n\n-------------- RETRY_RELATIONSHIP flow files: --------------\n");
		outFFList = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.RETRY_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff ) );
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
		
		testRunner.assertAllFlowFilesTransferred( EnrichDataRelationships.RETRY_RELATIONSHIP );
		testRunner.assertTransferCount( EnrichDataRelationships.RETRY_RELATIONSHIP , 1 );
		testRunner.assertAllFlowFilesContainAttribute( EnrichDataRelationships.RETRY_RELATIONSHIP , "failure" );
		testRunner.assertAllFlowFilesContainAttribute( EnrichDataRelationships.RETRY_RELATIONSHIP , "failure.cause" );
		
		System.out.println( "-------------- FAILURE_RELATIONSHIP flow files: --------------\n");
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.FAILURE_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		});
		
		System.out.println( "\n\n-------------- RETRY_RELATIONSHIP flow files: --------------\n");
		outFFList = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.RETRY_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff ) );
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
			JsonParser.parseString( "{'error':'Not Found'}" )
		);
		
		JsonObject mockInFFContent = new JsonObject();
		mockInFFContent.addProperty( "id" , failureDeviceId );
		mockInFFContent.addProperty( "date_time" , Instant.now().toString() );
		
		String uuid = testRunner.enqueue( mockInFFContent.toString() ).getAttribute( "uuid" );
		
		testRunner.run();
		
		testRunner.assertAllFlowFilesTransferred( EnrichDataRelationships.FAILURE_RELATIONSHIP );
		testRunner.assertTransferCount( EnrichDataRelationships.FAILURE_RELATIONSHIP , 1 );
		testRunner.assertAllFlowFilesContainAttribute( EnrichDataRelationships.FAILURE_RELATIONSHIP , "failure" );
		testRunner.assertAllFlowFilesContainAttribute( EnrichDataRelationships.FAILURE_RELATIONSHIP , "failure.cause" );
		
		System.out.println( "-------------- FAILURE_RELATIONSHIP flow files: --------------\n");
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.FAILURE_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		});
		
		System.out.println( "\n\n-------------- RETRY_RELATIONSHIP flow files: --------------\n");
		outFFList = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.RETRY_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff ) );
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
			JsonParser.parseString( "{'Service': { 'features': { 'properties': 1 } } }" )
		);
		
		JsonObject mockInFFContent = new JsonObject();
		mockInFFContent.addProperty( "id" , failureDeviceId );
		mockInFFContent.addProperty( "date_time" , Instant.now().toString() );
		JsonObject prop = new JsonObject();
		prop.addProperty( "value" , 12.1 );
		mockInFFContent.add( "measure" , prop );
		
		String uuid = testRunner.enqueue( mockInFFContent.toString() ).getAttribute( "uuid" );
		
		testRunner.run();
		
		testRunner.assertAllFlowFilesTransferred( EnrichDataRelationships.FAILURE_RELATIONSHIP );
		testRunner.assertTransferCount( EnrichDataRelationships.FAILURE_RELATIONSHIP , 1 );
		testRunner.assertAllFlowFilesContainAttribute( EnrichDataRelationships.FAILURE_RELATIONSHIP , "failure" );
		testRunner.assertAllFlowFilesContainAttribute( EnrichDataRelationships.FAILURE_RELATIONSHIP , "failure.cause" );
		
		System.out.println( "-------------- FAILURE_RELATIONSHIP flow files: --------------\n");
		List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.FAILURE_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		});
		
		System.out.println( "\n\n-------------- RETRY_RELATIONSHIP flow files: --------------\n");
		outFFList = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.RETRY_RELATIONSHIP );
		outFFList.stream().forEach( (MockFlowFile ff) -> {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		});
		
	}
	
	@After
    public void tearDown() throws Exception {
    	srv.close();
    }
	
}
