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

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.disit.snap4city.PPOI;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "ppoi")
public class Ppoi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ppoi_id")
	Long id;
	@ManyToOne
	@JoinColumn(name = "userprofile_id")
	Userprofile userprofile;
	private String name;
	private String latitude;
	private String longitude;
	private Date acquireddate;

	public Ppoi() {
	}

	public Ppoi(Long id, Userprofile userprofile, String name, String latitude, String longitude, Date acquireddate) {
		super();
		this.id = id;
		this.userprofile = userprofile;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.acquireddate = acquireddate;
	}

	public Ppoi(Userprofile userprofile, String name, String latitude, String longitude, Date acquireddate) {
		super();
		this.userprofile = userprofile;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.acquireddate = acquireddate;
	}

	public Ppoi(String name, String latitude, String longitude, Date acquireddate) {
		super();
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.acquireddate = acquireddate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Userprofile getUserprofile() {
		return userprofile;
	}

	public void setUserprofile(Userprofile userprofile) {
		this.userprofile = userprofile;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public Date getAcquireddate() {
		return acquireddate;
	}

	public void setAcquireddate(Date acquireddate) {
		this.acquireddate = acquireddate;
	}

	@Override
	public String toString() {
		return "Ppoi [id=" + id + ", userprofile=" + userprofile + ", name=" + name + ", latitude=" + latitude + ", longitude=" + longitude + ", acquireddate=" + acquireddate + "]";
	}

	@Override
	public boolean equals(Object obj) {

		if ((obj == null) || (((Ppoi) obj).name == null) || (this.name == null))
			return false;

		if (!(obj instanceof Ppoi))
			return false;
		if (obj == this)
			return true;
		return this.name.equals(((Ppoi) obj).name);
	}

	// ppoi is modificable
	// @Override
	// public int hashCode() {
	// int hash = 7;
	// hash = 31 * hash + (name == null ? 0 : name.hashCode());
	// hash = 31 * hash + (latitude == null ? 0 : latitude.hashCode());
	// hash = 31 * hash + (longitude == null ? 0 : longitude.hashCode());
	// return hash;
	// }

	// ppoi is not modificable TODO
	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	// fields in DROOLS can be null
	public PPOI toDrools() {
		PPOI p = new PPOI();

		if (latitude != null) {
			p.setLatitude(Float.valueOf(latitude));
			p.setLatitudeAprox(Float.valueOf(aprox(latitude)));
		}
		if (longitude != null) {
			p.setLongitude(Float.valueOf(longitude));
			p.setLongitudeAprox(Float.valueOf(aprox(longitude)));
		}
		if (name != null)
			p.setName(name);

		return p;
	}

	public String getLatitudeAprox() {
		return aprox(this.latitude);
	}

	public String getLongitudeAprox() {
		return aprox(this.longitude);
	}

	private String aprox(String s) {
		Double d = Double.parseDouble(s);
		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.CEILING);
		return df.format(d);
	}
}