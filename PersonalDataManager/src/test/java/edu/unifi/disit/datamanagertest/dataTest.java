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

import edu.unifi.disit.datamanager.datamodel.profiledb.Data;

public class dataTest {

	@Test
	public void get_dataNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/prova/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));

		// TODO : return error if the username is not found
		// assertThat(
		// httpResponse.getStatusLine().getStatusCode(),
		// equalTo(HttpStatus.BAD_REQUEST.value()));
		//
		// String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
		//
		// assertThat(
		// entityMsg,
		// startsWith("The passed DATA has the APP_ID field not recognized"));
	}

	@Test
	public void get_dataExist_kolastandfirst() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/data?last=10&first=4&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed request include a LAST and FIRST parameters together"));
	}

	@Test
	public void get_dataExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(46, result.size());
	}

	@Test
	public void get_dataExist_last() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?last=1&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals(1526467394000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_dataExist_last5() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?last=5&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals(1526467394000l, result.get(0).getDataTime().getTime());
		assertEquals(1525186291000l, result.get(4).getDataTime().getTime());
	}

	@Test
	public void get_dataExist_first() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?first=1&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals(1523529262000l, result.get(0).getDataTime().getTime());
	}

	// @Test
	// public void get_dataExist_first5() throws ClientProtocolException, IOException {
	//
	// // Given
	// HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?first=&accessToken=" + getAccessTokenRoot() + "5&sourceRequest=test");
	//
	// // When
	// HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
	//
	// // Then
	// ObjectMapper mapper = new ObjectMapper();
	// List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
	// });
	//
	// assertThat(
	// httpResponse.getStatusLine().getStatusCode(),
	// equalTo(HttpStatus.OK.value()));
	//
	// assertEquals(5, result.size());
	// assertEquals(1523529262000l, result.get(0).getDataTime().getTime());
	// assertEquals(1525087114000l, result.get(4).getDataTime().getTime());
	//
	// }

	@Test
	public void get_dataExist_from() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?from=2018-04-13T11:28&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(9, result.size());
	}

	@Test
	public void get_dataExist_to() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?to=2018-04-21T11:28&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_dataExist_fromto() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?from=2018-03-15T11:28&to=2018-04-21T11:28&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_dataExist_fromto_last() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?from=2018-03-15T11:28&to=2018-05-01T11:28&last=1&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
		assertEquals(1525087114000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_dataDelegation_OK_general_noresult() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/cab5c0cbf1585a072488954723e198c1c16f6fe3bb220120ba4a25416e7ed9a3/data?delegated=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_dataDelegation_OK_general_result() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(30, result.size());
	}

	@Test
	public void get_dataDelegation_OK_general_lat_mot() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&last=1&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("pb1", result.get(0).getUsername());
		assertEquals(1524231131000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_dataDelegation_OK_general_lat() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/244a29787d16e7ba720163890c87a76e05dfccfac835cc0fd2700ddf0480f137/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&last=1&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("badii", result.get(0).getUsername());
		assertEquals(1525007183000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_dataExist_anonymous_complexxo() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&anonymous=true&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_dataDelegation_OK_general_noresult_anonymous() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/cab5c0cbf1585a072488954723e198c1c16f6fe3bb220120ba4a25416e7ed9a3/data?delegated=true&anonymous=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_dataDelegation_OK_general_result_anonymous() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&anonymous=true&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_dataDelegation_OK_general_lat_mot_anonymous() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&last=1&anonymous=true&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_dataDelegation_OK_general_lat_anonymous() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/244a29787d16e7ba720163890c87a76e05dfccfac835cc0fd2700ddf0480f137/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&last=2&anonymous=true&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=test");

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
	public void get_dataExist_anonymous_complexxo_annotation() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/data?delegated=true&variableName=latitude_longitude&motivation=Annotation&anonymous=true&last=1&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_dataExist_complexxo_annotation() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/apps/d2799619ee16ae9ef5496859cb583eeec87a383af5d2ed51fc4cb1ca60b22f31/data?delegated=true&variableName=latitude_longitude&motivation=Annotation&last=1&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("adifino", result.get(0).getUsername());
		assertEquals(1526467394000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void post_dataNotValid_containerid() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/apps/f52b8e936ba2c0a23a6c059055546f1011d19df1305667aa25300b732a09f9eb/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Data data = new Data();
		// data.setUid("66");
		data.setDataTime(new Date());
		// data.setAppName("nr3");
		data.setAppId("2bef2df9920c6334074a8081852896f9047184119592205a14ecf808ff2ec93a");
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
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DATA has the APP_ID field different from the one specified in the called API"));
	}

	@Test
	public void post_dataNotValid_uid() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/apps/2bef2df9920c6334074a8081852896f9047184119592205a14ecf808ff2ec93a/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Data data = new Data();
		data.setDataTime(new Date());
		data.setAppId("2bef2df9920c6334074a8081852896f9047184119592205a14ecf808ff2ec93a");
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
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DATA has the APP_ID field not recognized"));
	}

	@Test
	public void post_dataValid() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/apps/f9208f95d7a81d04e7925bb9c7ed388fb9e0c2ce94dab850f55db554cc92c299/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Data data = new Data();
		data.setDataTime(new Date());
		// data.setAppName("nr3");
		data.setAppId("f9208f95d7a81d04e7925bb9c7ed388fb9e0c2ce94dab850f55db554cc92c299");
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
				equalTo(HttpStatus.OK.value()));
	}

	private HttpEntity createEntity(Data data) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mapper.writeValue(baos, data);
		return new ByteArrayEntity(baos.toByteArray());
	}

	@Test
	public void post_dataNotValid_containerid_username() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/prova/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Data data = new Data();
		data.setUsername("prova");
		request.setEntity(createEntity(data));
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
				startsWith("The passed DATA has the USERNAME not recognized"));
	}

	@Test
	public void post_dataNotValid_uid_username() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/adifino/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		Data data = new Data();
		data.setUsername("prova");
		data.setDataTime(new Date());
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
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed DATA has the USERNAME field different from the one specified in the called API"));
	}

	@Test
	public void post_dataValid_username() throws ClientProtocolException, IOException {

		// Given
		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/username/badii/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
				equalTo(HttpStatus.OK.value()));
	}

	@Test
	public void get_fromUsername_dataNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/prova/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_fromUsername_dataExist_kolastandfirst() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/data?last=10&first=4&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.BAD_REQUEST.value()));

		String entityMsg = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

		assertThat(
				entityMsg,
				startsWith("The passed request include a LAST and FIRST parameters together"));
	}

	@Test
	public void get_fromUsername_dataExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/nicola.mitolo/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(46, result.size());
	}

	// ----------------------------------------------------------------------------------------------------
	@Test
	public void get_fromUsername_dataExist_last() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/data?last=1&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals(1526467394000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataExist_last5() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/data?last=5&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals(1526467394000l, result.get(0).getDataTime().getTime());
		assertEquals(1525186291000l, result.get(4).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataExist_first() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/data?first=1&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals(1523529262000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataExist_first5() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/data?first=5&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals(1523529262000l, result.get(0).getDataTime().getTime());
		assertEquals(1525087114000l, result.get(4).getDataTime().getTime());

	}

	@Test
	public void get_fromUsername_dataExist_from() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/data?from=2018-04-13T11:28&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(9, result.size());
	}

	@Test
	public void get_fromUsername_dataExist_to() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/data?to=2018-04-21T11:28&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_fromto() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/data?from=2018-03-15T11:28&to=2018-04-21T11:28&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_fromto_last() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/adifino/data?from=2018-03-15T11:28&to=2018-05-01T11:28&last=1&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals(1525087114000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_noresult() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/pb1/data?delegated=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_result() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/nicola%2Emitolo/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(30, result.size());
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_lat_mot() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/adifino/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&last=1&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("pb1", result.get(0).getUsername());
		assertEquals(1524231131000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_lat() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/PaoloNesi/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&last=1&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("badii", result.get(0).getUsername());
		assertEquals(1525007183000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataExist_anonymous_complexxo() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/nicola%2Emitolo/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&anonymous=true&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
				"http://localhost:8080/datamanager/api/v1/username/pb1/data?delegated=true&anonymous=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
				"http://localhost:8080/datamanager/api/v1/username/nicola%2Emitolo/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&anonymous=true&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
				"http://localhost:8080/datamanager/api/v1/username/adifino/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&last=1&anonymous=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
				"http://localhost:8080/datamanager/api/v1/username/PaoloNesi/data?delegated=true&variableName=latitude_longitude&motivation=Shared%20Position&last=2&anonymous=true&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	public void get_fromUsername_dataExist_anonymous_complexxo_annotation() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/nicola%2Emitolo/data?delegated=true&variableName=latitude_longitude&motivation=Annotation&anonymous=true&last=1&accessToken=" + getAccessTokenRoot()
						+ "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_complexxo_annotation() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/username/nicola%2Emitolo/data?delegated=true&variableName=latitude_longitude&motivation=Annotation&last=1&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
		assertEquals("adifino", result.get(0).getUsername());
		assertEquals(1526467394000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_all_data() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		// ObjectMapper mapper = new ObjectMapper();
		// List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		// });

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

	}

	@Test
	public void get_all_data_last() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test&last=true");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		// ObjectMapper mapper = new ObjectMapper();
		// List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		// });

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

	}

	@Test
	public void get_fromUsername_dataExist_group1_delegated() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester1/data?delegated=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(6, result.size());
	}

	@Test
	public void get_fromUsername_dataExist_group2_delegated() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester2/data?delegated=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(4, result.size());
	}

	@Test
	public void get_fromUsername_dataExist_group3_delegated() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester3/data?delegated=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(6, result.size());
	}

	@Test
	public void get_fromUsername_dataExist_group4_delegated() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester4/data?delegated=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_group5_delegated() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester5/data?delegated=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(4, result.size());
	}

	@Test
	public void get_fromUsername_dataExist_group6_delegated() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester6/data?delegated=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_group7_delegated() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester7/data?delegated=true&accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Data> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<List<Data>>() {
		});

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		assertEquals(4, result.size());
	}

	@Test
	public void get_fromUsername_dataExist_group1() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester1/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_group2() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester2/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_group3() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester3/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_group4() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester4/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_group5() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester5/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_group6() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester6/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
	}

	@Test
	public void get_fromUsername_dataExist_group7() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/username/tester7/data?accessToken=" + getAccessTokenRoot() + "&sourceRequest=test");

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
