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

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.ActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.profiledb.Activity;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.exception.CredentialsException;

public interface IActivityService {

	List<Activity> getActivities(String appId, String elementType, Boolean delegated, Locale lang) throws CredentialsException;

	void saveActivityFromApp(String requestOwner, List<Data> datas, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, ActivityDomainType domain);

	void saveActivityFromUsername(String username, List<Data> datas, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, ActivityDomainType domain);

	void saveActivityDelegationFromUsername(String username, String usernameDelegator, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, ActivityDomainType domain);

	void saveActivityDelegationFromAppId(String appId, String usernameDelegator, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, ActivityDomainType domain);

	void saveActivityViolationFromAppId(String appId, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, String queryString, String message, Throwable stacktrace, String ipAddress);

	void saveActivityViolationFromUsername(String username, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, String queryString, String message, Throwable stacktrace, String ipAddress);

	void saveActivityViolation(String appId, String username, String sourceRequest, String variableName, String motivation, ActivityAccessType accessType, String queryString, String message, Throwable stacktrace, String ipAddress);
}
