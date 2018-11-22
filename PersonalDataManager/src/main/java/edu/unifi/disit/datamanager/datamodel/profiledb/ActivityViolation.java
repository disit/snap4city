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

import java.sql.Blob;
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
@Table(name = "activity_violation")
public class ActivityViolation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Date time;

	@Column(name = "app_id")
	private String appId;
	@Column(name = "app_name")
	private String appName;
	private String username;

	@Column(name = "source_request")
	private String sourceRequest;

	@Column(name = "variable_name")
	private String variableName;
	private String motivation;

	@Column(name = "access_type")
	private String accessType;

	private String query;

	@Column(name = "error_message")
	private String errorMessage;

	private Blob stacktrace;

	@Column(name = "ip_address")
	private String ipAddress;

	// default with nothing
	public ActivityViolation() {
		super();
	}

	// my activity
	public ActivityViolation(Date time, String appId, String appName, String username, String sourceRequest, String variableName, String motivation, String accessType, String query, String errorMessage, Blob stacktrace,
			String ipAddress) {
		super();
		this.time = time;
		this.appId = appId;
		this.appName = appName;
		this.username = username;
		this.sourceRequest = sourceRequest;
		this.variableName = variableName;
		this.motivation = motivation;
		this.accessType = accessType;
		this.query = query;
		this.errorMessage = errorMessage;
		this.stacktrace = stacktrace;
		this.ipAddress = ipAddress;
	}

	// default with everything
	public ActivityViolation(Long id, Date time, String appId, String appName, String username, String sourceRequest, String variableName, String motivation, String accessType, String query, String errorMessage, Blob stacktrace,
			String ipAddress) {
		super();
		this.id = id;
		this.time = time;
		this.appId = appId;
		this.appName = appName;
		this.username = username;
		this.sourceRequest = sourceRequest;
		this.variableName = variableName;
		this.motivation = motivation;
		this.accessType = accessType;
		this.query = query;
		this.errorMessage = errorMessage;
		this.stacktrace = stacktrace;
		this.ipAddress = ipAddress;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getSourceRequest() {
		return sourceRequest;
	}

	public void setSourceRequest(String sourceRequest) {
		this.sourceRequest = sourceRequest;
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

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Blob getStacktrace() {
		return stacktrace;
	}

	public void setStacktrace(Blob stacktrace) {
		this.stacktrace = stacktrace;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Override
	public String toString() {
		return "ActivityViolation [id=" + id + ", time=" + time + ", appId=" + appId + ", appName=" + appName + ", username=" + username + ", sourceRequest=" + sourceRequest + ", variableName=" + variableName + ", motivation=" + motivation
				+ ", accessType=" + accessType + ", query=" + query + ", errorMessage=" + errorMessage + ", stacktrace=" + stacktrace + ", ipAddress=" + ipAddress + "]";
	}
}
