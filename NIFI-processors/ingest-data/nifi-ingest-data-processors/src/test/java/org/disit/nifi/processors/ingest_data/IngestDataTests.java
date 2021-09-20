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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class IngestDataTests {

	private TestRunner testRunner;
	JsonParser parser;
	
	String misconfigReasonTemplate = "Misconfiguration: '%s' is correctly set, but '%s' evaluates to an empty list. They must be both correctly set or both not set at all.";
	
	@Before
	public void init() {
		parser = new JsonParser();
		testRunner = TestRunners.newTestRunner( IngestData.class );
		
		testRunner.setProperty( IngestData.GET_TIMESTAMP_FROM , ConfigConstants.GET_TIMESTAMP_FROM_VALUES[0] );
		testRunner.setProperty( IngestData.OUTPUT_TIMESTAMP_FIELD_NAME , "date_time" );
		testRunner.setProperty( IngestData.DATA_FIELD_NAME , "data" );
	}
	
	@Test
	public void testAttributesExtraction() throws IOException {
		testRunner.setProperty( IngestData.EXTRACT_FIELDS_AS_ATTRIBUTES , "subscriptionId" );
		testRunner.setProperty( IngestData.EXTRACT_FIELDS_AS_ATTRIBUTES , "data/latitude/aaa" );
		
		testRunner.enqueue( Paths.get( "src/test/resources/mock_in_ff/test.ff" ) );
		testRunner.run();
		testRunner.assertAllFlowFilesTransferred( IngestData.SUCCESS_RELATIONSHIP );
		MockFlowFile successFF = testRunner.getFlowFilesForRelationship( IngestData.SUCCESS_RELATIONSHIP ).get(0);
		
		System.out.println( TestUtils.prettyOutFF( successFF , parser ) );
	}
	
//	@Test
	public void testAttributeExpressionLanguageError() throws IOException {
		System.out.println( "\n=================================" );
		System.out.println( "TEST ATTRIBUTE EXPRESSION LANGUAGE ERROR \n\n" );
		// !! Intentional error in prefix configuration
		testRunner.setProperty( IngestData.PREFIX , "${asd:}" ); 
		testRunner.setProperty( IngestData.PREPEND_PREFIX_TO , "id" );
		
		JsonObject mockFFContentObj = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/mock_in_ff/test.ff" ) , 
			parser 
		);
		
		JsonObject resultReference = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/reference_results/test_no_prefix.ref" ) , 
			parser 
		); 
		
		testRunner.enqueue( mockFFContentObj.toString() );
		// Should log an error about an AttributeExpressionLanguageException
		try {
			testRunner.run();
		} catch( AssertionError err ) {
			System.out.println( "Correctly thrown exception: " );
			err.printStackTrace();
		}
//		testRunner.assertAllFlowFilesTransferred( IngestData.SUCCESS_RELATIONSHIP );
//		
//		List<MockFlowFile> successFFList = testRunner.getFlowFilesForRelationship( IngestData.SUCCESS_RELATIONSHIP );
//		
//		for( MockFlowFile ff : successFFList ) {
//			JsonElement ffContent = parser.parse( new String( ff.toByteArray() ) );
//			if( !ffContent.isJsonObject() ) {
//				fail( "The result flow file content is not a json object" );
//			}
//			
//			JsonObject ffContentObj = ffContent.getAsJsonObject();
//			
//			if( !ffContentObj.has( "date_time" ) ) {
//				fail( "The result flow file does not have 'date_time' in its content." );
//			}
//			
//			String dateTime = ffContentObj.get( "date_time" ).getAsString();
//			resultReference.addProperty( "date_time" , dateTime );
//			
//			assertEquals( "<Empty prefix>" , ff.getAttribute( "prefix" ) );
//			
//			assertEquals( resultReference , ffContentObj );
//			
//			System.out.println( TestUtils.prettyOutFF( ff , parser ) );
//			System.out.println( "=================================" );
//		}
		System.out.println( "=================================" );
	}
	
//	@Test
	public void testNoPrefixSet() throws IOException{
		System.out.println( "\n=================================" );
		System.out.println( "TEST NO PREFIX SET\n\n" );
		
		JsonObject mockFFContentObj = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/mock_in_ff/test.ff" ) , 
			parser 
		);
		
		JsonObject resultReference = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/reference_results/test_no_prefix.ref" ) , 
			parser 
		); 
		
		Map<String,String> attributes = new HashMap<>();
		attributes.put( "Fiware-Service" , "service" );
		attributes.put( "Fiware-Servicepath" , "servicepath" );
		
		testRunner.enqueue( mockFFContentObj.toString() , attributes );
		testRunner.run();
		testRunner.assertAllFlowFilesTransferred( IngestData.SUCCESS_RELATIONSHIP );
		
		List<MockFlowFile> successFFList = testRunner.getFlowFilesForRelationship( IngestData.SUCCESS_RELATIONSHIP );
		
		for( MockFlowFile ff : successFFList ) {
			JsonElement ffContent = parser.parse( new String( ff.toByteArray() ) );
			if( !ffContent.isJsonObject() ) {
				fail( "The result flow file content is not a json object" );
			}
			
			JsonObject ffContentObj = ffContent.getAsJsonObject();
			
			if( !ffContentObj.has( "date_time" ) ) {
				fail( "The result flow file does not have 'date_time' in its content." );
			}
			
			String dateTime = ffContentObj.get( "date_time" ).getAsString();
			resultReference.addProperty( "date_time" , dateTime );
			
			assertEquals( "<No prefix set>" , ff.getAttribute( "prefix" ) );
			
			assertEquals( resultReference , ffContentObj );
			
			System.out.println( TestUtils.prettyOutFF( ff , parser ) );
			System.out.println( "=================================" );
		}
	}
	
//	@Test
	public void testPrefix() throws IOException{
		System.out.println( "\n=================================" );
		System.out.println( "TEST PREFIX\n\n" );
		
		JsonObject mockFFContentObj = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/mock_in_ff/test.ff" ) , 
			parser 
		);
		
		JsonObject resultReference = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/reference_results/test_prefix.ref" ) , 
			parser 
		); 
		
		Map<String,String> attributes = new HashMap<>();
		attributes.put( "Fiware-Service" , "service" );
		attributes.put( "Fiware-Servicepath" , "servicepath" );
		
//		testRunner.setProperty( IngestData.PREFIX_ATTRIBUTES , "Fiware-Service,Fiware-Servicepath" );
//		testRunner.setProperty( IngestData.PREFIX_SEPARATOR , ";" );
		
		testRunner.setProperty( IngestData.PREFIX , "${Fiware-Service};${Fiware-Servicepath};" );
		
		testRunner.setProperty( IngestData.PREPEND_PREFIX_TO , "id" );
		
		testRunner.enqueue( mockFFContentObj.toString() , attributes );
		testRunner.run();
		testRunner.assertAllFlowFilesTransferred( IngestData.SUCCESS_RELATIONSHIP );
		
		List<MockFlowFile> successFFList = testRunner.getFlowFilesForRelationship( IngestData.SUCCESS_RELATIONSHIP );
		
		for( MockFlowFile ff : successFFList ) {
			JsonElement ffContent = parser.parse( new String( ff.toByteArray() ) );
			if( !ffContent.isJsonObject() ) {
				fail( "The result flow file content is not a json object" );
			}
			
			JsonObject ffContentObj = ffContent.getAsJsonObject();
			
			if( !ffContentObj.has( "date_time" ) ) {
				fail( "The result flow file does not have 'date_time' in its content." );
			}
			
			String dateTime = ffContentObj.get( "date_time" ).getAsString();
			resultReference.addProperty( "date_time" , dateTime );
			
			assertEquals( "service;servicepath;" , ff.getAttribute( "prefix" ) );
			
			assertEquals( resultReference , ffContentObj );
			
			System.out.println( TestUtils.prettyOutFF( ff , parser ) );
			System.out.println( "=================================" );
		}
	}
	
//	@Test
	public void testPartialPrefix() throws IOException{
		System.out.println( "\n=================================" );
		System.out.println( "TEST PARTIAL PREFIX\n\n" );
		
		JsonObject mockFFContentObj = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/mock_in_ff/test.ff" ) , 
			parser 
		);
		
		JsonObject resultReference = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/reference_results/test_partial_prefix.ref" ) , 
			parser 
		); 
		
		Map<String,String> attributes = new HashMap<>();
		attributes.put( "Fiware-Service" , "service" );
//		attributes.put( "Fiware-Servicepath" , "servicepath" );
		
//		testRunner.setProperty( IngestData.PREFIX_ATTRIBUTES , "Fiware-Service,Fiware-Servicepath" );
//		testRunner.setProperty( IngestData.PREFIX_SEPARATOR , ";" );
		testRunner.setProperty( IngestData.PREFIX , "${Fiware-Service};${Fiware-Servicepath};" );
		
		testRunner.setProperty( IngestData.PREPEND_PREFIX_TO , "id" );
		
		testRunner.enqueue( mockFFContentObj.toString() , attributes );
		testRunner.run();
		testRunner.assertAllFlowFilesTransferred( IngestData.SUCCESS_RELATIONSHIP );
		
		List<MockFlowFile> successFFList = testRunner.getFlowFilesForRelationship( IngestData.SUCCESS_RELATIONSHIP );
		
		for( MockFlowFile ff : successFFList ) {
			JsonElement ffContent = parser.parse( new String( ff.toByteArray() ) );
			if( !ffContent.isJsonObject() ) {
				fail( "The result flow file content is not a json object" );
			}
			
			JsonObject ffContentObj = ffContent.getAsJsonObject();
			
			if( !ffContentObj.has( "date_time" ) ) {
				fail( "The result flow file does not have 'date_time' in its content." );
			}
			
			String dateTime = ffContentObj.get( "date_time" ).getAsString();
			resultReference.addProperty( "date_time" , dateTime );
			
			assertEquals( "service;;" , ff.getAttribute( "prefix" ) );
			
			assertEquals( resultReference , ffContentObj );
			
			System.out.println( TestUtils.prettyOutFF( ff , parser ) );
			System.out.println( "=================================" );
		}
	}
	
//	@Test
	public void testAttributesNotPresentPrefix() throws IOException{
		System.out.println( "\n=================================" );
		System.out.println( "TEST ATTRIBUTES NOT PRESENT PREFIX\n\n" );
		
		JsonObject mockFFContentObj = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/mock_in_ff/test.ff" ) , 
			parser 
		);
		
		JsonObject resultReference = TestUtils.mockJsonObjFromFile( 
			Paths.get( "src/test/resources/reference_results/test_attributes_not_present_prefix.ref" ) , 
			parser 
		); 
		
		Map<String,String> attributes = new HashMap<>();
//		attributes.put( "Fiware-Service" , "service" );
//		attributes.put( "Fiware-Servicepath" , "servicepath" );
		
//		testRunner.setProperty( IngestData.PREFIX_ATTRIBUTES , "Fiware-Service,Fiware-Servicepath" );
//		testRunner.setProperty( IngestData.PREFIX_SEPARATOR , ";" );
		testRunner.setProperty( IngestData.PREFIX , "${Fiware-Service};${Fiware-Servicepath};" );
		
		testRunner.setProperty( IngestData.PREPEND_PREFIX_TO , "id" );
		
		testRunner.enqueue( mockFFContentObj.toString() , attributes );
		testRunner.run();
		testRunner.assertAllFlowFilesTransferred( IngestData.SUCCESS_RELATIONSHIP );
		
		List<MockFlowFile> successFFList = testRunner.getFlowFilesForRelationship( IngestData.SUCCESS_RELATIONSHIP );
		
		for( MockFlowFile ff : successFFList ) {
			JsonElement ffContent = parser.parse( new String( ff.toByteArray() ) );
			if( !ffContent.isJsonObject() ) {
				fail( "The result flow file content is not a json object" );
			}
			
			JsonObject ffContentObj = ffContent.getAsJsonObject();
			
			if( !ffContentObj.has( "date_time" ) ) {
				fail( "The result flow file does not have 'date_time' in its content." );
			}
			
			String dateTime = ffContentObj.get( "date_time" ).getAsString();
			resultReference.addProperty( "date_time" , dateTime );
			
			assertEquals( ";;" , ff.getAttribute( "prefix" ) );
			
			assertEquals( resultReference , ffContentObj );
			
			System.out.println( TestUtils.prettyOutFF( ff , parser ) );
			System.out.println( "=================================" );
		}
	}
	
//	@Test
	public void testMisconfiguration1() throws IOException{
		System.out.println( "\n=================================" );
		System.out.println( "TEST MISCONFIGURATION 1\n\n" );
		
//		String reason = String.format( 
//				misconfigReasonTemplate , 
//				IngestData.PREFIX_ATTRIBUTES.getDisplayName() , 
//				IngestData.PREPEND_PREFIX_TO.getDisplayName() 
//		);
		
//		testRunner.setProperty( IngestData.PREFIX_ATTRIBUTES , "Fiware-Service,Fiware-Servicepath" );
//		testRunner.setProperty( IngestData.PREFIX_SEPARATOR , ";" );
		testRunner.setProperty( IngestData.PREFIX , "${Fiware-Service};${Fiware-Servicepath};" );
		testRunner.setProperty( IngestData.PREPEND_PREFIX_TO , "" );
		
		IngestData ingestDataProcessor  = (IngestData) testRunner.getProcessor();
		try {
			ingestDataProcessor.onScheduled( testRunner.getProcessContext() );
			fail( "The misconfiguration exception has not been thrown." );
		} catch( ConfigurationException ex ) {
//			assertEquals( reason , ex.getMessage() );
			System.out.println( "Correctly thrown exception:\n" + ex.getMessage() );
			System.out.println( "=================================" );
		}
	}
	
//	@Test
	public void testMisconfiguration2() throws IOException{
		System.out.println( "\n=================================" );
		System.out.println( "TEST MISCONFIGURATION 2\n\n" );
		
//		String reason = String.format( 
//				misconfigReasonTemplate , 
//				IngestData.PREPEND_PREFIX_TO.getDisplayName() , 
//				IngestData.PREFIX_ATTRIBUTES.getDisplayName() 
//		);
		
//		testRunner.setProperty( IngestData.PREFIX_ATTRIBUTES , "" );
//		testRunner.setProperty( IngestData.PREFIX_SEPARATOR , ";" );
//		testRunner.setProperty( IngestData.PREFIX , "" );
		testRunner.setProperty( IngestData.PREPEND_PREFIX_TO , "id" );
		
		IngestData ingestDataProcessor  = (IngestData) testRunner.getProcessor();
		try {
			ingestDataProcessor.onScheduled( testRunner.getProcessContext() );
			fail( "The misconfiguration exception has not been thrown." );
		} catch( ConfigurationException ex ) {
//			assertEquals( reason , ex.getMessage() );
			System.out.println( "Correctly thrown exception:\n" + ex.getMessage() );
			System.out.println( "=================================" );
		}
	}

}
