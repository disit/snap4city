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
import java.util.List;

public class CachedCredentials {

	Boolean isPublic = false;// default is private
	Credentials ownerCredentials;
	List<Credentials> delegatedCredentials;
	Date elapsingDate;

	public CachedCredentials(Integer minutesElapsingCache) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, minutesElapsingCache);
		this.elapsingDate = c.getTime();
	}

	public boolean isElapsed() {
		return elapsingDate.before(new Date());
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Credentials getOwnerCredentials() {
		return ownerCredentials;
	}

	public void setOwnerCredentials(Credentials ownerCredentials) {
		this.ownerCredentials = ownerCredentials;
	}

	public List<Credentials> getDelegatedCredentials() {
		return delegatedCredentials;
	}

	public void setDelegatedCredentials(List<Credentials> delegatedCredentials) {
		this.delegatedCredentials = delegatedCredentials;
	}

	public Date getElapsingDate() {
		return elapsingDate;
	}

	public void setElapsingDate(Date elapsingDate) {
		this.elapsingDate = elapsingDate;
	}

	@Override
	public String toString() {
		return "CachedCredentials [isPublic=" + isPublic + ", ownerCredentials=" + ownerCredentials + ", delegatedCredentials=" + delegatedCredentials + ", elapsingDate=" + elapsingDate + "]";
	}
}