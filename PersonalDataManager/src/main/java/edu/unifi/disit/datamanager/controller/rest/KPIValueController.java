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
	@GetMapping("/api/v1/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> getKPIValueV1ById(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIValueV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().toLowerCase()
					.equals(credentialService.getLoggedUsername(lang).toLowerCase())
					&& !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()) {
				throw new CredentialsException();
			}

			KPIValue kpiValue = kpiValueService.getKPIValueById(id, lang);

			if (kpiValue == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpivalue {}", kpiValue.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.VALUE);
				return new ResponseEntity<Object>(kpiValue, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Public KPI Value From ID
	// ------------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> getPublicKPIValueV1ById(@PathVariable("kpiId") Long kpiId,
			@PathVariable("id") Long id, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIValueV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			KPIValue kpiValue = kpiValueService.getKPIValueById(id, lang);

			if (kpiValue == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpivalue {}", kpiValue.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.VALUE);
				return new ResponseEntity<Object>(kpiValue, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DEPRECATED ------------------------------------
	@Deprecated
	@PostMapping("/api/v1/kpivalue/save")
	public ResponseEntity<Object> saveKPIValueV1(@RequestBody KPIValue kpiValue,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested DEPRECATED saveKPIValueV1 id {} sourceRequest {}", kpiValue.getId(), sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiValue.getKpiId(), lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiValue.getKpiId(), ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().toLowerCase()
					.equals(credentialService.getLoggedUsername(lang).toLowerCase())
					&& !accessService.checkAccessFromApp(Long.toString(kpiValue.getKpiId()), lang).getResult()) {
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
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						d.getMessage(), d, request.getRemoteAddr());

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					kpiValue.getKpiId(), ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);

			kpiValueService.saveKPIValue(kpiValue);
			logger.info("Posted kpivalue {}", kpiValue.getId());

			return new ResponseEntity<Object>(kpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiValue.getKpiId(), ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Value ------------------------------------
	@PostMapping("/api/v1/kpidata/{kpiId}/values")
	public ResponseEntity<Object> postKPIValueV1(@PathVariable("kpiId") Long kpiId, @RequestBody KPIValue kpiValue,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested postKPIValueV1 id {} sourceRequest {}", kpiValue.getId(), sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().toLowerCase()
					.equals(credentialService.getLoggedUsername(lang).toLowerCase())
					&& !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()) {
				throw new CredentialsException();
			}

			try {
				if (kpiValue.getDataTime() != null) {
					List<KPIValue> lastKpiValue = kpiValueService.findByKpiIdNoPagesWithLimit(kpiId, null, null, 0, 1,
							lang);
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
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						d.getMessage(), d, request.getRemoteAddr());

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, kpiId,
					ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);
			kpiValue.setKpiId(kpiId);
			kpiValueService.saveKPIValue(kpiValue);
			logger.info("Posted kpivalue {}", kpiValue.getId());

			return new ResponseEntity<Object>(kpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Value Array
	// ------------------------------------
	@PostMapping("/api/v1/kpidata/{kpiId}/values/list")
	public ResponseEntity<Object> postKPIValueArrayV1(@PathVariable("kpiId") Long kpiId,
			@RequestBody List<KPIValue> kpiValueList, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested postKPIValueArrayV1 kpiId {} sourceRequest {}", kpiId, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().toLowerCase()
					.equals(credentialService.getLoggedUsername(lang).toLowerCase())
					&& !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()) {
				throw new CredentialsException();
			}

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, kpiId,
					ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);

			List<KPIValue> listInsertedKPIValue = new ArrayList<>();
			kpiValueList.sort((e1, e2) -> e1.getDataTime().compareTo(e2.getDataTime()));

			for (int i = 0; i < kpiValueList.size() - 1; i++) {
				kpiValueList.get(i).setKpiId(kpiId);
				listInsertedKPIValue.add(kpiValueService.saveKPIValue(kpiValueList.get(i)));
			}

			listInsertedKPIValue.add((KPIValue) postKPIValueV1(kpiId, kpiValueList.get(kpiValueList.size() - 1),
					sourceRequest, lang, request).getBody());

			return new ResponseEntity<Object>(listInsertedKPIValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PUT New KPI Value ------------------------------------
	@PutMapping("/api/v1/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> putKPIValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody KPIValue kpiValue, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested putKPIValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().toLowerCase()
					.equals(credentialService.getLoggedUsername(lang).toLowerCase())
					&& !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()) {
				throw new CredentialsException();
			}

			KPIValue oldKpiValue = kpiValueService.getKPIValueById(id, lang);
			if (oldKpiValue == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			}

			try {
				if (kpiValue.getDataTime() != null) {
					List<KPIValue> lastKpiValue = kpiValueService.findByKpiIdNoPagesWithLimit(kpiId, null, null, 0, 1,
							lang);
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
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						d.getMessage(), d, request.getRemoteAddr());

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			kpiValue.setId(oldKpiValue.getId());
			KPIValue newKpiValue = kpiValueService.saveKPIValue(kpiValue);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, kpiId,
					ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);
			logger.info("Putted kpivalue {}", kpiValue.getId());
			return new ResponseEntity<Object>(newKpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PATCH New KPI Value ------------------------------------
	@PatchMapping("/api/v1/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> patchKPIValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestBody Map<String, Object> fields, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested patchKPIValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().toLowerCase()
					.equals(credentialService.getLoggedUsername(lang).toLowerCase())
					&& !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()) {
				throw new CredentialsException();
			}

			KPIValue oldKpiValue = kpiValueService.getKPIValueById(id, lang);
			if (oldKpiValue == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			}

			// Problem with cast of int to long, but the id is also present and it must be
			// the same
			fields.remove("id");
			fields.remove("kpiId");
			Date date = new Date();
			date.setTime((Long) fields.get("dataTime"));
			fields.put("dataTime", date);
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
					List<KPIValue> lastKpiValue = kpiValueService.findByKpiIdNoPagesWithLimit(kpiId, null, null, 0, 1,
							lang);
					if (lastKpiValue.isEmpty() || (lastKpiValue.get(0) != null
							&& lastKpiValue.get(0).getDataTime() != null
							&& lastKpiValue.get(0).getDataTime().getTime() < oldKpiValue.getDataTime().getTime())) {
						kpiData.setLastDate(oldKpiValue.getDataTime());
						kpiData.setLastValue(oldKpiValue.getValue());
						kpiDataService.saveKPIData(kpiData);
					}
				} else {
					throw new DataNotValidException("Date format is wrong, please check or send the date as timestamp");
				}
			} catch (DataNotValidException d) {
				logger.warn("Wrong Arguments", d);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						d.getMessage(), d, request.getRemoteAddr());

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			KPIValue newKpiValue = kpiValueService.saveKPIValue(oldKpiValue);
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					newKpiValue.getKpiId(), ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);

			logger.info("Patched kpivalue {}", newKpiValue.getId());
			return new ResponseEntity<Object>(newKpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE New KPI Value ------------------------------------
	@DeleteMapping("/api/v1/kpidata/{kpiId}/values/{id}")
	public ResponseEntity<Object> deleteKPIValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested delete KPIValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().toLowerCase()
					.equals(credentialService.getLoggedUsername(lang).toLowerCase())
					&& !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()) {
				throw new CredentialsException();
			}

			KPIValue kpiValueToDelete = kpiValueService.getKPIValueById(id, lang);
			if (kpiValueToDelete == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			}

			try {
				List<KPIValue> lastKpiValue = kpiValueService.findByKpiIdNoPagesWithLimit(kpiId, null, null, 0, 2,
						lang);
				if (!lastKpiValue.isEmpty() && lastKpiValue.size() == 2 && lastKpiValue.get(0).getId() == id) {
					kpiData.setLastDate(lastKpiValue.get(1).getDataTime());
					kpiData.setLastValue(lastKpiValue.get(1).getValue());
					kpiDataService.saveKPIData(kpiData);
				} else if (!lastKpiValue.isEmpty() && lastKpiValue.size() == 1 && lastKpiValue.get(0).getId() == id) {
					kpiData.setLastDate(null);
					kpiData.setLastValue(null);
					kpiData.setLastLatitude(null);
					kpiData.setLastLongitude(null);
					kpiDataService.saveKPIData(kpiData);
				}
			} catch (NoSuchMessageException | DataNotValidException e) {
				e.printStackTrace();
			}

			kpiValueToDelete.setDeleteTime(new Date());
			KPIValue newKpiValue = kpiValueService.saveKPIValue(kpiValueToDelete);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, kpiId,
					ActivityAccessType.DELETE, KPIActivityDomainType.VALUE);
			logger.info("Deleted kpivalue {}", id);
			return new ResponseEntity<Object>(newKpiValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/values")
	public ResponseEntity<Object> getAllKPIValueV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false, defaultValue = "0") Integer first,
			@RequestParam(value = "last", required = false, defaultValue = "0") Integer last,
			HttpServletRequest request) {

		logger.info(
				"Requested getAllKPIValueV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().toLowerCase()
					.equals(credentialService.getLoggedUsername(lang).toLowerCase())
					&& !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()) {
				throw new CredentialsException();
			}

			Page<KPIValue> pageKpiValue = null;
			List<KPIValue> listKpiValue = null;
			if (pageNumber != -1) {
				if (searchKey.equals("")) {
					pageKpiValue = kpiValueService.findByKpiId(kpiId, new PageRequest(pageNumber, pageSize,
							new Sort(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiValue = kpiValueService.findByKpiIdFiltered(kpiId, searchKey, new PageRequest(pageNumber,
							pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (searchKey.equals("")) {
					if (from == null && last == null && to == null) {
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

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No value data found", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (pageKpiValue != null) {
				logger.info("Returning KpiValuepage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<Object>(pageKpiValue, HttpStatus.OK);
			} else if (listKpiValue != null) {
				logger.info("Returning KpiValuelist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<Object>(listKpiValue, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException | DataNotValidException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value of PUBLIC KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/values")
	public ResponseEntity<Object> getAllKPIValueOfPublicKPIV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false, defaultValue = "0") Integer first,
			@RequestParam(value = "last", required = false, defaultValue = "0") Integer last,
			HttpServletRequest request) {

		logger.info(
				"Requested getAllKPIValueV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			Page<KPIValue> pageKpiValue = null;
			List<KPIValue> listKpiValue = null;
			if (pageNumber != -1) {
				if (searchKey.equals("")) {
					pageKpiValue = kpiValueService.findByKpiId(kpiId, new PageRequest(pageNumber, pageSize,
							new Sort(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiValue = kpiValueService.findByKpiIdFiltered(kpiId, searchKey, new PageRequest(pageNumber,
							pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (searchKey.equals("")) {
					if (from == null && last == null && to == null) {
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
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No value data found", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (pageKpiValue != null) {
				logger.info("Returning KpiValuepage ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, kpiId, ActivityAccessType.READ,
						KPIActivityDomainType.VALUE);

				return new ResponseEntity<Object>(pageKpiValue, HttpStatus.OK);
			} else if (listKpiValue != null) {
				logger.info("Returning KpiValuelist ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, kpiId, ActivityAccessType.READ,
						KPIActivityDomainType.VALUE);

				return new ResponseEntity<Object>(listKpiValue, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException | DataNotValidException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Dates of PUBLIC KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/values/dates")
	public ResponseEntity<Object> getDistinctKPIValuesDateOfPublicKPIV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
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
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
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

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, kpiId, ActivityAccessType.READ,
						KPIActivityDomainType.VALUEDATES);

				return new ResponseEntity<Object>(listKpiValueDate, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Dates Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/values/dates")
	public ResponseEntity<Object> getDistinctKPIValuesDateV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
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
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"Wrong KPI Data", null, request.getRemoteAddr());
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().toLowerCase()
					.equals(credentialService.getLoggedUsername(lang).toLowerCase())
					&& !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()) {
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
						kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES);

				return new ResponseEntity<Object>(listKpiValueDate, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

}