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
package edu.unifi.disit.datamanager.datamodel.dto;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = KPIElasticValueDTODeserializer.class)
public class KPIElasticValueDTO {
	
	String id;
	String organization;
	String valueStr;
    String valueType;
    String serviceUri;
    String src;
    String kind;
    String deviceName;
    String healthinessCriteria;
    String kpiId;
    String sensorId;
    Date dateTime;
    Date dataTime;
    String latlon;
	String latitude;
	String longitude;
    String dataType;
    String valueRefreshRate;
    String valueUnit;
    Float value;
    String valueName;
    String username;
    String nature;
    String subNature;
    
	public KPIElasticValueDTO() {
		super();
	}

	public KPIElasticValueDTO(String id, String organization, String valueStr, String valueType, String serviceUri,
			String src, String kind, String deviceName, String healthinessCriteria, String kpiId, String sensorId,
			Date dateTime, Date dataTime, String latlon, String latitude, String longitude, String dataType,
			String valueRefreshRate, String valueUnit, Float value, String valueName, String username, String nature,
			String subNature) {
		this(organization, valueStr, valueType, serviceUri, src, kind, deviceName, healthinessCriteria, kpiId, sensorId, dateTime, dataTime, latlon, 
				 latitude, longitude, dataType, valueRefreshRate, valueUnit, value, valueName, username, nature, subNature);
		this.id = id;
	}

	public KPIElasticValueDTO(String organization, String valueStr, String valueType, String serviceUri, String src,
			String kind, String deviceName, String healthinessCriteria, String kpiId, String sensorId, Date dateTime,
			Date dataTime, String latlon, String latitude, String longitude, String dataType, String valueRefreshRate,
			String valueUnit, Float value, String valueName, String username, String nature, String subNature) {
		super();
		this.organization = organization;
		this.valueStr = valueStr;
		this.valueType = valueType;
		this.serviceUri = serviceUri;
		this.src = src;
		this.kind = kind;
		this.deviceName = deviceName;
		this.healthinessCriteria = healthinessCriteria;
		this.kpiId = kpiId;
		this.sensorId = sensorId;
		this.dateTime = dateTime;
		this.dataTime = dataTime;
		this.latlon = latlon;
		this.latitude = latitude;
		this.longitude = longitude;
		this.dataType = dataType;
		this.valueRefreshRate = valueRefreshRate;
		this.valueUnit = valueUnit;
		this.value = value;
		this.valueName = valueName;
		this.username = username;
		this.nature = nature;
		this.subNature = subNature;
	}
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
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

	public String getKpiId() {
		return kpiId;
	}

	public void setKpiId(String kpiId) {
		this.kpiId = kpiId;
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

	public Date getDataTime() {
		return dataTime;
	}

	public void setDataTime(Date dataTime) {
		this.dataTime = dataTime;
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

	@Override
	public String toString() {
		return "KPIElasticValueDTO [id=" + id + ", organization=" + organization + ", valueStr=" + valueStr
				+ ", valueType=" + valueType + ", serviceUri=" + serviceUri + ", src=" + src + ", kind=" + kind
				+ ", deviceName=" + deviceName + ", healthinessCriteria=" + healthinessCriteria + ", kpiId=" + kpiId
				+ ", sensorId=" + sensorId + ", dateTime=" + dateTime + ", dataTime=" + dataTime + ", latlon=" + latlon
				+ ", latitude=" + latitude + ", longitude=" + longitude + ", dataType=" + dataType
				+ ", valueRefreshRate=" + valueRefreshRate + ", valueUnit=" + valueUnit + ", value=" + value
				+ ", valueName=" + valueName + ", username=" + username + ", nature=" + nature + ", subNature="
				+ subNature + "]";
	}

}