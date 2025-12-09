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


package org.disit.nifi.processors.enrich_data;

import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.disit.nifi.processors.enrich_data.enrichment_source.EnrichmentSourceClientService;

public class EnrichmentSourceServiceValidators {
	
	private EnrichmentSourceServiceValidators() { }

	
	public static final Validator STANDARD_ENRICHMENT_SOURCE_VALIDATOR = new Validator() {
		@Override
		public ValidationResult validate(String subject, String input, ValidationContext context) {
			ValidationResult.Builder builder = new ValidationResult.Builder();
			
			try {
				context.getProperty( EnrichDataProperties.ENRICHMENT_SOURCE_CLIENT_SERVICE )
					   .asControllerService( EnrichmentSourceClientService.class );
				builder.subject( subject ).explanation( "Valid EnrichmentSourceClientService" )
					   .valid( true );
			} catch( IllegalArgumentException e ) {
				builder.subject( subject ).explanation( "Invalid EnrichmentSourceClientService" )
					   .valid( false );
			}
			
			return builder.build();
		}
	};
	
	public static final Validator ENRICHMENT_SOURCE_VALIDATOR = new Validator() {
		@Override
		public ValidationResult validate(String subject, String input, ValidationContext context) {
			ValidationResult.Builder builder = new ValidationResult.Builder();
			
			try {
				context.getProperty( EnrichDataProperties.ENRICHMENT_SOURCE_CLIENT_SERVICE )
					   .asControllerService( EnrichmentSourceClientService.class );
				builder.subject( subject ).explanation( "Valid EnrichmentSourceClientService" )
					   .valid( true );
			} catch( IllegalArgumentException e ) {
				builder.subject( subject ).explanation( "Invalid EnrichmentSourceClientService" )
					   .valid( false );
			}
			
			return builder.build();
		}
	};

}
