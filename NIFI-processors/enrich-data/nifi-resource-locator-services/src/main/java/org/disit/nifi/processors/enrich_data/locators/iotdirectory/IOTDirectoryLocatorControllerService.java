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

package org.disit.nifi.processors.enrich_data.locators.iotdirectory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocator;
import org.disit.nifi.processors.enrich_data.locators.EnrichmentResourceLocatorService;

public class IOTDirectoryLocatorControllerService extends AbstractControllerService implements EnrichmentResourceLocatorService {

	public static final String DEFAULT_SUBSCRIPTION_ID_ATTRIBUTE_NAME_VALUE = "subscriptionId";
	public static final String DEFAULT_SUBSCRIPTION_ID_REQUEST_NAME_VALUE = "sub_ID";
	public static final String DEFAULT_SERVICE_URI_PREFIX_RESPONSE_PATH_VALUE = "content/serviceUriPrefix";
	public static final String DEFAULT_ORGANIZATION_RESPONSE_PATH_VALUE = "content/organization";
	public static final String DEFAULT_CB_NAME_RESPONSE_PATH_VALUE = "content/name";
	public static final String DEFAULT_MAX_CACHE_SIZE_VALUE = "50";
	
	protected static final List<PropertyDescriptor> descriptors;
	
	public static final PropertyDescriptor IOTDIRECTORY_URL = new PropertyDescriptor
		.Builder().name( "IOTDIRECTORY_URL" )
		.displayName( "IOTDirectory URL" )
		.description( "The URL of the IOTDirectory service" )
		.required( true )
		.expressionLanguageSupported( ExpressionLanguageScope.VARIABLE_REGISTRY )
		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
		.build();
	
	public static final PropertyDescriptor SUBSCRIPTION_ID_ATTRIBUTE_NAME = new PropertyDescriptor
		.Builder().name( "SUBSCRIPTION_ID_ATTRIBUTE_NAME" )
		.displayName( "Subscription ID attribute name" )
		.description( "The name of the flow file attribute containing the subscription ID." )
		.required( true )
		.defaultValue( DEFAULT_SUBSCRIPTION_ID_ATTRIBUTE_NAME_VALUE )
		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
		.build();
	
	public static final PropertyDescriptor SUBSCRIPTION_ID_REQUEST_NAME = new PropertyDescriptor
		.Builder().name( "SUBSCRIPTION_ID_REQUEST_PARAM_NAME" )
		.displayName( "Subscription ID request param name" )
		.description( "The name of the subscription id parameter in the request to the IOTDirectory service (using the query string)." )
		.required( true )
		.defaultValue( DEFAULT_SUBSCRIPTION_ID_REQUEST_NAME_VALUE )
		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
		.build();
	
	public static final PropertyDescriptor ADDITIONAL_QUERY_STRING = new PropertyDescriptor
		.Builder().name("ADDITIONAL QUERY STRING" )
		.displayName( "Additional query string" )
		.description( "An additional part to be appended to the query string for the the IOTDirectory service request." )
		.required( false )
		.expressionLanguageSupported( ExpressionLanguageScope.VARIABLE_REGISTRY )
		.addValidator( Validator.VALID )
		.build();

	public static final PropertyDescriptor SERVICE_URI_PREFIX_RESPONSE_PATH = new PropertyDescriptor
		.Builder().name( "SERVICE_URI_PREFIX_RESPONSE_PATH" )
		.displayName( "Service Uri prefix response path" )
		.description( "The path inside the JSON response from the IOTDirectory service containing the service uri prefix (forward '/' slash syntax)." )
		.required( true )
		.defaultValue( DEFAULT_SERVICE_URI_PREFIX_RESPONSE_PATH_VALUE )
		.addValidator( StandardValidators.NON_EMPTY_VALIDATOR )
		.build();
	
	public static final PropertyDescriptor ORGANIZATION_RESPONSE_PATH = new PropertyDescriptor
		.Builder().name( "ORGANIZATION_RESPONSE_PATH" )
		.displayName( "Organization response path" )
		.description( "The path inside the JSON response from the IOTDirectory service containing the organization name. This property is used together with 'Context Broker Name response path' to determine the ownership prefix. Unset this property or 'Context Broker Name response path' to disable the automatic ownership prefix." )
		.required( false )
		.defaultValue( DEFAULT_ORGANIZATION_RESPONSE_PATH_VALUE )
		.addValidator( Validator.VALID )
		.build();
	
	public static final PropertyDescriptor CB_NAME_RESPONSE_PATH = new PropertyDescriptor
		.Builder().name( "CB_NAME_RESPONSE_PATH" )
		.displayName( "Context Broker Name response path" )
		.description( "The path inside the JSON response from the IOTDirectory service containing the context broker name. This property is used together with 'Organization Name response path' to determine the ownership prefix. Unset this property or 'Organization response path' to disable the automatic ownership prefix." )
		.required( false )
		.defaultValue( DEFAULT_CB_NAME_RESPONSE_PATH_VALUE )
		.addValidator( Validator.VALID )
		.build();
	
	public static final PropertyDescriptor MAX_CACHE_SIZE = new PropertyDescriptor
		.Builder().name( "MAX_CACHE_SIZE" )
		.displayName( "Max cache size" )
		.defaultValue( DEFAULT_MAX_CACHE_SIZE_VALUE )
		.description( "The maximum number of service uri prefixes cached in memory by every processor using this locator service, the eviction is done on the oldest used entry. Set this to 0 to disable the caching of prefixes." )
		.required( false )
		.addValidator( StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR )
		.build();
	
	public static final PropertyDescriptor EXPIRE_CACHE_ENTRIES_TIME = new PropertyDescriptor
		.Builder().name( "EXPIRE_CACHE_ENTRIES_TIME" )
		.displayName( "Expire cache entries after" )
		.description( "If set, this property specifies for how much time the service uri prefixes will be cached. When the cache entries epiration time elapses, the entry will be evicted from the cache and such service uri will be retrieved with a request to the IOTDirectory service." )
		.required( false )
		.addValidator( StandardValidators.createTimePeriodValidator( 1 , TimeUnit.SECONDS , 365 , TimeUnit.DAYS ) )
		.build();	
	
	static {
		final List<PropertyDescriptor> descs = new ArrayList<>();
		
		descs.add( IOTDIRECTORY_URL );
		descs.add( SUBSCRIPTION_ID_ATTRIBUTE_NAME );
		descs.add( SUBSCRIPTION_ID_REQUEST_NAME );
		descs.add( ADDITIONAL_QUERY_STRING );
		descs.add( SERVICE_URI_PREFIX_RESPONSE_PATH );
		descs.add( ORGANIZATION_RESPONSE_PATH );
		descs.add( CB_NAME_RESPONSE_PATH );
		descs.add( MAX_CACHE_SIZE );
		descs.add( EXPIRE_CACHE_ENTRIES_TIME );
		
		descriptors = Collections.unmodifiableList( descs );
	}
	
	protected IOTDirectoryResourceLocatorConfig config;
	
	@Override
	protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return descriptors;
	}
	
	@OnEnabled
	public void onEnabled( final ConfigurationContext context ) throws InitializationException {
//		String iotDirectoryUrl = context.getProperty( IOTDIRECTORY_URL ).getValue();
//		String subIdAttributeName = context.getProperty( SUBSCRIPTION_ID_ATTRIBUTE_NAME ).getValue();
//		String serviceUriPrefixResponsePath = context.getProperty( SERVICE_URI_PREFIX_RESPONSE_PATH ).getValue();
//		String additionalQueryString = context.getProperty( ADDITIONAL_QUERY_STRING ).getValue();
		
		String additionalQueryString = context.getProperty( ADDITIONAL_QUERY_STRING ).evaluateAttributeExpressions().getValue();
		if( additionalQueryString == null )
			additionalQueryString = "";
		
		String organizationResponsePath = null;
		if( context.getProperty( ORGANIZATION_RESPONSE_PATH ).isSet() )
			organizationResponsePath = context.getProperty(ORGANIZATION_RESPONSE_PATH).getValue();
		
		String cbNameResponsePath = null;
		if( context.getProperty( CB_NAME_RESPONSE_PATH ).isSet() )
			cbNameResponsePath = context.getProperty(CB_NAME_RESPONSE_PATH).getValue();
		
		IOTDirectoryResourceLocatorConfig config = new IOTDirectoryResourceLocatorConfig( 
			context.getProperty( IOTDIRECTORY_URL ).evaluateAttributeExpressions().getValue() , 
			context.getProperty( SUBSCRIPTION_ID_ATTRIBUTE_NAME ).getValue() ,
			context.getProperty( SUBSCRIPTION_ID_REQUEST_NAME ).getValue() , 
			additionalQueryString ,
			context.getProperty( SERVICE_URI_PREFIX_RESPONSE_PATH ).getValue() ,
			organizationResponsePath ,
			cbNameResponsePath
		);
		config.setMaxCacheSize( Long.parseLong( context.getProperty( MAX_CACHE_SIZE ).getValue() ) );
		if( context.getProperty( EXPIRE_CACHE_ENTRIES_TIME ).isSet() ) {
			long expireCacheTime = context.getProperty( EXPIRE_CACHE_ENTRIES_TIME ).asTimePeriod(TimeUnit.MILLISECONDS).longValue();
			config.setCacheExpireEntriesAfter( expireCacheTime );
		}
		
		this.config = config;
	}
	
	@OnDisabled
	public void onDisabled() { }
	
	@Override
	public EnrichmentResourceLocator getResourceLocator( ProcessContext context ) {
		return new IOTDirectoryResourceLocator( config , context );
	}
	
	
	
}
