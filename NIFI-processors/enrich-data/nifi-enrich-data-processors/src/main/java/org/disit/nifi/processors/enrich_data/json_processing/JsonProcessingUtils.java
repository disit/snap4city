package org.disit.nifi.processors.enrich_data.json_processing;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonProcessingUtils {

	private JsonProcessingUtils() {};
	
	/**
	 * Retrieve a JsonElement from a JsonObject whose path is specified by the path list.
	 * @param object the object to seek.
	 * @param path a List<String> containing the elements of the path for the desired element from the root to the element.
	 * @return the specified JsonElement on success.
	 * @throws NoSuchElementException if the specified element is not contained in the object.
	 */
	public static JsonElement getElementByPath( JsonElement rootEl , List<String> path ) throws NoSuchElementException{
    	JsonElement el = rootEl;
    	StringBuilder exploredPath = new StringBuilder(); // logging purpose
    	for( String pathEl : path ) {
    		while( el.isJsonArray() ) {
				el = el.getAsJsonArray();
				if( el.getAsJsonArray().size() > 0 ) {
					el = el.getAsJsonArray().get( 0 );
				}else {
					if( exploredPath.length() > 0) exploredPath.deleteCharAt(0);
					throw new NoSuchElementException( String.format( "Cannot obtain the specified element. The '%s' field in the root element contains an empty array." , 
															   exploredPath.toString() ) );
				}
			}
    		
    		if( el.isJsonObject() ) {
    			JsonObject elObj = el.getAsJsonObject();
    			
    			if( elObj.size() > 0 ) {
	    			if( elObj.has( pathEl ) ) {
	    				el = elObj.get( pathEl );
	    			}else {
	    				if( exploredPath.length() > 0) exploredPath.deleteCharAt(0);
	    				throw new NoSuchElementException( String.format( "Cannot obtain the specified element. The '%s' field in the root element does not exists. ", 
								   							        exploredPath.toString() ) );
	    			}
    			} else {
    				if( exploredPath.length() > 0) exploredPath.deleteCharAt(0);
    				throw new NoSuchElementException( String.format( "Cannot obtain enrichment object. The '%s' field in the root element is empty. ", 
							   								   exploredPath.toString() ) );
    			}
    		} else {
    			if( exploredPath.length() > 0) exploredPath.deleteCharAt(0);
    			throw new NoSuchElementException( String.format( "The '%s' field in the root element is not a JsonObject." , 
    													   exploredPath.toString() ) );
    		}
    		
    		exploredPath.append("/").append( pathEl );
    	}
    	
    	return el;
    }
	
	public static JsonElement getElementByPath( JsonElement rootEl , String path ) throws NoSuchElementException {
		List<String> pathList = pathStringToPathList(path);
		return getElementByPath( rootEl , pathList );
	}
	
	public static List<String> pathStringToPathList( String path ){
		List<String> pathList = Arrays.asList( path.split("/") ).stream()
									  .map( (String s) -> {return s.trim();} )
									  .collect( Collectors.toList() );
		return pathList;
	}
}
