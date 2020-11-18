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

public class DelegationPublic {

	String elementType;
	Date elapsingDate;

	public DelegationPublic(Integer minutesElapsingCache) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, minutesElapsingCache);
		this.elapsingDate = c.getTime();
	}

	public DelegationPublic(String elementType, Integer minutesElapsingCache) {

		this.elementType = elementType;

		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, minutesElapsingCache);
		this.elapsingDate = c.getTime();
	}

	public boolean isElapsed() {
		return this.elapsingDate.before(new Date());
	}

	public String getElementType() {
		return elementType;
	}

	public void setElementType(String elementType) {
		this.elementType = elementType;
	}

	public Date getElapsingDate() {
		return elapsingDate;
	}

	public void setElapsingDate(Date elapsingDate) {
		this.elapsingDate = elapsingDate;
	}

	@Override
	public String toString() {
		return "PublicDelegation [elementType=" + elementType + ", elapsingDate=" + elapsingDate + "]";
	}
}