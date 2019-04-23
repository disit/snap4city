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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.snapengager.datamodel.Experience;
import edu.unifi.disit.snapengager.service.IExperienceService;

@RestController
public class ExperienceController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IExperienceService experienceService;

	// -------------------GET Experience ---------------------------------------------
	@RequestMapping(value = "/api/v1/experiences", method = RequestMethod.GET)
	public ResponseEntity<Object> getExperiencesV1(
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "org", required = false, defaultValue = "DISIT") String org,
			@RequestParam(value = "group", required = false, defaultValue = "Developer") String group,
			HttpServletRequest request) {

		logger.info("Requested getExperiences V1 lang {} org {} group {}", lang, org, group);

		try {
			List<Experience> ex = experienceService.getRandomize(org, group, lang);
			return new ResponseEntity<Object>(ex, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Errore {}", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) e.getMessage());
		}
	}
}