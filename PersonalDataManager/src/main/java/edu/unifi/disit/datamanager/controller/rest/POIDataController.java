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
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIMetadata;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIValue;
import edu.unifi.disit.datamanager.datamodel.profiledb.POIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
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
	IActivityService activityService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET KPI Data From ID ------------------------------------
	@RequestMapping(value = "/api/v1/poidata/{id}", method = RequestMethod.GET)
	public ResponseEntity<Object> getPOIDataV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIDataV1ById id {} lang {}", id, lang);

		try {
			KPIData kpidata = kpiDataService.getKPIDataById(id, lang);
			List<KPIValue> listKPIValue = kpiValueService.findByKpiIdNoPages(id);
			List<KPIMetadata> listKPIMetadata = kpiMetadataService.findByKpiIdNoPages(id);
			
			if (kpidata == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpidata {}", kpidata.getValueName());
				POIData poiData = new POIData(kpidata, listKPIValue, listKPIMetadata);
				return new ResponseEntity<Object>(poiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
	
	@RequestMapping(value = "/api/v1/poidata/save", method = RequestMethod.POST)
	public ResponseEntity<Object> savePOIDataV1(@RequestBody POIData poiData,
			@RequestParam(value = "sourceRequest") String sourceRequest) {

		logger.info("Requested savePOIDataV1 id {}", poiData.getKpidata().getId());
		try {
			KPIData newKpiData = kpiDataService.saveKPIData(poiData.getKpidata());
			for (KPIMetadata kpiMetadata : poiData.getListKPIMetadata()) {
				kpiMetadata.setKpiId(newKpiData.getId());
				kpiMetadataService.saveKPIMetadata(kpiMetadata);
			}
			return new ResponseEntity<Object>(poiData, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Data Pageable
	// ------------------------------------
	@RequestMapping(value = "/api/v1/poidata/public", method = RequestMethod.GET)
	public ResponseEntity<Object> getAllPOIDataPublicV1(
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {
		

		logger.info("Requested getAllPOIDataPublicV1");

		logger.info("Current user {} root {}", credentialService.getLoggedUsername(lang),
				credentialService.isRoot(lang));

		try {
			
			List<KPIData> listKpiData = null;
			
				listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages("ANONYMOUS", highLevelType, highLevelType, searchKey);
				if (!credentialService.isRoot(lang)) {
					for (KPIData kpidata : listKpiData) {
						kpidata.setUsername("");
					}
				}
			
			// activityService.saveActivityFromUsername(null, datas, sourceRequest, null,
			// null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if (listKpiData == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpidatalist ");
				List<POIData> listPoiData = new ArrayList<POIData>();
				for (KPIData kpidata : listKpiData) {
					List<KPIValue> listKPIValue = kpiValueService.findByKpiIdNoPages(kpidata.getId());
					List<KPIMetadata> listKPIMetadata = kpiMetadataService.findByKpiIdNoPages(kpidata.getId());
					listPoiData.add(new POIData(kpidata, listKPIValue, listKPIMetadata));
				}
				
				return new ResponseEntity<Object>(listPoiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
		
	}
	
	@RequestMapping(value = "/api/v1/poidata/delegated", method = RequestMethod.GET)
	public ResponseEntity<Object> getAllPOIDataDelegatedV1(@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {
		

		logger.info("Requested getAllPOIDataDelegatedV1");

		logger.info("Current user {} root {}", credentialService.getLoggedUsername(lang),
				credentialService.isRoot(lang));
		

		try {
		
			List<KPIData> listKpiData = null;
			
			listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages(credentialService.getLoggedUsername(lang), highLevelType, highLevelType, searchKey);
			
			// activityService.saveActivityFromUsername(null, datas, sourceRequest, null,
			// null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if (listKpiData == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpidatalist ");
				List<POIData> listPoiData = new ArrayList<POIData>();
				for (KPIData kpidata : listKpiData) {
					List<KPIValue> listKPIValue = kpiValueService.findByKpiIdNoPages(kpidata.getId());
					List<KPIMetadata> listKPIMetadata = kpiMetadataService.findByKpiIdNoPages(kpidata.getId());
					listPoiData.add(new POIData(kpidata, listKPIValue, listKPIMetadata));
				}
				
				return new ResponseEntity<Object>(listPoiData, HttpStatus.OK);
			}
			
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
		
	}
	
	@RequestMapping(value = "/api/v1/poidata", method = RequestMethod.GET)
	public ResponseEntity<Object> getAllPOIDataV1(@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {
		

		logger.info("Requested getAllPOIDataV1 searchKey {}",searchKey);

		logger.info("Current user {} root {}", credentialService.getLoggedUsername(lang),
				credentialService.isRoot(lang));
		
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
						listKpiData = kpiDataService.findByUsernameFilteredNoPages(credentialService.getLoggedUsername(lang),
								searchKey);
					} else if (searchKey.equals("")) {
						listKpiData = kpiDataService.findByUsernameByHighLevelTypeNoPages(
								credentialService.getLoggedUsername(lang), highLevelType);
					}
				}
			
			// activityService.saveActivityFromUsername(null, datas, sourceRequest, null,
			// null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if (listKpiData == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpidatalist ");
				List<POIData> listPoiData = new ArrayList<POIData>();
				for (KPIData kpidata : listKpiData) {
					List<KPIValue> listKPIValue = kpiValueService.findByKpiIdNoPages(kpidata.getId());
					List<KPIMetadata> listKPIMetadata = kpiMetadataService.findByKpiIdNoPages(kpidata.getId());
					listPoiData.add(new POIData(kpidata, listKPIValue, listKPIMetadata));
				}
				
				return new ResponseEntity<Object>(listPoiData, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
		
	}

}