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

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = KPIValueDTODeserializer.class)
public class KPIValueDTO {

	private Long id;
	private Long kpiId;
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date insertTime;
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date dataTime;
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date elapseTime;
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date deleteTime;
	private String value;
	private String latitude;
	private String longitude;

	public KPIValueDTO() {
		super();
	}

	public KPIValueDTO(Long id, Date insertTime, Date dataTime, Date elapseTime, Date deleteTime, String value,
			String latitude, String longitude) {
		this(insertTime, dataTime, elapseTime, deleteTime, value, latitude, longitude);
		this.id = id;
	}

	public KPIValueDTO(Date insertTime, Date dataTime, Date elapseTime, Date deleteTime, String value, String latitude,
			String longitude) {
		super();
		this.insertTime = insertTime;
		this.dataTime = dataTime;
		this.elapseTime = elapseTime;
		this.deleteTime = deleteTime;
		this.value = value;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getKpiId() {
		return kpiId;
	}

	public void setKpiId(Long kpiId) {
		this.kpiId = kpiId;
	}

	public Date getInsertTime() {
		return insertTime;
	}

	public void setInsertTime(Date insertTime) {
		this.insertTime = insertTime;
	}

	public Date getDataTime() {
		return dataTime;
	}

	public void setDataTime(Date dataTime) {
		this.dataTime = dataTime;
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

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
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

	@Override
	public String toString() {
		return "KPIValue [id=" + id + ", kpiId=" + kpiId + ", insertTime=" + insertTime + ", dataTime=" + dataTime
				+ ", elapseTime=" + elapseTime + ", deleteTime=" + deleteTime + ", value=" + value + ", latitude="
				+ latitude + ", longitude=" + longitude + "]";
	}


}