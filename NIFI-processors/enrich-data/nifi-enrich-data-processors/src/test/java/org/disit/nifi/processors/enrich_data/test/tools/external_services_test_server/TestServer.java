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

package org.disit.nifi.processors.enrich_data.test.tools.external_services_test_server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.disit.nifi.processors.enrich_data.test.api_mock.JsonResourceMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.KeycloakMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.ServicemapMockHandler;
import org.disit.nifi.processors.enrich_data.test.api_mock.SimpleProtectedAPIServer;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Simple server to mock external resources:
 */
public class TestServer {
	
	public static final JsonParser parser = new JsonParser();
	
	// Test server config file
	public static String workDir;
	public static String confFilePath = "conf/conf.yml";
	
	public static void main( String[] args ) throws Exception {
//		// Set Jetty Logging
    	System.setProperty( "org.eclipse.jetty.util.log.class" , "org.eclipse.jetty.util.log.StdErrLog" );
    	System.setProperty( "org.eclipse.jetty.LEVEL" , "INFO" );
		
		if( args.length < 1 ) {
			System.out.println( "ERR: No working directory specified." );
			System.exit( 1 );
		}
		
		workDir = args[0];
		System.out.println( "INFO: Using working dir: \"" + workDir + "\"" );
		StringBuilder confFile = new StringBuilder( workDir )
			.append("/").append( confFilePath );
		
		File f = new File( confFile.toString() );
		if( !f.isFile() ) {
			System.out.println( "ERR: Invalid configuration file: " + confFile.toString() );
			System.exit(1);
		}
		
		ServiceConfig conf = getServiceConfig( confFile.toString() );
		Gson prettyGson = new GsonBuilder()
			.setPrettyPrinting().create();
		System.out.println( prettyGson.toJson( conf.toJson() ) );
		
		System.out.println( "\n\n** QUICK ENDPOINT REFERENCE ** \n" );
		System.out.println( "To make requests to Servicemap use:\n\n\t" + conf.servicemap.endpoint + "?serviceUri=<SENSOR SERVICE URI>" );
		System.out.println( "\n\t-----\n" );
		System.out.println( "To make requests to the Ownership service use:\n\n\t" + conf.ownership.endpoint + "?" + conf.ownership.idParameterName + "=<SENSOR OWNERSHIP IDENTIFIER>" );
		System.out.println( "\n\t-----\n" );
		System.out.println( "To make requests to the IoTDirectory service use:\n\n\t" + conf.iotdirectory.endpoint + "?" + conf.iotdirectory.idParameterName + "=<SENSOR IOTDIR IDENTIFIER>" );
		System.out.println( "\n\t-----\n" );
		System.out.println( "To get an access token from keycloak use:\n\n\t" + conf.keycloak.endpoint + "/auth/realms/<REALM>/protocol/openid-connect/token" );
		System.out.println( "\n\t- HTTP method: POST");
		System.out.println( "\t- with 'Basic auth' specifying client credentials" );
		System.out.println( "\t- with body containing: " );
		System.out.println( "\t\t - username=<USERNAME>" );
		System.out.println( "\t\t - password=<USER PASSOWRD>" );
		System.out.println( "\t\t - grant_type=password" );
		System.out.println( "\t\t - scope=openid" );
		
		SimpleProtectedAPIServer srv = buildServer(conf);
		srv.start();
		
	}
	
	public static ServiceConfig getServiceConfig( String confFilePath ) throws FileNotFoundException {
		Yaml yaml = new Yaml();
		InputStream confStream = new FileInputStream( new File( confFilePath ) );
		return yaml.loadAs( confStream , ServiceConfig.class );
	}
	
	public static SimpleProtectedAPIServer buildServer( ServiceConfig conf ) throws IOException{
		
		SimpleProtectedAPIServer srv = new SimpleProtectedAPIServer( conf.server.port );
		
		assert (conf.keycloak.enabled || conf.servicemap.enabled || conf.iotdirectory.enabled) == true : 
			"All services are disabled in the configurations.";
		
		if( conf.servicemap.usesOAuth || conf.ownership.usesOAuth || conf.iotdirectory.usesOAuth )
			assert conf.keycloak.enabled : "Some service is configured to use OAuth but the Keycloak service is disabled";
		
		// Keycloak handler setup
		String localKeycloakUrl = null;
		if( conf.keycloak.enabled ) {
			KeycloakMockHandler keycloak = new KeycloakMockHandler(
				conf.keycloak.realm , conf.server.port
			);
			
			keycloak.addClient( conf.keycloak.client.id , conf.keycloak.client.secret );
			conf.keycloak.client.users.forEach( (String username , String password) -> {
				keycloak.addUser( conf.keycloak.client.id , username , password );
			});
				
			keycloak.setAccessTokenExpire( conf.keycloak.accessTokenExpire );
			keycloak.setRefreshTokenExpire( conf.keycloak.refreshTokenExpire );
			srv.addHandler( keycloak , conf.keycloak.endpoint );
			
			localKeycloakUrl = new StringBuilder( "http://localhost:" )
				.append( conf.server.port )
				.append( conf.keycloak.endpoint )
				.toString();
		}

		// Servicemap handler setup
		if( conf.servicemap.enabled ) {
			ServicemapMockHandler servicemap = new ServicemapMockHandler();
			
			conf.servicemap.resources.forEach( (String serviceUriPrefix , Map<String , String> sensors) -> {
				sensors.forEach( (String deviceId, String resourceFile) -> {
					try {
						addServicemapResource( servicemap , deviceId , serviceUriPrefix , resourceFile );
					} catch (IOException e) { e.printStackTrace(); }
				});
			} );
			
			if( conf.servicemap.usesOAuth && localKeycloakUrl != null ) {
				srv.addProtectedHandler( servicemap , 
					conf.servicemap.endpoint ,
					conf.keycloak.client.id ,
					conf.keycloak.client.secret ,
					localKeycloakUrl , 
					conf.keycloak.realm 
				);
			}else {
				srv.addHandler( servicemap , conf.servicemap.endpoint );
			}
		}
		
		// Ownership
		if( conf.ownership.enabled ) {
			JsonResourceMockHandler ownership = new JsonResourceMockHandler( 
				conf.ownership.idParameterName , "Ownership handler" );
			
			conf.ownership.resources.forEach( (String idPrefix , Map<String , String> sensors) -> {
				sensors.forEach( (String deviceId , String ownershipFile) -> {
					try {
						addOwnershipResource( ownership , deviceId, idPrefix, ownershipFile );
					} catch (IOException e) { e.printStackTrace(); }
				});
			});
			
			if( conf.ownership.usesOAuth && localKeycloakUrl != null ) {
				srv.addProtectedHandler( ownership , 
					conf.ownership.endpoint , 
					conf.keycloak.client.id , conf.keycloak.client.secret , 
					localKeycloakUrl , 
					conf.keycloak.realm );
			}else {
				srv.addHandler( ownership , conf.ownership.endpoint );
			}
		}
		
		// IOTDirectory
		if( conf.iotdirectory.enabled ) {
			JsonResourceMockHandler iotdirectory = new JsonResourceMockHandler( 
				conf.iotdirectory.idParameterName , "IOTDirectory handler" );
			
			conf.iotdirectory.subscriptions.forEach( (String subId, String subscriptionFile ) -> {
				try {
					addIOTDirectoryResource( iotdirectory , subId , subscriptionFile );
				} catch (IOException e) { e.printStackTrace(); }
			});
			
			if( conf.iotdirectory.usesOAuth && localKeycloakUrl != null ) {
				srv.addProtectedHandler( iotdirectory , 
					conf.iotdirectory.endpoint ,
					conf.keycloak.client.id , conf.keycloak.client.secret ,
					localKeycloakUrl , 
					conf.keycloak.realm );
			}else {
				srv.addHandler( iotdirectory , conf.iotdirectory.endpoint );
			}
		}
		
		return srv;
	}
	
	public static void addServicemapResource( ServicemapMockHandler servicemap , String deviceId , String serviceUriPrefix , String resourceFilePath ) throws IOException {		
		StringBuilder serviceUri = new StringBuilder( serviceUriPrefix );
		if( !serviceUriPrefix.endsWith("/") )
			serviceUri.append( "/" );
		serviceUri.append( deviceId );
		
		servicemap.addResourceFromFile( 
			serviceUri.toString(), 
			absFilePath( resourceFilePath ) 
		);
	}
	
	public static void addOwnershipResource( JsonResourceMockHandler handler , String deviceId , String idPrefix , String filePath ) throws IOException {
		String ownershipIdentifier = new StringBuilder( idPrefix )
			.append( deviceId ).toString();
		handler.addJsonResourceFromFile( 
			ownershipIdentifier , 
			absFilePath( filePath ) 
		);
	}
	
	public static void addIOTDirectoryResource( JsonResourceMockHandler handler , String subId , String filePath ) throws IOException {
		handler.addJsonResourceFromFile( 
			subId , 
			absFilePath( filePath ) 
		);
	}
	
	public static String absFilePath( String filePath ) {
		if( filePath.startsWith("/") )
			return filePath;
		
		if( workDir == null )
			return filePath;
		
		StringBuilder absPath = new StringBuilder( workDir )
			.append( "/" ).append( filePath );
		return absPath.toString();
	}
	
	/**
	 * Services configuration classes
	 */
	public static class ServiceConfig {
		public ServerConfig server = new ServerConfig();
		public ServicemapConfig servicemap = new ServicemapConfig();
		public OwnershipConfig ownership = null;
		public KeycloakConfig keycloak = null;
		public IOTDirectoryConfig iotdirectory = null;
		
		@Override
		public String toString() {
			return new StringBuilder("{ server: " )
				.append( server.toString() )
				.append( " , keycloak: " )
				.append( keycloak != null ? keycloak.toString() : "null" )
				.append( " , servicemap: " )
				.append( servicemap.toString() )
				.append( " , ownership: " ).append( ownership != null ? ownership : "null" )
				.append( " , iotdirectory: " )
				.append( iotdirectory != null ? iotdirectory.toString() : "null" )
				.append( " }" )
				.toString();
		}
		
		public JsonElement toJson() {
			JsonObject serviceConfigObj = new JsonObject();
			serviceConfigObj.add( "server" , server.toJson() );
			serviceConfigObj.add( "servicemap" , servicemap.toJson() );
			serviceConfigObj.add( "keycloak" , keycloak.toJson() );
			serviceConfigObj.add( "ownership" , ownership.toJson() );
			serviceConfigObj.add( "iotdirectory" , iotdirectory.toJson() );
			return serviceConfigObj;
		}
		
		/**
		 * Server configurations
		 */
		public static class ServerConfig {
			public int port = 10000;
			
			@Override
			public String toString() {
				return new StringBuilder( "{ port: " ) 
					.append( Integer.toString( port ) )
					.append( " }" )
					.toString();
			}
			
			public JsonElement toJson() {
				return parser.parse( this.toString() );
			}
		}
		
		/**
		 * Keycloak configurations
		 */
		public static class KeycloakConfig {
			public boolean enabled = false;
			public String endpoint = "";
			public String realm = "";
//			public Map<String , KeycloakClientConfig> clients = null;
			public KeycloakClientConfig client = null;
			public int accessTokenExpire = 1800;
			public int refreshTokenExpire = 1800;
			
			@Override
			public String toString() {
				return new StringBuilder( "{ enabled: " )
					.append( Boolean.toString( enabled ) )
					.append( " , endpoint : " ).append( endpoint )
					.append( " , realm : " ).append( realm )
					.append( " , accessTokenExpire : " ).append( accessTokenExpire )
					.append( " , refreshTokenExpire : " ).append( refreshTokenExpire )
					.append( " , clients : " )
					.append( client != null ? client.toString() : "null" )
					.append( " }" )
					.toString();
					
			}
			
			public JsonElement toJson() {
				JsonObject keycloakObj = new JsonObject();
				keycloakObj.addProperty( "enabled" , this.enabled );
				keycloakObj.addProperty( "endpoint" , this.endpoint );
				keycloakObj.addProperty( "realm" , this.realm );
				keycloakObj.addProperty( "accessTokenExpire" , this.accessTokenExpire );
				keycloakObj.addProperty( "refreshTokenExpire" , this.refreshTokenExpire );
				keycloakObj.add( "client" , client.toJson() );
				return keycloakObj;
			}
		
			// Single Keycloack Client configuration
			public static class KeycloakClientConfig {
				public String id = "";
				public String secret = "";
				public Map<String , String> users;
				
				@Override
				public String toString() {
					return new StringBuilder("{ id: ").append( id )
						.append( " , secret: ").append( secret )
						.append( " , users: " ).append( users.toString() )
						.toString();
				}
				
				public JsonElement toJson() {
					JsonObject clientConfigObj = new JsonObject();
					clientConfigObj.addProperty( "id" , this.id );
					clientConfigObj.addProperty( "secret" , this.secret );
					
					JsonObject usersObj = new JsonObject();
					users.forEach( (String username , String password) -> {
						usersObj.addProperty( username , password );
					});
					clientConfigObj.add( "users" , usersObj );
					return clientConfigObj;
				}
			}
			
		}
		
		/**
		 * Servicemap configurations
		 */
		public static class ServicemapConfig {
			public boolean enabled = false;
			public boolean usesOAuth = false;
			public String endpoint = "/servicemap";
			public Map<String , Map<String , String>> resources = null;
			
			@Override
			public String toString() {
				return new StringBuilder( "{ enabled : " )
					.append( Boolean.toString( enabled ) )
					.append( " , usesOAuth : " ).append( Boolean.toString( usesOAuth ) )
					.append( " , endpoint : " ).append( endpoint )
					.append( " , resources : " ).append( resources != null ? resources : "null" )
					.append( " }" ).toString();
			}
			
			public JsonElement toJson() {
				JsonObject servicemap = new JsonObject();
				servicemap.addProperty( "enabled" , this.enabled );
				servicemap.addProperty( "usesOAuth" , this.usesOAuth );
				servicemap.addProperty( "endpoint" , this.endpoint );
				
				JsonObject resourcesObj = new JsonObject();
				resources.forEach( (String uri, Map<String , String> sensorFiles) -> {
					JsonObject sensorsObj = new JsonObject();
					sensorFiles.forEach( (String sensorId , String filePath ) -> { 
						sensorsObj.addProperty( sensorId , filePath );
					});
					resourcesObj.add( uri , sensorsObj );
				});
				servicemap.add( "resources" , resourcesObj );
				return servicemap;
			}
		}
		
		/**
		 * Ownership configurations
		 */
		public static class OwnershipConfig{
			public boolean enabled = false;
			public boolean usesOAuth = false;
			public String endpoint = "/ownership";
			public String idParameterName = "";
			
			public Map<String , Map<String , String>> resources = null;
			
			@Override
			public String toString() {
				return new StringBuilder( "{ enabled: " )
					.append( Boolean.toString( enabled ) )
					.append( " , endpoint: " ).append( endpoint )
					.append( " , usesOAuth: " ).append( Boolean.toString( usesOAuth ) )
					.append( " , idParameterName: " ).append( idParameterName )
					.append( " , resources: " ).append( resources != null ? resources : "null" )
					.append( " }" ).toString();
			}
			
			public JsonElement toJson() {
				JsonObject ownership = new JsonObject();
				ownership.addProperty( "enabled" , this.endpoint );
				ownership.addProperty( "usesOAuth" , this.usesOAuth );
				ownership.addProperty( "endpoint" , this.endpoint );
				ownership.addProperty( "idParameterName" , this.idParameterName );
				
				JsonObject resourcesObj = new JsonObject();
				resources.forEach( (String prefix , Map<String , String> ownershipFiles) -> { 
					JsonObject ownershipObj = new JsonObject();
					ownershipFiles.forEach( (String sensorId , String ownershipFile ) -> {
						ownershipObj.addProperty( sensorId , ownershipFile );
					});
					resourcesObj.add( prefix , ownershipObj );
				});
				ownership.add( "resources" , resourcesObj );
				return ownership;
			}
		}
		
		/**
		 * IOTDirectory configurations
		 */
		public static class IOTDirectoryConfig {
			public boolean enabled = false;
			public boolean usesOAuth = false;
			public String endpoint = "/iotdirectory";
			public String idParameterName = "sub_ID";
			public Map<String , String> subscriptions = null;
			
			@Override
			public String toString() {
				return new StringBuilder( "{ enabled : " )
					.append( Boolean.toString( enabled ) )
					.append( " , usesOAuth: " ).append( Boolean.toString( usesOAuth ) )
					.append( " , endpoint: " ).append( endpoint )
					.append( " , idParameterName: " ).append( idParameterName )
					.append( " , subscriptions: " )
					.append( subscriptions != null ? subscriptions : "null" )
					.toString();
			}
			
			public JsonElement toJson() {
				JsonObject iotdirectory = new JsonObject();
				iotdirectory.addProperty( "enabled" , this.enabled );
				iotdirectory.addProperty( "usesOAuth" , this.usesOAuth );
				iotdirectory.addProperty( "endpoint" , this.endpoint );
				iotdirectory.addProperty( "idParameterName" , this.idParameterName );
				
				JsonObject subs = new JsonObject();
				subscriptions.forEach( (String subId , String prefix ) -> {
					subs.addProperty( subId , prefix );
				});
				iotdirectory.add( "subscriptions" , subs );
				return iotdirectory;
			}
		}
		
	}

}
