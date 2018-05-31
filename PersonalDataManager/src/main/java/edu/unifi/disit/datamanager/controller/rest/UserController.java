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
import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.exception.DelegationNotFoundException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.IDataService;
import edu.unifi.disit.datamanager.service.IDelegationService;

@RestController
public class UserController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IDataService dataService;

	@Autowired
	IActivityService activityService;

	// -------------------GET Data for app ---------------------------------------------
	@RequestMapping(value = "/api/v1/username/{username}/data", method = RequestMethod.GET)
	public ResponseEntity<Object> getDataV1(@PathVariable("username") String username,
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

		logger.info("Requested GET All Data for {}, delegated {},  anonymous {} lang {}", username, delegated, first, last, anonymous, lang);

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
			List<Data> datas = dataService.getDataFromUser(username, delegated, variableName, motivation, from, to, first, last, anonymous, lang);

			if ((datas == null) || (datas.isEmpty())) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				// save the activity for later auction
				activityService.saveActivityFromUsername(username, datas, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DATA);

				logger.info("Returning data {}", datas.size());
				for (int index = 0; index < datas.size(); index++)
					logger.debug("{}- {}", index, datas.get(index));

				return new ResponseEntity<Object>(datas, HttpStatus.OK);
			}
		} catch (DelegationNotFoundException | NoSuchMessageException | DataNotValidException d) {

			activityService.saveActivityViolationFromUsername(username, sourceRequest, variableName, motivation, ActivityAccessType.READ, request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			logger.info("Delegation not found {}", d);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------POST Data for username ---------------------------------------------
	@RequestMapping(value = "/api/v1/username/{username}/data", method = RequestMethod.POST)
	public ResponseEntity<Object> postDataV1(@PathVariable("username") String username,
			@RequestBody Data data,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested POST new Data {} for {}, lang {}", data, username, lang);
		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			Data newData = dataService.postDataFromUser(username, data, lang);

			activityService.saveActivityFromUsername(username, null, sourceRequest, data.getVariableName(), data.getMotivation(), ActivityAccessType.WRITE, ActivityDomainType.DATA);

			logger.info("Returning new Data {}", newData);
			return new ResponseEntity<Object>(newData, HttpStatus.OK);
		} catch (DataNotValidException d) {

			activityService.saveActivityViolationFromUsername(username, sourceRequest, null, null, ActivityAccessType.READ, request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			logger.info("Data not valid {}", d);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET Delegation for username (delegator)---------------------------------------------
	@RequestMapping(value = "/api/v1/username/{username}/delegator", method = RequestMethod.GET)
	public ResponseEntity<Object> getDelegatorV1(@PathVariable("username") String username,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested GET All Delegation for {} lang {}", username, lang);

		if (variableName != null)
			logger.info("VariableName specified {}", variableName);
		if (motivation != null)
			logger.info("Motivation specified {}", motivation);
		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		List<Delegation> delegations = delegationService.getDelegationsDelegatorForUsername(username, variableName, motivation, lang);

		if ((delegations == null) || (delegations.isEmpty())) {
			logger.info("No data found");
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} else {
			logger.info("Returning delegations {}", delegations.size());
			for (int index = 0; index < delegations.size(); index++)
				logger.debug("{}- {}", index, delegations.get(index));

			activityService.saveActivityDelegationFromUsername(username, null, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DELEGATION);

			return new ResponseEntity<Object>(delegations, HttpStatus.OK);
		}
	}

	// -------------------GET Delegation fom username (delegated)---------------------------------------------
	@RequestMapping(value = "/api/v1/username/{username}/delegated", method = RequestMethod.GET)
	public ResponseEntity<Object> getDelegatedV1(@PathVariable("username") String username,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested GET All Delegation for {} lang {}", username, lang);

		if (variableName != null)
			logger.info("VariableName specified {}", variableName);
		if (motivation != null)
			logger.info("Motivation specified {}", motivation);
		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		List<Delegation> delegations = delegationService.getDelegationsDelegatedForUsername(username, variableName, motivation, lang);

		if ((delegations == null) || (delegations.isEmpty())) {
			logger.info("No data found");
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} else {
			logger.info("Returning delegations {}", delegations.size());
			for (int index = 0; index < delegations.size(); index++)
				logger.debug("{}- {}", index, delegations.get(index));

			activityService.saveActivityDelegationFromUsername(username, null, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DELEGATION);

			return new ResponseEntity<Object>(delegations, HttpStatus.OK);
		}
	}

	// -------------------GET Check Delegation fom username ---------------------------------------------
	@RequestMapping(value = "/api/v1/username/{username}/delegation/check", method = RequestMethod.GET)
	public ResponseEntity<Response> checkDelegationV1(@PathVariable("username") String username,
			@RequestParam(value = "elementID", required = true) String elementID,
			@RequestParam(value = "variableName", required = true) String variableName,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested CHECK Delegation for {}, elementID {} variableName {} lang {}", username, elementID, variableName, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		Response response = delegationService.checkDelegations(username, variableName, elementID, lang);
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	// -------------------POST Delegation for username ---------------------------------------------
	@RequestMapping(value = "/api/v1/username/{username}/delegation", method = RequestMethod.POST)
	public ResponseEntity<Object> postDelegationV1(@PathVariable("username") String username,
			@RequestBody Delegation delegation,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested POST new Delgation {} for {}, lang {}", delegation, username, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			Delegation newDelegation = delegationService.postDelegationFromUser(username, delegation, lang);

			activityService.saveActivityDelegationFromUsername(username, delegation.getUsernameDelegator(), sourceRequest, delegation.getVariableName(), delegation.getMotivation(), ActivityAccessType.WRITE, ActivityDomainType.DELEGATION);

			logger.info("Returning new Delegation {}", newDelegation);
			return new ResponseEntity<Object>(newDelegation, HttpStatus.OK);
		} catch (DelegationNotValidException de) {
			logger.info("Delegation not valid {}", de);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) de.getMessage());
		}
	}

	// -------------------PUT Delegation for username ---------------------------------------------
	@RequestMapping(value = "/api/v1/username/{username}/delegation/{delegationid}", method = RequestMethod.PUT)
	public ResponseEntity<Object> putDelegationV1(@PathVariable("username") String username,
			@PathVariable("delegationid") Long delegationId,
			@RequestBody Delegation delegation,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested PUT new Delegation {} for {}, lang {}, delegationId {}", delegation, username, lang, delegationId);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			Delegation newDelegation = delegationService.putDelegationFromUser(username, delegation, delegationId, lang);

			activityService.saveActivityDelegationFromUsername(username, delegation.getUsernameDelegator(), sourceRequest, delegation.getVariableName(), delegation.getMotivation(), ActivityAccessType.WRITE, ActivityDomainType.DELEGATION);

			logger.info("Returning new Delegation {}", newDelegation);
			return new ResponseEntity<Object>(newDelegation, HttpStatus.OK);
		} catch (DelegationNotValidException de) {
			logger.info("Delegation not valid {}", de);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) de.getMessage());
		}
	}

	// -------------------DELETE Delegation for username ---------------------------------------------
	@RequestMapping(value = "/api/v1/username/{username}/delegation/{delegationid}", method = RequestMethod.DELETE)
	public ResponseEntity<Object> deleteDelegationV1(@PathVariable("username") String username,
			@PathVariable("delegationid") Long delegationId,
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested DELETE Delegation {} for {}, lang {}", delegationId, username, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			delegationService.deleteDelegationFromUser(username, delegationId, lang);

			return new ResponseEntity<Object>(HttpStatus.OK);
		} catch (DelegationNotValidException de) {
			logger.info("Delegation not valid {}", de);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) de.getMessage());
		}
	}
}