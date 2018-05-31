# /* AUTOMATIC DATA INTEGRATION CONTRIB SNAP4CITY USER
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


#AUTOMATIC DATA INTEGRATION
dataIntegration <- function () {
  
  setwd("~/Snap4City/Sensors Data/FinalDatasets")
  directory <- "~/Snap4City/Sensors Data/FinalDatasets"
  availablesDataset <- list.files(directory)
  
  datasetList <- vector("list", length(availablesDataset))
  names(datasetList) <- availablesDataset
  
  for (i in 1:length(availablesDataset)){
    datasetList[[availablesDataset[i]]] <- read.csv(availablesDataset[i], sep=",")
    datasetList[[availablesDataset[i]]] <- datasetList[[availablesDataset[i]]][, colnames(datasetList[[availablesDataset[1]]])]
    #print(colnames(datasetList[[availablesDataset[i]]]))
  }
  for (i in 1:length(availablesDataset)){
    datasetList[[availablesDataset[i]]] <- datasetList[[availablesDataset[i]]][, -grep("X",colnames(datasetList[[availablesDataset[i]]]))]
    #print(colnames(datasetList[[availablesDataset[i]]]))
  }
  for (i in 1:length(availablesDataset)){
  datasetList[[availablesDataset[i]]][, "sensorType"] <- substr(availablesDataset[i], 1, (which(strsplit(availablesDataset[i], "")[[1]]=="D")-1))
  } 

  # integration between traffic and parking
  sensorDataset <- as.data.frame(datasetList[[names(datasetList)[1]]])
  for (j in 2:length(availablesDataset)) {
    sensorDataset <- rbind(sensorDataset, as.data.frame(datasetList[[names(datasetList)[j]]]))
  }
  
  sensorDataset$value <- round(sensorDataset$value ,3)
  sensorDataset <- na.omit(sensorDataset)

  #temporal adjustment - every 5 minutes
  dateTime <- as.POSIXct(sensorDataset[, "date_time"])
  sensorDataset[, "time"] <-format(dateTime, "%H:%M")
  
  sensorDatasetUnique <- unique(sensorDataset)
  
  setwd("~/Snap4City/Sensors Data")
  write.csv(sensorDatasetUnique, "SensorsDatasetFinal.csv")
  

}
