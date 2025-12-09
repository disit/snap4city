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

package org.disit.nifi.processors.enrich_data.web.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ControllerService;

/**
 * Custom implementation for the serialized PropertyDescriptor.
 * 
 * @author panconi.christian@gmail.com
 *
 */
public class PropertyComponentDescriptor {

    private final String name;
    private final String displayName;
    private final String description;
    private final String defaultValue;
    private final String controllerServiceApiClass;
    private final boolean isRequired;
    private final boolean isSensitive;
    private final boolean isDynamic;
    private final boolean isControllerService;
    private final Map<String,String> allowableValues;

    public static final PropertyComponentDescriptor fromPropertyDescriptor( PropertyDescriptor descriptor ) {
    	
    	Map<String , String> allowableVals;
    	if( descriptor.getAllowableValues() == null ) {
    		allowableVals = null;
    	}else {
    		allowableVals = new HashMap<>();
    		for( int i=0 ; i < descriptor.getAllowableValues().size() ; i++  ) {
    			allowableVals.put( 
    				Integer.toString(i+1) , 
    				descriptor.getAllowableValues().get(i).getValue() 
    			);
    		}
    	}
    	
    	Builder builder = new Builder()
    		.name( descriptor.getName() )
    		.displayName( descriptor.getDisplayName() )
    		.description( descriptor.getDescription() )
    		.defaultValue( descriptor.getDefaultValue() )
    		.allowableValues( allowableVals )
    		.isRequired( descriptor.isRequired() )
    		.isSensitive( descriptor.isSensitive() )
    		.isDynamic( descriptor.isDynamic() );
    	
    	// Controller service details
    	if( descriptor.getControllerServiceDefinition() == null ) {
    		builder.isControllerService(false)
    			   .controllerServiceApiClass( null );
    	}else {
    		Class<? extends ControllerService> csd = descriptor.getControllerServiceDefinition();
    		builder.isControllerService( true )
    			   .controllerServiceApiClass( csd.getCanonicalName() );
    	}
    	
    	return builder.build();
    }
    
    private PropertyComponentDescriptor(Builder builder){
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.defaultValue = builder.defaultValue;
        this.allowableValues = builder.allowableValues;
        this.isRequired = builder.isRequired;
        this.isSensitive = builder.isSensitive;
        this.isDynamic = builder.isDynamic;
        this.isControllerService = builder.isControllerService;
        this.controllerServiceApiClass = builder.controllerServiceApiClass;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Map<String,String> getAllowableValues() {
        return allowableValues;
    }
    
    public boolean isRequired() {
    	return this.isRequired;
    }
    
    public boolean isSensitive() {
    	return this.isSensitive;
    }
    
    public boolean isDynamic() {
    	return this.isDynamic;
    }
    
    public boolean isControllerService() {
    	return this.isControllerService;
    }
    
    public String getControllerServiceApiClass() {
    	return this.controllerServiceApiClass;
    }

    public static final class Builder{
        private String name;
        private String displayName;
        private String description;
        private String defaultValue;
        private String controllerServiceApiClass;
        private boolean isRequired;
        private boolean isSensitive;
        private boolean isDynamic;
        private boolean isControllerService;
        private Map<String,String> allowableValues;

        public Builder name(String name){
            this.name = name;
            return this;
        }

        public Builder displayName(String displayName){
            this.displayName = displayName;
            return this;
        }

        public  Builder description(String description){
            this.description = description;
            return this;
        }

        public Builder defaultValue(String defaultValue){
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder allowableValues(Map<String,String> allowableValues){
            this.allowableValues = allowableValues;
            return this;
        }
        
        public Builder isRequired( boolean isRequired ) {
        	this.isRequired = isRequired;
        	return this;
        }
        
        public Builder isSensitive( boolean isSensitive ) {
        	this.isSensitive = isSensitive;
        	return this;
        }
        
        public Builder isDynamic( boolean isDynamic ) {
        	this.isDynamic = isDynamic;
        	return this;
        }
        
        public Builder isControllerService( boolean isControllerService ) {
        	this.isControllerService = isControllerService;
        	return this;
        }

        public Builder controllerServiceApiClass( String controllerServiceApiClass ) {
        	this.controllerServiceApiClass = controllerServiceApiClass;
        	return this;
        }
        
        public PropertyComponentDescriptor build(){
            return new PropertyComponentDescriptor(this);
        }
    }
}
