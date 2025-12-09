/** 
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

package org.disit.nifi.processors.enrich_data.locators;

import java.util.Map;
import java.util.TreeMap;

public class ResourceLocations {

	public Map<Service , String> locations;
	
	public enum Service{
		SERVICEMAP , 
		OWNERSHIP
	}
	
	public ResourceLocations() {
		locations = new TreeMap<>();
	}
	
	public void putLocation( Service service , String location ) {
		locations.put( service , location );
	}
	
	public String getLocationForService( Service service ) {
		if( !locations.containsKey( service ) )
			return null;
		return locations.get( service );
	}
	
	public boolean hasLocationForService( Service service ) {
		return locations.containsKey( service );
	}
	
	public String toString() {
		return locations.toString();
	}
	
}
