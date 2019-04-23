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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;

import edu.unifi.disit.snap4city.engager_utils.GroupType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "groupname")
public class Groupname {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "groupname_id")
	Long id;
	@ManyToOne
	@JoinColumn(name = "userprofile_id")
	Userprofile userprofile;
	@Enumerated(EnumType.STRING)
	private GroupType groupname;

	public Groupname() {
	}

	public Groupname(Long id, GroupType groupname) {
		this.id = id;
		this.groupname = groupname;
	}

	public Groupname(GroupType groupname) {
		this.groupname = groupname;
	}

	public Groupname(String groupname) {
		this.groupname = GroupType.fromString(groupname);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Userprofile getUserprofile() {
		return userprofile;
	}

	public void setUserprofile(Userprofile userprofile) {
		this.userprofile = userprofile;
	}

	public GroupType getGroupname() {
		return groupname;
	}

	public void setGroupname(GroupType groupname) {
		this.groupname = groupname;
	}

	@Override
	public String toString() {
		return "Group [id=" + id + ", userprofile=" + userprofile.getUsername() + ", groupname=" + groupname + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Groupname))
			return false;
		if (obj == this)
			return true;
		return this.groupname.equals(((Groupname) obj).groupname);
	}

	@Override
	public int hashCode() {
		return groupname.hashCode();
	}
}