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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.keycloak.ServicemapKeycloakClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.keycloak.ServicemapKeycloakControllerService;
import org.disit.nifi.processors.enrich_data.test.api_mock.ServicemapMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EnrichDataKeycloakTetsts {

	/**
	 * NOTE: Those tests needs a keycloak instance to run.
	 */
	
	private TestRunner testRunner;
	private JsonParser parser;
	
	private static final String serviceUriPrefix = "http://serviceuriprefix.org";
	private ServicemapKeycloakControllerService servicemapKeycloakControllerService;
	
	private int servicemapMockPort = 8090;
	private String servicemapMockEndpoint = "/servicemap";
	private String servicemapUrl = "http://localhost:" + servicemapMockPort + servicemapMockEndpoint;
    
    private String keycloakUrl = ">>> KEYCLOAK URL <<<";
    private String clientId = ">>> CLIENT ID <<<";
    private String clientSecret = ">>> CLIENT SECRET<<<";
    private String realm = ">>> REALM <<<";
    private String username = ">>> USERNAME <<<";
    private String password = ">>> PASSWORD <<<";
	
    private SimpleProtectedAPIServer srv;
    private ServicemapMockHandler servicemap;
    
	@Before
	public void init() throws Exception {
		// Jetty Logging
    	System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
		
		testRunner = TestRunners.newTestRunner(EnrichData.class);
		parser = new JsonParser();
		
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
        testRunner.setProperty( EnrichData.NODE_CONFIG_FILE_PATH , "src/test/resources/enrich-data.conf" );
        
        testRunner.setProperty( "deviceName" , "Service/features/properties/name" );
        testRunner.setProperty( "organization" , "Service/features/properties/organization" );
        
        setupServicemapKeycloakControllerService();
        
        srv = new SimpleProtectedAPIServer( servicemapMockPort );
        srv.setVerbose( true );
        servicemap = new ServicemapMockHandler();
        servicemap.setVerbose( false );
        srv.addProtectedHandler( servicemap , servicemapMockEndpoint , clientId, clientSecret, keycloakUrl , realm);
        
        srv.start();
	}
	
	/**
     * Setup a ServicemapKeycloakControllerService
     * @throws InitializationException
     */
    public void setupServicemapKeycloakControllerService() throws InitializationException {
    	servicemapKeycloakControllerService = new ServicemapKeycloakControllerService();
        testRunner.addControllerService( "ServicemapKeycloakControllerService" , servicemapKeycloakControllerService );
        testRunner.setProperty( servicemapKeycloakControllerService , ServicemapKeycloakClientService.SERVICEMAP_URL , this.servicemapUrl );
        testRunner.setProperty( servicemapKeycloakControllerService , ServicemapKeycloakClientService.SERVICE_URI_PREFIX , EnrichDataKeycloakTetsts.serviceUriPrefix );
        testRunner.setProperty( servicemapKeycloakControllerService , ServicemapKeycloakClientService.ADDITIONAL_QUERY_STRING , "realtime=false" );
        testRunner.setProperty( servicemapKeycloakControllerService , ServicemapKeycloakClientService.KEYCLOAK_URL ,this.keycloakUrl );
        testRunner.setProperty( servicemapKeycloakControllerService , ServicemapKeycloakClientService.CLIENT_ID , this.clientId );
        testRunner.setProperty( servicemapKeycloakControllerService , ServicemapKeycloakClientService.CLIENT_SECRET , this.clientSecret );
        testRunner.setProperty( servicemapKeycloakControllerService , ServicemapKeycloakClientService.REALM , this.realm );
        testRunner.setProperty( servicemapKeycloakControllerService , ServicemapKeycloakClientService.USERNAME , this.username );
        testRunner.setProperty( servicemapKeycloakControllerService , ServicemapKeycloakClientService.PASSWORD , this.password );
        testRunner.assertValid( servicemapKeycloakControllerService );
        testRunner.enableControllerService( servicemapKeycloakControllerService );
        testRunner.setProperty( EnrichData.ENRICHMENT_SOURCE_CLIENT_SERVICE , "ServicemapKeycloakControllerService" );
    }
    
//    @Test
    public void testJsonOutput() throws UnsupportedEncodingException, IOException {
    	System.out.println( "**** TEST JSON OUTPUT (Keycloak) ***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] );
    	
    	mockFromFileTest( "src/test/resources/mock_in_ff/testOutputs.ff" , 
    					  "src/test/resources/mock_servicemap_response/testOutputs.resp" );
    	
    	testRunner.assertAllFlowFilesTransferred( EnrichData.SUCCESS_RELATIONSHIP );
    	testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
    	
    	JsonObject resultReferenceObj = 
    			TestUtils.mockJsonObjFromFile( Paths.get( "src/test/resources/reference_results/testOutputs_jsonOut.ref" ) , parser );
    	List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
    	MockFlowFile outFF = outFFList.get( 0 );
    	
    	JsonObject outFFContent = parser.parse( new String( outFF.toByteArray() ) )
    									.getAsJsonObject();	
    	String uuid = outFF.getAttribute( "uuid" );
    	
    	resultReferenceObj.entrySet().stream().forEach( (Map.Entry<String , JsonElement> member) -> {
    		resultReferenceObj.get( member.getKey() ).getAsJsonObject()
    						  .addProperty( "uuid" , uuid );
    	});
    	
    	String outFFStr = TestUtils.prettyOutFF( outFF , parser );
    	Files.write( Paths.get( "src/test/resources/tests_output/testOutputs_jsonOut.result" ) , 
				 	 outFFStr.getBytes() );
    	System.out.println( outFFStr );
    	
    	assertEquals( outFFContent.toString() , resultReferenceObj.toString() );
    	
    	System.out.println( "**** END TEST JSON OUTPUT (Keycloak)***" );
    }
    
    // @Test
    public void testTokenRefresh() throws UnsupportedEncodingException, IOException, InterruptedException {
    	System.out.println( "**** TEST TOKEN REFRESH (Keycloak) ***" );
    	
    	testRunner.setThreadCount( 4 );
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] );
    	
    	JsonObject resultReferenceObj = 
    			TestUtils.mockJsonObjFromFile( Paths.get( "src/test/resources/reference_results/testOutputs_jsonOut.ref" ) , parser );
    	
    	int nFlowFilesPerTest = 20;
//    	long delay = 10000; // in ms
    	
    	for( int i = 0 ; i < 1 ; i++ ) {
    		System.out.println( " -- Cycle #" + (i+1) + " start. --" );
			mockFromFileTest( "src/test/resources/mock_in_ff/testOutputs.ff" , 
					  		  "src/test/resources/mock_servicemap_response/testOutputs.resp" , 
					  		  nFlowFilesPerTest );
		
			testRunner.assertAllFlowFilesTransferred( EnrichData.SUCCESS_RELATIONSHIP );
	    	testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , nFlowFilesPerTest );
	    	
	    	List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
	    	MockFlowFile outFF = outFFList.get( 0 );
	    	
	    	JsonObject outFFContent = parser.parse( new String( outFF.toByteArray() ) )
					.getAsJsonObject();	
	    	String uuid = outFF.getAttribute( "uuid" );
	
			resultReferenceObj.entrySet().stream().forEach( (Map.Entry<String , JsonElement> member) -> {
				resultReferenceObj.get( member.getKey() ).getAsJsonObject()
								  .addProperty( "uuid" , uuid );
			});
	    	
			assertEquals( outFFContent.toString() , resultReferenceObj.toString() );
			testRunner.clearTransferState(); // Clear the previous run
			
			System.out.println( " -- Cycle #" + (i+1) + " done. -- \n" );
    	}
    	
    	System.out.println( "**** END TEST REFRESH TOKEN (Keycloak)***" );
    }
    
    private String baseMockFromFileTest( String ffMockFile , String responseMockFile ) throws IOException {
    	String mockInFFContent = TestUtils.mockJsonStringFromFile( Paths.get( ffMockFile ) , parser );
    	
    	JsonObject inFFrootObj = parser.parse( mockInFFContent ).getAsJsonObject();
    	String deviceId = inFFrootObj.get( 
    		testRunner.getProcessContext()
    				  .getProperty( EnrichData.DEVICE_ID_NAME ).getValue() 
    	).getAsString();
    	
    	JsonElement mockServicemapResponse = TestUtils.mockJsonElementFromFile( Paths.get( responseMockFile ) , parser );
    	StringBuilder serviceUriPrefix = new StringBuilder( EnrichDataKeycloakTetsts.serviceUriPrefix );
    	if( !serviceUriPrefix.toString().endsWith( "/" )  )
    		serviceUriPrefix.append( "/" );
    	
    	String serviceUri = serviceUriPrefix.toString() + deviceId;
    	
//    	System.out.println( "*** mock response *** " );
//    	System.out.println( mockServicemapResponse.toString() );
    	
    	servicemap.addResource( serviceUri , mockServicemapResponse );
    	return mockInFFContent;
    }
    
    public String mockFromFileTest( String ffMockFile , String responseMockFile ) throws IOException {
    	String mockInFFContent = TestUtils.mockJsonStringFromFile( Paths.get( ffMockFile ) , parser );
//    	String mockServicemapResponse = TestUtils.mockJsonStringFromFile( Paths.get( responseMockFile ), parser);
    	
    	JsonObject inFFrootObj = parser.parse( mockInFFContent ).getAsJsonObject();
    	String deviceId = inFFrootObj.get( 
    		testRunner.getProcessContext()
    				  .getProperty( EnrichData.DEVICE_ID_NAME ).getValue() 
    	).getAsString();
    	
    	JsonElement mockServicemapResponse = TestUtils.mockJsonElementFromFile( Paths.get( responseMockFile ) , parser );

    	StringBuilder serviceUriPrefix = new StringBuilder( EnrichDataKeycloakTetsts.serviceUriPrefix );
    	if( !serviceUriPrefix.toString().endsWith( "/" )  )
    		serviceUriPrefix.append( "/" );
    	
    	String serviceUri = serviceUriPrefix.toString() + deviceId;
    	
    	System.out.println( "*** mock response *** " );
    	System.out.println( mockServicemapResponse.toString() );
    	
    	servicemap.addResource( serviceUri , mockServicemapResponse );
    	
    	String uuid = testRunner.enqueue( mockInFFContent ).getAttribute( "uuid" );
    	
    	testRunner.run();
    	
    	return uuid;
    }
    
    public void mockFromFileTest( String ffMockFile , String responseMockFile , int nFlowFiles ) throws IOException {
    	String ffContent = baseMockFromFileTest( ffMockFile , responseMockFile );
    	
    	for( int i = 0 ; i < nFlowFiles ; i++ ) {
    		testRunner.enqueue( ffContent );
    	}
    	
    	System.out.println( "Enqueued flow files: " + testRunner.getQueueSize().toString() );
    	
    	testRunner.run( nFlowFiles );
    }
    
    public void mockFromFileTest( String ffMockFile , String responseMockFile , int nFlowFiles , long delayMs ) throws IOException, InterruptedException {
    	String ffContent = baseMockFromFileTest( ffMockFile , responseMockFile );
    	
    	for( int i = 0 ; i < nFlowFiles ; i++ ) {
    		testRunner.enqueue( ffContent );
    	}
    	
    	System.out.println( "Enqueued flow files: " + testRunner.getQueueSize().toString() );
    	System.out.println( "Running with delay between flow files processing of " + delayMs + " ms" );

    	if( delayMs > 0 ) {
    		for( int i = 0 ; i < nFlowFiles ; i++ ) {
    			testRunner.run();
    			Thread.sleep( delayMs );
    		}
    	}else {
    		testRunner.run( nFlowFiles );
    	}
    }
    
    @After
    public void tearDown() throws Exception {
    	srv.close();
    }
    
}


