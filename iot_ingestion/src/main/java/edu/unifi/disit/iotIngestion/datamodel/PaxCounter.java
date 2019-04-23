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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaxCounter {

	String id;
	String type;
	DateObserved dateObserved;
	WiFi WiFi;
	Bluetooth Bluetooth;
	Location location;
	Address address;

	public PaxCounter() {
		super();
	}

	public PaxCounter(String id, String type, DateObserved dateObserved, WiFi wiFi, Bluetooth bluetooth, Location location, Address address) {
		super();
		this.id = id;
		this.type = type;
		this.dateObserved = dateObserved;
		this.WiFi = wiFi;
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

	public DateObserved getDateObserved() {
		return dateObserved;
	}

	public void setDateObserved(DateObserved dateObserved) {
		this.dateObserved = dateObserved;
	}

	@JsonProperty("WiFi")
	public WiFi getWiFi() {
		return WiFi;
	}

	@JsonProperty("WiFi")
	public void setWiFi(WiFi wiFi) {
		this.WiFi = wiFi;
	}

	@JsonProperty("Bluetooth")
	public Bluetooth getBluetooth() {
		return Bluetooth;
	}

	@JsonProperty("Bluetooth")
	public void setBluetooth(Bluetooth bluetooth) {
		this.Bluetooth = bluetooth;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	static class DateObserved {

		String type = "Text";
		String value;

		public DateObserved() {
			super();
		}

		public DateObserved(String value) {
			super();
			this.value = value;
		}

		public DateObserved(String type, String value) {
			super();
			this.type = type;
			this.value = value;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "DateObserved [type=" + type + ", value=" + value + "]";
		}

	}

	static class WiFi {

		String type = "Number";
		Integer value;

		public WiFi() {
			super();
		}

		public WiFi(Integer value) {
			super();
			this.value = value;
		}

		public WiFi(String type, Integer value) {
			super();
			this.type = type;
			this.value = value;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "WiFi [type=" + type + ", value=" + value + "]";
		}
	}

	static class Bluetooth {

		String type = "Number";
		Integer value;

		public Bluetooth() {
			super();
		}

		public Bluetooth(Integer value) {
			super();
			this.value = value;
		}

		public Bluetooth(String type, Integer value) {
			super();
			this.type = type;
			this.value = value;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "Bluetooth [type=" + type + ", value=" + value + "]";
		}
	}

	static class Location {

		String type = "StructuredValue";
		LocationValue value;

		public Location() {
			super();
		}

		public Location(Float[] coordinates) {
			super();
			this.value = new LocationValue(coordinates);
		}

		public Location(String type, LocationValue value) {
			super();
			this.type = type;
			this.value = value;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public LocationValue getValue() {
			return value;
		}

		public void setValue(LocationValue value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "Location [type=" + type + ", value=" + value + "]";
		}

	}

	static class LocationValue {

		String type = "Point";
		Float[] coordinates;

		public LocationValue() {
			super();
		}

		public LocationValue(Float[] coordinates) {
			super();
			this.coordinates = coordinates;
		}

		public LocationValue(String type, Float[] coordinates) {
			super();
			this.type = type;
			this.coordinates = coordinates;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Float[] getCoordinates() {
			return coordinates;
		}

		public void setCoordinates(Float[] coordinates) {
			this.coordinates = coordinates;
		}

		@Override
		public String toString() {
			return "LocationValue [type=" + type + ", coordinates=" + Arrays.toString(coordinates) + "]";
		}
	}

	static class Address {

		String type = "StructuredValue";
		AddressValue value;

		public Address() {
			super();
		}

		public Address(String addressCountry, String addressLocality, String streetAddress) {
			this.value = new AddressValue(addressCountry, addressLocality, streetAddress);
		}

		public Address(String type, AddressValue value) {
			super();
			this.type = type;
			this.value = value;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public AddressValue getValue() {
			return value;
		}

		public void setValue(AddressValue value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "Address [type=" + type + ", value=" + value + "]";
		}
	}

	static class AddressValue {

		String addressCountry;
		String addressLocality;
		String streetAddress;

		public AddressValue() {
			super();
		}

		public AddressValue(String addressCountry, String addressLocality, String streetAddress) {
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
			return "AddressValue [addressCountry=" + addressCountry + ", addressLocality=" + addressLocality + ", streetAddress=" + streetAddress + "]";
		}
	}
}