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

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.LightActivity;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IKPIDataService;
import edu.unifi.disit.datamanager.service.ILightActivityService;

@RestController
public class LightActivityController {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	IKPIDataService kpiDataService;

	@Autowired
	IAccessService accessService;

	@Autowired
	ILightActivityService lightActivityService;

	@Autowired
	ICredentialsService credentialService;

	// -------------------GET ALL Light Activities
	// ---------------------------------
	@GetMapping("/api/v1/lightactivities")
	public ResponseEntity<Object> getAllLightActivityV1(@RequestParam("elementType") String elementType,
			@RequestParam("elementId") String elementId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getAllLightActivityV1 elementType {} elementId {}", elementType, elementId);

		try {
			if (elementType.equals("MyKPI")) {
				KPIData kpiData = kpiDataService.getKPIDataById(Long.valueOf(elementId), lang, false);
				if (kpiData == null) {
					logger.warn("Wrong KPI Data");
					return new ResponseEntity<>(HttpStatus.NO_CONTENT);
				} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
						&& !Boolean.TRUE
								.equals(accessService.checkAccessFromApp(elementId, elementType, lang).getResult())) {
					throw new CredentialsException();
				}
			} else if (elementType.equals("IOTID") && !Boolean.TRUE
					.equals(accessService.checkAccessFromApp(elementId, elementType, lang).getResult())) {
				throw new CredentialsException();
			}

			List<LightActivity> listLightActivity = lightActivityService.findByElementIdAndElementType(elementId,
					elementType);

			if (listLightActivity == null) {
				logger.info("No activity data found");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning listLightActivityList ");
				return new ResponseEntity<>(listLightActivity, HttpStatus.OK);
			}

		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------POST Activities
	// ---------------------------------
	@PostMapping("/api/v1/lightactivities")
	public ResponseEntity<Object> postLightActivityV1(@RequestParam("elementType") String elementType,
			@RequestParam("elementId") String elementId, @RequestParam("sourceRequest") String sourceRequest,
			@RequestParam("sourceId") String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested postLightActivityV1 elementType {} elementId {} sourceRequest {} sourceId {}",
				elementType, elementId, sourceRequest, sourceId);

		try {
			if (elementType.equals("MyKPI")) {
				KPIData kpiData = kpiDataService.getKPIDataById(Long.valueOf(elementId), lang, false);
				if (kpiData == null) {
					logger.warn("Wrong KPI Data");
					return new ResponseEntity<>(HttpStatus.NO_CONTENT);
				} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
						&& !Boolean.TRUE
								.equals(accessService.checkAccessFromApp(elementId, elementType, lang).getResult())) {
					throw new CredentialsException();
				}
			} else if (elementType.equals("IOTID") && !Boolean.TRUE
					.equals(accessService.checkAccessFromApp(elementId, elementType, lang).getResult())) {
				throw new CredentialsException();
			}

			return new ResponseEntity<>(lightActivityService.saveLightActivity(elementId,elementType,sourceRequest,sourceId), HttpStatus.OK);

		} catch (CredentialsException d) {
			logger.warn("Rights exception", d);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn("Wrong Arguments", d);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

}