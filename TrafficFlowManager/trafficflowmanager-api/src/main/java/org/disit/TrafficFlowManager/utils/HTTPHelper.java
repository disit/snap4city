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

package org.disit.TrafficFlowManager.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class HTTPHelper {

    public static Integer uploadFile(String urlString, String contentType, String user, String pass, String fileToUpload) throws IOException {
        return doRequest(urlString, "PUT", contentType, user, pass, fileToUpload, true);
    }

    public static Integer uploadData(String urlString, String contentType, String user, String pass, String data) throws IOException {
        return doRequest(urlString, "PUT", contentType, user, pass, data, false);
    }

    private static Integer doRequest(String urlString, String method, String contentType, String user,
                                     String pass, String data, Boolean isFileUpload) throws IOException {

        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod(method);

        // Headers
        con.setRequestProperty("Content-type", contentType);
        con.setRequestProperty("Accept", "application/json");

        // Authentication
        String userPass = user + ":" + pass;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userPass.getBytes()));
        con.setRequestProperty("Authorization", basicAuth);

        // File or data upload
        if (data != null) {
            con.setDoOutput(true);
            con.setDoInput(true);
            if (isFileUpload) {
                File file = new File(data);
                BufferedOutputStream bos = new BufferedOutputStream(con.getOutputStream());
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                int i;
                while ((i = bis.read()) > -1) {
                    bos.write(i);
                }
                bis.close();
                bos.flush();
                bos.close();
            } else {
                OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
                wr.write(data);
                wr.flush();
                wr.close();
            }
        }

        int responseCode = con.getResponseCode();

        // Print response
        Reader streamReader;
        if (responseCode > 299) {
            streamReader = new InputStreamReader(con.getErrorStream());
        } else {
            streamReader = new InputStreamReader(con.getInputStream());
        }
        BufferedReader in = new BufferedReader(streamReader);
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        if (!content.toString().isEmpty())
            Logger.log(content.toString());

        con.disconnect();
        return responseCode;
    }
}
