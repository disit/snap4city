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
package edu.unifi.disit.snapengager.datamodel.datamanagerdb;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface KPIValueDAO extends JpaRepository<KPIValue, Long> {

	Long countByKpiId(Long kpiid);

	Long countByKpiIdAndValueOrValueOrValue(Long id, String uno, String due, String tre);

	List<KPIValue> findByKpiId(Long id);

	List<KPIValue> findByKpiIdAndDataTimeAfterAndDataTimeBefore(Long id, Date from, Date to);

	KPIValue findTopByKpiIdOrderById(Long id);

	@Query("SELECT COUNT(k) from KPIValue k where k.kpiId=?1 and k.insertTime>?2 and (k.value=?3 or	k.value=?4 or k.value=?5)")
	Long countByKpiIdAndInsertTimeAfterAndValue3(Long id, Date lastInjested, String string, String string2, String string3);

	@Query("SELECT COUNT(k) from KPIValue k where k.kpiId=?1 and k.insertTime>?2 and (k.value=?3 or	k.value=?4 or k.value=?5 or k.value=?6 or k.value=?7 or k.value=?8 or k.value=?9)")
	Long countByKpiIdAndInsertTimeAfterAndValue7(Long id, Date lastInjested, String string, String string2, String string3, String string4, String string5, String string6, String string7);

	@Query("SELECT COUNT(k) from KPIValue k where k.kpiId=?1 and k.insertTime>?2 and (k.value=?3 or	k.value=?4 or k.value=?5 or k.value=?6 or k.value=?7 or k.value=?8 or k.value=?9 or k.value=?10 or k.value=?11)")
	Long countByKpiIdAndInsertTimeAfterAndValue9(Long id, Date lastInjested, String string, String string2, String string3, String string4, String string5, String string6,
			String string7, String string8, String string9);

	@Query("SELECT COUNT(k) from KPIValue k where k.kpiId=?1 and k.insertTime>?2 and (k.value=?3 or	k.value=?4 or k.value=?5 or k.value=?6 or k.value=?7 or k.value=?8 or k.value=?9 or k.value=?10 or k.value=?11 or k.value=?12 or k.value=?13 or k.value=?14 or k.value=?15)")
	Long countByKpiIdAndInsertTimeAfterAndValue13(Long id, Date lastInjested, String string, String string2, String string3, String string4, String string5, String string6,
			String string7, String string8, String string9, String string10, String string11, String string12, String string13);

	@Query("SELECT COUNT(k) from KPIValue k where k.kpiId=?1 and k.insertTime>?2 and (k.value=?3 or	k.value=?4 or k.value=?5 or k.value=?6 or k.value=?7 or k.value=?8 or k.value=?9 or k.value=?10 or k.value=?11 or k.value=?12 or k.value=?13 or k.value=?14 or k.value=?15 or k.value=?16 or k.value=?17)")
	Long countByKpiIdAndInsertTimeAfterAndValue15(Long id, Date lastInjested, String string, String string2, String string3, String string4, String string5,
			String string6, String string7, String string8, String string9, String string10, String string11, String string12, String string13, String string14, String string15);
}