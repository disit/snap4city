/*
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
package org.disit.nifi.processors.enrich_data.web.api.tester;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ControllerService;
import org.apache.nifi.nar.NarClassLoader;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.LogMessage;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.disit.nifi.processors.enrich_data.EnrichData;
import org.disit.nifi.processors.enrich_data.EnrichDataProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

public class EnrichDataWebTester {

	private static final Logger logger = LoggerFactory.getLogger( EnrichDataWebTester.class );
	
	private String processorId;
	private String groupId;
	
	private TestRunner testRunner;
	
	public EnrichDataWebTester( String processorId , String groupId , 
								JsonObject propertiesConfig , Map<String , String> registryVariables) {
		
		this.processorId = processorId;
		this.groupId = groupId;
	 
		try {
			configureTestRunner( propertiesConfig , registryVariables );
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException
				| InitializationException e) {
			logger.error( ExceptionUtils.getStackTrace(e) );
		}
		
	}
	
	/**
	 * Configure the test runner.
	 * 
	 * @param processorPropertiesConfig The properties for the processor.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InitializationException
	 */
	private void configureTestRunner( JsonObject processorPropertiesConfig , Map<String , String> registryVariables ) throws IOException , ClassNotFoundException, InstantiationException, IllegalAccessException, InitializationException{
		
		this.testRunner = TestRunners.newTestRunner( EnrichData.class );
		
		// Mimic env variables on test runner
		// Do this and registry variables before setting anything in the test runner!
		System.getenv().forEach( (String name, String value) -> { 
			this.testRunner.setVariable( name , value );
		});
		
		// Mimic registry variable on test runner
		if( registryVariables != null ) {
			registryVariables.forEach( (String name , String value )->{
				this.testRunner.setVariable( name , value );
			});
		}
		
		// Enrichment source client service
		String enrichmentSourceId = registerControllerService( 
			processorPropertiesConfig.get( EnrichDataProperties.ENRICHMENT_SOURCE_CLIENT_SERVICE.getName() ).getAsJsonObject() ,
			EnrichDataProperties.ENRICHMENT_SOURCE_CLIENT_SERVICE.getControllerServiceDefinition()
		);
		this.testRunner.enableControllerService( this.testRunner.getControllerService( enrichmentSourceId ) );
		this.testRunner.setProperty(EnrichDataProperties.ENRICHMENT_SOURCE_CLIENT_SERVICE.getName() , enrichmentSourceId);
		
		// Enrichment resource locator service
		if( processorPropertiesConfig.has( EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE.getName() ) && 
			!processorPropertiesConfig.get( EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE.getName() ).isJsonNull() &&
			processorPropertiesConfig.get( EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE.getName() ).isJsonObject() ) {
			
			String resourceLocatorId = registerControllerService( 
				processorPropertiesConfig.get( EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE.getName() ).getAsJsonObject() ,
				EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE.getControllerServiceDefinition() 
			);
			this.testRunner.enableControllerService( this.testRunner.getControllerService( resourceLocatorId ) );
			this.testRunner.setProperty( EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE , resourceLocatorId );
		}
		
		// Ownership client service
		if( processorPropertiesConfig.has( EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE.getName() ) && 
			!processorPropertiesConfig.get( EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE.getName() ).isJsonNull()  && 
			processorPropertiesConfig.get( EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE.getName() ).isJsonObject() ) {
			
			String ownershipSourceId = registerControllerService( 
				processorPropertiesConfig.get( EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE.getName() ).getAsJsonObject() ,
				EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE.getControllerServiceDefinition()
			);
			this.testRunner.enableControllerService( this.testRunner.getControllerService( ownershipSourceId ) );
			this.testRunner.setProperty( EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE.getName() , ownershipSourceId );
		}
		
		// Non-controller service properties
		for( String pName : processorPropertiesConfig.keySet() ) {
			if( !pName.equals( EnrichDataProperties.ENRICHMENT_SOURCE_CLIENT_SERVICE.getName() ) && 
				!pName.equals( EnrichDataProperties.OWNERSHIP_CLIENT_SERVICE.getName() ) &&
				!pName.equals( EnrichDataProperties.ENRICHMENT_RESOURCE_LOCATOR_SERVICE.getName() ) ) {
				
				if( !processorPropertiesConfig.get(pName).isJsonNull() )
					this.testRunner.setProperty( pName , processorPropertiesConfig.get(pName).getAsString() );
			}
		}
		
		this.testRunner.assertValid();
		
		//EnrichData processor = (EnrichData)this.testRunner.getProcessor();
	}
	
	/*
	 * Creates and register a controller service by loading the CS class from the 
	 * nar bundle passed in the csConfig parameter.
	 * 
	 * @param csConfig The configuration object for the controller service.
	 * @param apiClass The API class implemented by the controller service.
	 * @return The identifier of the controller service registered on the test runner.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InitializationException
	 */
	private String registerControllerService( JsonObject csConfig , Class<?> apiClass ) throws IOException , ClassNotFoundException, InstantiationException, IllegalAccessException, InitializationException{
		String id = csConfig.get("id").getAsString() + "_mock";
		if( this.testRunner.getControllerService( id ) != null )
			return id;
		
		Class<?> csImplClass = loadBundledNarClass( 
			csConfig.get("type").getAsString() ,
			csConfig.get("bundle").getAsJsonObject().get("artifact").getAsString() ,
			csConfig.get("bundle").getAsJsonObject().get("version").getAsString() ,
			(NarClassLoader) apiClass.getClassLoader()
		);
		
		ControllerService csInstance = (ControllerService) csImplClass.newInstance();
		
		this.testRunner.addControllerService( id , csInstance );
		
		JsonObject csProperties = csConfig.get("properties").getAsJsonObject();
		for( String pName : csProperties.keySet() ) {
			JsonElement pVal = csProperties.get( pName );
			if( pVal.isJsonObject() ) {
				Class<?> nestedApiClass = csInstance.getPropertyDescriptors().stream()
											.filter( (PropertyDescriptor d) -> {
												return d.getName().equals( pName );
											}).findFirst().get()
											.getControllerServiceDefinition();
				String nestedCsId = registerControllerService( pVal.getAsJsonObject() , nestedApiClass );
				if( !this.testRunner.isControllerServiceEnabled( this.testRunner.getControllerService( nestedCsId ) ) ) {
					this.testRunner.enableControllerService( this.testRunner.getControllerService( nestedCsId ) );
				}
				this.testRunner.setProperty( csInstance , pName , nestedCsId );	
			} else {
				if( !pVal.isJsonNull() )
					this.testRunner.setProperty( csInstance , pName , pVal.getAsString() );
			}
		}
		
		this.testRunner.assertValid( csInstance );
		
		return id;
	}
	
	/**
	 * Load a class from the specified nar bundle.
	 * 
	 * @param type Class type.
	 * @param artifact NAR artifact name.
	 * @param version  NAR artifact version.
	 * @param parentClassLoader The classloader wich will be the parent of the classloader for the loaded class.
	 * @return The loaded class. 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Class<?> loadBundledNarClass( String type , String artifact , String version , NarClassLoader parentClassLoader ) throws IOException , ClassNotFoundException{
		String narPath = new StringBuilder( ((NarClassLoader) parentClassLoader.getParent()).getWorkingDirectory().getParent() )
							.append( "/" ).append( artifact )
							.append( "-" ).append( version )
							.append( ".nar-unpacked" )
							.toString();
		
		File narFile = new File(narPath);
		if( !narFile.exists() )
			throw new IOException( String.format( "Nar file '%s' does not exists." ) );
		
		Class<?> loadedClass;
		NarClassLoader narClassLoader = new NarClassLoader( narFile , parentClassLoader );
		try {
			loadedClass = narClassLoader.loadClass( type );
		}catch( ClassNotFoundException e ) {
			narClassLoader.close();
			throw e;
		}
		
		return loadedClass;
	}
	
	/**
	 * Run a test usingthe configured test runner using the provided data and flow file attributes.
	 * @param data
	 * @param attributes
	 * @return
	 */
	public JsonObject runTest( String data , Map<String , String> attributes ) {
		
		this.testRunner.enqueue( data , attributes );		
		
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		this.testRunner.run( 1 , false );
		
		Set<Relationship> rels = this.testRunner.getProcessor().getRelationships();
		JsonObject response = new JsonObject();
		JsonObject responseRels = new JsonObject();
		for( Relationship rel : rels ) {
			List<MockFlowFile> ffs = this.testRunner.getFlowFilesForRelationship( rel );
			if( !ffs.isEmpty() ) {
				JsonArray relationshipFFs = new JsonArray();
				ffs.stream().map( (MockFlowFile ff) -> {
					return flowFileToJsonObject( ff );
				}).forEach( (JsonObject jsonFF) -> {
					relationshipFFs.add( jsonFF );
				});;
				
				responseRels.add( rel.getName() , relationshipFFs );
			}
		}
		response.add( "output" , responseRels );
		
		// Collect test runner component logs
		JsonObject responseLogs = new JsonObject();
		List<LogMessage> errors = this.testRunner.getLogger().getErrorMessages();
		if( !errors.isEmpty() ) {
			JsonArray errorsArr = new JsonArray();
			errors.stream().map( (LogMessage m) -> { return m.getMsg(); })
				  .forEach( (String m) -> { errorsArr.add(m); });
			responseLogs.add( "error" , errorsArr );
		};
		List<LogMessage> warnings = this.testRunner.getLogger().getWarnMessages();
		if( !warnings.isEmpty() ) {
			JsonArray warningsArr = new JsonArray();
			warnings.stream().map( (LogMessage m) -> { return m.getMsg(); })
				  .forEach( (String m) -> { warningsArr.add(m); });
			responseLogs.add( "warning" , warningsArr );
		};
		List<LogMessage> infos = this.testRunner.getLogger().getInfoMessages();
		if( !infos.isEmpty() ) {
			JsonArray infosArr = new JsonArray();
			infos.stream().map( (LogMessage m) -> { return m.getMsg(); })
				  .forEach( (String m) -> { infosArr.add(m); });
			responseLogs.add( "info" , infosArr );
		};
		List<LogMessage> debugs = this.testRunner.getLogger().getDebugMessages();
		if( !debugs.isEmpty() ) {
			JsonArray debugsArr = new JsonArray();
			debugs.stream().map( (LogMessage m) -> { return m.getMsg(); })
				  .forEach( (String m) -> { debugsArr.add(m); });
			responseLogs.add( "debug" , debugsArr );
		};
		List<LogMessage> traces = this.testRunner.getLogger().getTraceMessages();
		if( !traces.isEmpty() ) {
			JsonArray tracesArr = new JsonArray();
			traces.stream().map( (LogMessage m) -> { return m.getMsg(); })
				  .forEach( (String m) -> { tracesArr.add(m); });
			responseLogs.add( "trace" , tracesArr );
		};
		response.add( "logs" , responseLogs );
		
		return response;
	}
	
	/**
	 * Utility method to convert a MockFlowFile instance to a JsonObject
	 * @param ff
	 * @return A JsonObject representation of the flow file.
	 */
	private JsonObject flowFileToJsonObject( MockFlowFile ff ) {
		JsonObject ffAttributes = new JsonObject();
		ff.getAttributes().forEach( (String attrName , String attrValue ) -> {
			ffAttributes.addProperty( attrName , attrValue );
		});
		
		String ffContent = new String( ff.toByteArray() );
		
		JsonObject jsonFF = new JsonObject();
		jsonFF.add( "attributes" , ffAttributes );
		jsonFF.addProperty( "content" , ffContent );
		return jsonFF;
	}
	
}
