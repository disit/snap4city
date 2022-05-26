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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.nifi.reporting.InitializationException;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.locators.iotdirectory.IOTDirectoryLocatorControllerService;
import org.disit.nifi.processors.enrich_data.test.TestUtils;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.disit.nifi.processors.enrich_data.test.simple.EnrichDataSimpleTests;
import org.junit.BeforeClass;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Unit tests for the EnrichData processor using:
 * 	Servicemap
 *  iotdirectory 
 */
public class EnrichDataLocatorTests extends EnrichDataSimpleTests {
	
	// IOTDirectoryLocator configs
	protected static final String iotDirectoryEndpoint = "/iotdirectory";
	protected static final String iotDirectoryUrl = "http://localhost:" + 
									   mockServicemapPort +
									   iotDirectoryEndpoint;
	
	protected static String subIdAttrName = "subscriptionId";
	protected static String subIdReqName = "sub_ID";
	protected static String serviceUriPrefixRespPath = "content/serviceUriPrefix";
	protected static String iotDirectoryAdditionalQueryString = "action=get_specific_context_broker";
	
	// IOTDirectory server mock
	// Add the mapping sub_id -> file_path here
	protected static JsonResourceMockHandler iotDirectory;
	
	protected static Map<String , String> serviceUriPrefixes = new HashMap<>();
	
	// IOTDirectoryLocator controller service
	protected IOTDirectoryLocatorControllerService iotDirectoryService;
	
	protected void setupIOTDirectoryControllerService() throws InitializationException{
		String csName = "IOTDirectoryLocatorControllerService";
		iotDirectoryService = new IOTDirectoryLocatorControllerService();
		testRunner.addControllerService( csName , iotDirectoryService );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.IOTDIRECTORY_URL , iotDirectoryUrl );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.SUBSCRIPTION_ID_ATTRIBUTE_NAME , subIdAttrName );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.SUBSCRIPTION_ID_REQUEST_NAME , subIdReqName );
		testRunner.setProperty( iotDirectoryService , 
			IOTDirectoryLocatorControllerService.SERVICE_URI_PREFIX_RESPONSE_PATH , serviceUriPrefixRespPath );
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
	
	protected static void setupIOTDirectoryMock() throws IOException {
		iotDirectory = new JsonResourceMockHandler( subIdReqName , "IOTDirectory Mock Handler" );
		srv.addHandler( iotDirectory , iotDirectoryEndpoint );
	}
	
	protected static String addIOTDirectoryResource( String subId , String resourceFilePath , String serviceUriPrefixPath ) throws IOException {
		JsonObject resourceObj = TestUtils.mockJsonObjFromFile( Paths.get( resourceFilePath ), parser );
		
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
		setupServicemapMock( false );
		setupIOTDirectoryMock();
		srv.start();
	}
	
	@Override
	public void init() throws Exception {
		super.init();
		setupIOTDirectoryControllerService();
		validateProcessorProperties();
		addIOTDirectoryResource( 
			"subid_1" , 
			"src/test/resources/mock_iotdirectory_response/subid_1.json" ,
			serviceUriPrefixRespPath 
		);
		configureFFAttributes( ImmutableMap.of( "subscriptionId" , "subid_1" ) );
	}
	
}
