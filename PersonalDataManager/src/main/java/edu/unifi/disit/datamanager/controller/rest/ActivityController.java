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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.profiledb.Activity;
import edu.unifi.disit.datamanager.service.IActivityService;

@RestController
public class ActivityController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IActivityService activityService;

	// -------------------GET ALL Activity for appId ---------------------------------------------
	@RequestMapping(value = "/api/v1/apps/{appId}/activity", method = RequestMethod.GET)
	public ResponseEntity<Object> getConsentV1(@PathVariable("appId") String appId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") String lang,
			@RequestParam(value = "delegated", required = false, defaultValue = "false") Boolean delegated) {

		logger.info("Requested GET All actvity for {}, delegated {} lang {}", appId, delegated, lang);

		List<Activity> actvities = activityService.getActivities(appId, delegated);

		if ((actvities == null) || (actvities.isEmpty())) {
			logger.info("No actvities found");
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} else {
			logger.info("Returning actvities {}", actvities.size());
			for (int index = 0; index < actvities.size(); index++)
				logger.debug("{}- {}", index, actvities.get(index));

			return new ResponseEntity<Object>(actvities, HttpStatus.OK);
		}
	}
}