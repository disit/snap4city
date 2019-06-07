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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIData;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIDataDAO;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIValue;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIValueDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.ActiveTime;
import edu.unifi.disit.snapengager.datamodel.profiledb.ActiveTimeDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.Userprofile;
import edu.unifi.disit.snapengager.datamodel.profiledb.UserprofileDAO;
import edu.unifi.disit.snapengager.exception.CredentialsException;
import edu.unifi.disit.snapengager.exception.UserprofileException;
import edu.unifi.disit.snapengager.service.IUserprofileService;

@EnableScheduling
@Component
public class ActiveTimeTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${activetime.task.cron}")
	private String cronrefresh;

	@Autowired
	IUserprofileService upservice;

	@Autowired
	UserprofileDAO uprepo;

	@Autowired
	ActiveTimeDAO atrepo;

	@Autowired
	KPIValueDAO kvrepo;

	@Autowired
	KPIDataDAO kdrepo;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug("-----------------------------------------------start execution for ACTIVE TIME-----------------------------------------------");
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
				logger.debug("-----------------------------------------------next execution for ACTIVE TIME will be fired at:{}-----------------------------------------------", nextExec);
				return nextExec;
			}
		});
	}

	private void myTask() throws CredentialsException, IOException, UserprofileException {
		Locale lang = new Locale("en");

		for (Userprofile up : upservice.getAll(lang)) {
			logger.debug("Analyze {}", up.getUsername());
			if (up.getFirstlogin() == null) {
				logger.warn("The user {} never logged in, ignoring", up.getUsername());
				continue;
			}
			Set<ActiveTime> ats = up.getActivetime();
			LocalDate last = getLastUpdate(ats, up.getFirstlogin());
			logger.debug("last is {}", last);
			while (last.isBefore(LocalDate.now())) {
				List<KPIValue> events = getEvents(up.getUsername(), last);
				logger.debug("Spazzolo {}, got {} events", last, events.size());
				int seconds = aggregate(events);
				if (seconds < 0)
					logger.error("duration cannot be negative!!!");
				else
					atrepo.save(new ActiveTime(up, last, seconds));
				last = last.plusDays(1);
			}
		}
	}

	private int aggregate(List<KPIValue> values) {
		long mseconds = 0;
		if (values.size() > 1) {
			long begin = values.get(0).getDataTime().getTime();
			logger.debug("begin is {}", begin);
			for (int i = 1; i < values.size(); i++) {
				if ((values.get(i).getDataTime().getTime() - values.get(i - 1).getDataTime().getTime()) >= 180000) {// max difference of 3 minutes
					logger.debug("adding {}", values.get(i - 1).getDataTime().getTime() - begin);
					mseconds = mseconds + values.get(i - 1).getDataTime().getTime() - begin;
					logger.debug("msec is {}", mseconds);
					begin = values.get(i).getDataTime().getTime();
					logger.debug("new begin is {}", begin);
				}
			}
			logger.debug("adding last{}", values.get(values.size() - 1).getDataTime().getTime() - begin);
			// adding last entry
			mseconds = mseconds + values.get(values.size() - 1).getDataTime().getTime() - begin;
		}
		return (int) mseconds / 1000;
	}

	private List<KPIValue> getEvents(String username, LocalDate last) {

		List<KPIValue> toreturn = new ArrayList<KPIValue>();

		// retrieve kpidata for TrackerLocation
		List<KPIData> kpidatas = kdrepo.findByUsernameAndValueNameContainingAndDeleteTimeIsNull(username, "TrackerLocation");

		// retrieve kpidata for AppUsage
		kpidatas.addAll(kdrepo.findByUsernameAndValueNameContainingAndDeleteTimeIsNull(username, "AppUsage"));

		// retrieve kpivalues
		for (KPIData kpidata : kpidatas)
			toreturn.addAll(kvrepo.findByKpiIdAndDataTimeAfterAndDataTimeBefore(kpidata.getId(),
					Date.from(last.atStartOfDay(ZoneId.systemDefault()).toInstant()),
					Date.from(last.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())));

		Collections.sort(toreturn);

		return toreturn;
	}

	private LocalDate getLastUpdate(Set<ActiveTime> ats, Date firstLogin) {

		// if there are not active already saved, take registration date
		if ((ats == null) || (ats.size() == 0))
			return firstLogin.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		LocalDate last = LocalDate.of(1970, 1, 1);
		for (ActiveTime at : ats)
			if (at.getData().isAfter(last))
				last = at.getData();

		return last.plusDays(1);
	}
}