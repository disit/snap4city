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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.ActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.service.IActivityService;
import edu.unifi.disit.datamanager.service.IDataService;

@RestController
public class DataController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IDataService dataService;

	@Autowired
	IActivityService activityService;

	// -------------------GET ALL Data ---------------------------------------------
	@RequestMapping(value = "/api/v1/data", method = RequestMethod.GET)
	public ResponseEntity<Object> getDataV1(
			@RequestParam(value = "sourceRequest", required = false) String sourceRequest,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested GET All Data  lang {}", lang);

		if (sourceRequest != null)
			logger.info("SourceRequest specified {}", sourceRequest);

		List<Data> datas = dataService.getAllData(lang);

		if ((datas == null) || (datas.isEmpty())) {
			logger.info("No data found");
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		} else {
			// save the activity for later auction
			activityService.saveActivityFromUsername(null, datas, sourceRequest, null, null, ActivityAccessType.READ, ActivityDomainType.DATA);

			logger.info("Returning data {}", datas.size());
			// for (int index = 0; index < datas.size(); index++)
			// logger.trace("{}- {}", index, datas.get(index));

			return new ResponseEntity<Object>(datas, HttpStatus.OK);
		}
	}
}