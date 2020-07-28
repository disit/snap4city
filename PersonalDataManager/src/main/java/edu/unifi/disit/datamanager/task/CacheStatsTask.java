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

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.cache.management.CacheStatisticsMXBean;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.cache.CacheManager;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class CacheStatsTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${cachestats.task.cron}")
	private String cronrefresh;

	@Autowired
	CacheManager cacheManager;

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

				logger.debug("next execution for Cache Stats will be fired at:{}", nextExec);

				return nextExec;
			}
		});
	}

	private void myTask() {

		logger.debug("Cache Stats STARTED");

		// Collection<Metric<?>> mets = metrics.metrics();
		// for (Iterator<Metric<?>> iterator = mets.iterator(); iterator.hasNext();) {
		// Metric<?> met = iterator.next();
		// String metName = met.getName();
		// logger.debug(metName + ":" + met.getValue());
		//
		// }

		Collection<String> cacheNames = cacheManager.getCacheNames();

		Iterator<String> iterator = cacheNames.iterator();

		// while loop
		while (iterator.hasNext()) {

			String name = iterator.next();

			logger.debug("---->>Cache name= " + name);

			CacheStatisticsMXBean CacheStatBean = getCacheStatisticsMXBean(name);
			if (CacheStatBean != null) {
				logger.debug("Cache hits #{} misses #{}", CacheStatBean.getCacheHits(), CacheStatBean.getCacheMisses());
				logger.debug("Cache hits %{} misses %{}", CacheStatBean.getCacheHitPercentage(),
						CacheStatBean.getCacheMissPercentage());
				logger.debug("Cache gets #{}", CacheStatBean.getCacheGets());
				logger.debug("Cache evictions #{}", CacheStatBean.getCacheEvictions());
				logger.debug("Cache average get time {} milliseconds", CacheStatBean.getAverageGetTime());
			}
		}

		logger.debug("Cache Stats TERMINATED");

	}

	public static CacheStatisticsMXBean getCacheStatisticsMXBean(final String cacheName) {
		final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = null;
		try {
			name = new ObjectName("*:type=CacheStatistics,*,Cache=" + cacheName);
		} catch (MalformedObjectNameException ex) {
			logger.error("Someting wrong with ObjectName {}", ex);
		}
		Set<ObjectName> beans = mbeanServer.queryNames(name, null);
		if (beans.isEmpty()) {
			logger.debug("Cache Statistics Bean not found");
			return null;
		}
		ObjectName[] objArray = beans.toArray(new ObjectName[beans.size()]);
		return JMX.newMBeanProxy(mbeanServer, objArray[0], CacheStatisticsMXBean.class);
	}
}
