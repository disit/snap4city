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
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class DeviceGroupElementDAOImpl implements DeviceGroupElementDAOCustom {

	@PersistenceContext
	EntityManager entityManager;

	@Override
	public Page<DeviceGroupElement> findByDeviceGroupIdAndDeleteTimeIsNullFiltered(Long grpId, PageRequest pageable, String searchKey) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<DeviceGroupElement> criteria = cb.createQuery(DeviceGroupElement.class);
		Root<DeviceGroupElement> dataRoot = criteria.from(DeviceGroupElement.class);

		// SELECT
		criteria.select(dataRoot);

		// WHERE
		List<Predicate> commonPredicates = new ArrayList<>();

                commonPredicates.add(cb.equal(dataRoot.get("deviceGroupId"), grpId));
		commonPredicates.add(cb.isNull(dataRoot.get("deleteTime")));

		if (searchKey != null) {
			List<Predicate> filterPredicates = new ArrayList<>();
			filterPredicates.add(cb.like(dataRoot.<String>get("elementId"), "%" + searchKey + "%"));
			filterPredicates.add(cb.like(dataRoot.<String>get("elementType"), "%" + searchKey + "%"));
                        filterPredicates.add(cb.like(dataRoot.<String>get("elementName"), "%" + searchKey + "%"));
                        filterPredicates.add(cb.like(dataRoot.<String>get("username"), "%" + searchKey + "%"));
			commonPredicates.add(cb.or(filterPredicates.toArray(new Predicate[filterPredicates.size()])));
		}

		criteria.where(cb.and(commonPredicates.toArray(new Predicate[commonPredicates.size()])));

		// ORDER BY
		criteria.orderBy(QueryUtils.toOrders(pageable.getSort(), dataRoot, cb));

		List<DeviceGroupElement> listKpiData = entityManager.createQuery(criteria).setFirstResult(pageable.getPageNumber()*pageable.getPageSize())
				.setMaxResults(pageable.getPageSize()).getResultList();

		return new PageImpl<>(listKpiData, pageable, entityManager.createQuery(criteria).getResultList().size());

	}

	@Override
	public List<DeviceGroupElement> findByDeviceGroupIdAndDeleteTimeIsNullFiltered(Long grpId, String searchKey) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<DeviceGroupElement> criteria = cb.createQuery(DeviceGroupElement.class);
		Root<DeviceGroupElement> dataRoot = criteria.from(DeviceGroupElement.class);

		// SELECT
		criteria.select(dataRoot);

		// WHERE
		List<Predicate> commonPredicates = new ArrayList<>();

                commonPredicates.add(cb.equal(dataRoot.get("deviceGroupId"), grpId));
		commonPredicates.add(cb.isNull(dataRoot.get("deleteTime")));

		if (searchKey != null) {
			List<Predicate> filterPredicates = new ArrayList<>();
			filterPredicates.add(cb.like(dataRoot.<String>get("elementId"), "%" + searchKey + "%"));
			filterPredicates.add(cb.like(dataRoot.<String>get("elementType"), "%" + searchKey + "%"));
                        filterPredicates.add(cb.like(dataRoot.<String>get("elementName"), "%" + searchKey + "%"));
                        filterPredicates.add(cb.like(dataRoot.<String>get("kpiName"), "%" + searchKey + "%"));
			commonPredicates.add(cb.or(filterPredicates.toArray(new Predicate[filterPredicates.size()])));
		}

		criteria.where(cb.and(commonPredicates.toArray(new Predicate[commonPredicates.size()])));

		return entityManager.createQuery(criteria).getResultList();

	}

}