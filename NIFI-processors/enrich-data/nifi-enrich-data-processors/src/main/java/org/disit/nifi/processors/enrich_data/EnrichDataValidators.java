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

package org.disit.nifi.processors.enrich_data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClientService;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public final class EnrichDataValidators {

	
	private  EnrichDataValidators() {}
	
	public static Validator enrichmentSourceServiceValidator() {
		return new Validator() {
			@Override
			public ValidationResult validate(String subject, String input, ValidationContext context) {
				try {
					context.getProperty( EnrichData.ENRICHMENT_SOURCE_CLIENT_SERVICE )
						   .asControllerService( EnrichmentSourceClientService.class );
					
					return new ValidationResult.Builder()
							   .subject(subject)
							   .explanation( "Valid EnrichmentSourceClientService" )
							   .valid( true )
							   .build();
				}catch( IllegalArgumentException ex ) {
					return new ValidationResult.Builder()
											   .subject(subject)
											   .explanation( "Invalid EnrichmentSourceClientService" )
											   .valid( false )
											   .build();
				}
			}	
		};
	}
	
	public static Validator jsonPropertyValidator( boolean allowBlank ) {
		return new Validator() {
			JsonParser parser = new JsonParser();
			
			
			@Override
			public ValidationResult validate(String subject, String input, ValidationContext context) {
				if( allowBlank && ( input == null || input.isEmpty() ) )
					return new ValidationResult.Builder().subject(subject).input(input)
											   .explanation( "Empty string is allowed for this property." )
											   .valid(true).build();
				try{
					JsonElement propEl = parser.parse( input );
					if( !propEl.isJsonObject() ) {
						return new ValidationResult.Builder().subject(subject).input(input)
												   .explanation( "The json confgiuration must be a json object." )
												   .valid( false ).build();
					}
					
					if( propEl.getAsJsonObject().entrySet().isEmpty() ) {
						return new ValidationResult.Builder().subject(subject).input(input)
								   .explanation( "The json object confguration cannot be empty." )
								   .valid( false ).build();
					}
					
				} catch ( JsonSyntaxException e ) {
					return new ValidationResult.Builder().subject(subject).input(input)
									    	   .explanation( "Error in json syntax. " + e.getMessage() )
									    	   .valid( false ).build();
				}
				
				return new ValidationResult.Builder().subject(subject).input(input)
				    	   .explanation( "Valid json configuration.").valid( true ).build();
			}
		};
	}
	
	public static Validator innerLatLonPropertyValidator() {
		 return new Validator() {
			JsonParser parser = new JsonParser();
			 
			@Override
			public ValidationResult validate(String subject, String input, ValidationContext context) {
				if( input.isEmpty() )
					return new ValidationResult.Builder().subject(subject).input(input)
											   .explanation( "Empty coordinates from flow file content configuration" )
											   .valid(true).build();
				
				JsonElement propsEl;
				try {
					propsEl = parser.parse( input );
				}catch( JsonSyntaxException e ) {
					return new ValidationResult.Builder().subject(subject).input(input)
					    	   .explanation( "Error in json syntax. " + e.getMessage() )
					    	   .valid( false ).build();
				}
				
				if( !propsEl.isJsonObject() ) {
					return new ValidationResult.Builder().subject(subject).input(input)
							   .explanation( "The coordinates from flow file content confgiuration must be a json object." )
							   .valid( false ).build();
				}
				
				JsonObject propsObj = propsEl.getAsJsonObject();
				
				for( String prop : propsObj.keySet() ) {
					if( !EnrichData.INNER_LATLON_COMPOUND_FIELDS_CONFIGS.contains(prop) && 
						!EnrichData.INNER_LATLON_SINGLE_FIELDS_CONFIGS.contains(prop) ) {
							return new ValidationResult.Builder().subject(subject).input(input)
											   		   .explanation( "'" + prop + "' is not an allowed configuration for the coordinates from the flow file content." )
												       .valid( false ).build();
					}
					
					if( EnrichData.INNER_LATLON_COMPOUND_FIELDS_CONFIGS.contains(prop) ) {
						JsonElement pvEl = propsObj.get( prop );
						if( !pvEl.isJsonArray() ) {
							return new ValidationResult.Builder().subject(subject).input(input)
													   .explanation( "The value of the '" + prop + "' property for the coordinates from the flow file content must be a json array." )
													   .valid(false).build();
						}
						
						JsonArray pvArr = pvEl.getAsJsonArray();
						Iterator<JsonElement> pvIt = pvArr.iterator();
						while( pvIt.hasNext() ) {
							JsonElement e = pvIt.next();
							if( !e.isJsonObject() )
								return new ValidationResult.Builder().subject(subject).input(input)
														   .explanation( "Every element of the json array for a compound field configuration must be a json object for the coordinates from the flow file content." )
														   .valid(false).build();
							JsonObject eObj = e.getAsJsonObject();
							for( String cfp : EnrichData.INNER_LATLON_COMPOUND_FIELD_PROPERTIES ) {
								if( !eObj.has( cfp ) )
									return new ValidationResult.Builder().subject(subject).input(input)
															   .explanation( "Missing '" + cfp +  "' for a field specification in '" + prop + "' while configuring coordinates from the flow file content." )
															   .valid(false).build();
								
								if( cfp.equals(EnrichData.INNER_LATLON_COMPOUND_FIELD_FORMAT) ) {
									List<String> parsedFormatContent = 
										Arrays.asList( eObj.get(cfp).getAsString().split(",") )
											  .stream().map( (String s) -> {return s.trim(); } )
											  .collect( Collectors.toList() );
									if( parsedFormatContent.size() != 2 || 
										!parsedFormatContent.contains("lat") ||
										!parsedFormatContent.contains("lon") ) {
											return new ValidationResult.Builder().subject(subject).input(input)
																	   .explanation( "The 'format' configuration for a field specification can only be \"lat,lon\" or \"lon,lat\"." )
																	   .valid(false).build();
									}
								}
							}
						}
					}
					
					if( EnrichData.INNER_LATLON_SINGLE_FIELDS_CONFIGS.contains(prop) ) {
						JsonElement pvEl = propsObj.get( prop );
						if( !pvEl.isJsonArray() ) {
							return new ValidationResult.Builder().subject(subject).input(input)
													   .explanation( "The value of the '" + prop + "' property for the coordinates from the flow file content must be a json array." )
													   .valid(false).build();
						}
						
						JsonArray pvArr = pvEl.getAsJsonArray();
						Iterator<JsonElement> it = pvArr.iterator();
						while( it.hasNext() ) {
							JsonElement sfp = it.next();
							if( !sfp.isJsonPrimitive() )
								return new ValidationResult.Builder().subject(subject).input(input)
														   .explanation( "The values contained in a single field specification for the coordinates from the flow file content must be json primitives." )
														   .valid(false).build();
						}
					}
				}
				
				int singleFieldsConfCount = 0;
				for( String prop : propsObj.keySet() ) {
					if( EnrichData.INNER_LATLON_SINGLE_FIELDS_CONFIGS.contains( prop ) )
						singleFieldsConfCount ++;
				}
				if( singleFieldsConfCount != 0 && singleFieldsConfCount != 2 ) 
					return new ValidationResult.Builder().subject(subject).input(input)
											   .explanation( "For the coordinates from the flow file content the configuration must specify both \"latitudeFields\" and \"longitudeFields\" or none of them, but only one is currently specified." )
											   .valid(false).build();
				
				return new ValidationResult.Builder().subject(subject).input(input)
										   .explanation( "Valid configuration for the coordinates from flow file content." )
										   .valid(true).build();
			}
		 };
	}
	
}
