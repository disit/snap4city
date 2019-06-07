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

import java.util.List;
import java.util.Locale;
import java.util.Random;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.unifi.disit.snapengager.datamodel.profiledb.Event;
import edu.unifi.disit.snapengager.datamodel.profiledb.EventDAO;

@Service
public class EventServiceImpl implements IEventService {

	// private static final Logger logger = LogManager.getLogger();

	@Autowired
	EventDAO eventRepo;

	@Override
	public Event getRandomEvent(String organization, Locale lang) {

		// TODO patch DISIT to Firenze
		if (organization.equals("DISIT"))
			organization = "Firenze";

		if (organization.equals("Toscana"))
			organization = "Firenze";

		List<Event> events = eventRepo.findByOrganization(organization);

		int size = events.size();

		if (size > 0) {

			Random r = new Random(System.currentTimeMillis());
			Integer i = r.nextInt(size);

			return events.get(i);
		} else
			return null;
	}
}