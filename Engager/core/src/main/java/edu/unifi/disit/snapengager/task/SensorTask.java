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
import java.util.List;
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

import edu.unifi.disit.snapengager.datamodel.DatasetType;
import edu.unifi.disit.snapengager.datamodel.profiledb.Ppoi;
import edu.unifi.disit.snapengager.datamodel.profiledb.PpoiDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.Sensor;
import edu.unifi.disit.snapengager.datamodel.profiledb.SensorDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.Sensortype;
import edu.unifi.disit.snapengager.datamodel.profiledb.SensortypeDAO;
import edu.unifi.disit.snapengager.service.IEngagementService;
import edu.unifi.disit.snapengager.service.ILastUpdateService;
import edu.unifi.disit.snapengager.service.ISensorService;
import edu.unifi.disit.snapengager.service.IUserprofileService;

@EnableScheduling
@Component
public class SensorTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${sensor.task.cron}")
	private String cronrefresh;

	KieContainer kContainer;
	KieScanner kScanner;

	@Autowired
	IUserprofileService upservice;

	@Autowired
	IEngagementService engaservice;

	@Autowired
	ISensorService sensorservice;

	@Autowired
	ILastUpdateService laservice;

	@Autowired
	PpoiDAO ppoiRepo;

	@Autowired
	SensorDAO sensorRepo;

	@Autowired
	SensortypeDAO sensortypeRepo;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug("-----------------------------------------------start execution for SENSOR-----------------------------------------------");
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
				logger.debug("-----------------------------------------------next execution for SENSOR will be fired at:{}-----------------------------------------------", nextExec);
				return nextExec;
			}
		});
	}

	private void myTask() throws IOException {
		Locale lang = new Locale("en");

		// for any available maps
		for (Sensortype sensor : sensortypeRepo.findAll()) {
			String mapname = sensor.getName();
			logger.debug("cycling on {}", mapname);

			// retrieve last entry of this heatmap
			Date last = laservice.getLastUpdate(DatasetType.SENSOR.toString() + "_" + mapname, lang);

			// retrieve current entry available
			Date current = sensorservice.getLastDate(mapname, lang);

			// for any ppoi of the users (TODO eventually here we can get just the ppoi of the active users)
			for (Ppoi ppoi : ppoiRepo.findAll()) {
				logger.debug("cycling on {}", ppoi.getName());

				// retrieve the old value
				List<Sensor> s = sensorRepo.findByLatitudeAndLongitudeAndMapname(ppoi.getLatitude(), ppoi.getLongitude(), mapname);

				// if this map is recent (or old value is missing), update
				if ((current.after(last) || (s.size() == 0))) {

					Sensor sensore = null;
					if (s.size() == 0) {
						sensore = new Sensor(ppoi.getLatitude(), ppoi.getLongitude(), current, mapname);
					} else if (s.size() > 1) {
						// throw error TODO
					} else {
						sensore = s.get(0);
						sensore.setInsertdate(current);
					}

					sensorservice.update(sensore, lang);
				}

				// update last entry
				laservice.updateLastUpdate(DatasetType.SENSOR.toString() + "_" + mapname, current, lang);
			}
		}
	}
}