/* TrafficFlowManager.
   Copyright (C) 2023 DISIT Lab http://www.disit.org - University of Florence

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

package org.disit.TrafficFlowManager.persistence;

import javax.json.JsonValue;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface StaticGraphPersistenceInterface {
    void saveStaticGraph(String staticGraphName, JsonValue json) throws IOException;
    JsonValue getStaticGraph(String staticGraphName) throws FileNotFoundException;
}
