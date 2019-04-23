/* Snap4City Engager (SE)
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
package edu.unifi.disit.snapengager.service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.snapengager.datamodel.profiledb.Sensor;
import edu.unifi.disit.snapengager.datamodel.profiledb.SensorDAO;

@Service
public class SensorServiceImpl implements ISensorService {

	private static final Logger logger = LogManager.getLogger();

	@Value("${sensor.endpoint}")
	private String sensorEndpoint;

	@Autowired
	ICredentialsService credservice;

	@Autowired
	private MessageSource messages;

	@Autowired
	private SensorDAO sensorRepo;

	@Autowired
	private RestTemplate restTemplate;

	@Override
	public Date getLastDate(String mapname, Locale lang) throws IOException {

		Date toreturn = null;

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it
		HttpEntity<Void> entity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(sensorEndpoint + "/heatmap.php?dataset=" + mapname + "&sort=desc&limit=1");
		logger.debug("Query Sensor {}", uriComponentsBuilder.build().toUri());

		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, entity, String.class);
		logger.debug("Response from Sensor {}", response);

		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
		Iterator<JsonNode> els = rootNode.elements();

		if ((els == null) || (!els.hasNext())) {
			logger.error("The retrieved data does not contains any elements");
			throw new IOException(messages.getMessage("sensor.ko.notvalidresponse", new Object[] { "element" }, lang));
		}

		JsonNode elNode = els.next();

		try {
			toreturn = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss").parse(elNode.asText());
		} catch (ParseException e) {
			logger.error("The retrieved data does not contains a valid date {}", elNode.asText());
			throw new IOException(messages.getMessage("sensor.ko.notvalidresponse", new Object[] { "date" }, lang));
		}

		return toreturn;
	}

	@Override
	public void update(Sensor sensore, Locale lang) throws IOException {

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
				.fromHttpUrl(sensorEndpoint + "/interp.php?latitude=" + sensore.getLatitude() + "&longitude=" + sensore.getLongitude() + "&dataset=" + sensore.getMapname() + "&date=" + format.format(sensore.getInsertdate()));
		logger.debug("Query Sensor {}", uriComponentsBuilder.build().toUri());

		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, entity, String.class);
		logger.debug("Response from Sensor {}", response);

		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());

		JsonNode valueNode = rootNode.path("value");
		if ((valueNode == null) || (valueNode.isNull()) || (valueNode.isMissingNode())) {
			logger.error("The retreived data does not contains value");
			throw new IOException(messages.getMessage("sensor.ko.notvalidresponse", new Object[] { "value" }, lang));
		}

		sensore.setValue((float) valueNode.asDouble());

		sensorRepo.save(sensore);
	}
}