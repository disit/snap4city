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
package edu.unifi.disit.datamanager.datamodel.profiledb;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceGroupElementDAO extends JpaRepository<DeviceGroupElement, Long>, DeviceGroupElementDAOCustom {

	Page<DeviceGroupElement> findByDeviceGroupIdAndDeleteTimeIsNull(Long grpId, Pageable pageable);

	// TODO, remove this api since it relay on a fieldd [username] that should be transient
	// see DeviceGroupElement.java
	List<DeviceGroupElement> findByUsernameAndElementIdAndElementTypeAndDeleteTimeIsNull(String username, String elementId, String elementType);

	// @Cacheable("deviceGroupElementByDeviceGroupId")
	List<DeviceGroupElement> findByDeviceGroupIdAndDeleteTimeIsNull(Long grpId);

	List<DeviceGroupElement> findByElementIdAndElementTypeAndDeleteTimeIsNull(String elementId, String elementType);
        
        //boolean existsByDeviceGroupIdAndElementTypeAndElementId(Long groupId, String elementType, String elementId);
        
        boolean existsByDeviceGroupIdAndElementTypeAndElementIdAndDeleteTimeIsNull(Long deviceGroupId, String elementType, String elementId);
}

