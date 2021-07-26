package org.disit.nifi.processors.enrich_data.web.api;

import java.util.Collection;
import java.util.Map;

/**
 * Details about a given processor. Contains configuration and current
 * validation errors.
 */
public class ProcessorComponentDetails {

    private final String id;
    private final String name;
    private final String type;
    private final String state;
    private final String annotationData;
    private final boolean supportsDynamicProperties;
    private final String dynamicPropertiesDescription;
    private final Map<String, String> properties;
    private final Map<String, PropertyComponentDescriptor> descriptors;

    private final Collection<String> validationErrors;

    private ProcessorComponentDetails(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.state = builder.state;
        this.annotationData = builder.annotationData;
        this.properties = builder.properties;
        this.descriptors = builder.descriptors;
        this.validationErrors = builder.validationErrors;
        this.supportsDynamicProperties = builder.supportsDynamicProperties;
        this.dynamicPropertiesDescription = builder.dynamicPropertiesDescription;
    }

    /**
     * @return component id
     */
    public String getId() {
        return id;
    }

    /**
     * @return component name
     */
    public String getName() {
        return name;
    }

    /**
     * @return component type
     */
    public String getType() {
        return type;
    }

    /**
     * @return component state
     */
    public String getState() {
        return state;
    }

    /**
     * @return component's annotation data
     */
    public String getAnnotationData() {
        return annotationData;
    }
    
    public boolean isDynamicPropertySupported() {
    	return supportsDynamicProperties;
    }
    
    public String getDynamicPropertiesDescription() {
    	return dynamicPropertiesDescription;
    }

    /**
     * @return Mapping of component properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }


    /**
     * @return Mapping of component descriptors
     */
    public Map<String,PropertyComponentDescriptor> getDescriptors(){
        return descriptors;
    }

    /**
     * @return Current validation errors for the component
     */
    public Collection<String> getValidationErrors() {
        return validationErrors;
    }

    public static final class Builder {

        private String id;
        private String name;
        private String type;
        private String state;
        private String annotationData;
        private boolean supportsDynamicProperties;
        private String dynamicPropertiesDescription;
        private Map<String, String> properties;
        private Map<String,PropertyComponentDescriptor> descriptors;

        private Collection<String> validationErrors;

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder type(final String type) {
            this.type = type;
            return this;
        }

        public Builder state(final String state) {
            this.state = state;
            return this;
        }

        public Builder annotationData(final String annotationData) {
            this.annotationData = annotationData;
            return this;
        }

        public Builder properties(final Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public Builder validateErrors(final Collection<String> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }
        
        public Builder supportsDynamicProperties( final boolean supportsDynamicProperties ) {
        	this.supportsDynamicProperties = supportsDynamicProperties;
        	return this;
        }
        
        public Builder dynamicPropertiesDescription( final String dynamicPropertiesDescription ) {
        	this.dynamicPropertiesDescription = dynamicPropertiesDescription;
        	return this;
        }

        public Builder descriptors(final Map<String,PropertyComponentDescriptor> descriptors){
            this.descriptors = descriptors;
            return this;
        }

        public ProcessorComponentDetails build() {
            return new ProcessorComponentDetails(this);
        }
    }
}
