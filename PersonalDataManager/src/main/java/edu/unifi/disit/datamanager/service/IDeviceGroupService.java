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
import java.io.IOException;
import java.net.MalformedURLException;
import org.springframework.data.domain.PageRequest;

public interface IDeviceGroupService {
	
    DeviceGroup saveDeviceGroup(DeviceGroup deviceGroup) throws  CredentialsException;
    
    boolean makeDeviceGroupPublic(String username, Long grpId, String elementType, Locale lang) throws DelegationNotValidException, CredentialsException;

    boolean makeDeviceGroupPrivate(Long grpId, Locale lang) throws DelegationNotValidException, CredentialsException;

    Page<DeviceGroup> findAll(PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException;

    Page<DeviceGroup> findByHighLevelTypeFiltered(String highLevelType, String searchKey, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException;

    Page<DeviceGroup> findAllFiltered(String searchKey, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException;

    Page<DeviceGroup> findByHighLevelType(String highLevelType, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException;

    Page<DeviceGroup> findByUsername(String loggedUsername, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException;

    Page<DeviceGroup> findByUsernameByHighLevelTypeFiltered(String loggedUsername, String highLevelType, String searchKey, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException;

    Page<DeviceGroup> findByUsernameFiltered(String loggedUsername, String searchKey, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException;

    Page<DeviceGroup> findByUsernameByHighLevelType(String loggedUsername, String highLevelType, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findAllNoPages() throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findByHighLevelTypeFilteredNoPages(String highLevelType, String searchKey) throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findAllFilteredNoPages(String searchKey) throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findByHighLevelTypeNoPages(String highLevelType) throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findByUsernameNoPages(String loggedUsername) throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findByUsernameByHighLevelTypeFilteredNoPages(String loggedUsername, String highLevelType, String searchKey) throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findByUsernameFilteredNoPages(String loggedUsername, String searchKey) throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findByUsernameByHighLevelTypeNoPages(String loggedUsername, String highLevelType) throws CredentialsException, MalformedURLException, IOException;
    
    Page<DeviceGroup> findByUsernameDelegatedByHighLevelTypeFiltered(String usernameDelegated, String elementType, String highLevelType,
                    String searchKey, Pageable pageable) throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findByUsernameDelegatedByHighLevelTypeFilteredNoPages(String usernameDelegated, String elementType,
                    String highLevelType, String searchKey) throws CredentialsException, MalformedURLException, IOException;

    Page<DeviceGroup> findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(String usernameDelegated,
			String elementType, String highLevelType, String searchKey, Pageable pageable) throws CredentialsException, MalformedURLException, IOException;

    List<DeviceGroup> findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(String usernameDelegated,
			String elementType, String highLevelType, String searchKey) throws CredentialsException, MalformedURLException, IOException;;
    
    DeviceGroup getDeviceGroupById(long id, Locale lang, boolean anonymize) throws  IOException, CredentialsException;
    
    boolean updateUsernameDelegatorOnOwnershipChange(String newOwner, Long kpiId, Locale lang)
			throws DelegationNotValidException, CredentialsException;
    
    boolean lastUpdatedNow(long grpId);
    
}