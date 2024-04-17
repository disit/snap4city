package org.disit.nifi.processors.enrich_data.json_processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonProcessing {

	private JsonProcessing() {};
	
	/**
	 * Retrieve a JsonElement from a JsonObject whose path is specified by the path list.
	 * @param object the object to seek.
	 * @param path a List<String> containing the elements of the path for the desired element from the root to the element.
	 * @return the specified JsonElement on success.
	 * @throws NoSuchElementException if the specified element is not contained in the object.
	 */
	public static JsonElement getElementByPath( JsonElement rootEl , List<String> path ) throws NoSuchElementException{
    	JsonElement el = rootEl;
//    	StringBuilder exploredPath = new StringBuilder(); // logging purpose
    	List<String> expPath = new ArrayList<>(); // logging
    	String targetPath = path.stream().reduce( (String s1, String s2) -> { return s1+"/"+s2;} ).get(); // logging 
    	for( String pathEl : path ) {
//    		exploredPath.append("/").append( pathEl );
    		expPath.add( pathEl );
    		
    		while( el.isJsonArray() ) {
				el = el.getAsJsonArray();
				if( el.getAsJsonArray().size() > 0 ) {
					el = el.getAsJsonArray().get( 0 );
				}else {
//					if( exploredPath.length() > 0) exploredPath.deleteCharAt(0);
					throw new NoSuchElementException( String.format( 
					    "Cannot obtain '%s'. The '%s' field in the root element contains an empty array." ,
					    targetPath ,
//					    exploredPath.toString() 
					    pathListToPathString( expPath )
					) );
				}
			}
    		
    		if( el.isJsonObject() ) {
    			JsonObject elObj = el.getAsJsonObject();
    			
    			if( elObj.size() > 0 ) {
	    			if( elObj.has( pathEl ) ) {
	    				el = elObj.get( pathEl );
	    			}else {
//	    				if( exploredPath.length() > 0) exploredPath.deleteCharAt(0);
	    				throw new NoSuchElementException( String.format( 
	    					"Cannot obtain '%s'. The '%s' field in the root element does not exists.",
						    targetPath ,
//					        exploredPath.toString()+"/"+pathEl
						    pathListToPathString( expPath )
					    ) );
	    			}
    			} else {
//    				if( exploredPath.length() > 0) exploredPath.deleteCharAt(0);
    				throw new NoSuchElementException( String.format( 
    					"Cannot obtain '%s'. The '%s' field in the root element is empty.", 
					    targetPath ,
//					    exploredPath.toString() 
					    pathListToPathString( expPath )
					) );
    			}
    		} else {
//    			if( exploredPath.length() > 0) exploredPath.deleteCharAt(0);
    			throw new NoSuchElementException( String.format( 
    				"Cannot obtain '%s'. The '%s' field in the root element is not a JsonObject." , 
					targetPath ,
//					exploredPath.toString() 
					pathListToPathString( expPath )
				) );
    		}
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
	
	public static String pathListToPathString( List<String> path ) {
		if( path.isEmpty() ) return "";
		return path.stream().reduce( (String s1, String s2) -> { return s1+"/"+s2; } ).get();
	}

	public static List<String> getAllLinearizedProperties( JsonObject obj ){
		List<String> properties = new ArrayList<>();
		
		for( Map.Entry<String, JsonElement> tle : obj.entrySet() ) {
			if( tle.getValue().isJsonObject() ) {
				Queue<Pair<String, JsonObject>> q = new LinkedList<>();
				q.offer( new Pair<String, JsonObject>( tle.getKey() , tle.getValue().getAsJsonObject() ) );
				
				while( !q.isEmpty() ) {
					Pair<String, JsonObject> cur = q.poll();
					properties.add( cur.getFirst() );
					for( Map.Entry<String, JsonElement> ce : cur.getSecond().entrySet() ) {
						if( ce.getValue().isJsonObject() )
							q.offer( new Pair<String, JsonObject>( 
								cur.getFirst().concat(".").concat( ce.getKey() ) , 
								ce.getValue().getAsJsonObject() )  );
					}
				}
			}else {
				properties.add( tle.getKey() );
			}
		}
		
		return properties;
	}
	
	// Utility pair class
	private static class Pair<X , Y>{
		private X x;
		private Y y;
		public Pair( X x , Y y){
			this.x = x;
			this.y = y;
		}
		public X getFirst() { return this.x; }
		public Y getSecond() { return this.y; }
	}
}
