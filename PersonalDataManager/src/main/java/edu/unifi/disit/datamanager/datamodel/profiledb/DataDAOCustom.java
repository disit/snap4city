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
package edu.unifi.disit.datamanager.datamodel.profiledb;

import java.util.Date;
import java.util.List;

public interface DataDAOCustom {

	List<Data> getDataByUsername(String username, String variableName, String motivation, Date from, Date to, Integer first, Integer last);

	List<Data> getDataByUsernameDelegated(String username, String variableName, String motivation, Date from, Date to, Integer first, Integer last);

	List<Data> getDataByAppId(String appId, String appOwner, String variableName, String motivation, Date from, Date to, Integer first, Integer last);
}
