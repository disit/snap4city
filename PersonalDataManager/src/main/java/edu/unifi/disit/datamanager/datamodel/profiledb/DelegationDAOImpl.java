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
package edu.unifi.disit.datamanager.datamodel.profiledb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.unifi.disit.datamanager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.datamanager.exception.LDAPException;

@Repository
@Transactional(readOnly = true)
public class DelegationDAOImpl implements DelegationDAOCustom {

	@Autowired
	LDAPUserDAO lu;

	@PersistenceContext
	EntityManager entityManager;

	@Override
	public List<Delegation> getDelegationDelegatedByUsername(String username, String variableName, String motivation, Boolean deleted, String groupnamefilter, Locale lang) throws LDAPException {

		List<String> groupnames = lu.getGroupAndOUnames(username);

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Delegation> criteria = cb.createQuery(Delegation.class);

		// mainquery
		Root<Delegation> delegationRoot = criteria.from(Delegation.class);
		criteria.select(delegationRoot);
		Path<Object> pathDelegatedGroup = delegationRoot.get("groupnameDelegated"); // field to map with groupname i belong
		Path<Object> pathDelegatorGroup = delegationRoot.get("usernameDelegator"); // field to map with usernameDelegator from groupnamefilter

		List<Predicate> predicates = getCommonPredicates(cb, delegationRoot, variableName, motivation, deleted);

		Predicate predicate1 = cb.conjunction();
		Predicate predicate2 = cb.conjunction();
		predicate1.getExpressions().add(cb.equal(delegationRoot.get("usernameDelegated"), username));// username
		if (groupnames.size() != 0) {
			predicate2.getExpressions().add(cb.in(pathDelegatedGroup).value(groupnames));// groupname i belong
			predicates.add(cb.or(predicate1, predicate2));
		} else {
			predicates.add(predicate1);
		}

		if (groupnamefilter != null) {
			List<String> usernameGroupnameFilter = lu.getUsernamesFromGroup(groupnamefilter, lang);
			predicates.add(cb.in(pathDelegatorGroup).value(usernameGroupnameFilter));// usernameDelegator from groupnamefilters
		}

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		return entityManager.createQuery(criteria).getResultList();
	}

	@Override
	public List<Delegation> getDelegationDelegatorByUsername(String username, String variableName, String motivation, Boolean deleted) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Delegation> criteria = cb.createQuery(Delegation.class);

		// mainquery
		Root<Delegation> delegationRoot = criteria.from(Delegation.class);
		criteria.select(delegationRoot);

		List<Predicate> predicates = getCommonPredicates(cb, delegationRoot, variableName, motivation, deleted);

		predicates.add(cb.equal(delegationRoot.get("usernameDelegator"), username));// username

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		return entityManager.createQuery(criteria).getResultList();
	}

	@Override
	public List<Delegation> getDelegationDelegatorFromAppId(String appId, String variableName, String motivation, Boolean deleted) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Delegation> criteria = cb.createQuery(Delegation.class);

		// mainquery
		Root<Delegation> delegationRoot = criteria.from(Delegation.class);
		criteria.select(delegationRoot);

		List<Predicate> predicates = getCommonPredicates(cb, delegationRoot, variableName, motivation, deleted);

		predicates.add(cb.equal(delegationRoot.get("elementId"), appId));// appId

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		return entityManager.createQuery(criteria).getResultList();
	}

	private List<Predicate> getCommonPredicates(CriteriaBuilder cb, Root<Delegation> dataRoot, String variableName, String motivation, Boolean deleted) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		if (variableName != null)
			predicates.add(cb.equal(dataRoot.get("variableName"), variableName));
		if (motivation != null)
			predicates.add(cb.equal(dataRoot.get("motivation"), motivation));
		if (!deleted)
			predicates.add(dataRoot.get("deleteTime").isNull());
		return predicates;
	}
}
