package org.disit.nifi.processors.enrich_data.locators;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EnrichmentResourceLocatorException extends Exception {

	private static final long serialVersionUID = 1L;
	private Map<String , String> additionalInfos;
	
	public EnrichmentResourceLocatorException( String message ) {
		super( message );
		this.additionalInfos = new HashMap<>();
	}
	
	public EnrichmentResourceLocatorException( String message , Throwable cause ) {
		super( message , cause );
	}
	
	public void addAdditionalInfo( String name , String value ) {
		this.additionalInfos.put( name , value );
	}
	
	public String getAdditionalInfo( String name ) {
		return this.additionalInfos.get( name );
	}
	
	public Map<String , String> getAllAdditionalInfos() {
		return Collections.unmodifiableMap( this.additionalInfos );
	}

}
