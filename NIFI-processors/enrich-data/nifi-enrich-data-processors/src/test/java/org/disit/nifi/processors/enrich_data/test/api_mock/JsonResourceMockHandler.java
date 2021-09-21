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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Handler to mock json resources
 */
public class JsonResourceMockHandler extends AbstractHandler{

	private Map<String , JsonElement> resources;
	private JsonParser parser;
	private String identifierParameterName;
	private String handlerName;
	
	public JsonResourceMockHandler( String identifierParameterName , String handlerName ) {
		this.resources = new HashMap<>();
		this.parser = new JsonParser();
		this.identifierParameterName = identifierParameterName;
		this.handlerName = handlerName;
	}
	
	public Map<String , JsonElement> getResources(){
		return resources;
	}
	
	public JsonElement getResource( String identifier ) throws Exception{
		JsonElement resource = resources.get( identifier );
		if( resource == null )
			throw new Exception( "Resource not found" );
		return resource;
	}
	
	@Override
	public void handle(String target, Request base, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		System.out.println( String.format( "------ %s ------" , this.handlerName) );
		System.out.println( "TARGET: " + target );
		System.out.println( "Request URL: " + request.getRequestURL() );
		System.out.println( "Request URI: " + request.getRequestURI() );
		
		response.setContentType( "application/json" );
		
		if( !request.getMethod().equals( HttpMethod.GET.name() ) ){
			response.setStatus( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
			response.getWriter().println( "{\"error\":\"" + request.getMethod() + " method not allowed.\"}" );
			base.setHandled( true );
			return;
		}
		
		Map<String,String[]> params = request.getParameterMap();
		System.out.println( "PARAMETERS: " + params.toString() );
		
		Enumeration<String> headerNames = request.getHeaderNames();
		Map<String , String> headersMap = new HashMap<>();
		while( headerNames.hasMoreElements() ) {
			String headerName = headerNames.nextElement();
			String headerValue = request.getHeader( headerName );
			headersMap.put( headerName , headerValue );
		}
		
		System.out.println( "HEADERS: " + headersMap.toString() );
		
		if( !params.containsKey( this.identifierParameterName ) ) {
			response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
			response.getWriter().println( "{\"error\":\"'" + this.identifierParameterName + "not provided.'\"}" );
			base.setHandled( true );
			return;
		}
		
		String identifier = params.get( this.identifierParameterName )[0];
		
		if( !resources.containsKey( identifier ) ) {
			response.setStatus( HttpServletResponse.SC_NOT_FOUND );
			response.getWriter().println( 
					String.format( "{\"error\":\"No resource for %s='%s'\"}" , 
								   identifierParameterName , identifier ) 
			);
			return;
		}
		
		System.out.println( "------------" );
		
		JsonElement resource = resources.get( identifier );
		response.setStatus( HttpServletResponse.SC_OK );
		base.setHandled( true );
		response.getWriter().write( resource.toString() );
		
	}
	
	public void addJsonResource( String identifier , JsonElement resource ) {
		resources.put( identifier , resource );
	}
	
	public void addJsonResourceFromFile( String identifier , String filePath ) throws IOException{
		String fileContent = Files.lines( Paths.get( filePath ) ).reduce( (String s1 ,String s2) -> { return s1 + s2; } )
				    .get();
		resources.put( identifier , parser.parse( fileContent ) );
	}
}
