package edu.unifi.disit.datamanager.service;

import java.util.List;

import edu.unifi.disit.datamanager.datamodel.profiledb.LightActivity;

public interface ILightActivityService {

	List<LightActivity> findByElementIdAndElementTypeAndSourceRequestAndSourceId(String elementId,
			String elementType, String sourceRequestFilter, String sourceIdFilter);

	List<LightActivity> findByElementIdAndElementType(String elementId, String elementType);

	LightActivity saveLightActivity(String elementId, String elementType, String sourceRequest, String sourceId);

}
