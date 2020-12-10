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
import javax.persistence.Transient;

import org.hibernate.annotations.Formula;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = DeviceGroupElementSerializer.class)
@JsonDeserialize(using = DeviceGroupElementDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "devicegroupelement")
public class DeviceGroupElement implements Serializable {

	private static final long serialVersionUID = -8775532689760406093L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "insert_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date insertTime;

	@Column(name = "delete_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date deleteTime;

	@Column(name = "elementId")
	private String elementId;

	@Column(name = "elementType")
	private String elementType;

	@Column(name = "device_group_id")
	private Long deviceGroupId;

	@Transient
	@Formula("( select g.username from devicegroup g where g.id = device_group_id ) ")
	private String deviceGroupContact;

	@Transient
	@Formula("( select g.name from devicegroup g where g.id = device_group_id ) ")
	private String deviceGroupName;

	// @Transient TODO , make this field transient, so it is not included in tehe SELECT query towards the DB
	// moreover, remove the findByUsernameAndElementIdAndElementTypeAndDeleteTimeIsNull in DAO
	// see DeviceGroupElementDAO.java
	@Formula("( select o.username from devicegroup g, ownership o where g.id = device_group_id and o.elementId = elementId and o.elementType = elementType union select o.username from devicegroup g, kpidata o where g.id = device_group_id and o.id = elementId and 'MyKPI' = elementType )")
	private String username;

	@Transient
	@Formula("( select o.elementName from devicegroup g, ownership o where g.id = device_group_id and o.elementId = elementId and o.elementType = elementType union select o.value_name from devicegroup g, kpidata o where g.id = device_group_id and o.id = elementId and 'MyKPI' = elementType )")
	private String elementName;

	@Transient
	@Formula("(select case when elementType = 'IOTID' then 'IOT Device' when elementType = 'AppID' then 'IOT App' when elementType = 'DAAppID' then 'Data Analytics' when elementType = 'BrokerID' then 'IOT Broker' when elementType = 'PortiaID' then 'Web Scraping' when elementType = 'ModelID' then 'IOT Device Model' when elementType = 'HeatmapID' then 'Heatmap' when elementType = 'ServiceGraphID' then 'Service Graph' when elementType = 'DashboardID' then 'Dashboard' when elementType = 'ServiceURI' then 'Service URI' else elementType end )")
	private String elmtTypeLbl;

	// default with nothing
	public DeviceGroupElement() {
		super();
	}

	// default with everything
	public DeviceGroupElement(Long id, Long deviceGroupId, String elementId, String elementType) {
		super();
		this.id = id;
		this.deviceGroupId = deviceGroupId;
		this.elementId = elementId;
		this.elementType = elementType;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Long getDeviceGroupId() {
		return deviceGroupId;
	}

	public void setDeviceGroupId(Long deviceGroupId) {
		this.deviceGroupId = deviceGroupId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getElementName() {
		return elementName;
	}

	public void setElementName(String elementName) {
		this.elementName = elementName;
	}

	public String getElmtTypeLbl() {
		return elmtTypeLbl;
	}

	public void setElmtTypeLbl(String elmtTypeLbl) {
		this.elmtTypeLbl = elmtTypeLbl;
	}

	public String getDeviceGroupContact() {
		return deviceGroupContact;
	}

	public void setDeviceGroupContact(String deviceGroupContact) {
		this.deviceGroupContact = deviceGroupContact;
	}

	public String getDeviceGroupName() {
		return deviceGroupName;
	}

	public void setDeviceGroupName(String deviceGroupName) {
		this.deviceGroupName = deviceGroupName;
	}

	@Override
	public String toString() {
		return "DeviceGroupElement [id=" + id + ", insertTime=" + insertTime + ", deleteTime=" + deleteTime + ", deviceGroupId=" + deviceGroupId + ", deviceGroupName=" + deviceGroupName + ", elementId=" + elementId + ", elementType="
				+ elementType + "]";
	}

}
