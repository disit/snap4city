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

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivity;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivityDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivityViolation;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivityViolationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;

@Service
public class KPIActivityServiceImpl implements IKPIActivityService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	KPIActivityDAO kpiActivityRepo;

	@Autowired
	KPIActivityViolationDAO kpiActivityViolationRepo;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Autowired
	ICredentialsService credentialsService;

	@Override
	public void saveActivityFromUsername(String username, String sourceRequest, Long kpiId,
			ActivityAccessType accessType, KPIActivityDomainType domain) {
		logger.debug("saveKPIActivityFromUsername INVOKED on username {} sourceRequest {} kpiId {} accessType {} domain {}", username, sourceRequest, kpiId, accessType, domain);

		KPIActivity kpiActivity = new KPIActivity(username, sourceRequest, kpiId, accessType.toString(),
				domain.toString(), new Date(), null, null);
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
			logger.error("SerialException {}", e);
		} catch (SQLException e) {
			logger.error("SQLException {}", e);
		}

	}

}
