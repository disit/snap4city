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

import java.util.Iterator;
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

import edu.unifi.disit.snapengager.datamodel.profiledb.StatsSurvey1Response;
import edu.unifi.disit.snapengager.datamodel.profiledb.StatsSurvey1ResponseDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.StatsSurvey1ResponseText;

@RestController
public class Survey1Controller {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	StatsSurvey1ResponseDAO surve1repo;

	// -------------------GET Survey1 response ---------------------------------------------
	@RequestMapping(value = "/api/v1/survey1", method = RequestMethod.GET)
	public ResponseEntity<Object> getEngagementsV1(
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "question", required = true) int questionNumber,
			@RequestParam(value = "organization", required = true) String organization,
			HttpServletRequest request) {

		logger.info("Requested survey1 V1 lang {} question {} organization {}", lang, questionNumber, organization);

		try {
			List<StatsSurvey1Response> ssrs = surve1repo.findTopByQuestionNameAndOrganizationOrderByCreatedDesc("question" + String.valueOf(questionNumber), organization);
			for (StatsSurvey1Response ssr : ssrs) {
				Iterator<StatsSurvey1ResponseText> i = ssr.getContributions().iterator();
				while (i.hasNext())
					logger.debug("{}", i.next());
			}

			if (ssrs.size() == 0) {
				logger.error("Cannot find it");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot find the requested values");
			}

			return new ResponseEntity<Object>(ssrs.get(0).getContributions(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Errore {}", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) e.getMessage());
		}
	}
}