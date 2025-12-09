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


package org.disit.nifi.processors.ingest_data.logging;

import java.util.function.Function;

import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.logging.LogLevel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utilities class for logging.
 */
public class LoggingUtils {

	private LoggingUtils() { }
	private static final JsonParser parser = new JsonParser();
	
	/**
	 * Produce a LoggableObject containing the given reason as 'reason'.
	 * @param reason
	 * @return a LoggableObject
	 */
	public static final LoggableObject produceErrorObj( String reason ) {
		JsonObject errorObj = new JsonObject();
		errorObj.addProperty( "reason" , reason );
		return new LoggableObject(errorObj);
	}
	
	/**
	 * Produce a LoggableObject containing the given reason as 'reason' and object content as 'content'.
	 * 
	 * @param reason
	 * @param contentObjStr 
	 * @return a LoggableObject
	 */
	public static final LoggableObject produceErrorObj( String reason ,  String contentObjStr ) {
		LoggableObject lo = produceErrorObj( reason );
		lo.errorObj.add( "content" , parser.parse( contentObjStr ) );
		return lo;
	}
	
	/**
	 * Produce a LoggableObject containing the given reason as 'reason' and object content as 'content'.
	 * 
	 * @param reason
	 * @param contentEl
	 * @return
	 */
	public static final LoggableObject produceErrorObj( String reason , JsonElement contentEl ) {
		LoggableObject lo = produceErrorObj( reason );
		lo.errorObj.add( "content" , contentEl );
		return lo;
	}
	
	/**
	 * Inner class as facility for methods chaining.
	 */
	public static class LoggableObject{
		
		public JsonObject errorObj; // encapsulated error obj
		public LoggableObject( JsonObject errorObj ) {
			this.errorObj = errorObj;
		}
		
		/**
		 * Error object manipulation methods.
		 * These methods returns a reference to the LoggableObject
		 * in order to allow calls chaining. 
		 */
		
		/**
		 * Applies a Function<JsonObject, JsonObject> to the errorObj contained in this LoggableObject.
		 * The function result is assigned to the errorObj contained in this LoggableObject.
		 * 
		 * @param func
		 * @return
		 */
		public LoggableObject applyFunction( Function<JsonObject, JsonObject> func ) {
			this.errorObj = func.apply( this.errorObj );
			return this;
		}
		
		/**
		 * Adds the Throwable info as 'exception' field to the errorObj contained in this LoggableObject.
		 * 
		 * @param t
		 * @return
		 */
		public LoggableObject withExceptionInfo( Throwable t ) {
			this.errorObj.addProperty( "exception" , t.toString() );
			if( t.getCause() != null ) {
				this.errorObj.addProperty( "cause" , t.getCause().toString() );
			}
				
			return this;
		}
		
		/** 
		 * Adds a string property with the given name and value to the errorObject contained in this LoggableObject.
		 * 
		 * @param name
		 * @param value
		 * @return
		 */
		public LoggableObject withProperty( String name , String value ) {
			this.errorObj.addProperty( name , value);
			return this;
		}
		
		/* Logging Methods (chain terminal methods)*/
		public void logAsError( ComponentLog logger ) {
			logger.error( this.errorObj.toString() );
		}
		
		public void logAsWarning( ComponentLog logger ) {
			logger.warn( this.errorObj.toString() );
		}
		
		public void logAsInfo( ComponentLog logger ) {
			logger.info( this.errorObj.toString() );
		}
		
		public void log( LogLevel level , ComponentLog logger ) {
			logger.log( level , this.errorObj.toString() );
		}
	}
	
}
