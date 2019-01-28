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

import java.util.Calendar;
import java.util.Date;

public class Ownership extends Credentials {

	String elementUrl;
	Date elapsingDate;

	public Ownership(Integer minutesElapsingCache) {
		super();

		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, minutesElapsingCache);
		this.elapsingDate = c.getTime();
	}

	public Ownership(String elementUrl, String k1, String k2, String username, String pksha1, Integer minutesElapsingCache) {
		super(k1, k2, username, pksha1);
		this.elementUrl = elementUrl;
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, minutesElapsingCache);
		this.elapsingDate = c.getTime();
	}

	public boolean isElapsed() {
		return this.elapsingDate.before(new Date());
	}

	public String getElementUrl() {
		return elementUrl;
	}

	public void setElementUrl(String elementUrl) {
		this.elementUrl = elementUrl;
	}

	@Override
	public String toString() {
		return "Ownership [elementUrl=" + elementUrl + ", elapsingDate=" + elapsingDate + ", username=" + username + ", pksha1=" + pksha1 + ", k1=" + k1 + ", k2=" + k2 + "]";
	}
}