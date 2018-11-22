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

import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DelegationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;

@Service
public class AccessServiceImpl implements IAccessService {

	@Autowired
	LDAPUserDAO lu;

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	DelegationDAO delegationRepo;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Autowired
	ICredentialsService credentialsService;

	@Override
	public Response checkAccessFromApp(String elementID, Locale lang) throws CredentialsException {
		logger.debug("checkDelegations INVOKED on elementId {} ", elementID);

		Response response = new Response(false, null);

		// if the AT is from a RootAdmin
		if (credentialsService.isRoot(lang)) {
			response.setResult(true);
			response.setMessage("ROOTADMIN");
			return response;
		}

		String loggedUserName = credentialsService.getLoggedUsername(lang);

		// if the AT is from the owner
		String strippedElementID = elementID;
		if (elementID.startsWith("http://") || elementID.startsWith("https://")) {// to enable iotdirectory scenario
			strippedElementID = elementID.substring(elementID.lastIndexOf("/") + 1);
			logger.debug("strippedElementID is: {}", strippedElementID);
		}

		List<Ownership> owns = ownershipRepo.findByElementId(strippedElementID);
		for (Ownership own : owns)
			if (own.getUsername().equals(loggedUserName)) {
				response.setResult(true);
				response.setMessage("OWNER");
				return response;
			}

		List<String> groupnames = lu.getGroupAndOUnames(loggedUserName);

		// check delegation
		List<Delegation> mydelegations = delegationRepo.getDelegationDelegatorFromAppId(elementID, null, null, false);

		for (Delegation d : mydelegations) {
			if ((d.getUsernameDelegated() != null) && (d.getUsernameDelegated().equals("ANONYMOUS"))) {
				response.setResult(true);
				response.setMessage("PUBLIC");
				return response;
			}

			if ((d.getUsernameDelegated() != null) && (d.getUsernameDelegated().equals(loggedUserName))) {
				response.setResult(true);
				response.setMessage("DELEGATED");
				return response;
			}

			if ((d.getGroupnameDelegated() != null) && (groupnames.contains(d.getGroupnameDelegated()))) {
				response.setResult(true);
				response.setMessage("GROUP-DELEGATED");
				return response;
			}
		}

		return response;
	}

}
