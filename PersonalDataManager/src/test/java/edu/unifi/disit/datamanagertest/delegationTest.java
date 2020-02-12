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
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
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
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;

public class delegationTest {

	@Test
	public void get_delegated_usernameNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/prova/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));

		// TODO return error if username not exist
	}

	@Test
	public void get_delegated_usernameExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/snap4city/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_public() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/ANONYMOUS/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_public_groupnamefilter() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(// cn=Firenze Servizi,ou=Firenze,dc=foo,dc=example,dc=org
				"http://localhost:8080/datamanager/api/v1/username/ANONYMOUS/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test&groupname=cn%3DFirenze+Servizi%2Cou%3DFirenze%2Cdc%3Dfoo%2Cdc%3Dexample%2Cdc%3Dorg");

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
	public void get_public_groupnamefilter_ou() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(// ou=Helsinki,dc=foo,dc=example,dc=org
				"http://localhost:8080/datamanager/api/v1/username/ANONYMOUS/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test&groupname=ou%3DHelsinki%2Cdc%3Dfoo%2Cdc%3Dexample%2Cdc%3Dorg");

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
	public void get_delegated_usernameExist2_V1() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegated_usernameExist2_V1_elementType_ok1() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test&elementType=DASHID");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_delegated_usernameExist2_V1_elementType_ok2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test&elementType=AppID");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegated_usernameExist2_V2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/adifino/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegated_usernameExistFiltrovariable() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?variableName=latitude_longitude&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_delegated_usernameExistFiltromotivation() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?motivation=Shared%20Position&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_delegated_usernameExistFiltromotivation_deleted() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?motivation=Shared%20Position&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test&deleted=true");

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
	public void get_delegated_usernameExistFiltromotivationVar() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?motivation=altro&variableName=altro&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_delegator_usernameNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/prova/delegator?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_delegator_usernameExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/snap4city/delegator?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_delegator_usernameExist2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegator?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(16, result.size());
	}

	@Test
	public void get_delegator_usernameExistFiltrovariable() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegator?variableName=latitude_longitude&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(5, result.size());
	}

	@Test
	public void get_delegator_usernameExistFiltrovariable_deleted() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegator?variableName=latitude_longitude&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test&deleted=true");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(5, result.size());
	}

	@Test
	public void get_delegator_usernameExistFiltromotivation() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegator?motivation=Shared%20Position&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegator_usernameExistFiltromotivationVar() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/pb1/delegator?motivation=altro&variableName=altro&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_delegated_usernameNotExist_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/nry9x99/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_delegated_usernameExist_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/244a29787d16e7ba720163890c87a76e05dfccfac835cc0fd2700ddf0480f137/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_delegated_usernameExist2_fromappidV1() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegated_usernameExist2_fromappidV2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegated_usernameExistFiltrovariable_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegated?variableName=latitude_longitude&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_delegated_usernameExistFiltromotivation_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegated?motivation=Shared%20Position&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_delegated_usernameExistFiltromotivationVar_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegated?motivation=altro&variableName=altro&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_delegator_usernameNotExist_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/prova/delegator?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_delegator_usernameExist_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegator?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegator_usernameExist2_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegator?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegator_usernameExistFiltrovariable_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegator?variableName=latitude_longitude&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegator_usernameExistFiltromotivation_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegator?motivation=Shared%20Position&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}

	@Test
	public void get_delegator_usernameExistFiltromotivationVar_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/cab5c0cbf1585a072488954723e198c1c16f6fe3bb220120ba4a25416e7ed9a3/delegator?motivation=altro&variableName=altro&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void check_not() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/adifino/delegation/check?variableName=latitude_longitude&elementID=244a29787d16e7ba720163890c87a76e05dfccfac835cc0fd2700ddf0480f137&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void check_notIgnoreCase() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/ADIFINO/delegation/check?variableName=latitude_longitude&elementID=244a29787d16e7ba720163890c87a76e05dfccfac835cc0fd2700ddf0480f137&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void check_not2_varname() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/adifino/delegation/check?variableName=latitude_longitude2&elementID=cab5c0cbf1585a072488954723e198c1c16f6fe3bb220120ba4a25416e7ed9a3&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void check_owner() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/adifino/delegation/check?variableName=latitude_longitude&elementID=6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void check_public() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/pb1/delegation/check?variableName=latitude_longitude&elementID=6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void check_delegation() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/adifino/delegation/check?variableName=latitude_longitude&elementID=cab5c0cbf1585a072488954723e198c1c16f6fe3bb220120ba4a25416e7ed9a3&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void post_delegationNotValid_uid() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/adifino/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation d = new Delegation();
		d.setId(12l);

		request.setEntity(createEntity(d));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION has the ID field NOT null"));
	}

	@Test
	public void post_delegationNotValid_notrecognized() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/prova/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation d = new Delegation();

		request.setEntity(createEntity(d));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION has the DELEGATED username/groupname not recognized"));
	}

	@Test
	public void post_delegationNotValid_username() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/adifino/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation d = new Delegation();
		d.setUsernameDelegator("prova");

		request.setEntity(createEntity(d));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION has the USERNAME field different from the one specified in the called API"));
	}

	@Test
	public void post_delegationNotValid_usernameDelegated() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/adifino/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation d = new Delegation();
		d.setUsernameDelegator("adifino");
		d.setUsernameDelegated("prova");

		request.setEntity(createEntity(d));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION has the DELEGATED username/groupname not recognized"));
	}

	@Test
	public void post_delegationValid1() throws ClientProtocolException, IOException {

		Long time = System.currentTimeMillis();

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/pb1/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation delegation = new Delegation();
		delegation.setUsernameDelegator("pb1");
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

		// do it twice.... next one has to fail

		// When
		HttpResponse httpResponse2 = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse2.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse2.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION is already present"));
	}

	@Test
	public void post_delegationValid2() throws ClientProtocolException, IOException {

		Long time = System.currentTimeMillis();

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/pb1/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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

		// do it twice.... next one has to fail

		// When
		HttpResponse httpResponse2 = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse2.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse2.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION is already present"));

	}

	@Test
	public void post_delegationValid_details() throws ClientProtocolException, IOException {

		Long time = System.currentTimeMillis();

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/pb1/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation delegation = new Delegation();
		delegation.setUsernameDelegated("badii");
		delegation.setElementId("dash_id" + time);
		delegation.setElementType("DASHID");
		delegation.setDelegationDetails("{\"k1\":\"1c1a98a0-4f13-47aa-930d-25302edebfcd\",\"k2\":\"19973b84-f6d4-4e28-b04a-6608aad39c4b\"}");

		request.setEntity(createEntity(delegation));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		// do it twice.... next one has to fail

		// When
		HttpResponse httpResponse2 = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse2.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse2.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION is already present"));
	}

	@Test
	public void delete_delegation_fromapp_ko_v1() throws ClientProtocolException, IOException {

		// Given
		HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v1/apps/prova/delegations?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void delete_delegation_fromapp_ko_v3() throws ClientProtocolException, IOException {

		// Given
		HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v3/apps/prova/delegations?elementType=IOTID&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void delete_delegationNotValid_notrecognized() throws ClientProtocolException, IOException {

		// Given
		HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/prova/delegation/40?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION has the ID not recognized"));
	}

	@Test
	public void delete_delegationNotValid_idnotrecognized() throws ClientProtocolException, IOException {

		// Given
		HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/prova/delegation/230000?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION has the ID not recognized"));
	}

	// @Test
	// public void delete_delegationNotValid_notowner() throws ClientProtocolException, IOException {
	//
	// // Given
	// HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/adifino/delegation/47?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");
	//
	// // When
	// HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
	//
	// // Then
	// assertThat(
	// httpResponse.getStatusLine().getStatusCode(),
	// equalTo(HttpStatus.BAD_REQUEST.value()));
	//
	// String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
	//
	// assertThat(
	// entityMsg,
	// startsWith("You're not owner of the delgation to DELETE"));
	// }

	// @Test
	// public void delete_delegation() throws ClientProtocolException, IOException {
	//
	// // Given
	// HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/pb1/delegation/47?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");
	//
	// // When
	// HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
	//
	// // Then
	// assertThat(
	// httpResponse.getStatusLine().getStatusCode(),
	// equalTo(HttpStatus.OK.value()));
	// }

	@Test
	public void put_delegationValid1() throws ClientProtocolException, IOException {

		// Given
		HttpPut request = new HttpPut("http://localhost:8080/datamanager/api/v1/username/pb1/delegation/59?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
				equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void get_group_delegated_tester1_v2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/tester1/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
		assertEquals("tester1", result.get(0).getUsernameDelegator());
		assertEquals("tester4", result.get(1).getUsernameDelegator());
		assertEquals("tester7", result.get(2).getUsernameDelegator());
	}

	@Test
	public void get_group_delegated_tester2_v1() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester2/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("tester1", result.get(0).getUsernameDelegator());
	}

	@Test
	public void get_group_delegated_tester2_v2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/tester2/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
		assertEquals("tester1", result.get(0).getUsernameDelegator());
		assertEquals("tester1", result.get(1).getUsernameDelegator());
		assertEquals("tester7", result.get(2).getUsernameDelegator());
	}

	@Test
	public void get_group_delegated_tester3_v2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/tester3/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Delegation>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
		assertEquals("tester1", result.get(0).getUsernameDelegator());
		assertEquals("tester4", result.get(1).getUsernameDelegator());
		assertEquals("tester7", result.get(2).getUsernameDelegator());
	}

	@Test
	public void get_group_delegated_tester4_v2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/tester4/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("tester7", result.get(0).getUsernameDelegator());
	}

	@Test
	public void get_group_delegated_tester5_v2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/tester5/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("tester4", result.get(0).getUsernameDelegator());
		assertEquals("tester7", result.get(1).getUsernameDelegator());
	}

	@Test
	public void get_group_delegated_tester6_v2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/tester6/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("tester2", result.get(0).getUsernameDelegator());
	}

	@Test
	public void get_group_delegated_tester7_v2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v2/username/tester7/delegated?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("tester2", result.get(0).getUsernameDelegator());
		assertEquals("tester3", result.get(1).getUsernameDelegator());
	}

	@Test
	public void post_group_delegationNotValid_usernameDelegated() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/adifino/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation d = new Delegation();
		d.setUsernameDelegator("adifino");
		d.setGroupnameDelegated("prova");

		request.setEntity(createEntity(d));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION has the DELEGATED username/groupname not recognized"));
	}

	@Test
	public void post_group_delegationNotExist_usernameDelegated() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/adifino/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation d = new Delegation();
		d.setUsernameDelegator("adifino");
		d.setGroupnameDelegated("cn=pippo,ou=pluto,dc=foo,dc=example,dc=org");

		request.setEntity(createEntity(d));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION has the DELEGATED username/groupname not recognized"));
	}

	@Test
	public void post_and_delete_group_delegationValid1() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/pb1/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation delegation = new Delegation();
		delegation.setUsernameDelegator("pb1");
		delegation.setGroupnameDelegated("cn=Firenze Musica,ou=Firenze,dc=foo,dc=example,dc=org");
		delegation.setElementId("dash_id");
		delegation.setElementType("DASHID");

		request.setEntity(createEntity(delegation));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Delegation result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<Delegation>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals("pb1", result.getUsernameDelegator());
		assertEquals("cn=Firenze Musica,ou=Firenze,dc=foo,dc=example,dc=org", result.getGroupnameDelegated());

		// Given
		HttpDelete requestD = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/pb1/delegation/" + result.getId() + "?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponseD = HttpClientBuilder.create().build().execute(requestD);

		// Then
		assertThat(
				httpResponseD.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

	}

	@Test
	public void post_and_delete_group_delegationValid1_ou() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/pb1/delegation?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Delegation delegation = new Delegation();
		delegation.setUsernameDelegator("pb1");
		delegation.setGroupnameDelegated("ou=Firenze,dc=foo,dc=example,dc=org");
		delegation.setElementId("dash_id");
		delegation.setElementType("DASHID");

		request.setEntity(createEntity(delegation));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Delegation result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<Delegation>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals("pb1", result.getUsernameDelegator());
		assertEquals("ou=Firenze,dc=foo,dc=example,dc=org", result.getGroupnameDelegated());

		// Given
		HttpDelete requestD = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/pb1/delegation/" + result.getId() + "?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponseD = HttpClientBuilder.create().build().execute(requestD);

		// Then
		assertThat(
				httpResponseD.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

	}

	@Test
	public void check_username_root() throws ClientProtocolException, IOException {
		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/access/check?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("ROOTADMIN", result.getMessage());
	}

	@Test
	public void check_not_v3() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v3/username/adifino/delegation/check?elementID=17055860&elementType=MyKPI&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void check_notIgnoreCase_V3() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v3/username/ADIFINO/delegation/check?elementID=17055860&elementType=MyKPI&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void check_owner_v3() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v3/username/adifino/delegation/check?elementID=6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e&elementType=AppID&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void check_public_v3() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v3/username/pb1/delegation/check?elementID=17055859&elementType=MyKPI&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void check_delegation_v3() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v3/username/angelokpi/delegation/check?elementType=IOTID&elementID=Firenze%3Abroker%3Adevice1&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
		assertEquals("MYGROUP-DELEGATED", result.getMessage());
	}

	private HttpEntity createEntity(Delegation delegation) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mapper.writeValue(baos, delegation);
		return new ByteArrayEntity(baos.toByteArray());
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
