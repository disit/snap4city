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

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroup;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IDelegationService;
import edu.unifi.disit.datamanager.service.IDeviceGroupService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;

@RestController
public class DeviceGroupController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDeviceGroupService deviceGroupService;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IKPIActivityService kpiActivityService;

	@Autowired
	ICredentialsService credentialService;

	@Autowired
	IAccessService accessService;

	// -------------------POST New KPI Data ------------------------------------
	@PostMapping("/api/v1/devicegroup")
	public ResponseEntity<Object> postDeviceGroupV1(@RequestBody DeviceGroup deviceGroup,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested postDeviceGroupV1 id {} sourceRequest {}", deviceGroup.getId(), sourceRequest);

		try {
			DeviceGroup newDeviceGroup = deviceGroupService.saveDeviceGroup(deviceGroup);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, newDeviceGroup.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.GROUP);

			logger.info("Posted deviceGroup {}", newDeviceGroup.getId());

			if (newDeviceGroup.getOwnership().equals("public")) {
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, newDeviceGroup.getId(), ActivityAccessType.READ, KPIActivityDomainType.CHANGEOWNERSHIP);

				deviceGroupService.makeDeviceGroupPublic(newDeviceGroup.getUsername(), newDeviceGroup.getId(),
						newDeviceGroup.getHighLevelType(), lang);
			}

			return new ResponseEntity<>(newDeviceGroup, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (DelegationNotValidException d) {
			logger.warn("Problem with public or private ownership", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					"Problem with public or private ownership", d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}
        
        // -------------------GET ALL KPI Data Pageable -----------------------------
	@GetMapping("/api/v1/devicegroup")
	public ResponseEntity<Object> getOwnDeviceGroupsV1Pageable(@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info(
				"Requested getOwnDeviceGroupsV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<DeviceGroup> pageDeviceGroups = null;
			List<DeviceGroup> listDeviceGroups = null;
			if (pageNumber != -1) {
				if (credentialService.isRoot(lang)) {
					if (searchKey.equals("") && highLevelType.equals("")) {
						pageDeviceGroups = deviceGroupService.findAll(new PageRequest(pageNumber, pageSize,
								new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						pageDeviceGroups = deviceGroupService.findByHighLevelTypeFiltered(highLevelType, searchKey,
								new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (highLevelType.equals("")) {
						pageDeviceGroups = deviceGroupService.findAllFiltered(searchKey, new PageRequest(pageNumber, pageSize,
								new Sort(Direction.fromString(sortDirection), sortBy)));
					} else {
						pageDeviceGroups = deviceGroupService.findByHighLevelType(highLevelType, new PageRequest(pageNumber,
								pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
					}
				} else {
					if (searchKey.equals("") && highLevelType.equals("")) {
						pageDeviceGroups = deviceGroupService.findByUsername(credentialService.getLoggedUsername(lang),
								new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						pageDeviceGroups = deviceGroupService.findByUsernameByHighLevelTypeFiltered(
								credentialService.getLoggedUsername(lang), highLevelType, searchKey, new PageRequest(
										pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (highLevelType.equals("")) {
						pageDeviceGroups = deviceGroupService.findByUsernameFiltered(credentialService.getLoggedUsername(lang),
								searchKey, new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)));
					} else {
						pageDeviceGroups = deviceGroupService.findByUsernameByHighLevelType(
								credentialService.getLoggedUsername(lang), highLevelType, new PageRequest(pageNumber,
										pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
					}
				}
			} else {
				if (credentialService.isRoot(lang)) {
					if (searchKey.equals("") && highLevelType.equals("")) {
						listDeviceGroups = deviceGroupService.findAllNoPages();
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						listDeviceGroups = deviceGroupService.findByHighLevelTypeFilteredNoPages(highLevelType, searchKey);
					} else if (highLevelType.equals("")) {
						listDeviceGroups = deviceGroupService.findAllFilteredNoPages(searchKey);
					} else {
						listDeviceGroups = deviceGroupService.findByHighLevelTypeNoPages(highLevelType);
					}
				} else {
					if (searchKey.equals("") && highLevelType.equals("")) {
						listDeviceGroups = deviceGroupService.findByUsernameNoPages(credentialService.getLoggedUsername(lang));
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						listDeviceGroups = deviceGroupService.findByUsernameByHighLevelTypeFilteredNoPages(
								credentialService.getLoggedUsername(lang), highLevelType, searchKey);
					} else if (highLevelType.equals("")) {
						listDeviceGroups = deviceGroupService
								.findByUsernameFilteredNoPages(credentialService.getLoggedUsername(lang), searchKey);
					} else {
						listDeviceGroups = deviceGroupService.findByUsernameByHighLevelTypeNoPages(
								credentialService.getLoggedUsername(lang), highLevelType);
					}
				}
			}
			if (pageDeviceGroups == null && listDeviceGroups == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageDeviceGroups != null) {
				logger.info("Returning device groups page ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(pageDeviceGroups, HttpStatus.OK);
			} else if (listDeviceGroups != null) {
				logger.info("Returning device groups list ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(listDeviceGroups, HttpStatus.OK);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}
        
        // -------------------DEPRECATED ------------------------------------
	// -------------------GET PUBLIC KPI Data Pageable -----------------------------
	/**
	    * @deprecated (when, Modificata la semantica del public, refactoring advice...)
	    */
	@Deprecated
	@GetMapping("/api/v1/devicegroup/public")
	public ResponseEntity<Object> getDeviceGroupsPublicV1Pageable(
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info(
				"Requested getDeviceGroupsPublicV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<DeviceGroup> pageKpiData = null;
			List<DeviceGroup> listKpiData = null;

			if (pageNumber != -1) {
				if (credentialService.isRoot(lang)) {
					pageKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeFiltered("ANONYMOUS", "My",
							highLevelType, searchKey, new PageRequest(pageNumber, pageSize,
									new Sort(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(
							"ANONYMOUS", "My", highLevelType, searchKey, new PageRequest(pageNumber, pageSize,
									new Sort(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (credentialService.isRoot(lang)) {
					listKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages("ANONYMOUS",
							"My", highLevelType, searchKey);
				} else {
					listKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(
							"ANONYMOUS", "My", highLevelType, searchKey);
				}
			}

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No public data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No public data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpiDatapage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						null, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(pageKpiData, HttpStatus.OK);
			} else {
				logger.info("Returning kpiDatalist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						null, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(listKpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

	// -------------------GET PUBLIC KPI Data Pageable -----------------------------
	@GetMapping("/api/v1/public/devicegroup")
	public ResponseEntity<Object> getPublicDeviceGroupV1Pageable(
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info(
				"Requested getPublicDeviceGroupV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<DeviceGroup> pageKpiData = null;
			List<DeviceGroup> listKpiData = null;

			if (pageNumber != -1) {

				pageKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeFiltered("ANONYMOUS", "My",
						highLevelType, searchKey,
						new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));

			} else {

				listKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages("ANONYMOUS", "My",
						highLevelType, searchKey);

			}

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No public data found");

				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, null,
						ActivityAccessType.READ, KPIActivityDomainType.GROUP,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No public data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpiDatapage ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, sourceId, null, ActivityAccessType.READ,
						KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(pageKpiData, HttpStatus.OK);
			} else {
				logger.info("Returning kpiDatalist ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, sourceId, null, ActivityAccessType.READ,
						KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(listKpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, null, ActivityAccessType.READ,
					KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, null, ActivityAccessType.READ,
					KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

	// -------------------GET ORGANIZATION KPI Data Pageable
	// -----------------------------
	@GetMapping("/api/v1/devicegroup/organization")
	public ResponseEntity<Object> getOrganizationDeviceGroupV1Pageable(
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info(
				"Requested getOrganizationDeviceGroupV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<DeviceGroup> pageKpiData = null;
			List<DeviceGroup> listKpiData = null;

			if (pageNumber != -1) {
				pageKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered("ANONYMOUS",
						"My", highLevelType, searchKey,
						new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));

			} else {
				listKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(
						"ANONYMOUS", "My", highLevelType, searchKey);
			}

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No organization data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No organization data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpiDatapage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(pageKpiData, HttpStatus.OK);
			} else {
				logger.info("Returning kpiDatalist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(listKpiData, HttpStatus.OK);
			}
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

	// -------------------GET DELEGATED KPI Data Pageable
	// -----------------------------
	@GetMapping("/api/v1/devicegroup/delegated")
	public ResponseEntity<Object> getDelegatedDeviceGroupV1Pageable(
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info(
				"Requested getDelegatedDeviceGroupV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<DeviceGroup> pageKpiData = null;
			List<DeviceGroup> listKpiData = null;

			if (pageNumber != -1) {
				pageKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeFiltered(
						credentialService.getLoggedUsername(lang), "My", highLevelType, searchKey,
						new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
			} else {
				listKpiData = deviceGroupService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages(
						credentialService.getLoggedUsername(lang), "My", highLevelType, searchKey);
			}

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No delegated data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No delegated data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpiDatapage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(pageKpiData, HttpStatus.OK);
			} else {
				logger.info("Returning kpiDatalist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(listKpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}
        
        // -------------------PATCH KPI Data From ID --------------------------------
	@PatchMapping("/api/v1/devicegroup/{id}")
	public ResponseEntity<Object> patchDeviceGroupV1ById(@PathVariable("id") Long id,
			@RequestBody Map<String, Object> fields, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested patchDeviceGroupV1ById id {} sourceRequest {}", id, sourceRequest);

		try {
			DeviceGroup oldGrpData = deviceGroupService.getDeviceGroupById(id, lang, false);
			if (oldGrpData == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, id, ActivityAccessType.WRITE, KPIActivityDomainType.GROUP,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			String oldUsername = oldGrpData.getUsername();
			String oldOwnership = oldGrpData.getOwnership();

			// Problem with cast of int to long, but the id is also present and it must be
			// the same
			fields.remove("id");
			// Map key is field name, v is value

                        Date date = new Date();				
                        fields.put("updateTime", date);
			
			fields.forEach((k, v) -> {
				// use reflection to get field k on manager and set it to value k
				Field field = ReflectionUtils.findField(DeviceGroup.class, k);

				if (field != null && v != null) {
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, oldGrpData, (field.getType()).cast(v));
				}
			});

			DeviceGroup newGrpData = deviceGroupService.saveDeviceGroup(oldGrpData);
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, newGrpData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.GROUP);

			logger.info("Patched kpivalue {}", newGrpData.getId());

			if (!newGrpData.getOwnership().equals(oldOwnership)) {
				if (newGrpData.getOwnership().equals("public")) {
					kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang),
							sourceRequest, sourceId, newGrpData.getId(), ActivityAccessType.WRITE,
							KPIActivityDomainType.CHANGEOWNERSHIP);

					deviceGroupService.makeDeviceGroupPrivate(newGrpData.getId(), lang);
					deviceGroupService.makeDeviceGroupPublic(newGrpData.getUsername(), newGrpData.getId(),
							newGrpData.getHighLevelType(), lang);
				} else {
					kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang),
							sourceRequest, sourceId, newGrpData.getId(), ActivityAccessType.WRITE,
							KPIActivityDomainType.CHANGEOWNERSHIP);

					deviceGroupService.makeDeviceGroupPrivate(newGrpData.getId(), lang);
				}
			}

			if (!newGrpData.getUsername().equals(oldUsername)) {
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, newGrpData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.CHANGEOWNER);
				deviceGroupService.updateUsernameDelegatorOnOwnershipChange(newGrpData.getUsername(), newGrpData.getId(),
						lang);
			}

			return new ResponseEntity<>(newGrpData, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, id, ActivityAccessType.WRITE, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (DelegationNotValidException d) {
			logger.warn("Problem with public or private ownership", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					"Problem with public or private ownership", d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

        // -------------------GET KPI Data From ID ------------------------------------
	@GetMapping("/api/v1/devicegroup/{id}")
	public ResponseEntity<Object> getGrpDataV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getGrpDataV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {
			DeviceGroup kpiData = deviceGroupService.getDeviceGroupById(id, lang, false);

			if (kpiData == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, id, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
						&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiData.getId()), lang).getResult())) {
					throw new CredentialsException();
				}

				logger.info("Returning Device Group {}", kpiData.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(kpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, id, ActivityAccessType.READ, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
        
        // -------------------DELETE KPI Data From ID --------------------------------
	@DeleteMapping("/api/v1/devicegroup/{id}")
	public ResponseEntity<Object> deleteDeviceGroupV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested deleteDeviceGroupV1ById id {} sourceRequest {}", id, sourceRequest);

		try {
			DeviceGroup kpiDataToDelete = deviceGroupService.getDeviceGroupById(id, lang, false);
			if (kpiDataToDelete == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, id, ActivityAccessType.DELETE, KPIActivityDomainType.GROUP,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			kpiDataToDelete.setDeleteTime(new Date());

			DeviceGroup newKpiData = deviceGroupService.saveDeviceGroup(kpiDataToDelete);
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, id,
					ActivityAccessType.DELETE, KPIActivityDomainType.GROUP);

			logger.info("Deleted {}", id);

			return new ResponseEntity<>(newKpiData, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, id, ActivityAccessType.DELETE, KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
        
        @GetMapping("/api/v1/public/devicegroup/{id}")
	public ResponseEntity<Object> getPublicDeviceGroupV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getPublicDeviceGroupV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {
			DeviceGroup grp = deviceGroupService.getDeviceGroupById(id, lang, true);

			if (grp == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, id,
						ActivityAccessType.READ, KPIActivityDomainType.GROUP,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				if (grp.getOwnership().equals("private") || !grp.getOwnership().equals("public")) {
					throw new CredentialsException();
				}

				logger.info("Returning device group {}", grp.getId());

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, null, grp.getId(),
						ActivityAccessType.READ, KPIActivityDomainType.GROUP);

				return new ResponseEntity<>(grp, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, id, ActivityAccessType.READ,
					KPIActivityDomainType.GROUP,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
           
}