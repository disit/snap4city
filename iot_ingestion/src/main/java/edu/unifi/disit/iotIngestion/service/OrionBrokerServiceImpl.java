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
package edu.unifi.disit.iotIngestion.service;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.iotIngestion.datamodel.PaxCounter;
import edu.unifi.disit.iotIngestion.datamodel.PaxCounterIngested;

@Service
public class OrionBrokerServiceImpl implements IOrionBrokerService {

	private static final Logger logger = LogManager.getLogger();

	@Value("${spring.orionbroker_endpoint}")
	private String orionbroker_endpoint;

	@Autowired
	private RestTemplate restTemplate;

	@Override
	public ResponseEntity<String> updatePaxCounterEntity(String payload) throws IOException {

		payload = "{\"actionType\": \"APPEND\",\"entities\": [" + enrichPaxCounter(payload) + "]}";

		logger.debug(payload);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> httpEntity = new HttpEntity<String>(payload, httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(orionbroker_endpoint + "/v2/op/update");

		logger.debug("postNsgiRegistration {} Payload: {}", uriComponentsBuilder.build().toUri(), payload);

		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.POST, httpEntity, String.class);

		logger.debug("Response postNsgiRegistration {}", response);

		return response;
	}

	private String enrichPaxCounter(String payload) throws IOException {
		// transform from
		// {
		// "id": "300773207E3FFFFF",
		// "type": "PaxcounterObserved",
		// "dateObserved": "2019-03-19T04:52:52.652000+00:00Z",
		// "WiFi": 14,
		// "Bluetooth": 3,
		// "location": {
		// "coordinates": [
		// 24.949541,
		// 60.189692
		// ],
		// "type": "Point"
		// },
		// "address": {
		// "addressCountry": "FI",
		// "addressLocality": "Helsinki",
		// "streetAddress": "Park Sinebrychoff"
		// }
		// }

		ObjectMapper mapper = new ObjectMapper();

		PaxCounterIngested received = mapper.readValue(payload.getBytes(), new TypeReference<PaxCounterIngested>() {
		});

		// transform to
		// {
		// "id": "300773207E3FFFFF",
		// "type": "PaxcounterObserved",
		// "dateObserved": {
		// "type": "Text",
		// "value": "2019-03-19T04:52:52.652000+00:00Z"
		// },
		// "WiFi": {
		// "type": "Number",
		// "value": 2
		// },
		// "Bluetooth": {
		// "type": "Number",
		// "value": 2
		// },
		// "location": {
		// "type": "StructuredValue",
		// "value": {
		// "type": "Point",
		// "coordinates": [
		// 24.90313,
		// 60.161827
		// ]
		// }
		// },
		// "address": {
		// "type": "StructuredValue",
		// "value": {
		// "addressCountry": "FI",
		// "addressLocality": "Helsinki",
		// "streetAddress": "Tallberginkatu 1 C"
		// }
		// }
		// }

		PaxCounter sending = received.toPaxCounter();

		return mapper.writeValueAsString(sending);
	}

}