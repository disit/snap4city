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

package org.disit.nifi.processors.enrich_data.ownership;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OwnershipClientConfig {
	
	protected String ownershipApiUrl;
	protected String elementIdParamName;
	protected String elementIdValuePrefix;
	protected final Map<String , String> additionalQueryParameters;
	protected final Set<String> ownershipFields;
	protected final Map<String , String> fieldsMapping;
	
	public OwnershipClientConfig( String ownershipApiUrl , String elementIdParamName ) {
		this.ownershipApiUrl = ownershipApiUrl;
		this.elementIdParamName = elementIdParamName;
		this.elementIdValuePrefix = "";
		this.additionalQueryParameters = new HashMap<>();
		this.ownershipFields = new HashSet<>();
		this.fieldsMapping = new HashMap<>();
	}
	
	public String getOwnershipApiUrl() {
		return this.ownershipApiUrl;
	}
	
	public String getElementIdPrefix() {
		return this.elementIdValuePrefix;
	}
	
	public String getElementIdParamName() {
		return this.elementIdParamName;
	}
	
	public void setElementIdPrefix( String prefix ) {
		this.elementIdValuePrefix = prefix;
	}
	
	public Map<String,String> getAdditionalQueryParameters(){
		return Collections.unmodifiableMap( this.additionalQueryParameters );
	}
	
	
	public Set<String> getOwnershipFields(){
		return Collections.unmodifiableSet( this.ownershipFields );
	}
	
	public void addQueryParameter( String name, String value) {
		additionalQueryParameters.put( name , value );
	}
	
	public void addOwnershipField( String name ) {
		ownershipFields.add( name );
	}
	
	public void addFieldMapping( String fieldName , String newName ) {
		fieldsMapping.put( fieldName , newName );
	}
	
}
