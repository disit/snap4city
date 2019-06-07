/* Snap4City Engager (SE)
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
package edu.unifi.disit.snapengager.datamodel.profiledb;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "stats_survey1_response_count")
public class StatsSurvey1ResponseCount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Integer conto = 0;
	private String organization;
	@Type(type = "timestamp")
	private Date created = new Date();

	public StatsSurvey1ResponseCount() {
		super();
	}

	public StatsSurvey1ResponseCount(String organization, Integer conto) {
		this(conto, organization, new Date());
	}

	public StatsSurvey1ResponseCount(Integer conto, String organization, Date created) {
		super();
		this.conto = conto;
		this.organization = organization;
		this.created = created;
	}

	public StatsSurvey1ResponseCount(Long id, Integer conto, String organization, Date created) {
		super();
		this.id = id;
		this.conto = conto;
		this.organization = organization;
		this.created = created;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getConto() {
		return conto;
	}

	public void setConto(Integer conto) {
		this.conto = conto;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	@Override
	public String toString() {
		return "StatsSurvey1Response [id=" + id + ", conto=" + conto + ", organization=" + organization + ", created=" + created + "]";
	}

}