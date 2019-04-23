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
import org.springframework.context.NoSuchMessageException;
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

	@Autowired
	IDelegationService delegationService;

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
		List<Delegation> mydelegations = delegationRepo.getDelegationDelegatorFromAppId(elementID, null, null, false, null);

		for (Delegation d : mydelegations) {
			if ((d.getUsernameDelegated() != null) && (d.getUsernameDelegated().equals("ANONYMOUS"))) {
				response.setResult(true);
				response.setMessage("PUBLIC");
				return response;
			}

			if ((d.getUsernameDelegated() != null) && (d.getUsernameDelegated().toLowerCase().equals(loggedUserName.toLowerCase()))) {
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

	@Override
	// the owner of the elementId, specified in the accesstoken, can check if another user has been delegated to access elementId
	public Response checkDelegationsFromUsername(String username, String variableName, String elementID, Locale lang) throws NoSuchMessageException, CredentialsException {
		logger.debug("checkDelegations INVOKED on username {} variableName {} elementId {} ", username, variableName, elementID);

		credentialsService.checkAppIdCredentials(elementID, lang);

		Response response = new Response(false, null);

		List<String> groupnames = lu.getGroupAndOUnames(username);

		// check delegation
		List<Delegation> mydelegations = delegationService.getDelegationsDelegatorFromApp(elementID, variableName, null, false, null, lang);

		for (Delegation d : mydelegations) {
			if ((d.getUsernameDelegated() != null) && (d.getUsernameDelegated().equals("ANONYMOUS"))) {
				response.setResult(true);
				response.setMessage("PUBLIC");
				return response;
			}

			if ((d.getUsernameDelegated() != null) && (d.getUsernameDelegated().equals(username))) {
				response.setResult(true);
				response.setMessage("DELEGATED");
				return response;
			}

			if (groupnames.contains(d.getGroupnameDelegated())) {
				response.setResult(true);
				response.setMessage("GROUP-DELEGATED");
				return response;
			}
		}

		return response;
	}

}
