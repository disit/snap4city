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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivity;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivityDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivityViolation;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivityViolationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;

@Service
public class KPIActivityServiceImpl implements IKPIActivityService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	KPIActivityDAO kpiActivityRepo;

	@Autowired
	private MessageSource messages;
	
	@Autowired
	KPIActivityViolationDAO kpiActivityViolationRepo;

	@Autowired
	OwnershipDAO ownershipRepo;
	
	@Autowired
	ILightActivityService lightActivityService;

	@Override
	public void saveActivityFromUsername(String username, String sourceRequest, String sourceId, Long kpiId,
			ActivityAccessType accessType, KPIActivityDomainType domain) {
		logger.debug("saveKPIActivityFromUsername INVOKED on username {} sourceRequest {} sourceId {} kpiId {} accessType {} domain {}", username, sourceRequest, sourceId, kpiId, accessType, domain);

		KPIActivity kpiActivity = new KPIActivity(username, sourceRequest, sourceId, kpiId, accessType.toString(),
				domain.toString(), new Date(), null, null);
		if (kpiId != null) {
			lightActivityService.saveLightActivity(kpiId.toString(), "MyKPI", sourceRequest, sourceId);
		}
		kpiActivityRepo.save(kpiActivity);

	}

	@Override
	public void saveActivityViolationFromUsername(String username, String sourceRequest, Long kpiId,
			ActivityAccessType accessType, KPIActivityDomainType domain, String query, String errorMessage,
			Throwable stacktrace, String ipAddress) {
		logger.debug("saveKPIActivityViolationFromUsername INVOKED on username {} sourceRequest {} kpiId {} accessType {} domain {}", username, sourceRequest, kpiId, accessType, domain);

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if (stacktrace != null) {
			stacktrace.printStackTrace(pw);
		}

		try {
			KPIActivityViolation kpiActivityViolation = new KPIActivityViolation(username, sourceRequest, kpiId,
					accessType.toString(), domain.toString(), new Date(), null, null, query, errorMessage,
					new SerialBlob(sw.toString().getBytes()), ipAddress);
			kpiActivityViolationRepo.save(kpiActivityViolation);
		} catch (SerialException e) {
			logger.error("SerialException ", e);
		} catch (SQLException e) {
			logger.error("SQLException ", e);
		}

	}
	
	@Override
	public KPIActivity getKPIActivityById(Long id, Locale lang) throws  CredentialsException {
		logger.debug("getKPIActivityById INVOKED on id {}", id);
		return kpiActivityRepo.findById(id).orElse(null);
	}

	@Override
	public Page<KPIActivity> findByKpiId(Long kpiId, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findByKpiId INVOKED on kpiId {}",  kpiId);
		return kpiActivityRepo.findByKpiIdAndDeleteTimeIsNullAndSourceIdIsNotNull(kpiId, pageable);
	}

	@Override
	public Page<KPIActivity> findByKpiIdByAccessTypeBySourceRequest(Long kpiId, String accessType, String sourceRequestFilter, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findByKpiIdByAccessTypeBySourceRequest INVOKED on kpiId {} accesstype {} sourceRequestFilter {}", kpiId, accessType, sourceRequestFilter);
		return kpiActivityRepo.findByKpiIdAndAccessTypeAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(kpiId, accessType, sourceRequestFilter, pageable);
	}
	@Override
	public Page<KPIActivity> findByKpiIdBySourceRequest(Long kpiId, String sourceRequestFilter, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findByKpiIdBySourceRequest INVOKED on sourceRequestFilter {} kpiId {}",  sourceRequestFilter, kpiId);
		return kpiActivityRepo.findByKpiIdAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(kpiId, sourceRequestFilter, pageable);
	}
	@Override
	public Page<KPIActivity> findByKpiIdByAccessType(Long kpiId, String accessType, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findByKpiIdByAccessType INVOKED on accessType {} kpiId {}",  accessType, kpiId);
		return kpiActivityRepo.findByKpiIdAndAccessTypeAndDeleteTimeIsNullAndSourceIdIsNotNull(kpiId, accessType, pageable);
	}

	@Override
	public List<KPIActivity> findByKpiIdNoPages(Long kpiId) {
		logger.debug("findByKpiIdNoPages INVOKED on kpiId {}",  kpiId);
		return kpiActivityRepo.findByKpiIdAndDeleteTimeIsNullAndSourceIdIsNotNull(kpiId);
	}
	
	@Override
	public List<KPIActivity> findByKpiIdByAccessTypeBySourceRequestNoPagesWithLimit(Long kpiId, Date from, Date to, Integer first, Integer last, Locale lang,String accessType, String sourceRequest) throws  DataNotValidException {
		logger.debug("findByKpiIdNoPagesWithLimit INVOKED on kpiId {}, from {}, to {}, first {}, last {}", kpiId,from, to, first, last);

		if ((first != 0) && (last != 0)) {
			throw new DataNotValidException(messages.getMessage("getdata.ko.firstandlastspecified", null, lang));
		}

		return kpiActivityRepo.findByKpiIdByAccessTypeBySourceRequestNoPagesWithLimit(kpiId, from, to, first, last, accessType, sourceRequest);
	}

	@Override
	public List<KPIActivity> findByKpiIdByAccessTypeBySourceRequestNoPages(Long kpiId, String accessType, String sourceRequestFilter)
			throws  CredentialsException {
		logger.debug("findByKpiIdByAccessTypeBySourceRequestNoPages INVOKED on kpiId {} accesstype {} sourceRequestFilter {}", kpiId, accessType, sourceRequestFilter);
		return kpiActivityRepo.findByKpiIdAndAccessTypeAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(kpiId, accessType, sourceRequestFilter);
	}
	@Override
	public List<KPIActivity> findByKpiIdBySourceRequestNoPages(Long kpiId, String sourceRequestFilter)
			throws  CredentialsException {
		logger.debug("findByKpiIdBySourceRequestNoPages INVOKED on sourceRequestFilter {} kpiId {}",  sourceRequestFilter, kpiId);
		return kpiActivityRepo.findByKpiIdAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(kpiId, sourceRequestFilter);
	}
	@Override
	public List<KPIActivity> findByKpiIdByAccessTypeNoPages(Long kpiId, String accessType)
			throws  CredentialsException {
		logger.debug("findByKpiIdByAccessTypeNoPages INVOKED on accessType {} kpiId {}",  accessType, kpiId);
		return kpiActivityRepo.findByKpiIdAndAccessTypeAndDeleteTimeIsNullAndSourceIdIsNotNull(kpiId, accessType);
	}
	
	
	
	@Override
	public Page<KPIActivity> findByUsernameByKpiId(String username, Long kpiId, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findByUsernameByKpiId INVOKED on username {} kpiId {}", username, kpiId);
		return kpiActivityRepo.findByUsernameAndKpiIdAndDeleteTimeIsNullAndSourceIdIsNotNull(username, kpiId, pageable);
	}

	@Override
	public Page<KPIActivity> findByUsernameByKpiIdByAccessTypeBySourceRequest(String username, Long kpiId, String accessType, String sourceRequestFilter, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findByUsernameByKpiIdByAccessTypeBySourceRequest INVOKED on username {} kpiId {} accesstype {} sourceRequestFilter {}", username, kpiId, accessType, sourceRequestFilter);
		return kpiActivityRepo.findByUsernameAndKpiIdAndAccessTypeAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(username, kpiId, accessType, sourceRequestFilter, pageable);
	}
	@Override
	public Page<KPIActivity> findByUsernameByKpiIdBySourceRequest(String username, Long kpiId, String sourceRequestFilter, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findByUsernameByKpiIdBySourceRequest INVOKED on username {} sourceRequestFilter {} kpiId {}", username, sourceRequestFilter, kpiId);
		return kpiActivityRepo.findByUsernameAndKpiIdAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(username, kpiId, sourceRequestFilter, pageable);
	}
	@Override
	public Page<KPIActivity> findByUsernameByKpiIdByAccessType(String username, Long kpiId, String accessType, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findByUsernameByKpiIdByAccessType INVOKED on username {} accessType {} kpiId {}", username, accessType, kpiId);
		return kpiActivityRepo.findByUsernameAndKpiIdAndAccessTypeAndDeleteTimeIsNullAndSourceIdIsNotNull(username, kpiId, accessType, pageable);
	}

	@Override
	public List<KPIActivity> findByUsernameByKpiIdNoPages(String username, Long kpiId) {
		logger.debug("findByUsernameByKpiIdNoPages INVOKED on username {} kpiId {}", username, kpiId);
		return kpiActivityRepo.findByUsernameAndKpiIdAndDeleteTimeIsNullAndSourceIdIsNotNull(username, kpiId);
	}

	@Override
	public List<KPIActivity> findByUsernameByKpiIdByAccessTypeBySourceRequestNoPages(String username, Long kpiId, String accessType, String sourceRequestFilter)
			throws  CredentialsException {
		logger.debug("findByUsernameByKpiIdByAccessTypeBySourceRequestNoPages INVOKED on username {} kpiId {} accesstype {} sourceRequestFilter {}", username, kpiId, accessType, sourceRequestFilter);
		return kpiActivityRepo.findByUsernameAndKpiIdAndAccessTypeAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(username, kpiId, accessType, sourceRequestFilter);
	}
	@Override
	public List<KPIActivity> findByUsernameByKpiIdBySourceRequestNoPages(String username, Long kpiId, String sourceRequestFilter)
			throws  CredentialsException {
		logger.debug("findByUsernameByKpiIdBySourceRequestNoPages INVOKED on username {} sourceRequestFilter {} kpiId {}", username, sourceRequestFilter, kpiId);
		return kpiActivityRepo.findByUsernameAndKpiIdAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(username, kpiId, sourceRequestFilter);
	}
	@Override
	public List<KPIActivity> findByUsernameByKpiIdByAccessTypeNoPages(String username, Long kpiId, String accessType)
			throws  CredentialsException {
		logger.debug("findByUsernameByKpiIdByAccessTypeNoPages INVOKED on username {} accessType {} kpiId {}", username, accessType, kpiId);
		return kpiActivityRepo.findByUsernameAndKpiIdAndAccessTypeAndDeleteTimeIsNullAndSourceIdIsNotNull(username, kpiId, accessType);
	}
}
