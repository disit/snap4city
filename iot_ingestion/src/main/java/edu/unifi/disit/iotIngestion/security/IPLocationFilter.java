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
package edu.unifi.disit.iotIngestion.security;

import java.io.IOException;
import java.util.List;

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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Component
public class IPLocationFilter extends GenericFilterBean {

	private static final Logger logger = LogManager.getLogger();

	@Value("#{'${whitelistIP}'.split(',')}")
	private List<String> whitelistIP;

	@Autowired
	private MessageSource messages;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

		HttpServletRequest myrequest = (HttpServletRequest) request;

		String ip = myrequest.getHeader("X-Forwarded-For");
		if (ip == null)
			ip = myrequest.getRemoteAddr();

		if ((ip == null) || (!whitelistIP.contains(ip))) {

			logger.error("Wrong IP");

			((HttpServletResponse) response).setStatus(401);
			((HttpServletResponse) response).getWriter().write(messages.getMessage("access.ko", null, request.getLocale()));

		} else
			filterChain.doFilter(request, response);
	}

}