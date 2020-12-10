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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "lightactivity")
public class LightActivity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "element_id")
	private String elementId;
	@Column(name = "element_type")
	private String elementType;
	@Column(name = "source_request")
	private String sourceRequest;
	@Column(name = "source_id")
	private String sourceId;
	@Column(name = "insert_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date insertTime;
	@Column(name = "delete_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date deleteTime;

	// default with nothing
	public LightActivity() {
		super();
	}

	public LightActivity(Long id, String elementId, String elementType, String sourceRequest, String sourceId,
			Date insertTime, Date deleteTime) {
		this(elementId, elementType, sourceRequest, sourceId, insertTime, deleteTime);
		this.id = id;
		
	}

	public LightActivity(String elementId, String elementType, String sourceRequest, String sourceId, Date insertTime,
			Date deleteTime) {
		super();
		this.elementId = elementId;
		this.elementType = elementType;
		this.sourceRequest = sourceRequest;
		this.sourceId = sourceId;
		this.insertTime = insertTime;
		this.deleteTime = deleteTime;
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

	public String getSourceRequest() {
		return sourceRequest;
	}

	public void setSourceRequest(String sourceRequest) {
		this.sourceRequest = sourceRequest;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
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

	@Override
	public String toString() {
		return "LightActivity [id=" + id + ", elementId=" + elementId + ", elementType=" + elementType
				+ ", sourceRequest=" + sourceRequest + ", sourceId=" + sourceId + ", insertTime=" + insertTime
				+ ", deleteTime=" + deleteTime + "]";
	}

}
