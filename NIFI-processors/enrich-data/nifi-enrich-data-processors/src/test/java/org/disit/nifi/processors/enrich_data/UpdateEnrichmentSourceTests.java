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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.MockProcessContext;
import org.apache.nifi.util.MockValidationContext;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapControllerService;
import org.disit.nifi.processors.enrich_data.test.TestUtils;
import org.disit.nifi.processors.enrich_data.test.api_mock.ServicemapMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.disit.nifi.processors.enrich_data.utility_processors.UpdateEnrichmentSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

// TODO: Refactor

public class UpdateEnrichmentSourceTests {

	private TestRunner testRunner;
	
	JsonParser jsonParser = new JsonParser();
	
	private static final String serviceUriPrefix = "http://serviceuriprefix.org";
	private static final String additionalQueryString = "";
	private ServicemapControllerService servicemapService;
	
	private int mockServicemapPort = 8090;
    private String mockServicemapEndpoint = "/servicemap";
    private String servicemapUrl = "http://localhost:" + mockServicemapPort + mockServicemapEndpoint;
    
    private SimpleProtectedAPIServer srv;
    private ServicemapMockHandler servicemap;
    
    private List<PropertyDescriptor> controllerServicesDescriptors = Arrays.asList( 
    	UpdateEnrichmentSource.ENRICHMENT_SOURCE_UPDATER_SERVICE
    );
	
 // Init method
    @Before
    public void init() throws Exception {
    	
    	// Jetty Logging
    	System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
    	
        testRunner = TestRunners.newTestRunner(UpdateEnrichmentSource.class);
        jsonParser = new JsonParser();
        
        testRunner.setProperty( UpdateEnrichmentSource.DEVICE_ID_NAME , "id" );
        testRunner.setProperty( UpdateEnrichmentSource.ENDPOINT , "move" );
        testRunner.setProperty( UpdateEnrichmentSource.CONDITION , "" );
        testRunner.setProperty( UpdateEnrichmentSource.REQ_RESOURCE_URI_NAME , "" );
//        testRunner.setProperty( "id" , "id" );
        
        setupServicemapControllerService();
        
        // Mock external services (Servicemap)
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
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICE_URI_PREFIX , serviceUriPrefix );
		testRunner.setProperty( servicemapService , ServicemapClientService.ADDITIONAL_QUERY_STRING , additionalQueryString );
		testRunner.assertValid( servicemapService );
		testRunner.enableControllerService( servicemapService );
		testRunner.setProperty( UpdateEnrichmentSource.ENRICHMENT_SOURCE_UPDATER_SERVICE , "ServicemapControllerService" );
    }
    
    
    private void validateProcessorProperties() {
    	System.out.println( "Validating processor properties ..." );
    	ValidationContext validationContext = new MockValidationContext(  
    		new MockProcessContext( testRunner.getProcessor() )
    	);
    	for( PropertyDescriptor p : testRunner.getProcessor().getPropertyDescriptors() ) {
    		String v = testRunner.getProcessContext().getProperty( p ).getValue();
    		if( !controllerServicesDescriptors.contains( p ) ) { // Do not validate controller services
	    		ValidationResult r = p.validate( v , validationContext );
	    		System.out.println( p.getName() + " : " + v + " | " + r.getExplanation() );
	    		assertEquals( true , r.isValid() );
    		}
    	}
    	System.out.println( "VALID processor properties." );
    }
	
    // ------------------------- Tests -------------------------
    
    @Test
    public void testFFContentCondition() throws IOException {
    	System.out.println( "**** TEST FF-CONTENT CONDITION *** " );
    	testRunner.setProperty( UpdateEnrichmentSource.CONDITION , "{\"a\":123}" );
//    	testRunner.setProperty( UpdateEnrichmentSource.CONDITION , "{\"id\":\"wind1\"}" );
        testRunner.setProperty( UpdateEnrichmentSource.REQ_RESOURCE_URI_NAME , "uri" );
        testRunner.setProperty( UpdateEnrichmentSource.TIMESTAMP_FIELD_NAME , "date_time" );
//        testRunner.setProperty( UpdateEnrichmentSource.STATIC_AUGMENT_PERFORMED_UPDATES , 
//        	"{\"value_name\":\"__location\",\"value_type\":\"location\"}" );
        testRunner.setProperty( "id" , "id" );
        testRunner.setProperty( "a" , "A" );
        testRunner.setProperty( "location/coordinates" , "[latitude , longitude]" );
//        testRunner.setProperty( "latitude/value" , "latitude" );
//        testRunner.setProperty( "longitude/value" , "longitude" );

        testRunner.addConnection( UpdateEnrichmentSource.PERFORMED_UPDATES_RELATIONSHIP );
        
        validateProcessorProperties();
    	
        Map<String, String> attributes = new HashMap<>();
        attributes.put( "date_time" , ZonedDateTime.now(ZoneOffset.UTC).format( DateTimeFormatter.ISO_INSTANT ) );
        attributes.put( "serviceUri" , "http://serviceuriprefix.org/test-id" );
//        attributes.put( "serviceUri" , "http://serviceuriprefix.org/wind1" );
        
        // Success mock flow file
    	testRunner.enqueue( 
    		TestUtils.mockJsonElementFromFile(
				Paths.get( "src/test/resources/test_update_enrichment_source/input.ff" ) ,
//				Paths.get( "src/test/resources/test_update_enrichment_source/input_alt.ff" ) ,
				jsonParser 
			).toString(),
    		attributes
		);
    	
    	// Condition not met mock flow file
    	testRunner.enqueue( 
    		TestUtils.mockJsonElementFromFile(
				Paths.get( "src/test/resources/test_update_enrichment_source/failContentConditionInput.ff" ) ,
				jsonParser 
			).toString(),
    		attributes
		);
    	
    	servicemap.addEndpoint( "/move" , (JsonElement reqBody) -> {
    		assertEquals( true , reqBody.isJsonObject() );
    		JsonObject obj = reqBody.getAsJsonObject();
    		assertEquals( true , obj.has("id") );
    		assertEquals( true , obj.get( "id" ).isJsonPrimitive() );
    		
    		assertEquals( true , obj.has("uri") );
    		assertEquals( serviceUriPrefix + "/" + obj.get("id").getAsString() , 
    				      obj.get("uri").getAsString() );
    		
    		System.out.println( "Servicemap received:" );
    		System.out.println( reqBody.toString() );
    	});
    	
    	testRunner.run( testRunner.getQueueSize().getObjectCount() );
    	testRunner.assertTransferCount( UpdateEnrichmentSource.SUCCESS_RELATIONSHIP , 1 );
    	testRunner.assertTransferCount( UpdateEnrichmentSource.CONDITION_NOT_MET_RELATIONSHIP , 1 );
    	
    	System.out.println( "--- Success FFs: " );
    	testRunner.getFlowFilesForRelationship( UpdateEnrichmentSource.SUCCESS_RELATIONSHIP )
				  .stream().forEach( (MockFlowFile ff) -> {
					  System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
				  });
    	System.out.println( "--- Condition not met FFs: " );
    	testRunner.getFlowFilesForRelationship( UpdateEnrichmentSource.CONDITION_NOT_MET_RELATIONSHIP )
				  .stream().forEach( (MockFlowFile ff) -> {
					  System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
				  });
//    	testRunner.assertTransferCount( UpdateEnrichmentSource.PERFORMED_UPDATES_RELATIONSHIP , 1 );
    	if( testRunner.getFlowFilesForRelationship( UpdateEnrichmentSource.PERFORMED_UPDATES_RELATIONSHIP ).size() > 0 ) {
	    	System.out.println( "--- Performed updates FFs: ");
	    	testRunner.getFlowFilesForRelationship( UpdateEnrichmentSource.PERFORMED_UPDATES_RELATIONSHIP )
	    			  .stream().forEach( (MockFlowFile ff)->{
	    				  System.out.println( TestUtils.prettyOutFF( ff ) );
	    			  });
    	}
    	System.out.println( "**** END TEST FF-CONTENT CONDITION ***\n\n" );
    }
    
    @Test
    public void testAttributesCondition() throws IOException {
    	System.out.println( "**** TEST FF-ATTRIBUTES CONDITION *** " );
    	testRunner.setProperty( UpdateEnrichmentSource.ATTRIBUTES_CONDITION , 
    							"{\"performUpdate\":true}");
        testRunner.setProperty( UpdateEnrichmentSource.REQ_RESOURCE_URI_NAME , "uri" );
        testRunner.setProperty( "id" , "id" );
        testRunner.setProperty( "a" , "A" );
        testRunner.setProperty( "location/coordinates" , "[latitude , longitude]" );

        validateProcessorProperties();
    	
        // Success flow file mock
        Map<String , String> ffAttributes = new TreeMap<>();
        ffAttributes.put( "performUpdate" , "true" );
    	JsonElement ffContent = TestUtils.mockJsonElementFromFile(
    		Paths.get( "src/test/resources/test_update_enrichment_source/input.ff" ) ,
    		jsonParser 
    	);
    	testRunner.enqueue( ffContent.toString() , ffAttributes );
    	
    	// Condition not met flow file mock
    	ffAttributes.clear();
    	ffAttributes.put( "performUpdate" , "false" );
    	testRunner.enqueue( ffContent.toString() , ffAttributes );
    	
    	// Endpoint mock
    	servicemap.addEndpoint( "/move" , (JsonElement reqBody) -> {
    		assertEquals( true , reqBody.isJsonObject() );
    		JsonObject obj = reqBody.getAsJsonObject();
    		assertEquals( true , obj.has("id") );
    		assertEquals( true , obj.get( "id" ).isJsonPrimitive() );
    		
    		assertEquals( true , obj.has("uri") );
    		assertEquals( serviceUriPrefix + "/" + obj.get("id").getAsString() , 
    				      obj.get("uri").getAsString() );
    		
    		System.out.println( "Servicemap received:" );
    		System.out.println( reqBody.toString() );
    	});
    	
    	testRunner.run( testRunner.getQueueSize().getObjectCount() );
    	
    	testRunner.assertTransferCount( UpdateEnrichmentSource.SUCCESS_RELATIONSHIP , 1 );
    	testRunner.assertTransferCount( UpdateEnrichmentSource.CONDITION_NOT_MET_RELATIONSHIP , 1 );
    	
    	System.out.println( "---- Success FFs: ");
    	testRunner.getFlowFilesForRelationship( UpdateEnrichmentSource.SUCCESS_RELATIONSHIP )
				  .stream().forEach( (MockFlowFile ff) -> {
					  System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
				  });
    	
    	System.out.println( "---- Condition not met FFs: ");
    	testRunner.getFlowFilesForRelationship( UpdateEnrichmentSource.CONDITION_NOT_MET_RELATIONSHIP )
    			  .stream().forEach( (MockFlowFile ff) -> {
    				  System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
    			  });
    	System.out.println( "**** END TEST FF-ATTRIBUTES CONDITION ***\n\n" );
    }
    
    @Test
    public void testHybridCondition() throws IOException {
    	System.out.println( "**** TEST HYBRID CONDITION *** " );
    	testRunner.setProperty( UpdateEnrichmentSource.CONDITION , 
    							"{\"a\":123}" );
    	testRunner.setProperty( UpdateEnrichmentSource.ATTRIBUTES_CONDITION , 
    							"{\"perform\":true}" );
        testRunner.setProperty( UpdateEnrichmentSource.REQ_RESOURCE_URI_NAME , "uri" );
        testRunner.setProperty( UpdateEnrichmentSource.TIMESTAMP_FIELD_NAME , "date_time" );
        testRunner.setProperty( "id" , "id" );
        testRunner.setProperty( "a" , "A" );
        testRunner.setProperty( "location/coordinates" , "[latitude , longitude]" );

        validateProcessorProperties();
    	
    	// Success mock flow file
    	testRunner.enqueue( 
    		TestUtils.mockJsonElementFromFile(
				Paths.get( "src/test/resources/test_update_enrichment_source/input.ff" ) ,
				jsonParser 
			).toString() , 
    		Stream.of( new String[][] { 
    			{ "perform" , "true" }
    		}).collect( Collectors.toMap( data -> data[0] , data -> data[1] ) ) 
		);
    	
    	// Condition not met on content
    	testRunner.enqueue( 
    		TestUtils.mockJsonElementFromFile(
				Paths.get( "src/test/resources/test_update_enrichment_source/failContentConditionInput.ff" ) ,
				jsonParser 
			).toString() , 
    		Stream.of( new String[][] { 
    			{ "perform" , "true" }
    		}).collect( Collectors.toMap( data -> data[0] , data -> data[1] ) )
		);
    	
    	// Condition not met on atttibutes
    	testRunner.enqueue( 
    		TestUtils.mockJsonElementFromFile(
				Paths.get( "src/test/resources/test_update_enrichment_source/input.ff" ) ,
				jsonParser 
			).toString() , 
    		Stream.of( new String[][] { 
    			{ "perform" , "false" }
    		}).collect( Collectors.toMap( data -> data[0] , data -> data[1] ) )
		);
    	
    	
    	servicemap.addEndpoint( "/move" , (JsonElement reqBody) -> {
    		assertEquals( true , reqBody.isJsonObject() );
    		JsonObject obj = reqBody.getAsJsonObject();
    		assertEquals( true , obj.has("id") );
    		assertEquals( true , obj.get( "id" ).isJsonPrimitive() );
    		
    		assertEquals( true , obj.has("uri") );
    		assertEquals( serviceUriPrefix + "/" + obj.get("id").getAsString() , 
    				      obj.get("uri").getAsString() );
    		
    		System.out.println( "Servicemap received:" );
    		System.out.println( reqBody.toString() );
    	});
    	
    	testRunner.run( testRunner.getQueueSize().getObjectCount() );
    	testRunner.assertTransferCount( UpdateEnrichmentSource.SUCCESS_RELATIONSHIP , 1 );
    	testRunner.assertTransferCount( UpdateEnrichmentSource.CONDITION_NOT_MET_RELATIONSHIP , 2 );
    	
    	System.out.println( "--- Success FFs: " );
    	testRunner.getFlowFilesForRelationship( UpdateEnrichmentSource.SUCCESS_RELATIONSHIP )
				  .stream().forEach( (MockFlowFile ff) -> {
					  System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
				  });
    	System.out.println( "--- Condition not met FFs: " );
    	testRunner.getFlowFilesForRelationship( UpdateEnrichmentSource.CONDITION_NOT_MET_RELATIONSHIP )
				  .stream().forEach( (MockFlowFile ff) -> {
					  System.out.println( TestUtils.prettyOutFF( ff , jsonParser ) );
				  });
    	System.out.println( "**** END TEST HYBRID CONDITION ***\n\n" );
    }
    
    @Test
    public void testServicemapError() throws IOException {
    	System.out.println( "**** TEST SERVICEMAP ERROR *** " );
    	testRunner.setProperty( UpdateEnrichmentSource.ENDPOINT , "move/error400" );
    	testRunner.setProperty( UpdateEnrichmentSource.CONDITION , "{\"id\":\"error400test\"}" );
        testRunner.setProperty( UpdateEnrichmentSource.REQ_RESOURCE_URI_NAME , "uri" );
        testRunner.setProperty( "id" , "id" );
        testRunner.setProperty( "location/coordinates" , "[latitude , longitude]" );
    	
        validateProcessorProperties();
        
		servicemap.addErrorEndpoint( 
			"/move/error400" ,
			(JsonElement reqBody) -> { } ,
			HttpServletResponse.SC_BAD_REQUEST
		);
		
		JsonElement ffContent = TestUtils.mockJsonElementFromFile(
    		Paths.get( "src/test/resources/test_update_enrichment_source/error400.ff" ) ,
    		jsonParser 
    	);
    	MockFlowFile inFF = testRunner.enqueue( ffContent.toString() );
    	
    	testRunner.run();
    	testRunner.assertAllFlowFilesTransferred( UpdateEnrichmentSource.FAILURE_RELATIONSHIP );
    	System.out.println( "**** END TEST SERVICEMAP ERROR ***\n\n" );
    }
    
    @After
    public void tearDown() throws Exception {
    	srv.close();
    }

}
