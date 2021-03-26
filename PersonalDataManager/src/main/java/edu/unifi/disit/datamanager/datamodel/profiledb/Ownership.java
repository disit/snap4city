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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import org.hibernate.annotations.Formula;
import org.springframework.format.annotation.DateTimeFormat;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "ownership")
public class Ownership {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String username;
	@Column(name = "elementId")
	private String elementId;
	@Column(name = "elementType")
	private String elementType;
	@Column(name = "elementName")
	private String elementName;
	@Column(name = "elementUrl")
	private String elementUrl;
        @Column(name = "deleted")
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private Date deleted;
	@Column(name = "elementDetails")
	@Type(type = "text")
	private String elementDetails;
        @Formula("(select case when elementType = 'IOTID' then 'IOT Device' when elementType = 'AppID' then 'IOT App' when elementType = 'DAAppID' then 'Data Analytics' when elementType = 'BrokerID' then 'IOT Broker' when elementType = 'PortiaID' then 'Web Scraping' when elementType = 'ModelID' then 'IOT Device Model' when elementType = 'HeatmapID' then 'Heatmap' when elementType = 'ServiceGraphID' then 'Service Graph' when elementType = 'DashboardID' then 'Dashboard' when elementType = 'ServiceURI' then 'Service URI' when elementType = 'SynopticID' then 'Synoptic' when elementType = 'SynopticTmplID' then 'Synoptic Template' when elementType = 'DataTableID' then 'Data Table' else elementType end )")
        private String elmtTypeLbl4Grps;

	public Ownership() {
		super();
	}

	public Ownership(String username, String elementId, String elementType, String elementName, String elementUrl, String elementDetails) {
		super();
		this.username = username;
		this.elementId = elementId;
		this.elementType = elementType;
		this.elementName = elementName;
		this.elementUrl = elementUrl;
		this.elementDetails = elementDetails;
	}

	public Ownership(Long id, String username, String elementId, String elementType, String elementName, String elementUrl, String elementDetails) {
		super();
		this.id = id;
		this.username = username;
		this.elementId = elementId;
		this.elementType = elementType;
		this.elementName = elementName;
		this.elementUrl = elementUrl;
		this.elementDetails = elementDetails;
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

	public String getElementName() {
		return elementName;
	}

	public void setElementName(String elementName) {
		this.elementName = elementName;
	}

	public String getElementUrl() {
		return elementUrl;
	}

	public void setElementUrl(String elementUrl) {
		this.elementUrl = elementUrl;
	}

	public String getElementDetails() {
		return elementDetails;
	}

	public void setElementDetails(String elementDetails) {
		this.elementDetails = elementDetails;
	}

        public Date getDeleted() {
            return deleted;
        }

        public void setDeleted(Date deleted) {
            this.deleted = deleted;
        }

	@Override
	public String toString() {
		return "Ownership [id=" + id + ", username=" + username + ", elementId=" + elementId + ", elementType=" + elementType + ", elementName=" + elementName + ", elementUrl=" + elementUrl + ", elementDetails=" + elementDetails + "]";
	}

        public String getElmtTypeLbl4Grps() {
            return elmtTypeLbl4Grps;
        }

        public void setElmtTypeLbl4Grps(String elmtTypeLbl4Grps) {
            this.elmtTypeLbl4Grps = elmtTypeLbl4Grps;
        }
          
}
