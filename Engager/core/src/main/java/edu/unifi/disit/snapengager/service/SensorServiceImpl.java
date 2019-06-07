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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import javax.annotation.PostConstruct;

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

import edu.unifi.disit.snap4city.engager_utils.OrganizationType;
import edu.unifi.disit.snapengager.datamodel.Organization;
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
	IDataManagerService dmservice;

	@Autowired
	private MessageSource messages;

	@Autowired
	private SensorDAO sensorRepo;

	@Autowired
	private RestTemplate restTemplate;

	private final HashMap<OrganizationType, Organization> organizations = new HashMap<OrganizationType, Organization>();

	@Override
	public Date getLastDate(String mapname, Locale lang) throws IOException {

		Date toreturn = null;

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it
		HttpEntity<Void> entity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(sensorEndpoint + "/heatmap.php?dataset=" + mapname + "&sort=desc&limit=1");
		logger.debug("Query Sensor {}", uriComponentsBuilder.build().toUri());

		// RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
		// List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		// interceptors.add(new LoggingRequestInterceptor());
		// restTemplate.setInterceptors(interceptors);
		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, entity, String.class);
		logger.debug("Response from Sensor {}", response);

		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
		Iterator<JsonNode> els = rootNode.elements();

		if ((els == null) || (!els.hasNext())) {
			logger.warn("The retrieved data does not contains any elements");
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
		//
		// HttpHeaders headers = new HttpHeaders();
		// HttpEntity<String> entity = new HttpEntity<>(headers);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
				.fromHttpUrl(sensorEndpoint + "/interp.php?latitude=" + sensore.getLatitude() + "&longitude=" + sensore.getLongitude() + "&dataset=" + sensore.getMapname() + "&date=" + format.format(sensore.getInsertdate()));
		logger.debug("Query Sensor {}", uriComponentsBuilder.build().toUri());

		// RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
		// List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		// interceptors.add(new LoggingRequestInterceptor());
		// restTemplate.setInterceptors(interceptors);
		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, null, String.class);
		logger.debug("Response from Sensor {}", response);

		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());

		JsonNode valueNode = rootNode.path("value");
		if ((valueNode == null) || (valueNode.isNull()) || (valueNode.isMissingNode())) {
			logger.error("The retreived data does not contains value");
			throw new IOException(messages.getMessage("sensor.ko.notvalidresponse", new Object[] { "value" }, lang));
		}

		if (valueNode.asText().length() != 0) {
			sensore.setValue((float) valueNode.asDouble());
			sensorRepo.save(sensore);
		} else
			logger.warn("Empty value returned for {}. Ignoring", sensore);
	}

	@Override
	public boolean checkSensorValidity(String heatmapname, String latit, String longit) {

		if (heatmapname.indexOf(OrganizationType.HELSINKI.toString()) != -1) {
			if (distance(latit, longit, organizations.get(OrganizationType.HELSINKI).getGpsCentreLatLng()) < 100d)
				return true;
		} else if (heatmapname.indexOf(OrganizationType.ANTWERP.toString()) != -1) {
			if (distance(latit, longit, organizations.get(OrganizationType.ANTWERP).getGpsCentreLatLng()) < 100d)
				return true;
		} else if (heatmapname.indexOf("Florence".toString()) != -1) {// we used Florence and not Firenze here
			if (distance(latit, longit, organizations.get(OrganizationType.FIRENZE).getGpsCentreLatLng()) < 100d)
				return true;
		} else {
			logger.warn("cannot check validity of heatmap {}. Returning false", heatmapname);

		}

		return false;
	}

	private Double distance(String latid, String longit, String gpsCentreLatLng) {

		Integer i = gpsCentreLatLng.indexOf(",");

		Double d = SensorServiceImpl.distance(Double.valueOf(latid), Double.valueOf(longit), Double.valueOf(gpsCentreLatLng.substring(0, i)), Double.valueOf(gpsCentreLatLng.substring(i + 1)), "K");

		logger.debug("distance between {} {} and {} {} is {}", latid, longit, Double.valueOf(gpsCentreLatLng.substring(0, i)), Double.valueOf(gpsCentreLatLng.substring(i + 1)), d);

		return d;
	}

	private static final Double distance(Double lat1, Double lon1, Double lat2, Double lon2, String unit) {

		if ((lat1.doubleValue() == lat2.doubleValue()) && (lon1.doubleValue() == lon2.doubleValue()))
			return 0d;

		Double theta = lon1 - lon2;
		Double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		if (unit == "K") {
			dist = dist * 1.609344;
		} else if (unit == "N") {
			dist = dist * 0.8684;
		}

		if (Double.isNaN(dist))
			dist = 0d;

		return (dist);
	}

	private static final Double deg2rad(Double deg) {
		return (deg * Math.PI / 180.0);
	}

	private static final Double rad2deg(Double rad) {
		return (rad * 180 / Math.PI);
	}

	@PostConstruct
	private void init() throws IOException {
		logger.debug("Init organization info");

		organizations.put(OrganizationType.ANTWERP, dmservice.getOrganizationInfo(OrganizationType.ANTWERP, new Locale("en")));
		organizations.put(OrganizationType.HELSINKI, dmservice.getOrganizationInfo(OrganizationType.HELSINKI, new Locale("en")));
		organizations.put(OrganizationType.FIRENZE, dmservice.getOrganizationInfo(OrganizationType.FIRENZE, new Locale("en")));

		logger.debug("Organization info are:");
		Iterator<OrganizationType> i = organizations.keySet().iterator();
		while (i.hasNext()) {
			OrganizationType o = i.next();
			logger.debug("{} -> {}", o, organizations.get(o));
		}
	}
}