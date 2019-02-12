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

import java.util.Locale;

import org.springframework.context.NoSuchMessageException;

import edu.unifi.disit.datamanager.exception.CredentialsException;

public interface ICredentialsService {

	public void checkAppIdCredentials(String appId, Locale lang) throws NoSuchMessageException, CredentialsException;

	public void checkUsernameCredentials(String username, Locale lang) throws NoSuchMessageException, CredentialsException;

	public void checkRootCredentials(Locale lang) throws NoSuchMessageException, CredentialsException;

	public String getLoggedUsername(Locale lang);

	public boolean isRoot(Locale lang);

	public String getOrganization(Locale lang);
}
