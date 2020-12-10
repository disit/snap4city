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
package edu.unifi.disit.datamanager.datamodel.ldap;

import java.util.List;
import java.util.Locale;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.exception.LDAPException;

@Service
public interface LDAPUserDAO {

	List<String> getOUnames(String username);

	List<String> getGroupnames(String username);

	@Cacheable(value = "LDAPgetGroupAndOUnames")
	List<String> getGroupAndOUnames(String username);

	boolean usernameExist(String username);

	boolean groupnameExist(String username);

	List<String> getUsernamesFromGroup(String groupnamefilter, Locale lang) throws LDAPException;
}
