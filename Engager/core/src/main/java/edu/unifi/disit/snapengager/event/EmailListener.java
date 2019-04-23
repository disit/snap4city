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
package edu.unifi.disit.snapengager.event;

import java.util.Locale;

import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.snapengager.datamodel.EmailScenarioType;
import edu.unifi.disit.snapengager.service.MailSubscriptionBuilder;

@Component
public class EmailListener implements ApplicationListener<OnPreparationEmailCompleteEvent> {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	MailSubscriptionBuilder msb;

	@Autowired
	private JavaMailSender mailSender;

	@Value("${spring.mail.from}")
	private String from;

	@Autowired
	private MessageSource messages;

	@Override
	public void onApplicationEvent(OnPreparationEmailCompleteEvent event) {
		this.sendEmail(event);
	}

	private void sendEmail(OnPreparationEmailCompleteEvent event) {
		logger.debug("prepare for sending an email to {}", event.getTo());

		if (event.getEmailScenarioType().equals(EmailScenarioType.SENSOR_SUBSCRIPTION)) {
			MimeMessage mm = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mm);
			try {
				helper.setFrom(from);
				helper.setTo(event.getTo());
				helper.setSubject(event.getSubject());
				helper.setText(msb.build(retrieveTextSensorSubscription(event.getText(), event.getLang()), event.getLang()), true);// one line of message
			} catch (Exception e) {// need to catch here all the exception
				logger.error("Error triggered creating the message for the event {} {}", event, e);
			}
			mailSender.send(mm);
		}

		logger.debug("sent an email to {}", event.getTo());
	}

	private String retrieveTextSensorSubscription(String message, Locale lang) throws Exception {
		String s = new String();

		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode rootNode = objectMapper.readTree(message.getBytes());

		JsonNode textNode = rootNode.path("text");
		if ((textNode == null) || (textNode.isNull()) || (textNode.isMissingNode())) {
			logger.error("The retreived data does not contains any text");
			throw new Exception(messages.getMessage("login.ko.configurationerror", null, lang));// TODO
		}

		s = s + textNode.asText();

		JsonNode subNameNode = rootNode.path("subscription_name");
		if ((subNameNode == null) || (subNameNode.isNull()) || (subNameNode.isMissingNode())) {
			logger.error("The retreived data does not contains any subscription name");
			throw new Exception(messages.getMessage("login.ko.configurationerror", null, lang));// TODO
		}

		s = s + ". SUBSCRIPT name is " + subNameNode.asText();

		JsonNode ppoiNode = rootNode.path("poi_name");
		if ((ppoiNode == null) || (ppoiNode.isNull()) || (ppoiNode.isMissingNode())) {
			logger.error("The retreived data does not contains any ppoi name");
			throw new Exception(messages.getMessage("login.ko.configurationerror", null, lang));// TODO
		}

		s = s + ". PPOI name is " + ppoiNode.asText();

		JsonNode valueNode = rootNode.path("value");
		if ((valueNode == null) || (valueNode.isNull()) || (valueNode.isMissingNode())) {
			logger.error("The retreived data does not contains any value");
			throw new Exception(messages.getMessage("login.ko.configurationerror", null, lang));// TODO
		}

		s = s + ". Value is " + valueNode.asText();

		return s;
	}
}