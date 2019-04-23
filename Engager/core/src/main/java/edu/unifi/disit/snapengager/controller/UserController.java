/* Snap4City Engager (SE)
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package edu.unifi.disit.snapengager.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.snapengager.datamodel.profiledb.Engagement;
import edu.unifi.disit.snapengager.service.ICredentialsService;
import edu.unifi.disit.snapengager.service.IEngagementService;

@RestController
public class UserController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IEngagementService engagementService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET alive ---------------------------------------------
	@RequestMapping(value = "/api/test", method = RequestMethod.GET)
	public ResponseEntity<String> dataControllerTest() {
		return new ResponseEntity<String>("alive", HttpStatus.OK);
	}

	// -------------------GET Engagements ---------------------------------------------
	@RequestMapping(value = "/api/v1/engagements", method = RequestMethod.GET)
	public ResponseEntity<Object> getEngagementsV1(
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			// @RequestParam(value = "highLevelType", required = false, defaultValue = "") String highLevelType,
			HttpServletRequest request) {

		logger.info("Requested getEngagements V1 lang {}", lang);

		if (pageNumber != -1) {
			logger.info("Pageable pageNumber {}", pageNumber);
			logger.info("Pageable pageSize {}", pageSize);
			logger.info("Pageable sortDirection {}", sortDirection);
			logger.info("Pageable sortBy {}", sortBy);
			logger.info("Pageable searchKey {}", searchKey);
		}

		try {

			credentialService.checkAssistanceEnabled(lang);

			if (pageNumber == -1) {// not pageable // TODOsupport RootToolAdmin get engagements from any users
				List<Engagement> engagements = engagementService.getActive(lang);

				for (Engagement e : engagements) {
					logger.debug(e.getId());
				}

				return new ResponseEntity<Object>(engagements, HttpStatus.OK);
			} else {
				Page<Engagement> engagements;
				if (credentialService.isRoot(lang)) {
					if (searchKey.equals("")) {
						engagements = engagementService.findAll(
								new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)),
								lang);
					} else {
						engagements = engagementService.findAllBySearch(searchKey,
								new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)),
								lang);
					}
				} else {
					if (searchKey.equals("")) {
						engagements = engagementService.findAllLogged(
								new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)),
								lang);
					} else {
						engagements = engagementService.findAllLoggedBySearch(searchKey,
								new PageRequest(pageNumber, pageSize,
										new Sort(Direction.fromString(sortDirection), sortBy)),
								lang);
					}
				}
				return new ResponseEntity<Object>(engagements, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error("Errore {}", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) e.getMessage());
		}
	}

	// -------------------GET Engagements ---------------------------------------------
	@RequestMapping(value = "/api/v1/engagements/last", method = RequestMethod.GET)
	public ResponseEntity<Object> getEngagementsLastV1(
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getEngagements LAST V1 lang {}", lang);

		try {

			credentialService.checkAssistanceEnabled(lang);

			Engagement engagement = engagementService.getLastActive(lang);
			return new ResponseEntity<Object>(engagement.getId(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Errore {}", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) e.getMessage());
		}
	}

	// -------------------DELETE Engagements ---------------------------------------------
	@RequestMapping(value = "/api/v1/engagements/{engagement_id}", method = RequestMethod.DELETE)
	public ResponseEntity<Object> deleteEngagementV1(@PathVariable("engagement_id") Long engagementId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested deleteEngagement V1 engagementId {}, lang {}", engagementId, lang);

		try {

			credentialService.checkAssistanceEnabled(lang);

			engagementService.setDeleted(engagementId, lang);
			return new ResponseEntity<Object>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Errore {}", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) e.getMessage());
		}
	}

	// -------------------DELETE ALL Engagements ---------------------------------------------
	@RequestMapping(value = "/api/v1/engagements", method = RequestMethod.DELETE)
	public ResponseEntity<Object> deleteEngagementV1(
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested deleteAllEngagement V1 lang {}", lang);

		try {
			credentialService.checkAssistanceEnabled(lang);

			engagementService.setAllDeleted(lang);
			return new ResponseEntity<Object>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Errore {}", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) e.getMessage());
		}
	}
}