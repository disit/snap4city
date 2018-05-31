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
package edu.unifi.disit.datamanager.controller.rest;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.ActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.exception.DelegationNotFoundException;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.IDataService;
import edu.unifi.disit.datamanager.service.IDelegationService;

@RestController
public class AppsController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDataService dataService;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IActivityService activityService;

	// -------------------GET Data for app ---------------------------------------------
	@RequestMapping(value = "/api/v1/apps/{appId}/data", method = RequestMethod.GET)
	public ResponseEntity<Object> getDataV1(@PathVariable("appId") String appId,
			@RequestParam(value = "delegated", required = false, defaultValue = "false") Boolean delegated,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false, defaultValue = "0") Integer first,
			@RequestParam(value = "last", required = false, defaultValue = "0") Integer last,
			@RequestParam(value = "anonymous", required = false, defaultValue = "false") Boolean anonymous,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested GET All Data for {}, delegated {}, first {} last {} anonymous {} lang {}", appId, delegated, first, last, anonymous, lang);

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
			List<Data> datas = dataService.getDataFromApp(appId, delegated, variableName, motivation, from, to, first, last, anonymous, lang);

			if ((datas == null) || (datas.isEmpty())) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				// save the activity for later auction
				activityService.saveActivityFromApp(appId, datas, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DATA);

				logger.info("Returning data {}", datas.size());
				for (int index = 0; index < datas.size(); index++)
					logger.debug("{}- {}", index, datas.get(index));

				return new ResponseEntity<Object>(datas, HttpStatus.OK);
			}
		} catch (DelegationNotFoundException | NoSuchMessageException | DataNotValidException d) {

			activityService.saveActivityViolationFromAppId(appId, sourceRequest, variableName, motivation, ActivityAccessType.READ, request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			logger.info("Delegation not found {}", d);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------POST Data for app ---------------------------------------------
	@RequestMapping(value = "/api/v1/apps/{appId}/data", method = RequestMethod.POST)
	public ResponseEntity<Object> postDataV1(@PathVariable("appId") String appId,
			@RequestBody Data data,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested POST new Data {} for {}, lang {}", data, appId, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			Data newData = dataService.postDataFromApp(appId, data, lang);

			activityService.saveActivityFromApp(appId, null, sourceRequest, data.getVariableName(), data.getMotivation(), ActivityAccessType.WRITE, ActivityDomainType.DATA);

			logger.info("Returning new Data {}", newData);
			return new ResponseEntity<Object>(newData, HttpStatus.OK);
		} catch (DataNotValidException d) {

			activityService.saveActivityViolationFromAppId(appId, sourceRequest, null, null, ActivityAccessType.WRITE, request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			logger.info("Data not valid {}", d);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET Delegation for app (delegator)---------------------------------------------
	@RequestMapping(value = "/api/v1/apps/{appId}/delegator", method = RequestMethod.GET)
	public ResponseEntity<Object> getDelegatorV1(@PathVariable("appId") String appId,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested GET All Delegation for appid {} lang {}", appId, lang);

		if (variableName != null)
			logger.info("VariableName specified {}", variableName);
		if (motivation != null)
			logger.info("Motivation specified {}", motivation);
		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		List<Delegation> delegations = delegationService.getDelegationsDelegatorFromApp(appId, variableName, motivation, lang);

		if ((delegations == null) || (delegations.isEmpty())) {
			logger.info("No data found");
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} else {
			logger.info("Returning delegations {}", delegations.size());
			for (int index = 0; index < delegations.size(); index++)
				logger.debug("{}- {}", index, delegations.get(index));

			return new ResponseEntity<Object>(delegations, HttpStatus.OK);
		}
	}

	// -------------------GET Delegation for app (delegated)---------------------------------------------
	@RequestMapping(value = "/api/v1/apps/{appId}/delegated", method = RequestMethod.GET)
	public ResponseEntity<Object> getDelegatedV1(@PathVariable("appId") String appId,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested GET All Delegation for appid {} lang {}", appId, lang);

		if (variableName != null)
			logger.info("VariableName specified {}", variableName);
		if (motivation != null)
			logger.info("Motivation specified {}", motivation);
		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		List<Delegation> delegations = delegationService.getDelegationsDelegatedFromApp(appId, variableName, motivation, lang);

		if ((delegations == null) || (delegations.isEmpty())) {
			logger.info("No data found");
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} else {
			logger.info("Returning delegations {}", delegations.size());
			for (int index = 0; index < delegations.size(); index++)
				logger.debug("{}- {}", index, delegations.get(index));

			// for post, add also delegated
			activityService.saveActivityDelegationFromAppId(appId, null, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DELEGATION);

			return new ResponseEntity<Object>(delegations, HttpStatus.OK);
		}
	}

	// -------------------DELETE Delegation for appid ---------------------------------------------
	@RequestMapping(value = "/api/v1/apps/{appId}/delegations", method = RequestMethod.DELETE)
	public ResponseEntity<Object> deleteDelegationV1(@PathVariable("appId") String appId,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested DELETE ALL Delegation {} for {}, lang {}", appId, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		delegationService.deleteAllDelegationFromApp(appId, lang);

		return new ResponseEntity<Object>(HttpStatus.OK);

	}

}