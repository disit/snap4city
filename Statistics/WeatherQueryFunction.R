#QUERY FROM PHOENIX
WeatherSensors <- function(currentDate, daysNumber) {
  
  finalDate <- as.Date(currentDate) - daysNumber
  startTime <- "00:00:00"
  endTime <- "23:59:00" 
  table <- c("WEATHERSENSOROBSERVATION") 
  tableNames <-  c("weatherSensorObservation")  

  myconn <- odbcConnect("Phoenix228") #open connection
  
    colNames <- sqlQuery(myconn, paste("SELECT COLUMN_NAME FROM SYSTEM.CATALOG WHERE TABLE_NAME = '", table, "'", sep=""))
    colNames <- as.character(colNames$COLUMN_NAME)
    colNames <- colNames[-1]
    
    if (length(grep("OBSERVATIONTIME", colNames)) != 0) {
      varToSelect <- colNames[-(grep("OBSERVATIONTIME", colNames))]
      timestamp <- colNames[(grep("OBSERVATIONTIME", colNames))]
      
    } else if (length(grep("OBSERVATIONTIME", colNames)) == 0) {
      varToSelect <- colNames[-(grep("ACQUISITIONTIME", colNames))]
      timestamp <- colNames[(grep("ACQUISITIONTIME", colNames))]
    }
    stringName <- colNames[1]
    
    # for (k in 2:length(colNames)){
    #   stringNameTemp <- colNames[k]
    #   stringName <- paste(stringName, stringNameTemp, sep=",")
    # }
    
    stringName <-"ACQUISITIONTIME,AIRHUMIDITY,AIRTEMPERATURE,ATMOSFERICPRESSURE,OBSERVATIONTIME,PRECIPATIONTYPE,RAIN,ROADCONDITION,ROADTEMPERATURE,SENSORID,SERVICEURI,WINDDIRECTION,WINDSPEED"
    selection <- paste("SELECT TO_CHAR(",timestamp,"),",stringName," FROM ", tableNames, 
                       " WHERE observationtime BETWEEN TO_TIMESTAMP('",finalDate," ", endTime,"') and TO_TIMESTAMP('",currentDate," ", startTime,"')", 
                       sep="")
    
    data <- sqlQuery(myconn, selection)
    colnames(data)[which(colnames(data) == "OBSERVATIONTIME")] = "DATE"
    colnames(data)[which(colnames(data) == "TO_CHAR(OBSERVATIONTIME)")] = "OBSERVATIONTIME"
    data <- data[, -(grep("DATE", colnames(data)))]
    
    setwd("~/Snap4City/Sensors Data/WeatherCSVFiles") 
    write.csv(data, paste(tableNames,".csv", sep=""))

  close(myconn) #close connection
  
  #------ Join of single sensors files into an unique dataset ----
  setwd("~/Snap4City/Sensors Data/WeatherCSVFiles")
  directory = "~/Snap4City/Sensors Data/WeatherCSVFiles"
  files <- list.files(directory)                                              
  
  dat <- read.csv(paste(directory, files[1], sep="/"))
  dat <- as.data.frame(dat[, -1])
  
  if (length(grep("OBSERVATIONTIME", colnames(dat))) == 0 ){
    colnames(dat)[which(colnames(dat) == "ACQUISITIONTIME")] = "OBSERVATIONTIME"
    
  }else if (length(which(colnames(dat) == "ACQUISITIONTIME")) > 0){
    dat <- dat[, -(which(colnames(dat) == "ACQUISITIONTIME"))]
  }
  
  if (length(which(colnames(dat) == "IDENTIFIER")) > 0){
    dat <- dat[, -(which(colnames(dat) == "IDENTIFIER"))]
  }
  
  if (length(which(colnames(dat) == "SENSORID")) > 0){
    dat <- dat[, -(which(colnames(dat) == "SENSORID"))]
  }
  
  #columns reordering 
  dat <- dat[, c(grep("SERVICEURI", colnames(dat)), grep("OBSERVATIONTIME", colnames(dat)), 
                 which(colnames(dat)!="SERVICEURI" & colnames(dat)!="OBSERVATIONTIME"))]
  
  #Automatic melt
  variableNames <- colnames(dat)[which(colnames(dat)!="SERVICEURI" & colnames(dat)!="OBSERVATIONTIME")]
  finalData <- matrix(NA, nrow=1, ncol=4)
  colnames(finalData) <- c("SERVICEURI","OBSERVATIONTIME", "variable", "value")
  finalData <- as.data.frame(finalData)
  
  for (i in 1:length(variableNames)){
    dataTemp <- dat[, c("SERVICEURI","OBSERVATIONTIME",variableNames[i])]
    meltingData <- melt(dataTemp, id=c("SERVICEURI","OBSERVATIONTIME"), na.rm = F)
    finalData <- rbind(finalData, meltingData)
  }
  finalData <- finalData[-1, ]
  
  
  #datetime format 
  finalData$date_time <- strptime(finalData$OBSERVATIONTIME, "%Y-%m-%d %H:%M") #CEST
  index <- grep("/", strsplit(as.character(finalData$SERVICEURI), "")[[1]])
  finalData[ , "identifier"] <- substr(finalData$SERVICEURI, index[length(index)]+1, nchar(finalData$SERVICEURI))
  
  
  finalData <- finalData[ , -(grep("OBSERVATIONTIME", colnames(finalData)))] 
  finalData <- finalData[ , -(grep("SERVICEURI", colnames(finalData)))]
  
  finalData <- finalData[, c(grep("identifier", colnames(finalData)), grep("date_time", colnames(finalData)), 
                 which(colnames(finalData)!="identifier" & colnames(finalData)!="date_time"))]
  
  finalData$date_time <- format(strptime(finalData$date_time, "%Y-%m-%d %H:%M"), "%Y-%m-%d %H:%M")
  finalData <- na.omit(finalData)
  
  #--------
  newDataset <- matrix(NA, ncol = 5, nrow = 1)
  newDataset <- as.data.frame(newDataset)
  colnames(newDataset) <- c("identifier","date_time","alignDateTime","value","variable")
  
  uniqueID <- unique(finalData$identifier)
  
  for (k in 1:length(uniqueID)){
    index <- which(finalData[,"identifier"] == uniqueID[k])
    dataTemp <- finalData[index, ]
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
  write.csv(DatasetCleanded, "WeatherDataset.csv")

}  