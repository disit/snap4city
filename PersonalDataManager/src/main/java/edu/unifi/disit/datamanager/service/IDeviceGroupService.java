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

import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroup;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;
import org.springframework.data.domain.PageRequest;

public interface IDeviceGroupService {
	
    DeviceGroup saveDeviceGroup(DeviceGroup deviceGroup) throws  CredentialsException;
    
    boolean makeDeviceGroupPublic(String username, Long grpId, String elementType, Locale lang) throws DelegationNotValidException, CredentialsException;

    boolean makeDeviceGroupPrivate(Long grpId, Locale lang) throws DelegationNotValidException, CredentialsException;

    Page<DeviceGroup> findAll(PageRequest pageRequest) throws CredentialsException;

    Page<DeviceGroup> findByHighLevelTypeFiltered(String highLevelType, String searchKey, PageRequest pageRequest) throws CredentialsException;

    Page<DeviceGroup> findAllFiltered(String searchKey, PageRequest pageRequest) throws CredentialsException;

    Page<DeviceGroup> findByHighLevelType(String highLevelType, PageRequest pageRequest) throws CredentialsException;

    Page<DeviceGroup> findByUsername(String loggedUsername, PageRequest pageRequest) throws CredentialsException;

    Page<DeviceGroup> findByUsernameByHighLevelTypeFiltered(String loggedUsername, String highLevelType, String searchKey, PageRequest pageRequest) throws CredentialsException;

    Page<DeviceGroup> findByUsernameFiltered(String loggedUsername, String searchKey, PageRequest pageRequest) throws CredentialsException;

    Page<DeviceGroup> findByUsernameByHighLevelType(String loggedUsername, String highLevelType, PageRequest pageRequest) throws CredentialsException;

    List<DeviceGroup> findAllNoPages() throws CredentialsException;

    List<DeviceGroup> findByHighLevelTypeFilteredNoPages(String highLevelType, String searchKey) throws CredentialsException;

    List<DeviceGroup> findAllFilteredNoPages(String searchKey) throws CredentialsException;

    List<DeviceGroup> findByHighLevelTypeNoPages(String highLevelType) throws CredentialsException;

    List<DeviceGroup> findByUsernameNoPages(String loggedUsername) throws CredentialsException;

    List<DeviceGroup> findByUsernameByHighLevelTypeFilteredNoPages(String loggedUsername, String highLevelType, String searchKey) throws CredentialsException;

    List<DeviceGroup> findByUsernameFilteredNoPages(String loggedUsername, String searchKey) throws CredentialsException;

    List<DeviceGroup> findByUsernameByHighLevelTypeNoPages(String loggedUsername, String highLevelType) throws CredentialsException;
    
    Page<DeviceGroup> findByUsernameDelegatedByHighLevelTypeFiltered(String usernameDelegated, String elementType, String highLevelType,
                    String searchKey, Pageable pageable) throws  CredentialsException;

    List<DeviceGroup> findByUsernameDelegatedByHighLevelTypeFilteredNoPages(String usernameDelegated, String elementType,
                    String highLevelType, String searchKey) throws  CredentialsException;

    Page<DeviceGroup> findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(String usernameDelegated,
			String elementType, String highLevelType, String searchKey, Pageable pageable);

    List<DeviceGroup> findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(String usernameDelegated,
			String elementType, String highLevelType, String searchKey);
    
    DeviceGroup getDeviceGroupById(long id, Locale lang, boolean anonymize) throws  CredentialsException;
    
    boolean updateUsernameDelegatorOnOwnershipChange(String newOwner, Long kpiId, Locale lang)
			throws DelegationNotValidException, CredentialsException;
    
    boolean lastUpdatedNow(long grpId);
    
}