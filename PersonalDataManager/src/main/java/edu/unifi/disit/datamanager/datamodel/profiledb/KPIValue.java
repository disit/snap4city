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

@JsonSerialize(using = KPIValueSerializer.class)
@JsonDeserialize(using = KPIValueDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "kpivalues")
public class KPIValue implements Comparable<KPIValue> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "kpi_id")
	private Long kpiId;
	@Column(name = "insert_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date insertTime;
	@Column(name = "data_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date dataTime;
	@Column(name = "elapse_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date elapseTime;
	@Column(name = "delete_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date deleteTime;
	@Column(name = "value")
	private String value;

	public KPIValue() {
		super();
	}

	public KPIValue(Long id, Date insertTime, Date dataTime, Date elapseTime, Date deleteTime, String value) {
		super();
		this.id = id;
		this.insertTime = insertTime;
		this.dataTime = dataTime;
		this.elapseTime = elapseTime;
		this.deleteTime = deleteTime;
		this.value = value;
	}

	public KPIValue(Date insertTime, Date dataTime, Date elapseTime, Date deleteTime, String value) {
		super();
		this.insertTime = insertTime;
		this.dataTime = dataTime;
		this.elapseTime = elapseTime;
		this.deleteTime = deleteTime;
		this.value = value;
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

	@Override
	public String toString() {
		return "KPIValues [id=" + id + ", kpiId=" + kpiId + ", insertTime=" + insertTime
				+ ", dataTime=" + dataTime + ", elapseTime=" + elapseTime + ", deleteTime=" + deleteTime + ", value="
				+ value + "]";
	}

	@Override
	public int compareTo(KPIValue o) {
		return this.getInsertTime().compareTo(o.getInsertTime());
	}

}