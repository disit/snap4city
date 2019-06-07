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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.disit.snap4city.ENGAGEMENT;
import com.disit.snap4city.ENGAGEMENTS;
import com.disit.snap4city.TIME;

import edu.unifi.disit.snap4city.engager_utils.TimeslotType;
import edu.unifi.disit.snapengager.datamodel.Poi;
import edu.unifi.disit.snapengager.datamodel.profiledb.Engagement;
import edu.unifi.disit.snapengager.datamodel.profiledb.Event;
import edu.unifi.disit.snapengager.datamodel.profiledb.Ppoi;
import edu.unifi.disit.snapengager.datamodel.profiledb.Sensor;
import edu.unifi.disit.snapengager.datamodel.profiledb.SensorDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.Userprofile;
import edu.unifi.disit.snapengager.exception.CredentialsException;
import edu.unifi.disit.snapengager.exception.UserprofileException;
import edu.unifi.disit.snapengager.service.IDataManagerService;
import edu.unifi.disit.snapengager.service.IEngagementService;
import edu.unifi.disit.snapengager.service.IEventService;
import edu.unifi.disit.snapengager.service.IUserprofileService;

@EnableScheduling
@Component
public class DroolTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${drools.task.cron}")
	private String cronrefresh;

	KieContainer kContainer;
	KieScanner kScanner;

	@Autowired
	IUserprofileService upservice;

	@Autowired
	IEngagementService engaservice;

	@Autowired
	SensorDAO sensorRepo;

	@Autowired
	IEventService eventService;

	@Autowired
	IDataManagerService dataservice;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug("-----------------------------------------------start execution for DROOL-----------------------------------------------");
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
				logger.debug("-----------------------------------------------next execution for DROOL will be fired at:{}-----------------------------------------------", nextExec);
				return nextExec;
			}
		});
	}

	private void myTask() throws UserprofileException, CredentialsException, IOException {
		Locale lang = new Locale("en");
		Hashtable<String, Boolean> assistanceEnabled = dataservice.getAssistanceEnabled(lang);

		for (Userprofile up : upservice.getAll(lang)) {
			// if this user has not specified to be assisted, remove the profile and don't enquire the engagement
			if ((assistanceEnabled.get(up.getUsername()) == null) || (!assistanceEnabled.get(up.getUsername())))
				upservice.delete(up, lang);
			else
				engaservice.add(convert(getEngagements(up, lang), up), lang);
		}
	}

	private List<Engagement> convert(ENGAGEMENTS droolsEngagements, Userprofile up) {
		List<Engagement> toreturn = new ArrayList<Engagement>();
		for (ENGAGEMENT e : droolsEngagements.getEngagements())
			toreturn.add(new Engagement(e, up));
		return toreturn;
	}

	private ENGAGEMENTS getEngagements(Userprofile up, Locale lang) throws CredentialsException, IOException {
		Ppoi currentPpoi = null;
		ENGAGEMENTS toreturn = new ENGAGEMENTS();
		KieSession kSession = kContainer.newKieSession("snap4citysession");
		kSession.setGlobal("engagements", toreturn);

		// inseriamo il fatto USER
		logger.debug("Adding user {}", up.getUsername());
		logger.debug("with groups {}", Arrays.toString(up.getGroupnames().toArray()));
		kSession.insert(up.toDrools());

		List<Ppoi> ppoiSensorElaborated = new ArrayList<Ppoi>();// ppoi elaborated for processing sensor value

		// inseriamo i ppoi dell'utente
		for (Ppoi ppoi : up.getPpois()) {
			logger.debug("Adding ppoi {} {} {}", ppoi, ppoi.toDrools().getLatitudeAprox(), ppoi.toDrools().getLongitudeAprox());
			kSession.insert(ppoi.toDrools());
			if (ppoi.getName().equalsIgnoreCase("current-location"))
				currentPpoi = ppoi;

			if (!alreadyElaborated(ppoiSensorElaborated, ppoi)) {// check if a ppoi has been already elaborated IN THE SAME cluster gps latitude longitude
				for (Sensor sensore : sensorRepo.findByLatitudeAndLongitude(ppoi.getLatitudeAprox(), ppoi.getLongitudeAprox())) {
					// inseriamo anche i sensori nei ppoi di questo utente (aprox)
					logger.debug("Adding sensor {}", sensore);
					kSession.insert(sensore.toDrools());
				}
				ppoiSensorElaborated.add(ppoi);// adding current ppoi to already elaborated
			}
		}

		// inseriamo il fatto TIME
		TIME t = retrieveTime(System.currentTimeMillis());
		logger.debug("Adding time {}", t);
		kSession.insert(t);

		// inseriamo un evento dell'organizzazione dell'utente
		if (up.getOrganization() != null) {
			Event ev = eventService.getRandomEvent(up.getOrganization(), lang);
			if (ev != null) {
				logger.debug("Adding event {}", ev);
				kSession.insert(ev.toDrools());
			}
		}

		// inseriamo i POI (garden nelle vicinanze, se il current-ppoi e' <10 minuti)
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, -10);
		if ((currentPpoi != null) && (currentPpoi.getAcquireddate() != null) && (currentPpoi.getAcquireddate().after(c.getTime()))) {
			for (Poi poi : dataservice.getPoiData(up.getOrganization(), currentPpoi.getLatitudeAprox(), currentPpoi.getLongitudeAprox(), lang)) {
				logger.debug("Adding poi {}", poi);
				kSession.insert(poi.toDrools());
			}
		}

		kSession.fireAllRules();
		kSession.dispose();

		if (toreturn != null)
			for (ENGAGEMENT e : toreturn.getEngagements())
				logger.debug("{}", e.getRulename().toString());

		return toreturn;
	}

	private boolean alreadyElaborated(List<Ppoi> ppoiSensorElaborated, Ppoi ppoi) {
		for (Ppoi elaborated : ppoiSensorElaborated) {
			if ((elaborated.getLatitudeAprox().equals(ppoi.getLatitudeAprox())) && (elaborated.getLongitudeAprox().equals(ppoi.getLongitudeAprox())))
				return true;
		}
		return false;
	}

	private TIME retrieveTime(long when) {
		TIME toreturn = new TIME();

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(when);

		// popolate time characteristics
		toreturn.setMonth(calendar.get(Calendar.MONTH) + 1);// 1->12
		toreturn.setDayOfMonth(calendar.get(Calendar.DAY_OF_MONTH));// 1->31
		toreturn.setDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK));// 1(sunday) -> 7(saturday)
		Integer hours = calendar.get(Calendar.HOUR_OF_DAY);// 0->23
		toreturn.setHours(hours);
		Integer minutes = calendar.get(Calendar.MINUTE);// 0->59
		toreturn.setMinutes(minutes);
		toreturn.setDaySlot(TimeslotType.retrieveDaySlot(hours, minutes).toString());

		// one weekbefore
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, -7);
		toreturn.setWeekbefore(calendar.getTime());

		// two hours before
		calendar.setTime(new Date());
		calendar.add(Calendar.HOUR, -2);
		toreturn.setTwohoursbefore(calendar.getTime());

		// two hours before
		calendar.setTime(new Date());
		calendar.add(Calendar.HOUR, -1);
		toreturn.setOnehourbefore(calendar.getTime());

		return toreturn;
	}

	public DroolTask() {
		KieServices ks = KieServices.Factory.get();
		// Dynamic container
		ReleaseId releaseId = ks.newReleaseId("com.disit", "snap4city", "LATEST");
		kContainer = ks.newKieContainer(releaseId);
		kScanner = ks.newKieScanner(kContainer);
		// Start the KieScanner polling the Maven repository every 1 minutes -> 60sec * 1000
		kScanner.start(60000L);
	}

	@PreDestroy
	public void destroy() {
		kScanner.shutdown();
	}
}