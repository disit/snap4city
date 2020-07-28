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

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = KPIDataSerializer.class)
@JsonDeserialize(using = KPIDataDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "kpidata")
public class KPIData implements Comparable<KPIData>, Serializable {

	private static final long serialVersionUID = 1877829622537559204L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	@Column(name = "high_level_type")
	private String highLevelType;
	@Column(name = "nature")
	private String nature;
	@Column(name = "sub_nature")
	private String subNature;
	@Column(name = "value_name")
	private String valueName;
	@Column(name = "value_type")
	private String valueType;
	@Column(name = "value_unit")
	private String valueUnit;
	@Column(name = "data_type")
	private String dataType;
	@Column(name = "instance_uri")
	private String instanceUri;
	@Column(name = "get_instances")
	private String getInstances;
	@Column(name = "last_date")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date lastDate;
	@Column(name = "last_value")
	private String lastValue;
	@Column(name = "last_check")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date lastCheck;
	@Column(name = "last_longitude")
	private String lastLongitude;
	@Column(name = "last_latitude")
	private String lastLatitude;
	@Column(name = "metric")
	private String metric;
	@Column(name = "saved_direct")
	private String savedDirect;
	@Column(name = "kb_based")
	private String kbBased;
	@Column(name = "sm_based")
	private String smBased;
	@Column(name = "username")
	private String username;
	@Column(name = "organizations")
	private String organizations;
	@Column(name = "app_id")
	private String appId;
	@Column(name = "app_name")
	private String appName;
	@Column(name = "widgets")
	private String widgets;
	@Column(name = "parameters")
	private String parameters;
	@Column(name = "healthiness")
	private String healthiness;
	@Column(name = "microAppExtServIcon")
	private String microAppExtServIcon;
	@Column(name = "ownership")
	private String ownership;
	@Column(name = "description")
	private String description;
	@Column(name = "info")
	private String info;
	@Column(name = "latitude")
	private String latitude;
	@Column(name = "longitude")
	private String longitude;
	@Column(name = "insert_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date insertTime;
	@Column(name = "delete_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date deleteTime;
	@Column(name = "db_values_type")
	private String dbValuesType;
	@Column(name = "db_values_link")
	private String dbValuesLink;

	public KPIData() {
		super();
	}

	public KPIData(String highLevelType, String nature, String subNature, String valueName, String valueType,
			String valueUnit, String dataType, String instanceUri, String getInstances, Date lastDate, String lastValue,
			Date lastCheck, String lastLatitude, String lastLongitude, String metric, String savedDirect,
			String kbBased, String smBased, String username, String organizations, String appId, String appName,
			String widgets, String parameters, String healtiness, String microAppExtServIcon, String ownership,
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
		this.lastLatitude = lastLatitude;
		this.lastLongitude = lastLongitude;
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
		this.healthiness = healtiness;
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

	public KPIData(Long id, String highLevelType, String nature, String subNature, String valueName, String valueType,
			String valueUnit, String dataType, String instanceUri, String getInstances, Date lastDate, String lastValue,
			Date lastCheck, String lastLatitude, String lastLongitude, String metric, String savedDirect,
			String kbBased, String smBased, String username, String organizations, String appId, String appName,
			String widgets, String parameters, String healtiness, String microAppExtServIcon, String ownership,
			String description, String info, String latitude, String longitude, Date insertTime, Date deleteTime,
			String dbValuesType, String dbValuesLink) {
		super();
		this.id = id;
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
		this.lastLatitude = lastLatitude;
		this.lastLongitude = lastLongitude;
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
		this.healthiness = healtiness;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((valueName == null) ? 0 : valueName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KPIData other = (KPIData) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (valueName == null) {
			if (other.valueName != null)
				return false;
		} else if (!valueName.equals(other.valueName))
			return false;
		return true;
	}

	@Override
	public int compareTo(KPIData o) {
		return this.getInsertTime().compareTo(o.getInsertTime());
	}
}