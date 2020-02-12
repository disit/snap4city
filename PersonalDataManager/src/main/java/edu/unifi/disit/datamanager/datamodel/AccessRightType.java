/* Data Manager (DM).
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
package edu.unifi.disit.datamanager.datamodel;

public enum AccessRightType {
	ROOTADMIN("ROOTADMIN"), OWNER("OWNER"), PUBLIC("PUBLIC"), DELEGATED("DELEGATED"), GROUP_DELEGATED("GROUP-DELEGATED"), MYGROUP_PUBLIC("MYGROUP-PUBLIC"), MYGROUP_DELEGATED("MYGROUP-DELEGATED");

	// GROUP-DELEGATED means elementId delegated to the organization the user belongs

	// MYGROUP_PUBLIC means elementId belonging to a group public
	// MYGROUP_DELEGATED means elementId belonging to a group delegated to the user
	// MYGROUP_GROUP_DELEGATED means elementId belonging to a group delegated to the organization the user belongs (TODO)

	private final String text;

	private AccessRightType(final String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}
}
