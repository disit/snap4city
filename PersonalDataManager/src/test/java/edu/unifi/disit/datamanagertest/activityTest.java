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
package edu.unifi.disit.datamanagertest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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

import edu.unifi.disit.datamanager.datamodel.profiledb.Consent;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;

public class activityTest {

	@Test
	public void get_activityNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/prova/activity?accessToken=FAKE");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_dataExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/0a18f0b11a97ce315d0ad6d479fac4d76d60fd5d7c41a9cb8c3c1d508e450bdd/activity?accessToken=FAKE");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Consent>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(8, result.size());
	}

	// @Test
	// public void get_dataExistDelegated() throws ClientProtocolException, IOException {
	//
	// // Given
	// HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/activity?delegated=true");
	//
	// // When
	// HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
	//
	// // Then
	// ObjectMapper mapper = new ObjectMapper();
	// List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Consent>>() {
	// });
	//
	// assertThat(
	// httpResponse.getStatusLine().getStatusCode(),
	// equalTo(HttpStatus.OK.value()));
	//
	// assertEquals(117, result.size());
	// }
}
