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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.OutputStreamCallback;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This OutputProducer implementation produces flow files with content
 * compliant with an Elasticsearch bulk indexing ("_bulk" endpoint). 
 * 
 */
//public class ElasticsearchBulkIndexingOutputProducer implements OutputProducer {
public class ElasticsearchBulkIndexingOutputProducer extends OutputProducer {

	private String esIndex;
	private String esType;
	
	public ElasticsearchBulkIndexingOutputProducer( String esIndex , String esType , ComponentLog logger ) {
		super( logger );
		this.esIndex = esIndex;
		this.esType = esType;
	}
	
	@Override
	public List<FlowFile> produceOutput(JsonObject rootObj, JsonObject enrichmentObj, FlowFile inFlowFile, final ProcessSession session){
		// ignores enrichmentObj
		return produceOutput( rootObj , inFlowFile , session );
	}
	
	@Override
	public List<FlowFile> produceOutput(JsonObject rootObj, FlowFile inFlowFile, final ProcessSession session) {
		
		StringBuilder esBulkStringBuilder = new StringBuilder( "" );
		rootObj.entrySet().stream().forEach( (Map.Entry<String , JsonElement> rootEntry) -> {
			
			esBulkStringBuilder.append( "{\"index\":{\"_index\":\"")
			   .append( this.esIndex )
			   .append( "\",\"_type\":\"" )
			   .append( this.esType )
			   .append( "\"}}\n" ) // ES NEEDS LINE BREAKS !!!!
			   .append( rootEntry.getValue().getAsJsonObject().toString() )
			   .append( "\n" ); // ES NEEDS LINE BREAKS !!!!
		});
		
		session.write( inFlowFile , new OutputStreamCallback() {
			
			@Override
			public void process(OutputStream out) throws IOException {
				out.write( esBulkStringBuilder.toString().getBytes() );
			}
		});
		
		List<FlowFile> outputList = new ArrayList<>();
		outputList.add( inFlowFile );
		
		return outputList;
	}

}
