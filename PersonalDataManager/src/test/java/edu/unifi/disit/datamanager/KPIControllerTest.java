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
package edu.unifi.disit.datamanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import edu.unifi.disit.datamanager.controller.rest.ConfigController;
import edu.unifi.disit.datamanager.controller.rest.KPIActivityController;
import edu.unifi.disit.datamanager.controller.rest.KPIDataController;
import edu.unifi.disit.datamanager.controller.rest.KPIDelegationController;
import edu.unifi.disit.datamanager.controller.rest.KPIMetadataController;
import edu.unifi.disit.datamanager.controller.rest.KPIValueController;
import edu.unifi.disit.datamanager.controller.rest.PublicController;
import edu.unifi.disit.datamanager.datamodel.Response;
import edu.unifi.disit.datamanager.datamodel.dto.KPIDataDTO;
import edu.unifi.disit.datamanager.datamodel.dto.KPIValueDTO;
import edu.unifi.disit.datamanager.datamodel.elasticdb.KPIElasticValue;
import edu.unifi.disit.datamanager.datamodel.profiledb.Data;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIActivity;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIMetadata;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIValue;

@SpringBootTest
@ActiveProfiles("local")
public class KPIControllerTest {

	@Autowired
	private KPIDataController kpiDataController;

	@Autowired
	private KPIMetadataController kpiMetadataController;

	@Autowired
	private KPIValueController kpiValueController;

	@Autowired
	private KPIDelegationController kpiDelegationController;

	@Autowired
	private KPIActivityController kpiActivityController;

	@Autowired
	private PublicController publicController;

	@Autowired
	private ConfigController configController;

	@Test
	public void contexLoads() throws Exception {
		assertThat(kpiDataController).isNotNull();
		assertThat(kpiMetadataController).isNotNull();
		assertThat(kpiValueController).isNotNull();
		assertThat(kpiDelegationController).isNotNull();
		assertThat(kpiActivityController).isNotNull();
	}

	@Test
	public void get_kpiDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/154312/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getKPIDataV1ById(154312L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void delete_kpiDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/154312/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.deleteKPIDataV1ById(154312L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_publickpiDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/154312");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getPublicKPIDataV1ById(154312L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_kpiValueNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/9999/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getKPIValueV1ById(17055844L, "9999", "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void delete_kpiValueNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/9999/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.deleteKPIValueV1(17055844L, "9999", "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_kpiMetadataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/metadata/9999/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiMetadataController.getKPIMetadataV1ById(17055844L, 9999L, "junittest",
				null, new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void delete_kpiMetadataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/metadata/9999/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiMetadataController.deleteKPIMetadataV1(17055844L, 9999L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_kpiDelegationNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/delegations/9999/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDelegationController.getKPIDelegationV1ById(17055844L, 9999L, "junittest",
				null, new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void delete_kpiDelegationNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/delegations/9999/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDelegationController.deleteKPIDelegationV1(17055844L, 9999L, "junittest",
				null, new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_kpiActivityNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/activities/25/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getKPIActivityV1ById(17055844L, 25L, new Locale("en"),
				request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_kpiDataWithID() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getKPIDataV1ById(17055844L, "junittest", null,
				new Locale("en"), request);

		KPIData result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals("Ospiti", result.getValueName());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("Accomodation", result.getNature());
		assertEquals("Hotel", result.getSubNature());
		assertEquals("Numero", result.getValueType());
		assertEquals("integer", result.getDataType());
		assertEquals("MySQL", result.getDbValuesType());
		assertEquals("test", result.getUsername());
	}

	@Test
	public void get_publickpiDataWithID() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055844");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getPublicKPIDataV1ById(17055844L, "junittest", null,
				new Locale("en"), request);

		KPIData result = (KPIData) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Ospiti", result.getValueName());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("Accomodation", result.getNature());
		assertEquals("Hotel", result.getSubNature());
		assertEquals("Numero", result.getValueType());
		assertEquals("integer", result.getDataType());
		assertEquals("MySQL", result.getDbValuesType());
		assertEquals(null, result.getUsername());

	}

	@Test
	public void get_kpiValueWithID() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/73/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getKPIValueV1ById(17055844L, "73", "junittest", null,
				new Locale("en"), request);

		KPIValue result = (KPIValue) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals("dasa", result.getValue());
		assertEquals(1551304800000L, result.getDataTime().getTime());
		assertEquals(1551218400000L, result.getInsertTime().getTime());
	}

	@Test
	public void get_publicKpiValueWithID() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055844/values/73/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getPublicKPIValueV1ById(17055844L, "73", "junittest", null,
				new Locale("en"), request);

		KPIValue result = (KPIValue) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals("dasa", result.getValue());
		assertEquals(1551304800000L, result.getDataTime().getTime());
		assertEquals(1551218400000L, result.getInsertTime().getTime());
	}

	@Test
	public void get_kpiValueWithID_KPIDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/534/values/73/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getKPIValueV1ById(534L, "73", "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_publicKpiValueWithID_KPIDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/534/values/73/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getPublicKPIValueV1ById(534L, "73", "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_kpiMetadataWithID() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/metadata/34/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiMetadataController.getKPIMetadataV1ById(17055844L, 34L, "junittest", null,
				new Locale("en"), request);

		KPIMetadata result = (KPIMetadata) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals("test", result.getValue());
		assertEquals("junittest", result.getKey());

	}

	@Test
	public void get_kpiMetadataWithID_KPIDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/534/metadata/34/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiMetadataController.getKPIMetadataV1ById(534L, 34L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

	}

	@Test
	public void get_kpiDelegationWithID() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/delegations/970/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDelegationController.getKPIDelegationV1ById(17055844L, 970L, "junittest",
				null, new Locale("en"), request);

		Delegation result = (Delegation) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals("dash_id", result.getElementId());
		assertEquals("DASHID", result.getElementType());
		assertEquals(1551273437000L, result.getInsertTime().getTime());
		assertEquals("badii", result.getUsernameDelegated());
		assertEquals("pb1", result.getUsernameDelegator());
	}

	@Test
	public void get_kpiDelegationWithID_KPIDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/534/delegations/970/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDelegationController.getKPIDelegationV1ById(534L, 970L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_kpiActivityWithID() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055838/activities/7260/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getKPIActivityV1ById(17055838L, 7260L, new Locale("en"),
				request);

		// KPIActivity result = (KPIActivity) response.getBody();

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

	}

	@Test
	public void get_kpiActivityWithID_KPIDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/534/activities/7260/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getKPIActivityV1ById(534L, 7260L, new Locale("en"),
				request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_noPages() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", null,
				new Locale("en"), -1, -1, null, null, "", null, null, null, null, request);

		List<KPIValue> result = (List<KPIValue>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(3, result.size());

	}

	@Test
	public void get_kpiData_values_kpiDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/534/values/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(534L, "junittest", null,
				new Locale("en"), -1, -1, null, null, "", null, null, null, null, request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

	}

	@Test
	public void get_kpiData_values_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055838/values/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(17055838L, "junittest", null,
				new Locale("en"), -1, -1, null, null, "", null, null, null, null, request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_publickpiData_values_noPages() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055844/values/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueOfPublicKPIV1Pageable(17055844L, "junittest",
				null, new Locale("en"), -1, -1, null, null, "", null, null, null, null, request);

		List<KPIValue> result = (List<KPIValue>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(3, result.size());

	}

	@Test
	public void get_publickpiData_values_kpiDataNotExist() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/534/values/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueOfPublicKPIV1Pageable(534L, "junittest",
				null, new Locale("en"), -1, -1, null, null, "", null, null, null, null, request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

	}

	@Test
	public void get_publickpiData_values_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055838/values/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueOfPublicKPIV1Pageable(17055838L, "junittest",
				null, new Locale("en"), -1, -1, null, null, "", null, null, null, null, request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_metadata_noPages() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/metadata/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiMetadataController.getAllKPIMetadataV1Pageable(17055844L, "junittest",
				null, new Locale("en"), -1, -1, null, null, "", request);

		List<KPIMetadata> result = (List<KPIMetadata>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, result.size());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_delegations_noPages() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/delegations/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDelegationController.getAllDelegationV1Pageable(17055844L, "junittest",
				null, new Locale("en"), -1, -1, null, null, request);

		List<Delegation> result = (List<Delegation>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(1, result.size());

		// The Anonymous Delegations are not returned assertEquals(1, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_activities_noPages() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055838/activities/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getAllKPIActivityV1Pageable(17055838L, new Locale("en"),
				-1, -1, null, null, null, null, null, null, "", "", request);

		List<Delegation> result = (List<Delegation>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, result.size());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_activities_filteredOnAccessType() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055838/activities/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getAllKPIActivityV1Pageable(17055838L, new Locale("en"),
				-1, -1, null, null, null, null, null, null, "", "WRITE", request);

		List<KPIActivity> result = (List<KPIActivity>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, result.size());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_activities_filteredOnSourceRequest() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055838/activities/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getAllKPIActivityV1Pageable(17055838L, new Locale("en"),
				-1, -1, null, null, null, null, null, null, "fakeForTest", "", request);

		List<KPIActivity> result = (List<KPIActivity>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_activities_filteredOnSourceRequestAndAccessType()
			throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055838/activities/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getAllKPIActivityV1Pageable(17055838L, new Locale("en"),
				-1, -1, null, null, null, null, null, null, "fakeForTest", "WRITE", request);

		List<KPIActivity> result = (List<KPIActivity>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_activities_filteredOnSourceRequestAndAccessTypeBis()
			throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055838/activities/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getAllKPIActivityV1Pageable(17055838L, new Locale("en"),
				-1, -1, null, null, null, null, null, null, "iotapp", "WRITE", request);

		List<KPIActivity> result = (List<KPIActivity>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, result.size());

	}

	@Test
	public void get_kpiMetadata_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/metadata/34/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiMetadataController.getKPIMetadataV1ById(17055856L, 34L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

	}

	@Test
	public void delete_kpiMetadata_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/metadata/34/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiMetadataController.deleteKPIMetadataV1(17055856L, 34L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

	}

	@Test
	public void get_kpiData_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getKPIDataV1ById(17055856L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void delete_kpiData_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.deleteKPIDataV1ById(17055856L, "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void get_publickpiData_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055838");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getPublicKPIDataV1ById(17055838L, "junittest", null,
				new Locale("en"), request);
		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

	}

	@Test
	public void get_kpiDelegation_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/delegations/970/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDelegationController.getKPIDelegationV1ById(17055856L, 970L, "junittest",
				null, new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void delete_kpiDelegation_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/delegations/970/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDelegationController.deleteKPIDelegationV1(17055856L, 970L, "junittest",
				null, new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void get_kpiValue_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/values/73/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getKPIValueV1ById(17055856L, "73", "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void delete_kpiValue_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/values/73/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.deleteKPIValueV1(17055856L, "73", "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void get_publickpiValue_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055856/values/73/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getPublicKPIValueV1ById(17055856L, "73", "junittest", null,
				new Locale("en"), request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void get_kpiData_activities_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/activities/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getAllKPIActivityV1Pageable(17055856L, new Locale("en"),
				-1, -1, null, null, null, null, null, null, "", "", request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void get_kpiData_activityWihtID_NotAuthorized() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055856/activities/7260");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiActivityController.getKPIActivityV1ById(17055856L, 7260L, new Locale("en"),
				request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_list() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getOwnKPIDataV1Pageable("junittest", "", new Locale("en"),
				-1, -1, null, null, "", "", request);

		List<KPIData> result = (List<KPIData>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(184, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_page() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getOwnKPIDataV1Pageable("junittest", "", new Locale("en"),
				1, 10, "desc", "id", "", "", request);

		// Then
		Page<KPIData> result = (Page<KPIData>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(10, result.getContent().size());
		assertEquals("id: DESC", result.getSort().toString());
		assertEquals(1, result.getNumber());
		assertEquals(10, result.getNumberOfElements());
		assertEquals(184, result.getTotalElements());
		assertEquals(19, result.getTotalPages());
	}

	@Test
	public void get_kpiData_page_noRoot() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getOwnKPIDataV1Pageable("junittest", "", new Locale("en"),
				1, 10, "desc", "id", "", "", request);

		// Then
		Page<KPIData> result = (Page<KPIData>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, result.getContent().size());
		assertEquals("id: DESC", result.getSort().toString());
		assertEquals(1, result.getNumber());
		assertEquals(0, result.getNumberOfElements());
		assertEquals(0, result.getTotalElements());
		assertEquals(0, result.getTotalPages());
	}

	@Test
	public void get_kpiData_wrongArguments() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getOwnKPIDataV1Pageable("junittest", "", new Locale("en"),
				1, -1, null, null, "", "", request);

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_notFound() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getOwnKPIDataV1Pageable("junittest", "", new Locale("en"),
				-1, -1, null, null, "", "", request);

		List<KPIData> result = (List<KPIData>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_last() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("last", "1");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "",
				new Locale("en"), -1, -1, null, null, "", null, null, null, 1, request);

		List<KPIValue> result = (List<KPIValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(1, result.size());
		assertEquals("43", result.get(0).getValue());
		assertEquals(1549357860000L, result.get(0).getDataTime().getTime());
		assertEquals(1549329780000L, result.get(0).getInsertTime().getTime());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_last3() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("last", "3");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "",
				new Locale("en"), -1, -1, null, null, "", null, null, null, 3, request);

		List<KPIValue> result = (List<KPIValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(3, result.size());
		assertEquals("43", result.get(0).getValue());
		assertEquals("213", result.get(2).getValue());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_first() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("first", "1");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "",
				new Locale("en"), -1, -1, null, null, "", null, null, 1, null, request);

		List<KPIValue> result = (List<KPIValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(1, result.size());
		assertEquals("213", result.get(0).getValue());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_from() throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("from", "2019-02-02T00:00");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "",
				new Locale("en"), -1, -1, null, null, "", "2019-02-02T00:00Z", null, null, null, request);

		List<KPIValue> result = (List<KPIValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(2, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_to() throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("to", "2019-02-02T00:00");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "",
				new Locale("en"), -1, -1, null, null, "", null, "2019-02-02T00:00Z", null, null, request);

		List<KPIValue> result = (List<KPIValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(1, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_from_to() throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("from", "2019-02-01T00:00");
		request.addParameter("to", "2019-02-04T00:00");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "",
				new Locale("en"), -1, -1, null, null, "", "2019-02-01T00:00Z", "2019-02-04T00:00Z", null, null,
				request);

		List<KPIValue> result = (List<KPIValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(2, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_from_to_last() throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("from", "2019-02-01T00:00");
		request.addParameter("to", "2019-02-04T00:00");
		request.addParameter("last", "1");
		ResponseEntity<Object> response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "",
				new Locale("en"), -1, -1, null, null, "", "2019-02-01T00:00Z", "2019-02-04T00:00Z", null, 1, request);

		List<KPIValue> result = (List<KPIValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(1, result.size());
		assertEquals("343", result.get(0).getValue());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_dates_withcoordinates() throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055838/values/dates");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getDistinctKPIValuesDateV1(17055838L, "junittest", null,
				new Locale("en"), true, request);

		List<String> result = (List<String>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(new ArrayList<String>(), result);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_values_dates_withoutcoordinates()
			throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/dates");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getDistinctKPIValuesDateV1(17055844L, "junittest", null,
				new Locale("en"), false, request);

		List<Date> result = (List<Date>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		List<Date> dates = new ArrayList<>();
		dates.add(new SimpleDateFormat("yyyy-MM-dd").parse("2019-02-05"));
		dates.add(new SimpleDateFormat("yyyy-MM-dd").parse("2019-02-02"));
		dates.add(new SimpleDateFormat("yyyy-MM-dd").parse("2019-02-01"));
		assertArrayEquals(dates.toArray(), result.toArray());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_publicKpiData_values_dates_withcoordinates()
			throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055844/values/dates");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getDistinctKPIValuesDateOfPublicKPIV1(17055844L,
				"junittest", null, new Locale("en"), true, request);

		List<String> result = (List<String>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(new ArrayList<String>(), result);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_publicKpiData_values_dates_withoutcoordinates()
			throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055844/values/dates");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getDistinctKPIValuesDateOfPublicKPIV1(17055844L,
				"junittest", null, new Locale("en"), false, request);

		List<String> result = (List<String>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		List<Date> dates = new ArrayList<>();
		dates.add(new SimpleDateFormat("yyyy-MM-dd").parse("2019-02-05"));
		dates.add(new SimpleDateFormat("yyyy-MM-dd").parse("2019-02-02"));
		dates.add(new SimpleDateFormat("yyyy-MM-dd").parse("2019-02-01"));
		assertArrayEquals(dates.toArray(), result.toArray());

	}

	@Test
	public void get_kpiData_values_dates_KPIDataNotExist() throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/534/values/dates");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getDistinctKPIValuesDateV1(534L, "junittest", null,
				new Locale("en"), true, request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

	}

	@Test
	public void get_publicKpiData_values_dates_KPIDataNotExist()
			throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/534/values/dates");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getDistinctKPIValuesDateOfPublicKPIV1(534L, "junittest",
				null, new Locale("en"), true, request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void get_kpiData_values_dates_NotAuthorized() throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055838/values/dates");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getDistinctKPIValuesDateV1(17055838L, "junittest", null,
				new Locale("en"), true, request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

	}

	@Test
	public void get_publicKpiData_values_dates_NotAuthorized()
			throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055838/values/dates");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiValueController.getDistinctKPIValuesDateOfPublicKPIV1(17055838L,
				"junittest", null, new Locale("en"), true, request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_delegated_list() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/delegated");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getDelegatedKPIDataV1Pageable("junittest", "",
				new Locale("en"), -1, -1, null, null, "", "", request);

		List<KPIData> result = (List<KPIData>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(2, result.size());
		assertEquals(null, result.get(1).getInstanceUri());
		assertEquals(null, result.get(1).getKbBased());
		assertEquals("TestLastLatitude", result.get(1).getValueName());
	}

	@Test
	public void get_kpiData_delegated_page() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/delegated");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getDelegatedKPIDataV1Pageable("junittest", "",
				new Locale("en"), 1, 10, "desc", "id", "", "", request);

		// Then
		Page<KPIData> result = (Page<KPIData>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(2, result.getContent().size());
		assertEquals("id: DESC", result.getSort().toString());
		assertEquals(1, result.getNumber());
		assertEquals(2, result.getNumberOfElements());
		assertEquals(12, result.getTotalElements());
		assertEquals(2, result.getTotalPages());
		assertEquals("", result.getContent().get(0).getInstanceUri());
		assertEquals("", result.getContent().get(0).getKbBased());
		assertEquals("Ospiti", result.getContent().get(0).getValueName());
	}

	@Test
	public void get_kpiData_delegated_wrongArguments() throws ClientProtocolException, IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/delegated");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getDelegatedKPIDataV1Pageable("junittest", "",
				new Locale("en"), 1, -1, null, null, "", "", request);

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_delegated_notFound() throws ClientProtocolException, IOException {

		// Authentication
		setFakeUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/delegated");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getDelegatedKPIDataV1Pageable("junittest", "",
				new Locale("en"), -1, -1, null, null, "", "", request);

		List<KPIData> result = (List<KPIData>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, result.size());
	}

	/*
	 * @Test public void get_kpiData_publicOrganization() throws ClientProtocolException, IOException {
	 * 
	 * // Authentication setRootUserOnSecurityContext();
	 * 
	 * // When MockHttpServletRequest request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/kpidata/organization"); request.addParameter("sourceRequest", "junittest"); ResponseEntity<Object> response =
	 * kpiDataController.getOrganizationKPIDataV1Pageable("junittest", "", new Locale("en"), -1, -1, null, null, "", "", request);
	 * 
	 * List<KPIData> result = (List<KPIData>) response.getBody(); // Then assertEquals(HttpStatus.OK, response.getStatusCode());
	 * 
	 * assertEquals(1, result.size()); assertEquals("{\"altitude\":43,\"speed\":12}", result.get(0).getLastValue()); assertEquals(1552429980000L, result.get(0).getLastDate().getTime()); assertEquals(Long.valueOf("17055843"),
	 * result.get(0).getId()); assertEquals(Long.valueOf("17055844"), result.get(1).getId()); }
	 */

	/*
	 * @Test public void get_kpiData_public_activities() throws ClientProtocolException, IOException {
	 * 
	 * // Authentication setRootUserOnSecurityContext();
	 * 
	 * // When MockHttpServletRequest request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/public/kpidata/17055844/activities/" ); request.addParameter("sourceRequest", "junittest"); ResponseEntity<Object>
	 * response = kpiActivityController.getAllKPIActivityOfPublicKPIV1Pageable(17055844L, new Locale("en"), -1, -1, null, null, null, null, null, null, "", "", request);
	 * 
	 * List<KPIActivity> result = (List<KPIActivity>) response.getBody(); // Then assertEquals(HttpStatus.OK, response.getStatusCode());
	 * 
	 * assertEquals(39, result.size()); assertEquals( "da5698be17b9b46962335799779fbeca8ce5d491c0d26243bafef9ea1837a9d8", result.get(0).getSourceId()); assertEquals(7257L, result.get(0).getId().longValue()); }
	 */

	@Test
	public void get_kpiDataValuesCopy_wrongArguments() throws IOException {
		// Authentication
		setRootUserOnSecurityContext();
		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/copy");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("sourceDB", "MySQL");
		request.addParameter("destinationDB", "MySQL");
		ResponseEntity<Object> response = kpiValueController.copyAllKPIValueV1(17055844L, "junittest", null,
				new Locale("en"), "MySQL", "MySQL", null, null, null, null, request);

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

	}

	@Test
	public void get_kpiDataValuesCopy_NotAuthorized() throws IOException {
		// Authentication
		setFakeUserOnSecurityContext();
		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/copy");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("sourceDB", "MySQL");
		request.addParameter("destinationDB", "ElasticSearch");
		ResponseEntity<Object> response = kpiValueController.copyAllKPIValueV1(17055844L, "junittest", null,
				new Locale("en"), "MySQL", "ElasticSearch", null, null, null, null, request);

		// Then
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

	}

	@Test
	public void get_kpiDataValuesCopy_WrongSourceDB() throws IOException {
		// Authentication
		setRootUserOnSecurityContext();
		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/copy");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("sourceDB", "FakeDB");
		request.addParameter("destinationDB", "ElasticSearch");
		ResponseEntity<Object> response = kpiValueController.copyAllKPIValueV1(17055844L, "junittest", null,
				new Locale("en"), "FakeDB", "ElasticSearch", null, null, null, null, request);

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

	}

	@Test
	public void get_kpiDataValuesCopy_WrongDestinationDB() throws IOException {
		// Authentication
		setRootUserOnSecurityContext();
		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/copy");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("sourceDB", "MySQL");
		request.addParameter("destinationDB", "FakeDB");
		ResponseEntity<Object> response = kpiValueController.copyAllKPIValueV1(17055844L, "junittest", null,
				new Locale("en"), "MySQL", "FakeDB", null, null, null, null, request);

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

	}

	/*
	 * @Test public void get_kpiDataValuesCopy_EmptyDestinationDB() throws IOException { // Authentication setRootUserOnSecurityContext(); // When MockHttpServletRequest request = new MockHttpServletRequest("get",
	 * "http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/copy"); request.addParameter("sourceRequest", "junittest"); request.addParameter("sourceDB", "ElasticSearch"); request.addParameter("destinationDB", "MySQL");
	 * ResponseEntity<Object> response = kpiValueController.copyAllKPIValueV1(17055844L, "junittest", null, new Locale("en"), "ElasticSearch", "MySQL", null, null, null, null, request);
	 * 
	 * // Then assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	 * 
	 * }
	 */

	@Test
	public void get_kpiDataValuesCopy_KPIDataNotExists() throws IOException {
		// Authentication
		setFakeUserOnSecurityContext();
		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/534/values/copy");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("sourceDB", "MySQL");
		request.addParameter("destinationDB", "ElasticSearch");
		ResponseEntity<Object> response = kpiValueController.copyAllKPIValueV1(534L, "junittest", null,
				new Locale("en"), "MySQL", "ElasticSearch", null, null, null, null, request);

		// Then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void get_kpiData_mysqlExportElasticCheck() throws ClientProtocolException, IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();
		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055844/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController.getKPIDataV1ById(17055844L, "junittest", null,
				new Locale("en"), request);

		KPIData resultKpiData = (KPIData) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals("MySQL", resultKpiData.getDbValuesType());

		// When

		request = new MockHttpServletRequest("patch",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055844/");
		request.addParameter("sourceRequest", "junittest");
		Map<String, Object> fields = new HashMap<>();
		fields.put("dbValuesType", "ElasticSearch");
		response = kpiDataController.patchKPIDataV1ById(17055844L, fields, "junittest", null, new Locale("en"),
				request);

		// Then
		resultKpiData = (KPIData) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals("ElasticSearch", resultKpiData.getDbValuesType());

		// When
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", null, new Locale("en"), -1, -1,
				null, null, "", null, null, null, null, request);

		List<KPIElasticValue> resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, resultKPIElasticValueList.size());

		// When
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/copy");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("sourceDB", "MySQL");
		request.addParameter("destinationDB", "ElasticSearch");
		response = kpiValueController.copyAllKPIValueV1(17055844L, "junittest", null, new Locale("en"), "MySQL",
				"ElasticSearch", null, null, null, null, request);
		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(3, resultKPIElasticValueList.size());

		// When

		request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("last", "1");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "", new Locale("en"), -1, -1,
				null, null, "", null, null, null, 1, request);
		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();

		// Then

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, resultKPIElasticValueList.size());
		assertEquals("43.0", resultKPIElasticValueList.get(0).getValue().toString());
		assertEquals(1549357860000L, resultKPIElasticValueList.get(0).getDataTime().getTime());
		assertEquals(resultKpiData.getNature(), resultKPIElasticValueList.get(0).getNature());
		assertEquals(resultKpiData.getSubNature(), resultKPIElasticValueList.get(0).getSubNature());

		// When

		request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("last", "3");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "", new Locale("en"), -1, -1,
				null, null, "", null, null, null, 3, request);
		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();

		// Then

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(3, resultKPIElasticValueList.size());
		assertEquals("43.0", resultKPIElasticValueList.get(0).getValue().toString());
		assertEquals("213.0", resultKPIElasticValueList.get(2).getValue().toString());

		// When

		request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("first", "1");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "", new Locale("en"), -1, -1,
				null, null, "", null, null, 1, null, request);
		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();

		// Then

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(1, resultKPIElasticValueList.size());
		assertEquals("213.0", resultKPIElasticValueList.get(0).getValue().toString());

		// When
		request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("from", "2019-02-02T00:00");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "", new Locale("en"), -1, -1,
				null, null, "", "2019-02-02T00:00Z", null, null, null, request);

		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, resultKPIElasticValueList.size());

		// When
		request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("to", "2019-02-02T00:00");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "", new Locale("en"), -1, -1,
				null, null, "", null, "2019-02-02T00:00Z", null, null, request);

		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(1, resultKPIElasticValueList.size());

		// When
		request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("from", "2019-02-01T00:00");
		request.addParameter("to", "2019-02-04T00:00");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "", new Locale("en"), -1, -1,
				null, null, "", "2019-02-01T00:00Z", "2019-02-04T00:00Z", null, null, request);

		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();
		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(2, resultKPIElasticValueList.size());

		// When
		request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/kpidata/17055844/values");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("from", "2019-02-01T00:00");
		request.addParameter("to", "2019-02-04T00:00");
		request.addParameter("last", "1");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", "", new Locale("en"), -1, -1,
				null, null, "", "2019-02-01T00:00Z", "2019-02-04T00:00Z", null, 1, request);

		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();

		assertEquals(1, resultKPIElasticValueList.size());
		assertEquals("343.0", resultKPIElasticValueList.get(0).getValue().toString());

		// When
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", null, new Locale("en"), -1, -1,
				null, null, "", null, null, null, null, request);

		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(3, resultKPIElasticValueList.size());

		for (KPIElasticValue kev : resultKPIElasticValueList) {
			request = new MockHttpServletRequest("delete",
					"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/" + kev.getId());
			request.addParameter("sourceRequest", "junittest");
			response = kpiValueController.deleteKPIValueV1(17055844L, kev.getId(), "junittest", "", new Locale("en"),
					request);

			KPIElasticValue resultDelete = (KPIElasticValue) response.getBody();

			assertEquals(resultDelete.getId(), kev.getId());
		}

		// When
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", null, new Locale("en"), -1, -1,
				null, null, "", null, null, null, null, request);

		resultKPIElasticValueList = (List<KPIElasticValue>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(0, resultKPIElasticValueList.size());

		// When

		request = new MockHttpServletRequest("patch",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055844/");
		request.addParameter("sourceRequest", "junittest");
		fields = new HashMap<>();
		fields.put("dbValuesType", "MySQL");
		response = kpiDataController.patchKPIDataV1ById(17055844L, fields, "junittest", null, new Locale("en"),
				request);

		// Then
		resultKpiData = (KPIData) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals("MySQL", resultKpiData.getDbValuesType());

		// When
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/kpidata/17055844/");
		request.addParameter("sourceRequest", "junittest");
		response = kpiDataController.getKPIDataV1ById(17055844L, "junittest", null, new Locale("en"), request);

		resultKpiData = (KPIData) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals("MySQL", resultKpiData.getDbValuesType());

		// When
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/17055844/values/");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.getAllKPIValueV1Pageable(17055844L, "junittest", null, new Locale("en"), -1, -1,
				null, null, "", null, null, null, null, request);

		List<KPIValue> resultKPIValueList = (List<KPIValue>) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(3, resultKPIValueList.size());
	}

	@Test
	public void post_delete_kpiData() throws IOException {

		// Authentication
		setRootUserOnSecurityContext();

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("post",
				"http://localhost:8080/datamanager/api/v1/kpidata/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController
				.postKPIDataV1(
						new KPIDataDTO("MyKPI", "junittest", "junittest", "junittest", "junittest", "junittest",
								"string", "junittest", "junittest", null, null, null, null, null, null, null, null,
								null, null, null, null, null, null, null, null, null, null, "junittest", "junittest",
								"43.15", "40.65", null, null, "MySQL", null),
						"junittest", null, new Locale("en"), request);

		KPIData result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("junittest", result.getNature());
		assertEquals("junittest", result.getSubNature());
		assertEquals("junittest", result.getValueName());
		assertEquals("junittest", result.getValueType());
		assertEquals("junittest", result.getValueUnit());
		assertEquals("string", result.getDataType());
		assertEquals("junittest", result.getInstanceUri());
		assertEquals("junittest", result.getGetInstances());
		assertEquals("adifino", result.getUsername());
		assertEquals("[ou=Antwerp,dc=foo,dc=example,dc=org]", result.getOrganizations());
		assertEquals("private", result.getOwnership());
		assertEquals("junittest", result.getDescription());
		assertEquals("junittest", result.getInfo());
		assertEquals("43.15", result.getLatitude());
		assertEquals("40.65", result.getLongitude());
		assertEquals("MySQL", result.getDbValuesType());
		assertEquals(null, result.getDeleteTime());

		request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId());
		request.addParameter("sourceRequest", "junittest");
		response = kpiDataController.deleteKPIDataV1ById(result.getId(), "junittest", null, new Locale("en"), request);

		assertEquals(HttpStatus.OK, response.getStatusCode());

		// When
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId());
		request.addParameter("sourceRequest", "junittest");
		response = kpiDataController.getKPIDataV1ById(result.getId(), "junittest", null, new Locale("en"), request);

		result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotEquals(null, result.getDeleteTime());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void post_values_delete_values_kpiData() throws IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When CREATE KPI
		MockHttpServletRequest request = new MockHttpServletRequest("post",
				"http://localhost:8080/datamanager/api/v1/kpidata/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController
				.postKPIDataV1(
						new KPIDataDTO("MyKPI", "junittest", "junittest", "junittest", "junittest", "junittest",
								"string", "junittest", "junittest", null, null, null, null, null, null, null, null,
								null, null, null, null, null, null, null, null, null, null, "junittest", "junittest",
								"43.15", "40.65", null, null, "MySQL", null),
						"junittest", null, new Locale("en"), request);

		KPIData result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("junittest", result.getNature());
		assertEquals("junittest", result.getSubNature());
		assertEquals("junittest", result.getValueName());
		assertEquals("junittest", result.getValueType());
		assertEquals("junittest", result.getValueUnit());
		assertEquals("string", result.getDataType());
		assertEquals("junittest", result.getInstanceUri());
		assertEquals("junittest", result.getGetInstances());
		assertEquals("adifino", result.getUsername());
		assertEquals("[ou=Antwerp,dc=foo,dc=example,dc=org]", result.getOrganizations());
		assertEquals("private", result.getOwnership());
		assertEquals("junittest", result.getDescription());
		assertEquals("junittest", result.getInfo());
		assertEquals("43.15", result.getLatitude());
		assertEquals("40.65", result.getLongitude());
		assertEquals("MySQL", result.getDbValuesType());
		assertEquals(null, result.getDeleteTime());
		assertEquals(null, result.getLastLatitude());
		assertEquals(null, result.getLastLongitude());
		assertEquals(null, result.getLastDate());
		assertEquals(null, result.getLastValue());

		// When CREATE VALUE
		request = new MockHttpServletRequest("post",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId() + "/values");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.postKPIValueV1(result.getId(),
				new KPIValueDTO(null, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-05-25T00:00:00Z"),
						null, null, "35", "40.78", "45.09"),
				"junittest", null, new Locale("en"), request);

		KPIValue resultValue = (KPIValue) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-05-25T00:00:00Z"),
				resultValue.getDataTime());
		assertEquals(result.getId(), resultValue.getKpiId());
		assertEquals("35", resultValue.getValue());
		assertEquals("40.78", resultValue.getLatitude());
		assertEquals("45.09", resultValue.getLongitude());

		// When GET KPI
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId());
		request.addParameter("sourceRequest", "junittest");
		response = kpiDataController.getKPIDataV1ById(result.getId(), "junittest", null, new Locale("en"), request);

		result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("junittest", result.getNature());
		assertEquals("junittest", result.getSubNature());
		assertEquals("junittest", result.getValueName());
		assertEquals("junittest", result.getValueType());
		assertEquals("junittest", result.getValueUnit());
		assertEquals("string", result.getDataType());
		assertEquals("junittest", result.getInstanceUri());
		assertEquals("junittest", result.getGetInstances());
		assertEquals("adifino", result.getUsername());
		assertEquals("[ou=Antwerp,dc=foo,dc=example,dc=org]", result.getOrganizations());
		assertEquals("private", result.getOwnership());
		assertEquals("junittest", result.getDescription());
		assertEquals("junittest", result.getInfo());
		assertEquals("43.15", result.getLatitude());
		assertEquals("40.65", result.getLongitude());
		assertEquals("MySQL", result.getDbValuesType());
		assertEquals(null, result.getDeleteTime());
		assertEquals("40.78", result.getLastLatitude());
		assertEquals("45.09", result.getLastLongitude());
		assertNotEquals(null, result.getLastDate());
		assertEquals("35", result.getLastValue());

		// When VALUES LIST
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId() + "/values");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.getAllKPIValueV1Pageable(result.getId(), "junittest", null, new Locale("en"), -1,
				-1, null, null, null, null, null, null, null, request);

		List<KPIValue> resultValueList = (List<KPIValue>) response.getBody();
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, resultValueList.size());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-05-25T00:00:00Z"),
				resultValueList.get(0).getDataTime());
		assertEquals(result.getId(), resultValueList.get(0).getKpiId());
		assertEquals("35", resultValueList.get(0).getValue());
		assertEquals("40.78", resultValueList.get(0).getLatitude());
		assertEquals("45.09", resultValueList.get(0).getLongitude());

		// When DELETE VALUES
		request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId() + "/values");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.deleteAllKPIValuesV1(result.getId(), "junittest", null, new Locale("en"),
				request);

		resultValueList = (List<KPIValue>) response.getBody();
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, resultValueList.size());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-05-25T00:00:00Z"),
				resultValueList.get(0).getDataTime());
		assertEquals(result.getId(), resultValueList.get(0).getKpiId());
		assertEquals("35", resultValueList.get(0).getValue());
		assertEquals("40.78", resultValueList.get(0).getLatitude());
		assertEquals("45.09", resultValueList.get(0).getLongitude());

		// When GET KPI
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId());
		request.addParameter("sourceRequest", "junittest");
		response = kpiDataController.getKPIDataV1ById(result.getId(), "junittest", null, new Locale("en"), request);

		result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("junittest", result.getNature());
		assertEquals("junittest", result.getSubNature());
		assertEquals("junittest", result.getValueName());
		assertEquals("junittest", result.getValueType());
		assertEquals("junittest", result.getValueUnit());
		assertEquals("string", result.getDataType());
		assertEquals("junittest", result.getInstanceUri());
		assertEquals("junittest", result.getGetInstances());
		assertEquals("adifino", result.getUsername());
		assertEquals("[ou=Antwerp,dc=foo,dc=example,dc=org]", result.getOrganizations());
		assertEquals("private", result.getOwnership());
		assertEquals("junittest", result.getDescription());
		assertEquals("junittest", result.getInfo());
		assertEquals("43.15", result.getLatitude());
		assertEquals("40.65", result.getLongitude());
		assertEquals("MySQL", result.getDbValuesType());
		assertEquals(null, result.getDeleteTime());
		assertEquals(null, result.getLastLatitude());
		assertEquals(null, result.getLastLongitude());
		assertEquals(null, result.getLastDate());
		assertEquals(null, result.getLastValue());

		// When VALUES LIST
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId() + "/values");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.getAllKPIValueV1Pageable(result.getId(), "junittest", null, new Locale("en"), -1,
				-1, null, null, null, null, null, null, null, request);

		resultValueList = (List<KPIValue>) response.getBody();
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(0, resultValueList.size());

		// When DELETE KPI
		request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId());
		request.addParameter("sourceRequest", "junittest");
		response = kpiDataController.deleteKPIDataV1ById(result.getId(), "junittest", null, new Locale("en"), request);

		result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotEquals(null, result.getDeleteTime());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void post_values_delete_values_kpiData_Elastic() throws IOException, ParseException {

		// Authentication
		setRootUserOnSecurityContext();

		// When CREATE KPI
		MockHttpServletRequest request = new MockHttpServletRequest("post",
				"http://localhost:8080/datamanager/api/v1/kpidata/");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = kpiDataController
				.postKPIDataV1(
						new KPIDataDTO("MyKPI", "junittest", "junittest", "junittest", "junittest", "junittest",
								"string", "junittest", "junittest", null, null, null, null, null, null, null, null,
								null, null, null, null, null, null, null, null, null, null, "junittest", "junittest",
								"43.15", "40.65", null, null, "ElasticSearch", null),
						"junittest", null, new Locale("en"), request);

		KPIData result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("junittest", result.getNature());
		assertEquals("junittest", result.getSubNature());
		assertEquals("junittest", result.getValueName());
		assertEquals("junittest", result.getValueType());
		assertEquals("junittest", result.getValueUnit());
		assertEquals("string", result.getDataType());
		assertEquals("junittest", result.getInstanceUri());
		assertEquals("junittest", result.getGetInstances());
		assertEquals("adifino", result.getUsername());
		assertEquals("[ou=Antwerp,dc=foo,dc=example,dc=org]", result.getOrganizations());
		assertEquals("private", result.getOwnership());
		assertEquals("junittest", result.getDescription());
		assertEquals("junittest", result.getInfo());
		assertEquals("43.15", result.getLatitude());
		assertEquals("40.65", result.getLongitude());
		assertEquals("ElasticSearch", result.getDbValuesType());
		assertEquals(null, result.getDeleteTime());
		assertEquals(null, result.getLastLatitude());
		assertEquals(null, result.getLastLongitude());
		assertEquals(null, result.getLastDate());
		assertEquals(null, result.getLastValue());

		// When CREATE VALUE
		request = new MockHttpServletRequest("post",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId() + "/values");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.postKPIValueV1(result.getId(),
				new KPIValueDTO(null, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-05-25T00:00:00Z"),
						null, null, "35", "40.78", "45.09"),
				"junittest", null, new Locale("en"), request);

		KPIElasticValue resultValue = (KPIElasticValue) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-05-25T00:00:00Z"),
				resultValue.getDataTime());
		assertEquals(result.getId().toString(), resultValue.getSensorId());
		assertEquals("35.0", resultValue.getValue().toString());
		assertEquals("40.78", resultValue.getLatitude());
		assertEquals("45.09", resultValue.getLongitude());

		// When GET KPI
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId());
		request.addParameter("sourceRequest", "junittest");
		response = kpiDataController.getKPIDataV1ById(result.getId(), "junittest", null, new Locale("en"), request);

		result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("junittest", result.getNature());
		assertEquals("junittest", result.getSubNature());
		assertEquals("junittest", result.getValueName());
		assertEquals("junittest", result.getValueType());
		assertEquals("junittest", result.getValueUnit());
		assertEquals("string", result.getDataType());
		assertEquals("junittest", result.getInstanceUri());
		assertEquals("junittest", result.getGetInstances());
		assertEquals("adifino", result.getUsername());
		assertEquals("[ou=Antwerp,dc=foo,dc=example,dc=org]", result.getOrganizations());
		assertEquals("private", result.getOwnership());
		assertEquals("junittest", result.getDescription());
		assertEquals("junittest", result.getInfo());
		assertEquals("43.15", result.getLatitude());
		assertEquals("40.65", result.getLongitude());
		assertEquals("ElasticSearch", result.getDbValuesType());
		assertEquals(null, result.getDeleteTime());
		assertEquals("40.78", result.getLastLatitude());
		assertEquals("45.09", result.getLastLongitude());
		assertNotEquals(null, result.getLastDate());
		assertEquals("35.0", result.getLastValue().toString());

		// When VALUES LIST
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId() + "/values");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.getAllKPIValueV1Pageable(result.getId(), "junittest", null, new Locale("en"), -1,
				-1, null, null, null, null, null, null, null, request);

		List<KPIElasticValue> resultValueList = (List<KPIElasticValue>) response.getBody();
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, resultValueList.size());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-05-25T00:00:00Z"),
				resultValueList.get(0).getDataTime());
		assertEquals(result.getId().toString(), resultValueList.get(0).getSensorId());
		assertEquals("35.0", resultValueList.get(0).getValue().toString());
		assertEquals("40.78", resultValueList.get(0).getLatitude());
		assertEquals("45.09", resultValueList.get(0).getLongitude());

		// When DELETE VALUES
		request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId() + "/values");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.deleteAllKPIValuesV1(result.getId(), "junittest", null, new Locale("en"),
				request);

		resultValueList = (List<KPIElasticValue>) response.getBody();
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, resultValueList.size());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-05-25T00:00:00Z"),
				resultValueList.get(0).getDataTime());
		assertEquals(result.getId().toString(), resultValueList.get(0).getSensorId());
		assertEquals("35.0", resultValueList.get(0).getValue().toString());
		assertEquals("40.78", resultValueList.get(0).getLatitude());
		assertEquals("45.09", resultValueList.get(0).getLongitude());

		// When GET KPI
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId());
		request.addParameter("sourceRequest", "junittest");
		response = kpiDataController.getKPIDataV1ById(result.getId(), "junittest", null, new Locale("en"), request);

		result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("MyKPI", result.getHighLevelType());
		assertEquals("junittest", result.getNature());
		assertEquals("junittest", result.getSubNature());
		assertEquals("junittest", result.getValueName());
		assertEquals("junittest", result.getValueType());
		assertEquals("junittest", result.getValueUnit());
		assertEquals("string", result.getDataType());
		assertEquals("junittest", result.getInstanceUri());
		assertEquals("junittest", result.getGetInstances());
		assertEquals("adifino", result.getUsername());
		assertEquals("[ou=Antwerp,dc=foo,dc=example,dc=org]", result.getOrganizations());
		assertEquals("private", result.getOwnership());
		assertEquals("junittest", result.getDescription());
		assertEquals("junittest", result.getInfo());
		assertEquals("43.15", result.getLatitude());
		assertEquals("40.65", result.getLongitude());
		assertEquals("ElasticSearch", result.getDbValuesType());
		assertEquals(null, result.getDeleteTime());
		assertEquals(null, result.getLastLatitude());
		assertEquals(null, result.getLastLongitude());
		assertEquals(null, result.getLastDate());
		assertEquals(null, result.getLastValue());

		// When VALUES LIST
		request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId() + "/values");
		request.addParameter("sourceRequest", "junittest");
		response = kpiValueController.getAllKPIValueV1Pageable(result.getId(), "junittest", null, new Locale("en"), -1,
				-1, null, null, null, null, null, null, null, request);

		resultValueList = (List<KPIElasticValue>) response.getBody();
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(0, resultValueList.size());

		// When DELETE KPI
		request = new MockHttpServletRequest("delete",
				"http://localhost:8080/datamanager/api/v1/kpidata/" + result.getId());
		request.addParameter("sourceRequest", "junittest");
		response = kpiDataController.deleteKPIDataV1ById(result.getId(), "junittest", null, new Locale("en"), request);

		result = (KPIData) response.getBody();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotEquals(null, result.getDeleteTime());

	}

	@Test
	public void check_publicOK1_from_external() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/access/check");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("elementID", "6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e");
		request.addParameter("elementType", "AppID");
		ResponseEntity<Object> response = publicController.checkAccessPublicV1("junittest",
				"6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e", "AppID", null, new Locale("en"),
				request);

		// Then

		Response result = (Response) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(true, result.getResult());
	}

	@Test
	public void check_publicOK2_from_external() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/access/check");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("elementID", "6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e");
		request.addParameter("elementType", "AppID");
		request.addParameter("variableName", "latitude_longitude");
		ResponseEntity<Object> response = publicController.checkAccessPublicV1("junittest",
				"6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e", "AppID", "latitude_longitude",
				new Locale("en"), request);

		// Then

		Response result = (Response) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(true, result.getResult());
	}

	@Test
	public void check_publicKO_from_external() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/access/check");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("elementID", "dash_id");
		request.addParameter("elementType", "DASHID");
		ResponseEntity<Object> response = publicController.checkAccessPublicV1("junittest", "dash_id", "DASHID", null,
				new Locale("en"), request);

		// Then

		Response result = (Response) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(false, result.getResult());

	}

	@Test
	public void check_publicKO2_from_external() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/access/check");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("elementID", "6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e");
		request.addParameter("elementType", "AppID");
		request.addParameter("variableName", "altro");
		ResponseEntity<Object> response = publicController.checkAccessPublicV1("junittest",
				"6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e", "AppID", "altro", new Locale("en"),
				request);

		// Then

		Response result = (Response) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(false, result.getResult());
	}

	@Test
	public void get_dataExist_public_complexxo() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/data");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("variableName", "latitude_longitude");
		request.addParameter("motivation", "Shared Position");
		ResponseEntity<Object> response = publicController.getDataPublicV1("junittest", "latitude_longitude",
				"Shared Position", null, null, 0, 0, new Locale("en"), request);

		List<Data> result = (List<Data>) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(5, result.size());
	}

	/*
	 * @SuppressWarnings("unchecked")
	 * 
	 * @Test public void get_dataDelegation_OK_general_noresult_public() throws ClientProtocolException, IOException {
	 * 
	 * // When MockHttpServletRequest request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/public/data"); request.addParameter("sourceRequest", "junittest"); ResponseEntity<Object> response =
	 * publicController.getDataPublicV1("junittest", null, null, null, null, 0, 0, new Locale("en"), request);
	 * 
	 * List<Data> result = (List<Data>) response.getBody();
	 * 
	 * assertEquals(HttpStatus.OK, response.getStatusCode());
	 * 
	 * assertEquals(349, result.size()); }
	 */

	@SuppressWarnings("unchecked")
	@Test
	public void get_dataDelegation_OK_general_result_public() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/data");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("variableName", "latitude_longitude");
		request.addParameter("motivation", "Shared Position");
		ResponseEntity<Object> response = publicController.getDataPublicV1("junittest", "latitude_longitude",
				"Shared Position", null, null, 0, 0, new Locale("en"), request);

		List<Data> result = (List<Data>) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(5, result.size());
	}

	@Test
	public void get_dataDelegation_OK_general_lat_mot_public() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/data");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("variableName", "latitude_longitude");
		request.addParameter("motivation", "Shared Position");
		request.addParameter("last", "1");
		ResponseEntity<Object> response = publicController.getDataPublicV1("junittest", "latitude_longitude",
				"Shared Position", null, null, 0, 1, new Locale("en"), request);

		List<Data> result = (List<Data>) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(1, result.size());

		assertEquals(null, result.get(0).getUsername());
		assertEquals(1525186380000l, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_dataDelegation_OK_general_lat_public() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/data");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("variableName", "latitude_longitude");
		request.addParameter("motivation", "Shared Position");
		request.addParameter("last", "2");
		ResponseEntity<Object> response = publicController.getDataPublicV1("junittest", "latitude_longitude",
				"Shared Position", null, null, 0, 2, new Locale("en"), request);

		List<Data> result = (List<Data>) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(2, result.size());
		assertEquals(null, result.get(0).getUsername());
		assertEquals(1525186380000L, result.get(0).getDataTime().getTime());
		assertEquals(null, result.get(1).getUsername());
		assertEquals(1525186352000L, result.get(1).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataExist_anonymous_complexxo() throws ClientProtocolException, IOException {

		/// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/data");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("variableName", "latitude_longitude");
		request.addParameter("motivation", "Shared Position");

		ResponseEntity<Object> response = publicController.getDataPublicV1("junittest", "latitude_longitude",
				"Shared Position", null, null, 0, 0, new Locale("en"), request);

		List<Data> result = (List<Data>) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(5, result.size());
	}

	/*
	 * @Test public void get_fromUsername_dataDelegation_OK_general_noresult_anonymous() throws ClientProtocolException, IOException {
	 * 
	 * // When MockHttpServletRequest request = new MockHttpServletRequest("get", "http://localhost:8080/datamanager/api/v1/public/data"); request.addParameter("sourceRequest", "junittest");
	 * 
	 * ResponseEntity<Object> response = publicController.getDataPublicV1("junittest", null, null, null, null, 0, 0, new Locale("en"), request);
	 * 
	 * List<Data> result = (List<Data>) response.getBody();
	 * 
	 * assertEquals(HttpStatus.OK, response.getStatusCode());
	 * 
	 * assertEquals(349, result.size()); }
	 */

	@Test
	public void get_fromUsername_dataDelegation_OK_general_result_anonymous()
			throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/data");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("variableName", "latitude_longitude");
		request.addParameter("motivation", "Shared Position");
		ResponseEntity<Object> response = publicController.getDataPublicV1("junittest", "latitude_longitude",
				"Shared Position", null, null, 0, 0, new Locale("en"), request);

		List<Data> result = (List<Data>) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(5, result.size());
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_lat_mot_anonymous()
			throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/data");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("variableName", "latitude_longitude");
		request.addParameter("motivation", "Shared Position");
		request.addParameter("last", "1");
		ResponseEntity<Object> response = publicController.getDataPublicV1("junittest", "latitude_longitude",
				"Shared Position", null, null, 0, 1, new Locale("en"), request);

		List<Data> result = (List<Data>) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(1, result.size());
		assertEquals(null, result.get(0).getUsername());
		assertEquals(1525186380000L, result.get(0).getDataTime().getTime());
	}

	@Test
	public void get_fromUsername_dataDelegation_OK_general_lat_anonymous() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/data");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("variableName", "latitude_longitude");
		request.addParameter("motivation", "Shared Position");
		request.addParameter("last", "2");
		ResponseEntity<Object> response = publicController.getDataPublicV1("junittest", "latitude_longitude",
				"Shared Position", null, null, 0, 2, new Locale("en"), request);

		List<Data> result = (List<Data>) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(2, result.size());
		assertEquals(null, result.get(0).getUsername());
		assertEquals(1525186380000l, result.get(0).getDataTime().getTime());
		assertEquals(null, result.get(1).getUsername());
		assertEquals(1525186352000l, result.get(1).getDataTime().getTime());
	}

	@Test
	public void config_ok() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/configuration/v1");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = configController.getConfiguration("junittest", "v1", new Locale("en"));
		HashMap<String, String> result = (HashMap<String, String>) response.getBody();

		HashMap<String, String> totest = new HashMap<String, String>();
		totest.put("organization.list", "[\"Antwerp\",\"DISIT\", \"Firenze\",\"GardaLake\",\"Helsinki\",\"LonatoDelGarda\",\"Other\",\"Sardegna\",\"SmartBed\",\"Toscana\"]");
		totest.put("Dictionary.url", "https://processloader.snap4city.org/processloader/api/dictionary/");
		totest.put("Authentication.url", "https://www.disit.org/auth/");
		totest.put("orgInfo.url", "https://www.snap4city.org/dashboardSmartCity/api/getOrganizationParams.php");
		totest.put("grp.Authentication.clientId", "js-grp-client-test");
		totest.put("kpi.Authentication.clientId", "js-kpi-client-test");
		totest.put("ldap.basicdn", "dc=foo,dc=example,dc=org");
		totest.put("elasticsearch.hosts", "192.168.1.103");

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(totest, result);
	}

	@Test
	public void config_ko() throws ClientProtocolException, IOException {
		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/configuration/v2");
		request.addParameter("sourceRequest", "junittest");
		ResponseEntity<Object> response = configController.getConfiguration("junittest", "v2", new Locale("en"));

		// Then assertThat( httpResponse.getStatusLine().getStatusCode(),
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void public_mygroups_ok1() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/access/check");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("elementType", "MyKPI");
		request.addParameter("elementID", "17055859");
		ResponseEntity<Object> response = publicController.checkAccessPublicV1("junittest", "17055859", "MyKPI", null,
				new Locale("en"), request);

		Response result = (Response) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(true, result.getResult());
	}

	@Test
	public void public_mygroups_ko1() throws ClientProtocolException, IOException { // When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/access/check");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("elementType", "MyKPI");
		request.addParameter("elementID", "17055860");
		ResponseEntity<Object> response = publicController.checkAccessPublicV1("junittest", "17055860", "MyKPI", null,
				new Locale("en"), request);

		Response result = (Response) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(false, result.getResult());
	}

	@Test
	public void public_mygroups_ko2() throws ClientProtocolException, IOException {

		// When
		MockHttpServletRequest request = new MockHttpServletRequest("get",
				"http://localhost:8080/datamanager/api/v1/public/access/check");
		request.addParameter("sourceRequest", "junittest");
		request.addParameter("elementType", "IOTID");
		request.addParameter("elementID", "Firenze%3Abroker%3Adevice1");
		ResponseEntity<Object> response = publicController.checkAccessPublicV1("junittest",
				"Firenze%3Abroker%3Adevice1", "IOTID", null, new Locale("en"), request);

		Response result = (Response) response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(false, result.getResult());
	}

	private void setRootUserOnSecurityContext() throws IOException {
		List<GrantedAuthority> authorities = new ArrayList<>();
		// here we set jus a rule for the user
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		List<String> roles = new ArrayList<>();
		roles.add("RootAdmin");
		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
				"adifino", roles, authorities);

		SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
	}

	private void setFakeUserOnSecurityContext() throws IOException {
		List<GrantedAuthority> authorities = new ArrayList<>();
		// here we set jus a rule for the user
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		List<String> roles = new ArrayList<>();
		roles.add("Observer");
		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
				"fakeUser", roles, authorities);

		SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
	}

}