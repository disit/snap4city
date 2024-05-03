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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.util.MockFlowFile;
import org.disit.nifi.processors.enrich_data.EnrichDataConstants;
import org.disit.nifi.processors.enrich_data.EnrichDataProperties;
import org.disit.nifi.processors.enrich_data.EnrichDataRelationships;
import org.disit.nifi.processors.enrich_data.test.EnrichDataTestBase;
import org.disit.nifi.processors.enrich_data.test.TestUtils;
import org.junit.Test;

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
		
		testRunner.setProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD , "24 h" );
		
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testOutputs.ff" );
		
		JsonElement expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testOutputs_jsonOut.ref" , 
			inputFF );

		testRunner.run();
		
		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		
		MockFlowFile outFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP ).get(0);
		JsonElement outFFContent = JsonParser.parseString( new String( outFF.toByteArray() ) ); 
		assertEquals( true , outFFContent.isJsonObject() );
		// entry_date
		outFFContent.getAsJsonObject().keySet().stream().forEach( (String prop) -> {
			TestUtils.fixJsonOutputAttribute( 
				outFFContent , expectedResult , 
				prop , EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME );
		});
//		System.out.println( expectedResult.toString() );
		
		assertEquals( true , outFFContent.getAsJsonObject().equals( expectedResult.getAsJsonObject() ) );
		System.out.println( TestUtils.prettyOutFF( outFF ) );
	}
	
	@Test
	public void testSplitJsonOutput() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		
		testRunner.setProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD , "24 h" );
		
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testOutputs.ff" );
		
		JsonArray expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testOutputs_splitJson.ref" , 
			inputFF ).getAsJsonArray();
		
		// set "Split JSON" as output format
		testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] );
		testRunner.assertValid();
		testRunner.run();
		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 8 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		
		System.out.println( "-------- Success FFs: --------" );
		List<MockFlowFile> successFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP );
		for( int i=0 ; i < successFFs.size() ; i++ ) {
			JsonElement content = JsonParser.parseString( new String( successFFs.get(i).toByteArray() ) );
			assertEquals( true , content.isJsonObject() );
			TestUtils.fixSplitJsonAttribute( content , expectedResult , i , EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME );
			assertEquals( true , expectedResult.get(i).equals(content.getAsJsonObject() ) );
			System.out.println( TestUtils.prettyOutFF( successFFs.get(i) ) );
		}
		
		System.out.println( "-------- Original FFs: --------" );
		List<MockFlowFile> originalFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.ORIGINAL_RELATIONSHIP );
		for( int i=0 ; i<originalFFs.size() ; i++ ) {
			System.out.println( TestUtils.prettyOutFF( originalFFs.get(i) ) );
		}
	}
	
	@Test
	public void testEmptyOutObj() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testEmptyOutObj.ff" , 
			"src/test/resources/mock_servicemap_response/testEmptyOutObj.resp" );
		
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testEmptyOutObj.ff" );
		
		testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ); // Json Object
		testRunner.run();
		testRunner.assertTransferCount( EnrichDataRelationships.FAILURE_RELATIONSHIP , 1 );
		
		MockFlowFile failedFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.FAILURE_RELATIONSHIP ).get(0);
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
		
		testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] ); // Split JSON
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 14 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		
		JsonArray expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testOutputs_arrayValue.ref" , 
			inputFF ).getAsJsonArray();
		List<MockFlowFile> successFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP );
		for( int i=0 ; i < successFFs.size() ; i++ ) {
			MockFlowFile ff = successFFs.get(i);
			JsonElement content = JsonParser.parseString( new String( ff.toByteArray() ) );
			assertEquals( true , content.isJsonObject() );
			TestUtils.fixSplitJsonAttribute( content , expectedResult , i , EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME );
			System.out.println( TestUtils.prettyOutFF( ff ) );
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
		
		testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ); // JSON Object
		testRunner.setProperty( EnrichDataProperties.ATTEMPT_STRING_VALUES_PARSING , "Yes" );
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		
		JsonObject expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testOutputs_parseStringValue.ref" , 
			inputFF ).getAsJsonObject();

		JsonElement outputContent = JsonParser.parseString( 
			new String( testRunner.getFlowFilesForRelationship(EnrichDataRelationships.SUCCESS_RELATIONSHIP)
								  .get(0).toByteArray() ) 
		);
		assertEquals( true , outputContent.isJsonObject() );
		outputContent.getAsJsonObject().keySet().stream().forEach( (String prop) -> {
			TestUtils.fixJsonOutputAttribute(outputContent, expectedResult, prop, EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME);
		});
		assertEquals( true , expectedResult.equals( outputContent.getAsJsonObject() ) );
	}
	
	// -------- Coordinates Tests --------
	protected void testInnerLatLon( String inputFlowFilePath , String responseFilePath , String referenceFilePath , String innerLatLonConfig ,
									int expectedSuccessFFCount ) throws IOException {
		addServicemapResource( "id" , serviceUriPrefix , inputFlowFilePath , responseFilePath );
			
		MockFlowFile inputFF = enqueueFlowFile( inputFlowFilePath );
		
		testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[2] ); // Split Json
		testRunner.setProperty( EnrichDataProperties.LATLON_PRIORITY , EnrichDataConstants.LATLON_PRIORITY_VALUES[1] ); // Priority inner
		testRunner.setProperty( EnrichDataProperties.INNER_LAT_LON_CONFIG , innerLatLonConfig );
		
		ValidationResult vr = getValidationResult( EnrichDataProperties.INNER_LAT_LON_CONFIG );
		System.out.println( "Validation: \n" + vr.toString() );
		validateProcessorProperties();
		
		testRunner.run();
		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , expectedSuccessFFCount );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		
		JsonArray expectedResult = TestUtils.prepareExpectedResult( referenceFilePath , inputFF )
											.getAsJsonArray();
		List<MockFlowFile> successFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP );
		for( int i=0 ; i < successFFs.size() ; i++ ) {
			JsonElement content = JsonParser.parseString( new String( successFFs.get(i).toByteArray() ) );
			assertEquals( true , content.isJsonObject() );
			TestUtils.fixSplitJsonAttribute(content, expectedResult, i, EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME);
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
		
		testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_VALUES[0] ); // JsonObject
    	testRunner.setProperty( EnrichDataProperties.ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG ,
    							"{\"format\":\"Service/features/properties/format\",\"err\":\"Service/not/present\"}" );
    	
    	testRunner.assertValid();
    	validateProcessorProperties();
    	ValidationResult vr = getValidationResult( EnrichDataProperties.ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG );
    	System.out.println("Validation of '" + EnrichDataProperties.ORIGINAL_FLOW_FILE_ATTRIBUTES_AUG.getDisplayName() + "':\n\t" + vr.toString() );
    	
    	enqueueFlowFile( "src/test/resources/mock_in_ff/testOutputs.ff" );
    	
    	testRunner.run();
    	
    	testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 1 );
    	testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
    	
    	MockFlowFile originalFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.ORIGINAL_RELATIONSHIP ).get(0);
    	originalFF.assertAttributeExists( "format" );
    	originalFF.assertAttributeEquals( "format" , "json" );
    	System.out.println( TestUtils.prettyOutFF( originalFF ) );
	}
	
	// -------- Test extract attributes tests --------
	@Test
	public void testExtractAttributes() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		
		testRunner.addConnection( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		
		testRunner.setProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD , "24 h" );
		
		List<String> extractAttributes = new ArrayList<>();
		extractAttributes.add( "Service/features/properties/ownership" );
		extractAttributes.add( "Service/features/properties/organization" );
		extractAttributes.add( "Service/features/properties/model" );
		
		testRunner.setProperty( 
			EnrichDataProperties.EXTRACT_ENRICHMENT_ATTRIBUTES , 
			extractAttributes.stream().reduce( (String s1, String s2) -> { return s1+","+s2; } )
							 .get()
		);
		
		addServicemapResource( "id" , serviceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testOutputs.ff" );
		
		JsonElement expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testOutputs_jsonOut.ref" , 
			inputFF );

		testRunner.run();
		
		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP , 1 );
		
		// SUCCESS
		MockFlowFile outFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP ).get(0);
		JsonElement outFFContent = JsonParser.parseString( new String( outFF.toByteArray() ) ); 
		assertEquals( true , outFFContent.isJsonObject() );
		// entry_date
		outFFContent.getAsJsonObject().keySet().stream().forEach( (String prop) -> {
			TestUtils.fixJsonOutputAttribute( 
				outFFContent , expectedResult , 
				prop , EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME );
		});
//		System.out.println( expectedResult.toString() );
		assertEquals( true , outFFContent.getAsJsonObject().equals( expectedResult.getAsJsonObject() ) );
		extractAttributes.stream().forEach( (String path) -> { 
			String attrName = path.replace( "/" , "." );
			String attrValue = outFF.getAttribute( attrName );
			assertEquals( true , attrValue != null );
		});
		System.out.println( "======== SUCCESS ========" );
		System.out.println( TestUtils.prettyOutFF( outFF ) );
		
		// DEVICE STATE
		MockFlowFile deviceStateFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP ).get(0);
		JsonElement deviceStateFFContent = JsonParser.parseString( new String( deviceStateFF.toByteArray() ) );
		assertEquals( true , deviceStateFFContent.isJsonObject() );
		System.out.println( "======== DEVICE STATE ========" );
		System.out.println( TestUtils.prettyOutFF( deviceStateFF ) );
	}
	
	// -------- deviceId and serviceUriPrefix from attributes tests --------
	@Test
	public void testDeviceIdFromAttribute() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		testRunner.addConnection( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		testRunner.setProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD , "24 h" );
		
		testRunner.setProperty( EnrichDataProperties.DEVICE_ID_FROM_ATTRIBUTE , "deviceIdAttribute" );
		
		String customDeviceId = "device001";
		
		addServicemapResource( customDeviceId , serviceUriPrefix , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		Map<String,String> flowFileAttributes = new HashMap<>();
		flowFileAttributes.put( "deviceIdAttribute" , customDeviceId );
		
		MockFlowFile inputFF = enqueueFlowFile( 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			flowFileAttributes );
		JsonElement expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testDeviceIdFromAttribute_jsonOut.ref" , 
			inputFF );
		
		testRunner.run();
		
		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		
		// SUCCESS
		MockFlowFile outFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP ).get(0);
		JsonElement outFFContent = JsonParser.parseString( new String(outFF.toByteArray()) );
		assertEquals( true , outFFContent.isJsonObject() );
		outFFContent.getAsJsonObject().keySet().stream().forEach( (String prop) -> {
			TestUtils.fixJsonOutputAttribute( 
				outFFContent , expectedResult , 
				prop, EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME );
		});
		assertEquals( true , outFFContent.getAsJsonObject().equals( expectedResult.getAsJsonObject() ) );
		System.out.println( "======== SUCCESS ========" );
		System.out.println( TestUtils.prettyOutFF( outFF )  );
		
		// DEVICE STATE
		MockFlowFile deviceStateFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP ).get(0);
		JsonElement deviceStateFFContent = JsonParser.parseString( new String(deviceStateFF.toByteArray()) );
		assertEquals( true , deviceStateFFContent.isJsonObject() );
		System.out.println( "======== DEVICE STATE ========" );
		System.out.println( TestUtils.prettyOutFF( deviceStateFF ) );
	}
	
	@Test
	public void testServiceUriPrefixFromAttribute() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		testRunner.addConnection( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		testRunner.setProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD , "24 h" );
		
		testRunner.setProperty( EnrichDataProperties.SERVICE_URI_PREFIX_FROM_ATTRIBUTE , "serviceUriPrefix" );
		
		String customServiceUriPrefix = "http://customserviceuriprefix.org";
		
		addServicemapResource( "id" , customServiceUriPrefix , 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );

		Map<String,String> flowFileAttributes = new HashMap<>();
		flowFileAttributes.put( "serviceUriPrefix" , customServiceUriPrefix );
		
		MockFlowFile inputFF = enqueueFlowFile( 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			flowFileAttributes );
		JsonElement expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testServiceUriPrefixFromAttribute_jsonOut.ref" , 
			inputFF );
		
		testRunner.run();
		
		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP , 1 );
		
		// SUCCESS
		MockFlowFile outFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP ).get(0);
		JsonElement outFFContent = JsonParser.parseString( new String(outFF.toByteArray()) );
		assertEquals( true , outFFContent.isJsonObject() );
		outFFContent.getAsJsonObject().keySet().stream().forEach( (String prop) -> {
			TestUtils.fixJsonOutputAttribute( 
				outFFContent , expectedResult , 
				prop, EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME );
		});
		assertEquals( true , outFFContent.getAsJsonObject().equals( expectedResult.getAsJsonObject() ) );
		System.out.println( "======== SUCCESS ========" );
		System.out.println( TestUtils.prettyOutFF( outFF )  );
		
		// DEVICE STATE
		MockFlowFile deviceStateFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP ).get(0);
		JsonElement deviceStateFFContent = JsonParser.parseString( new String(deviceStateFF.toByteArray()) );
		assertEquals( true , deviceStateFFContent.isJsonObject() );
		System.out.println( "======== DEVICE STATE ========" );
		System.out.println( TestUtils.prettyOutFF( deviceStateFF ) );
	}
	
	@Test
	public void testServiceUriPrefixAndDeviceIdFromAttribute() throws IOException {
		System.out.println( "\n######## " + testName() + " ########" );
		testRunner.addConnection( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		testRunner.setProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD , "24 h" );
		
		testRunner.setProperty( EnrichDataProperties.SERVICE_URI_PREFIX_FROM_ATTRIBUTE , "serviceUriPrefix" );
		testRunner.setProperty( EnrichDataProperties.DEVICE_ID_FROM_ATTRIBUTE , "deviceIdAttribute" );
		
		String customServiceUriPrefix = "http://customserviceuriprefix.org";
		String customDeviceId = "device001";

		addServicemapResource( customDeviceId , customServiceUriPrefix , 
			"src/test/resources/mock_servicemap_response/testOutputs.resp" );
		
		Map<String,String> flowFileAttributes = new HashMap<>();
		flowFileAttributes.put( "deviceIdAttribute" , customDeviceId );
		flowFileAttributes.put( "serviceUriPrefix" , customServiceUriPrefix );
		
		MockFlowFile inputFF = enqueueFlowFile( 
			"src/test/resources/mock_in_ff/testOutputs.ff" , 
			flowFileAttributes );
		JsonElement expectedResult = TestUtils.prepareExpectedResult( 
			"src/test/resources/reference_results/testServiceUriPrefixAndDeviceIdFromAttribute_jsonOut.ref" , 
			inputFF );
		
		testRunner.run();
		
		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP , 1 );
		
		// SUCCESS
		MockFlowFile outFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP ).get(0);
		JsonElement outFFContent = JsonParser.parseString( new String(outFF.toByteArray()) );
		assertEquals( true , outFFContent.isJsonObject() );
		outFFContent.getAsJsonObject().keySet().stream().forEach( (String prop) -> {
			TestUtils.fixJsonOutputAttribute( 
				outFFContent , expectedResult , 
				prop, EnrichDataConstants.ENTRY_DATE_ATTRIBUTE_NAME );
		});
		assertEquals( true , outFFContent.getAsJsonObject().equals( expectedResult.getAsJsonObject() ) );
		System.out.println( "======== SUCCESS ========" );
		System.out.println( TestUtils.prettyOutFF( outFF )  );
		
		// DEVICE STATE
		MockFlowFile deviceStateFF = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP ).get(0);
		JsonElement deviceStateFFContent = JsonParser.parseString( new String(deviceStateFF.toByteArray()) );
		assertEquals( true , deviceStateFFContent.isJsonObject() );
		System.out.println( "======== DEVICE STATE ========" );
		System.out.println( TestUtils.prettyOutFF( deviceStateFF ) );
	}
}
