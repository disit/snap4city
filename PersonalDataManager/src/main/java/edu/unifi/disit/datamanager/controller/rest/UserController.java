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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.ActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.exception.DelegationNotFoundException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;
import edu.unifi.disit.datamanager.exception.LDAPException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
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
	IAccessService accessService;

	@Autowired
	IActivityService activityService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET Data for app ---------------------------------------------
	@GetMapping(value = "/api/v1/username/{username}/data")
	public ResponseEntity<Object> getDataV1(@PathVariable("username") String username,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "from", required = false) String from,
			@RequestParam(value = "to", required = false) String to,
			@RequestParam(value = "first", required = false, defaultValue = "0") Integer first,
			@RequestParam(value = "last", required = false, defaultValue = "0") Integer last,
			@RequestParam(value = "delegated", required = false, defaultValue = "false") Boolean delegated,
			@RequestParam(value = "anonymous", required = false, defaultValue = "false") Boolean anonymous,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested getDataV1 for {}, delegated {}, first {}, last {}, anonymous {}, lang {}", username, delegated, first, last, anonymous, lang);

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

			List<Data> datas = dataService.getDataFromUser(username, delegated, variableName, motivation, convertDate(from), convertDate(to), first, last, anonymous, lang);

			activityService.saveActivityFromUsername(username, datas, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DATA);

			if ((datas == null) || (datas.isEmpty())) {
				logger.info("No data found");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning data {}", datas.size());
				for (int index = 0; index < datas.size(); index++)
					logger.trace("{}- {}", index, datas.get(index));
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (DelegationNotFoundException | NoSuchMessageException | DataNotValidException d) {
			logger.error("Delegation not found", d);

			activityService.saveActivityViolationFromUsername(username, sourceRequest, variableName, motivation, ActivityAccessType.READ,
					request.getContextPath() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.READ,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (ParseException e) {
			logger.error("Parsing error ", e);

			activityService.saveActivityViolationFromUsername(username, sourceRequest, variableName, motivation, ActivityAccessType.READ,
					request.getContextPath() + "?" + request.getQueryString(), e.getMessage(), e, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) e.getMessage());
		}
	}

	// -------------------POST Data for username ---------------------------------------------
	@PostMapping(value = "/api/v1/username/{username}/data")
	public ResponseEntity<Object> postDataV1(@PathVariable("username") String username,
			@RequestBody Data data,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested postDataV1 {} for {}, lang {}", data, username, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			Data newData = dataService.postDataFromUser(username, data, lang);

			activityService.saveActivityFromUsername(username, null, sourceRequest, data.getVariableName(), data.getMotivation(), ActivityAccessType.WRITE, ActivityDomainType.DATA);

			logger.info("Returning new Data {}", newData);

			return new ResponseEntity<>(newData, HttpStatus.OK);
		} catch (DataNotValidException d) {
			logger.error("Data not valid", d);

			activityService.saveActivityViolationFromUsername(username, sourceRequest, null, null, ActivityAccessType.READ, request.getContextPath() + "?" + request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.WRITE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Delegation for username (delegator)---------------------------------------------
	// V1 does not return Delegation with UsernameDelegated==null
	@Deprecated
	@SuppressWarnings("unchecked")
	@GetMapping(value = "/api/v1/username/{username}/delegator")
	public ResponseEntity<Object> getDelegatorV1(@PathVariable("username") String username,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
			@RequestParam(value = "elementType", required = false) String elementType,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested getDelegatorV1 for {} lang {}", username, lang);

		ResponseEntity<Object> toreturn = getDelegatorV2(username, sourceRequest, variableName, motivation, deleted, elementType, lang, request);

		if (toreturn.getStatusCode().compareTo(HttpStatus.OK) == 0) {
			toreturn = new ResponseEntity<>(delegationService.stripUsernameDelegatedNull((List<Delegation>) toreturn.getBody()), HttpStatus.OK);
		}

		return toreturn;
	}

	// V2 return also Delegation with UsernameDelegated==nul
	@GetMapping(value = "/api/v2/username/{username}/delegator")
	public ResponseEntity<Object> getDelegatorV2(@PathVariable("username") String username,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
			@RequestParam(value = "elementType", required = false) String elementType,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested getDelegatorV2 for {} lang {}", username, lang);

		if (variableName != null)
			logger.info("VariableName specified {}", variableName);
		if (motivation != null)
			logger.info("Motivation specified {}", motivation);
		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);
		if (elementType != null) {
			logger.info("elementType specified {}", elementType);
		}

		try

		{
			List<Delegation> delegations = delegationService.getDelegationsDelegatorForUsername(username, variableName, motivation, deleted, elementType, lang);

			activityService.saveActivityDelegationFromUsername(username, null, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DELEGATION);

			if ((delegations == null) || (delegations.isEmpty())) {
				logger.info("No data found");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning delegations {}", delegations.size());
				for (int index = 0; index < delegations.size(); index++)
					logger.trace("{}- {}", index, delegations.get(index));
				return new ResponseEntity<>(delegations, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.READ,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Delegation fom username (delegated)---------------------------------------------
	// V1 does not return Delegation with UsernameDelegated==null
	@Deprecated
	@SuppressWarnings("unchecked")
	@GetMapping(value = "/api/v1/username/{username}/delegated")
	public ResponseEntity<Object> getDelegatedV1(@PathVariable("username") String username,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
			@RequestParam(value = "groupname", required = false) String groupname,
			@RequestParam(value = "elementType", required = false) String elementType,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException, CloneNotSupportedException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested getDelegatedV1 for {} lang {}", username, lang);

		ResponseEntity<Object> toreturn = getDelegatedV2(username, sourceRequest, variableName, motivation, deleted, groupname, elementType, lang, request);

		if (toreturn.getStatusCode().compareTo(HttpStatus.OK) == 0) {
			toreturn = new ResponseEntity<>(delegationService.stripUsernameDelegatedNull((List<Delegation>) toreturn.getBody()), HttpStatus.OK);
		}

		return toreturn;
	}

	// V2 return also Delegation with UsernameDelegated==null
	@GetMapping(value = "/api/v2/username/{username}/delegated")
	public ResponseEntity<Object> getDelegatedV2(@PathVariable("username") String username,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "motivation", required = false) String motivation,
			@RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
			@RequestParam(value = "groupname", required = false) String groupname,
			@RequestParam(value = "elementType", required = false) String elementType,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException, CloneNotSupportedException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested getDelegatedV2 for {} deleted {} lang {}", username, deleted, lang);

		if (variableName != null)
			logger.info("VariableName specified {}", variableName);
		if (motivation != null)
			logger.info("Motivation specified {}", motivation);
		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);
		if (groupname != null) {
			logger.info("groupname specified {}", groupname);
			groupname = URLDecoder.decode(groupname, StandardCharsets.UTF_8.toString());
			logger.info("groupname decoded to {}", groupname);
		}
		if (elementType != null) {
			logger.info("elementType specified {}", elementType);
		}

		try {
			List<Delegation> delegations = delegationService.getDelegationsDelegatedForUsername(username, variableName, motivation, deleted, groupname, elementType, lang);

			activityService.saveActivityDelegationFromUsername(username, null, sourceRequest, variableName, motivation, ActivityAccessType.READ, ActivityDomainType.DELEGATION);

			if ((delegations == null) || (delegations.isEmpty())) {
				logger.info("No data found");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning delegations {}", delegations.size());
				for (int index = 0; index < delegations.size(); index++)
					logger.trace("{}- {}", index, delegations.get(index));
				return new ResponseEntity<>(delegations, HttpStatus.OK);
			}
		} catch (LDAPException le) {
			logger.error("LDAP not valid ", le);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) le.getMessage());
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.READ,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Check Delegation fom username ---------------------------------------------
	// V1 does not require elementType (set to NULL)
	@Deprecated
	@GetMapping(value = "/api/v1/username/{username}/delegation/check")
	public ResponseEntity<Object> checkDelegationV1(@PathVariable("username") String username,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "elementID", required = true) String elementID,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested checkDelegationV1 for {}, elementID {} variableName {} lang {}", username, elementID, variableName, lang);

		return checkDelegationV3(username, sourceRequest, elementID, null, variableName, lang, request);
	}

	// V3 requires elementType
	@GetMapping(value = "/api/v3/username/{username}/delegation/check")
	public ResponseEntity<Object> checkDelegationV3(@PathVariable("username") String username,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam("elementID") String elementID,
			@RequestParam("elementType") String elementType,
			@RequestParam(value = "variableName", required = false) String variableName,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested checkDelegationV3 for {}, elementID {}, elementType {}, variableName {} lang {}", username, elementID, elementType, variableName, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		if (username.contains("%20")) {
			username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());
			logger.info("username decoded to {}", username);
		}

		try {

			Response response = accessService.checkDelegationsFromUsername(elementID, elementType, variableName, username, lang);

			activityService.saveActivityDelegationFromUsername(username, null, sourceRequest, null, null, ActivityAccessType.READ, ActivityDomainType.DELEGATION);

			logger.info("Response {}", response);

			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.READ,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST Delegation for username ---------------------------------------------
	@PostMapping(value = "/api/v1/username/{username}/delegation")
	public ResponseEntity<Object> postDelegationV1(@PathVariable("username") String username,
			@RequestBody Delegation delegation,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested postDelegationV1 {} for {}, lang {}", delegation, username, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			Delegation newDelegation = delegationService.postDelegationFromUser(username, delegation, lang);

			activityService.saveActivityDelegationFromUsername(username, delegation.getUsernameDelegator(), sourceRequest, delegation.getVariableName(), delegation.getMotivation(), ActivityAccessType.WRITE, ActivityDomainType.DELEGATION);

			logger.info("Returning new Delegation {}", newDelegation);

			return new ResponseEntity<>(newDelegation, HttpStatus.OK);
		} catch (DelegationNotValidException de) {
			logger.error("Delegation not valid ", de);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.WRITE,
					request.getRequestURI() + "?" + request.getQueryString(), de.getMessage(), de, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) de.getMessage());
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.WRITE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PUT Delegation for username ---------------------------------------------
	@PutMapping(value = "/api/v1/username/{username}/delegation/{delegationid}")
	public ResponseEntity<Object> putDelegationV1(@PathVariable("username") String username,
			@PathVariable("delegationid") Long delegationId,
			@RequestBody Delegation delegation,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested putDelegationV1 {} for {}, lang {}, delegationId {}", delegation, username, lang, delegationId);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			Delegation newDelegation = delegationService.putDelegationFromUser(username, delegation, delegationId, lang);

			activityService.saveActivityDelegationFromUsername(username, delegation.getUsernameDelegator(), sourceRequest, delegation.getVariableName(), delegation.getMotivation(), ActivityAccessType.WRITE, ActivityDomainType.DELEGATION);

			logger.info("Returning new Delegation {}", newDelegation);

			return new ResponseEntity<>(newDelegation, HttpStatus.OK);
		} catch (DelegationNotValidException de) {
			logger.error("Delegation not valid ", de);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.WRITE,
					request.getRequestURI() + "?" + request.getQueryString(), de.getMessage(), de, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) de.getMessage());
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.WRITE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE Delegation for username ---------------------------------------------
	@DeleteMapping(value = "/api/v1/username/{username}/delegation/{delegationid}")
	public ResponseEntity<Object> deleteDelegationV1(@PathVariable("username") String username,
			@PathVariable("delegationid") Long delegationId,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested deleteDelegationV1 {} for {}, lang {}", delegationId, username, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			delegationService.deleteDelegationFromUser(username, delegationId, lang);

			activityService.saveActivityDelegationFromUsername(username, null, sourceRequest, null, null, ActivityAccessType.DELETE, ActivityDomainType.DELEGATION);

			logger.info("Deleted {}", delegationId);

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (DelegationNotValidException de) {
			logger.error("Delegation not valid ", de);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.DELETE,
					request.getRequestURI() + "?" + request.getQueryString(), de.getMessage(), de, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) de.getMessage());
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.DELETE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
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
				throw e1;
			}
		}
		return text;
	}

	// -------------------GET Organization of an user ---------------------------------------------
	@GetMapping(value = "/api/v1/username/organization")
	public ResponseEntity<Object> getOrganizationV1(
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getOrganizationV1");

		try {
			return new ResponseEntity<>(credentialService.getOrganization(lang), HttpStatus.OK);
		} catch (NoSuchMessageException d) {
			logger.error("Delegation not found", d);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE Data for username ---------------------------------------------
	@DeleteMapping(value = "/api/v1/username/{username}/data/{dataid}")
	public ResponseEntity<Object> deleteDataV1(@PathVariable("username") String username,
			@PathVariable("dataid") Long dataId,
			@RequestParam("sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		username = URLDecoder.decode(username, StandardCharsets.UTF_8.toString());

		logger.info("Requested deleteDataV1 {} for {}, lang {}", dataId, username, lang);

		if (sourceRequest != null)
			logger.info("sourceRequest specified {}", sourceRequest);

		try {
			dataService.deleteDataFromUser(username, dataId, lang);

			activityService.saveActivityDelegationFromUsername(username, null, sourceRequest, null, null, ActivityAccessType.DELETE, ActivityDomainType.DATA);

			logger.info("Deleted {}", dataId);

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (DataNotValidException d) {
			logger.error("Data not valid", d);

			activityService.saveActivityViolationFromUsername(username, sourceRequest, null, null, ActivityAccessType.DELETE, request.getContextPath() + "?" + request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			activityService.saveActivityViolationFromUsername(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(), null, null, null, ActivityAccessType.DELETE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

}