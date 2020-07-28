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

import java.util.Date;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface KPIDataDAO extends JpaRepository<KPIData, Long>, KPIDataDAOCustom {

	KPIData findOne(long id);

	Page<KPIData> findAll(Pageable pageable);

	Page<KPIData> findByDeleteTimeIsNull(Pageable pageable);

	List<KPIData> findByDeleteTimeIsNull();

	@Query("select k from KPIData as k where k.id in (select elementId from Delegation where elementType like %?2% and usernameDelegated = ?1 and deleteTime = NULL) and k.highLevelType like %?3% and deleteTime = NULL and (k.nature like %?4% or k.subNature like %?4% or k.valueName like %?4% or k.valueType like %?4% or k.dataType like %?4%)")
	Page<KPIData> findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(
			String usernameDelegated, String elementType, String highLevelType, String searchKey, Pageable pageable);

	@Cacheable("kpidata")
	@Query("select k from KPIData as k where k.id in (select elementId from Delegation where elementType like %?2% and usernameDelegated = ?1 and deleteTime = NULL) and k.highLevelType like %?3% and deleteTime = NULL and (k.nature like %?4% or k.subNature like %?4% or k.valueName like %?4% or k.valueType like %?4% or k.dataType like %?4%)")
	List<KPIData> findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(String usernameDelegated, String elementType,
			String highLevelType, String searchKey);

	@Query("select k from KPIData as k where k.id in (select elementId from Delegation where elementType like %?2% and usernameDelegated = ?1 and deleteTime = NULL) and k.highLevelType like %?3% and k.organizations like ?5 and deleteTime = NULL and (k.nature like %?4% or k.subNature like %?4% or k.valueName like %?4% or k.valueType like %?4% or k.dataType like %?4%)")
	Page<KPIData> findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(
			String usernameDelegated, String elementType, String highLevelType, String searchKey, String organization, Pageable pageable);

	@Query("select k from KPIData as k where k.id in (select elementId from Delegation where elementType like %?2% and usernameDelegated = ?1 and deleteTime = NULL) and k.highLevelType like %?3% and k.organizations like ?5 and deleteTime = NULL and (k.nature like %?4% or k.subNature like %?4% or k.valueName like %?4% or k.valueType like %?4% or k.dataType like %?4%)")
	List<KPIData> findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(
			String usernameDelegated, String elementType, String highLevelType, String searchKey, String organization);

	List<KPIData> findByDeleteTimeBefore(Date time);

	@Modifying
	@Transactional
	@Query("delete from KPIData a where a.deleteTime < ?1")
	void deleteByDeleteTimeBefore(Date time);

	List<KPIData> findByUsername(String username);

	List<KPIData> findByUsernameAndDeleteTimeIsNull(String username);

	List<KPIData> findByHighLevelTypeAndDeleteTimeIsNull(String elmtType);

	List<KPIData> findByUsernameAndHighLevelTypeAndDeleteTimeIsNull(String username, String elmtType);

	List<KPIData> findByUsernameAndHighLevelTypeIsNotNullAndDeleteTimeIsNull(String username);

	Page<KPIData> findByHighLevelTypeIsNotNullAndDeleteTimeIsNull(Pageable pageable);

	List<KPIData> findByHighLevelTypeIsNotNullAndDeleteTimeIsNull();

}