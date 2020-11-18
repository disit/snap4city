/* Orion Broker Filter (OBF).
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
package edu.unifi.disit.orionbrokerfilter.datamodel;

public class Credentials {

	String username; // if ownership -> means owner username, if delegation -> means delegated username
	String pksha1;
	String k1;
	String k2;

	public Credentials() {
		super();
	}

	public Credentials(String k1, String k2, String username, String pksha1) {
		this.k1 = k1;
		this.k2 = k2;
		this.username = username;
		this.pksha1 = pksha1;
	}

	public String getK1() {
		return k1;
	}

	public void setK1(String k1) {
		this.k1 = k1;
	}

	public String getK2() {
		return k2;
	}

	public void setK2(String k2) {
		this.k2 = k2;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPksha1() {
		return pksha1;
	}

	public void setPksha1(String pksha1) {
		this.pksha1 = pksha1;
	}

	// check validity of the user
	public boolean isValidUsername(String username) {
		return ((username != null) && (this.username != null) && (this.username.equals(username)));
	}

	// check validity of k1,k2
	private boolean isValidK1K2(String k1, String k2) {
		return ((k1 != null) && (k2 != null) && (this.k1 != null) && (this.k2 != null) && (this.k1.equals(k1) && (this.k2.equals(k2))));
	}

	// check validity of PKSHA1
	private boolean isValidPK(String pksha1) {
		return ((pksha1 != null) && (this.pksha1 != null) && (this.pksha1.equals(pksha1)));
	}

	// if a reqUsername is specified: check validity of this user
	// otherwise, if pkasha is set: check validity of this pksha
	// otherwise: check validity of k1, k2
	public boolean isValid(String k1, String k2, String pksha1) {

		if (this.pksha1 != null)
			return isValidPK(pksha1);
		else
			return isValidK1K2(k1, k2);
	}

	@Override
	public String toString() {
		return "Credentials [username=" + username + ", pksha1=" + pksha1 + ", k1=" + k1 + ", k2=" + k2 + "]";
	}

}