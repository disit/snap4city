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
package edu.unifi.disit.datamanager.datamodel.profiledb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class KPIValueDAOImpl implements KPIValueDAOCustom {

	@PersistenceContext
	EntityManager entityManager;

	@Override
	public List<KPIValue> findByKpiIdNoPagesWithLimit(Long kpiId, Date from, Date to, Integer first, Integer last) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<KPIValue> criteria = cb.createQuery(KPIValue.class);
		Root<KPIValue> dataRoot = criteria.from(KPIValue.class);
		criteria.select(dataRoot);

		if (last != null) {
			criteria.orderBy(cb.desc(dataRoot.get("dataTime")));
		} else {
			criteria.orderBy(cb.asc(dataRoot.get("dataTime")));
		}

		List<Predicate> predicates = getCommonPredicates(cb, dataRoot, from, to);

		predicates.add(cb.equal(dataRoot.get("kpiId"), kpiId));//
		predicates.add(cb.isNull(dataRoot.get("deleteTime")));

		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));


		return getResults(criteria, first, last);
	}

	private List<KPIValue> getResults(CriteriaQuery<KPIValue> criteria, Integer first, Integer last) {
		if (first != null) {
			return entityManager.createQuery(criteria).setMaxResults(first).getResultList();
		} else if (last != null) {
			return entityManager.createQuery(criteria).setMaxResults(last).getResultList();
		} else {
			return entityManager.createQuery(criteria).getResultList();
		}
	}

	private List<Predicate> getCommonPredicates(CriteriaBuilder cb, Root<KPIValue> dataRoot, Date from, Date to) {
		List<Predicate> predicates = new ArrayList<>();
		if (from != null)
			predicates.add(cb.greaterThan(dataRoot.get("dataTime"), from));
		if (to != null)
			predicates.add(cb.lessThan(dataRoot.get("dataTime"), to));
		return predicates;
	}
}