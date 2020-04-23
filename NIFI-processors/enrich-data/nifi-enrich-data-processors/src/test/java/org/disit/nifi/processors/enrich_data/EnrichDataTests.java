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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapControllerService;
import org.disit.nifi.processors.enrich_data.test.api_mock.ServicemapMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class EnrichDataTests {

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
        
        testRunner.setProperty( "deviceName" , "Service/features/properties/name" );
        testRunner.setProperty( "organization" , "Service/features/properties/organization" );
        
        
        setupServicemapControllerService();
        
        srv = new SimpleProtectedAPIServer( mockServicemapPort );
        servicemap = new ServicemapMockHandler();
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
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICE_URI_PREFIX , EnrichDataTests.serviceUriPrefix );
		testRunner.setProperty( servicemapService , ServicemapClientService.ADDITIONAL_QUERY_STRING , EnrichDataTests.additionalQueryString );
		testRunner.assertValid( servicemapService );
		testRunner.enableControllerService( servicemapService );
		testRunner.setProperty( EnrichData.ENRICHMENT_SOURCE_CLIENT_SERVICE , "ServicemapControllerService" );
    }
    
    
    // Tests
    //-------------------------------------------------------------------------------------
    
    
    
    /**
     * Test for the Json Object output format
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    //@Test
    public void testJsonOutput() throws UnsupportedEncodingException, IOException {
    	System.out.println( "**** TEST JSON OUTPUT ***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] );
    	
    	mockFromFileTest( "src/test/resources/mock_in_ff/testOutputs.ff" , 
    					  "src/test/resources/mock_servicemap_response/testOutputs.resp" );
    	
    	testRunner.assertAllFlowFilesTransferred( EnrichData.SUCCESS_RELATIONSHIP );
    	testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
    	
    	JsonObject resultReferenceObj = 
    			TestUtils.mockJsonObjFromFile( Paths.get( "src/test/resources/reference_results/testOutputs_jsonOut.ref" ) , jsonParser );
    	List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
    	MockFlowFile outFF = outFFList.get( 0 );
    	
    	JsonObject outFFContent = jsonParser.parse( new String( outFF.toByteArray() ) )
    										.getAsJsonObject();	
    	String uuid = outFF.getAttribute( "uuid" );
    	
    	resultReferenceObj.entrySet().stream().forEach( (Map.Entry<String , JsonElement> member) -> {
    		resultReferenceObj.get( member.getKey() ).getAsJsonObject()
    						  .addProperty( "uuid" , uuid );
    	});
    	
    	String outFFStr = TestUtils.prettyOutFF( outFF , jsonParser );
    	Files.write( Paths.get( "src/test/resources/tests_output/testOutputs_jsonOut.result" ) , 
				 	 outFFStr.getBytes() );
    	System.out.println( outFFStr );
    	
    	assertEquals( outFFContent.toString() , resultReferenceObj.toString() );
    	
    	System.out.println( "**** END TEST JSON OUTPUT ***" );
    }
    
    /**
     * Test for the Elasticsearch Bulk Compliant output format 
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    // @Test
    public void testEsBulkCompliantOutput() throws UnsupportedEncodingException , IOException {
    	System.out.println( "**** TEST ES BULK COMPLIANT OUTPUT ***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[1] );
    	testRunner.setProperty( EnrichData.ES_INDEX , "ES_index_name" );
    	testRunner.setProperty( EnrichData.ES_TYPE , "ES_type_name" );
    	
    	mockFromFileTest( "src/test/resources/mock_in_ff/testOutputs.ff" , 
				  		  "src/test/resources/mock_servicemap_response/testOutputs.resp" );
    	
    	testRunner.assertAllFlowFilesTransferred( EnrichData.SUCCESS_RELATIONSHIP );
    	testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
    								  
    	List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
    	MockFlowFile outFF = outFFList.get( 0 );
    	
    	String uuid = outFF.getAttribute( "uuid" );
    	
    	String resultReference = Files.lines( Paths.get( "src/test/resources/reference_results/testOutputs_ESbulk.ref" ) )
									  .map( (String line) -> { 
										  JsonObject lineObj = jsonParser.parse( line ).getAsJsonObject();
										  if( lineObj.has( "uuid" ) )
											  lineObj.addProperty( "uuid" , uuid );
										  return lineObj.toString() + "\n";
									  })
									  .reduce( (s1 , s2) -> { return s1 + s2; } )
									  .get();
    	
//    	System.out.println( new String( outFF.toByteArray() ) );
//    	System.out.println( resultReference );
    	
    	String outFFStr = TestUtils.notPrettyOutFF( outFF );
    	Files.write( Paths.get( "src/test/resources/tests_output/testOutputs_esBulk.result" ) , 
 			    				outFFStr.getBytes() );
    	
    	outFF.assertContentEquals( resultReference );
    	
    	System.out.println( outFFStr );
    	
    	System.out.println( "**** END TEST ES BULK COMPLIANT OUTPUT ***" );
    }
    
    /**
     * Test for the Split Json Object output format
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    @Test
    public void testSplitJsonOutput() throws UnsupportedEncodingException , IOException {
    	System.out.println( "**** TEST SPLIT JSON OUTPUT ***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] );
    	
    	String uuid = mockFromFileTest( "src/test/resources/mock_in_ff/testOutputs.ff" , 
		  		  		  				"src/test/resources/mock_servicemap_response/testOutputs.resp" );
    	
//    	String uuid = mockFromFileTest( "src/test/resources/mock_in_ff/testSensor.ff" , 
//	  									"src/test/resources/mock_servicemap_response/testSensor.resp" );
    	
    	//testRunner.assertAllFlowFilesTransferred( EnrichData.SUCCESS_RELATIONSHIP );
    	//testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 7 );
    	
    	List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
    	
    	StringBuilder outputBuilder = new StringBuilder();
    	outFFList.stream().forEach( mockFF -> { 
    		outputBuilder.append( TestUtils.prettyOutFF( mockFF , jsonParser ) )
    						 .append("\n");
    		
//    		JsonObject content = jsonParser.parse( new String( mockFF.toByteArray() ) ).getAsJsonObject();
//    		String dateTime = content.get( "date_time" ).getAsString();
//    		String valueName = content.get( "value_name" ).getAsString();
//    		String serviceUri = content.get( "serviceUri" ).getAsString();
//    		
//    		String hexHash = DigestUtils.sha256Hex( valueName + serviceUri + dateTime );
//    		System.out.println( hexHash );
    	} );
    	
    	System.out.println( outputBuilder.toString() );
    	
    	Files.write( Paths.get( "src/test/resources/tests_output/testOutputs_splitJson.result" ) , 
 			    	 outputBuilder.toString().getBytes() );
    	
    	JsonArray resposeReferenceArr = 
    			TestUtils.mockJsonArrayFromFile( Paths.get( "src/test/resources/reference_results/testOutputs_splitJson.ref" ) , jsonParser );
    	for( int i = 0 ; i < resposeReferenceArr.size() ; i++ ) {
    		
    		JsonObject ff = resposeReferenceArr.get(i).getAsJsonObject();
    		ff.addProperty( "uuid" , uuid );
    		
    		outFFList.get(i).assertContentEquals( ff.toString() );
    	}
    	
//    	StringBuilder outputBuilder = new StringBuilder();
//    	outFFList.stream().forEach( mockFF -> { 
//    		outputBuilder.append( prettyOutFF( mockFF ) )
//    						 .append("\n");
//    	} );
//    	
//    	System.out.println( outputBuilder.toString() );
//    	
//    	Files.write( Paths.get( "src/test/resources/tests_output/01_splitJson.result" ) , 
// 			    	 outputBuilder.toString().getBytes() );
    	
    	System.out.println( "**** END TEST SPLIT JSON OUTPUT ***" );
    }
    
    /**
     * Test for an empty out object.
     * If the members of the incoming flow file content are not mapped
     * (or mapped incorrectly) on the servicemap, the resulting output object is empty.
     * In this case the processor should route the input ff to the failure relationship
     * without enrichment and setting the values of the members which does not have a 
     * value (in the input ff content) to null.
     * 
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    // @Test
    public void testEmptyOutObject() throws UnsupportedEncodingException , IOException {
    	System.out.println( "**** TEST EMPTY OUT OBJECT ***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] );
    	testRunner.setProperty( EnrichData.ENRICHMENT_LAT_LON_PATH , "Service/features/geometry/coordinates" );
    	testRunner.setProperty( EnrichData.ENRICHMENT_RESPONSE_BASE_PATH , "Service/features/properties/realtimeAttributes" );
    	
    	mockFromFileTest( "src/test/resources/mock_in_ff/testEmptyOutObj.ff" , 
		  		  		  "src/test/resources/mock_servicemap_response/testEmptyOutObj.resp" );
    	
    	
    	testRunner.assertAllFlowFilesTransferred( EnrichData.FAILURE_RELATIONSHIP );
    	testRunner.assertTransferCount( EnrichData.FAILURE_RELATIONSHIP , 1 );

    	String resultReference = 
    			TestUtils.mockJsonStringFromFile( Paths.get( "src/test/resources/reference_results/testEmptyOutObj.ref" ) , jsonParser  );
    	List<MockFlowFile> outFFListFailed = testRunner.getFlowFilesForRelationship( EnrichData.FAILURE_RELATIONSHIP );
    	String failedFFContent = jsonParser.parse( new String( outFFListFailed.get(0).toByteArray() ) )
    									   .getAsJsonObject().toString();
    	
    	outFFListFailed.get( 0 ).assertAttributeExists( "failure" );
    	assertEquals( failedFFContent , resultReference );
    	
    	StringBuilder outputBuilderFailure = new StringBuilder();
    	outFFListFailed.stream().forEach( mockFF -> { 
    		outputBuilderFailure.append( TestUtils.prettyOutFF( mockFF , jsonParser ) )
    						 	.append("\n");
    	});
    	System.out.println( "FAILURE_RELATIONSHIP flow files: " );
    	System.out.println( outputBuilderFailure.toString() );
    	
    	System.out.println( "**** TEST EMPTY OUT OBJECT ***" );
    }
    
    /**
     * Test for value containing an array.
     * 
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    // @Test
    public void testArrayValue() throws UnsupportedEncodingException , IOException {
    	System.out.println( "**** TEST ARRAY VALUE ***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , 
    						    EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] ); //Split JSON
    	
    	String uuid = mockFromFileTest( "src/test/resources/mock_in_ff/testArrayValue.ff" , 
		  		  		  				"src/test/resources/mock_servicemap_response/testArrayValue.resp" );
    	
    	testRunner.assertAllFlowFilesTransferred( EnrichData.SUCCESS_RELATIONSHIP );
    	testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 8 );
    	
    	List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
    	
    	StringBuilder outputBuilder = new StringBuilder();
    	outFFList.stream().forEach( mockFF -> { 
    		outputBuilder.append( TestUtils.prettyOutFF( mockFF , jsonParser ) )
    						 .append("\n");
    	} );
    	
    	System.out.println( outputBuilder.toString() );
    	
    	Files.write( Paths.get( "src/test/resources/tests_output/testOutputs_arrayValue.result" ) , 
 			    	 outputBuilder.toString().getBytes() );
    	
    	JsonArray resposeReferenceArr = 
    			TestUtils.mockJsonArrayFromFile( Paths.get( "src/test/resources/reference_results/testOutputs_arrayValue.ref" ) , jsonParser );
    	for( int i = 0 ; i < resposeReferenceArr.size() ; i++ ) {
    		
    		JsonObject ff = resposeReferenceArr.get(i).getAsJsonObject();
    		ff.addProperty( "uuid" , uuid );
    		
    		outFFList.get(i).assertContentEquals( ff.toString() );
    	}    	
    }
    	
    /**
     * Test for numeric string values parsing.
     * 
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    // @Test
    public void testStringValueParsing() throws UnsupportedEncodingException , IOException{
    	System.out.println( "**** TEST VALUES PARSING ***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , 
    							EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ); //JSON
    	testRunner.setProperty( EnrichData.ATTEMPT_STRING_VALUES_PARSING , "Yes" );
    	
    	mockFromFileTest( "src/test/resources/mock_in_ff/testParseStringValue.ff" , 
    					  "src/test/resources/mock_servicemap_response/testOutputs.resp" );
    	
    	testRunner.assertAllFlowFilesTransferred( EnrichData.SUCCESS_RELATIONSHIP );
    	testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
    	
    	JsonObject resultReferenceObj = TestUtils.mockJsonObjFromFile( 
    			Paths.get( "src/test/resources/reference_results/testOutputs_parseStringValue.ref" ) , jsonParser  );
    	List<MockFlowFile> outFFList = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
    	MockFlowFile outFF = outFFList.get( 0 );
    	
    	JsonObject outFFContent = jsonParser.parse( new String( outFF.toByteArray() ) )
    										.getAsJsonObject();	
    	String uuid = outFF.getAttribute( "uuid" );
    	
    	resultReferenceObj.entrySet().stream().forEach( (Map.Entry<String , JsonElement> member) -> {
    		resultReferenceObj.get( member.getKey() ).getAsJsonObject()
    						  .addProperty( "uuid" , uuid );
    	});
    	
    	String outFFStr = TestUtils.prettyOutFF( outFF , jsonParser );
    	Files.write( Paths.get( "src/test/resources/tests_output/testOutputs_parseStringValue.result" ) , 
				 	 outFFStr.getBytes() );
    	System.out.println( outFFStr );
    	
    	assertEquals( outFFContent.toString() , resultReferenceObj.toString() );
    	
    	System.out.println( "*** END TEST VALUE PARSING ***" );
    }
    
    
    
    // @Test
    public void testExceptions() {
    	System.out.println( "**** TEST EXCEPTIONS ***" );
    	
    	testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , 
    							EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ); //JSON
    	
    	try {
			mockFromFileTest( "src/test/resources/mock_in_ff/testExceptions.ff" , 
							  "src/test/resources/mock_servicemap_response/testExceptions.resp" );
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
    
    /**
     * Mock flow file content from a file
     * 
     * @param ffMockFile
     * @param responseMockFile
     * @return
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public String mockFromFileTest( String ffMockFile , String responseMockFile ) throws UnsupportedEncodingException , IOException{
    	 
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

    	StringBuilder serviceUriPrefix = new StringBuilder( EnrichDataTests.serviceUriPrefix );
    	if( !serviceUriPrefix.toString().endsWith( "/" )  )
    		serviceUriPrefix.append( "/" );
    	
    	StringBuilder addQueryString = new StringBuilder( EnrichDataTests.additionalQueryString );
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
    	
        // Run tests
        testRunner.run();
        
        return uuid;
    }
    
    //@Test
    public void testHash() {
    	String toHash = "I'm a string to be hashed by a beautiful algorithm";
    	String sha256Hex = DigestUtils.sha256Hex( toHash );
    	
    	System.out.println( sha256Hex );
    }
    
    
    
    @After
    public void tearDown() throws Exception {
    	srv.close();
    }
    
}

