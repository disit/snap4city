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

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIValue;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;

public interface IKPIValueService {

	KPIValue getKPIValueById(Long id, Locale lang) throws  CredentialsException;

	Page<KPIValue> findByKpiId(Long kpiId, Pageable pageable) throws  CredentialsException;
	
	Page<KPIValue> findByKpiIdFiltered(Long kpiId, String searchKey, Pageable pageable) throws  CredentialsException;

	List<KPIValue> findByKpiIdNoPages(Long kpiId)  throws  CredentialsException;
	
	List<KPIValue> findByKpiIdGeoLocated(Long kpiId)  throws  CredentialsException;

	List<KPIValue> findByKpiIdFilteredNoPages(Long kpiId, String searchKey) throws  CredentialsException;
	
	KPIValue saveKPIValue(KPIValue kpivalue) throws  CredentialsException;

	void deleteKPIValue(Long id) throws  CredentialsException;

	List<KPIValue> findByKpiIdNoPagesWithLimit(Long kpiId, Date from, Date to, Integer first, Integer last, Locale lang) throws  DataNotValidException;

	List<Date> getKPIValueDates(Long kpiId) throws  CredentialsException;

	List<Date> getKPIValueDatesCoordinatesOptionallyNull(Long kpiId)
			throws  CredentialsException;
	
}