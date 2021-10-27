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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.ActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.Activity;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IActivityService;

@RestController
public class ActivityController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IActivityService activityService;

	// -------------------GET ALL Activity for appId ---------------------------------------------
	// V1 does not require elementType (set to NULL)
	@Deprecated
	@GetMapping(value = "/api/v1/apps/{appId}/activity")
	public ResponseEntity<Object> getActivityV1(@PathVariable("appId") String appId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "delegated", required = false, defaultValue = "false") Boolean delegated,
			HttpServletRequest request) {

		logger.info("Requested getActivityV1 for {}, delegated {} lang {}", appId, delegated, lang);

		return getActivityV3(appId, null, lang, delegated, request);
	}

	// V3 requires elementType
	@GetMapping(value = "/api/v3/apps/{appId}/activity")
	public ResponseEntity<Object> getActivityV3(@PathVariable("appId") String appId,
			@RequestParam(value = "elementType") String elementType,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "delegated", required = false, defaultValue = "false") Boolean delegated,
			HttpServletRequest request) {

		logger.info("Requested getActivityV3 for {}, elementType {}, delegated {} lang {}", appId, elementType, delegated, lang);

		try {
			List<Activity> actvities = activityService.getActivities(appId, elementType, delegated, lang);

			activityService.saveActivityDelegationFromAppId(appId, null, null, null, null, ActivityAccessType.READ, ActivityDomainType.ACTIVITY);

			if ((actvities == null) || (actvities.isEmpty())) {
				logger.info("No actvities found");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning actvities {}", actvities.size());
				for (int index = 0; index < actvities.size(); index++)
					logger.trace("{}- {}", index, actvities.get(index));
				return new ResponseEntity<>(actvities, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolation(appId, SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.READ,
					RequestHelper.getUrl(request), d.getMessage(), d, RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
}
