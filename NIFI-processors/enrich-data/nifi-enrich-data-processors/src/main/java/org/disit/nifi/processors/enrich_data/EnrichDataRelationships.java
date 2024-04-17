package org.disit.nifi.processors.enrich_data;

import org.apache.nifi.processor.Relationship;

public class EnrichDataRelationships {

	
	// Relationships
	public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
        .name("SUCCESS_RELATIONSHIP")
        .description("The correctly enriched flow files will be routed to this relationship." )
        .build();
	
	public static final Relationship FAILURE_RELATIONSHIP = new Relationship.Builder()
		.name("FAILURE_RELATIONSHIP")
		.description("Flow files which cannot be correctly enriched will be routed to this relationship." )
		.build();
	
	public static final Relationship ORIGINAL_RELATIONSHIP = new Relationship.Builder()
		.name("original")
		.autoTerminateDefault( true )
		.description("The original incoming flow file is routed to this relationship." )
		.build();

	public static final Relationship RETRY_RELATIONSHIP = new Relationship.Builder()
		.name( "retry" )
		.description( "Flow files which cannot be correctly enriched but are considered retriable will be routed to this relationship.\nA flow file is considered retriable if the cause of the failure is an enrichment service unavailability." )
		.build();

	public static final Relationship DEVICE_STATE_RELATIONSHIP = new Relationship.Builder()
		.name( "device state" )
		.autoTerminateDefault( true )
		.description( "If this relationship is not auto-terminated, the processor will also produce flow file containing the enriched data in a format suitable to represent the sensor state. Such flow files will be routed to this relationship." )
		.build();

	private EnrichDataRelationships() { } 
}
