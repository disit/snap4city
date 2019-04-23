/* IoT Ingestion (II).
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
package edu.unifi.disit.iotIngestion.datamodel;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.unifi.disit.iotIngestion.datamodel.PaxCounter.Address;
import edu.unifi.disit.iotIngestion.datamodel.PaxCounter.Bluetooth;
import edu.unifi.disit.iotIngestion.datamodel.PaxCounter.DateObserved;
import edu.unifi.disit.iotIngestion.datamodel.PaxCounter.Location;
import edu.unifi.disit.iotIngestion.datamodel.PaxCounter.WiFi;

public class PaxCounterIngested {

	private static final Logger logger = LogManager.getLogger();

	String id;
	String type;
	String dateObserved;
	Integer WiFi;
	Integer Bluetooth;
	LocationReceived location;
	AddressReceived address;

	public PaxCounterIngested() {
		super();
	}

	public PaxCounterIngested(String id, String type, String dateObserved, Integer WiFi, Integer bluetooth, LocationReceived location, AddressReceived address) {
		super();
		this.id = id;
		this.type = type;
		this.dateObserved = dateObserved;
		this.WiFi = WiFi;
		this.Bluetooth = bluetooth;
		this.location = location;
		this.address = address;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDateObserved() {
		return dateObserved;
	}

	public void setDataObserved(String dateObserved) {
		this.dateObserved = dateObserved;
	}

	@JsonProperty("WiFi")
	public Integer getWiFi() {
		return WiFi;
	}

	@JsonProperty("WiFi")
	public void setWiFi(Integer wiFi) {
		this.WiFi = wiFi;
	}

	@JsonProperty("Bluetooth")
	public Integer getBluetooth() {
		return Bluetooth;
	}

	@JsonProperty("Bluetooth")
	public void setBluetooth(Integer bluetooth) {
		this.Bluetooth = bluetooth;
	}

	public LocationReceived getLocation() {
		return location;
	}

	public void setLocation(LocationReceived location) {
		this.location = location;
	}

	public AddressReceived getAddress() {
		return address;
	}

	public void setAddress(AddressReceived address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "PaxCounterReceived [id=" + id + ", type=" + type + ", dateObserved=" + dateObserved + ", WiFi=" + WiFi + ", Bluetooth=" + Bluetooth + ", location=" + location + ", address=" + address + "]";
	}

	public PaxCounter toPaxCounter() {
		PaxCounter toreturn = new PaxCounter();

		toreturn.setId(this.id);
		toreturn.setType(this.type);

		toreturn.setDateObserved(new DateObserved(this.getDateObserved()));
		toreturn.setWiFi(new WiFi(this.getWiFi()));
		toreturn.setBluetooth(new Bluetooth(this.getBluetooth()));

		if ((this.getLocation() != null) && (this.getLocation().getCoordinates() != null))
			toreturn.setLocation(new Location(this.getLocation().getCoordinates()));
		else
			logger.error("Location is missing");

		toreturn.setAddress(new Address(this.getAddress().getAddressCountry(), this.getAddress().getAddressLocality(), this.getAddress().getStreetAddress()));

		return toreturn;
	}

	public class LocationReceived {

		Float[] coordinates;
		String type;

		public LocationReceived() {
			super();
		}

		public LocationReceived(Float[] coordinates, String type) {
			super();
			this.coordinates = coordinates;
			this.type = type;
		}

		public Float[] getCoordinates() {
			return coordinates;
		}

		public void setCoordinates(Float[] coordinates) {
			this.coordinates = coordinates;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return "LocationReceived [coordinates=" + Arrays.toString(coordinates) + ", type=" + type + "]";
		}
	}

	public class AddressReceived {

		String addressCountry;
		String addressLocality;
		String streetAddress;

		public AddressReceived() {
			super();
		}

		public AddressReceived(String addressCountry, String addressLocality, String streetAddress) {
			super();
			this.addressCountry = addressCountry;
			this.addressLocality = addressLocality;
			this.streetAddress = streetAddress;
		}

		public String getAddressCountry() {
			return addressCountry;
		}

		public void setAddressCountry(String addressCountry) {
			this.addressCountry = addressCountry;
		}

		public String getAddressLocality() {
			return addressLocality;
		}

		public void setAddressLocality(String addressLocality) {
			this.addressLocality = addressLocality;
		}

		public String getStreetAddress() {
			return streetAddress;
		}

		public void setStreetAddress(String streetAddress) {
			this.streetAddress = streetAddress;
		}

		@Override
		public String toString() {
			return "AddressReceived [addressCountry=" + addressCountry + ", addressLocality=" + addressLocality + ", streetAddress=" + streetAddress + "]";
		}
	}
}