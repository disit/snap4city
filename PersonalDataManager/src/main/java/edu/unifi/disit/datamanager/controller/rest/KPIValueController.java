/* Data Manager pi(DM).piDa
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.dto.KPIElasticValueDTO;
import edu.unifi.disit.datamanager.datamodel.dto.KPIValueDTO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIValue;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;
import edu.unifi.disit.datamanager.service.IKPIDataService;
import edu.unifi.disit.datamanager.service.IKPIValueService;

@RestController
public class KPIValueController {

	private static final Logger logger = LogManager.getLogger();
	private static final String MYSQL = "MySQL";
	private static final String ELASTICSEARCH = "ElasticSearch";

	@Autowired
	IKPIValueService kpiValueService;

	@Autowired
	IKPIDataService kpiDataService;

	@Autowired
	IAccessService accessService;

	@Autowired
	IKPIActivityService kpiActivityService;

	@Autowired
	ICredentialsService credentialService;

	@Autowired
	KPIElasticValueController kpiElasticValueController;

	@Autowired
	KPISQLValueController kpiSQLValueController;

	// -------------------GET KPI Value From ID ------------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> getKPIValueV1ById(@PathVariable("kpiId") Long kpiId, @PathVariable("id") String id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIValueV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.getKPISQLValueV1ById(kpiId, Long.valueOf(id), sourceRequest, sourceId,
						lang, request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.getKPIElasticValueV1ById(kpiId, id, sourceRequest, sourceId, lang,
						request);
			}

			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Public KPI Value From ID
	// ------------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> getPublicKPIValueV1ById(@PathVariable("kpiId") Long kpiId,
			@PathVariable("id") String id, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIValueV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.getPublicKPISQLValueV1ById(kpiId, Long.valueOf(id), sourceRequest,
						sourceId, lang, request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.getPublicKPIElasticValueV1ById(kpiId, id, sourceRequest, sourceId,
						lang, request);
			}

			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DEPRECATED ------------------------------------
	/**
	 * @deprecated (when, Cambiato il formato dell'url, refactoring advice...)
	 */
	@Deprecated
	@PostMapping("/api/v1/kpivalue/save")
	public ResponseEntity<Object> saveKPIValueV1(@RequestBody KPIValueDTO dto,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		KPIValue kpiValue = new KPIValue(dto);

		logger.info("Requested DEPRECATED saveKPIValueV1 id {} sourceRequest {}", kpiValue.getId(), sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiValue.getKpiId(), lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiValue.getKpiId(), ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiValue.getKpiId()), kpiData.getHighLevelType(), lang)
							.getResult())) {
				throw new CredentialsException();
			}

			try {
				if (kpiValue.getDataTime() != null) {
					List<KPIValue> lastKpiValue = kpiValueService.findByKpiIdNoPagesWithLimit(kpiValue.getKpiId(), null,
							null, 0, 1, lang);
					if (lastKpiValue.isEmpty() || (lastKpiValue.get(0) != null
							&& lastKpiValue.get(0).getDataTime() != null
							&& lastKpiValue.get(0).getDataTime().getTime() < kpiValue.getDataTime().getTime())) {
						kpiData.setLastDate(kpiValue.getDataTime());
						kpiData.setLastValue(kpiValue.getValue());
						if (kpiValue.getLatitude() != null && !kpiValue.getLatitude().equals("")) {
							kpiData.setLastLatitude(kpiValue.getLatitude());
						}
						if (kpiValue.getLongitude() != null && !kpiValue.getLongitude().equals("")) {
							kpiData.setLastLongitude(kpiValue.getLongitude());
						}
						kpiDataService.saveKPIData(kpiData);
					}
				} else {
					throw new DataNotValidException("Date format is wrong, please check or send the date as timestamp");
				}
			} catch (DataNotValidException d) {
				logger.warn("Wrong Arguments", d);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiValue.getKpiId(), ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
						request.getRemoteAddr());

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, null,
					kpiValue.getKpiId(), ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);

			kpiValueService.saveKPIValue(kpiValue);
			logger.info("Posted kpivalue {}", kpiValue.getId());

			return new ResponseEntity<>(kpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiValue.getKpiId(), ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Value ------------------------------------
	@PostMapping("/api/v1/kpidata/{kpiId}/values")
	public ResponseEntity<Object> postKPIValueV1(@PathVariable("kpiId") Long kpiId, @RequestBody Object objectKpiValue,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}
			ObjectMapper objectMapper = new ObjectMapper();
			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				try {
					kpiElasticValueController.postKPIElasticValueV1(kpiId,
							objectMapper.convertValue(objectKpiValue, KPIElasticValueDTO.class), sourceRequest,
							sourceId, lang, request);
				} catch (Exception e) {
					// FOR NOW DO NOTHING
				}
				return kpiSQLValueController.postKPISQLValueV1(kpiId,
						objectMapper.convertValue(objectKpiValue, KPIValueDTO.class), sourceRequest, sourceId, lang,
						request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.postKPIElasticValueV1(kpiId,
						objectMapper.convertValue(objectKpiValue, KPIElasticValueDTO.class), sourceRequest, sourceId,
						lang, request);
			}

			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Value Array
	// ------------------------------------
	@PostMapping("/api/v1/kpidata/{kpiId}/values/list")
	public ResponseEntity<Object> postKPIValueArrayV1(@PathVariable("kpiId") Long kpiId,
			@RequestBody Object kpiValueList, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested postKPIValueArrayV1 kpiId {} sourceRequest {}", kpiId, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);

			ObjectMapper objectMapper = new ObjectMapper();

			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				KPIValueDTO[] dtoList = objectMapper.convertValue(kpiValueList, KPIValueDTO[].class);
				return kpiSQLValueController.postKPISQLValueArrayV1(kpiId, Arrays.asList(dtoList), sourceRequest,
						sourceId, lang, request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				KPIElasticValueDTO[] dtoList = objectMapper.convertValue(kpiValueList, KPIElasticValueDTO[].class);
				return kpiElasticValueController.postKPIElasticValueArrayV1(kpiId, Arrays.asList(dtoList),
						sourceRequest, sourceId, lang, request);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PUT New KPI Value ------------------------------------
	@PutMapping("/api/v1/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> putKPIValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") String id,
			@RequestBody Object objectKpiValue, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested putKPIValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			ObjectMapper objectMapper = new ObjectMapper();
			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.putKPISQLValueV1(kpiId, Long.valueOf(id),
						objectMapper.convertValue(objectKpiValue, KPIValueDTO.class), sourceRequest, sourceId, lang,
						request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.putKPIElasticValueV1(kpiId, id,
						objectMapper.convertValue(objectKpiValue, KPIElasticValueDTO.class), sourceRequest, sourceId,
						lang, request);
			}

			return new ResponseEntity<>(HttpStatus.NO_CONTENT);

		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PATCH New KPI Value ------------------------------------
	@PatchMapping("/api/v1/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> patchKPIValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") String id,
			@RequestBody Map<String, Object> fields, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested patchKPIValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.patchKPISQLValueV1(kpiId, Long.parseLong(id), fields, sourceRequest,
						sourceId, lang, request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.patchKPIElasticValueV1(kpiId, id, fields, sourceRequest, sourceId,
						lang, request);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE New KPI Value ------------------------------------
	@DeleteMapping("/api/v1/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> deleteKPIValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") String id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested delete KPIValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.deleteKPISQLValueV1(kpiId, Long.valueOf(id), sourceRequest, sourceId, lang,
						request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.deleteKPIElasticValueV1(kpiId, id, sourceRequest, sourceId, lang,
						request);
			}

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), "No data found", null,
					request.getRemoteAddr());

			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE All KPI Value ------------------------------------
	@DeleteMapping("/api/v1/kpidata/{kpiId}/values")
	public ResponseEntity<Object> deleteAllKPIValuesV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested delete deleteAllKPIValuesV1 kpiId {} sourceRequest {}", kpiId, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.deleteKAllPISQLValuesV1(kpiId, sourceRequest, sourceId, lang, request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.deleteKAllPIElasticValuesV1(kpiId, sourceRequest, sourceId, lang,
						request);
			}

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), "No data found", null,
					request.getRemoteAddr());

			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/values")
	public ResponseEntity<Object> getAllKPIValueV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "date_time") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "from", required = false) String fromString,
			@RequestParam(value = "to", required = false) String toString,
			@RequestParam(value = "first", required = false) Integer first,
			@RequestParam(value = "last", required = false) Integer last, HttpServletRequest request) {

		Date from = new Date();
		if (fromString != null) {
			try {
				from.setTime(Long.parseLong(fromString));
			} catch (NumberFormatException e) {
				try {
					from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX").parse(fromString);
				} catch (ParseException | NullPointerException d) {
					try {
						from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(fromString);
					} catch (ParseException | NullPointerException f) {
						logger.warn("Parsing error date {} username {} kpiId {} sourceid {}", fromString,
								credentialService.getLoggedUsername(lang), kpiId, sourceId);
						from = null;
					}
				}
			}
		} else {
			from = null;
		}

		Date to = new Date();
		if (toString != null) {
			try {
				to.setTime(Long.parseLong(toString));
			} catch (NumberFormatException e) {
				try {
					to = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX").parse(toString);
				} catch (ParseException | NullPointerException d) {
					try {
						to = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(toString);
					} catch (ParseException | NullPointerException f) {
						logger.warn("Parsing error date {} username {} kpiId {} sourceid {}", toString,
								credentialService.getLoggedUsername(lang), kpiId, sourceId);
						to = null;
					}
				}
			}
		} else {
			to = null;
		}

		logger.info(
				"Requested getAllKPIValueV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.getAllKPISQLValueV1Pageable(kpiId, sourceRequest, sourceId, lang,
						pageNumber, pageSize, sortDirection, sortBy, searchKey, from, to, first, last, request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.getAllKPIElasticValueV1Pageable(kpiId, sourceRequest, sourceId, lang,
						pageNumber, pageSize, sortDirection, sortBy, searchKey, from, to, first, last, request);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value of PUBLIC KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/values")
	public ResponseEntity<Object> getAllKPIValueOfPublicKPIV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "date_time") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "from", required = false) String fromString,
			@RequestParam(value = "to", required = false) String toString,
			@RequestParam(value = "first", required = false) Integer first,
			@RequestParam(value = "last", required = false) Integer last, HttpServletRequest request) {

		Date from = new Date();
		if (fromString != null) {
			try {
				from.setTime(Long.parseLong(fromString));
			} catch (NumberFormatException e) {
				try {
					from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX").parse(fromString);
				} catch (ParseException | NullPointerException d) {
					try {
						from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(fromString);
					} catch (ParseException | NullPointerException f) {
						logger.warn("Parsing error date {} username {} kpiId {} sourceid {}", fromString,
								credentialService.getLoggedUsername(lang), kpiId, sourceId);
						from = null;
					}
				}
			}
		} else {
			from = null;
		}

		Date to = new Date();
		if (toString != null) {
			try {
				to.setTime(Long.parseLong(toString));
			} catch (NumberFormatException e) {
				try {
					to = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX").parse(toString);
				} catch (ParseException | NullPointerException d) {
					try {
						to = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(toString);
					} catch (ParseException | NullPointerException f) {
						logger.warn("Parsing error date {} username {} kpiId {} sourceid {}", toString,
								credentialService.getLoggedUsername(lang), kpiId, sourceId);
						to = null;
					}
				}
			}
		} else {
			to = null;
		}

		logger.info(
				"Requested getAllKPIValueOfPublicKPIV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.getAllKPISQLValueOfPublicKPIV1Pageable(kpiId, sourceRequest, sourceId,
						lang, pageNumber, pageSize, sortDirection, sortBy, searchKey, from, to, first, last, request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.getAllKPIElasticValueOfPublicKPIV1Pageable(kpiId, sourceRequest,
						sourceId, lang, pageNumber, pageSize, sortDirection, sortBy, searchKey, from, to, first, last,
						request);
			}

			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------COPY ALL KPI Value Pageable To Another DB Type
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/values/copy")
	public ResponseEntity<Object> copyAllKPIValueV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "sourceDB", required = true) String sourceDB,
			@RequestParam(value = "destinationDB", required = true) String destinationDB,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false) Integer first,
			@RequestParam(value = "last", required = false) Integer last, HttpServletRequest request) {

		logger.info("Requested copyAllKPIValueV1 sourceDB {} destinationDB {} kpiId {}", sourceDB, destinationDB,
				kpiId);

		try {

			if (sourceDB.equals(destinationDB)) {
				throw new IllegalArgumentException();
			}

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !credentialService.isRoot(lang)) {
				throw new CredentialsException();
			}

			List<? extends Object> elementToCopyList;
			if (sourceDB.equals(MYSQL)) {
				elementToCopyList = kpiValueService.findByKpiIdNoPagesWithLimit(kpiId, from, to, first, last, lang);
			} else if (sourceDB.equals(ELASTICSEARCH)) {
				elementToCopyList = kpiValueService.findBySensorIdNoPagesWithLimit(kpiId, from, to, first, last, lang);
			} else {
				throw new IllegalArgumentException();
			}

			if (destinationDB.equals(MYSQL)) {
				if (!elementToCopyList.isEmpty()) {
					ObjectMapper objectMapper = new ObjectMapper();
					KPIValueDTO[] dtoList = objectMapper.convertValue(elementToCopyList, KPIValueDTO[].class);
					return kpiSQLValueController.postKPISQLValueArrayV1(kpiId, Arrays.asList(dtoList), sourceRequest,
							sourceId, lang, request);
				}
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (destinationDB.equals(ELASTICSEARCH)) {
				ObjectMapper objectMapper = new ObjectMapper();
				KPIElasticValueDTO[] dtoList = objectMapper.convertValue(elementToCopyList, KPIElasticValueDTO[].class);
				return kpiElasticValueController.postKPIElasticValueArrayV1(kpiId, Arrays.asList(dtoList),
						sourceRequest, sourceId, lang, request);
			} else {
				throw new IllegalArgumentException();
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException | DataNotValidException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Dates of PUBLIC KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/values/dates")
	public ResponseEntity<Object> getDistinctKPIValuesDateOfPublicKPIV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "checkCoordinates", required = false, defaultValue = "true") boolean checkCoordinates,
			HttpServletRequest request) {

		logger.info("Requested getDistinctKPIValuesDateV1 kpiId {}", kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}
			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.getDistinctKPISQLValuesDateOfPublicKPIV1(kpiId, sourceRequest, sourceId,
						lang, checkCoordinates, request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.getDistinctKPIElasticValuesDateOfPublicKPIV1(kpiId, sourceRequest,
						sourceId, lang, checkCoordinates, request);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Dates Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/values/dates")
	public ResponseEntity<Object> getDistinctKPIValuesDateV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "checkCoordinates", required = false, defaultValue = "true") boolean checkCoordinates,
			HttpServletRequest request) {

		logger.info("Requested getDistinctKPIValuesDateV1 kpiId {}", kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
						request.getRequestURI() + "?" + request.getQueryString(), "Wrong KPI Data", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}
			if (kpiData.getDbValuesType() == null || kpiData.getDbValuesType().equals("")
					|| kpiData.getDbValuesType().contains(MYSQL)) {
				return kpiSQLValueController.getDistinctKPISQLValuesDateV1(kpiId, sourceRequest, sourceId, lang,
						checkCoordinates, request);
			} else if (kpiData.getDbValuesType().contains(ELASTICSEARCH)) {
				return kpiElasticValueController.getDistinctKPIElasticValuesDateV1(kpiId, sourceRequest, sourceId, lang,
						checkCoordinates, request);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

}