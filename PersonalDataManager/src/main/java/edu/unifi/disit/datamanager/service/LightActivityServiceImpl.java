/* Data Manager (DM).
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
package edu.unifi.disit.datamanager.service;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.profiledb.LightActivity;
import edu.unifi.disit.datamanager.datamodel.profiledb.LightActivityDAO;

@Service
public class LightActivityServiceImpl implements ILightActivityService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	LightActivityDAO lightActivityRepo;

	@Override
	public LightActivity saveLightActivity(String elementId, String elementType, String sourceRequest,
			String sourceId) {
		if (elementId != null && elementType != null && sourceRequest != null && sourceId != null) {
			logger.debug("saveLightActivity INVOKED on elementId {} elementType {} sourceRequest {} sourceId {}",
					elementId, elementType, sourceRequest, sourceId);

			List<LightActivity> listLightActivity = lightActivityRepo
					.findByElementIdAndElementTypeAndSourceRequestAndSourceIdAndDeleteTimeIsNull(elementId, elementType,
							sourceRequest, sourceId);

			LightActivity lightActivity = null;

			if (listLightActivity.isEmpty()) {
				lightActivity = new LightActivity(elementId, elementType, sourceRequest, sourceId, new Date(), null);
			} else {
				lightActivity = listLightActivity.get(0);
				lightActivity.setInsertTime(new Date());
			}
			return lightActivityRepo.save(lightActivity);
		}
		return null;
	}

	@Override
	public List<LightActivity> findByElementIdAndElementTypeAndSourceRequestAndSourceId(String elementId,
			String elementType, String sourceRequestFilter, String sourceIdFilter) {
		logger.debug(
				"findByElementIdAndElementTypeAndSourceRequestAndSourceId INVOKED elementId {}, elementType {}, sourceRequestFilter {}, sourceIdFilter {}",
				elementId, elementType, sourceRequestFilter, sourceIdFilter);
		return lightActivityRepo.findByElementIdAndElementTypeAndSourceRequestAndSourceIdAndDeleteTimeIsNull(elementId,
				elementType, sourceRequestFilter, sourceIdFilter);
	}

	@Override
	public List<LightActivity> findByElementIdAndElementType(String elementId, String elementType) {
		logger.debug("findByElementIdAndElementType INVOKED on elementId {}, elementType {}", elementId, elementType);
		return lightActivityRepo.findByElementIdAndElementTypeAndDeleteTimeIsNull(elementId, elementType);
	}

}
