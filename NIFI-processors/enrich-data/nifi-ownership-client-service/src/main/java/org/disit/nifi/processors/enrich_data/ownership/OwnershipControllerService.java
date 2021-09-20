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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClientService;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Ownership controller service class. 
 */

public class OwnershipControllerService extends AbstractControllerService implements EnrichmentSourceClientService {

	protected static final List<PropertyDescriptor> descriptors;
	
    public static final PropertyDescriptor OWNERSHIP_API_URL = new PropertyDescriptor
            .Builder().name("OWNERSHIP_API_URL")
            .displayName("Ownership API Url")
            .description("The endpoint to contact to get the ownership data.")
            .required(true)
            .expressionLanguageSupported( ExpressionLanguageScope.VARIABLE_REGISTRY )
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    public static final PropertyDescriptor ELEMENT_ID_NAME = new PropertyDescriptor
    		.Builder().name( "ELEMENT_ID_NAME" )
    		.displayName( "ElementId name" )
    		.description( "The parameter name for the element id to put in the request url query string." )
    		.required( true )
    		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    		.build();

    public static final PropertyDescriptor ELEMENT_ID_PREFIX = new PropertyDescriptor
    		.Builder().name( "ELEMENT_ID_PREFIX" )
    		.displayName( "ElementId prefix" )
    		.description( "An optional prefix to prepend to the element id (ex: 'organization:broker:')" )
    		.required( false )
    		.addValidator( Validator.VALID )
    		.build();
    
    public static final PropertyDescriptor OWNERSHIP_FIELDS = new PropertyDescriptor
            .Builder().name("OWNERSHIP_FIELDS")
            .displayName("Ownership Fields")
            .description("A comma-separated list of fields to be extracted from the ownership response." )
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();    
    
    public static final Validator FIELDS_MAPPING_VALIDATOR = new Validator() {
    	@Override
		public ValidationResult validate(String subject, String input, ValidationContext context) {
			ValidationResult.Builder builder = new ValidationResult.Builder();
			JsonParser parser = new JsonParser();
			
			if( !input.isEmpty() ) {
				try {
					JsonElement mapping = parser.parse( input );
					
					if( mapping.isJsonObject() ) {
						JsonObject mappingObj = mapping.getAsJsonObject();
						for( String prop : mappingObj.keySet() ) {
							if( !mappingObj.get( prop ).isJsonPrimitive() ) {
								builder.subject( subject )
									   .explanation( String.format( "Invalid propery '%s' specified in mapping. The mapped value must be a string, Object or Array found." , 
											   					    prop ) )
									   .valid( false );
								break;
							}
						}
						
						builder.subject( subject ).valid( true );
					} else {
						builder.subject( subject ).explanation( "The specified mapping is not a JsonObject." )
							   .valid( false );
					}
					
				} catch ( JsonParseException ex ) {
					builder.subject( subject ).explanation( ex.getMessage() )
						   .valid( false );
				}
			} else {
				builder.subject( subject ).explanation( "No mapping set." )
					   .valid( true );
			}
			
			return builder.build();
    	}
    };
    
    public static final PropertyDescriptor FIELDS_MAPPING = new PropertyDescriptor
    		.Builder().name( "FIELDS_MAPPING" )
    		.displayName( "Fields mapping" )
    		.description( "A mapping to rename the retrieved ownership fields. The mapping must specify the name of the field to rename as key and the new name as value." )
    		.required( false )
    		.addValidator( FIELDS_MAPPING_VALIDATOR )
    		.build();
    		

    static {
    	final List<PropertyDescriptor> descs = new ArrayList<>();
    	
        descs.add( OWNERSHIP_API_URL );
        descs.add( ELEMENT_ID_NAME );
        descs.add( ELEMENT_ID_PREFIX );
        descs.add( OWNERSHIP_FIELDS );
        descs.add( FIELDS_MAPPING );
        descriptors = Collections.unmodifiableList( descs );
    }
    
    protected static final Set<String> staticProperties = new HashSet<>( Arrays.asList( 
    	OWNERSHIP_API_URL.getName() ,
    	ELEMENT_ID_NAME.getName() ,
    	ELEMENT_ID_PREFIX.getName() ,
    	OWNERSHIP_FIELDS.getName() ,
    	FIELDS_MAPPING.getName()
    ) );
    
    protected OwnershipClientConfig clientConfig;
    
    private JsonParser parser = new JsonParser();
    
    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }
    
    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor( final String propertyDescriptorName ) {
    	return new PropertyDescriptor.Builder().name( propertyDescriptorName )
    							 	 .addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    							 	 .description( "Every dynamic property specifies an additional parameter to be added to the query string for the request to the Ownership API Url." )
    							 	 .dynamic( true )
    							 	 .required( false )
    							 	 .build();
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException {
//    	String ownershipApiUrl = context.getProperty( OWNERSHIP_API_URL ).getValue();
    	String ownershipApiUrl = context.getProperty( OWNERSHIP_API_URL )
    									.evaluateAttributeExpressions()
    									.getValue();
    	String elementIdPrefix = context.getProperty( ELEMENT_ID_PREFIX ).getValue();
    	String elementIdName = context.getProperty( ELEMENT_ID_NAME ).getValue();
    	
    	OwnershipClientConfig clientConfig = new OwnershipClientConfig( ownershipApiUrl , elementIdName );
    	clientConfig.setElementIdPrefix( elementIdPrefix );
    	
    	context.getAllProperties().forEach( (String paramName , String paramValue) -> {
    		if( !staticProperties.contains( paramName ) ) {
    			clientConfig.addQueryParameter( paramName , paramValue );
    		}
    	});
    	
    	
    	//Ownership fields
//    	this.ownershipFields = 
    	Arrays.asList( context.getProperty( OWNERSHIP_FIELDS ).getValue()
    				  		  .trim().split(",") 
    	).stream().map( s -> { return s.trim(); } )
    	.forEach( (String field) -> {
    		clientConfig.addOwnershipField( field );
    	});
    		
    	String fieldsMappingStr = context.getProperty( FIELDS_MAPPING ).getValue();
    	
    	if( fieldsMappingStr != null && !fieldsMappingStr.isEmpty() ) {
	    	JsonObject fieldsMappingObj = parser.parse( fieldsMappingStr ).getAsJsonObject();
	    	
	    	for( String field : fieldsMappingObj.keySet() ) {
	    		clientConfig.addFieldMapping( field , fieldsMappingObj.get( field ).getAsString() );
	    	}
    	}
    	
    	this.clientConfig = clientConfig;
//    	System.out.println( this.clientConfig );
    }

    @OnDisabled
    public void onDisable() {

    }

    @Override
    public EnrichmentSourceClient getClient( ProcessContext context ) throws InstantiationException {
    	return new OwnershipHttpClient( clientConfig , context );
    }

}
