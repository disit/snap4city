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

import edu.unifi.disit.snapengager.datamodel.profiledb.Executed;
import edu.unifi.disit.snapengager.datamodel.profiledb.Ppoi;
import edu.unifi.disit.snapengager.datamodel.profiledb.Userprofile;
import edu.unifi.disit.snapengager.exception.UserprofileException;

public interface IUserprofileService {

	Userprofile get(String username, Locale lang) throws UserprofileException;

	List<Userprofile> getAll(Locale lang);

	void save(Userprofile up, Locale lang);

	void addExecuted(Userprofile up, Executed ee, Locale lang);

	void addPpoi(Userprofile up, Ppoi ppoi, Locale lang);

	void delete(Userprofile up, Locale lang);

	void removeAllSubscriptions(Locale lang);

	void removeAllPpois(Locale lang);
}