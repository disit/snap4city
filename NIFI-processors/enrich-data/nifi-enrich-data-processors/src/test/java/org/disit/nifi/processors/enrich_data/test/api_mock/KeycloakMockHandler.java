package org.disit.nifi.processors.enrich_data.test.api_mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.github.scribejava.core.model.Verb;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tngtech.keycloakmock.api.KeycloakMock;


/**
 * Handler to mock Keycloak
 */
public class KeycloakMockHandler extends AbstractHandler{

	// Supported grant types
	public enum GrantType {
		PASSWORD ,
		REFRESH_TOKEN;
		
		@Override
		public String toString() {
			return this.name().toLowerCase().toString();
		}
	}
	public static List<String> GRANT_TYPES;
	static {
		GRANT_TYPES = Arrays.asList( GrantType.values() )
							.stream().map( GrantType::toString )
							.collect( Collectors.toList() );
	}
	
	// Server configs
	private String realm;
	private int serverPort;
	
	// <client-id , client-secret>
	private Map<String , String> clientIdentities; 
	// <client_id , [<username, password> , ... , <username, password>] >
	private Map<String , Map<String , String>> clientUsersDatabase;
	// <username , client_id>
	private Map<String , String> userClientsMap;
	
	// Expires
	private int accessTokenExpire = 1800;
	private int refreshTokenExpire = 1800;
	
	// Endpoints
	private String tokenEndpoint;
	private String userinfoEndpoint;
	private Map< String , List<Verb>> allowedMethodsOnEndpoints;
	
	// Mocking and utilities
	private KeycloakMock kcmock;
//	private JsonParser parser = new JsonParser();
	
	public KeycloakMockHandler( String realm , int serverPort ) {
		this.realm = realm;
		this.serverPort = serverPort;
		
		tokenEndpoint = "/auth/realms/" + realm + "/protocol/openid-connect/token";
		userinfoEndpoint = "/auth/realms/" + realm + "/protocol/openid-connect/userinfo";
		allowedMethodsOnEndpoints = ImmutableMap.of( 
			tokenEndpoint , Arrays.asList( Verb.POST ) ,
			userinfoEndpoint , Arrays.asList( Verb.GET )
		);
		
		this.clientIdentities = new HashMap<>();
		this.clientUsersDatabase = new HashMap<>();
		this.userClientsMap = new HashMap<>();
		
		kcmock = new KeycloakMock( 
			com.tngtech.keycloakmock.api.ServerConfig.aServerConfig()
				.withDefaultRealm( realm )
				.build()
		);
	}
	
	// Token request: (POST) http://localhost:8090/keycloak/auth/realms/nifi/protocol/openid-connect/token
	
	// Refresh token: (POST) http://localhost:8090/keycloak/auth/realms/nifi/protocol/openid-connect/token
	//		client_id: <client-name>
	//		grant_type: refresh_token
	// 		refresh_token: <token to be refreshed>
	
	// Verification: (GET) http://192.168.1.50:8080/auth/realms/nifi/protocol/openid-connect/userinfo
	
	
	@Override
	public void handle(String target, Request base, HttpServletRequest request, HttpServletResponse response) 
		throws IOException, ServletException {
		
		System.out.println( "------ Keycloak mock handler ------" );
		System.out.println( "Target:" + target);
		System.out.println( "Request: " + request.toString() );
		System.out.println( "Parameters: \n\t" + request.getParameterMap().toString().replace(",","\n\t") );
		
		
		if( !allowedMethodsOnEndpoints.keySet().contains( target ) ) {
			setResponse( base , response , HttpServletResponse.SC_NOT_FOUND , 
				String.format( "Endpoint '%s' not found." , target , MediaType.PLAIN_TEXT_UTF_8 ) );
			return;
		}
		
		Verb method = Verb.valueOf( request.getMethod() );
		if( !allowedMethodsOnEndpoints.get( target ).contains( method ) ) {
			setResponse( base , response , HttpServletResponse.SC_BAD_REQUEST , 
				String.format( "Method %s not allowed for endpoint '%s'." , method.toString() , target ) , 
				MediaType.PLAIN_TEXT_UTF_8 );
			return;
		}
		System.out.println( "Authorization: " + ( request.getHeader( "Authorization" ) != null ? request.getHeader( "Authorization" ) : "<NONE>" ) );
		
		if( target.equals( tokenEndpoint ) ) {
			handleTokenEndpoint(target, base, request, response);
			return;
		}
		
		if( target.equals( userinfoEndpoint ) ) {
			handleVerificationEndpoint(target, base, request, response);
			return;
		}
			
		// Target is not a supported endpoint
		setResponse( base , response , HttpServletResponse.SC_BAD_REQUEST , 
			String.format( "Target '%s' is not a supported endpoint." , target ) );
	}
	
	private void handleTokenEndpoint( String target , Request base , HttpServletRequest request , HttpServletResponse response ) throws IOException {
		// target == tokenEndpoint
		String grantType = request.getParameter( "grant_type" );
		if( grantType == null ) {
			setResponse( base , response , HttpServletResponse.SC_BAD_REQUEST , "'grant_type' not specified in the request body." );
			return;
		}
		
		// grant_types: password , refresh_token 
		if( !GRANT_TYPES.contains( grantType ) ) {
			setResponse( base , response , HttpServletResponse.SC_BAD_REQUEST , String.format( "Grant type '%s' not supported." , grantType ) );
			return;
		}
		
		try {
			String username = null;
			if( grantType.equals( GrantType.PASSWORD.toString() ) ) { // PASSWORD Grant
				// (POST parameters): username , password , scope , grant_type
				
				// Basic auth client_id:client_secret
				Credentials basicAuthCredentials;
				Credentials bodyCredentials;
				basicAuthCredentials = getBasicAuthCredentials( request );
				checkClientIdentity( basicAuthCredentials );
				bodyCredentials = getRequestBodyCredentials( request );
				checkUserIdentity( bodyCredentials );
				
				username = bodyCredentials.username;
			} else if( grantType.equals( GrantType.REFRESH_TOKEN.toString() ) ) { // REFRESH_TOKEN Grant
				// (POST parameters): client_id , refresh_token , grant_type
				Credentials basicAuthCredentials = getBasicAuthCredentials( request );
				checkClientIdentity( basicAuthCredentials );
				
				
				String refreshToken = request.getParameter( "refresh_token" );
				if( refreshToken == null )
					throw new ServletException( "Miissing 'refresh_token' prarameter from request body." );
				JsonObject tokenPayload = decodeAccessTokenPayload( refreshToken );
				username = checkAccessTokenPayload( tokenPayload );
			}
			
			if( username != null )
				setResponse( base , response , HttpServletResponse.SC_OK , 
					supplyAccessToken( username ).toString() , 
					MediaType.JSON_UTF_8 
				);
			else
				setResponse( base , response , HttpServletResponse.SC_NOT_FOUND , "Unsupported 'grant_type' specified." );
		} catch (ServletException e) {
			setResponse( base , response , HttpServletResponse.SC_UNAUTHORIZED , e.getMessage() );
			return;
		}
	}

	private void handleVerificationEndpoint( String target , Request base , HttpServletRequest request , HttpServletResponse response ) throws IOException{
		// GET request
		try {
			String accessToken = getBearerTokenFromHeaders( request );
			JsonObject payload = decodeAccessTokenPayload( accessToken );
			String username = checkAccessTokenPayload( payload );
			
			JsonObject responseObj = new JsonObject();
			responseObj.addProperty( "preferred_username" , username );
			setResponse( base , response , HttpServletResponse.SC_OK , responseObj.toString() , MediaType.JSON_UTF_8 );
			return;
		} catch (ServletException e) {
			setResponse( base , response , HttpServletResponse.SC_BAD_REQUEST , e.getMessage() );
			return;
		}
	}
	
	public JsonObject decodeAccessTokenPayload( String acccessToken ) throws ServletException{
		String[] parts = acccessToken.split("\\.");
		String payload = new String( Base64.getDecoder().decode( parts[1] ) );
		JsonElement payloadObj = JsonParser.parseString( payload );
		if( !payloadObj.isJsonObject() )
			throw new ServletException( "The access token payload is not a JsonObject." );
		return payloadObj.getAsJsonObject();
	}
	
	public String checkAccessTokenPayload( JsonObject tokenPayload ) throws ServletException{		
		if( !tokenPayload.has("exp") )
			throw new ServletException( "Token payload missing 'exp' field." );
		long exp = tokenPayload.get("exp").getAsLong();
		if( !Instant.now().isAfter( Instant.ofEpochMilli(exp) ) )
			throw new ServletException( "Expired access token." );
		
		String identity = null;
		if( tokenPayload.has( "azp" ) )
			identity = tokenPayload.get("azp").getAsString();
		if( tokenPayload.has( "preferred_username" ) )
			identity = tokenPayload.get("preferred_username").getAsString();
		
		if( identity == null )
			throw new ServletException( "Cannot determine an identity from the token payload, 'azp' and 'preferred_username' are both absent." );
		
		return identity;
	}
	
	private JsonObject supplyAccessToken( String username ) {
		
		String token = kcmock.getAccessToken(
			com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig()
				.withRealm( realm )
				.withRealmRole( "offline_access" )
				.withRealmRole( "uma_authorization" )
				.withHostname( "localhost:" + serverPort )
				.withExpiration( Instant.now().plus( (long)accessTokenExpire , ChronoUnit.SECONDS ) )
				.withScope( "email" )
				.withScope( "profile" )
				.withAudience( "account" )
				.withAudience( "enrichment-client" )
				.withClaim( "preferred_username" , username )
				.withClaim( "azp" , userClientsMap.get( username ) )
				.build()
		);
		
		String refreshToken = kcmock.getAccessToken(
			com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig()
				.withRealm( realm )
				.withRealmRole( "offline_access" )
				.withRealmRole( "uma_authorization" )
				.withHostname( "localhost:" + serverPort )
				.withAudience( "http://localhost:" + serverPort + "/auth/realms/" + realm )
				.withExpiration( Instant.now().plus( (long)refreshTokenExpire , ChronoUnit.SECONDS ) )
				.withClaim( "typ" , "Refresh" )
				.withScope( "email" )
				.withScope( "profile" )
				.withClaim( "azp" , userClientsMap.get(username) )
				.build()
		);
		
		String idToken = kcmock.getAccessToken(
			com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig()
				.withRealm( realm )
				.withHostname( "localhost:" + serverPort )
				.withExpiration( Instant.now().plus( (long)accessTokenExpire , ChronoUnit.SECONDS ) )
				.withClaim( "typ" , "ID" )
				.withAudiences( clientIdentities.keySet() )
				.withClaim( "azp" , userClientsMap.get( username ) )
				.build()
		);
		
		JsonObject responseObj = new JsonObject();
		responseObj.addProperty( "access_token" , token );
		responseObj.addProperty( "expires_in" , accessTokenExpire );
		responseObj.addProperty( "refresh_expires_in" , refreshTokenExpire );
		responseObj.addProperty( "refresh_token" , refreshToken );
		responseObj.addProperty( "id_token" , idToken );
		responseObj.addProperty( "token_type" , "bearer" );
		responseObj.addProperty( "not-before-policy" , 0 );
		return responseObj;
	}
	
	private Credentials getBasicAuthCredentials( HttpServletRequest request ) throws ServletException {
		String authorization = request.getHeader( "Authorization" );
		if( authorization == null ) 
			throw new ServletException( "Authorization header not provided." );
		
		if( !authorization.startsWith("Basic ") && !authorization.startsWith( "basic ") )
			throw new ServletException( "Only Basic authentication is supported." );
		
		String credentialsString = authorization.substring(6);
		String[] credentials = new String( Base64.getDecoder().decode( credentialsString ) , StandardCharsets.UTF_8 )
			.split( ":" );
		return new Credentials( credentials[0] , credentials[1] );
	}
	
	private Credentials getRequestBodyCredentials( HttpServletRequest request ) throws ServletException{
		String username = request.getParameter( "username" );
		String password = request.getParameter( "password" );
		if( username == null )
			throw new ServletException( "The request body does not contain the 'username' attribute." );
		if( password == null )
			throw new ServletException( "The request body does not contain the 'password' attribute." );
		return new Credentials( username , password );
	}
	
	private String getBearerTokenFromHeaders( HttpServletRequest request ) throws ServletException{
		String authorization = request.getHeader( "Authorization" );
		if( authorization == null )
			throw new ServletException( "Authorization header not provided." );
		if( !authorization.startsWith( "Bearer ") && !authorization.startsWith( "bearer " ) ) {
			throw new ServletException( "Only bearer tokens authorization is supported for this endpoint." );
		}
		
		String token = authorization.substring(7);
		return token;
	}
	
	private void setResponse( Request base , HttpServletResponse response , int statusCode , String responseContent ) throws IOException {
		response.setStatus( statusCode );
		response.getWriter().println( responseContent );
		base.setHandled( true );
	}
	
	private void setResponse( Request base , HttpServletResponse response , int statusCode , String responseContent , MediaType contentType ) throws IOException {
		response.setContentType( contentType.toString() );
		setResponse( base , response , statusCode , responseContent );
	}
	
	public void checkClientIdentity( Credentials clientCredentials ) throws ServletException{
		if( !clientIdentities.containsKey( clientCredentials.username ) ) {
			throw new ServletException( String.format( "Client id '%s' not found." , clientCredentials.username ) );
		}
		
		if( !clientIdentities.get( clientCredentials.username ).equals( clientCredentials.password ) ) {
			throw new ServletException( String.format( "Wrong password for client id '%s'" , clientCredentials.password ) );
		}
	}
	
	public void checkUserIdentity( Credentials userCredentials ) throws ServletException{
		if( !userClientsMap.containsKey( userCredentials.username ) )
			throw new ServletException( String.format( "Username '%s' not found." , userCredentials.username ) );
		String clientId = userClientsMap.get( userCredentials.username );
		Map<String , String> clientUsers = clientUsersDatabase.get( clientId );
		if( !clientUsers.get( userCredentials.username ).equals( userCredentials.password ) )
			throw new ServletException( String.format( "Wrong password for user '%s'." , userCredentials.username ) );
	}
	
	public void addClient( String clientId , String clientSecret ) {
		this.clientIdentities.put( clientId , clientSecret );
		this.clientUsersDatabase.put( clientId , new HashMap<String , String>() );
	}
	
	public boolean addUser( String clientId , String username , String password ) {
		if( !clientUsersDatabase.keySet().contains( clientId ) )
			return false;
		Map<String , String> users = clientUsersDatabase.get( clientId );
		users.put( username , password );
		userClientsMap.put( username , clientId );
		return true;
	}
	
	public void setAccessTokenExpire( int accessTokenExpireSecs ) {
		this.accessTokenExpire = accessTokenExpireSecs;
	}
	
	public void setRefreshTokenExpire( int refreshTokenExpireSecs ) {
		this.refreshTokenExpire = refreshTokenExpireSecs;
	}
	
	private class Credentials {
		public String username;
		public String password;
		
		public Credentials( String username , String password ) {
			this.username = username;
			this.password = password;
		}
		
		@Override
		public String toString() {
			return username + ":" + password;
		}
	}

}
