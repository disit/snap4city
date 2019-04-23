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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DelegationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DelegationNotValidException;
import edu.unifi.disit.datamanager.exception.LDAPException;

@Service
public class DelegationServiceImpl implements IDelegationService {

	@Autowired
	LDAPUserDAO lu;

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private MessageSource messages;

	@Autowired
	DelegationDAO delegationRepo;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Autowired
	ICredentialsService credentialsService;

	@Override
	public Delegation getDelegationById(Long id, Locale lang) throws NoSuchMessageException, CredentialsException {
		logger.debug("getDelegationById INVOKED on id {}", id);
		return delegationRepo.findOne(id);
	}

	@Override
	public Page<Delegation> findByElementId(String elementId, Pageable pageable)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByElementId INVOKED on elementId {}", elementId);
		return delegationRepo.findByElementIdAndDeleteTimeIsNull(elementId, pageable);
	}

	@Override
	public Page<Delegation> findByElementIdWithoutAnonymous(String elementId, Pageable pageable)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByElementId INVOKED on elementId {} usernameDelegated", elementId, "ANONYMOUS");
		return delegationRepo.findByElementIdAndDeleteTimeIsNullAndUsernameDelegatedNotLike(elementId, "ANONYMOUS", pageable);
	}

	@Override
	public List<Delegation> findByElementIdNoPages(String elementId)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByElementIdNoPages INVOKED on elementId {}", elementId);
		return delegationRepo.findByElementIdAndDeleteTimeIsNull(elementId);
	}

	@Override
	public List<Delegation> findByElementIdNoPagesWithoutAnonymous(String elementId)
			throws NoSuchMessageException, CredentialsException {
		logger.debug("findByElementId INVOKED on elementId {} usernameDelegated", elementId, "ANONYMOUS");
		return delegationRepo.findByElementIdAndDeleteTimeIsNullAndUsernameDelegatedNotLike(elementId, "ANONYMOUS");
	}

	@Override
	// same as below
	public List<Delegation> getDelegationsDelegatedForUsername(String username, String variableName, String motivation, Boolean deleted, String groupname, String elementType, Locale lang)
			throws NoSuchMessageException, CredentialsException, LDAPException {
		logger.debug("getDelegationsDelegatedForUsername INVOKED on username {} variableName {} motivation {} deleted {} groupname {}", username, variableName, motivation, deleted, groupname);

		if (!username.equals("ANONYMOUS"))// avoid check credentials for PUBLIC elements
			credentialsService.checkUsernameCredentials(username, lang);

		return delegationRepo.getDelegationDelegatedByUsername(username, variableName, motivation, deleted, groupname, elementType, lang);
	}

	@Override
	// same as above
	public List<Delegation> getDelegationsDelegatorForUsername(String username, String variableName, String motivation, Boolean deleted, String elementType, Locale lang) throws NoSuchMessageException, CredentialsException {
		logger.debug("getDelegationsDelegatorForUsername INVOKED on username {} variableName {} motivation {} deleted {}", username, variableName, motivation, deleted);

		credentialsService.checkUsernameCredentials(username, lang);

		return delegationRepo.getDelegationDelegatorByUsername(username, variableName, motivation, deleted, elementType);
	}

	@Override
	// same as above
	public List<Delegation> getDelegationsDelegatorFromApp(String appId, String variableName, String motivation, Boolean deleted, String elementType, Locale lang) throws NoSuchMessageException, CredentialsException {
		logger.debug("getDelegationsDelegatorFromApp INVOKED on appid {} variableName {} motivation {} deleted {}", appId, variableName, motivation, deleted);

		credentialsService.checkAppIdCredentials(appId, lang);

		return delegationRepo.getDelegationDelegatorFromAppId(appId, variableName, motivation, deleted, elementType);

	}

	@Override
	public List<Delegation> getDelegationsDelegatedFromApp(String appId, String variableName, String motivation, Boolean deleted, String appOwner, String groupname, String elementType, Locale lang)
			throws DelegationNotValidException, NoSuchMessageException, CredentialsException, LDAPException {
		logger.debug("getDelegationsDelegatedFromApp INVOKED on appid {} variableName {} motivation {} appOwner {} deleted {}", appId, variableName, motivation, appOwner, deleted);

		credentialsService.checkAppIdCredentials(appId, lang);

		// retrieve the user belonging this appId
		if (appOwner == null) {
			List<Ownership> owns = ownershipRepo.findByElementId(appId);
			if (owns.size() == 0)
				throw new DelegationNotValidException(messages.getMessage("getdelegation.ko.appidnotrecognized", null, lang));
			else if (owns.size() > 1)
				throw new DelegationNotValidException(messages.getMessage("getdelegation.ko.multipleownershipdetected", null, lang));
			appOwner = owns.get(0).getUsername();
		}

		return delegationRepo.getDelegationDelegatedByUsername(appOwner, variableName, motivation, deleted, groupname, elementType, lang);
	}

	@Override
	public Delegation postDelegationFromUser(String username, Delegation delegation, Locale lang) throws DelegationNotValidException, NoSuchMessageException, CredentialsException {
		logger.debug("postDelegationFromUser INVOKED on username {} delegation {}", username, delegation);

		credentialsService.checkUsernameCredentials(username, lang);

		// check if id is specified
		if (delegation.getId() != null)
			throw new DelegationNotValidException(messages.getMessage("postdelegation.ko.idnotnull", null, lang));

		// check if the username is null, eventually populate
		if (delegation.getUsernameDelegator() == null)
			delegation.setUsernameDelegator(username);
		// check if the username is different
		else if (!delegation.getUsernameDelegator().equals(username))
			throw new DelegationNotValidException(messages.getMessage("postdelegation.ko.usernamedifferent", null, lang));

		// check username delegated exist
		if (((delegation.getUsernameDelegated() == null) || ((!delegation.getUsernameDelegated().equals("ANONYMOUS")) && (!lu.usernameExist(delegation.getUsernameDelegated()))))
				// check groupname exist
				&& ((delegation.getGroupnameDelegated() == null) || ((!lu.groupnameExist(delegation.getGroupnameDelegated())))))
			throw new DelegationNotValidException(messages.getMessage("postdelegation.ko.delegatednotrecognized", null, lang));

		// check that this delegation does not exist already
		if (delegationRepo.getSameDelegation(delegation, lang).size() != 0)
			throw new DelegationNotValidException(messages.getMessage("postdelegation.ko.delegationalreadypresent", null, lang));

		// update delegation insert time
		delegation.setInsertTime(new Date());
		delegation.setDeleteTime(null);

		return delegationRepo.save(delegation);
	}

	@Override
	public void deleteDelegationFromUser(String username, Long delegationId, Locale lang) throws DelegationNotValidException, NoSuchMessageException, CredentialsException {
		logger.debug("deleteDelegationFromUser INVOKED on username {} delegationId {}", username, delegationId);

		credentialsService.checkUsernameCredentials(username, lang);

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
	public void deleteAllDelegationFromApp(String appId, Locale lang) throws NoSuchMessageException, CredentialsException {
		logger.debug("deleteAllDelegationFromApp INVOKED on appid {} ", appId);

		credentialsService.checkAppIdCredentials(appId, lang);

		List<Delegation> delegations = delegationRepo.findByElementIdAndDeleteTimeIsNull(appId);

		for (Delegation d : delegations) {
			d.setDeleteTime(new Date());
			delegationRepo.save(d);
		}
	}

	@Override
	public Delegation putDelegationFromUser(String username, Delegation delegation, Long delegationId, Locale lang) throws DelegationNotValidException, NoSuchMessageException, CredentialsException {
		logger.debug("putDelegationFromUser INVOKED on username {} delegation {} delegationId {} {}", username, delegation, delegationId);

		credentialsService.checkUsernameCredentials(username, lang);

		// check id present and equal
		if ((delegation == null) || (delegationId == null) || (delegation.getId() == null) || (delegation.getId().longValue() != delegationId.longValue()))
			throw new DelegationNotValidException(messages.getMessage("putdelegation.ko.delegationnotowner", null, lang));

		// check username delegated exist
		if (((delegation.getUsernameDelegated() == null) || ((!delegation.getUsernameDelegated().equals("ANONYMOUS")) && (!lu.usernameExist(delegation.getUsernameDelegated()))))
				// check groupname exist
				&& ((delegation.getGroupnameDelegated() == null) || ((!lu.groupnameExist(delegation.getGroupnameDelegated())))))
			throw new DelegationNotValidException(messages.getMessage("postdelegation.ko.delegatednotrecognized", null, lang));

		return delegationRepo.save(delegation);
	}

	@Override
	public List<Delegation> stripUsernameDelegatedNull(List<Delegation> delegations) {
		List<Delegation> toreturn = new ArrayList<Delegation>();
		for (Delegation d : delegations) {
			if (d.getUsernameDelegated() != null)
				toreturn.add(d);
		}
		return toreturn;
	}

}
