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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// all with contrains: AndDeleteTimeIsNull
public interface DataDAO extends JpaRepository<Data, Long> {

	List<Data> findByAppIdAndDeleteTimeIsNull(String appId);

	List<Data> findByDeleteTimeBefore(Date date);// used for proper delete

	List<Data> findByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNull(String appId, Date from, Date to);

	List<Data> findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullOrderByDataTimeDesc(String appId, Date from, Date to);

	List<Data> findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullOrderByDataTimeAsc(String appId, Date from, Date to);

	List<Data> findByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndMotivation(String appId, Date from, Date to, String motivation);

	List<Data> findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndMotivationOrderByDataTimeDesc(String appId, Date from, Date to, String motivation);

	List<Data> findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndMotivationOrderByDataTimeAsc(String appId, Date from, Date to, String motivation);

	List<Data> findByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableName(String appId, Date from, Date to, String valueName);

	List<Data> findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameOrderByDataTimeDesc(String appId, Date from, Date to, String valueName);

	List<Data> findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameOrderByDataTimeAsc(String appId, Date from, Date to, String valueName);

	List<Data> findByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameAndMotivation(String appId, Date from, Date to, String valueName, String motivation);

	List<Data> findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameAndMotivationOrderByDataTimeDesc(String appId, Date from, Date to, String valueName, String motivation);

	List<Data> findFirst1000ByAppIdAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndVariableNameAndMotivationOrderByDataTimeAsc(String appId, Date from, Date to, String valueName, String motivation);

	List<Data> findByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNull(String username, Date from, Date to);

	List<Data> findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullOrderByDataTimeAsc(String username, Date from, Date to);

	List<Data> findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullOrderByDataTimeDesc(String username, Date from, Date to);

	List<Data> findByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndMotivation(String username, Date from, Date to, String motivation);

	List<Data> findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndMotivationOrderByDataTimeAsc(String username, Date from, Date to, String motivation);

	List<Data> findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndMotivationOrderByDataTimeDesc(String username, Date from, Date to, String motivation);

	List<Data> findByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableName(String username, Date from, Date to, String variableName);

	List<Data> findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameOrderByDataTimeAsc(String username, Date from, Date to, String variableName);

	List<Data> findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameOrderByDataTimeDesc(String username, Date from, Date to, String variableName);

	List<Data> findByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameAndMotivation(String username, Date from, Date to, String variableName, String motivation);

	List<Data> findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameAndMotivationOrderByDataTimeAsc(String username, Date from, Date to, String variableName, String motivation);

	List<Data> findFirst1000ByUsernameAndDataTimeAfterAndDataTimeBeforeAndDeleteTimeIsNullAndAppIdIsNullAndVariableNameAndMotivationOrderByDataTimeDesc(String username, Date from, Date to, String variableName, String motivation);

	List<Data> findAllAndDeleteTimeIsNullByOrderByDataTimeAsc();

}