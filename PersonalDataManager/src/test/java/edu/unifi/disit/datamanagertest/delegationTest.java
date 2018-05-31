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
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/prova/delegated?accessToken=FAKE");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_delegated_usernameExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/snap4city/delegated?accessToken=FAKE");

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
	public void get_delegated_usernameExist2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?variableName=latitude_longitude&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?motivation=Shared%20Position&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegated?motivation=altro&variableName=altro&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/prova/delegator?accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/snap4city/delegator?accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegator?accessToken=FAKE");

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
	public void get_delegator_usernameExistFiltrovariable() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegator?variableName=latitude_longitude&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/delegator?motivation=Shared%20Position&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/pb1/delegator?motivation=altro&variableName=altro&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/prova/delegated?accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/244a29787d16e7ba720163890c87a76e05dfccfac835cc0fd2700ddf0480f137/delegated?accessToken=FAKE");

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
	public void get_delegated_usernameExist2_fromappid() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegated?accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegated?variableName=latitude_longitude&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegated?motivation=Shared%20Position&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegated?motivation=altro&variableName=altro&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/prova/delegator?accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegator?accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegator?accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegator?variableName=latitude_longitude&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/delegator?motivation=Shared%20Position&accessToken=FAKE");

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
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/cab5c0cbf1585a072488954723e198c1c16f6fe3bb220120ba4a25416e7ed9a3/delegator?motivation=altro&variableName=altro&accessToken=FAKE");

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
				"http://localhost:8080/datamanager/api/v1/username/adifino/delegation/check?variableName=latitude_longitude&elementID=244a29787d16e7ba720163890c87a76e05dfccfac835cc0fd2700ddf0480f137&accessToken=FAKE");

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
				"http://localhost:8080/datamanager/api/v1/username/adifino/delegation/check?variableName=latitude_longitude2&elementID=cab5c0cbf1585a072488954723e198c1c16f6fe3bb220120ba4a25416e7ed9a3&accessToken=FAKE");

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
				"http://localhost:8080/datamanager/api/v1/username/adifino/delegation/check?variableName=latitude_longitude&elementID=6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e&accessToken=FAKE");

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
	public void check_delegation() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/adifino/delegation/check?variableName=latitude_longitude&elementID=cab5c0cbf1585a072488954723e198c1c16f6fe3bb220120ba4a25416e7ed9a3&accessToken=FAKE");

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
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/adifino/delegation?accessToken=FAKE");

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
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/prova/delegation?accessToken=FAKE");

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
				startsWith("The passed DELEGATION has the USERNAME not recognized"));
	}

	@Test
	public void post_delegationNotValid_username() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/adifino/delegation?accessToken=FAKE");

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
	public void post_delegationValid1() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/pb1/delegation?accessToken=FAKE");

		Delegation delegation = new Delegation();
		delegation.setUsernameDelegator("pb1");
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
				equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void post_delegationValid2() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/pb1/delegation?accessToken=FAKE");

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
				equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void delete_delegationNotValid_notrecognized() throws ClientProtocolException, IOException {

		// Given
		HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/prova/delegation/40?accessToken=FAKE");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DELEGATION has the USERNAME not recognized"));
	}

	@Test
	public void delete_delegationNotValid_idnotrecognized() throws ClientProtocolException, IOException {

		// Given
		HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/prova/delegation/230000?accessToken=FAKE");

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
	// HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/adifino/delegation/47");
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
	// HttpDelete request = new HttpDelete("http://localhost:8080/datamanager/api/v1/username/pb1/delegation/47");
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
		HttpPut request = new HttpPut("http://localhost:8080/datamanager/api/v1/username/pb1/delegation/59?accessToken=FAKE");

		Delegation delegation = new Delegation();
		delegation.setId(59l);
		delegation.setUsernameDelegator("adifino");
		delegation.setUsernameDelegated("badii");
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

	private HttpEntity createEntity(Delegation delegation) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mapper.writeValue(baos, delegation);
		return new ByteArrayEntity(baos.toByteArray());
	}

}