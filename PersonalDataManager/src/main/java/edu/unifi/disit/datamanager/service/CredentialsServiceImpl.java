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

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.UserRolesType;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;

@Service
public class CredentialsServiceImpl implements ICredentialsService {

	// private static final Logger logger = LogManager.getLogger();

	@Autowired
	private MessageSource messages;

	@Autowired
	OwnershipDAO ownershipRepo;

	@SuppressWarnings("unchecked")
	@Override
	public void checkAppIdCredentials(String appId, Locale lang) throws NoSuchMessageException, CredentialsException {
		List<String> roles = (List<String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();
		String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		// assure the username is included in the list of the ownership of this appid
		List<Ownership> owns = ownershipRepo.findByElementId(appId);
		boolean found = false;
		for (Ownership own : owns)
			if (own.getUsername().equals(username))
				found = true;
		if ((found == false) && (!roles.contains(UserRolesType.RootAdmin.toString())))
			throw new CredentialsException(messages.getMessage("credentials.ko.appidowner", null, lang));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void checkUsernameCredentials(String username, Locale lang) throws NoSuchMessageException, CredentialsException {
		List<String> roles = (List<String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();
		String usernameSC = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		// assure the username is the same than the specified one
		if ((!usernameSC.equals(username)) && (!roles.contains(UserRolesType.RootAdmin.toString())))
			throw new CredentialsException(messages.getMessage("credentials.ko.usernameowner", null, lang));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void checkRootCredentials(Locale lang) throws NoSuchMessageException, CredentialsException {
		List<String> roles = (List<String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();
		// assure role is RootAdmin
		if (!roles.contains(UserRolesType.RootAdmin.toString()))
			throw new CredentialsException(messages.getMessage("credentials.ko.rights", null, lang));
	}

	@Override
	public String getLoggedUsername(Locale lang) {
		return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isRoot(Locale lang) {
		List<String> roles = (List<String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();

		return (roles.contains(UserRolesType.RootAdmin.toString()));
	}
}
