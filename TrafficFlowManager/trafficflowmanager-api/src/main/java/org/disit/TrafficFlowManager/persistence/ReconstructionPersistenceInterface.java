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

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;

public interface ReconstructionPersistenceInterface {
    void addEntry(JsonObject metadata, String layerName) throws IOException;
    void saveReconstructionAsZippedJson(JsonValue json, String layerName) throws IOException;
    JsonArray allLayersClustered() throws IOException;
    JsonArray layersForFluxName(String fluxName, int offset, int limit) throws IOException;
    void changeColorMapForFluxName(String fluxName, String colorMap) throws IOException;
    void deleteFlux(String fluxName) throws IOException;
    void deleteLayer(String layerName) throws IOException;
}
