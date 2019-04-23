/* Snap4City Engager (SE)
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
package edu.unifi.disit.snapengager.datamodel.profiledb;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.unifi.disit.snapengager.datamodel.EngagementStatusType;

@Repository
public interface EngagementDAO extends JpaRepository<Engagement, Long> {

	List<Engagement> findByUsername(String loggedUsername);

	Engagement findTop1ByUsernameAndRulenameOrderByIdDesc(String username, String rulename);

	Engagement findTop1ByUsernameAndRulenameAndStatusOrderByIdDesc(String username, String rulename, EngagementStatusType created);

	Engagement findById(Long id);

	List<Engagement> findByUsernameAndStatus(String username, EngagementStatusType status);

	List<Engagement> findByStatusAndElapseBefore(EngagementStatusType created, Date date);

	Page<Engagement> findByDeletedIsNull(Pageable pageable);

	Page<Engagement> findByUsernameContainingOrTitleContainingOrSubtitleContainingOrRulenameContainingOrMessageContainingAllIgnoreCaseAndDeletedIsNull(String searchKey, String searchKey2, String searchKey3, String searchKey4,
			String searchKey5, Pageable pageable);

	Page<Engagement> findByUsernameAndDeletedIsNull(String loggedUsername, Pageable pageable);

	Page<Engagement> findByUsernameAndTitleContainingOrSubtitleContainingOrRulenameContainingOrMessageContainingAllIgnoreCaseAndDeletedIsNull(String loggedUsername, String searchKey, String searchKey2, String searchKey3,
			String searchKey4, Pageable pageable);

	Engagement findTopByUsernameAndStatusOrderByIdDesc(String loggedUsername, EngagementStatusType created);
}