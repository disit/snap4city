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
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElement;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import java.io.IOException;

public interface IDeviceGroupElementService {
    
    Page<DeviceGroupElement> findByDeviceGroupId(Long grpId, PageRequest pageRequest) throws CredentialsException, IOException;
    
    List<DeviceGroupElement> findByDeviceGroupIdNoPages(Long grpId) throws CredentialsException, IOException;
    
    Set<String> getAvailElmtTypesToAdd(String username);
    
    Set<Object> getAvailItemsToAdd(String username, String elmtType);
    
    List<DeviceGroupElement> addElmtsToGrp(Long grpId, List<DeviceGroupElement> elements);

    Page<DeviceGroupElement> findByDeviceGroupIdFiltered(Long grpId, String searchKey, PageRequest pageRequest) throws IOException;

    List<DeviceGroupElement> findByDeviceGroupIdNoPagesFiltered(Long grpId, String searchKey) throws IOException;
    
    DeviceGroupElement getDeviceGroupElementById(Long id) throws  CredentialsException, IOException;

    public Set<String> getAllElmtTypes();

    public Set<Object> getAllItems(String elmtType);
    
    List<DeviceGroupElement> getByUserAndElmtIdAndElmtType(String username, String elementId, String elementType);
    
}