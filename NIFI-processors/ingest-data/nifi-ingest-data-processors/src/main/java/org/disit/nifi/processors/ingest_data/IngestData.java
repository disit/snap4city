/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Panconi Christian ( panconi.christian@gmail.com )
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

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
    
    @Override
    protected void init(final ProcessorInitializationContext context) {
    	// property descriptors
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add( GET_TIMESTAMP_FROM );
        descriptors.add( TIMESTAMP_FIELD_PATH );
        descriptors.add( OUTPUT_TIMESTAMP_FIELD_NAME );
        descriptors.add( DATA_FIELD_NAME );
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
    							     .expressionLanguageSupported( true )
    							     .required( false )
    							     .build();
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
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
        		String newDataFieldContent = dataFieldObject.toString(); 	
        		
        		// Write to flow file content
        		flowFile = session.write( flowFile , new OutputStreamCallback() {
					@Override
					public void process(OutputStream out) throws IOException {
						out.write( newDataFieldContent.getBytes( StandardCharsets.UTF_8 ) );
					}
				} );
        		
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
}
