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
package edu.unifi.disit.datamanager.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.exception.AccessTokenNotValidException;
import edu.unifi.disit.datamanager.service.IActivityService;

@Component
public class AccessTokenAuthenticationFilter extends GenericFilterBean {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IActivityService activityService;

	@Value("${spring.openidconnect.userinfo_endpoint}")
	private String userinfo_endpoint;

	@Value("${spring.openidconnect.userinfo_endpoint_test}")
	private String userinfo_endpoint_test;

	@Autowired
	private MessageSource messages;

	ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {

		final HttpServletRequest req = (HttpServletRequest) request;
		String accessToken = req.getParameter("accessToken");

		ActivityAccessType activityType = ActivityAccessType.READ;
		if (((HttpServletRequest) request).getMethod().equals("POST"))
			activityType = ActivityAccessType.WRITE;

		if ((accessToken != null) && (accessToken.length() != 0)) {

			try {

				validation(accessToken, request.getLocale());
				logger.info("Access token IS VALID");

			} catch (AccessTokenNotValidException e) {

				logger.info("Access token NOT VALID");

				activityService.saveActivityViolationFromUsername(null, req.getParameter("sourceRequest"), req.getParameter("variableName"), req.getParameter("motivation"), activityType,
						((HttpServletRequest) request).getContextPath() + "?" + ((HttpServletRequest) request).getQueryString(),
						e.getMessage(), e, ((HttpServletRequest) request).getRemoteAddr());

				Response toreturn2 = new Response();
				toreturn2.setResult(false);
				toreturn2.setMessage(e.getMessage());

				((HttpServletResponse) response).setStatus(401);
				((HttpServletResponse) response).getWriter().write(objectMapper.writeValueAsString(toreturn2));

				return;
			}

		} else {

			logger.warn("Access token NOT PRESENT");

			activityService.saveActivityViolationFromUsername(null, req.getParameter("sourceRequest"), req.getParameter("variableName"), req.getParameter("motivation"), activityType,
					((HttpServletRequest) request).getContextPath() + "?" + ((HttpServletRequest) request).getQueryString(),
					messages.getMessage("login.ko.accesstokennotpresent", null, request.getLocale()), null, ((HttpServletRequest) request).getRemoteAddr());

			// Response toreturn2 = new Response();
			// toreturn2.setResult(false);
			// toreturn2.setMessage(messages.getMessage("login.ko.accesstokennotpresent", null, request.getLocale()));
			// ((HttpServletResponse) response).setStatus(401);
			// ((HttpServletResponse) response).getWriter().write(objectMapper.writeValueAsString(toreturn2));

		}
		filterChain.doFilter(request, response);// continue

	}

	private void authUser(String username, List<String> roles) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		// here we set jus a rule for the user
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, roles, authorities);

		SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
	}

	private void validation(String accesstoken, Locale lang) throws AccessTokenNotValidException {

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + accesstoken);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			checkValidationOnKeycloakServer(UriComponentsBuilder.fromHttpUrl(userinfo_endpoint).build(), restTemplate, entity, lang);// check validation on www.snap4city.org/auth keycloak server
		} catch (HttpClientErrorException e) {
			logger.warn("AccessToken WAS NOT VALIDATED HttpClientErrorException {}", e);
			try {
				checkValidationOnKeycloakServer(UriComponentsBuilder.fromHttpUrl(userinfo_endpoint_test).build(), restTemplate, entity, lang);// check validation on www.snap4city.org/auth keycloak server
			} catch (HttpClientErrorException e2) {
				logger.error("AccessToken WAS NOT VALIDATED even on keycloak TEST HttpClientErrorException {}", e2);
				throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
			} catch (JsonProcessingException e2) {
				logger.warn("AccessToken WAS NOT VALIDATED even on keycloak TEST  JsonProcessingException {}", e2);
				throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
			} catch (IOException e2) {
				logger.warn("AccessToken WAS NOT VALIDATED even on keycloak TEST  IOException {}", e2);
				throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
			}
		} catch (JsonProcessingException e) {
			logger.warn("AccessToken WAS NOT VALIDATED JsonProcessingException {}", e);
			throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		} catch (IOException e) {
			logger.warn("AccessToken WAS NOT VALIDATED IOException {}", e);
			throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}
	}

	private void checkValidationOnKeycloakServer(UriComponents uriComponents, RestTemplate restTemplate, HttpEntity<String> entity, Locale lang)
			throws JsonProcessingException, IOException, HttpClientErrorException, AccessTokenNotValidException {

		logger.info("AccessToken check on {}", uriComponents.toUri());

		ResponseEntity<String> re = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);

		String response = re.getBody();

		logger.debug("got response:{}", response);

		JsonNode jsonNode = objectMapper.readTree(response);

		// retrieve username
		String username = null;
		JsonNode jn = jsonNode.get("username");

		if (jn != null)
			username = jn.asText();

		jn = jsonNode.get("preferred_username");

		if ((username == null) && (jn != null))
			username = jn.asText();

		if (username == null) {
			logger.error("username not found");
			throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}

		// retrieve roles
		List<String> roles = new ArrayList<String>();
		Iterator<JsonNode> iroles = null;

		jn = jsonNode.get("roles");

		if (jn != null) {
			iroles = jn.elements();
		}

		jn = jsonNode.get("role");

		if ((iroles == null) && (jn != null)) {
			iroles = jn.elements();
		}

		if (iroles == null) {
			logger.error("roles not found");
			throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}

		while (iroles.hasNext())
			roles.add(iroles.next().asText());

		logger.info("AccessToken username {} + Roles {}", username, roles.toArray());

		authUser(username, roles);
	}
}
