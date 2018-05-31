# /* CAR PARK QUERY FUNCTION CONTRIB SNAP4CITY USER
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

#CAR PARK QUERY FUNCTION
CarParkDataQuery = function(currentDate, daysNumber){
  
  endDate <- as.Date(currentDate) - daysNumber  
  #QUERY SENSORS LIST
  #list initialization
  carParkArray = c("CarParkBeccaria", "CarParkCareggi", "CarParkS.Lorenzo", "CarParkPieracciniMeyer", "CarParkStazioneFirenzeS.M.N.")
  
  setwd("~/Snap4City/Sensors Data/CarParkCSVFiles")
  for (i in length(carParkArray):1) {
    
    urlSPARQLCarPark = paste(
      "http://192.168.0.206:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fdate+%3Ffree+%7B%0D%0A%3Chttp%3A%2F%2Fwww.disit.org%2Fkm4city%2Fresource%2F",
      carParkArray[i],
      "%3E+km4c%3AhasCarParkSensor+%3Fo.%0D%0A%3Fo+km4c%3AhasRecord+%3Fr.%0D%0A%3Fr+km4c%3Afree+%3Ffree.%0D%0A%3Fr+km4c%3AobservationTime%2Fdcterms%3Aidentifier+%3Fdate+FILTER+%28%3Fdate+%3C%3D+%22",
      currentDate,
      "T00%3A00%3A00%22+%5E%5Exsd%3AdateTime+%26%26+%3Fdate+%3E%3D+%22",
      endDate,
      "T23%3A59%3A00%22+%5E%5Exsd%3AdateTime%29%0D%0A%7D+%0D%0Aorder+by+desc%28%3Fdate%29&format=text%2Fcsv&timeout=0&debug=on",
      sep = "")
    
    
    temp = read.csv(urlSPARQLCarPark, sep=",",header=T)
    write.csv(temp, paste(carParkArray[i],".csv", sep="")) # saving of csv files on a specific directory named CSVFiles: one csv for a single car park
  }
  
  directory = "~/Snap4City/Sensors Data/CarParkCSVFiles"
  files <- list.files(directory)                                              
  dat <- read.csv(paste(directory, files[1], sep="/"))
  dat <- as.data.frame(dat)
  dat[, "identifier"] <- substr(files[1], 1, nchar(files[1])-4)
  dat[, "variable"] <- colnames(dat)[grep("free", colnames(dat))]
  colnames(dat)[grep("free", colnames(dat))] <- "value"
  colnames(dat)[grep("date", colnames(dat))] <- "date_time"
  
  for (j in 2: (length(files))){
    file <- read.csv(paste(directory, files[j], sep="/"))
    file <- as.data.frame(file)
    file[, "identifier"] <- substr(files[j], 1, nchar(files[j])-4)
    file[, "variable"] <- colnames(file)[grep("free", colnames(file))]
    colnames(file)[grep("free", colnames(file))] <- "value"
    colnames(file)[grep("date", colnames(file))] <- "date_time"
    dat <- rbind(dat, file) 
  }
  dat <- dat[, c("identifier", "date_time", "value", "variable")]
  dat$date_time <- format(strptime(dat$date_time, "%Y-%m-%dT%H:%M"), "%Y-%m-%d %H:%M")
  
  newDataset <- matrix(NA, ncol = 5, nrow = 1)
  newDataset <- as.data.frame(newDataset)
  colnames(newDataset) <- c("identifier","date_time","alignDateTime","value","variable")
  
  uniqueID <- unique(dat$identifier)
  for (k in 1:length(uniqueID)){
    index <- which(dat[,"identifier"] == uniqueID[k])
    dataTemp <- dat[index, ]
    uniqueVar <- unique(dataTemp$variable)
    
    dateTimeTemp <- as.POSIXct(dataTemp[ , "date_time"])
    dataTemp[ , "alignDateTime"] <- format(align.time(dateTimeTemp, n=60*10), "%Y-%m-%d %H:%M")  # n is in seconds
    
    dataTemp <- dataTemp[, c("identifier","date_time","alignDateTime","value","variable")]
    newDataset <- rbind(newDataset, dataTemp)
  }
  newDataset <- newDataset[-1,]
  
  indexDup <- row.names(newDataset[which(duplicated(newDataset)), c("identifier","alignDateTime","variable")])
  indexDup <- as.numeric(indexDup)
  
  if (length(indexDup) != 0){
    DatasetCleanded <- newDataset[-indexDup, ]
  }else{
    DatasetCleanded <- newDataset
  }
  
  setwd("~/Snap4City/Sensors Data/FinalDatasets")
  write.csv(newDataset, "CarParkDataset.csv")
  
}

