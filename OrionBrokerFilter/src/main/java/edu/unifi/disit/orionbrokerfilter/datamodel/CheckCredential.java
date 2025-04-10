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

public class CheckCredential extends DelegationPublic {

	String username;
	Boolean result = false;
	String kind;

	public CheckCredential(Integer minutesElapsingCache) {
		super(minutesElapsingCache);
	}

	public CheckCredential(String elementType, String username, Boolean result, Integer minutesElapsingCache) {
		super(elementType, minutesElapsingCache);

		this.username = username;
		this.result = result;
		this.kind = "READ_ACCESS";
	}

	public CheckCredential(String elementType, String username, Boolean result, Integer minutesElapsingCache, String kind) {
		super(elementType, minutesElapsingCache);

		this.username = username;
		this.result = result;
		this.kind = ("READ_ACCESS".equals(kind) || "READ_WRITE".equals(kind) || 
                        "WRITE_ONLY".equals(kind) || "MODIFY".equals(kind)) ? kind : "READ_ACCESS";
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Boolean getResult() {
		return result;
	}

	public void setResult(Boolean result) {
		this.result = result;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	@Override
	public String toString() {
		return "CheckCredential [username=" + username + ", result=" + result + ", elementType=" + elementType + ", elapsingDate=" + elapsingDate + ", kind=" + kind + "]";
	}

}