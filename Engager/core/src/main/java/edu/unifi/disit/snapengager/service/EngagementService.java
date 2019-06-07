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
package edu.unifi.disit.snapengager.service;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.unifi.disit.snap4city.engager_utils.EngagementType;
import edu.unifi.disit.snapengager.datamodel.EmailScenarioType;
import edu.unifi.disit.snapengager.datamodel.EngagementStatusType;
import edu.unifi.disit.snapengager.datamodel.ldap.LDAPUserDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.Engagement;
import edu.unifi.disit.snapengager.datamodel.profiledb.EngagementDAO;
import edu.unifi.disit.snapengager.event.OnPreparationEmailCompleteEvent;
import edu.unifi.disit.snapengager.exception.CredentialsException;
import edu.unifi.disit.snapengager.exception.EngagementException;
import edu.unifi.disit.snapengager.exception.UserprofileException;

@Service
public class EngagementService implements IEngagementService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	LDAPUserDAO ldaprepo;

	@Autowired
	EngagementDAO engarepo;

	@Autowired
	ICredentialsService credService;

	@Autowired
	IUserprofileService upService;

	@Autowired
	ApplicationEventPublisher eventPublisher;

	@Autowired
	private MessageSource messages;

	@Override
	public Engagement get(Long id, Locale lang) {
		return engarepo.findById(id);
	}

	@Override
	public List<Engagement> getActive(Locale lang) {
		return engarepo.findByUsernameAndStatus(credService.getLoggedUsername(lang), EngagementStatusType.CREATED);
	}

	@Override
	public List<Engagement> getElapsed(Locale lang) {
		return engarepo.findByStatusAndElapseBefore(EngagementStatusType.CREATED, new Date());
	}

	@Override
	public void add(List<Engagement> en, Locale lang) throws UserprofileException {
		for (Engagement e : en) {
			logger.debug("Elaborating engagement {}", e.toString());

			// --------------rules about SENDRATE
			Engagement previous = engarepo.findTop1ByUsernameAndRulenameOrderByIdDesc(e.getUsername(), e.getRulename());

			// IF already sent one engagement of same class + SENDRATE=never ---> SKIPPING
			if ((previous != null) && (e.getSendrate() == 0)) {
				logger.debug("Skipping due sendrate zero");
				continue;
			}

			// IF already sent one engagement of same class + tempo passato dall'ultimo send<SENDRATE ---> SKIPPING
			if ((previous != null) && (System.currentTimeMillis() - previous.getCreated().getTime()) < e.getSendrateMS()) {
				logger.debug("Skipping due sendrate {}", previous.toString());
				continue;
			}

			// --------------rules about ELAPSED

			// IF the last is still active active (not deleted nor elapsed) + elapsed=NEVER ---> SKIPPING
			if ((previous != null) && (previous.getStatus().equals(EngagementStatusType.CREATED)) && /* (e.getSendrate() != 0) && */ (e.getElapse() == null)) {
				logger.debug("Skipping due elapse {}", previous.toString());
				continue;
			}

			logger.debug("Adding");
			engarepo.save(e);

			// if is a special engagement (SUBSCRIPTION, send an EMAIL)
			if ((e.getType() != null) && (EngagementType.SUBSCRIPTION.equals(e.getType()))) {
				eventPublisher.publishEvent(prepareEventForRegistration(e, lang));
			}
		}
	}

	@Override
	public void setDeleted(Long engagementId, Locale lang) throws EngagementException, NoSuchMessageException, CredentialsException {
		Engagement e = setElapsedAbstract(engagementId, lang);
		credService.checkUsernameCredentials(e.getUsername(), lang);
		e.setDeleted(new Date());
		engarepo.save(e);
	}

	@Override
	public void setElapsed(Long engagementId, Locale lang) throws EngagementException {
		Engagement e = setElapsedAbstract(engagementId, lang);
		engarepo.save(e);
	}

	private Engagement setElapsedAbstract(Long engagementId, Locale lang) throws EngagementException {
		Engagement e = engarepo.findById(engagementId);
		if (e == null) {
			logger.error("The engagement with id {} is not found", engagementId);
			throw new EngagementException(messages.getMessage("engagement.ko.notfound", new Object[] { engagementId }, lang));
		}

		e.setStatus(EngagementStatusType.ELAPSED);

		return e;
	}

	@Override
	public void setAllDeleted(Locale lang) throws EngagementException {
		// different from above since we don't need to enforce credential in this case
		List<Engagement> engagements = engarepo.findByUsername(credService.getLoggedUsername(lang));
		for (Engagement engagement : engagements) {
			engagement.setStatus(EngagementStatusType.ELAPSED);
			engagement.setDeleted(new Date());
		}
		engarepo.save(engagements);
	}

	private OnPreparationEmailCompleteEvent prepareEventForRegistration(Engagement e, Locale lang) throws UserprofileException {

		// enrich email
		List<String> emails = ldaprepo.getEmails(e.getUsername());
		if (emails.size() != 0) {
			String recipientAddress = emails.get(0);
			String subject = "Alert from Snap4City";
			return new OnPreparationEmailCompleteEvent(recipientAddress, subject, e.getMessage(), EmailScenarioType.SENSOR_SUBSCRIPTION, lang);
		} else {
			logger.error("no mail found for user {}", e.getUsername());
			throw new UserprofileException(messages.getMessage("userprofile.ko.noemail", new Object[] { e.getUsername() }, lang));
		}
	}

	@Override
	public Page<Engagement> findAll(Pageable pageable, Locale lang) {
		logger.debug("findAll INVOKED on pageNumber {}, pageSize {}", pageable.getPageNumber(), pageable.getPageSize());
		return engarepo.findByDeletedIsNull(pageable);
	}

	@Override
	public Page<Engagement> findAllBySearch(String searchKey, Pageable pageable, Locale lang) {
		logger.debug("findAllBySearch INVOKED on searchKey {} pageNumber {}, pageSize {}", searchKey, pageable.getPageNumber(), pageable.getPageSize());
		return engarepo
				.findByUsernameContainingOrTitleContainingOrSubtitleContainingOrRulenameContainingOrMessageContainingAllIgnoreCaseAndDeletedIsNull(
						searchKey, searchKey, searchKey, searchKey, searchKey, pageable);
	}

	@Override
	public Page<Engagement> findAllLogged(Pageable pageable, Locale lang) {
		logger.debug("findAllLogged INVOKED on pageNumber {}, pageSize {}", pageable.getPageNumber(), pageable.getPageSize());
		return engarepo
				.findByUsernameAndDeletedIsNull(credService.getLoggedUsername(lang), pageable);
	}

	@Override
	public Page<Engagement> findAllLoggedBySearch(String searchKey, Pageable pageable, Locale lang) {
		logger.debug("findAllBySearch INVOKED on searchKey {} pageNumber {}, pageSize {}", searchKey, pageable.getPageNumber(), pageable.getPageSize());
		return engarepo
				.findByUsernameAndTitleContainingOrSubtitleContainingOrRulenameContainingOrMessageContainingAllIgnoreCaseAndDeletedIsNull(
						credService.getLoggedUsername(lang), searchKey, searchKey, searchKey, searchKey, pageable);
	}

	@Override
	public Engagement getLastActive(Locale lang) {
		return engarepo.findTopByUsernameAndStatusOrderByIdDesc(credService.getLoggedUsername(lang), EngagementStatusType.CREATED);
	}
}