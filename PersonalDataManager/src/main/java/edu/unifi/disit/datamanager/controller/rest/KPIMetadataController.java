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
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.dto.KPIMetadataDTO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIMetadata;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;
import edu.unifi.disit.datamanager.service.IKPIDataService;
import edu.unifi.disit.datamanager.service.IKPIMetadataService;

@RestController
public class KPIMetadataController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IKPIMetadataService kpiMetadataService;

	@Autowired
	IKPIDataService kpiDataService;

	@Autowired
	IAccessService accessService;

	@Autowired
	IKPIActivityService kpiActivityService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET KPI Metadata From ID
	// ------------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/metadata/{id}")
	public ResponseEntity<Object> getKPIMetadataV1ById(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIMetadataV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername()
					.equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIMetadata kpimetadata = kpiMetadataService.getKPIMetadataById(id, lang);

			if (kpimetadata == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpimetadata {}", kpimetadata.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.METADATA);

				return new ResponseEntity<>(kpimetadata, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Public KPI MEtadata From ID
	// ------------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/metadata/{id}")
	public ResponseEntity<Object> getPublicKPIMetadataV1ById(@PathVariable("kpiId") Long kpiId,
			@PathVariable("id") Long id, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getPublicKPIMetadataV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			KPIMetadata kpimetadata = kpiMetadataService.getKPIMetadataById(id, lang);

			if (kpimetadata == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpimetadata {}", kpimetadata.getId());

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, sourceId, kpiData.getId(),
						ActivityAccessType.READ, KPIActivityDomainType.METADATA);

				return new ResponseEntity<>(kpimetadata, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Metadata ------------------------------------
	@PostMapping("/api/v1/kpidata/{kpiId}/metadata")
	public ResponseEntity<Object> postKPIMetadataV1(@PathVariable("kpiId") Long kpiId,
			@RequestBody KPIMetadataDTO dto, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		KPIMetadata kpiMetadata = new KPIMetadata(dto);
		
		logger.info("Requested postKPIMetadataV1 id {} sourceRequest {}", kpiMetadata.getId(), sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername()
					.equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
					ActivityAccessType.WRITE, KPIActivityDomainType.METADATA);

			kpiMetadata.setKpiId(kpiId);
			kpiMetadataService.saveKPIMetadata(kpiMetadata);
			logger.info("Posted kpiMetadata {}", kpiMetadata.getId());

			return new ResponseEntity<>(kpiMetadata, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PUT New KPI Metadata ------------------------------------
	@PutMapping("/api/v1/kpidata/{kpiId}/metadata/{id}")
	public ResponseEntity<Object> putKPIMetadataV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody KPIMetadataDTO dto, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		KPIMetadata kpiMetadata = new KPIMetadata(dto);
		
		logger.info("Requested putKPIMetadataV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername()
					.equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIMetadata oldKpiMetadata = kpiMetadataService.getKPIMetadataById(id, lang);
			if (oldKpiMetadata == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			kpiMetadata.setId(oldKpiMetadata.getId());
			KPIMetadata newKpiMetadata = kpiMetadataService.saveKPIMetadata(oldKpiMetadata);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
					ActivityAccessType.WRITE, KPIActivityDomainType.METADATA);
			logger.info("Putted kpimetadata {}", kpiMetadata.getId());
			return new ResponseEntity<>(newKpiMetadata, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PATCH New KPI Metadata
	// ------------------------------------
	@PatchMapping("/api/v1/kpidata/{kpiId}/metadata/{id}")
	public ResponseEntity<Object> patchKPIMetadataV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody Map<String, Object> fields, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested patchKPIMetadataV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername()
					.equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIMetadata oldKpiMetadata = kpiMetadataService.getKPIMetadataById(id, lang);
			if (oldKpiMetadata == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			// Problem with cast of int to long, but the id is also present and it must be
			// the same
			fields.remove("id");
			fields.remove("kpiId");
			// Map key is field name, v is value
			fields.forEach((k, v) -> {
				// use reflection to get field k on manager and set it to value k
				Field field = ReflectionUtils.findField(KPIMetadata.class, k);

				if (field != null && v != null) {
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, oldKpiMetadata, (field.getType()).cast(v));
				}
			});

			KPIMetadata newKpiMetadata = kpiMetadataService.saveKPIMetadata(oldKpiMetadata);
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
					ActivityAccessType.WRITE, KPIActivityDomainType.METADATA);

			logger.info("Patched kpimetadata {}", newKpiMetadata.getId());
			return new ResponseEntity<>(newKpiMetadata, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE New KPI Metadata
	// ------------------------------------
	@DeleteMapping("/api/v1/kpidata/{kpiId}/metadata/{id}")
	public ResponseEntity<Object> deleteKPIMetadataV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested putKPIValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername()
					.equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIMetadata kpiMetadataToDelete = kpiMetadataService.getKPIMetadataById(id, lang);
			if (kpiMetadataToDelete == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			kpiMetadataToDelete.setDeleteTime(new Date());
			KPIMetadata newKpiMetadata = kpiMetadataService.saveKPIMetadata(kpiMetadataToDelete);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, kpiId,
					ActivityAccessType.DELETE, KPIActivityDomainType.METADATA);
			logger.info("Deleted kpivalue {}", id);
			return new ResponseEntity<>(newKpiMetadata, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Data Metadata Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/metadata")
	public ResponseEntity<Object> getAllKPIMetadataV1Pageable(@PathVariable("kpiId") Long kpiId,
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
				"Requested getAllKPIMetadataV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername()
					.equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			Page<KPIMetadata> pageKpiMetadata = null;
			List<KPIMetadata> listKpiMetadata = null;
			if (pageNumber != -1) {
				if (searchKey.equals("")) {
					pageKpiMetadata = kpiMetadataService.findAllByKpiId(kpiId, PageRequest.of(pageNumber, pageSize,
							Sort.by(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiMetadata = kpiMetadataService.findAllFilteredByKpiId(kpiId, searchKey, PageRequest.of(
							pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (searchKey.equals("")) {
					listKpiMetadata = kpiMetadataService.findByKpiIdNoPages(kpiId);
				} else {
					listKpiMetadata = kpiMetadataService.findFilteredByKpiIdNoPages(kpiId, searchKey);
				}
			}

			if (pageKpiMetadata == null && listKpiMetadata == null) {
				logger.info("No metadata data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No metadata data found", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiMetadata != null) {
				logger.info("Returning KpiVMetadataPage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.METADATA);

				return new ResponseEntity<>(pageKpiMetadata, HttpStatus.OK);
			} else {
				logger.info("Returning KpiMetadataList ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.METADATA);

				return new ResponseEntity<>(listKpiMetadata, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Data Metadata of Public KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/metadata")
	public ResponseEntity<Object> getAllKPIMetadataOfPublicKPIV1Pageable(@PathVariable("kpiId") Long kpiId,
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
				"Requested getAllKPIMetadataOfPublicKPIV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			Page<KPIMetadata> pageKpiMetadata = null;
			List<KPIMetadata> listKpiMetadata = null;
			if (pageNumber != -1) {
				if (searchKey.equals("")) {
					pageKpiMetadata = kpiMetadataService.findAllByKpiId(kpiId, PageRequest.of(pageNumber, pageSize,
							Sort.by(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiMetadata = kpiMetadataService.findAllFilteredByKpiId(kpiId, searchKey, PageRequest.of(
							pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (searchKey.equals("")) {
					listKpiMetadata = kpiMetadataService.findByKpiIdNoPages(kpiId);
				} else {
					listKpiMetadata = kpiMetadataService.findFilteredByKpiIdNoPages(kpiId, searchKey);
				}
			}

			if (pageKpiMetadata == null && listKpiMetadata == null) {
				logger.info("No metadata data found");

				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.METADATA,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No metadata data found", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiMetadata != null) {
				logger.info("Returning KpiVMetadataPage ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, sourceId, kpiId, ActivityAccessType.READ,
						KPIActivityDomainType.METADATA);

				return new ResponseEntity<>(pageKpiMetadata, HttpStatus.OK);
			} else {
				logger.info("Returning KpiMetadataList ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, sourceId, kpiId, ActivityAccessType.READ,
						KPIActivityDomainType.METADATA);

				return new ResponseEntity<>(listKpiMetadata, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.METADATA,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}
}