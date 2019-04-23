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
package edu.unifi.disit.snapengager.service;

import java.util.List;
import java.util.Locale;

import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.unifi.disit.snapengager.datamodel.profiledb.Engagement;
import edu.unifi.disit.snapengager.exception.CredentialsException;
import edu.unifi.disit.snapengager.exception.EngagementException;
import edu.unifi.disit.snapengager.exception.UserprofileException;

public interface IEngagementService {

	Engagement get(Long id, Locale lang);

	List<Engagement> getActive(Locale lang);

	Engagement getLastActive(Locale lang);

	List<Engagement> getElapsed(Locale lang);

	void add(List<Engagement> e, Locale lang) throws UserprofileException;

	void setDeleted(Long engagementId, Locale lang) throws EngagementException, NoSuchMessageException, CredentialsException;

	void setAllDeleted(Locale lang) throws EngagementException;

	void setElapsed(Long engagementId, Locale lang) throws EngagementException;

	Page<Engagement> findAll(Pageable pageable, Locale lang);

	Page<Engagement> findAllBySearch(String searchKey, Pageable pageable, Locale lang);

	Page<Engagement> findAllLogged(Pageable pageable, Locale lang);

	Page<Engagement> findAllLoggedBySearch(String searchKey, Pageable pageable, Locale lang);
}