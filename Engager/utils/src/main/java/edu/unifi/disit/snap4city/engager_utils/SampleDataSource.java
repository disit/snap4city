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
package edu.unifi.disit.snap4city.engager_utils;

import java.util.ArrayList;
import java.util.List;

public class SampleDataSource {

	public List<String> getListGenderTypes() {
		List<String> d = new ArrayList<String>();
		d.add(GenderType.MALE.toString());
		d.add(GenderType.FEMALE.toString());
		return d;
	}

	public List<String> getListOrganizationTypes() {
		List<String> d = new ArrayList<String>();
		d.add(OrganizationType.ANTWERP.toString());
		d.add(OrganizationType.DISIT.toString());
		d.add(OrganizationType.FIRENZE.toString());
		d.add(OrganizationType.HELSINKI.toString());
		return d;
	}

	public List<String> getListGroupTypes() {
		List<String> d = new ArrayList<String>();
		// COMMONS
		d.add(GroupType.BUSINESS_OWNERS.toString());
		d.add(GroupType.CITIZENS.toString());
		// ANTWERP
		d.add(GroupType.CITY_COUNCIL.toString());
		d.add(GroupType.CITY_OFFICIALS.toString());
		d.add(GroupType.DATA_PROVIDERS.toString());
		d.add(GroupType.DEVELOPERS.toString());
		d.add(GroupType.DIGIPOLIS.toString());
		// HELSINKI
		d.add(GroupType.CITIZENS_WITH_RESPIRATORY_PROBLEMS.toString());
		d.add(GroupType.CITY_OFFICIAL_DEVELOPERS.toString());
		d.add(GroupType.CITY_OFFICIAL_DOMAIN_EXPERTS.toString());
		d.add(GroupType.FORUMVIRIUM.toString());
		d.add(GroupType.THIRD_PARTY_DEVELOPERS.toString());
		d.add(GroupType.TOURISTS.toString());
		// DISIT
		d.add(GroupType.DEVELOPER.toString());
		d.add(GroupType.OPERATIVO.toString());
		// FIRENZE
		d.add(GroupType.AMBIENTE.toString());
		d.add(GroupType.SINDACO.toString());
		return d;
	}

	public List<String> getListRoleTypes() {
		List<String> d = new ArrayList<String>();
		d.add(RoleType.PUBLIC.toString());
		d.add(RoleType.OBSERVER.toString());
		d.add(RoleType.MANAGER.toString());
		d.add(RoleType.AREAMANAGER.toString());
		d.add(RoleType.TOOLADMIN.toString());
		d.add(RoleType.ROOTADMIN.toString());
		return d;
	}

	public List<String> getListTimeslotTypes() {
		List<String> d = new ArrayList<String>();
		d.add(TimeslotType.MORNING.toString());
		d.add(TimeslotType.LUNCH.toString());
		d.add(TimeslotType.AFTERNOON.toString());
		d.add(TimeslotType.EVENING.toString());
		d.add(TimeslotType.NIGHT.toString());
		return d;
	}

	public List<String> getListSensorTypes() {
		List<String> d = new ArrayList<String>();
		d.add("AirTemperatureAverage2HourHelsinki");// helsinki
		d.add("AirHumidityAverage2HourHelsinki");
		d.add("LAeqAverage2HourHelsinki");
		d.add("AirQualityPM10Average2HourHelsinki");
		d.add("AirQualityPM2_5Average2HourHelsinki");
		d.add("AirQualityNO2Average2HourHelsinki");
		d.add("AirQualityAQIAverage2HourHelsinki");
		d.add("EAQI1hourAverageHelsinki");
		d.add("EnfuserHelsinkiEnfuserAirQualityIndex");
		d.add("EnfuserHelsinkiHighDensityPM10");
		d.add("EnfuserHelsinkiHighDensityPM25");
		d.add("AirTemperatureAverage2HourAntwerp");// antwerp
		d.add("AirHumidityAverage2HourAntwerp");
		d.add("AirQualityPM10Average2HourAntwerp");
		d.add("AirQualityPM2_5Average2HourAntwerp");
		d.add("BikeFeelingAntwerp");
		d.add("EAQI1hourAverageAntwerp");// florence
		d.add("FlorenceAccidentsDensity");
		d.add("WindSpeedAverage2HourFlorence");
		d.add("AirTemperatureAverage2HourFlorence");
		d.add("AirHumidityAverage2HourFlorence");
		d.add("PM10Average24HourFlorence");
		d.add("PM2_5Average24HourFlorence");
		d.add("NO2Average24HourFlorence");
		return d;
	}

	public List<String> getListEngagementTypes() {
		List<String> d = new ArrayList<String>();
		d.add(EngagementType.ALERT.toString());
		d.add(EngagementType.REWARD.toString());
		d.add(EngagementType.SUBSCRIPTION.toString());
		d.add(EngagementType.SURVEY.toString());
		d.add(EngagementType.EVENT.toString());
		return d;
	}
}