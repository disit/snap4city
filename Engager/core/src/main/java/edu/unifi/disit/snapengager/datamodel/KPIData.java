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

@JsonDeserialize(using = KPIDataDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KPIData implements Comparable<KPIData> {

	private Long id;
	private String username;
	private Float latitude;
	private Float longitude;
	private String valueName;
	private Date lastDate;
	private Date insertTime;

	public KPIData() {
		super();
	}

	public KPIData(String username, String valueName, Date lastDate, Float latitude, Float longitude, Date insertTime) {
		super();
		this.username = username;
		this.valueName = valueName;
		this.lastDate = lastDate;
		this.insertTime = insertTime;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public KPIData(Long id, String username, String valueName, Date lastDate, Float latitude, Float longitude, Date insertTime) {
		super();
		this.id = id;
		this.username = username;
		this.valueName = valueName;
		this.lastDate = lastDate;
		this.insertTime = insertTime;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Float getLatitude() {
		return latitude;
	}

	public void setLatitude(Float latitude) {
		this.latitude = latitude;
	}

	public Float getLongitude() {
		return longitude;
	}

	public void setLongitude(Float longitude) {
		this.longitude = longitude;
	}

	public Date getLastDate() {
		return lastDate;
	}

	public void setLastDate(Date lastDate) {
		this.lastDate = lastDate;
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

	public String getValueName() {
		return valueName;
	}

	public void setValueName(String valueName) {
		this.valueName = valueName;
	}

	public Date getInsertTime() {
		return insertTime;
	}

	public void setInsertTime(Date insertTime) {
		this.insertTime = insertTime;
	}

	@Override
	public String toString() {
		return "KPIData [id=" + id + ", username=" + username + ", latitude=" + latitude + ", longitude=" + longitude + ", valueName=" + valueName + ", lastDate=" + lastDate + ", insertTime=" + insertTime + "]";
	}

	@Override
	public int compareTo(KPIData o) {
		return this.getLastDate().compareTo(o.getLastDate());
	}
}