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

		// for any available heatmaps
		for (Sensortype sensor : sensortypeRepo.findAll()) {
			String mapname = sensor.getName();
			logger.debug("cycling on {}", mapname);

			try {

				// retrieve date of last cached entry of this heatmap (on engager)
				Date last = laservice.getLastUpdate(DatasetType.SENSOR.toString() + "_" + mapname, lang);

				// retrieve date of last updated entry of this heatmap (from heatmap server)
				Date current = sensorservice.getLastDate(mapname, lang);

				// for any ppoi of the users
				// TODO avoid considering ppoi of user not active
				for (Ppoi ppoi : ppoiRepo.findAll()) {

					String latit = ppoi.getLatitudeAprox();
					String longit = ppoi.getLongitudeAprox();

					if (sensorservice.checkSensorValidity(mapname, latit, longit)) {
						logger.debug("cycling on {}", ppoi.getName());

						// retrieve cached value (on engager)
						List<Sensor> cachedSensor = sensorRepo.findByLatitudeAndLongitudeAndMapname(latit, longit, mapname);

						// for this heatmap on this ppoi: if there is an update or there is not yet a cached value
						if ((current.after(last) || (cachedSensor.size() == 0))) {

							Sensor updatedSensor = null;
							if (cachedSensor.size() == 0) {
								updatedSensor = new Sensor(latit, longit, current, mapname);
							} else {
								updatedSensor = cachedSensor.get(0);
								updatedSensor.setInsertdate(current);
								if (cachedSensor.size() > 1)
									logger.warn("there is more than a value cached for heatmap {} ppoi {}. Ignoring and use first one", mapname, ppoi);
							}

							// retrieve updated value (from heatmap server)
							sensorservice.update(updatedSensor, lang);
						}
					} else {
						logger.debug("skipping {}", ppoi.getName());
					}
				}

				// update last entry
				laservice.updateLastUpdate(DatasetType.SENSOR.toString() + "_" + mapname, current, lang);
			} catch (IOException ioe) {
				logger.warn("Error catched in sensoring {}", ioe);
			}
		}
	}

}