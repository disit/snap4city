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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIMetadata;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIValue;
import edu.unifi.disit.datamanager.datamodel.profiledb.POIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;
import edu.unifi.disit.datamanager.service.IKPIDataService;
import edu.unifi.disit.datamanager.service.IKPIMetadataService;
import edu.unifi.disit.datamanager.service.IKPIValueService;

@RestController
public class POIDataController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IKPIDataService kpiDataService;

	@Autowired
	IKPIValueService kpiValueService;

	@Autowired
	IKPIMetadataService kpiMetadataService;

	@Autowired
	IKPIActivityService kpiActivityService;

	@Autowired
	ICredentialsService credentialService;

	@Autowired
	IAccessService accessService;

	// -------------------GET KPI Data From ID as GEOJSON -------------
	@GetMapping("/api/v1/poidata/{id}")
	public ResponseEntity<Object> getPOIDataV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getPOIDataV1ById id {} lang {}", id, lang);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(id, lang);

			if (kpiData == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, id, ActivityAccessType.READ, KPIActivityDomainType.POI,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				if (!kpiData.getUsername().toLowerCase().equals(credentialService.getLoggedUsername(lang).toLowerCase())
						&& !accessService.checkAccessFromApp(Long.toString(kpiData.getId()), lang).getResult()) {
					throw new CredentialsException();
				}

				List<KPIValue> listKPIValue = null;
				if (from == null && to == null) {
					listKPIValue = kpiValueService.findByKpiIdNoPages(id);
				} else {
					listKPIValue = kpiValueService.findByKpiIdNoPagesWithLimit(id, from, to, 0, 0, lang);
				}

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						id, ActivityAccessType.READ, KPIActivityDomainType.POIVALUE);

				List<KPIMetadata> listKPIMetadata = kpiMetadataService.findByKpiIdNoPages(id);

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						id, ActivityAccessType.READ, KPIActivityDomainType.POIMETADATA);

				logger.info("Returning kpiData as POIData {}", kpiData.getId());
				POIData poiData = new POIData(kpiData, listKPIValue, listKPIMetadata);

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						id, ActivityAccessType.READ, KPIActivityDomainType.POI);
				return new ResponseEntity<Object>(poiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (DataNotValidException d) {
			logger.warn("Wrong Arguments", d);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------DEPRECATED POST New POI Data
	// ------------------------------------
	@Deprecated
	@PostMapping("/api/v1/poidata/save")
	public ResponseEntity<Object> savePOIDataV1(@RequestBody POIData poiData,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested DEPRECATED savePOIDataV1 id {}sourceRequest {}", poiData.getKpidata().getId(),
				sourceRequest);
		try {
			KPIData newKpiData = kpiDataService.saveKPIData(poiData.getKpidata());

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					newKpiData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.DATA);

			logger.info("Posted kpiData {}", newKpiData.getId());

			for (KPIMetadata kpiMetadata : poiData.getListKPIMetadata()) {
				kpiMetadata.setKpiId(newKpiData.getId());
				kpiMetadataService.saveKPIMetadata(kpiMetadata);

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						newKpiData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.METADATA);
			}
			return new ResponseEntity<Object>(poiData, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New POI Data ------------------------------------
	@PostMapping("/api/v1/poidata")
	public ResponseEntity<Object> postPOIDataV1(@RequestBody POIData poiData,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested postKPIDataV1 id {} sourceRequest {}", poiData.getKpidata().getId(), sourceRequest);

		try {
			KPIData newKpiData = kpiDataService.saveKPIData(poiData.getKpidata());

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					newKpiData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.DATA);

			logger.info("Posted kpiData {}", newKpiData.getId());

			if (newKpiData.getOwnership().equals("public")) {
				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						newKpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.CHANGEOWNERSHIP);

				kpiDataService.makeKPIDataPublic(newKpiData.getUsername(), newKpiData.getId(),
						newKpiData.getHighLevelType(), lang);
			}

			for (KPIMetadata kpiMetadata : poiData.getListKPIMetadata()) {
				kpiMetadata.setKpiId(newKpiData.getId());
				kpiMetadataService.saveKPIMetadata(kpiMetadata);

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						newKpiData.getId(), ActivityAccessType.WRITE, KPIActivityDomainType.METADATA);
			}

			return new ResponseEntity<Object>(newKpiData, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (DelegationNotValidException d) {
			logger.warn("Problem with public or private ownership", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.WRITE, KPIActivityDomainType.DATA,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					"Problem with public or private ownership", d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET PUBLIC POI Data Pageable ----------
	@GetMapping("/api/v1/poidata/public")
	public ResponseEntity<Object> getPublicPOIDataV1Pageable(
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info("Requested getPublicPOIDataV1Pageable highLevelType{} searchKey {}", highLevelType, searchKey);

		try {

			List<KPIData> listKpiData = null;

			listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages("ANONYMOUS", "My",
					highLevelType, searchKey);

			if (listKpiData == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.POI,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {

				List<POIData> listPoiData = new ArrayList<POIData>();

				for (KPIData kpiData : listKpiData) {
					if (kpiData.getLatitude() == null || kpiData.getLatitude().equals("")
							|| kpiData.getLongitude() == null || kpiData.getLongitude().equals("")) {

						if (kpiData.getLastLatitude() != null && !kpiData.getLastLatitude().equals("")
								&& kpiData.getLastLongitude() != null && !kpiData.getLastLongitude().equals("")) {
							kpiData = kpiDataService.detachEntity(kpiData);
							kpiData.setLatitude(kpiData.getLastLatitude());
							kpiData.setLongitude(kpiData.getLastLongitude());
						} else {

							List<KPIValue> kpiValues = kpiValueService.findByKpiIdGeoLocated(kpiData.getId());

							kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang),
									sourceRequest, kpiData.getId(), ActivityAccessType.READ,
									KPIActivityDomainType.POIVALUE);

							if (!kpiValues.isEmpty()) {
								kpiData = kpiDataService.detachEntity(kpiData);
								kpiData.setLatitude(kpiValues.get(0).getLatitude());
								kpiData.setLongitude(kpiValues.get(0).getLongitude());
							}
						}
					}
					if (kpiData.getLatitude() != null && kpiData.getLatitude() != "" && kpiData.getLongitude() != null
							&& kpiData.getLongitude() != "") {
						listPoiData.add(new POIData(kpiData));
					}

				}

				logger.info("Returning kpidatalist as GeoJson");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						null, ActivityAccessType.READ, KPIActivityDomainType.POI);

				return new ResponseEntity<Object>(listPoiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.POI,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.POI,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET DELEGATED POI Data Pageable ----------
	@GetMapping("/api/v1/poidata/delegated")
	public ResponseEntity<Object> getDelegatedPOIDataV1Pageable(
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info("Requested getDelegatedPOIDataV1Pageable highLevelType{} searchKey {}", highLevelType, searchKey);

		try {

			List<KPIData> listKpiData = null;

			listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages(
					credentialService.getLoggedUsername(lang), "My", highLevelType, searchKey);

			if (listKpiData == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.POI,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {

				List<POIData> listPoiData = new ArrayList<POIData>();

				for (KPIData kpiData : listKpiData) {
					if (kpiData.getLatitude() == null || kpiData.getLatitude().equals("")
							|| kpiData.getLongitude() == null || kpiData.getLongitude().equals("")) {

						if (kpiData.getLastLatitude() != null && !kpiData.getLastLatitude().equals("")
								&& kpiData.getLastLongitude() != null && !kpiData.getLastLongitude().equals("")) {
							kpiData = kpiDataService.detachEntity(kpiData);
							kpiData.setLatitude(kpiData.getLastLatitude());
							kpiData.setLongitude(kpiData.getLastLongitude());
						} else {

							List<KPIValue> kpiValues = kpiValueService.findByKpiIdGeoLocated(kpiData.getId());

							kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang),
									sourceRequest, kpiData.getId(), ActivityAccessType.READ,
									KPIActivityDomainType.POIVALUE);

							if (!kpiValues.isEmpty()) {
								kpiData = kpiDataService.detachEntity(kpiData);
								kpiData.setLatitude(kpiValues.get(0).getLatitude());
								kpiData.setLongitude(kpiValues.get(0).getLongitude());
							}
						}
					}
					if (kpiData.getLatitude() != null && kpiData.getLatitude() != "" && kpiData.getLongitude() != null
							&& kpiData.getLongitude() != "") {
						listPoiData.add(new POIData(kpiData));
					}

				}

				logger.info("Returning kpidatalist as GeoJson");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						null, ActivityAccessType.READ, KPIActivityDomainType.POI);

				return new ResponseEntity<Object>(listPoiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.POI,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.POI,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET Own POI Data Pageable ----------
	@GetMapping("/api/v1/poidata")
	public ResponseEntity<Object> getOwnPOIDataV1(@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info("Requested getAllKPIDataV1Pageable highLevelType{} searchKey {}", highLevelType, searchKey);

		try {

			List<KPIData> listKpiData = null;

			if (credentialService.isRoot(lang)) {
				if (searchKey.equals("") && highLevelType.equals("")) {
					listKpiData = kpiDataService.findAllNoPages();
				} else if (!searchKey.equals("") && !highLevelType.equals("")) {
					listKpiData = kpiDataService.findByHighLevelTypeFilteredNoPages(highLevelType, searchKey);
				} else if (highLevelType.equals("")) {
					listKpiData = kpiDataService.findAllFilteredNoPages(searchKey);
				} else if (searchKey.equals("")) {
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
				} else if (searchKey.equals("")) {
					listKpiData = kpiDataService.findByUsernameByHighLevelTypeNoPages(
							credentialService.getLoggedUsername(lang), highLevelType);
				}
			}

			if (listKpiData == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.POI,
						((HttpServletRequest) request).getRequestURI() + "?"
								+ ((HttpServletRequest) request).getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {

				List<POIData> listPoiData = new ArrayList<POIData>();

				for (KPIData kpiData : listKpiData) {
					if (kpiData.getLatitude() == null || kpiData.getLatitude().equals("")
							|| kpiData.getLongitude() == null || kpiData.getLongitude().equals("")) {

						if (kpiData.getLastLatitude() != null && !kpiData.getLastLatitude().equals("")
								&& kpiData.getLastLongitude() != null && !kpiData.getLastLongitude().equals("")) {
							kpiData = kpiDataService.detachEntity(kpiData);
							kpiData.setLatitude(kpiData.getLastLatitude());
							kpiData.setLongitude(kpiData.getLastLongitude());
						} else {

							List<KPIValue> kpiValues = kpiValueService.findByKpiIdGeoLocated(kpiData.getId());

							kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang),
									sourceRequest, kpiData.getId(), ActivityAccessType.READ,
									KPIActivityDomainType.POIVALUE);

							if (!kpiValues.isEmpty()) {
								kpiData = kpiDataService.detachEntity(kpiData);
								kpiData.setLatitude(kpiValues.get(0).getLatitude());
								kpiData.setLongitude(kpiValues.get(0).getLongitude());
							}
						}
					}
					if (kpiData.getLatitude() != null && kpiData.getLatitude() != "" && kpiData.getLongitude() != null
							&& kpiData.getLongitude() != "") {
						listPoiData.add(new POIData(kpiData));
					}

				}

				logger.info("Returning kpidatalist as GeoJson");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						null, ActivityAccessType.READ, KPIActivityDomainType.POI);

				return new ResponseEntity<Object>(listPoiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.POI,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, null, ActivityAccessType.READ, KPIActivityDomainType.POI,
					((HttpServletRequest) request).getRequestURI() + "?"
							+ ((HttpServletRequest) request).getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

}