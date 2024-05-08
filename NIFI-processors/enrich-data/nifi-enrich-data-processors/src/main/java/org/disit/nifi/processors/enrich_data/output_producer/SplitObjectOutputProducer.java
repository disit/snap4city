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


package org.disit.nifi.processors.enrich_data.output_producer;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.disit.nifi.processors.enrich_data.EnrichDataConstants;
import org.disit.nifi.processors.enrich_data.json_processing.JsonProcessing;
import org.disit.nifi.processors.enrich_data.logging.LoggingUtils;

import com.google.common.net.MediaType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
/**
 * This OutputProducer implementation produces 
 * a flow file per member. 
 *
 */
public class SplitObjectOutputProducer extends OutputProducer {

	private List<String> hashedIdFields;
	private List<String> extractEnrichmentAttributesList;
	
	public SplitObjectOutputProducer( ComponentLog logger ) {
		super( logger );
		this.hashedIdFields = new ArrayList<>();
		this.extractEnrichmentAttributesList = new ArrayList<>();
	}
	
	public void setHashedIdFields( List<String> hashedIdFields ) {
		if( hashedIdFields != null && !hashedIdFields.isEmpty() ) {
			this.hashedIdFields.clear();
			this.hashedIdFields.addAll( hashedIdFields );
		}
	}
	
	public void setExtractEnrichmentAttributesList( List<String> extractEnrichmentAttributesList ) {
		if( extractEnrichmentAttributesList != null && !extractEnrichmentAttributesList.isEmpty() ) {
			this.extractEnrichmentAttributesList.clear();
			this.extractEnrichmentAttributesList.addAll( extractEnrichmentAttributesList );
		}
	}
	
	@Override
	public List<FlowFile> produceOutput(JsonObject rootObj , JsonObject enrichmentObj , FlowFile inFlowFile , final ProcessSession session ) {
		Map<String,Map<String,String>> extractedAttributes = extractEnrichmentAttributes(enrichmentObj);
		
		return produceOutput( rootObj , inFlowFile , session , 
			(FlowFile ff, String valueName) -> {
				FlowFile nff = ff;
				if( extractedAttributes.containsKey(valueName) && !extractedAttributes.get(valueName).isEmpty() ) {
					nff = session.putAllAttributes( ff , extractedAttributes.get(valueName) );
				}
				return nff;
			}
		);
	}
	
	@Override
	public List<FlowFile> produceOutput(JsonObject rootObj, FlowFile inFlowFile, final ProcessSession session) {
		return produceOutput( rootObj , inFlowFile , session , null );
	}
	
	private Map<String,Map<String,String>> extractEnrichmentAttributes( JsonObject enrichmentObj ){
		// Extract enrichment attributes
		Map<String,Map<String,String>> extractedAttributes = new HashMap<>();
		if( !this.extractEnrichmentAttributesList.isEmpty() ) {
			enrichmentObj.keySet().stream().forEach( (String enrichmentPropName) -> {
				JsonElement enrichmentPropEl = enrichmentObj.get( enrichmentPropName );
				if( enrichmentPropEl.isJsonObject() ) {
					Map<String,String> enrichmentPropExtractedAttributes = new HashMap<>();
					for( String extractAttributePath : this.extractEnrichmentAttributesList ) {
						try {
							JsonElement extractAttributeEl = JsonProcessing.getElementByPath(enrichmentPropEl, extractAttributePath);
							if( extractAttributeEl.isJsonPrimitive() )
								enrichmentPropExtractedAttributes.put( 
									extractAttributePath.replace( "/" , "." ) , 
									extractAttributeEl.getAsString() 
								);
						}catch( NoSuchElementException ex ) {
							LoggingUtils.produceErrorObj( "Cannot find '" + enrichmentPropName + "/" + extractAttributePath + "' in the Enrichment respose base path object. Skipping attribute extraction for this path." )
								.withExceptionInfo(ex)
								.logAsWarning( logger );
						}
					}
					if( !enrichmentPropExtractedAttributes.isEmpty() )
						extractedAttributes.put( enrichmentPropName , enrichmentPropExtractedAttributes );
				}
			});
		}
		return extractedAttributes;
	}
	
	private List<FlowFile> produceOutput( JsonObject rootObj , FlowFile inFlowFile , final ProcessSession session , 
										  BiFunction<FlowFile, String, FlowFile> ffOp ){
		// Produce output flow files
		List<FlowFile> outputList = new ArrayList<>();
		Instant nowRef = Instant.now();
		
		rootObj.entrySet().stream().forEach( (Map.Entry<String , JsonElement> rootEntry) -> { 
			
			JsonObject entryObj = rootEntry.getValue().getAsJsonObject();
			
			FlowFile ff = session.create( inFlowFile );
			ff = session.write( ff , new OutputStreamCallback() {
				
				@Override
				public void process(OutputStream out) throws IOException {
					out.write( entryObj.toString().getBytes() );
				}
			});
			
			if( !this.hashedIdFields.isEmpty() ) {
				String id = generateHexHashedId( entryObj );
				ff = session.putAttribute( ff , "_id" , id );
			}
			
			ff = session.putAttribute( ff , EnrichDataConstants.MIME_TYPE_ATTRIBUTE_NAME , MediaType.JSON_UTF_8.toString() );
			// value name and timestamp for each produced flow file as attributes
			String valueName = rootEntry.getKey();
			ff = session.putAttribute( ff , EnrichDataConstants.VALUE_NAME_ATTRIBUTE_NAME , valueName );
			ff = putTimestampAttributes( ff , session , entryObj , nowRef );
				
			// Perform ff op
			if( ffOp != null ) {
				ff = ffOp.apply(ff,valueName);
			}
			
			outputList.add( ff );
		});
		
		session.remove( inFlowFile );
		return outputList;
	}
	
	private String generateHexHashedId( JsonObject entryObj ) {
		String toHash = this.hashedIdFields.stream()
				   .map( (String field) -> { 
					   return ( entryObj.has(field) ? entryObj.get( field ).getAsString() : "" ); 
				   })
				   .reduce( (String s1, String s2) -> {return s1+s2;} ).get();
		return ( !toHash.isEmpty() ? DigestUtils.sha256Hex( toHash ) : "" );
	}

}
