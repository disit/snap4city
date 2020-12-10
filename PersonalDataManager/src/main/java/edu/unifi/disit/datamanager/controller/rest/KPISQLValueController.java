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

import java.lang.reflect.Field;
import java.util.ArrayList;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.format.annotation.DateTimeFormat;
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
public class KPISQLValueController {

	private static final Logger logger = LogManager.getLogger();
	private static final String NO_DATA_FOUND = "No data found";
	private static final String RIGHTS_EXCEPTION = "Rights exception";
	private static final String WRONG_KPI_DATA = "Wrong KPI Data";
	private static final String WRONG_ARGUMENTS = "Wrong Arguments";

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

	// -------------------GET KPI Value From ID ------------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/sqlvalues/{id}")
	public ResponseEntity<Object> getKPISQLValueV1ById(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPISQLValueV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

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

			KPIValue kpiValue = kpiValueService.getKPIValueById(id, lang);

			if (kpiValue == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), NO_DATA_FOUND, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpivalue {}", kpiValue.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.VALUE);
				return new ResponseEntity<>(kpiValue, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Public KPI Value From ID
	// ------------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/sqlvalues/{id}")
	public ResponseEntity<Object> getPublicKPISQLValueV1ById(@PathVariable("kpiId") Long kpiId,
			@PathVariable("id") Long id, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getPublicKPISQLValueV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			KPIValue kpiValue = kpiValueService.getKPIValueById(id, lang);

			if (kpiValue == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), NO_DATA_FOUND, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpivalue {}", kpiValue.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.VALUE);
				return new ResponseEntity<>(kpiValue, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Value ------------------------------------
	@PostMapping("/api/v1/kpidata/{kpiId}/sqlvalues")
	public ResponseEntity<Object> postKPISQLValueV1(@PathVariable("kpiId") Long kpiId, @RequestBody KPIValueDTO dto,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		KPIValue kpiValue = new KPIValue(dto);

		logger.info("Requested postKPISQLValueV1 id {} sourceRequest {}", kpiValue.getId(), sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);
			kpiValue.setKpiId(kpiId);
			kpiValueService.saveKPIValue(kpiValue);
			logger.info("Posted kpivalue {}", kpiValue.getId());
			
			try {
				if (kpiValue.getDataTime() != null) {
					if ((kpiData.getLastDate() != null
							&& kpiData.getLastDate().getTime() <= kpiValue.getDataTime().getTime())
							|| kpiData.getLastDate() == null) {
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
				logger.warn(WRONG_ARGUMENTS, d);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
						request.getRemoteAddr());

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			return new ResponseEntity<>(kpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Value Array
	// ------------------------------------
	@PostMapping("/api/v1/kpidata/{kpiId}/sqlvalues/list")
	public ResponseEntity<Object> postKPISQLValueArrayV1(@PathVariable("kpiId") Long kpiId,
			@RequestBody List<KPIValueDTO> dtoList, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		List<KPIValue> kpiValueList = dtoList.stream().map(KPIValue::new).collect(Collectors.toList());

		logger.info("Requested postKPISQLValueArrayV1 kpiId {} sourceRequest {}", kpiId, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);

			kpiValueList.sort((e1, e2) -> e1.getDataTime().compareTo(e2.getDataTime()));

			for (int i = 0; i < kpiValueList.size() - 1; i++) {
				kpiValueList.get(i).setKpiId(kpiId);
				kpiValueList.get(i).setInsertTime(new Date());
			}

			List<KPIValue> listInsertedKPIValue = kpiValueService
					.saveKPIValueList(kpiValueList.subList(0, kpiValueList.size() - 1));

			listInsertedKPIValue.add((KPIValue) postKPISQLValueV1(kpiId, dtoList.get(dtoList.size() - 1), sourceRequest,
					sourceId, lang, request).getBody());

			return new ResponseEntity<>(listInsertedKPIValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PUT New KPI Value ------------------------------------
	@PutMapping("/api/v1/kpidata/{kpiId}/sqlvalues/{id}")
	public ResponseEntity<Object> putKPISQLValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody KPIValueDTO dto, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		KPIValue kpiValue = new KPIValue(dto);

		logger.info("Requested putKPISQLValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIValue oldKpiValue = kpiValueService.getKPIValueById(id, lang);
			if (oldKpiValue == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), NO_DATA_FOUND, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			try {
				if (kpiValue.getDataTime() != null) {
					if ((kpiData.getLastDate() != null
							&& kpiData.getLastDate().getTime() <= kpiValue.getDataTime().getTime())
							|| kpiData.getLastDate() == null) {
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
				logger.warn(WRONG_ARGUMENTS, d);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
						request.getRemoteAddr());

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			kpiValue.setId(oldKpiValue.getId());
			KPIValue newKpiValue = kpiValueService.saveKPIValue(kpiValue);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);
			logger.info("Putted kpivalue {}", kpiValue.getId());
			return new ResponseEntity<>(newKpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PATCH New KPI Value ------------------------------------
	@PatchMapping("/api/v1/kpidata/{kpiId}/sqlvalues/{id}")
	public ResponseEntity<Object> patchKPISQLValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody Map<String, Object> fields, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested patchKPISQLValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIValue oldKpiValue = kpiValueService.getKPIValueById(id, lang);
			if (oldKpiValue == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), NO_DATA_FOUND, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			// Problem with cast of int to long, but the id is also present and it must be
			// the same
			fields.remove("id");
			fields.remove("kpiId");
			if (fields.get("dataTime") != null) {
				Date date = new Date();
				date.setTime((Long) fields.get("dataTime"));
				fields.put("dataTime", date);
			}
			// Map key is field name, v is value
			fields.forEach((k, v) -> {
				// use reflection to get field k on manager and set it to value k
				Field field = ReflectionUtils.findField(KPIValue.class, k);

				if (field != null && v != null) {
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, oldKpiValue, (field.getType()).cast(v));
				}
			});

			try {
				if (oldKpiValue.getDataTime() != null) {
					if ((kpiData.getLastDate() != null
							&& kpiData.getLastDate().getTime() <= oldKpiValue.getDataTime().getTime())
							|| kpiData.getLastDate() == null) {
						kpiData.setLastDate(oldKpiValue.getDataTime());
						kpiData.setLastValue(oldKpiValue.getValue());
						if (oldKpiValue.getLatitude() != null && !oldKpiValue.getLatitude().equals("")) {
							kpiData.setLastLatitude(oldKpiValue.getLatitude());
						}
						if (oldKpiValue.getLongitude() != null && !oldKpiValue.getLongitude().equals("")) {
							kpiData.setLastLongitude(oldKpiValue.getLongitude());
						}
						kpiDataService.saveKPIData(kpiData);
					}
				} else {
					throw new DataNotValidException("Date format is wrong, please check or send the date as timestamp");
				}
			} catch (DataNotValidException d) {
				logger.warn(WRONG_ARGUMENTS, d);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
						request.getRemoteAddr());

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			KPIValue newKpiValue = kpiValueService.saveKPIValue(oldKpiValue);
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, newKpiValue.getKpiId(), ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);

			logger.info("Patched kpivalue {}", newKpiValue.getId());
			return new ResponseEntity<>(newKpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE New KPI Value ------------------------------------
	@DeleteMapping("/api/v1/kpidata/{kpiId}/sqlvalues/{id}")
	public ResponseEntity<Object> deleteKPISQLValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested deleteKPISQLValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIValue kpiValueToDelete = kpiValueService.getKPIValueById(id, lang);
			if (kpiValueToDelete == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), NO_DATA_FOUND, null,
						request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			try {
				List<KPIValue> lastKpiValue = kpiValueService.findByKpiIdNoPagesWithLimit(kpiId, null, null, null, 2,
						lang);
				if (!lastKpiValue.isEmpty() && lastKpiValue.size() == 2 && lastKpiValue.get(0).getId().equals(id)) {
					kpiData.setLastDate(lastKpiValue.get(1).getDataTime());
					kpiData.setLastValue(lastKpiValue.get(1).getValue());
					kpiDataService.saveKPIData(kpiData);
				} else if (!lastKpiValue.isEmpty() && lastKpiValue.size() == 1
						&& lastKpiValue.get(0).getId().equals(id)) {
					kpiData.setLastDate(null);
					kpiData.setLastValue(null);
					kpiData.setLastLatitude(null);
					kpiData.setLastLongitude(null);
					kpiDataService.saveKPIData(kpiData);
				}
			} catch (NoSuchMessageException | DataNotValidException e) {
				logger.warn("Problem setting last longitude, latitude", e);
			}

			kpiValueToDelete.setDeleteTime(new Date());
			KPIValue newKpiValue = kpiValueService.saveKPIValue(kpiValueToDelete);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE);
			logger.info("Deleted kpivalue {}", id);
			return new ResponseEntity<>(newKpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE All KPI Value ------------------------------------
	@DeleteMapping("/api/v1/kpidata/{kpiId}/sqlvalues")
	public ResponseEntity<Object> deleteKAllPISQLValuesV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested deleteKAllPISQLValuesV1 kpiId {} sourceRequest {}", kpiId, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			List<KPIValue> kpiValueList = kpiValueService.findByKpiIdNoPages(kpiId);

			kpiValueList.stream().forEach(e -> e.setDeleteTime(new Date()));

			kpiData.setLastDate(null);
			kpiData.setLastValue(null);
			kpiData.setLastLatitude(null);
			kpiData.setLastLongitude(null);
			kpiDataService.saveKPIData(kpiData);

			kpiValueList = kpiValueService.saveKPIValueList(kpiValueList);

			logger.info("Deleted all kpiValues of kpiId {}", kpiId);
			return new ResponseEntity<>(kpiValueList, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/sqlvalues")
	public ResponseEntity<Object> getAllKPISQLValueV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false) Integer first,
			@RequestParam(value = "last", required = false) Integer last, HttpServletRequest request) {

		logger.info(
				"Requested getAllKPISQLValueV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

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

			Page<KPIValue> pageKpiValue = null;
			List<KPIValue> listKpiValue = null;
			if (pageNumber != -1) {
				if (searchKey == null || searchKey.equals("")) {
					pageKpiValue = kpiValueService.findByKpiId(kpiId,
							PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiValue = kpiValueService.findByKpiIdFiltered(kpiId, searchKey,
							PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (searchKey == null || searchKey.equals("")) {
					if (from == null && last == null && to == null && first == null) {
						listKpiValue = kpiValueService.findByKpiIdNoPages(kpiId);
					} else if (last != null && last == 1 && from == null && to == null && first == null && kpiData.getLastValue() != null) {
						listKpiValue = new ArrayList<>();
						listKpiValue.add(new KPIValue(null, kpiData.getLastDate(), null, null, kpiData.getLastValue(),
								kpiData.getLastLatitude(), kpiData.getLastLongitude(), kpiData.getId()));
					} else {
						listKpiValue = kpiValueService.findByKpiIdNoPagesWithLimit(kpiId, from, to, first, last, lang);
					}
				} else {
					listKpiValue = kpiValueService.findByKpiIdFilteredNoPages(kpiId, searchKey);
				}
			}

			if (pageKpiValue == null && listKpiValue == null) {
				logger.info("No value data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "No value data found", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiValue != null) {
				logger.info("Returning KpiValuepage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<>(pageKpiValue, HttpStatus.OK);
			} else {
				logger.info("Returning KpiValuelist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<>(listKpiValue, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException | DataNotValidException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value of PUBLIC KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/sqlvalues")
	public ResponseEntity<Object> getAllKPISQLValueOfPublicKPIV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false) Integer first,
			@RequestParam(value = "last", required = false) Integer last, HttpServletRequest request) {

		logger.info(
				"Requested getAllKPISQLValueOfPublicKPIV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			Page<KPIValue> pageKpiValue = null;
			List<KPIValue> listKpiValue = null;
			if (pageNumber != -1) {
				if (searchKey.equals("")) {
					pageKpiValue = kpiValueService.findByKpiId(kpiId,
							PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiValue = kpiValueService.findByKpiIdFiltered(kpiId, searchKey,
							PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (searchKey.equals("")) {
					if (from == null && last == null && to == null && first == null) {
						listKpiValue = kpiValueService.findByKpiIdNoPages(kpiId);
					} else {
						listKpiValue = kpiValueService.findByKpiIdNoPagesWithLimit(kpiId, from, to, first, last, lang);
					}
				} else {
					listKpiValue = kpiValueService.findByKpiIdFilteredNoPages(kpiId, searchKey);
				}
			}

			if (pageKpiValue == null && listKpiValue == null) {
				logger.info("No value data found");

				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						request.getRequestURI() + "?" + request.getQueryString(), "No value data found", null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiValue != null) {
				logger.info("Returning KpiValuepage ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, sourceId, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<>(pageKpiValue, HttpStatus.OK);
			} else {
				logger.info("Returning KpiValuelist ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, sourceId, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<>(listKpiValue, HttpStatus.OK);
			}

		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException | DataNotValidException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Dates of PUBLIC KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/sqlvalues/dates")
	public ResponseEntity<Object> getDistinctKPISQLValuesDateOfPublicKPIV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "checkCoordinates", required = false, defaultValue = "true") boolean checkCoordinates,
			HttpServletRequest request) {

		logger.info("Requested getDistinctKPISQLValuesDateOfPublicKPIV1 kpiId {}", kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}
			List<Date> listKpiValueDate = null;
			if (checkCoordinates) {
				listKpiValueDate = kpiValueService.getKPIValueDates(kpiId);
			} else {
				listKpiValueDate = kpiValueService.getKPIValueDatesCoordinatesOptionallyNull(kpiId);
			}

			if (listKpiValueDate != null) {
				logger.info("Returning KpiValuesDatesList ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, sourceId, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES);

				return new ResponseEntity<>(listKpiValueDate, HttpStatus.OK);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Dates Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/sqlvalues/dates")
	public ResponseEntity<Object> getDistinctKPISQLValuesDateV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "checkCoordinates", required = false, defaultValue = "true") boolean checkCoordinates,
			HttpServletRequest request) {

		logger.info("Requested getDistinctKPISQLValuesDateV1 kpiId {}", kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
						request.getRequestURI() + "?" + request.getQueryString(), WRONG_KPI_DATA, null,
						request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}
			List<Date> listKpiValueDate = null;
			if (checkCoordinates) {
				listKpiValueDate = kpiValueService.getKPIValueDates(kpiId);
			} else {
				listKpiValueDate = kpiValueService.getKPIValueDatesCoordinatesOptionallyNull(kpiId);
			}

			if (listKpiValueDate != null) {
				logger.info("Returning KpiValuesDatesList ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES);

				return new ResponseEntity<>(listKpiValueDate, HttpStatus.OK);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					request.getRequestURI() + "?" + request.getQueryString(), d.getMessage(), d,
					request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

}