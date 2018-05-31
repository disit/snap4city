# /* DESCRIPTIVE STATISTICS CONTRIB SNAP4CITY USER
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

#DESCRIPTIVE STATISTICS
descriptiveStatistics = function(currentDate) {
  
  inputWD <- "~/Snap4City/Sensors Data"
  outWD <- "~/Snap4City/StatisticsOutput"
  csvFileName <- "SensorsDatasetFinal.csv"
  
  #setwd("~/Snap4City/Sensors Data")
  setwd(inputWD)
  #dataset <- read.csv("SensorsDatasetFinal.csv", sep=",")
  dataset <- read.csv(csvFileName, sep=",")
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
  dataset[indexWe, "dayOfTheWeek"] <- "Weekend"
  dataset[-indexWe, "dayOfTheWeek"] <- "Working Days"
  
  dataset <- dataset[order(dataset$alignDateTime), ]
  
  #splitting of workdays and weekend
  indexWE <- which(dataset$dayOfTheWeek  == "Weekend")
  dataWE <- dataset[indexWE, c("time","identifier","variable","value","dayMoment")] 
  dataWD <- dataset[-indexWE, c("time","identifier","variable","value","dayMoment")] 
  
  dataFinalWE <- dcast(dataWE, time+dayMoment ~ identifier+variable, fun.aggregate=mean)
  dataFinalWD <- dcast(dataWD, time+dayMoment ~ identifier+variable, fun.aggregate=mean)
  
  #DATA "IMPUTATION":
  uniqueSensorType <- colnames(dataFinalWE)[which(colnames(dataFinalWE)!="time" & colnames(dataFinalWE)!="dayMoment")]
  options(digits = 3)
  for(i in 1:length(uniqueSensorType)){
    for(j in 1:dim(dataFinalWE)[1]){
      if(is.na(dataFinalWE[j, uniqueSensorType[i]]) == T){
        index <- which(dataFinalWE[, uniqueSensorType[i]]!= 0)
        currentValue = dataFinalWE[index[which(j < index)][1] , uniqueSensorType[i]]
        dataFinalWE[j, uniqueSensorType[i]] = currentValue
      }
    }
  }
  uniqueSensorType <- colnames(dataFinalWD)[which(colnames(dataFinalWD)!="time" & colnames(dataFinalWD)!="dayMoment")]
  options(digits = 3)
  for(i in 1:length(uniqueSensorType)){
    for(j in 1:dim(dataFinalWD)[1]){
      if(is.na(dataFinalWD[j, uniqueSensorType[i]]) == T){
        index <- which(dataFinalWD[, uniqueSensorType[i]]!= 0)
        currentValue = dataFinalWD[index[which(j < index)][1] , uniqueSensorType[i]]
        dataFinalWD[j, uniqueSensorType[i]] = currentValue
      }
    }
  }

  dataFinal <- dcast(dataset[, c("time","identifier","variable","value","dayMoment")], time+dayMoment ~ identifier+variable, fun.aggregate=mean)
  
  #DATA "IMPUTATION":
  uniqueSensorType <- colnames(dataFinal)[which(colnames(dataFinal)!="time" & colnames(dataFinal)!="dayMoment")]
  options(digits = 3)
  for(i in 1:length(uniqueSensorType)){
    for(j in 1:dim(dataFinal)[1]){
      if(is.na(dataFinal[j, uniqueSensorType[i]]) == T){
        index <- which(dataFinal[, uniqueSensorType[i]]!= 0)
        currentValue = dataFinal[index[which(j < index)][1] , uniqueSensorType[i]]
        dataFinal[j, uniqueSensorType[i]] = currentValue
      }
    }
  }
  #-------------------------------------------------------------------------------------------------------------------------
  # -------------- DESCRIPTIVE STATISTISTICS  ------------------------------------------------------------------------------
  #------------------------------------------------------------------------------------------------------------------------- 
  #--Sensors Mean Per Day Moment, day of the week and type of sensor
  uniqueST <- unique(dataset$sensorType)
  indfolder = 1
  indResult = 1
  statisticsResult = list()
  statisticsResult[indfolder]$statisticsOutputName = unbox("MeanPerDayMoment")
  statisticsResult[[indfolder]]$statisticsOutputName = unbox("MeanPerDayMoment")
  statisticsResult[[indfolder]]$resultFiles = list()
  
  for (i in 1:length(uniqueST)){
    sensorsName <- unique(dataset[which(dataset$sensorType == uniqueST[i]), "identifier"])
    dataPerSensors_WE <- dataWE[which(dataWE$identifier == sensorsName), ]
    dataPerSensors_WD <- dataWD[which(dataWD$identifier == sensorsName), ]
    options(digits=0)  
    #week end
    D_WE <- dcast(dataPerSensors_WE, identifier+variable ~ dayMoment, fun.aggregate=mean)
    setwd(paste(outWD, "MeanPerDayMoment", sep="/"))
    WE <- tableGrob(D_WE,  rows = rownames(D_WE), cols = colnames(D_WE), theme = ttheme_default())
    
    WEtitle <- textGrob(paste(uniqueST[i], "\nAverage during the Weekend", sep=""), gp=gpar(fontsize=20))
    padding <- unit(5,"mm")
    WEtable <- gtable_add_rows(WE, heights = grobHeight(WEtitle) + padding, pos = 0)
    WEtable <- gtable_add_grob(WEtable, WEtitle, 1, 1, 1, ncol(WEtable))
    grid.draw(WEtable)
    grid.arrange(WEtable)
    h <- convertHeight(sum(WEtable$heights), "in", TRUE)
    w <- convertWidth(sum(WEtable$widths), "in", TRUE)
    ggsave(paste(uniqueST[i], "DataMeanPerDayMomentOnWeekEnd.png", sep=""), WEtable, height = h, width = w)
    
    write.csv(dataPerSensors_WE, paste(uniqueST[i], "DataMeanPerDayMomentOnWeekEnd.csv",sep=""))
    
    statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(as.character(uniqueST[i]))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$csv=unbox(paste(outWD, "MeanPerDayMoment",paste(uniqueST[i], "DataMeanPerDayMomentOnWeekEnd.csv", sep=""), sep="/"))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, "MeanPerDayMoment",paste(uniqueST[i], "DataMeanPerDayMomentOnWeekEnd.png", sep=""), sep="/"))
    indResult = indResult + 1
    
    #working days
    D_WD <- dcast(dataPerSensors_WD, identifier+variable ~ dayMoment, fun.aggregate=mean)
    setwd(paste(outWD, "MeanPerDayMoment", sep="/"))
    
    WD <- tableGrob(D_WD,  rows = rownames(D_WD), cols = colnames(D_WD), theme = ttheme_default())
    WDtitle <- textGrob(paste(uniqueST[i], "\nAverage during the Working Days", sep=""), gp=gpar(fontsize=20))
    padding <- unit(5,"mm")
    WDtable <- gtable_add_rows(WD, heights = grobHeight(WDtitle) + padding, pos = 0)
    WDtable <- gtable_add_grob(WDtable, WDtitle, 1, 1, 1, ncol(WDtable))
    grid.draw(WDtable)
    grid.arrange(WDtable)
    h <- convertHeight(sum(WDtable$heights), "in", TRUE)
    w <- convertWidth(sum(WDtable$widths), "in", TRUE)
    ggsave(paste(uniqueST[i], "DataMeanPerDayMomentOnWorkingDays.png", sep=""), WDtable, height = h, width = w)
    
    write.csv(dataPerSensors_WD, paste(uniqueST[i], "DataMeanPerDayMomentOnWorkingDays.csv", sep=""))
    
    statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(as.character(uniqueST[i]))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$csv=unbox(paste(outWD, "MeanPerDayMoment",paste(uniqueST[i], "DataMeanPerDayMomentOnWorkingDays.csv", sep=""), sep="/"))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, "MeanPerDayMoment",paste(uniqueST[i], "DataMeanPerDayMomentOnWorkingDays.png", sep=""), sep="/"))
    indResult = indResult + 1
  }  
  
  indfolder = indfolder + 1
  
  #----------------------------------------------------------------------------------------------------------------
  #--Statistics Group By Sensors and Day of the week
  indResult = 1
  
  statisticsResult[indfolder]$statisticsOutputName = unbox("StatisticsBySensors")
  statisticsResult[[indfolder]]$statisticsOutputName = unbox("StatisticsBySensors")
  
  options(digits=2)
  uniqueST <- unique(dataset$sensorType)
  dataset[, "idPerVar"] <- paste(dataset$identifier, dataset$variable, sep=":")
  
  for (i in 1:length(uniqueST)){
    
    SBSdat <- dataset[which(dataset$sensorType == uniqueST[i]), ]
    
    SBS_DoW <- describeBy(SBSdat$value, list(SBSdat$idPerVar, SBSdat$dayOfTheWeek))
    SBS_DoW_Tab <-do.call("rbind", SBS_DoW)
    SBS_DoW_Tab <- as.data.frame(SBS_DoW_Tab)
    
    for (j in 1:length(row.names(SBS_DoW))){
      SBS_DoW_Tab[j ,"vars"] = paste(row.names(SBS_DoW)[j], colnames(SBS_DoW)[1], sep = "-")
      SBS_DoW_Tab[length(row.names(SBS_DoW))+j,"vars"] = paste(row.names(SBS_DoW)[j], colnames(SBS_DoW)[2], sep = "-")
    }
    
    row.names(SBS_DoW_Tab) <- SBS_DoW_Tab$vars
    SBS_DoW_Tab <- SBS_DoW_Tab[, which(colnames(SBS_DoW_Tab) != "vars" & colnames(SBS_DoW_Tab) != "n")]
    
    statsWE <- SBS_DoW_Tab[1:length(row.names(SBS_DoW)), ] 
    statsWD <- SBS_DoW_Tab[(length(row.names(SBS_DoW))+1): dim(SBS_DoW_Tab)[1], ] 
    
    newName=c()
    newName <- substr(row.names(statsWE)[1], 1, (which(strsplit(row.names(statsWE)[1], "")[[1]]=="-")-1))
    for (k in 2:length(row.names(statsWE))){
      newNameTemp <- substr(row.names(statsWE)[k], 1, (which(strsplit(row.names(statsWE)[k], "")[[1]]=="-")-1))
      newName <- c(newName, newNameTemp)
    }
    
    row.names(statsWE) <- newName
    row.names(statsWD) <- newName
    
    setwd(paste(outWD, "StatisticsBySensors", sep="/"))
    stats <- tableGrob(round(statsWE, 2),  rows = rownames(statsWE), cols = colnames(statsWE), theme = ttheme_default())
    title <- textGrob(paste(uniqueST[i],"\nStatistics on the WeekEnd", sep=""), gp=gpar(fontsize=20))
    padding <- unit(5,"mm")
    table <- gtable_add_rows(stats, heights = grobHeight(title) + padding, pos = 0)
    table <- gtable_add_grob(table, title, 1, 1, 1, ncol(table))
    grid.draw(table)
    grid.arrange(table)
    
    h <- convertHeight(sum(table$heights), "in", TRUE)
    w <- convertWidth(sum(table$widths), "in", TRUE)
    ggsave(paste(uniqueST[i], "StatisticsOnWeekEnd.png", sep=""), table, height = h, width = w)
    
    write.csv(statsWE, paste(uniqueST[i], "StatisticsOnWeekEnd.csv", sep=""))
    
    statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(as.character(uniqueST[i]))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$csv=unbox(paste(outWD, "StatisticsBySensors",paste(uniqueST[i], "StatisticsOnWeekEnd.csv", sep=""), sep="/"))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, "StatisticsBySensors",paste(uniqueST[i], "StatisticsOnWeekEnd.png", sep=""), sep="/"))
    indResult = indResult + 1
    
    setwd(paste(outWD, "StatisticsBySensors", sep="/"))
    stats <- tableGrob(round(statsWD, 2),  rows = rownames(statsWD), cols = colnames(statsWD), theme = ttheme_default())
    title <- textGrob(paste(uniqueST[i],"\nStatistics on Working Days", sep=""), gp=gpar(fontsize=20))
    padding <- unit(5,"mm")
    table <- gtable_add_rows(stats, heights = grobHeight(title) + padding, pos = 0)
    table <- gtable_add_grob(table, title, 1, 1, 1, ncol(table))
    grid.draw(table)
    grid.arrange(table)
    
    h <- convertHeight(sum(table$heights), "in", TRUE)
    w <- convertWidth(sum(table$widths), "in", TRUE)
    ggsave(paste(uniqueST[i], "StatisticsOnWorkingDays.png", sep=""), table, height = h, width = w)
    
    write.csv(statsWD, paste(uniqueST[i], "StatisticsOnWorkingDays.csv", sep=""))
    
    statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(as.character(uniqueST[i]))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$csv=unbox(paste(outWD, "StatisticsBySensors",paste(uniqueST[i], "StatisticsOnWorkingDays.csv", sep=""), sep="/"))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, "StatisticsBySensors",paste(uniqueST[i], "StatisticsOnWorkingDays.png", sep=""), sep="/"))
    indResult = indResult + 1
    
  }
  
  indfolder = indfolder + 1
  
  #----------------------------------------------------------------------------------------------------------------
  #---- CORRELATION MATRIX ----------------------------------------------------------------------------------------
  #----------------------------------------------------------------------------------------------------------------
  options(digits=2)
  #CORRELATION AMONG THE AVERAGE VALUES OF EACH VARIABLE - AVERAGE IN A PERIOD OF 60 DAYS
  indResult = 1
  
  statisticsResult[indfolder]$statisticsOutputName = unbox("Correlations")
  statisticsResult[[indfolder]]$statisticsOutputName = unbox("Correlations")
  
  uniqueSensorType <- colnames(dataFinalWE)[which(colnames(dataFinalWE)!="time" & colnames(dataFinalWE)!="dayMoment")]
  
  correlation = cor(na.omit(dataFinal[, uniqueSensorType]))
  setwd(paste(outWD, "Correlations", sep="/"))
  png('CorrelationMatrix.png',width = 1500, height = 1500, units = "px", pointsize = 20)
  corPlot <- corrplot.mixed(correlation, tl.pos = c("lt"), diag = c("l"), cl.lim=c(-1,1),
                            number.cex = 0.4, number.font = 1.8, tl.cex = 0.5, tl.col = "blue",
                            title = "Correlation among the average values of each variable related to the entire period",
                            mar=c(0,0,2,0))
  dev.off()
  write.csv(correlation, "CorrelationMatrix.csv")
  
  statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox("All Sensors")
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$csv=unbox(paste(outWD, "Correlations", "CorrelationMatrix.csv",  sep="/"))
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, "Correlations", "CorrelationMatrix.png",  sep="/"))
  indResult = indResult + 1
  
  #----------------------------------------------------------------------------------------------------------------
  #CORRELATION AMONG THE AVERAGE VALUES OF EACH VARIABLE DURING THE WEEK END- AVERAGE IN A PERIOD OF 60 DAYS
  correlation = cor(na.omit(dataFinalWE[, uniqueSensorType]))
  setwd(paste(outWD, "Correlations", sep="/"))
  png('CorrelationMatrixWeekEnd.png',width = 1500, height = 1500, units = "px", pointsize = 20)
  corPlot <- corrplot.mixed(correlation, tl.pos = c("lt"), diag = c("l"), cl.lim=c(-1,1),
                            number.cex = 0.4, number.font = 1.8, tl.cex = 0.5, tl.col = "blue",
                            title = "Correlation among the average values of each variable considering the weekend related to the entire period",
                            mar=c(0,0,2,0))
  dev.off()
  write.csv(correlation, "CorrelationMatrixWeekEnd.csv")
  
  statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox("All Sensors")
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$csv=unbox(paste(outWD, "Correlations", "CorrelationMatrixWeekEnd.csv", sep="/"))
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, "Correlations", "CorrelationMatrixWeekEnd.png", sep="/"))
  indResult = indResult + 1
  
  #----------------------------------------------------------------------------------------------------------------
  #CORRELATION AMONG THE AVERAGE VALUES OF EACH VARIABLE DURING WORKING DAYS- AVERAGE IN A PERIOD OF 60 DAYS
  correlation = cor(na.omit(dataFinalWD[, uniqueSensorType]))
  setwd(paste(outWD, "Correlations", sep="/"))
  png('CorrelationMatrixWorkingDays.png',width = 1500, height = 1500, units = "px", pointsize = 20)
  corPlot <- corrplot.mixed(correlation, tl.pos = c("lt"), diag = c("l"), cl.lim=c(-1,1),
                            number.cex = 0.4, number.font = 1.8, tl.cex = 0.5, tl.col = "blue",
                            title = "Correlation among the average values of each variable considering working days related to the entire period",
                            mar=c(0,0,2,0))
  dev.off()
  write.csv(correlation, "CorrelationMatrixWorkingDays.csv")
  
  statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox("All Sensors")
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$csv=unbox(paste(outWD, "Correlations", "CorrelationMatrixWorkingDays.csv", sep="/"))
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, "Correlations", "CorrelationMatrixWorkingDays.png", sep="/"))
  indResult = indResult + 1

  setwd("~/Snap4City")
  write(jsonlite::toJSON(statisticsResult), "JsonStatisticsResult.json")
  return(statisticsResult[[1]])

}








