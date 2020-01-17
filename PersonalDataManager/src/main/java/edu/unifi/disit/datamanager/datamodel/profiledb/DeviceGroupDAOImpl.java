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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class DeviceGroupDAOImpl implements DeviceGroupDAOCustom {

	@PersistenceContext
	EntityManager entityManager;

	@Override
	public Page<DeviceGroup> findKPIDataFilteredPage(String username, String highLevelType, String searchKey, Pageable pageable) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<DeviceGroup> criteria = cb.createQuery(DeviceGroup.class);
		Root<DeviceGroup> dataRoot = criteria.from(DeviceGroup.class);

		// SELECT
		criteria.select(dataRoot);

		// WHERE
		List<Predicate> commonPredicates = new ArrayList<>();

		if (username != null) {
			commonPredicates.add(cb.equal(dataRoot.get("username"), username));
		}
		if (highLevelType != null) {
			commonPredicates.add(cb.equal(dataRoot.get("highLevelType"), highLevelType));
		}
		commonPredicates.add(cb.isNull(dataRoot.get("deleteTime")));

		if (searchKey != null) {
			List<Predicate> filterPredicates = new ArrayList<>();
			if (highLevelType == null) {
				filterPredicates.add(cb.like(dataRoot.<String>get("highLevelType"), "%" + searchKey + "%"));
			}
			filterPredicates.add(cb.like(dataRoot.<String>get("name"), "%" + searchKey + "%"));
			filterPredicates.add(cb.like(dataRoot.<String>get("description"), "%" + searchKey + "%"));		
			filterPredicates.add(cb.like(dataRoot.<String>get("ownership"), "%" + searchKey + "%"));
			filterPredicates.add(cb.like(dataRoot.<String>get("username"), "%" + searchKey + "%"));
			filterPredicates.add(cb.like(dataRoot.<Long>get("id").as(String.class), "%" + searchKey + "%"));

			commonPredicates.add(cb.or(filterPredicates.toArray(new Predicate[filterPredicates.size()])));
		}

		criteria.where(cb.and(commonPredicates.toArray(new Predicate[commonPredicates.size()])));

		// ORDER BY
		criteria.orderBy(QueryUtils.toOrders(pageable.getSort(), dataRoot, cb));

		List<DeviceGroup> listKpiData = entityManager.createQuery(criteria).setFirstResult(pageable.getPageNumber()*pageable.getPageSize())
				.setMaxResults(pageable.getPageSize()).getResultList();

		return new PageImpl<>(listKpiData, pageable, entityManager.createQuery(criteria).getResultList().size());

	}

	@Override
	public List<DeviceGroup> findKPIDataFilteredList(String username, String highLevelType, String searchKey) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<DeviceGroup> criteria = cb.createQuery(DeviceGroup.class);
		Root<DeviceGroup> dataRoot = criteria.from(DeviceGroup.class);

		// SELECT
		criteria.select(dataRoot);

		// WHERE
		List<Predicate> commonPredicates = new ArrayList<>();

		if (username != null) {
			commonPredicates.add(cb.equal(dataRoot.get("username"), username));
		}
		if (highLevelType != null) {
			commonPredicates.add(cb.equal(dataRoot.get("highLevelType"), highLevelType));
		}
		commonPredicates.add(cb.isNull(dataRoot.get("deleteTime")));

		if (searchKey != null) {
			List<Predicate> filterPredicates = new ArrayList<>();
			if (highLevelType == null) {
				filterPredicates.add(cb.like(dataRoot.<String>get("highLevelType"), "%" + searchKey + "%"));
			}
			filterPredicates.add(cb.like(dataRoot.<String>get("name"), "%" + searchKey + "%"));
			filterPredicates.add(cb.like(dataRoot.<String>get("description"), "%" + searchKey + "%"));
			filterPredicates.add(cb.like(dataRoot.<String>get("ownership"), "%" + searchKey + "%"));
			filterPredicates.add(cb.like(dataRoot.<String>get("username"), "%" + searchKey + "%"));
			filterPredicates.add(cb.like(dataRoot.<Long>get("id").as(String.class), "%" + searchKey + "%"));

			commonPredicates.add(cb.or(filterPredicates.toArray(new Predicate[filterPredicates.size()])));
		}

		criteria.where(cb.and(commonPredicates.toArray(new Predicate[commonPredicates.size()])));

		return entityManager.createQuery(criteria).getResultList();

	}

}