/* IoT Ingestion (II).
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
package edu.unifi.disit.iotIngestion.controller.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.iotIngestion.service.IOrionBrokerService;

@RestController
public class PaxCounterController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IOrionBrokerService brokerservice;

	@RequestMapping(value = "/api/test", method = RequestMethod.GET)
	public ResponseEntity<String> engagerTest() {
		return new ResponseEntity<String>("alive", HttpStatus.OK);
	}

	// -------------------POST updateContext ---------------------------------------------
	@RequestMapping(value = "/v1/data/paxcounter", method = RequestMethod.POST, consumes = { "application/json" }, produces = { "application/json" })
	@ResponseBody
	public ResponseEntity<String> postPaxCounterV1(@RequestBody String payload, HttpServletRequest request) throws IOException {

		logger.info("Request received postPaxCounterV1 {} Payload {}", request.toString(), payload);

		// return new ResponseEntity<String>(HttpStatus.OK);

		return brokerservice.updatePaxCounterEntity(payload);
	}
}