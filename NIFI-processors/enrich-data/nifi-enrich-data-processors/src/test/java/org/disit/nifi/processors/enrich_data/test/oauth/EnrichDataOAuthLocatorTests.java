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

import java.io.IOException;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.EnrichDataProperties;
import org.disit.nifi.processors.enrich_data.locators.iotdirectory.IOTDirectoryLocatorControllerService;
import org.disit.nifi.processors.enrich_data.locators.iotdirectory.oauth.IOTDirectoryOAuthLocatorControllerService;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderService;
import org.disit.nifi.processors.enrich_data.oauth.keycloak.KeycloakTokenProviderControllerService;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.KeycloakMockHandler;
import org.disit.nifi.processors.enrich_data.test.locator.EnrichDataLocatorTests;
import org.junit.BeforeClass;

import com.google.common.collect.ImmutableMap;

/**
 * Unit tests for the EnrichData processor using:
 * 	Servicemap 		
 *  iotdirectory 	<- OAUTH
 */
public class EnrichDataOAuthLocatorTests extends EnrichDataLocatorTests{
	
	// Keycloak configs
	protected final static String clientId = "nifi-node";
	protected final static String clientSecret = "nifi-node-secret";
	protected final static String mockKeycloakEndpoint = "/keycloak";
	protected final static String keycloakUrl = "http://localhost:" + 
												mockServicemapPort + 
												mockKeycloakEndpoint;
	protected final static String realm="nifi";
	protected final static String username = "nifi-node-1";
	protected final static String password = "password";
	
	// Services
	protected OAuthTokenProviderService keycloakService;
	protected static KeycloakMockHandler keycloak;
	
	public void setupKeycloakControllerService() throws InitializationException {
		String csName = "KeycloakTokenProviderControllerService";
		OAuthTokenProviderService keycloakService = new KeycloakTokenProviderControllerService();
		testRunner.addControllerService( csName , keycloakService );
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
	
	@Override
	protected void setupIOTDirectoryControllerService() throws InitializationException {
		String csName = "IOTDirectoryOAuthLocatorControllerService";
		iotDirectoryService = new IOTDirectoryOAuthLocatorControllerService();
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
		
		testRunner.setProperty( iotDirectoryService , 
				IOTDirectoryOAuthLocatorControllerService.OAUTH_TOKEN_PROVIDER_SERVICE , "KeycloakTokenProviderControllerService" );
		
		testRunner.assertValid( iotDirectoryService );
		testRunner.enableControllerService( iotDirectoryService );
		testRunner.setProperty( EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE , csName );
	}
	
	protected static void setupOAuthIOTDirectoryMock() throws IOException{
		keycloak = new KeycloakMockHandler( realm , mockServicemapPort );
		keycloak.addClient( clientId , clientSecret );
		keycloak.addUser( clientId , username , password );
		srv.addHandler( keycloak , mockKeycloakEndpoint );
		
		iotDirectory = new JsonResourceMockHandler( subIdReqName , "IOTDirectory Mock Handler" );
		srv.addProtectedHandler( iotDirectory , iotDirectoryEndpoint , 
			clientId , clientSecret , 
			keycloakUrl , realm );
	}
	
	@BeforeClass
	public static void startServices() throws Exception {
		setupJettyLogging();
		setupServicemapMock( false );
		setupOAuthIOTDirectoryMock();
		srv.start();
	}
	
	@Override
	public void init() throws InitializationException, IOException {
		testRunner = TestRunners.newTestRunner( EnrichData.class );
		
		setupKeycloakControllerService();
		setupIOTDirectoryControllerService();
		setupEnrichDataProperties();
		setupServicemapControllerService();
		
		validateProcessorProperties();
		
		addIOTDirectoryResource( 
			"subid_1" , 
			"src/test/resources/mock_iotdirectory_response/subid_1.json" ,
			serviceUriPrefixRespPath 
		);
		configureFFAttributes( ImmutableMap.of( "subscriptionId" , "subid_1" ) );
	}
}
