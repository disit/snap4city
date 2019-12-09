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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface KPIValueDAO extends JpaRepository<KPIValue, Long>, KPIValueDAOCustom {

	KPIValue findOne(long id);
	
	Page<KPIValue> findAll(Pageable pageable);
	
	Page<KPIValue> findByKpiIdAndDeleteTimeIsNull(Long kpiId, Pageable pageable);
	
	Page<KPIValue> findByKpiIdAndValueContainingAllIgnoreCaseAndDeleteTimeIsNull(Long kpiId, String value, Pageable pageable);

	List<KPIValue> findByKpiIdAndDeleteTimeIsNull(Long kpiId);
	
	List<KPIValue> findByKpiIdAndValueContainingAllIgnoreCaseAndDeleteTimeIsNull(Long kpiId, String searchKey); 
	
	@Query("select distinct(date(dataTime)) from KPIValue as v where v.kpiId = ?1 and v.latitude IS NOT NULL and v.longitude IS NOT NULL and v.deleteTime IS NULL")
	List<Date> findByKpiIdDistinctDateAndDeleteTimeIsNull(Long kpiId);

	@Query("select distinct(date(dataTime)) from KPIValue as v where v.kpiId = ?1 and v.deleteTime IS NULL")
	List<Date> findByKpiIdDistinctDateAndDeleteTimeIsNullWithCoordinatesOptionallyNull(Long kpiId);
	
	List<KPIValue> findByKpiIdAndDeleteTimeIsNullAndLatitudeIsNotNullAndLongitudeIsNotNullAndLatitudeNotLikeAndLongitudeNotLike(
			Long kpiId, String string, String string2);
	
}