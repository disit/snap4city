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
package edu.unifi.disit.snapengagertest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.snapengager.datamodel.profiledb.Engagement;

public class engagementTest {

	@Test
	public void get_engagement_empty() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8082/snapengager/api/v1/engagements?accessToken=" + getAccessTokenRoot());

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Engagement> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Engagement>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(0, result.size());
	}

	@Test
	public void get_engagement_ok() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8082/snapengager/api/v1/engagements?accessToken=" + getAccessTokenRoot());

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Engagement> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Engagement>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(0, result.size());
	}

	private String getAccessTokenRoot() throws IOException {
		return get("accesstoken.rootuser=");
	}

	@SuppressWarnings("resource")
	private String get(String tosearch) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("application-local-test.properties"));
		String line;
		while ((line = br.readLine()) != null) {
			Integer index;
			if ((index = line.indexOf(tosearch)) != -1) {
				return line.substring(index + tosearch.length());
			}
		}
		throw new IOException(tosearch + " not found");
	}
}
