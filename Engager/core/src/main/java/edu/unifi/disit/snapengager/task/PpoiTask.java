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
import java.util.Hashtable;
import java.util.Locale;

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

import edu.unifi.disit.snapengager.datamodel.KPIData;
import edu.unifi.disit.snapengager.datamodel.profiledb.Ppoi;
import edu.unifi.disit.snapengager.datamodel.profiledb.Userprofile;
import edu.unifi.disit.snapengager.exception.CredentialsException;
import edu.unifi.disit.snapengager.exception.UserprofileException;
import edu.unifi.disit.snapengager.service.IDataManagerService;
import edu.unifi.disit.snapengager.service.IEngagementService;
import edu.unifi.disit.snapengager.service.IUserprofileService;

@EnableScheduling
@Component
public class PpoiTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${ppoi.task.cron}")
	private String cronrefresh;

	KieContainer kContainer;
	KieScanner kScanner;

	@Autowired
	IUserprofileService upservice;

	@Autowired
	IEngagementService engaservice;

	@Autowired
	IDataManagerService dataservice;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug("-----------------------------------------------start execution for PPOI-----------------------------------------------");
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
				logger.debug("-----------------------------------------------next execution for PPOI will be fired at:{}-----------------------------------------------", nextExec);
				return nextExec;
			}
		});
	}

	private void myTask() throws CredentialsException, IOException, UserprofileException {
		Locale lang = new Locale("en");

		// remove all ppois
		upservice.removeAllPpois(lang);

		Hashtable<String, Boolean> assistanceEnabled = dataservice.getAssistanceEnabled(lang);

		// -------------------------------------------------------------MYPOI
		logger.debug("updating MYPOI");
		for (KPIData ppoi : dataservice.getPpoiKpidata(lang)) {
			logger.debug("updating ppoi {}", ppoi);

			Userprofile up = upservice.get(ppoi.getUsername(), lang);

			// check if the user has enable the assistance
			if ((assistanceEnabled.get(ppoi.getUsername()) != null) && (assistanceEnabled.get(ppoi.getUsername()))) {
				if (up == null)
					up = new Userprofile(ppoi.getUsername());
				upservice.addPpoi(up, new Ppoi(ppoi.getValueName(), String.valueOf(ppoi.getLatitude()), String.valueOf(ppoi.getLongitude())), lang);
			} else {
				// if the user is not enabled, remove completly the up with its cached groups, executeds, ppois, subscriptions
				if (up != null)
					upservice.delete(up, lang);
			}
		}

		// -------------------------------------------------------------MY CURRENT POSITION
		logger.debug("updating Current Position");
		for (KPIData ppoi : dataservice.getLocationKpidata(lang)) {
			logger.debug("updating ppoi {}", ppoi);
			// check if the user has enable the assistance
			if ((assistanceEnabled.get(ppoi.getUsername()) != null) && (assistanceEnabled.get(ppoi.getUsername()))) {
				Userprofile up = upservice.get(ppoi.getUsername(), lang);
				if (up == null)
					up = new Userprofile(ppoi.getUsername());
				if ((ppoi.getLatitude() != null) && (ppoi.getLongitude() != null))
					upservice.addPpoi(up, new Ppoi("current-location", String.valueOf(ppoi.getLatitude()), String.valueOf(ppoi.getLongitude())), lang);
			} else {
				// if the user is not enabled, remove completly the up with its cached groups, executeds, ppois, subscriptions
				Userprofile up = upservice.get(ppoi.getUsername(), lang);
				if (up != null)
					upservice.delete(up, lang);
			}
		}
	}
}