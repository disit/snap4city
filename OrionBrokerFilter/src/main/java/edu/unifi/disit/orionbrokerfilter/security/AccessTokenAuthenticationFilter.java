/* Orion Broker Filter (OBF).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
package edu.unifi.disit.orionbrokerfilter.security;

import java.io.IOException;
import java.util.Iterator;
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
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	@Autowired
	private MessageSource messages;

	ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {

		final HttpServletRequest req = (HttpServletRequest) request;
		String k1 = req.getParameter("k1");
		String k2 = req.getParameter("k2");
		String elementId = req.getParameter("elementid");

		if ((k1 != null) && (k2 != null) && (elementId != null)) {

			logger.debug("Received a request for {}", elementId);
			logger.debug("K1 {}", k1);
			logger.debug("K2 {}", k2);

			try {
				String accessToken = getAccessToken(request.getLocale());

				checkAuthorized(k1, k2, elementId, accessToken, request.getLocale());

				logger.debug("Credentials ARE VALID");

				filterChain.doFilter(request, response);// continue
			} catch (CredentialsNotValidException e) {
				logger.debug("Credentials ARE NOT VALID");

				Response toreturn2 = new Response();
				toreturn2.setResult(false);
				toreturn2.setMessage(e.getMessage());

				((HttpServletResponse) response).setStatus(401);
				((HttpServletResponse) response).getWriter().write(objectMapper.writeValueAsString(toreturn2));
			}

		} else {
			logger.debug("Credentials ARE NOT PRESENT");

			Response toreturn2 = new Response();
			toreturn2.setResult(false);
			toreturn2.setMessage(messages.getMessage("login.ko.credentialsnotpresent", null, request.getLocale()));

			((HttpServletResponse) response).setStatus(401);
			((HttpServletResponse) response).getWriter().write(objectMapper.writeValueAsString(toreturn2));
		}
	}

	private String getAccessToken(Locale lang) throws CredentialsNotValidException {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("grant_type", "password");
		params.add("client_id", clientId);
		params.add("username", username);
		params.add("password", password);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(token_endpoint).build();

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(params, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.POST, entity, String.class);
			logger.debug("Response from keyclock {}" + response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			JsonNode atNode = rootNode.path("access_token");

			return atNode.asText();
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getAccessToken", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.credentialsnotvalid", null, lang));
		}
	}

	private void checkAuthorized(String k1, String k2, String elementId, String accessToken, Locale lang) throws CredentialsNotValidException {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("type", "IOTID");
		params.add("accessToken", accessToken);
		params.add("elementId", elementId);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(ownership_endpoint)
				.queryParams(params)
				.build();

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();

		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from server ownership {}" + response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			Iterator<JsonNode> els = rootNode.elements();

			if (!els.hasNext())
				throw new CredentialsNotValidException(messages.getMessage("login.ko.ownershipnotfound", null, lang));

			JsonNode edNode = els.next().path("elementDetails");

			if ((edNode == null) || (edNode.isNull()))
				throw new CredentialsNotValidException(messages.getMessage("login.ko.keynotvalid", null, lang));

			JsonNode k1Node = edNode.path("k1");
			JsonNode k2Node = edNode.path("k2");

			if ((k1Node == null) || (k2Node == null) || (k1Node.isNull()) || (k2Node.isNull()) || (!k1.equals(k1Node.asText())) || (!k2.equals(k2Node.asText()))) {
				throw new CredentialsNotValidException(messages.getMessage("login.ko.keynotvalid", null, lang));
			}

		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in checkAuthorized", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.ownershipnotfound", null, lang));
		}
	}
}