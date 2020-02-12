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
package edu.unifi.disit.datamanagertest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.HashMap;
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

import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;

public class publicTest {

	@Test
	public void check_publicOK1_from_external() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/public/access/check?elementID=6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e&elementType=AppID&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Response result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<Response>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(true, result.getResult());
	}

	@Test
	public void check_publicOK2_from_external() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/access/check?elementID=6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e&elementType=AppID&variableName=latitude_longitude&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Response result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<Response>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(true, result.getResult());
	}

	@Test
	public void check_publicKO_from_external() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/public/access/check?elementID=dash_id&elementType=DASHID&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Response result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<Response>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(false, result.getResult());
	}

	@Test
	public void check_publicKO2_from_external() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/access/check?elementID=6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e&elementType=AppID&variableName=altro&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Response result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<Response>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(false, result.getResult());
	}

	@Test
	public void get_dataExist_public_complexxo() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?variableName=latitude_longitude&motivation=Shared%20Position&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(5, result.size());
	}

	@Test
	public void get_dataDelegation_OK_general_noresult_public() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(10, result.size());
	}

	@Test
	public void get_dataDelegation_OK_general_result_public() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?variableName=latitude_longitude&motivation=Shared%20Position&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(5, result.size());
	}

	@Test
	public void get_dataDelegation_OK_general_lat_mot_public() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?variableName=latitude_longitude&motivation=Shared%20Position&last=1&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(1, result.size());
		// assertEquals("adifino", result.get(0).getUsername());
		assertEquals(1525186380000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_dataDelegation_OK_general_lat_public() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?variableName=latitude_longitude&motivation=Shared%20Position&last=2&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(2, result.size());
		// assertEquals("adifino", result.get(0).getUsername());
		assertEquals(1525186380000l, result.get(0).getDataTime().getTime());
		// assertEquals("adifino", result.get(1).getUsername());
		assertEquals(1525186352000l, result.get(1).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataExist_anonymous_complexxo() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?variableName=latitude_longitude&motivation=Shared%20Position&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(5, result.size());
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_noresult_anonymous() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(10, result.size());
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_result_anonymous() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?variableName=latitude_longitude&motivation=Shared%20Position&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(5, result.size());
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_lat_mot_anonymous() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?variableName=latitude_longitude&motivation=Shared%20Position&last=1&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(1, result.size());
		// assertEquals("adifino", result.get(0).getUsername());
		assertEquals(1525186380000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_lat_anonymous() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/data?variableName=latitude_longitude&motivation=Shared%20Position&last=2&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(2, result.size());
		// assertEquals("adifino", result.get(0).getUsername());
		assertEquals(1525186380000l, result.get(0).getDataTime().getTime());
		// assertEquals("adifino", result.get(1).getUsername());
		assertEquals(1525186352000l, result.get(1).getDataTime().getTime());
	}

	@Test
	public void config_ok() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/configuration/v1?sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, String> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<HashMap<String, String>>() {
		});

		HashMap<String, String> totest = new HashMap<String, String>();
		totest.put("Authentication.url", "https://www.disit.org/auth/");
		totest.put("grp.Authentication.clientId", "js-grp-client-test");
		totest.put("kpi.Authentication.clientId", "js-kpi-client-test");

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(totest, result);
	}

	@Test
	public void config_ko() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/configuration/v2?sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));

	}

	@Test
	public void public_mygroups_ok1() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/access/check?elementID=17055859&elementType=MyKPI&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Response result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<Response>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(true, result.getResult());
	}

	@Test
	public void public_mygroups_ko1() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/access/check?elementID=17055860&elementType=MyKPI&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Response result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<Response>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(false, result.getResult());
	}

	@Test
	public void public_mygroups_ko2() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/public/access/check?elementID=Firenze%3Abroker%3Adevice1&elementType=IOTID&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Response result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<Response>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(false, result.getResult());
	}

	// private HttpEntity createEntity(Delegation delegation) throws JsonGenerationException, JsonMappingException, IOException {
	// ObjectMapper mapper = new ObjectMapper();
	// ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// mapper.writeValue(baos, delegation);
	// return new ByteArrayEntity(baos.toByteArray());
	// }

}