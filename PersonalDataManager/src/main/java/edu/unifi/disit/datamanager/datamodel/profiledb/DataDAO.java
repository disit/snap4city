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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
// all with contrains: AndDeleteTimeIsNull
public interface DataDAO extends JpaRepository<Data, Long>, DataDAOCustom {

	List<Data> findByDeleteTimeBefore(Date date);// used for proper delete

	List<Data> findAllAndDeleteTimeIsNullByOrderByDataTimeAsc();

	// SELECT * FROM data d1 INNER JOIN (SELECT MAX(d2.id) maxid, id FROM data d2 where d2.app_id IS NOT NULL group by username, app_id, motivation, variable_name ) t on d1.id=t.maxid order by d1.id

	@Query("SELECT d1 FROM Data d1 WHERE d1.id = (SELECT MAX(d2.id) FROM Data d2 WHERE d1.username = d2.username AND d1.appId = d2.appId AND d1.motivation = d2.motivation AND d1.variableName = d2.variableName)")
	List<Data> findLastData();

	Data findById(Long dataId);

	@Modifying
	@Transactional
	@Query("delete from Data a where a.deleteTime < ?1")
	void deleteByDeleteTimeBefore(Date time);
}