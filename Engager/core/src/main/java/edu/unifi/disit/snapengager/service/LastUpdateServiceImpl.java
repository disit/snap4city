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
import org.springframework.stereotype.Service;

import edu.unifi.disit.snapengager.datamodel.DatasetType;
import edu.unifi.disit.snapengager.datamodel.profiledb.LastUpdate;
import edu.unifi.disit.snapengager.datamodel.profiledb.LastUpdateDAO;

@Service
public class LastUpdateServiceImpl implements ILastUpdateService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	LastUpdateDAO larepo;

	@Override
	public Date getLastUpdate(String type, Locale lang) {
		logger.debug("getLastUpdate {}", type);

		List<LastUpdate> last = larepo.findByDataset(type);
		if (last.isEmpty()) {
			logger.debug("LastUpdate was not specified, use 1970");
			return new Date(0);
		} else {
			logger.debug("LastUpdate is {}", last.get(0).getLastupdate());
			return last.get(0).getLastupdate();
		}
	}

	@Override
	public void updateLastUpdate(String type, Date date, Locale lang) {
		logger.debug("updateLastUpdate {} {}", type, date);

		List<LastUpdate> last = larepo.findByDataset(type);
		LastUpdate tosave = null;
		if (last.isEmpty()) {
			tosave = new LastUpdate(type, date);
		} else {
			tosave = last.get(0);
			tosave.setLastupdate(date);
		}
		larepo.save(tosave);
	}

	@Override
	public Date getLastUpdate(DatasetType type, Locale lang) {
		return getLastUpdate(type.toString(), lang);
	}

	@Override
	public void updateLastUpdate(DatasetType type, Date date, Locale lang) {
		updateLastUpdate(type.toString(), date, lang);
	}
}