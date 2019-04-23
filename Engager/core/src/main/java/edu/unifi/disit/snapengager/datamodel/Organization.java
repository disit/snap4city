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

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Organization {

	Long id;
	String organizationName;
	String kbUrl;
	String gpsCentreLatLng;
	String zoomLevel;
	String lang;

	public Organization() {
	}

	public Organization(Long id, String organizationName, String kbUrl, String gpsCentreLatLng, String zoomLevel, String lang) {
		super();
		this.id = id;
		this.organizationName = organizationName;
		this.kbUrl = kbUrl;
		this.gpsCentreLatLng = gpsCentreLatLng;
		this.zoomLevel = zoomLevel;
		this.lang = lang;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

	public String getKbUrl() {
		return kbUrl;
	}

	public void setKbUrl(String kbUrl) {
		this.kbUrl = kbUrl;
	}

	public String getGpsCentreLatLng() {
		return gpsCentreLatLng;
	}

	public void setGpsCentreLatLng(String gpsCentreLatLng) {
		this.gpsCentreLatLng = gpsCentreLatLng;
	}

	public String getZoomLevel() {
		return zoomLevel;
	}

	public void setZoomLevel(String zoomLevel) {
		this.zoomLevel = zoomLevel;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	@Override
	public String toString() {
		return "Organization [id=" + id + ", organizationName=" + organizationName + ", kbUrl=" + kbUrl + ", gpsCentreLatLng=" + gpsCentreLatLng + ", zoomLevel=" + zoomLevel + ", lang=" + lang + "]";
	}

}