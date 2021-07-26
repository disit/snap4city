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
package org.disit.nifi.processors.enrich_data.web.api;

import org.apache.nifi.web.ComponentDetails;
import org.apache.nifi.web.HttpServletConfigurationRequestContext;
import org.apache.nifi.web.HttpServletRequestContext;
import org.apache.nifi.web.NiFiWebConfigurationContext;
import org.apache.nifi.web.NiFiWebConfigurationRequestContext;
import org.apache.nifi.web.NiFiWebRequestContext;
import org.apache.nifi.web.Revision;
import org.apache.nifi.web.UiExtensionType;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

public class ProcessorWebUtils {

    public static ComponentDetails getComponentDetails(final NiFiWebConfigurationContext configurationContext,final String processorId, HttpServletRequest request){
        final NiFiWebRequestContext requestContext = getRequestContext(processorId,request);
        return configurationContext.getComponentDetails(requestContext);
    }
    
    public static ComponentDetails getControllerServiceDetails( final NiFiWebConfigurationContext configurationContext , final String csId , HttpServletRequest request) {
    	final NiFiWebRequestContext requestContext = getCSRequestContext( csId , request );
    	return configurationContext.getComponentDetails( requestContext );
    }

    static Response.ResponseBuilder applyCacheControl(Response.ResponseBuilder response) {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setPrivate(true);
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return response.cacheControl(cacheControl);
    }

    static NiFiWebConfigurationRequestContext getRequestContext(final String processorId, final Long revision, final String clientId,
                                                                final Boolean isDisconnectionAcknowledged, HttpServletRequest request) {

        return new HttpServletConfigurationRequestContext(UiExtensionType.ProcessorConfiguration, request) {
            @Override
            public String getId() {
                return processorId;
            }

            @Override
            public Revision getRevision() {
                return new Revision(revision, clientId, processorId);
            }

            @Override
            public boolean isDisconnectionAcknowledged() {
                return Boolean.TRUE.equals(isDisconnectionAcknowledged);
            }
        };
    }


    private static NiFiWebRequestContext getRequestContext(final String processorId, HttpServletRequest request) {
        return new HttpServletRequestContext(UiExtensionType.ProcessorConfiguration, request) {
            @Override
            public String getId() {
                return processorId;
            }
        };
    }
    
    static NiFiWebConfigurationRequestContext getCSRequestContext( final String processorId , final Long revision , final String clientId,
			   													   final Boolean isDisconnectionAcknowledged, HttpServletRequest request) {

		return new HttpServletConfigurationRequestContext( UiExtensionType.ControllerServiceConfiguration, request) {
			@Override
			public String getId() {
				return processorId;
			}
		
			@Override
			public Revision getRevision() {
				return new Revision( revision , clientId , processorId );
			}
		
			@Override
			public boolean isDisconnectionAcknowledged() {
				return Boolean.TRUE.equals( isDisconnectionAcknowledged );
			}
		};
	}
    
    private static NiFiWebRequestContext getCSRequestContext( String processorId , HttpServletRequest request ) {
    	return new HttpServletRequestContext(UiExtensionType.ControllerServiceConfiguration , request) {
    		@Override
    		public String getId() {
    			return processorId;
    		}
    	};
    }


}

