/* <NOME COMPONENTE>.
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package edu.unifi.disit.orionbrokerfilter.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class StartupApplicationListener implements
		ApplicationListener<ContextRefreshedEvent> {

	private static final Logger logger = LogManager.getLogger();

	@Value("${spring.servicemapkb_endpoint:#{null}}")
	private String servicemapkb_endpoint;

	@Value("${cors.origins.accepted:#{null}}")
	private String originsAccepted;

	@Value("${spring.prefix_serviceuri}")
	private String prefixServiceuri;

	@Value("${spring.organization}")
	private String organization;

	@Value("${spring.context_broker_name}")
	private String contextBrokerName;

	@Value("${multitenancy:false}")
	private Boolean multitenancy;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		logger.info("OrionBrokerFilter UP and RUNNING");
		if (servicemapkb_endpoint != null)
			logger.info("ServiceMap scenario enabled on {}", servicemapkb_endpoint);
		if (originsAccepted != null)
			logger.info("cors enabled on {}", originsAccepted);
		logger.info("Prefix ServiceURI {}", prefixServiceuri);
		logger.info("Organization {}", organization);
		logger.info("Context Broker Name {}", contextBrokerName);

		if (multitenancy)
			logger.info("Support MT/SP enabled");
		else
			logger.info("Support MT/SP disabled");
		logger.info("Support v2 enabled");
		logger.info("Auth Bear Support enabled");
	}
}