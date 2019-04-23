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
import java.util.List;
import java.util.Locale;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.snap4city.engager_utils.RoleType;
import edu.unifi.disit.snapengager.exception.CredentialsException;

@Service
public class CredentialsServiceImpl implements ICredentialsService {

	private static final Logger logger = LogManager.getLogger();

	@Value("${spring.openidconnect.clientid}")
	private String clientId;

	@Value("${spring.openidconnect.username}")
	private String username;

	@Value("${spring.openidconnect.password}")
	private String password;

	@Value("${spring.openidconnect.endpoint}")
	private String endpoint;

	@Autowired
	private MessageSource messages;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private IDataManagerService dataService;

	String refreshToken = null;

	@SuppressWarnings("unchecked")
	@Override
	public void checkAssistanceEnabled(Locale lang) throws NoSuchMessageException, CredentialsException, IOException {
		List<String> roles = (List<String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();
		String usernameSC = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Boolean enabled = dataService.getAssistanceEnabled(lang).get(usernameSC);

		// assure the user has accepted to be assistant
		if (!roles.contains(RoleType.ROOTADMIN.toString()) && ((enabled == null) || (!enabled)))
			throw new CredentialsException(messages.getMessage("credentials.ko.notenabled", null, lang));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void checkUsernameCredentials(String username, Locale lang) throws NoSuchMessageException, CredentialsException {
		List<String> roles = (List<String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();
		String usernameSC = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		// assure the username is the same than the specified one
		if ((!usernameSC.equals(username)) && (!roles.contains(RoleType.ROOTADMIN.toString())))
			throw new CredentialsException(messages.getMessage("credentials.ko.usernameowner", null, lang));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void checkRootCredentials(Locale lang) throws NoSuchMessageException, CredentialsException {
		List<String> roles = (List<String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();
		// assure role is RootAdmin
		if (!roles.contains(RoleType.ROOTADMIN.toString()))
			throw new CredentialsException(messages.getMessage("credentials.ko.rights", null, lang));
	}

	@Override
	public String getLoggedUsername(Locale lang) {
		return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isRoot(Locale lang) {
		List<String> roles = (List<String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();

		return (roles.contains(RoleType.ROOTADMIN.toString()));
	}

	@Override
	public String getAccessToken(Locale lang) throws CredentialsException {

		if (refreshToken == null)
			getRefreshToken(lang);

		String toreturn = new String();

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("grant_type", "refresh_token");
		params.add("refresh_token", refreshToken);
		params.add("client_id", clientId);
		params.add("username", username);
		params.add("password", password);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(endpoint + "/token").build();
		logger.debug("Query getAccessToken {}", uriComponents.toUri());

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
				logger.error("The retrieved data does not contains access_token");
				throw new CredentialsException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			JsonNode rtNode = rootNode.path("refresh_token");

			if ((rtNode == null) || (rtNode.isNull()) || (rtNode.isMissingNode())) {
				refreshToken = null; // force to re-login
				logger.error("The retrieved data does not contains refresh_token");
				throw new CredentialsException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			refreshToken = rtNode.asText();
			toreturn = atNode.asText();
		} catch (HttpClientErrorException | IOException e) {
			refreshToken = null; // force to re-login
			logger.error("Trouble in getAccessToken", e);
			throw new CredentialsException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		return toreturn;
	}

	public void getRefreshToken(Locale lang) throws CredentialsException {

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("grant_type", "password");
		params.add("client_id", clientId);
		params.add("username", username);
		params.add("password", password);

		logger.debug("cl {}", clientId);
		logger.debug("un {}", username);
		logger.debug("pw {}", password);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(endpoint + "/token").build();
		logger.debug("Query getRefreshToken {}", uriComponents.toUri());

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
				throw new CredentialsException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			refreshToken = atNode.asText();
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getAccessToken", e);
			throw new CredentialsException(messages.getMessage("login.ko.networkproblems", null, lang));
		}
	}
}