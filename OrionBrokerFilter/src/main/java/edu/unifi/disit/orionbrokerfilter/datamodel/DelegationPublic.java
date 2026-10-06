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
import java.util.concurrent.ThreadLocalRandom;

public class DelegationPublic {

	String elementType;
        Date requestDate;
	Date elapsingDate;

	public DelegationPublic(Integer minutesElapsingCache, Integer maxMinutesRandomCache) {
		Calendar c = Calendar.getInstance();
                requestDate = c.getTime();
                if(maxMinutesRandomCache !=null && maxMinutesRandomCache > 0) {
                   minutesElapsingCache += ThreadLocalRandom.current().nextInt(maxMinutesRandomCache);
                }
		c.add(Calendar.MINUTE, minutesElapsingCache);
		this.elapsingDate = c.getTime();
	}

	public DelegationPublic(String elementType, Integer minutesElapsingCache, Integer maxMinutesRandomCache) {

		this.elementType = elementType;

		Calendar c = Calendar.getInstance();
                this.requestDate = c.getTime();
                if(maxMinutesRandomCache !=null && maxMinutesRandomCache > 0) {
                   minutesElapsingCache += ThreadLocalRandom.current().nextInt(maxMinutesRandomCache);
                }
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

	public void setElapsingDate(Integer minutesElapsingCache) {
		Calendar c = Calendar.getInstance();
                c.setTime(requestDate);
		c.add(Calendar.MINUTE, minutesElapsingCache);
		this.elapsingDate = c.getTime();
	}
        
	@Override
	public String toString() {
		return "PublicDelegation [elementType=" + elementType + ", elapsingDate=" + elapsingDate + "]";
	}
}