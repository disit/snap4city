/* Data Manager (DM).
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
package edu.unifi.disit.datamanager.controller.rest;

import java.util.HashMap;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.ActivityDomainType;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.IConfigurationService;

@RestController
public class ConfigController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IActivityService activityService;

	@Autowired
	IConfigurationService configurationService;

	// -------------------GET alive ---------------------------------------------
	@GetMapping(value = "/api/test")
	public ResponseEntity<String> controllerTest() {
		return new ResponseEntity<>("alive", HttpStatus.OK);
	}

	// -------------------GET configuration ---------------------------------------------
	@GetMapping(value = "/api/configuration/{version}")
	public ResponseEntity<Object> getConfiguration(
			@RequestParam("sourceRequest") String sourceRequest,
			@PathVariable("version") String version,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested getConfigurationV1 for {}, version {}, lang {}", sourceRequest, version, lang);

		HashMap<String, String> config = configurationService.getConfiguration(version, lang);

		activityService.saveActivityFromUsername(null, null, sourceRequest, null, null, ActivityAccessType.READ, ActivityDomainType.ACTIVITY);

		if ((config == null) || (config.isEmpty())) {
			logger.info("No config found");
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} else {
			logger.info("Returning config {}", config.toString());
			return new ResponseEntity<>(config,
					HttpStatus.OK);
		}
	}
}