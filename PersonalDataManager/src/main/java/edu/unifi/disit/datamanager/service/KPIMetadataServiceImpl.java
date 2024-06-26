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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.profiledb.KPIMetadata;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIMetadataDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;

@Service
public class KPIMetadataServiceImpl implements IKPIMetadataService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	KPIMetadataDAO kpiMetadataRepository;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	ICredentialsService credentialsService;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public KPIMetadata getKPIMetadataById(Long id, Locale lang) throws  CredentialsException {
		logger.debug("getKPIMetadataById INVOKED on id {}", id);
		return kpiMetadataRepository.findById(id).orElse(null);
	}

	@Override
	public Page<KPIMetadata> findAllByKpiId(Long kpiId, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findAllByKpiId INVOKED on kpiId {}",  kpiId);
		return kpiMetadataRepository.findByKpiIdAndDeleteTimeIsNull(kpiId, pageable);
	}

	@Override
	public List<KPIMetadata> findByKpiIdNoPages(Long kpiId) throws  CredentialsException {
		logger.debug("findByKpiIdNoPages INVOKED on kpiId {}",  kpiId);
		return kpiMetadataRepository.findByKpiIdAndDeleteTimeIsNull(kpiId);
	}

	@Override
	public Page<KPIMetadata> findAllFilteredByKpiId(Long kpiId, String searchKey, Pageable pageable)
			throws  CredentialsException {
		logger.debug("findAllFilteredByKpiId INVOKED on searchKey {} kpiId {}",  searchKey, kpiId);
		return kpiMetadataRepository.findByKpiIdAndValueContainingOrKeyContainingAllIgnoreCaseAndDeleteTimeIsNull(kpiId, searchKey, searchKey, pageable);
	
	}
	
	@Override
	public List<KPIMetadata> findFilteredByKpiIdNoPages(Long kpiId, String searchKey)
			throws  CredentialsException {
		logger.debug("findAllFilteredByKpiId INVOKED on searchKey {} kpiId {}",  searchKey, kpiId);
		return kpiMetadataRepository.findByKpiIdAndValueContainingOrKeyContainingAllIgnoreCaseAndDeleteTimeIsNull(kpiId, searchKey, searchKey);
	
	}

	@Override
	public KPIMetadata saveKPIMetadata(KPIMetadata kpimetadata) throws  CredentialsException {
		logger.debug("saveKPIMetadata INVOKED on kpimetadata {}",  kpimetadata.getId());
		return kpiMetadataRepository.save(kpimetadata);
	}

	@Override
	public void deleteKPIMetadata(Long id) throws  CredentialsException {
		logger.debug("deleteKPIMetadata INVOKED on id {}",  id);
		kpiMetadataRepository.deleteById(id);
		
	}
	
	
	
	
}