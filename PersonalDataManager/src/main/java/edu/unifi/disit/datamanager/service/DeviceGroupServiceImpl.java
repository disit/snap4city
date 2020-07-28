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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.datamanager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroup;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.datamodel.sensors.Sensor;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;

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

	@Autowired
	ISensorService sensorService;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private HttpServletRequest request;

	@Override
	public DeviceGroup saveDeviceGroup(DeviceGroup deviceGroup) throws CredentialsException {
		logger.debug("saveDeviceGroup INVOKED on deviceGroup {}", deviceGroup.getName());
		if (deviceGroup.getUsername() == null || deviceGroup.getUsername().equals("")) {
			deviceGroup.setUsername(credentialsService.getLoggedUsername(new Locale("en")));
		}
		if (deviceGroup.getOwnership() == null || deviceGroup.getOwnership().equals("")) {
			deviceGroup.setOwnership("private");
		}
		if (deviceGroup.getInsertTime() == null)
			deviceGroup.setInsertTime(new Date());
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
				} catch (CredentialsException e) {
					logger.warn("Rights exception", e);
				}
			});
		}
		return false;

	}

	@Override
	public Page<DeviceGroup> findAll(PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findAll INVOKED on pageNumber {}, pageSize {}", pageRequest.getPageNumber(), pageRequest.getPageSize());
		Page<DeviceGroup> groups = deviceGroupRepository.findByDeleteTimeIsNull(pageRequest);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public Page<DeviceGroup> findByHighLevelTypeFiltered(String highLevelType, String searchKey, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByHighLevelTypeFiltered INVOKED on highLevelType {} searchKey {}", highLevelType, searchKey);
		Page<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredPage(null, highLevelType, searchKey, pageRequest);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public Page<DeviceGroup> findAllFiltered(String searchKey, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findAllFiltered INVOKED on searchKey {}", searchKey);
		Page<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredPage(null, null, searchKey, pageRequest);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public Page<DeviceGroup> findByHighLevelType(String highLevelType, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByHighLevelType INVOKED on highLevelType {}", highLevelType);
		Page<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredPage(null, highLevelType, null, pageRequest);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public Page<DeviceGroup> findByUsername(String loggedUsername, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByUsername INVOKED on username {}", loggedUsername);
		Page<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredPage(loggedUsername, null, null, pageRequest);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public Page<DeviceGroup> findByUsernameByHighLevelTypeFiltered(String loggedUsername, String highLevelType, String searchKey, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByUsernameByHighLevelTypeFiltered INVOKED on username {} highLevelType {} searchKey {}", loggedUsername, highLevelType, searchKey);
		Page<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredPage(loggedUsername, highLevelType, searchKey, pageRequest);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public Page<DeviceGroup> findByUsernameFiltered(String loggedUsername, String searchKey, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByUsernameFiltered INVOKED on username {} searchKey {}", loggedUsername, searchKey);
		Page<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredPage(loggedUsername, null, searchKey, pageRequest);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public Page<DeviceGroup> findByUsernameByHighLevelType(String loggedUsername, String highLevelType, PageRequest pageRequest) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByUsernameByHighLevelType INVOKED on username {} highLevelType {}", loggedUsername, highLevelType);
		Page<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredPage(loggedUsername, highLevelType, null, pageRequest);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findAllNoPages() throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findAll INVOKED");
		List<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredList(null, null, null);
		fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findByHighLevelTypeFilteredNoPages(String highLevelType, String searchKey) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByHighLevelTypeFiltered INVOKED on highLevelType {} searchKey {}", highLevelType, searchKey);
		List<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredList(null, highLevelType, searchKey);
		fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findAllFilteredNoPages(String searchKey) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findAllFiltered INVOKED on searchKey {}", searchKey);
		List<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredList(null, null, searchKey);
		fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findByHighLevelTypeNoPages(String highLevelType) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByHighLevelType INVOKED on highLevelType {}", highLevelType);
		List<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredList(null, highLevelType, null);
		fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findByUsernameNoPages(String loggedUsername) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByUsername INVOKED on username {}", loggedUsername);
		List<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredList(loggedUsername, null, null);
		fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findByUsernameByHighLevelTypeFilteredNoPages(String loggedUsername, String highLevelType, String searchKey) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByUsernameByHighLevelTypeFiltered INVOKED on username {} highLevelType {} searchKey {}", loggedUsername, highLevelType, searchKey);
		List<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredList(loggedUsername, highLevelType, searchKey);
		fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findByUsernameFilteredNoPages(String loggedUsername, String searchKey) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByUsernameFiltered INVOKED on username {} searchKey {}", loggedUsername, searchKey);
		List<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredList(loggedUsername, null, searchKey);
		fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findByUsernameByHighLevelTypeNoPages(String loggedUsername, String highLevelType) throws CredentialsException, MalformedURLException, IOException {
		logger.debug("findByUsernameByHighLevelType INVOKED on username {} highLevelType {}", loggedUsername, highLevelType);
		List<DeviceGroup> groups = deviceGroupRepository.findKPIDataFilteredList(loggedUsername, highLevelType, null);
		fixSizes(groups);
		return groups;
	}

	@Override
	public Page<DeviceGroup> findByUsernameDelegatedByHighLevelTypeFiltered(String usernameDelegated, String elementType, String highLevelType, String searchKey, Pageable pageable)
			throws CredentialsException, MalformedURLException, IOException {
		logger.debug(
				"findByUsernameByHighLevelTypeFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {}",
				usernameDelegated, elementType, highLevelType, searchKey);
		if (usernameDelegated.equals("ANONYMOUS")) {
			Page<DeviceGroup> pageKPIData = deviceGroupRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
					elementType, highLevelType, searchKey, pageable);
			pageKPIData.getContent().forEach(x -> anonymize(x));
			DeviceGroupServiceImpl.this.fixSizes(pageKPIData);
			return pageKPIData;
		}
		Page<DeviceGroup> groups = deviceGroupRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
				elementType, highLevelType, searchKey, pageable);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findByUsernameDelegatedByHighLevelTypeFilteredNoPages(String usernameDelegated, String elementType, String highLevelType, String searchKey) throws CredentialsException, MalformedURLException, IOException {
		logger.debug(
				"findByUsernameByHighLevelTypeFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {}",
				usernameDelegated, elementType, highLevelType, searchKey);
		if (usernameDelegated.equals("ANONYMOUS")) {
			List<DeviceGroup> listKPIData = deviceGroupRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
					elementType, highLevelType, searchKey);
			listKPIData.forEach(x -> anonymize(x));
			fixSizes(listKPIData);
			return listKPIData;
		}
		List<DeviceGroup> groups = deviceGroupRepository.findByUsernameDelegatedAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
				elementType, highLevelType, searchKey);
		fixSizes(groups);
		return groups;
	}

	@Override
	public Page<DeviceGroup> findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered(String usernameDelegated,
			String elementType, String highLevelType, String searchKey, Pageable pageable) throws CredentialsException, MalformedURLException, IOException {
		String organization = ldapRepository.getOUnames(credentialsService.getLoggedUsername(new Locale("en")))
				.toString();
		logger.debug(
				"findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {} organization {}",
				usernameDelegated, elementType, highLevelType, searchKey, organization);
		if (usernameDelegated.equals("ANONYMOUS")) {
			Page<DeviceGroup> pageKPIData = deviceGroupRepository
					.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
							elementType, highLevelType, searchKey, organization, pageable);
			pageKPIData.getContent().forEach(x -> anonymize(x));
			DeviceGroupServiceImpl.this.fixSizes(pageKPIData);
			return pageKPIData;
		}
		Page<DeviceGroup> groups = deviceGroupRepository.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(
				usernameDelegated, elementType, highLevelType, searchKey, organization, pageable);
		DeviceGroupServiceImpl.this.fixSizes(groups);
		return groups;
	}

	@Override
	public List<DeviceGroup> findByUsernameDelegatedByHighLevelTypeByOrganizationFilteredNoPages(String usernameDelegated,
			String elementType, String highLevelType, String searchKey) throws CredentialsException, MalformedURLException, IOException {
		String organization = ldapRepository.getOUnames(credentialsService.getLoggedUsername(new Locale("en")))
				.toString();
		logger.debug(
				"findByUsernameDelegatedByHighLevelTypeByOrganizationFiltered INVOKED on usernameDelegated {} elementType {} highLevelType {} searchKey {} organization {}",
				usernameDelegated, elementType, highLevelType, searchKey, organization);
		if (usernameDelegated.equals("ANONYMOUS")) {
			List<DeviceGroup> listKPIData = deviceGroupRepository
					.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(usernameDelegated,
							elementType, highLevelType, searchKey, organization);
			listKPIData.forEach(x -> anonymize(x));
			fixSizes(listKPIData);
			return listKPIData;
		}
		List<DeviceGroup> groups = deviceGroupRepository.findByUsernameDelegatedByOrganizationAndElementTypeContainingAndDeleteTimeIsNull(
				usernameDelegated, elementType, highLevelType, searchKey, organization);
		fixSizes(groups);
		return groups;
	}

	private DeviceGroup anonymize(DeviceGroup toReturn) {
		entityManager.detach(toReturn);
		toReturn.setUsername(null);
		return toReturn;
	}

	@Override
	public DeviceGroup getDeviceGroupById(long id, Locale lang, boolean anonymize) throws IOException {
		logger.debug("getDeviceGroupById INVOKED on id {}", id);
		if (anonymize) {
			return anonymize(deviceGroupRepository.findOne(id));
		}
		DeviceGroup grp = deviceGroupRepository.findOne(id);
		if (grp != null)
			fixSize(grp);
		return grp;
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
			} catch (CredentialsException e) {
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
		} catch (Exception e) {
			return false;
		}
	}

	private void fixSizes(Page<DeviceGroup> groups) throws IOException {
		String sensorsToValidate = "";
		for (DeviceGroup group : groups) {
			if (group.getSensors() != null) {
				if (!sensorsToValidate.isEmpty())
					sensorsToValidate += ",";
				sensorsToValidate += group.getSensors();
			}
		}
		if (sensorsToValidate.isEmpty())
			return;
		Vector<String> validSensorIds = validSensors(sensorsToValidate);
		for (DeviceGroup group : groups) {
			if (group.getSensors() != null) {
				for (String sensorId : group.getSensors().split(",")) {
					if (validSensorIds.contains(sensorId)) {
						group.setSize(group.getSize() + 1);
					}
				}
			}
		}
	}

	private void fixSizes(List<DeviceGroup> groups) throws IOException {
		String sensorsToValidate = "";
		for (DeviceGroup group : groups) {
			if (group.getSensors() != null) {
				if (!sensorsToValidate.isEmpty())
					sensorsToValidate += ",";
				sensorsToValidate += group.getSensors();
			}
		}
		if (sensorsToValidate.isEmpty())
			return;
		Vector<String> validSensorIds = validSensors(sensorsToValidate);
		for (DeviceGroup group : groups) {
			if (group.getSensors() != null) {
				for (String sensorId : group.getSensors().split(",")) {
					if (validSensorIds.contains(sensorId)) {
						group.setSize(group.getSize() + 1);
					}
				}
			}
		}
	}

	private void fixSize(DeviceGroup group) throws IOException {
		String sensorsToValidate = "";
		if (group.getSensors() != null) {
			if (!sensorsToValidate.isEmpty())
				sensorsToValidate += ",";
			sensorsToValidate += group.getSensors();
		}
		if (sensorsToValidate.isEmpty())
			return;
		Vector<String> validSensorIds = validSensors(sensorsToValidate);
		if (group.getSensors() != null) {
			for (String sensorId : group.getSensors().split(",")) {
				if (validSensorIds.contains(sensorId)) {
					group.setSize(group.getSize() + 1);
				}
			}
		}
	}

	private Vector<String> validSensors(String sensors) throws MalformedURLException, IOException {

		String response = sensorService.getSensors(request.getParameter("accessToken"), null, null, null, sensors);
		/*
		 * URL url = new URL(request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+"/api/v1/sensors?accessToken="+request.getParameter("accessToken")+"&id="+sensors);
		 * logger.debug("CALL TO SENSORS API FROM validSensors(String sensors) IN DeviceGroupElementServiceImpl {}",request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+
		 * "/api/v1/sensors?accessToken="+request.getParameter("accessToken")+"&id="+sensors);
		 * 
		 * HttpURLConnection huc = (HttpURLConnection) url.openConnection(); huc.setRequestMethod("GET"); int responseCode = huc.getResponseCode(); if(responseCode == 404) return new Vector<>(); BufferedReader in = new BufferedReader(new
		 * InputStreamReader(huc.getInputStream())); String response = ""; String inputLine; while ((inputLine = in.readLine()) != null) response+=inputLine; in.close();
		 */

		ObjectMapper mapper = new ObjectMapper();
		Sensor[] validSensors = mapper.readValue(response, Sensor[].class);
		Vector<String> validSensorIds = new Vector<>();
		for (Sensor validSensor : validSensors) {
			validSensorIds.add(String.valueOf(validSensor.getId()));
		}
		return validSensorIds;

	}

}