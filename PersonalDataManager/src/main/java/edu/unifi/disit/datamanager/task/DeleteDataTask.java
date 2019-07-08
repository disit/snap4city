/* Data Manager (DM).
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
package edu.unifi.disit.datamanager.task;

import java.util.Calendar;
import java.util.Date;

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

import edu.unifi.disit.datamanager.datamodel.profiledb.ActivityDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.ActivityViolationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.DataDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.DelegationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivityDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivityViolationDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIDataDAO;

@EnableScheduling
@Component
public class DeleteDataTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${deletedata.task.cron}")
	private String cronrefresh;

	@Value("${deletedata.howmanymonthdata}")
	private Integer howmanymonthdata;

	@Value("${deletedata.howmanymonthactivity}")
	private Integer howmanymonthactivity;

	@Autowired
	DataDAO dataRepo;

	@Autowired
	KPIDataDAO kpidataRepo;

	@Autowired
	DelegationDAO delegationRepo;

	@Autowired
	ActivityDAO activitiesRepo;

	@Autowired
	ActivityViolationDAO activitiesViolationRepo;

	@Autowired
	KPIActivityDAO kpiactivitiesRepo;

	@Autowired
	KPIActivityViolationDAO kpiactivitiesViolationRepo;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			@Override
			public void run() {
				myTask();
			}
		}, new Trigger() {

			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {

				CronTrigger trigger = new CronTrigger(cronrefresh);
				Date nextExec = trigger.nextExecutionTime(triggerContext);

				logger.debug("next execution for DATA DELETE will be fired at:{}", nextExec);

				return nextExec;
			}
		});
	}

	private void myTask() {

		logger.debug("Deleting STARTED");

		// removal of the data
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, howmanymonthdata * -1);

		// List<Data> datas = dataRepo.findByDeleteTimeBefore(c.getTime());
		// if (datas.size() != 0) {
		logger.debug("Deleting data");
		dataRepo.deleteByDeleteTimeBefore(c.getTime());
		// }

		// List<Delegation> delegations = delegationRepo.findByDeleteTimeBefore(c.getTime());
		// if (delegations.size() != 0) {
		logger.debug("Deleting delegation");
		delegationRepo.deleteByDeleteTimeBefore(c.getTime());
		// }

		// List<KPIData> kpidatas = kpidataRepo.findByDeleteTimeBefore(c.getTime());
		// if (kpidatas.size() != 0) {
		logger.debug("Deleting kpidata");
		kpidataRepo.deleteByDeleteTimeBefore(c.getTime());
		// }

		// removal of the activities
		c = Calendar.getInstance();
		c.add(Calendar.MONTH, howmanymonthactivity * -1);

		// List<Activity> activities = activitiesRepo.findByTimeBefore(c.getTime());
		// if (activities.size() != 0) {
		logger.debug("Deleting activity");
		activitiesRepo.deleteByTimeBefore(c.getTime());
		// }

		// List<ActivityViolation> activitiesViolation = activitiesViolationRepo.findByTimeBefore(c.getTime());
		// if (activitiesViolation.size() != 0) {
		logger.debug("Deleting activity violation");
		activitiesViolationRepo.deleteByTimeBefore(c.getTime());
		// }

		// List<KPIActivity> kpiactivities = kpiactivitiesRepo.findByInsertTimeBefore(c.getTime());
		// if (kpiactivities.size() != 0) {
		logger.debug("Deleting kpiactivity");
		kpiactivitiesRepo.deleteByInsertTimeBefore(c.getTime());
		// }

		// List<KPIActivityViolation> kpiactivitiesViolation = kpiactivitiesViolationRepo.findByInsertTimeBefore(c.getTime());
		// if (kpiactivitiesViolation.size() != 0) {
		logger.debug("Deleting activity kpiviolation");
		kpiactivitiesViolationRepo.deleteByInsertTimeBefore(c.getTime());
		// }

		logger.debug("Deleting TERMINATED");

	}
}
