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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = KPIMetadataSerializer.class)
@JsonDeserialize(using = KPIMetadataDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "kpimetadata")
public class KPIMetadata implements Comparable<KPIMetadata> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "kpi_id")
	private Long kpiId;
	@Column(name = "value_key")
	private String key;
	@Column(name = "value")
	private String value;
	@Column(name = "delete_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date deleteTime;
	@Column(name = "elapse_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date elapseTime;
	
	
	public KPIMetadata() {
		super();
	}

	public KPIMetadata(Long id, String key, String value, Date deleteTime, Date elapseTime) {
		super();
		this.id = id;
		this.key = key;
		this.value = value;
		this.deleteTime = deleteTime;
		this.elapseTime = elapseTime;
	}

	public KPIMetadata(String key, String value, Date deleteTime, Date elapseTime) {
		super();
		this.key = key;
		this.value = value;
		this.deleteTime = deleteTime;
		this.elapseTime = elapseTime;
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

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Date getDeleteTime() {
		return deleteTime;
	}

	public void setDeleteTime(Date deleteTime) {
		this.deleteTime = deleteTime;
	}

	public Date getElapseTime() {
		return elapseTime;
	}

	public void setElapseTime(Date elapseTime) {
		this.elapseTime = elapseTime;
	}

	@Override
	public String toString() {
		return "KPIMetadata [id=" + id + ", kpiId=" + kpiId + ", key=" + key + ", value=" + value + ", deleteTime="
				+ deleteTime + ", elapseTime=" + elapseTime + "]";
	}

	@Override
	public int compareTo(KPIMetadata o) {
		return this.getKey().compareTo(o.getKey());
	}

	
}