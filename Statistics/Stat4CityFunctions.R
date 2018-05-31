# /* STAT4CITY FUNCTIONS CONTRIB SNAP4CITY USER
# Copyright (C) 2018 DISIT Lab http://www.disit.org - University of Florence
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>. */

#STAT4CITY FUNCTIONS

#' @get /descriptive
descriptive <- function(sensorTypeList, currentDate, daysNumber) {
  unlink(paste("~/Snap4City/Sensors Data/FinalDatasets", list.files("~/Snap4City/Sensors Data/FinalDatasets"), sep="/"))
  
  unlink(paste("~/Snap4City/Sensors Data/TrafficCSVFiles", list.files("~/Snap4City/Sensors Data/TrafficCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/TwitterCSVFiles", list.files("~/Snap4City/Sensors Data/TwitterCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/CarParkCSVFiles", list.files("~/Snap4City/Sensors Data/CarParkCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/WeatherCSVFiles", list.files("~/Snap4City/Sensors Data/WeatherCSVFiles"), sep="/"))
  
  unlink(paste("~/Snap4City/StatisticsOutput/Correlations", list.files("~/Snap4City/StatisticsOutput/Correlations"), sep="/"))
  unlink(paste("~/Snap4City/StatisticsOutput/MeanPerDayMoment", list.files("~/Snap4City/StatisticsOutput/MeanPerDayMoment"), sep="/"))
  unlink(paste("~/Snap4City/StatisticsOutput/StatisticsBySensors", list.files("~/Snap4City/StatisticsOutput/StatisticsBySensors"), sep="/"))

  currentDate = as.Date(currentDate)
  daysNumber = as.numeric(daysNumber)

  source('~/Snap4City/Snap4CityStatistics/UploadLibrariesFunction.R')
  packagesInstallation()
  
  if (length(grep("traffic", sensorTypeList)) !=  0){
    source('~/Snap4City/Snap4CityStatistics/TrafficQueryFunction.R')
    TrafficDataQuery(currentDate, daysNumber)
  }
  
  if (length(grep("carpark", sensorTypeList)) != 0){
    source('~/Snap4City/Snap4CityStatistics/CarParkQueryFunction.R')
    CarParkDataQuery(currentDate, daysNumber)
  }
  if (length(grep("twitter", sensorTypeList)) != 0){ 
    source('~/Snap4City/Snap4CityStatistics/TwitterQueryFunction.R')
    TwitterDataQuery(currentDate, daysNumber)
  }
  if (length(grep("weather", sensorTypeList)) != 0){
    source('~/Snap4City/Snap4CityStatistics/WeatherQueryFunction.R')
    WeatherSensors(currentDate, daysNumber)
  }
  
  source('~/Snap4City/Snap4CityStatistics/AutomaticDataIntegration.R')
  dataIntegration()
  
  source('~/Snap4City/Snap4CityStatistics/DescriptiveStatistics.R')
  descriptiveStatistics(currentDate)
  
}

#' @get /trend
trend <- function(sensorTypeList, currentDate, daysNumber) {
  unlink(paste("~/Snap4City/Sensors Data/FinalDatasets", list.files("~/Snap4City/Sensors Data/FinalDatasets"), sep="/"))
  
  unlink(paste("~/Snap4City/Sensors Data/TrafficCSVFiles", list.files("~/Snap4City/Sensors Data/TrafficCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/TwitterCSVFiles", list.files("~/Snap4City/Sensors Data/TwitterCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/CarParkCSVFiles", list.files("~/Snap4City/Sensors Data/CarParkCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/WeatherCSVFiles", list.files("~/Snap4City/Sensors Data/WeatherCSVFiles"), sep="/"))
  
  unlink(paste("~/Snap4City/StatisticsOutput/TrendPlot", list.files("~/Snap4City/StatisticsOutput/TrendPlot"), sep="/"))
  
  currentDate = as.Date(currentDate)
  daysNumber = as.numeric(daysNumber)
  
  source('~/Snap4City/Snap4CityStatistics/UploadLibrariesFunction.R')
  packagesInstallation()
  
  if (length(grep("traffic", sensorTypeList)) !=  0){
    source('~/Snap4City/Snap4CityStatistics/TrafficQueryFunction.R')
    TrafficDataQuery(currentDate, daysNumber)
  }
  
  if (length(grep("carpark", sensorTypeList)) != 0){
    source('~/Snap4City/Snap4CityStatistics/CarParkQueryFunction.R')
    CarParkDataQuery(currentDate, daysNumber)
  }
  if (length(grep("twitter", sensorTypeList)) != 0){ 
    source('~/Snap4City/Snap4CityStatistics/TwitterQueryFunction.R')
    TwitterDataQuery(currentDate, daysNumber)
  }
  if (length(grep("weather", sensorTypeList)) != 0){
    source('~/Snap4City/Snap4CityStatistics/WeatherQueryFunction')
    WeatherSensors(currentDate,  daysNumber)
  }
  
  source('~/Snap4City/Snap4CityStatistics/AutomaticDataIntegration.R')
  dataIntegration()
  
  source('~/Snap4City/Snap4CityStatistics/TrendPlots.R')
  TrendPlots(currentDate)
  
}

#' @get /arima
arima <- function(sensorTypeList) {
print(sensorTypeList)  
  unlink(paste("~/Snap4City/Sensors Data/FinalDatasets", list.files("~/Snap4City/Sensors Data/FinalDatasets"), sep="/"))
  
  unlink(paste("~/Snap4City/Sensors Data/TrafficCSVFiles", list.files("~/Snap4City/Sensors Data/TrafficCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/TwitterCSVFiles", list.files("~/Snap4City/Sensors Data/TwitterCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/CarParkCSVFiles", list.files("~/Snap4City/Sensors Data/CarParkCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/WeatherCSVFiles", list.files("~/Snap4City/Sensors Data/WeatherCSVFiles"), sep="/"))
  
  unlink(paste("~/Snap4City/StatisticsOutput/Predictions", list.files("~/Snap4City/StatisticsOutput/Predictions"), sep="/"))
  
  currentDate = Sys.Date()
  daysNumber = 60
    
  source('~/Snap4City/Snap4CityStatistics/UploadLibrariesFunction.R')
  packagesInstallation()
  
  if (length(grep("traffic", sensorTypeList)) !=  0){
    source('~/Snap4City/Snap4CityStatistics/TrafficQueryFunction.R')
    TrafficDataQuery(currentDate, daysNumber)
  }
  
  if (length(grep("carpark", sensorTypeList)) != 0){
    source('~/Snap4City/Snap4CityStatistics/CarParkQueryFunction.R')
    CarParkDataQuery(currentDate, daysNumber)
  }
  
  source('~/Snap4City/Snap4CityStatistics/AutomaticDataIntegration.R')
  dataIntegration()

  source('~/Snap4City/Snap4CityStatistics/ArimaPredictions.R')
  ArimaPredictions(sensorTypeList)
}

#' @get /anomaly
anomaly <- function(sensorTypeList, anomalyDate) {
  print(sensorTypeList)  
  unlink(paste("~/Snap4City/Sensors Data/FinalDatasets", list.files("~/Snap4City/Sensors Data/FinalDatasets"), sep="/"))
  
  unlink(paste("~/Snap4City/Sensors Data/TrafficCSVFiles", list.files("~/Snap4City/Sensors Data/TrafficCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/TwitterCSVFiles", list.files("~/Snap4City/Sensors Data/TwitterCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/CarParkCSVFiles", list.files("~/Snap4City/Sensors Data/CarParkCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/WeatherCSVFiles", list.files("~/Snap4City/Sensors Data/WeatherCSVFiles"), sep="/"))
  
  unlink(paste("~/Snap4City/StatisticsOutput/AnomalyDetection", list.files("~/Snap4City/StatisticsOutput/AnomalyDetection"), sep="/"))
  
  anomalyDate = as.Date(anomalyDate)
  currentDate = anomalyDate
  daysNumber = 60
  
  source('~/Snap4City/Snap4CityStatistics/UploadLibrariesFunction.R')
  packagesInstallation()
  
  if (length(grep("traffic", sensorTypeList)) !=  0){
    source('~/Snap4City/Snap4CityStatistics/TrafficQueryFunction.R')
    TrafficDataQuery(currentDate, daysNumber)
  }
  
  if (length(grep("carpark", sensorTypeList)) != 0){
    source('~/Snap4City/Snap4CityStatistics/CarParkQueryFunction.R')
    CarParkDataQuery(currentDate, daysNumber)
  }
  
  source('~/Snap4City/Snap4CityStatistics/AutomaticDataIntegration.R')
  dataIntegration()
  
  source('~/Snap4City/Snap4CityStatistics/AnomalyDetection.R')
  anomalyDetection(anomalyDate)
}

#' @get /ml
ml <- function(CarParkToPredict, currentDate) {
  unlink(paste("~/Snap4City/Sensors Data/FinalDatasets", list.files("~/Snap4City/Sensors Data/FinalDatasets"), sep="/"))

  unlink(paste("~/Snap4City/Sensors Data/TrafficCSVFiles", list.files("~/Snap4City/Sensors Data/TrafficCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/TwitterCSVFiles", list.files("~/Snap4City/Sensors Data/TwitterCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/CarParkCSVFiles", list.files("~/Snap4City/Sensors Data/CarParkCSVFiles"), sep="/"))
  unlink(paste("~/Snap4City/Sensors Data/WeatherCSVFiles", list.files("~/Snap4City/Sensors Data/WeatherCSVFiles"), sep="/"))  
  
  unlink(paste("~/Snap4City/StatisticsOutput/MachineLearningPredictions", list.files("~/Snap4City/StatisticsOutput/MachineLearningPredictions"), sep="/"))  
  
  currentDate = as.Date(currentDate)
  daysNumber = 20

  source('~/Snap4City/Snap4CityStatistics/UploadLibrariesFunction.R')
  packagesInstallation()

    source('~/Snap4City/Snap4CityStatistics/CarParkQueryFunction.R')
    CarParkDataQuery(currentDate, daysNumber)
    
    source('~/Snap4City/Snap4CityStatistics/TrafficQueryFunction.R')
    TrafficDataQuery(currentDate, daysNumber)
    
    source('~/Snap4City/Snap4CityStatistics/AutomaticDataIntegration.R')
    dataIntegration()

  source('~/Snap4City/Snap4CityStatistics/MachineLearningPrediction.R')
    MachineLearingCarParkPredictions(CarParkToPredict)
}
