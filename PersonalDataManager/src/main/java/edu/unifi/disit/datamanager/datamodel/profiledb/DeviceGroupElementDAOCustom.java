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
package edu.unifi.disit.datamanager.datamodel.profiledb;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface DeviceGroupElementDAOCustom {

    Page<DeviceGroupElement> findByDeviceGroupIdAndDeleteTimeIsNullFiltered(Long grpId, PageRequest pageRequest, String searchKey);

    List<DeviceGroupElement> findByDeviceGroupIdAndDeleteTimeIsNullFiltered(Long grpId, String searchKey);

}