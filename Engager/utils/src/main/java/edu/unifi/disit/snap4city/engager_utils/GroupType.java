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

public enum GroupType {

	// COMMONS
	BUSINESS_OWNERS("Business Owners"), CITIZENS("Citizens"),
	// ANTWERP
	CITY_COUNCIL("City Council"), CITY_OFFICIALS("City Officials"), DATA_PROVIDERS("Data Providers"), DEVELOPERS("Developers"), DIGIPOLIS("DIGIPOLIS"),
	// HELSINKI
	CITIZENS_WITH_RESPIRATORY_PROBLEMS("Citizens with respiratory problems"), CITY_OFFICIAL_DEVELOPERS("City Official Developers"), CITY_OFFICIAL_DOMAIN_EXPERTS("City Official Domain Experts"), FORUMVIRIUM(
			"ForumVirium"), THIRD_PARTY_DEVELOPERS("Third party developers"), TOURISTS("Tourists"),
	// DISIT
	DEVELOPER("Developer"), OPERATIVO("Operativo"),
	// FIRENZE
	AMBIENTE("Ambiente"), SINDACO("Sindaco");

	private final String text;

	private GroupType(final String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

	public static GroupType fromString(String text) {
		switch (text) {
		// COMMONS
		case "Business Owners":
			return GroupType.BUSINESS_OWNERS;
		case "Citizens":
			return GroupType.CITIZENS;
		// ANTWERP
		case "City Council":
			return GroupType.CITY_COUNCIL;
		case "City Officials":
			return GroupType.CITY_OFFICIALS;
		case "Data Providers":
			return GroupType.DATA_PROVIDERS;
		case "Developers":
			return GroupType.DEVELOPERS;
		case "DIGIPOLIS":
			return GroupType.DIGIPOLIS;
		// HELSINKI
		case "Citizens with respiratory problems":
			return GroupType.CITIZENS_WITH_RESPIRATORY_PROBLEMS;
		case "City Official Developers":
			return GroupType.CITY_OFFICIAL_DEVELOPERS;
		case "City Official Domain Experts":
			return GroupType.CITY_OFFICIAL_DOMAIN_EXPERTS;
		case "ForumVirium":
			return GroupType.FORUMVIRIUM;
		case "Third party developers":
			return GroupType.THIRD_PARTY_DEVELOPERS;
		case "Tourists":
			return GroupType.TOURISTS;
		// DISIT
		case "Developer":
			return GroupType.DEVELOPER;
		case "Operativo":
			return GroupType.OPERATIVO;
		// FIRENZE
		case "Ambiente":
			return GroupType.AMBIENTE;
		case "Sindaco":
			return GroupType.SINDACO;

		default:
			throw new IllegalArgumentException("Profile [" + text + "] not supported.");
		}
	}
}