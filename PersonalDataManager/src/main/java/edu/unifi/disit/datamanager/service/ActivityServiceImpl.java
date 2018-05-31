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
package edu.unifi.disit.datamanager.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

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

@Service
public class ActivityServiceImpl implements IActivityService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	ActivityDAO activityRepo;

	@Autowired
	ActivityViolationDAO activityViolationRepo;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Override
	public void saveActivityFromApp(String requestAppidOwner, List<Data> delegatedDatas, String sourceRequest, String variableName, String motivation, ActivityAccessType accesstype, ActivityDomainType domain) {
		logger.debug("Save activity for requestAppidOwner {}", requestAppidOwner);
		logger.debug("data are");
		if (delegatedDatas != null)
			for (Data data : delegatedDatas)
				logger.debug("{}", data);
		if (sourceRequest != null)
			logger.debug("sourceRequest is {}", sourceRequest);

		// owner of the activity
		Ownership own = ownershipRepo.findByElementId(requestAppidOwner);
		String requestUsername = own.getUsername();
		String requestAppname = own.getElementName();

		Activity owner = new Activity(new Date(), requestAppidOwner, requestUsername, requestAppname, sourceRequest, variableName, motivation, accesstype.toString(), domain.toString(), null);
		activityRepo.save(owner);// save owner

		Hashtable<String, String> ht = new Hashtable<String, String>();

		ht.put(requestAppidOwner, requestUsername);// insert owner so it will not be inserted anymore

		if (delegatedDatas != null)
			for (Data data : delegatedDatas) {
				if ((data.getAppId() != null) && (ht.get(data.getAppId()) == null)) {

					Ownership ownDelega = ownershipRepo.findByElementId(data.getAppId());

					Activity delegated = new Activity(new Date(), requestAppidOwner, requestUsername, requestAppname, data.getAppId(), data.getUsername(), ownDelega.getElementName(), sourceRequest, variableName, motivation,
							accesstype.toString(), domain.toString(), null);
					activityRepo.save(delegated);// save delegated
					ht.put(data.getAppId(), data.getUsername());// insert this delegated so it will not be inserted anymore
				}
			}
	}

	@Override
	public void saveActivityFromUsername(String username, List<Data> delegatedDatas, String sourceRequest, String variableName, String motivation, ActivityAccessType accesstype, ActivityDomainType domain) {
		logger.debug("Save activityFromUsername for requestAppidOwner {}", username);
		// logger.debug("data are");
		// if (delegatedDatas != null)
		// for (Data data : delegatedDatas)
		// logger.debug("{}", data);

		// owner of the activity
		Activity owner = new Activity(new Date(), username, sourceRequest, variableName, motivation, accesstype.toString(), domain.toString(), null);
		activityRepo.save(owner);// save owner

		Hashtable<String, String> ht = new Hashtable<String, String>();

		if (delegatedDatas != null)
			for (Data data : delegatedDatas) {
				if ((username != data.getUsername()) && (data.getAppId() != null) && (ht.get(data.getAppId()) == null)) {

					Ownership ownDelega = ownershipRepo.findByElementId(data.getAppId());

					Activity delegated = new Activity(new Date(), null, username, username, data.getAppId(), data.getUsername(), ownDelega.getElementName(), sourceRequest, variableName, motivation, accesstype.toString(), domain.toString(),
							null);
					activityRepo.save(delegated);// save delegated
					ht.put(data.getAppId(), data.getUsername());// insert this delegated so it will not be inserted anymore
				}
			}
	}

	@Override
	public List<Activity> getActivities(String appId, Boolean delegated) {
		logger.debug("Get activities for appid {}, delegated {}", appId, delegated);

		// if delegated == true, return the delegated
		// if delegated == false, return the delegator (check appid and delegated==null)

		if (!delegated)
			return activityRepo.findByAppIdAndDelegatedAppIdIsNullAndDeleteTimeIsNull(appId);
		else
			return activityRepo.findByDelegatedAppIdAndDeleteTimeIsNull(appId);
	}

	@Override
	public void saveActivityDelegationFromUsername(String username, String usernameDelegator, String sourceRequest, String variableName, String motivation, ActivityAccessType accesstype, ActivityDomainType domain) {
		logger.debug("Save activityDelegationFromUsername for requestAppidOwner {}", username);

		Activity owner = new Activity(new Date(), null, username, null, null, usernameDelegator, null, sourceRequest, variableName, motivation, accesstype.toString(), domain.toString(), null);
		activityRepo.save(owner);// save owner
	}

	@Override
	public void saveActivityDelegationFromAppId(String requestAppidOwner, String usernameDelegator, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, ActivityDomainType domain) {
		logger.debug("Save activityDelegationFromAppId for requestAppidOwner {}", requestAppidOwner);

		// owner of the activity
		Ownership own = ownershipRepo.findByElementId(requestAppidOwner);
		String requestUsername = own.getUsername();
		String requestAppname = own.getElementName();

		Activity owner = new Activity(new Date(), requestAppidOwner, requestUsername, requestAppname, null, usernameDelegator, null, sourceRequest, variableName, motivation, accessType.toString(), domain.toString(), null);

		activityRepo.save(owner);// save owner
	}

	@Override
	public void saveActivityViolationFromAppId(String appId, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, String queryString, String message, Throwable stacktrace, String ipAddress) {
		logger.debug("Save saveActivityViolationFromAppId for requestAppidOwner {}", appId);

		// owner of the activity
		Ownership own = ownershipRepo.findByElementId(appId);
		String elementName = null;
		if (own != null)
			own.getElementName();

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if (stacktrace != null)
			stacktrace.printStackTrace(pw);

		try {
			ActivityViolation av = new ActivityViolation(new Date(), appId, elementName, null, sourceRequest, variableName, motivation, accessType.toString(), queryString, message, new SerialBlob(sw.toString().getBytes()), ipAddress);
			activityViolationRepo.save(av);
		} catch (SerialException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void saveActivityViolationFromUsername(String username, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, String queryString, String message, Throwable stacktrace, String ipAddress) {
		logger.debug("Save saveActivityViolationFromUsername for requestAppidOwner {}", username);

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if (stacktrace != null)
			stacktrace.printStackTrace(pw);

		try {
			ActivityViolation av = new ActivityViolation(new Date(), null, null, username, sourceRequest, variableName, motivation, accessType.toString(), queryString, message, new SerialBlob(sw.toString().getBytes()), ipAddress);
			activityViolationRepo.save(av);
		} catch (SerialException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}