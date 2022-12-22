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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.disit.nifi.processors.enrich_data.enricher.EnrichUtils;

import com.google.common.net.MediaType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The simplest OutputProducer implementation.
 * Takes the rootObject and produces an output flow file 
 * containing the rootObject unmodified. 
 */
public class JsonOutputProducer implements OutputProducer {

	private String timestampAttribute = null;
	
	public void setTimestampAttribute( String timestampAttribute ) {
		this.timestampAttribute = timestampAttribute;
	}
	
	@Override
	public List<FlowFile> produceOutput(JsonObject rootObj , FlowFile inFlowFile , final ProcessSession session ) {
		 List<FlowFile> outputList = new ArrayList<>();
		 
		 byte[] outputFlowFileBytes = rootObj.toString().getBytes();
		 
		 session.write( inFlowFile , new OutputStreamCallback() {
			
			@Override
			public void process(OutputStream out) throws IOException {
				out.write( outputFlowFileBytes );
			}
		});
		 
		inFlowFile = session.putAttribute( inFlowFile , "mime.type" , MediaType.JSON_UTF_8.toString() );
		if( timestampAttribute != null ) { // put timestamp of the first measure in the root object as attribute
			JsonElement firstElement = rootObj.entrySet().stream().findFirst().get().getValue();
			if( firstElement.isJsonObject() && firstElement.getAsJsonObject().has(timestampAttribute) ) {
				String timestampAttrVal = firstElement.getAsJsonObject().get(timestampAttribute).getAsString();
				try {
					timestampAttrVal = EnrichUtils.toFullISO8601Format(timestampAttrVal);
				}catch( DateTimeParseException ex ) { /* if cannot parse, leave as it is */ }
				inFlowFile = session.putAttribute( inFlowFile , timestampAttribute , timestampAttrVal );
			}
		}
		
		outputList.add( inFlowFile );
		return outputList;
 	}

}
