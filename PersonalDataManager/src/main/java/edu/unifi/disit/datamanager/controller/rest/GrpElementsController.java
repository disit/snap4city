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
import java.util.Set;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroup;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElement;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IDelegationService;
import edu.unifi.disit.datamanager.service.IDeviceGroupElementService;
import edu.unifi.disit.datamanager.service.IDeviceGroupService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;
import java.util.Iterator;

@RestController
public class GrpElementsController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDelegationService delegationService;

	@Autowired
	IDeviceGroupService deviceGroupService;
        
        @Autowired
	IDeviceGroupElementService deviceGroupElementService;

	@Autowired
	IAccessService accessService;

	@Autowired
	IKPIActivityService kpiActivityService;

	@Autowired
	ICredentialsService credentialService;

	@Autowired
	AppsController appsController;

	@Autowired
	UserController userController;

	// -------------------GET ALL Device Group Element Pageable ---------------
	@GetMapping("/api/v1/devicegroup/{grpId}/elements")
	public ResponseEntity<Object> getAllElementV1Pageable(@PathVariable("grpId") Long grpId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
                        @RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			HttpServletRequest request) {

		logger.info(
				"Requested getAllElementV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, grpId);

		try {
			DeviceGroup grpData = deviceGroupService.getDeviceGroupById(grpId, lang, false);
			if (grpData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!grpData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(grpId), lang).getResult())) {
				throw new CredentialsException();
			}

			Page<DeviceGroupElement> pageElement = null;
			List<DeviceGroupElement> listElement = null;
			if (pageNumber != -1) {
				if(searchKey.isEmpty()) pageElement = deviceGroupElementService.findByDeviceGroupId(grpId,
						new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
                                else pageElement = deviceGroupElementService.findByDeviceGroupIdFiltered(grpId, searchKey,
						new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
			} else {
				if(searchKey.isEmpty()) listElement = deviceGroupElementService.findByDeviceGroupIdNoPages(grpId);
                                else listElement = deviceGroupElementService.findByDeviceGroupIdNoPagesFiltered(grpId, searchKey);
			}

			if (pageElement == null && listElement == null) {
				logger.info("No elements found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No elements found", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageElement != null) {
				logger.info("Returning GrpElementPage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT);

				return new ResponseEntity<>(pageElement, HttpStatus.OK);
			} else {
				logger.info("Returning GrpElementList ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT);

				return new ResponseEntity<>(listElement, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}
        
        // -------------------GET ALL Device Group Element Pageable ---------------
	@DeleteMapping("/api/v1/devicegroup/{grpId}/elements")
	public ResponseEntity<Object> deleteAllElementV1(@PathVariable("grpId") Long grpId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info(
				"Requested deleteAllElementV1 grpId {}",
				grpId);

		try {
			DeviceGroup grpData = deviceGroupService.getDeviceGroupById(grpId, lang, false);
			if (grpData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.DELETE, KPIActivityDomainType.GROUPELEMENT,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!grpData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(grpId), lang).getResult())) {
				throw new CredentialsException();
			}

			List<DeviceGroupElement> listElement = deviceGroupElementService.findByDeviceGroupIdNoPages(grpId);                        
                        
			if (listElement == null) {
				logger.info("No elements found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.DELETE, KPIActivityDomainType.GROUPELEMENT,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No elements found", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);			
			} else {				
                                
                                for(DeviceGroupElement e: listElement ) {
                                    e.setDeleteTime(new Date());                                                  
                                }                               
                                
                                deviceGroupService.lastUpdatedNow(grpId);                      
                                
                                logger.info("Deleted all elements from device group {}", grpId);
                                
                                kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, grpId, ActivityAccessType.DELETE, KPIActivityDomainType.GROUPELEMENT);

				return new ResponseEntity<>(listElement, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.DELETE, KPIActivityDomainType.GROUPELEMENT,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.DELETE, KPIActivityDomainType.GROUPELEMENT,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}
        
        // -------------------GET Group Element Types by Group Owner ---------------
	@GetMapping("/api/v1/devicegroup/{grpId}/availElmtTypesToAdd")
	public ResponseEntity<Object> getAvailElmtTypesToAdd(
                @PathVariable("grpId") Long grpId, 
                @RequestParam(value = "sourceRequest") String sourceRequest, 
                @RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
                HttpServletRequest request) {
            try {
                logger.info("Requested getAvailElmtTypesToAdd sourceRequest {} grpId {} lang {} ",sourceRequest,grpId,lang);
                
                kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELMTTYPES,
						request.getRequestURI() + "?" + request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
                
                Set<String> elementTypes = null;
                DeviceGroup grpData = deviceGroupService.getDeviceGroupById(grpId, null, false);
                if(credentialService.isRoot(lang)) {
                    elementTypes = deviceGroupElementService.getAllElmtTypes();  
                }
                else {
                    elementTypes = deviceGroupElementService.getAvailElmtTypesToAdd(credentialService.getLoggedUsername(lang));                
                }
                if (elementTypes == null || elementTypes.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                else return new ResponseEntity<>(elementTypes, HttpStatus.OK);                        			
                
            } catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELMTTYPES,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
            }
            catch (Exception e) {
                logger.info("Failed getAvailElmtTypesToAdd sourceRequest {} exception {}  ",sourceRequest,e);	
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } 
	}
        
        // -------------------GET Items to be added to group by Group owner ---------------
	@GetMapping("/api/v1/devicegroup/{grpId}/availElmtToAdd")
	public ResponseEntity<Object> getAvailElmtToAdd(
                @PathVariable("grpId") Long grpId, 
                @RequestParam("elmtType") String elmtType, 
                @RequestParam(value = "sourceRequest") String sourceRequest, 
                @RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
                HttpServletRequest request) {
            try {
                
                logger.info("Requested getAvailElmtToAdd sourceRequest {} grpId {} elmtType {} lang {} ",sourceRequest,grpId,elmtType,lang);
                
                kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
                        sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.METADATA,
                        request.getRequestURI() + "?" + request.getQueryString(),
                        "Wrong Device Group Data", null, request.getRemoteAddr());

                Set<Object> items = null;
                DeviceGroup grpData = deviceGroupService.getDeviceGroupById(grpId, null, false);
                if(credentialService.isRoot(lang)) {
                    items = deviceGroupElementService.getAllItems(elmtType);
                }
                else {
                    items = deviceGroupElementService.getAvailItemsToAdd(credentialService.getLoggedUsername(lang),elmtType);
                }
                if (items == null || items.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                else return new ResponseEntity<>(items, HttpStatus.OK);        

            } catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELMTTYPES,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
            }
            catch (Exception e) {
                logger.info("Failed getAvailElmtToAdd sourceRequest {} exception {}  ",sourceRequest,e);	
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } 
	}
        
	// -------------------POST New Device Group Value ------------------------------------
	@PostMapping("/api/v1/devicegroup/{grpId}/elements")
	public ResponseEntity<Object> postAddNewElmtToGrpV1(@PathVariable("grpId") Long grpId,
			@RequestBody List<DeviceGroupElement> elements, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		String logElmts = "";
                String logElmtsType = "";
                for(DeviceGroupElement e: elements) { 
                    logElmts = logElmts.concat(e.getElementId()).concat(" "); 
                    logElmtsType = e.getElementType();
                }

                logger.info("Requested postAddNewElmtToGrpV1 grpId {} elmtsType {} logElmts {} sourceRequest {} lang{}", grpId, logElmtsType, logElmts, sourceRequest, lang);

		try {

			DeviceGroup kpiData = deviceGroupService.getDeviceGroupById(grpId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.WRITE, KPIActivityDomainType.DELEGATION,
						request.getRequestURI() + "?" + request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(grpId), lang).getResult())) {
				throw new CredentialsException();
			}
			
                        kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, grpId,
                                        ActivityAccessType.WRITE, KPIActivityDomainType.GROUPELEMENT);

                        List<DeviceGroupElement> createdElements = deviceGroupElementService.addElmtsToGrp(grpId,elements);
			
			logger.info("Posted grpId {} elmtsType {} logElmts {} sourceRequest {} lang{}", grpId, logElmtsType, logElmts, sourceRequest, lang);
                        
                        return ResponseEntity.status(HttpStatus.CREATED).body(createdElements);

		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.WRITE, KPIActivityDomainType.GROUPELEMENT,
					request.getRequestURI() + "?" + request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}     
        
        // -------------------DELETE New Device Group Value ------------------------------------
	@DeleteMapping("/api/v1/devicegroup/{grpId}/elements/{id}")
	public ResponseEntity<Object> removeGrpElmtFromGrpV1(@PathVariable("grpId") Long grpId, @PathVariable("id") Long id,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested removeGrpElmtFromGrpV1 grpId {} id {} sourceRequest {}", grpId, id, sourceRequest);

		try {

			DeviceGroup kpiData = deviceGroupService.getDeviceGroupById(grpId, lang, false);

			if (kpiData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.DELETE, KPIActivityDomainType.GROUPELEMENT,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(grpId), lang).getResult())) {
				throw new CredentialsException();
			}

			DeviceGroupElement kpiDelegationToDelete = deviceGroupElementService.getDeviceGroupElementById(id);
			if (kpiDelegationToDelete == null) {
				logger.info("No data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.DELETE, KPIActivityDomainType.GROUPELEMENT,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No data found", null, request.getRemoteAddr());

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			kpiDelegationToDelete.setDeleteTime(new Date());

                        deviceGroupService.lastUpdatedNow(grpId);
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, sourceId, grpId,

					ActivityAccessType.DELETE, KPIActivityDomainType.GROUPELEMENT);
			logger.info("Deleted grpElement {}", kpiDelegationToDelete.getId());                        
			return ResponseEntity.status(HttpStatus.OK).body(kpiDelegationToDelete.getId());
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.DELETE, KPIActivityDomainType.GROUPELEMENT,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
        
        @GetMapping("/api/v1/public/devicegroup/{grpId}/elements")
	public ResponseEntity<Object> getPublicAllElementV1Pageable(@PathVariable("grpId") Long grpId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
                        @RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			HttpServletRequest request) {

		logger.info(
				"Requested getPublicAllElementV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, grpId);

		try {
			DeviceGroup grpData = deviceGroupService.getDeviceGroupById(grpId, lang, false);
			if (grpData == null) {
				logger.warn("Wrong Device Group Data");
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"Wrong Device Group Data", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} /*else if (!grpData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(grpId), lang).getResult())) {
				throw new CredentialsException();
			}*/
                        else {                            
                            if(!deviceGroupService.findByUsernameDelegatedByHighLevelTypeFilteredNoPages("ANONYMOUS", "My",
						"MyGroup", "").contains(grpData)) {
                                    throw new CredentialsException();
                            }
                        }

			Page<DeviceGroupElement> pageElement = null;
			List<DeviceGroupElement> listElement = null;
			if (pageNumber != -1) {
				if(searchKey.isEmpty()) pageElement = deviceGroupElementService.findByDeviceGroupId(grpId,
						new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
                                else pageElement = deviceGroupElementService.findByDeviceGroupIdFiltered(grpId, searchKey,
						new PageRequest(pageNumber, pageSize, new Sort(Direction.fromString(sortDirection), sortBy)));
			} else {
				if(searchKey.isEmpty()) listElement = deviceGroupElementService.findByDeviceGroupIdNoPages(grpId);
                                else listElement = deviceGroupElementService.findByDeviceGroupIdNoPagesFiltered(grpId, searchKey);
			}

			if (pageElement == null && listElement == null) {
				logger.info("No elements found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT,
						request.getRequestURI() + "?"
								+ request.getQueryString(),
						"No elements found", null, request.getRemoteAddr());
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageElement != null) {
				logger.info("Returning GrpElementPage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, null,
						grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT);

				return new ResponseEntity<>(pageElement, HttpStatus.OK);
			} else {
				logger.info("Returning GrpElementList ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest, null,
						grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT);

				return new ResponseEntity<>(listElement, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, grpId, ActivityAccessType.READ, KPIActivityDomainType.GROUPELEMENT,
					request.getRequestURI() + "?"
							+ request.getQueryString(),
					d.getMessage(), d, request.getRemoteAddr());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}

	}
        
}