/**
 *  Nifi IngestData processor
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
package org.disit.nifi.processors.ingest_data;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.ConfigurationException;

@Tags({"ingestion"})

@CapabilityDescription("Provide a description")

@SeeAlso({})

@ReadsAttributes({@ReadsAttribute(attribute="", description="")})

@WritesAttributes({@WritesAttribute(attribute="", description="")})

public class IngestData extends AbstractProcessor {

    public static final PropertyDescriptor GET_TIMESTAMP_FROM = new PropertyDescriptor
            .Builder().name("GET_TIMESTAMP_FROM")
            .displayName("Get timestamp from")
            .description("Specify how the output flow-file timestamp is set. If 'flow-file-entry-date' is chosen, " + 
            			 "the timestamp is picked from the entryDate ('Time' in flow file details), if 'content-field' " +
            		     "is chosen, the timestamp is parsed from the JSON field specified by 'Timestamp field path' " +
            			 "property" )
            .required(true)
            .allowableValues( ConfigConstants.GET_TIMESTAMP_FROM_VALUES )
            .addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
            .build();
    
    public static final PropertyDescriptor TIMESTAMP_FIELD_PATH = new PropertyDescriptor
    		.Builder().name( "TIMESTAMP_FIELD_PATH" )
    		.displayName( "Timestamp field path" )
    		.description( "Specify the attribute from which the timestamp is parsed in the flow file JSON content." + 
    					  "This property is used only if 'Get timestamp from' is set to 'content-field." )
    		.required(false)
    		.addValidator( Validator.VALID ) // The validator must be added in any case to avoid 'Property not supported error',
    										 // the Validator.VALID specifies to not perform any validation
    		.build();
    
    public static final PropertyDescriptor OUTPUT_TIMESTAMP_FIELD_NAME = new PropertyDescriptor
    		.Builder().name( "OUTPUT_TIMESTAMP_FIELD_NAME" )
    		.displayName( "Output timestamp field name" )
    		.description( "Specify the name for the parsed timestamp field in the output flow file content." )
    		.required(true)
    		.addValidator( StandardValidators.NON_EMPTY_EL_VALIDATOR )
    		.build();
    
    public static final PropertyDescriptor DATA_FIELD_NAME = new PropertyDescriptor
    		.Builder().name( "DATA_FIELD_NAME" )
    		.displayName( "Data field name" )
    		.description( "Specifies the field name to treat as data field in the flow file JSON content.\n" + 
    					  "The specified field could contain a single JSON object or an array of JSON objects." )
    		.required(true)
    		.addValidator( StandardValidators.NON_EMPTY_EL_VALIDATOR )
    		.build();
    		
    public static final PropertyDescriptor PREFIX_ATTRIBUTES = new PropertyDescriptor
    		.Builder().name( "PREFIX_ATTRIBUTES" )
    		.displayName( "Prefix attributes" )
    		.description( "A comma-separated list of flow-file attributes to be used to build a prefix for the fields specified by the 'Prepend prefix to' property.\n" + 
    					  "The prefix is built using the ordering specified in this property and the attribute-values are separated using the value of the 'Prefix separator' property.\n" +
    					  "The prefixing operation is performed if the source flow-file contains at least one of the specified attributes, missing attributes will lead to an empty part in the prefix.\n" +
    					  "If the source flow-file does not contain any of the specified attributes the prefixing operation will not be performed." )
    		.required( false )
    		.addValidator( Validator.VALID )
    		.build();
    
    public static final PropertyDescriptor PREFIX_SEPARATOR = new PropertyDescriptor
    		.Builder().name( "PREFIX_SEPARATOR" )
    		.displayName( "Prefix separator" )
    		.description( "A string to use as separator for the attribute values." )
    		.required( false )
    		.addValidator( Validator.VALID )
    		.build();
    
    public static final PropertyDescriptor PREPEND_PREFIX_TO = new PropertyDescriptor
    		.Builder().name( "PREPEND_PREFIX_TO" )
    		.displayName( "Prepend prefix to" )
    		.description( "A comma-separated list of fields contained in the JSON object whose value will be augmented with the prefix." )
    		.required( false )
    		.addValidator( Validator.VALID )
    		.build();

    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name( "SUCCESS_RELATIONSHIP" )
            .description( "Successfully processed flow files will be routed to this relationship." )
            .build();
    
    public static final Relationship FAILURE_RELATIONSHIP = new Relationship.Builder()
    		.name( "FAILURE_REALTIONSHIP" )
    		.description( "Flow file which does not contain a valid JsonObject will be routed to this relationship." )
    		.build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;
    
    private Gson gson;
    private JsonParser parser;
    private ComponentLog logger;
    
    // Property values attributes
    private boolean timestampFromAttribute = true;
    private String outTimestampName;
    private String timestampPath;
    private String dataFieldName;
    
    private List<String> prefixAttributes;
    private String prefixSeparator;
    private Set<String> fieldsToPrefix;
    private boolean performPrefix;
    
    @Override
    protected void init(final ProcessorInitializationContext context) {
    	// property descriptors
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add( GET_TIMESTAMP_FROM );
        descriptors.add( TIMESTAMP_FIELD_PATH );
        descriptors.add( OUTPUT_TIMESTAMP_FIELD_NAME );
        descriptors.add( DATA_FIELD_NAME );
        descriptors.add( PREFIX_ATTRIBUTES );
        descriptors.add( PREFIX_SEPARATOR );
        descriptors.add( PREPEND_PREFIX_TO );
        this.descriptors = Collections.unmodifiableList(descriptors);

        // relationships
        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add( SUCCESS_RELATIONSHIP );
        relationships.add( FAILURE_RELATIONSHIP );
        this.relationships = Collections.unmodifiableSet(relationships);
    }
    
    

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }
    
    /**
     * This method provides the dynamic attributes descriptor.
     */
    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName ) {
    	
    	return new PropertyDescriptor.Builder().name( propertyDescriptorName )
    							     .addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
    							     .description( "A dynamic property attribute specifies an attribute to be added " + 
    							    		 	   "to the output flow file content with the specified value.\n" + 
    							    		 	   "If the output flow file contains multiple data chunks, the attribute " + 
    							    		 	   "will be added to every chunk." )
    							     .dynamic( true )
//    							     .expressionLanguageSupported( true )
    							     .required( false )
    							     .build();
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) throws ConfigurationException {
    	//Set up processor 
    	this.gson = new Gson();
    	this.logger = getLogger();
    	this.parser = new JsonParser();
    	
    	//reading 'Get timestamp from' property 
    	if( context.getProperty( GET_TIMESTAMP_FROM ).getValue().equals( ConfigConstants.GET_TIMESTAMP_FROM_VALUES[0] ) ) {
    		// 'flow-file-entry-date' case
    		timestampFromAttribute = false;
    	} else {
    		// 'content-field' case 
    		timestampFromAttribute = true;
    		timestampPath = context.getProperty( TIMESTAMP_FIELD_PATH ).getValue(); // get 'Timestamp field path' value
    	}
    	
    	outTimestampName = context.getProperty( OUTPUT_TIMESTAMP_FIELD_NAME ).getValue(); // get 'Output timestamp field name' value
    	
    	dataFieldName = context.getProperty( DATA_FIELD_NAME ).getValue(); // get 'Data field name' value
    	
    	
    	// Prefix properties
    	prefixAttributes = new ArrayList<>();
    	if( context.getProperty( PREFIX_ATTRIBUTES ).isSet() ) {
    		String[] prefixAttributeNames = context.getProperty( PREFIX_ATTRIBUTES ).getValue().split( "," );
    		for( int i = 0 ; i < prefixAttributeNames.length ; i++ ) {
    			String attribute = prefixAttributeNames[i].trim();
    			if( !attribute.isEmpty() )
    				prefixAttributes.add( attribute );
    		}
    	}
    	
    	if( context.getProperty( PREFIX_SEPARATOR ).isSet() ) {
    		prefixSeparator = context.getProperty( PREFIX_SEPARATOR ).getValue();
    	} else {
    		prefixSeparator = "";
    	}
    	
    	fieldsToPrefix = new HashSet<>();
    	if( context.getProperty( PREPEND_PREFIX_TO ).isSet() ) {
    		fieldsToPrefix = Arrays.asList( context.getProperty( PREPEND_PREFIX_TO ).getValue().split( "," ) )
    			  .stream().map( (String fieldName) -> {
    				  return fieldName.trim();
    			  })
    			  .filter( (String fieldName) -> {
    				  return !fieldName.isEmpty();
    			  })
    			  .collect( Collectors.toSet() );
    	}

    	// Prefix properties validation
    	boolean prefixMisconfig = false;
    	String reason = "Misconfiguration: '%s' is correctly set, but '%s' evaluates to an empty list. They must be both correctly set or both not set at all.";
    	if( !prefixAttributes.isEmpty() && fieldsToPrefix.isEmpty() ) {
    		reason = String.format( reason , PREFIX_ATTRIBUTES.getDisplayName() , 
    									     PREPEND_PREFIX_TO.getDisplayName() );
    		prefixMisconfig = true;
    	}
    	
    	if( prefixAttributes.isEmpty() && !fieldsToPrefix.isEmpty() ) {
    		reason = String.format( reason , PREPEND_PREFIX_TO.getDisplayName() , 
    										 PREFIX_ATTRIBUTES.getDisplayName() );
    		prefixMisconfig = true;
    	}
    	
    	if( prefixMisconfig ) {
    		throw new ConfigurationException( reason );
    	}
    	
    	if( prefixAttributes.isEmpty() && fieldsToPrefix.isEmpty() ) {
    		performPrefix = false;
    	} else {
    		performPrefix = true;
    	}
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
    	// Get the flow file from the session
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }
        
        // Get flow file content as a byte stream
        final ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();
        session.exportTo( flowFile , contentBytes );
        String flowFileContent = contentBytes.toString();
        
        JsonElement rootEl = parser.parse( flowFileContent );
        if( rootEl.isJsonObject() ) { // check if the root element (whole flow file content) is a valid JsonObject
        	
        	JsonObject rootObj = rootEl.getAsJsonObject();
        	if( rootObj.has( dataFieldName ) ) { // check if the root element contains a field named as the dataFieldName content
        		JsonElement dataFieldElement = rootObj.get( dataFieldName );
        		JsonObject dataFieldObject;
        		
        		if( dataFieldElement.isJsonObject() ) // check if the data field contains a json object
        			dataFieldObject = dataFieldElement.getAsJsonObject();
        		else
        			if( dataFieldElement.isJsonArray() ) // or a json array, in this case take the first element
        					if( dataFieldElement.getAsJsonArray().get(0).isJsonObject() ) // check if the first element is a json object
        						dataFieldObject = dataFieldElement.getAsJsonArray().get(0).getAsJsonObject();
        					else {
        						logger.error( String.format( "The first element of the data array is not a JsonObject, routing to failure. Data content: %s", 
        												     dataFieldElement.toString() ) );
        						session.transfer( flowFile , FAILURE_RELATIONSHIP );
        						return;
        					}
        			else { // the data field content does not contain an array or a json object routing to fauilure.
        				logger.error( String.format( "The data field content does not contain an array or a json object, routing to fauilure. Data content: %s" , 
        										     dataFieldElement.toString() ) );
        				session.transfer( flowFile , FAILURE_RELATIONSHIP );
        				return;
        			}
        		
//        		JsonObject dataFieldObject = rootObj.get( dataFieldName ).getAsJsonObject();
//        		String dataFieldContent = rootObj.get( dataFieldName ).toString();
//        		String dataFieldContent = dataFieldObject.toString();
        		String timestamp;
        		if( !timestampFromAttribute )
        			timestamp = getTimestampFromEntryDate( flowFile );
        		else {
        			try {
        				timestamp = getTimestampFromContentField( rootObj , timestampPath );
        			} catch (NoSuchElementException ex) { // If the timestamp could not be extracted from the ff-content
        												  // fallback to the entry date.
        				timestamp = getTimestampFromEntryDate( flowFile );
        			}
        		}
        		
        		dataFieldObject.addProperty( outTimestampName , timestamp );
        		
        		String prefixReport;
        		if( performPrefix ) {
        			prefixReport = performPrefixing( dataFieldObject , flowFile.getAttributes() );
        			
        			if( prefixReport.isEmpty() )
        				prefixReport = "<Empty prefix>";
        		} else {
        			prefixReport = "<No prefix set>";
        		}
        		
        		String newDataFieldContent = dataFieldObject.toString(); 	
        		
        		// Write to flow file content
        		flowFile = session.write( flowFile , new OutputStreamCallback() {
					@Override
					public void process(OutputStream out) throws IOException {
						out.write( newDataFieldContent.getBytes( StandardCharsets.UTF_8 ) );
					}
				} );
        		
        		flowFile = session.putAttribute( flowFile , "prefix" , prefixReport );
        		
        		session.transfer( flowFile , SUCCESS_RELATIONSHIP );
        		
        	} else { // the flow file JSON content does not contain a field named as dataFieldName content
        		
        		logger.error( String.format( "Flow file %s does not contain '%s' specified as 'Data field name'. Routing to failure | f-f content: %s" ,
        									 flowFile.getAttribute( "uuid" ) , dataFieldName , flowFileContent ) );
        		flowFile = session.putAttribute( flowFile , "failure" , String.format( "The JSON object in f-f content does not contain the '%s' member specified as 'Data field name'." , 
        																	dataFieldName ) );
        		session.transfer( flowFile , FAILURE_RELATIONSHIP );
        	}
   
     
        	
        } else { // the flow file content is not a JsonObject
        	logger.error( String.format( "Flow file %s does not contain a valid JsonObject , routing to FAILURE_RELATIONSHIP | f-f content : %s" , 
        								 flowFile.getAttribute( "uuid" ) , flowFileContent ) );
        	flowFile = session.putAttribute( flowFile , "failure" , "Does not contain a valid JSON object." );
        	session.transfer( flowFile , FAILURE_RELATIONSHIP );
        }
        
    }
    
    
    /**
     * Extracts the timestamp from the flow-file entry-date.
     * 
     * @param flowFile the flow file from which to extract the timestamp.
     * @return the timestamp as a String.
     */
    private String getTimestampFromEntryDate( FlowFile flowFile ) {
    	return Instant.ofEpochMilli( flowFile.getEntryDate() ).toString();
    }
    
    /**
     * Extracts the timestamp from a JsonObject given a path.
     * Also removes the timestamp field from the passed rootObj.
     * 
     * @param rootObj the root object from which to extract the timestamp.
     * @param timestampFieldPath the path for the timestamp inside the jsonObject as field name hierarchy in which each field name is separated by a "/" 
     * @return the timestamp as a String.
     * @throws NoSuchElementException if the jsonObject deos not contain the attribute specified by the path.
     */
    private String getTimestampFromContentField( JsonObject rootObj , String timestampFieldPath ) throws NoSuchElementException{
    	String[] timestampTokens = timestampFieldPath.split( "/" );
    	JsonObject current = rootObj;
    	
    	for( int i = 0 ; i < timestampTokens.length ; i++ ) {
    		if( current.has( timestampTokens[i] ) ) {
    			if( i < timestampTokens.length - 1 ) { // if is not the last token (i.e timestampTokens[i] contains the timestamp field name)
    				if( current.get( timestampTokens[i] ).isJsonObject() ) {
    					current = current.get( timestampTokens[i] ).getAsJsonObject();
    				} else { 
    					if( current.get( timestampTokens[i] ).isJsonArray() && 
    					    current.get( timestampTokens[i] ).getAsJsonArray().get( 0 ).isJsonObject() ) {
    						
    						current = current.get( timestampTokens[i] ).getAsJsonArray().get( 0 ).getAsJsonObject();
    					} else {
    						throw new NoSuchElementException( "Wrong timestamp attribute format." );
    					}
    				}
    			}
    		} else {
    			throw new NoSuchElementException( String.format( "The flow file content does not contain the timestamp attribute specified bypath '%s'. \nLast detected path field: '%s'." ,
    						                                         timestampFieldPath , timestampTokens[i-1] ) );
    		}
    	}
    	
    	// If we reach this point, 'current' contains the jsonObject one level before the timestamp field
    	// so we can return the timestamp as String (the check: if the jsonObject contains the timestamp field is done
    	// in the last iteration of the for loop)
    	String timestamp = current.get( timestampTokens[timestampTokens.length-1] ).getAsString();
    	//current.remove( timestampTokens[timestampTokens.length - 1] );
    	return timestamp;
    }
    
    private String performPrefixing( JsonObject dataObject , Map<String , String> ffAttributes ) {
    	String prefix = buildPrefix( ffAttributes );
    	
    	if( !prefix.isEmpty() ) {
	    	for( String fieldName : fieldsToPrefix ) {
	    		if( dataObject.has( fieldName ) ) {
	    			dataObject.addProperty( 
	    				fieldName , 
	    				prefix.concat( dataObject.get( fieldName ).getAsString() ) 
	    			);
	    		}
	    	}
    	}
    	
    	return prefix;
    }
    
    private String buildPrefix( Map<String,String> ffAttributes ) {
    	StringBuilder prefixBuilder = new StringBuilder();
    	
    	int attributesFound = 0;
    	for( int i=0 ; i < prefixAttributes.size() ; i++ ) {
    		String attributeName = prefixAttributes.get(i);
    		if( ffAttributes.containsKey( attributeName ) ) {
    			prefixBuilder.append( ffAttributes.get( attributeName ) );
    			attributesFound ++;
    		}
    		prefixBuilder.append( prefixSeparator );
    	}
    	
    	if( attributesFound > 0 )
    		return prefixBuilder.toString();
    	else
    		return "";
    }
    
}
