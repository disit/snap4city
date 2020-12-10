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
package edu.unifi.disit.datamanager.datamodel.dto;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = KPIDataDTODeserializer.class)
public class KPIDataDTO {

	private Long id;
	private String highLevelType;
	private String nature;
	private String subNature;
	private String valueName;
	private String valueType;
	private String valueUnit;
	private String dataType;
	private String instanceUri;
	private String getInstances;
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date lastDate;
	private String lastValue;
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date lastCheck;
	private String lastLongitude;
	private String lastLatitude;
	private String metric;
	private String savedDirect;
	private String kbBased;
	private String smBased;
	private String username;
	private String organizations;
	private String appId;
	private String appName;
	private String widgets;
	private String parameters;
	private String healthiness;
	private String microAppExtServIcon;
	private String ownership;
	private String description;
	private String info;
	private String latitude;
	private String longitude;
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date insertTime;
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date deleteTime;
	private String dbValuesType;
	private String dbValuesLink;

	public KPIDataDTO() {
		super();
	}

	public KPIDataDTO(Long id, String highLevelType, String nature, String subNature, String valueName,
			String valueType, String valueUnit, String dataType, String instanceUri, String getInstances, Date lastDate,
			String lastValue, Date lastCheck, String lastLongitude, String lastLatitude, String metric,
			String savedDirect, String kbBased, String smBased, String username, String organizations, String appId,
			String appName, String widgets, String parameters, String healthiness, String microAppExtServIcon,
			String ownership, String description, String info, String latitude, String longitude, Date insertTime,
			Date deleteTime, String dbValuesType, String dbValuesLink) {
		this(highLevelType, nature, subNature, valueName, valueType,
			valueUnit, dataType, instanceUri, getInstances, lastDate, lastValue,
			lastCheck, lastLongitude, lastLatitude, metric, savedDirect,
			kbBased, smBased, username, organizations, appId, appName,
			widgets, parameters, healthiness, microAppExtServIcon, ownership,
			description, info, latitude, longitude, insertTime, deleteTime,
			dbValuesType, dbValuesLink);
		this.id = id;
	}
	
	

	public KPIDataDTO(String highLevelType, String nature, String subNature, String valueName, String valueType,
			String valueUnit, String dataType, String instanceUri, String getInstances, Date lastDate, String lastValue,
			Date lastCheck, String lastLongitude, String lastLatitude, String metric, String savedDirect,
			String kbBased, String smBased, String username, String organizations, String appId, String appName,
			String widgets, String parameters, String healthiness, String microAppExtServIcon, String ownership,
			String description, String info, String latitude, String longitude, Date insertTime, Date deleteTime,
			String dbValuesType, String dbValuesLink) {
		super();
		this.highLevelType = highLevelType;
		this.nature = nature;
		this.subNature = subNature;
		this.valueName = valueName;
		this.valueType = valueType;
		this.valueUnit = valueUnit;
		this.dataType = dataType;
		this.instanceUri = instanceUri;
		this.getInstances = getInstances;
		this.lastDate = lastDate;
		this.lastValue = lastValue;
		this.lastCheck = lastCheck;
		this.lastLongitude = lastLongitude;
		this.lastLatitude = lastLatitude;
		this.metric = metric;
		this.savedDirect = savedDirect;
		this.kbBased = kbBased;
		this.smBased = smBased;
		this.username = username;
		this.organizations = organizations;
		this.appId = appId;
		this.appName = appName;
		this.widgets = widgets;
		this.parameters = parameters;
		this.healthiness = healthiness;
		this.microAppExtServIcon = microAppExtServIcon;
		this.ownership = ownership;
		this.description = description;
		this.info = info;
		this.latitude = latitude;
		this.longitude = longitude;
		this.insertTime = insertTime;
		this.deleteTime = deleteTime;
		this.dbValuesType = dbValuesType;
		this.dbValuesLink = dbValuesLink;
	}



	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getHighLevelType() {
		return highLevelType;
	}

	public void setHighLevelType(String highLevelType) {
		this.highLevelType = highLevelType;
	}

	public String getNature() {
		return nature;
	}

	public void setNature(String nature) {
		this.nature = nature;
	}

	public String getSubNature() {
		return subNature;
	}

	public void setSubNature(String subNature) {
		this.subNature = subNature;
	}

	public String getValueName() {
		return valueName;
	}

	public void setValueName(String valueName) {
		this.valueName = valueName;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public String getValueUnit() {
		return valueUnit;
	}

	public void setValueUnit(String valueUnit) {
		this.valueUnit = valueUnit;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getInstanceUri() {
		return instanceUri;
	}

	public void setInstanceUri(String instanceUri) {
		this.instanceUri = instanceUri;
	}

	public String getGetInstances() {
		return getInstances;
	}

	public void setGetInstances(String getInstances) {
		this.getInstances = getInstances;
	}

	public Date getLastDate() {
		return lastDate;
	}

	public void setLastDate(Date lastDate) {
		this.lastDate = lastDate;
	}

	public String getLastValue() {
		return lastValue;
	}

	public void setLastValue(String lastValue) {
		this.lastValue = lastValue;
	}

	public Date getLastCheck() {
		return lastCheck;
	}

	public void setLastCheck(Date lastCheck) {
		this.lastCheck = lastCheck;
	}

	public String getLastLongitude() {
		return lastLongitude;
	}

	public void setLastLongitude(String lastLongitude) {
		this.lastLongitude = lastLongitude;
	}

	public String getLastLatitude() {
		return lastLatitude;
	}

	public void setLastLatitude(String lastLatitude) {
		this.lastLatitude = lastLatitude;
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public String getSavedDirect() {
		return savedDirect;
	}

	public void setSavedDirect(String savedDirect) {
		this.savedDirect = savedDirect;
	}

	public String getKbBased() {
		return kbBased;
	}

	public void setKbBased(String kbBased) {
		this.kbBased = kbBased;
	}

	public String getSmBased() {
		return smBased;
	}

	public void setSmBased(String smBased) {
		this.smBased = smBased;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getOrganizations() {
		return organizations;
	}

	public void setOrganizations(String organizations) {
		this.organizations = organizations;
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

	public String getWidgets() {
		return widgets;
	}

	public void setWidgets(String widgets) {
		this.widgets = widgets;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getHealthiness() {
		return healthiness;
	}

	public void setHealthiness(String healthiness) {
		this.healthiness = healthiness;
	}

	public String getMicroAppExtServIcon() {
		return microAppExtServIcon;
	}

	public void setMicroAppExtServIcon(String microAppExtServIcon) {
		this.microAppExtServIcon = microAppExtServIcon;
	}

	public String getOwnership() {
		return ownership;
	}

	public void setOwnership(String ownership) {
		this.ownership = ownership;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
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

	public String getDbValuesType() {
		return dbValuesType;
	}

	public void setDbValuesType(String dbValuesType) {
		this.dbValuesType = dbValuesType;
	}

	public String getDbValuesLink() {
		return dbValuesLink;
	}

	public void setDbValuesLink(String dbValuesLink) {
		this.dbValuesLink = dbValuesLink;
	}

	@Override
	public String toString() {
		return "KPIData [id=" + id + ", highLevelType=" + highLevelType + ", nature=" + nature + ", subNature="
				+ subNature + ", valueName=" + valueName + ", valueType=" + valueType + ", dataType=" + dataType
				+ ", instanceUri=" + instanceUri + ", getInstances=" + getInstances + ", lastDate=" + lastDate
				+ ", lastValue=" + lastValue + ", lastCheck=" + lastCheck + ", metric=" + metric + ", savedDirect="
				+ savedDirect + ", kbBased=" + kbBased + ", smBased=" + smBased + ", username=" + username
				+ ", organizations=" + organizations + ", appId=" + appId + ", appName=" + appName + ", widgets="
				+ widgets + ", parameters=" + parameters + ", healthiness=" + healthiness + ", microAppExtServIcon="
				+ microAppExtServIcon + ", ownership=" + ownership + ", description=" + description + ", info=" + info
				+ ", latitude=" + latitude + ", longitude=" + longitude + ", insertTime=" + insertTime + ", deleteTime="
				+ deleteTime + ", dbValuesType=" + dbValuesType + ", dbValuesLink=" + dbValuesLink + "]";
	}


}