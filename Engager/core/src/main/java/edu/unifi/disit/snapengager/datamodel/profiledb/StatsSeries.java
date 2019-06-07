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
@Table(name = "stats_series")
public class StatsSeries {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Type(type = "timestamp")
	private Date created = new Date();
	private String organization;
	private String type;

	private String category;
	private Integer eng = 0;
	private Integer ita = 0;
	private Integer esp = 0;
	private Integer fra = 0;
	private Integer deu = 0;

	public StatsSeries() {
		super();
	}

	public StatsSeries(String type, String category) {
		this(new Date(), null, type, category);
	}

	public StatsSeries(String organization, String type, String category) {
		this(new Date(), organization, type, category);
	}

	public StatsSeries(Date created, String organization, String type, String category) {
		super();
		this.created = created;
		this.organization = organization;
		this.type = type;
		this.category = category;
	}

	public StatsSeries(Date created, String organization, String type, String category, Integer eng, Integer ita, Integer esp, Integer fra, Integer deu) {
		super();
		this.created = created;
		this.organization = organization;
		this.type = type;
		this.category = category;
		this.eng = eng;
		this.ita = ita;
		this.esp = esp;
		this.fra = fra;
		this.deu = deu;
	}

	public StatsSeries(Long id, Date created, String organization, String type, String category, Integer eng, Integer ita, Integer esp, Integer fra, Integer deu) {
		super();
		this.id = id;
		this.created = created;
		this.organization = organization;
		this.type = type;
		this.category = category;
		this.eng = eng;
		this.ita = ita;
		this.esp = esp;
		this.fra = fra;
		this.deu = deu;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Integer getEng() {
		return eng;
	}

	public void setEng(Integer eng) {
		this.eng = eng;
	}

	public Integer getIta() {
		return ita;
	}

	public void setIta(Integer ita) {
		this.ita = ita;
	}

	public Integer getEsp() {
		return esp;
	}

	public void setEsp(Integer esp) {
		this.esp = esp;
	}

	public Integer getFra() {
		return fra;
	}

	public void setFra(Integer fra) {
		this.fra = fra;
	}

	public Integer getDeu() {
		return deu;
	}

	public void setDeu(Integer deu) {
		this.deu = deu;
	}

	@Override
	public String toString() {
		return "StatsSeries [id=" + id + ", created=" + created + ", organization=" + organization + ", type=" + type + ", category=" + category + ", eng=" + eng + ", ita=" + ita + ", esp=" + esp + ", fra=" + fra + ", deu=" + deu + "]";
	}

	public void addEng(Integer count) {
		this.eng = this.eng + count;
	}

	public void addIta(Integer count) {
		this.ita = this.ita + count;
	}

	public void addEsp(Integer count) {
		this.esp = this.esp + count;
	}

	public void addFra(Integer count) {
		this.fra = this.fra + count;
	}

	public void addDeu(Integer count) {
		this.deu = this.deu + count;
	}

}