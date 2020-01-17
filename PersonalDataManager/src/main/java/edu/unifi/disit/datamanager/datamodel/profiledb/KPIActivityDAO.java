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

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface KPIActivityDAO extends JpaRepository<KPIActivity, Long>, KPIActivityDAOCustom {

	List<KPIActivity> findByInsertTimeBefore(Date time);

	@Modifying
	@Transactional
	@Query("delete from KPIActivity a where a.insertTime < ?1")
	void deleteByInsertTimeBefore(Date time);

	Page<KPIActivity> findBySourceIdAndDeleteTimeIsNull(Long sourceIdFilter, Pageable pageable);
	
	List<KPIActivity> findBySourceIdAndDeleteTimeIsNull(Long sourceIdFilter);
	
	Page<KPIActivity> findByKpiIdAndDeleteTimeIsNullAndSourceIdIsNotNull(Long kpiId, Pageable pageable);

	Page<KPIActivity> findByKpiIdAndAccessTypeAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(Long kpiId, String accessType,
			String sourceRequest, Pageable pageable);
	
	Page<KPIActivity> findByKpiIdAndAccessTypeAndDeleteTimeIsNullAndSourceIdIsNotNull(Long kpiId, String accessType, Pageable pageable);

	Page<KPIActivity> findByKpiIdAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(Long kpiId, String sourceRequest, Pageable pageable);

	List<KPIActivity> findByKpiIdAndDeleteTimeIsNullAndSourceIdIsNotNull(Long kpiId);
	
	List<KPIActivity> findByKpiIdAndAccessTypeAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(Long kpiId, String accessType,
			String sourceRequest);
	
	List<KPIActivity> findByKpiIdAndAccessTypeAndDeleteTimeIsNullAndSourceIdIsNotNull(Long kpiId, String accessType);

	List<KPIActivity> findByKpiIdAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(Long kpiId, String sourceRequest);

	Page<KPIActivity> findByUsernameAndKpiIdAndDeleteTimeIsNullAndSourceIdIsNotNull(String username, Long kpiId, Pageable pageable);

	Page<KPIActivity> findByUsernameAndKpiIdAndAccessTypeAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(String username, Long kpiId,
			String accessType, String sourceRequest, Pageable pageable);

	Page<KPIActivity> findByUsernameAndKpiIdAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(String username, Long kpiId, String sourceRequest,
			Pageable pageable);

	Page<KPIActivity> findByUsernameAndKpiIdAndAccessTypeAndDeleteTimeIsNullAndSourceIdIsNotNull(String username, Long kpiId, String accessType,
			Pageable pageable);

	List<KPIActivity> findByUsernameAndKpiIdAndDeleteTimeIsNullAndSourceIdIsNotNull(String username, Long kpiId);

	List<KPIActivity> findByUsernameAndKpiIdAndAccessTypeAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(String username, Long kpiId,
			String accessType, String sourceRequest);

	List<KPIActivity> findByUsernameAndKpiIdAndSourceRequestAndDeleteTimeIsNullAndSourceIdIsNotNull(String username, Long kpiId, String sourceRequest);

	List<KPIActivity> findByUsernameAndKpiIdAndAccessTypeAndDeleteTimeIsNullAndSourceIdIsNotNull(String username, Long kpiId, String accessType);


}