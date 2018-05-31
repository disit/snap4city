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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.drupaluser.DrupalUserDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.datamodel.profiledb.DataDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.exception.DelegationNotFoundException;

@Service
public class DataServiceImpl implements IDataService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private MessageSource messages;

	@Autowired
	DataDAO dataRepo;

	@Autowired
	// IOTApplicationDAO iotapplicationRepo;
	OwnershipDAO ownershipRepo;

	@Autowired
	DrupalUserDAO drupalUserRepo;

	@Autowired
	IDelegationService delegationService;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Data> getDataFromApp(String appId, Boolean delegated, String variableName, String motivation, Date from, Date to, Integer first, Integer last, Boolean anonymous, Locale lang)
			throws NoSuchMessageException, DelegationNotFoundException, DataNotValidException {
		logger.debug("getDataFromApp INVOKED on appId {}, delegated {}, variablename {}, motivation {}, from {}, to {}, first {}, last {}, anonymous {}", appId, delegated, variableName, motivation, from, to, first, last, anonymous);

		List<Data> toreturn = new ArrayList<Data>();

		// retrieve ownership of this appId
		Ownership ownership = ownershipRepo.findByElementId(appId);

		if (ownership == null)
			throw new DelegationNotFoundException(messages.getMessage("postdata.ko.appidnotrecognized", null, lang));

		if (!delegated) {
			// returning my data-----------------------------------------------------------------------------------
			toreturn = getData(appId, ownership.getUsername(), variableName, motivation, from, to, first, last, lang);

		} else {
			logger.debug("logging for data from delegator ");

			// returning delegator data----------------------------------------------------------------------------
			String targetUserName;

			if (anonymous)
				targetUserName = "ANONYMOUS";
			else
				targetUserName = ownership.getUsername();

			List<Delegation> dus = delegationService.getDelegationsDelegatedForUsername(targetUserName, variableName, motivation, lang);

			// adding from every delegation
			for (Delegation du : dus) {
				// returning data from delegator appid-----------------------------------------------------------------------------------
				List<Data> d = getData(du.getElementId(), du.getUsernameDelegator(), variableName, motivation, from, to, first, last, lang);
				toreturn = addWithoutDuplicate(toreturn, d);
			}

			// order and filter the last/first
			toreturn = sortAndFilter(toreturn, first, last);

			if (anonymous)
				toreturn = anonymize(toreturn);

		}

		return toreturn;
	}

	@Override
	// similar to below
	public Data postDataFromApp(String appId, Data data, Locale lang) throws DataNotValidException {
		logger.debug("postData from app {} INVOKED on {}", appId, data);

		// check if id is specified
		if (data.getId() != null)
			throw new DataNotValidException(messages.getMessage("postdata.ko.idnotnull", null, lang));

		// check if the appid is null, eventually populate
		if (data.getAppId() == null)
			data.setAppId(appId);
		// check if the appid is different
		else if (!data.getAppId().equals(appId))
			throw new DataNotValidException(messages.getMessage("postdata.ko.appiddifferent", null, lang));

		// check appid exist (and popolate appname)
		Ownership iotapp = ownershipRepo.findByElementId(appId);
		if (iotapp == null)
			throw new DataNotValidException(messages.getMessage("postdata.ko.appidnotrecognized", null, lang));
		else
			data.setAppName(iotapp.getElementName());

		// check if the username is null, eventually popolate
		if (data.getUsername() == null) {
			data.setUsername(iotapp.getUsername());
		}

		// override insertTime, deleteTime, elapseTime + appName
		data.setInsertTime(new Date());
		data.setDeleteTime(null);
		data.setElapseTime(null);

		return dataRepo.save(data);
	}

	@Override
	// similar to above
	public Data postDataFromUser(String username, Data data, Locale lang) throws DataNotValidException {
		logger.debug("postData from user {} INVOKED on {}", username, data);

		// check if id is specified
		if (data.getId() != null)
			throw new DataNotValidException(messages.getMessage("postdata.ko.idnotnull", null, lang));

		// check if the username is null, eventually populate
		if (data.getUsername() == null)
			data.setUsername(username);
		// check if the username is different
		else if (!data.getUsername().equals(username))
			throw new DataNotValidException(messages.getMessage("postdata.ko.usernamedifferent", null, lang));

		// check username exist
		List<Ownership> ownership = ownershipRepo.findByUsername(username);
		if ((ownership == null) || (ownership.size() == 0))
			throw new DataNotValidException(messages.getMessage("postdata.ko.usernamenotrecognized", null, lang));

		// override insertTime, deleteTime, elapseTime + appName
		data.setInsertTime(new Date());
		data.setDeleteTime(null);
		data.setElapseTime(null);

		return dataRepo.save(data);
	}

	// it does not return the Data with DeletedTime!=null for both scenario
	// // it does not return the Data with containerId!=null for anonymous scenario
	private List<Data> getData(String appId, String username, String variableName, String motivation, Date from, Date to, Integer first, Integer last, Locale lang) throws NoSuchMessageException, DataNotValidException {

		if ((first != 0) && (last != 0))
			throw new DataNotValidException(messages.getMessage("getdata.ko.firstandlastspecified", null, lang));

		List<Data> toreturn = null;

		if (from == null)
			from = new Date(0);// lower limit not specified, using 01-01-1970
		if (to == null)
			to = new Date(4102448400000l);// upper limit not specified, using 01-01-2100

		if (variableName == null) {
			if (motivation == null) {
				if (last == 0) {
					if (first == 0) {// variableName=null, motivation=null, last=0, first=0-------------------------------------------------------------------------------------------------------------
						toreturn = dataRepo.findByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNull(username, from, to);// annotations

						if (appId != null)
							toreturn = add(toreturn, dataRepo.findByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNull(appId, from, to));
					} else {// variableName=null, motivation=null, last=0, first!=0---------------------------------------------------------------------------------------------------------------------
						toreturn = dataRepo.findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullOrderByDataTimeAsc(username, from, to);// annotations

						if (appId != null)
							toreturn = add(toreturn, dataRepo.findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullOrderByDataTimeAsc(appId, from, to));

						if (toreturn.size() >= first)
							toreturn = toreturn.subList(0, first);
					}
				} else {// variableName=null, motivation=null, last!=0---------------------------------------------------------------------------------------------------------------------------------
					toreturn = dataRepo.findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullOrderByDataTimeDesc(username, from, to);// annotations

					if (appId != null)
						toreturn = add(toreturn, dataRepo.findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullOrderByDataTimeDesc(appId, from, to));

					if (toreturn.size() >= last)
						toreturn = toreturn.subList(toreturn.size() - last, toreturn.size());
				}
			} else {
				if (last == 0) {
					if (first == 0) {// variableName=null, motivation!=null, last=0, first=0-------------------------------------------------------------------------------------------------------------
						toreturn = dataRepo.findByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndMotivation(username, from, to, motivation);// annotations

						if (appId != null)
							toreturn = add(toreturn, dataRepo.findByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndMotivation(appId, from, to, motivation));
					} else {// variableName=null, motivation!=null, last=0, first!=0-------------------------------------------------------------------------------------------------------------
						toreturn = dataRepo.findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndMotivationOrderByDataTimeAsc(username, from, to, motivation);// annotations

						if (appId != null)
							toreturn = add(toreturn, dataRepo.findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndMotivationOrderByDataTimeAsc(appId, from, to, motivation));

						if (toreturn.size() >= first)
							toreturn = toreturn.subList(0, first);
					}
				} else {// variableName=null, motivation!=null, last!=0-------------------------------------------------------------------------------------------------------------
					toreturn = dataRepo.findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndMotivationOrderByDataTimeDesc(username, from, to, motivation);// annotations

					if (appId != null)
						toreturn = add(toreturn, dataRepo.findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndMotivationOrderByDataTimeDesc(appId, from, to, motivation));

					if (toreturn.size() >= last)
						toreturn = toreturn.subList(toreturn.size() - last, toreturn.size());
				}
			}
		} else {
			if (motivation == null) {
				if (last == 0) {
					if (first == 0) {// variableName!=null, motivation=null, last=0, first=0-------------------------------------------------------------------------------------------------------------
						toreturn = dataRepo.findByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableName(username, from, to, variableName);

						if (appId != null)
							toreturn = add(toreturn, dataRepo.findByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableName(appId, from, to, variableName));
					} else {// variableName!=null, motivation=null, last=0, first=!0-------------------------------------------------------------------------------------------------------------
						toreturn = dataRepo.findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameOrderByDataTimeAsc(username, from, to, variableName);

						if (appId != null)
							toreturn = add(toreturn, dataRepo.findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameOrderByDataTimeAsc(appId, from, to, variableName));

						if (toreturn.size() >= first)
							toreturn = toreturn.subList(0, first);
					}
				} else {// variableName!=null, motivation=null, last!=0-------------------------------------------------------------------------------------------------------------
					toreturn = dataRepo.findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameOrderByDataTimeDesc(username, from, to, variableName);

					if (appId != null)
						toreturn = add(toreturn, dataRepo.findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameOrderByDataTimeDesc(appId, from, to, variableName));

					if (toreturn.size() >= last)
						toreturn = toreturn.subList(toreturn.size() - last, toreturn.size());
				}
			} else {
				if (last == 0) {
					if (first == 0) {// variableName!=null, motivatio!n=null, last=0, first=0-------------------------------------------------------------------------------------------------------------
						toreturn = dataRepo.findByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameAndMotivation(username, from, to, variableName, motivation);

						if (appId != null)
							toreturn = add(toreturn, dataRepo.findByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameAndMotivation(appId, from, to, variableName, motivation));
					} else {// variableName!=null, motivatio!n=null, last=0, first!=0-------------------------------------------------------------------------------------------------------------
						toreturn = dataRepo.findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameAndMotivationOrderByDataTimeAsc(username, from, to, variableName, motivation);

						toreturn = add(toreturn, dataRepo.findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameAndMotivationOrderByDataTimeAsc(appId, from, to, variableName, motivation));

						if (toreturn.size() >= first)
							toreturn = toreturn.subList(0, first);
					}
				} else {// variableName!=null, motivatio!n=null, last!=0-------------------------------------------------------------------------------------------------------------
					toreturn = dataRepo.findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameAndMotivationOrderByDataTimeDesc(username, from, to, variableName, motivation);

					if (appId != null)
						toreturn = add(toreturn, dataRepo.findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameAndMotivationOrderByDataTimeDesc(appId, from, to, variableName, motivation));
					if (toreturn.size() >= last)
						toreturn = toreturn.subList(toreturn.size() - last, toreturn.size());
				}
			}
		}

		return toreturn;
	}

	private List<Data> add(List<Data> data1, List<Data> data2) {
		data1.addAll(data2);
		Collections.sort(data1);
		return data1;
	}

	private List<Data> sortAndFilter(List<Data> toreturn, Integer first, Integer last) {
		Collections.sort(toreturn);

		if (first != 0) {
			if (toreturn.size() >= first)
				return toreturn.subList(0, first);
		}

		if (last != 0) {
			if (toreturn.size() >= last)
				return toreturn.subList(toreturn.size() - last, toreturn.size());
		}

		return toreturn;
	}

	private List<Data> addWithoutDuplicate(List<Data> toreturns, List<Data> toinserts) {
		for (Data toinsert : toinserts) {
			boolean found = false;
			for (Data toreturn : toreturns) {
				if (toinsert.getId() == toreturn.getId()) {
					found = true;
					break;
				}
			}
			if (!found)
				toreturns.add(toinsert);
		}
		return toreturns;
	}

	private List<Data> anonymize(List<Data> toreturn) {
		for (Data data : toreturn) {

			entityManager.detach(data);

			data.setAppId(null);
			data.setAppName(null);
			data.setUsername(null);
		}
		return toreturn;
	}

	@Override
	public List<Data> getDataFromUser(String username, Boolean delegated, String variableName, String motivation, Date from, Date to, Integer first, Integer last, Boolean anonymous, Locale lang)
			throws NoSuchMessageException, DataNotValidException, DelegationNotFoundException {
		logger.debug("getDataFromUser INVOKED on appId {}, delegated {}, variablename {}, motivation {}, from {}, to {}, anonymous {}", username, delegated, variableName, motivation, from, to, anonymous);

		List<Data> toreturn = new ArrayList<Data>();

		if ((first != 0) && (last != 0))
			throw new DataNotValidException(messages.getMessage("getdata.ko.firstandlastspecified", null, lang));

		List<Ownership> ownerships = ownershipRepo.findByUsername(username);

		for (Ownership o : ownerships) {
			toreturn = addWithoutDuplicate(toreturn, getDataFromApp(o.getElementId(), delegated, variableName, motivation, from, to, 0, 0, anonymous, lang));
		}

		return sortAndFilter(toreturn, first, last);
	}

	@Override
	public List<Data> getAllData(Locale lang) {
		return dataRepo.findAllAndDeleteTimeIsNullByOrderByDataTimeAsc();
	}

}