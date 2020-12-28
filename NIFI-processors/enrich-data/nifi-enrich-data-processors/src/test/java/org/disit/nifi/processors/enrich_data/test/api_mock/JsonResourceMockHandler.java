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

public class JsonResourceMockHandler extends AbstractHandler{

	private Map<String , JsonElement> resources;
	private JsonParser parser;
	private String identifierParameterName;
	
	public JsonResourceMockHandler( String identifierParameterName ) {
		this.resources = new HashMap<>();
		this.parser = new JsonParser();
		this.identifierParameterName = identifierParameterName;
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
		
		System.out.println( "------ Handler ------" );
		System.out.println( "OWNERSHIP GET TARGET: " + target );
		
		response.setContentType( "application/json" );
		
		if( !request.getMethod().equals( HttpMethod.GET.name() ) ){
			response.setStatus( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
			response.getWriter().println( "{\"error\":\"" + request.getMethod() + " method not allowed.\"}" );
			base.setHandled( true );
			return;
		}
		
		Map<String,String[]> params = request.getParameterMap();
		System.out.println( "OWNERSHIP GET PARAMETERS: " + params.toString() );
		
		Enumeration<String> headerNames = request.getHeaderNames();
		Map<String , String> headersMap = new HashMap<>();
		while( headerNames.hasMoreElements() ) {
			String headerName = headerNames.nextElement();
			String headerValue = request.getHeader( headerName );
			headersMap.put( headerName , headerValue );
		}
		
		System.out.println( "OWNERSHIP GET HEADERS: " + headersMap.toString() );
		
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
