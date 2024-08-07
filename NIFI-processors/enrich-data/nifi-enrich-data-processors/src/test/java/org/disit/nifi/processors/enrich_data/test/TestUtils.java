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

package org.disit.nifi.processors.enrich_data.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.nifi.util.LogMessage;
import org.apache.nifi.util.MockComponentLog;
import org.apache.nifi.util.MockFlowFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestUtils {

	private TestUtils() {}
	
	public static String mockJsonStringFromFile( Path path ) throws IOException{
    	
		String content = Files.lines( path )
							  .reduce( (String s1 , String s2) -> { return s1 + s2; } )
							  .get();
		
		return JsonParser.parseString( content ).getAsJsonObject().toString();
    }
    
    public static JsonObject mockJsonObjFromFile( Path path ) throws IOException{
    	
		String content = Files.lines( path )
							  .reduce( (String s1 , String s2) -> { return s1 + s2; } )
							  .get();
		
		return JsonParser.parseString(content).getAsJsonObject();
    }
    
    public static JsonArray mockJsonArrayFromFile( Path path ) throws IOException{
    	
    	String content = Files.lines( path )
    						  .reduce( (s1 , s2) -> { return s1 + s2; } )
    						  .get();
    	
    	return JsonParser.parseString(content).getAsJsonArray();
    }
    
    public static JsonElement mockJsonElementFromFile( Path path ) throws IOException{
    	
    	String content = Files.lines( path )
				  			  .reduce( (s1 , s2) -> { return s1 + s2; } )
				  			  .get();
    	
    	return JsonParser.parseString(content);
    	
    }
    
    public static JsonElement prepareExpectedResult( String resultContentFile , MockFlowFile ff ) throws IOException {
    	JsonElement expectedResult = mockJsonElementFromFile( Paths.get( resultContentFile ) );
    	String uuid = ff.getAttribute( "uuid" );
    	if( expectedResult.isJsonArray() ) {
    		JsonArray er = expectedResult.getAsJsonArray();
    		for( int i=0 ; i < er.size() ; i++ ) {
    			er.get( i ).getAsJsonObject().addProperty( "uuid" , uuid );
    		}
    	}
    	
    	if( expectedResult.isJsonObject() ) {
    		JsonObject er = expectedResult.getAsJsonObject();
    		er.entrySet().stream().forEach( (Map.Entry<String , JsonElement> entry) -> {
    			entry.getValue().getAsJsonObject().addProperty( "uuid" , uuid );
    		});
    	}
    	
    	return expectedResult;
    }
    
    public static JsonElement prepareExpectedDeviceState( String resultContentFile , MockFlowFile ff ) throws IOException{
    	JsonElement expectedState = mockJsonElementFromFile( Paths.get( resultContentFile ) );
    	String uuid = ff.getAttribute("uuid");
    	
    	if( expectedState.isJsonObject() ) {
    		JsonObject es = expectedState.getAsJsonObject();
    		es.addProperty( "uuid" , uuid );
    	}
    	
    	return expectedState;
    }
    
    public static String prettyOutFF( MockFlowFile outFF ) {
        
    	Gson gson = new GsonBuilder().setPrettyPrinting().create();
    	
    	String prettyOutFFContent = gson.toJson( JsonParser.parseString( new String( outFF.toByteArray() ) ) );
    	
    	String prettyOutFFAttributes = outFF.getAttributes().entrySet().stream()
       		 .map( entry -> { return entry.getKey() + " : " + entry.getValue(); } )
       		 .reduce( (s1 , s2) -> { return s1 + "\n\t" + s2; } )
       		 .get();
    	
    	StringBuilder prettyOutResult = new StringBuilder( "FF-Attributes:\n{\n\t" )
    			.append( prettyOutFFAttributes )
    			.append( "\n}\n\nFF-Content:\n" )
    			.append( prettyOutFFContent );
    	
    	return prettyOutResult.toString();
    }
    
    public static String notPrettyOutFF( MockFlowFile outFF ) {
    	
    	String prettyOutFFAttributes = outFF.getAttributes().entrySet().stream()
          		 .map( entry -> { return entry.getKey() + " : " + entry.getValue(); } )
          		 .reduce( (s1 , s2) -> { return s1 + "\n\t" + s2; } )
          		 .get();
    	
    	StringBuilder outResult = new StringBuilder( "FF-Attributes:\n{\n\t" )
    			.append( prettyOutFFAttributes )
    			.append( "\n}\n\nFF-Content:\n" )
    			.append( new String( outFF.toByteArray() ) );
    	
    	return outResult.toString();
    	
    }

    public static void logsToStderr( MockComponentLog logger ) {
    	List<LogMessage> logs = new ArrayList<>();
		logs.addAll( logger.getTraceMessages() );
		logs.addAll( logger.getDebugMessages() );
		logs.addAll( logger.getInfoMessages() );
		logs.addAll( logger.getWarnMessages() );
		logs.addAll( logger.getErrorMessages() );
		logs.stream().forEach( (LogMessage l) -> {
			System.err.println( l.toString() );
		});
    }

    public static void fixJsonOutputAttribute( JsonElement content , JsonElement expected , String prop , String attrName ) {
    	if( content.getAsJsonObject().get(prop).getAsJsonObject().getAsJsonObject().has(attrName) ) {
    		expected.getAsJsonObject().get(prop).getAsJsonObject().add(
    			attrName , content.getAsJsonObject().get(prop).getAsJsonObject().get( attrName ) );
    	}
    }
    
    public static void fixSplitJsonAttribute( JsonElement content , JsonArray expected , int outIndex , String attrName ) {
    	if( content.getAsJsonObject().has( attrName ) ) {
    		expected.get(outIndex).getAsJsonObject().add( attrName , 
    			content.getAsJsonObject().get( attrName ) );
    	}
    }
    
    public static void fixDeviceStateAttribute( JsonElement deviceState , JsonElement expectedState , String attrName ) {
    	if( deviceState.getAsJsonObject().has( attrName ) ) {
    		expectedState.getAsJsonObject().add( attrName , 
    			deviceState.getAsJsonObject().get( attrName ) );
    	}
    }

}
