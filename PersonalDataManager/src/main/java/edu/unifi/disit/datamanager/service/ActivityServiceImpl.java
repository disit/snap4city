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
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.ActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.Activity;
import edu.unifi.disit.datamanager.datamodel.profiledb.ActivityDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.ActivityViolation;
import edu.unifi.disit.datamanager.datamodel.profiledb.ActivityViolationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;

@Service
public class ActivityServiceImpl implements IActivityService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	ActivityDAO activityRepo;

	@Autowired
	ActivityViolationDAO activityViolationRepo;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Autowired
	ICredentialsService credentialsService;

	// if delegated == true, return the delegated
	// if delegated == false, return the delegator (check appid and delegated==null)
	@Override
	public List<Activity> getActivities(String appId, Boolean delegated, Locale lang) throws CredentialsException {
		logger.debug("getActivities INVOKED on appid {}, delegated {}", appId, delegated);

		credentialsService.checkAppIdCredentials(appId, lang);// enforcement credentials

		if (!delegated)
			return activityRepo.findByAppIdAndDelegatedAppIdIsNullAndDeleteTimeIsNull(appId);
		else
			return activityRepo.findByDelegatedAppIdAndDeleteTimeIsNull(appId);
	}

	@Override
	public void saveActivityFromApp(String requestAppidOwner, List<Data> delegatedDatas, String sourceRequest, String variableName, String motivation, ActivityAccessType accesstype, ActivityDomainType domain) {
		logger.debug("saveActivityFromApp INVOKED on requestAppidOwner {}", requestAppidOwner);

		// owner of the activity
		List<Ownership> owns = ownershipRepo.findByElementId(requestAppidOwner);
		String requestUsername = getUsernames(owns);
		String requestAppname = getElementNames(owns);

		Activity owner = new Activity(new Date(), requestAppidOwner, requestUsername, requestAppname, sourceRequest, variableName, motivation, accesstype.toString(), domain.toString(), null);
		activityRepo.save(owner);// save owner

		Hashtable<String, String> ht = new Hashtable<String, String>();

		ht.put(requestAppidOwner, requestUsername);// insert owner so it will not be inserted anymore

		if (delegatedDatas != null)
			for (Data data : delegatedDatas) {
				if ((data.getAppId() != null) && (ht.get(data.getAppId()) == null)) {

					List<Ownership> ownsDelega = ownershipRepo.findByElementId(data.getAppId());

					Activity delegated = new Activity(new Date(), requestAppidOwner, requestUsername, requestAppname, data.getAppId(), data.getUsername(), getElementNames(ownsDelega), sourceRequest, variableName, motivation,
							accesstype.toString(), domain.toString(), null);
					activityRepo.save(delegated);// save delegated
					ht.put(data.getAppId(), data.getUsername());// insert this delegated so it will not be inserted anymore
				}
			}
	}

	@Override
	public void saveActivityFromUsername(String username, List<Data> delegatedDatas, String sourceRequest, String variableName, String motivation, ActivityAccessType accesstype, ActivityDomainType domain) {
		logger.debug("saveActivityFromUsername INVOKED on username {}", username);

		// owner of the activity
		Activity owner = new Activity(new Date(), username, sourceRequest, variableName, motivation, accesstype.toString(), domain.toString(), null);
		activityRepo.save(owner);// save owner

		Hashtable<String, String> ht = new Hashtable<String, String>();

		if (delegatedDatas != null)
			for (Data data : delegatedDatas) {
				if ((username != data.getUsername()) && (data.getAppId() != null) && (ht.get(data.getAppId()) == null)) {

					List<Ownership> ownsDelega = ownershipRepo.findByElementId(data.getAppId());

					Activity delegated = new Activity(new Date(), null, username, username, data.getAppId(), data.getUsername(), getElementNames(ownsDelega), sourceRequest, variableName, motivation, accesstype.toString(), domain.toString(),
							null);
					activityRepo.save(delegated);// save delegated
					ht.put(data.getAppId(), data.getUsername());// insert this delegated so it will not be inserted anymore
				}
			}
	}

	@Override
	public void saveActivityDelegationFromUsername(String username, String usernameDelegator, String sourceRequest, String variableName, String motivation, ActivityAccessType accesstype, ActivityDomainType domain) {
		logger.debug("saveActivityDelegationFromUsername INVOKED on user {}", username);

		Activity owner = new Activity(new Date(), null, username, null, null, usernameDelegator, null, sourceRequest, variableName, motivation, accesstype.toString(), domain.toString(), null);
		activityRepo.save(owner);// save owner
	}

	@Override
	public void saveActivityDelegationFromAppId(String requestAppidOwner, String usernameDelegator, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, ActivityDomainType domain) {
		logger.debug("saveActivityDelegationFromAppId INVOKED on requestAppidOwner {}", requestAppidOwner);

		// owner of the activity
		List<Ownership> owns = ownershipRepo.findByElementId(requestAppidOwner);
		Activity owner = new Activity(new Date(), requestAppidOwner, getUsernames(owns), getElementNames(owns), null, usernameDelegator, null, sourceRequest, variableName, motivation, accessType.toString(), domain.toString(), null);

		activityRepo.save(owner);// save owner
	}

	@Override
	public void saveActivityViolationFromAppId(String appId, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, String queryString, String message, Throwable stacktrace, String ipAddress) {
		logger.debug("saveActivityViolationFromAppId INVOKED on requestAppidOwner {}", appId);

		// owner of the activity
		List<Ownership> owns = ownershipRepo.findByElementId(appId);

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if (stacktrace != null)
			stacktrace.printStackTrace(pw);

		try {
			ActivityViolation av = new ActivityViolation(new Date(), appId, getElementNames(owns), null, sourceRequest, variableName, motivation, accessType.toString(), queryString, message, new SerialBlob(sw.toString().getBytes()),
					ipAddress);
			activityViolationRepo.save(av);
		} catch (SerialException e) {
			logger.error("SerialException {}", e);
		} catch (SQLException e) {
			logger.error("SQLException {}", e);
		}
	}

	@Override
	public void saveActivityViolationFromUsername(String username, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, String queryString, String message, Throwable stacktrace, String ipAddress) {
		logger.debug("saveActivityViolationFromUsername INVOKED on user {}", username);

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if (stacktrace != null)
			stacktrace.printStackTrace(pw);

		try {
			ActivityViolation av = new ActivityViolation(new Date(), null, null, username, sourceRequest, variableName, motivation, accessType.toString(), queryString, message, new SerialBlob(sw.toString().getBytes()), ipAddress);
			activityViolationRepo.save(av);
		} catch (SerialException e) {
			logger.error("SerialException {}", e);
		} catch (SQLException e) {
			logger.error("SQLException {}", e);
		}
	}

	@Override
	public void saveActivityViolation(String appId, String username, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, String queryString, String message, Throwable stacktrace,
			String ipAddress) {
		logger.debug("saveActivityViolation INVOKED on requestAppidOwner {} user {}", appId, username);

		// owner of the activity
		List<Ownership> owns = ownershipRepo.findByElementId(appId);

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if (stacktrace != null)
			stacktrace.printStackTrace(pw);

		try {
			ActivityViolation av = new ActivityViolation(new Date(), appId, getElementNames(owns), username, sourceRequest, variableName, motivation, accessType.toString(), queryString, message, new SerialBlob(sw.toString().getBytes()),
					ipAddress);
			activityViolationRepo.save(av);
		} catch (SerialException e) {
			logger.error("SerialException {}", e);
		} catch (SQLException e) {
			logger.error("SQLException {}", e);
		}
	}

	private String getUsernames(List<Ownership> ownerships) {

		if (ownerships.size() == 0)
			logger.debug("Empty ownership retrieved");

		String usernames = "";
		for (int i = 0; i < ownerships.size(); i++) {
			usernames = usernames + ownerships.get(i).getUsername();
			if (i != (ownerships.size() - 1)) {
				usernames = usernames + ",";
			}
		}
		return usernames;
	}

	private String getElementNames(List<Ownership> ownerships) {

		if (ownerships.size() == 0)
			logger.debug("Empty ownership retrieved");

		String elementNames = "";
		for (int i = 0; i < ownerships.size(); i++) {
			elementNames = elementNames + ownerships.get(i).getElementName();
			if (i != (ownerships.size() - 1)) {
				elementNames = elementNames + ",";
			}
		}
		return elementNames;
	}
}
