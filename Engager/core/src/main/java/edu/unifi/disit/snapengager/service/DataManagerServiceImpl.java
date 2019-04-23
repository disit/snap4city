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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.snapengager.datamodel.Data;
import edu.unifi.disit.snapengager.datamodel.DatasetType;
import edu.unifi.disit.snapengager.datamodel.KPIData;
import edu.unifi.disit.snapengager.datamodel.KPIDataType;
import edu.unifi.disit.snapengager.datamodel.Organization;
import edu.unifi.disit.snapengager.datamodel.drupaldb.DrupalData;
import edu.unifi.disit.snapengager.datamodel.drupaldb.DrupalDataDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.Event;
import edu.unifi.disit.snapengager.exception.CredentialsException;

@Service
public class DataManagerServiceImpl implements IDataManagerService {

	private static final Logger logger = LogManager.getLogger();

	@Value("${datamanager.endpoint}")
	private String datamanagerEndpoint;

	@Value("${dashboard.endpoint}")
	private String dashboardEndpoint;

	@Autowired
	ICredentialsService credservice;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	ILastUpdateService laservice;

	@Autowired
	private DrupalDataDAO drupaldatarepo;

	// ------------------------public

	@Override
	public List<Event> getEventData(String organization, Locale lang) throws CredentialsException, IOException {
		return getSMdata(organization, lang);
	}

	@Override
	public List<DrupalData> getLastDrupalData(Locale lang) {
		Date from = laservice.getLastUpdate(DatasetType.DRUPAL, lang);
		List<DrupalData> toreturn = getDrupalData(from, lang);
		if (toreturn.size() != 0)
			laservice.updateLastUpdate(DatasetType.DRUPAL, new Date(toreturn.get(toreturn.size() - 1).getCreated() * 1000), lang);
		return toreturn;
	}

	@Override
	public List<KPIData> getPpoiKpidata(Locale lang) throws CredentialsException, IOException {
		// Date from = laservice.getLastUpdate(DatasetType.PPOI, lang);
		// List<KPIData> toreturn = getKpidata(null, "MyPOI", lang);
		// if (toreturn.size() != 0)
		// laservice.updateLastUpdate(DatasetType.PPOI, toreturn.get(toreturn.size() - 1).getInsertTime(), lang);
		// return toreturn;
		return getKpidata(null, "MyPOI", null, lang);
	}

	@Override
	public List<KPIData> getLocationKpidata(Locale lang) throws CredentialsException, IOException {
		return getKpidata(null, "MyKPI", KPIDataType.S4CHelsinkiTrackerLocation.toString(), lang);
	}

	@Override
	public List<Data> getLastLoginData(Locale lang) throws CredentialsException, IOException {
		Date from = laservice.getLastUpdate(DatasetType.DEVICE_LASTLOGIN, lang);
		List<Data> toreturn = getData(from, null, DatasetType.DEVICE_LASTLOGIN.toString(), lang);
		if (toreturn.size() != 0)
			laservice.updateLastUpdate(DatasetType.DEVICE_LASTLOGIN, toreturn.get(toreturn.size() - 1).getDataTime(), lang);
		return toreturn;
	}

	@Override
	public List<Data> getSurveyData(Locale lang) throws CredentialsException, IOException {
		Date from = laservice.getLastUpdate(DatasetType.SURVEY_RESPONSE, lang);
		List<Data> toreturn = getData(from, null, DatasetType.SURVEY_RESPONSE.toString(), lang);
		if (toreturn.size() != 0)
			laservice.updateLastUpdate(DatasetType.SURVEY_RESPONSE, toreturn.get(toreturn.size() - 1).getDataTime(), lang);
		return toreturn;
	}

	@Override
	public List<Data> getSubscriptionData(Locale lang) throws CredentialsException, IOException {
		// Date from = laservice.getLastUpdate(DatasetType.SENSOR_SUBSCRIPTION, lang);
		// List<Data> toreturn = getData(from, null, DatasetType.SENSOR_SUBSCRIPTION.toString(), lang);
		// if (toreturn.size() != 0)
		// laservice.updateLastUpdate(DatasetType.SENSOR_SUBSCRIPTION, toreturn.get(toreturn.size() - 1).getDataTime(), lang);
		// return toreturn;
		return getData(null, null, DatasetType.SENSOR_SUBSCRIPTION.toString(), lang);
	}

	@Override
	public Hashtable<String, Boolean> getAssistanceEnabled(Locale lang) throws CredentialsException, IOException {
		Hashtable<String, Boolean> toreturn = new Hashtable<String, Boolean>();
		List<Data> datas = getData(null, null, DatasetType.ASSISTANCE_ENABLED.toString(), lang);
		for (Data data : datas)
			toreturn.put(data.getUsername(), Boolean.valueOf(data.getVariableValue()));
		return toreturn;
	}

	// ---------------------private

	private List<Data> getData(Date from, String variablename, String motivation, Locale lang) throws CredentialsException, IOException {

		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		ObjectMapper mapper = new ObjectMapper();
		List<Data> toreturn = new ArrayList<Data>();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it

		HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(datamanagerEndpoint + "/username/ANY/data");
		if (variablename != null)
			uriComponentsBuilder = uriComponentsBuilder.query("variableName=" + variablename);
		if (motivation != null)
			uriComponentsBuilder = uriComponentsBuilder.query("motivation=" + motivation);
		if (from != null)
			uriComponentsBuilder = uriComponentsBuilder.query("from=" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(from));
		uriComponentsBuilder = uriComponentsBuilder.query("accessToken=" + credservice.getAccessToken(lang));
		uriComponentsBuilder = uriComponentsBuilder.query("sourceRequest=engager");
		logger.debug("Query Datamanager {}", uriComponentsBuilder.build().toUri());

		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, httpEntity, String.class);
		logger.debug("Response from Datamanager {}", response);

		if ((!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) && ((response != null) && (response.getBody() != null) && (response.getBody().getBytes() != null))) {
			toreturn = mapper.readValue(response.getBody().getBytes(), new TypeReference<List<Data>>() {
			});
		}

		return toreturn;
	}

	private List<KPIData> getKpidata(Date from, String highLevelType, String valueName, Locale lang) throws CredentialsException, IOException {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

		ObjectMapper mapper = new ObjectMapper();

		List<KPIData> toreturn = new ArrayList<KPIData>();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it

		HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(datamanagerEndpoint + "/kpidata");
		if (highLevelType != null)
			uriComponentsBuilder = uriComponentsBuilder.query("highLevelType=" + highLevelType);
		if (valueName != null)
			uriComponentsBuilder = uriComponentsBuilder.query("valueName=" + valueName);
		// if (from != null)
		// uriComponentsBuilder = uriComponentsBuilder.query("from=" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(from));
		uriComponentsBuilder = uriComponentsBuilder.query("accessToken=" + credservice.getAccessToken(lang));
		uriComponentsBuilder = uriComponentsBuilder.query("sourceRequest=engager");
		logger.debug("Query KPI Datamanager {}", uriComponentsBuilder.build().toUri());

		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, httpEntity, String.class);
		logger.debug("Response from KPI Datamanager {}", response);

		if ((!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) && ((response != null) && (response.getBody() != null) && (response.getBody().getBytes() != null))) {
			toreturn = mapper.readValue(response.getBody().getBytes(), new TypeReference<List<KPIData>>() {
			});
		}

		// filter manually "valueName"
		if (valueName != null)
			toreturn = filterValueName(toreturn, valueName);

		// filter manually "from"
		if (from != null)
			toreturn = filterFrom(toreturn, from);

		return toreturn;
	}

	private List<KPIData> filterFrom(List<KPIData> datas, Date from) {
		List<KPIData> toreturn = new ArrayList<KPIData>();
		for (KPIData data : datas) {
			if (data.getInsertTime().after(from))
				toreturn.add(data);
		}
		return toreturn;
	}

	private List<KPIData> filterValueName(List<KPIData> datas, String valueName) {
		List<KPIData> toreturn = new ArrayList<KPIData>();
		for (KPIData data : datas) {
			if (data.getValueName().equalsIgnoreCase(KPIDataType.S4CHelsinkiTrackerLocation.toString()))
				toreturn.add(data);
		}
		return toreturn;
	}

	// now return just service map data
	private List<Event> getSMdata(String organizationString, Locale lang) throws IOException {
		Organization organization = getOrganizationInfo(organizationString, lang);

		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

		List<Event> toreturn = new ArrayList<Event>();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it

		HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(organization.getKbUrl() + "events/");
		uriComponentsBuilder = uriComponentsBuilder.query("range=day");
		uriComponentsBuilder = uriComponentsBuilder.query("maxDists=10");
		uriComponentsBuilder = uriComponentsBuilder.query("selection=" + organization.getGpsCentreLatLng().replaceAll(",", ";"));

		logger.debug("Query SM {}", uriComponentsBuilder.build().toUri());

		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, httpEntity, String.class);
		logger.debug("Response from SM {}", response);

		if ((!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) && ((response != null) && (response.getBody() != null) && (response.getBody().getBytes() != null))) {
			toreturn = parsaEvent(response.getBody(), organizationString);
		}

		return toreturn;
	}

	private Organization getOrganizationInfo(String organization, Locale lang) throws IOException {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

		ObjectMapper mapper = new ObjectMapper();

		List<Organization> toreturn = new ArrayList<Organization>();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it

		HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(dashboardEndpoint + "organizations.php");
		if (organization != null)
			uriComponentsBuilder = uriComponentsBuilder.query("org=" + organization);

		logger.debug("Query ORganization {}", uriComponentsBuilder.build().toUri());

		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, httpEntity, String.class);
		logger.debug("Response from Organization {}", response);

		if ((!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) && ((response != null) && (response.getBody() != null) && (response.getBody().getBytes() != null))) {
			toreturn = mapper.readValue(response.getBody().getBytes(), new TypeReference<List<Organization>>() {
			});
		}

		if (toreturn.size() == 0)
			throw new IOException("organization not found");
		if (toreturn.size() > 1)
			logger.warn("more than one organization found");

		return toreturn.get(0);
	}

	private List<Event> parsaEvent(String response, String organizationString) throws IOException {

		List<Event> toreturn = new ArrayList<Event>();

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		// read JSON like DOM Parser
		JsonNode rootNode = objectMapper.readTree(response.getBytes());
		JsonNode serviceNode = rootNode.path("Event");
		JsonNode featNode = serviceNode.path("features");

		toreturn = objectMapper.readValue(featNode.toString(), new TypeReference<List<Event>>() {
		});

		for (Event event : toreturn) {
			event.setOrganization(organizationString);
		}

		return toreturn;

	}

	private List<DrupalData> getDrupalData(Date from, Locale lang) {
		// List<DrupalData> dds = drupaldatarepo.findAll();
		List<DrupalData> dds = drupaldatarepo.findByCreatedGreaterThan(Math.round(from.getTime() / 1000));
		for (DrupalData dd : dds)
			dd.toString();
		return dds;
	}

	// return just one, randomly
	/*
	 * private List<EVENT> parsaEvent(String response) throws JsonProcessingException, IOException { List<EVENT> toreturn = new ArrayList<EVENT>();
	 * 
	 * if ((response != null) && (response.length() != 0)) { // create ObjectMapper instance ObjectMapper objectMapper = new ObjectMapper();
	 * 
	 * // read JSON like DOM Parser JsonNode rootNode = objectMapper.readTree(response.getBytes()); JsonNode serviceNode = rootNode.path("Event"); JsonNode featNode = serviceNode.path("features");
	 * 
	 * Iterator<JsonNode> elements = featNode.elements(); while (elements.hasNext()) { JsonNode feature = elements.next();
	 * 
	 * EVENT event = new EVENT(); POI p = new POI(); LOCATION l = new LOCATION();
	 * 
	 * JsonNode geometries = feature.path("geometry"); Iterator<JsonNode> GPS_element = geometries.get("coordinates").elements(); int gps_index = 0; while (GPS_element.hasNext()) { JsonNode GPS = GPS_element.next(); if (gps_index == 0) {
	 * l.setGpsLongitude(GPS.doubleValue()); } else { l.setGpsLatitude(GPS.doubleValue()); } gps_index++; }
	 * 
	 * JsonNode properties = feature.path("properties");
	 * 
	 * p.setName(properties.get("name").textValue()); p.setServiceUri(properties.get("serviceUri").textValue()); p.setTypeLabel(properties.get("categoryIT").textValue()); // beware, this is specific for Italian
	 * p.setServiceType(properties.get("serviceType").textValue()); event.setPlace(properties.get("place").textValue()); event.setEndData(properties.get("endDate").textValue());
	 * 
	 * if (properties.get("price").isNull()) event.setPrice(0f); else event.setPrice(properties.get("price").floatValue());
	 * 
	 * p.setLocation(l); event.setPoi(p);
	 * 
	 * toreturn.add(event); }
	 * 
	 * // randomize, so we always take a new one Collections.shuffle(toreturn, new Random(System.nanoTime()));
	 * 
	 * if (toreturn.size() > 1) toreturn = toreturn.subList(0, 1); } else logger.warn("Null value to PARSE or EMPTY to parsaEvent, return empty");
	 * 
	 * return toreturn; }
	 */

}