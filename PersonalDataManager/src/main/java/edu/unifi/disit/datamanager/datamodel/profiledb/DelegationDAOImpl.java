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

	// used for check existence before new insert
	@Override
	public List<Delegation> getSameDelegation(Delegation d) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Delegation> criteria = cb.createQuery(Delegation.class);

		// mainquery
		Root<Delegation> delegationRoot = criteria.from(Delegation.class);
		criteria.select(delegationRoot);

		List<Predicate> predicates = getCommonPredicates(cb, delegationRoot, d.getVariableName(), d.getMotivation(), false, d.getElementType());

		if (d.getUsernameDelegator() != null)
			predicates.add(cb.equal(cb.upper(delegationRoot.get("usernameDelegator")), d.getUsernameDelegator().toUpperCase()));

		if (d.getUsernameDelegated() != null)
			predicates.add(cb.equal(cb.upper(delegationRoot.get("usernameDelegated")), d.getUsernameDelegated().toUpperCase()));

		if (d.getElementId() != null)
			predicates.add(cb.equal(delegationRoot.get("elementId"), d.getElementId()));

		if (d.getElementType() != null)
			predicates.add(cb.equal(delegationRoot.get("elementType"), d.getElementType()));

		if (d.getDelegationDetails() != null)
			predicates.add(cb.equal(delegationRoot.get("delegationDetails"), d.getDelegationDetails()));

		if (d.getGroupnameDelegated() != null)
			predicates.add(cb.equal(delegationRoot.get("groupnameDelegated"), d.getGroupnameDelegated()));

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		return entityManager.createQuery(criteria).getResultList();
	}

	@Override
	public List<Delegation> getDelegationDelegatedByUsername(String username, String variableName, String motivation, Boolean deleted, String groupnamefilter, String elementType, Locale lang) throws LDAPException {

		List<String> groupnames = lu.getGroupAndOUnames(username);// organization the user belongs

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Delegation> criteria = cb.createQuery(Delegation.class);

		// mainquery
		Root<Delegation> delegationRoot = criteria.from(Delegation.class);
		criteria.select(delegationRoot);
		Path<Object> pathDelegatedGroup = delegationRoot.get("groupnameDelegated"); // field to map with groupname i belong
		Path<Object> pathDelegatorGroup = delegationRoot.get("usernameDelegator"); // field to map with usernameDelegator from groupnamefilter

		List<Predicate> predicates = getCommonPredicates(cb, delegationRoot, variableName, motivation, deleted, elementType);

		Predicate predicate1 = cb.conjunction();
		Predicate predicate2 = cb.conjunction();
		predicate1.getExpressions().add(cb.equal(cb.upper(delegationRoot.get("usernameDelegated")), username.toUpperCase()));// username
		if (!groupnames.isEmpty()) {
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
		// to be used instead of @Cachable
		// return entityManager.createQuery(criteria).setHint("org.hibernate.cacheable", true).setHint("org.hibernate.cacheRegion", "delegation").getResultList();
	}

	@Override
	public List<Delegation> getDelegationDelegatorByUsername(String username, String variableName, String motivation, Boolean deleted, String elementType) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Delegation> criteria = cb.createQuery(Delegation.class);

		// mainquery
		Root<Delegation> delegationRoot = criteria.from(Delegation.class);
		criteria.select(delegationRoot);

		List<Predicate> predicates = getCommonPredicates(cb, delegationRoot, variableName, motivation, deleted, elementType);

		predicates.add(cb.equal(cb.upper(delegationRoot.get("usernameDelegator")), username.toUpperCase()));// username

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		return entityManager.createQuery(criteria).getResultList();
	}

	@Override
	public List<Delegation> getDelegationDelegatorFromAppId(String appId, String variableName, String motivation, Boolean deleted, String elementType) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Delegation> criteria = cb.createQuery(Delegation.class);

		// mainquery
		Root<Delegation> delegationRoot = criteria.from(Delegation.class);
		criteria.select(delegationRoot);

		List<Predicate> predicates = getCommonPredicates(cb, delegationRoot, variableName, motivation, deleted, elementType);

		predicates.add(cb.equal(delegationRoot.get("elementId"), appId));// appId

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		return entityManager.createQuery(criteria).getResultList();
	}

	@Override
	public List<Delegation> getPublicDelegationFromAppId(String appId, String variableName, String motivation, Boolean deleted, String elementType) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Delegation> criteria = cb.createQuery(Delegation.class);

		// mainquery
		Root<Delegation> delegationRoot = criteria.from(Delegation.class);
		criteria.select(delegationRoot);

		List<Predicate> predicates = getCommonPredicates(cb, delegationRoot, variableName, motivation, deleted, elementType);

		predicates.add(cb.equal(delegationRoot.get("elementId"), appId));// appId
		predicates.add(cb.equal(delegationRoot.get("usernameDelegated"), "ANONYMOUS"));// specific user

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		return entityManager.createQuery(criteria).getResultList();
	}

	@Override
	public List<Delegation> getAllDelegations(String variableName, String motivation, String elementType) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Delegation> criteria = cb.createQuery(Delegation.class);

		// mainquery
		Root<Delegation> delegationRoot = criteria.from(Delegation.class);
		criteria.select(delegationRoot);

		List<Predicate> predicates = getCommonPredicates(cb, delegationRoot, variableName, motivation, null, elementType);

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		return entityManager.createQuery(criteria).getResultList();
	}

	private List<Predicate> getCommonPredicates(CriteriaBuilder cb, Root<Delegation> delegationRoot, String variableName, String motivation, Boolean deleted, String elementType) {
		List<Predicate> predicates = new ArrayList<>();
		if (variableName != null)
			predicates.add(cb.equal(delegationRoot.get("variableName"), variableName));
		if (motivation != null)
			predicates.add(cb.equal(delegationRoot.get("motivation"), motivation));
		if (elementType != null)// elementType can be null for backword compatibility
			predicates.add(cb.equal(delegationRoot.get("elementType"), elementType));
		if (!Boolean.TRUE.equals(deleted))
			predicates.add(delegationRoot.get("deleteTime").isNull());
		return predicates;
	}

}
