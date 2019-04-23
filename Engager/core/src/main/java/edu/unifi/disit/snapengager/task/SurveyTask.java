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

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.builder.KieScanner;
import org.kie.api.runtime.KieContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.snapengager.datamodel.Data;
import edu.unifi.disit.snapengager.datamodel.profiledb.Engagement;
import edu.unifi.disit.snapengager.datamodel.profiledb.Executed;
import edu.unifi.disit.snapengager.datamodel.profiledb.Userprofile;
import edu.unifi.disit.snapengager.service.IDataManagerService;
import edu.unifi.disit.snapengager.service.IEngagementService;
import edu.unifi.disit.snapengager.service.IUserprofileService;

@EnableScheduling
@Component
public class SurveyTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${survey.task.cron}")
	private String cronrefresh;

	KieContainer kContainer;
	KieScanner kScanner;

	@Autowired
	IUserprofileService upservice;

	@Autowired
	IEngagementService engaservice;

	@Autowired
	IDataManagerService dataservice;

	@Autowired
	private MessageSource messages;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug("-----------------------------------------------start execution for SURVEY_RESPONSE-----------------------------------------------");
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
				logger.debug("-----------------------------------------------next execution for SURVEY_RESPONSE will be fired at:{}-----------------------------------------------", nextExec);
				return nextExec;
			}
		});
	}

	private void myTask() throws Exception {
		Locale lang = new Locale("en");

		Hashtable<String, Boolean> assistanceEnabled = dataservice.getAssistanceEnabled(lang);

		for (Data submittedSurvey : dataservice.getSurveyData(lang)) {
			// check if the user has enable the assistance
			if ((assistanceEnabled.get(submittedSurvey.getUsername()) != null) && (assistanceEnabled.get(submittedSurvey.getUsername()))) {
				Userprofile up = upservice.get(submittedSurvey.getUsername(), lang);
				if (up == null)
					up = new Userprofile(submittedSurvey.getUsername());
				upservice.addExecuted(up, createEngagementExecuted(submittedSurvey, lang), lang);
			} else {
				// if the user is not enabled, remove completly the up with its cached groups, executeds, ppois, subscriptions
				Userprofile up = upservice.get(submittedSurvey.getUsername(), lang);
				if (up != null)
					upservice.delete(up, lang);
			}
		}
	}

	private Executed createEngagementExecuted(Data submittedSurvey, Locale lang) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();

		String s = StringEscapeUtils.unescapeJson(submittedSurvey.getVariableValue());

		JsonNode rootNode = objectMapper.readTree(s);
		JsonNode engNode = rootNode.path("engagement_id");

		if ((engNode == null) || (engNode.isNull()) || (engNode.isMissingNode())) {
			logger.error("The retrieved survey does not contains engagement_id");
			throw new IOException(messages.getMessage("survey.ko.notvalidresponse", new Object[] { "engagement_id" }, lang));

		}

		Engagement e = engaservice.get(new Long(engNode.asLong()), lang);

		if (e == null) // if the engagment in the survey is not recognized
			return null;

		Executed ee = new Executed();

		ee.setCreated(new Date());
		ee.setRulename(e.getRulename());
		ee.setPoints(e.getPoints());
		return ee;
	}
}