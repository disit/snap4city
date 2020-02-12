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
package edu.unifi.disit.datamanager.controller.rest;

import edu.unifi.disit.datamanager.service.ISensorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

@RestController
public class SensorController {
    
        @Value("${grpsensors.datasource.url}")
	private String baseurl;
        
        @Autowired
	ISensorService sensorService;

        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/api/v1/sensors")
        public String getSensors(
                @RequestParam("accessToken") String accessToken,
                @RequestParam(value = "pageNum", required = false, defaultValue = "1") String pageNum,
                @RequestParam(value = "pageSize", required = false, defaultValue = "10") String pageSize,
                @RequestParam(value = "search", required = false, defaultValue = "") String search,
                @RequestParam(value = "id", required = false, defaultValue = "") String id) throws IOException {          
            
            return sensorService.getSensors(accessToken, pageNum, pageSize, search, id);
            
        }

}