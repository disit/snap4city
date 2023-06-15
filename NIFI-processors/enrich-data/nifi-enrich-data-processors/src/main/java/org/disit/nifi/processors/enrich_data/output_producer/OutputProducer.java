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
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.disit.nifi.processors.enrich_data.enricher.EnrichUtils;

import com.google.gson.JsonObject;

/** 
 *	This is the interface to implement to create output producers class.
 * 	For every output format, an OutputProducer subclass must be created. 
 *	
 */
//public interface OutputProducer{
public abstract class OutputProducer{
	
	public static final String TIME_SLACK_ATTRIBUTE_NAME = "time_slack";
	
	protected String timestampAttribute = null;
	protected long timestampThreshold = 0;
	
	/**
	 * Implement this method to realize an output format for the processor.
	 * 
	 * @param rootObj the JsonObject from which output flow files are created.
	 * @return a java.util.List of output flow files.
	 */
	public abstract List<FlowFile> produceOutput( JsonObject rootObj , FlowFile inFlowFile , final ProcessSession session );
	
	public void setTimestampAttribute( String attributeName ) {
		this.timestampAttribute = attributeName;
	}
	
	public void setTimestampThreshold( long timestampThreshold ) {
		this.timestampThreshold = timestampThreshold;
	}
	
	protected FlowFile putTimestampAttributes( FlowFile ff , ProcessSession session , JsonObject entryObj ) {
		Instant nowRef = Instant.now();
		return putTimestampAttributes( ff , session , entryObj , nowRef );
	}
	
	protected FlowFile putTimestampAttributes( FlowFile ff , ProcessSession session , JsonObject entryObj , Instant nowRef ) {
		if( this.timestampAttribute != null ) {
			if( entryObj.has( this.timestampAttribute ) ) {
				String timestampAttrVal = entryObj.get( this.timestampAttribute ).getAsString();
				try {
					OffsetDateTime ot = OffsetDateTime.parse( timestampAttrVal );
					timestampAttrVal = EnrichUtils.toFullISO8601Format( ot );
					if( this.timestampThreshold > 0 ) {
						long timeSlack = ot.toInstant().toEpochMilli() - ( nowRef.toEpochMilli() - this.timestampThreshold);
						ff = session.putAttribute( ff , TIME_SLACK_ATTRIBUTE_NAME , String.valueOf( timeSlack ) );
					}
				}catch( DateTimeException ex ) { /* leave as it it*/ }
				ff = session.putAttribute( ff , this.timestampAttribute , timestampAttrVal );
			}
		}
		return ff;
	}
	
}