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

import com.disit.snap4city.POI;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = PoiDeserializer.class)
public class Poi {

	private String name;
	private String latitude;
	private String longitude;
	private String tipo;
	private String typeLabel;
	private String serviceType;
	private String serviceUri;

	public Poi() {
	}

	public Poi(String name, String latitude, String longitude, String tipo, String typeLabel, String serviceType, String serviceUri) {
		super();
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.tipo = tipo;
		this.typeLabel = typeLabel;
		this.serviceType = serviceType;
		this.serviceUri = serviceUri;
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

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getTypeLabel() {
		return typeLabel;
	}

	public void setTypeLabel(String typeLabel) {
		this.typeLabel = typeLabel;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getServiceUri() {
		return serviceUri;
	}

	public void setServiceUri(String serviceUri) {
		this.serviceUri = serviceUri;
	}

	@Override
	public String toString() {
		return "Poi [name=" + name + ", latitude=" + latitude + ", longitude=" + longitude + ", tipo=" + tipo + ", typeLabel=" + typeLabel + ", serviceType=" + serviceType + ", serviceUri=" + serviceUri + "]";
	}

	public POI toDrools() {
		POI p = new POI();
		p.setName(name);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setServiceUri(serviceUri);
		p.setServiceType(serviceType);
		p.setTypeLabel(typeLabel);
		p.setTipo(tipo);
		return p;
	}
}