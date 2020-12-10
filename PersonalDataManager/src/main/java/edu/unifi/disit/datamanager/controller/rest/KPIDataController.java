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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.dto.KPIDataDTO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;
import edu.unifi.disit.datamanager.exception.LDAPException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IDelegationService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;
import edu.unifi.disit.datamanager.service.IKPIDataService;

@RestController
public class KPIDataController {

	private static final Logger logger = LogManager.getLogger();
	private static final String NO_DATA_FOUND = "No data found";
	private static final String RIGHTS_EXCEPTION = "Rights exception";
	private static final String PUBLIC = "public";
	private static final String OWNERSHIP_PROBLEM = "Problem with public or private ownership";
	private static final String ANONYMOUS = "ANONYMOUS";
	private static final String WRONG_ARGUMENTS = "Wrong Arguments";
	
	@Autowired
	IKPIDataService kpiDataService;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IKPIActivityService kpiActivityService;

	@Autowired
	ICredentialsService credentialService;

	@Autowired
	IAccessService accessService;
	
	@Autowired
	KPIValueController kpiValueController;

	// -------------------GET KPI Data From ID ------------------------------------
	@GetMapping("/api/v1/kpidata/{id}")
	public ResponseEntity<Object> getKPIDataV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIDataV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(id, lang, false);

			if (kpiData == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, id, ActivityAccessType.READ, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						NO_DATA_FOUND, null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
						&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiData.getId()), kpiData.getHighLevelType(), lang).getResult())) {
					throw new CredentialsException();
				}

				logger.info("Returning kpiData {}", kpiData.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(kpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, id, ActivityAccessType.READ, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Public KPI Data From ID
	// ------------------------------------
	@GetMapping("/api/v1/public/kpidata/{id}")
	public ResponseEntity<Object> getPublicKPIDataV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getPublicKPIDataV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(id, lang, true);

			if (kpiData == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(PUBLIC, sourceRequest, id,
						ActivityAccessType.READ, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						NO_DATA_FOUND, null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals(PUBLIC)) {
					throw new CredentialsException();
				}

				logger.info("Returning kpiData {}", kpiData.getId());

				kpiActivityService.saveActivityFromUsername(PUBLIC, sourceRequest, sourceId, kpiData.getId(),
						ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(kpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(PUBLIC, sourceRequest, id, ActivityAccessType.READ,
					KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Data ------------------------------------
	@PostMapping("/api/v1/kpidata")
	public ResponseEntity<Object> postKPIDataV1(@RequestBody KPIDataDTO dto,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		
		KPIData kpiData = new KPIData(dto);
		
		logger.info("Requested postKPIDataV1 id {} sourceRequest {}", kpiData.getId(), sourceRequest);

		try {
			KPIData newKpiData = kpiDataService.saveKPIData(kpiData);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, newKpiData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.DATA);

			logger.info("Posted kpiData {}", newKpiData.getId());

			if (newKpiData.getOwnership().equals(PUBLIC)) {
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, newKpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.CHANGEOWNERSHIP);

				kpiDataService.makeKPIDataPublic(newKpiData.getUsername(), newKpiData.getId(),
						newKpiData.getHighLevelType(), lang);
			}

			return new ResponseEntity<>(newKpiData, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (DelegationNotValidException d) {
			logger.warn(OWNERSHIP_PROBLEM, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					OWNERSHIP_PROBLEM, d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------PUT KPI Data From ID ------------------------------------
	@PutMapping("/api/v1/kpidata/{id}")
	public ResponseEntity<Object> putKPIDataV1ById(@PathVariable("id") Long id, @RequestBody KPIDataDTO dto,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		KPIData kpiData = new KPIData(dto);
		
		logger.info("Requested putKPIDataV1 id {} sourceRequest {}", id, sourceRequest);

		try {
			KPIData oldKpiData = kpiDataService.getKPIDataById(id, lang, false);
			if (oldKpiData == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, id, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						NO_DATA_FOUND, null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			kpiData.setId(oldKpiData.getId());
			KPIData newKpiData = kpiDataService.saveKPIData(kpiData);
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, newKpiData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.DATA);
			logger.info("Putted kpiData {}", kpiData.getId());

			if (!newKpiData.getOwnership().equals(oldKpiData.getOwnership())) {
				if (newKpiData.getOwnership().equals(PUBLIC)) {
					kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang),
							sourceRequest, sourceId, newKpiData.getId(), ActivityAccessType.WRITE,
							KPIActivityDomainType.CHANGEOWNERSHIP);

					kpiDataService.makeKPIDataPrivate(newKpiData.getId(),newKpiData.getHighLevelType(), lang);
					kpiDataService.makeKPIDataPublic(newKpiData.getUsername(), newKpiData.getId(),
							newKpiData.getHighLevelType(), lang);
				} else {
					kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang),
							sourceRequest, sourceId, newKpiData.getId(), ActivityAccessType.WRITE,
							KPIActivityDomainType.CHANGEOWNERSHIP);

					kpiDataService.makeKPIDataPrivate(newKpiData.getId(), newKpiData.getHighLevelType(),lang);
				}
			}

			if (!newKpiData.getUsername().equals(oldKpiData.getUsername())) {
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, newKpiData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.CHANGEOWNER);
				kpiDataService.updateUsernameDelegatorOnOwnershipChange(newKpiData.getUsername(), newKpiData.getId(),
						newKpiData.getHighLevelType(), lang);
			}

			return new ResponseEntity<>(newKpiData, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, id, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (DelegationNotValidException d) {
			logger.warn(OWNERSHIP_PROBLEM, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					OWNERSHIP_PROBLEM, d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------PATCH KPI Data From ID --------------------------------
	@PatchMapping("/api/v1/kpidata/{id}")
	public ResponseEntity<Object> patchKPIDataV1ById(@PathVariable("id") Long id,
			@RequestBody Map<String, Object> fields, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested patchKPIDataV1 id {} sourceRequest {}", id, sourceRequest);

		try {
			KPIData oldKpiData = kpiDataService.getKPIDataById(id, lang, false);
			if (oldKpiData == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, id, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						NO_DATA_FOUND, null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			String oldUsername = oldKpiData.getUsername();
			String oldOwnership = oldKpiData.getOwnership();

			// Problem with cast of int to long, but the id is also present and it must be
			// the same
			fields.remove("id");
			// Map key is field name, v is value

			if (fields.get("lastDate") != null) {
				Date date = new Date();
				date.setTime((Long) fields.get("lastDate"));
				fields.put("lastDate", date);
			}
			fields.forEach((k, v) -> {
				// use reflection to get field k on manager and set it to value k
				Field field = ReflectionUtils.findField(KPIData.class, k);

				if (field != null && v != null) {
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, oldKpiData, (field.getType()).cast(v));
				}
			});

			KPIData newKpiData = kpiDataService.saveKPIData(oldKpiData);
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, newKpiData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.DATA);
			logger.info("Patched kpivalue {}", newKpiData.getId());

			if (!newKpiData.getOwnership().equals(oldOwnership)) {
				if (newKpiData.getOwnership().equals(PUBLIC)) {
					kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang),
							sourceRequest, sourceId, newKpiData.getId(), ActivityAccessType.WRITE,
							KPIActivityDomainType.CHANGEOWNERSHIP);

					kpiDataService.makeKPIDataPrivate(newKpiData.getId(), newKpiData.getHighLevelType(), lang);
					kpiDataService.makeKPIDataPublic(newKpiData.getUsername(), newKpiData.getId(),
							newKpiData.getHighLevelType(), lang);
				} else {
					kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang),
							sourceRequest, sourceId, newKpiData.getId(), ActivityAccessType.WRITE,
							KPIActivityDomainType.CHANGEOWNERSHIP);

					kpiDataService.makeKPIDataPrivate(newKpiData.getId(), newKpiData.getHighLevelType(),lang);
				}
			}

			if (!newKpiData.getUsername().equals(oldUsername)) {
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, newKpiData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.CHANGEOWNER);
				kpiDataService.updateUsernameDelegatorOnOwnershipChange(newKpiData.getUsername(), newKpiData.getId(),
						newKpiData.getHighLevelType(),lang);
			}

			return new ResponseEntity<>(newKpiData, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, id, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (DelegationNotValidException d) {
			logger.warn(OWNERSHIP_PROBLEM, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					OWNERSHIP_PROBLEM, d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE KPI Data From ID --------------------------------
	@DeleteMapping("/api/v1/kpidata/{id}")
	public ResponseEntity<Object> deleteKPIDataV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested deleteKPIDataV1 id {} sourceRequest {}", id, sourceRequest);

		try {
			KPIData kpiDataToDelete = kpiDataService.getKPIDataById(id, lang, false);
			if (kpiDataToDelete == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, id, ActivityAccessType.DELETE, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						NO_DATA_FOUND, null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				if (!kpiDataToDelete.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
						&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiDataToDelete.getId()), kpiDataToDelete.getHighLevelType(), lang).getResult())) {
					throw new CredentialsException();
				}

				kpiValueController.deleteAllKPIValuesV1(id, sourceRequest, sourceId, lang, request);

				kpiDataToDelete.setDeleteTime(new Date());

				KPIData newKpiData = kpiDataService.saveKPIData(kpiDataToDelete);
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, id,
						ActivityAccessType.DELETE, KPIActivityDomainType.DATA);

				logger.info("Deleted {}", id);

				return new ResponseEntity<>(newKpiData, HttpStatus.OK);
			}
			
			
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, id, ActivityAccessType.DELETE, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DEPRECATED ------------------------------------
	// -------------------GET PUBLIC KPI Data Pageable -----------------------------
	/**
	 * @deprecated (when, Modificata la semantica del public, refactoring advice...)
	 */
	@Deprecated
	@GetMapping("/api/v1/kpidata/public")
	public ResponseEntity<Object> getKPIDataPublicV1Pageable(
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
				"Requested getKPIDataPublicV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<KPIData> pageKpiData = null;
			List<KPIData> listKpiData = null;

			if (pageNumber != -1) {
				if (credentialService.isRoot(lang)) {
					pageKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFiltered(ANONYMOUS, "My",
							highLevelType, searchKey, PageRequest.of(pageNumber, pageSize,
									Sort.by(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(
							ANONYMOUS, "My", highLevelType, searchKey, PageRequest.of(pageNumber, pageSize,
									Sort.by(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (credentialService.isRoot(lang)) {
					listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages(ANONYMOUS,
							"My", highLevelType, searchKey);
				} else {
					listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(
							ANONYMOUS, "My", highLevelType, searchKey);
				}
			}

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No public data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No public data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpiDatapage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						null, null, ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(pageKpiData, HttpStatus.OK);
			} else {
				logger.info("Returning kpiDatalist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						null, null, ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(listKpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | LDAPException |CloneNotSupportedException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

	// -------------------GET PUBLIC KPI Data Pageable -----------------------------
	@GetMapping("/api/v1/public/kpidata")
	public ResponseEntity<Object> getPublicKPIDataV1Pageable(
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
				"Requested getPublicKPIDataV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<KPIData> pageKpiData = null;
			List<KPIData> listKpiData = null;

			if (pageNumber != -1) {

				pageKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFiltered(ANONYMOUS, "My",
						highLevelType, searchKey,
						PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));

			} else {

				listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages(ANONYMOUS, "My",
						highLevelType, searchKey);

			}

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No public data found");

				kpiActivityService.saveActivityViolationFromUsername(PUBLIC, sourceRequest, null,
						ActivityAccessType.READ, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No public data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpiDatapage ");

				kpiActivityService.saveActivityFromUsername(PUBLIC, sourceRequest, sourceId, null, ActivityAccessType.READ,
						KPIActivityDomainType.DATA);

				return new ResponseEntity<>(pageKpiData, HttpStatus.OK);
			} else {
				logger.info("Returning kpiDatalist ");

				kpiActivityService.saveActivityFromUsername(PUBLIC, sourceRequest, sourceId, null, ActivityAccessType.READ,
						KPIActivityDomainType.DATA);

				return new ResponseEntity<>(listKpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(PUBLIC, sourceRequest, null, ActivityAccessType.READ,
					KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | LDAPException |CloneNotSupportedException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(PUBLIC, sourceRequest, null, ActivityAccessType.READ,
					KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

	// -------------------GET ORGANIZATION KPI Data Pageable
	// -----------------------------
	@GetMapping("/api/v1/kpidata/organization")
	public ResponseEntity<Object> getOrganizationKPIDataV1Pageable(
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
				"Requested getOrganizationKPIDataV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<KPIData> pageKpiData = null;
			List<KPIData> listKpiData = null;

			if (pageNumber != -1) {
				pageKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(ANONYMOUS,
						"My", highLevelType, searchKey,
						PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));

			} else {
				listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(
						ANONYMOUS, "My", highLevelType, searchKey);
			}

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No organization data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No organization data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpiDatapage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(pageKpiData, HttpStatus.OK);
			} else {
				logger.info("Returning kpiDatalist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(listKpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(PUBLIC, sourceRequest, null, ActivityAccessType.READ,
					KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | LDAPException | CloneNotSupportedException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

	// -------------------GET DELEGATED KPI Data Pageable
	// -----------------------------
	@GetMapping("/api/v1/kpidata/delegated")
	public ResponseEntity<Object> getDelegatedKPIDataV1Pageable(
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
				"Requested getDelegatedKPIDataV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<KPIData> pageKpiData = null;
			List<KPIData> listKpiData = null;

			if (pageNumber != -1) {
				pageKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFiltered(
						credentialService.getLoggedUsername(lang), "My", highLevelType, searchKey,
						PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
			} else {
				listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages(
						credentialService.getLoggedUsername(lang), "My", highLevelType, searchKey);
			}

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No delegated data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No delegated data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpiDatapage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(pageKpiData, HttpStatus.OK);
			} else {
				logger.info("Returning kpiDatalist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(listKpiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | LDAPException |CloneNotSupportedException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		} 

	}

	// -------------------GET ALL KPI Data Pageable -----------------------------
	@GetMapping("/api/v1/kpidata")
	public ResponseEntity<Object> getOwnKPIDataV1Pageable(@RequestParam(value = "sourceRequest") String sourceRequest,
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
				"Requested getAllKPIDataV1Pageable highLevelType{} searchKey {} pageNumber {} pageSize {} sortDirection {} sortBy {}",
				highLevelType, searchKey, pageNumber, pageSize, sortDirection, sortBy);

		try {

			Page<KPIData> pageKpiData = null;
			List<KPIData> listKpiData = null;
			if (pageNumber != -1) {
				if (credentialService.isRoot(lang)) {
					if (searchKey.equals("") && highLevelType.equals("")) {
						pageKpiData = kpiDataService.findAll(PageRequest.of(pageNumber, pageSize,
								Sort.by(Direction.fromString(sortDirection), sortBy)));
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						pageKpiData = kpiDataService.findByHighLevelTypeFiltered(highLevelType, searchKey,
								PageRequest.of(pageNumber, pageSize,
										Sort.by(Direction.fromString(sortDirection), sortBy)));
					} else if (highLevelType.equals("")) {
						pageKpiData = kpiDataService.findAllFiltered(searchKey, PageRequest.of(pageNumber, pageSize,
								Sort.by(Direction.fromString(sortDirection), sortBy)));
					} else {
						pageKpiData = kpiDataService.findByHighLevelType(highLevelType, PageRequest.of(pageNumber,
								pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
					}
				} else {
					if (searchKey.equals("") && highLevelType.equals("")) {
						pageKpiData = kpiDataService.findByUsername(credentialService.getLoggedUsername(lang),
								PageRequest.of(pageNumber, pageSize,
										Sort.by(Direction.fromString(sortDirection), sortBy)));
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						pageKpiData = kpiDataService.findByUsernameByHighLevelTypeFiltered(
								credentialService.getLoggedUsername(lang), highLevelType, searchKey, PageRequest.of(
										pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
					} else if (highLevelType.equals("")) {
						pageKpiData = kpiDataService.findByUsernameFiltered(credentialService.getLoggedUsername(lang),
								searchKey, PageRequest.of(pageNumber, pageSize,
										Sort.by(Direction.fromString(sortDirection), sortBy)));
					} else {
						pageKpiData = kpiDataService.findByUsernameByHighLevelType(
								credentialService.getLoggedUsername(lang), highLevelType, PageRequest.of(pageNumber,
										pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
					}
				}
			} else {
				if (credentialService.isRoot(lang)) {
					if (searchKey.equals("") && highLevelType.equals("")) {
						listKpiData = kpiDataService.findAllNoPages();
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						listKpiData = kpiDataService.findByHighLevelTypeFilteredNoPages(highLevelType, searchKey);
					} else if (highLevelType.equals("")) {
						listKpiData = kpiDataService.findAllFilteredNoPages(searchKey);
					} else {
						listKpiData = kpiDataService.findByHighLevelTypeNoPages(highLevelType);
					}
				} else {
					if (searchKey.equals("") && highLevelType.equals("")) {
						listKpiData = kpiDataService.findByUsernameNoPages(credentialService.getLoggedUsername(lang));
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						listKpiData = kpiDataService.findByUsernameByHighLevelTypeFilteredNoPages(
								credentialService.getLoggedUsername(lang), highLevelType, searchKey);
					} else if (highLevelType.equals("")) {
						listKpiData = kpiDataService
								.findByUsernameFilteredNoPages(credentialService.getLoggedUsername(lang), searchKey);
					} else {
						listKpiData = kpiDataService.findByUsernameByHighLevelTypeNoPages(
								credentialService.getLoggedUsername(lang), highLevelType);
					}
				}
			}
			if (pageKpiData == null && listKpiData == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						NO_DATA_FOUND, null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpiDatapage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(pageKpiData, HttpStatus.OK);
			} else if (listKpiData != null) {
				logger.info("Returning kpiDatalist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, null, ActivityAccessType.READ, KPIActivityDomainType.DATA);

				return new ResponseEntity<>(listKpiData, HttpStatus.OK);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.DATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

}