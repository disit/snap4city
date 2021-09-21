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

package org.disit.nifi.processors.enrich_data.test.api_mock;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import com.github.scribejava.apis.KeycloakApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A simple Jetty-based server which supports protected APIs
 * throught OAuth2/Open-ID access tokens. 
 */
public class SimpleProtectedAPIServer {

	private Server server;
	private ContextHandlerCollection handlers;
	private JsonParser jsonParser;
	private boolean verbose;
	
	public SimpleProtectedAPIServer( int port ) {
		server = new Server();
		jsonParser = new JsonParser();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort( port );
		server.setConnectors( new Connector[] { connector } );
		
		handlers = new ContextHandlerCollection();
		server.setHandler(handlers);
		verbose = false;
	}
	
	public void setVerbose( boolean verbose ) {
		this.verbose = verbose;
	}
	
	public void addHandler( Handler h , String url ) {
		ContextHandler ch = new ContextHandler(url);
		ch.setContextPath( url );
		ch.setHandler( h );
		handlers.addHandler( ch );
	}
	
	public void addProtectedHandler( Handler h , String url , String clientId , String clientSecret , String baseUrl , String realm ) {
		ProtectionHandler p = new ProtectionHandler( h , clientId , clientSecret , baseUrl , realm );
		ContextHandler ch = new ContextHandler( url );
		ch.setContextPath( url );
		ch.setHandler( p );
		handlers.addHandler( ch );
	}
	
	public void start() throws Exception{
		server.start();
//		server.join();
	}
	
	public boolean isRunning() {
		return server.isRunning();
	}
	
	public void stop() throws Exception {
		server.stop();
	}
	
	public void close() throws Exception {
		server.stop();
		server.destroy();
	}
	
	public class ProtectionHandler extends AbstractHandler{

		private Handler protectedHandler;
		
		private String clientId, clientSecret, baseUrl, realm;
		private String userinfoUrl;
		final OAuth20Service service;
		
		public ProtectionHandler( Handler protectedHandler , String clientId , String clientSecret ,
								  String baseUrl , String realm ) {
			this.protectedHandler = protectedHandler;
			
			this.clientId = clientId;
			this.clientSecret = clientSecret;
			
			this.baseUrl = baseUrl.endsWith( "/" ) ? baseUrl.substring( 0 , baseUrl.length() - 1 ) : baseUrl;
			this.realm = realm;
			
			this.service = new ServiceBuilder( this.clientId )
					.apiSecret( this.clientSecret )
					.defaultScope( "openid" )
					.build( KeycloakApi.instance( this.baseUrl , this.realm ) );
			
			StringBuilder urlBuilder = new StringBuilder();
			urlBuilder.append( this.baseUrl )
					  .append( "/auth/realms/" )
					  .append( this.realm )
					  .append( "/protocol/openid-connect/userinfo" );
			this.userinfoUrl = urlBuilder.toString();
		}
		
		private Map<String , String> headersMap( HttpServletRequest request ) {
			Map<String , String> map = new TreeMap<>();
			
			Enumeration<String> names = request.getHeaderNames();
			while( names.hasMoreElements() ) {
				String n = names.nextElement();
				map.put( n , request.getHeader( n ) );
			}

			return map;
		}
		
		private Response verifyAccessToken( String accessToken ) throws InterruptedException, ExecutionException, IOException {
			
			OAuth2AccessToken token = new OAuth2AccessToken( accessToken );
			
			final OAuthRequest oaReq = new OAuthRequest( Verb.GET , this.userinfoUrl );
//			oaReq.addHeader( "Authorization" , "Bearer " + accessToken );
			service.signRequest( token , oaReq );
			
			if( verbose ) {
				System.out.println( "Verification request: " + oaReq.toString() );
				System.out.println( "Verification request headers: " + oaReq.getHeaders().toString() );
//				System.out.println( "Verification request OAuth parameters: " + oaRequest.getOauthParameters().toString() );
			}
			
			return service.execute( oaReq );
		}
		
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			
			Map<String,String> headers = this.headersMap( request );
			if( !headers.containsKey("Authorization") ) {
				response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
				baseRequest.setHandled(true);
				response.getWriter().println( "{\"error\":\"No 'Authorization' header provided.\"}" );
				return;
			}
			
			String authHeaderValue = headers.get( "Authorization" );
			if( !authHeaderValue.startsWith( "bearer " )  && !authHeaderValue.startsWith( "Bearer " ) ) {
				response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
				baseRequest.setHandled(true);
				response.getWriter().println( "{\"error\":\"Expecting 'Authorization' header value to start with 'bearer '.\"}" );
				return;
			}
			
			String accessTokenStr = authHeaderValue.substring( 7 , authHeaderValue.length() );
			
			JsonObject rObj;
			try( Response oaResponse = verifyAccessToken( accessTokenStr ) ) {
				rObj = new JsonObject();
				rObj.addProperty( "verified-access-token", accessTokenStr );
				rObj.addProperty( "keycloak-response-code" , oaResponse.getCode() );
				rObj.add( "keycloak-response-body", 
							SimpleProtectedAPIServer.this.jsonParser.parse( oaResponse.getBody() ) );
				
				if( this.protectedHandler == null ) {
					response.setContentType( "application/json" );
					response.setStatus( HttpServletResponse.SC_OK );
					baseRequest.setHandled(true);
					response.getWriter().println( rObj.toString() );
					return;
				} 
				
				if( oaResponse.getCode() == HttpServletResponse.SC_OK ) {
					protectedHandler.handle( target , baseRequest , request , response );
				} else {
					response.setContentType( "application/json" );
					response.setStatus( oaResponse.getCode() );
					baseRequest.setHandled(true);
					response.getWriter().println( rObj.toString() );
				}
				
				
			} catch (InterruptedException | ExecutionException e) {
				response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
				baseRequest.setHandled(true);
				response.getWriter().println( "{\"error\":\"Exception during token verification request.\"}" );
				return;
			}
		}
		
	}
	
}
