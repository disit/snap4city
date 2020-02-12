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
import java.nio.file.Files;
import java.nio.file.Paths;
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

public class ServicemapMockHandler extends AbstractHandler{

	
	private Map<String, JsonElement> resources;
	private JsonParser parser;
	private boolean verbose;
	
	public ServicemapMockHandler() {
		resources = new HashMap<>();
		parser = new JsonParser();
		verbose = false;
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
		resources.put( serviceUri , parser.parse( fileContent ) );
	}
	
	@Override
	public void handle(String target, Request base, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setContentType( "application/json" );
		
		if( !request.getMethod().equals( HttpMethod.GET.name() ) ){
			response.setStatus( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
			response.getWriter().println( "{\"error\":\"" + request.getMethod() + " method not allowed.\"}" );
			base.setHandled( true );
			return;
		}
		
		Map<String,String[]> params = request.getParameterMap();
		
		if( verbose )
			System.out.println( "Request parameters: " + params.toString() );
		
		if( !params.containsKey( "serviceUri" ) ) {
			response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
			response.getWriter().println( "{\"error\":\"'serviceUri not provided.'\"}" );
			base.setHandled( true );
			return;
		}
		
		String serviceUri = params.get("serviceUri")[0];
		
		if( verbose )
			System.out.println( "Found serviceUri: " + serviceUri );
		
		if( !resources.containsKey( serviceUri ) ) {
			response.setStatus( HttpServletResponse.SC_NOT_FOUND );
			response.getWriter().println( 
					String.format( "{\"error\":\"No resource for serviceUri='%s'\"}" , serviceUri ) 
			);
		}
		
		JsonElement resource = resources.get( serviceUri );
	
		if( verbose )
			System.out.println( "Respond with resource: " + resource.toString() );
		
		response.setStatus( HttpServletResponse.SC_OK );
		base.setHandled(true);
		response.getWriter().write( resource.toString() );
	}

}
