/**
 *  Nifi IngestData processor
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

package org.disit.nifi.processors.ingest_data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.nifi.util.MockFlowFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestUtils {

	private TestUtils() {}
	
	public static String mockJsonStringFromFile( Path path , JsonParser parser ) throws IOException{
    	
		String content = Files.lines( path )
							  .reduce( (String s1 , String s2) -> { return s1 + s2; } )
							  .get();
		
		return parser.parse( content ).getAsJsonObject().toString();
    }
    
    public static JsonObject mockJsonObjFromFile( Path path , JsonParser parser ) throws IOException{
    	
		String content = Files.lines( path )
							  .reduce( (String s1 , String s2) -> { return s1 + s2; } )
							  .get();
		
		return parser.parse( content ).getAsJsonObject();
    }
    
    
    public static JsonArray mockJsonArrayFromFile( Path path , JsonParser parser ) throws IOException{
    	
    	String content = Files.lines( path )
    						  .reduce( (s1 , s2) -> { return s1 + s2; } )
    						  .get();
    	
    	return parser.parse( content ).getAsJsonArray();
    }
    
    public static JsonElement mockJsonElementFromFile( Path path , JsonParser parser ) throws IOException{
    	
    	String content = Files.lines( path )
				  			  .reduce( (s1 , s2) -> { return s1 + s2; } )
				  			  .get();
    	
    	return parser.parse( content );
    	
    }
    
    public static String prettyOutFF( MockFlowFile outFF , JsonParser jsonParser ) {
        
    	Gson gson = new GsonBuilder().setPrettyPrinting().create();
    	
    	String prettyOutFFContent = gson.toJson( jsonParser.parse( new String( outFF.toByteArray() ) ) );
    	
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
}
