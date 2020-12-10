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
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.profiledb.Activity;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;

public class roleTest {

	// ------headers
	@Test
	public void get_dataExist_OKrole_HTTPHEADER() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/nrb404g/data?sourceRequest=test");

		request.addHeader("Authorization", "Bearer " + getAccessTokenADifino());

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(true, result.size() > 5);
	}

	@Test
	public void get_dataExist_OKrole_HTTPHEADER_KO() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/nrb404g/data?sourceRequest=test");

		request.addHeader("Authorization", "Bearer asd" + getAccessTokenADifino());

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));
	}

	// -----------------------------------------------------------------------------activity
	@Test
	public void get_dataExist_OK() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/nrb404g/activity?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Activity> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Activity>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(true, result.size() > 5);
	}

	@Test
	public void get_dataExist_KO() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/0a18f0b11a97ce315d0ad6d479fac4d76d60fd5d7c41a9cb8c3c1d508e450bdd/activity?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the owner of the specified appId"));
	}

	// -----------------------------------------------------------------------------data

	@Test
	public void get_dataExist_OKrole() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/nrb404g/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(true, result.size() > 5);
	}

	@Test
	public void get_dataExist_KOrole() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the owner of the specified appId"));
	}

	@Test
	public void post_dataValid_OKrole() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/apps/nrb404g/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		Data data = new Data();
		data.setDataTime(new Date()); // data.setAppName("nr3"); data.setAppId("f9208f95d7a81d04e7925bb9c7ed388fb9e0c2ce94dab850f55db554cc92c299"); data.setMotivation("TIPO4"); data.setVariableName("LATITUDE");
		data.setVariableValue("50.50");
		data.setVariableUnit("coord");
		request.setEntity(createEntity(data));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void post_dataValid_KOrole() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/apps/f9208f95d7a81d04e7925bb9c7ed388fb9e0c2ce94dab850f55db554cc92c299/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		Data data = new Data();
		data.setDataTime(new Date()); // data.setAppName("nr3"); data.setAppId("f9208f95d7a81d04e7925bb9c7ed388fb9e0c2ce94dab850f55db554cc92c299"); data.setMotivation("TIPO4"); data.setVariableName("LATITUDE");
		data.setVariableValue("50.50");
		data.setVariableUnit("coord");
		request.setEntity(createEntity(data));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the owner of the specified appId"));
	}

	@Test
	public void post_dataValid_username_OKrole() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/angelo.difino/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		Data data = new Data();
		data.setDataTime(new Date());
		data.setUsername("angelo.difino");
		data.setMotivation("TIPO4");
		data.setVariableName("LATITUDE");
		data.setVariableValue("50.50");
		data.setVariableUnit("coord");
		request.setEntity(createEntity(data));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void post_dataValid_username_KOrole() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/badii/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		Data data = new Data();
		data.setDataTime(new Date());
		data.setUsername("badii");
		data.setMotivation("TIPO4");
		data.setVariableName("LATITUDE");
		data.setVariableValue("50.50");
		data.setVariableUnit("coord");
		request.setEntity(createEntity(data));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the same of the specified username"));
	}

	@Test
	public void get_fromUsername_dataExist_OKrole() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/angelo.difino/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(true, result.size() > 5);
	}

	@Test
	public void get_fromUsername_dataExist_KOrole() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/nicola.mitolo/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the same of the specified username"));
	}

	@Test
	public void get_all_data_korole() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user has not enough rights to access this resource"));

	}

	@Test
	public void get_any_data_korole() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/ANY/data?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the same of the specified username"));
	}

	// -----------------------------------------------------------------------------delegation

	@Test
	public void get_delegated_usernameExist2_V1_roleok() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/angelo.difino/delegated?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(2, result.size());
	}

	@Test
	public void get_delegated_usernameExist2_V1_roleko() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the same of the specified username"));
	}

	@Test
	public void get_delegated_usernameExist2_V2_roleok() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/angelo.difino/delegated?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(4, result.size());
	}

	@Test
	public void get_delegated_usernameExist2_V2_roleko() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/adifino/delegated?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the same of the specified username"));
	}

	@Test
	public void get_delegator_usernameExist_fromappid_roleok() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/nrb404g/delegator?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(1, result.size());
	}

	@Test
	public void get_delegator_usernameExist_fromappid_roleko() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegator?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the owner of the specified appId"));
	}

	@Test
	public void check_owner_roleko_username() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/pb1/delegation/check?variableName=latitude_longitude&elementID=nr2gwzc&accessToken="
						+ getAccessTokenADifino()
						+ "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

	}

	@Test
	public void check_owner_roleko() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/pb1/delegation/check?variableName=latitude_longitude&elementID=6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e&accessToken=" + getAccessTokenADifino()
						+ "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the owner of the specified appId"));
	}

	@Test
	public void post_delegationValid2_roleok() throws ClientProtocolException, IOException {

		Long time = System.currentTimeMillis();

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/angelo.difino/delegation?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		Delegation delegation = new Delegation();
		delegation.setUsernameDelegated("badii");
		delegation.setElementId("dash_id" + time);
		delegation.setElementType("DASHID");

		request.setEntity(createEntity(delegation));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void post_delegationValid2_roleko() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/pb1/delegation?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		Delegation delegation = new Delegation();
		delegation.setUsernameDelegated("badii");
		delegation.setElementId("dash_id");
		delegation.setElementType("DASHID");

		request.setEntity(createEntity(delegation));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the same of the specified username"));
	}

	@Test
	public void put_delegationValid1_roleok() throws ClientProtocolException, IOException {

		// Given
		HttpPut request = new HttpPut("http://localhost:8080/datamanager/api/v1/username/angelo.difino/delegation/59?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		Delegation delegation = new Delegation();
		delegation.setId(59l);
		delegation.setUsernameDelegator("angelo.difino");
		delegation.setUsernameDelegated("nicola.mitolo");
		delegation.setElementId("dash_id2");
		delegation.setElementType("DASHID2");

		request.setEntity(createEntity(delegation));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void put_delegationValid1_roleko() throws ClientProtocolException, IOException {

		// Given
		HttpPut request = new HttpPut("http://localhost:8080/datamanager/api/v1/username/pb1/delegation/59?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		Delegation delegation = new Delegation();
		delegation.setId(59l);
		delegation.setUsernameDelegator("PaoloNesi");
		delegation.setUsernameDelegated("nicola.mitolo");
		delegation.setElementId("dash_id2");
		delegation.setElementType("DASHID2");

		request.setEntity(createEntity(delegation));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The logged user is not the same of the specified username"));
	}

	@Test
	public void get_public() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/ANONYMOUS/delegated?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(7, result.size());
	}

	@Test
	public void check_username_owner() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/nrb404g/access/check?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

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
		assertEquals("OWNER", result.getMessage());
	}

	@Test
	public void check_username_notowner() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/cab5c0cbf1585a072488954723e198c1c16f6fe3bb220120ba4a25416e7ed9a3/access/check?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

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
	public void check_username_public() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/dash_id5/access/check?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

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
		assertEquals("PUBLIC", result.getMessage());
	}

	@Test
	public void check_username_delegated() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/dash_id/access/check?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

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
		assertEquals("DELEGATED", result.getMessage());
	}

	@Test
	public void check_username_groupdelegated() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/app-tester3/access/check?accessToken=" + getAccessTokenADifino() + "&sourceRequest=test");

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
		assertEquals("GROUP-DELEGATED", result.getMessage());
	}

	@Test
	public void check_username_notowner_v3() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v3/apps/17055861/access/check?accessToken=" + getAccessTokenADifino() + "&elementType=MyKPI&sourceRequest=test");

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
	public void check_username_public_v3() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v3/apps/17055859/access/check?accessToken=" + getAccessTokenADifino() + "&elementType=MyKPI&sourceRequest=test");

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
	public void check_username_delegated_v3() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v3/apps/17055860/access/check?accessToken=" + getAccessTokenADifino() + "&elementType=MyKPI&sourceRequest=test");

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

	public void check_username_delegated_ko1_v3() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v3/apps/Firenze%3Abroker%3Adevice1/access/check?accessToken=" + getAccessTokenADifino() + "&elementType=IOTID&sourceRequest=test");

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

	public void check_username_delegated_ko2_v3() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v3/apps/17055860/access/check?accessToken=" + getAccessTokenADifino() + "&elementType=IOTID&sourceRequest=test");

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
	public void check_alldelegation_ko() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/delegation&accessToken=" + getAccessTokenADifino()
						+ "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.FORBIDDEN.value()));
	}

	// --------------------------------------------------------------------------------------------------------------------
	private String getAccessTokenADifino() throws IOException {
		return get("accesstoken.finaluser=");
	}

	private String get(String tosearch) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("target/test-classes/application-local-test.properties"));
		} catch (Exception e) {
			br = new BufferedReader(new FileReader("application-local-test.properties"));
		}
		if (br != null) {
			String line;
			while ((line = br.readLine()) != null) {
				Integer index;
				if ((index = line.indexOf(tosearch)) != -1) {
					return line.substring(index + tosearch.length());
				}
			}
		}
		throw new IOException(tosearch + " not found");
	}

	private HttpEntity createEntity(Data data) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mapper.writeValue(baos, data);
		return new ByteArrayEntity(baos.toByteArray());
	}

	private HttpEntity createEntity(Delegation delegation) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mapper.writeValue(baos, delegation);
		return new ByteArrayEntity(baos.toByteArray());
	}

}
