/* Orion Broker Filter (OBF).
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
package edu.unifi.disit.orionbrokerfilter.controller.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class OrionController {

	private static final Logger logger = LogManager.getLogger();

	@Value("${spring.orionbroker_endpoint}")
	private String orionbroker_endpoint;

	@Autowired
	RestTemplate restTemplate;

	@RequestMapping(value = "/api/test", method = RequestMethod.GET)
	public ResponseEntity<String> engagerTest() {
		return new ResponseEntity<String>("alive", HttpStatus.OK);
	}

	// -------------------POST queryContex ---------------------------------------------
	@RequestMapping(value = "/v1/queryContext", method = RequestMethod.POST, consumes = { "application/json" }, produces = { "application/json" })
	@ResponseBody
	public ResponseEntity<String> queryContextV1(@RequestBody String payload, @RequestParam(value = "limit", required = false) String limit,
			@RequestParam(value = "details", required = false) String details,
			@RequestHeader HttpHeaders headers) {

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		if (limit != null)
			params.add("limit", limit);
		if (details != null)
			params.add("details", details);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(orionbroker_endpoint + "/v1/queryContext")
				.queryParams(params)
				.build();

		return proxyPostRequest(uriComponents, payload, headers);
	}

	// -------------------POST updateContext ---------------------------------------------
	@RequestMapping(value = "/v1/updateContext", method = RequestMethod.POST, consumes = { "application/json" }, produces = { "application/json" })
	@ResponseBody
	public ResponseEntity<String> updateContextV1(@RequestBody String payload, @RequestHeader HttpHeaders headers) {

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(orionbroker_endpoint + "/v1/updateContext")
				.build();

		return proxyPostRequest(uriComponents, payload, headers);
	}

	// -------------------POST subscribeContext ---------------------------------------------
	@RequestMapping(value = "/v1/subscribeContext", method = RequestMethod.POST, consumes = { "application/json" }, produces = { "application/json" })
	@ResponseBody
	public ResponseEntity<String> subscribeContextV1(@RequestBody String payload, @RequestHeader HttpHeaders headers) {

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(orionbroker_endpoint + "/v1/subscribeContext")
				.build();

		return proxyPostRequest(uriComponents, payload, headers);
	}

	// -------------------POST unsubscribeContext ---------------------------------------------
	@RequestMapping(value = "/v1/unsubscribeContext", method = RequestMethod.POST, consumes = { "application/json" }, produces = { "application/json" })
	@ResponseBody
	public ResponseEntity<String> unsubscribeContextV1(@RequestBody String payload, @RequestHeader HttpHeaders headers) {

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(orionbroker_endpoint + "/v1/unsubscribeContext")
				.build();

		return proxyPostRequest(uriComponents, payload, headers);
	}

	private ResponseEntity<String> proxyPostRequest(UriComponents uriComponents, String payload, HttpHeaders headers) {
		logger.info("Proxying request to {} on {}", uriComponents.toString(), payload);

		// RestTemplate restTemplate = new RestTemplate();
		HttpEntity<String> entity = new HttpEntity<>(payload, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.POST, entity, String.class);
			logger.debug(response);
			return response;
		} catch (HttpClientErrorException e) {
			logger.error("Trouble in proxyPostRequest", e);
			return new ResponseEntity<String>(e.getMessage(), e.getStatusCode());
		} catch (Exception e) {
			logger.error("BIG Trouble in proxyPostRequest", e);
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}