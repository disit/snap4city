/* Data Manager pi(DM).piDa
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
package edu.unifi.disit.datamanager.controller.rest;

import edu.unifi.disit.datamanager.RequestHelper;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unifi.disit.datamanager.datamodel.ActivityAccessType;
import edu.unifi.disit.datamanager.datamodel.KPIActivityDomainType;
import edu.unifi.disit.datamanager.datamodel.dto.KPIElasticValueDTO;
import edu.unifi.disit.datamanager.datamodel.elasticdb.KPIElasticValue;
import edu.unifi.disit.datamanager.datamodel.profiledb.Delegation;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElement;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;
import edu.unifi.disit.datamanager.security.Encryptor;
import edu.unifi.disit.datamanager.service.IAccessService;
import edu.unifi.disit.datamanager.service.ICredentialsService;
import edu.unifi.disit.datamanager.service.IDelegationService;
import edu.unifi.disit.datamanager.service.IDeviceGroupElementService;
import edu.unifi.disit.datamanager.service.IDeviceGroupService;
import edu.unifi.disit.datamanager.service.IKPIActivityService;
import edu.unifi.disit.datamanager.service.IKPIDataService;
import edu.unifi.disit.datamanager.service.IKPIValueService;

@RestController
public class KPIElasticValueController {

	private static final Logger logger = LogManager.getLogger();
	private static final String NO_DATA_FOUND = "No data found";
	private static final String RIGHTS_EXCEPTION = "Rights exception";
	private static final String WRONG_KPI_DATA = "Wrong KPI Data";
	private static final String WRONG_ARGUMENTS = "Wrong Arguments";

	@Autowired
	IKPIValueService kpiValueService;

	@Autowired
	IKPIDataService kpiDataService;
	
	@Autowired
	IDeviceGroupElementService deviceGroupElementService;
	@Autowired
	IDeviceGroupService deviceGroupService;

	@Autowired
	IAccessService accessService;

	@Autowired
	IKPIActivityService kpiActivityService;

	@Autowired
	ICredentialsService credentialService;
	
	@Autowired
	IDelegationService delegationService;
	
	@Autowired
	Encryptor encryptor;

	// -------------------GET KPI Value From ID ------------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/elasticvalues/{id}")
	public ResponseEntity<Object> getKPIElasticValueV1ById(@PathVariable("kpiId") Long kpiId,
			@PathVariable("id") String id, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getKPIElasticValueV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			
			KPIElasticValue kpiElasticValue = kpiValueService.getKPIElasticValueById(id, lang);

			if (kpiElasticValue == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), NO_DATA_FOUND, null,
						RequestHelper.getClientIpAddr(request));

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpivalue {}", kpiElasticValue.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.VALUE);
				return new ResponseEntity<>(kpiElasticValue, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------GET Public KPI Value From ID
	// ------------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/elasticvalues/{id}")
	public ResponseEntity<Object> getPublicKPIElasticValueV1ById(@PathVariable("kpiId") Long kpiId,
			@PathVariable("id") String id, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested getPublicKPIElasticValueV1ById id {} lang {} sourceRequest {}", id, lang, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			KPIElasticValue kpiElasticValue = kpiValueService.getKPIElasticValueById(sourceId, lang);

			if (kpiElasticValue == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), NO_DATA_FOUND, null,
						RequestHelper.getClientIpAddr(request));

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else {
				logger.info("Returning kpivalue {}", kpiElasticValue.getId());

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiData.getId(), ActivityAccessType.READ, KPIActivityDomainType.VALUE);
				return new ResponseEntity<>(kpiElasticValue, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------POST New KPI Elastic Value
	// ------------------------------------
	@PostMapping("/api/v1/kpidata/{kpiId}/elasticvalues")
	public ResponseEntity<Object> postKPIElasticValueV1(@PathVariable("kpiId") Long kpiId,
			@RequestBody KPIElasticValueDTO dto, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		KPIElasticValue kpiElasticValue = new KPIElasticValue(dto);
		
		logger.info("Requested postKPIElasticValueV1 id {} sourceRequest {}", kpiElasticValue.getId(), sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);
			
			List<Delegation> delegationsList = delegationService.findByElementIdByElementTypeNoPages(kpiData.getId().toString(), kpiData.getHighLevelType());
			
			
			List<DeviceGroupElement> deviceGroupElementList = deviceGroupElementService.getByUserAndElmtIdAndElmtType(kpiData.getUsername(),kpiId.toString(), "MyKPI");
			
			
			for (int i = 0; i < deviceGroupElementList.size(); i++) {
				delegationsList.addAll(delegationService.findByElementIdByElementTypeNoPages(deviceGroupElementList.get(i).getDeviceGroupId().toString(), "MyGroup"));
			}
			
			List<String> cryptedUserDelegations = delegationsList.stream()
					.filter(e -> e.getUsernameDelegated() != null)
					.map(e -> encryptor.encrypt(e.getUsernameDelegated()))
					.distinct()
					.collect(Collectors.toList());
			
			List<String> organizationDelegations = delegationsList.stream()
					.filter(e -> e.getGroupnameDelegated() != null)
					.map(e -> e.getGroupnameDelegated().substring(e.getGroupnameDelegated().indexOf('=') + 1, e.getGroupnameDelegated().indexOf(',')))
					.distinct()
					.collect(Collectors.toList());
			
			kpiElasticValue.setGroups(deviceGroupElementList.stream().map(e-> e.getDeviceGroupName()).collect(Collectors.toList()));
			kpiElasticValue.setUserDelegations(cryptedUserDelegations);
			kpiElasticValue.setOrganizationDelegations(organizationDelegations);
			if (!"[]".equals(kpiData.getOrganizations())) {
				kpiElasticValue.setOrganization(kpiData.getOrganizations().substring(kpiData.getOrganizations().indexOf('=') + 1, kpiData.getOrganizations().indexOf(',')));
			}
			kpiElasticValue.setSensorId(kpiId.toString());
			kpiElasticValue.setKpiId(kpiId.toString());
			kpiElasticValue.setNature(kpiData.getNature());
			kpiElasticValue.setSubNature(kpiData.getSubNature());
			kpiElasticValue.setValueName(kpiData.getValueName());
			kpiElasticValue.setValueType(kpiData.getValueType());
			kpiElasticValue.setValueUnit(kpiData.getValueUnit());
			kpiElasticValue.setUsername(encryptor.encrypt(kpiData.getUsername()));
			kpiElasticValue.setSrc("KPI");
			kpiElasticValue.setKind(kpiData.getHighLevelType());
			kpiElasticValue.setDeviceName(kpiId.toString());
			kpiElasticValue.setHealthinessCriteria("refresh_rate");
			kpiElasticValue.setValueRefreshRate("1800");
			kpiElasticValue.setDataType(kpiData.getDataType());
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			kpiElasticValue.setId(DigestUtils.sha256Hex(kpiElasticValue.getSensorId() + dateFormat.format(kpiElasticValue.getDateTime()) + Math.random()*1000 + kpiElasticValue.getValueName()).substring(0, 20));
			kpiValueService.saveKPIElasticValue(kpiElasticValue);
			logger.info("Posted kpielasticvalue {}", kpiElasticValue.getId());
			
			try {
				if (kpiElasticValue.getDateTime() != null) {
					if (kpiData.getLastDate() == null
							|| (kpiData.getLastDate() != null && kpiData.getLastDate().getTime() <= kpiElasticValue.getDateTime().getTime())) {
						kpiData.setLastDate(kpiElasticValue.getDateTime());
						if (kpiElasticValue.getValue() != null) {
							kpiData.setLastValue(kpiElasticValue.getValue().toString());
						} else if (kpiElasticValue.getValueStr() != null) {
							kpiData.setLastValue(kpiElasticValue.getValueStr());
						}
						if (kpiElasticValue.getLatitude() != null && !kpiElasticValue.getLatitude().equals("")) {
							kpiData.setLastLatitude(kpiElasticValue.getLatitude());
						}
						if (kpiElasticValue.getLongitude() != null && !kpiElasticValue.getLongitude().equals("")) {
							kpiData.setLastLongitude(kpiElasticValue.getLongitude());
						}
						kpiDataService.saveKPIData(kpiData);
					}
				} else {
					throw new DataNotValidException("Date format is wrong, please check or send the date as timestamp");
				}
			} catch (DataNotValidException d) {
				logger.warn(WRONG_ARGUMENTS, d);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), d.getMessage(), d,
						RequestHelper.getClientIpAddr(request));

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			return new ResponseEntity<>(kpiElasticValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
	
		// -------------------POST New KPI Value Array
		// ------------------------------------
		@PostMapping("/api/v1/kpidata/{kpiId}/elasticvalues/list")
		public ResponseEntity<Object> postKPIElasticValueArrayV1(@PathVariable("kpiId") Long kpiId,
				@RequestBody List<KPIElasticValueDTO> dtoList, @RequestParam(value = "sourceRequest") String sourceRequest,
				@RequestParam(value = "sourceId", required = false) String sourceId,
				@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
				HttpServletRequest request) {

			List<KPIElasticValue> kpiElasticValueList = dtoList.stream()
					.map(KPIElasticValue::new)
					.collect(Collectors.toList());
			
			logger.info("Requested postKPIElasticValueArrayV1 kpiId {} sourceRequest {}", kpiId, sourceRequest);

			try {

				KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

				if (kpiData == null) {
					logger.warn(WRONG_KPI_DATA);
					kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
							sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
							RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
							RequestHelper.getClientIpAddr(request));
					return new ResponseEntity<>(HttpStatus.NO_CONTENT);
				} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
						&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId),kpiData.getHighLevelType(), lang).getResult())) {
					throw new CredentialsException();
				}

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);

				
				kpiElasticValueList.sort((e1, e2) -> e1.getDateTime().compareTo(e2.getDateTime()));

				List<Delegation> delegationsList = delegationService.findByElementIdByElementTypeNoPages(kpiData.getId().toString(), kpiData.getHighLevelType());
				
				
				List<DeviceGroupElement> deviceGroupElementList = deviceGroupElementService.getByUserAndElmtIdAndElmtType(kpiData.getUsername(),kpiId.toString(), "MyKPI");
				
				
				for (int i = 0; i < deviceGroupElementList.size(); i++) {
					delegationsList.addAll(delegationService.findByElementIdByElementTypeNoPages(deviceGroupElementList.get(i).getDeviceGroupId().toString(), "MyGroup"));
				}
				
				List<String> cryptedUserDelegations = delegationsList.stream()
						.filter(e -> e.getUsernameDelegated() != null)
						.map(e -> encryptor.encrypt(e.getUsernameDelegated()))
						.distinct()
						.collect(Collectors.toList());
				
				List<String> organizationDelegations = delegationsList.stream()
						.filter(e -> e.getGroupnameDelegated() != null)
						.map(e -> e.getGroupnameDelegated().substring(e.getGroupnameDelegated().indexOf('=') + 1, e.getGroupnameDelegated().indexOf(',')))
						.distinct()
						.collect(Collectors.toList());
				
				
				for (int i = 0; i < kpiElasticValueList.size() - 1; i++) {
					kpiElasticValueList.get(i).setUserDelegations(cryptedUserDelegations);
					kpiElasticValueList.get(i).setOrganizationDelegations(organizationDelegations);
					kpiElasticValueList.get(i).setGroups(deviceGroupElementList.stream().map(e-> e.getDeviceGroupName()).collect(Collectors.toList()));
					if (!"[]".equals(kpiData.getOrganizations())) {
						kpiElasticValueList.get(i).setOrganization(kpiData.getOrganizations().substring(kpiData.getOrganizations().indexOf('=') + 1, kpiData.getOrganizations().indexOf(',')));
					}
					kpiElasticValueList.get(i).setKpiId(kpiId.toString());
					kpiElasticValueList.get(i).setSensorId(kpiId.toString());
					kpiElasticValueList.get(i).setNature(kpiData.getNature());
					kpiElasticValueList.get(i).setSubNature(kpiData.getSubNature());
					kpiElasticValueList.get(i).setValueName(kpiData.getValueName());
					kpiElasticValueList.get(i).setValueType(kpiData.getValueType());
					kpiElasticValueList.get(i).setValueUnit(kpiData.getValueUnit());
					kpiElasticValueList.get(i).setUsername(encryptor.encrypt(kpiData.getUsername()));
					kpiElasticValueList.get(i).setSrc("KPI");
					kpiElasticValueList.get(i).setKind(kpiData.getHighLevelType());
					kpiElasticValueList.get(i).setDeviceName(kpiId.toString());
					kpiElasticValueList.get(i).setHealthinessCriteria("refresh_rate");
					kpiElasticValueList.get(i).setValueRefreshRate("1800");
					kpiElasticValueList.get(i).setDataType(kpiData.getDataType());
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					kpiElasticValueList.get(i).setId(
							DigestUtils.sha256Hex(kpiElasticValueList.get(i).getSensorId() + dateFormat.format(kpiElasticValueList.get(i).getDateTime()) + Math.random()*1000 + kpiElasticValueList.get(i).getValueName()).substring(0, 20));
				}

				List<KPIElasticValue> listInsertedKPIElsaticValue = kpiValueService.saveKPIElasticValueList(kpiElasticValueList.subList(0, kpiElasticValueList.size() - 1));
				listInsertedKPIElsaticValue.add((KPIElasticValue) postKPIElasticValueV1(kpiId, dtoList.get(dtoList.size() - 1),
						sourceRequest, sourceId, lang, request).getBody());

				return new ResponseEntity<>(listInsertedKPIElsaticValue, HttpStatus.OK);
			} catch (CredentialsException d) {
				logger.warn(RIGHTS_EXCEPTION, d);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), d.getMessage(), d,
						RequestHelper.getClientIpAddr(request));

				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
			}
		}

	// -------------------PUT New KPI Value ------------------------------------
	@PutMapping("/api/v1/kpidata/{kpiId}/elasticvalues/{id}")
	public ResponseEntity<Object> putKPIElasticValueV1(@PathVariable("kpiId") Long kpiId, @PathVariable("id") String id,
			@RequestBody KPIElasticValueDTO dto, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		
		KPIElasticValue kpiElasticValue = new KPIElasticValue(dto);
		logger.info("Requested putKPIElasticValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIElasticValue oldKpiElasticValue = kpiValueService.getKPIElasticValueById(id, lang);
			if (oldKpiElasticValue == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), NO_DATA_FOUND, null,
						RequestHelper.getClientIpAddr(request));

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			try {
				if (kpiElasticValue.getDateTime() != null) {
					if (kpiData.getLastDate() == null
							|| (kpiData.getLastDate() != null && kpiData.getLastDate().getTime() <= kpiElasticValue.getDateTime().getTime())) {
						kpiData.setLastDate(kpiElasticValue.getDateTime());
						kpiData.setLastValue(kpiElasticValue.getValue().toString());
						if (kpiElasticValue.getLatitude() != null && !kpiElasticValue.getLatitude().equals("")) {
							kpiData.setLastLatitude(kpiElasticValue.getLatitude());
						}
						if (kpiElasticValue.getLongitude() != null && !kpiElasticValue.getLongitude().equals("")) {
							kpiData.setLastLongitude(kpiElasticValue.getLongitude());
						}
						kpiDataService.saveKPIData(kpiData);
					}
				} else {
					throw new DataNotValidException("Date format is wrong, please check or send the date as timestamp");
				}
			} catch (DataNotValidException d) {
				logger.warn(WRONG_ARGUMENTS, d);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), d.getMessage(), d,
						RequestHelper.getClientIpAddr(request));

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}

			kpiElasticValue.setId(oldKpiElasticValue.getId());
			kpiElasticValue.setUsername(encryptor.encrypt(kpiData.getUsername()));
			kpiElasticValue.setOrganization(kpiData.getOrganizations().substring(kpiData.getOrganizations().indexOf('=') + 1, kpiData.getOrganizations().indexOf(',')));
			kpiElasticValue.setGroups(deviceGroupElementService.getByUserAndElmtIdAndElmtType(kpiData.getUsername(),kpiId.toString(), kpiData.getHighLevelType()).stream().map(e-> e.getDeviceGroupName()).collect(Collectors.toList()));
			KPIElasticValue newKpiElasticValue = kpiValueService.saveKPIElasticValue(kpiElasticValue);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE);
			logger.info("Putted kpielasticvalue {}", kpiElasticValue.getId());
			return new ResponseEntity<>(newKpiElasticValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------PATCH New KPI Value ------------------------------------
	@PatchMapping("/api/v1/kpidata/{kpiId}/elasticvalues/{id}")
	public ResponseEntity<Object> patchKPIElasticValueV1(@PathVariable("kpiId") Long kpiId,
			@PathVariable("id") String id, @RequestBody Map<String, Object> fields,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested patchKPIElasticValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIElasticValue oldKpiElasticValue = kpiValueService.getKPIElasticValueById(id, lang);
			if (oldKpiElasticValue == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), NO_DATA_FOUND, null,
						RequestHelper.getClientIpAddr(request));

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			// Problem with cast of int to long, but the id is also present and it must be
			// the same
			fields.remove("id");
			fields.remove("kpiId");
			if (fields.get("dataTime") != null) {
				Date date = new Date();
				date.setTime((Long) fields.get("dataTime"));
				fields.put("dataTime", date);
			}
			if (fields.get("value") != null) {
				try {
					fields.put("value", Float.valueOf(fields.get("value").toString()));
				} catch (NumberFormatException e) {
					fields.put("value_str", Float.valueOf(fields.get("value").toString()));
					fields.remove("value");
				}
			}
			// Map key is field name, v is value
			fields.forEach((k, v) -> {
				// use reflection to get field k on manager and set it to value k
				Field field = ReflectionUtils.findField(KPIElasticValue.class, k);

				if (field != null && v != null) {
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, oldKpiElasticValue, (field.getType()).cast(v));
				}
			});

			try {
				if (oldKpiElasticValue.getDateTime() != null) {
					if (kpiData.getLastDate() == null
							|| (kpiData.getLastDate() != null && kpiData.getLastDate().getTime() <= oldKpiElasticValue.getDateTime().getTime())) {
						kpiData.setLastDate(oldKpiElasticValue.getDateTime());
						kpiData.setLastValue(oldKpiElasticValue.getValue().toString());
						if (oldKpiElasticValue.getLatitude() != null && !oldKpiElasticValue.getLatitude().equals("")) {
							kpiData.setLastLatitude(oldKpiElasticValue.getLatitude());
						}
						if (oldKpiElasticValue.getLongitude() != null
								&& !oldKpiElasticValue.getLongitude().equals("")) {
							kpiData.setLastLongitude(oldKpiElasticValue.getLongitude());
						}
						kpiDataService.saveKPIData(kpiData);
					}
				} else {
					throw new DataNotValidException("Date format is wrong, please check or send the date as timestamp");
				}
			} catch (DataNotValidException d) {
				logger.warn(WRONG_ARGUMENTS, d);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), d.getMessage(), d,
						RequestHelper.getClientIpAddr(request));

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
			}
			oldKpiElasticValue.setUsername(encryptor.encrypt(kpiData.getUsername()));
			oldKpiElasticValue.setOrganization(kpiData.getOrganizations().substring(kpiData.getOrganizations().indexOf('=') + 1, kpiData.getOrganizations().indexOf(',')));
			oldKpiElasticValue.setGroups(deviceGroupElementService.getByUserAndElmtIdAndElmtType(kpiData.getUsername(),kpiId.toString(), kpiData.getHighLevelType()).stream().map(e-> e.getDeviceGroupName()).collect(Collectors.toList()));
			KPIElasticValue newKpiElasticValue = kpiValueService.saveKPIElasticValue(oldKpiElasticValue);
			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, Long.valueOf(newKpiElasticValue.getSensorId()), ActivityAccessType.WRITE,
					KPIActivityDomainType.VALUE);

			logger.info("Patched kpivalue {}", newKpiElasticValue.getId());
			return new ResponseEntity<>(newKpiElasticValue, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.WRITE, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}

	// -------------------DELETE New KPI Value ------------------------------------
	@DeleteMapping("/api/v1/kpidata/{kpiId}/elasticvalues/{id}")
	public ResponseEntity<Object> deleteKPIElasticValueV1(@PathVariable("kpiId") Long kpiId,
			@PathVariable("id") String id, @RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			HttpServletRequest request) {

		logger.info("Requested deleteKPIElasticValueV1 id {} sourceRequest {}", id, sourceRequest);

		try {

			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			KPIElasticValue kpiElasticValueToDelete = kpiValueService.getKPIElasticValueById(id, lang);
			if (kpiElasticValueToDelete == null) {
				logger.info(NO_DATA_FOUND);

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), NO_DATA_FOUND, null,
						RequestHelper.getClientIpAddr(request));

				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			try {
				List<KPIElasticValue> lastKpiElasticValues = kpiValueService.findBySensorIdNoPagesWithLimit(kpiId, null,
						null, null, 2, lang);
				if (!lastKpiElasticValues.isEmpty() && lastKpiElasticValues.size() == 2
						&& lastKpiElasticValues.get(0).getId().equals(id)) {
					kpiData.setLastDate(lastKpiElasticValues.get(1).getDateTime());
					kpiData.setLastValue(lastKpiElasticValues.get(1).getValue().toString());
					kpiDataService.saveKPIData(kpiData);
				} else if (!lastKpiElasticValues.isEmpty() && lastKpiElasticValues.size() == 1
						&& lastKpiElasticValues.get(0).getId().equals(id)) {
					kpiData.setLastDate(null);
					kpiData.setLastValue(null);
					kpiData.setLastLatitude(null);
					kpiData.setLastLongitude(null);
					kpiDataService.saveKPIData(kpiData);
				}
			} catch (NoSuchMessageException | DataNotValidException e) {
				logger.warn("Problem setting last longitude, latitude", e);
			}

			kpiValueService.deleteKPIElasticValue(id);

			kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
					sourceId, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE);
			logger.info("Deleted kpivalue {}", id);
			return new ResponseEntity<>(kpiElasticValueToDelete, HttpStatus.OK);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		}
	}
	
	// -------------------DELETE All KPI Value ------------------------------------
			@DeleteMapping("/api/v1/kpidata/{kpiId}/elasticvalues")
			public ResponseEntity<Object> deleteKAllPIElasticValuesV1(@PathVariable("kpiId") Long kpiId,
					@RequestParam(value = "sourceRequest") String sourceRequest,
					@RequestParam(value = "sourceId", required = false) String sourceId,
					@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
					HttpServletRequest request) {

				logger.info("Requested deleteKAllPIElasticValuesV1 kpiId {} sourceRequest {}", kpiId, sourceRequest);

				try {

					KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);

					if (kpiData == null) {
						logger.warn(WRONG_KPI_DATA);
						kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
								sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
								RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
								RequestHelper.getClientIpAddr(request));
						return new ResponseEntity<>(HttpStatus.NO_CONTENT);
					} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
							&& !Boolean.TRUE.equals(accessService.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(),lang).getResult())) {
						throw new CredentialsException();
					}

					kpiData.setLastDate(null);
					kpiData.setLastValue(null);
					kpiData.setLastLatitude(null);
					kpiData.setLastLongitude(null);
					kpiDataService.saveKPIData(kpiData);
						
					List<KPIElasticValue> kpiValueList = kpiValueService.deleteKPIElasticValuesOfKpiId(kpiId);

					logger.info("Deleted all kpielaticValues of kpiId {}", kpiId);
					return new ResponseEntity<>(kpiValueList, HttpStatus.OK);
				} catch (CredentialsException d) {
					logger.warn(RIGHTS_EXCEPTION, d);

					kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
							sourceRequest, kpiId, ActivityAccessType.DELETE, KPIActivityDomainType.VALUE,
							RequestHelper.getUrl(request), d.getMessage(), d,
							RequestHelper.getClientIpAddr(request));

					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
				}
			}

	// -------------------GET ALL KPI Value Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/elasticvalues")
	public ResponseEntity<Object> getAllKPIElasticValueV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "date_time") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false) Integer first,
			@RequestParam(value = "last", required = false) Integer last, HttpServletRequest request) {

		logger.info(
				"Requested getAllKPIElasticValueV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}

			Page<KPIElasticValue> pageKpiValue = null;
			List<KPIElasticValue> listKpiValue = null;
			if (pageNumber != -1) {
				if (searchKey.equals("")) {
					pageKpiValue = kpiValueService.findBySensorId(kpiId,
							PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiValue = kpiValueService.findBySensorIdFiltered(kpiId, searchKey,
							PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (searchKey == null || searchKey.equals("")) {
					if (from == null && last == null && to == null && first == null) {
						listKpiValue = kpiValueService.findBySensorIdNoPages(kpiId);
					} else {
						listKpiValue = kpiValueService.findBySensorIdNoPagesWithLimit(kpiId, from, to, first, last,
								lang);
					}
				} else {
					listKpiValue = kpiValueService.findBySensorIdFilteredNoPages(kpiId, searchKey);
				}
			}

			if (pageKpiValue == null && listKpiValue == null) {
				logger.info("No value data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), "No value data found", null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiValue != null) {
				logger.info("Returning KpiValuepage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<>(pageKpiValue, HttpStatus.OK);
			} else {
				logger.info("Returning KpiValuelist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<>(listKpiValue, HttpStatus.OK);
			}
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | DataNotValidException | NoSuchMessageException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value of PUBLIC KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/elasticvalues")
	public ResponseEntity<Object> getAllKPIElasticValueOfPublicKPIV1Pageable(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "pageNumber", required = false, defaultValue = "-1") int pageNumber,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
			@RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
			@RequestParam(value = "sortBy", required = false, defaultValue = "insert_time") String sortBy,
			@RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
			@RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date to,
			@RequestParam(value = "first", required = false) Integer first,
			@RequestParam(value = "last", required = false) Integer last, HttpServletRequest request) {

		logger.info(
				"Requested getAllKPIElasticValueOfPublicKPIV1Pageable pageNumber {} pageSize {} sortDirection {} sortBy {} searchKey {} kpiId {}",
				pageNumber, pageSize, sortDirection, sortBy, searchKey, kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}

			Page<KPIElasticValue> pageKpiValue = null;
			List<KPIElasticValue> listKpiValue = null;
			if (pageNumber != -1) {
				if (searchKey.equals("")) {
					pageKpiValue = kpiValueService.findBySensorId(kpiId,
							PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				} else {
					pageKpiValue = kpiValueService.findBySensorIdFiltered(kpiId, searchKey,
							PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(sortDirection), sortBy)));
				}
			} else {
				if (searchKey.equals("")) {
					if (from == null && last == null && to == null && first == null) {
						listKpiValue = kpiValueService.findBySensorIdNoPages(kpiId);
					} else {
						listKpiValue = kpiValueService.findBySensorIdNoPagesWithLimit(kpiId, from, to, first, last,
								lang);
					}
				} else {
					listKpiValue = kpiValueService.findBySensorIdFilteredNoPages(kpiId, searchKey);
				}
			}

			if (pageKpiValue == null && listKpiValue == null) {
				logger.info("No value data found");

				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE,
						RequestHelper.getUrl(request), "No value data found", null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (pageKpiValue != null) {
				logger.info("Returning KpiValuepage ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<>(pageKpiValue, HttpStatus.OK);
			} else {
				logger.info("Returning KpiValuelist ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUE);

				return new ResponseEntity<>(listKpiValue, HttpStatus.OK);
			}

		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException | DataNotValidException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUE,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Dates of PUBLIC KPI Pageable
	// ---------------------------------
	@GetMapping("/api/v1/public/kpidata/{kpiId}/elasticvalues/dates")
	public ResponseEntity<Object> getDistinctKPIElasticValuesDateOfPublicKPIV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "checkCoordinates", required = false, defaultValue = "true") boolean checkCoordinates,
			HttpServletRequest request) {

		logger.info("Requested getDistinctKPIElasticValuesDateOfPublicKPIV1 kpiId {}", kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, true);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (kpiData.getOwnership().equals("private") || !kpiData.getOwnership().equals("public")) {
				throw new CredentialsException();
			}
			
			List<String> listKpiElasticValueDate = kpiValueService.getKPIElasticValueDates(kpiId, checkCoordinates);

			if (listKpiElasticValueDate != null) {
				logger.info("Returning KpiValuesDatesList ");

				kpiActivityService.saveActivityFromUsername("PUBLIC", sourceRequest, sourceId, kpiId,
						ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES);

				return new ResponseEntity<>(listKpiElasticValueDate, HttpStatus.OK);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername("PUBLIC", sourceRequest, kpiId,
					ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}

	// -------------------GET ALL KPI Value Dates Pageable
	// ---------------------------------
	@GetMapping("/api/v1/kpidata/{kpiId}/elasticvalues/dates")
	public ResponseEntity<Object> getDistinctKPIElasticValuesDateV1(@PathVariable("kpiId") Long kpiId,
			@RequestParam(value = "sourceRequest") String sourceRequest,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(value = "lang", required = false, defaultValue = "en") Locale lang,
			@RequestParam(value = "checkCoordinates", required = false, defaultValue = "true") boolean checkCoordinates,
			HttpServletRequest request) {

		logger.info("Requested getDistinctKPIElasticValuesDateV1 kpiId {}", kpiId);

		try {
			KPIData kpiData = kpiDataService.getKPIDataById(kpiId, lang, false);
			if (kpiData == null) {
				logger.warn(WRONG_KPI_DATA);
				kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
						sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
						RequestHelper.getUrl(request), WRONG_KPI_DATA, null,
						RequestHelper.getClientIpAddr(request));
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			} else if (!kpiData.getUsername().equalsIgnoreCase(credentialService.getLoggedUsername(lang))
					&& !Boolean.TRUE.equals(accessService
							.checkAccessFromApp(Long.toString(kpiId), kpiData.getHighLevelType(), lang).getResult())) {
				throw new CredentialsException();
			}
			List<String> listKpiElasticValueDate = kpiValueService.getKPIElasticValueDates(kpiId, checkCoordinates);

			if (listKpiElasticValueDate != null) {
				logger.info("Returning KpiValuesDatesList ");

				kpiActivityService.saveActivityFromUsername(credentialService.getLoggedUsername(lang), sourceRequest,
						sourceId, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES);

				return new ResponseEntity<>(listKpiElasticValueDate, HttpStatus.OK);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (CredentialsException d) {
			logger.warn(RIGHTS_EXCEPTION, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((Object) d.getMessage());
		} catch (IllegalArgumentException | NoSuchMessageException d) {
			logger.warn(WRONG_ARGUMENTS, d);

			kpiActivityService.saveActivityViolationFromUsername(credentialService.getLoggedUsername(lang),
					sourceRequest, kpiId, ActivityAccessType.READ, KPIActivityDomainType.VALUEDATES,
					RequestHelper.getUrl(request), d.getMessage(), d,
					RequestHelper.getClientIpAddr(request));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) d.getMessage());
		}
	}
	

}