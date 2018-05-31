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
public interface DelegationDAO extends JpaRepository<Delegation, Long> {

	List<Delegation> findByUsernameDelegatorAndUsernameDelegatedAndDeleteTimeIsNull(String uidDelegator, String uidDelegated);

	List<Delegation> findByUsernameDelegatedAndDeleteTimeIsNull(String username);

	List<Delegation> findByUsernameDelegatedAndMotivationAndDeleteTimeIsNull(String username, String motivation);

	List<Delegation> findByUsernameDelegatedAndVariableNameAndDeleteTimeIsNull(String username, String variableName);

	List<Delegation> findByUsernameDelegatedAndVariableNameAndMotivationAndDeleteTimeIsNull(String username, String variableName, String motivation);

	List<Delegation> findByUsernameDelegatorAndDeleteTimeIsNull(String username);

	List<Delegation> findByUsernameDelegatorAndMotivationAndDeleteTimeIsNull(String username, String motivation);

	List<Delegation> findByUsernameDelegatorAndVariableNameAndDeleteTimeIsNull(String username, String variableName);

	List<Delegation> findByUsernameDelegatorAndVariableNameAndMotivationAndDeleteTimeIsNull(String username, String variableName, String motivation);

	List<Delegation> findByElementIdAndDeleteTimeIsNull(String appId);

	List<Delegation> findByElementIdAndMotivationAndDeleteTimeIsNull(String appId, String motivation);

	List<Delegation> findByElementIdAndVariableNameAndDeleteTimeIsNull(String appId, String variableName);

	List<Delegation> findByElementIdAndVariableNameAndMotivationAndDeleteTimeIsNull(String appId, String variableName, String motivation);

	List<Delegation> findByUsernameDelegatedAndElementIdAndDeleteTimeIsNull(String string, String appId);

	Delegation findByIdAndDeleteTimeIsNull(Long delegationId);

	List<Delegation> findByDeleteTimeBefore(Date date);// used for proper delete
}