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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DelegationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;

@Service
public class DelegationServiceImpl implements IDelegationService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private MessageSource messages;

	@Autowired
	DelegationDAO delegationRepo;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Override
	// same as below
	public List<Delegation> getDelegationsDelegatedForUsername(String username, String variableName, String motivation, Locale lang) {
		logger.debug("get delegations delegated from username {} variableName {} motivation {}", username, variableName, motivation);

		List<Delegation> toreturn = new ArrayList<Delegation>();

		if (variableName == null) {
			if (motivation == null) {
				toreturn = delegationRepo.findByUsernameDelegatedAndDeleteTimeIsNull(username);
			} else {
				toreturn = delegationRepo.findByUsernameDelegatedAndMotivationAndDeleteTimeIsNull(username, motivation);
			}
		} else {
			if (motivation == null) {
				toreturn = delegationRepo.findByUsernameDelegatedAndVariableNameAndDeleteTimeIsNull(username, variableName);
			} else {
				toreturn = delegationRepo.findByUsernameDelegatedAndVariableNameAndMotivationAndDeleteTimeIsNull(username, variableName, motivation);
			}
		}

		return toreturn;
	}

	@Override
	// same as above
	public List<Delegation> getDelegationsDelegatorForUsername(String username, String variableName, String motivation, Locale lang) {
		logger.debug("get delegations delegator from username {} variableName {} motivation {}", username, variableName, motivation);

		List<Delegation> toreturn = new ArrayList<Delegation>();

		if (variableName == null) {
			if (motivation == null) {
				toreturn = delegationRepo.findByUsernameDelegatorAndDeleteTimeIsNull(username);
			} else {
				toreturn = delegationRepo.findByUsernameDelegatorAndMotivationAndDeleteTimeIsNull(username, motivation);
			}
		} else {
			if (motivation == null) {
				toreturn = delegationRepo.findByUsernameDelegatorAndVariableNameAndDeleteTimeIsNull(username, variableName);
			} else {
				toreturn = delegationRepo.findByUsernameDelegatorAndVariableNameAndMotivationAndDeleteTimeIsNull(username, variableName, motivation);
			}
		}

		return toreturn;
	}

	@Override
	// same as above
	public List<Delegation> getDelegationsDelegatorFromApp(String appId, String variableName, String motivation, Locale lang) {
		logger.debug("get delegations delegated from appid {} variableName {} motivation {}", appId, variableName, motivation);

		List<Delegation> toreturn = new ArrayList<Delegation>();

		if (variableName == null) {
			if (motivation == null) {
				toreturn = delegationRepo.findByElementIdAndDeleteTimeIsNull(appId);
			} else {
				toreturn = delegationRepo.findByElementIdAndMotivationAndDeleteTimeIsNull(appId, motivation);
			}
		} else {
			if (motivation == null) {
				toreturn = delegationRepo.findByElementIdAndVariableNameAndDeleteTimeIsNull(appId, variableName);
			} else {
				toreturn = delegationRepo.findByElementIdAndVariableNameAndMotivationAndDeleteTimeIsNull(appId, variableName, motivation);
			}
		}

		return toreturn;
	}

	@Override
	public List<Delegation> getDelegationsDelegatedFromApp(String appId, String variableName, String motivation, Locale lang) {
		logger.debug("get delegations delegated from appid {} variableName {} motivation {}", appId, variableName, motivation);

		// retrieve the user belonging this appId
		Ownership usernameDelegated = ownershipRepo.findByElementId(appId);
		if (usernameDelegated == null)
			return null;

		return getDelegationsDelegatedForUsername(usernameDelegated.getUsername(), variableName, motivation, lang);
	}

	// @Override
	// public boolean check(String appId, Integer uidDelegator, String variableName, String motivation) {
	//
	// logger.debug("checking delegation {} {}", appId, uidDelegator);
	//
	// // Integer uidDelegator = iotapplicationRepo.getDrupalUid(delegatorUID);
	// Ownership uidDelegated = ownershipRepo.findByElementId(appId);
	//
	// if (uidDelegated == null)
	// return false;
	//
	// String uidDelegated_username = uidDelegated.getUsername();
	//
	// if ((uidDelegator == null) || (uidDelegated_username == null))
	// return false;
	//
	// // retrieve all the delegation from delegator to delegated
	// List<Delegation> delegations = delegationRepo.findByUsernameDelegatorAndUsernameDelegated(uidDelegator.toString(), uidDelegated_username);
	//
	// if (delegations.size() == 0) {
	// logger.debug("no delegation found, return false");
	// return false;
	// }
	//
	// for (Delegation d : delegations) {
	//
	// logger.debug("checking delegation {}", d);
	//
	// if ((variableName == null) && (motivation == null)) {
	// if ((d.getVariableName() == null) && (d.getMotivation() == null)) {
	// return true;
	// }
	// } else if ((variableName == null) && (motivation != null)) {
	// if ((d.getVariableName() == null) && ((d.getMotivation() == null) || (d.getMotivation().equals(motivation)))) {
	// return true;
	// }
	// } else if ((variableName != null) && (motivation == null)) {
	// if (((d.getVariableName() == null) || (d.getVariableName().equals(variableName))) && (d.getMotivation() == null)) {
	// return true;
	// }
	// } else if ((variableName != null) && (motivation != null)) {
	// if (((d.getVariableName() == null) || (d.getVariableName().equals(variableName))) && ((d.getMotivation() == null) || (d.getMotivation().equals(motivation)))) {
	// return true;
	// }
	// }
	// }
	//
	// logger.debug("no valid delegation found, return false");
	//
	// return false;
	// }

	@Override
	// check if someone delegated "username" to access the variablename from elementId
	public Response checkDelegations(String username, String variableName, String elementID, Locale lang) {
		logger.debug("checking delegations from username {} variableName {} elementId {} ", username, variableName, elementID);

		Response response = new Response(false, null);

		// check if i'm the owner
		List<Ownership> myownership = ownershipRepo.findByUsername(username);

		for (Ownership o : myownership) {
			if (o.getElementId().equals(elementID)) {
				response.setResult(true);
				response.setMessage("OWNER");
			}
		}

		// otherwise
		List<Delegation> mydelegations = getDelegationsDelegatedForUsername(username, variableName, null, lang);

		for (Delegation d : mydelegations) {
			if (d.getElementId().equals(elementID)) {
				response.setResult(true);
				response.setMessage("DELEGATED");
			}
		}

		return response;
	}

	@Override
	public Delegation postDelegationFromUser(String username, Delegation delegation, Locale lang) throws DelegationNotValidException {

		logger.debug("postDelegation from user {} INVOKED on {}", username, delegation);

		// check if id is specified
		if (delegation.getId() != null)
			throw new DelegationNotValidException(messages.getMessage("postdelegation.ko.idnotnull", null, lang));

		// check if the username is null, eventually populate
		if (delegation.getUsernameDelegator() == null)
			delegation.setUsernameDelegator(username);
		// check if the username is different
		else if (!delegation.getUsernameDelegator().equals(username))
			throw new DelegationNotValidException(messages.getMessage("postdelegation.ko.usernamedifferent", null, lang));

		// check username exist
		List<Ownership> ownership = ownershipRepo.findByUsername(username);
		if ((ownership == null) || (ownership.size() == 0))
			throw new DelegationNotValidException(messages.getMessage("postdelegation.ko.usernamenotrecognized", null, lang));

		// update delegation insert time
		delegation.setInsertTime(new Date());
		delegation.setDeleteTime(null);

		return delegationRepo.save(delegation);
	}

	@Override
	public void deleteDelegationFromUser(String username, Long delegationId, Locale lang) throws DelegationNotValidException {
		logger.debug("deleteDelegation from user {} INVOKED on {}", username, delegationId);

		Delegation todelete = delegationRepo.findByIdAndDeleteTimeIsNull(delegationId);

		if (todelete == null)
			throw new DelegationNotValidException(messages.getMessage("deletedelegation.ko.idnotrecognized", null, lang));

		// check username exist
		List<Ownership> ownership = ownershipRepo.findByUsername(username);
		if ((ownership == null) || (ownership.size() == 0))
			throw new DelegationNotValidException(messages.getMessage("deletedelegation.ko.usernamenotrecognized", null, lang));

		// minor enforcment, you've to be the owner to delete
		if (!todelete.getUsernameDelegator().equals(username))
			throw new DelegationNotValidException(messages.getMessage("deletedelegation.ko.delegationnotowner", null, lang));

		todelete.setDeleteTime(new Date());

		delegationRepo.save(todelete);
	}

	@Override
	public void deleteAllDelegationFromApp(String appId, Locale lang) {
		logger.debug("delete all delegations from appid {} ", appId);

		// TODO manage enforcement

		List<Delegation> delegations = delegationRepo.findByElementIdAndDeleteTimeIsNull(appId);

		for (Delegation d : delegations) {
			d.setDeleteTime(new Date());
			delegationRepo.save(d);
		}

	}

	@Override
	public Delegation putDelegationFromUser(String username, Delegation delegation, Long delegationId, Locale lang) throws DelegationNotValidException {
		logger.debug("put delegations from username {} delegation {} delegationId {}", username, delegation, delegationId);

		// TODO manage enforcement

		// check id present and equal
		if ((delegation == null) || (delegationId == null) || (delegation.getId() == null) || (delegation.getId() != delegationId))
			throw new DelegationNotValidException(messages.getMessage("deletedelegation.ko.delegationnotowner", null, lang));

		return delegationRepo.save(delegation);
	}
}
