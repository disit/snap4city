# /* TRAFFIC QUERY FUNCTION CONTRIB SNAP4CITY USER
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

#TRAFFIC QUERY FUNCTION
TrafficDataQuery = function(currentDate, daysNumber){

#limit_query_sensor = 144*60  #limit: one day corresponds to 144 ten minutes'slots   
latitudeMax = 43.816788
longitudeMax = 11.288442
latitudeMin = 43.756291
longitudeMin = 11.190046
limit = 70000
endDate = as.Date(currentDate) - daysNumber

#QUERY SENSORS LIST
urlSPARQLSensors <- paste("http://192.168.0.206:8890/sparql?default-graph-uri=&query=select+distinct+%3Fsensor+xsd%3Afloat%28%3FsLat%29+as+%3FnALat+xsd%3Afloat%28%3FsLong%29+as%3FnALong++%3Froad+%3Fg1+%3Fdir%7B%0D%0A++%3Fs+a+km4c%3ASensorSite.%0D%0A++%3Fs+%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2Fidentifier%3E+%3Fsensor.%0D%0A++%3Fs+geo%3Ageometry+%3Fg1.%0D%0A++%3Fs+geo%3Alat+%3FsLat.%0D%0A++%3Fs+geo%3Along+%3FsLong.%0D%0A++%3Fs+schema%3AstreetAddress+%3Fdir.%0D%0A++filter%28bif%3Ast_x%28%3Fg1%29%3E%3D", longitudeMin,"+%26%26+bif%3Ast_x%28%3Fg1%29%3C%3D", longitudeMax,"+%26%26+bif%3Ast_y%28%3Fg1%29%3E%3D", latitudeMin,"+%26%26+bif%3Ast_y%28%3Fg1%29%3C%3D", latitudeMax,"%29%0D%0A++++filter%28+regex%28str%28%3Fsensor%29%2C+%22METRO%22+%29%29%0D%0A%7B%0D%0A++%3Fs+km4c%3AplacedOnRoad+%3Fr.%0D%0A++%3Fr+%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2Fidentifier%3E+%3Froad%0D%0A+++%7D+%0D%0A%7D+limit+", limit,"&format=text%2Fcsv&timeout=0&debug=on", sep="")
sensors = read.csv(urlSPARQLSensors, sep=",",header=T, colClasses=c(rep("character",1),rep("numeric",2),rep("character",3)))
sensors = unique(sensors)  
vfh=50 
sensor_list=sensors[,"sensor"][1:5] #FIRST FIVE METRO SENSORS
sensor_list=unique(sensor_list)

#list initialization
sensorDatasetList <- vector("list", length(sensor_list))
names(sensorDatasetList) <- sensor_list

  for(i in length(sensor_list):1){
      
    urlSPARQLSensore <- paste(
      "http://192.168.0.206:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fidentifier%2C%3Fdate%2C+%3FaverageSpeed%2C+%3FaverageTime%2C+%3Fconcentration%2C+%3FvehicleFlow++{%0D%0A<http%3A%2F%2Fwww.disit.org%2Fkm4city%2Fresource%2F",sensor_list[i], ">+km4c%3AhasObservation+%3Fobs.%0D%0A<http%3A%2F%2Fwww.disit.org%2Fkm4city%2Fresource%2F",sensor_list[i], ">+<http%3A%2F%2Fpurl.org%2Fdc%2Fterms%2Fidentifier>+%3Fidentifier.%0D%0A%3Fobs+km4c%3AaverageSpeed+%3FaverageSpeed%3B%0D%0Akm4c%3AaverageTime+%3FaverageTime%3B%0D%0Akm4c%3Aconcentration+%3Fconcentration%3B%0D%0Akm4c%3AvehicleFlow+%3FvehicleFlow%3B%0D%0A<http%3A%2F%2Fpurl.org%2Fdc%2Fterms%2Fdate%3E+%3Fdate.+FILTER+%28%3Fdate+%3C%3D+%22",
      currentDate,
      "T00%3A00%3A00%22+%5E%5Exsd%3AdateTime+%26%26+%3Fdate+%3E%3D+%22",
      endDate,
      "T23%3A59%3A00%22+%5E%5Exsd%3AdateTime%29%0D%0A%7D%0D%0Aorder+by+desc(%3Fdate)%0D%0A&format=text%2Fcsv&timeout=0&debug=on",
      sep="")
    
   temp = read.csv(urlSPARQLSensore, sep=",",header=T)
    if (dim(temp)[1] != 0){
      for (j in 1:dim(temp)[1]){
        temp[j,"density"]=temp[j,"vehicleFlow"]/vfh*0.02    
        
        if (is.na(temp[j,"density"])){
          print(paste("i",i, sep=" "))
        }
        
      }
    }
    if(floor(dim(temp)[1]/2) != 0){
      for(j in 1: floor(dim(temp)[1]/2)){                                                      
        copia = temp[j,]                                                                       
        temp[j,] = temp[dim(temp)[1]+1-j,]
        temp[dim(temp)[1]+1-j, ] = copia 
      }
    }
    if (dim(temp)[1] != 0){
      sensorDatasetList[i] = list(temp[which(!is.na(temp$density)),]) 
      sensorDatasetList[[i]]$identifier = as.character(temp[1,"identifier"])
    } else {
      sensorDatasetList = sensorDatasetList[-i]
      sensor_list = sensor_list[-i]
    }
  }

# saving of csv files on a specific directory named CSVFiles: one csv for a single traffic sensor
setwd("~/Snap4City/Sensors Data/TrafficCSVFiles")
for(i in 1:length(sensor_list)){
  write.csv(sensorDatasetList[[i]], paste(sensor_list[i],".csv", sep=""))
}

directory = "~/Snap4City/Sensors Data/TrafficCSVFiles"
files <- list.files(directory)                                              
dat <- read.csv(paste(directory, files[1], sep="/"))
dat <- as.data.frame(dat)
colnames(dat)[grep("date", colnames(dat))] <- "date_time"

for (j in 2: (length(files))){
  file = read.csv(paste(directory, files[j], sep="/"))
  file <- as.data.frame(file)
  colnames(file)[grep("date", colnames(file))] <- "date_time"
  dat = rbind(dat, file) 
}
  dat <- dat[, -1]

variableNames <- c("vehicleFlow", "averageSpeed")  #I'm interest only in vehicle flow and average speed
newTraffData <- matrix(NA, nrow = 1, ncol = 4)
colnames(newTraffData) <- c("identifier", "date_time", "value", "variable")
for (i in 1:length(variableNames)) {
  dataTemp <- dat[, c("identifier","date_time", variableNames[i])]
  meltingData <- melt(dataTemp,
                      id = c("identifier", "date_time"),
                      na.rm = F)
  newTraffData <- rbind(newTraffData, meltingData)
}

#date and time format
newTraffData$date_time <- format(strptime(newTraffData$date_time, "%Y-%m-%dT%H:%M"), "%Y-%m-%d %H:%M")
newTraffData <- newTraffData[-1, ]



newDataset <- matrix(NA, ncol = 5, nrow = 1)
newDataset <- as.data.frame(newDataset)
colnames(newDataset) <- c("identifier","date_time","alignDateTime","value","variable")

uniqueID <- unique(newTraffData$identifier)

for (k in 1:length(uniqueID)){
  index <- which(newTraffData[,"identifier"] == uniqueID[k])
  dataTemp <- newTraffData[index, ]
  uniqueVar <- unique(dataTemp$variable)
  
  dateTimeTemp <- as.POSIXct(dataTemp[ , "date_time"])
  dataTemp[ , "alignDateTime"] <- format(align.time(dateTimeTemp, n=60*10), "%Y-%m-%d %H:%M")  # n is in seconds
  
  dataTemp <- dataTemp[, c("identifier","date_time","alignDateTime","value","variable")]
  newDataset <- rbind(newDataset, dataTemp)
}

indexDup <- row.names(newDataset[which(duplicated(newDataset)), c("identifier","alignDateTime","variable")])
indexDup <- as.numeric(indexDup)

if (length(indexDup) != 0){
  DatasetCleanded <- newDataset[-indexDup, ]
}else{
  DatasetCleanded <- newDataset
}

setwd("~/Snap4City/Sensors Data/FinalDatasets")
write.csv(DatasetCleanded, "TrafficSensorsDataset.csv")

}

