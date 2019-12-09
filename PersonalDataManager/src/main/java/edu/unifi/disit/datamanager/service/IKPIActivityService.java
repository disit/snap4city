package edu.unifi.disit.datamanager.service;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;

public interface IKPIActivityService {

	void saveActivityFromUsername(String username, String sourceRequest, Long kpiId, ActivityAccessType accessType,
			KPIActivityDomainType domain);

	void saveActivityViolationFromUsername(String username, String sourceRequest, Long kpiId,
			ActivityAccessType accessType, KPIActivityDomainType domain, String query, String errorMessage, Throwable stacktrace, String ipAddress);

}
