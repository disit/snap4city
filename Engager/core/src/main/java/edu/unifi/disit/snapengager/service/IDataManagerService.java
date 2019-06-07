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

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import edu.unifi.disit.snap4city.engager_utils.OrganizationType;
import edu.unifi.disit.snapengager.datamodel.Data;
import edu.unifi.disit.snapengager.datamodel.Organization;
import edu.unifi.disit.snapengager.datamodel.Poi;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIData;
import edu.unifi.disit.snapengager.datamodel.drupaldb.DrupalData;
import edu.unifi.disit.snapengager.datamodel.profiledb.Event;
import edu.unifi.disit.snapengager.exception.CredentialsException;

public interface IDataManagerService {

	public List<Data> getAllSurveyData(Locale lang) throws CredentialsException, IOException;

	public List<Data> getSurveyData(Locale lang) throws CredentialsException, IOException;

	public List<Data> getLastLoginData(Locale lang) throws CredentialsException, IOException;

	public List<KPIData> getPpoiKpidata(Locale lang) throws CredentialsException, IOException;

	public List<KPIData> getLocationKpidata(Locale lang) throws CredentialsException, IOException;

	public List<Data> getSubscriptionData(Locale lang) throws CredentialsException, IOException;

	public Hashtable<String, Boolean> getAssistanceEnabled(Locale lang) throws CredentialsException, IOException;

	public List<Event> getEventData(OrganizationType organization, Locale lang) throws CredentialsException, IOException;

	public List<DrupalData> getDrupalData(Locale lang);

	public List<Data> getLangData(Locale lang) throws CredentialsException, IOException;

	public List<Poi> getPoiData(String organization, String latitude, String longitude, Locale lang) throws CredentialsException, IOException;

	Organization getOrganizationInfo(OrganizationType organizationType, Locale lang) throws IOException;

}