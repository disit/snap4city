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
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.ServicemapClientService;
import org.disit.nifi.processors.enrich_data.enrichment_source.servicemap.oauth.ServicemapOAuthControllerService;
import org.disit.nifi.processors.enrich_data.oauth.OAuthTokenProviderService;
import org.disit.nifi.processors.enrich_data.oauth.keycloak.KeycloakTokenProviderControllerService;
import org.disit.nifi.processors.enrich_data.test.api_mock.ServicemapMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.disit.nifi.processors.enrich_data.test.simple.EnrichDataSimpleTests;
import org.junit.BeforeClass;

public class EnrichDataOAuthTests extends EnrichDataSimpleTests {
	
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
	
	protected OAuthTokenProviderService keycloakService;
	
	public void setupServicemapOAuthControllerService() throws InitializationException {
		servicemapService = new ServicemapOAuthControllerService();
		String csName = "ServicemapOAuthControllerService";
		testRunner.addControllerService( csName , servicemapService );
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICE_URI_PREFIX , serviceUriPrefix );
		testRunner.setProperty( servicemapService , ServicemapClientService.ADDITIONAL_QUERY_STRING , additionalQueryString );
		testRunner.setProperty( servicemapService , ServicemapClientService.SERVICEMAP_URL , servicemapUrl );
		testRunner.setProperty( servicemapService , ServicemapOAuthControllerService.OAUTH_TOKEN_PROVIDER_SERVICE , "KeycloakTokenProviderControllerService" );
		testRunner.assertValid( servicemapService );
		testRunner.enableControllerService( servicemapService );
		testRunner.setProperty( EnrichData.ENRICHMENT_SOURCE_CLIENT_SERVICE , csName );
	}
	
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
	
	protected static void setupOAuthServicemapMock( boolean startServer ) throws Exception {
		srv = new SimpleProtectedAPIServer( mockServicemapPort );
		servicemap = new ServicemapMockHandler();
		srv.addProtectedHandler( servicemap , 
			mockServicemapEndpoint , 
			clientId, clientSecret, 
			keycloakUrl , realm );
	}
	
	@BeforeClass
	public static void startServices() throws Exception {
		setupJettyLogging();
		setupOAuthServicemapMock( false );
		srv.start();
	}
	
	// @Before
	@Override
	public void init() throws Exception {
		testRunner = TestRunners.newTestRunner( EnrichData.class );
		
		setupEnrichDataProperties();
		setupKeycloakControllerService();
		setupServicemapOAuthControllerService();
		
		validateProcessorProperties();
	}

}
