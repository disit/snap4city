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

package org.disit.nifi.processors.enrich_data.test.simple;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.util.MockFlowFile;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.test.EnrichDataTestBase;
import org.disit.nifi.processors.enrich_data.test.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Unit tests for the EnrichData processor using:
 * 	Servicemap
 */
public class EnrichDataSimpleTests extends EnrichDataTestBase{
	
	@Test
	public void testJsonOutput() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testOutputs.ff" );
		
		JsonElement expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testOutputs_jsonOut.ref" , 
			inputFF , parser );

		testRunner.run();
		
		testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
		
		MockFlowFile outFF = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP ).get(0);
		JsonElement outFFContent = JsonParser.parseString( new String( outFF.toByteArray() ) ); 
		assertEquals( true , outFFContent.isJsonObject() );
		assertEquals( true , outFFContent.getAsJsonObject().equals( expectedResult.getAsJsonObject() ) );
//		System.out.println( TestUtils.prettyOutFF( outFF ) );
	}
	
	@Test
	public void testSplitJsonOutput() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testOutputs.ff" );
		
		JsonArray expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testOutputs_splitJson.ref" , 
			inputFF , parser ).getAsJsonArray();
		
		// set "Split JSON" as output format
		testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] );
		testRunner.assertValid();
		testRunner.run();
		testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 8 );
		testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
		
		List<MockFlowFile> successFFs = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
		for( int i=0 ; i < successFFs.size() ; i++ ) {
			JsonElement content = parser.parse( new String( successFFs.get(i).toByteArray() ) );
			assertEquals( true , content.isJsonObject() );
			assertEquals( true , expectedResult.get(i).equals(content.getAsJsonObject() ) );
//			System.out.println( TestUtils.prettyOutFF( successFFs.get(i) ) );
		}
	}
	
	@Test
	public void testEmptyOutObj() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testEmptyOutObj.ff" , 
			"src/test/resources/mock_servicemap_response/testEmptyOutObj.resp" );
		
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testEmptyOutObj.ff" );
		
		testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ); // Json Object
		testRunner.run();
		testRunner.assertTransferCount( EnrichData.FAILURE_RELATIONSHIP , 1 );
		
		MockFlowFile failedFF = testRunner.getFlowFilesForRelationship( EnrichData.FAILURE_RELATIONSHIP ).get(0);
		failedFF.assertAttributeExists( "failure" );
		assertEquals( "The resulting JsonObject after the enrichment is empty." , failedFF.getAttribute( "failure" ) );
	}
	
	@Test
	public void testArrayValue() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testArrayValue.ff" , 
			"src/test/resources/mock_servicemap_response/testArrayValue.resp" );
		
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testArrayValue.ff" );
		
		testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] ); // Split JSON
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 14 );
		testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
		
		JsonArray expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testOutputs_arrayValue.ref" , 
			inputFF , parser ).getAsJsonArray();
		List<MockFlowFile> successFFs = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
		for( int i=0 ; i < successFFs.size() ; i++ ) {
			JsonElement content = parser.parse( new String( successFFs.get(i).toByteArray() ) );
			assertEquals( true , content.isJsonObject() );
			assertEquals( true , expectedResult.get(i).equals( content.getAsJsonObject() ) );
		}
	}
	
	@Test
	public void testStringValueParsing() throws IOException{
		System.out.println( "\n######## " + testName() + " ########" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testParseStringValue.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testParseStringValue.ff" );
		
		testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ); // JSON Object
		testRunner.setProperty( EnrichData.ATTEMPT_STRING_VALUES_PARSING , "Yes" );
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
		
		JsonObject expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testOutputs_parseStringValue.ref" , 
			inputFF , parser ).getAsJsonObject();
		JsonElement outputContent = parser.parse( 
			new String( testRunner.getFlowFilesForRelationship(EnrichData.SUCCESS_RELATIONSHIP)
								  .get(0).toByteArray() ) 
		);
		assertEquals( true , outputContent.isJsonObject() );
		assertEquals( true , expectedResult.equals( outputContent.getAsJsonObject() ) );
	}
	
	// -------- Coordinates Tests --------
	protected void testInnerLatLon( String inputFlowFilePath , String responseFilePath , String referenceFilePath , String innerLatLonConfig ,
									int expectedSuccessFFCount ) throws IOException {
		addServicemapResource( "id" , serviceUriPrefix , inputFlowFilePath , responseFilePath );
			
		MockFlowFile inputFF = enqueueFlowFile( inputFlowFilePath );
		
		testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] ); // Split Json
		testRunner.setProperty( EnrichData.LATLON_PRIORITY , EnrichData.LATLON_PRIORITY_VALUES[1] ); // Priority inner
		testRunner.setProperty( EnrichData.INNER_LAT_LON_CONFIG , innerLatLonConfig );
		
		ValidationResult vr = getValidationResult( EnrichData.INNER_LAT_LON_CONFIG );
		System.out.println( "Validation: \n" + vr.toString() );
		validateProcessorProperties();
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , expectedSuccessFFCount );
		testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
		
		JsonArray expectedResult = TestUtils.prepareExpectedResult( referenceFilePath , inputFF , parser)
											.getAsJsonArray();
		List<MockFlowFile> successFFs = testRunner.getFlowFilesForRelationship( EnrichData.SUCCESS_RELATIONSHIP );
		for( int i=0 ; i < successFFs.size() ; i++ ) {
			JsonElement content = parser.parse( 
				new String( successFFs.get(i).toByteArray() )
			);
			assertEquals( true , content.isJsonObject() );
			assertEquals( true , expectedResult.get(i).equals( content.getAsJsonObject() ) );
		}
	}
	
	@Test
	public void testInnerLatLonGeoJson() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		testInnerLatLon(
			"src/test/resources/mock_in_ff/testInnerLatLonGeoJson.ff" ,
			"src/test/resources/mock_servicemap_response/testOutputs.resp" ,
			"src/test/resources/reference_results/testInnerLatLonGeoJson.ref" ,
    		"{"+
    		   "\"geoJsonFields\":[" +
    				"{ \"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\" }" +
    		   "]," + 
    		   "\"geoPointFields\":[" + 
    		   		"{\"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\"}" +
    		   "]," +
    		   "\"latitudeFields\":[ \"latitude/value\" ]," +
    		   "\"longitudeFields\":[ \"longitude/value\" ]" +
    		"}" ,
    		7
		);
	}
	
	@Test
	public void testInnerLatLonGeoPoint() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		testInnerLatLon(
			"src/test/resources/mock_in_ff/testInnerLatLonGeoPoint.ff" ,
			"src/test/resources/mock_servicemap_response/testOutputs.resp" ,
			"src/test/resources/reference_results/testInnerLatLonGeoPoint.ref" ,
			"{"+
    		   "\"geoJsonFields\":[" +
    				"{ \"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\" }" +
    		   "]," + 
    		   "\"geoPointFields\":[" + 
    		   		"{\"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\"}" +
    		   "]," +
    		   "\"latitudeFields\":[ \"latitude/value\" ]," +
    		   "\"longitudeFields\":[ \"longitude/value\" ]" +
    		"}" ,
    		7
		);
	}
	
	@Test
	public void testInnerLatLonGeoDistinctFields() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		testInnerLatLon(
			"src/test/resources/mock_in_ff/testInnerLatLonDistinctFields.ff" ,
			"src/test/resources/mock_servicemap_response/testOutputs.resp" ,
			"src/test/resources/reference_results/testInnerLatLonDistinctFields.ref" ,
			"{"+
    		   "\"geoJsonFields\":[" +
    				"{ \"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\" }" +
    		   "]," + 
    		   "\"geoPointFields\":[" + 
    		   		"{\"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\"}" +
    		   "]," +
    		   "\"latitudeFields\":[ \"latitude/value\" ]," +
    		   "\"longitudeFields\":[ \"longitude/value\" ]" +
    		"}" ,
    		6
		);
	}
	
	@Test
	public void testInnerLatLonMissing() throws IOException {
		System.out.println( ""
				+ "\n######## " + testName() + " ########" );
		testInnerLatLon(
			"src/test/resources/mock_in_ff/testInnerLatLonMissing.ff" ,
			"src/test/resources/mock_servicemap_response/testOutputs.resp" ,
			"src/test/resources/reference_results/testInnerLatLonError.ref" ,
			"{"+
    		   "\"geoJsonFields\":[" +
    				"{ \"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\" }" +
    		   "]," + 
    		   "\"geoPointFields\":[" + 
    		   		"{\"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\"}" +
    		   "]," +
    		   "\"latitudeFields\":[ \"latitude/value\" ]," +
    		   "\"longitudeFields\":[ \"longitude/value\" ]" +
    		"}" ,
    		6
		);
	}
	
	@Test
	public void testInnerLatLonNotParsable() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		testInnerLatLon(
			"src/test/resources/mock_in_ff/testInnerLatLonErrorNotParsable.ff" ,
			"src/test/resources/mock_servicemap_response/testOutputs.resp" ,
			"src/test/resources/reference_results/testInnerLatLonError.ref" ,
			"{"+
    		   "\"geoJsonFields\":[" +
    				"{ \"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\" }" +
    		   "]," + 
    		   "\"geoPointFields\":[" + 
    		   		"{\"path\":\"location/value/coordinates\" , \"format\":\"lon , lat\"}" +
    		   "]," +
    		   "\"latitudeFields\":[ \"latitude/value\" ]," +
    		   "\"longitudeFields\":[ \"longitude/value\" ]" +
    		"}" ,
    		6
		);
	}
	
	// -------- Original FF augmentation tests --------
	@Test
	public void testOriginalFFAugmentation() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		testRunner.setProperty( EnrichData.OUTPUT_FF_CONTENT_FORMAT , EnrichData.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ); // JsonObject
    	testRunner.setProperty( EnrichData.ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG ,
    							"{\"format\":\"Service/features/properties/format\",\"err\":\"Service/not/present\"}" );
    	
    	testRunner.assertValid();
    	validateProcessorProperties();
    	ValidationResult vr = getValidationResult( EnrichData.ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG );
    	System.out.println("Validation of '" + EnrichData.ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG.getDisplayName() + "':\n\t" + vr.toString() );
    	
    	enqueueFlowFile( "src/test/resources/mock_in_ff/testOutputs.ff" );
    	
    	testRunner.run();
    	
    	testRunner.assertTransferCount( EnrichData.SUCCESS_RELATIONSHIP , 1 );
    	testRunner.assertTransferCount( EnrichData.ORIGINAL_RELATIONSHIP , 1 );
    	
    	MockFlowFile originalFF = testRunner.getFlowFilesForRelationship( EnrichData.ORIGINAL_RELATIONSHIP ).get(0);
    	originalFF.assertAttributeExists( "format" );
    	originalFF.assertAttributeEquals( "format" , "json" );
	}
	
}
