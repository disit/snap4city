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

package org.disit.nifi.processors.enrich_data.locators.iotdirectory;

import java.util.Arrays;
import java.util.List;

public class IOTDirectoryResourceLocatorConfig {

	protected String iotDirectoryUrl;
	protected String subIdAttributeName;
	protected String subIdParamName;
	protected String additionalQueryString;
	protected List<String> serviceUriPrefixResponsePath;
	
	protected long maxCacheSize;
	protected long expireCacheEntriesAfterMillis;
	protected boolean expireCache;
	
	public IOTDirectoryResourceLocatorConfig( String iotDirectoryUrl , String subIdAttributeName ,
											  String subIdParamName , String additionalQueryString ,
			  								  List<String> serviceUriPrefixResponsePath ) {

		this.iotDirectoryUrl = iotDirectoryUrl;
		this.subIdAttributeName = subIdAttributeName;
		this.subIdParamName = subIdParamName;
		this.serviceUriPrefixResponsePath = serviceUriPrefixResponsePath;
		this.additionalQueryString = additionalQueryString;
		
		this.maxCacheSize = 50;
		this.expireCache = false;
		this.expireCacheEntriesAfterMillis = 0;
	}
	
	public IOTDirectoryResourceLocatorConfig( String iotDirectoryUrl , String subIdAttributeName ,
											  String subIdParamName , String additionalQueryString ,
											  String serviceUriPrefixResponsePath ) {
		this( iotDirectoryUrl , subIdAttributeName ,
			  subIdParamName , additionalQueryString ,
			  Arrays.asList( serviceUriPrefixResponsePath.split("/" ) ) );
	}
	
	public String getIotDirectoryUrl() {
		return iotDirectoryUrl;
	}
	
	public String getSubscriptionIdAttributeName() {
		return subIdAttributeName;
	}
	
	public String getSubscriptionIdRequestParamName() {
		return subIdParamName;
	}
	
	public List<String> getServiceUriPrefixResponsePath() {
		return serviceUriPrefixResponsePath;
	}
	
	public String getAdditionalQueryString() {
		return additionalQueryString;
	}
	
	public void setMaxCacheSize( long maxCacheSize ) {
		this.maxCacheSize = maxCacheSize;
	}
	
	public void setCacheExpireEntriesAfter( long millis ) {
		this.expireCache = true;
		this.expireCacheEntriesAfterMillis = millis;
	}
	
	public void setExpireCache( boolean expire ) {
		this.expireCache = expire;
	}
	
	public long getMaxCacheSize() {
		return maxCacheSize;
	}
	
	public long getCacheEntriesExpireTime() {
		return expireCacheEntriesAfterMillis;
	}
	
	public boolean cacheExpires() {
		return expireCache;
	}
}
