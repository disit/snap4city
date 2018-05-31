/* Data Manager (DM).
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
package edu.unifi.disit.datamanager.security;

import java.io.IOException;
import java.util.Collections;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

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

		if (accessToken != null) {

			try {
				validation(accessToken, request.getLocale());
				logger.debug("Access token IS VALID");

				filterChain.doFilter(request, response);// continue

			} catch (AccessTokenNotValidException e) {

				logger.debug("Access token NOT VALID");

				activityService.saveActivityViolationFromUsername(null, req.getParameter("sourceRequest"), req.getParameter("variableName"), req.getParameter("motivation"), activityType, ((HttpServletRequest) request).getQueryString(),
						e.getMessage(), e, ((HttpServletRequest) request).getRemoteAddr());

				Response toreturn2 = new Response();
				toreturn2.setResult(false);
				toreturn2.setMessage(e.getMessage());

				((HttpServletResponse) response).setStatus(401);
				((HttpServletResponse) response).getWriter().write(objectMapper.writeValueAsString(toreturn2));
			}

		} else {

			logger.debug("Access token NOT PRESENT");

			activityService.saveActivityViolationFromUsername(null, req.getParameter("sourceRequest"), req.getParameter("variableName"), req.getParameter("motivation"), activityType, ((HttpServletRequest) request).getQueryString(),
					messages.getMessage("login.ko.accesstokennotpresent", null, request.getLocale()), null, ((HttpServletRequest) request).getRemoteAddr());

			Response toreturn2 = new Response();
			toreturn2.setResult(false);
			toreturn2.setMessage(messages.getMessage("login.ko.accesstokennotpresent", null, request.getLocale()));

			((HttpServletResponse) response).setStatus(401);
			((HttpServletResponse) response).getWriter().write(objectMapper.writeValueAsString(toreturn2));
		}
	}

	private void validation(String accesstoken, Locale lang) throws AccessTokenNotValidException {

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(userinfo_endpoint).build();

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + accesstoken);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			logger.debug(restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class));
		} catch (HttpClientErrorException e) {
			logger.debug("AccessToken WAS NOT VALIDATED");
			// throw new AccessTokenNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}
	}
}