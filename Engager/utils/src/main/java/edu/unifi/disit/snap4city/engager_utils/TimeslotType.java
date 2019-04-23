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

import java.util.Calendar;

public enum TimeslotType {
	NIGHT("NIGHT"), MORNING("MORNING"), LUNCH("LUNCH"), AFTERNOON("AFTERNOON"), EVENING("EVENING");

	private final String text;

	private TimeslotType(final String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

	public static TimeslotType fromString(String text) {
		switch (text) {
		case "NIGHT":
			return TimeslotType.NIGHT;
		case "MORNING":
			return TimeslotType.MORNING;
		case "LUNCH":
			return TimeslotType.LUNCH;
		case "AFTERNOON":
			return TimeslotType.AFTERNOON;
		case "EVENING":
			return TimeslotType.EVENING;
		default:
			throw new IllegalArgumentException("Profile [" + text + "] not supported.");
		}
	}

	public static TimeslotType retrieveDaySlot(Long milliseconds) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(milliseconds);
		return retrieveDaySlot(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
	}

	public static TimeslotType retrieveDaySlot(Integer hours, Integer minutes) {
		if (hours < 5)
			return TimeslotType.NIGHT;
		else if ((hours >= 5) && (hours < 8))
			return TimeslotType.MORNING;
		else if ((hours >= 8) && (hours < 10))
			return TimeslotType.MORNING;
		else if (((hours >= 10) && (hours < 12)) ||
				((minutes < 30) && (hours == 12)))
			return TimeslotType.LUNCH;
		else if (((hours >= 13) && (hours < 14)) ||
				((minutes >= 30) && (hours == 12)))
			return TimeslotType.LUNCH;
		else if (((hours >= 14) && (hours < 18)) ||
				((minutes < 30) && (hours == 18)))
			return TimeslotType.AFTERNOON;
		else if (((hours >= 19) && (hours < 21)) ||
				((minutes >= 30) && (hours == 18)))
			return TimeslotType.AFTERNOON;
		else if (hours >= 21)
			return TimeslotType.EVENING;
		else
			throw new IllegalArgumentException("Profile [" + hours + "," + minutes + "] not supported.");
	}
}