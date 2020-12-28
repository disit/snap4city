/**
 *  Nifi EnrichData processor
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

package org.disit.nifi.processors.enrich_data.enrichment_source.http;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.nifi.processor.ProcessContext;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClient;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceException;

public abstract class HttpEnrichmentSourceClient implements EnrichmentSourceClient{

	//Http client
	protected PoolingHttpClientConnectionManager connectionManager;
	protected CloseableHttpClient httpClient;
//	protected ResponseHandler<String> responseHandler;
	
	protected HttpEnrichmentSourceClient( int maxPerRoute , int maxTotal ){
		connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setDefaultMaxPerRoute( maxPerRoute );
		connectionManager.setMaxTotal( maxTotal );
		
		httpClient = HttpClients.custom()
							    .setConnectionManager( connectionManager )
							    .build();
//		responseHandler = new BasicResponseHandler();
	}
	
	protected HttpEnrichmentSourceClient( ProcessContext context ){
		this( context.getMaxConcurrentTasks() , context.getMaxConcurrentTasks() );
	}
	
	protected HttpResponse executeRequest( HttpUriRequest request ) throws EnrichmentSourceException{
		try {
			HttpResponse response = httpClient.execute( request );
			return response;
		} catch (ClientProtocolException e) {
			throw new EnrichmentSourceException( "ClientProtocolException while retrieving data from HTTP source." , e );
		} catch (IOException e) {
			throw new EnrichmentSourceException( "IOException while retrieving data from HTTP source." , e );
		}
	}
	
	@Override
	public void close() throws EnrichmentSourceException{
		try {
			this.httpClient.close();
		} catch (IOException e) {
			throw new EnrichmentSourceException( "IOException while closing http client." , e ); 
		} finally {
			this.connectionManager.close();
		}
	}
}
