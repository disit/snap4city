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

import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.exception.DelegationNotFoundException;
import edu.unifi.disit.datamanager.exception.CredentialsException;

public interface IDataService {

	List<Data> getDataFromApp(String appId, Boolean delegated, String variableName, String motivation, Date from, Date to, Integer first, Integer last, Boolean anonymous, String appOwner, Locale lang)
			throws  DelegationNotFoundException, DataNotValidException, CredentialsException;

	List<Data> getDataFromUser(String username, Boolean delegated, String variableName, String motivation, Date from, Date to, Integer first, Integer last, Boolean anonymous, Locale lang)
			throws  DataNotValidException, DelegationNotFoundException, CredentialsException;

	Data postDataFromApp(String appId, Data data, Locale lang) throws DataNotValidException,  CredentialsException;

	Data postDataFromUser(String username, Data data, Locale lang) throws DataNotValidException, CredentialsException;

	List<Data> getAllData(Boolean last, Locale lang) throws  CredentialsException;

	void deleteDataFromUser(String username, Long dataId, Locale lang) throws CredentialsException, DataNotValidException;

	List<Data> getPublicData(String variableName, String motivation, Date from, Date to, Integer first, Integer last, Locale lang) throws DataNotValidException;
}