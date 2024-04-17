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

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.EnrichDataProperties;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderService;
import org.disit.nifi.processors.enrich_data.oauth.keycloak.KeycloakTokenProviderControllerService;
import org.disit.nifi.processors.enrich_data.ownership.OwnershipControllerService;
import org.disit.nifi.processors.enrich_data.ownership.oauth.OwnershipOAuthControllerService;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.KeycloakMockHandler;
import org.disit.nifi.processors.enrich_data.test.ownership.EnrichDataOwnershipTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Unit tests for the EnrichData processor using:
 * 	Servicemap 		
 *  Ownership  		<- OAUTH
 */
public class EnrichDataOAuthOwnershipTests extends EnrichDataOwnershipTests{

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
		keycloakService = new KeycloakTokenProviderControllerService();
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
	protected void setupOwnershipControllerService() throws InitializationException {
		ownershipService = new OwnershipOAuthControllerService();
		String csName = "OwnershipOAuthControllerService";
		testRunner.addControllerService( csName , ownershipService );
		
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.OWNERSHIP_API_URL , ownershipUrl );
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.ELEMENT_ID_NAME , "elementId" );
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.ELEMENT_ID_PREFIX , "organization:broker:" );
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.OWNERSHIP_FIELDS , "username" );
		testRunner.setProperty( ownershipService , 
			OwnershipControllerService.FIELDS_MAPPING , "{\"username\":\"owner\"}" );
		testRunner.setProperty( ownershipService , 
			OwnershipOAuthControllerService.OAUTH_TOKEN_PROVIDER_SERVICE , "KeycloakTokenProviderControllerService" );
		testRunner.setProperty( ownershipService , 
			OwnershipOAuthControllerService.TOKEN_MODE , OwnershipOAuthControllerService.TOKEN_MODE_VALUES[1] );
		testRunner.assertValid( ownershipService );
		testRunner.enableControllerService( ownershipService );
		testRunner.setProperty( EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE , csName );	
	}
	
	protected static void setupOAuthOwnershipMock() {
		keycloak = new KeycloakMockHandler( realm , mockServicemapPort );
		keycloak.addClient( clientId , clientSecret );
		keycloak.addUser( clientId , username , password );
		srv.addHandler( keycloak , mockKeycloakEndpoint );
		
		ownership = new JsonResourceMockHandler( "elementId" , "Ownership Mock Handler" );
		srv.addProtectedHandler( ownership , 
			mockOwnershipEndpoint , 
			clientId, clientSecret, 
			keycloakUrl , realm );
	}
	
	@BeforeClass
	public static void startServices() throws Exception {
		setupJettyLogging();
		setupServicemapMock( false );
		setupOAuthOwnershipMock();
		srv.start();
	}
	
	@Override
	public void init() throws InitializationException {
		testRunner = TestRunners.newTestRunner( EnrichData.class );
		
		setupKeycloakControllerService();
		setupEnrichDataProperties();
		setupOwnershipControllerService();
		setupServicemapControllerService();
		
		validateProcessorProperties();	
	}
	
}
