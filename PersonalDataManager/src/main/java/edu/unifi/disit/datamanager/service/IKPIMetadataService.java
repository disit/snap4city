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
package edu.unifi.disit.datamanager.service;

import java.util.List;
import java.util.Locale;

import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIMetadata;
import edu.unifi.disit.datamanager.exception.CredentialsException;

public interface IKPIMetadataService {

	KPIMetadata getKPIMetadataById(Long id, Locale lang) throws NoSuchMessageException, CredentialsException;

	Page<KPIMetadata> findAllByKpiId(Long kpiId, Pageable pageable) throws NoSuchMessageException, CredentialsException;
	
	List<KPIMetadata> findByKpiIdNoPages(Long id) throws NoSuchMessageException, CredentialsException;
	
	Page<KPIMetadata> findAllFilteredByKpiId(Long kpiId, String searchKey, Pageable pageable) throws NoSuchMessageException, CredentialsException;

	KPIMetadata saveKPIMetadata(KPIMetadata kpiMetadata) throws NoSuchMessageException, CredentialsException;

	void deleteKPIMetadata(Long id) throws NoSuchMessageException, CredentialsException;

	
	
}