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
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.representations.AccessToken;
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

	private static final Logger loggerDM = LogManager.getLogger();

	@Autowired
	IActivityService activityService;

	@Value("${spring.openidconnect.userinfo_endpoint}")
	private String userinfo_endpoint;

	@Value("${spring.openidconnect.userinfo_endpoint_test:#{null}}")
	private String userinfo_endpoint_test;

	@Value("${spring.openidconnect.enable_server_auth:#{false}}")
	private boolean enableServerAuth;

	@Autowired
	private MessageSource messages;

	ObjectMapper objectMapper = new ObjectMapper();

	static Map<String, Object> certInfos;

	@SuppressWarnings("unchecked")
	@PostConstruct
	private void postConstruct() {
		ObjectMapper om = new ObjectMapper();
		try {
			certInfos = om.readValue(new URL(userinfo_endpoint + "realms/master/protocol/openid-connect/certs").openStream(), Map.class);
		} catch (Exception e) {
			logger.error("Cannot retrieve the certInfo");
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {

		final HttpServletRequest req = (HttpServletRequest) request;

		String accessToken = null;

		if ((req.getHeader("Authorization") != null) && (req.getHeader("Authorization").length() > 8))
			accessToken = req.getHeader("Authorization").substring(7);
		else
			accessToken = req.getParameter("accessToken");

		ActivityAccessType activityType = ActivityAccessType.READ;
		if (((HttpServletRequest) request).getMethod().equals("POST"))
			activityType = ActivityAccessType.WRITE;

		if ((accessToken != null) && (accessToken.length() != 0)) {

			try {

				validation(accessToken, request.getLocale());
				loggerDM.info("Access token IS VALID");

			} catch (AccessTokenNotValidException e) {

				loggerDM.info("Access token NOT VALID");

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

			loggerDM.warn("Access token NOT PRESENT");

			activityService.saveActivityViolationFromUsername(null, req.getParameter("sourceRequest"), req.getParameter("variableName"), req.getParameter("motivation"), activityType,
					((HttpServletRequest) request).getContextPath() + "?" + ((HttpServletRequest) request).getQueryString(),
					messages.getMessage("login.ko.accesstokennotpresent", null, request.getLocale()), null, ((HttpServletRequest) request).getRemoteAddr());

		}
		filterChain.doFilter(request, response);// continue

	}

	private void authUser(String username, List<String> roles) {
		List<GrantedAuthority> authorities = new ArrayList<>();
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

			if (!enableServerAuth)
				checkValidationLocally(accesstoken, lang);
			else
				checkValidationOnKeycloakServer(UriComponentsBuilder.fromHttpUrl(userinfo_endpoint + "realms/master/protocol/openid-connect/userinfo").build(), restTemplate, entity, lang);// check validation on www.snap4city.org/auth
																																															// keycloak
			// server
		} catch (Exception e) {
			loggerDM.warn("AccessToken WAS NOT VALIDATED HttpClientErrorException ", e);
			try {
				if (userinfo_endpoint_test != null)
					checkValidationOnKeycloakServer(UriComponentsBuilder.fromHttpUrl(userinfo_endpoint_test + "realms/master/protocol/openid-connect/userinfo").build(), restTemplate, entity, lang);// check validation on
																																																		// www.snap4city.org/auth
																																																		// keycloak server
				else {
					loggerDM.warn("AccessToken WAS NOT VALIDATED even on keycloak HttpClientErrorException ", e);
					throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
				}

			} catch (HttpClientErrorException e2) {
				loggerDM.warn("AccessToken WAS NOT VALIDATED even on keycloak TEST HttpClientErrorException", e2);
				throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
			} catch (JsonProcessingException e2) {
				loggerDM.warn("AccessToken WAS NOT VALIDATED even on keycloak TEST  JsonProcessingException", e2);
				throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
			} catch (IOException e2) {
				loggerDM.warn("AccessToken WAS NOT VALIDATED even on keycloak TEST  IOException", e2);
				throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
			}
			// } catch (JsonProcessingException e) {
			// loggerDM.warn("AccessToken WAS NOT VALIDATED JsonProcessingException ", e);
			// throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
			// } catch (IOException e) {
			// loggerDM.warn("AccessToken WAS NOT VALIDATED IOException ", e);
			// throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}
	}

	@SuppressWarnings("unchecked")
	private PublicKey retrievePublicKeyFromCertsEndpoint(JWSHeader jwsHeader) {
		try {

			List<Map<String, Object>> keys = (List<Map<String, Object>>) certInfos.get("keys");

			Map<String, Object> keyInfo = null;
			for (Map<String, Object> key : keys) {
				String kid = (String) key.get("kid");

				if (jwsHeader.getKeyId().equals(kid)) {
					keyInfo = key;
					break;
				}
			}

			if (keyInfo == null) {
				return null;
			}

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			String modulusBase64 = (String) keyInfo.get("n");
			String exponentBase64 = (String) keyInfo.get("e");

			// see org.keycloak.jose.jwk.JWKBuilder#rs256
			java.util.Base64.Decoder urlDecoder = java.util.Base64.getUrlDecoder();
			BigInteger modulus = new BigInteger(1, urlDecoder.decode(modulusBase64));
			BigInteger publicExponent = new BigInteger(1, urlDecoder.decode(exponentBase64));

			return keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void checkValidationLocally(String requestAccessToken, Locale lang) throws NoSuchMessageException, AccessTokenNotValidException {

		if (requestAccessToken == null) {
			logger.warn("Empty accesstoken");
			throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}

		try {
			TokenVerifier<AccessToken> verifier = TokenVerifier.create(requestAccessToken, AccessToken.class);
			AccessToken token = verifier
					.publicKey(retrievePublicKeyFromCertsEndpoint(verifier.getHeader()))
					.verify()
					.getToken();

			if (!token.isExpired()) {
				Map<String, Object> otherclaims = token.getOtherClaims();

				String username = token.getPreferredUsername();// try get from prefered username
				if (username == null) {
					username = (String) otherclaims.get("username");// fallback on username
					if (username == null) {
						logger.warn("Empty username in accesstoken");
						throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
					}
				}

				ArrayList<String> roles = (ArrayList<String>) otherclaims.get("roles");// try get from roles
				if (roles == null) {
					roles = (ArrayList<String>) otherclaims.get("role");// fallback on role
					if (roles == null) {
						logger.warn("Empty roles in accesstoken");
						throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
					}
				}

				loggerDM.info("AccessToken username {} + Roles {}", username, roles.toArray());
				authUser(username, roles);
			} else {
				logger.warn("The passed accessToken is Expired");
				throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
			}
		} catch (VerificationException e) {
			logger.warn("Verification failed");
			throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}

	}

	private void checkValidationOnKeycloakServer(UriComponents uriComponents, RestTemplate restTemplate, HttpEntity<String> entity, Locale lang)
			throws IOException, AccessTokenNotValidException {

		loggerDM.info("AccessToken check on {}", uriComponents.toUri());

		ResponseEntity<String> re = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);

		String response = re.getBody();

		loggerDM.debug("got response:{}", response);

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
			loggerDM.error("username not found");
			throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}

		// retrieve roles
		List<String> roles = new ArrayList<>();
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
			loggerDM.error("roles not found");
			throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}

		while (iroles.hasNext())
			roles.add(iroles.next().asText());

		loggerDM.info("AccessToken username {} + Roles {}", username, roles.toArray());

		authUser(username, roles);
	}
}
