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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.Promise;

import com.google.common.net.MediaType;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Handler to mock Servicemap.
 */
//public class ServicemapMockHandler extends AbstractHandler{
public class ServicemapMockHandler extends Handler.Abstract{

	
	private Map<String, JsonElement> resources;
	private Map<String , ServicemapError> failures;
	
	// Jetty 11
//	private Map<String , AbstractHandler> endpoints;
//	private Map<String , AbstractHandler> errorEndpoints;
	
	// Jetty 12
	private Map<String , Handler> endpoints;
	private Map<String , Handler> errorEndpoints;

	private boolean verbose;
	
	private String servicemapEndpoint;
	
	public ServicemapMockHandler( String servicemapEndpoint ) {
		resources = new HashMap<>();
		failures = new HashMap<>();
		endpoints = new HashMap<>();
		errorEndpoints = new HashMap<>();
		verbose = false;
		this.servicemapEndpoint = servicemapEndpoint;
		if( !this.servicemapEndpoint.endsWith("/") )
			this.servicemapEndpoint += "/";
	}
	
	public void setVerbose( boolean verbose ) {
		this.verbose = verbose;
	}
	
	public void addResource( String serviceUri , JsonElement resource ) {
		resources.put( serviceUri , resource );
	}
	
	public void addResourceFromFile( String serviceUri , String path ) throws IOException {
		String fileContent = Files.lines( Paths.get( path ) ).reduce( (String s1 ,String s2) -> { return s1 + s2; } )
						   					    .get();
		resources.put( serviceUri , JsonParser.parseString( fileContent ) );
	}
	
	public void addError( String serviceUri , int statusCode , JsonElement errorJsonResponse ) {
		failures.put( serviceUri , new ServicemapError( statusCode , errorJsonResponse ) );
	}
	
	private void setResponse( Response response , Callback callback , int statusCode , String payload ) {
		ByteBuffer payloadBytes = ByteBuffer.wrap( payload.getBytes(StandardCharsets.UTF_8 ) );
		response.setStatus(statusCode);
		response.write( true , payloadBytes, callback);
	}
	
	private void setResponse( Response response , Callback callback , int statusCode , String payload , MediaType mediaType ) {
		HttpFields.Mutable responseHeaders = response.getHeaders();
		responseHeaders.put( HttpHeader.CONTENT_TYPE , mediaType.toString() );
		setResponse( response , callback , statusCode , payload );
	}
	
	// Jetty 12 handle
	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		HttpURI httpURI = HttpURI.build( request.getHttpURI() ).query(null);
		
		System.out.println( "Registered error endpoints" );
		errorEndpoints.forEach( (String e, Handler h) -> {
			System.out.println( "\t" + e );
		});
		
		System.out.println( "Registered endpoints" );
		endpoints.forEach( (String e, Handler h) -> {
			System.out.println( "\t" + e );
		});
		
		System.out.println( "Registered resources" );
		resources.forEach( (String r, JsonElement el) -> {
			System.out.println( "\t" + r );
		});
		
		System.out.println( "Registered failures" );
		failures.forEach( (String f, ServicemapError el) -> {
			System.out.println( "\t" + f );
		});
				
		if( httpURI.getPath().isEmpty() || httpURI.getPath().equals( "/" ) || 
			httpURI.getPath().equals( this.servicemapEndpoint ) ) {
			handleResource( request , response , callback );
		} else {
			handleEndpoint( request , response , callback );
		}
		return true;
	}
	
	// Jetty 11 handle
//	@Override
//	public void handle(String target, Request base, HttpServletRequest request, HttpServletResponse response)
//			throws IOException, ServletException {
//		
//		if( target.isEmpty() || target.equals( "/" ) ) {
//			handleResource( target , base , request , response );
//		}else {
//			handleEndpoint( target , base , request , response );
//		}
//	}
	
	// Jetty 12
	public void addEndpoint( String target , Consumer<JsonElement> operation ) {
		if( target.startsWith("/") )
			target = target.substring(1);
		Handler.Abstract h = new Handler.Abstract() {
			@Override
			public boolean handle(Request request, Response response, Callback callback) throws Exception {
				if( HttpMethod.GET.is(request.getMethod() ) )  {
					ByteBuffer responsePayload = ByteBuffer.wrap( "GET".getBytes( StandardCharsets.UTF_8 ) );
					response.write( true , responsePayload , Callback.NOOP );
					return true;
				}
				
				if( HttpMethod.POST.is(request.getMethod()) ) {
					Content.Source.asString(request, StandardCharsets.UTF_8 , new Promise<String>() {
						@Override
						public void succeeded(String result) {
							
							JsonElement reqBody = JsonParser.parseString( result );
							operation.accept(reqBody);
							
//							ByteBuffer responseBody = ByteBuffer.wrap( reqBody.toString().getBytes(StandardCharsets.UTF_8) );
//							response.setStatus( HttpStatus.OK_200 );
//							response.write( true , responseBody , callback );
							setResponse( response , callback , HttpStatus.OK_200 , result );
							callback.succeeded();
						}
						
						@Override
						public void failed(Throwable x) {
							response.setStatus( HttpStatus.INTERNAL_SERVER_ERROR_500 );
							callback.failed(x);
						}
					});
				}
				return true;
			}
		};
		endpoints.put( target , h );
	}
	
	// Jetty 11
//	public void addEndpoint11( String target , Consumer<JsonElement> operation ) {
//		AbstractHandler h = new AbstractHandler() {
//			@Override
//			public void handle(String target, Request base, HttpServletRequest request, HttpServletResponse response)
//					throws IOException, ServletException {
//				if( request.getMethod().equals( HttpMethod.GET.name() ) ){
//					response.getWriter().println( "GET" );
//				}
//				
//				if( request.getMethod().equals( HttpMethod.POST.name() ) ){
////					String body = request.getReader().lines().reduce( "" , 
////						(String s1, String s2) -> {
////							return s1 + s2;
////					});
//					
//					String body = request.getReader().lines().collect(
//						Collectors.joining( System.lineSeparator() )
//					);
//					
//					
//					JsonElement reqBody = JsonParser.parseString( body );
//					operation.accept( reqBody );
//					
//					response.getWriter().println( reqBody.toString() );
//				}
//				
//				response.setStatus( HttpServletResponse.SC_OK );
//				base.setHandled( true );
//			}
//			
//		};
//		endpoints.put( target , h );
//	}
	
	// Jetty 12
	public void addErrorEndpoint( String target , Consumer<JsonElement> operation , int statusCode ) {
		Handler.Abstract h = new Handler.Abstract() {
			@Override
			public boolean handle(Request request, Response response, Callback callback) throws Exception {
				if( HttpMethod.POST.is( request.getMethod() ) ) {
					Content.Source.asString(request, StandardCharsets.UTF_8, new Promise<String>() {
						@Override
						public void succeeded(String result) {
							JsonElement reqBody = JsonParser.parseString( result );
							operation.accept(reqBody);							
							setResponse( response , callback , statusCode , result );
							callback.succeeded();
						}
						@Override
						public void failed(Throwable x) {
							response.setStatus( HttpStatus.INTERNAL_SERVER_ERROR_500 );
							callback.failed(x);
						}
					});
				}
				return true;
			}
		};
		errorEndpoints.put( target , h );
	}
	
	// Jetty 11
//	public void addErrorEndpoint11( String target , Consumer<JsonElement> operation , int statusCode ) {
//		AbstractHandler h = new AbstractHandler() {
//			@Override
//			public void handle(String target, Request base, HttpServletRequest request, HttpServletResponse response)
//					throws IOException, ServletException {
//				if( request.getMethod().equals( HttpMethod.POST.name() ) ){
////					String body = request.getReader().lines().reduce( "" , 
////						(String s1, String s2) -> {
////							return s1 + s2;
////					});
//					
//					String body = request.getReader().lines().collect(
//						Collectors.joining( System.lineSeparator() )
//					);
//					
//					JsonElement reqBody = JsonParser.parseString( body );
//					operation.accept( reqBody );
//					response.getWriter().println( reqBody.toString() );
//				}
//				
//				response.setStatus( statusCode );
//				base.setHandled( true );
//			}
//		};
//		errorEndpoints.put( target , h );
//	}
	
	// Jetty 12
	private void handleEndpoint( Request request , Response response, Callback callback)
			throws Exception {
		HttpURI httpURI = HttpURI.build( request.getHttpURI() ).query(null);
		Fields params = Request.getParameters(request).asImmutable();
		HttpFields requestHeaders = request.getHeaders().asImmutable();
		
		System.out.println( "------ Servicemap Mock Handler ------" );
		System.out.println( "Handling an Endpoint");
		System.out.println( "Target: " + httpURI.getPath() );
		System.out.println( "Parameters: " + params.toString() );
		System.out.println( "RequestURI: " + httpURI.asString() );
		System.out.println( "Headers: " + requestHeaders.toString() );
		
		String endpoint = httpURI.getPath();
		if( endpoint.startsWith(this.servicemapEndpoint) )
			endpoint = endpoint.substring( this.servicemapEndpoint.length() );
		System.out.println( "Endpont: " + endpoint );
		
		if( errorEndpoints.containsKey( endpoint ) ) {
			errorEndpoints.get( endpoint ).handle( request , response , callback );
		} 
		else if ( endpoints.containsKey( endpoint ) )
			endpoints.get( endpoint ).handle( request , response , callback );
		else {
			setResponse( response , callback , HttpStatus.NOT_FOUND_404 , 
				String.format( "{\"error\":\"No service for target='%s'\"}" , httpURI.getPath() )
			);
		}		
	}
	
	// Jetty 11
//	private void handleEndpoint( String target , Request base , HttpServletRequest request, HttpServletResponse response)
//			throws IOException, ServletException {
//		System.out.println( "------ Servicemap Mock Handler ------" );
//		System.out.println( "Handling an Endpoint");
//		System.out.println( "Target: " + target );
//		System.out.println( "Base: " + base.toString() );
//		System.out.println( "Parameters: " + request.getParameterMap().toString() );
//		System.out.println( "RequestURL: " + request.getRequestURL() );
//		System.out.println( "requestURI: " + request.getRequestURI() );
//		Enumeration<String> headerNames = request.getHeaderNames();
//		Map<String , String> headersMap = new HashMap<>();
//		while( headerNames.hasMoreElements() ) {
//			String name = headerNames.nextElement();
//			String value = request.getHeader( name );
//			headersMap.put( name , value );
//		}
//		System.out.println( "Headers: " + headersMap.toString() );
//		
//		if( errorEndpoints.containsKey( target ) ) {
//			errorEndpoints.get( target ).handle( target , base , request , response );
//		} 
//		else if( endpoints.containsKey( target ) )
//			endpoints.get(target).handle( target , base , request , response );
//		else {
//			response.setStatus( HttpServletResponse.SC_NOT_FOUND );
//			response.getWriter().println( 
//					String.format( "{\"error\":\"No service for target='%s'\"}" , target ) 
//			);
//			base.setHandled(true);
//			return;
//		}
//	}
	
	// Jetty 12
	private void handleResource( Request request , Response response , Callback callback )
			throws Exception {
		
		HttpURI httpURI = HttpURI.build( request.getHttpURI() ).query(null);
		Fields params = Request.getParameters(request).asImmutable();
		HttpFields requestHeaders = request.getHeaders().asImmutable();
		HttpFields.Mutable responseHeaders = response.getHeaders();
		
		System.out.println( "------ Servicemap Mock Handler ------" );
		System.out.println( "Handling a Resouce");
		System.out.println( "Target: " + httpURI.getPath() );
		System.out.println( "requestURI: " + httpURI.toString() );
		System.out.println( "Headers: " + requestHeaders.toString() );
		
		
		responseHeaders.put( HttpHeader.CONTENT_TYPE , "application/json" );

		if( !HttpMethod.GET.is( request.getMethod() ) ) {
			response.setStatus( HttpStatus.METHOD_NOT_ALLOWED_405 );
			setResponse( response , callback , HttpStatus.METHOD_NOT_ALLOWED_405 , 
				"{\"error\":\"" + request.getMethod() + " method not allowed.\"}"
			);
			return;
		}
		
		if( verbose )
			System.out.println( "Request parameters: " + params.toString() );
		
		if( params.get( "serviceUri") == null ) {
			setResponse( response , callback , HttpStatus.BAD_REQUEST_400 , 
				"{\"error\":\"'serviceUri not provided.'\"}"
			);
			return;
		}
		
		String serviceUri = params.getValue( "serviceUri" ); 
		if( verbose )
			System.out.println( "Found serviceUri: " + serviceUri );
		
		if( failures.containsKey( serviceUri ) ) {
			ServicemapError failure = failures.get( serviceUri );
			setResponse( response , callback , failure.statusCode , failure.errorJsonResponse.toString() );
			return;
		}
		
		if( !resources.containsKey( serviceUri ) ) {
			setResponse( response , callback , HttpStatus.NOT_FOUND_404 , 
				String.format( "{\"error\":\"No resource for serviceUri='%s'\"}" , serviceUri )
			);
			return;
		}
		
		JsonElement resource = resources.get( serviceUri );	
		if( verbose )
			System.out.println( "Reply with resource: " + resource.toString() );
		setResponse( response , callback , HttpStatus.OK_200 , resource.toString() );
	}
	
	// Jetty 11
//	private void handleResource( String target , Request base , HttpServletRequest request, HttpServletResponse response)
//			throws IOException, ServletException {
//		
//		System.out.println( "------ Servicemap Mock Handler ------" );
//		System.out.println( "Handling a Resouce");
//		System.out.println( "Target: " + target );
//		System.out.println( "Base: " + base.toString() );
//		System.out.println( "Parameters: " + request.getParameterMap().toString() );
//		System.out.println( "RequestURL: " + request.getRequestURL() );
//		System.out.println( "requestURI: " + request.getRequestURI() );
//		Enumeration<String> headerNames = request.getHeaderNames();
//		Map<String , String> headersMap = new HashMap<>();
//		while( headerNames.hasMoreElements() ) {
//			String name = headerNames.nextElement();
//			String value = request.getHeader( name );
//			headersMap.put( name , value );
//		}
//		System.out.println( "Headers: " + headersMap.toString() );
//		
//		
//		response.setContentType( "application/json" );
//		
//		if( !request.getMethod().equals( HttpMethod.GET.name() ) ){
//			response.setStatus( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
//			response.getWriter().println( "{\"error\":\"" + request.getMethod() + " method not allowed.\"}" );
//			base.setHandled( true );
//			return;
//		}
//		
//		Map<String,String[]> params = request.getParameterMap();
//		
//		if( verbose )
//			System.out.println( "Request parameters: " + params.toString() );
//		
//		if( !params.containsKey( "serviceUri" ) ) {
//			response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
//			response.getWriter().println( "{\"error\":\"'serviceUri not provided.'\"}" );
//			base.setHandled( true );
//			return;
//		}
//		
//		String serviceUri = params.get("serviceUri")[0];
//		
//		if( verbose )
//			System.out.println( "Found serviceUri: " + serviceUri );
//		
//		if( failures.containsKey( serviceUri ) ){
//			ServicemapError failure = failures.get( serviceUri );
//			response.setStatus( failure.statusCode );
//			response.getWriter().println( failure.errorJsonResponse.toString() );
//			base.setHandled(true);
//			return;
//		}
//		
//		if( !resources.containsKey( serviceUri ) ) {
//			response.setStatus( HttpServletResponse.SC_NOT_FOUND );
//			response.getWriter().println( 
//					String.format( "{\"error\":\"No resource for serviceUri='%s'\"}" , serviceUri ) 
//			);
//			base.setHandled(true);
//			return;
//		}
//		
//		JsonElement resource = resources.get( serviceUri );
//	
//		if( verbose )
//			System.out.println( "Reply with resource: " + resource.toString() );
//		
//		response.setStatus( HttpServletResponse.SC_OK );
//		base.setHandled(true);
//		response.getWriter().write( resource.toString() );
//	}
	
	public class ServicemapError{
		public final int statusCode;
		public final JsonElement errorJsonResponse;
		
		public ServicemapError( int statusCode , JsonElement errorJsonResponse ) {
			this.statusCode = statusCode;
			this.errorJsonResponse = errorJsonResponse;
		}
		
		public String toString() {
			return String.format( "{'statusCode':%d , 'errorJsonResponse':%s}", 
					statusCode , errorJsonResponse.toString() );
		}
	}
}
