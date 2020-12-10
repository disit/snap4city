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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IDelegationService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;
import edu.unifi.disit.datamanager.service.IKPIDataService;

@RestController
public class KPIDelegationController {

	private static final Logger logger = LogManager.getLogger();

	private static final String NO_DATA_FOUND = "No data found";
	private static final String RIGHTS_EXCEPTION = "Rights exception";
	private static final String WRONG_KPI_DATA = "Wrong KPI Data";
	private static final String WRONG_ARGUMENTS = "Wrong Arguments";

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IKPIDataService kpiDataService;

	@Autowired
	IAccessService accessService;

	@Autowired
	IKPIActivityService kpiActivityService;

	@Autowired
	ICredentialsService credentialService;

	@Autowired
	AppsController appsController;

	@Autowired
	UserController userController;

	// -------------------GET KPI Delegation From ID
	// ------------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/delegations/{id}")
	public ResponseEntity<Object> getKPIDelegationV1ById(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIDelegationV1ById id {} lang {}sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			Delegation delegation = delegationService.getDelegationById(id, lang);

			if (delegation == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), NO_DATA_FOUND, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning delegation {}", delegation.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.DELEGATION);

				return new ResponseEntity<>(delegation, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Value ------------------------------------
	@PostMapping("/api/v1/kpidata/{kpiId}/delegations")
	public ResponseEntity<Object> postKPIDelegationV1(@PathVariable("kpiId") Long kpiId,
			@RequestBody Delegation kpiDelegation, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		logger.info("Requested postKPIDelegationV1 id {} sourceRequest {}", kpiDelegation.getId(), sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			if (kpiDelegation.getUsernameDelegated() != null
					&& kpiDelegation.getUsernameDelegated().equals("ANONYMOUS")) {
				kpiData.setOwnership("public");
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.CHANGEOWNERSHIP);
				kpiDataService.saveKPIData(kpiData);
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION);

			logger.info("Posted kpiDelegation {}", kpiDelegation.getId());
			kpiDelegation.setElementId(kpiId.toString());
			return userController.postDelegationV1(credentialService.getLoggedUsername(lang), kpiDelegation,
					sourceRequest, lang, request);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PUT New KPI Delegation
	// ------------------------------------
	@PutMapping("/api/v1/kpidata/{kpiId}/delegations/{id}")
	public ResponseEntity<Object> putKPIDelegationV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody Delegation kpiDelegation, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		logger.info("Requested putKPIDelegationV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			Delegation oldKpiDelegation = delegationService.getDelegationById(id, lang);
			if (oldKpiDelegation == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), NO_DATA_FOUND, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			kpiDelegation.setId(oldKpiDelegation.getId());
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION);

			if (kpiDelegation.getUsernameDelegated() != null
					&& kpiDelegation.getUsernameDelegated().equals("ANONYMOUS")) {
				kpiData.setOwnership("public");
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.CHANGEOWNERSHIP);
				kpiDataService.saveKPIData(kpiData);
			}

			logger.info("Putted kpiDelegation {}", kpiDelegation.getId());
			return userController.putDelegationV1(credentialService.getLoggedUsername(lang), kpiDelegation.getId(),
					kpiDelegation, sourceRequest, lang, request);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PATCH New KPI Delegation
	// ------------------------------------
	@PatchMapping("/api/v1/kpidata/{kpiId}/delegations/{id}")
	public ResponseEntity<Object> patchKPIDelegationV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody Map<String, Object> fields, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		logger.info("Requested patchKPIDelegationV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			Delegation oldKpiDelegation = delegationService.getDelegationById(id, lang);
			if (oldKpiDelegation == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), NO_DATA_FOUND, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			// Problem with cast of int to long, but the id is also present and it must be
			// the same
			fields.remove("id");
			// Map key is field name, v is value
			fields.forEach((k, v) -> {
				// use reflection to get field k on manager and set it to value k
				Field field = ReflectionUtils.findField(Delegation.class, k);

				if (field != null && v != null) {
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, oldKpiDelegation, (field.getType()).cast(v));
				}
			});

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION);

			if (oldKpiDelegation.getUsernameDelegated() != null
					&& oldKpiDelegation.getUsernameDelegated().equals("ANONYMOUS")) {
				kpiData.setOwnership("public");
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.CHANGEOWNERSHIP);
				kpiDataService.saveKPIData(kpiData);
			}

			logger.info("Patched kpiDelegation {}", oldKpiDelegation.getId());
			return userController.putDelegationV1(credentialService.getLoggedUsername(lang), oldKpiDelegation.getId(),
					oldKpiDelegation, sourceRequest, lang, request);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE New KPI Value ------------------------------------
	@DeleteMapping("/api/v1/kpidata/{kpiId}/delegations/{id}")
	public ResponseEntity<Object> deleteKPIDelegationV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		logger.info("Requested deleteKPIDelegationV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			Delegation kpiDelegationToDelete = delegationService.getDelegationById(id, lang);
			if (kpiDelegationToDelete == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), NO_DATA_FOUND, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			kpiDelegationToDelete.setDeleteTime(new Date());
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.DELEGATION);
			logger.info("Deleted kpiDelegation {}", kpiDelegationToDelete.getId());
			return userController.putDelegationV1(credentialService.getLoggedUsername(lang),
					kpiDelegationToDelete.getId(), kpiDelegationToDelete, sourceRequest, lang, request);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL USER KPI Delegation Pageable ---------------
	@GetMapping("/api/v1/kpidata/{kpiId}/delegations")
	public ResponseEntity<Object> getAllDelegationV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			HttpServletRequest request) {

		logger.info(
				"Requested getAllDelegationV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			Page<Delegation> pageKpiDelegation = null;
			List<Delegation> listKpiDelegation = null;
			if (pageNumber != -1) {
				pageKpiDelegation = delegationService.findByElementIdByElementTypeWithoutAnonymous(Long.toString(kpiId),
						kpiData.getHighLevelType(),
						PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
			} else {
				listKpiDelegation = delegationService.findByElementIdByElementTypeNoPagesWithoutAnonymous(
						Long.toString(kpiId), kpiData.getHighLevelType());
			}

			if (pageKpiDelegation == null && listKpiDelegation == null) {
				logger.info("No delegation data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), "No delegation data found", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiDelegation != null) {
				logger.info("Returning KpiDelegationPage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION);

				return new ResponseEntity<>(pageKpiDelegation, HttpStatus.OK);
			} else {
				logger.info("Returning KpiDelegationList ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION);

				return new ResponseEntity<>(listKpiDelegation, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

	// -------------------GET ALL ORG KPI Delegation Pageable ---------------
	@GetMapping("/api/v1/kpidata/{kpiId}/orgdelegations")
	public ResponseEntity<Object> getAllOrgDelegationV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			HttpServletRequest request) {

		logger.info(
				"Requested getAllDelegationV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			List<Delegation> delegationsList = delegationService
					.findByElementIdByElementTypeNoPages(kpiData.getId().toString(), kpiData.getHighLevelType());
			delegationsList = delegationsList.stream().filter(e -> e.getGroupnameDelegated() != null)
					.collect(Collectors.toList());

			Page<Delegation> pageKpiDelegation = null;
			List<Delegation> listKpiDelegation = null;
			if (pageNumber != -1) {
				pageKpiDelegation = new PageImpl<>(delegationsList,
						PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)),
						delegationsList.size());
			} else {
				listKpiDelegation = delegationsList;
			}

			if (pageKpiDelegation == null && listKpiDelegation == null) {
				logger.info("No org delegation data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(), "No delegation data found", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiDelegation != null) {
				logger.info("Returning KpiDelegationPage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION);

				return new ResponseEntity<>(pageKpiDelegation, HttpStatus.OK);
			} else {
				logger.info("Returning KpiDelegationList ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION);

				return new ResponseEntity<>(listKpiDelegation, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

}