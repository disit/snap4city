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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.disit.nifi.processors.enrich_data.EnrichDataConstants;
import org.disit.nifi.processors.enrich_data.enricher.EnrichUtils;

import com.google.common.net.MediaType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
/**
 * This OutputProducer implementation produces 
 * a flow file per member. 
 *
 */
//public class SplitObjectOutputProducer implements OutputProducer {
public class SplitObjectOutputProducer extends OutputProducer {

	private List<String> hashedIdFields;
//	private String timestampAttribute = null;
	
	public SplitObjectOutputProducer() {
		this.hashedIdFields = new ArrayList<>();
	}
	
	public SplitObjectOutputProducer( List<String> hashedIdFields ) {
		this.hashedIdFields = hashedIdFields;
	}
	
//	public void setTimestampAttribute( String timestampAttribute ) {
//		this.timestampAttribute = timestampAttribute;
//	}
	
	@Override
	public List<FlowFile> produceOutput(JsonObject rootObj, FlowFile inFlowFile, final ProcessSession session) {		
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
			ff = session.putAttribute( ff , EnrichDataConstants.VALUE_NAME_ATTRIBUTE_NAME , rootEntry.getKey() );
//			if( this.timestampAttribute != null ) { 
//				if( entryObj.has(timestampAttribute) ) {
//					String timestampAttrVal = entryObj.get(timestampAttribute).getAsString();
//					try { 
//						timestampAttrVal = EnrichUtils.toFullISO8601Format( timestampAttrVal );
//					}catch( DateTimeParseException ex ) { /*if cannot parse leave as it is*/ }
//					ff = session.putAttribute( ff , timestampAttribute , timestampAttrVal );
//				}
//			}
			ff = putTimestampAttributes( ff , session , entryObj , nowRef );
				
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
