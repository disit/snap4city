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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.disit.snap4city.EVENT;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "event")
@JsonDeserialize(using = EventDeserializer.class)
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;
	@Column(name = "start_date")
	String startDate;
	@Column(name = "end_date")
	String endDate;
	String latitude;
	String longitude;
	@Column(name = "service_uri")
	String serviceUri;
	@Column(name = "service_type")
	String serviceType;
	@Column(name = "type_label")
	String typeLabel;
	String name;
	String place;
	String organization;
	Float price;

	public Event() {
	}

	public Event(String startDate, String endDate, String latitude, String longitude, String serviceUri, String serviceType, String typeLabel, String name, String place, String organization, Float price) {
		super();
		this.startDate = startDate;
		this.endDate = endDate;
		this.latitude = latitude;
		this.longitude = longitude;
		this.serviceUri = serviceUri;
		this.serviceType = serviceType;
		this.typeLabel = typeLabel;
		this.name = name;
		this.place = place;
		this.organization = organization;
		this.price = price;
	}

	public Event(Long id, String startDate, String endDate, String latitude, String longitude, String serviceUri, String serviceType, String typeLabel, String name, String place, String organization, Float price) {
		super();
		this.id = id;
		this.startDate = startDate;
		this.endDate = endDate;
		this.latitude = latitude;
		this.longitude = longitude;
		this.serviceUri = serviceUri;
		this.serviceType = serviceType;
		this.typeLabel = typeLabel;
		this.name = name;
		this.place = place;
		this.organization = organization;
		this.price = price;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
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

	public String getServiceUri() {
		return serviceUri;
	}

	public void setServiceUri(String serviceUri) {
		this.serviceUri = serviceUri;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getTypeLabel() {
		return typeLabel;
	}

	public void setTypeLabel(String typeLabel) {
		this.typeLabel = typeLabel;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public Float getPrice() {
		return price;
	}

	public void setPrice(Float price) {
		this.price = price;
	}

	@Override
	public String toString() {
		return "Event [id=" + id + ", startDate=" + startDate + ", endDate=" + endDate + ", latitude=" + latitude + ", longitude=" + longitude + ", serviceUri=" + serviceUri + ", serviceType=" + serviceType + ", typeLabel=" + typeLabel
				+ ", name=" + name + ", place=" + place + ", organization=" + organization + ", price=" + price + "]";
	}

	public EVENT toDrools() {
		EVENT e = new EVENT();

		e.setStartDate(startDate);
		e.setEndDate(endDate);
		e.setLatitude(latitude);
		e.setLongitude(longitude);
		e.setServiceUri(serviceUri);
		e.setServiceType(serviceType);
		e.setTypeLabel(typeLabel);
		e.setName(name);
		e.setPlace(place);
		e.setPrice(price);

		return e;
	}
}