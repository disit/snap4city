/**
 * @author Panconi Christian ( panconi.christian@gmail.com )
 * 
 * This class contains the configuration constants for the 
 * processor. ( Probably can be moved in IngestNGSI to keep all compact )
 * 
 */

package org.disit.nifi.processors.ingest_data;

public final class ConfigConstants {

	private ConfigConstants() { };
	
	public static String[] GET_TIMESTAMP_FROM_VALUES = { "flow-file-entry-date" , "content-field" };
	
}
