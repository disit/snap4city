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
package edu.unifi.disit.snapengager.task;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.builder.KieScanner;
import org.kie.api.runtime.KieContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import edu.unifi.disit.snap4city.engager_utils.RoleType;
import edu.unifi.disit.snapengager.datamodel.ldap.LDAPEntity;
import edu.unifi.disit.snapengager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.Groupname;
import edu.unifi.disit.snapengager.datamodel.profiledb.Userprofile;
import edu.unifi.disit.snapengager.exception.CredentialsException;
import edu.unifi.disit.snapengager.exception.LDAPException;
import edu.unifi.disit.snapengager.exception.UserprofileException;
import edu.unifi.disit.snapengager.service.IDataManagerService;
import edu.unifi.disit.snapengager.service.IUserprofileService;

@EnableScheduling
@Component
public class LdapTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${ldap.task.cron}")
	private String cronrefresh;

	KieContainer kContainer;
	KieScanner kScanner;

	@Autowired
	LDAPUserDAO ldaprepo;

	@Autowired
	IUserprofileService upservice;

	@Autowired
	IDataManagerService dataservice;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug("-----------------------------------------------start execution for LDAP-----------------------------------------------");
					myTask();
				} catch (Exception e) {
					logger.error("Error catched {}", e);
				}
			}
		}, new Trigger() {
			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				CronTrigger trigger = new CronTrigger(cronrefresh);
				Date nextExec = trigger.nextExecutionTime(triggerContext);
				logger.debug("-----------------------------------------------next execution for LDAP will be fired at:{}-----------------------------------------------", nextExec);
				return nextExec;
			}
		});
	}

	private void myTask() throws LDAPException, UserprofileException, CredentialsException, IOException {
		Locale lang = new Locale("en");

		logger.debug("retrieve all avaiable groups");
		List<LDAPEntity> groups = ldaprepo.getAllGroups(lang);

		logger.debug("retrieve all avaiable roles");
		List<LDAPEntity> roles = ldaprepo.getAllRoles(lang);

		Hashtable<String, Boolean> assistanceEnabled = dataservice.getAssistanceEnabled(lang);

		// cycling on organization (a user has always be in an organization)
		for (LDAPEntity ou : ldaprepo.getAllOrganization(lang)) {

			// for any user in a cycling
			for (String username : ou.getUsernames()) {

				// check if the user has enable the assistance
				if ((assistanceEnabled.get(username) != null) && (assistanceEnabled.get(username))) {
					Userprofile up = upservice.get(username, lang);
					if (up == null)
						up = new Userprofile(username);
					else {
						up.removeAllGroups();
						// upservice.save(up, lang);// TODO make it better
					}

					// enrich organization
					up.setOrganization(ou.getName());
					// enrich group
					Set<Groupname> myGroups = null;
					myGroups = retrieveMyEntities(username, groups);
					if (myGroups.size() != 0)
						up.addGroupnames(myGroups);// add groups
					// enrich role
					LDAPEntity role = retrieveMyEntity(username, roles);
					if (role != null)
						up.setRole(RoleType.fromString(role.getName()));// override role
					upservice.save(up, lang);
				} else {
					// if the user is not enabled, remove completly the up with its cached groups, executeds, ppois, subscriptions
					Userprofile up = upservice.get(username, lang);
					if (up != null)
						upservice.delete(up, lang);
				}
			}
		}
	}

	private LDAPEntity retrieveMyEntity(String username, List<LDAPEntity> entities) {
		for (LDAPEntity entity : entities)
			if (entity.getUsernames().contains(username))
				return entity;
		return null;
	}

	// group can return more than one groups
	private Set<Groupname> retrieveMyEntities(String username, List<LDAPEntity> entities) {
		Set<Groupname> toreturn = new HashSet<Groupname>();

		for (LDAPEntity entity : entities)
			if (entity.getUsernames().contains(username)) {
				try {
					toreturn.add(new Groupname(entity.getName()));
				} catch (IllegalArgumentException iae) {
					logger.warn("catched/ignoring {}", iae.getMessage());
					continue;
				}
			}

		return toreturn;
	}
}