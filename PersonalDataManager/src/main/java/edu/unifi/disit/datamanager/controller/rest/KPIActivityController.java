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

import java.util.Date;
import java.util.List;
import java.util.Locale;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivity;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;
import edu.unifi.disit.datamanager.service.IKPIDataService;

@RestController
public class KPIActivityController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IKPIDataService kpiDataService;

	@Autowired
	IAccessService accessService;

	@Autowired
	IKPIActivityService kpiActivityService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET KPI Activity From ID
	// ------------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/activities/{id}")
	public ResponseEntity<Object> getKPIActivityV1ById(@PathVariable("kpiId") Long kpiId, @PathVariable("id") Long id,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIActivityV1ById id {} lang {}", id, lang);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIActivity kpiActivity = kpiActivityService.getKPIActivityById(id, lang);

			if (kpiActivity == null) {
				logger.info("No data found");

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpiactivity {}", kpiActivity.getId());

				return new ResponseEntity<>(kpiActivity, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Public KPI Value From ID
	// ------------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/activities/{id}")
	public ResponseEntity<Object> getPublicKPIActivityV1ById(@PathVariable("kpiId") Long kpiId,
			@PathVariable("id") Long id,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getPublicKPIActivityV1ById id {} lang {}", id, lang);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			KPIActivity kpiActivity = kpiActivityService.getKPIActivityById(id, lang);

			if (kpiActivity == null) {
				logger.info("No data found");

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpiActivity {}", kpiActivity.getId());

				return new ResponseEntity<>(kpiActivity, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Activity Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/activities")
	public ResponseEntity<Object> getAllKPIActivityV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false, defaultValue = "0") Integer first,
			@RequestParam(value = "last", required = false, defaultValue = "0") Integer last,
			@RequestParam(value = "sourceRequestFilter", required = false, defaultValue = "") String sourceRequestFilter,
			@RequestParam(value = "accessTypeFilter", required = false, defaultValue = "") String accessType, // READ, WRITE,
			// DELETE
			HttpServletRequest request) {

		logger.info(
				"Requested getAllKPIActivityV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} kpiId {} source {} accessType {}",
				pageNumber, pageSize, sortDirection, sortBy, kpiId, sourceRequestFilter, accessType);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			Page<KPIActivity> pageKpiActivity = null;
			List<KPIActivity> listKpiActivity = null;
			if (pageNumber != -1) {

				if (sourceRequestFilter.equals("") && accessType.equals("")) {
					pageKpiActivity = kpiActivityService.findByKpiId(kpiId, new PageRequest(pageNumber, pageSize,
							new Sort(Direction.fromString(sortDirection), sortBy)));
				} else if (!sourceRequestFilter.equals("") && !accessType.equals("")) {
					pageKpiActivity = kpiActivityService.findByKpiIdByAccessTypeBySourceRequest(kpiId, accessType,
							sourceRequestFilter, new PageRequest(pageNumber, pageSize,
									new Sort(Direction.fromString(sortDirection), sortBy)));
				} else if (accessType.equals("")) {
					pageKpiActivity = kpiActivityService.findByKpiIdBySourceRequest(kpiId, sourceRequestFilter,
							new PageRequest(pageNumber, pageSize,
									new Sort(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiActivity = kpiActivityService.findByKpiIdByAccessType(kpiId, accessType, new PageRequest(
							pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
				}

			} else {
				if (sourceRequestFilter.equals("") && accessType.equals("")) {
					if (from == null && last == null && to == null && first == null) {
						listKpiActivity = kpiActivityService.findByKpiIdNoPages(kpiId);
					} else {
						listKpiActivity = kpiActivityService.findByKpiIdByAccessTypeBySourceRequestNoPagesWithLimit(
								kpiId, from, to, first, last, lang, null, null);
					}
				} else if (!sourceRequestFilter.equals("") && !accessType.equals("")) {
					if (from == null && last == null && to == null && first == null) {
						listKpiActivity = kpiActivityService.findByKpiIdByAccessTypeBySourceRequestNoPages(kpiId,
								accessType, sourceRequestFilter);
					} else {
						listKpiActivity = kpiActivityService.findByKpiIdByAccessTypeBySourceRequestNoPagesWithLimit(
								kpiId, from, to, first, last, lang, accessType, sourceRequestFilter);
					}
				} else if (accessType.equals("")) {
					if (from == null && last == null && to == null && first == null) {
						listKpiActivity = kpiActivityService.findByKpiIdBySourceRequestNoPages(kpiId,
								sourceRequestFilter);
					} else {
						listKpiActivity = kpiActivityService.findByKpiIdByAccessTypeBySourceRequestNoPagesWithLimit(
								kpiId, from, to, first, last, lang, null, sourceRequestFilter);
					}

				} else {
					if (from == null && last == null && to == null && first == null) {
						listKpiActivity = kpiActivityService.findByKpiIdByAccessTypeNoPages(kpiId, accessType);
					} else {
						listKpiActivity = kpiActivityService.findByKpiIdByAccessTypeBySourceRequestNoPagesWithLimit(
								kpiId, from, to, first, last, lang, accessType, null);
					}

				}
			}

			if (pageKpiActivity == null && listKpiActivity == null) {
				logger.info("No activity data found");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiActivity != null) {
				logger.info("Returning KpiActivitypage ");
				return new ResponseEntity<>(pageKpiActivity, HttpStatus.OK);
			} else {
				logger.info("Returning KpiActivitylist ");
				return new ResponseEntity<>(listKpiActivity, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException | DataNotValidException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Activity of PUBLIC KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/activities")
	public ResponseEntity<Object> getAllKPIActivityOfPublicKPIV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false, defaultValue = "0") Integer first,
			@RequestParam(value = "last", required = false, defaultValue = "0") Integer last,
			@RequestParam(value = "sourceRequestFilter", required = false, defaultValue = "") String sourceRequestFilter,
			@RequestParam(value = "accessType", required = false, defaultValue = "") String accessType, // READ, WRITE,
																										// DELETE
			HttpServletRequest request) {

		logger.info(
				"Requested getAllKPIActivityOfPublicKPIV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} sourceRequestFilter {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, sourceRequestFilter, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn("Wrong KPI Data");

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			Page<KPIActivity> pageKpiActivity = null;
			List<KPIActivity> listKpiActivity = null;
			if (pageNumber != -1) {
				if (sourceRequestFilter.equals("") && accessType.equals("")) {
					pageKpiActivity = kpiActivityService.findByKpiId(kpiId, new PageRequest(pageNumber, pageSize,
							new Sort(Direction.fromString(sortDirection), sortBy)));
				} else if (!sourceRequestFilter.equals("") && !accessType.equals("")) {
					pageKpiActivity = kpiActivityService.findByKpiIdByAccessTypeBySourceRequest(kpiId, accessType,
							sourceRequestFilter, new PageRequest(pageNumber, pageSize,
									new Sort(Direction.fromString(sortDirection), sortBy)));
				} else if (accessType.equals("")) {
					pageKpiActivity = kpiActivityService.findByKpiIdBySourceRequest(kpiId, sourceRequestFilter,
							new PageRequest(pageNumber, pageSize,
									new Sort(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiActivity = kpiActivityService.findByKpiIdByAccessType(kpiId, accessType, new PageRequest(
							pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (sourceRequestFilter.equals("") && accessType.equals("")) {
					listKpiActivity = kpiActivityService.findByKpiIdNoPages(kpiId);
				} else if (!sourceRequestFilter.equals("") && !accessType.equals("")) {
					listKpiActivity = kpiActivityService.findByKpiIdByAccessTypeBySourceRequestNoPages(kpiId,
							accessType, sourceRequestFilter);
				} else if (accessType.equals("")) {
					listKpiActivity = kpiActivityService.findByKpiIdBySourceRequestNoPages(kpiId, sourceRequestFilter);
				} else {
					listKpiActivity = kpiActivityService.findByKpiIdByAccessTypeNoPages(kpiId, accessType);
				}
			}

			if (pageKpiActivity == null && listKpiActivity == null) {
				logger.info("No activity data found");

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiActivity != null) {
				logger.info("Returning KpiActivitypage ");

				return new ResponseEntity<>(pageKpiActivity, HttpStatus.OK);
			} else {
				logger.info("Returning KpiActivitylist ");

				return new ResponseEntity<>(listKpiActivity, HttpStatus.OK);
			}

		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

}