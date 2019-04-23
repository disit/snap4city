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

import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIMetadata;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIValue;

public class kpiDataTest {

	@Test
	public void get_kpiDataNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/kpidata/154312/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_kpiValueNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/9999/?accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_kpiMetadataNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/metadata/9999/?accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_kpiDelegationNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/delegations/9999/?accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_kpiDataWithID() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/kpidata/17055844/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		KPIData result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<KPIData>() {
		});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals("Ospiti", result.getValueName());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("Accomodation", result.getNature());
		assertEquals("Hotel", result.getSubNature());
		assertEquals("Numero", result.getValueType());
		assertEquals("integer", result.getDataType());
		assertEquals("test", result.getUsername());
	}

	@Test
	public void get_kpiValueWithID() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/73/?accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then

		ObjectMapper mapper = new ObjectMapper();
		KPIValue result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<KPIValue>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals("dasa", result.getValue());
		assertEquals(1551308400000L, result.getDataTime().getTime());
		assertEquals(1551222000000L, result.getInsertTime().getTime());
	}

	@Test
	public void get_kpiMetadataWithID() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/metadata/34/?accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		KPIMetadata result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<KPIMetadata>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals("test", result.getValue());
		assertEquals("junittest", result.getKey());
	}

	@Test
	public void get_kpiDelegationWithID() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/delegations/970/?accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		Delegation result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<Delegation>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals("17055848", result.getElementId());
		assertEquals("MyData", result.getElementType());
		assertEquals(1551949062000L, result.getInsertTime().getTime());
		assertEquals("adifino", result.getUsernameDelegated());
		assertEquals("badii", result.getUsernameDelegator());
	}
	
	@Test
	public void get_kpiDataExist_values() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/?&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIValue> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIValue>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
	}
	
	@Test
	public void get_kpiDataExist_metadata() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/metadata/?&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIMetadata> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIMetadata>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(0, result.size());
	}
	
	@Test
	public void get_kpiDataExist_delegations() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/delegations/?&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<Delegation>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		//The Anonymous Delegations are not returned
		assertEquals(1, result.size());
	}

	@Test
	public void get_kpiMetadataNotAuthorized() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/metadata/34/?sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.FORBIDDEN.value()));
	}

	@Test
	public void get_kpiDataNotAuthorized() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/?sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.FORBIDDEN.value()));
	}

	@Test
	public void get_kpiDelegationNotAuthorized() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/delegations/970/?sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.FORBIDDEN.value()));
	}

	@Test
	public void get_kpiValueNotAuthorized() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/73/?sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.FORBIDDEN.value()));
	}

	@Test
	public void get_kpiDataRootList() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/kpidata/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIData> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIData>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(175, result.size());
	}

	@Test
	public void get_kpiDataExist_last() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values?last=1&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIValue> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIValue>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(1, result.size());
		assertEquals("43", result.get(0).getValue());
		assertEquals(1549361460000L, result.get(0).getDataTime().getTime());
		assertEquals(1549333380000L, result.get(0).getInsertTime().getTime());
	}

	@Test
	public void get_kpiDataExist_last3() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values?last=3&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIValue> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIValue>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
		assertEquals("43", result.get(0).getValue());
		assertEquals("213", result.get(2).getValue());
	}

	@Test
	public void get_kpiDataExist_first() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values?first=1&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIValue> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIValue>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(1, result.size());
		assertEquals("213", result.get(0).getValue());
	}

	@Test
	public void get_kpiDataExist_from() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values?from=2019-02-02T00:00&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIValue> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIValue>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(2, result.size());
	}

	@Test
	public void get_kpiDataExist_to() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values?to=2019-02-02T00:00&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIValue> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIValue>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(1, result.size());
	}

	@Test
	public void get_kpiDataExist_fromto() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values?from=2019-02-01T00:00&to=2019-02-04T00:00&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIValue> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIValue>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(2, result.size());
	}

	@Test
	public void get_kpiDataExist_fromto_last() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values?from=2019-02-01T00:00&to=2019-02-04T00:00&last=1&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIValue> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIValue>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(1, result.size());
		assertEquals("343", result.get(0).getValue());
	}

	@Test
	public void get_kpiDataDelegation_OK_general_result() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/kpidata/delegated/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIData> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIData>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(2, result.size());
		assertEquals("43", result.get(0).getLastValue());
		assertEquals("test", result.get(0).getUsername());
	}

	@Test
	public void get_kpiDataPublic_OK_general_result() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/kpidata/public/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<KPIData> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<KPIData>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(2, result.size());
		assertEquals("43", result.get(0).getLastValue());
		assertEquals("test", result.get(0).getUsername());
		assertEquals(Long.valueOf("17055843"), result.get(1).getId());
		assertEquals(Long.valueOf("17055844"), result.get(0).getId());
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
