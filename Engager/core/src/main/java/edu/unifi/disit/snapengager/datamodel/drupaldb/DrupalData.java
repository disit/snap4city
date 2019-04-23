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
package edu.unifi.disit.snapengager.datamodel.drupaldb;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

//import java.util.Date;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//@JsonSerialize(using = DataSerializer.class)
//@JsonDeserialize(using = DataDeserializer.class)
//@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "users")
public class DrupalData {
	@Id
	private Long uid;
	private String name;
	private Long created;
	private Long access;
	private Long login;
	private String timezone;

	public DrupalData() {
		super();
	}

	public DrupalData(Long uid, String name, Long created, Long access, Long login, String timezone) {
		super();
		this.uid = uid;
		this.name = name;
		this.created = created;
		this.access = access;
		this.login = login;
		this.timezone = timezone;
	}

	public Long getUid() {
		return uid;
	}

	public void setUid(Long uid) {
		this.uid = uid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getCreated() {
		return created;
	}

	public void setCreated(Long created) {
		this.created = created;
	}

	public Long getAccess() {
		return access;
	}

	public void setAccess(Long access) {
		this.access = access;
	}

	public Long getLogin() {
		return login;
	}

	public void setLogin(Long login) {
		this.login = login;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	@Override
	public String toString() {
		return "DrupalData [uid=" + uid + ", name=" + name + ", created=" + created + ", access=" + access + ", login=" + login + ", timezone=" + timezone + "]";
	}
}