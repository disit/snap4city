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

import java.util.List;
import java.util.Locale;

import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;

public interface IDelegationService {

	// boolean check(String appId, Integer uidDelegator, String valueType, String motivation);

	Response checkDelegations(String username, String variableName, String elementID, Locale lang);

	List<Delegation> getDelegationsDelegatedFromApp(String appId, String variableName, String motivation, Locale lang);

	List<Delegation> getDelegationsDelegatorFromApp(String appId, String variableName, String motivation, Locale lang);

	List<Delegation> getDelegationsDelegatedForUsername(String username, String variableName, String motivation, Locale lang);

	List<Delegation> getDelegationsDelegatorForUsername(String username, String variableName, String motivation, Locale lang);

	Delegation postDelegationFromUser(String username, Delegation delegation, Locale lang) throws DelegationNotValidException;

	void deleteDelegationFromUser(String username, Long delegationId, Locale lang) throws DelegationNotValidException;

	void deleteAllDelegationFromApp(String appId, Locale lang);

	Delegation putDelegationFromUser(String username, Delegation delegation, Long delegationId, Locale lang) throws DelegationNotValidException;
}