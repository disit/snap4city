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

import java.util.List;
import java.util.Locale;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IDelegationService;
import edu.unifi.disit.datamanager.service.IKPIDataService;

@RestController
public class KPIDataController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IKPIDataService kpiDataService;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IActivityService activityService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET KPI Data From ID ------------------------------------
	@RequestMapping(value = "/api/v1/kpidata/{id}", method = RequestMethod.GET)
	public ResponseEntity<Object> getKPIDataV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIDataV1ById id {} lang {}", id, lang);

		try {
			KPIData kpidata = kpiDataService.getKPIDataById(id, lang);
			if (kpidata == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpidata {}", kpidata.getValueName());
				return new ResponseEntity<Object>(kpidata, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	@RequestMapping(value = "/api/v1/kpidata/save", method = RequestMethod.POST)
	public ResponseEntity<Object> saveKPIDataV1(@RequestBody KPIData kpidata,
			@RequestParam(value = "sourceRequest") String sourceRequest) {

		logger.info("Requested saveKPIDataV1 id {}", kpidata.getId());
		try {
			kpiDataService.saveKPIData(kpidata);
			return new ResponseEntity<Object>(kpidata, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Data Pageable
	// ------------------------------------
	@RequestMapping(value = "/api/v1/kpidata/public", method = RequestMethod.GET)
	public ResponseEntity<Object> getAllKPIDataPublicV1Pageable(
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "asc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info("Requested getAllKPIDataPublicV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {}",
				pageNumber, pageSize, sortDirection, sortBy);

		logger.info("Current user {} root {}", credentialService.getLoggedUsername(lang),
				credentialService.isRoot(lang));

		try {

			Page<KPIData> pageKpiData = null;
			List<KPIData> listKpiData = null;

			if (pageNumber != -1) {
				if (credentialService.isRoot(lang)) {
					pageKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFiltered("ANONYMOUS", "My",
							highLevelType, searchKey, new PageRequest(pageNumber, pageSize,
									new Sort(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(
							"ANONYMOUS", "My", highLevelType, searchKey, new PageRequest(pageNumber, pageSize,
									new Sort(Direction.fromString(sortDirection), sortBy)));
					if (!credentialService.isRoot(lang)) {
						for (KPIData kpidata : pageKpiData.getContent()) {
							kpidata.setUsername("");
						}
					}
				}
			} else {
				if (credentialService.isRoot(lang)) {
					listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages("ANONYMOUS",
							"My", highLevelType, searchKey);
				} else {
					listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages("ANONYMOUS",
							"My", highLevelType, searchKey);
					if (!credentialService.isRoot(lang)) {
						for (KPIData kpidata : listKpiData) {
							kpidata.setUsername("");
						}
					}
				}
			}
			// activityService.saveActivityFromUsername(null, datas, sourceRequest, null,
			// null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpidatapage ");
				return new ResponseEntity<Object>(pageKpiData, HttpStatus.OK);
			} else if (listKpiData != null) {
				logger.info("Returning kpidatalist ");
				return new ResponseEntity<Object>(listKpiData, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

	@RequestMapping(value = "/api/v1/kpidata/delegated", method = RequestMethod.GET)
	public ResponseEntity<Object> getAllKPIDataDelegatedV1Pageable(
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "asc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info("Requested getAllKPIDataDelegatedV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {}",
				pageNumber, pageSize, sortDirection, sortBy);

		logger.info("Current user {} root {}", credentialService.getLoggedUsername(lang),
				credentialService.isRoot(lang));

		try {

			Page<KPIData> pageKpiData = null;
			List<KPIData> listKpiData = null;

			if (pageNumber != -1) {
				pageKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFiltered(
						credentialService.getLoggedUsername(lang), "My", highLevelType, searchKey,
						new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
			} else {
				listKpiData = kpiDataService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages(
						credentialService.getLoggedUsername(lang), "My", highLevelType, searchKey);
			}
			// activityService.saveActivityFromUsername(null, datas, sourceRequest, null,
			// null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpidatapage ");
				return new ResponseEntity<Object>(pageKpiData, HttpStatus.OK);
			} else if (listKpiData != null) {
				logger.info("Returning kpidatalist ");
				return new ResponseEntity<Object>(listKpiData, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

	@RequestMapping(value = "/api/v1/kpidata", method = RequestMethod.GET)
	public ResponseEntity<Object> getAllKPIDataV1Pageable(@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "asc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info("Requested getAllKPIDataV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {}",
				pageNumber, pageSize, sortDirection, sortBy);

		logger.info("Current user {} root {}", credentialService.getLoggedUsername(lang),
				credentialService.isRoot(lang));

		try {

			Page<KPIData> pageKpiData = null;
			List<KPIData> listKpiData = null;
			if (pageNumber != -1) {
				if (credentialService.isRoot(lang)) {
					if (searchKey.equals("") && highLevelType.equals("")) {
						pageKpiData = kpiDataService.findAll(new PageRequest(pageNumber, pageSize,
								new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						pageKpiData = kpiDataService.findByHighLevelTypeFiltered(highLevelType, searchKey,
								new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (highLevelType.equals("")) {
						pageKpiData = kpiDataService.findAllFiltered(searchKey, new PageRequest(pageNumber, pageSize,
								new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (searchKey.equals("")) {
						pageKpiData = kpiDataService.findByHighLevelType(highLevelType, new PageRequest(pageNumber,
								pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
					}
				} else {
					if (searchKey.equals("") && highLevelType.equals("")) {
						pageKpiData = kpiDataService.findByUsername(credentialService.getLoggedUsername(lang),
								new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (!searchKey.equals("") && !highLevelType.equals("")) {
						pageKpiData = kpiDataService.findByUsernameByHighLevelTypeFiltered(
								credentialService.getLoggedUsername(lang), highLevelType, searchKey, new PageRequest(
										pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (highLevelType.equals("")) {
						pageKpiData = kpiDataService.findByUsernameFiltered(credentialService.getLoggedUsername(lang),
								searchKey, new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)));
					} else if (searchKey.equals("")) {
						pageKpiData = kpiDataService.findByUsernameByHighLevelType(
								credentialService.getLoggedUsername(lang), highLevelType, new PageRequest(pageNumber,
										pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
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
			}
			// activityService.saveActivityFromUsername(null, datas, sourceRequest, null,
			// null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if (pageKpiData == null && listKpiData == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (pageKpiData != null) {
				logger.info("Returning kpidatapage ");
				return new ResponseEntity<Object>(pageKpiData, HttpStatus.OK);
			} else if (listKpiData != null) {
				logger.info("Returning kpidatalist ");
				return new ResponseEntity<Object>(listKpiData, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}

}