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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.disit.snap4city.ENGAGEMENT;
import com.fasterxml.jackson.annotation.JsonInclude;

import edu.unifi.disit.snap4city.engager_utils.EngagementType;
import edu.unifi.disit.snapengager.datamodel.EngagementStatusType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "engagement")
public class Engagement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;
	String username;
	String title;
	String subtitle;
	@Enumerated(EnumType.STRING)
	EngagementType type;
	String rulename;
	String message;
	@Type(type = "timestamp")
	private Date created;
	@Type(type = "timestamp")
	private Date elapse;
	@Column(name = "points")
	private Integer points;
	private Integer sendrate;
	// internal use
	@Enumerated(EnumType.STRING)
	private EngagementStatusType status;
	@Type(type = "timestamp")
	private Date deleted;

	public Engagement() {
	}

	public Engagement(Long id, String username, String title, String subtitle, EngagementType type, String rulename, String message, Date created, Date elapse, Integer points, EngagementStatusType status, Integer sendrate,
			Date deleted) {
		this.id = id;
		this.username = username;
		this.title = title;
		this.subtitle = subtitle;
		this.type = type;
		this.rulename = rulename;
		this.message = message;
		this.created = created;
		this.elapse = elapse;
		this.points = points;
		this.status = status;
		this.sendrate = sendrate;
		this.deleted = deleted;
	}

	public Engagement(String username, String title, String subtitle, EngagementType type, String rulename, String message, Date created, Date elapse, Integer points, EngagementStatusType status, Integer sendrate, Date deleted) {
		this.username = username;
		this.title = title;
		this.subtitle = subtitle;
		this.type = type;
		this.rulename = rulename;
		this.message = message;
		this.created = created;
		this.elapse = elapse;
		this.points = points;
		this.status = status;
		this.sendrate = sendrate;
		this.deleted = deleted;
	}

	public Engagement(ENGAGEMENT e, Userprofile up) {
		if (up.getUsername() != null)
			this.username = up.getUsername();
		if (e.getTitle() != null)
			this.title = e.getTitle();
		if (e.getSubtitle() != null)
			this.subtitle = e.getSubtitle();
		if (e.getType() != null)
			this.type = EngagementType.fromString(e.getType());
		else
			this.type = EngagementType.ALERT;// DEFAULT EngagementType
		if (e.getRulename() != null)
			this.rulename = e.getRulename();
		if (e.getMessage() != null)
			this.message = e.getMessage();
		this.created = new Date();
		if (e.getElapse() != null)
			this.elapse = new Date(System.currentTimeMillis() + e.getElapse() * 60 * 1000);
		// DEFAULT Elapse is null
		if (e.getPoints() != null)
			this.points = e.getPoints();
		else
			this.points = 0;// DEFAULT Points
		this.status = EngagementStatusType.CREATED;
		if (e.getSendrate() != null)
			this.sendrate = e.getSendrate();
		else
			this.sendrate = 0;// DEFAULT Sendrate
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public EngagementType getType() {
		return type;
	}

	public void setType(EngagementType type) {
		this.type = type;
	}

	public String getRulename() {
		return rulename;
	}

	public void setRulename(String rulename) {
		this.rulename = rulename;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getElapse() {
		return elapse;
	}

	public void setElapse(Date elapse) {
		this.elapse = elapse;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public EngagementStatusType getStatus() {
		return status;
	}

	public void setStatus(EngagementStatusType status) {
		this.status = status;
	}

	public Integer getSendrate() {
		return sendrate;
	}

	public Integer getSendrateMS() {
		return sendrate * 60000;
	}

	public void setSendrate(Integer sendrate) {
		this.sendrate = sendrate;
	}

	public void setSendrateMS(Integer sendrate) {
		this.sendrate = sendrate / 60000;
	}

	public Date getDeleted() {
		return deleted;
	}

	public void setDeleted(Date deleted) {
		this.deleted = deleted;
	}

	@Override
	public String toString() {
		return "Engagement [id=" + id + ", username=" + username + ", title=" + title + ", subtitle=" + subtitle + ", type=" + type + ", rulename=" + rulename + ", message=" + message + ", created=" + created + ", elapse=" + elapse
				+ ", points=" + points + ", status=" + status + ", sendrate=" + sendrate + ", deleted=" + deleted + "]";
	}
}