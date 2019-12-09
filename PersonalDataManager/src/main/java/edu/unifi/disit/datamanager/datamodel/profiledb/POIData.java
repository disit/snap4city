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
package edu.unifi.disit.datamanager.datamodel.profiledb;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = POIDataSerializer.class)
@JsonDeserialize(using = POIDataDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class POIData {

	private KPIData kpidata;
	private List<KPIValue> listKPIValue;
	private List<KPIMetadata> listKPIMetadata;
	
	public POIData() {
		super();
		this.kpidata = new KPIData();
		this.listKPIMetadata = new ArrayList<>();
		this.listKPIValue = new ArrayList<>();
	}

	public POIData(KPIData kpidata, List<KPIValue> listKPIValue, List<KPIMetadata> listKPIMetadata) {
		super();
		this.kpidata = kpidata;
		this.listKPIValue = listKPIValue;
		this.listKPIMetadata = listKPIMetadata;
	}
	
	public POIData(KPIData kpidata) {
		this(kpidata, new ArrayList<KPIValue>(), new ArrayList<KPIMetadata>());
	}

	public POIData(KPIData kpidata, List<KPIMetadata> listKPIMetadata) {
		this(kpidata, new ArrayList<KPIValue>(), listKPIMetadata);
	}

	public KPIData getKpidata() {
		return kpidata;
	}

	public void setKpidata(KPIData kpidata) {
		this.kpidata = kpidata;
	}

	public List<KPIValue> getListKPIValue() {
		return listKPIValue;
	}

	public void setListKPIValue(List<KPIValue> listKPIValue) {
		this.listKPIValue = listKPIValue;
	}

	public List<KPIMetadata> getListKPIMetadata() {
		return listKPIMetadata;
	}

	public void setListKPIMetadata(List<KPIMetadata> listKPIMetadata) {
		this.listKPIMetadata = listKPIMetadata;
	}

}