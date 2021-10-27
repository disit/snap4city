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
package edu.unifi.disit.datamanager.controller.rest;

import edu.unifi.disit.datamanager.RequestHelper;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.ActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.IDelegationService;

@RestController
public class DelegationController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IActivityService activityService;

	// -------------------GET ALL Delegations ---------------------------------------------
	@GetMapping(value = "/api/v1/delegation")
	public ResponseEntity<Object> getDelegationV1(
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "elementType", required = false) String elementType,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getDelegationV1 lang {} elementType {} varName {} motivation {}", lang, elementType, variableName, motivation);

		if (sourceRequest != null)
			logger.info("SourceRequest specified {}", sourceRequest);

		try {
			List<Delegation> delegations = delegationService.getAllDelegations(variableName, motivation, elementType, lang);

			activityService.saveActivityDelegationFromUsername(null, null, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DELEGATION);

			if ((delegations == null) || (delegations.isEmpty())) {
				logger.info("No delegation found");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning deelgation {}", delegations.size());
				for (int index = 0; index < delegations.size(); index++)
					logger.trace("{}- {}", index, delegations.get(index));
				return new ResponseEntity<>(delegations, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), sourceRequest, null, null, ActivityAccessType.READ,
					RequestHelper.getUrl(request), d.getMessage(), d, RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
}
