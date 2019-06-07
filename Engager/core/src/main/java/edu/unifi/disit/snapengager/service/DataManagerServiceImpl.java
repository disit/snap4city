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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.snap4city.engager_utils.OrganizationType;
import edu.unifi.disit.snapengager.datamodel.Data;
import edu.unifi.disit.snapengager.datamodel.DatasetType;
import edu.unifi.disit.snapengager.datamodel.Organization;
import edu.unifi.disit.snapengager.datamodel.Poi;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIData;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIDataType;
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

	@Value("${servicemap.squid.endpoint}")
	private String servicemapEndpoint;

	@Autowired
	ICredentialsService credservice;

	@Autowired
	RestTemplate restTemplate;// = new RestTemplate();

	@Autowired
	ILastUpdateService laservice;

	@Autowired
	private DrupalDataDAO drupaldatarepo;

	// ------------------------public
	@Override
	public List<Event> getEventData(OrganizationType organizationType, Locale lang) throws CredentialsException, IOException {
		return getSMdataEvent(organizationType, lang);
	}

	@Override
	public List<Poi> getPoiData(String organization, String latitude, String longitude, Locale lang) throws CredentialsException, IOException {
		return getSMdataPoi(organization, latitude, longitude, lang);
	}

	@Override
	public List<DrupalData> getDrupalData(Locale lang) {
		return drupaldatarepo.findAll();
	}

	@Override
	public List<KPIData> getPpoiKpidata(Locale lang) throws CredentialsException, IOException {
		return getKpidata(null, "MyPOI", null, lang);
	}

	@Override
	public List<KPIData> getLocationKpidata(Locale lang) throws CredentialsException, IOException {
		List<KPIData> toreturn = getKpidata(null, "MyKPI", KPIDataType.S4CHelsinkiTrackerLocation.toString(), lang);
		toreturn.addAll(getKpidata(null, "MyKPI", KPIDataType.S4CAntwerpTrackerLocation.toString(), lang));
		toreturn.addAll(getKpidata(null, "MyKPI", KPIDataType.S4CTuscanyTrackerLocation.toString(), lang));
		return toreturn;
	}

	@Override
	public List<Data> getLangData(Locale lang) throws CredentialsException, IOException {
		return getData(null, null, DatasetType.USER_LANGUAGE.toString(), null, null, lang);
	}

	@Override
	public List<Data> getSubscriptionData(Locale lang) throws CredentialsException, IOException {
		return getData(null, null, DatasetType.SENSOR_SUBSCRIPTION.toString(), null, null, lang);
	}

	@Override
	public List<Data> getLastLoginData(Locale lang) throws CredentialsException, IOException {
		Date from = laservice.getLastUpdate(DatasetType.DEVICE_LASTLOGIN, lang);
		List<Data> toreturn = getData(from, null, DatasetType.DEVICE_LASTLOGIN.toString(), null, null, lang);
		if (toreturn.size() != 0)
			laservice.updateLastUpdate(DatasetType.DEVICE_LASTLOGIN, toreturn.get(toreturn.size() - 1).getDataTime(), lang);
		return toreturn;
	}

	@Override
	public List<Data> getAllSurveyData(Locale lang) throws CredentialsException, IOException {
		return getData(null, null, DatasetType.SURVEY.toString(), null, null, lang);
	}

	@Override
	public List<Data> getSurveyData(Locale lang) throws CredentialsException, IOException {
		Date from = laservice.getLastUpdate(DatasetType.SURVEY, lang);
		List<Data> toreturn = getData(from, null, DatasetType.SURVEY.toString(), null, null, lang);
		if (toreturn.size() != 0)
			laservice.updateLastUpdate(DatasetType.SURVEY, toreturn.get(toreturn.size() - 1).getDataTime(), lang);
		return toreturn;
	}

	@Override
	public Hashtable<String, Boolean> getAssistanceEnabled(Locale lang) throws CredentialsException, IOException {
		Hashtable<String, Boolean> toreturn = new Hashtable<String, Boolean>();
		List<Data> datas = getData(null, null, DatasetType.ASSISTANCE_ENABLED.toString(), null, null, lang);
		for (Data data : datas)
			toreturn.put(data.getUsername(), Boolean.valueOf(data.getVariableValue()));
		return toreturn;
	}

	// ---------------------private

	private List<Data> getData(Date from, String variablename, String motivation, String username, Integer first, Locale lang) throws CredentialsException, IOException {

		if (username == null)
			username = "ANY";

		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		ObjectMapper mapper = new ObjectMapper();
		List<Data> toreturn = new ArrayList<Data>();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it

		HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(datamanagerEndpoint + "/username/" + username + "/data");
		if (variablename != null)
			uriComponentsBuilder = uriComponentsBuilder.query("variableName=" + variablename);
		if (motivation != null)
			uriComponentsBuilder = uriComponentsBuilder.query("motivation=" + motivation);
		if (from != null)
			uriComponentsBuilder = uriComponentsBuilder.query("from=" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(from));
		if (first != null)
			uriComponentsBuilder = uriComponentsBuilder.query("first=" + first);
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
			if (data.getValueName().equalsIgnoreCase(valueName))
				toreturn.add(data);
		}
		return toreturn;
	}

	// now return just service map data
	private List<Event> getSMdataEvent(OrganizationType organizationType, Locale lang) throws IOException {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		Organization organization = getOrganizationInfo(organizationType, lang);

		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

		List<Event> toreturn = new ArrayList<Event>();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it
		httpHeaders.setCacheControl("max-age=2592000");// 30days (60sec * 60min * 24hours * 30days)

		HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(servicemapEndpoint + "events/");
		uriComponentsBuilder = uriComponentsBuilder.query("range=month");
		uriComponentsBuilder = uriComponentsBuilder.query("maxDists=10");
		uriComponentsBuilder = uriComponentsBuilder.query("timestamp=" + sdf.format(new Date()));// to avoid caching
		uriComponentsBuilder = uriComponentsBuilder.query("selection=" + organization.getGpsCentreLatLng().replaceAll(",", ";"));

		logger.debug("Query SM {}", uriComponentsBuilder.build().toUri());

		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, httpEntity, String.class);
		logger.debug("Response from SM {}", response);

		if ((!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) && ((response != null) && (response.getBody() != null) && (response.getBody().getBytes() != null))) {
			toreturn = parsaEvent(response.getBody(), organizationType);
		}

		return toreturn;
	}

	private List<Poi> getSMdataPoi(String organizationString, String latitude, String longitude, Locale lang) throws IOException {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

		List<Poi> toreturn = new ArrayList<Poi>();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it

		HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(servicemapEndpoint);
		uriComponentsBuilder = uriComponentsBuilder.query("selection=" + latitude.replaceAll(",", ".") + ";" + longitude.replaceAll(",", "."));
		uriComponentsBuilder = uriComponentsBuilder.query("categories=Gardens;Green_areas;Accommodation;CulturalActivity;WineAndFood;Entertainment;ShoppingAndService");
		// Gardens;Green_areas --> used for feedback on Entertainment_Gardens
		// Accommodation;CulturalActivity;WineAndFood;
		uriComponentsBuilder = uriComponentsBuilder.query("maxResults=100");
		uriComponentsBuilder = uriComponentsBuilder.query("maxDists=0.2");
		uriComponentsBuilder = uriComponentsBuilder.query("format=json");
		logger.debug("Query SM {}", uriComponentsBuilder.build().toUri());

		ResponseEntity<String> response = restTemplate.exchange(uriComponentsBuilder.build().toUri(), HttpMethod.GET, httpEntity, String.class);
		logger.debug("Response from SM {}", response);

		if ((!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) && ((response != null) && (response.getBody() != null) && (response.getBody().getBytes() != null))) {
			toreturn = parsaPoi(response.getBody());
		}

		return toreturn;
	}

	@Override
	public Organization getOrganizationInfo(OrganizationType organizationType, Locale lang) throws IOException {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

		ObjectMapper mapper = new ObjectMapper();

		List<Organization> toreturn = new ArrayList<Organization>();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setOrigin("http://localhost");// TODO adjust it

		HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(dashboardEndpoint + "organizations.php");

		uriComponentsBuilder = uriComponentsBuilder.query("org=" + organizationType.toString());

		logger.debug("Query Organization {}", uriComponentsBuilder.build().toUri());

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

	private List<Event> parsaEvent(String response, OrganizationType organizationType) throws IOException {

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
			event.setOrganization(organizationType.toString());
		}

		return toreturn;
	}

	private List<Poi> parsaPoi(String response) throws JsonProcessingException, IOException {
		List<Poi> toreturn = new ArrayList<Poi>();

		if ((response != null) && (response.length() != 0)) {
			// create ObjectMapper instance
			ObjectMapper objectMapper = new ObjectMapper();

			// read JSON like DOM Parser
			JsonNode rootNode = objectMapper.readTree(response.getBytes());
			JsonNode serviceNode = rootNode.path("Services");
			JsonNode featNode = serviceNode.path("features");

			toreturn = objectMapper.readValue(featNode.toString(), new TypeReference<List<Poi>>() {
			});

		}
		return toreturn;
	}

}