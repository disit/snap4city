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

package org.disit.nifi.processors.enrich_data.enricher.converter;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.nifi.logging.ComponentLog;
import org.disit.nifi.processors.enrich_data.enricher.EnrichUtils;
import org.disit.nifi.processors.enrich_data.enricher.Enricher;
import org.disit.nifi.processors.enrich_data.json_processing.JsonProcessingUtils;
import org.disit.nifi.processors.enrich_data.logging.LoggingUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


/**
 * This converter class is designed to convert the output to a 
 * format suitable to represent the device state, with all measures
 * in a single json object.
 * 
 * This class is intended to operate on the output produced from an
 * Enricher implementation.
 */
public class DeviceStateConverter {
	
	public enum OutputMode {
		MINIMAL ,
		FULL
	}
	
	OutputMode outputMode;
	private Set<String> nonMeasureProperties;
	private Set<String> valueFieldNames;
	private Set<String> ignoredDeviceLvlProperties;
	
	private String timestampFieldName;
	private String updateFrequencyFieldName;
	private final String NEXT_UPDATE_FIELD_PREFIX = "expected_next_";
	
	ComponentLog logger;
	
	/**
	 * 
	 * @param enricher a reference to the Enricher which produces the output on which this class operates.
	 */
	public DeviceStateConverter( Enricher enricher , OutputMode outputMode , ComponentLog logger ) {
		this.outputMode = outputMode;
		this.logger = logger;
		this.timestampFieldName = enricher.getTimestampFieldName();
		
		nonMeasureProperties = new HashSet<>();
		if( !enricher.getDeviceIdNameMapping().isEmpty() )
			nonMeasureProperties.add( enricher.getDeviceIdNameMapping() );
		else
			nonMeasureProperties.add( Enricher.DEFAULT_DEVICE_ID_PROPERTY_NAME );
		
		nonMeasureProperties.add( enricher.getTimestampFieldName() );
		nonMeasureProperties.addAll( EnrichUtils.getTruncatedDateTimeFieldNames( enricher.getTimestampFieldName() ) );
		nonMeasureProperties.addAll( enricher.getAdditionalFieldPaths().keySet() );
		nonMeasureProperties.addAll( enricher.getStaticProperties().keySet() );
		
		valueFieldNames = enricher.getAllValueFieldNames();
		
		ignoredDeviceLvlProperties = new HashSet<>();
		if( outputMode == OutputMode.MINIMAL ) {
			ignoredDeviceLvlProperties.addAll( 
				EnrichUtils.getTruncatedDateTimeFieldNames( enricher.getTimestampFieldName() ) 
			);
		}
		
		this.updateFrequencyFieldName = null;
	}
	
	public DeviceStateConverter( Enricher enricher , OutputMode outputMode , ComponentLog logger , String updateFrequencyFieldName ) {
		this( enricher , outputMode , logger );
		this.updateFrequencyFieldName = updateFrequencyFieldName;
	}
	
	/**
	 * Convert the srcObj.
	 * 
	 * @param srcObj the object to convert.
	 * @param additionalProperties the additional properties used to enrich the initial object.
	 * @return a new JsonObject representing the srcObj in the "Device state" format.
	 */
	public JsonElement convert( JsonObject srcObj , Map<String , JsonElement> additionalProperties ,
								JsonElement responseRootEl ) {
		
		Set<String> toUpperLvlProperties = new HashSet<String>( nonMeasureProperties );
		toUpperLvlProperties.addAll( additionalProperties.keySet() );		
//		toUpperLvlProperties.stream().forEach( (p) -> { System.out.println( p ); });
		
		// Map device-level properties
		Map<String , JsonElement> deviceLvlProperties = new HashMap<>();
		srcObj.get( srcObj.keySet().iterator().next() )
				 .getAsJsonObject().entrySet().stream().forEach(
			(Map.Entry<String, JsonElement> entry) -> {
				if( toUpperLvlProperties.contains( entry.getKey() ) ) {
					deviceLvlProperties.put( entry.getKey() , entry.getValue() );
				}
			} 
		);
		
		JsonObject outputObj = new JsonObject();
		srcObj.keySet().forEach( (String measure) -> {
			JsonObject measureObj = new JsonObject();
			srcObj.get( measure ).getAsJsonObject().entrySet().forEach(
				(Map.Entry<String , JsonElement> entry) -> {
					
					if( outputMode == OutputMode.FULL && !toUpperLvlProperties.contains( entry.getKey() ) ) 
						measureObj.add( entry.getKey() , entry.getValue() );
					
					if( outputMode == OutputMode.MINIMAL && !toUpperLvlProperties.contains( entry.getKey() ) 
														 && valueFieldNames.contains( entry.getKey() ) ) 
						measureObj.add( entry.getKey() , entry.getValue() );
				}
			);
			outputObj.add( measure , measureObj );
		});
		
		deviceLvlProperties.forEach( (String pName, JsonElement pValue) -> {
			if( !ignoredDeviceLvlProperties.contains( pName ) )
				outputObj.add( pName , pValue );
		});
		
		// Expected next update
		if( this.updateFrequencyFieldName != null && !this.updateFrequencyFieldName.isEmpty() ) {
			this.addNextUpdate(srcObj, responseRootEl, outputObj);
		}
		
		return outputObj;
	}
	
	private void addNextUpdate( JsonObject srcObj , JsonElement responseRootEl , JsonObject outputObj ) {
		JsonObject responseRootObj;
		if( responseRootEl.isJsonObject() )
			responseRootObj = responseRootEl.getAsJsonObject();
		else {
			LoggingUtils.produceErrorObj( "Cannot calculate the next update time: the enrichment response ins not a JSON object. Skipping.")
						.logAsWarning(logger);
			return;
		}
			
		try {
//			JsonElement currentTimeEl = JsonProcessingUtils.getElementByPath(srcObj , timestampFieldName);
			JsonElement currentTimeEl = JsonProcessingUtils.getElementByPath(outputObj , this.timestampFieldName);
			JsonElement updateFrequency = JsonProcessingUtils.getElementByPath(responseRootObj, this.updateFrequencyFieldName);
			if( currentTimeEl.isJsonPrimitive() && currentTimeEl.getAsJsonPrimitive().isString() ) {
				OffsetDateTime currentTime = OffsetDateTime.parse( currentTimeEl.getAsJsonPrimitive().getAsString() );
				
				if( updateFrequency.isJsonPrimitive() ) {
					JsonPrimitive freqPrimitive = updateFrequency.getAsJsonPrimitive();
					long freq;
					if( freqPrimitive.isNumber() ) {
						freq = updateFrequency.getAsJsonPrimitive().getAsLong();
					} else if( freqPrimitive.isString() ) {
						freq = (long)Float.parseFloat( freqPrimitive.getAsString() );
					} else {
						LoggingUtils.produceErrorObj( String.format( "Cannot calculate the next update time: the '%s' field in the enrichment response must be a number or a string parsable as a number. Skipping." ,
														this.updateFrequencyFieldName) )
									.logAsWarning( logger );
						return;
					}
					
					if( freq > 0 ) {
						outputObj.addProperty(
							NEXT_UPDATE_FIELD_PREFIX + timestampFieldName ,
							currentTime.plusSeconds( freq ).toString()
						);
					}else{
						// Negative update frequency
						LoggingUtils.produceErrorObj( "Cannot calculate the next update time: the parsed update frequency is negative. Skipping." )
									.logAsWarning( logger );
					}
				}else {
					LoggingUtils.produceErrorObj( String.format( "Cannot calculate the next update time: the '%s' field in the enrichment response is not a json primitive type. Skipping." ,
													this.updateFrequencyFieldName) )
								.logAsWarning( logger );
				}
			}else {
				// CurrentTimeEl is not a string
				LoggingUtils.produceErrorObj( String.format("Cannot calculate the next update time: the specified timestamp field '%s' is not a string. Skipping." , 
												this.timestampFieldName ) )
							.logAsWarning( logger );
			}
		}catch( DateTimeParseException | NoSuchElementException | NumberFormatException ex ) {
			// The timestamp cannot be parsed
			LoggingUtils.produceErrorObj( "Cannot calculate the next update time. Skipping." )
						.withExceptionInfo( ex )
						.logAsWarning( logger );
		}
	}

}
