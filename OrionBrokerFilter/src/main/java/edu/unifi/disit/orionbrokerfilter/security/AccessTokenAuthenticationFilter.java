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
package edu.unifi.disit.orionbrokerfilter.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import java.security.cert.X509Certificate;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.orionbrokerfilter.datamodel.CachedCredentials;
import edu.unifi.disit.orionbrokerfilter.datamodel.Credentials;
import edu.unifi.disit.orionbrokerfilter.datamodel.Ownership;
import edu.unifi.disit.orionbrokerfilter.datamodel.Response;
import edu.unifi.disit.orionbrokerfilter.exception.CredentialsNotValidException;

@Component
public class AccessTokenAuthenticationFilter extends GenericFilterBean {

	private static final Logger logger = LogManager.getLogger();

	@Value("${spring.openidconnect.clientid}")
	private String clientId;

	@Value("${spring.openidconnect.username}")
	private String username;

	@Value("${spring.openidconnect.password}")
	private String password;

	@Value("${spring.openidconnect.token_endpoint}")
	private String token_endpoint;

	@Value("${spring.ownership_endpoint}")
	private String ownership_endpoint;

	@Value("${spring.delegation_endpoint}")
	private String delegation_endpoint;

	@Value("${spring.servicemapkb_endpoint:#{null}}")
	private String servicemapkb_endpoint;

	@Value("${spring.prefixelementID}")
	private String prefixelementID;

	@Value("${spring.elapsingcache.minutes}")
	private Integer minutesElapsingCache;

	@Value("${multitenancy:false}")
	private Boolean multitenancy;

	@Autowired
	private MessageSource messages;

	ObjectMapper objectMapper = new ObjectMapper();

	HashMap<String, CachedCredentials> cachedCredentials = new HashMap<String, CachedCredentials>();

	HashMap<String, String> cachedPksha1UsernameOwnership = new HashMap<String, String>();

	HashMap<String, Ownership> cachedOwnership = new HashMap<String, Ownership>();

	@Autowired
	private RestTemplate restTemplate;

	String refreshToken = null;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws JsonProcessingException, IOException, ServletException {

		final HttpServletRequest req = (HttpServletRequest) request;
		MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest((HttpServletRequest) request);

		// retrieve eventually certicate
		String pksha1 = null;
		X509Certificate[] certs = (X509Certificate[]) multiReadRequest.getAttribute("javax.servlet.request.X509Certificate");
		if ((certs != null) && (certs.length > 0)) {
			String pk = new String(Base64.encode(certs[0].getPublicKey().getEncoded()), StandardCharsets.UTF_8);
			logger.debug("certificate arrived, public key is:" + pk);
			pksha1 = DigestUtils.sha1Hex(pk.getBytes());
			logger.debug("sha1 public key is: {}", pksha1);
		}

		// retrieve k1 k2 credentials
		String k1 = multiReadRequest.getParameter("k1");
		String k2 = multiReadRequest.getParameter("k2");

		// retrieve NGSI info
		String queryType = ((HttpServletRequest) request).getServletPath();
		String elementId = req.getParameter("elementid");

		// eventually enrich with Multi-tenancy info
		if (multitenancy) {// check always to be made
			if (req.getHeader("Fiware-ServicePath") != null)
				elementId = req.getHeader("Fiware-ServicePath") + "." + elementId;
			else
				elementId = "/." + elementId;
			if (req.getHeader("Fiware-Service") != null)
				elementId = req.getHeader("Fiware-Service") + "." + elementId;
			else
				elementId = "." + elementId;

			logger.debug("elementid became:" + elementId);
		}

		// retrieve eventually accessToken. The enforcement/management of this access token is still not used in the Orion Filter
		// the access is granted via k1/k2/certificate
		String accessToken = null;
		if ((req.getHeader("Authorization") != null) && (req.getHeader("Authorization").length() > 8)) {
			accessToken = req.getHeader("Authorization").substring(7);
			logger.debug("accessToken arrived:" + accessToken);
		}

		if ((elementId != null)) {

			logger.debug("Received a request of type {} for {}", queryType, elementId);
			if (k1 != null)
				logger.debug("K1 {}", k1);
			if (k2 != null)
				logger.debug("K2 {}", k2);

			try {

				String sensorName = null;

				if (req.getRequestURL().toString().indexOf("/v1/") >= 0) {
					logger.debug("Searching sensor name in API v1 body.");
					sensorName = getSensorNameV1(multiReadRequest, isWriteQuery(queryType), elementId);// can return null, the passed elementid is the original one
				} else if (req.getRequestURL().toString().indexOf("/v2/") >= 0) {
					logger.debug("Searching sensor name in API v2 body.");
					sensorName = getSensorNameV2(multiReadRequest, req);// can return null, the passed elementid is the original one
				} else
					throw new CredentialsNotValidException(messages.getMessage("login.ko.requesturlmalformed", null, multiReadRequest.getLocale()));

				if (sensorName != null)
					logger.debug("sensor's name {}", sensorName);

				// if (!queryType.contains("unsubscribeContext"))
				checkAuthorization(prefixelementID + ":" + elementId, k1, k2, queryType, sensorName, pksha1, request.getLocale());

				logger.debug("Credentials ARE VALID");

				authUser();

			} catch (CredentialsNotValidException e) {
				logger.error("Credentials ARE NOT VALID", e);

				writeResponseError(response, e.getMessage());

				return;
			}

		} else {
			logger.error("Credentials ARE NOT PRESENT");

			writeResponseError(response, messages.getMessage("login.ko.credentialsnotpresent", null, request.getLocale()));

			return;

		}

		filterChain.doFilter(multiReadRequest, response);// DO WE NEED IT???
	}

	private String getSensorNameV1(HttpServletRequest multiReadRequest, boolean isWriteQuery, String elementId) throws IOException, NoSuchMessageException, CredentialsNotValidException {

		String entityBody = IOUtils.toString(multiReadRequest.getInputStream(), StandardCharsets.UTF_8.toString());

		logger.debug("searching sensor name in --{}--", entityBody);

		// retrieve "attributes index
		int startIndex = entityBody.indexOf("attributes");
		if (startIndex == -1) {
			logger.warn(messages.getMessage("login.ko.sensornamenotpresent", null, multiReadRequest.getLocale()) + " entityBody is {}", entityBody);
			// throw new CredentialsNotValidException(messages.getMessage("login.ko.sensornamenotpresent", null, multiReadRequest.getLocale()));
			return null;
		}

		// calcolate startindex of sensor name
		if (isWriteQuery)
			startIndex = startIndex + 10 + 4 + 8;
		else {
			startIndex = startIndex + 10 + 3;
			if (entityBody.indexOf("\"", startIndex) == startIndex) {
				startIndex++;
			}
		}

		if (startIndex > entityBody.length()) {
			logger.warn(messages.getMessage("login.ko.sensornamenotvalid", null, multiReadRequest.getLocale()) + "(1) entityBody is {}", entityBody);
			// throw new CredentialsNotValidException(messages.getMessage("login.ko.sensornamenotvalid", null, multiReadRequest.getLocale()) + "(1)");
			return null;
		}

		int endIndex = entityBody.indexOf("\"", startIndex);
		if (endIndex == -1) {
			// if the attribute field is empty, the sensor name is empty
			logger.debug("detecting empty attributes, sensor name is empty-string (the delegation has to be formatted correctly)");
			return null;
		}

		if (endIndex - startIndex >= 3) {
			// a sensor name is retrieved -> it's a query or an update!
			// ensure the elementid passed as parameter is included in the entityBody
			if (entityBody.indexOf(elementId) == -1) {
				logger.warn(messages.getMessage("login.ko.elementidnotvalid", null, multiReadRequest.getLocale()) + "(2) entityBody is {}", entityBody);
				throw new CredentialsNotValidException(messages.getMessage("login.ko.elementidnotvalid", null, multiReadRequest.getLocale()));
			}

			return entityBody.substring(startIndex, endIndex);
		} else {
			return null;// no sensor name are retrieved -> it's a subscription or unsubscription!
		}
	}

	private String getSensorNameV2(HttpServletRequest multiReadRequest, HttpServletRequest req) throws IOException, NoSuchMessageException, CredentialsNotValidException {
		String attribute = null;
		int attributeStart;
		int attributeEnd;
		String entityBody = IOUtils.toString(multiReadRequest.getInputStream(), StandardCharsets.UTF_8.toString()).replace(" ", "");

		try {
			switch (req.getMethod()) {
			case "GET":// Query
				attribute = req.getParameter("attrs");
				if (attribute.indexOf(',') != -1) {
					attribute = attribute.substring(0, attribute.indexOf(','));// consider just the first one
				}
				;
				return attribute;
			case "POST":// Subscription
				int notificationIndex = entityBody.indexOf("notification");
				int attrsIndex = entityBody.indexOf("attrs", notificationIndex);
				attributeStart = entityBody.indexOf("[\"", attrsIndex) + 2;
				attributeEnd = entityBody.indexOf("\"", attributeStart);
				attribute = entityBody.substring(attributeStart, attributeEnd);
				if (entityBody.indexOf(req.getParameter("elementid")) == -1) {
					logger.warn(messages.getMessage("login.ko.elementidnotvalid", null, multiReadRequest.getLocale()) + " entityBody is {}", entityBody);
					throw new CredentialsNotValidException(messages.getMessage("login.ko.elementidnotvalid", null, multiReadRequest.getLocale()));
				}
				return attribute;
			case "PATCH":// Update
				attributeStart = entityBody.indexOf("{\"") + 2;
				attributeEnd = entityBody.indexOf("\":");
				attribute = entityBody.substring(attributeStart, attributeEnd).trim();
				return attribute;
			case "DELETE":// Unsubscription
				return null;
			}
		} catch (Exception err) {
			logger.warn("Attribute name not detected!");
			return null;
		}
		return null;
	}

	private void writeResponseError(ServletResponse response, String msg) throws JsonProcessingException, IOException {
		Response toreturn2 = new Response();
		toreturn2.setResult(false);
		toreturn2.setMessage(msg);

		((HttpServletResponse) response).setStatus(401);
		((HttpServletResponse) response).getWriter().write(objectMapper.writeValueAsString(toreturn2));
	}

	private void authUser() {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // here we just set the basic role to USER
		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken("username", "userpwd", authorities);// here we just set a fake password
		SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
	}

	private void checkAuthorization(String elementId, String k1, String k2, String queryType, String sensorName, String pksha1, Locale lang) throws CredentialsNotValidException, UnsupportedEncodingException {

		CachedCredentials cc = getCachedCredentials(elementId, sensorName, lang);

		if (cc.getIsPublic()) {
			if (isWriteQuery(queryType)) {
				logger.debug("The operation is WRITE on public");
				if (cc.getOwnerCredentials().isValid(k1, k2, pksha1)) {
					logger.debug("The owner credentials are valid");
					return;
				} else {
					logger.debug("The owner credential are NOT valid");
					throw new CredentialsNotValidException(messages.getMessage("login.ko.credentialsnotvalid", null, lang));
				}
			} else {
				logger.debug("The operation is READ on public");
				return;
			}
		} else {
			if (isWriteQuery(queryType)) {
				logger.debug("The operation is WRITE on private");
				if (cc.getOwnerCredentials().isValid(k1, k2, pksha1)) {
					logger.debug("The owner credentials are valid");
					return;
				} else {
					logger.debug("The owner credentials are NOT valid");
					throw new CredentialsNotValidException(messages.getMessage("login.ko.credentialsnotvalid", null, lang));
				}
			} else {
				logger.debug("The operation is READ on private");
				if (cc.getOwnerCredentials().isValid(k1, k2, pksha1)) {
					logger.debug("The owner credentials are valid");
					return;
				} else {
					// TODO if the elementID is private, the invoked query is a READ, and it's not the owner, first we need to check if the sensorID is PUBLIC (from cached and via packet inspection) and if not we need to validate the
					// delegation as below
					logger.debug("The owner credentials are not valid, check if there are any delegation");
					for (Credentials c : cc.getDelegatedCredentials()) {
						if (cc.getOwnerCredentials().getPksha1() == null) {// if the elementID is not protected with certificate, use k1, k2 enforcement
							if (c.isValid(k1, k2)) {
								logger.debug("One of the delegated credentials are valid, certificate not involved");
								return;
							}
						} else {// if the elementID is protected with certificate: (1) check if there is a pksha1 and (2) check the username delegated is the same of the username included in the certicate
							if ((pksha1 != null) && (c.isValid(getUsername(pksha1, lang)))) {
								logger.debug("One of the delegated credentials are valid, certificate involved");
								return;
							}
						}
					}
					logger.debug("None of the delegated credential are valid");
					throw new CredentialsNotValidException(messages.getMessage("login.ko.credentialsnotvalid", null, lang));
				}
			}
		}
	}

	private String getUsername(String pksha1, Locale lang) throws CredentialsNotValidException {
		String toreturn = null;
		if ((toreturn = cachedPksha1UsernameOwnership.get(pksha1)) != null)
			return toreturn;
		else {
			String accessToken = getAccessToken(lang);
			toreturn = getUsernamePKCredentials(accessToken, pksha1, lang);
			if (toreturn != null)
				cachedPksha1UsernameOwnership.put(pksha1, toreturn);

		}
		return toreturn;
	}

	private boolean isWriteQuery(String queryType) {
		return queryType.contains("updateContext");
	}

	private CachedCredentials getCachedCredentials(String elementId, String sensorName, Locale lang) throws CredentialsNotValidException, UnsupportedEncodingException {

		String accessToken = null;

		Ownership o = cachedOwnership.get(elementId);
		if (o == null) {
			logger.debug("ownership not found in cache");
		} else {
			logger.debug("ownership found in cache: {}", o);
			if (o.isElapsed()) {
				logger.debug("ownership remove from cache since not valid anymore");
				cachedOwnership.remove(elementId);
				o = null;
			}
		}

		if (o == null) {
			accessToken = getAccessToken(lang);
			o = getOwnership(accessToken, elementId, lang);
			cachedOwnership.put(elementId, o);
		}

		String sensorUri = (sensorName == null) ? elementId : o.getElementUrl() + "/" + sensorName;

		// retrieve cached credentials
		CachedCredentials cc = cachedCredentials.get(sensorUri);

		if (cc == null) {
			logger.debug("not found in cache");
		} else {
			logger.debug("found in cache: {}", cc);
			if (cc.isElapsed()) {
				logger.debug("remove from cache since not valid anymore");
				cachedCredentials.remove(sensorUri);
				cc = null;
			}
		}

		// if not found or invalidated, retrieve new credentials
		if (cc == null) {
			logger.debug("retrieving credentials");

			cc = new CachedCredentials(minutesElapsingCache);
			if (servicemapkb_endpoint != null)// if the servicemap endpoint is set, enable the check on public/private
				cc.setIsPublic(isPublicFromKB(o.getElementUrl(), lang));

			cc.setOwnerCredentials(o);

			if (accessToken == null)
				accessToken = getAccessToken(lang);

			cc = enrichDelegatedCredentials(cc, accessToken, sensorUri, lang);

			cachedCredentials.put(sensorUri, cc);
		}

		logger.debug("Cached credentials are: {}", cc);

		return cc;
	}

	// default value:private (old scenario from iotdirectory)
	// if there is a string that contains "public", in the ow field, it is considered public
	private boolean isPublicFromKB(String elementUrl, Locale lang) throws CredentialsNotValidException {

		String queryKB = "select distinct ?ow { " +
				"OPTIONAL {<" + elementUrl + "> km4c:ownership ?ow.}" + "}";// assume that any elementId here are on this orion broker

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("format", "json");
		params.add("timeout", "0");
		params.add("query", queryKB);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(servicemapkb_endpoint)
				.queryParams(params)
				.build();
		logger.debug("query isPublicFromKB {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from isPublicFromKB {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			if ((response.getBody() == null)) {
				logger.debug("no body found, it's private");
				return false;
			}

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());

			JsonNode resultNode = rootNode.path("results");
			if ((resultNode == null) || (resultNode.isNull()) || (resultNode.isMissingNode())) {
				logger.debug("no results found, it's private");
				return false;
			}

			JsonNode bindingsNode = resultNode.path("bindings");
			if ((bindingsNode == null) || (bindingsNode.isNull()) || (bindingsNode.isMissingNode())) {
				logger.debug("no bindings found, it's private");
				return false;
			}

			Iterator<JsonNode> els = bindingsNode.elements();

			if (els.hasNext()) {
				JsonNode owNode = els.next().path("ow");
				if ((owNode == null) || (owNode.isNull()) || (owNode.isMissingNode())) {
					logger.debug("no ow found, it's private");
					return false;
				}

				JsonNode valueNode = owNode.path("value");
				if ((valueNode == null) || (valueNode.isNull()) || (valueNode.isMissingNode())) {
					logger.debug("no value found, it's private");
					return false;
				}

				return valueNode.asText().contains("public");

			} else {
				logger.debug("bindings empty, it's private");
				return false;
			}

		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in isPublicFromKB", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}
	}

	private String getAccessToken(Locale lang) throws CredentialsNotValidException {

		// TODO: check validity of the refreshtoken. If it is elapsed, forse refreshToken = null

		if (refreshToken == null)
			getRefreshToken(lang);

		String toreturn = new String();

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("grant_type", "refresh_token");
		params.add("refresh_token", refreshToken);
		params.add("client_id", clientId);
		params.add("username", username);
		params.add("password", password);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(token_endpoint).build();
		logger.debug("query getAccessToken {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(params, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.POST, entity, String.class);
			logger.debug("Response from getAccessToken {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			JsonNode atNode = rootNode.path("access_token");

			if ((atNode == null) || (atNode.isNull()) || (atNode.isMissingNode())) {
				refreshToken = null; // force to re-login
				logger.error("The retrieved data does not contains access_token");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			JsonNode rtNode = rootNode.path("refresh_token");

			if ((rtNode == null) || (rtNode.isNull()) || (rtNode.isMissingNode())) {
				refreshToken = null; // force to re-login
				logger.error("The retrieved data does not contains refresh_token");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			refreshToken = rtNode.asText();
			toreturn = atNode.asText();
		} catch (HttpClientErrorException | IOException e) {
			refreshToken = null; // force to re-login
			logger.error("Trouble in getAccessToken", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		return toreturn;
	}

	public void getRefreshToken(Locale lang) throws CredentialsNotValidException {

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("grant_type", "password");
		params.add("client_id", clientId);
		params.add("username", username);
		params.add("password", password);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(token_endpoint).build();
		logger.debug("Query getRefreshToken {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(params, headers);

		try {

			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.POST, entity, String.class);
			logger.debug("Response from getRefreshToken {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			JsonNode atNode = rootNode.path("refresh_token");

			if ((atNode == null) || (atNode.isNull()) || (atNode.isMissingNode())) {
				logger.error("The retrieved data does not contains access_token");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			refreshToken = atNode.asText();
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getAccessToken", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}
	}

	private Ownership getOwnership(String accessToken, String elementId, Locale lang) throws CredentialsNotValidException {

		Ownership toreturn = null;

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("type", "IOTID");
		params.add("accessToken", accessToken);
		params.add("elementId", elementId);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(ownership_endpoint)
				.queryParams(params)
				.build();
		logger.debug("query getOwnerCredentials {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from  getOwnerCredentials {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			Iterator<JsonNode> els = rootNode.elements();

			if ((els == null) || (!els.hasNext())) {
				logger.error("The retrieved data does not contains any elements");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			JsonNode elNode = els.next();

			// username - has to be present
			JsonNode usernameNode = elNode.path("username");
			if ((usernameNode == null) || (usernameNode.isNull()) || (usernameNode.isMissingNode())) {
				logger.error("The retreived data does not contains username");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			// elementUrl - has to be present
			JsonNode elementUrlNode = elNode.path("elementUrl");
			if ((elementUrlNode == null) || (elementUrlNode.isNull()) || (elementUrlNode.isMissingNode())) {
				logger.error("The retreived data does not contains elementUrlNode");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			// elementDetails - has to be present
			JsonNode edNode = elNode.path("elementDetails");
			if ((edNode == null) || (edNode.isNull()) || (edNode.isMissingNode())) {
				logger.error("The retreived data does not contains elementDetails");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			// k1, k2 - has to be present
			JsonNode k1Node = edNode.path("k1");
			JsonNode k2Node = edNode.path("k2");
			if ((k1Node == null) || (k1Node.isNull()) || ((k2Node == null) || (k2Node.isNull())) || (k1Node.isMissingNode()) || (k2Node.isMissingNode())) {
				logger.error("The retreived data does not contains k1, k2");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			// publickeySHA1 - can be missing
			String pksha1 = null;
			JsonNode pksha1Node = elNode.path("publickeySHA1");
			if ((pksha1Node != null) && (!pksha1Node.isNull()) && (!pksha1Node.isMissingNode())) {
				pksha1 = pksha1Node.asText();
			}

			toreturn = new Ownership(elementUrlNode.asText(), k1Node.asText(), k2Node.asText(), usernameNode.asText(), pksha1, minutesElapsingCache);
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getOwnerCredentials", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		return toreturn;
	}

	private String getUsernamePKCredentials(String accessToken, String pksha1, Locale lang) throws CredentialsNotValidException {

		String toreturn = null;

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("type", "IOTID");
		params.add("accessToken", accessToken);
		params.add("pubkeySHA1", pksha1);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(ownership_endpoint)
				.queryParams(params)
				.build();
		logger.debug("query getOwnerCredentials {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from getOwnerCredentials {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			Iterator<JsonNode> els = rootNode.elements();

			if ((els == null) || (!els.hasNext())) {
				logger.error("The retrieved data does not contains any elements");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			JsonNode elNode = els.next();
			JsonNode usernameNode = elNode.path("username");

			if ((usernameNode == null) || (usernameNode.isNull()) || (usernameNode.isMissingNode())) {
				logger.error("The retreived data does not contains usernmae");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			toreturn = usernameNode.asText();

		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getOwnerCredentials", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		return toreturn;
	}

	private CachedCredentials enrichDelegatedCredentials(CachedCredentials cc, String accessToken, String sensorUri, Locale lang) throws CredentialsNotValidException, UnsupportedEncodingException {

		List<Credentials> toreturn = new ArrayList<Credentials>();

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("accessToken", accessToken);
		params.add("sourceRequest", "orionbrokerfilter");

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(delegation_endpoint + "/" + URLEncoder.encode(sensorUri, StandardCharsets.UTF_8.toString()) + "/delegator")
				.queryParams(params)
				.build();
		logger.debug("query getDelegatedCredentials {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from getDelegatedCredentials {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			if (response.getBody() != null) {// 204 no content body
				JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
				Iterator<JsonNode> els = rootNode.elements();

				while ((els != null) && (els.hasNext())) {
					JsonNode elNode = els.next();

					JsonNode udNode = elNode.path("usernameDelegated");
					if ((udNode == null) || (udNode.isNull()) || (udNode.isMissingNode())) {
						logger.error("The retrieved data does not contains userDelegated");
						// throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
						continue;
					}
					if (udNode.asText().equals("ANONYMOUS"))
						cc.setIsPublic(true);

					String ud = udNode.asText();

					JsonNode deNode = elNode.path("delegationDetails");

					if ((deNode == null) || (deNode.isNull()) || (deNode.isMissingNode())) {
						logger.debug("The retrieved data does not contains delegationDetails (probably it's PUBLIC)");
						// throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
						continue;
					}

					logger.debug("delegation details are:" + deNode.asText());

					// delegation details are a string that has to be decoded
					JsonNode deNodeParsed = objectMapper.readTree(deNode.asText());

					JsonNode k1Node = deNodeParsed.path("k1");
					JsonNode k2Node = deNodeParsed.path("k2");

					if ((k1Node == null) || (k1Node.isNull()) || ((k2Node == null) || (k2Node.isNull())) || (k1Node.isMissingNode()) || (k2Node.isMissingNode())) {
						logger.error("The retreived data does not contains k1, k2");
						// throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
						continue;
					}

					logger.debug("k1 is:" + k1Node.asText());
					logger.debug("k2 is:" + k2Node.asText());

					toreturn.add(new Credentials(k1Node.asText(), k2Node.asText(), ud, null));
				}
			}
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getDelegatedCredentials", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		cc.setDelegatedCredentials(toreturn);

		return cc;
	}
}