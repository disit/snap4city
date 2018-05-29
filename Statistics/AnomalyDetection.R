#ANOMALY DETECTION
anomalyDetection <- function(anomalyDate) {
  
  inputWD <- "~/Snap4City/Sensors Data"
  outWD <- "~/Snap4City/StatisticsOutput/AnomalyDetection"
  csvFileName <- "SensorsDatasetFinal.csv"
  
  setwd(inputWD)
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
  dataset[indexWe, "dayOfTheWeek"] <- "weekend"
  dataset[-indexWe, "dayOfTheWeek"] <- "Working Days"
  
  dataset <- dataset[order(dataset$alignDateTime), ]
  
  uniqueST <- as.character(unique(dataset$sensorType))
  
  dataFinalAll <- dcast(dataset, date+alignDateTime ~ identifier+variable, fun.aggregate=mean)
  
  #DATA "IMPUTATION":
  uniqueSensorType <- colnames(dataFinalAll)[which(colnames(dataFinalAll)!="time" & colnames(dataFinalAll)!="dayMoment")]
  options(digits = 3)
  for(i in 1:length(uniqueSensorType)){
    for(j in 1:dim(dataFinalAll)[1]){
      if(is.na(dataFinalAll[j, uniqueSensorType[i]]) == T){
        index <- which(dataFinalAll[, uniqueSensorType[i]]!= 0)
        currentValue = dataFinalAll[index[which(j < index)][1] , uniqueSensorType[i]]
        dataFinalAll[j, uniqueSensorType[i]] = currentValue
      }
    }
  }
  
  #Anomalies in the last 1 DAY period
  columnsName <- colnames(dataFinalAll)[which(colnames(dataFinalAll)!= "alignDateTime" & colnames(dataFinalAll)!= "date")]
  anomaliesMatr <- matrix(NA, ncol=2, nrow=1)
  colnames(anomaliesMatr) <- c("Timestamp","AnomalyValue")
  anomaliesMatrTemp <- anomaliesMatr
  
  dataFinal <- dataFinalAll[1:which(dataFinalAll$date == anomalyDate)[length(which(dataFinalAll$date == anomalyDate))], ]
  
  
  indfolder = 1
  indResult = 1
  statisticsResult = list()
  statisticsResult[indfolder]$statisticsOutputName = unbox("Anomalies")
  statisticsResult[[indfolder]]$statisticsOutputName = unbox("Anomalies")
  statisticsResult[[indfolder]]$resultFiles = list()
  
  for (i in 1: length(columnsName)){
    options(digits = 10)

    tryCatch({
      dat <- na.omit(dataFinal[ ,c("alignDateTime", columnsName[i])])
      res <- AnomalyDetectionTs(dat, max_anoms = 0.02, direction='both', alpha = 0.05,
                                plot=TRUE, only_last = "day", xlabel = "Date", ylabel = "Count", title = columnsName[i])
    }, error = function(e) {
      dat <- na.omit(dataFinal[ ,columnsName[i]])
      res <- AnomalyDetectionVec(as.data.frame(dat), max_anoms = 0.02, direction = "both", alpha = 0.05, period = 96, only_last = TRUE, 
                                 plot=TRUE, xlabel = "Date", ylabel = "Count", title = columnsName[i])
    }, finally = {
      
    })
    
    if (dim(res$anoms)[1]!=0){
      print(paste("PRESENCE OF ANOMALIES ON THE SENSOR ","- ",columnsName[i], "-", sep=""))
      anomaliesMatr <- matrix(NA, ncol=2, nrow=dim(res$anoms)[1])
      colnames(anomaliesMatr) <- colnames(res$anoms)
      
      tryCatch({
        anomaliesMatr[, "timestamp"] <- as.character(res$anoms[,"timestamp"])
        anomaliesMatr[, "anoms"] <- as.numeric(res$anoms[,"anoms"])
        #table with anomalies
        setwd(outWD)
        options(digits = 1)
        tBtable <- tableGrob(anomaliesMatr, rows = NULL, cols = c("Date and Time", "Anomaly"), theme=ttheme_default(base_size = 10, base_colour = "black"))
        grid.draw(tBtable)
        h <- convertHeight(sum(tBtable$heights), "in", TRUE)
        w <- convertWidth(sum(tBtable$widths), "in", TRUE)
        grid.draw(tBtable)
        h <- convertHeight(sum(tBtable$heights), "in", TRUE)
        w <- convertWidth(sum(tBtable$widths), "in", TRUE)

        plot <- res$plot
        
        plotMix <- grid.arrange(plot, tBtable,
                                ncol = 2,
                                heights=c(5,1),
                                as.table=TRUE)
        setwd(outWD)
        ggsave(paste(columnsName[i],"Anomalies.png", sep=""), plotMix, width=22, height=h+5)
        
        
      }, error = function(e) {
        anomaliesMatr[, "timestamp"] <- as.character(dataFinal[res$anoms$index ,"alignDateTime"])
        anomaliesMatr[, "anoms"] <- as.numeric(res$anoms[,"anoms"])
        
        #table with anomalies

        setwd(outWD)
        options(digits = 1)
        tBtable <- tableGrob(anomaliesMatr, rows = NULL, cols = c("Date and Time", "Anomaly"), theme=ttheme_default(base_size = 10, base_colour = "black"))
        grid.draw(tBtable)
        h <- convertHeight(sum(tBtable$heights), "in", TRUE)
        w <- convertWidth(sum(tBtable$widths), "in", TRUE)

        plot <- res$plot

        plotMix <- grid.arrange(plot, tBtable,
                                ncol = 2,
                                heights=c(5,1),
                                as.table=TRUE)
        setwd(outWD)
        ggsave(paste(columnsName[i],"Anomalies.png", sep=""), plotMix, width=22, height=h+5)
        
      }, finally = {
        
      })
      statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
      statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(as.character(columnsName[i]))
      statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, paste(columnsName[i], "Anomalies.png", sep=""), sep="/"))
      indResult = indResult + 1
      
      
    }else{
      print(paste("NO ANOMALIES ON THE SENSOR ", "-", columnsName[i], "-", sep=""))
    }

  }
  
  setwd("~/Snap4City")
  write(jsonlite::toJSON(statisticsResult[[1]]), "JsonStatisticsResult.json")
  return(statisticsResult[[1]])
}

