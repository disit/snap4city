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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "stats_survey1_response_text")
public class StatsSurvey1ResponseText {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String contribution;
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "stats_survey1_response_id")
	StatsSurvey1Response statsSurvey1Response;

	public StatsSurvey1ResponseText() {
	}

	public StatsSurvey1ResponseText(String contribution, StatsSurvey1Response statsSurvey1Response) {
		super();
		this.contribution = contribution;
		this.statsSurvey1Response = statsSurvey1Response;
	}

	public StatsSurvey1ResponseText(Long id, String contribution, StatsSurvey1Response statsSurvey1Response) {
		super();
		this.id = id;
		this.contribution = contribution;
		this.statsSurvey1Response = statsSurvey1Response;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getContribution() {
		return contribution;
	}

	public void setContribution(String contribution) {
		this.contribution = contribution;
	}

	public StatsSurvey1Response getStatsSurvey1Response() {
		return statsSurvey1Response;
	}

	public void setStatsSurvey1Response(StatsSurvey1Response statsSurvey1Response) {
		this.statsSurvey1Response = statsSurvey1Response;
	}

	@Override
	public String toString() {
		return "StatsSurvey1ResponseText [id=" + id + ", contribution=" + contribution + ", statsSurvey1Response=" + statsSurvey1Response.getId() + "]";
	}

}