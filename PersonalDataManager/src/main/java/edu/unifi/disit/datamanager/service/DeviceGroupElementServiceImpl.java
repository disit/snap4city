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
package edu.unifi.disit.datamanager.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroup;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElement;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElementDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIDataDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.datamodel.sensors.Sensor;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import java.util.Arrays;

@Service
public class DeviceGroupElementServiceImpl implements IDeviceGroupElementService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	DeviceGroupElementDAO deviceGroupElementRepository;

	@Autowired
	DeviceGroupDAO deviceGroupRepository;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Autowired
	KPIDataDAO kpiDataRepo;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	ICredentialsService credentialsService;

	@Autowired
	ISensorService sensorService;

	@Autowired
	private HttpServletRequest request;

	@PersistenceContext
	private EntityManager entityManager;

	@Value("${grpsensors.datasource.url}")
	private String sensorApiUrl;

	@Override
	public Page<DeviceGroupElement> findByDeviceGroupId(Long grpId, PageRequest pageRequest)
			throws CredentialsException, IOException {
		logger.debug("findByDeviceGroupId INVOKED on grpId {}", grpId);
		Page<DeviceGroupElement> elmts = deviceGroupElementRepository.findByDeviceGroupIdAndDeleteTimeIsNull(grpId,pageRequest);
		Iterator<DeviceGroupElement> it = elmts.iterator();
		String sensorsToFix = "";
		while (it.hasNext()) {
			DeviceGroupElement elmt = it.next();
			try {
                            if (Arrays.asList("MyKPI", "MyData", "MyPOI").contains(elmt.getElementType())) {
                                    if (kpiDataRepo.findById(Long.valueOf(elmt.getElementId())).orElse(new KPIData())
                                                    .getDeleteTime() != null)
                                            it.remove();
                            } else if (!"Sensor".equals(elmt.getElementType())) {
                                    if (ownershipRepo.findByElementIdAndDeletedIsNull(elmt.getElementId()).get(0).getDeleted() != null)
                                            it.remove();
                            } else {
                                    if (!sensorsToFix.isEmpty())
                                            sensorsToFix += ","; //
                                    sensorsToFix += elmt.getElementId(); //
                            }
                        } catch(Exception e) { e.printStackTrace(System.out); }
		}
		if (!sensorsToFix.isEmpty()) {
			HashMap<String, Sensor> sensors = getSensors(sensorsToFix); //
			it = elmts.iterator();
			while (it.hasNext()) {
				DeviceGroupElement elmt = it.next();
				if ("Sensor".equals(elmt.getElementType())) {
					Sensor sensor = sensors.get(elmt.getElementId());
					if (sensor == null) {
						it.remove();
					} else {
						String elmtName = sensor.getDeviceType() + " " + sensor.getDeviceName() + " " + sensor.getValueName();
						elmt.setElementName(elmtName.replaceAll("_", " "));
						elmt.setUsername(sensor.getDeviceOwner());
					}
				}
			}
		}
		return elmts; //return new PageImpl<>(elmts, pageRequest, elmts.size());                
	}

	@Override
	public List<DeviceGroupElement> findByDeviceGroupIdNoPages(Long grpId) throws CredentialsException, IOException {
		logger.debug("findByDeviceGroupNoPages INVOKED on grpId {}", grpId);
		List<DeviceGroupElement> elmts = deviceGroupElementRepository.findByDeviceGroupIdAndDeleteTimeIsNull(grpId);
		Iterator<DeviceGroupElement> it = elmts.iterator();
		String sensorsToFix = "";
		while (it.hasNext()) {
			DeviceGroupElement elmt = it.next();
			try {
                            if (Arrays.asList("MyKPI", "MyData", "MyPOI").contains(elmt.getElementType())) {
                                    if (kpiDataRepo.findById(Long.valueOf(elmt.getElementId())).orElse(new KPIData())
                                                    .getDeleteTime() != null)
                                            it.remove();
                            } else if (!"Sensor".equals(elmt.getElementType())) {
                                    if (ownershipRepo.findByElementIdAndDeletedIsNull(elmt.getElementId()).get(0).getDeleted() != null)
                                            it.remove();
                            } else {
                                    if (!sensorsToFix.isEmpty())
                                            sensorsToFix += ","; //
                                    sensorsToFix += elmt.getElementId(); //
                            }
                        }
                        catch(Exception e) { e.printStackTrace(System.out); }
		}
		if (!sensorsToFix.isEmpty()) {
			HashMap<String, Sensor> sensors = getSensors(sensorsToFix); //
			it = elmts.iterator();
			while (it.hasNext()) {
				DeviceGroupElement elmt = it.next();
				if ("Sensor".equals(elmt.getElementType())) {
					Sensor sensor = sensors.get(elmt.getElementId());
					if (sensor == null) {
						it.remove();
					} else {
						// elmt.setElementName(sensor.getDeviceType()+" "+sensor.getDeviceName()+" "+sensor.getValueName());
						String elmtName = sensor.getDeviceType() + " " + sensor.getDeviceName() + " " + sensor.getValueName();
						elmt.setElementName(elmtName.replaceAll("_", " "));
						elmt.setUsername(sensor.getDeviceOwner());
					}
				}
			}
		}
		return elmts;
	}

	@Override
	public Set<String> getAvailElmtTypesToAdd(String username) {
		List<Ownership> ownerships = ownershipRepo.findByUsernameAndDeletedIsNull(username);
		Set<String> elementTypes = new HashSet<>();
		for (Ownership o : ownerships)
			if ((!"Service Graph".equals(o.getElmtTypeLbl4Grps())) && !"Service URI".equals(o.getElmtTypeLbl4Grps()))
				elementTypes.add(o.getElmtTypeLbl4Grps());
		List<KPIData> kpiData = kpiDataRepo.findByUsernameAndHighLevelTypeIsNotNullAndDeleteTimeIsNull(username);
		// if(kpiData != null && !kpiData.isEmpty()) elementTypes.add("MyKPI");
		for (KPIData d : kpiData)
			elementTypes.add(d.getHighLevelType());
		return elementTypes;
	}

	@Override
	public Set<String> getAllElmtTypes() {
		List<Ownership> ownerships = ownershipRepo.findByDeletedIsNull();
		Set<String> elementTypes = new HashSet<>();
		for (Ownership o : ownerships)
			if ((!"Service Graph".equals(o.getElmtTypeLbl4Grps())) && !"Service URI".equals(o.getElmtTypeLbl4Grps()))
				elementTypes.add(o.getElmtTypeLbl4Grps());
		List<KPIData> kpiData = kpiDataRepo.findByHighLevelTypeIsNotNullAndDeleteTimeIsNull();
		for (KPIData d : kpiData)
			elementTypes.add(d.getHighLevelType());
		return elementTypes;
	}

	@Override
	public Set<Object> getAllItems(String elmtType) {
		HashSet<KPIData> kset = new HashSet<>(kpiDataRepo.findByHighLevelTypeAndDeleteTimeIsNull(elmtType));
		HashSet<Ownership> oset = new HashSet<>(ownershipRepo.findByElmtTypeLbl4GrpsAndDeletedIsNull(elmtType));
		if (!kset.isEmpty()) {
			ArrayList<KPIData> sset = new ArrayList<>(kset);
			Collections.sort(sset, new MyKPISorter());
			return new HashSet<>(sset);
		} else {
			ArrayList<Ownership> sset = new ArrayList<>(oset);
			Collections.sort(sset, new MyItemSorter());
			return new HashSet<>(sset);
		}
	}

	@Override
	public List<DeviceGroupElement> getByUserAndElmtIdAndElmtType(String username, String elementId, String elementType) {
		if (!"Sensor".equals(remap(elementType))) {
			if (credentialsService.isRoot(null)) {
				return deviceGroupElementRepository.findByElementIdAndElementTypeAndDeleteTimeIsNull(elementId, remap(elementType));
			} else {
				return deviceGroupElementRepository.findByUsernameAndElementIdAndElementTypeAndDeleteTimeIsNull(username, elementId, remap(elementType));
			}
		} else {
			try {
				Sensor sensor = getSensor(elementId);
				if (credentialsService.isRoot(null) || sensor.getDeviceOwner().equals(username)) {
					List<DeviceGroupElement> list = deviceGroupElementRepository.findByElementIdAndElementTypeAndDeleteTimeIsNull(elementId, remap(elementType));
					Iterator<DeviceGroupElement> i = list.iterator();
					while (i.hasNext()) {
						DeviceGroupElement e = i.next();
						e.setUsername(sensor.getDeviceOwner());
						String elmtName = sensor.getDeviceType() + " " + sensor.getDeviceName() + " " + sensor.getValueName();
						e.setElementName(elmtName.replaceAll("_", " "));
						DeviceGroup g = deviceGroupRepository.findById(e.getDeviceGroupId()).orElse(new DeviceGroup());
						if (g.getDeleteTime() != null)
							i.remove();
					}
					return list;
				}
			} catch (Exception e) {
			}
		}
		return new ArrayList<>();
	}

	private String remap(String elementType) {
		switch (elementType) {
		case "IOT Device":
			return "IOTID";
		case "IOT App":
			return "AppID";
		case "Data Analytics":
			return "DAAppID";
		case "IOT Broker":
			return "BrokerID";
		case "Web Scraping":
			return "PortiaID";
		case "IOT Device Model":
			return "ModelID";
		case "Heatmap":
			return "HeatmapID";
		case "Service Graph":
			return "ServiceGraphID";
		case "Dashboard":
			return "DashboardID";
		case "Service URI":
			return "ServiceURI";
		default:
			return elementType;
		}
	}

	class MyKPISorter implements Comparator<KPIData> {
		// Used for sorting in ascending order of
		// roll number
		public int compare(KPIData a, KPIData b) {
			return a.getValueName().compareTo(b.getValueName());
		}
	}

	class MyItemSorter implements Comparator<Ownership> {
		// Used for sorting in ascending order of
		// roll number
		public int compare(Ownership a, Ownership b) {
			if (a.getElementName() != null && b.getElementName() != null) {
				return a.getElementName().compareTo(b.getElementName());
			} else {
				return a.getId().compareTo(b.getId());
			}
		}
	}

	@Override
	public HashSet<Object> getAvailItemsToAdd(String username, String elmtType) {
		HashSet<KPIData> kset = new HashSet<>(kpiDataRepo.findByUsernameAndHighLevelTypeAndDeleteTimeIsNull(username, elmtType));
		HashSet<Ownership> oset = new HashSet<>(ownershipRepo.findByUsernameAndElmtTypeLbl4GrpsAndDeletedIsNull(username, elmtType));
		if (!kset.isEmpty()) {
			ArrayList<KPIData> sset = new ArrayList<>(kset);
			Collections.sort(sset, new MyKPISorter());
			return new HashSet<>(sset);
		} else {
			ArrayList<Ownership> sset = new ArrayList<>(oset);
			Collections.sort(sset, new MyItemSorter());
			return new HashSet<>(sset);
		}
	}

	@Override
	public List<DeviceGroupElement> addElmtsToGrp(Long grpId, List<DeviceGroupElement> elements) {
		try {
			DeviceGroup grp = deviceGroupRepository.findById(grpId).orElse(new DeviceGroup());
			grp.setUpdateTime(new Date());
			deviceGroupRepository.save(grp);
		} catch (Exception e) {
		}
		return deviceGroupElementRepository.saveAll(elements);
	}

	@Override
	public Page<DeviceGroupElement> findByDeviceGroupIdFiltered(Long grpId, String searchKey, PageRequest pageRequest)
			throws IOException {
		logger.debug("findByDeviceGroupIdFiltered INVOKED on grpId {} searchKey {}", grpId, searchKey);
		List<DeviceGroupElement> elmts = deviceGroupElementRepository
				.findByDeviceGroupIdAndDeleteTimeIsNullFiltered(grpId, searchKey);
		Iterator<DeviceGroupElement> it = elmts.iterator();
		while (it.hasNext()) {
			DeviceGroupElement elmt = it.next();
			try {
                            if (Arrays.asList("MyKPI", "MyData", "MyPOI").contains(elmt.getElementType())) {
                                    if (kpiDataRepo.findById(Long.valueOf(elmt.getElementId())).orElse(new KPIData())
                                                    .getDeleteTime() != null)
                                            it.remove();
                            } else if (!"Sensor".equals(elmt.getElementType())) {
                                    if (ownershipRepo.findByElementIdAndDeletedIsNull(elmt.getElementId()).get(0).getDeleted() != null)
                                            it.remove();
                            } else {
                                    it.remove();
                            }
                        } catch(Exception e) { e.printStackTrace(System.out); }
		}
		String sensorsToChk = "";
		List<DeviceGroupElement> forSensors = deviceGroupElementRepository
				.findByDeviceGroupIdAndDeleteTimeIsNull(grpId);
		Iterator<DeviceGroupElement> fsit = forSensors.iterator();
		while (fsit.hasNext()) {
			DeviceGroupElement elmt = fsit.next();
			if ("Sensor".equals(elmt.getElementType())) {
				if (!sensorsToChk.isEmpty())
					sensorsToChk += ",";
				sensorsToChk += elmt.getElementId();
			}
		}
		if (!sensorsToChk.isEmpty()) {
			HashMap<String, Sensor> sensors = getSensors(sensorsToChk, searchKey); //
			fsit = forSensors.iterator();
			while (fsit.hasNext()) {
				DeviceGroupElement elmt = fsit.next();
				if ("Sensor".equals(elmt.getElementType())) {
					Sensor sensor = sensors.get(elmt.getElementId());
					if (sensor != null) {
						// elmt.setElementName(sensor.getDeviceType()+" "+sensor.getDeviceName()+" "+sensor.getValueName());
						String elmtName = sensor.getDeviceType() + " " + sensor.getDeviceName() + " " + sensor.getValueName();
						elmt.setElementName(elmtName.replaceAll("_", " "));
						elmt.setUsername(sensor.getDeviceOwner());
						elmts.add(elmt);
					}
				}
			}
		}
		//return new PageImpl<>(elmts, pageRequest, elmts.size());
                int start = pageRequest.getPageNumber()*pageRequest.getPageSize();
                int end = (start + pageRequest.getPageSize()) > elmts.size() ? elmts.size() : (start + pageRequest.getPageSize());
                Page<DeviceGroupElement> pages = new PageImpl<>(elmts.subList(start, end), pageRequest, elmts.size());
                return pages; 
	}

	@Override
	public List<DeviceGroupElement> findByDeviceGroupIdNoPagesFiltered(Long grpId, String searchKey)
			throws IOException {
		logger.debug("findByDeviceGroupIdNoPagesFiltered INVOKED on grpId {} searchKey {}", grpId, searchKey);
		List<DeviceGroupElement> elmts = deviceGroupElementRepository.findByDeviceGroupIdAndDeleteTimeIsNullFiltered(grpId, searchKey);
		Iterator<DeviceGroupElement> it = elmts.iterator();
		while (it.hasNext()) {
			DeviceGroupElement elmt = it.next();
			try { 
                            if (Arrays.asList("MyKPI", "MyData", "MyPOI").contains(elmt.getElementType())) {
                                    if (kpiDataRepo.findById(Long.valueOf(elmt.getElementId())).orElse(new KPIData())
                                                    .getDeleteTime() != null)
                                            it.remove();
                            } else if (!"Sensor".equals(elmt.getElementType())) {
                                    if (ownershipRepo.findByElementIdAndDeletedIsNull(elmt.getElementId()).get(0).getDeleted() != null)
                                            it.remove();
                            } else {
                                    it.remove();
                            }
                        } catch(Exception e) { e.printStackTrace(System.out); }
		}
		String sensorsToChk = "";
		List<DeviceGroupElement> forSensors = deviceGroupElementRepository
				.findByDeviceGroupIdAndDeleteTimeIsNull(grpId);
		Iterator<DeviceGroupElement> fsit = forSensors.iterator();
		while (fsit.hasNext()) {
			DeviceGroupElement elmt = fsit.next();
			if ("Sensor".equals(elmt.getElementType())) {
				if (!sensorsToChk.isEmpty())
					sensorsToChk += ",";
				sensorsToChk += elmt.getElementId();
			}
		}
		if (!sensorsToChk.isEmpty()) {
			HashMap<String, Sensor> sensors = getSensors(sensorsToChk, searchKey); //
			fsit = forSensors.iterator();
			while (fsit.hasNext()) {
				DeviceGroupElement elmt = fsit.next();
				if ("Sensor".equals(elmt.getElementType())) {
					Sensor sensor = sensors.get(elmt.getElementId());
					if (sensor != null) {
						// elmt.setElementName(sensor.getDeviceType()+" "+sensor.getDeviceName()+" "+sensor.getValueName());
						String elmtName = sensor.getDeviceType() + " " + sensor.getDeviceName() + " " + sensor.getValueName();
						elmt.setElementName(elmtName.replaceAll("_", " "));
						elmt.setUsername(sensor.getDeviceOwner());
						elmts.add(elmt);
					}
				}
			}
		}
		return elmts;
	}

	@Override
	public DeviceGroupElement getDeviceGroupElementById(Long id)
			throws CredentialsException, IOException {
		logger.debug("getDeviceGroupElementById INVOKED on id {}", id);
		DeviceGroupElement elmt = deviceGroupElementRepository.findById(id).orElse(new DeviceGroupElement());
		if ("Sensor".equals(elmt.getElementType())) {
			Sensor sensor = getSensor(elmt.getElementId());
			if (sensor == null) {
				return null;
			} else {
				elmt.setElementName(sensor.getDeviceType() + " " + sensor.getDeviceName() + " " + sensor.getValueName());
				elmt.setUsername(sensor.getDeviceOwner());
			}
		}
		return elmt;
	}

	private Sensor getSensor(String sensorId) throws IOException {

		String response = sensorService.getSensors(request.getParameter("accessToken"), null, null, null, sensorId);
		/*
		 * URL url = new URL(request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+"/api/v1/sensors?accessToken="+request.getParameter("accessToken")+"&id="+sensorId);
		 * logger.debug("CALL TO SENSORS API FROM getSensor(String sensorId) IN DeviceGroupElementServiceImpl {}",request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+
		 * "/api/v1/sensors?accessToken="+request.getParameter("accessToken")+"&id="+sensorId); HttpURLConnection huc = (HttpURLConnection) url.openConnection(); huc.setRequestMethod("GET"); int responseCode = huc.getResponseCode();
		 * if(responseCode == 404) return null; BufferedReader in = new BufferedReader(new InputStreamReader(huc.getInputStream())); String response = ""; String inputLine; while ((inputLine = in.readLine()) != null) response+=inputLine;
		 * in.close();
		 */

		ObjectMapper mapper = new ObjectMapper();
		Sensor[] validSensors = mapper.readValue(response, Sensor[].class);
		return validSensors[0];

	}

	private HashMap<String, Sensor> getSensors(String sensorIds) throws IOException {

		String response = sensorService.getSensors(request.getParameter("accessToken"), null, null, null, sensorIds);
		/*
		 * URL url = new URL(request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+"/api/v1/sensors?accessToken="+request.getParameter("accessToken")+"&id="+sensorIds);
		 * logger.debug("CALL TO SENSORS API FROM getSensors(String sensorIds) IN DeviceGroupElementServiceImpl {}",request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+
		 * "/api/v1/sensors?accessToken="+request.getParameter("accessToken")+"&id="+sensorIds); HttpURLConnection huc = (HttpURLConnection) url.openConnection(); huc.setRequestMethod("GET"); int responseCode = huc.getResponseCode();
		 * if(responseCode == 404) return map; BufferedReader in = new BufferedReader(new InputStreamReader(huc.getInputStream())); String response = ""; String inputLine; while ((inputLine = in.readLine()) != null) response+=inputLine;
		 * in.close();
		 */

		HashMap<String, Sensor> map = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		Sensor[] validSensors = mapper.readValue(response, Sensor[].class);
		for (Sensor validSensor : validSensors) {
			map.put(String.valueOf(validSensor.getId()), validSensor);
		}
		return map;

	}

	private HashMap<String, Sensor> getSensors(String sensorIds, String search) throws IOException {

		String response = sensorService.getSensors(request.getParameter("accessToken"), null, null, search, sensorIds);
		/*
		 * URL url = new URL(request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+"/api/v1/sensors?accessToken="+request.getParameter("accessToken")+"&id="+sensorIds+"&search="+search);
		 * logger.debug("CALL TO SENSORS API FROM getSensors(String sensorIds, String search) IN DeviceGroupElementServiceImpl {}",request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+
		 * "/api/v1/sensors?accessToken="+request.getParameter("accessToken")+"&id="+sensorIds+"&search="+search); HttpURLConnection huc = (HttpURLConnection) url.openConnection(); huc.setRequestMethod("GET"); int responseCode =
		 * huc.getResponseCode(); if(responseCode == 404) return map; BufferedReader in = new BufferedReader(new InputStreamReader(huc.getInputStream())); String response = ""; String inputLine; while ((inputLine = in.readLine()) != null)
		 * response+=inputLine; in.close();
		 */

		HashMap<String, Sensor> map = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		Sensor[] validSensors = mapper.readValue(response, Sensor[].class);
		for (Sensor validSensor : validSensors) {
			map.put(String.valueOf(validSensor.getId()), validSensor);
		}
		return map;

	}
}