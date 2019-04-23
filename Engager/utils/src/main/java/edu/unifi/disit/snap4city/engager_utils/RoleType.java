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

public enum RoleType {
	OBSERVER("Observer"), PUBLIC("Public"), MANAGER("Manager"), AREAMANAGER("AreaManager"), TOOLADMIN("ToolAdmin"), ROOTADMIN("RootAdmin");

	private final String text;

	private RoleType(final String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

	public static RoleType fromString(String text) {
		switch (text) {
		case "Observer":
			return RoleType.OBSERVER;
		case "Public":
			return RoleType.PUBLIC;
		case "Manager":
			return RoleType.MANAGER;
		case "AreaManager":
			return RoleType.AREAMANAGER;
		case "ToolAdmin":
			return RoleType.TOOLADMIN;
		case "RootAdmin":
			return RoleType.ROOTADMIN;
		default:
			throw new IllegalArgumentException("Profile [" + text + "] not supported.");
		}
	}
}