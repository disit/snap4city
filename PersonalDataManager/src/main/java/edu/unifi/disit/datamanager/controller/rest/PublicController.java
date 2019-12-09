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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.ActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IDataService;

@RestController
public class PublicController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDataService dataService;

	@Autowired
	IAccessService accessService;

	@Autowired
	IActivityService activityService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET Check Public Delegation without accesstoken ---------------------------------------------
	@GetMapping(value = "/api/v1/public/access/check")
	public ResponseEntity<Object> checkAccessPublicV1(
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "elementID", required = true) String elementID,
			@RequestParam(value = "elementType", required = true) String elementType,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested checkPublicV1 for elementID {} elementType {} variableName {} lang {}", elementID, elementType, variableName, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		Response response = accessService.checkPublic(elementID, elementType, variableName, lang);

		activityService.saveActivityDelegationFromUsername("PUBLIC", null, sourceRequest, null, null, ActivityAccessType.READ, ActivityDomainType.DELEGATION);

		logger.info("Response {}", response);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// -------------------GET Data Public without accesstoken ---------------------------------------------
	@GetMapping(value = "/api/v1/public/data")
	public ResponseEntity<Object> getDataPublicV1(
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "from", required = false) String from,
			@RequestParam(value = "to", required = false) String to,
			@RequestParam(value = "first", required = false, defaultValue = "0") Integer first,
			@RequestParam(value = "last", required = false, defaultValue = "0") Integer last,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getDataPublicV1 first {}, last {}, anonymous, lang {}", first, last, lang);

		if (variableName != null)
			logger.info("VariableName specified {}", variableName);
		if (motivation != null)
			logger.info("Motivation specified {}", motivation);
		if (from != null)
			logger.info("From specified {}", from);
		if (to != null)
			logger.info("To specified {}", to);
		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {

			List<Data> datas = dataService.getPublicData(variableName, motivation, convertDate(from), convertDate(to), first, last, lang);

			activityService.saveActivityFromUsername("PUBLIC", datas, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DATA);

			if ((datas == null) || (datas.isEmpty())) {
				logger.info("No data found");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning data {}", datas.size());
				for (int index = 0; index < datas.size(); index++)
					logger.trace("{}- {}", index, datas.get(index));
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}

		} catch (DataNotValidException d) {
			logger.error("Delegation not found {}", d);

			activityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, variableName, motivation, ActivityAccessType.READ,
					request.getContextPath() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());

		} catch (ParseException e) {
			logger.error("Parsing error {}", e);

			activityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, variableName, motivation, ActivityAccessType.READ,
					request.getContextPath() + "?" + request.getQueryString(), e.getMessage(), e, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) e.getMessage());
		}

	}

	private Date convertDate(String text_string) throws ParseException {
		if ((text_string == null) || (text_string.isEmpty()))
			return null;

		Date text = null;

		try {
			text = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(text_string);
		} catch (ParseException e) {
			try {
				text = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(text_string);
			} catch (ParseException e1) {
				logger.error("Parsing error {}", e1);
				throw e1;
			}
		}
		return text;
	}
}