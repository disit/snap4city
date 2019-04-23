package edu.unifi.disit.datamanager.service;

import java.util.List;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;

public interface IKPIActivityService {

	void saveActivityFromUsername(String username, String sourceRequest, Long kpiId, ActivityAccessType accessType,
			KPIActivityDomainType domain);
	
	void saveActivityFromUsername(String username, String sourceRequest, List<KPIData> listKpiData,
			ActivityAccessType accessType, KPIActivityDomainType domain);

	void saveActivityViolationFromUsername(String username, String sourceRequest, Long kpiId,
			ActivityAccessType accessType, KPIActivityDomainType domain, String query, String errorMessage, Throwable stacktrace, String ipAddress);

	

}
