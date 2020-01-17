package edu.unifi.disit.datamanager.service;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivity;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;

public interface IKPIActivityService {

	void saveActivityFromUsername(String username, String sourceRequest, String sourceId, Long kpiId,
			ActivityAccessType accessType, KPIActivityDomainType domain);

	void saveActivityViolationFromUsername(String username, String sourceRequest, Long kpiId,
			ActivityAccessType accessType, KPIActivityDomainType domain, String query, String errorMessage,
			Throwable stacktrace, String ipAddress);

	KPIActivity getKPIActivityById(Long id, Locale lang) throws CredentialsException;

	Page<KPIActivity> findByKpiId(Long kpiId, Pageable pageable) throws CredentialsException;

	Page<KPIActivity> findByKpiIdByAccessTypeBySourceRequest(Long kpiId, String accessType, String sourceRequestFilter, Pageable pageable)
			throws CredentialsException;

	Page<KPIActivity> findByKpiIdBySourceRequest(Long kpiId, String sourceRequestFilter, Pageable pageable) throws CredentialsException;

	Page<KPIActivity> findByKpiIdByAccessType(Long kpiId, String accessType, Pageable pageable)
			throws CredentialsException;

	List<KPIActivity> findByKpiIdNoPages(Long kpiId) throws CredentialsException;
	
	List<KPIActivity> findByKpiIdByAccessTypeBySourceRequestNoPagesWithLimit(Long kpiId, Date from, Date to, Integer first, Integer last,
			Locale lang, String accessType, String sourceRequest) throws DataNotValidException;

	List<KPIActivity> findByKpiIdByAccessTypeBySourceRequestNoPages(Long kpiId, String accessType, String sourceRequestFilter)
			throws CredentialsException;

	List<KPIActivity> findByKpiIdBySourceRequestNoPages(Long kpiId, String sourceRequestFilter) throws CredentialsException;

	List<KPIActivity> findByKpiIdByAccessTypeNoPages(Long kpiId, String accessType) throws CredentialsException;

	Page<KPIActivity> findByUsernameByKpiId(String username, Long kpiId, Pageable pageable) throws CredentialsException;

	Page<KPIActivity> findByUsernameByKpiIdByAccessTypeBySourceRequest(String username, Long kpiId, String accessType,
			String sourceRequestFilter, Pageable pageable) throws CredentialsException;

	Page<KPIActivity> findByUsernameByKpiIdBySourceRequest(String username, Long kpiId, String sourceRequestFilter, Pageable pageable)
			throws CredentialsException;

	Page<KPIActivity> findByUsernameByKpiIdByAccessType(String username, Long kpiId, String accessType,
			Pageable pageable) throws CredentialsException;

	List<KPIActivity> findByUsernameByKpiIdNoPages(String username, Long kpiId) throws CredentialsException;

	List<KPIActivity> findByUsernameByKpiIdByAccessTypeBySourceRequestNoPages(String username, Long kpiId, String accessType,
			String source) throws CredentialsException;

	List<KPIActivity> findByUsernameByKpiIdBySourceRequestNoPages(String username, Long kpiId, String sourceRequestFilter)
			throws CredentialsException;

	List<KPIActivity> findByUsernameByKpiIdByAccessTypeNoPages(String username, Long kpiId, String accessType)
			throws CredentialsException;



	

}
