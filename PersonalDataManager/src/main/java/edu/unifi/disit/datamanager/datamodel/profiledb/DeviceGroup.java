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
package edu.unifi.disit.datamanager.datamodel.profiledb;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.unifi.disit.datamanager.service.DeviceGroupElementServiceImpl;
import edu.unifi.disit.datamanager.service.IDeviceGroupElementService;
import org.hibernate.annotations.Formula;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;

@JsonSerialize(using = DeviceGroupSerializer.class)
@JsonDeserialize(using = DeviceGroupDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "devicegroup")
public class DeviceGroup {
        
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "insert_time")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date insertTime;

    @Column(name = "delete_time")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date deleteTime;

    @Column(name = "update_time")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date updateTime;

    @Column(name = "ownership")
    private String ownership;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "high_level_type")
    private String highLevelType;
    
    @Column(name = "organizations")
    private String organizations;
    
    @Formula("(select count(*) from devicegroupelement e left join kpidata kpi on e.elementId = kpi.id and e.elementType = 'MyKPI' left join ownership o on e.elementId = o.elementId and e.elementType <> 'MyKPI' where e.device_group_id = id and e.delete_time is null and kpi.delete_time is null and o.deleted is null)")
    private int size;

    // default with nothing
    public DeviceGroup() {
            super();
    }

    // default with everything
    public DeviceGroup(Long id, Date insertTime, Date updateTime, Date deleteTime, String ownership, String name, String description, String username, String highLevelType) {
            super();
            this.id = id;
            this.insertTime = insertTime;
            this.updateTime = updateTime;
            this.deleteTime = deleteTime;
            this.ownership = ownership;
            this.name = name;
            this.description = description;
            this.username = username;
            this.highLevelType = highLevelType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(Date insertTime) {
        this.insertTime = insertTime;
    }

    public Date getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(Date deleteTime) {
        this.deleteTime = deleteTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getOwnership() {
        return ownership;
    }

    public void setOwnership(String ownership) {
        this.ownership = ownership;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHighLevelType() {
        return highLevelType;
    }

    public void setHighLevelType(String highLevelType) {
        this.highLevelType = highLevelType;
    }

    public String getOrganizations() {
        return organizations;
    }

    public void setOrganizations(String organizations) {
        this.organizations = organizations;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    
    @Override
    public String toString() {
            return "DeviceGroup [id=" + id + ", insertTime=" + insertTime + ", updateTime=" + updateTime + ", deleteTime=" + deleteTime + ", ownership=" + ownership + ", name=" + name + ", description=" + description + "]";
    }

}
