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
   aDate with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
package edu.unifi.disit.datamanager.datamodel.elasticdb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.unifi.disit.datamanager.datamodel.dto.KPIElasticValueDTO;

@Entity
@JsonSerialize(using = KPIElasticValueSerializer.class)
@JsonDeserialize(using = KPIElasticValueDeserializer.class)
@Document(indexName = "#{T(edu.unifi.disit.datamanager.config.ConfigIndexBean).getIndexName()}", createIndex = false)
@TypeAlias("KPIElasticValue")
public class KPIElasticValue {
	
	@Id
	String id;
	@Field(name = "organization", type = FieldType.Keyword)
	String organization;
	@Field(name = "value_str", type = FieldType.Keyword)
	String valueStr;
	@Field(name = "value_type",type = FieldType.Keyword)
    String valueType;
	@Field(name = "serviceUri",type = FieldType.Keyword)
    String serviceUri;
	@Field(name = "src",type = FieldType.Keyword)
    String src;
	@Field(name = "kind",type = FieldType.Keyword)
    String kind;
	@Field(name = "deviceName",type = FieldType.Keyword)
    String deviceName;
	@Field(name = "healthiness_criteria",type = FieldType.Keyword)
    String healthinessCriteria;
	@Field(name = "kpiId",type = FieldType.Keyword)
    String kpiId;
	@Field(name = "sensorID",type = FieldType.Keyword)
    String sensorId;
	@Field(name = "date_time",type = FieldType.Date, format= DateFormat.custom, pattern="strict_date_optional_time||epoch_millis")
    Date dateTime;
	@Field(name = "dataTime",type = FieldType.Date,  format= DateFormat.custom, pattern="strict_date_optional_time||epoch_millis")
    Date dataTime;
	@Field(name = "latlon")
	@GeoPointField
    String latlon;
	String latitude;
	String longitude;
	@Field(name = "data_type",type = FieldType.Keyword)
    String dataType;
	@Field(name = "value_refresh_rate",type = FieldType.Keyword)
    String valueRefreshRate;
	@Field(name = "value_unit",type = FieldType.Keyword)
    String valueUnit;
	@Field(name = "value",type = FieldType.Float)
    Float value;
	@Field(name = "value_name",type = FieldType.Keyword)
    String valueName;
	@Field(name = "username",type = FieldType.Keyword)
    String username;
	@Field(name = "nature",type = FieldType.Keyword)
    String nature;
	@Field(name = "subnature",type = FieldType.Keyword)
    String subNature;
	@Field(name = "groups",type = FieldType.Keyword)
	List<String> groups;
	@Field(name = "user_delegations",type = FieldType.Keyword)
	List<String> userDelegations;
	@Field(name = "organization_delegations",type = FieldType.Keyword)
	List<String> organizationDelegations;
	
	public KPIElasticValue() {
		super();
		groups = new ArrayList<>();
		userDelegations = new ArrayList<>();
		organizationDelegations = new ArrayList<>();
	}
	
	public KPIElasticValue(String id, String valueStr, String valueType, String serviceUri, String src, String kind,
			String deviceName, String healthinessCriteria, String sensorId, Date dateTime, String latlon,
			String dataType, String valueRefreshRate, String valueUnit, Float value, String valueName, String username,
			String nature, String subNature) {
		this(valueStr, valueType, serviceUri, src, kind, deviceName, healthinessCriteria, sensorId, dateTime, latlon, 
			 dataType, valueRefreshRate, valueUnit, value, valueName, username, nature, subNature);
		this.id = id;
		
	}
	
	public KPIElasticValue(String valueStr, String valueType, String serviceUri, String src, String kind,
			String deviceName, String healthinessCriteria, String sensorId, Date dateTime, String latlon,
			String dataType, String valueRefreshRate, String valueUnit, Float value, String valueName, String username,
			String nature, String subNature) {
		super();
		this.valueStr = valueStr;
		this.valueType = valueType;
		this.serviceUri = serviceUri;
		this.src = src;
		this.kind = kind;
		this.deviceName = deviceName;
		this.healthinessCriteria = healthinessCriteria;
		this.sensorId = sensorId;
		this.dateTime = dateTime;
		this.latlon = latlon;
		this.dataType = dataType;
		this.valueRefreshRate = valueRefreshRate;
		this.valueUnit = valueUnit;
		this.value = value;
		this.valueName = valueName;
		this.username = username;
		this.nature = nature;
		this.subNature = subNature;
	}
	
	
	public KPIElasticValue(KPIElasticValueDTO dto) {
		this.id = dto.getId();
		this.valueStr = dto.getValueStr();
		this.valueType = dto.getValueType();
		this.serviceUri = dto.getServiceUri();
		this.src = dto.getSrc();
		this.kind = dto.getKind();
		this.deviceName = dto.getDeviceName();
		this.healthinessCriteria = dto.getHealthinessCriteria();
		this.sensorId = dto.getSensorId();
		this.dateTime = dto.getDataTime();
		this.dataTime = dto.getDataTime();
		this.latlon = dto.getLatlon();
		this.latitude = dto.getLatitude();
		this.longitude = dto.getLongitude();
		this.dataType = dto.getDataType();
		this.valueRefreshRate = dto.getValueRefreshRate();
		this.valueUnit = dto.getValueUnit();
		this.value = dto.getValue();
		this.valueName = dto.getValueName();
		this.username = dto.getUsername();
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getValueStr() {
		return valueStr;
	}
	public void setValueStr(String valueStr) {
		this.valueStr = valueStr;
	}
	public String getValueType() {
		return valueType;
	}
	public void setValueType(String valueType) {
		this.valueType = valueType;
	}
	public String getServiceUri() {
		return serviceUri;
	}
	public void setServiceUri(String serviceUri) {
		this.serviceUri = serviceUri;
	}
	public String getSrc() {
		return src;
	}
	public void setSrc(String src) {
		this.src = src;
	}
	public String getKind() {
		return kind;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	public String getDeviceName() {
		return deviceName;
	}
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	public String getHealthinessCriteria() {
		return healthinessCriteria;
	}
	public void setHealthinessCriteria(String healthinessCriteria) {
		this.healthinessCriteria = healthinessCriteria;
	}
	public String getSensorId() {
		return sensorId;
	}
	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}
	public Date getDateTime() {
		return dateTime;
	}
	public void setDateTime(Date dateTime) {
		this.dateTime = dateTime;
	}
	public String getLatlon() {
		return latlon;
	}
	public void setLatlon(String latlon) {
		this.latlon = latlon;
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
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	public String getValueRefreshRate() {
		return valueRefreshRate;
	}
	public void setValueRefreshRate(String valueRefreshRate) {
		this.valueRefreshRate = valueRefreshRate;
	}
	public String getValueUnit() {
		return valueUnit;
	}
	public void setValueUnit(String valueUnit) {
		this.valueUnit = valueUnit;
	}
	public Float getValue() {
		return value;
	}
	public void setValue(Float value) {
		this.value = value;
	}
	public String getValueName() {
		return valueName;
	}
	public void setValueName(String valueName) {
		this.valueName = valueName;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
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

	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	public List<String> getGroups() {
		return groups;
	}
	public void setGroups(List<String> groups) {
		this.groups = groups;
	}
	public List<String> getUserDelegations() {
		return userDelegations;
	}

	public void setUserDelegations(List<String> userDelegations) {
		this.userDelegations = userDelegations;
	}

	public List<String> getOrganizationDelegations() {
		return organizationDelegations;
	}

	public void setOrganizationDelegations(List<String> organizationDelegations) {
		this.organizationDelegations = organizationDelegations;
	}

	public String getKpiId() {
		return kpiId;
	}
	public void setKpiId(String kpiId) {
		this.kpiId = kpiId;
	}
	public Date getDataTime() {
		return dataTime;
	}
	public void setDataTime(Date dataTime) {
		this.dataTime = dataTime;
	}

	@Override
	public String toString() {
		return "KPIElasticValue [id=" + id + ", organization=" + organization + ", valueStr=" + valueStr
				+ ", valueType=" + valueType + ", serviceUri=" + serviceUri + ", src=" + src + ", kind=" + kind
				+ ", deviceName=" + deviceName + ", healthinessCriteria=" + healthinessCriteria + ", kpiId=" + kpiId
				+ ", sensorId=" + sensorId + ", dateTime=" + dateTime + ", dataTime=" + dataTime + ", latlon=" + latlon
				+ ", latitude=" + latitude + ", longitude=" + longitude + ", dataType=" + dataType
				+ ", valueRefreshRate=" + valueRefreshRate + ", valueUnit=" + valueUnit + ", value=" + value
				+ ", valueName=" + valueName + ", username=" + username + ", nature=" + nature + ", subNature="
				+ subNature + ", groups=" + groups + ", userDelegations=" + userDelegations
				+ ", organizationDelegations=" + organizationDelegations + "]";
	}

}