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

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonDeserialize(using = DelegationDeserializer.class)
@JsonSerialize(using = DelegationSerializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "delegation")
public class Delegation implements Cloneable, Serializable {

	private static final long serialVersionUID = 4846169944943418363L;

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

	@Column(name = "delegation_details")
	@Type(type = "text")
	@JsonRawValue
	private String delegationDetails;

	@Column(name = "groupname_delegated")
	private String groupnameDelegated;

	public Delegation() {
		super();
	}

	public Delegation(String usernameDelegator, String usernameDelegated, String variableName, String motivation, String elementId, String elementType, Date insertTime, Date deleteTime, String delegationDetails, String groupnameDelegated) {
		super();
		this.usernameDelegator = usernameDelegator;
		this.usernameDelegated = usernameDelegated;
		this.variableName = variableName;
		this.motivation = motivation;
		this.elementId = elementId;
		this.elementType = elementType;
		this.insertTime = insertTime;
		this.deleteTime = deleteTime;
		this.delegationDetails = delegationDetails;
		this.groupnameDelegated = groupnameDelegated;
	}

	public Delegation(Long id, String usernameDelegator, String usernameDelegated, String variableName, String motivation, String elementId, String elementType, Date insertTime, Date deleteTime, String delegationDetails,
			String groupnameDelegated) {
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
		this.delegationDetails = delegationDetails;
		this.groupnameDelegated = groupnameDelegated;
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

	public String getDelegationDetails() {
		return delegationDetails;
	}

	public void setDelegationDetails(String delegationDetails) {
		this.delegationDetails = delegationDetails;
	}

	public String getGroupnameDelegated() {
		return groupnameDelegated;
	}

	public void setGroupnameDelegated(String groupnameDelegated) {
		this.groupnameDelegated = groupnameDelegated;
	}

	@Override
	public String toString() {
		return "Delegation [id=" + id + ", usernameDelegator=" + usernameDelegator + ", usernameDelegated=" + usernameDelegated + ", variableName=" + variableName + ", motivation=" + motivation + ", elementId=" + elementId
				+ ", elementType=" + elementType + ", insertTime=" + insertTime + ", deleteTime=" + deleteTime + ", delegationDetails=" + delegationDetails + ", groupnameDelegated=" + groupnameDelegated + "]";
	}

	public Delegation clone() throws CloneNotSupportedException {
		return (Delegation) super.clone();
	}
}
