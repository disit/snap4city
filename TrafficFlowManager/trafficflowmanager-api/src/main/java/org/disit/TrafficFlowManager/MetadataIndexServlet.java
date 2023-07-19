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

package org.disit.TrafficFlowManager;

import org.disit.TrafficFlowManager.persistence.JSONReconstructionPersistence;
import org.disit.TrafficFlowManager.persistence.ReconstructionPersistenceInterface;

import javax.json.JsonArray;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet(name = "MetadataIndexServlet", value = "/api/metadata")
public class MetadataIndexServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");

        String fluxName = request.getParameter("fluxName");
        String sOffset = request.getParameter("offset");
        int offset = 0;
        if(sOffset!=null) {
          try {
            offset = Integer.parseInt(sOffset);
          } catch(NumberFormatException e) {
            //ignore
          }          
        }
        String sLimit = request.getParameter("limit");
        int limit = 500;
        if(sLimit!=null) {
          try {
            limit = Integer.parseInt(sLimit);
          } catch(NumberFormatException e) {
            //ignore
          }          
        }

        JsonArray result;
        ReconstructionPersistenceInterface db = new JSONReconstructionPersistence();
        if (fluxName == null) {
            result = db.allLayersClustered();
        } else {
            result = db.layersForFluxName(fluxName, offset, limit);
        }
        response.getWriter().write(result.toString());
        response.getWriter().close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.addHeader("Access-Control-Allow-Origin", "*");
        String action = req.getParameter("action");

        ReconstructionPersistenceInterface db = new JSONReconstructionPersistence();

        switch (action) {
            case "change_color_map": {
                String fluxName = req.getParameter("id");
                String newColorMap = req.getParameter("valore");
                db.changeColorMapForFluxName(fluxName, newColorMap);
                resp.setStatus(HttpServletResponse.SC_OK);
                break;
            }
            case "delete_metadata": {
                String fluxName = req.getParameter("id");
                db.deleteFlux(fluxName);
                resp.setStatus(HttpServletResponse.SC_OK);
                break;
            }
            case "delete_data":
                String layerName = req.getParameter("id");
                db.deleteLayer(layerName);
                resp.setStatus(HttpServletResponse.SC_OK);
                break;
            default:
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }

        resp.getWriter().close();
    }
}
