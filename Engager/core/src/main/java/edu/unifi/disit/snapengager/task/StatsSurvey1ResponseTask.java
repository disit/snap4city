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
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.snap4city.engager_utils.OrganizationType;
import edu.unifi.disit.snapengager.datamodel.Data;
import edu.unifi.disit.snapengager.datamodel.SurveyRuleNameType;
import edu.unifi.disit.snapengager.datamodel.profiledb.StatsSurvey1Response;
import edu.unifi.disit.snapengager.datamodel.profiledb.StatsSurvey1ResponseCount;
import edu.unifi.disit.snapengager.datamodel.profiledb.StatsSurvey1ResponseCountDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.StatsSurvey1ResponseDAO;
import edu.unifi.disit.snapengager.exception.CredentialsException;
import edu.unifi.disit.snapengager.exception.UserprofileException;
import edu.unifi.disit.snapengager.service.IDataManagerService;

@EnableScheduling
@Component
public class StatsSurvey1ResponseTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${stats.survey1.task.cron}")
	private String cronrefresh;

	@Autowired
	IDataManagerService dmService;

	@Autowired
	StatsSurvey1ResponseCountDAO ssrcRepo;

	@Autowired
	StatsSurvey1ResponseDAO ssrRepo;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug("-----------------------------------------------start execution for STATS SURVEY1 RESPONSE -----------------------------------------------");
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
				logger.debug("-----------------------------------------------next execution for STATS SURVEY1 RESPONSE will be fired at:{}-----------------------------------------------", nextExec);
				return nextExec;
			}
		});
	}

	private void myTask() throws UserprofileException, CredentialsException, IOException {
		Locale lang = new Locale("en");

		// get all the submitted surveis
		List<Data> allsurveis = dmService.getAllSurveyData(lang);

		List<JsonNode> surveyHelsinki = new ArrayList<JsonNode>();
		List<JsonNode> surveyAntwerp = new ArrayList<JsonNode>();
		List<JsonNode> surveyToscana = new ArrayList<JsonNode>();

		// smista all submitted surveis
		smista(allsurveis, surveyHelsinki, surveyAntwerp, surveyToscana);

		// update the helsinki
		if (!surveyHelsinki.isEmpty())
			update(surveyHelsinki, OrganizationType.HELSINKI);

		// update the antwerp
		if (!surveyAntwerp.isEmpty())
			update(surveyAntwerp, OrganizationType.ANTWERP);

		// update the firenze
		if (!surveyToscana.isEmpty())
			update(surveyToscana, OrganizationType.TOSCANA);

	}

	private void smista(List<Data> allsurveis, List<JsonNode> surveyHelsinki, List<JsonNode> surveyAntwerp, List<JsonNode> surveyToscana) throws IOException {
		for (Data survey : allsurveis) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode actualObj = mapper.readTree(survey.getVariableValue());
			JsonNode rulenameNode = actualObj.get("rulename");
			if ((rulenameNode != null) && (!rulenameNode.isNull()) && (rulenameNode.asText().equals(SurveyRuleNameType.survey_antwerp_experimentation.toString())))
				surveyAntwerp.add(actualObj.get("response"));
			else if ((rulenameNode != null) && (!rulenameNode.isNull()) && (rulenameNode.asText().equals(SurveyRuleNameType.survey_helsinki_experimentation.toString())))
				surveyHelsinki.add(actualObj.get("response"));
			else if ((rulenameNode != null) && (!rulenameNode.isNull()) && (rulenameNode.asText().equals(SurveyRuleNameType.survey_firenze_experimentation.toString())))
				surveyToscana.add(actualObj.get("response"));
			else
				logger.warn("{} not recognized, ignoring", rulenameNode);
		}
	}

	private void update(List<JsonNode> responses, OrganizationType o) {
		// update count
		StatsSurvey1ResponseCount ssrc = new StatsSurvey1ResponseCount(o.toString(), responses.size());
		ssrcRepo.save(ssrc);

		// update entries
		StatsSurvey1Response ssr1 = new StatsSurvey1Response(o.toString(), "question1");
		StatsSurvey1Response ssr2 = new StatsSurvey1Response(o.toString(), "question2");
		StatsSurvey1Response ssr3 = new StatsSurvey1Response(o.toString(), "question3");
		StatsSurvey1Response ssr4 = new StatsSurvey1Response(o.toString(), "question4");
		StatsSurvey1Response ssr5 = new StatsSurvey1Response(o.toString(), "question5");
		StatsSurvey1Response ssr6 = new StatsSurvey1Response(o.toString(), "question6");
		StatsSurvey1Response ssr7 = new StatsSurvey1Response(o.toString(), "question7");
		StatsSurvey1Response ssr8 = new StatsSurvey1Response(o.toString(), "question8");
		StatsSurvey1Response ssr9 = new StatsSurvey1Response(o.toString(), "question9");
		StatsSurvey1Response ssr10 = new StatsSurvey1Response(o.toString(), "question10");
		StatsSurvey1Response ssr11 = new StatsSurvey1Response(o.toString(), "question11");
		StatsSurvey1Response ssr12 = new StatsSurvey1Response(o.toString(), "question12");
		StatsSurvey1Response ssr13 = new StatsSurvey1Response(o.toString(), "question13");

		for (JsonNode jn : responses) {
			addProperly(ssr1, jn.get("question1"));
			addProperly(ssr2, jn.get("question2"));
			addProperly(ssr3, jn.get("question3"));
			addProperly(ssr4, jn.get("question4"));
			addProperly(ssr5, jn.get("question5"));
			addProperly(ssr6, jn.get("question6"));
			addProperly(ssr7, jn.get("question7"));
			addProperly(ssr8, jn.get("question8"));
			addProperly(ssr9, jn.get("question9"));
			addProperly(ssr10, jn.get("question10"));
			addProperly(ssr11, jn.get("question11"));
			addProperly(ssr12, jn.get("question12"));
			addProperly(ssr12, jn.get("question13"));
		}

		ssrRepo.save(ssr1);
		ssrRepo.save(ssr2);
		ssrRepo.save(ssr3);
		ssrRepo.save(ssr4);
		ssrRepo.save(ssr5);
		ssrRepo.save(ssr6);
		ssrRepo.save(ssr7);
		ssrRepo.save(ssr8);
		ssrRepo.save(ssr9);
		ssrRepo.save(ssr10);
		ssrRepo.save(ssr11);
		ssrRepo.save(ssr12);
		ssrRepo.save(ssr13);
	}

	private void addProperly(StatsSurvey1Response ssr, JsonNode node) {
		if ((node != null) && (!node.isNull()) && (node.isTextual())) {
			ssr.addAnswer(node.asText());
		} else if ((node != null) && (!node.isNull()) && (node.isNumber())) {
			ssr.addAnswer(node.asInt());
		} else if ((node != null) && (!node.isNull()) && (node.isArray())) {
			List<String> lista = new ArrayList<String>();
			for (final JsonNode objNode : node)
				lista.add(objNode.asText());
			ssr.addAnswers(lista);
		} else
			logger.warn("node is null or not recognized, ignoring");
	}
}