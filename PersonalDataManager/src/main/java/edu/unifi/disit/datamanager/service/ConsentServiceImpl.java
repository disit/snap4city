/* Data Manager (DM).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
package edu.unifi.disit.datamanager.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.profiledb.Consent;
import edu.unifi.disit.datamanager.datamodel.profiledb.ConsentDAO;

@Service
public class ConsentServiceImpl implements IConsentService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	ConsentDAO consentRepo;

	@Override
	public List<Consent> getConsents(String appId) {
		logger.debug("getConsents INVOKED on {}", appId);

		return consentRepo.findByAppId(appId);
	}
}