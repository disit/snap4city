/*
 * Processor UI builder
 */
export class ProcessorUIBuilder {
	
	// Build a standard (non-grouped) UI
	static buildUI( processorDetailsResult , controllerServicesResult , jsonProperties ){
		var nav = PropertiesNavigationBuilder.buildPropertiesNavigation( 
			processorDetailsResult.descriptors
		);
		
		var mainView = $('<div id="mainView"></div>');
		mainView.append( nav );
		var tabsContainer = $('<div class="tabscontainer"></div>' );
		mainView.append( tabsContainer );
		
		var configs = {};
		$.each( processorDetailsResult.descriptors , function (name , descriptor){ 
			var conf = null;
			if( descriptor.controllerService ){
				conf = PropertyConfigurationBuilder.controllerServicePropertyConfiguration(
					descriptor , processorDetailsResult.properties[name] , 
					controllerServicesResult.controllerServices
				);
			}else{
				if( descriptor.allowableValues == null ){ 
					if( typeof jsonProperties !== typeof undefined && jsonProperties != null && 
					    jsonProperties.includes(name) ){ // json property configuration
						
						conf = PropertyConfigurationBuilder.jsonPropertyConfiguration(
							descriptor , processorDetailsResult.properties[name]
						);
					}else{ // string property configuration
						conf = PropertyConfigurationBuilder.stringPropertyConfiguration( 
							descriptor , processorDetailsResult.properties[name] 
						);
					}
				}else{ // combo property configuration
					conf = PropertyConfigurationBuilder.comboPropertyConfiguration(
						descriptor , processorDetailsResult.properties[name]	
					);
				}
			}
			configs[name] = conf;
			tabsContainer.append( conf.getPanel() );
		});
		nav.find(".tablinks")[0].click();
		$(mainView.find(".tabcontent")[0]).show();
		
		var controls = ProcessorUIBuilder.buildControls();
		var modal = ProcessorUIBuilder.buildModal();
		return {
			mainView: mainView,
			configs: configs,
			controls: controls,
			modal: modal
		};
	}
	
	// Build controls bar
	static buildControls(){
		var controlsPanel = $('<div id="controls"></div>');
		var uiModeSwitch = new OnOffSwitch("ui-mode-switch" , "CONFIGURE" , "TEST" );
		controlsPanel.append( uiModeSwitch.onoff );
		
		
		var actionsPanel = $('<div id="actions"></div>');
		controlsPanel.append( actionsPanel );
		
		var test = $('<button>Run Test</button>');
		var apply = $('<button>Apply</apply>');
		actionsPanel.append( test );
		actionsPanel.append( apply );
		
		return {
			controlsContainer: controlsPanel,
			actionsContainer: actionsPanel,
			uiModeSwitch: uiModeSwitch,
			test: test,
			apply: apply,
			bindTest: function (callback) {
				test.click( callback );
			},
			bindApply: function (callback){
				apply.click( callback );
			},
			bindUIModeSwitch: function (callback) {
				uiModeSwitch.input.change( callback );
			}
		};
	}
	
	// Build modal dialog
	static buildModal(){
		var modalNode = $(ProcessorUIBuilder.modalTemplate);
		var modal = new bootstrap.Modal( modalNode );
		return {
			getModalNode: function (){
				return modalNode;
			} , 
			getModal: function (){
				return modal;
			} , 
			setTitle: function (title){
				modalNode.find(".modal-title").text( title );
				return this;
			} ,
			setContent: function (content){
				var modalBody = modalNode.find(".modal-body");
				modalBody.empty();
				modalBody.append( content );
				return this;
			} ,
			appendValidationErrors: function(validationErrors){
				var modalBody = modalNode.find(".modal-body");
				
				var validationErrorsContainer = $('<div class="validation-errors"><h5>Validation errors:</h5></div>');
				var errorList = $('<ul></ul>');
				validationErrorsContainer.append( errorList );
				$.each( validationErrors , function(n, err){
					errorList.append( $('<li>' + err + '</li>') );
				});
				modalBody.append( validationErrorsContainer );
				return this;
			} ,
			show: function (){
				modal.show();
			} ,
			hide: function (){
				modal.hide();
			}
		};
	}
	
	// Testing UI
	static buildTestingUI(){
		
		var testingUI = $('<div id="testing-ui"></div>' );
		
		/* Input section */
		var inputSection = $(
			`<div id="input-ff-section">
			</div>`
		);
		testingUI.append( inputSection );
		
		// input FF attributes
		var inputFFAttributesContainer = $(
			`<div id="input-ff-attributes-container">
				<div id="input-ff-attributes-heading">
					FF Attributes 
					<button class="btn btn-sm btn-outline-primary add-attribute-button">Add attribute</button>
					<button class="btn btn-sm collapse-button" data-bs-toggle="collapse"
							data-bs-target="#input-ff-attributes">
						<i class="fa fa-angle-down"></i>
					</button>
				</div>
				<div id="input-ff-attributes" class="collapse">
				</div>
			</div>`	
		);
		var collapseFFAttributesButton = inputFFAttributesContainer.find( "#input-ff-attributes-heading .collapse-button" ); 
		collapseFFAttributesButton.click( function (){
				$(this).toggleClass( "up" );
			});
		inputSection.append( inputFFAttributesContainer );
		
		var addAttributeButton = inputFFAttributesContainer.find( ".add-attribute-button" );
		var inputFFAttributesTable = $(
			`<table class="input-ff-attributes-table">
				<thead>
					<th>Attribute name</th>
					<th>Value</th>
					<th></th>
				</thead>
				<tbody>
				</tbody>
			</table>`
		);
		inputFFAttributesContainer.find( "#input-ff-attributes" )
	    	.append( inputFFAttributesTable );
		
		var ffAttributesRowTemplate = $( 
			`<tr>
			    <td><input type="text" class="attribute-name-input"></td>
				<td><input type="text" class="attribute-value-input"></td>
				<td><button class="btn btn-danger btn-sm"><i class="fa fa-trash"></i></button></td>
		  	</tr>` 
		);
		
		addAttributeButton.click( function (){
			var row = ffAttributesRowTemplate.clone();
			row.find('button').click( function (){
				row.remove();
			});
			inputFFAttributesTable.find('tbody').append( row );
			if( !inputFFAttributesContainer.find( "#input-ff-attributes" )
					.hasClass( "show" ) ){
				collapseFFAttributesButton.click();
			}
		});
		
		// input FF content
		var inputFFContentEditor = $( '<div id="input-ff-content-editor" class="jsoneditor"></div>');
		const inputFFContentJsonEditor = new JSONEditor(
			inputFFContentEditor[0] , 
			{ 
				mode: "code",
				statusBar: false
			}
		);
		inputFFContentJsonEditor.set( // set a template input 
			{
				id: "sensor1" , 
				date_time: (new Date()).toISOString() , 
				temperature: {
					value: 25
				}
			}
		);
		inputSection.append( inputFFContentEditor );
		
		// input OAuth credentials
		var oAuthCredentialsContainer = $(
			`<div id="oauth-credentials-container-heading">
				<span>OAuth credentials<i class="fa fa-question-circle" data-bs-toggle="popover"
					  style="margin-left: 7px;" 
					  data-bs-trigger="hover" 
					  data-bs-content="Since this testing tool cannot access sensitive (encrypted) NiFi properties, 
					  			       if the configured controller service chain for this processor uses 
					  			       OAuth authentication, you need to provide some valid OAuth credentials 
					  			       to run the tests."></i></span>
				<button class="btn btn-sm collapse-button" data-bs-toggle="collapse" 
				        data-bs-target="#oauth-credentials-collapse">
					<i class="fa fa-angle-down"></i>
				</button>
			</div>
			<div id="oauth-credentials-collapse" class="collapse">
				<div id="oauth-credentials-container">
					<div>
						<input type="text" class="form-control" id="oauth-username" placeholder="OAuth username">
					</div>
					<div>
						<input type="password" class="form-control" id="oauth-password" placeholder="OAuth password">
					</div>
				</div>
			</div>` 
		);
		oAuthCredentialsContainer.find( "i.fa-question-circle").popover();
		oAuthCredentialsContainer.find( ".collapse-button" ).click( function (){
			$(this).toggleClass("up");
		})
		inputSection.append( oAuthCredentialsContainer );
		
		/* Output section */
		var outputSection = $(
			`<div id="output-ff-section">
				<div id="test-output-loader">
					<div class="lds-ring"><div></div><div></div><div></div><div></div></div>
				</div>
				<div id="output-relationships-select-container"></div>
				<div id="output-relationships-container" class="tab-content"></div>
				<div id="output-test-notifications" class="toast-container position-absolute end-0 bottom-0">
					<div class="toast hidden">
						<div class="toast-header">
							<strong class="me-auto">Test terminated successfully</strong>
							<small></small>
							<button type="button"><i class="fa fa-window-minimize"></i></button>
						</div>
						<div class="toast-body">
							<table class="table">
								<tbody></tbody>
							<table>
							<div id="notification-logs">
							</div>
						</div>
					</div>
				</div>
			</div>`);
		var bsNotification = new bootstrap.Toast( 
			outputSection.find( ".toast" )[0] , 
			{ "autohide":false } 
		);
		
		var testOutputLoader = outputSection.find( "#test-output-loader" );
		testOutputLoader.hide();
		
		var notificationToast = outputSection.find( "#output-test-notifications .toast" );
		var notificationBody = outputSection.find( "#output-test-notifications .toast-body" );
		var notificationTitle = outputSection.find( "#output-test-notifications .toast-header strong" );
		var toggleNotificationButton = outputSection.find( "#output-test-notifications .toast-header button" );
		toggleNotificationButton.click( function (){
			var icon = toggleNotificationButton.find( "i" );
			icon.toggleClass( "fa-window-maximize" );
			icon.toggleClass( "fa-window-minimize" );
			notificationToast.toggleClass( "minimized" );
//			notificationTitle.toggleClass("minimized");
			notificationBody.toggle();	
		});
		
		notificationTitle.click( function (){
			if( notificationTitle.hasClass("minimized") ){
				var icon = toggleNotificationButton.find( "i" );
				notificationToast.toggleClass( "minimized" );
//				notificationTitle.toggleClass("minimized");
				icon.toggleClass("fa-window-minimize");
				icon.toggleClass("fa-window-maximize");
				notificationBody.toggle();
			}
		});
		testingUI.append( outputSection );
		
		/*Return custom object*/
		return {
			getPanel: function (){
				return testingUI;
			} ,
			getInputFlowFile: function (){
				var attributes = {};
				$.each( inputFFAttributesTable.find('tbody tr') , function (i , row){
					row = $(row);
					var attrName = row.find('.attribute-name-input').val();
					var attrValue = row.find('.attribute-value-input').val();
					attributes[attrName] = attrValue;
				});
				
				var content = inputFFContentJsonEditor.getText();
				
				return {
					attributes: attributes,
					content: content
				}
			} ,
			getOAuthCredentials: function (){
				return {
					username: oAuthCredentialsContainer.find("#oauth-username").val(), 
					password: oAuthCredentialsContainer.find("#oauth-password").val()
				}
			} , 
			testOutputLoader: testOutputLoader , // test output loader
			clearTestOutput: function (){
				outputSection.find( "#output-relationships-select-container" ).empty();
				outputSection.find( "#output-relationships-container" ).empty();
				var notification = outputSection.find( "#output-test-notification" );
				notification.find( ".toast-header small" ).empty();
				notification.find( ".toast-body table tbody" ).empty();
				notification.find( ".toast-body #notification-logs" ).empty();
				notification.find( ".toast-body" ).show();
				bsNotification.hide();
			},
			showTestOutput: function (testResult,logs) { // shows test output in the output section
				testOutputLoader.hide();
				
				var relationships = Object.keys(testResult);
				var relationshipsSelectContainer = outputSection.find( "#output-relationships-select-container" );
				relationshipsSelectContainer.empty();
				
				var relationshipsTabsContainer = outputSection.find( "#output-relationships-container" );
				relationshipsTabsContainer.empty();
				
				
				var relationshipSelect = $( '<select id="output-relationships-select" class="form-select"></select>' );
				relationshipsSelectContainer.append( 
					`<div style="margin-bottom: 2px;">
						<span style="font-weight: bold;">Test output</span>: populated relationships
					</div>`
				);
				relationshipsSelectContainer.append( relationshipSelect );
				
				relationshipSelect.change( function (){
					outputSection.find( "#output-relationships-container .rel-tab" )
								 .hide();
					var selectedRelTab = outputSection.find( "#output-relationships-container #" + $(this).val() );
//					selectedRelTab.find( ".output-relation-ff-nav .btn-group button:first-child" )
//								  .click();
					selectedRelTab.find( ".output-relation-ff-nav ul.pagination li:first-child a" )
					  			  .click();
					selectedRelTab.show();
				});
				
				$.each( relationships , function (i , relationship) {
					var relationshipFFs = testResult[relationship];
					var tabId = "out-relationship-" + i; 
					
					relationshipSelect.append( $(
						'<option value="' + tabId +'">' + 
							relationship + ": " + relationshipFFs.length + " flow files" +
						'</option>"'
					) );
					
					var relTab = $(
						`<div class="rel-tab" id="` + tabId + `">
							<div class="output-relation-ff-nav"></div>
							<div class="output-relation-ff-view">
								<div class="output-ff-attributes"></div>
								<div class="output-ff-editor"></div>
							</div>
						</div>`
					);
					var outputEditor = new JSONEditor( 
						relTab.find(".output-ff-editor")[0] ,
						{ 
							mode: "code" ,
							mainMenuBar: false ,
							statusBar: false ,
							onEditable: function (editable){ return false; }
						}
					);
					relTab.hide();
					relationshipsTabsContainer.append( relTab );
					
					var ffNav = relTab.find( ".output-relation-ff-nav");
					
					var ffNavPagination = $('<ul class="pagination pagination-sm"></ul>');
					ffNav.append( ffNavPagination );
					
					var ffAttributesContainer = relTab.find(".output-ff-attributes"); 
					$.each( relationshipFFs , function (i ,ff){
						var ffPage = $('<li class="page-item"><a class="page-link">' + (i+1).toString() + '</a></li>' );
						var ffPageLink = ffPage.find( "a" );
						ffPageLink.click( function(){
							ffAttributesContainer.empty();
							var attrTable = $(`<table class="table">
								<thead><th>Attribute</th><th>Value</th></thead>
								<tbody></tbody></table>`);
							for( const [attrName , attrValue] of Object.entries(ff.attributes) ){
								attrTable.find("tbody").append( $( 
									`<tr>
										<td>` + attrName + `</td>
										<td>` + attrValue + `</td>
									</tr>` ) );
							}
							ffAttributesContainer.append( attrTable );
							outputEditor.setText( JSON.stringify( JSON.parse(ff.content) , null , 2 ) );
						});
						ffNavPagination.append( ffPage );
					});
				});
				outputSection.find( "#show-notification-icon" ).show();
				outputSection.find( "#output-relationships-container .rel-tab:first-child" )
							 .show();
				outputSection.find( "#output-relationships-container .rel-tab:first-child .output-relation-ff-nav ul.pagination li:first-child a" )
							 .click();
				
				// Notification
				var notification = outputSection.find( "#output-test-notifications .toast" );
				notification.find( "table tbody" ).empty() 
				$.each( relationships , function (i , relationship) {
					notification.find("table tbody").append(
						$('<tr><td style="font-weight: bold;">' + relationship + '</td><td>' + testResult[relationship].length + ' flow files</td></tr>' )
					);
				});
				var currentTime = new Date();
				notification.find( "small" ).text( 
					String( currentTime.getHours() ).padStart(2,'0') + ":" + 
					String( currentTime.getMinutes() ).padStart(2,'0') + ":" + 
					String( currentTime.getSeconds() ).padStart(2,'0') 	
				);
				var notificationLogs = notification.find( "#notification-logs" );
				notificationLogs.empty();
				if( typeof logs !== typeof undefined && logs != null ){
					for( const [logLevel , messages] of Object.entries(logs) ){
						$.each( messages , function (i, msg){
							// clean and format log messages
							if( msg.startsWith("{} " ) )
								msg = msg.substring(3);
							try{
								msg = JSON.stringify( JSON.parse( msg ) , null , 2 );
							}catch ( e ){ }
							
							notificationLogs.append( $( 
								'<div><span style="font-weight: bold;">' + logLevel + ': </span><pre>' +
								msg + '</pre></div>'
							));
						});
					}
				}
				notification.removeClass("hidden");
				bsNotification.show();
			}
		}
	}
	
	// Build a grouped UI for the processor's configurations
	static buildUIGrouped( groups , processorDetailsResult , controllerServicesResult , jsonProperties ){
		
		// Determine dynamic properties support		
		var dynamicProperties = {};
		if( processorDetailsResult.dynamicPropertySupported ){
			for( const [name , descriptor] of Object.entries(processorDetailsResult.descriptors) ){
				if( descriptor.dynamic ){
					dynamicProperties[name] = processorDetailsResult.properties[name];
				}
			}
		}
		
		var mainView = $('<div id="mainView"></div>');
		
		var groupsNav = $('<ul id="property-groups-navigation" class="nav nav-tabs" role="tablist"></ul>');
		var groupsContent = $('<div class="tab-content"></div>');
		mainView.append( groupsNav );
		mainView.append( groupsContent );
		
		var ui = {};
		var configs = {};
		var activeSet = false;
		$.each( groups , function(groupName , descriptorNames){
			var name = groupName.replaceAll( " " , "_" );
			var navItem = $('<li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#' + name + '_groupcontent" type="button" role="tab">'
								+ groupName + '</button></li>' );
			groupsNav.append( navItem );
			
			var groupContent = $('<div class="tab-pane" id="' + name + '_groupcontent" role="tabpanel"></div>' );
			groupsContent.append( groupContent );
			
			if( !activeSet ){
				navItem.find(".nav-link").addClass("active");
				groupContent.addClass( "active" );
				groupContent.addClass( "show" );
				activeSet = true;
			}
			
			var groupDescriptors = {};
			var innerGroupContainer = $( '<div class="tabscontainer"></div>' );
			
			$.each( descriptorNames , function (i , name){
				if( processorDetailsResult.descriptors.hasOwnProperty(name) ){
					var conf = null;
					var descriptor = processorDetailsResult.descriptors[name];
					groupDescriptors[name] = descriptor;
					
					if( descriptor.controllerService ){
						conf = PropertyConfigurationBuilder.controllerServicePropertyConfiguration(
							descriptor , processorDetailsResult.properties[name] , 
							controllerServicesResult.controllerServices
						);
					}else{
						if( descriptor.allowableValues == null ){
							if( typeof jsonProperties != typeof undefined && jsonProperties != null && 
								jsonProperties.includes(name) ){ // json property configuration
								 
								conf = PropertyConfigurationBuilder.jsonPropertyConfiguration(
									descriptor , processorDetailsResult.properties[name]
								);
							}else{ // string property configuration
								conf = PropertyConfigurationBuilder.stringPropertyConfiguration( 
									descriptor , processorDetailsResult.properties[name] 
								);
							}
						}else{ // combo property configuration
							conf = PropertyConfigurationBuilder.comboPropertyConfiguration(
								descriptor , processorDetailsResult.properties[name]	
							);
						}
					}
					
					innerGroupContainer.append( conf.getPanel() );
					configs[name] = conf;
				}
			});
			
			var innerGroupNav = PropertiesNavigationBuilder
									.buildPropertiesNavigation( groupDescriptors , descriptorNames );
			groupContent.append( innerGroupNav );
			groupContent.append( innerGroupContainer );
			
			navItem.find( "button.nav-link" ).click( function (){
				innerGroupNav.find(".tablinks")[0].click();
			});
		});
		ui.configs = configs;
		
		if( processorDetailsResult.dynamicPropertySupported ){
			var navItem = $(`<li class="nav-item">
								<button class="nav-link" data-bs-toggle="tab" data-bs-target="#dynamic_properties_groupcontent" type="button" role="tab">
									Dynamic properties
								</button>
							</li>` );
			groupsNav.append( navItem );
			var groupContent = $('<div class="tab-pane" id="dynamic_properties_groupcontent" role="tabpanel"></div>' );
			groupsContent.append( groupContent );
			
			var conf = PropertyConfigurationBuilder.dynamicPropertiesConfiguration(
				processorDetailsResult.dynamicPropertiesDescription ,
				dynamicProperties
			);
			groupContent.append( conf.getPanel() );
			ui.dynamicConfigs = conf;
		}
		
		groupsContent.find( ".tab-pane.active .tablinks" )[0].click();
		$(groupsContent.find( ".tab-pane.active .tabscontainer .tabcontent" )[0]).show();
		
		ui.mainView = mainView;
		ui.controls = ProcessorUIBuilder.buildControls();
		ui.controls.test.hide();
		ui.modal = ProcessorUIBuilder.buildModal();
		ui.testingUI = ProcessorUIBuilder.buildTestingUI();
		
		ui.controls.bindUIModeSwitch( function (){
			if( this.checked ){
				ui.testingUI.getPanel().hide();
				ui.controls.test.hide();
				ui.controls.apply.show();
				ui.mainView.fadeIn( 400 );
			}else{
				ui.mainView.hide();
				ui.controls.apply.hide();
				ui.controls.test.show();
				ui.testingUI.getPanel().fadeIn( 400 );
			}
		});
		
		return ui;
	}
}

//Modal template
ProcessorUIBuilder.modalTemplate = `<div class="modal" tabindex="-1"> 
									   <div class="modal-dialog modal-dialog-scrollable">
									     <div class="modal-content">
									     	<div class="modal-header">
									     		<h5 class="modal-title"></h5> 
									     		<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
									     	</div>
									     	<div class="modal-body"></div>
									     	<div class="modal-footer">
									     		<button type="button" class="btn btn-secondary" 
									     				data-bs-dismiss="modal">
									     			Close
									     		</button>
									     	</div>
									  	</div>
									  </div>
									</div>`;

/* 
 * Properties navigation builder
 */
export class PropertiesNavigationBuilder {
	
	static buildPropertiesNavigation( descriptors , descriptorsOrder ){
		var entries = [];
		if( typeof descriptorsOrder !== typeof undefined && descriptorsOrder != null && 
			Array.isArray( descriptorsOrder ) ){
			
			$.each( descriptorsOrder , function( i , name ){
				if( !descriptors.hasOwnProperty( name ) ){
					throw '"' + name + '" is not present in the specified group descriptors'; 
				}
				
				var descriptor = descriptors[name];
				var entry = $('<button class="tablinks">' + descriptor.displayName +'</button>');
				entry.attr( "data-descriptor" , JSON.stringify( descriptor ) );
				if( descriptor.required ){
					entry.addClass( "required-property" );
				}
				
				entry.click( PropertiesNavigationBuilder.openPropertyConfiguration );
				entries.push( entry );
			});
		}else{
			$.each( descriptors , function (name , descriptor){
				var entry = $('<button class="tablinks">' + descriptor.displayName +'</button>');
				entry.attr( "data-descriptor" , JSON.stringify( descriptor ) );
				if( descriptor.required ){
					entry.addClass( "required-property" );
				}
				entry.click( PropertiesNavigationBuilder.openPropertyConfiguration );
				entries.push( entry );
			});
		}
		
		var tab = $('<div class="tab properties-navigation"></div>');
		tab.append( entries );
		return tab;
	}
	
	// Nav button callback 
	static openPropertyConfiguration( event ){
		var targetBtn = $(event.target);
		var descriptor = JSON.parse( targetBtn.attr("data-descriptor") );
//		console.log( descriptor );
		
		targetBtn.parent().find( ".tablinks" ).removeClass("active");
//		$(".tablinks").removeClass("active");
		targetBtn.addClass("active");
//		$(event.target).addClass( "active" );
		$(".tabcontent").hide();
		$("#" + descriptor.name).show();
	}
}

/* 
 * Properties configuration builder 
 */
export class PropertyConfigurationBuilder {
	
	/* 
	 * BASE PANEL 
	 */
	static basePanel( descriptor ){
		var panel = $( PropertyConfigurationBuilder.tabcontentTemplate );
		panel.attr( "id" , descriptor.name );
		panel.append( $("<h2>" + descriptor.displayName + "</h2>" ) );
		panel.append( $("<p>" + descriptor.description + "</p>") );
		
		return panel;
	}
	
	/* 
	 * STRING PROPERTY CONFIGURATION 
	 */
	static stringPropertyConfiguration( descriptor , value ){
		var panel = PropertyConfigurationBuilder.basePanel( descriptor );
		
		var checkControlsContainer = $( PropertyConfigurationBuilder.checkControlsContainer );
		panel.append( checkControlsContainer );
		
		var emptyCheckbox = $( PropertyConfigurationBuilder.emptyStringCheckboxTemplate );
		var emptyCheckContainer = $( "<div>Set empty string</div>" );
		checkControlsContainer.append( emptyCheckContainer );
		
		var input = $( PropertyConfigurationBuilder.textAreaTemplate );
		
		// "Set empty string" checkbox
		emptyCheckbox.change( function (){ 
			if( this.checked ){
				input.attr( "placeholder" , "Empty string set" ); 
				input.val( "" );
				input.prop( "readonly" );
			}else{
				input.attr( "placeholder" , "No value set" );
				input.removeProp( "readonly" );
				input.val( "" );
			}
		});
		emptyCheckContainer.prepend( emptyCheckbox );

		// Text area input
		if( value != null ){
			if( value == "" ){
				emptyCheckbox.prop( "checked" , true );
				input.removeAttr( "placeholder" );
			}
			input.val( value );
		}
		panel.append( input );
		
		panel.hide();
		//return custom obj
		return {
			getPanel: function (){
				return panel;
			} ,
			getValue: function (){
				if( emptyCheckbox.prop("checked") )
					return "";
				if( input.val() === "" )
					return null;
				return input.val()
			}
		}
	}
	
	/*
	 * DYNAMIC PROPERTIES CONFIGURATION 
	 */
	static dynamicPropertiesConfiguration( description , dynamicProperties ){
		var panel = PropertyConfigurationBuilder.basePanel(
			{
				name: "DYNAMIC_PROPERTIES" ,
				displayName: "Dynamic properties",
				description: description
			}
		);
		panel.removeClass("tabcontent");
		panel.addClass( "dynamic-properties-container" );
		
		var addPropertyButton = $('<button class="btn btn-primary add-property-button">Add property</button>' );
		panel.append( $('<div class="add-property-button-container"></div>').append( addPropertyButton ) );
		var dynPropertiesTable = $('<table><thead><th>Property name</th><th>Value</th><th></th></thead><tbody></tbody></table>');
		panel.append( dynPropertiesTable );
		
		var rowTemplate = $( 
			`<tr>
			    <td><input type="text" class="property-name-input"></td>
				<td><input type="text" class="property-value-input"></td>
				<td><button class="btn btn-danger btn-sm"><i class="fa fa-trash"></i></button></td>
		  	</tr>` );
		
		addPropertyButton.click( function(){
			var row = rowTemplate.clone();
			row.find( 'button' ).click( function (){
				row.remove();
			});
			dynPropertiesTable.append( row );
		});
		
		if( typeof dynamicProperties !== typeof undefined && dynamicProperties != null ){
			$.each( dynamicProperties , function( name , value ){ 
				var row = rowTemplate.clone();
				row.find( 'button' ).click( function(){ row.remove(); });
				row.find( 'input.property-name-input').val( name );
				row.find( 'input.property-value-input').val( value );
				dynPropertiesTable.find('tbody').append( row );
			});
		}
		
		// return custom obj
		return {
			getPanel: function (){
				return panel;
			} ,
			getValues: function(){
				var dynProperties = {};
				$.each( dynPropertiesTable.find('tbody tr') , function (n , row){
					row = $(row);
					var pName = row.find( "input.property-name-input" ).val();
					var pVal = row.find( "input.property-value-input" ).val();
					dynProperties[pName] = pVal;
				});
				return dynProperties;
			}
		}
	}
	
	/* 
	 * CONTROLLER SERVICE PROPERTY CONFIGURATION 
	 */
	static controllerServicePropertyConfiguration( descriptor , value , controllerServicesDetails ){
		var panel = PropertyConfigurationBuilder.basePanel( descriptor );
		
		var input = $( PropertyConfigurationBuilder.comboTemplate );
		panel.append( input );
		
		var statusPanel = $( PropertyConfigurationBuilder.csStatusPanelTemplate );
		panel.append( statusPanel );
		
		var detailsTable = $( PropertyConfigurationBuilder.csDetailsTableTemplate );
		panel.append( detailsTable );
		
		// Filter compatible controller services
		var compatibleControllerServices = [];
		$.each( controllerServicesDetails , function (n , csd){
			var supportedApis = [];
//			console.log( csd );
			$.each( csd.component.controllerServiceApis , function (n , api){
				supportedApis.push( api.type );
			});
			
			if( supportedApis.includes(descriptor.controllerServiceApiClass) ){
				compatibleControllerServices.push( csd );
			}
		});
		
		// Populate select
		if( !descriptor.required || value == null )
			input.append( $('<option class="no-value-combo-entry" value="">No value set</option>') );
		var selectionSet = false;
		$.each( compatibleControllerServices , function (n , csd){
			var opt = '<option value="' + csd.id + '" ' ;
			if( value != null ){
				if( csd.id == value ){
					opt += 'selected="selected"';
					selectionSet = true;
					PropertyConfigurationBuilder.updateCsView( 
						statusPanel , detailsTable , 
						csd , controllerServicesDetails 
					);
				}
			}
			opt += '>' + csd.component.name + '</option>';
			
			var optEntry = $(opt);
			optEntry.attr( "data-controller-service" , JSON.stringify(csd) );
			input.append( optEntry );
		});
		if( !selectionSet ){
			input.children( '.no-value-combo-entry' ).attr( 'selected' , 'selected' );
		}
		
		// Change callback
		input.change( function(){
			var selectedOpt = $(this.options[this.selectedIndex]);
			if( selectedOpt[0].hasAttribute( "data-controller-service" ) ){
				var csd = JSON.parse( selectedOpt.attr("data-controller-service") );
				PropertyConfigurationBuilder.updateCsView( 
					statusPanel , detailsTable , 
					csd , controllerServicesDetails 
				);
//				console.log( csd );
			}else{
				statusPanel.children( ".cs-run-status" ).empty();
				statusPanel.children( ".cs-validation-status" ).empty();
				detailsTable.children("tbody").empty();
			}
		});
		
		panel.hide();
		// Return custom obj
		return {
			getPanel: function(){
				return panel;
			} , 
			getValue: function(){
				if( $(input[0].options[input[0].selectedIndex]).hasClass("no-value-combo-entry") )
					return null;
				return input.val();
			}
		}
		
	}
	
	// update the controller service view 
	static updateCsView( statusPanel , table , csd , controllerServicesDetails ){
		var runStatusContainer = statusPanel.children(".cs-run-status" );
		var validationStatusContainer = statusPanel.children( ".cs-validation-status" );
		runStatusContainer.empty();
		runStatusContainer.append( 
			$(PropertyConfigurationBuilder.csRunStatus[csd.status.runStatus])  
		);
		validationStatusContainer.empty();
		validationStatusContainer.append( 
			$(PropertyConfigurationBuilder.csValidationStatus[csd.status.validationStatus])
		);
		if( csd.component.validationErrors ){
			var valStatusIcon = validationStatusContainer.children("span")
									.children("i");
			valStatusIcon.attr( "data-bs-toggle" , "popover" );
			valStatusIcon.attr( "data-bs-trigger" , "hover" );
			valStatusIcon.attr( "data-bs-placement" , "top" );
			valStatusIcon.attr( "data-bs-html" , "true" );
			var valPopoverTemplateStr = "<div class='popover validation-popover' role='tooltip'><div class='popover-arrow'></div><div class='popover-body'></div></div>";
			valStatusIcon.attr( "data-bs-template" , valPopoverTemplateStr );
			var valPopoverContent = "<ul>";
			$.each( csd.component.validationErrors , function(n , err){
				valPopoverContent += '<li>' + err + '</li>';
			});
			valPopoverContent += '</ul>';
			valStatusIcon.attr( "data-bs-content" , valPopoverContent );
			valStatusIcon.popover();
		}
		
		
		var tableContent = table.children("tbody");
		tableContent.empty();
		
		$.each( csd.component.descriptors , function (name , descriptor){ 
			var row = $("<tr></tr>");				
			var rowContentStr = '<td'
			if( descriptor.required )
				rowContentStr += ' class="required-property"';
			rowContentStr += '>' + descriptor.displayName 
                	+ '<span class="fa fa-question-circle" data-bs-toggle="popover" data-bs-trigger="hover" data-bs-placement="top" '
                	+ 'data-bs-template=\'<div class="popover property-popover" role="tooltip"><div class="popover-arrow"></div><div class="popover-body"></div></div>\' '
                	+ 'data-bs-content="' + descriptor.description + '"></span>' 
                	+ '</td><td>';
			
			// Nested CS
			// Check if the property value can be an id of a NiFi component
			// (when a controller service depends on another controller service)
			if( /[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}/i.test( csd.component.properties[name] ) ){
				var pRowValueStr = null; 
				for( const [ n , controllerService ] of Object.entries(controllerServicesDetails) ){
					if( controllerService.id == csd.component.properties[name] ){
						// Found match with cs id
						pRowValueStr = '<div class="accordion accordion-flush" id="accordion_' + controllerService.id + '">';
						pRowValueStr += '<div class="accordion-item" id="heading_' + controllerService.id + '"><h2 class="accordion-header">';
						pRowValueStr += '<button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#content_' 
											+ controllerService.id + '" aria-expanded="true" aria-controls="content_' + controllerService.id + '">';
						pRowValueStr += controllerService.component.name; // Accordion heading content
						pRowValueStr += '</button></h2>';
						pRowValueStr += '<div id="content_' + controllerService.id + '" class="accordion-collapse collapse" '
											+ 'aira-labelledby="heading_' + controllerService.id + '" '
											+ 'data-bs-parent="#accordion_' + controllerService.id + '">';
						pRowValueStr += '<div class="accordion-body">';
						// Accordion content
						pRowValueStr += '<table class="table nested-cs-table">';
						pRowValueStr += '<tr><td class="bold">id</td><td>' + controllerService.id + '</td></tr>';
						pRowValueStr += '<tr><td class="bold">class</td><td>' + controllerService.component.type + '</td></tr>';
						pRowValueStr += '<tr><td class="bottom-border">' + PropertyConfigurationBuilder.csRunStatus[controllerService.status.runStatus] + '</td>';
						pRowValueStr += '<td class="bottom-border">';
						var valStatus = $(PropertyConfigurationBuilder.csValidationStatus[controllerService.status.validationStatus]);
						var valStatusIcon = valStatus.children('i');
						if( controllerService.component.validationErrors ){
							valStatusIcon.attr( "data-bs-toggle" , "popover" );
							valStatusIcon.attr( "data-bs-trigger" , "hover" );
							valStatusIcon.attr( "data-bs-html" , "true" );
							valStatusIcon.attr( "data-bs-template" , "<div class='popover validation-popover' role='tooltip'><div class='popover-arrow'></div><div class='popover-body'></div></div>" );
							var valPopoverContent = "<ul>";
							$.each( controllerService.component.validationErrors , function (n,err){
								valPopoverContent += '<li>' + err + '</li>';
							});
							valStatusIcon.attr( "data-bs-content" , valPopoverContent );
						}
						pRowValueStr += valStatus.html();
						pRowValueStr += '</td></tr>';
						
						$.each( controllerService.component.properties , function (name,value){
							pRowValueStr += '<tr><td'
							if( controllerService.component.descriptors[name].required )
								pRowValueStr += ' class="required-property"'
							pRowValueStr += '>' 
										 + controllerService.component.descriptors[name].displayName
				 						 + '<span class="fa fa-question-circle" data-bs-toggle="popover" data-bs-trigger="hover" data-bs-placement="top" '
						                 + 'data-bs-template=\'<div class="popover property-popover" role="tooltip"><div class="popover-arrow"></div><div class="popover-body"></div></div>\' '
						                 + 'data-bs-content="' + controllerService.component.descriptors[name].description + '"></span>' 
						                 + '</td><td>' 
						                 + controllerService.component.properties[name]
					                     + '</td></tr>';
						});
						pRowValueStr += '</table>';
						pRowValueStr += '</div></div></div></div>'
						
						break;
					}
				}
				if( pRowValueStr == null ) // No match with cs id after checking all retrieved cs 
					pRowValueStr = csd.component.properties[name];
				rowContentStr += pRowValueStr;
			}else{
				rowContentStr += csd.component.properties[name];
			}
			rowContentStr += '</td>';
			var rowContent = $(rowContentStr);	
			
			
			if( descriptor.required ){
				$( rowContent.children("td")[0] ).addClass( "required-property" );
			}
			rowContent.find( '[data-bs-toggle="popover"]' ).popover();
			row.append( rowContent );
			tableContent.append( row );
		});
	}
	
	/* 
	 * COMBO-BOX/SELECT PROPERTY CONFIGURATION  
	 */
	static comboPropertyConfiguration( descriptor , value ){
		var panel = PropertyConfigurationBuilder.basePanel( descriptor );
		
		var input = $( PropertyConfigurationBuilder.comboTemplate );
		panel.append( input );
		
		if( !descriptor.required || value == null )
			input.append( $('<option class="no-value-combo-entry" value="">No value set</option>') );
		var selectionSet = false;
		$.each( descriptor.allowableValues , function (key , v){
			var opt = '<option value="' + v + '" ';
			if( value != null ){
				if( v == value ){
					opt += 'selected="selected"';
					selectionSet = true;
				}
			}else{
				if(descriptor.required && descriptor.defaultValue != null && v == descriptor.defaultValue){
					opt += 'selected="selected"';
					selectionSet = true;
				}
			}
			opt += ">" + v + "</option>";
			input.append( $(opt) );
		});
		if( !selectionSet ){
			input.children(".no-value-combo-entry").attr("selected" , "selected");
		}
		
		input.change( function (){
			if( descriptor.required && this.value != "" ){
				var selectedOpt = $(this.options[this.selectedIndex]);
				if( !selectedOpt.hasClass( "no-value-combo-entry" ) ){
					$(this).remove( ".no-value-combo-entry" );
				}
			}
		});
		
		panel.hide();
		// return custom obj
		return {
			getPanel: function(){
				return panel;
			} ,
			getValue: function(){
				if( $(input[0].options[input[0].selectedIndex]).hasClass("no-value-combo-entry") ){
					return null;
				}
				return input.val();		
			}
		}
	}
	
	/*
	 *  JSON PROPERTY CONFIGURATION 
	 */
	static jsonPropertyConfiguration( descriptor , value ){
		var panel = PropertyConfigurationBuilder.basePanel( descriptor );
		
		var checkControlsContainer = $( PropertyConfigurationBuilder.checkControlsContainer );
		panel.append( checkControlsContainer );
		var emptyCheckbox = $( PropertyConfigurationBuilder.emptyStringCheckboxTemplate );
		var emptyCheckContainer = $( "<div>Set empty string</div>" );
		checkControlsContainer.append( emptyCheckContainer );
		
		var input = $( PropertyConfigurationBuilder.jsonEditorTemplate );
		const editor = new JSONEditor(
			input[0] , { "mode": "code" }
		);
		
		// Determine if no value allowed
		var noValueAllowed = !descriptor.required;
		if( noValueAllowed ){
			var noValueCheckbox = $( PropertyConfigurationBuilder.noValueCheckboxTemplate );
			var noValueCheckContainer = $( "<div>Set no value</div>" );
			checkControlsContainer.append( noValueCheckContainer );
		}
		
		// Set empty string checkbox
		emptyCheckbox.change( function (){
			if( this.checked ){
				if( noValueAllowed )
					noValueCheckbox.prop( "checked" , false );
				editor.setMode("text");
				editor.textarea.readOnly = true;
				editor.textarea.placeholder = "Empty string set";
				editor.textarea.value = "";
			}else{
				editor.setMode( "code" );
				editor.set( JSON.parse("{}") );
			}
		});
		emptyCheckContainer.prepend( emptyCheckbox );
		
		// Set no value checkbox
		if( noValueAllowed ){
			noValueCheckbox.change( function(){
				if( this.checked ){
					emptyCheckbox.prop( "checked" , false );
					editor.setMode("text");
					editor.textarea.readOnly = true;
					editor.textarea.placeholder = "No value set";
					editor.textarea.value = "";
				}else{
					editor.setMode( "code" );
					editor.set( JSON.parse("{}") );
				}
			});
			noValueCheckContainer.prepend( noValueCheckbox );
		}
		
		// Set initial value
		if( value != null ){
			if( value == "" ){
				emptyCheckbox.prop( "checked" , true );
				emptyCheckbox.trigger( "change" );
			}else{
				try{
					var jsonValue = JSON.parse( value );
					editor.set( jsonValue );
				}catch(e){
					editor.set( "" );
				}
			}
		}else{
			if( noValueAllowed ){
				noValueCheckbox.prop( "checked" , true );
				noValueCheckbox.trigger( "change" );
			}else{
				editor.setMode( "code" );
				editor.set( JSON.parse("{}") );
			}
		}
		panel.append( input );
		
		panel.hide();
		// return custom obj
		return {
			getPanel: function (){
				return panel;
			} ,
			getValue: function(){
				if( emptyCheckbox.prop("checked") )
					return "";
				if( noValueAllowed && noValueCheckbox.prop("checked") )
					return null;
				return editor.getText();
			}
		}
	}
}

// ------------------------------ 
// PropertyConfigurationBuilder templates
//------------------------------
// Panel and containers
PropertyConfigurationBuilder.tabcontentTemplate = '<div class="tabcontent"></div>';
PropertyConfigurationBuilder.checkControlsContainer = '<div class="check-controls-container"></div>';

// Checkboxes
PropertyConfigurationBuilder.emptyStringCheckboxTemplate = '<input class="empty-string-checkbox" type="checkbox">';
PropertyConfigurationBuilder.noValueCheckboxTemplate = '<input class="no-value-checkbox" type="checkbox">';

// Inputs
PropertyConfigurationBuilder.textAreaTemplate = '<textarea class="property-value-input" rows="4" placeholder="No value set"></textarea>';
PropertyConfigurationBuilder.jsonEditorTemplate = '<div class="property-value-input jsoneditor"></div>';
PropertyConfigurationBuilder.comboTemplate = '<select class="property-value-input form-select"></select>';

// CS status panel
PropertyConfigurationBuilder.csStatusPanelTemplate = '<div class="cs-status-panel"><div class="cs-run-status"></div><div class="cs-validation-status"></div>';
PropertyConfigurationBuilder.csRunStatus = {
	"ENABLED" : '<span><span style="font-weight: bold;">Status: </span><i class="fa fa-flash"></i> Enabled</span>' , 
	"DISABLED" : '<span><span style="font-weight: bold;">Status: </span><i class="icon icon-enable-false"></i> Disabled</span>'
}
PropertyConfigurationBuilder.csValidationStatus = {
	"VALID" : '<span><span style="font-weight: bold;">Validation: </span><i class="fa fa-check"></i> Valid</span>' , 
	"INVALID" : '<span><span style="font-weight: bold;">Validation: </span><i class="fa fa-warning"></i> Invalid</span>'
}

// Table
PropertyConfigurationBuilder.csDetailsTableTemplate = '<table class="table table-striped cs-details-table"><thead><tr><th>Property</th><th>Value</th></tr></thead><tbody></tbody></table>';

