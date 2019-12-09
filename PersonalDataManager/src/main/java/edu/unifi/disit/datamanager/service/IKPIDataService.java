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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;

public interface IKPIDataService {

	KPIData getKPIDataById(long id, Locale lang, boolean anonymize) throws  CredentialsException;

	Page<KPIData> findAll(Pageable pageable) throws  CredentialsException;
	
	Page<KPIData> findByUsername(String username, Pageable pageable) throws  CredentialsException;
	
	Page<KPIData> findByHighLevelType(String highLevelType, Pageable pageable) throws  CredentialsException;
	
	Page<KPIData> findByUsernameByHighLevelType(String username, String highLevelType, Pageable pageable) throws  CredentialsException;
	
	Page<KPIData> findAllFiltered(String searchKey, Pageable pageable) throws  CredentialsException;
	
	Page<KPIData> findByUsernameFiltered(String username, String searchKey, Pageable pageable) throws  CredentialsException;

	Page<KPIData> findByHighLevelTypeFiltered(String highLevelType, String searchKey, Pageable pageable) throws  CredentialsException;
	
	Page<KPIData> findByUsernameByHighLevelTypeFiltered(String username, String highLevelType, String searchKey, Pageable pageable) throws  CredentialsException;

	List<KPIData> findAllNoPages() throws  CredentialsException;
	
	List<KPIData> findByUsernameNoPages(String username) throws  CredentialsException;
	
	List<KPIData> findByHighLevelTypeNoPages(String highLevelType) throws  CredentialsException;
	
	List<KPIData> findByUsernameByHighLevelTypeNoPages(String username, String highLevelType) throws  CredentialsException;
	
	List<KPIData> findAllFilteredNoPages(String searchKey) throws  CredentialsException;
	
	List<KPIData> findByUsernameFilteredNoPages(String username, String searchKey) throws  CredentialsException;

	List<KPIData> findByHighLevelTypeFilteredNoPages(String highLevelType, String searchKey) throws  CredentialsException;
	
	List<KPIData> findByUsernameByHighLevelTypeFilteredNoPages(String username, String highLevelType, String searchKey) throws  CredentialsException;

	
	Iterable<KPIData> listAllKPIData() throws  CredentialsException;

	KPIData saveKPIData(KPIData kpidata) throws  CredentialsException;

	void deleteKPIData(Long id) throws  CredentialsException;

	Page<KPIData> findByUsernameDelegatedByHighLevelTypeFiltered(String usernameDelegated, String elementType, String highLevelType,
			String searchKey, Pageable pageable) throws  CredentialsException;

	List<KPIData> findByUsernameDelegatedByHighLevelTypeFilteredNoPages(String string, String highLevelType,
			String highLevelType2, String searchKey) throws  CredentialsException;

	Page<KPIData> findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(String string, String elementType,
			String highLevelType, String searchKey, Pageable pageable);

	List<KPIData> findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(String string, String elementType,
			String highLevelType, String searchKey);

	boolean makeKPIDataPublic(String username, Long kpiId, String elementType, Locale lang) throws DelegationNotValidException, CredentialsException;

	boolean makeKPIDataPrivate(Long kpiId, Locale lang)
			throws DelegationNotValidException, CredentialsException;

	boolean updateUsernameDelegatorOnOwnershipChange(String newOwner, Long kpiId, Locale lang)
			throws DelegationNotValidException, CredentialsException;
	
	KPIData detachEntity(KPIData toReturn);
}