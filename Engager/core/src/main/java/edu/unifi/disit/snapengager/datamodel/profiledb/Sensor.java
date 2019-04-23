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
package edu.unifi.disit.snapengager.datamodel.profiledb;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.disit.snap4city.SENSOR;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "sensor")
public class Sensor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;
	private String latitude;
	private String longitude;
	@Type(type = "timestamp")
	private Date insertdate;
	private String mapname;
	private Float value;

	public Sensor() {
	}

	public Sensor(Long id, String latitude, String longitude, Date insertdate, String mapname, Float value) {
		super();
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.insertdate = insertdate;
		this.mapname = mapname;
		this.value = value;
	}

	public Sensor(String latitude, String longitude, Date insertdate, String mapname, Float value) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.insertdate = insertdate;
		this.mapname = mapname;
		this.value = value;
	}

	public Sensor(String latitude, String longitude, Date insertdate, String mapname) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.insertdate = insertdate;
		this.mapname = mapname;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Date getInsertdate() {
		return insertdate;
	}

	public void setInsertdate(Date insertdate) {
		this.insertdate = insertdate;
	}

	public String getMapname() {
		return mapname;
	}

	public void setMapname(String mapname) {
		this.mapname = mapname;
	}

	public Float getValue() {
		return value;
	}

	public void setValue(Float value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Sensor [id=" + id + ", latitude=" + latitude + ", longitude=" + longitude + ", insertdate=" + insertdate + ", mapname=" + mapname + ", value=" + value + "]";
	}

	// fields in DROOLS can be null
	public SENSOR toDrools() {
		SENSOR s = new SENSOR();

		if (latitude != null)
			s.setLatitude(Float.valueOf(latitude));
		if (longitude != null)
			s.setLongitude(Float.valueOf(longitude));
		if (insertdate != null)
			s.setInsertdate(insertdate);
		if (mapname != null)
			s.setMapname(mapname);
		if (value != null)
			s.setValue(value);

		return s;
	}
}