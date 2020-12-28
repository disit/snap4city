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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapControllerService;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipControllerService;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.ServicemapMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EnrichDataOwnershipTests {

	private TestRunner testRunner;
    JsonParser jsonParser;
    
    private ServicemapControllerService servicemapService;
    private OwnershipControllerService ownershipService;
    
    private static final String serviceUriPrefix = "http://serviceuriprefix.org";
    private static final String additionalQueryString = "realtime=false";
    
    private int mockServerPort = 8090;
    private String mockServicemapEndpoint = "/servicemap";
    private String servicemapUrl = "http://localhost:" + mockServerPort + mockServicemapEndpoint;
    
    private String mockOwnershipEndpoint = "/ownership";
    private String ownershipUrl = "http://localhost:" + mockServerPort + mockOwnershipEndpoint; 
    
    private SimpleProtectedAPIServer srv;
    private ServicemapMockHandler servicemap;
    private JsonResourceMockHandler ownership;
    
 // Init method
    @Before
    public void init() throws Exception {
    	
    	// Jetty Logging
    	System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
    	
        testRunner = TestRunners.newTestRunner(EnrichData.class);
        jsonParser = new JsonParser();
        
//        testRunner.setProperty( EnrichData.SERVICE_URI_PREFIX , "http://serviceuriprefix.org" );
//        testRunner.setProperty( EnrichData.ADDITIONAL_QUERY_STRING , "realtime=false" );
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
        testRunner.setThreadCount( 4 );
        
        testRunner.setProperty( "deviceName" , "Service/features/properties/name" );
        testRunner.setProperty( "organization" , "Service/features/properties/organization" );
        
        
        setupServicemapControllerService();
        
        setupOwnershipControllerService();
        
        srv = new SimpleProtectedAPIServer( mockServerPort );
        servicemap = new ServicemapMockHandler();
        srv.addHandler( servicemap , mockServicemapEndpoint );
        
        ownership = new JsonResourceMockHandler( "elementId" );
        srv.addHandler( ownership , mockOwnershipEndpoint );
        
        srv.start();
    }
    
    public void setupServicemapControllerService() throws InitializationException{
		servicemapService = new ServicemapControllerService();
		testRunner.addControllerService( "ServicemapControllerService" , servicemapService );
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICEMAP_URL , this.servicemapUrl );
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICE_URI_PREFIX , EnrichDataOwnershipTests.serviceUriPrefix );
		testRunner.setProperty( servicemapService , ServicemapClientService.ADDITIONAL_QUERY_STRING , EnrichDataOwnershipTests.additionalQueryString );
		testRunner.assertValid( servicemapService );
		testRunner.enableControllerService( servicemapService );
		testRunner.setProperty( EnrichData.ENRICHMENT_SOURCE_CLIENT_SERVICE , "ServicemapControllerService" );
    }
    
    public void setupOwnershipControllerService() throws InitializationException{
    	ownershipService = new OwnershipControllerService();
    	testRunner.addControllerService( "OwnershipControllerService" , ownershipService );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.OWNERSHIP_API_URL , this.ownershipUrl );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.ELEMENT_ID_NAME , "elementId" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.ELEMENT_ID_PREFIX , "broker:organization:" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.OWNERSHIP_FIELDS , "username" );
    	testRunner.setProperty( ownershipService , OwnershipControllerService.FIELDS_MAPPING , "{\"username\":\"owner\"}" );
    	testRunner.setProperty( ownershipService , "elementType" , "IOTID" );
    	testRunner.assertValid( ownershipService );
    	testRunner.enableControllerService( ownershipService );
    	testRunner.setProperty( EnrichData.OWNERSHIP_CLIENT_SERVICE , "OwnershipControllerService" );
    }
    
    @Test
    public void testSplitJsonOutput() throws UnsupportedEncodingException, IOException {
    	System.out.println( "**** TEST SPLIT JSON OUTPUT (Ownership)***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] );
    	
    	String uuid = mockFromFileTest( "src/test/resources/mock_in_ff/testOutputs.ff" , 
	  								    "src/test/resources/mock_servicemap_response/testOutputs.resp" ,
	  								  	"src/test/resources/mock_ownership_response/ownership_mock.resp" );
    	
    	System.out.println( "Ownership RESOURCES: " + ownership.getResources() );
    	
//    	testRunner.assertAllFlowFilesTransferred( EnrichData.SUCCESS_RELATIONSHIP );
    	testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 7 );
    	testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
    	
    	List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
    	
    	StringBuilder outputBuilder = new StringBuilder();
    	outFFList.stream().forEach( mockFF -> { 
    		outputBuilder.append( TestUtils.prettyOutFF( mockFF , jsonParser ) )
    						 .append("\n");
    	} );
    	
    	JsonArray resposeReferenceArr = 
    			TestUtils.mockJsonArrayFromFile( Paths.get( "src/test/resources/reference_results/ownership/testOutputs_ownershipSplitJson.ref" ) , jsonParser );
    	for( int i = 0 ; i < resposeReferenceArr.size() ; i++ ) {
    		
    		JsonObject ff = resposeReferenceArr.get(i).getAsJsonObject();
    		ff.addProperty( "uuid" , uuid );
    		
    		outFFList.get(i).assertContentEquals( ff.toString() );
    	}
    	
    	System.out.println( outputBuilder.toString() );
    	
    	System.out.println( "**** END TEST SPLIT JSON OUTPUT (Ownership)***" );
    }
    
    @Test
    public void testOwnershipFieldsMapping() throws UnsupportedEncodingException, IOException {
    	System.out.println( "**** TEST OWNERSHIP FIELDS MAPPING (Ownership)***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] );
    	
    	String uuid = mockFromFileTest( "src/test/resources/mock_in_ff/testOutputs.ff" , 
	  								    "src/test/resources/mock_servicemap_response/testOutputs.resp" ,
	  								  	"src/test/resources/mock_ownership_response/ownership_mock.resp" );
    	
    	System.out.println( "Ownership RESOURCES: " + ownership.getResources() );
    	
    	testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 7 );
    	testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
    	
    	List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
    	
    	StringBuilder outputBuilder = new StringBuilder();
    	outFFList.stream().forEach( mockFF -> { 
    		outputBuilder.append( TestUtils.prettyOutFF( mockFF , jsonParser ) )
    						 .append("\n");
    	} );
    	
    	JsonArray resposeReferenceArr = 
    			TestUtils.mockJsonArrayFromFile( Paths.get( "src/test/resources/reference_results/ownership/testOutputs_ownershipFieldsMapping.ref" ) , jsonParser );
    	for( int i = 0 ; i < resposeReferenceArr.size() ; i++ ) {
    		
    		JsonObject ff = resposeReferenceArr.get(i).getAsJsonObject();
    		ff.addProperty( "uuid" , uuid );
    		
//    		outFFList.get(i).assertContentEquals( ff.toString() );
    		assertEquals( ff , jsonParser.parse( new String( outFFList.get(i).toByteArray() ) )
  				  					     .getAsJsonObject() );
    	}
    	
    	System.out.println( outputBuilder.toString() );
    	
    	System.out.println( "**** END TEST SPLIT JSON OUTPUT (Ownership)***" );
    }
    
    
    public String mockFromFileTest( String ffMockFile , String responseMockFile , String ownershipMockFile ) throws UnsupportedEncodingException , IOException{
   	 
    	String mockInFFContent = TestUtils.mockJsonStringFromFile( Paths.get( ffMockFile ) , jsonParser );
    	
//    	JsonParser parser = new JsonParser();
    	JsonObject inFFrootObj = jsonParser.parse( mockInFFContent ).getAsJsonObject();
    	String deviceId = inFFrootObj.get( 
    		testRunner.getProcessContext()
    				  .getProperty( EnrichData.DEVICE_ID_NAME ).getValue() 
    	).getAsString();
    	
//    	String mockServicemapResponse = TestUtils.mockJsonStringFromFile( Paths.get( responseMockFile ) , jsonParser );
    	JsonElement mockServicemapResponse = TestUtils.mockJsonElementFromFile( Paths.get( responseMockFile ) , jsonParser ); 
    	
//    	String deviceId = "123456789";

    	StringBuilder serviceUriPrefix = new StringBuilder( EnrichDataOwnershipTests.serviceUriPrefix );
    	if( !serviceUriPrefix.toString().endsWith( "/" )  )
    		serviceUriPrefix.append( "/" );
    	
    	StringBuilder addQueryString = new StringBuilder( EnrichDataOwnershipTests.additionalQueryString );
    	if( !addQueryString.toString().startsWith("&") )
    		addQueryString.insert( 0 , "&" );
    	
    	String queryString = "serviceUri=" 
    						 + URLEncoder.encode( serviceUriPrefix.toString() + deviceId , 
    								 		      StandardCharsets.UTF_8.toString() )
			 		         + addQueryString.toString();
    	
    	String serviceUri = serviceUriPrefix.toString() + deviceId;
    	String encodedServiceUri = URLEncoder.encode( serviceUri , StandardCharsets.UTF_8.toString() );
    	
    	System.out.println( "*** mock response *** " );
    	System.out.println( mockServicemapResponse.toString() );
    	
    	servicemap.addResource( serviceUri , mockServicemapResponse );	
    	
    	// Enqueue flow files
    	String uuid = testRunner.enqueue( mockInFFContent ).getAttribute("uuid");
    	
    	ownership.addJsonResourceFromFile( "broker:organization:" + deviceId , ownershipMockFile );
    	
        // Run tests
        testRunner.run();
        
        return uuid;
    }
    
	
    @After
    public void tearDown() throws Exception{
    	srv.close();
    }
}
