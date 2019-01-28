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

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIMetadata;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IKPIMetadataService;

@RestController
public class KPIMetadataController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IKPIMetadataService kpiMetadataService;

	//@Autowired
	//IActivityService activityService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET KPI Data From ID ------------------------------------
	@RequestMapping(value = "/api/v1/kpimetadata/{id}", method = RequestMethod.GET)
	public ResponseEntity<Object> getKPIMetadataV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIMetadataV1ById id {} lang {}", id, lang);

		try {
			KPIMetadata kpimetadata = kpiMetadataService.getKPIMetadataById(id, lang);

			if (kpimetadata == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpidata {}", kpimetadata.getId());
				return new ResponseEntity<Object>(kpimetadata, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	@RequestMapping(value = "/api/v1/kpimetadata/save", method = RequestMethod.POST)
	public ResponseEntity<Object> saveKPIMetadataV1(@RequestBody KPIMetadata kpimetadata,
			@RequestParam(value = "sourceRequest") String sourceRequest) {

		logger.info("Requested saveKPIMetadataV1 id {}", kpimetadata.getId());
		try {
			kpiMetadataService.saveKPIMetadata(kpimetadata);
			return new ResponseEntity<Object>(kpimetadata, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Data Pageable
	// ------------------------------------
	@RequestMapping(value = "/api/v1/kpidata/{kpiId}/metadata", method = RequestMethod.GET)
	public ResponseEntity<Object> getAllKPIMetadataV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = true) int pageNumber,
			@RequestParam(value = "pageSize", required = true, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "asc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			HttpServletRequest request) {

		logger.info("Requested getAllKPIMetadataV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			Page<KPIMetadata> pageKpiMetadata = null;
				if (searchKey.equals("")) {
					pageKpiMetadata = kpiMetadataService.findAllByKpiId(kpiId, new PageRequest(pageNumber, pageSize,
							new Sort(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiMetadata = kpiMetadataService.findAllFilteredByKpiId(kpiId, searchKey, new PageRequest(pageNumber, pageSize,
							new Sort(Direction.fromString(sortDirection), sortBy)));
				}
			
			// activityService.saveActivityFromUsername(null, datas, sourceRequest, null,
			// null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if (pageKpiMetadata == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpidatapage ");
				return new ResponseEntity<Object>(pageKpiMetadata, HttpStatus.OK);
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