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

import org.disit.TrafficFlowManager.utils.ConfigProperties;
import org.disit.TrafficFlowManager.utils.Logger;

import javax.json.*;
import java.io.*;

public class JSONStaticGraphPersistence implements StaticGraphPersistenceInterface {

    private final String staticGraphFolder;

    public JSONStaticGraphPersistence() throws IOException {
        staticGraphFolder = ConfigProperties.getProperties().getProperty("staticGraphsFolder");
        new File(staticGraphFolder).mkdir();
    }

    @Override
    public void saveStaticGraph(String staticGraphName, JsonValue json) throws IOException {
        Logger.log("[SDB] Saving static graph " + staticGraphName);
        String filename = staticGraphFolder + "/" + staticGraphName + ".json";
        try (FileWriter file = new FileWriter(filename)) {
            file.write(json.toString());
            file.flush();
        }
    }

    @Override
    public JsonValue getStaticGraph(String staticGraphName) throws FileNotFoundException {
        Logger.log("[SDB] retrieving static graph " + staticGraphName);
        String filename = staticGraphFolder + "/" + staticGraphName + ".json";
        InputStream inputStream = new FileInputStream(filename);
        JsonReader reader = Json.createReader(inputStream);
        return reader.readValue();
    }
}
