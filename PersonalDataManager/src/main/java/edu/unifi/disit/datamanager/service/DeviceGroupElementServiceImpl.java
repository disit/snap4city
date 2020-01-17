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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroup;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElement;
import edu.unifi.disit.datamanager.datamodel.profiledb.DeviceGroupElementDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIData;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIDataDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.Ownership;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import java.util.Iterator;

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

	@PersistenceContext 
	private EntityManager entityManager;  

        @Override
        public Page<DeviceGroupElement> findByDeviceGroupId(Long grpId, PageRequest pageRequest) throws CredentialsException {
            logger.debug("findByDeviceGroupId INVOKED on grpId {}", grpId);
	    Page<DeviceGroupElement> elmts = deviceGroupElementRepository.findByDeviceGroupIdAndDeleteTimeIsNull(grpId, pageRequest);
            Iterator<DeviceGroupElement> it = elmts.iterator();
            while( it.hasNext() ) {
              DeviceGroupElement elmt = it.next();
              if( "MyKPI".equals(elmt.getElementType()) ) 
              {
                  if(kpiDataRepo.findOne(Long.valueOf(elmt.getElementId())).getDeleteTime() != null) it.remove();
              }
              else {
                  if(ownershipRepo.findByElementId(elmt.getElementId()).get(0).getDeleted() != null) it.remove();
              }
            }
            return elmts;
        }

        @Override
        public List<DeviceGroupElement> findByDeviceGroupIdNoPages(Long grpId) throws CredentialsException {
            logger.debug("findByDeviceGroupNoPages INVOKED on grpId {}", grpId);
	    List<DeviceGroupElement> elmts = deviceGroupElementRepository.findByDeviceGroupIdAndDeleteTimeIsNull(grpId);
            Iterator<DeviceGroupElement> it = elmts.iterator();
            while( it.hasNext() ) {
              DeviceGroupElement elmt = it.next();
              if( "MyKPI".equals(elmt.getElementType()) ) 
              {
                  if(kpiDataRepo.findOne(Long.valueOf(elmt.getElementId())).getDeleteTime() != null) it.remove();
              }
              else {
                  if(ownershipRepo.findByElementId(elmt.getElementId()).get(0).getDeleted() != null) it.remove();
              }
            }
            return elmts;
        }

    @Override
    public Set<String> getAvailElmtTypesToAdd(String username) {
        List<Ownership> ownerships = ownershipRepo.findByUsernameAndDeletedIsNull(username);
        Set<String> elementTypes = new HashSet<String>();
        for(Ownership o: ownerships) elementTypes.add(o.getElmtTypeLbl4Grps());
        List<KPIData> kpiData = kpiDataRepo.findByUsernameAndDeleteTimeIsNull(username);
        //if(kpiData != null && !kpiData.isEmpty()) elementTypes.add("MyKPI");
        for(KPIData d: kpiData) elementTypes.add(d.getHighLevelType());
        return elementTypes;        
    }

    @Override
    public Set<String> getAllElmtTypes() {
        List<Ownership> ownerships = ownershipRepo.findByDeletedIsNull();
        Set<String> elementTypes = new HashSet<String>();
        for(Ownership o: ownerships) elementTypes.add(o.getElmtTypeLbl4Grps());
        List<KPIData> kpiData = kpiDataRepo.findByDeleteTimeIsNull();
        for(KPIData d: kpiData) elementTypes.add(d.getHighLevelType());
        return elementTypes;       
    }

    @Override
    public Set<Object> getAllItems(String elmtType) {
        HashSet<KPIData> kset = new HashSet<>(kpiDataRepo.findByHighLevelTypeAndDeleteTimeIsNull(elmtType));  
        HashSet<Ownership> oset = new HashSet<>(ownershipRepo.findByElmtTypeLbl4GrpsAndDeletedIsNull(elmtType)); 
        if(!kset.isEmpty()) {
            ArrayList<KPIData> sset = new ArrayList<>(kset);
            Collections.sort(sset,new MyKPISorter());
            return new HashSet<>(sset);
        }
        else {
            ArrayList<Ownership> sset = new ArrayList<>(oset);
            Collections.sort(sset,new MyItemSorter());
            return new HashSet<>(sset);
        }
    }
    
    class MyKPISorter implements Comparator<KPIData> 
    { 
        // Used for sorting in ascending order of 
        // roll number 
        public int compare(KPIData a, KPIData b) 
        { 
            return a.getValueName().compareTo(b.getValueName());
        } 
    } 
    
    class MyItemSorter implements Comparator<Ownership> 
    { 
        // Used for sorting in ascending order of 
        // roll number 
        public int compare(Ownership a, Ownership b) 
        { 
            if(a.getElementName() != null && b.getElementName() != null) {
                return a.getElementName().compareTo(b.getElementName());
            }
            else {
                return a.getId().compareTo(b.getId());
            }
        } 
    } 
    
    @Override
    public HashSet<Object> getAvailItemsToAdd(String username, String elmtType) {
        HashSet<KPIData> kset = new HashSet<>(kpiDataRepo.findByUsernameAndHighLevelTypeAndDeleteTimeIsNull(username, elmtType));  
        HashSet<Ownership> oset = new HashSet<>(ownershipRepo.findByUsernameAndElmtTypeLbl4GrpsAndDeletedIsNull(username, elmtType)); 
        if(!kset.isEmpty()) {
            ArrayList<KPIData> sset = new ArrayList<>(kset);
            Collections.sort(sset,new MyKPISorter());
            return new HashSet<>(sset);
        }
        else {
            ArrayList<Ownership> sset = new ArrayList<>(oset);
            Collections.sort(sset,new MyItemSorter());
            return new HashSet<>(sset);
        }
    }

    @Override
    public List<DeviceGroupElement> addElmtsToGrp(Long grpId, List<DeviceGroupElement> elements) {
        try {
            DeviceGroup grp = deviceGroupRepository.findOne(grpId);
            grp.setUpdateTime(new Date());
            deviceGroupRepository.save(grp);
        }
        catch(Exception e) {}
        return deviceGroupElementRepository.save(elements);        
    }

    @Override
    public Page<DeviceGroupElement> findByDeviceGroupIdFiltered(Long grpId, String searchKey, PageRequest pageRequest) {
        logger.debug("findByDeviceGroupIdFiltered INVOKED on grpId {} searchKey {}", grpId, searchKey);
        Page<DeviceGroupElement> elmts = deviceGroupElementRepository.findByDeviceGroupIdAndDeleteTimeIsNullFiltered(grpId, pageRequest, searchKey);
        Iterator<DeviceGroupElement> it = elmts.iterator();
        while( it.hasNext() ) {
          DeviceGroupElement elmt = it.next();
          if( "MyKPI".equals(elmt.getElementType()) ) 
          {
              if(kpiDataRepo.findOne(Long.valueOf(elmt.getElementId())).getDeleteTime() != null) it.remove();
          }
          else {
              if(ownershipRepo.findByElementId(elmt.getElementId()).get(0).getDeleted() != null) it.remove();
          }
        }
        return elmts;
    }

    @Override
    public List<DeviceGroupElement> findByDeviceGroupIdNoPagesFiltered(Long grpId, String searchKey) {
        logger.debug("findByDeviceGroupIdNoPagesFiltered INVOKED on grpId {} searchKey {}", grpId, searchKey);
	List<DeviceGroupElement> elmts = deviceGroupElementRepository.findByDeviceGroupIdAndDeleteTimeIsNullFiltered(grpId, searchKey);
        Iterator<DeviceGroupElement> it = elmts.iterator();
        while( it.hasNext() ) {
          DeviceGroupElement elmt = it.next();
          if( "MyKPI".equals(elmt.getElementType()) ) 
          {
              if(kpiDataRepo.findOne(Long.valueOf(elmt.getElementId())).getDeleteTime() != null) it.remove();
          }
          else {
              if(ownershipRepo.findByElementId(elmt.getElementId()).get(0).getDeleted() != null) it.remove();
          }
        }
        return elmts;
    }

    @Override
    public DeviceGroupElement getDeviceGroupElementById(Long id) throws  CredentialsException {
            logger.debug("getDeviceGroupElementById INVOKED on id {}", id);
            return deviceGroupElementRepository.findOne(id);
    }
}