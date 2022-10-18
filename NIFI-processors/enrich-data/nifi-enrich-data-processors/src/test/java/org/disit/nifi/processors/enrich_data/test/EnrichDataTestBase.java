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

package org.disit.nifi.processors.enrich_data.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.MockProcessContext;
import org.apache.nifi.util.MockValidationContext;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapControllerService;
import org.disit.nifi.processors.enrich_data.test.api_mock.ServicemapMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Base class to build tests for the EnrichData processor
 */
public class EnrichDataTestBase {
	
	// Core
	protected TestRunner testRunner;
	protected static JsonParser parser = new JsonParser();
	
	// ---- Servicemap mock Stuffs -----
	// Local servicemap path and uris
    protected static int mockServicemapPort = 8090;
    protected static String mockServicemapEndpoint = "/servicemap";
    protected static String servicemapUrl = "http://localhost:" 
    								 + mockServicemapPort 
    		                         + mockServicemapEndpoint;
    // Mock servicemap server
    protected static SimpleProtectedAPIServer srv;
    protected static ServicemapMockHandler servicemap;
    
    
    // ---- Controller services -----
    // Servicemap
    protected ServicemapControllerService servicemapService;
    // configs
    protected static String serviceUriPrefix = "http://serviceuriprefix.org";
    protected static String additionalQueryString = "realtime=false";
    
    
    // Controller services properties
    protected List<PropertyDescriptor> controllerServicesDescriptors = Arrays.asList(
    	EnrichData.ENRICHMENT_SOURCE_CLIENT_SERVICE ,
    	EnrichData.ENRICHMENT_RESOURCE_LOCATOR_SERVICE ,
    	EnrichData.OWNERSHIP_CLIENT_SERVICE
    );
    
    protected Map<String , String> ffAttributes;
    
    // ---- Setup Methods ----
    protected void setupEnrichDataProperties() {
    	testRunner.setProperty( EnrichData.DEVICE_ID_NAME , "id" );
        testRunner.setProperty( EnrichData.DEVICE_ID_NAME_MAPPING , "sensorId" );
        // testRunner.setProperty( EnrichData.DEVICE_ID_VALUE_PREFIX_SUBST , "{ \"TA-\" : \"TA__\"}" );
        testRunner.setProperty( EnrichData.TIMESTAMP_FIELD_NAME , "date_time" );
        testRunner.setProperty( EnrichData.TIMESTAMP_FROM_CONTENT_PROPERTY_NAME , "value_type" );
        testRunner.setProperty( EnrichData.TIMESTAMP_FROM_CONTENT_PROPERTY_VALUE , "timestamp" );
        testRunner.setProperty( EnrichData.VALUE_FIELD_NAME , "value" );
        testRunner.setProperty( EnrichData.ENRICHMENT_RESPONSE_BASE_PATH , "Service/features/properties/realtimeAttributes" );
        testRunner.setProperty( EnrichData.LATLON_PRIORITY , EnrichData.LATLON_PRIORITY_VALUES[0] );
        testRunner.setProperty( EnrichData.ENRICHMENT_LAT_LON_PATH , "Service/features/geometry/coordinates" );
        testRunner.setProperty( EnrichData.ENRICHMENT_LAT_LON_FORMAT , "[lon , lat]" );
        testRunner.setProperty( EnrichData.ENRICHMENT_BEHAVIOR , EnrichData.ENRICHMENT_BEHAVIOR_VALUES[0] );
        testRunner.setProperty( EnrichData.SRC_PROPERTY , "IOT" );
        testRunner.setProperty( EnrichData.KIND_PROPERTY , "sensor" );
        testRunner.setProperty( EnrichData.PURGE_FIELDS , "type,metadata,value_bounds,different_values,attr_type" );
        testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] );
        testRunner.setProperty( EnrichData.HASHED_ID_FIELDS , "serviceUri,value_name,date_time" );
//        testRunner.setProperty( EnrichData.NODE_CONFIG_FILE_PATH , "src/test/resources/enrich-data.conf" ); 
        
        testRunner.setProperty( "deviceName" , "Service/features/properties/name" );
        testRunner.setProperty( "organization" , "Service/features/properties/organization" );
    }
    
    protected void setupServicemapControllerService() throws InitializationException {
    	String csName = "ServicemapControllerService";
    	servicemapService = new ServicemapControllerService();
    	testRunner.addControllerService( csName , servicemapService );
    	testRunner.setProperty( servicemapService ,
    		ServicemapClientService.SERVICEMAP_URL , servicemapUrl );
    	testRunner.setProperty( servicemapService ,
			ServicemapClientService.SERVICE_URI_PREFIX , serviceUriPrefix );
    	testRunner.setProperty( servicemapService ,
			ServicemapClientService.ADDITIONAL_QUERY_STRING , additionalQueryString );
    	testRunner.assertValid( servicemapService );
    	testRunner.enableControllerService( servicemapService );
    	testRunner.setProperty( EnrichData.ENRICHMENT_SOURCE_CLIENT_SERVICE , csName );
    }
    
    protected void validateProcessorProperties() {
    	System.out.println( "Validating processor properties ... " );
    	ValidationContext validationContext = new MockValidationContext(  
    		new MockProcessContext( testRunner.getProcessor() )
    	);
    	for( PropertyDescriptor p : testRunner.getProcessor().getPropertyDescriptors() ) {
    		String v = testRunner.getProcessContext().getProperty( p ).getValue();
    		if( !controllerServicesDescriptors.contains( p ) ) { // Do not validate controller services
	    		ValidationResult r = p.validate( v , validationContext );
	    		System.out.println( "Valid: " + r.isValid() + " | " + r.getExplanation() );
	    		assertEquals( true , r.isValid() );
    		}
    	}
    	System.out.println( "VALID processor properties." );
    }
    
    protected ValidationResult getValidationResult( PropertyDescriptor p ) {
    	ValidationContext validationContext = new MockValidationContext(
    		new MockProcessContext( testRunner.getProcessor() )
    	);
    	return p.validate( testRunner.getProcessContext().getProperty(p).getValue() , 
    		               validationContext );
    }
    
    protected static void setupServicemapMock( boolean startServer ) throws Exception{
    	srv = new SimpleProtectedAPIServer( mockServicemapPort );
    	servicemap = new ServicemapMockHandler();
    	srv.addHandler( servicemap , mockServicemapEndpoint );
    	
    	if( startServer )
    		srv.start();
    }
    
    protected static String addServicemapResource( String deviceIdName , String serviceUriPrefix , String inputFilePath , String resourceFilePath ) throws IOException {
    	JsonObject inputObject = TestUtils.mockJsonObjFromFile( Paths.get( inputFilePath ) , parser );
    	String deviceId = inputObject.get( deviceIdName ).getAsString();
    	
    	StringBuilder serviceUri = new StringBuilder( serviceUriPrefix );
    	if( !serviceUriPrefix.endsWith("/") )
    		serviceUri.append("/");
    	serviceUri.append( deviceId );
    	
    	servicemap.addResourceFromFile( serviceUri.toString() , resourceFilePath );
    	return deviceId;
    }
    
	protected void configureFFAttributes( Map<String , String> attributes ) {
		this.ffAttributes = ImmutableMap.copyOf( attributes );
	}
    
    public void clearConfiguredAttributes() {
		if( ffAttributes != null )
			ffAttributes = null;
	}
	
	public MockFlowFile enqueueFlowFile( String path ) throws IOException {
		MockFlowFile inputFF;
		if( this.ffAttributes != null && !this.ffAttributes.isEmpty() )
			inputFF = testRunner.enqueue( Paths.get( path ) , this.ffAttributes );
		else
			inputFF = testRunner.enqueue( Paths.get( path ) );
		return inputFF;
	}
    
//    @BeforeClass
//    public static void initBaseMockServices() throws Exception {
//    	System.out.println( "Base initMockServices" );
//    	
//    	// Jetty Logging
//    	System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
//    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
//    	
//    	setupServicemapMock( false );
//    }
    
    public static void setupJettyLogging() {
    	// Jetty Logging
    	System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
    }
    
    @BeforeClass
    public static void startServices() throws Exception {
    	setupJettyLogging();
    	setupServicemapMock( false );
    	srv.start();
    }
    
    @AfterClass
    public static void tearDownBaseMockServices() throws Exception {
    	srv.close();
    }
    
    @Before
    public void init() throws Exception {
    	testRunner = TestRunners.newTestRunner( EnrichData.class );
    	
    	// Setup EnrichData
    	setupEnrichDataProperties();
    	setupServicemapControllerService();
//    	validateProcessorProperties();
    	
    	// Setup Servicemap mock server
//    	setupServicemapMock( false );
//    	srv.start();
    }
    
    protected String testName() {
    	return Thread.currentThread().getStackTrace()[2].getMethodName();
    }
    
    
    // For testing services mock purpose
    public static void main( String[] args ) throws Exception {
//    	initBaseMockServices();
    	String id = addServicemapResource( "id" , serviceUriPrefix , 
    		"src/test/resources/mock_in_ff/testOutputs.ff" , 
    		"src/test/resources/mock_servicemap_response/testOutputs.resp" 
    	);
    	System.out.println( "Added sensor: " + id );
    	srv.start();
    }
    
    
}
