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
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIDataDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;

@Service
public class KPIDataServiceImpl implements IKPIDataService {

	private static final Logger logger = LogManager.getLogger();

	//@Autowired
	//private MessageSource messages;

	@Autowired
	KPIDataDAO kpiDataRepository;

	@Autowired
	OwnershipDAO ownershipRepo;
	
	@Autowired
	private LDAPUserDAO ldapRepository;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	ICredentialsService credentialsService;

	@PersistenceContext
	private EntityManager entityManager;

	


	@Override
	public KPIData getKPIDataById(long id, Locale lang) throws NoSuchMessageException {
		logger.debug("getKPIDataById INVOKED on id {}", id);
		return kpiDataRepository.findOne(id);
	}
	
	@Override
	public Page<KPIData> findAll(Pageable pageable) {
		logger.debug("findAll INVOKED on pageNumber {}, pageSize {}", pageable.getPageNumber(), pageable.getPageSize());
		return kpiDataRepository.findByDeleteTimeIsNull(pageable);
	}

	@Override
	public KPIData saveKPIData(KPIData kpidata) {
		logger.debug("saveKPIData INVOKED on kpidata {}",  kpidata.getValueName());
		if(kpidata.getUsername() == null || kpidata.getUsername().equals("")) {
			kpidata.setUsername(credentialsService.getLoggedUsername(new Locale("en")));
		}
		kpidata.setOrganizations(ldapRepository.getOUnames(credentialsService.getLoggedUsername(new Locale("en"))).toString());
		return kpiDataRepository.save(kpidata);
	}

	@Override
	public void deleteKPIData(Long id) {
		logger.debug("deleteKPIData INVOKED on id {}",  id);
		kpiDataRepository.delete(id);
	}


	@Override
	public Iterable<KPIData> listAllKPIData() {
		logger.debug("listAllKPIData");
		return kpiDataRepository.findAll();
	}

	@Override
	public Page<KPIData> findByUsername(String username, Pageable pageable)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByUsername INVOKED on username {}",  username);
		return kpiDataRepository.findByUsernameAndDeleteTimeIsNull(username, pageable);
	}

	@Override
	public Page<KPIData> findAllFiltered(String searchKey, Pageable pageable)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findAllFiltered INVOKED on searchKey {}",  searchKey);
		return kpiDataRepository.findByHighLevelTypeContainingOrNatureContainingOrSubNatureContainingOrValueNameContainingOrValueTypeContainingOrDataTypeContainingOrHealthinessContainingOrOwnershipContainingOrUsernameContainingAllIgnoreCaseAndDeleteTimeIsNull(searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, pageable);
	}
	
	@Override
	public Page<KPIData> findByUsernameFiltered(String username, String searchKey, Pageable pageable)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByUsernameFiltered INVOKED on username {} searchKey {}", username,  searchKey);
		return kpiDataRepository.findByUsernameAndHighLevelTypeContainingOrNatureContainingOrSubNatureContainingOrValueNameContainingOrValueTypeContainingOrDataTypeContainingOrHealthinessContainingOrOwnershipContainingAllIgnoreCaseAndDeleteTimeIsNull(username, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, pageable);
	}

	@Override
	public Page<KPIData> findByHighLevelType(String highLevelType, Pageable pageable)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByHighLevelType INVOKED on highLevelType {}",  highLevelType);
		return kpiDataRepository.findByHighLevelTypeAndDeleteTimeIsNull(highLevelType, pageable);
	}

	@Override
	public Page<KPIData> findByUsernameByHighLevelType(String username, String highLevelType, Pageable pageable)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByUsernameByHighLevelType INVOKED on username {} highLevelType {}", username,  highLevelType);
		return kpiDataRepository.findByUsernameAndHighLevelTypeAndDeleteTimeIsNull(username, highLevelType, pageable);
	}

	@Override
	public Page<KPIData> findByHighLevelTypeFiltered(String highLevelType, String searchKey, Pageable pageable)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByHighLevelTypeFiltered INVOKED on highLevelType {} searchKey {}", highLevelType,  searchKey);
		return kpiDataRepository.findByHighLevelTypeAndNatureContainingOrSubNatureContainingOrValueNameContainingOrValueTypeContainingOrDataTypeContainingOrHealthinessContainingOrOwnershipContainingOrUsernameContainingAllIgnoreCaseAndDeleteTimeIsNull(highLevelType, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, pageable);
	}

	@Override
	public Page<KPIData> findByUsernameByHighLevelTypeFiltered(String username, String highLevelType, String searchKey,
			Pageable pageable) throws NoSuchMessageException, CredentialsException {
		logger.debug("findByUsernameByHighLevelTypeFiltered INVOKED on highLevelType {} searchKey {}", username, highLevelType,  searchKey);
		return kpiDataRepository.findByUsernameAndHighLevelTypeAndNatureContainingOrSubNatureContainingOrValueNameContainingOrValueTypeContainingOrDataTypeContainingOrHealthinessContainingOrOwnershipContainingAllIgnoreCaseAndDeleteTimeIsNull(username, highLevelType, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, pageable);
	
	}
	
	@Override
	public Page<KPIData> findByUsernameDelegatedByHighLevelTypeFiltered(String usernameDelegated, String elementType, String highLevelType, String searchKey,
			Pageable pageable) throws NoSuchMessageException, CredentialsException {
		logger.debug("findByUsernameByHighLevelTypeFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {}", usernameDelegated, elementType, highLevelType,  searchKey);
		return kpiDataRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated, elementType, highLevelType, searchKey, pageable);
	}
	
	@Override
	public List<KPIData> findByUsernameDelegatedByHighLevelTypeFilteredNoPages(String usernameDelegated, String elementType,
			String highLevelType, String searchKey) throws NoSuchMessageException, CredentialsException {
		logger.debug("findByUsernameByHighLevelTypeFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {}", usernameDelegated, elementType, highLevelType,  searchKey);
		return kpiDataRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated, elementType, highLevelType, searchKey);
	}
	
	

	@Override
	public Page<KPIData> findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(String usernameDelegated, String elementType,
			String highLevelType, String searchKey, Pageable pageable) {
		
		String organization = ldapRepository.getOUnames(credentialsService.getLoggedUsername(new Locale("en"))).toString();
		logger.debug("findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {} organization {}", usernameDelegated, elementType, highLevelType,  searchKey, organization);
		return kpiDataRepository.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated, elementType, highLevelType, searchKey, organization, pageable);
	}

	@Override
	public List<KPIData> findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(String usernameDelegated,
			String elementType, String highLevelType, String searchKey) {
		String organization = ldapRepository.getOUnames(credentialsService.getLoggedUsername(new Locale("en"))).toString();
		logger.debug("findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {} organization {}", usernameDelegated, elementType, highLevelType,  searchKey, organization);
		return kpiDataRepository.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated, elementType, highLevelType, searchKey, organization);
	}

	@Override
	public List<KPIData> findAllNoPages() {
		logger.debug("findAll INVOKED");
		return kpiDataRepository.findByDeleteTimeIsNull();
	}

	@Override
	public List<KPIData> findByUsernameNoPages(String username) {
		logger.debug("findByUsername INVOKED on username {}",  username);
		return kpiDataRepository.findByUsernameAndDeleteTimeIsNull(username);
	}

	@Override
	public List<KPIData> findAllFilteredNoPages(String searchKey) {
		logger.debug("findAllFiltered INVOKED on searchKey {}",  searchKey);
		return kpiDataRepository.findByHighLevelTypeContainingOrNatureContainingOrSubNatureContainingOrValueNameContainingOrValueTypeContainingOrDataTypeContainingOrHealthinessContainingOrOwnershipContainingOrUsernameContainingAllIgnoreCaseAndDeleteTimeIsNull(searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey);
	}
	
	@Override
	public List<KPIData> findByUsernameFilteredNoPages(String username, String searchKey) {
		logger.debug("findByUsernameFiltered INVOKED on username {} searchKey {}", username,  searchKey);
		return kpiDataRepository.findByUsernameAndHighLevelTypeContainingOrNatureContainingOrSubNatureContainingOrValueNameContainingOrValueTypeContainingOrDataTypeContainingOrHealthinessContainingOrOwnershipContainingAllIgnoreCaseAndDeleteTimeIsNull(username, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey);
	}

	@Override
	public List<KPIData> findByHighLevelTypeNoPages(String highLevelType) {
		logger.debug("findByHighLevelType INVOKED on highLevelType {}",  highLevelType);
		return kpiDataRepository.findByHighLevelTypeAndDeleteTimeIsNull(highLevelType);
	}

	@Override
	public List<KPIData> findByUsernameByHighLevelTypeNoPages(String username, String highLevelType) {
		logger.debug("findByUsernameByHighLevelType INVOKED on username {} highLevelType {}", username,  highLevelType);
		return kpiDataRepository.findByUsernameAndHighLevelTypeAndDeleteTimeIsNull(username, highLevelType);
	}

	@Override
	public List<KPIData> findByHighLevelTypeFilteredNoPages(String highLevelType, String searchKey)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByHighLevelTypeFiltered INVOKED on highLevelType {} searchKey {}", highLevelType,  searchKey);
		return kpiDataRepository.findByHighLevelTypeAndNatureContainingOrSubNatureContainingOrValueNameContainingOrValueTypeContainingOrDataTypeContainingOrHealthinessContainingOrOwnershipContainingOrUsernameContainingAllIgnoreCaseAndDeleteTimeIsNull(highLevelType, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey);
	}

	@Override
	public List<KPIData> findByUsernameByHighLevelTypeFilteredNoPages(String username, String highLevelType,
			String searchKey) throws NoSuchMessageException, CredentialsException {
		logger.debug("findByUsernameByHighLevelTypeFiltered INVOKED on highLevelType {} searchKey {}", username, highLevelType,  searchKey);
		return kpiDataRepository.findByUsernameAndHighLevelTypeOrNatureContainingOrSubNatureContainingOrValueNameContainingOrValueTypeContainingOrDataTypeContainingOrHealthinessContainingOrOwnershipContainingAllIgnoreCaseAndDeleteTimeIsNull(username, highLevelType, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey, searchKey);
	}


	
	
	
	
}