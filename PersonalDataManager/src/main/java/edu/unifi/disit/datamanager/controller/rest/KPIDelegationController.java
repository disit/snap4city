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
import org.springframework.context.NoSuchMessageException;
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

import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IDelegationService;
import edu.unifi.disit.datamanager.service.IKPIDataService;

@RestController
public class KPIDelegationController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IKPIDataService kpiDataService;

	@Autowired
	IAccessService accessService;

	// @Autowired
	// IActivityService activityService;

	@Autowired
	ICredentialsService credentialService;

	@Autowired
	AppsController appsController;

	@Autowired
	UserController userController;

	// -------------------GET KPI Data From ID ------------------------------------
	@RequestMapping(value = "/api/v1/kpidelegation/{id}", method = RequestMethod.GET)
	public ResponseEntity<Object> getKPIDelegationV1ById(@PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIDelegationV1ById id {} lang {}", id, lang);

		try {
			Delegation delegation = delegationService.getDelegationById(id, lang);

			if (delegation == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else {
				KPIData kpiData = kpiDataService.getKPIDataById(Long.parseLong(delegation.getElementId()), lang);
				if (!kpiData.getUsername().equals(credentialService.getLoggedUsername(lang))){
					return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
				}
				logger.info("Returning delegation {}", delegation.getId());
				return new ResponseEntity<Object>(delegation, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	@RequestMapping(value = "/api/v1/kpidelegation/save", method = RequestMethod.POST)
	public ResponseEntity<Object> saveKPIDelegationV1(@RequestBody Delegation delegation,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {
		
		if(delegation.getUsernameDelegator() == null) {
			delegation.setUsernameDelegator(credentialService.getLoggedUsername(lang));
		}
		if (delegation.getId() != null) {
		return userController.putDelegationV1(credentialService.getLoggedUsername(lang), delegation.getId(), delegation, sourceRequest,
				lang, request);
		}
		return userController.postDelegationV1(credentialService.getLoggedUsername(lang), delegation, sourceRequest,
				lang, request);
	}

	// -------------------GET ALL KPI Data Pageable
	// ------------------------------------
	@RequestMapping(value = "/api/v1/kpidata/{kpiId}/delegation", method = RequestMethod.GET)
	public ResponseEntity<Object> getAllDelegationV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			HttpServletRequest request) {

		logger.info(
				"Requested getAllKPIValueV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang);
			if (!kpiData.getUsername().equals(credentialService.getLoggedUsername(lang))
					&& !accessService.checkAccessFromApp(Long.toString(kpiId), lang).getResult()) {
				return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
			}

			Page<Delegation> pageDelegation = null;
			List<Delegation> listDelegation = null;
			if (pageNumber != -1) {
				pageDelegation = delegationService.findByElementId(Long.toString(kpiId), new PageRequest(pageNumber, pageSize,
							new Sort(Direction.fromString(sortDirection), sortBy)));
			} else {
				listDelegation = delegationService.findByElementIdNoPages(Long.toString(kpiId));
			}
			// activityService.saveActivityFromUsername(null, datas, sourceRequest, null,
			// null, ActivityAccessType.READ, ActivityDomainType.DATA);

			if (pageDelegation == null && listDelegation == null) {
				logger.info("No data found");
				return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			} else if (pageDelegation != null){
				logger.info("Returning kpidatapage ");
				return new ResponseEntity<Object>(pageDelegation, HttpStatus.OK);
			} else if (listDelegation != null) {
				logger.info("Returning kpidatalist ");
				return new ResponseEntity<Object>(listDelegation, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException d) {
			logger.warn("Wrong Arguments", d);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		} catch (NoSuchMessageException e) {
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		}

	}

}