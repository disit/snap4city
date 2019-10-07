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

import java.util.ArrayList;
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

import edu.unifi.disit.datamanager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.datamodel.profiledb.DataDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.exception.DelegationNotFoundException;

@Service
public class DataServiceImpl implements IDataService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private MessageSource messages;

	@Autowired
	LDAPUserDAO lu;

	@Autowired
	DataDAO dataRepo;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	ICredentialsService credentialsService;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Data> getDataFromApp(String appId, Boolean delegated, String variableName, String motivation, Date from, Date to, Integer first, Integer last, Boolean anonymous, String appOwner, Locale lang)
			throws NoSuchMessageException, DelegationNotFoundException, DataNotValidException, CredentialsException {
		logger.debug("getDataFromApp INVOKED on appId {}, delegated {}, variablename {}, motivation {}, from {}, to {}, first {}, last {}, anonymous {}, appOwner {}", appId, delegated, variableName, motivation, from, to, first, last,
				anonymous, appOwner);

		credentialsService.checkAppIdCredentials(appId, lang);// enforcement credentials

		if ((first != 0) && (last != 0))
			throw new DataNotValidException(messages.getMessage("getdata.ko.firstandlastspecified", null, lang));

		List<Data> toreturn = new ArrayList<Data>();

		if (appOwner == null) {// to enable multi ownership
			// retrieve ownership of this appId, if appOwner is not specified
			List<Ownership> owns = ownershipRepo.findByElementId(appId);

			if (owns.size() == 0)
				throw new DataNotValidException(messages.getMessage("getdata.ko.appidnotrecognized", null, lang));
			else if (owns.size() > 1)
				throw new DataNotValidException(messages.getMessage("getdata.ko.multipleownershipdetected", null, lang));

			appOwner = owns.get(0).getUsername();
		}

		if (!delegated) {
			// returning my data-----------------------------------------------------------------------------------
			toreturn = dataRepo.getDataByAppId(appId, appOwner, variableName, motivation, from, to, first, last);
		} else {
			// returning delegator data----------------------------------------------------------------------------
			if (anonymous) {
				toreturn = anonymize(dataRepo.getDataByUsernameDelegated("ANONYMOUS", variableName, motivation, from, to, first, last));
			} else {
				toreturn = dataRepo.getDataByUsernameDelegated(appOwner, variableName, motivation, from, to, first, last);
			}
		}

		return toreturn;
	}

	@Override
	// similar to below
	public Data postDataFromApp(String appId, Data data, Locale lang) throws DataNotValidException, NoSuchMessageException, CredentialsException {
		logger.debug("postData from app {} INVOKED on {}", appId, data);

		credentialsService.checkAppIdCredentials(appId, lang);// enforcement credentials

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
		List<Ownership> iotapps = ownershipRepo.findByElementId(appId);
		if (iotapps.size() == 0)
			throw new DataNotValidException(messages.getMessage("postdata.ko.appidnotrecognized", null, lang));

		data.setAppName(iotapps.get(0).getElementName());

		// check if the username is null, eventually popolate
		if (data.getUsername() == null) {
			if (iotapps.size() > 1) {
				throw new DataNotValidException(messages.getMessage("postdata.ko.multipleownershipdetected", null, lang));
			}
			data.setUsername(iotapps.get(0).getUsername());
		}

		// override insertTime, deleteTime, elapseTime + appName
		data.setInsertTime(new Date());
		data.setDeleteTime(null);
		data.setElapseTime(null);

		return dataRepo.save(data);
	}

	@Override
	// similar to above
	public Data postDataFromUser(String username, Data data, Locale lang) throws DataNotValidException, CredentialsException {
		logger.debug("postData from user {} INVOKED on {}", username, data);

		credentialsService.checkUsernameCredentials(username, lang);// enforcement credentials

		// check if id is specified
		// if (data.getId() != null)
		// throw new DataNotValidException(messages.getMessage("postdata.ko.idnotnull", null, lang));

		// check if the username is null, eventually populate
		if (data.getUsername() == null)
			data.setUsername(username);
		// check if the username is different
		else if (!data.getUsername().equals(username))
			throw new DataNotValidException(messages.getMessage("postdata.ko.usernamedifferent", null, lang));

		// // check username exist

		// List<Ownership> ownership = ownershipRepo.findByUsername(username);
		// if ((ownership == null) || (ownership.size() == 0))
		if (!lu.usernameExist(username))
			throw new DataNotValidException(messages.getMessage("postdata.ko.usernamenotrecognized", null, lang));

		// override insertTime, deleteTime, elapseTime + appName
		data.setInsertTime(new Date());
		data.setDeleteTime(null);
		data.setElapseTime(null);

		return dataRepo.save(data);
	}

	@Override
	public List<Data> getDataFromUser(String username, Boolean delegated, String variableName, String motivation, Date from, Date to, Integer first, Integer last, Boolean anonymous, Locale lang)
			throws NoSuchMessageException, DataNotValidException, DelegationNotFoundException, CredentialsException {
		logger.debug("getDataFromUser INVOKED on username {}, delegated {}, variablename {}, motivation {}, from {}, to {}, first {}, last {}, anonymous {}", username, delegated, variableName, motivation, from, to, first, last, anonymous);

		credentialsService.checkUsernameCredentials(username, lang);// enforcement credentials

		if ((first != 0) && (last != 0))
			throw new DataNotValidException(messages.getMessage("getdata.ko.firstandlastspecified", null, lang));

		List<Data> toreturn = new ArrayList<Data>();

		if (!delegated) {
			// returning my data-----------------------------------------------------------------------------------
			toreturn = dataRepo.getDataByUsername(username, variableName, motivation, from, to, first, last);
		} else {
			// returning delegator data----------------------------------------------------------------------------
			if (anonymous) {
				toreturn = anonymize(dataRepo.getDataByUsernameDelegated("ANONYMOUS", variableName, motivation, from, to, first, last));
			} else {
				toreturn = dataRepo.getDataByUsernameDelegated(username, variableName, motivation, from, to, first, last);
			}
		}

		return toreturn;
	}

	@Override
	public List<Data> getAllData(Boolean last, Locale lang) throws NoSuchMessageException, CredentialsException {
		logger.debug("getAllData INVOKED on last {}", last);

		credentialsService.checkRootCredentials(lang);// enforcement credentials

		List<Data> toreturn = new ArrayList<Data>();

		if (last)
			toreturn = dataRepo.findLastData();
		else
			toreturn = dataRepo.findAllAndDeleteTimeIsNullByOrderByDataTimeAsc();

		return toreturn;
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
	public void deleteDataFromUser(String username, Long dataId, Locale lang) throws DataNotValidException, CredentialsException {
		logger.debug("deleteDataFromUser INVOKED on username {}, dataId {} {}", username, dataId, lang);

		credentialsService.checkUsernameCredentials(username, lang);// enforcement credentials

		Data todelete = dataRepo.findById(dataId);

		if (todelete == null)
			throw new DataNotValidException(messages.getMessage("deletedata.ko.dataidnotfound", null, lang));

		todelete.setDeleteTime(new Date());

		dataRepo.saveAndFlush(todelete);
	}

	@Override
	public List<Data> getPublicData(String variableName, String motivation, Date from, Date to, Integer first, Integer last, Locale lang) throws DataNotValidException {
		logger.debug("getPublicData INVOKED variablename {}, motivation {}, from {}, to {}, first {}, last {}, anonymous {}", variableName, motivation, from, to, first, last);

		// no credentials enforcement needed

		if ((first != 0) && (last != 0))
			throw new DataNotValidException(messages.getMessage("getdata.ko.firstandlastspecified", null, lang));

		return anonymize(dataRepo.getDataByUsernameDelegated("ANONYMOUS", variableName, motivation, from, to, first, last));
	}
}