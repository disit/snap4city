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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

//import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;
import edu.unifi.disit.datamanager.exception.LDAPException;
import org.springframework.data.domain.PageRequest;

public interface IDelegationService {

	// Response checkDelegationsFromUsername(String username, String variableName, String elementID, Locale lang) throws CredentialsException;
	//
	// Response checkDelegationsFromApp(String elementID, Locale lang) throws CredentialsException;

	List<Delegation> getDelegationsDelegatedFromApp(String appId, String variableName, String motivation, Boolean deleted, String appOwner, String groupname, String elementType, Locale lang)
			throws DelegationNotValidException, CredentialsException, LDAPException;

	List<Delegation> getDelegationsDelegatorFromApp(String appId, String variableName, String motivation, Boolean deleted, String elementType, Locale lang) throws CredentialsException;

	List<Delegation> getDelegationsDelegatedForUsername(String username, String variableName, String motivation, Boolean deleted, String groupname, String elementType, Locale lang)
			throws CredentialsException, LDAPException, CloneNotSupportedException;

	List<Delegation> getDelegationsDelegatorForUsername(String username, String variableName, String motivation, Boolean deleted, String elementType, Locale lang) throws CredentialsException;

	Delegation postDelegationFromUser(String username, Delegation delegation, Locale lang) throws DelegationNotValidException, CredentialsException;

	void deleteDelegationFromUser(String username, Long delegationId, Locale lang) throws DelegationNotValidException, CredentialsException;

	void deleteAllDelegationFromApp(String appId, String elementType, Locale lang) throws CredentialsException;

	Delegation putDelegationFromUser(String username, Delegation delegation, Long delegationId, Locale lang) throws DelegationNotValidException, CredentialsException;

	List<Delegation> stripUsernameDelegatedNull(List<Delegation> delegations);

	// Aggiunti per KPI

	Delegation getDelegationById(Long id, Locale lang) throws CredentialsException;

	Page<Delegation> findByElementId(String elementId, Pageable pageable) throws CredentialsException;

	Page<Delegation> findByElementIdWithoutAnonymous(String elementId, Pageable pageable)
			throws CredentialsException;

	List<Delegation> findByElementIdNoPages(String elementId) throws CredentialsException;

	List<Delegation> findByElementIdNoPagesWithoutAnonymous(String elementId)
			throws CredentialsException;

	Page<Delegation> findByElementIdWithoutAnonymousFiltered(String elementId, String searchKey, PageRequest pageRequest);

	List<Delegation> findByElementIdNoPagesWithoutAnonymousFiltered(String elementId, String searchKey);

	List<Delegation> getAllDelegations(String variableName, String motivation, String elementType, Locale lang) throws CredentialsException;
}