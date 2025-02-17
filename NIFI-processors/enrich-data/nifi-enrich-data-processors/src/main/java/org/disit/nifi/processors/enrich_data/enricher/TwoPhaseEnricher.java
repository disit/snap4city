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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TwoPhaseEnricher extends Enricher {
	
	private String timestampFromContentPropertyName;
	private String timestampFromContentPropertyValue; 
	private boolean useTimestampFallback;
	private boolean attemptNumericStringParsing;
	
	public TwoPhaseEnricher( String valueFieldName , String timestampFieldName , String deviceIdNameMapping , Map<String , List<String>> additionalFieldPaths , List<String> fieldsToPurge , 
							 String timestampFromContentPropertyName , String timestampFromContentPropertyValue , 
							 boolean attemptNumericStringParsing ) {
		super( valueFieldName , timestampFieldName , deviceIdNameMapping , additionalFieldPaths , fieldsToPurge );
		this.timestampFieldName = timestampFieldName;
		
		this.timestampFromContentPropertyName = timestampFromContentPropertyName;
		this.timestampFromContentPropertyValue = timestampFromContentPropertyValue;
		this.useTimestampFallback = true;
		this.attemptNumericStringParsing = attemptNumericStringParsing;
	}
	
	public void setTimestampFromContent( String timestampFromContentPropertyName , String timestampFromContentPropertyValue ) {
		this.timestampFromContentPropertyName = timestampFromContentPropertyName;
		this.timestampFromContentPropertyValue = timestampFromContentPropertyValue; 
	}
	
	public void setUseTimestampFallback( boolean use ) {
		this.useTimestampFallback = use;
	}
	
	public boolean getUseTimestampFallback() {
		return this.useTimestampFallback;
	}
	
	/**
	 * In this enricher implementation, the timestamp is the timestamp to use
	 * if all candidates cannot be parsed and the useTimestampFallback is set to true.
	 */
	@Override
	public Map<String , String> enrich( String deviceId , JsonObject rootObject, JsonObject enrichmentObj , JsonElement responseRootEl , String timestamp , 
										Map<String, JsonElement> additionalProperties ) {
		// Enrich with data from servicemap
		Set<String> rootKeySet = rootObject.entrySet().stream()
									   .map( ( Map.Entry<String , JsonElement> e ) -> { 
										   return e.getKey(); 
									   } ).collect( Collectors.toSet() );
		
		List<String> timestampFromContentMembers = new ArrayList<>();
		Map<String , String> additionalFieldErrors = new TreeMap<>();
		
		rootKeySet.stream().forEach( (String member) -> { 
			
			if( rootObject.get( member ).isJsonObject() ) { //Ensures all the kept members are JsonObjects
				JsonObject rootObjMember = rootObject.getAsJsonObject( member );
				if( enrichmentObj.has( member ) ) {	// Check if the member is present in the enrichment response base object
													// if it's present in such object do the enrichment operations
					JsonObject enrichmentMemberObj = enrichmentObj.getAsJsonObject( member );
					
					Set<String> enrichMemberKeySet = enrichmentMemberObj.entrySet().stream()
																	    .map( (Map.Entry<String , JsonElement> e) -> { 
																	    	return e.getKey();
																	    }).collect( Collectors.toSet() );
					
					enrichMemberKeySet.stream().forEach( (String emom) -> { 
						
						if( !rootObjMember.has( emom ) ) { // Avoid overwriting existing object members
							rootObjMember.addProperty( emom , enrichmentMemberObj.get(emom).getAsString() );
						}
						
					});
					 
				} else { //Otherwise remove if not left join. In case of left join keep member not eneriched.
					
					if( !this.isLeftJoin ) {
						rootObject.remove( member );
					}
					
				}
				
				// Remove fields to purge
				this.fieldsToPurge.forEach( (String fieldName) -> { 
					if( rootObjMember.has( fieldName ) )
						rootObjMember.remove( fieldName );
				});
				
				// Check if the current member is valid for the timestamp from content, 
				// if none has been set and if enabled.
				// If it's valid add it to a List to track.
				if( rootObjMember.has( this.timestampFromContentPropertyName ) ) {
					String propertyValue = rootObjMember.get( this.timestampFromContentPropertyName ).getAsString();
					if( propertyValue.equals( this.timestampFromContentPropertyValue ) )
						timestampFromContentMembers.add( member );
				}
				
				// Perform value mapping
				EnrichUtils.mapObjectValue( rootObjMember , this.valueFieldName , this.attemptNumericStringParsing );

				// Device Id
				if( !this.deviceIdNameMapping.isEmpty() ) {
					rootObjMember.addProperty( this.deviceIdNameMapping , deviceId );
				} else {
					rootObjMember.addProperty( Enricher.DEFAULT_DEVICE_ID_PROPERTY_NAME , deviceId );
				}
				
				// Static properties
				this.additionalStaticProperties.entrySet().stream().forEach( 
					(Map.Entry<String , String> property) -> {
						
						rootObjMember.addProperty( property.getKey() , property.getValue() );
						
					}
				);
				
				// Additional properties (only for this enriched object)
				additionalProperties.entrySet().stream().forEach( 
					(Map.Entry<String , JsonElement> property) -> {
						rootObjMember.add( property.getKey() , property.getValue() );
					}
				);
				
				// Value name
				rootObjMember.addProperty( Enricher.DEFAULT_VALUE_NAME_PROPERTY_NAME , member );
				
				//Add additional fields to the rootObjMember
				this.additionalFieldPaths.forEach( (String name , List<String> path) -> { 
					try {
						rootObjMember.addProperty( name , EnrichUtils.getAdditionalFieldValue( path , responseRootEl ) );
					} catch (NoSuchElementException | IllegalArgumentException ex) {
						// Currently if there are errors in additional fields, the field will 
						// be ignored and an error message is put as a flow file attribute 
						// named as the erroneous field name.
						additionalFieldErrors.put( name , ex.getMessage() ); //Track errors 
					}
				});
			
			} else { //JsonPrimitive or array
				//Discard property from the rootObject
				rootObject.remove( member );
			}
		});
		
		// If the timestamp must be picked from content and a valid candidate 
		// has been found, updates the timestamp variable to put in every object of 
		// the enriched object.
		 
		Map.Entry<String, String> timestampFromContentResult = EnrichUtils.getTimestampFromContentField( rootObject , this.valueFieldName , timestampFromContentMembers );
		if( !timestampFromContentResult.getKey().isEmpty() ) {
			timestamp = timestampFromContentResult.getValue();
			this.lastTimestampSource = timestampFromContentResult.getKey();
		} else {
			if( this.useTimestampFallback )
				this.lastTimestampSource = "Input value: cannot parse any candidate. Fallback applied.";
			else
				this.lastTimestampSource = "None. Fallback not applied.";
		}
			
		// Enrich with the determined timestamp if any
		if( !timestamp.isEmpty() )
			EnrichUtils.enrichWithDateTime( timestampFieldName , rootObject , timestamp );
		
		return additionalFieldErrors;
	}
}
