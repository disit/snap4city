/* Data Manager (DM).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
package edu.unifi.disit.datamanager.task;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.datamodel.profiledb.DataDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DelegationDAO;

@EnableScheduling
@Component
public class DeleteDataTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${deletedata.task.cron}")
	private String cronrefresh;

	@Autowired
	DataDAO dataRepo;

	@Autowired
	DelegationDAO delegationRepo;

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

		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -1);

		List<Data> datas = dataRepo.findByDeleteTimeBefore(c.getTime());

		if (datas.size() != 0) {
			logger.debug("Deleting {} data", datas.size());
			dataRepo.delete(datas);
		}

		List<Delegation> delegations = delegationRepo.findByDeleteTimeBefore(c.getTime());

		if (delegations.size() != 0) {
			logger.debug("Deleting {} data", delegations.size());
			delegationRepo.delete(delegations);
		}
	}
}