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
package edu.unifi.disit.snapengager.datamodel;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = DataSerializer.class)
@JsonDeserialize(using = DataDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data implements Comparable<Data> {

	private Long id;
	private String username;
	private Date dataTime;
	private Date insertTime;
	private Date deleteTime;
	private Date elapseTime;
	private String appName;
	private String appId;
	private String motivation;
	private String variableName;
	private String variableValue;
	private String variableUnit;

	public Data() {
		super();
	}

	public Data(String username, Date dataTime, Date insertTime, Date deleteTime, Date elapseTime, String appName, String appId, String motivation, String variableName, String variableValue, String variableUnit) {
		super();
		this.username = username;
		this.dataTime = dataTime;
		this.insertTime = insertTime;
		this.deleteTime = deleteTime;
		this.elapseTime = elapseTime;
		this.appName = appName;
		this.appId = appId;
		this.motivation = motivation;
		this.variableName = variableName;
		this.variableValue = variableValue;
		this.variableUnit = variableUnit;
	}

	public Data(Long id, String username, Date dataTime, Date insertTime, Date deleteTime, Date elapseTime, String appName, String appId, String motivation, String variableName, String variableValue, String variableUnit) {
		super();
		this.id = id;
		this.username = username;
		this.dataTime = dataTime;
		this.insertTime = insertTime;
		this.deleteTime = deleteTime;
		this.elapseTime = elapseTime;
		this.appName = appName;
		this.appId = appId;
		this.motivation = motivation;
		this.variableName = variableName;
		this.variableValue = variableValue;
		this.variableUnit = variableUnit;
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

	public Date getDataTime() {
		return dataTime;
	}

	public void setDataTime(Date dataTime) {
		this.dataTime = dataTime;
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

	public Date getElapseTime() {
		return elapseTime;
	}

	public void setElapseTime(Date elapseTime) {
		this.elapseTime = elapseTime;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getMotivation() {
		return motivation;
	}

	public void setMotivation(String motivation) {
		this.motivation = motivation;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableValue() {
		return variableValue;
	}

	public void setVariableValue(String variableValue) {
		this.variableValue = variableValue;
	}

	public String getVariableUnit() {
		return variableUnit;
	}

	public void setVariableUnit(String variableUnit) {
		this.variableUnit = variableUnit;
	}

	@Override
	public String toString() {
		return "Data [id=" + id + ", username=" + username + ", dataTime=" + dataTime + ", insertTime=" + insertTime + ", deleteTime=" + deleteTime + ", elapseTime=" + elapseTime + ", appName=" + appName + ", appId=" + appId
				+ ", motivation=" + motivation + ", variableName=" + variableName + ", variableValue=" + variableValue + ", variableUnit=" + variableUnit + "]";
	}

	@Override
	public int compareTo(Data o) {
		return this.getDataTime().compareTo(o.getDataTime());
	}
}