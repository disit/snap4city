/* Data Manager (DM).
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
package edu.unifi.disit.datamanager.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.AccessRightType;
import edu.unifi.disit.datamanager.datamodel.ElementType;
import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DelegationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.DelegationDAOCustom;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElement;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElementDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;

@Service
public class AccessServiceImpl implements IAccessService {

	@Autowired
	LDAPUserDAO lu;

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	DelegationDAO delegationRepo;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Autowired
	ICredentialsService credentialsService;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	DeviceGroupElementDAO dgeRepo;

	@Autowired
	ICredentialsService credentialService;

	@Override
	// check if loggedUser can access to elementID. Firstly check if there are any delegation, if not found, check if the loggedUser is ROOTADMIN or is the OWNER
	public Response checkAccessFromApp(String elementID, String elementType, Locale lang) throws CredentialsException {
		Response response = checkDelegationAccess(elementID, elementType, null, credentialsService.getLoggedUsername(lang), lang);

		// if no delegation has been found, investigate more on the ROOTADMIN role and OWNERSHIP
		if (!Boolean.TRUE.equals(response.getResult())) {
			// if the AT is from a RootAdmin
			if (credentialsService.isRoot(lang)) {
				response.setResult(true);
				response.setMessage(AccessRightType.ROOTADMIN.toString());
				return response;
			}

			// if the AT is from the owner
			String strippedElementID = elementID;
			if (elementID.startsWith("http://") || elementID.startsWith("https://")) {// to enable iotdirectory scenario
				strippedElementID = elementID.substring(elementID.lastIndexOf('/') + 1);
				logger.debug("strippedElementID is: {}", strippedElementID);
			}
			List<Ownership> owns = ownershipRepo.findByElementIdAndDeletedIsNull(strippedElementID);
			for (Ownership own : owns)
				if (own.getUsername().equals(credentialsService.getLoggedUsername(lang))) {
					response.setResult(true);
					response.setMessage(AccessRightType.OWNER.toString());
					return response;
				}
		}

		return response;
	}

	@Override
	// backword compatibility
	// the owner of the elementID, specified in the accesstoken, can check if another user has been delegated to access elementId
	public Response checkDelegationsFromUsername(String elementID, String elementType, String variableName, String username, Locale lang) throws CredentialsException {

		credentialService.checkAppIdCredentials(elementID, elementType, lang);

		return checkDelegationAccess(elementID, elementType, variableName, username, lang);
	}

	public Response checkDelegationAccess(String elementID, String elementType, String variableName, String username, Locale lang) {
		logger.debug("checkDelegations INVOKED on elementId {} elementType {} variableName {} username {} ", elementID, elementType, variableName, username);

		Response response = new Response(false, null, null);

		// if there are any delegation to elementID
		List<Delegation> mydelegations = delegationRepo.getDelegationDelegatorFromAppId(elementID, variableName, null, false, elementType);
		List<Delegation> orderedDelegations = Delegation.orderDelegations(mydelegations);
                
                List<String> groupnames = null;;
		for (Delegation d : orderedDelegations) {
                        logger.debug("check delegation {}", d);
			if ((d.getUsernameDelegated() != null) && (d.getUsernameDelegated().equals("ANONYMOUS"))) {
				response.setResult(true);
				response.setMessage(AccessRightType.PUBLIC.toString());
				response.setKind(d.getKind());
				return response;
			}

			if ((d.getUsernameDelegated() != null) && (d.getUsernameDelegated().equalsIgnoreCase(username))) {
				response.setResult(true);
				response.setMessage(AccessRightType.DELEGATED.toString());
				response.setKind(d.getKind());
				return response;
			}

			if (d.getGroupnameDelegated() != null) {
                            if(groupnames == null) {
                                    groupnames = lu.getGroupAndOUnames(username);
                                    logger.debug("LDAP username {} --> groups {}",username, groupnames);
                            }
                            if (groupnames.contains(d.getGroupnameDelegated())) {
				response.setResult(true);
				response.setMessage(AccessRightType.GROUP_DELEGATED.toString());// delegation to the organization the user belong
				response.setKind(d.getKind());
				return response;
                            }
			}
		}
                
                logger.debug("check devicegroups");

		// if the elementId belong to any groups that got some delegation
		List<DeviceGroupElement> dges = dgeRepo.findByElementIdAndElementTypeAndDeleteTimeIsNull(elementID, elementType);

		for (DeviceGroupElement dge : dges) {
			if (!delegationRepo.getPublicDelegationFromAppId(dge.getDeviceGroupId().toString(), variableName, null, false, ElementType.MYGROUP.toString()).isEmpty()) {
				response.setMessage(AccessRightType.MYGROUP_PUBLIC.toString());
				response.setResult(true);
				return response;
			}

			List<Delegation> mygroupDelegations = delegationRepo.getDelegationDelegatorFromAppId(dge.getDeviceGroupId().toString(), null, null, false, ElementType.MYGROUP.toString());
                        List<Delegation> orderedGroupDelegations = Delegation.orderDelegations(mygroupDelegations);
			for (Delegation d : orderedGroupDelegations) {
				if ((d.getUsernameDelegated() != null) && (d.getUsernameDelegated().equalsIgnoreCase(username))) {
					response.setResult(true);
					response.setMessage(AccessRightType.MYGROUP_DELEGATED.toString());
                                        response.setKind(d.getKind());
					return response;
				}
				
				List<String> groupnames = lu.getGroupAndOUnames(username);
				if ((d.getGroupnameDelegated() != null) && (groupnames.contains(d.getGroupnameDelegated()))) {
					response.setResult(true);
					response.setMessage(AccessRightType.GROUP_DELEGATED.toString());// mygroup delegation to the organization the user belong
					return response;
				}
			}
		}
		return response;
	}

	@Override
	public Response checkPublic(String elementId, String elementType, String variableName, Locale lang) {
		logger.debug("checkPublic INVOKED on elemntId {} elementType {} variableName {} ", elementId, elementType, variableName);

		Response response = new Response(false, null);

		// check if this elementId has a PUBLIC delegation
		if (!delegationRepo.getPublicDelegationFromAppId(elementId, variableName, null, false, elementType).isEmpty()) {
			response.setMessage(AccessRightType.PUBLIC.toString());
			response.setResult(true);
			return response;
		}

		// check if the groups belonging the elementId has a PUBLIC delegation
		List<DeviceGroupElement> dges = dgeRepo.findByElementIdAndElementTypeAndDeleteTimeIsNull(elementId, elementType);
		for (DeviceGroupElement dge : dges) {
			if (!delegationRepo.getPublicDelegationFromAppId(dge.getDeviceGroupId().toString(), variableName, null, false, "MyGroup").isEmpty()) {
				response.setMessage(AccessRightType.PUBLIC.toString());
				response.setResult(true);
				return response;
			}
		}

		return response;
	}
}
