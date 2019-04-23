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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.unifi.disit.datamanager.datamodel.ldap.LDAPUserDAO;

@Repository
@Transactional(readOnly = true)
public class DataDAOImpl implements DataDAOCustom {

	@Autowired
	LDAPUserDAO lu;

	@PersistenceContext
	EntityManager entityManager;

	@Override
	public List<Data> getDataByUsername(String username, String variableName, String motivation, Date from, Date to, Integer first, Integer last) {

		// TODO do we need to check the list of the ownership of the user? here we just use the "username" field of the data table, so if the user will give the ownership to someone else, the data is not update (imho correctly)
		List<Data> toreturn;

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Data> criteria = cb.createQuery(Data.class);

		// mainquery
		Root<Data> dataRoot = criteria.from(Data.class);
		criteria.select(dataRoot);

		if (last != 0) {
			criteria.orderBy(cb.desc(dataRoot.get("dataTime")));
		} else {
			criteria.orderBy(cb.asc(dataRoot.get("dataTime")));
		}

		List<Predicate> predicates = getCommonPredicates(cb, dataRoot, variableName, motivation, from, to);

		if (!username.equals("ANY"))
			predicates.add(cb.equal(dataRoot.get("username"), username));// username

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		toreturn = getResults(criteria, first, last);

		return toreturn;
	}

	@Override
	public List<Data> getDataByUsernameDelegated(String username, String variableName, String motivation, Date from, Date to, Integer first, Integer last) {

		List<Data> toreturn;

		List<String> groupnames = lu.getGroupAndOUnames(username);

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Data> criteria = cb.createQuery(Data.class);

		// subquery
		Subquery<Delegation> subquery = criteria.subquery(Delegation.class);
		Root<Delegation> delegationRoot = subquery.from(Delegation.class);
		subquery.select(delegationRoot.get("elementId")); // field to map with main-query
		Path<Object> pathGroup = delegationRoot.get("groupnameDelegated"); // field to map with group

		final List<Predicate> predicatesSubquery = new ArrayList<Predicate>();// group
		Predicate predicate1 = cb.conjunction();
		Predicate predicate2 = cb.conjunction();
		predicate1.getExpressions().add(cb.equal(delegationRoot.get("usernameDelegated"), username));
		if (groupnames.size() != 0) {
			predicate2.getExpressions().add(cb.in(pathGroup).value(groupnames));
			predicatesSubquery.add(cb.or(predicate1, predicate2));
		} else {
			predicatesSubquery.add(predicate1);
		}

		if (variableName != null)
			predicatesSubquery.add(cb.equal(delegationRoot.get("variableName"), variableName));

		if (motivation != null)
			predicatesSubquery.add(cb.equal(delegationRoot.get("motivation"), motivation));

		subquery.where(cb.and(predicatesSubquery.toArray(new Predicate[predicatesSubquery.size()])));

		// mainquery
		Root<Data> dataRoot = criteria.from(Data.class);
		Path<Object> path = dataRoot.get("appId"); // field to map with sub-query
		criteria.select(dataRoot);

		if (last != 0) {
			criteria.orderBy(cb.desc(dataRoot.get("dataTime")));
		} else {
			criteria.orderBy(cb.asc(dataRoot.get("dataTime")));
		}

		List<Predicate> predicates = getCommonPredicates(cb, dataRoot, variableName, motivation, from, to);

		predicates.add(cb.in(path).value(subquery));// subquery

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		toreturn = getResults(criteria, first, last);

		// annotations management
		List<Data> annotations = getDataByUsernameDelegatedAnnotation(username, variableName, motivation, from, to, first, last);

		if (annotations.size() != 0) {
			toreturn.addAll(annotations);

			if ((first != 0) && (toreturn.size() != first)) {
				Collections.sort(toreturn);
				return toreturn.subList(0, first);
			} else if ((last != 0) && (toreturn.size() != last)) {
				Collections.sort(toreturn, Collections.reverseOrder());
				return toreturn.subList(0, last);
			}
		}

		return toreturn;
	}

	private List<Data> getDataByUsernameDelegatedAnnotation(String username, String variableName, String motivation, Date from, Date to, Integer first, Integer last) {

		List<Data> toreturn;

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Data> criteria = cb.createQuery(Data.class);

		// subquery
		Subquery<Delegation> subquery = criteria.subquery(Delegation.class);
		Root<Delegation> delegationRoot = subquery.from(Delegation.class);
		subquery.select(delegationRoot.get("usernameDelegator")); // field to map with main-query

		final List<Predicate> predicatesSubquery = new ArrayList<Predicate>();

		predicatesSubquery.add(cb.equal(delegationRoot.get("usernameDelegated"), username));

		if (variableName != null)
			predicatesSubquery.add(cb.equal(delegationRoot.get("variableName"), variableName));

		if (motivation != null)
			predicatesSubquery.add(cb.equal(delegationRoot.get("motivation"), motivation));

		subquery.where(cb.and(predicatesSubquery.toArray(new Predicate[predicatesSubquery.size()])));

		// mainquery
		Root<Data> dataRoot = criteria.from(Data.class);
		Path<Object> path = dataRoot.get("username"); // field to map with sub-query
		criteria.select(dataRoot);

		if (last != 0) {
			criteria.orderBy(cb.desc(dataRoot.get("dataTime")));
		} else {
			criteria.orderBy(cb.asc(dataRoot.get("dataTime")));
		}

		List<Predicate> predicates = getCommonPredicates(cb, dataRoot, variableName, motivation, from, to);

		predicates.add(dataRoot.get("appId").isNull());// annotation
		predicates.add(cb.in(path).value(subquery));// subquery

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		toreturn = getResults(criteria, first, last);

		return toreturn;
	}

	@Override
	public List<Data> getDataByAppId(String appId, String appOwner, String variableName, String motivation, Date from, Date to, Integer first, Integer last) {

		List<Data> toreturn;

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Data> criteria = cb.createQuery(Data.class);
		Root<Data> dataRoot = criteria.from(Data.class);
		criteria.select(dataRoot);

		if (last != 0) {
			criteria.orderBy(cb.desc(dataRoot.get("dataTime")));
		} else {
			criteria.orderBy(cb.asc(dataRoot.get("dataTime")));
		}

		List<Predicate> predicates = getCommonPredicates(cb, dataRoot, variableName, motivation, from, to);

		predicates.add(cb.equal(dataRoot.get("username"), appOwner));// username
		predicates.add(cb.equal(dataRoot.get("appId"), appId));// appId

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		toreturn = getResults(criteria, first, last);

		// annotations management
		List<Data> annotations = getDataByAppIdAnnotation(appOwner, variableName, motivation, from, to, first, last);

		if (annotations.size() != 0) {
			toreturn.addAll(annotations);

			if ((first != 0) && (toreturn.size() != first)) {
				Collections.sort(toreturn);
				return toreturn.subList(0, first);
			} else if ((last != 0) && (toreturn.size() != last)) {
				Collections.sort(toreturn, Collections.reverseOrder());
				return toreturn.subList(0, last);
			}
		}

		return toreturn;
	}

	private List<Data> getDataByAppIdAnnotation(String username, String variableName, String motivation, Date from, Date to, Integer first, Integer last) {

		List<Data> toreturn;

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Data> criteria = cb.createQuery(Data.class);

		// mainquery
		Root<Data> dataRoot = criteria.from(Data.class);
		criteria.select(dataRoot);

		if (last != 0) {
			criteria.orderBy(cb.desc(dataRoot.get("dataTime")));
		} else {
			criteria.orderBy(cb.asc(dataRoot.get("dataTime")));
		}

		List<Predicate> predicates = getCommonPredicates(cb, dataRoot, variableName, motivation, from, to);

		predicates.add(dataRoot.get("appId").isNull());// annotation
		predicates.add(cb.equal(dataRoot.get("username"), username));// username

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

		toreturn = getResults(criteria, first, last);

		return toreturn;
	}

	private List<Data> getResults(CriteriaQuery<Data> criteria, Integer first, Integer last) {
		if (first != 0) {
			return entityManager.createQuery(criteria).setMaxResults(first).getResultList();
		} else if (last != 0) {
			return entityManager.createQuery(criteria).setMaxResults(last).getResultList();
		} else
			return entityManager.createQuery(criteria).getResultList();
	}

	private List<Predicate> getCommonPredicates(CriteriaBuilder cb, Root<Data> dataRoot, String variableName, String motivation, Date from, Date to) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		if (from != null)
			predicates.add(cb.greaterThan(dataRoot.get("dataTime"), from));
		if (to != null)
			predicates.add(cb.lessThan(dataRoot.get("dataTime"), to));
		if (variableName != null)
			predicates.add(cb.equal(dataRoot.get("variableName"), variableName));
		if (motivation != null)
			predicates.add(cb.equal(dataRoot.get("motivation"), motivation));

		predicates.add(dataRoot.get("deleteTime").isNull());

		return predicates;
	}
}
