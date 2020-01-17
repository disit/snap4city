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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroup;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;
import org.springframework.data.domain.PageRequest;

@Service
public class DeviceGroupServiceImpl implements IDeviceGroupService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	DeviceGroupDAO deviceGroupRepository;

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
    public DeviceGroup saveDeviceGroup(DeviceGroup deviceGroup) throws CredentialsException {
        logger.debug("saveDeviceGroup INVOKED on deviceGroup {}", deviceGroup.getName());
		if (deviceGroup.getUsername() == null || deviceGroup.getUsername().equals("")) {
			deviceGroup.setUsername(credentialsService.getLoggedUsername(new Locale("en")));
		}
		if (deviceGroup.getOwnership() == null || deviceGroup.getOwnership().equals("")) {
			deviceGroup.setOwnership("private");
		}
		if(deviceGroup.getInsertTime() == null) deviceGroup.setInsertTime(new Date());
                deviceGroup.setUpdateTime(new Date());
		deviceGroup.setOrganizations(
				ldapRepository.getOUnames(credentialsService.getLoggedUsername(new Locale("en"))).toString());
		return deviceGroupRepository.save(deviceGroup);
    }

    @Override
    public boolean makeDeviceGroupPublic(String username, Long grpId, String elementType, Locale lang) throws DelegationNotValidException, CredentialsException {
        Delegation delegation = new Delegation(username, "ANONYMOUS", null, null, grpId.toString(), elementType,
				new Date(), null, null, null);

        logger.debug("makeDeviceGroupPublic");

        if (delegationService.postDelegationFromUser(username, delegation, lang) != null) {
                logger.debug("makeDeviceGroupPublic TRUE");
                return true;
        }

        logger.debug("makeDeviceGroupPublic FALSE");
        return false;
        
    }

    @Override
    public boolean makeDeviceGroupPrivate(Long grpId, Locale lang) throws DelegationNotValidException, CredentialsException {
        List<Delegation> listDelegation = delegationService.findByElementIdNoPages(Long.toString(grpId));

        logger.debug("makeDeviceGroupPrivate");

        listDelegation.removeIf(x -> !x.getUsernameDelegated().equals("ANONYMOUS"));

        if (!listDelegation.isEmpty()) {
                listDelegation.forEach(x -> {
                        x.setDeleteTime(new Date());
                        try {
                                delegationService.putDelegationFromUser(x.getUsernameDelegator(), x, x.getId(), lang);
                        } catch (DelegationNotValidException e) {
                                logger.warn("Delegation Not Valid", e);
                        }catch (CredentialsException e) {
                                logger.warn("Rights exception", e);
                        }
                });
        }
        return false;
        
    }

    @Override
    public Page<DeviceGroup> findAll(PageRequest pageRequest) throws CredentialsException {
        logger.debug("findAll INVOKED on pageNumber {}, pageSize {}", pageRequest.getPageNumber(), pageRequest.getPageSize());
	return deviceGroupRepository.findByDeleteTimeIsNull(pageRequest);        
    }

    @Override
    public Page<DeviceGroup> findByHighLevelTypeFiltered(String highLevelType, String searchKey, PageRequest pageRequest) throws CredentialsException {
        logger.debug("findByHighLevelTypeFiltered INVOKED on highLevelType {} searchKey {}", highLevelType, searchKey);
	return deviceGroupRepository.findKPIDataFilteredPage(null, highLevelType, searchKey, pageRequest);
    }

    @Override
    public Page<DeviceGroup> findAllFiltered(String searchKey, PageRequest pageRequest) throws CredentialsException {
        logger.debug("findAllFiltered INVOKED on searchKey {}", searchKey);
	return deviceGroupRepository.findKPIDataFilteredPage(null, null, searchKey, pageRequest);
    }

    @Override
    public Page<DeviceGroup> findByHighLevelType(String highLevelType, PageRequest pageRequest) throws CredentialsException {
        logger.debug("findByHighLevelType INVOKED on highLevelType {}", highLevelType);
	return deviceGroupRepository.findKPIDataFilteredPage(null, highLevelType, null, pageRequest);
    }

    @Override
    public Page<DeviceGroup> findByUsername(String loggedUsername, PageRequest pageRequest) throws CredentialsException {
        logger.debug("findByUsername INVOKED on username {}", loggedUsername);
	return deviceGroupRepository.findKPIDataFilteredPage(loggedUsername, null, null, pageRequest);
    }

    @Override
    public Page<DeviceGroup> findByUsernameByHighLevelTypeFiltered(String loggedUsername, String highLevelType, String searchKey, PageRequest pageRequest) throws CredentialsException {
        logger.debug("findByUsernameByHighLevelTypeFiltered INVOKED on username {} highLevelType {} searchKey {}", loggedUsername, highLevelType, searchKey);
	return deviceGroupRepository.findKPIDataFilteredPage(loggedUsername, highLevelType, searchKey, pageRequest);
    }

    @Override
    public Page<DeviceGroup> findByUsernameFiltered(String loggedUsername, String searchKey, PageRequest pageRequest) throws CredentialsException {
        logger.debug("findByUsernameFiltered INVOKED on username {} searchKey {}", loggedUsername, searchKey);
	return deviceGroupRepository.findKPIDataFilteredPage(loggedUsername, null, searchKey, pageRequest);
    }

    @Override
    public Page<DeviceGroup> findByUsernameByHighLevelType(String loggedUsername, String highLevelType, PageRequest pageRequest) throws CredentialsException {
        logger.debug("findByUsernameByHighLevelType INVOKED on username {} highLevelType {}", loggedUsername, highLevelType);
	return deviceGroupRepository.findKPIDataFilteredPage(loggedUsername, highLevelType, null, pageRequest);
    }

    @Override
    public List<DeviceGroup> findAllNoPages() throws CredentialsException {
        logger.debug("findAll INVOKED");
	return deviceGroupRepository.findKPIDataFilteredList(null, null, null);
    }

    @Override
    public List<DeviceGroup> findByHighLevelTypeFilteredNoPages(String highLevelType, String searchKey) throws CredentialsException {
        logger.debug("findByHighLevelTypeFiltered INVOKED on highLevelType {} searchKey {}", highLevelType, searchKey);
	return deviceGroupRepository.findKPIDataFilteredList(null, highLevelType, searchKey);
    }

    @Override
    public List<DeviceGroup> findAllFilteredNoPages(String searchKey) throws CredentialsException {
        logger.debug("findAllFiltered INVOKED on searchKey {}", searchKey);
	return deviceGroupRepository.findKPIDataFilteredList(null, null, searchKey);
    }

    @Override
    public List<DeviceGroup> findByHighLevelTypeNoPages(String highLevelType) throws CredentialsException {
        logger.debug("findByHighLevelType INVOKED on highLevelType {}", highLevelType);
	return deviceGroupRepository.findKPIDataFilteredList(null, highLevelType, null);
    }

    @Override
    public List<DeviceGroup> findByUsernameNoPages(String loggedUsername) throws CredentialsException {
        logger.debug("findByUsername INVOKED on username {}", loggedUsername);
	return deviceGroupRepository.findKPIDataFilteredList(loggedUsername, null, null);
    }

    @Override
    public List<DeviceGroup> findByUsernameByHighLevelTypeFilteredNoPages(String loggedUsername, String highLevelType, String searchKey) throws CredentialsException {
        logger.debug("findByUsernameByHighLevelTypeFiltered INVOKED on username {} highLevelType {} searchKey {}", loggedUsername, highLevelType, searchKey);
	return deviceGroupRepository.findKPIDataFilteredList(loggedUsername, highLevelType, searchKey);
    }

    @Override
    public List<DeviceGroup> findByUsernameFilteredNoPages(String loggedUsername, String searchKey) throws CredentialsException {
        logger.debug("findByUsernameFiltered INVOKED on username {} searchKey {}", loggedUsername, searchKey);
	return deviceGroupRepository.findKPIDataFilteredList(loggedUsername, null, searchKey);
    }

    @Override
    public List<DeviceGroup> findByUsernameByHighLevelTypeNoPages(String loggedUsername, String highLevelType) throws CredentialsException {
        logger.debug("findByUsernameByHighLevelType INVOKED on username {} highLevelType {}", loggedUsername, highLevelType);
	return deviceGroupRepository.findKPIDataFilteredList(loggedUsername, highLevelType, null);
    }

    @Override
    public Page<DeviceGroup> findByUsernameDelegatedByHighLevelTypeFiltered(String usernameDelegated, String elementType, String highLevelType, String searchKey, Pageable pageable) throws CredentialsException {
        logger.debug(
				"findByUsernameByHighLevelTypeFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {}",
				usernameDelegated, elementType, highLevelType, searchKey);
		if (usernameDelegated.equals("ANONYMOUS")) {
			Page<DeviceGroup> pageKPIData = deviceGroupRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
					elementType, highLevelType, searchKey, pageable);
			pageKPIData.getContent().forEach(x->anonymize(x));
			return pageKPIData;
		}
		return deviceGroupRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
				elementType, highLevelType, searchKey, pageable);
    }

    @Override
    public List<DeviceGroup> findByUsernameDelegatedByHighLevelTypeFilteredNoPages(String usernameDelegated, String elementType, String highLevelType, String searchKey) throws CredentialsException {
        logger.debug(
				"findByUsernameByHighLevelTypeFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {}",
				usernameDelegated, elementType, highLevelType, searchKey);
		if (usernameDelegated.equals("ANONYMOUS")) {
			List<DeviceGroup> listKPIData = deviceGroupRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
					elementType, highLevelType, searchKey);
			listKPIData.forEach(x->anonymize(x));
			return listKPIData;
		}
		return deviceGroupRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
				elementType, highLevelType, searchKey);
    }

    @Override
    public Page<DeviceGroup> findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(String usernameDelegated,
			String elementType, String highLevelType, String searchKey, Pageable pageable) {
        String organization = ldapRepository.getOUnames(credentialsService.getLoggedUsername(new Locale("en")))
				.toString();
		logger.debug(
				"findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {} organization {}",
				usernameDelegated, elementType, highLevelType, searchKey, organization);
		if (usernameDelegated.equals("ANONYMOUS")) {
			Page<DeviceGroup> pageKPIData = deviceGroupRepository
					.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
							elementType, highLevelType, searchKey, organization, pageable);
			pageKPIData.getContent().forEach(x->anonymize(x));
			return pageKPIData;
		}
		return deviceGroupRepository.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(
				usernameDelegated, elementType, highLevelType, searchKey, organization, pageable);
    }

    @Override
    public List<DeviceGroup> findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(String usernameDelegated,
			String elementType, String highLevelType, String searchKey) {
        String organization = ldapRepository.getOUnames(credentialsService.getLoggedUsername(new Locale("en")))
				.toString();
		logger.debug(
				"findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {} organization {}",
				usernameDelegated, elementType, highLevelType, searchKey, organization);
		if (usernameDelegated.equals("ANONYMOUS")) {
			List<DeviceGroup> listKPIData = deviceGroupRepository
					.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
							elementType, highLevelType, searchKey, organization);
			listKPIData.forEach(x->anonymize(x));
			return listKPIData;
		}
		return deviceGroupRepository.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(
				usernameDelegated, elementType, highLevelType, searchKey, organization);
    }
    
    private DeviceGroup anonymize(DeviceGroup toReturn) {
                    entityManager.detach(toReturn);
                    toReturn.setUsername(null);
            return toReturn;
    }
        
    @Override
    public DeviceGroup getDeviceGroupById(long id, Locale lang, boolean anonymize) {
            logger.debug("getDeviceGroupById INVOKED on id {}", id);
            if (anonymize) {
                    return anonymize(deviceGroupRepository.findOne(id));
            }
            return deviceGroupRepository.findOne(id);
    }
    
    @Override
    public boolean updateUsernameDelegatorOnOwnershipChange(String newOwner, Long kpiId, Locale lang)
                    throws DelegationNotValidException, CredentialsException {

            List<Delegation> listDelegation = delegationService.findByElementIdNoPages(Long.toString(kpiId));

            listDelegation.forEach(x -> {
                    x.setUsernameDelegator(newOwner);
                    try {
                            delegationService.putDelegationFromUser(credentialsService.getLoggedUsername(lang), x, x.getId(), lang);
                    } catch (DelegationNotValidException e) {
                            logger.warn("Delegation Not Valid", e);
                    }catch (CredentialsException e) {
                            logger.warn("Rights exception", e);
                    }
            });

            return true;
    }

    @Override
    public boolean lastUpdatedNow(long grpId) {
        try {
            DeviceGroup grp = deviceGroupRepository.findOne(grpId);
            grp.setUpdateTime(new Date());
            deviceGroupRepository.save(grp);
            return true;
        }
        catch(Exception e) {
            return false;
        }
    }
    
    
    
}