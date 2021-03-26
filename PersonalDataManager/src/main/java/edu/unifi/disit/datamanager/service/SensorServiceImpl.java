/* Data Manager (DM).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
package edu.unifi.disit.datamanager.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SensorServiceImpl implements ISensorService {

	// private static final Logger logger = LogManager.getLogger();

        @Value("${grpsensors.datasource.url}")
	private String baseurl;

    @Override
    public String getSensors(String accessToken, String pageNum, String pageSize, String search, String id) throws IOException {
        String pPageNum = pageNum != null ? pageNum : "1";
        String pPageSize = pageSize != null ? pageSize : "10";
        String pSearch = search != null ? search : "";
        String pId = id != null ? id : "0";
		URL url = new URL(baseurl + "?accessToken=" + URLEncoder.encode(accessToken, "UTF-8") + "&pageNum=" + URLEncoder.encode(pPageNum, "UTF-8") + "&pageSize=" + URLEncoder.encode(pPageSize, "UTF-8") + "&search=" + URLEncoder.encode(pSearch, "UTF-8") + "&id=" + URLEncoder.encode(pId, "UTF-8"));
        URLConnection c = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        String response = "";
        String inputLine;
		while ((inputLine = in.readLine()) != null)
			response += inputLine;
		in.close();
		return response;
    }

}