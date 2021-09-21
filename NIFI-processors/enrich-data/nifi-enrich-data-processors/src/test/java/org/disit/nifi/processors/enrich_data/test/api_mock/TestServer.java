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

package org.disit.nifi.processors.enrich_data.test.api_mock;

/**
 * 
 * Simple server to mock external resources:
 * 	- Keycloak IMPLEMENTED
 * 
 *  - TODO: Add servicemap
 *  - TODO: Add ownership
 *  - TODO: Add iotirectory
 *  
 *  - TODO: complete server with all services and 
 * 		 	add support for external file configurations/resources
 */
public class TestServer {
	
	// Server configs
	public static int serverPort = 8081;
	
	// Keycloak related stuffs
	public static String mockKeycloakEndpoint = "/keycloak";
	public static String realm = "nifi";
	public static String clientId = "nifi-node";
	public static String clientSecret = "nifi-node-secret";
	public static String username = "nifi-node-1";
	public static String password = "password";

	public static void main( String[] args ) throws Exception {
		// Set Jetty Logging
    	System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
		
    	// Instance server
		SimpleProtectedAPIServer srv = new SimpleProtectedAPIServer( serverPort );
		
		// Setup keycloak
		KeycloakMockHandler keycloak = new KeycloakMockHandler( realm , serverPort );
		keycloak.addClient( clientId , clientSecret );
		keycloak.addUser( clientId , username , password );
		srv.addHandler( keycloak , mockKeycloakEndpoint );
		
		// START
		srv.start();
	}
}
