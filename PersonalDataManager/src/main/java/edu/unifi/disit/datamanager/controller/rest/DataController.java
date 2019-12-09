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
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.IDataService;

@RestController
public class DataController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDataService dataService;

	@Autowired
	IActivityService activityService;

	// -------------------GET alive ---------------------------------------------
	@GetMapping(value = "/api/test")
	public ResponseEntity<String> dataControllerTest() {
		return new ResponseEntity<>("alive", HttpStatus.OK);
	}

	// -------------------GET ALL Data ---------------------------------------------
	@GetMapping(value = "/api/v1/data")
	public ResponseEntity<Object> getDataV1(
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "last", required = false, defaultValue = "false") Boolean last,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getDataV1 lang {} last {}", lang, last);

		if (sourceRequest != null)
			logger.info("SourceRequest specified {}", sourceRequest);

		try {
			List<Data> datas = dataService.getAllData(last, lang);

			activityService.saveActivityFromUsername(null, datas, sourceRequest, null, null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if ((datas == null) || (datas.isEmpty())) {
				logger.info("No data found");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning data {}", datas.size());
				for (int index = 0; index < datas.size(); index++)
					logger.trace("{}- {}", index, datas.get(index));
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), sourceRequest, null, null, ActivityAccessType.READ,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
}
