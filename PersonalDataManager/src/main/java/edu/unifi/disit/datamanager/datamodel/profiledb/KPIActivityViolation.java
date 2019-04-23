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

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "kpiactivity_violation")
public class KPIActivityViolation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "username")
	private String username;
	@Column(name = "kpi_id")
	private Long kpiId;
	@Column(name = "source_request")
	private String sourceRequest;
	@Column(name = "access_type")
	private String accessType;
	@Column(name = "domain")
	private String domain;
	@Column(name = "insert_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date insertTime;
	@Column(name = "elapse_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date elapseTime;
	@Column(name = "delete_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date deleteTime;
	@Column(name = "query")
	private String query;
	@Column(name = "error_message")
	private String errorMessage;
	@Column(name = "stacktrace")
	private Blob stacktrace;
	@Column(name = "ip_address")
	private String ipAddress;

	public KPIActivityViolation() {
		super();
	}

	public KPIActivityViolation(Long id, String username, String sourceRequest, Long kpiId, String accessType,
			String domain, Date insertTime, Date elapseTime, Date deleteTime, String query, String errorMessage,
			Blob stacktrace, String ipAddress) {
		super();
		this.id = id;
		this.username = username;
		this.kpiId = kpiId;
		this.sourceRequest = sourceRequest;
		this.accessType = accessType;
		this.domain = domain;
		this.insertTime = insertTime;
		this.elapseTime = elapseTime;
		this.deleteTime = deleteTime;
		this.query = query;
		this.errorMessage = errorMessage;
		this.stacktrace = stacktrace;
		this.ipAddress = ipAddress;
	}

	public KPIActivityViolation(String username, String sourceRequest, Long kpiId, String accessType,
			String delegationId, String domain, Date insertTime, Date elapseTime, Date deleteTime, String query,
			String errorMessage, Blob stacktrace, String ipAddress) {
		super();
		this.username = username;
		this.kpiId = kpiId;
		this.sourceRequest = sourceRequest;
		this.accessType = accessType;
		this.domain = domain;
		this.insertTime = insertTime;
		this.elapseTime = elapseTime;
		this.deleteTime = deleteTime;
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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Long getKpiId() {
		return kpiId;
	}

	public void setKpiId(Long kpiId) {
		this.kpiId = kpiId;
	}

	public String getSourceRequest() {
		return sourceRequest;
	}

	public void setSourceRequest(String sourceRequest) {
		this.sourceRequest = sourceRequest;
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

	public Date getInsertTime() {
		return insertTime;
	}

	public void setInsertTime(Date insertTime) {
		this.insertTime = insertTime;
	}

	public Date getElapseTime() {
		return elapseTime;
	}

	public void setElapseTime(Date elapseTime) {
		this.elapseTime = elapseTime;
	}

	public Date getDeleteTime() {
		return deleteTime;
	}

	public void setDeleteTime(Date deleteTime) {
		this.deleteTime = deleteTime;
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
		return "KPIActivityViolation [id=" + id + ", username=" + username + ", kpiId=" + kpiId + ", sourceRequest="
				+ sourceRequest + ", accessType=" + accessType + ", domain=" + domain + ", insertTime=" + insertTime
				+ ", elapseTime=" + elapseTime + ", deleteTime=" + deleteTime + ", query=" + query + ", errorMessage="
				+ errorMessage + ", stacktrace=" + stacktrace + ", ipAddress=" + ipAddress + "]";
	}

}
