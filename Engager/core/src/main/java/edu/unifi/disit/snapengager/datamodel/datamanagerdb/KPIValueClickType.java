/* Snap4City Engager (SE)
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
package edu.unifi.disit.snapengager.datamodel.datamanagerdb;

public enum KPIValueClickType {

	// search
	discoverCity("discoverCity"), aroundYouMenu("aroundYouMenu"), events("events"), suggestionsNearYou("suggestionsNearYou"), textSearch("textSearch"), poi("poi"), healtCareMenu("healtCareMenu"),

	// transport
	transportMenu("transportMenu"), ticketSale("ticketSale"), vehicleRental("vehicleRental"), bikeSharing("bikeSharing"), carPosition("carPosition"), navigator("navigator"), parking("parking"), pathFinder("pathFinder"), timetable(
			"timetable"), tpl("tpl"), tunnelAndFerry("tunnelAndFerry"), cyclePaths("cyclePaths"), fuelStation("fuelStation"),

	// enviroment_all
	airQualityHeatmap("airQualityHeatmap"), WeatherHeatmap("WeatherHeatmap"), WeatherForecast("WeatherForecast"),

	// enviroment antwerp
	heatmapAirQualityPM10Average2HourAntwerp("heatmapAirQualityPM10Average2HourAntwerp"), heatmapAirQualityPM2_5Average2HourAntwerp("heatmapAirQualityPM2_5Average2HourAntwerp"), heatmapAirQualityNO2Average2HourAntwerp(
			"heatmapAirQualityNO2Average2HourAntwerp"), heatmapAirQualitySO2Average2HourAntwerp("heatmapAirQualitySO2Average2HourAntwerp"), heatmapAirQualityO3Average2HourAntwerp(
					"heatmapAirQualityO3Average2HourAntwerp"), heatmapEAQI1hourAverageAntwerp("heatmapEAQI1hourAverageAntwerp"), heatmapAirQualityNOAverage2HourAntwerp(
							"heatmapAirQualityNOAverage2HourAntwerp"), heatmapAirTemperatureAverage2HourAntwerp("heatmapAirTemperatureAverage2HourAntwerp"), heatmapAirHumidityAverage2HourAntwerp(
									"heatmapAirHumidityAverage2HourAntwerp"),

	// enviroment helsinki
	heatmapGRALheatmapHelsinki3mPM("heatmapGRALheatmapHelsinki3mPM"), heatmapEnfuserHelsinkiHighDensityPM25(
			"heatmapEnfuserHelsinkiHighDensityPM25"), heatmapEnfuserHelsinkiHighDensityPM10(
					"heatmapEnfuserHelsinkiHighDensityPM10"), heatmapEnfuserHelsinkiEnfuserAirQualityIndex("heatmapEnfuserHelsinkiEnfuserAirQualityIndex"), heatmapEAQI1hourAverageHelsinki(
							"heatmapEAQI1hourAverageHelsinki"), heatmapLAeqAverage2HourHelsinki("heatmapLAeqAverage2HourHelsinki"), heatmapAirQualityAQIAverage2HourHelsinki(
									"heatmapAirQualityAQIAverage2HourHelsinki"), heatmapAirQualityNO2Average2HourHelsinki("heatmapAirQualityNO2Average2HourHelsinki"), heatmapAirQualityPM2_5Average2HourHelsinki(
											"heatmapAirQualityPM2_5Average2HourHelsinki"), heatmapAirQualityPM10Average2HourHelsinki("heatmapAirQualityPM10Average2HourHelsinki"), heatmapAirHumidityAverage2HourHelsinki(
													"heatmapAirHumidityAverage2HourHelsinki"), heatmapAirTemperatureAverage2HourHelsinki("heatmapAirTemperatureAverage2HourHelsinki"), AirQualityPM10Average2HourHelsinkiJ(
															"heatmapAirQualityPM10Average2HourHelsinkiJ"), AirQualityPM2_5Average2HourHelsinkiJ(
																	"heatmapAirQualityPM2_5Average2HourHelsinkiJ"), EAQI1hourAverageHelsinkiJ("heatmapEAQI1hourAverageHelsinkiJ"),

	// enviroment toscana
	FlorenceAccidentsDensity("heatmapFlorenceAccidentsDensity"), WindSpeedAverage2HourFlorence("heatmapWindSpeedAverage2HourFlorence"), AirTemperatureAverage2HourFlorence(
			"heatmapAirTemperatureAverage2HourFlorence"), AirHumidityAverage2HourFlorence(
					"heatmapAirHumidityAverage2HourFlorence"), PM10Average24HourFlorence("heatmapPM10Average24HourFlorence"), PM2_5Average24HourFlorence(
							"heatmapPM2_5Average24HourFlorence"), NO2Average24HourFlorence(
									"heatmapNO2Average24HourFlorence"), COAverage24HourFlorence("heatmapCOAverage24HourFlorence"), BenzeneAverage24HourFlorence("heatmapBenzeneAverage24HourFlorence"),

	// assistance
	userExperiences("userExperiences"), personalAssistant("personalAssistant"), helpContact("helpContact"), myActivity("myActivity"), snap4cityForum("snap4cityForum"), information("information"), snap4cityPortal("snap4cityPortal");

	private final String text;

	private KPIValueClickType(final String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}
}