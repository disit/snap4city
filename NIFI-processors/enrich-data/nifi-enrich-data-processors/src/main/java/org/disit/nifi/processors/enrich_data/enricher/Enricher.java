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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class Enricher {
	
	protected String valueFieldName;
	protected String timestampFieldName;
	
	protected Map< String , List<String>> additionalFieldPaths;
	protected List<String> fieldsToPurge; 
	protected String deviceIdNameMapping;
	protected boolean isLeftJoin;
	protected Map<String , String> additionalStaticProperties;
	
	// Tracking
	protected String lastTimestampSource;
	
	public Enricher( String valueFieldName , String timestampFieldName , String deviceIdNameMapping , Map<String , List<String> > additionalFieldPaths , List<String> fieldsToPurge ) {
		this.valueFieldName = valueFieldName;
		this.timestampFieldName = timestampFieldName;
		
		this.deviceIdNameMapping = deviceIdNameMapping;
		
		this.additionalFieldPaths = additionalFieldPaths;
		this.fieldsToPurge = fieldsToPurge;
		
		this.isLeftJoin = false;
		
		this.additionalStaticProperties = new ConcurrentHashMap<>();
		
		this.lastTimestampSource = "";
	}
	
	public abstract Map<String, String> enrich( String deviceId , JsonObject rootObject , JsonObject enrichmentObject , JsonElement responseRootEl , String timestamp , 
												Map<String , JsonElement> staticProperties );

	public void setLeftJoin( boolean isLeftJoin ) {
		this.isLeftJoin = isLeftJoin;
	}
	
	public void putStaticProperty( String name , String value ) {
		this.additionalStaticProperties.put( name , value );
	}
	
	public void removeStaticProperty( String name ) {
		this.additionalStaticProperties.remove( name );
	}
	
	public Map<String , String> getStaticProperties(){
		return Collections.unmodifiableMap( additionalStaticProperties  );
	}
	
	public String getLastTimestampSource() {
		return this.lastTimestampSource;
	}
	
	public void throwEmptyObjectException()  {
		
	}
}
