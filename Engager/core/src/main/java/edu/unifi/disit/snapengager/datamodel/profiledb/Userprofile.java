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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.disit.snap4city.USER;
import com.fasterxml.jackson.annotation.JsonInclude;

import edu.unifi.disit.snap4city.engager_utils.GenderType;
import edu.unifi.disit.snap4city.engager_utils.GroupType;
import edu.unifi.disit.snap4city.engager_utils.RoleType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "userprofile")
public class Userprofile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "userprofile_id")
	private Long id;
	private String username;
	private String organization;
	@OneToMany(mappedBy = "userprofile", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<Groupname> groupnames;
	@Enumerated(EnumType.STRING)
	private RoleType role;
	private String language;
	private Byte age;
	@Enumerated(EnumType.STRING)
	private GenderType gender;
	@OneToMany(mappedBy = "userprofile", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<Executed> executeds;
	@Type(type = "timestamp")
	private Date lastupdate;
	@Type(type = "timestamp")
	private Date lastlogin;
	@Type(type = "timestamp")
	private Date registrationdate;
	private String timezone;
	@OneToMany(mappedBy = "userprofile", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<Ppoi> ppois;
	@OneToMany(mappedBy = "userprofile", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<Subscription> subscriptions;

	public Userprofile() {
		super();
	}

	public Userprofile(String username, String organization, String groupname, String language, GroupType profile, Byte age, GenderType gender, Date lastupdate, Date lastlogin, Date registrationdate, String timezone) {
		super();
		this.username = username;
		this.organization = organization;
		this.language = language;
		this.age = age;
		this.gender = gender;
		this.lastupdate = lastupdate;
		this.lastlogin = lastlogin;
		this.registrationdate = registrationdate;
		this.timezone = timezone;
	}

	public Userprofile(Long id, String username, String organization, String groupname, String language, GroupType profile, Byte age, GenderType gender, Date lastupdate, Date lastlogin, Date registrationdate, String timezone) {
		super();
		this.id = id;
		this.username = username;
		this.organization = organization;
		this.language = language;
		this.age = age;
		this.gender = gender;
		this.lastupdate = lastupdate;
		this.lastlogin = lastlogin;
		this.registrationdate = registrationdate;
		this.timezone = timezone;
	}

	public Userprofile(String username) {
		this.username = username;
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

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Byte getAge() {
		return age;
	}

	public void setAge(Byte age) {
		this.age = age;
	}

	public GenderType getGenderType() {
		return gender;
	}

	public void setGenderType(GenderType genderType) {
		this.gender = genderType;
	}

	public GenderType getGender() {
		return gender;
	}

	public void setGender(GenderType gender) {
		this.gender = gender;
	}

	public Date getLastupdate() {
		return lastupdate;
	}

	public void setLastupdate(Date lastupdate) {
		this.lastupdate = lastupdate;
	}

	public Date getLastlogin() {
		return lastlogin;
	}

	public void setLastlogin(Date lastlogin) {
		this.lastlogin = lastlogin;
	}

	public RoleType getRole() {
		return role;
	}

	public void setRole(RoleType role) {
		this.role = role;
	}

	public Set<Groupname> getGroupnames() {
		return groupnames;
	}

	public void setGroupnames(Set<Groupname> groupnames) {
		this.groupnames = groupnames;
	}

	public Set<Executed> getExecuteds() {
		return executeds;
	}

	public void setExecuteds(Set<Executed> executeds) {
		this.executeds = executeds;
	}

	public Set<Ppoi> getPpois() {
		return ppois;
	}

	public void setPpois(Set<Ppoi> ppois) {
		this.ppois = ppois;
	}

	public Set<Subscription> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(Set<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
	}

	public Date getRegistrationdate() {
		return registrationdate;
	}

	public void setRegistrationdate(Date registrationdate) {
		this.registrationdate = registrationdate;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	@Override
	public String toString() {
		return "Userprofile [id=" + id + ", username=" + username + ", organization=" + organization + ", groupnames=" + groupnames + ", role=" + role + ", language=" + language + ", age=" + age + ", gender=" + gender + ", executeds="
				+ executeds + ", lastupdate=" + lastupdate + ", lastlogin=" + lastlogin + ", registrationdate=" + registrationdate + ", timezone=" + timezone + ", ppois=" + ppois + ", subscriptions=" + subscriptions + "]";
	}

	// fields in DROOLS can be null
	public USER toDrools() {
		USER u = new USER();
		u.setId(id);
		if (username != null)
			u.setUsername(username);
		if (organization != null)
			u.setOrganization(organization);
		if (language != null)
			u.setLanguage(language);
		if (age != null)
			u.setAge(age);
		if (gender != null)
			u.setGender(gender.toString());
		if (role != null)
			u.setRole(role.toString());

		if (groupnames.size() != 0) {
			List<String> l = new ArrayList<String>();
			for (Groupname g : groupnames)
				l.add(g.getGroupname().toString());
			u.setGroups(l);
		}

		if (executeds.size() != 0) {
			List<String> l = new ArrayList<String>();
			for (Executed e : executeds)
				l.add(e.getRulename());
			u.setExecuteds(l);
		}

		if (subscriptions.size() != 0) {
			List<String> s = new ArrayList<String>();
			for (Subscription subscription : subscriptions)
				s.add(subscription.getName().toString());
			u.setSubscriptions(s);
		}

		if (registrationdate != null)
			u.setRegistrationdate(registrationdate);

		return u;
	}

	// ------------------------------------------my implementation
	public void addExecuted(Executed e) {
		e.setUserprofile(this);
		executeds.add(e);
	}

	public void addGroupname(Groupname g) {

		if (groupnames == null) {
			groupnames = new HashSet<Groupname>();
		}

		if (!groupnames.contains(g)) {
			g.setUserprofile(this);
			groupnames.add(g);
		}
	}

	public void addGroupnames(Set<Groupname> groupnames) {
		for (Groupname gropnname : groupnames) {
			addGroupname(gropnname);
		}
	}

	public void addPpoi(Ppoi ppoi) {

		if (ppois == null) {
			ppois = new HashSet<Ppoi>();
		}

		if (!ppois.contains(ppoi)) {
			ppoi.setUserprofile(this);
			ppois.add(ppoi);
		}
	}

	public void addSubscription(Subscription s) {
		if (subscriptions == null) {
			subscriptions = new HashSet<Subscription>();
		}

		if (!subscriptions.contains(s)) {
			s.setUserprofile(this);
			subscriptions.add(s);
		}
	}

	public void addSubscription(Set<Subscription> subscriptions) {
		for (Subscription subscription : subscriptions) {
			addSubscription(subscription);
		}
	}

	public void removeAllSubscriptions() {
		subscriptions.clear();
		// subscriptions = null;
	}

	public void removeAllPpois() {
		ppois.clear();
		// ppois = null;
	}

	public void removeAllGroups() {
		groupnames.clear();
		// groupnames = null;
	}
}