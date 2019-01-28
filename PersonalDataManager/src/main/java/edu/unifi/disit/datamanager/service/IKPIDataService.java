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

import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;

public interface IKPIDataService {

	KPIData getKPIDataById(long id, Locale lang) throws NoSuchMessageException, CredentialsException;

	Page<KPIData> findAll(Pageable pageable) throws NoSuchMessageException, CredentialsException;
	
	Page<KPIData> findByUsername(String username, Pageable pageable) throws NoSuchMessageException, CredentialsException;
	
	Page<KPIData> findByHighLevelType(String highLevelType, Pageable pageable) throws NoSuchMessageException, CredentialsException;
	
	Page<KPIData> findByUsernameByHighLevelType(String username, String highLevelType, Pageable pageable) throws NoSuchMessageException, CredentialsException;
	
	Page<KPIData> findAllFiltered(String searchKey, Pageable pageable) throws NoSuchMessageException, CredentialsException;
	
	Page<KPIData> findByUsernameFiltered(String username, String searchKey, Pageable pageable) throws NoSuchMessageException, CredentialsException;

	Page<KPIData> findByHighLevelTypeFiltered(String highLevelType, String searchKey, Pageable pageable) throws NoSuchMessageException, CredentialsException;
	
	Page<KPIData> findByUsernameByHighLevelTypeFiltered(String username, String highLevelType, String searchKey, Pageable pageable) throws NoSuchMessageException, CredentialsException;

	List<KPIData> findAllNoPages() throws NoSuchMessageException, CredentialsException;
	
	List<KPIData> findByUsernameNoPages(String username) throws NoSuchMessageException, CredentialsException;
	
	List<KPIData> findByHighLevelTypeNoPages(String highLevelType) throws NoSuchMessageException, CredentialsException;
	
	List<KPIData> findByUsernameByHighLevelTypeNoPages(String username, String highLevelType) throws NoSuchMessageException, CredentialsException;
	
	List<KPIData> findAllFilteredNoPages(String searchKey) throws NoSuchMessageException, CredentialsException;
	
	List<KPIData> findByUsernameFilteredNoPages(String username, String searchKey) throws NoSuchMessageException, CredentialsException;

	List<KPIData> findByHighLevelTypeFilteredNoPages(String highLevelType, String searchKey) throws NoSuchMessageException, CredentialsException;
	
	List<KPIData> findByUsernameByHighLevelTypeFilteredNoPages(String username, String highLevelType, String searchKey) throws NoSuchMessageException, CredentialsException;

	
	Iterable<KPIData> listAllKPIData() throws NoSuchMessageException, CredentialsException;

	KPIData saveKPIData(KPIData kpidata) throws NoSuchMessageException, CredentialsException;

	void deleteKPIData(Long id) throws NoSuchMessageException, CredentialsException;

	Page<KPIData> findByUsernameDelegatedByHighLevelTypeFiltered(String usernameDelegated, String elementType, String highLevelType,
			String searchKey, Pageable pageable) throws NoSuchMessageException, CredentialsException;

	List<KPIData> findByUsernameDelegatedByHighLevelTypeFilteredNoPages(String string, String highLevelType,
			String highLevelType2, String searchKey) throws NoSuchMessageException, CredentialsException;

	Page<KPIData> findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(String string, String elementType,
			String highLevelType, String searchKey, Pageable pageable);

	List<KPIData> findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(String string, String elementType,
			String highLevelType, String searchKey);
	
	

}