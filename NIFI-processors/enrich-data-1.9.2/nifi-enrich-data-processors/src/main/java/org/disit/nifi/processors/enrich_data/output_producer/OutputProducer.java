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
import java.util.List;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;

import com.google.gson.JsonObject;

/** 
 *	This is the interface to implement to create output producers class.
 * 	For every output format, an OutputProducer subclass must be created. 
 *	
 */
public interface OutputProducer{
	
	/**
	 * Implement this method to realize an output format for the processor.
	 * 
	 * @param rootObj the JsonObject from which output flow files are created.
	 * @return a java.util.Set of output flow files.
	 */
	public List<FlowFile> produceOutput( JsonObject rootObj , FlowFile inFlowFile , final ProcessSession session );
	
}