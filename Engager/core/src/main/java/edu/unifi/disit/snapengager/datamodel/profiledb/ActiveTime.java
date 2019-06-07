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

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "activetime")
public class ActiveTime {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "activetime_id")
	Long id;
	@ManyToOne
	@JoinColumn(name = "userprofile_id")
	Userprofile userprofile;
	private LocalDate data;
	private Integer seconds;

	public ActiveTime() {
	}

	public ActiveTime(Userprofile userprofile, LocalDate data, Integer seconds) {
		super();
		this.userprofile = userprofile;
		this.data = data;
		this.seconds = seconds;
	}

	public ActiveTime(Long id, Userprofile userprofile, LocalDate data, Integer seconds) {
		super();
		this.id = id;
		this.userprofile = userprofile;
		this.data = data;
		this.seconds = seconds;
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

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate data) {
		this.data = data;
	}

	public Integer getSeconds() {
		return seconds;
	}

	public void setSeconds(Integer seconds) {
		this.seconds = seconds;
	}

	@Override
	public String toString() {
		return "ActiveTime [id=" + id + ", userprofile=" + userprofile + ", data=" + data + ", seconds=" + seconds + "]";
	}
}