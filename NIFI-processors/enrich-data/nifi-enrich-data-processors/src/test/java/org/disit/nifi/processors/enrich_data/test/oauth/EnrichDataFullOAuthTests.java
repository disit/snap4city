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

package org.disit.nifi.processors.enrich_data.test.oauth;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.oauth.ServicemapOAuthControllerService;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocatorService;
import org.disit.nifi.processors.enrich_data.locators.iotdirectory.IOTDirectoryLocatorControllerService;
import org.disit.nifi.processors.enrich_data.locators.iotdirectory.oauth.IOTDirectoryOAuthLocatorControllerService;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderService;
import org.disit.nifi.processors.enrich_data.oauth.keycloak.KeycloakTokenProviderControllerService;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipControllerService;
import org.disit.nifi.processors.enrich_data.ownership.oauth.OwnershipOAuthControllerService;
import org.disit.nifi.processors.enrich_data.test.EnrichDataTestBase;
import org.disit.nifi.processors.enrich_data.test.TestUtils;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.ServicemapMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EnrichDataFullOAuthTests extends EnrichDataTestBase{
	
	// NOTE: configure for a valid running keycloak instance
	
//  private String keycloakUrl = ">>> KEYCLOAK URL <<<";
//  private String clientId = ">>> CLIENT ID <<<";
//  private String clientSecret = ">>> CLIENT SECRET<<<";
//  private String realm = ">>> REALM <<<";
//  private String username = ">>> USERNAME <<<";
//  private String password = ">>> PASSWORD <<<";
	protected final static String clientId = "nifi-node";
	protected final static String clientSecret = "127ee466-6255-4336-bf93-bb9d652a7011";
	protected final static String keycloakUrl = "http://192.168.1.50:8080";
	protected final static String realm="nifi";
	protected final static String username = "nifi-node-1";
	protected final static String password = "password";
	
	// IOTDirectory configs
	protected static final String iotDirectoryEndpoint = "/iotdirectory";
	protected static final String iotDirectoryUrl = "http://localhost:" + 
									   mockServicemapPort +
									   iotDirectoryEndpoint;
	
	protected static final String subIdAttrName = "subscriptionId";
	protected static final String subIdReqName = "sub_ID";
	protected static final String serviceUriPrefixRespPath = "content/serviceUriPrefix";
	protected static final String iotDirectoryAdditionalQueryString = "action=get_specific_context_broker";

	// Ownership configs
	protected static final String mockOwnershipEndpoint = "/ownership";
	protected static final String ownershipUrl = "http://localhost:" + mockServicemapPort + mockOwnershipEndpoint;
	
	// ControllerServices
	protected final static String keycloakCsName = "KeycloakTokenProviderControllerService";
	protected final static String servicemapCsName = "ServicemapOAuthControllerService";
	protected final static String ownershipCsName = "OwnershipOAuthControllerService";
	protected final static String iotDirectoryCsName = "IOTDirectoryOAuthLocatorControllerService";
	
	protected OAuthTokenProviderService keycloakService;
	protected EnrichmentResourceLocatorService iotDirectoryService;
	protected OwnershipControllerService ownershipService;
	
	// Services
	protected static JsonResourceMockHandler iotDirectory;
	protected static JsonResourceMockHandler ownership;
	
	public void setupServicemapOAuthControllerService() throws InitializationException {
		servicemapService = new ServicemapOAuthControllerService();
		testRunner.addControllerService( servicemapCsName , servicemapService );
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICE_URI_PREFIX , serviceUriPrefix );
		testRunner.setProperty( servicemapService , ServicemapClientService.ADDITIONAL_QUERY_STRING , additionalQueryString );
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICEMAP_URL , servicemapUrl );
		testRunner.setProperty( servicemapService , ServicemapOAuthControllerService.OAUTH_TOKEN_PROVIDER_SERVICE , "KeycloakTokenProviderControllerService" );
		testRunner.assertValid( servicemapService );
		testRunner.enableControllerService( servicemapService );
		testRunner.setProperty( EnrichData.ENRICHMENT_SOURCE_CLIENT_SERVICE , servicemapCsName );
	}
	
	protected void setupIOTDirectoryControllerService() throws InitializationException {
		iotDirectoryService = new IOTDirectoryOAuthLocatorControllerService();
		testRunner.addControllerService( iotDirectoryCsName , iotDirectoryService );
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
		
		testRunner.setProperty( iotDirectoryService , 
				IOTDirectoryOAuthLocatorControllerService.OAUTH_TOKEN_PROVIDER_SERVICE , keycloakCsName );
		
		testRunner.assertValid( iotDirectoryService );
		testRunner.enableControllerService( iotDirectoryService );
		testRunner.setProperty( EnrichData.ENRICHMENT_RESOURCE_LOCATOR_SERVICE , iotDirectoryCsName );
	}
	
	protected void setupOwnershipControllerService() throws InitializationException {
		ownershipService = new OwnershipOAuthControllerService();
		testRunner.addControllerService( ownershipCsName , ownershipService );
		
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.OWNERSHIP_API_URL , ownershipUrl );
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.ELEMENT_ID_NAME , "elementId" );
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.ELEMENT_ID_PREFIX , "broker:organization:" );
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.OWNERSHIP_FIELDS , "username" );
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.FIELDS_MAPPING , "{\"username\":\"owner\"}" );
		testRunner.setProperty( ownershipService , 
			OwnershipOAuthControllerService.OAUTH_TOKEN_PROVIDER_SERVICE , keycloakCsName );
		testRunner.setProperty( ownershipService , 
			OwnershipOAuthControllerService.TOKEN_MODE , OwnershipOAuthControllerService.TOKEN_MODE_VALUES[1] );
		testRunner.assertValid( ownershipService );
		testRunner.enableControllerService( ownershipService );
		testRunner.setProperty( EnrichData.OWNERSHIP_CLIENT_SERVICE , ownershipCsName );	
	}
	
	
	public void setupKeycloakControllerService() throws InitializationException {
		OAuthTokenProviderService keycloakService = new KeycloakTokenProviderControllerService();
		testRunner.addControllerService( keycloakCsName , keycloakService );
		testRunner.setProperty( keycloakService , 
			KeycloakTokenProviderControllerService.KEYCLOAK_URL , keycloakUrl );
		testRunner.setProperty( keycloakService , 
			KeycloakTokenProviderControllerService.CLIENT_ID , clientId );
		testRunner.setProperty( keycloakService , 
			KeycloakTokenProviderControllerService.CLIENT_SECRET , clientSecret );
		testRunner.setProperty( keycloakService , 
			KeycloakTokenProviderControllerService.REALM , realm );
		testRunner.setProperty( keycloakService , 
			KeycloakTokenProviderControllerService.USERNAME , username );
		testRunner.setProperty( keycloakService , 
			KeycloakTokenProviderControllerService.PASSWORD , password );
		testRunner.assertValid( keycloakService );
		testRunner.enableControllerService( keycloakService );
	}
	
	protected static String addOwnershipResource( String deviceIdName , String idPrefix , String inputFilePath , String resourceFilePath ) throws IOException {
		JsonObject inputObj = TestUtils.mockJsonObjFromFile( Paths.get( inputFilePath ) , parser );
		String deviceId = inputObj.get( deviceIdName ).getAsString();
		
		StringBuilder ownershipIdentifier = new StringBuilder( idPrefix ).append( deviceId );
		ownership.addJsonResourceFromFile( ownershipIdentifier.toString() , resourceFilePath );
		return deviceId;
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
	
	protected static void setupServicesMock() {
		srv = new SimpleProtectedAPIServer( mockServicemapPort );
		// Servicemap
		servicemap = new ServicemapMockHandler();
		srv.addProtectedHandler( servicemap , 
			mockServicemapEndpoint , 
			clientId, clientSecret, 
			keycloakUrl , realm );
		// Ownership
		ownership = new JsonResourceMockHandler( "elementId" , "Ownership Mock Handler");
		srv.addProtectedHandler( ownership , 
			mockOwnershipEndpoint , 
			clientId, clientSecret, 
			keycloakUrl , realm );
		// IOTDirectory
		iotDirectory = new JsonResourceMockHandler( subIdReqName , "IOTDirectory Mock Handler");
		srv.addProtectedHandler( iotDirectory , 
			iotDirectoryEndpoint , 
			clientId, clientSecret , 
			keycloakUrl , realm );
	}
	
	@BeforeClass
	public static void startServices() throws Exception {
		setupJettyLogging();
		setupServicesMock();
		srv.start();
	}
	
	@Override
	public void init() throws InitializationException {
		testRunner = TestRunners.newTestRunner( EnrichData.class );
		
		setupKeycloakControllerService();
		setupEnrichDataProperties();
		setupIOTDirectoryControllerService();
		setupOwnershipControllerService();
		setupServicemapOAuthControllerService();
		
		validateProcessorProperties();
	}
	
	@Test
	public void test() throws IOException {
		System.out.println( "######## " + testName() + " ########" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" ,
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		addOwnershipResource( "id" , "broker:organization:" , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_ownership_response/ownership_mock.resp" );
		addIOTDirectoryResource( "subid_1" , 
			"src/test/resources/mock_iotdirectory_response/subid_1.json" , 
			serviceUriPrefixRespPath );
		
		configureFFAttributes( ImmutableMap.of( "subscriptionId" , "subid_1" ) );
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testOutputs.ff" );
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
		
		JsonObject expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/ownership/testOutputs_ownershipJsonObject.ref" , 
			inputFF , parser ).getAsJsonObject();
		JsonElement content = parser.parse( new String ( 
			testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP )
					  .get(0).toByteArray() ) 
		);
		assertEquals( true , content.isJsonObject() );
		assertEquals( true , expectedResult.equals( content.getAsJsonObject() ) );
	}

}
