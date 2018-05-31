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
@Table(name = "activity")
public class Activity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Date time;

	@Column(name = "app_id")
	private String appId;
	@Column(name = "app_name")
	private String appName;
	private String username;

	@Column(name = "delegated_app_id")
	private String delegatedAppId;
	@Column(name = "delegated_app_name")
	private String delegatedAppName;
	@Column(name = "delegated_username")
	private String delagatedUsername;

	@Column(name = "source_request")
	private String sourceRequest;

	@Column(name = "variable_name")
	private String variableName;
	private String motivation;

	@Column(name = "access_type")
	private String accessType;

	private String domain;

	@Column(name = "delete_time")
	private Date deleteTime;

	// default with nothing
	public Activity() {
		super();
	}

	// my activity (requested by dashboard)
	public Activity(Date time, String username, String sourceRequest, String variableName, String motivation, String accessType, String domain, Date deleteTime) {
		super();
		this.time = time;
		this.username = username;
		this.sourceRequest = sourceRequest;
		this.variableName = variableName;
		this.motivation = motivation;
		this.accessType = accessType;
		this.domain = domain;
		this.deleteTime = deleteTime;
	}

	// my activity (without delegation involved)
	public Activity(Date time, String appId, String username, String appName, String sourceRequest, String variableName, String motivation, String accessType, String domain, Date deleteTime) {
		super();
		this.time = time;
		this.appId = appId;
		this.appName = appName;
		this.username = username;
		this.sourceRequest = sourceRequest;
		this.variableName = variableName;
		this.motivation = motivation;
		this.accessType = accessType;
		this.domain = domain;
		this.deleteTime = deleteTime;
	}

	// my activity (without delegation involved)
	public Activity(Date time, String appId, String username, String appName, String delegatedAppId, String delagatedUsername, String delegatedAppName, String sourceRequest, String variableName, String motivation, String accessType,
			String domain, Date deleteTime) {
		super();
		this.time = time;
		this.appId = appId;
		this.appName = appName;
		this.username = username;
		this.delegatedAppId = delegatedAppId;
		this.delagatedUsername = delagatedUsername;
		this.delegatedAppName = delegatedAppName;
		this.sourceRequest = sourceRequest;
		this.variableName = variableName;
		this.motivation = motivation;
		this.accessType = accessType;
		this.domain = domain;
		this.deleteTime = deleteTime;
	}

	// default with everything
	public Activity(Long id, Date time, String appId, String username, String appName, String delegatedAppId, String delagatedUsername, String delegatedAppName, String sourceRequest, String variableName, String motivation,
			String accessType, String domain, Date deleteTime) {
		super();
		this.id = id;
		this.time = time;
		this.appId = appId;
		this.appName = appName;
		this.username = username;
		this.delegatedAppId = delegatedAppId;
		this.delagatedUsername = delagatedUsername;
		this.delegatedAppName = delegatedAppName;
		this.sourceRequest = sourceRequest;
		this.variableName = variableName;
		this.motivation = motivation;
		this.accessType = accessType;
		this.domain = domain;
		this.deleteTime = deleteTime;
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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getDelegatedAppId() {
		return delegatedAppId;
	}

	public void setDelegatedAppId(String delegatedAppId) {
		this.delegatedAppId = delegatedAppId;
	}

	public String getDelagatedUsername() {
		return delagatedUsername;
	}

	public void setDelagatedUsername(String delagatedUsername) {
		this.delagatedUsername = delagatedUsername;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getDelegatedAppName() {
		return delegatedAppName;
	}

	public void setDelegatedAppName(String delegatedAppName) {
		this.delegatedAppName = delegatedAppName;
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

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Date getDeleteTime() {
		return deleteTime;
	}

	public void setDeleteTime(Date deleteTime) {
		this.deleteTime = deleteTime;
	}

	@Override
	public String toString() {
		return "Activity [id=" + id + ", time=" + time + ", appId=" + appId + ", appName=" + appName + ", username=" + username + ", delegatedAppId=" + delegatedAppId + ", delegatedAppName=" + delegatedAppName + ", delagatedUsername="
				+ delagatedUsername + ", sourceRequest=" + sourceRequest + ", variableName=" + variableName + ", motivation=" + motivation + ", accessType=" + accessType + ", domain=" + domain + ", deleteTime=" + deleteTime + "]";
	}

}