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

package org.disit.nifi.processors.enrich_data.enricher;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class EnrichUtils {
	
	private EnrichUtils() {};
	
	public static final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" );
	public static final DateTimeFormatter iso8601FullOutFormatter = DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXXXX" );
	
	public static final String toFullISO8601Format( String timestamp ) throws DateTimeParseException, DateTimeException{
//		OffsetDateTime ot = OffsetDateTime.parse( timestamp , DateTimeFormatter.ISO_OFFSET_DATE_TIME );
		OffsetDateTime ot = OffsetDateTime.parse( timestamp );
		return iso8601FullOutFormatter.format( ot );
	}
	
	public static final String toFullISO8601Format( OffsetDateTime timestamp ) throws DateTimeException {
		return iso8601FullOutFormatter.format( timestamp );
	}
	
	/**
     * Get the value of a dynamic field.
     * 
     * @param fieldPath the path in the enrichment response from which to pick the field value
     * @param responseRootEl enrichment response object
     * @return the value of the spiecified field as a String
     * @throws NoSuchElementException if the field cannot be found in the enrichment response object
     * @throws IllegalArgumentException if the field specified does not contain a primitive type in the enrichment response object
     */
    public static String getAdditionalFieldValue( List<String> fieldPath , JsonElement responseRootEl ) throws NoSuchElementException , IllegalArgumentException{
    	
    	JsonElement el = responseRootEl;
    	for( String pathEl : fieldPath ) {
    		while( el.isJsonArray() ) {
    			el = el.getAsJsonArray().get( 0 );
    		}
    		
    		if( el.isJsonObject() ) {
    			JsonObject elObj = el.getAsJsonObject();
    			
    			if( elObj.has( pathEl ) ) {
    				el = elObj.get( pathEl );
    			} else {
    				throw new NoSuchElementException( "Cannot find the specified field in the enrichment response object." );
    			}
    		}
    	}
    	
    	if( el.isJsonPrimitive() ) {
    		return el.getAsString();
    	}else {
    		throw new IllegalArgumentException( "The specified field does not contain a primitive type in the enrichment response." );
    	}	
    }
	
	 /* Get the timestamp from the objects inside rootObject which names
     * are in candidateMembers list.
     * 
     * @param rootObject the JsonObject to analyze to get the timestamp.
     * @param candidateMembers a List of member names. The timestamp will be picked from
     * the rootObject as the first field in the list which can be correctly parsed as an OffsetDatetime.
     * @return a Map.Entry<String , String> containing the name of the field from which the timestamp 
     * has been picked as the key, and the timestamp converted to string as value.
     */
    public static Map.Entry<String, String> getTimestampFromContentField( JsonObject rootObject , String valueFieldName , List<String> candidateMembers ) {
    	
    	for( String candidate : candidateMembers ) {
    		
    		JsonObject candidateObject = rootObject.get( candidate ).getAsJsonObject();
    		
    		String timestampStr = ""; 
    		
    		// The candidate object contains a field named as valueFieldName from which to pick the timestamp.
    		// This function is called after the enrichment, so the field containing the value could have been 
    		// renamed concatenating "_str" to the end.
    		if( candidateObject.has( valueFieldName ) ) {
    			JsonElement dateValue = candidateObject.get( valueFieldName );
    			if( dateValue.isJsonPrimitive() ) 
    				try {
    					timestampStr = dateValue.toString();
    				}catch(IllegalStateException ex ) { } 
    		}
    		
			if( candidateObject.has( valueFieldName.concat( Enricher.VALUE_NAME_STR_SUFFIX ) ) ) {
				JsonElement dateValue = candidateObject.get( valueFieldName.concat( Enricher.VALUE_NAME_STR_SUFFIX ) );
				if( dateValue.isJsonPrimitive() && dateValue.getAsJsonPrimitive().isString() )
					try {
						timestampStr = dateValue.getAsString();
					}catch(IllegalStateException ex) { }
			}
    		
    		if( !timestampStr.isEmpty() ) {
				try {
					OffsetDateTime parsedTimestamp = OffsetDateTime.parse( timestampStr );
					
					return new AbstractMap.SimpleEntry<String , String>( candidate , parsedTimestamp.toString() );
				} catch( DateTimeParseException ex ) { }
    		}
    	}
    	
    	return new AbstractMap.SimpleEntry<String , String>( "" , "" ); // If the date cannot be parsed from the candidates or there's no candidate return an empty string.
    }
	
    /**
     * Enrich the rootObject with the given timestamp.
     * 
     * @param rootObject the rootObject containing the objects to be enriched.
     * @param timestamp the timestamp to enrich objects with.
     */
    public static void enrichWithDateTime( String timestampFieldName, JsonObject rootObject , String timestamp ) {
    	
    	OffsetDateTime dateTime = OffsetDateTime.parse( timestamp );
    	Map<String , String> dateTimes = generateTruncatedDateTimes( timestampFieldName , dateTime );
    	
    	rootObject.entrySet().forEach( ( Map.Entry<String , JsonElement> rootMember) -> {
    		
    		JsonObject memberObj = rootMember.getValue().getAsJsonObject();
    		
    		dateTimes.entrySet().stream().forEach( ( Map.Entry<String , String> t) -> {
    			memberObj.addProperty( t.getKey() , t.getValue() );
    		});
    		
    	});
    	
    }
    
    /**
     * Generate timestamp with truncations and approximations.
     * 
     * @param dateTime the OffsetDateTime used to generate the timestamp with truncatins.
     * @return a Map<String , String> having as key the entries with the name for the truncation
     * and the truncated dateTime converted to string as value.
     */
    public static Map<String , String> generateTruncatedDateTimes( String timestampFieldName , OffsetDateTime dateTime ){
    	Map<String , String> dateTimes = new TreeMap<>();
    	
    	int seconds10th = dateTime.get( ChronoField.SECOND_OF_MINUTE );
		int minutes10th = dateTime.get( ChronoField.MINUTE_OF_HOUR );
		
		seconds10th = ( seconds10th % 10 ) >= 5 ? seconds10th + ( 10 - ( seconds10th % 10 ) ) : 
												  seconds10th - ( seconds10th % 10 );
		
		minutes10th = ( minutes10th % 10 ) >= 5 ? minutes10th + ( 10 - ( minutes10th % 10 ) ) : 
			  									  minutes10th - ( minutes10th % 10 );	
		
		// Generate timestamp with truncations and approximations
		dateTimes.put( timestampFieldName , dateTime.format( timestampFormatter ) ); //Original timestamp
		dateTimes.put( timestampFieldName + "1sec" , dateTime.truncatedTo( ChronoUnit.SECONDS ).format( timestampFormatter ) );
		dateTimes.put( timestampFieldName + "1min" , dateTime.truncatedTo( ChronoUnit.MINUTES ).format( timestampFormatter ) );
		dateTimes.put( timestampFieldName + "1h" , dateTime.truncatedTo( ChronoUnit.HOURS ).format( timestampFormatter ) );
		dateTimes.put( timestampFieldName + "1d" , dateTime.truncatedTo( ChronoUnit.DAYS ).format( timestampFormatter ) );
		
		if( seconds10th < 59 )
			dateTimes.put( timestampFieldName + "10sec" , dateTime.truncatedTo( ChronoUnit.SECONDS ).withSecond( seconds10th ).format( timestampFormatter ) );
		else
			dateTimes.put( timestampFieldName + "10sec" , dateTime.plusMinutes(1).truncatedTo( ChronoUnit.SECONDS ).withSecond( 0 ).format( timestampFormatter ) );
		
		if( minutes10th < 59 )
			dateTimes.put( timestampFieldName + "10min" , dateTime.truncatedTo( ChronoUnit.MINUTES ).withMinute( minutes10th ).format( timestampFormatter ) );
		else
			dateTimes.put( timestampFieldName + "10min" , dateTime.plusHours(1).truncatedTo( ChronoUnit.MINUTES ).withMinute( 0 ).format( timestampFormatter ) );

		return dateTimes;
    }
    
    public static List<String> getTruncatedDateTimeFieldNames( String timestampFieldName ){
     	return Arrays.asList(
     		timestampFieldName + "1sec" ,
     		timestampFieldName + "1min" ,
     		timestampFieldName + "1h" ,
     		timestampFieldName + "1d" ,
     		timestampFieldName + "10sec" ,
     		timestampFieldName + "10min" 
     	);
    }
    
    public static String generateTimestampSlack( String timestamp , int secondsThreshold ) {
    	OffsetDateTime ot = OffsetDateTime.parse( timestamp );
    	long tsSlack = ot.toInstant().toEpochMilli() - (Instant.now().toEpochMilli() - secondsThreshold*1000 );
    	return String.valueOf( tsSlack );
    }
    
    /**
     * Perform the mapping of the field specified by valueFieldName
     * to one of the following types: 
     * 	- String
     *  - Boolean
     *  - Number
     *  - JsonObject
     *  - JsonArray
     * 
     * The mapping is done in-place on the rootObjMember, the resulting field in such
     * object is named with a suffix indicating the type:
     * 	- "_str" suffix for strings
     *  - "_bool" suffix for booleans
     *  - "_obj" suffix for objects
     *  - "_json_str" suffix for arrays of objects and mixed arrays
     *  
     * Arrays of objects and mixed arrays are converted to string.
     *  
     * If the value type is numeric the field is left untouched;
     *  
     * @param rootObjMember the JsonObject on which the mapping is performed
     * @param valueFieldName the name of the value attribute to map
     */
    public static void mapObjectValue( JsonObject rootObjMember , String valueFieldName , boolean parseNumericStrings ) {
		if( rootObjMember.has( valueFieldName ) ) {
			
			JsonElement valueEl = rootObjMember.get( valueFieldName );
			
			if( valueEl.isJsonPrimitive() ) { // Check if "value" contains a primitive JSON type
				JsonPrimitive primitive = valueEl.getAsJsonPrimitive();
				if( primitive.isString() ) { // Value contains a string
					if( parseNumericStrings ) { // Try numeric string parsing
						tryParseStringValues( rootObjMember , valueEl , valueFieldName );
					} else { // Map directly to string value
						rootObjMember.remove( valueFieldName );
						rootObjMember.addProperty( 
							valueFieldName.concat( Enricher.VALUE_NAME_STR_SUFFIX ) , 
							valueEl.getAsString() );
					}
				}
				
				// Map boolean values to string values
//				if( primitive.isBoolean() ) {
//					rootObjMember.remove( valueFieldName );
//					rootObjMember.addProperty( 
//						valueFieldName.concat( Enricher.VALUE_NAME_STR_SUFFIX ) , 
//						valueEl.getAsString() );
//				}
				if( primitive.isBoolean() ) {
					rootObjMember.remove( valueFieldName );
					rootObjMember.addProperty( 
						valueFieldName.concat( Enricher.VALUE_NAME_BOOL_SUFFIX ) , 
						valueEl.getAsBoolean() );
				}
				
				
				// If the value is a number the "value" attribute is left untouch
			} else { // Non-primitive value types
				mapNonPrimitiveValue( rootObjMember , valueFieldName , valueEl );
			}
		}
    }
    
    /**
     * 
     * @param rootObjMember
     * @param valueEl
     * @param valueFieldName
     */
    private static void tryParseStringValues( JsonObject rootObjMember , JsonElement valueEl , String valueFieldName ) {
    	Number parsedValue = null;
    	String strValue = valueEl.getAsString();
		try {
			parsedValue = Integer.valueOf( strValue );
		} catch( NumberFormatException ex ) {
			try {
				parsedValue = Float.valueOf( strValue );
				if( ((Float)parsedValue).isNaN() )
					parsedValue = null;
			} catch( NumberFormatException ex1 ) { 
				parsedValue = null; 
			}
		}
		
		
		// If the value has been parsed correctly into a numeric type, then add it 
		// with the original valueFieldName, ow add it as string value
		rootObjMember.remove( valueFieldName );
		if( parsedValue != null ) 
			rootObjMember.addProperty( valueFieldName , parsedValue );
		else 
			rootObjMember.addProperty( 
				valueFieldName.concat( Enricher.VALUE_NAME_STR_SUFFIX ) , 
				valueEl.getAsString() );
		
    }
    
    /**
	 * Provides the specialized class for a generic JsonElement, 
	 * either:  JsonNull, JsonObject, JsonArray, Boolean, String, Number
     */
    private static Class<?> getSpecializedClass( JsonElement el ) {
    	if( el.isJsonNull() )
    		return JsonNull.class;
    	if( el.isJsonObject() )
    		return JsonObject.class;
    	if( el.isJsonArray() )
    		return JsonArray.class;
    	else {
    		JsonPrimitive primitive = el.getAsJsonPrimitive();
    		if( primitive.isBoolean() )
    			return Boolean.class;
    		if( primitive.isString() )
    			return String.class;
			return Number.class;
    	}
    }
    
    /**
     * Maps non primitives values
     */
    private static void mapNonPrimitiveValue( JsonObject rootObjMember , String valueFieldName , JsonElement valueEl ) {
    	if( valueEl.isJsonObject() ) { // Check if "value" contains a JsonObject
    		rootObjMember.add( 
    			valueFieldName.concat( Enricher.VALUE_NAME_OBJ_SUFFIX ) , 
    			valueEl.getAsJsonObject() );
			rootObjMember.remove( valueFieldName );
		} else {
			if( valueEl.isJsonArray() ) { // Check if "value" contains a JsonArray
				Iterator<JsonElement> it = valueEl.getAsJsonArray().iterator();
				
				/*
				boolean primitiveTypesArray = true;
				while( it.hasNext() ) {
					if( !it.next().isJsonPrimitive() ) { // use first array value to infer the array type
						primitiveTypesArray = false;
						break;
					}
				}
				if( primitiveTypesArray ) { // Array of primitve types
					rootObjMember.add( 
						valueFieldName.concat( Enricher.VALUE_NAME_ARR_SUFFIX ) , 
						valueEl.getAsJsonArray() );
				} else { // Array of JsonObjects
					JsonArray valueArrObj = new JsonArray();
					valueEl.getAsJsonArray().forEach( ( JsonElement e )-> {
						valueArrObj.add( e.getAsJsonObject() );
					});
					rootObjMember.add( 
						valueFieldName.concat( Enricher.VALUE_NAME_ARR_OBJ_SUFFIX ) , 
						valueArrObj );
				}
				rootObjMember.remove( valueFieldName );
				*/
				
				/**
				 * FIX: map arrays of numbes, strings booleans and objects using the 
				 * the same name of the primitive type.
				 * Array of objects and mixed arrays are 
				 * encoded to a single string value.
				 */
				if( it.hasNext() ) {
					JsonElement arrEl = it.next();
					JsonElement lastValidEl = null;
					Set<Class<?>> foundClasses = new HashSet<>();
					Class<?> elClass = getSpecializedClass(arrEl);
					if( !elClass.equals(JsonNull.class) ) {
						foundClasses.add( elClass );
						lastValidEl = arrEl;
					}
					while( it.hasNext() ) {
						arrEl = it.next();
						elClass = getSpecializedClass(arrEl);
						if( !elClass.equals(JsonNull.class) ) {
							foundClasses.add(elClass);
							lastValidEl = arrEl;
						}
					}
					String valueArrName = valueFieldName;
					JsonArray valueArr;
//					System.out.println( foundClasses.toString() );
					if( foundClasses.size() == 0 ) { // array of null values
						valueArr = valueEl.getAsJsonArray();
						rootObjMember.remove( valueFieldName );
						rootObjMember.add( valueArrName , valueArr );
					} else if( foundClasses.size() == 1 ) { // all array elements of the same type
						if( lastValidEl.isJsonPrimitive() ) { // array of primitives
							if( foundClasses.contains(String.class) ) { 		// strings
								valueArrName = valueArrName.concat( Enricher.VALUE_NAME_STR_SUFFIX );
								valueArr = valueEl.getAsJsonArray();
							} else if( foundClasses.contains(Number.class) ) {  // numbers
								valueArr = valueEl.getAsJsonArray();
							} else if( foundClasses.contains(Boolean.class) ) { // booleans
								valueArrName = valueArrName.concat( Enricher.VALUE_NAME_BOOL_SUFFIX );
								valueArr = valueEl.getAsJsonArray();
							} else {
								valueArr = null;
							}
							if( valueArr != null ) {
								rootObjMember.remove( valueFieldName );
								rootObjMember.add( valueArrName , valueArr );
							}
						} else { // array of objects
							valueArrName = valueArrName.concat( Enricher.VALUE_NAME_JSON_STR_SUFFIX );
							rootObjMember.remove( valueFieldName );
							rootObjMember.addProperty( valueArrName , valueEl.toString() );
						}
					} else { // mixed type array
						valueArrName = valueArrName.concat( Enricher.VALUE_NAME_JSON_STR_SUFFIX );
						rootObjMember.remove( valueFieldName );
						rootObjMember.addProperty( valueArrName , valueEl.toString() );
					}
				}
				// ---
			}
			
		}
    }
    
}
