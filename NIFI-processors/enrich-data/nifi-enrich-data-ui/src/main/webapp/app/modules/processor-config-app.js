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

import { ProcessorServices } from "./processor-services.js";
import { ProcessorUIBuilder } from "./processor-ui.js";

export class ProcessorConfigApp {
	
	// Constructor
	constructor( paramsObj ){
		
		if( typeof paramsObj.processorId === typeof undefined || paramsObj.processorId == null ){
			throw "Undefined or null 'processorId' not allowed";
		}
		this.processorId = paramsObj.processorId;
		
		if( typeof paramsObj.revision === typeof undefined || paramsObj.revision == null ){
			throw "Undefined or null 'revision' not allowed";
		}
		this.revision = paramsObj.revision;
		
		if( typeof paramsObj.clientId === typeof undefined || paramsObj.clientId == null ){
			throw "Undefined or null 'clientId' not allowed";
		}
		this.clientId = paramsObj.clientId;
		
		if( typeof paramsObj.disconnectedNodeAcknowledged === typeof undefined || typeof paramsObj.disconnectedNodeAcknowledged == null ){
			throw "Undefined or null 'diconnectedNodeAcknowledged' not allowed."
		}
		this.disconnectedNodeAcknowledged = paramsObj.disconnectedNodeAcknowledged;
		
		if( typeof paramsObj.editable === typeof undefined || paramsObj.editable == null ){
			paramsObj.editable = false;
		}
		this.editable = paramsObj.editable;
		
		this.processGroupId = null;
		this.processorDetails = null;
		this.processorInfo = null;
		this.processGroupControllerServices = null;
		
		this.view = {
			jsonProperties: null,
			groupsConfig: null,
			ui: null
		};
	}
	
	// Configure a configuration group. The specified group properties 
	// will be grouped in a tab named as the group name in the configuration ui.
	configureGroup( groupName , groupProperties ){
		if( this.view.groupsConfig == null ){
			this.view.groupsConfig = {};
		}
		this.view.groupsConfig[groupName] = groupProperties;
		return this;
	}
	
	// Set the property with the given property name as a jsonProperty.
	// This allows to use a json editor as input component in the configuration ui
	//  for such property.
	configureJsonProperty( propertyName ){
		if( this.view.jsonProperties == null ){
			this.view.jsonProperties = [];
		}
		this.view.jsonProperties.push( propertyName );
		return this;
	}
	
	// Initialize the processor configuration app
	init(){
		var app = this;
		
		var processorDetailsReq = ProcessorServices.getProcessorDetails(app.processorId);
		var processorInfoReq = ProcessorServices.getProcessorInfo(app.processorId);
	
		$.when( processorDetailsReq , processorInfoReq ).done(
			function (processorDetailsResult , processorInfoResult){
				// store processGroupId	
				app.processGroupId = processorInfoResult[0].status.groupId;
				app.processorDetails = processorDetailsResult[0];
				app.processorInfo = processorInfoResult[0];
				
				// store controller service properties names 
				app.controllerServiceProperties = [];
				$.each( app.processorDetails.descriptors , function (i , descriptor){
					if( typeof descriptor.controllerService !== typeof undefined && descriptor.controllerService )
						app.controllerServiceProperties.push( descriptor.name );
				});
				
				ProcessorServices.getControllerServices( app.processGroupId ).done( 
					function(controllerServicesResult){
						// collect controller services
						app.processGroupControllerServices = [];
						$.each( controllerServicesResult.controllerServices , function (n, cs){
							app.processGroupControllerServices.push( cs );
						});
						
						var ui = null;
						if( app.view.groupsConfig == null ){
							ui = ProcessorUIBuilder.buildUI(
								processorDetailsResult[0] ,
								controllerServicesResult ,
								app.view.jsonProperties
							);
						}else{
							ui = ProcessorUIBuilder.buildUIGrouped(
								app.view.groupsConfig , 
								processorDetailsResult[0] ,
								controllerServicesResult ,
								app.view.jsonProperties
							);
						}
						
						app.view.ui = ui;
						// Apply configs control
						app.view.ui.controls.bindApply( function (){
							app.applyProcessorConfiguration();
						});
						$("body").append( app.view.ui.mainView );
						
						//Testing UI
						app.view.ui.testingUI.getPanel().hide();
						$("body").append( app.view.ui.testingUI.getPanel() );
						// Test control
						app.view.ui.controls.bindTest( function (){ 
							
							var testConfigs = app.getRemoteTestInput();
							app.view.ui.testingUI.clearTestOutput();
							app.view.ui.testingUI.testOutputLoader.show();
							
							ProcessorServices.remoteTest(
								app.processorDetails.id ,
								app.processGroupId , 
								testConfigs 
							).done( function(response){
								app.view.ui.testingUI.showTestOutput( 
									response.output , 
									response.logs 
								);
							}).fail( function( jqXHR , statusText , errorThrown ){
								var error;
								try{
									error = JSON.parse( jqXHR.statusText );
									error = '<pre style="white-space: pre-wrap;">' + 
												JSON.stringify( error , null , 2 ) + 
											'<pre>';
								}catch( e ){
									error = jqXHR.statusText;
								}
								
								app.view.ui.testingUI.testOutputLoader.hide();
								app.view.ui.modal
									.setTitle( "Error: " +  jqXHR.status )
									.setContent( $('<span>' + error + '</span>') )
									.show();
							});
						});
						
						$("body").append( app.view.ui.controls.controlsContainer );
						$("body").append( app.view.ui.modal.getModalNode() );
					}
				);
			}
		);
	}
	
	// Return the remote test configuration from the testingUI.
	getRemoteTestInput(){
		var app = this;
		
		var inputFlowFile = app.view.ui.testingUI.getInputFlowFile();
		var processorConfig = app.getProcessorConfiguration();
	
		$.each( app.controllerServiceProperties , function (i , csPropertyName) {
			if( processorConfig.hasOwnProperty(csPropertyName) && processorConfig[csPropertyName] != null ){
				processorConfig[csPropertyName] = app.resolveControllerServicePropertyConfig( processorConfig[csPropertyName] );
			}
		});
		
		var testConfig = {
			properties: processorConfig , 
			inputFlowFile: inputFlowFile 
		}
		
		app.credentialsCorrection( 
			testConfig ,
			app.view.ui.testingUI.getOAuthCredentials()
		);
		
		return testConfig;
	}
	
	// Substitute the value of "USERNAME" and "PASSWORD" properties descriptors
	// with the supplied ones in the passed config object.
	// The substitution is performed recursively for any field named "USERNAME" 
	// and/or "PASSWORD".
	credentialsCorrection( configs , credentials ){
		var app = this;
		
		for( const [name , prop] of Object.entries(configs) ){
			if( name == "USERNAME" || name == "PASSWORD" ){
				configs[name] = credentials[name.toLowerCase()];
			}else{
				if( typeof configs[name] === 'object' && configs[name] != null ){
					app.credentialsCorrection( configs[name] , credentials );
				}
			}
		}
	}
	
	// Get the specified controller services details resolving recursively nested 
	// controller services i.e. embedding the nested controller services configuration
	// properties as nested objects.
	resolveControllerServicePropertyConfig( controllerServiceId ){
		var app = this;
		var csInfo = null;
		
		for( var csi of app.processGroupControllerServices ){
			if( csi.id == controllerServiceId ){
				csInfo = JSON.parse( JSON.stringify( csi ) ); // deep copy
				break;
			}
		}
		
		if( csInfo != null ){
			var csConfig = {
				id: csInfo.id , 
				type: csInfo.component.type ,
				bundle: csInfo.component.bundle ,
				properties: csInfo.component.properties
			};
			$.each( csInfo.component.descriptors , function (name , descriptor){
				if( descriptor.hasOwnProperty( "identifiesControllerService" ) && descriptor.identifiesControllerService != null && 
					descriptor.identifiesControllerService != "" ){
					
					if( csInfo.component.properties[name] != null && csInfo.component.properties[name] != "" ){
						var nestedCSId = csInfo.component.properties[name];
						csConfig.properties[name] = app.resolveControllerServicePropertyConfig( nestedCSId );
					}
				}
			});
			return csConfig;
		}else{
			return null;
		}
	}
	
	// Get the configured processor properties from the ui
	getProcessorConfiguration(){
		var properties = {};
		
		// static properties
		$.each( this.view.ui.configs , function (pName , cfg){
			properties[pName] = cfg.getValue();
		});
		// dynamic properties if supported
		if( this.view.ui.dynamicConfigs ){
			var dynProperties = this.view.ui.dynamicConfigs.getValues();
			$.each( dynProperties , function(name , value){
				properties[name] = value;
			});
		}
		
		return properties;
	}
	
	// Apply processor configurations
	applyProcessorConfiguration(){
		var app = this;
		
		var properties = app.getProcessorConfiguration();
		
		// Check for deleted dynamic properties
		$.each( Object.keys(app.processorDetails.properties) , function (i , name){
        	if( !properties.hasOwnProperty(name) ){ 
                properties[name] = null; // Setting null for the missing dynamic properties will delete them
                						 // from the configured processor properties
        	}
        });
		
		ProcessorServices.setProperties( 
			this.processorId , this.revision , 
			this.clientId , this.disconnectedNodeAcknowledged ,
			properties
		).done( function( response , statusText , jqXHR ){
			app.processorDetails.properties = response.properties;
			app.view.ui.modal
				.setTitle("Success")
				.setContent($('<span>The processor configurations have been applied.</span>') );
			if( response.validationErrors && response.validationErrors != null ){
				app.view.ui.modal.appendValidationErrors( response.validationErrors );
			}
			app.view.ui.modal.show();
			console.log( response );
			console.log( app.processorDetails );
		}).fail( function( jqXHR , statusText , errorThrown ){
			app.view.ui.modal
				.setTitle( "Error: " +  jqXHR.status )
				.setContent( $('<span>' + statusText + '</span>') )
				.show();
		});
	}
}