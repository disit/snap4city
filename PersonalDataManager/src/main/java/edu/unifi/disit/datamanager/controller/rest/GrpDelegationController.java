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

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
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
import edu.unifi.disit.datamanager.datamodel.ElementType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroup;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IDelegationService;
import edu.unifi.disit.datamanager.service.IDeviceGroupService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;
import java.io.IOException;

@RestController
public class GrpDelegationController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IDeviceGroupService deviceGroupService;

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

	// -------------------GET Device Group Delegation From ID
	// ------------------------------------
	@GetMapping("/api/v1/devicegroup/{kpiId}/delegations/{id}")
	public ResponseEntity<Object> getGrpDelegationV1ById(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getGrpDelegationV1ById id {} lang {}sourceRequest {}", id, lang, sourceRequest);

		try {

			DeviceGroup kpiData = deviceGroupService.getDeviceGroupById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong Device Group Data");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), ElementType.MYGROUP.toString(), lang).getResult())) {
				throw new CredentialsException();
			}

			Delegation delegation = delegationService.getDelegationById(id, lang);

			if (delegation == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning delegation {}", delegation.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.DELEGATION);

				return new ResponseEntity<>(delegation, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IOException d) {
			logger.warn("Probable connection error to sensors API of dashboardSmartCity", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) d.getMessage());
		}
	}

	// -------------------POST New Device Group Value ------------------------------------
	@PostMapping("/api/v1/devicegroup/{kpiId}/delegations")
	public ResponseEntity<Object> postGrpDelegationV1(@PathVariable("kpiId") Long kpiId,
			@RequestBody Delegation kpiDelegation, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		logger.info("Requested postGrpDelegationV1 id {} sourceRequest {}", kpiDelegation.getId(), sourceRequest);

		try {

			DeviceGroup kpiData = deviceGroupService.getDeviceGroupById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), ElementType.MYGROUP.toString(), lang).getResult())) {
				throw new CredentialsException();
			}

			if (kpiDelegation.getUsernameDelegated().equals("ANONYMOUS")) {
				kpiData.setOwnership("public");
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
						ActivityAccessType.WRITE, KPIActivityDomainType.CHANGEOWNERSHIP);
				deviceGroupService.saveDeviceGroup(kpiData);
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
					ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION);

			logger.info("Posted grpDelegation {}", kpiDelegation.getId());
			kpiDelegation.setElementId(kpiId.toString());
			return userController.postDelegationV1(credentialService.getLoggedUsername(lang), kpiDelegation,
					sourceRequest, lang, request);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IOException d) {
			logger.warn("Probable connection error to sensors API of dashboardSmartCity", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) d.getMessage());
		}
	}

	// -------------------PUT New Device Group Delegation
	// ------------------------------------
	@PutMapping("/api/v1/devicegroup/{kpiId}/delegations/{id}")
	public ResponseEntity<Object> putGrpDelegationV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody Delegation kpiDelegation, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		logger.info("Requested putGrpDelegationV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			DeviceGroup kpiData = deviceGroupService.getDeviceGroupById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), ElementType.MYGROUP.toString(), lang).getResult())) {
				throw new CredentialsException();
			}

			Delegation oldKpiDelegation = delegationService.getDelegationById(id, lang);
			if (oldKpiDelegation == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			kpiDelegation.setId(oldKpiDelegation.getId());
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
					ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION);

			if (kpiDelegation.getUsernameDelegated().equals("ANONYMOUS")) {
				kpiData.setOwnership("public");
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
						ActivityAccessType.WRITE, KPIActivityDomainType.CHANGEOWNERSHIP);
				deviceGroupService.saveDeviceGroup(kpiData);
			}

			logger.info("Putted grpDelegation {}", kpiDelegation.getId());
			return userController.putDelegationV1(credentialService.getLoggedUsername(lang), kpiDelegation.getId(),
					kpiDelegation, sourceRequest, lang, request);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IOException d) {
			logger.warn("Probable connection error to sensors API of dashboardSmartCity", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) d.getMessage());
		}
	}

	// -------------------PATCH New Device Group Delegation
	// ------------------------------------
	@PatchMapping("/api/v1/devicegroup/{kpiId}/delegations/{id}")
	public ResponseEntity<Object> patchGrpDelegationV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody Map<String, Object> fields, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		logger.info("Requested patchGrpDelegationV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			DeviceGroup kpiData = deviceGroupService.getDeviceGroupById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), ElementType.MYGROUP.toString(), lang).getResult())) {
				throw new CredentialsException();
			}

			Delegation oldKpiDelegation = delegationService.getDelegationById(id, lang);
			if (oldKpiDelegation == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

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

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
					ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION);

			if (oldKpiDelegation.getUsernameDelegated().equals("ANONYMOUS")) {
				kpiData.setOwnership("public");
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
						ActivityAccessType.WRITE, KPIActivityDomainType.CHANGEOWNERSHIP);
				deviceGroupService.saveDeviceGroup(kpiData);
			}

			logger.info("Patched grpDelegation {}", oldKpiDelegation.getId());
			return userController.putDelegationV1(credentialService.getLoggedUsername(lang), oldKpiDelegation.getId(),
					oldKpiDelegation, sourceRequest, lang, request);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IOException d) {
			logger.warn("Probable connection error to sensors API of dashboardSmartCity", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE New Device Group Value ------------------------------------
	@DeleteMapping("/api/v1/devicegroup/{kpiId}/delegations/{id}")
	public ResponseEntity<Object> deleteGrpDelegationV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) throws UnsupportedEncodingException {

		logger.info("Requested deleteGrpDelegationV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			DeviceGroup kpiData = deviceGroupService.getDeviceGroupById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), ElementType.MYGROUP.toString(), lang).getResult())) {
				throw new CredentialsException();
			}

			Delegation kpiDelegationToDelete = delegationService.getDelegationById(id, lang);
			if (kpiDelegationToDelete == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			kpiDelegationToDelete.setDeleteTime(new Date());
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
					ActivityAccessType.DELETE, KPIActivityDomainType.DELEGATION);
			logger.info("Deleted grpDelegation {}", kpiDelegationToDelete.getId());
			return userController.putDelegationV1(credentialService.getLoggedUsername(lang),
					kpiDelegationToDelete.getId(), kpiDelegationToDelete, sourceRequest, lang, request);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IOException d) {
			logger.warn("Probable connection error to sensors API of dashboardSmartCity", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.DELETE, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL Device Group Delegation Pageable ---------------
	@GetMapping("/api/v1/devicegroup/{kpiId}/delegations")
	public ResponseEntity<Object> getAllDelegationV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			HttpServletRequest request) {

		logger.info(
				"Requested getAllDelegationV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, kpiId);

		try {
			DeviceGroup kpiData = deviceGroupService.getDeviceGroupById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), ElementType.MYGROUP.toString(), lang).getResult())) {
				throw new CredentialsException();
			}

			Page<Delegation> pageKpiDelegation = null;
			List<Delegation> listKpiDelegation = null;
			if (pageNumber != -1) {
				if (searchKey.equals("")) {
					pageKpiDelegation = delegationService.findByElementIdWithoutAnonymous(Long.toString(kpiId),
							new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiDelegation = delegationService.findByElementIdWithoutAnonymousFiltered(Long.toString(kpiId), searchKey,
							new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (searchKey.equals("")) {
					listKpiDelegation = delegationService.findByElementIdNoPagesWithoutAnonymous(Long.toString(kpiId));
				} else {
					listKpiDelegation = delegationService.findByElementIdNoPagesWithoutAnonymousFiltered(Long.toString(kpiId), searchKey);
				}
			}

			if (pageKpiDelegation == null && listKpiDelegation == null) {
				logger.info("No delegation data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No delegation data found", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiDelegation != null) {
				logger.info("Returning GrpDelegationPage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION);

				return new ResponseEntity<>(pageKpiDelegation, HttpStatus.OK);
			} else {
				logger.info("Returning GrpDelegationList ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION);

				return new ResponseEntity<>(listKpiDelegation, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		} catch (IOException d) {
			logger.warn("Probable connection error to sensors API of dashboardSmartCity", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DELEGATION,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) d.getMessage());
		}

	}

}