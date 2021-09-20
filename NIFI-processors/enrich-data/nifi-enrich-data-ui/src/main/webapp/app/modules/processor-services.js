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
export class ProcessorServices {
	
	/**
	 * Setup method.
	 */
	static setup(){
		// Add authentication header to requests if a token is found
		var authToken = ProcessorServices.getAuthenticationToken();
		if( authToken != null ){
			$.ajaxSetup({
				"headers" : { 'Authorization' : 'Bearer ' + authToken }
			});
		}
	}
	
	/**
	 * Retrieves the authentication token from the nf.Storage module if any,
	 * or null if there are no access tokens in the NiFi storage.
	 */
	static getAuthenticationToken(){
		if( typeof nf !== typeof undefined && nf != null ){
			var token = null;
			if( nf.Storage.hasItem('jwt') ){
				token = nf.Storage.getItem('jwt')
			}
			return token;
		}else{
			throw "'nf' object undefined or null, check the '../nifi/js/nf/nifi-namespace.js' import."
		}
	}
	
	/**
	 * Retrieve processor details from the custom endpoint.
	 * 
	 * GET api/enrich-data/details?processorId=<PROCESSOR ID>
	 * 
	 * @return a jQuery.ajax containing the GET request. 
	 */
	static getProcessorDetails( processorId ){
		return $.get('api/enrich-data/details?processorId=' + processorId );
	}
	
	/**
	 * Retrieve processor details from the  "nifi-api/processors" endpoint.
	 * 
	 * GET nifi-api/processors/<PROCESSOR ID>
	 * 
	 * @return a jQuery.ajax containing the GET request.
	 */
	static getProcessorInfo( processorId ){
		return $.get( '../nifi-api/processors/' + processorId );
	}
	
	/**
	 * Retrieve the list of available controller services in the specified process group from the 
	 * "nifi-api/flow/processor-group/<PROCESS GROUP ID>/controller-services" endpoint.
	 * 
	 * GET nifi-api/flow/processor-group/<PROCESS GROUP ID>/controller-services
	 * 
	 * @return a jQuery.ajax containing the GET request.
	 */
	static getControllerServices( processGroupId ){
		return $.get( '../nifi-api/flow/process-groups/' + processGroupId + '/controller-services' );
	}
	
	/**
	 * Retrieve the variables defined in the VariableRegistry for the process group with the 
	 * supplied id.
	 * 
	 * GET nifi-api/process-groups/<PROCESS GROUP ID>
	 * 
	 * @return a jQuery.ajax object containing the GET request.
	 */
	static getGroupVariableRegistry( processGroupId ){
		return $.get( '../nifi-api/process-groups/' + processGroupId );
	}
	
	/**
	 * Set processor configuration properties.
	 * 
	 * PUT api/enrich-data/properties?processorId=<PROCESSOR ID>&revisionId=<REVISION ID>&clientId=<CLIENT ID>&disconnectedNodeAcknowledged=<true|false>
	 * 
	 * @return a jQuery.ajax containing the PUT request.
	 */
	static setProperties( processorId , revisionId , clientId , disconnectedNodeAcknowledged , properties ){
		var urlParams = 'processorId=' + processorId 
					  + '&revisionId=' + revisionId 
					  + '&clientId=' + clientId 
					  + '&disconnectedNodeAcknowledged=' + disconnectedNodeAcknowledged;
		return $.ajax( {
			url: 'api/enrich-data/properties?' + urlParams,
			method: "PUT",
			contentType: "application/json" ,
			data: JSON.stringify( properties ) 
		});
	}
	
	/**
	 * Runs a remote test for the EnrichData processor using the specified configuration.
	 * 
	 * POST api/enrich-data/test?processorId=<PROCESSOR ID>&groupId=<PROCESS GROUP ID>
	 * 
	 * @return a jQuery.ajax containing the POST request.
	 */
	static remoteTest( processorId , groupId , testInput ){
		var urlParams = 'processorId=' + processorId 
				      + '&groupId=' + groupId; 
		return $.ajax({
			url: 'api/enrich-data/test?' + urlParams , 
			method: "POST" ,
			contentType: "application/json" ,
			data: JSON.stringify( testInput )
		});
	}
}