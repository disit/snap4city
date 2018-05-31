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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "delegation")
public class Delegation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "username_delegator")
	private String usernameDelegator;
	@Column(name = "username_delegated")
	private String usernameDelegated;
	@Column(name = "variable_name")
	private String variableName;
	private String motivation;

	@Column(name = "element_id")
	private String elementId;

	@Column(name = "element_type")
	private String elementType;

	@Column(name = "insert_time")
	private Date insertTime;
	@Column(name = "delete_time")
	private Date deleteTime;

	public Delegation() {
		super();
	}

	public Delegation(String usernameDelegator, String usernameDelegated, String variableName, String motivation, String elementId, String elementType, Date insertTime, Date deleteTime) {
		super();
		this.usernameDelegator = usernameDelegator;
		this.usernameDelegated = usernameDelegated;
		this.variableName = variableName;
		this.motivation = motivation;
		this.elementId = elementId;
		this.elementType = elementType;
		this.insertTime = insertTime;
		this.deleteTime = deleteTime;
	}

	public Delegation(Long id, String usernameDelegator, String usernameDelegated, String variableName, String motivation, String elementId, String elementType, Date insertTime, Date deleteTime) {
		super();
		this.id = id;
		this.usernameDelegator = usernameDelegator;
		this.usernameDelegated = usernameDelegated;
		this.variableName = variableName;
		this.motivation = motivation;
		this.elementId = elementId;
		this.elementType = elementType;
		this.insertTime = insertTime;
		this.deleteTime = deleteTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsernameDelegator() {
		return usernameDelegator;
	}

	public void setUsernameDelegator(String usernameDelegator) {
		this.usernameDelegator = usernameDelegator;
	}

	public String getUsernameDelegated() {
		return usernameDelegated;
	}

	public void setUsernameDelegated(String usernameDelegated) {
		this.usernameDelegated = usernameDelegated;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getMotivation() {
		return motivation;
	}

	public void setMotivation(String motivation) {
		this.motivation = motivation;
	}

	public String getElementId() {
		return elementId;
	}

	public void setElementId(String elementId) {
		this.elementId = elementId;
	}

	public String getElementType() {
		return elementType;
	}

	public void setElementType(String elementType) {
		this.elementType = elementType;
	}

	public Date getInsertTime() {
		return insertTime;
	}

	public void setInsertTime(Date insertTime) {
		this.insertTime = insertTime;
	}

	public Date getDeleteTime() {
		return deleteTime;
	}

	public void setDeleteTime(Date deleteTime) {
		this.deleteTime = deleteTime;
	}

	@Override
	public String toString() {
		return "Delegation [id=" + id + ", usernameDelegator=" + usernameDelegator + ", usernameDelegated=" + usernameDelegated + ", variableName=" + variableName + ", motivation=" + motivation + ", elementId=" + elementId
				+ ", elementType=" + elementType + ", insertTime=" + insertTime + ", deleteTime=" + deleteTime + "]";
	}
}