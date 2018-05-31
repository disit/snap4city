# /* ARIMA PREDICTIONS CONTRIB SNAP4CITY USER
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


#ARIMA PREDICTIONS
ArimaPredictions <- function(SensorToPredict){  
  
  currentDate = Sys.Date()
  inputWD <- "~/Snap4City/Sensors Data"
  outWD <- "~/Snap4City/StatisticsOutput/Predictions"
  csvFileName <- "SensorsDatasetFinal.csv"
  
  setwd(inputWD)
  dataset <- read.csv(paste("~/Snap4City/Sensors Data", csvFileName, sep ="/"), sep=",")
  dataset <- dataset[-1 , -grep("X", colnames(dataset))]
  
  #minute, hours, time and date
  dataset$minutes <- format(strptime(dataset$alignDateTime, "%Y-%m-%d %H:%M"), "%M")
  dataset$hour <- format(strptime(dataset$alignDateTime, "%Y-%m-%d %H:%M"), "%H")
  dataset$time <- format(strptime(dataset$alignDateTime, "%Y-%m-%d %H:%M"), "%H:%M")
  dataset$date <- format(strptime(dataset$alignDateTime, "%Y-%m-%d %H:%M"), "%Y-%m-%d")
  
  #day moment
  dataset$dayMoment[as.numeric(dataset$hour)>=0 & as.numeric(dataset$hour)<=5] <- "Night"
  dataset$dayMoment[as.numeric(dataset$hour)>=6 & as.numeric(dataset$hour)<=13] <- "Morning"
  dataset$dayMoment[as.numeric(dataset$hour)>=14 & as.numeric(dataset$hour)<=19] <- "Afternoon"
  dataset$dayMoment[as.numeric(dataset$hour)>=19 & as.numeric(dataset$hour)<=23] <- "Evening"
  
  #day of the week
  dataset[ , "days"] <- as.POSIXlt(dataset$date)$wday 
  indexWe <- which(dataset$days  == 6 | dataset$days == 0)
  dataset[indexWe, "dayOfTheWeek"] <- "weekend"
  dataset[-indexWe, "dayOfTheWeek"] <- "Working Days"
  
  dataset <- dataset[order(dataset$alignDateTime), ]
  
  #vector of 10 minutes
  hoursTemp = rep(0:23)
  minutesTemp = c("00",10,20,30,40,50)
  timeVec = rep(NA, length(hoursTemp)*length(minutesTemp))
  for (i in 1:length(hoursTemp)){
    for (j in 1:length(minutesTemp)){
      if (hoursTemp[i] < 10){
        temp = paste( "0", hoursTemp[i], sep="")
        timeVec[(i-1)*length(minutesTemp) + j ] = paste( temp, minutesTemp[j], sep=":")
      } else {
        timeVec[(i-1)*length(minutesTemp) + j ] = paste(hoursTemp[i], minutesTemp[j], sep=":")
      }
    }
  }
  
  if (SensorToPredict == "carpark") {
    SensorToPredict = "CarPark"
    uniqueVar = "free"
  }else if(SensorToPredict == "traffic"){
    SensorToPredict = "TrafficSensors"
    uniqueVar = "vehicleFlow"
  }

  indfolder = 1
  indResult = 1
  statisticsResult = list()
  statisticsResult[indfolder]$statisticsOutputName = unbox("Predictions")
  statisticsResult[[indfolder]]$statisticsOutputName = unbox("Predictions")
  statisticsResult[[indfolder]]$resultFiles = list()
  
  dataST <- dataset[which(dataset$sensorType == SensorToPredict), ]
  uniqueID <-  as.character(unique(dataST$identifier))
  
  for (k in 1:length(uniqueID)){
    dataID <- dataST[which(dataST$identifier==uniqueID[k]), ]
    
    dataTemp <- dataID[which(as.character(dataID$variable) == uniqueVar), ]
    dataTemp <- dataTemp[order(dataTemp$alignDateTime), ]
    
    if (uniqueVar == "free") {
      variableName = "Number of Free Slots"
    }
    if (uniqueVar == "vehicleFlow") {
      variableName = "Vehicle Flow"
    }
    
    dataToPredict <- array(dataTemp$value)
    
    ariMod <- auto.arima(dataTemp$value)
    pred <- forecast(ariMod, 4)

    if ((which(timeVec == dataTemp$time[dim(dataTemp)[1]])+1) == length(timeVec)) {
      predTime <- timeVec[1:4]
    }else if((which(timeVec == dataTemp$time[dim(dataTemp)[1]])+1) == length(timeVec)-1) {
      predTime <- c(timeVec[(which(timeVec == dataTemp$time[dim(dataTemp)[1]])+1)], timeVec[1:3])
    }else if((which(timeVec == dataTemp$time[dim(dataTemp)[1]])+1) == length(timeVec)-2) {
      predTime <- c(timeVec[(which(timeVec == dataTemp$time[dim(dataTemp)[1]])+1)], 
                    timeVec[(which(timeVec == dataTemp$time[dim(dataTemp)[1]])+2)],
                    timeVec[1:2])
    }else if((which(timeVec == dataTemp$time[dim(dataTemp)[1]])+1) == length(timeVec)-3) {
      predTime <- c(timeVec[(which(timeVec == dataTemp$time[dim(dataTemp)[1]])+1)], 
                    timeVec[(which(timeVec == dataTemp$time[dim(dataTemp)[1]])+2)],
                    timeVec[(which(timeVec == dataTemp$time[dim(dataTemp)[1]])+3)],
                    timeVec[1])
    }
    predTime <- timeVec[(which(timeVec == dataTemp$time[dim(dataTemp)[1]])+1):(which(timeVec == dataTemp$time[dim(dataTemp)[1]])+4)]
    
    predMatr <- matrix(NA, 4, 3)
    predMatr <- as.data.frame(predMatr)
    
    colnames(predMatr) <- c("time", "value", "status")
    
    predMatr[,"status"] <- "Predicted"
    predMatr[,"time"] <- predTime
    predMatr[,"value"] <- round(pred$mean[1:4], 0)
    
    lastDayData <- dataTemp[which(dataTemp$date == currentDate), c("time", "value")]
    lastDayData[, "status"] <- "Observed"
    
    datAndPred <- rbind(lastDayData, predMatr)
    Legend <- datAndPred$status
    plt <- ggplot(datAndPred, aes(time, value, group=Legend, color=Legend)) +
      geom_point(color="black", size=0.8) +
      geom_xspline(spline_shape=-0.1, size=1)+
      xlab("Time of the day") + 
      ylab(variableName) +  
      theme(axis.title = element_text(size=21), axis.text = element_text(size=18), axis.text.x = element_text(angle = 90))+
      theme(legend.text = element_text(size=20), 
            legend.title = element_text(size=25))+ 
      theme(plot.title = element_text(size=30, face="bold", hjust = 0.5))+
      ggtitle(paste(uniqueID[k], "\n","'",variableName,"'", "\nDaily Trend Prediction", sep=""))
    
    predMatrTab <- predMatr[,c("time","value")]
    predMatrTab["Lower 80%"] = pred$lower[1:4,1]
    predMatrTab["Upper 80%"] = pred$upper[1:4,1]
    predMatrTab["Lower 95%"] = pred$lower[1:4,2]
    predMatrTab["Upper 95%"] = pred$upper[1:4,2]
    names(predMatrTab) <- c("Time","Forecast","Lower 80%","Upper 80%","Lower 95%","Upper 95%")
    
    tt <- ttheme_default(colhead=list(fg_params = list(parse=TRUE)))
    tbl <- tableGrob(predMatrTab, rows=NULL, theme=tt)
    title <- textGrob("Predicted Values and Confidence Intervals", gp=gpar(fontsize=10))
    padding <- unit(5,"mm")
    tblT <- gtable_add_rows(tbl, heights = grobHeight(title) + padding, pos = 0)
    tblT <- gtable_add_grob(tblT, title, 1, 1, 1, ncol(tblT))
    
    tab <- tidy(ariMod, conf.int = F)
    tab <- tableGrob(tab)
    titleAr <- textGrob(paste("ARIMA Model", "\nCoefficients"), gp=gpar(fontsize=10))
    padding <- unit(5,"mm")
    tblA <- gtable_add_rows(tab, heights = grobHeight(titleAr) + padding, pos = 0)
    tblA <- gtable_add_grob(tblA, titleAr, 1, 1, 1, ncol(tblA))
    
    plotMix <- grid.arrange(plt, grid.arrange(tblT, tblA, ncol=2,heights=c(2,2),as.table=TRUE),
                            nrow = 2,
                            heights=c(4,2),
                            as.table=TRUE)
    setwd(outWD)
    ggsave(paste(uniqueID[k], uniqueVar,"DailyTrendPrediction.png", sep=""), plotMix, width=3, height=2, units="in",scale=10)
    
    statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(as.character(uniqueID[k]))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, paste(uniqueID[k],uniqueVar, "DailyTrendPrediction.png", sep=""), sep="/"))
    indResult = indResult + 1
    
  }
  
  setwd("~/Snap4City")
  write(jsonlite::toJSON(statisticsResult), "JsonStatisticsResult.json")
  return(statisticsResult[[1]])
  
} 



