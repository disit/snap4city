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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import edu.unifi.disit.snap4city.engager_utils.LanguageType;
import edu.unifi.disit.snapengager.datamodel.profiledb.Executed;
import edu.unifi.disit.snapengager.datamodel.profiledb.Ppoi;
import edu.unifi.disit.snapengager.datamodel.profiledb.PpoiDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.Userprofile;
import edu.unifi.disit.snapengager.datamodel.profiledb.UserprofileDAO;
import edu.unifi.disit.snapengager.exception.UserprofileException;

@Service
public class UserprofileService implements IUserprofileService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private MessageSource messages;

	@Autowired
	UserprofileDAO uprepo;

	@Autowired
	PpoiDAO ppoiRepo;

	@Override
	public Userprofile get(String username, Locale lang) throws UserprofileException {
		logger.debug("get userprofile for {}", username);

		List<Userprofile> ups = uprepo.findByUsername(username);
		if (ups.isEmpty()) {
			logger.debug("userprofile not found");
			return null;
		} else if (ups.size() > 1) {
			logger.error("too many user registered with same account {}", username);
			throw new UserprofileException(messages.getMessage("userprofile.ko.toomanyuser", new Object[] { username }, lang));
		}

		logger.debug("userprofile is {}", ups.get(0));

		return ups.get(0);
	}

	@Override
	public void save(Userprofile up, Locale lang) {
		up.setLastupdate(new Date());
		uprepo.save(up);
	}

	@Override
	public void addExecuted(Userprofile up, Executed ee, Locale lang) {
		if (ee != null) {
			up.addExecuted(ee);
			save(up, lang);
		}
	}

	@Override
	public void addPpoi(Userprofile up, Ppoi ppoi, Locale lang) {
		if (ppoi != null) {
			// check if this poi is already here
			if (!updatePpoi(up, ppoi, lang))
				up.addPpoi(ppoi);// add
			save(up, lang);
		}
	}

	@Override
	public List<Userprofile> getAll(Locale lang) {
		return uprepo.findAll();
	}

	public boolean updatePpoi(Userprofile up, Ppoi ppoi, Locale lang) {
		if (up.getPpois() == null)
			return false;
		Iterator<Ppoi> it = up.getPpois().iterator();
		while (it.hasNext()) {
			Ppoi current = it.next();
			if (current.getName().equals(ppoi.getName())) {
				current.setLatitude(ppoi.getLatitude());
				current.setLongitude(ppoi.getLongitude());
				return true;
			}
		}
		return false;
	}

	@Override
	public void delete(Userprofile up, Locale lang) {
		uprepo.delete(up);
	}

	@Override
	public void removeAllSubscriptions(Locale lang) {
		for (Userprofile up : this.getAll(lang)) {
			up.removeAllSubscriptions();
			uprepo.save(up);
		}

	}

	@Override
	public void removeAllPpois(Locale lang) {
		for (Userprofile up : this.getAll(lang)) {
			up.removeAllPpois();
			uprepo.save(up);
		}
	}

	@Override
	public Hashtable<String, LanguageType> getUserLanguage(Locale lang) {
		Hashtable<String, LanguageType> toreturn = new Hashtable<String, LanguageType>();
		for (Userprofile up : this.getAll(lang)) {
			if (up.getLanguage() == null) {
				toreturn.put(up.getUsername(), LanguageType.ENG);// default language is ENG
			} else
				toreturn.put(up.getUsername(), LanguageType.fromString(up.getLanguage()));
		}
		return toreturn;
	}
}