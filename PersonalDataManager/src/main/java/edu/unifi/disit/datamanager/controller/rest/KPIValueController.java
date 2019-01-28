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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIValue;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
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

	//@Autowired
	//IActivityService activityService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET KPI Data From ID ------------------------------------
	@RequestMapping(value = "/api/v1/kpivalue/{id}", method = RequestMethod.GET)
	public ResponseEntity<Object> getKPIValueV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIValueV1ByIdAndKPIId id {} lang {}", id, lang);

		try {
			KPIValue kpivalue = kpiValueService.getKPIValueById(id, lang);

			if (kpivalue == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				KPIData kpiData = kpiDataService.getKPIDataById(kpivalue.getKpiId(), lang);
				if (!kpiData.getUsername().equals(credentialService.getLoggedUsername(lang)) && !accessService.checkAccessFromApp(Long.toString(kpivalue.getKpiId()), lang).getResult()){
					return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
				}
				logger.info("Returning kpidata {}", kpivalue.getId());
				return new ResponseEntity<Object>(kpivalue, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	@RequestMapping(value = "/api/v1/kpivalue/save", method = RequestMethod.POST)
	public ResponseEntity<Object> saveKPIValueV1(@RequestBody KPIValue kpivalue,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang) {

		logger.info("Requested saveKPIDataV1 id {}", kpivalue.getId());
		
		try {
			
			KPIData kpiData = kpiDataService.getKPIDataById(kpivalue.getKpiId(), lang);
			
			if (!kpiData.getUsername().equals(credentialService.getLoggedUsername(lang)) ){
				
				return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
			}
			try {
				List<KPIValue> lastKpiValue  = kpiValueService.findByKpiIdNoPagesWithLimit(kpivalue.getKpiId(), null, null, 0, 1, lang);
				if (lastKpiValue.isEmpty() || lastKpiValue.get(0).getInsertTime().getTime() < kpivalue.getInsertTime().getTime()) {
					kpiData.setLastDate(kpivalue.getInsertTime());
					kpiData.setLastValue(kpivalue.getValue());
					kpiDataService.saveKPIData(kpiData);
				}
			} catch (NoSuchMessageException | DataNotValidException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			kpiValueService.saveKPIValue(kpivalue);
			return new ResponseEntity<Object>(kpivalue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Data Pageable
	// ------------------------------------
	@RequestMapping(value = "/api/v1/kpidata/{kpiId}/values", method = RequestMethod.GET)
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

		logger.info("Requested getAllKPIValueV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		
		
		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang);
			if (!kpiData.getUsername().equals(credentialService.getLoggedUsername(lang)) && !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()){
				return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
			}
			
			Page<KPIValue> pageKpiValue = null;
			List<KPIValue> listKpiValue = null;
			if (pageNumber != -1) {
				if (searchKey.equals("")) {
					pageKpiValue = kpiValueService.findByKpiId(kpiId, new PageRequest(pageNumber, pageSize,
							new Sort(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiValue = kpiValueService.findByKpiIdFiltered(kpiId, searchKey, new PageRequest(pageNumber, pageSize,
							new Sort(Direction.fromString(sortDirection), sortBy)));
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
			// activityService.saveActivityFromUsername(null, datas, sourceRequest, null,
			// null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if (pageKpiValue == null && listKpiValue == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (pageKpiValue != null){
				logger.info("Returning KpiValuepage ");
				return new ResponseEntity<Object>(pageKpiValue, HttpStatus.OK);
			} else if (listKpiValue != null) {
				logger.info("Returning KpiValuelist ");
				return new ResponseEntity<Object>(listKpiValue, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException | DataNotValidException d) {
			logger.warn("Wrong Arguments", d);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		} 
	}

}