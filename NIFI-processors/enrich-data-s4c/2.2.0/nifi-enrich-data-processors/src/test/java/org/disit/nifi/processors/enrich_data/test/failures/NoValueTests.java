package org.disit.nifi.processors.enrich_data.test.failures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.nifi.util.MockFlowFile;
import org.disit.nifi.processors.enrich_data.EnrichDataConstants;
import org.disit.nifi.processors.enrich_data.EnrichDataProperties;
import org.disit.nifi.processors.enrich_data.EnrichDataRelationships;
import org.disit.nifi.processors.enrich_data.enricher.EnrichUtils;
import org.disit.nifi.processors.enrich_data.output_producer.OutputProducer;
import org.disit.nifi.processors.enrich_data.test.EnrichDataTestBase;
import org.disit.nifi.processors.enrich_data.test.TestUtils;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NoValueTests extends EnrichDataTestBase {
	
	@Test
	public void testNoValue() throws Exception{
		System.out.println( "\n######## " + testName() + " ########" );

		testRunner.setProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD , "24 h" );
		testRunner.setProperty( EnrichDataProperties.ATTEMPT_STRING_VALUES_PARSING , "Yes" );
		
		addServicemapResource( "id" , serviceUriPrefix , 
				"src/test/resources/mock_in_ff/testNoValue2.ff" , 
				"src/test/resources/mock_servicemap_response/testOutputs.resp" );

		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testNoValue2.ff" );
		
		// Set split json as output mode
		testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , 
								EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_SPLIT_JSON);
		// Add mock devices state relationship connection to produce device state
		testRunner.addConnection( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		testRunner.run();
		
		List<MockFlowFile> successFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP );
		List<MockFlowFile> originalFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.ORIGINAL_RELATIONSHIP );
		List<MockFlowFile> devicesStateFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		List<MockFlowFile> failureFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.FAILURE_RELATIONSHIP );
		List<MockFlowFile> retryFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.RETRY_RELATIONSHIP );
		
		// Print flow files and verify
		System.out.println( "\n-------- SUCCESS FFs: " + successFFs.size() + " --------");
		successFFs.stream().forEach( (MockFlowFile ff) -> System.out.println( TestUtils.prettyOutFF(ff) ) );
		
		for( MockFlowFile ff : successFFs ) {
			String noValueAttr = ff.getAttribute( EnrichDataConstants.NO_VALUE_ATTRIBUTE_NAME );
			assertNotEquals( null , noValueAttr );
			assertEquals( true , Boolean.parseBoolean(noValueAttr) );
		}
		
		System.out.println( "\n-------- ORIGINAL FFs: " + originalFFs.size() + " --------" );
		originalFFs.stream().forEach( (MockFlowFile ff) -> System.out.println( TestUtils.prettyOutFF(ff) ) );
		System.out.println( "\n-------- FAILURE FFs: " + failureFFs.size() + " --------");
		failureFFs.stream().forEach( (MockFlowFile ff) -> System.out.println( TestUtils.prettyOutFF(ff) ) );
		System.out.println( "\n-------- RETRY FFs: " + retryFFs.size() + " --------");
		retryFFs.stream().forEach( (MockFlowFile ff) -> System.out.println( TestUtils.prettyOutFF(ff) ) );
		
		System.out.println( "\n-------- DEVICES STATE FFs: " + devicesStateFFs.size() + " --------");
		devicesStateFFs.stream().forEach( (MockFlowFile ff) -> System.out.println( TestUtils.prettyOutFF(ff) ) );
		
		for( MockFlowFile ff : devicesStateFFs ) {
			String noValueAttr = ff.getAttribute( EnrichDataConstants.NO_VALUE_ATTRIBUTE_NAME );
			assertNotEquals( null , noValueAttr );
			assertEquals( true , Boolean.parseBoolean(noValueAttr) );
		}
	}
	
	@Test
	public void testNoValueMixed() throws Exception {
		System.out.println( "\n######## " + testName() + " ########" );

		testRunner.setProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD , "24 h" );
		testRunner.setProperty( EnrichDataProperties.ATTEMPT_STRING_VALUES_PARSING , "Yes" );
		
		addServicemapResource( "id" , serviceUriPrefix , 
				"src/test/resources/mock_in_ff/testNoValue.ff" , 
				"src/test/resources/mock_servicemap_response/testOutputs.resp" );

		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testNoValue.ff" );

		// Set split json as output mode
		testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_SPLIT_JSON );
		// Add mock devices state relationship connection to produce device state
		testRunner.addConnection( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		testRunner.run();

		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 8 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.RETRY_RELATIONSHIP , 0 );
		testRunner.assertTransferCount( EnrichDataRelationships.FAILURE_RELATIONSHIP , 0 );
		
		List<MockFlowFile> successFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP );
		List<MockFlowFile> originalFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.ORIGINAL_RELATIONSHIP );
		List<MockFlowFile> devicesStateFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		List<MockFlowFile> failureFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.FAILURE_RELATIONSHIP );
		List<MockFlowFile> retryFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.RETRY_RELATIONSHIP );
		
		System.out.println( "\n-------- SUCCESS FFs: " + successFFs.size() + " --------");
		for( MockFlowFile ff : successFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
			
			JsonElement ffContentEl = JsonParser.parseString( ff.getContent() );
			assertEquals( true , ffContentEl.isJsonObject() );
			JsonObject ffContentObj = ffContentEl.getAsJsonObject();
			String noValueAttr = ff.getAttribute( EnrichDataConstants.NO_VALUE_ATTRIBUTE_NAME );
			assertNotEquals( null , noValueAttr );
			assertEquals( true , Boolean.parseBoolean( noValueAttr ) );
			
		}
		
		System.out.println( "\n-------- ORIGINAL FFs: " + originalFFs.size() + " --------" );
		for( MockFlowFile ff : originalFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		}
		
		System.out.println( "\n-------- FAILURE FFs: " + failureFFs.size() + " --------");
		for( MockFlowFile ff : failureFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		}
		
		System.out.println( "\n-------- RETRY FFs: " + retryFFs.size() + " --------");
		for( MockFlowFile ff : retryFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		}
		
		System.out.println( "\n-------- DEVICES STATE FFs: " + devicesStateFFs.size() + " --------");
		for( MockFlowFile ff : devicesStateFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		}
		
		// Check success ffs
		for( MockFlowFile ff : successFFs ) {
			JsonElement contentEl = JsonParser.parseString( ff.getContent() );
			assertEquals( contentEl.isJsonObject() , true );
			assertNotEquals( ff.getAttribute( "value_name") , null );
			String valueName = ff.getAttribute( "value_name" );
			
			JsonObject contentObj = contentEl.getAsJsonObject();
			
			switch ( valueName ) {
			case "BooleanTestAttr":
				assertNoValuePresent( contentObj );
				break;
			case "PM10":
				assertEmptyValue( contentObj );
				break;
			case "PM2.5":
				assertEmptyValue( contentObj );
				break;
			case "address":
				assertNoValuePresent( contentObj );
				break;
			case "dateObserved":
				assertNullValuePresent( contentObj );
				break;
			case "location":
//				assertNoValuePresent( contentObj );
				assertEmptyValue( contentObj );
				break;
			case "reliability":
				assertNoValuePresent( contentObj );
				break;
			case "source":
				assertNoValuePresent( contentObj );
				break;
			default:
				System.out.println( "[WARN] No check defined for value_name='" + valueName + "'" );
			}
		}
		
		// Check devices state ffs
		JsonElement contentEl = JsonParser.parseString( devicesStateFFs.get(0).getContent() );
		assertEquals( contentEl.isJsonObject() , true );
		JsonObject contentObj = contentEl.getAsJsonObject();
		
		assertEmptyProperty( "BooleanTestAttr" , contentObj );
		assertEmptyProperty( "PM10/value_str" , contentObj );
		assertEmptyProperty( "PM2.5/value_obj" , contentObj );
		assertEmptyProperty( "address" , contentObj );
		assertNullProperty( "dateObserved/value" , contentObj );
		assertEmptyProperty( "location/value" , contentObj );
		assertEmptyProperty( "reliability" , contentObj );
		assertEmptyProperty( "source" , contentObj );
		
		String noValueAttr = devicesStateFFs.get(0).getAttribute( EnrichDataConstants.NO_VALUE_ATTRIBUTE_NAME );
		assertNotEquals( null , noValueAttr );
		assertEquals( true , Boolean.parseBoolean( noValueAttr ) );
	}
	
	private void assertEmptyProperty( String path , JsonObject contentObj ) {
		assertProperty( path , contentObj , 
			(JsonElement lastEl) -> {
				if( lastEl.isJsonObject() )
					assertEquals( 0 , lastEl.getAsJsonObject().entrySet().size() );
				else if( lastEl.isJsonArray() )
					assertEquals( 0 , lastEl.getAsJsonArray().size() );
				else
					assertEquals( true , lastEl.getAsString().isEmpty() );
			}
		);
	}
	
	private void assertNullProperty( String path , JsonObject contentObj ) {
		assertProperty( path , contentObj , 
			(JsonElement lastEl) -> {
				assertEquals( true , lastEl.isJsonNull() );
			}
		);
	}
	
	private void assertProperty( String path , JsonObject contentObj , Consumer<JsonElement> lastElConsumer ) {
		List<String> pathList = Arrays.asList( path.split("/") ).stream()
				.map( (String s) -> s.trim() )
				.collect( Collectors.toList() );
			
		JsonObject curObj = contentObj;
		for( int i=0 ; i < pathList.size() ; i++ ) {
			String elName = pathList.get(i);
			assertEquals( curObj.has( elName ) , true );
			JsonElement nextEl = curObj.get( elName );
			if( i == pathList.size() - 1 ) {
				// check final path element
				lastElConsumer.accept( nextEl );
			} else {
				// next path element
				assertEquals( true , nextEl.isJsonObject() );
				curObj = nextEl.getAsJsonObject();
			}
		}
	}
	
	private void assertNullValuePresent( JsonObject contentObj ) {
		boolean nullValueFound = false;
		if( contentObj.has( "value" ) ) {
			assertEquals( true , contentObj.get( "value" ).isJsonNull() );
			nullValueFound = true;
		}
		if( contentObj.has( "value_str" ) ) {
			assertEquals( true , contentObj.get( "value_str" ).isJsonNull() );
			nullValueFound = true;
		}
		if( contentObj.has( "value_obj" ) ) {
			assertEquals( true , contentObj.get( "value_obj" ).isJsonNull() );
			nullValueFound = true;
		}
		if( contentObj.has( "value_json_str" ) ) {
			assertEquals( true , contentObj.get( "value_json_str" ).isJsonNull() );
			nullValueFound = true;
		}
		assertEquals( true , nullValueFound );
	}
	
	private void assertNoValuePresent( JsonObject contentObj ) {
		assertEquals( false , contentObj.has( "value" ) );
		assertEquals( false , contentObj.has( "value_str" ) );
		assertEquals( false , contentObj.has( "value_obj" ) );
		assertEquals( false , contentObj.has( "value_json_str" ) );
	}
	
	private void assertEmptyValue( JsonObject contentObj ) {
		boolean emptyValueFound = false;
		if( contentObj.has( "value" ) && contentObj.get( "value" ).isJsonArray() ) {
			JsonArray valueArr = contentObj.get( "value" ).getAsJsonArray();
			if( valueArr.size() == 0 )
				emptyValueFound = true; 
		}
		
		if( contentObj.has( "value_str" ) ) {
			String valueStr = contentObj.get( "value_str" ).getAsString();
			if( valueStr.isEmpty() )
				emptyValueFound = true;
		}
		if( contentObj.has( "value_obj" ) ) {
			JsonElement valueObj = contentObj.get( "value_obj" );
			if( valueObj.isJsonObject() ) {
				if( valueObj.getAsJsonObject().entrySet().isEmpty() )
					emptyValueFound = true;
			}
		}
		if( contentObj.has( "value_json_str" ) ) {
			String valueJsonStr = contentObj.get( "value_json_str" ).getAsString();
			if( valueJsonStr.isEmpty() )
				emptyValueFound = true;
		}
		assertEquals( true , emptyValueFound );
	}
	
	@Test
	public void testNoValuePartial() throws Exception {
		System.out.println( "\n######## " + testName() + " ########" );

		testRunner.setProperty( EnrichDataProperties.TIMESTAMP_THRESHOLD , "24 h" );
		testRunner.setProperty( EnrichDataProperties.ATTEMPT_STRING_VALUES_PARSING , "Yes" );
		
		addServicemapResource( "id" , serviceUriPrefix , 
				"src/test/resources/mock_in_ff/testNoValue.ff" , 
				"src/test/resources/mock_servicemap_response/testOutputs.resp" );

		MockFlowFile inputFF = enqueueFlowFile( "src/test/resources/mock_in_ff/testNoValuePartial.ff" );

		// Set split json as output mode
		testRunner.setProperty( EnrichDataProperties.OUTPUT_FF_CONTENT_FORMAT , EnrichDataConstants.OUTPUT_FF_CONTENT_FORMAT_SPLIT_JSON );
		// Add mock devices state relationship connection to produce device state
		testRunner.addConnection( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		testRunner.run();

		testRunner.assertTransferCount( EnrichDataRelationships.SUCCESS_RELATIONSHIP , 8 );
		testRunner.assertTransferCount( EnrichDataRelationships.ORIGINAL_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP , 1 );
		testRunner.assertTransferCount( EnrichDataRelationships.RETRY_RELATIONSHIP , 0 );
		testRunner.assertTransferCount( EnrichDataRelationships.FAILURE_RELATIONSHIP , 0 );
		
		List<MockFlowFile> successFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.SUCCESS_RELATIONSHIP );
		List<MockFlowFile> originalFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.ORIGINAL_RELATIONSHIP );
		List<MockFlowFile> devicesStateFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.DEVICE_STATE_RELATIONSHIP );
		List<MockFlowFile> failureFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.FAILURE_RELATIONSHIP );
		List<MockFlowFile> retryFFs = testRunner.getFlowFilesForRelationship( EnrichDataRelationships.RETRY_RELATIONSHIP );
		
		System.out.println( "\n-------- SUCCESS FFs: " + successFFs.size() + " --------");
		for( MockFlowFile ff : successFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
			
			JsonElement ffContentEl = JsonParser.parseString( ff.getContent() );
			assertEquals( true , ffContentEl.isJsonObject() );
			JsonObject ffContentObj = ffContentEl.getAsJsonObject();
			String noValueAttr = ff.getAttribute( EnrichDataConstants.NO_VALUE_ATTRIBUTE_NAME );
			assertEquals( null , noValueAttr );			
		}
		
		System.out.println( "\n-------- ORIGINAL FFs: " + originalFFs.size() + " --------" );
		for( MockFlowFile ff : originalFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		}
		
		System.out.println( "\n-------- FAILURE FFs: " + failureFFs.size() + " --------");
		for( MockFlowFile ff : failureFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		}
		
		System.out.println( "\n-------- RETRY FFs: " + retryFFs.size() + " --------");
		for( MockFlowFile ff : retryFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		}
		
		System.out.println( "\n-------- DEVICES STATE FFs: " + devicesStateFFs.size() + " --------");
		for( MockFlowFile ff : devicesStateFFs ) {
			System.out.println( TestUtils.prettyOutFF( ff ) );
		}
	}
	
}
