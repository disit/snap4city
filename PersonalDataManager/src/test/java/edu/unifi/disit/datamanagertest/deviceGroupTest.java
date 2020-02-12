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
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
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

import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroup;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElement;

public class deviceGroupTest {

	@Test
	public void get_grpNotExist() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/devicegroup/1000/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.NO_CONTENT.value()));
	}

	@Test
	public void get_grpWithID() throws ClientProtocolException, IOException, java.text.ParseException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/devicegroup/6/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		DeviceGroup result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()), new TypeReference<DeviceGroup>() {
		});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(null, result.getDeleteTime());
		assertEquals("Descrizione del sesto gruppo", result.getDescription());
		assertEquals("MyGroup", result.getHighLevelType());
		assertEquals(Long.valueOf("6"), result.getId());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-12-11 17:00:40"), result.getInsertTime());
		assertEquals("Sesto gruppo", result.getName());
		assertEquals("[ou=Firenze,dc=foo,dc=example,dc=org]", result.getOrganizations());
		assertEquals("private", result.getOwnership());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-12-11 17:00:40"), result.getUpdateTime());
		assertEquals("mirco-rootadmin", result.getUsername());

	}

	@Test
	public void get_grpExist_values() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/devicegroup/40/elements/?&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<DeviceGroupElement> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<DeviceGroupElement>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(2, result.size());
	}

	@Test
	public void get_grpExist_delegations() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/devicegroup/40/delegations/?&accessToken="
						+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<Delegation> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<Delegation>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		// The Anonymous Delegations are not returned
		assertEquals(1, result.size());
	}

	@Test
	public void get_grpNotAuthorized() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet(
				"http://localhost:8080/datamanager/api/v1/devicegroup/5/?sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.FORBIDDEN.value()));
	}

	@Test
	public void get_grpRootList() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/devicegroup/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<DeviceGroup> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<DeviceGroup>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertThat("cardinality", result.size(), greaterThan(0));

	}

	@Test
	public void get_grpDelegation_OK_general_result() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/devicegroup/delegated/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<DeviceGroup> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<DeviceGroup>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(2, result.size());
		assertEquals("E se ne faccio un altro ah ok", result.get(0).getName());
		assertEquals("mirco-tooladmin", result.get(0).getUsername());
	}

	@Test
	public void get_grpPublic_OK_general_result() throws ClientProtocolException, IOException {

		// Given
		HttpUriRequest request = new HttpGet("http://localhost:8080/datamanager/api/v1/devicegroup/public/?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		// When
		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		List<DeviceGroup> result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<List<DeviceGroup>>() {
				});

		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK.value()));

		assertEquals(3, result.size());
		assertEquals("Quinto gruppo", result.get(0).getName());
		assertEquals(null, result.get(0).getUsername());
		assertEquals(Long.valueOf("10"), result.get(1).getId());
		assertEquals(Long.valueOf("5"), result.get(0).getId());
	}

	@Test
	public void post_createGrp() throws ClientProtocolException, IOException {

		// Add group

		HttpPost request = new HttpPost("http://localhost:8080/datamanager/api/v1/devicegroup?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		DeviceGroup grp = new DeviceGroup();
		grp.setDescription("Description of JUnit test group");
		grp.setHighLevelType("MyGroup");
		grp.setName("JUnit test group");
		request.setEntity(createGrpEntity(grp));
		request.addHeader("Content-Type", "application/json; charset=utf8");

		HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

		assertThat(
				httpResponse.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		ObjectMapper mapper = new ObjectMapper();
		DeviceGroup result = mapper.readValue(EntityUtils.toString(httpResponse.getEntity()),
				new TypeReference<DeviceGroup>() {
				});

		Long grpId = result.getId();

		// Add Element

		HttpPost request2 = new HttpPost("http://localhost:8080/datamanager/api/v1/devicegroup/" + String.valueOf(grpId) + "/elements?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");

		ArrayList<DeviceGroupElement> grpel = new ArrayList<DeviceGroupElement>();
		DeviceGroupElement grpe = new DeviceGroupElement();
		grpe.setDeviceGroupId(grpId);
		grpe.setElementId("17055860");
		grpe.setElementType("MyKPI");
		grpe.setInsertTime(new Date());
		grpel.add(grpe);
		request2.setEntity(createGrpElmtEntity(grpel));
		request2.addHeader("Content-Type", "application/json; charset=utf8");
		HttpResponse httpResponse2 = HttpClientBuilder.create().build().execute(request2);
		assertThat(
				httpResponse2.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.CREATED.value()));

		ObjectMapper mapper2 = new ObjectMapper();
		ArrayList<DeviceGroupElement> result2 = mapper2.readValue(EntityUtils.toString(httpResponse2.getEntity()),
				new TypeReference<ArrayList<DeviceGroupElement>>() {
				});

		Long elmtId = result2.get(0).getId();

		// Delete Element

		HttpDelete request3 = new HttpDelete("http://localhost:8080/datamanager/api/v1/devicegroup/" + String.valueOf(grpId) + "/elements/" + String.valueOf(elmtId) + "?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");
		HttpResponse httpResponse3 = HttpClientBuilder.create().build().execute(request3);
		assertThat(
				httpResponse3.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));

		// Delete group

		HttpDelete request4 = new HttpDelete("http://localhost:8080/datamanager/api/v1/devicegroup/" + String.valueOf(grpId) + "?accessToken="
				+ getAccessTokenRoot() + "&sourceRequest=junittest");
		HttpResponse httpResponse4 = HttpClientBuilder.create().build().execute(request4);
		assertThat(
				httpResponse4.getStatusLine().getStatusCode(),
				equalTo(HttpStatus.OK.value()));
	}

	private HttpEntity createGrpEntity(DeviceGroup data) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mapper.writeValue(baos, data);
		return new ByteArrayEntity(baos.toByteArray());
	}

	private HttpEntity createGrpElmtEntity(ArrayList<DeviceGroupElement> data) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mapper.writeValue(baos, data);
		return new ByteArrayEntity(baos.toByteArray());
	}

	private String getAccessTokenRoot() throws IOException {
		return get("accesstoken.mircorootadmin=");
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
