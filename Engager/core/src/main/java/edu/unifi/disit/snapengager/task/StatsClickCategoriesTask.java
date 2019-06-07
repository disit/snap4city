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
package edu.unifi.disit.snapengager.task;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import edu.unifi.disit.snap4city.engager_utils.LanguageType;
import edu.unifi.disit.snap4city.engager_utils.OrganizationType;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIData;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIDataDAO;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIDataType;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIValueClickType;
import edu.unifi.disit.snapengager.datamodel.datamanagerdb.KPIValueDAO;
import edu.unifi.disit.snapengager.datamodel.profiledb.StatsSeries;
import edu.unifi.disit.snapengager.datamodel.profiledb.StatsSeriesDAO;
import edu.unifi.disit.snapengager.exception.CredentialsException;
import edu.unifi.disit.snapengager.exception.UserprofileException;
import edu.unifi.disit.snapengager.service.UserprofileService;

@EnableScheduling
@Component
public class StatsClickCategoriesTask implements SchedulingConfigurer {

	private static final Logger logger = LogManager.getLogger();

	@Value("${stats.categories.task.cron}")
	private String cronrefresh;

	@Autowired
	private KPIValueDAO kpivaluerepo;

	@Autowired
	private KPIDataDAO kpidatarepo;

	@Autowired
	private StatsSeriesDAO ssrepo;

	@Autowired
	private UserprofileService upservice;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug("-----------------------------------------------start execution for STATS CLICK CATEGORIES -----------------------------------------------");
					myTask();
				} catch (Exception e) {
					logger.error("Error catched {}", e);
				}
			}
		}, new Trigger() {
			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				CronTrigger trigger = new CronTrigger(cronrefresh);
				Date nextExec = trigger.nextExecutionTime(triggerContext);
				logger.debug("-----------------------------------------------next execution for STATS CLICK CATEGORIES will be fired at:{}-----------------------------------------------", nextExec);
				return nextExec;
			}
		});
	}

	private void myTask() throws UserprofileException, CredentialsException, IOException {
		Locale lang = new Locale("en");
		Hashtable<String, LanguageType> languages = upservice.getUserLanguage(lang);
		List<KPIData> kpidatas = kpidatarepo.findByValueNameContainingAndDeleteTimeIsNull("AppUsage");

		// retrieve old stats
		StatsSeries ssHelsinkiAssist = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.HELSINKI.toString(), "clickcategory", "Assistance");
		StatsSeries ssAntwerpAssist = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.ANTWERP.toString(), "clickcategory", "Assistance");
		StatsSeries ssToscanaAssist = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.TOSCANA.toString(), "clickcategory", "Assistance");
		StatsSeries ssAnyAssist = ssrepo.findTopByOrganizationIsNullAndTypeAndCategoryOrderByCreatedDesc("clickcategory", "Assistance");
		StatsSeries ssHelsinkiSearch = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.HELSINKI.toString(), "clickcategory", "Search");
		StatsSeries ssAntwerpSearch = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.ANTWERP.toString(), "clickcategory", "Search");
		StatsSeries ssToscanaSearch = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.TOSCANA.toString(), "clickcategory", "Search");
		StatsSeries ssAnySearch = ssrepo.findTopByOrganizationIsNullAndTypeAndCategoryOrderByCreatedDesc("clickcategory", "Search");
		StatsSeries ssHelsinkiTransp = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.HELSINKI.toString(), "clickcategory", "Transportation");
		StatsSeries ssAntwerpTransp = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.ANTWERP.toString(), "clickcategory", "Transportation");
		StatsSeries ssToscanaTransp = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.TOSCANA.toString(), "clickcategory", "Transportation");
		StatsSeries ssAnyTransp = ssrepo.findTopByOrganizationIsNullAndTypeAndCategoryOrderByCreatedDesc("clickcategory", "Transportation");
		StatsSeries ssHelsinkiEnv = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.HELSINKI.toString(), "clickcategory", "Enviroment");
		StatsSeries ssAntwerpEnv = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.ANTWERP.toString(), "clickcategory", "Enviroment");
		StatsSeries ssToscanaEnv = ssrepo.findTopByOrganizationAndTypeAndCategoryOrderByCreatedDesc(OrganizationType.TOSCANA.toString(), "clickcategory", "Enviroment");
		StatsSeries ssAnyEnv = ssrepo.findTopByOrganizationIsNullAndTypeAndCategoryOrderByCreatedDesc("clickcategory", "Enviroment");

		// retrieve last date
		Date lastInjested = ssHelsinkiAssist.getCreated(); // consider the first one

		logger.debug("last inj {}", lastInjested);

		for (KPIData kpidata : kpidatas) {

			logger.debug("elaborate {}", kpidata);

			LanguageType language = languages.get(kpidata.getUsername());

			if (language == null)
				language = LanguageType.ENG;

			Integer countSearch = kpivaluerepo.countByKpiIdAndInsertTimeAfterAndValue7(kpidata.getId(), lastInjested, KPIValueClickType.discoverCity.toString(), KPIValueClickType.aroundYouMenu.toString(),
					KPIValueClickType.events.toString(),
					KPIValueClickType.suggestionsNearYou.toString(), KPIValueClickType.textSearch.toString(), KPIValueClickType.poi.toString(), KPIValueClickType.healtCareMenu.toString()).intValue();
			Integer countTransp = kpivaluerepo.countByKpiIdAndInsertTimeAfterAndValue13(kpidata.getId(), lastInjested, KPIValueClickType.transportMenu.toString(),
					KPIValueClickType.ticketSale.toString(),
					KPIValueClickType.vehicleRental.toString(), KPIValueClickType.bikeSharing.toString(), KPIValueClickType.carPosition.toString(), KPIValueClickType.navigator.toString(), KPIValueClickType.parking.toString(),
					KPIValueClickType.pathFinder.toString(), KPIValueClickType.timetable.toString(), KPIValueClickType.tpl.toString(), KPIValueClickType.tunnelAndFerry.toString(), KPIValueClickType.cyclePaths.toString(),
					KPIValueClickType.fuelStation.toString()).intValue();
			Integer countEnviro_all = kpivaluerepo.countByKpiIdAndInsertTimeAfterAndValue3(kpidata.getId(), lastInjested, KPIValueClickType.airQualityHeatmap.toString(), KPIValueClickType.WeatherHeatmap.toString(),
					KPIValueClickType.WeatherForecast.toString()).intValue();
			Integer countEnviro_ant = kpivaluerepo.countByKpiIdAndInsertTimeAfterAndValue9(kpidata.getId(), lastInjested,
					KPIValueClickType.heatmapAirQualityPM10Average2HourAntwerp.toString(), KPIValueClickType.heatmapAirQualityPM2_5Average2HourAntwerp.toString(),
					KPIValueClickType.heatmapAirQualityNO2Average2HourAntwerp.toString(), KPIValueClickType.heatmapAirQualitySO2Average2HourAntwerp.toString(),
					KPIValueClickType.heatmapAirQualityO3Average2HourAntwerp.toString(), KPIValueClickType.heatmapEAQI1hourAverageAntwerp.toString(),
					KPIValueClickType.heatmapAirQualityNOAverage2HourAntwerp.toString(), KPIValueClickType.heatmapAirTemperatureAverage2HourAntwerp.toString(),
					KPIValueClickType.heatmapAirHumidityAverage2HourAntwerp.toString()).intValue();
			Integer countEnviro_hel = kpivaluerepo.countByKpiIdAndInsertTimeAfterAndValue15(kpidata.getId(), lastInjested,
					KPIValueClickType.heatmapGRALheatmapHelsinki3mPM.toString(), KPIValueClickType.heatmapEnfuserHelsinkiHighDensityPM25.toString(),
					KPIValueClickType.heatmapEnfuserHelsinkiHighDensityPM10.toString(), KPIValueClickType.heatmapEnfuserHelsinkiEnfuserAirQualityIndex.toString(),
					KPIValueClickType.heatmapEAQI1hourAverageHelsinki.toString(), KPIValueClickType.heatmapLAeqAverage2HourHelsinki.toString(),
					KPIValueClickType.heatmapAirQualityAQIAverage2HourHelsinki.toString(), KPIValueClickType.heatmapAirQualityNO2Average2HourHelsinki.toString(),
					KPIValueClickType.heatmapAirQualityPM2_5Average2HourHelsinki.toString(), KPIValueClickType.heatmapAirQualityPM10Average2HourHelsinki.toString(),
					KPIValueClickType.heatmapAirHumidityAverage2HourHelsinki.toString(), KPIValueClickType.heatmapAirTemperatureAverage2HourHelsinki.toString(),
					KPIValueClickType.AirQualityPM10Average2HourHelsinkiJ.toString(), KPIValueClickType.AirQualityPM2_5Average2HourHelsinkiJ.toString(),
					KPIValueClickType.EAQI1hourAverageHelsinkiJ.toString()).intValue();
			Integer countEnviro_toscany = kpivaluerepo.countByKpiIdAndInsertTimeAfterAndValue9(kpidata.getId(), lastInjested,
					KPIValueClickType.FlorenceAccidentsDensity.toString(), KPIValueClickType.WindSpeedAverage2HourFlorence.toString(),
					KPIValueClickType.AirTemperatureAverage2HourFlorence.toString(), KPIValueClickType.AirHumidityAverage2HourFlorence.toString(),
					KPIValueClickType.PM10Average24HourFlorence.toString(), KPIValueClickType.PM2_5Average24HourFlorence.toString(),
					KPIValueClickType.NO2Average24HourFlorence.toString(), KPIValueClickType.COAverage24HourFlorence.toString(),
					KPIValueClickType.BenzeneAverage24HourFlorence.toString()).intValue();
			Integer countAssist = kpivaluerepo
					.countByKpiIdAndInsertTimeAfterAndValue7(kpidata.getId(), lastInjested, KPIValueClickType.userExperiences.toString(), KPIValueClickType.personalAssistant.toString(),
							KPIValueClickType.helpContact.toString(),
							KPIValueClickType.myActivity.toString(), KPIValueClickType.snap4cityForum.toString(), KPIValueClickType.information.toString(), KPIValueClickType.snap4cityPortal.toString())
					.intValue();

			if (kpidata.getValueName().equalsIgnoreCase(KPIDataType.S4CHelsinkiAppUsage.toString())) {
				switch (language) {
				case ENG: {
					logger.debug("eng hel {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssHelsinkiAssist.addEng(countAssist);
					ssHelsinkiSearch.addEng(countSearch);
					ssHelsinkiTransp.addEng(countTransp);
					ssHelsinkiEnv.addEng(countEnviro_hel);
					ssHelsinkiEnv.addEng(countEnviro_all);
					break;
				}
				case ITA: {
					logger.debug("ita hel {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssHelsinkiAssist.addIta(countAssist);
					ssHelsinkiSearch.addIta(countSearch);
					ssHelsinkiTransp.addIta(countTransp);
					ssHelsinkiEnv.addIta(countEnviro_hel);
					ssHelsinkiEnv.addIta(countEnviro_all);
					break;
				}
				case DEU: {
					logger.debug("deu hel {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssHelsinkiAssist.addDeu(countAssist);
					ssHelsinkiSearch.addDeu(countSearch);
					ssHelsinkiTransp.addDeu(countTransp);
					ssHelsinkiEnv.addDeu(countEnviro_hel);
					ssHelsinkiEnv.addDeu(countEnviro_all);
					break;
				}
				case ESP: {
					logger.debug("esp hel {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssHelsinkiAssist.addEsp(countAssist);
					ssHelsinkiSearch.addEsp(countSearch);
					ssHelsinkiTransp.addEsp(countTransp);
					ssHelsinkiEnv.addEsp(countEnviro_hel);
					ssHelsinkiEnv.addEsp(countEnviro_all);
					break;
				}
				case FRA: {
					logger.debug("fra hel {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssHelsinkiAssist.addFra(countAssist);
					ssHelsinkiSearch.addFra(countSearch);
					ssHelsinkiTransp.addFra(countTransp);
					ssHelsinkiEnv.addFra(countEnviro_hel);
					ssHelsinkiEnv.addFra(countEnviro_all);
					break;
				}
				default: {
					logger.warn("language not recognized {}, ignoring", language);
				}
				}

			} else if (kpidata.getValueName().equalsIgnoreCase(KPIDataType.S4CAntwerpAppUsage.toString())) {

				switch (language) {
				case ENG: {
					logger.debug("eng ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssAntwerpAssist.addEng(countAssist);
					ssAntwerpSearch.addEng(countSearch);
					ssAntwerpTransp.addEng(countTransp);
					ssAntwerpEnv.addEng(countEnviro_ant);
					ssAntwerpEnv.addEng(countEnviro_all);
					break;
				}
				case ITA: {
					logger.debug("ita ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssAntwerpAssist.addIta(countAssist);
					ssAntwerpSearch.addIta(countSearch);
					ssAntwerpTransp.addIta(countTransp);
					ssAntwerpEnv.addIta(countEnviro_ant);
					ssAntwerpEnv.addIta(countEnviro_all);
					break;
				}
				case DEU: {
					logger.debug("deu ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssAntwerpAssist.addDeu(countAssist);
					ssAntwerpSearch.addDeu(countSearch);
					ssAntwerpTransp.addDeu(countTransp);
					ssAntwerpEnv.addDeu(countEnviro_ant);
					ssAntwerpEnv.addDeu(countEnviro_all);
					break;
				}
				case ESP: {
					logger.debug("esp ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssAntwerpAssist.addEsp(countAssist);
					ssAntwerpSearch.addEsp(countSearch);
					ssAntwerpTransp.addEsp(countTransp);
					ssAntwerpEnv.addEsp(countEnviro_ant);
					ssAntwerpEnv.addEsp(countEnviro_all);
					break;
				}
				case FRA: {
					logger.debug("fra ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssAntwerpAssist.addFra(countAssist);
					ssAntwerpSearch.addFra(countSearch);
					ssAntwerpTransp.addFra(countTransp);
					ssAntwerpEnv.addFra(countEnviro_ant);
					ssAntwerpEnv.addFra(countEnviro_all);
					break;
				}
				default: {
					logger.warn("language not recognized {}, ignoring", language);
				}
				}
			} else if (kpidata.getValueName().equalsIgnoreCase(KPIDataType.S4CTuscanyAppUsage.toString())) {

				switch (language) {
				case ENG: {
					logger.debug("eng ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssToscanaAssist.addEng(countAssist);
					ssToscanaSearch.addEng(countSearch);
					ssToscanaTransp.addEng(countTransp);
					ssToscanaEnv.addEng(countEnviro_toscany);
					ssToscanaEnv.addEng(countEnviro_all);
					break;
				}
				case ITA: {
					logger.debug("ita ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssToscanaAssist.addIta(countAssist);
					ssToscanaSearch.addIta(countSearch);
					ssToscanaTransp.addIta(countTransp);
					ssToscanaEnv.addIta(countEnviro_toscany);
					ssToscanaEnv.addIta(countEnviro_all);
					break;
				}
				case DEU: {
					logger.debug("deu ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssToscanaAssist.addDeu(countAssist);
					ssToscanaSearch.addDeu(countSearch);
					ssToscanaTransp.addDeu(countTransp);
					ssToscanaEnv.addDeu(countEnviro_toscany);
					ssToscanaEnv.addDeu(countEnviro_all);
					break;
				}
				case ESP: {
					logger.debug("esp ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssToscanaAssist.addEsp(countAssist);
					ssToscanaSearch.addEsp(countSearch);
					ssToscanaTransp.addEsp(countTransp);
					ssToscanaEnv.addEsp(countEnviro_toscany);
					ssToscanaEnv.addEsp(countEnviro_all);
					break;
				}
				case FRA: {
					logger.debug("fra ant {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

					ssToscanaAssist.addFra(countAssist);
					ssToscanaSearch.addFra(countSearch);
					ssToscanaTransp.addFra(countTransp);
					ssToscanaEnv.addFra(countEnviro_toscany);
					ssToscanaEnv.addFra(countEnviro_all);
					break;
				}
				default: {
					logger.warn("language not recognized {}, ignoring", language);
				}
				}
			}

			switch (language) {
			case ENG: {
				logger.debug("eng any {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

				ssAnyAssist.addEng(countAssist);
				ssAnySearch.addEng(countSearch);
				ssAnyTransp.addEng(countTransp);
				ssAnyEnv.addEng(countEnviro_ant);
				ssAnyEnv.addEng(countEnviro_hel);
				ssAnyEnv.addEng(countEnviro_toscany);
				ssAnyEnv.addEng(countEnviro_all);
				break;
			}
			case ITA: {
				logger.debug("ita any {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

				ssAnyAssist.addIta(countAssist);
				ssAnySearch.addIta(countSearch);
				ssAnyTransp.addIta(countTransp);
				ssAnyEnv.addIta(countEnviro_ant);
				ssAnyEnv.addIta(countEnviro_hel);
				ssAnyEnv.addIta(countEnviro_toscany);
				ssAnyEnv.addIta(countEnviro_all);
				break;
			}
			case DEU: {
				logger.debug("deu any {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

				ssAnyAssist.addDeu(countAssist);
				ssAnySearch.addDeu(countSearch);
				ssAnyTransp.addDeu(countTransp);
				ssAnyEnv.addDeu(countEnviro_ant);
				ssAnyEnv.addDeu(countEnviro_hel);
				ssAnyEnv.addDeu(countEnviro_toscany);
				ssAnyEnv.addDeu(countEnviro_all);
				break;
			}
			case ESP: {
				logger.debug("esp any {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

				ssAnyAssist.addEsp(countAssist);
				ssAnySearch.addEsp(countSearch);
				ssAnyTransp.addEsp(countTransp);
				ssAnyEnv.addEsp(countEnviro_ant);
				ssAnyEnv.addEsp(countEnviro_hel);
				ssAnyEnv.addEsp(countEnviro_toscany);
				ssAnyEnv.addEsp(countEnviro_all);
				break;
			}
			case FRA: {
				logger.debug("fra any {} {} {} {} {} {}", countSearch, countTransp, countEnviro_all, countEnviro_ant, countEnviro_hel, countAssist);

				ssAnyAssist.addFra(countAssist);
				ssAnySearch.addFra(countSearch);
				ssAnyTransp.addFra(countTransp);
				ssAnyEnv.addFra(countEnviro_ant);
				ssAnyEnv.addFra(countEnviro_hel);
				ssAnyEnv.addFra(countEnviro_toscany);
				ssAnyEnv.addFra(countEnviro_all);
				break;
			}
			default: {
				logger.warn("language not recognized {}, ignoring", language);
			}
			}
		}

		ssrepo.save(update(ssHelsinkiAssist));
		ssrepo.save(update(ssAntwerpAssist));
		ssrepo.save(update(ssToscanaAssist));
		ssrepo.save(update(ssAnyAssist));
		ssrepo.save(update(ssHelsinkiSearch));
		ssrepo.save(update(ssAntwerpSearch));
		ssrepo.save(update(ssToscanaSearch));
		ssrepo.save(update(ssAnySearch));
		ssrepo.save(update(ssHelsinkiTransp));
		ssrepo.save(update(ssAntwerpTransp));
		ssrepo.save(update(ssToscanaTransp));
		ssrepo.save(update(ssAnyTransp));
		ssrepo.save(update(ssHelsinkiEnv));
		ssrepo.save(update(ssAntwerpEnv));
		ssrepo.save(update(ssToscanaEnv));
		ssrepo.save(update(ssAnyEnv));
	}

	private StatsSeries update(StatsSeries ss) {
		ss.setId(null);
		ss.setCreated(new Date());
		return ss;
	}
}