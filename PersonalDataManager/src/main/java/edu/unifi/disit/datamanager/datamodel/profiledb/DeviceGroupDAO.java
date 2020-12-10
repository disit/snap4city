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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceGroupDAO extends JpaRepository<DeviceGroup, Long>, DeviceGroupDAOCustom {

    Page<DeviceGroup> findByDeleteTimeIsNull(Pageable pageable);
    
    @Query("select k from DeviceGroup as k where k.id in (select elementId from Delegation where elementType like %?2% and usernameDelegated = ?1 and deleteTime = NULL) and k.highLevelType like %?3% and deleteTime = NULL and (k.name like %?4% or k.description like %?4% or k.ownership like %?4% or k.username like %?4%)")
    Page<DeviceGroup> findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(
                    String usernameDelegated, String elementType, String highLevelType, String searchKey, Pageable pageable);

    @Query("select k from DeviceGroup as k where k.id in (select elementId from Delegation where elementType like %?2% and usernameDelegated = ?1 and deleteTime = NULL) and k.highLevelType like %?3% and deleteTime = NULL and (k.name like %?4% or k.description like %?4% or k.ownership like %?4% or k.username like %?4%)")
    List<DeviceGroup> findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(String usernameDelegated, String elementType,
                    String highLevelType, String searchKey);

    @Query("select k from DeviceGroup as k where k.id in (select elementId from Delegation where elementType like %?2% and usernameDelegated = ?1 and deleteTime = NULL) and k.highLevelType like %?3% and k.organizations like ?5 and deleteTime = NULL and (k.name like %?4% or k.description like %?4% or k.ownership like %?4% or k.username like %?4% )")
    Page<DeviceGroup> findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(
                    String usernameDelegated, String elementType, String highLevelType, String searchKey, String organization, Pageable pageable);

    @Query("select k from DeviceGroup as k where k.id in (select elementId from Delegation where elementType like %?2% and usernameDelegated = ?1 and deleteTime = NULL) and k.highLevelType like %?3% and k.organizations like ?5 and deleteTime = NULL and (k.name like %?4% or k.description like %?4% or k.ownership like %?4% or k.username like %?4% )")
    List<DeviceGroup> findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(
                    String usernameDelegated, String elementType, String highLevelType, String searchKey, String organization);

}